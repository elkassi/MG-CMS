package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.ShiftZoneConfirmation;
import com.lear.MGCMS.domain.dispatcher.ShiftZoneConfirmationMachine;
import com.lear.MGCMS.repositories.EtatMachineHistoriqueRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.ShiftZoneConfirmationMachineRepository;
import com.lear.MGCMS.repositories.dispatcher.ShiftZoneConfirmationRepository;

/**
 * Writes chef-de-zone confirmations for (date, shift, zone). One confirmation
 * row per triple, with machine children capturing the chef's up/down call.
 *
 * <p>A second {@link #confirm} call for the same triple is idempotent — it
 * updates the existing row and replaces its machine children. A
 * {@link ShiftZoneConfirmedEvent} is fired on every successful write.</p>
 *
 * <p>{@link #toggleMachine} flips one machine mid-shift and emits
 * {@link ZoneMachineToggledEvent} instead.</p>
 */
@Service
public class ShiftZoneConfirmationService {

    @Autowired private ShiftZoneConfirmationRepository confirmationRepository;
    @Autowired private ShiftZoneConfirmationMachineRepository machineRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired private EtatMachineHistoriqueRepository etatMachineRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;

    /**
     * Create or update the confirmation for (date, shift, zoneNom). The
     * {@code upMachineNoms} list is authoritative: any machine in the zone
     * that isn't in the list is written as {@code is_up = false}.
     */
    @Transactional
    public ShiftZoneConfirmation confirm(LocalDate date, int shift, String zoneNom,
                                         List<String> upMachineNoms, User confirmedBy) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zoneNom, "zoneNom");
        Objects.requireNonNull(confirmedBy, "confirmedBy");

        Zone zone = zoneRepository.findByObjId(zoneNom);
        if (zone == null) {
            throw new IllegalArgumentException("Unknown zone: " + zoneNom);
        }

        ShiftZoneConfirmation szc = confirmationRepository
                .findForTriple(date, shift, zoneNom)
                .orElseGet(() -> {
                    ShiftZoneConfirmation fresh = new ShiftZoneConfirmation();
                    fresh.setDateProduction(date);
                    fresh.setShiftNumber(shift);
                    fresh.setZone(zone);
                    return fresh;
                });
        szc.setConfirmedBy(confirmedBy);
        szc.setConfirmedAt(LocalDateTime.now());

        // Replace the machine child list — authoritative on every call.
        szc.getMachines().clear();
        Map<String, Boolean> requested = new LinkedHashMap<>();
        if (upMachineNoms != null) {
            for (String nm : upMachineNoms) requested.put(nm, true);
        }
        List<Object[]> machinesInZone =
                productionTableRepository.findMachinesWithTypeInZone(zoneNom);
        for (Object[] row : machinesInZone) {
            String machineNom = (String) row[0];
            boolean isUp = Boolean.TRUE.equals(requested.get(machineNom));
            szc.addMachine(machineNom, isUp);
        }

        ShiftZoneConfirmation saved = confirmationRepository.save(szc);
        List<String> normalizedUp = new ArrayList<>();
        for (ShiftZoneConfirmationMachine m : saved.getMachines()) {
            if (m.isUp()) normalizedUp.add(m.getMachineNom());
        }
        eventPublisher.publishEvent(new ShiftZoneConfirmedEvent(
                date, shift, zoneNom, confirmedBy.getMatricule(), normalizedUp));
        return saved;
    }

    /**
     * Flip one machine inside an existing confirmation. Throws if no
     * confirmation exists for the triple yet.
     */
    @Transactional
    public ShiftZoneConfirmationMachine toggleMachine(LocalDate date, int shift, String zoneNom,
                                                      String machineNom, boolean nowUp,
                                                      User toggledBy) {
        ShiftZoneConfirmation szc = confirmationRepository.findForTriple(date, shift, zoneNom)
                .orElseThrow(() -> new IllegalStateException(
                        "Zone not confirmed yet for " + date + " shift=" + shift + " zone=" + zoneNom));
        ShiftZoneConfirmationMachine target = null;
        for (ShiftZoneConfirmationMachine m : szc.getMachines()) {
            if (machineNom.equals(m.getMachineNom())) { target = m; break; }
        }
        if (target == null) {
            target = szc.addMachine(machineNom, nowUp);
        } else {
            target.setUp(nowUp);
        }
        confirmationRepository.save(szc);
        eventPublisher.publishEvent(new ZoneMachineToggledEvent(
                date, shift, zoneNom, machineNom, nowUp,
                toggledBy == null ? null : toggledBy.getMatricule()));
        return target;
    }

    /** Lookup shortcut for the REST layer. */
    @Transactional(readOnly = true)
    public Optional<ShiftZoneConfirmation> find(LocalDate date, int shift, String zoneNom) {
        return confirmationRepository.findForTriple(date, shift, zoneNom);
    }

    /** Every confirmation for a (date, shift) — chef Process & ops dashboards. */
    @Transactional(readOnly = true)
    public List<ShiftZoneConfirmation> findAllForShift(LocalDate date, int shift) {
        return confirmationRepository.findForShift(date, shift);
    }

    /**
     * Returns the existing confirmation if one exists, otherwise a synthesised
     * preview based on {@link EtatMachineHistoriqueRepository}: a machine is
     * pre-checked when its current {@code codeEtat} is {@code M} or null.
     *
     * <p>Always exposes every machine in the zone, even those missing from a
     * stale confirmation (newly-added since last save), and always reports
     * the machine's current {@code codeEtat} so the UI can show why each
     * checkbox starts in its given state.</p>
     *
     * <p>Wire shape (top-level keys):</p>
     * <ul>
     *   <li>{@code preview} — true when no confirmation exists yet</li>
     *   <li>{@code date}, {@code shift}, {@code zoneNom}</li>
     *   <li>{@code id}, {@code confirmedAt}, {@code confirmedByMatricule} — null in preview</li>
     *   <li>{@code machines} — list of {machineNom, machineType, isUp, codeEtat, currentStatusUp}</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public Map<String, Object> findOrPreview(LocalDate date, int shift, String zoneNom) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zoneNom, "zoneNom");

        // 1. Current EtatMachineHistorique status for every machine, batch-loaded
        //    once. Midday-of-shift sample is fine: the chef confirmation is a
        //    point-in-time signal, not a continuous stream.
        LocalDateTime atShift = date.atTime(12, 0);
        Map<String, String> statusByMachine = new HashMap<>();
        for (Object[] sr : etatMachineRepository.findAllCurrentStatuses(atShift)) {
            statusByMachine.put((String) sr[0], (String) sr[1]);
        }

        // 2. Every machine in the zone with its machine type.
        Map<String, String> typeByMachine = new LinkedHashMap<>();
        for (Object[] mr : productionTableRepository.findMachinesWithTypeInZone(zoneNom)) {
            typeByMachine.put((String) mr[0], (String) mr[1]);
        }

        Optional<ShiftZoneConfirmation> existing =
                confirmationRepository.findForTriple(date, shift, zoneNom);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date);
        result.put("shift", shift);
        result.put("zoneNom", zoneNom);

        List<Map<String, Object>> machineList = new ArrayList<>();
        if (existing.isPresent()) {
            ShiftZoneConfirmation szc = existing.get();
            Set<String> covered = new HashSet<>();
            for (ShiftZoneConfirmationMachine m : szc.getMachines()) {
                covered.add(m.getMachineNom());
                machineList.add(machineRow(
                        m.getMachineNom(),
                        typeByMachine.get(m.getMachineNom()),
                        m.isUp(),
                        statusByMachine.get(m.getMachineNom())));
            }
            // Surface machines added to the zone after the confirmation was
            // saved — pre-fill them from EtatMachineHistorique.
            for (Map.Entry<String, String> e : typeByMachine.entrySet()) {
                if (covered.contains(e.getKey())) continue;
                String code = statusByMachine.get(e.getKey());
                machineList.add(machineRow(
                        e.getKey(), e.getValue(), isUpFromStatus(code), code));
            }
            result.put("preview", false);
            result.put("id", szc.getId());
            result.put("confirmedAt", szc.getConfirmedAt());
            result.put("confirmedByMatricule",
                    szc.getConfirmedBy() == null ? null : szc.getConfirmedBy().getMatricule());
        } else {
            // No confirmation yet — synthesise from EtatMachineHistorique.
            for (Map.Entry<String, String> e : typeByMachine.entrySet()) {
                String code = statusByMachine.get(e.getKey());
                machineList.add(machineRow(
                        e.getKey(), e.getValue(), isUpFromStatus(code), code));
            }
            result.put("preview", true);
            result.put("id", null);
            result.put("confirmedAt", null);
            result.put("confirmedByMatricule", null);
        }
        result.put("machines", machineList);
        return result;
    }

    private static boolean isUpFromStatus(String codeEtat) {
        return codeEtat == null || "M".equalsIgnoreCase(codeEtat);
    }

    private static Map<String, Object> machineRow(String machineNom, String machineType,
                                                  boolean isUp, String codeEtat) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("machineNom", machineNom);
        row.put("machineType", machineType);
        row.put("isUp", isUp);
        row.put("codeEtat", codeEtat);
        row.put("currentStatusUp", isUpFromStatus(codeEtat));
        return row;
    }
}
