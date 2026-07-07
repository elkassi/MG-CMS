package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.services.CncPsService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.ctc.GammeTechniqueImprimerService;
import com.lear.cms.domain.GammeTechniqueImprimer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cncPs")
public class CncPsController {

    @Autowired
    private CncPsService cncPsService;

    @Autowired
    private GammeTechniqueImprimerService gammeTechniqueImprimerService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.lear.MGCMS.repositories.ProgrammeDistributionRepository programmeDistributionRepository;

    @Autowired
    private com.lear.MGCMS.repositories.CncControlRepository cncControlRepository;

    @Value("${cnc.template.path:C:\\\\template}")
    private String templatePath;

    // ===================== BOX DETAILS =====================

    @GetMapping("/boxDetails/{boxId}")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN') or hasRole('CNC_CONTROL') or hasRole('QUALITE')")
    public ResponseEntity<?> getBoxDetails(@PathVariable String boxId) {
        String serieStr = boxId.trim();
        if (serieStr.toUpperCase().startsWith("S")) {
            serieStr = serieStr.substring(1);
        }
        List<GammeTechniqueImprimer> results = gammeTechniqueImprimerService.findByNSerieGammeImp(serieStr);
        if (results == null || results.isEmpty()) {
            return new ResponseEntity<>("Box ID " + boxId + " n'est pas trouvé dans les gammes CMS", HttpStatus.BAD_REQUEST);
        }
        GammeTechniqueImprimer gamme = results.get(0);
        Map<String, Object> response = new HashMap<>();
        response.put("nSequenceImp", gamme.getnSequenceImp());
        response.put("partNumberImp", gamme.getPartNumberImp());
        response.put("code1Imp", gamme.getCode1Imp());
        response.put("code3Imp", gamme.getCode3Imp());
        response.put("quantiteImp", gamme.getQuantiteImp());

        // Check if session already exists for this boxId
        List<CncPsSession> existingSessions = cncPsService.findByBoxId(boxId.trim());
        if (existingSessions != null && !existingSessions.isEmpty()) {
            CncPsSession existingSession = existingSessions.get(0);
            response.put("existingSession", existingSession);
        }

        // Get CNC patterns from ProgramCNC (single source of truth — no ctc Files)
        String partNumberCover = gamme.getPartNumberImp();
        if (partNumberCover != null) {
            List<ProgramCNC> programs = cncPsService.findProgramCNCByPartNumber(partNumberCover.trim());
            List<Map<String, Object>> patternList = new ArrayList<>();
            if (programs != null) {
                for (ProgramCNC prog : programs) {
                    Map<String, Object> pm = new HashMap<>();
                    pm.put("panelNumber", prog.getPanelNumber());
                    pm.put("pattern", prog.getPattern());
                    pm.put("programNumber", prog.getProgramNumber());
                    pm.put("casette", prog.getCasette());
                    pm.put("version", prog.getVersion());
                    pm.put("row", prog.getRow());
                    pm.put("set", prog.getSet());
                    pm.put("coutureDecorativeCnc", prog.getCoutureDecorativeCnc());
                    pm.put("cavitePress", prog.getCavitePress());
                    pm.put("blindStitch", prog.getBlindStitch());
                    pm.put("profil", prog.getProfil());
                    pm.put("type", prog.getType());
                    patternList.add(pm);
                }
            }
            response.put("patterns", patternList);
        }

        return ResponseEntity.ok(response);
    }

    // ===================== SESSION CRUD =====================

    @PostMapping("/session")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN')")
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String boxId = (String) payload.get("boxId");
        if (boxId == null || boxId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("boxId est obligatoire");
        }

        // Check for duplicate boxId
        if (cncPsService.existsByBoxId(boxId.trim())) {
            return new ResponseEntity<>("Une session existe déjà pour la boîte " + boxId, HttpStatus.CONFLICT);
        }

        String serieStr = boxId.trim();
        if (serieStr.toUpperCase().startsWith("S")) {
            serieStr = serieStr.substring(1);
        }
        List<GammeTechniqueImprimer> results = gammeTechniqueImprimerService.findByNSerieGammeImp(serieStr);
        if (results == null || results.isEmpty()) {
            return new ResponseEntity<>("Box ID " + boxId + " n'est pas trouvé", HttpStatus.BAD_REQUEST);
        }

        GammeTechniqueImprimer gamme = results.get(0);
        CncPsSession session = new CncPsSession();
        session.setBoxId(boxId.trim());
        session.setnSequenceImp(gamme.getnSequenceImp());
        session.setPartNumberImp(gamme.getPartNumberImp());
        session.setCode1Imp(gamme.getCode1Imp());
        session.setCode3Imp(gamme.getCode3Imp());
        session.setQuantiteImp(gamme.getQuantiteImp());
        session.setOperator(authentication.getName());
        session.setCreatedAt(LocalDateTime.now());
        session.setCompleted(false);
        session.setLabelPrinted(false);

        CncPsSession saved = cncPsService.saveSession(session);
        return ResponseEntity.ok(saved);
    }

    // ===================== SERIAL CHECK =====================

    @GetMapping("/serialCheck/{serial}")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN')")
    public ResponseEntity<?> checkSerial(@PathVariable String serial) {
        List<CncPsLeatherConsumption> consumptions = cncPsService.findConsumptionsBySerial(serial.trim());
        Map<String, Object> result = new HashMap<>();
        if (consumptions == null || consumptions.isEmpty()) {
            result.put("found", false);
            return ResponseEntity.ok(result);
        }
        // Get the most recent consumption for this serial
        CncPsLeatherConsumption last = consumptions.get(0);
        result.put("found", true);
        result.put("lastRetour", last.getQuantiteRetour() != null ? last.getQuantiteRetour() : 0.0);
        result.put("lastLot", last.getLot());
        result.put("lastLeatherPartNumber", last.getLeatherPartNumber());
        result.put("lastQuantiteInitial", last.getQuantiteInitial());
        result.put("lastQuantiteConsumed", last.getQuantiteConsumed());
        return ResponseEntity.ok(result);
    }

    // ===================== CONSUMPTION =====================

    @PostMapping("/session/{sessionId}/consume")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN')")
    public ResponseEntity<?> addConsumption(@PathVariable Long sessionId,
                                             @RequestBody Map<String, Object> payload) {
        String leatherPartNumber = (String) payload.get("leatherPartNumber");
        String serial = (String) payload.get("serial");
        String lot = (String) payload.get("lot");
        Double quantiteInitial = payload.get("quantiteInitial") != null
                ? ((Number) payload.get("quantiteInitial")).doubleValue() : null;

        if (leatherPartNumber == null || serial == null || lot == null || quantiteInitial == null) {
            return ResponseEntity.badRequest().body("leatherPartNumber, serial, lot et quantiteInitial sont obligatoires");
        }

        Optional<CncPsSession> sessionOpt = cncPsService.findSessionById(sessionId);
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        CncPsSession session = sessionOpt.get();

        // Validate lot must start with "H"
        if (!lot.trim().toUpperCase().startsWith("H")) {
            return new ResponseEntity<>("Le lot doit commencer par 'H'", HttpStatus.BAD_REQUEST);
        }

        // Calculate quantiteConsumed: Math.min(remaining box quantity, quantiteInitial)
        double boxQuantity = 0;
        try {
            boxQuantity = Double.parseDouble(session.getQuantiteImp());
        } catch (NumberFormatException e) {
            return new ResponseEntity<>("Quantité boîte invalide", HttpStatus.BAD_REQUEST);
        }

        double totalAlreadyConsumed = cncPsService.getTotalConsumedForSession(sessionId);
        double remaining = boxQuantity - totalAlreadyConsumed;
        if (remaining <= 0) {
            return new ResponseEntity<>("La quantité de la boîte est déjà entièrement consommée", HttpStatus.BAD_REQUEST);
        }

        double quantiteConsumed = Math.min(remaining, quantiteInitial);
        double quantiteRetour = quantiteInitial - quantiteConsumed;

        CncPsLeatherConsumption consumption = cncPsService.addConsumption(
                sessionId, leatherPartNumber.trim(), serial.trim(), lot.trim(),
                quantiteInitial, quantiteConsumed, quantiteRetour);
        if (consumption == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("consumption", consumption);
        result.put("totalConsumed", totalAlreadyConsumed + quantiteConsumed);
        return ResponseEntity.ok(result);
    }

    // ===================== COMPLETE SESSION =====================

    @PostMapping("/session/{sessionId}/complete")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN')")
    public ResponseEntity<?> completeSession(@PathVariable Long sessionId) {
        Optional<CncPsSession> sessionOpt = cncPsService.findSessionById(sessionId);
        if (!sessionOpt.isPresent()) return ResponseEntity.notFound().build();
        CncPsSession session = sessionOpt.get();

        // If code1Imp exists (leather needed), validate total consumed == box qty
        if (session.getCode1Imp() != null && !session.getCode1Imp().trim().isEmpty()) {
            double boxQuantity = 0;
            try { boxQuantity = Double.parseDouble(session.getQuantiteImp()); }
            catch (NumberFormatException e) { return new ResponseEntity<>("Quantité de boîte invalide", HttpStatus.BAD_REQUEST); }

            double totalConsumed = cncPsService.getTotalConsumedForSession(sessionId);
            if (Math.abs(totalConsumed - boxQuantity) > 0.01) {
                return new ResponseEntity<>(
                        "La quantité consommée totale (" + totalConsumed + ") doit être égale à la quantité boîte (" + boxQuantity + ")",
                        HttpStatus.BAD_REQUEST);
            }
        }

        CncPsSession completed = cncPsService.completeSession(sessionId);
        return completed != null ? ResponseEntity.ok(completed) : ResponseEntity.notFound().build();
    }

    // ===================== QUALITY MANAGEMENT =====================

    @PostMapping("/session/{sessionId}/reopen")
    @PreAuthorize("hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> reopenSession(@PathVariable Long sessionId) {
        CncPsSession session = cncPsService.reopenSession(sessionId);
        return session != null ? ResponseEntity.ok(session) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteSession(@PathVariable Long sessionId) {
        Optional<CncPsSession> opt = cncPsService.findSessionById(sessionId);
        if (!opt.isPresent()) return ResponseEntity.notFound().build();
        cncPsService.deleteSession(sessionId);
        return ResponseEntity.ok("Session supprimée");
    }

    // ===================== CNC CONTROL =====================

    @PostMapping("/session/{sessionId}/startProduction")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> startProduction(@PathVariable Long sessionId,
                                              @RequestBody Map<String, Object> payload,
                                              Authentication authentication) {
        Optional<CncPsSession> opt = cncPsService.findSessionById(sessionId);
        if (!opt.isPresent()) return ResponseEntity.notFound().build();
        CncPsSession session = opt.get();

        // Validate: if code1Imp exists, leather consumption must be filled
        if (session.getCode1Imp() != null && !session.getCode1Imp().trim().isEmpty()) {
            if (!Boolean.TRUE.equals(session.getCompleted())) {
                return new ResponseEntity<>("La consommation cuir doit être complétée avant de démarrer la production", HttpStatus.BAD_REQUEST);
            }
        }

        Long machineCncId = payload.get("machineCncId") != null ? ((Number) payload.get("machineCncId")).longValue() : null;
        String startDateStr = (String) payload.get("startProductionDate");
        String endDateStr = (String) payload.get("endProductionDate");

        if (machineCncId == null) {
            return ResponseEntity.badRequest().body("machineCncId est obligatoire");
        }

        Optional<MachineCnc> machineOpt = cncPsService.findMachineById(machineCncId);
        if (!machineOpt.isPresent()) {
            return new ResponseEntity<>("Machine CNC non trouvée", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();

        // Parse start date - default to now if not provided
        LocalDateTime startDate;
        if (startDateStr != null && !startDateStr.isEmpty()) {
            startDateStr = startDateStr.replace("T", " ");
            startDate = LocalDateTime.parse(startDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } else {
            startDate = now;
        }

        // Parse end date - optional at start
        LocalDateTime endDate = null;
        if (endDateStr != null && !endDateStr.isEmpty()) {
            endDateStr = endDateStr.replace("T", " ");
            endDate = LocalDateTime.parse(endDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }

        session.setMachineCnc(machineOpt.get());
        session.setStartProductionDate(startDate);
        if (endDate != null) {
            session.setEndProductionDate(endDate);
        }
        session.setProductionStatus("In progress");
        session.setProductionOperator(authentication.getName());
        CncPsSession saved = cncPsService.saveSession(session);

        // Create per-programme production record if panelNumber is provided
        String panelNumber = (String) payload.get("panelNumber");
        String programmeNumber = payload.get("programmeNumber") != null ? payload.get("programmeNumber").toString() : null;
        String pattern = (String) payload.get("pattern");
        if (panelNumber != null && !panelNumber.isEmpty()) {
            CncProduction production = new CncProduction();
            production.setSession(saved);
            production.setPanelNumber(panelNumber);
            production.setProgrammeNumber(programmeNumber);
            production.setPattern(pattern);
            production.setMachineCnc(machineOpt.get());
            production.setStartDate(startDate);
            production.setStatus("In progress");
            production.setOperator(authentication.getName());
            cncPsService.saveProduction(production);
        }

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/session/{sessionId}/control")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> addControl(@PathVariable Long sessionId,
                                         @RequestBody Map<String, Object> payload,
                                         Authentication authentication) {
        Optional<CncPsSession> opt = cncPsService.findSessionById(sessionId);
        if (!opt.isPresent()) return ResponseEntity.notFound().build();
        CncPsSession session = opt.get();

        // Validate: if code1Imp exists, leather consumption must be completed
        if (session.getCode1Imp() != null && !session.getCode1Imp().trim().isEmpty()) {
            if (!Boolean.TRUE.equals(session.getCompleted())) {
                return new ResponseEntity<>("La consommation cuir doit être complétée avant le contrôle CNC", HttpStatus.BAD_REQUEST);
            }
        }

        Integer quantite = payload.get("quantite") != null ? ((Number) payload.get("quantite")).intValue() : null;
        String resultVal = (String) payload.get("result");
        String codeDefaut = (String) payload.get("codeDefaut");
        String codeScrap = (String) payload.get("codeScrap");
        String matricule = (String) payload.get("matricule");
        String programNumber = (String) payload.get("programNumber");
        String panelNumber = (String) payload.get("panelNumber");
        String patternVal = (String) payload.get("pattern");
        String numBonScrap = (String) payload.get("numBonScrap");
        String scrapStatus = (String) payload.get("scrapStatus");
        Long machineId = payload.get("machineId") != null ? ((Number) payload.get("machineId")).longValue() : null;
        String stage = payload.get("stage") != null ? payload.get("stage").toString().trim().toUpperCase() : "CNC";

        if (quantite == null || resultVal == null) {
            return ResponseEntity.badRequest().body("quantite et result sont obligatoires");
        }
        if (quantite <= 0) {
            return ResponseEntity.badRequest().body("La quantité doit être positive");
        }
        if (!"OK".equals(resultVal) && !"NOK".equals(resultVal)) {
            return ResponseEntity.badRequest().body("result doit être 'OK' ou 'NOK'");
        }
        if (!"CNC".equals(stage) && !"PRESS".equals(stage) && !"BLIND".equals(stage)) {
            return ResponseEntity.badRequest().body("stage doit être 'CNC', 'PRESS' ou 'BLIND'");
        }
        if ("NOK".equals(resultVal)) {
            if (codeDefaut == null || codeDefaut.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Pour un résultat NOK, le code défaut est obligatoire");
            }
            if (matricule == null || matricule.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Pour un résultat NOK, le matricule est obligatoire");
            }
        }

        // A machine must be selected for this stage before controlling (now per control row).
        if (machineId == null) {
            return new ResponseEntity<>("Veuillez sélectionner la machine " + stage + " avant de contrôler", HttpStatus.BAD_REQUEST);
        }
        if (!cncPsService.findMachineById(machineId).isPresent()) {
            return new ResponseEntity<>("Machine non trouvée", HttpStatus.BAD_REQUEST);
        }

        double boxQuantity;
        try { boxQuantity = Double.parseDouble(session.getQuantiteImp()); }
        catch (NumberFormatException e) { return new ResponseEntity<>("Quantité boîte invalide", HttpStatus.BAD_REQUEST); }
        int boxQty = (int) boxQuantity;

        // Each CNC programme is controlled separately against the box quantity.
        // The programme is identified by programNumber+panelNumber+pattern and chained per-programme.
        if ("BLIND".equals(stage)) {
            boolean blindOk = false;
            if (session.getPartNumberImp() != null) {
                for (ProgramCNC p : cncPsService.findProgramCNCByPartNumber(session.getPartNumberImp().trim())) {
                    if (nz(p.getProgramNumber()).equals(nz(programNumber))
                            && nz(p.getPanelNumber()).equals(nz(panelNumber))
                            && nz(p.getPattern()).equals(nz(patternVal))) {
                        blindOk = hasBlind(p.getBlindStitch());
                        break;
                    }
                }
            }
            if (!blindOk) {
                return new ResponseEntity<>("Ce programme n'a pas de fil blind — aucun contrôle Blind requis", HttpStatus.BAD_REQUEST);
            }
        }

        int cncControlled = cncPsService.getControlledQtyForStageAndProg(sessionId, "CNC", programNumber, panelNumber, patternVal);

        // Determine how many pieces this stage must control for this programme, gating on the previous stage
        int target;
        if ("CNC".equals(stage)) {
            target = boxQty;
        } else if ("PRESS".equals(stage)) {
            if (cncControlled < boxQty) {
                return new ResponseEntity<>("Terminez d'abord le contrôle CNC de ce programme", HttpStatus.BAD_REQUEST);
            }
            target = cncPsService.getPassingQtyForStageAndProg(sessionId, "CNC", programNumber, panelNumber, patternVal);
        } else { // BLIND
            if (cncControlled < boxQty) {
                return new ResponseEntity<>("Terminez d'abord le contrôle CNC de ce programme", HttpStatus.BAD_REQUEST);
            }
            int pressTarget = cncPsService.getPassingQtyForStageAndProg(sessionId, "CNC", programNumber, panelNumber, patternVal);
            if (cncPsService.getControlledQtyForStageAndProg(sessionId, "PRESS", programNumber, panelNumber, patternVal) < pressTarget) {
                return new ResponseEntity<>("Terminez d'abord le contrôle Press de ce programme", HttpStatus.BAD_REQUEST);
            }
            target = cncPsService.getPassingQtyForStageAndProg(sessionId, "PRESS", programNumber, panelNumber, patternVal);
        }

        int alreadyControlled = cncPsService.getControlledQtyForStageAndProg(sessionId, stage, programNumber, panelNumber, patternVal);
        if (alreadyControlled + quantite > target) {
            return new ResponseEntity<>("La quantité contrôlée pour l'étape " + stage + " (" + (alreadyControlled + quantite) +
                    ") dépasse la quantité à contrôler (" + target + ")", HttpStatus.BAD_REQUEST);
        }

        boolean nok = "NOK".equals(resultVal);
        boolean scrap = nok && codeScrap != null && !codeScrap.trim().isEmpty();
        CncControl control = cncPsService.addControl(sessionId, quantite, resultVal,
                nok ? codeDefaut : null,
                nok ? codeScrap : null,
                matricule, stage,
                programNumber, panelNumber, patternVal,
                scrap ? numBonScrap : null,
                scrap ? scrapStatus : null,
                machineId);
        if (control == null) return ResponseEntity.notFound().build();

        recomputeQualite(session, authentication.getName());

        return ResponseEntity.ok(control);
    }

    @DeleteMapping("/control/{controlId}")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteControl(@PathVariable Long controlId, Authentication authentication) {
        Optional<CncControl> controlOpt = cncPsService.findControlById(controlId);
        if (!controlOpt.isPresent()) return ResponseEntity.notFound().build();

        // ROLE_CNC_CONTROL can't modify after 24 hours
        if (isRoleCncControlOnly(authentication)) {
            CncControl control = controlOpt.get();
            if (control.getCreatedAt() != null && ChronoUnit.HOURS.between(control.getCreatedAt(), LocalDateTime.now()) > 24) {
                return new ResponseEntity<>("Les contrôleurs CNC ne peuvent pas modifier après 24 heures", HttpStatus.FORBIDDEN);
            }
        }

        Long sid = controlOpt.get().getSession() != null ? controlOpt.get().getSession().getId() : null;
        boolean deleted = cncPsService.deleteControl(controlId);
        if (deleted && sid != null) {
            cncPsService.findSessionById(sid).ifPresent(s -> recomputeQualite(s, authentication.getName()));
        }
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/control/{controlId}/scrap")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> updateControlScrap(@PathVariable Long controlId,
                                                @RequestBody Map<String, Object> payload,
                                                Authentication authentication) {
        Optional<CncControl> controlOpt = cncPsService.findControlById(controlId);
        if (!controlOpt.isPresent()) return ResponseEntity.notFound().build();
        String numBonScrap = (String) payload.get("numBonScrap");
        String scrapStatus = (String) payload.get("scrapStatus");
        if (scrapStatus != null && scrapStatus.trim().isEmpty()) scrapStatus = null;
        CncControl updated = cncPsService.updateControlScrap(controlId,
                numBonScrap != null && numBonScrap.trim().isEmpty() ? null : numBonScrap, scrapStatus);
        if (updated == null) return ResponseEntity.notFound().build();
        if (updated.getSession() != null) {
            cncPsService.findSessionById(updated.getSession().getId())
                    .ifPresent(s -> recomputeQualite(s, authentication.getName()));
        }
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/session/{sessionId}/controls")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public List<CncControl> getControls(@PathVariable Long sessionId) {
        return cncPsService.findControlsBySession(sessionId);
    }

    @GetMapping("/session/{sessionId}/productions")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public List<CncProduction> getProductions(@PathVariable Long sessionId) {
        return cncPsService.findProductionsBySession(sessionId);
    }

    @PostMapping("/session/{sessionId}/completeProduction")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> completeProduction(@PathVariable Long sessionId,
                                                 @RequestBody Map<String, Object> payload) {
        Optional<CncPsSession> opt = cncPsService.findSessionById(sessionId);
        if (!opt.isPresent()) return ResponseEntity.notFound().build();
        CncPsSession session = opt.get();

        // Parse end date from payload
        String endDateStr = (String) payload.get("endProductionDate");
        if (endDateStr == null || endDateStr.isEmpty()) {
            return ResponseEntity.badRequest().body("La date de fin de production est obligatoire pour terminer la production");
        }
        endDateStr = endDateStr.replace("T", " ");
        LocalDateTime endDate = LocalDateTime.parse(endDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // Check if this is a per-production completion
        Long productionId = payload.get("productionId") != null ? ((Number) payload.get("productionId")).longValue() : null;
        if (productionId != null) {
            // Complete a specific production
            Optional<CncProduction> prodOpt = cncPsService.findProductionById(productionId);
            if (!prodOpt.isPresent()) return ResponseEntity.badRequest().body("Production non trouvée");
            CncProduction production = prodOpt.get();

            double boxQuantity = 0;
            try { boxQuantity = Double.parseDouble(session.getQuantiteImp()); }
            catch (NumberFormatException e) { return new ResponseEntity<>("Quantité boîte invalide", HttpStatus.BAD_REQUEST); }

            int productionControlQty = cncPsService.getTotalControlQuantityForSessionAndPanel(sessionId, production.getPanelNumber());
            if (Math.abs(productionControlQty - boxQuantity) > 0.01) {
                return new ResponseEntity<>(
                        "La quantité contrôlée du panel " + production.getPanelNumber() + " (" + productionControlQty + ") doit être égale à la quantité boîte (" + (int) boxQuantity + ")",
                        HttpStatus.BAD_REQUEST);
            }

            production.setEndDate(endDate);
            production.setStatus("Complete");
            cncPsService.saveProduction(production);

            // Check if all productions for this session are complete
            List<CncProduction> activeProductions = cncPsService.findActiveProductionsBySession(sessionId);
            if (activeProductions.isEmpty()) {
                // All productions done -> complete session
                session.setEndProductionDate(endDate);
                session.setProductionStatus("Complete");
                cncPsService.saveSession(session);
            }
            return ResponseEntity.ok(session);
        }

        // Single-programme completion (legacy)
        double boxQuantity = 0;
        try { boxQuantity = Double.parseDouble(session.getQuantiteImp()); }
        catch (NumberFormatException e) { return new ResponseEntity<>("Quantité boîte invalide", HttpStatus.BAD_REQUEST); }

        int totalControlQty = cncPsService.getTotalControlQuantityForSession(sessionId);
        if (Math.abs(totalControlQty - boxQuantity) > 0.01) {
            return new ResponseEntity<>(
                    "La quantité contrôlée (" + totalControlQty + ") doit être égale à la quantité boîte (" + (int) boxQuantity + ")",
                    HttpStatus.BAD_REQUEST);
        }

        session.setEndProductionDate(endDate);
        session.setProductionStatus("Complete");
        return ResponseEntity.ok(cncPsService.saveSession(session));
    }

    // ===================== CODES LISTS =====================

    @GetMapping("/codesDefautCNC")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public List<Map<String, String>> codesDefautCNC() {
        return cncPsService.findCodeDefautCNC().stream().map(cd -> {
            Map<String, String> m = new HashMap<>();
            m.put("code", cd.getCode());
            m.put("description", cd.getDescription());
            m.put("label", cd.getCode() + " - " + (cd.getDescription() != null ? cd.getDescription() : ""));
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/codesScrapCNC")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public List<Map<String, String>> codesScrapCNC() {
        return cncPsService.findCodeScrapCNC().stream().map(cs -> {
            Map<String, String> m = new HashMap<>();
            m.put("code", cs.getCode());
            m.put("description", cs.getDescription());
            m.put("label", cs.getCode() + " - " + (cs.getDescription() != null ? cs.getDescription() : ""));
            return m;
        }).collect(Collectors.toList());
    }

    // ===================== MACHINES =====================

    @GetMapping("/machines")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public List<MachineCnc> allMachines() {
        return cncPsService.findAllMachines();
    }

    @PostMapping("/machines")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveMachine(@RequestBody MachineCnc machine) {
        return ResponseEntity.ok(cncPsService.saveMachine(machine));
    }

    @GetMapping("/machinesByProgramme/{programmeNumber}")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public List<MachineCnc> machinesByProgramme(@PathVariable String programmeNumber) {
        try {
            Integer progNum = Integer.parseInt(programmeNumber.trim());
            List<com.lear.MGCMS.domain.ProgrammeDistribution> distributions =
                    programmeDistributionRepository.findByProgrammeNumber(progNum);
            if (distributions != null && !distributions.isEmpty()) {
                return distributions.stream()
                        .map(com.lear.MGCMS.domain.ProgrammeDistribution::getMachine)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
            }
        } catch (NumberFormatException ignored) {}
        // Fallback: return all machines
        return cncPsService.findAllMachines();
    }

    // ===================== PRINTER =====================

    @GetMapping("/printerStatus")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN') or hasRole('CNC_CONTROL') or hasRole('QUALITE')")
    public ResponseEntity<?> checkPrinterStatus(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Map<String, Object> result = new HashMap<>();

        // Check network printer (IP)
        boolean networkConnected = false;
        String ipPrinter = (user != null && user.getIpPrinter() != null) ? user.getIpPrinter().trim() : "";
        if (!ipPrinter.isEmpty()) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ipPrinter, 9100), 2000);
                socket.close();
                networkConnected = true;
            } catch (Exception e) { /* not connected */ }
        }
        result.put("networkConnected", networkConnected);
        result.put("ip", ipPrinter);

        // Check local ZEBRA printer
        boolean localZebraConnected = false;
        String localZebraName = "";
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService ps : printServices) {
                if (ps.getName().toUpperCase().contains("ZEBRA")) {
                    localZebraConnected = true;
                    localZebraName = ps.getName();
                    break;
                }
            }
        } catch (Exception e) { /* ignore */ }
        result.put("localZebraConnected", localZebraConnected);
        result.put("localZebraName", localZebraName);

        result.put("connected", networkConnected || localZebraConnected);
        result.put("message", networkConnected ? "Imprimante réseau connectée (" + ipPrinter + ")"
                : localZebraConnected ? "Imprimante locale ZEBRA disponible (" + localZebraName + ")"
                : "Aucune imprimante disponible");

        return ResponseEntity.ok(result);
    }

    /**
     * Send ZPL to the best available printer: network first, fallback to local ZEBRA.
     */
    private void sendZpl(String zpl, User user) throws Exception {
        String ipPrinter = (user != null && user.getIpPrinter() != null) ? user.getIpPrinter().trim() : "";

        // Try network printer first
        if (!ipPrinter.isEmpty()) {
            try {
                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(ipPrinter, 9100), 3000);
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                out.writeBytes(zpl);
                out.flush();
                clientSocket.close();
                return;
            } catch (Exception e) { /* Fall through to local printer */ }
        }

        // Try local ZEBRA printer
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService zebraPrinter = null;
        for (PrintService ps : printServices) {
            if (ps.getName().toUpperCase().contains("ZEBRA")) {
                zebraPrinter = ps;
                break;
            }
        }
        if (zebraPrinter != null) {
            javax.print.Doc doc = new javax.print.SimpleDoc(
                    zpl.getBytes(), javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            javax.print.DocPrintJob job = zebraPrinter.createPrintJob();
            job.print(doc, null);
            return;
        }

        throw new Exception("Aucune imprimante disponible (ni réseau ni locale ZEBRA)");
    }

    @PostMapping("/session/{sessionId}/printLeatherReturn")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN')")
    public ResponseEntity<?> printLeatherReturn(@PathVariable Long sessionId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Optional<CncPsSession> sessionOpt = cncPsService.findSessionById(sessionId);
        if (!sessionOpt.isPresent()) return ResponseEntity.notFound().build();

        CncPsSession session = sessionOpt.get();
        List<CncPsLeatherConsumption> consumptions = cncPsService.findConsumptionsBySession(sessionId);
        List<CncPsLeatherConsumption> retourConsumptions = consumptions.stream()
                .filter(c -> c.getQuantiteRetour() != null && c.getQuantiteRetour() > 0)
                .collect(Collectors.toList());
        if (retourConsumptions.isEmpty()) {
            return new ResponseEntity<>("Aucune consommation avec retour à imprimer", HttpStatus.BAD_REQUEST);
        }

        try {
            String template = loadTemplate("leather_return.prn");
            for (CncPsLeatherConsumption c : retourConsumptions) {
                String zpl;
                if (template != null) {
                    zpl = applyLeatherReturnTemplate(template, c, session, consumptions);
                } else {
                    zpl = buildLeatherReturnZpl(c, session, consumptions);
                }
                sendZpl(zpl, user);
            }
            cncPsService.markLabelPrinted(sessionId);
            return ResponseEntity.ok("Étiquette(s) retour cuir imprimée(s): " + retourConsumptions.size());
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur imprimante: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/session/{sessionId}/printBoxLabel")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN')")
    public ResponseEntity<?> printBoxLabel(@PathVariable Long sessionId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Optional<CncPsSession> sessionOpt = cncPsService.findSessionById(sessionId);
        if (!sessionOpt.isPresent()) return ResponseEntity.notFound().build();

        CncPsSession session = sessionOpt.get();
        List<CncPsLeatherConsumption> consumptions = cncPsService.findConsumptionsBySession(sessionId);
        List<Map<String, String>> patternPrograms = getPatternPrograms(session);

        try {
            String template = loadTemplate("box_label.prn");
            String zpl;
            if (template != null) {
                zpl = applyBoxLabelTemplate(template, session, consumptions, patternPrograms);
            } else {
                zpl = buildBoxLabelZpl(session, consumptions, patternPrograms);
            }
            sendZpl(zpl, user);
            return ResponseEntity.ok("Étiquette boîte imprimée");
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur imprimante: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // ===================== SESSION GETTERS =====================

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN') or hasRole('CNC_CONTROL') or hasRole('QUALITE')")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId) {
        Optional<CncPsSession> sessionOpt = cncPsService.findSessionById(sessionId);
        return sessionOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/myRecent")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN') or hasRole('CNC_CONTROL') or hasRole('QUALITE')")
    public List<CncPsSession> myRecentSessions(Authentication authentication) {
        return cncPsService.findRecentSessionsByOperator(authentication.getName(), 8);
    }

    @GetMapping("/all")
    public Page<CncPsSession> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageRequest.of(page, size,
                dir.equals("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());
        return cncPsService.findAllSessions(pageable);
    }

    @GetMapping("/session/{sessionId}/consumptions")
    public List<CncPsLeatherConsumption> getConsumptions(@PathVariable Long sessionId) {
        return cncPsService.findConsumptionsBySession(sessionId);
    }

    @DeleteMapping("/consumption/{consumptionId}")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN') or hasRole('QUALITE')")
    public ResponseEntity<?> deleteConsumption(@PathVariable Long consumptionId) {
        return cncPsService.deleteConsumption(consumptionId) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // ===================== SHIFT STATUS =====================

    @GetMapping("/shiftStatus")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN') or hasRole('CNC_CONTROL') or hasRole('QUALITE')")
    public ResponseEntity<?> shiftStatus(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer shift) {

        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        int targetShift = shift != null ? shift : getCurrentShift();

        LocalDateTime shiftStart, shiftEnd;
        switch (targetShift) {
            case 1: // 21:50 previous day to 05:50 current day
                shiftStart = targetDate.minusDays(1).atTime(LocalTime.of(21, 50));
                shiftEnd = targetDate.atTime(LocalTime.of(5, 50));
                break;
            case 2: // 05:50 to 13:50
                shiftStart = targetDate.atTime(LocalTime.of(5, 50));
                shiftEnd = targetDate.atTime(LocalTime.of(13, 50));
                break;
            case 3: // 13:50 to 21:50
                shiftStart = targetDate.atTime(LocalTime.of(13, 50));
                shiftEnd = targetDate.atTime(LocalTime.of(21, 50));
                break;
            default:
                shiftStart = targetDate.atTime(LocalTime.of(5, 50));
                shiftEnd = targetDate.atTime(LocalTime.of(13, 50));
        }

        List<CncPsSession> sessions = cncPsService.findSessionsBetween(shiftStart, shiftEnd);

        Map<String, Object> result = new HashMap<>();
        result.put("date", targetDate.toString());
        result.put("shift", targetShift);
        result.put("shiftLabel", "Shift " + targetShift);
        result.put("shiftStart", shiftStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        result.put("shiftEnd", shiftEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        result.put("totalSessions", sessions.size());
        result.put("completedSessions", sessions.stream().filter(s -> Boolean.TRUE.equals(s.getCompleted())).count());
        result.put("inProgressSessions", sessions.stream().filter(s -> !Boolean.TRUE.equals(s.getCompleted())).count());
        result.put("totalBoxQuantity", sessions.stream()
                .mapToDouble(s -> { try { return Double.parseDouble(s.getQuantiteImp()); } catch (Exception e) { return 0; } }).sum());

        result.put("waitingSessions", sessions.stream().filter(s -> s.getProductionStatus() == null || "Waiting".equals(s.getProductionStatus())).count());
        result.put("productionInProgress", sessions.stream().filter(s -> "In progress".equals(s.getProductionStatus())).count());
        result.put("productionComplete", sessions.stream().filter(s -> "Complete".equals(s.getProductionStatus())).count());

        Map<String, Long> byOperator = sessions.stream()
                .filter(s -> s.getOperator() != null)
                .collect(Collectors.groupingBy(CncPsSession::getOperator, Collectors.counting()));
        result.put("byOperator", byOperator);

        Map<String, Long> byPartNumber = sessions.stream()
                .filter(s -> s.getPartNumberImp() != null)
                .collect(Collectors.groupingBy(CncPsSession::getPartNumberImp, Collectors.counting()));
        result.put("byPartNumber", byPartNumber);

        int totalOK = 0, totalNOK = 0;
        for (CncPsSession s : sessions) {
            if (s.getControls() != null) {
                for (CncControl c : s.getControls()) {
                    if ("OK".equals(c.getResult())) totalOK += c.getQuantite() != null ? c.getQuantite() : 0;
                    if ("NOK".equals(c.getResult())) totalNOK += c.getQuantite() != null ? c.getQuantite() : 0;
                }
            }
        }
        result.put("totalControlOK", totalOK);
        result.put("totalControlNOK", totalNOK);
        result.put("sessions", sessions);
        return ResponseEntity.ok(result);
    }

    // ===================== KPI =====================

    @GetMapping("/kpi")
    @PreAuthorize("hasRole('CNC_PS') or hasRole('ADMIN') or hasRole('CNC_CONTROL') or hasRole('QUALITE')")
    public ResponseEntity<?> kpi(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);

        List<CncPsSession> sessions = cncPsService.findSessionsBetween(start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalSessions", sessions.size());
        result.put("completedSessions", sessions.stream().filter(s -> Boolean.TRUE.equals(s.getCompleted())).count());
        result.put("inProgressSessions", sessions.stream().filter(s -> !Boolean.TRUE.equals(s.getCompleted())).count());

        double totalBoxQty = sessions.stream()
                .mapToDouble(s -> { try { return Double.parseDouble(s.getQuantiteImp()); } catch (Exception e) { return 0; } }).sum();
        result.put("totalBoxQuantity", totalBoxQty);

        result.put("productionWaiting", sessions.stream().filter(s -> s.getProductionStatus() == null || "Waiting".equals(s.getProductionStatus())).count());
        result.put("productionInProgress", sessions.stream().filter(s -> "In progress".equals(s.getProductionStatus())).count());
        result.put("productionComplete", sessions.stream().filter(s -> "Complete".equals(s.getProductionStatus())).count());

        int totalOK = 0, totalNOK = 0;
        Map<String, Integer> defautCounts = new HashMap<>();
        Map<String, Integer> scrapCounts = new HashMap<>();
        for (CncPsSession s : sessions) {
            if (s.getControls() != null) {
                for (CncControl c : s.getControls()) {
                    int qty = c.getQuantite() != null ? c.getQuantite() : 0;
                    if ("OK".equals(c.getResult())) totalOK += qty;
                    if ("NOK".equals(c.getResult())) {
                        totalNOK += qty;
                        if (c.getCodeDefaut() != null && !c.getCodeDefaut().isEmpty())
                            defautCounts.merge(c.getCodeDefaut(), qty, Integer::sum);
                        if (c.getCodeScrap() != null && !c.getCodeScrap().isEmpty())
                            scrapCounts.merge(c.getCodeScrap(), qty, Integer::sum);
                    }
                }
            }
        }
        result.put("totalControlOK", totalOK);
        result.put("totalControlNOK", totalNOK);
        result.put("qualityRate", (totalOK + totalNOK) > 0 ? Math.round(((double) totalOK / (totalOK + totalNOK)) * 10000.0) / 100.0 : 100.0);
        result.put("defautBreakdown", defautCounts);
        result.put("scrapBreakdown", scrapCounts);

        Map<String, Long> byDay = sessions.stream()
                .filter(s -> s.getCreatedAt() != null)
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().toLocalDate().toString(), Collectors.counting()));
        result.put("byDay", new TreeMap<>(byDay));

        Map<String, Long> byPartNumber = sessions.stream()
                .filter(s -> s.getPartNumberImp() != null)
                .collect(Collectors.groupingBy(CncPsSession::getPartNumberImp, Collectors.counting()));
        result.put("byPartNumber", byPartNumber);

        Map<String, Long> byOperator = sessions.stream()
                .filter(s -> s.getOperator() != null)
                .collect(Collectors.groupingBy(CncPsSession::getOperator, Collectors.counting()));
        result.put("byOperator", byOperator);

        Map<String, Long> byMachine = sessions.stream()
                .filter(s -> s.getMachineCnc() != null)
                .collect(Collectors.groupingBy(s -> s.getMachineCnc().getName(), Collectors.counting()));
        result.put("byMachine", byMachine);

        return ResponseEntity.ok(result);
    }

    // ===================== MACHINE QUALITY REPORT =====================

    /**
     * All CNC quality controls performed for a machine on a given date + shift, each row enriched
     * with its session's details. machine optional (null = all machines), date required,
     * shift optional (1/2/3; null = whole production day).
     */
    @GetMapping("/machineQualiteReport")
    @PreAuthorize("hasRole('CNC_CONTROL') or hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> machineQualiteReport(
            @RequestParam(required = false) Long machineId,
            @RequestParam String date,
            @RequestParam(required = false) Integer shift) {

        LocalDate targetDate = LocalDate.parse(date);
        LocalDateTime[] window = shiftWindow(targetDate, shift);
        LocalDateTime shiftStart = window[0], shiftEnd = window[1];

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> rows = new ArrayList<>();
        int totalOK = 0, totalNOK = 0;
        for (CncControl c : cncControlRepository.findControlsWithSessionBetween(shiftStart, shiftEnd)) {
            if (machineId != null && (c.getMachine() == null || !machineId.equals(c.getMachine().getId()))) continue;
            CncPsSession s = c.getSession();
            int qty = c.getQuantite() != null ? c.getQuantite() : 0;
            if ("OK".equals(c.getResult())) totalOK += qty;
            else if ("NOK".equals(c.getResult())) totalNOK += qty;

            Map<String, Object> m = new HashMap<>();
            m.put("controlId", c.getId());
            m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(dtf) : null);
            m.put("machine", c.getMachine() != null ? c.getMachine().getName() : null);
            m.put("stage", c.getStage());
            m.put("result", c.getResult());
            m.put("quantite", c.getQuantite());
            m.put("codeDefaut", c.getCodeDefaut());
            m.put("codeScrap", c.getCodeScrap());
            m.put("matricule", c.getMatricule());
            m.put("programNumber", c.getProgramNumber());
            m.put("panelNumber", c.getPanelNumber());
            m.put("pattern", c.getPattern());
            m.put("numBonScrap", c.getNumBonScrap());
            m.put("scrapStatus", c.getScrapStatus());
            m.put("boxId", s != null ? s.getBoxId() : null);
            m.put("partNumberImp", s != null ? s.getPartNumberImp() : null);
            m.put("boxQuantite", s != null ? s.getQuantiteImp() : null);
            m.put("code1Imp", s != null ? s.getCode1Imp() : null);
            m.put("operator", s != null ? s.getOperator() : null);
            m.put("qualiteStatus", s != null ? s.getQualiteStatus() : null);
            m.put("userQualite", s != null ? s.getUserQualite() : null);
            rows.add(m);
        }
        rows.sort(Comparator.comparing(r -> r.get("createdAt") != null ? (String) r.get("createdAt") : ""));

        Map<String, Object> result = new HashMap<>();
        result.put("date", targetDate.toString());
        result.put("shift", shift);
        result.put("shiftStart", shiftStart.format(dtf));
        result.put("shiftEnd", shiftEnd.format(dtf));
        result.put("count", rows.size());
        result.put("totalOK", totalOK);
        result.put("totalNOK", totalNOK);
        result.put("rows", rows);
        return ResponseEntity.ok(result);
    }

    // ===================== HELPERS =====================

    /** Shift time window (plant convention); shift null/invalid → whole production day (21:50 prev → 21:50). */
    private LocalDateTime[] shiftWindow(LocalDate targetDate, Integer shift) {
        LocalDateTime start, end;
        switch (shift != null ? shift : 0) {
            case 1:
                start = targetDate.minusDays(1).atTime(LocalTime.of(21, 50));
                end = targetDate.atTime(LocalTime.of(5, 50));
                break;
            case 2:
                start = targetDate.atTime(LocalTime.of(5, 50));
                end = targetDate.atTime(LocalTime.of(13, 50));
                break;
            case 3:
                start = targetDate.atTime(LocalTime.of(13, 50));
                end = targetDate.atTime(LocalTime.of(21, 50));
                break;
            default:
                start = targetDate.minusDays(1).atTime(LocalTime.of(21, 50));
                end = targetDate.atTime(LocalTime.of(21, 50));
        }
        return new LocalDateTime[]{start, end};
    }

    private int getCurrentShift() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(5, 50))) return 1;
        if (now.isBefore(LocalTime.of(13, 50))) return 2;
        if (now.isBefore(LocalTime.of(21, 50))) return 3;
        return 1;
    }

    private boolean isRoleCncControlOnly(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        if (user == null || user.getRoles() == null) return false;
        boolean hasCncControl = false, hasAdmin = false, hasQualite = false;
        for (Role role : user.getRoles()) {
            if ("ROLE_CNC_CONTROL".equals(role.getName())) hasCncControl = true;
            if ("ROLE_ADMIN".equals(role.getName())) hasAdmin = true;
            if ("ROLE_QUALITE".equals(role.getName())) hasQualite = true;
        }
        return hasCncControl && !hasAdmin && !hasQualite;
    }

    /** Recompute the session's quality status/user/dates from its controls (after add or delete). */
    private void recomputeQualite(CncPsSession session, String username) {
        Long sid = session.getId();
        int totalControlled = cncPsService.getTotalControlQuantityForSession(sid);
        if (totalControlled <= 0) {
            session.setQualiteStatus(null);
            session.setUserQualite(null);
            session.setStartDateControl(null);
            session.setEndDateControl(null);
            cncPsService.saveSession(session);
            return;
        }
        if (session.getStartDateControl() == null) session.setStartDateControl(LocalDateTime.now());
        if (session.getUserQualite() == null) session.setUserQualite(username);

        int boxQty;
        try { boxQty = (int) Double.parseDouble(session.getQuantiteImp()); }
        catch (NumberFormatException e) { boxQty = 0; }

        List<ProgramCNC> programs = session.getPartNumberImp() != null
                ? cncPsService.findProgramCNCByPartNumber(session.getPartNumberImp().trim())
                : Collections.emptyList();
        // Terminé only when every programme has finished all of its active stages (per-programme chaining).
        boolean allDone = boxQty > 0;
        if (programs == null || programs.isEmpty()) {
            // Legacy: no CNC programmes — single implicit bucket (null key).
            boolean cncDone = cncPsService.getControlledQtyForStageAndProg(sid, "CNC", null, null, null) >= boxQty;
            boolean pressDone = cncPsService.getControlledQtyForStageAndProg(sid, "PRESS", null, null, null)
                    >= cncPsService.getPassingQtyForStageAndProg(sid, "CNC", null, null, null);
            allDone = allDone && cncDone && pressDone;
        } else {
            for (ProgramCNC p : programs) {
                String pn = p.getProgramNumber(), panel = p.getPanelNumber(), pat = p.getPattern();
                boolean cncDone = cncPsService.getControlledQtyForStageAndProg(sid, "CNC", pn, panel, pat) >= boxQty;
                boolean pressDone = cncPsService.getControlledQtyForStageAndProg(sid, "PRESS", pn, panel, pat)
                        >= cncPsService.getPassingQtyForStageAndProg(sid, "CNC", pn, panel, pat);
                boolean blindDone = !hasBlind(p.getBlindStitch())
                        || cncPsService.getControlledQtyForStageAndProg(sid, "BLIND", pn, panel, pat)
                                >= cncPsService.getPassingQtyForStageAndProg(sid, "PRESS", pn, panel, pat);
                allDone = allDone && cncDone && pressDone && blindDone;
            }
        }

        // Tally scrap statuses across the box: pending scraps keep it waiting; non-remplaçable shortfalls
        // are reported in the status; remplaçé scraps are treated as resolved.
        int pendingScrap = 0, nonReplaceable = 0;
        for (CncControl c : cncPsService.findControlsBySession(sid)) {
            if (c.getCodeScrap() == null || c.getCodeScrap().trim().isEmpty()) continue;
            int q = c.getQuantite() != null ? c.getQuantite() : 0;
            String st = c.getScrapStatus() == null ? "" : c.getScrapStatus().trim();
            if ("REMPLACE".equals(st)) continue;          // resolved
            else if ("NON_REMPLACABLE".equals(st)) nonReplaceable += q;
            else pendingScrap += q;                        // null / EN_ATTENTE_VALIDATION / EN_ATTENTE_MATIERE
        }

        if (!allDone || pendingScrap > 0) {
            session.setQualiteStatus("En cours");
            session.setEndDateControl(null);
        } else if (nonReplaceable > 0) {
            session.setQualiteStatus("Terminé (-" + nonReplaceable + " pièce" + (nonReplaceable > 1 ? "s" : "") + ")");
            if (session.getEndDateControl() == null) session.setEndDateControl(LocalDateTime.now());
        } else {
            session.setQualiteStatus("Terminé");
            if (session.getEndDateControl() == null) session.setEndDateControl(LocalDateTime.now());
        }
        cncPsService.saveSession(session);
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    /** A programme has blind stitching only when its fil blind is present and at least 4 chars long. */
    private static boolean hasBlind(String blindStitch) {
        return blindStitch != null && blindStitch.trim().length() >= 4;
    }

    private List<Map<String, String>> getPatternPrograms(CncPsSession session) {
        List<Map<String, String>> patternPrograms = new ArrayList<>();
        if (session.getPartNumberImp() != null) {
            List<ProgramCNC> programs = cncPsService.findProgramCNCByPartNumber(session.getPartNumberImp().trim());
            if (programs != null) {
                for (ProgramCNC prog : programs) {
                    Map<String, String> pp = new HashMap<>();
                    pp.put("panelNumber", prog.getPanelNumber());
                    pp.put("pattern", prog.getPattern());
                    pp.put("programNumber", prog.getProgramNumber());
                    pp.put("casette", prog.getCasette());
                    pp.put("version", prog.getVersion());
                    pp.put("row", prog.getRow());
                    pp.put("set", prog.getSet());
                    pp.put("coutureDecorativeCnc", prog.getCoutureDecorativeCnc());
                    pp.put("cavitePress", prog.getCavitePress());
                    pp.put("blindStitch", prog.getBlindStitch());
                    pp.put("profil", prog.getProfil());
                    pp.put("type", prog.getType());
                    patternPrograms.add(pp);
                }
            }
        }
        return patternPrograms;
    }

    // ===================== ZPL BUILDERS =====================

    private String buildLeatherReturnZpl(CncPsLeatherConsumption c, CncPsSession session,
                                          List<CncPsLeatherConsumption> allConsumptions) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder sb = new StringBuilder();
        //CT~~CD,~CC^~CT~
        //^XA~TA000~JSN^LT0^MNN^MTD^PON^PMN^LH0,0^JMA^PR5,5~SD15^JUS^LRN^CI0^XZ
        //^MMT
        sb.append("\u0010CT~~CD,~CC^~CT~\n");
        sb.append("^XA~TA000~JSN^LT0^MNN^MTD^PON^PMN^LH0,0^JMA^PR5,5~SD15^JUS^LRN^CI0^XZ\n");
        sb.append("^MMT\n");
        sb.append("^XA\n");
        sb.append("^PW799\n");
        sb.append("^LL799\n");
        //^LL1678
        sb.append("^FO20,20^A0N,35,35^FDRetour Cuir CNC^FS\n");
        sb.append("^FO20,60^GB759,2,2^FS\n");

        sb.append("^FO20,70^A0N,25,25^FDBoîte: ").append(toStr(session.getBoxId())).append("^FS\n");
        sb.append("^FO400,70^A0N,25,25^FDSeq: ").append(toStr(session.getnSequenceImp())).append("^FS\n");
        sb.append("^FO20,100^A0N,25,25^FDPN Boîte: ").append(toStr(session.getPartNumberImp())).append("^FS\n");
        sb.append("^FO400,100^A0N,25,25^FDQté Boîte: ").append(toStr(session.getQuantiteImp())).append("^FS\n");
        sb.append("^FO20,130^A0N,20,20^FDOp: ").append(toStr(session.getOperator())).append("^FS\n");
        sb.append("^FO400,130^A0N,20,20^FDDate: ").append(date).append("^FS\n");
        sb.append("^FO20,155^GB759,2,2^FS\n");

        sb.append("^FO20,165^A0N,28,28^FDDétails Retour:^FS\n");
        sb.append("^FO20,200^A0N,25,25^FDPN Cuir: ").append(toStr(c.getLeatherPartNumber())).append("^FS\n");
        sb.append("^FO20,230^A0N,25,25^FDSerial: ").append(toStr(c.getSerial())).append("^FS\n");
        sb.append("^FO20,260^A0N,25,25^FDLot: ").append(toStr(c.getLot())).append("^FS\n");
        sb.append("^FO20,290^A0N,25,25^FDQté Initiale: ").append(toStr(c.getQuantiteInitial())).append("^FS\n");
        sb.append("^FO400,290^A0N,25,25^FDQté Consommée: ").append(toStr(c.getQuantiteConsumed())).append("^FS\n");
        sb.append("^FO20,320^A0N,30,30^FDQté Retour: ").append(toStr(c.getQuantiteRetour())).append("^FS\n");
        sb.append("^FO20,355^GB759,2,2^FS\n");

        sb.append("^FO20,365^A0N,18,18^FDPN Cuir:^FS\n");
        sb.append("^FO20,385^BY2^BCN,60,Y,N,N^FD").append(toStr(c.getLeatherPartNumber())).append("^FS\n");
        sb.append("^FO400,365^A0N,18,18^FDSerial:^FS\n");
        sb.append("^FO400,385^BY2^BCN,60,Y,N,N^FD").append(toStr(c.getSerial())).append("^FS\n");
        sb.append("^FO20,475^A0N,18,18^FDLot:^FS\n");
        sb.append("^FO20,495^BY2^BCN,60,Y,N,N^FD").append(toStr(c.getLot())).append("^FS\n");
        sb.append("^FO400,475^A0N,18,18^FDQté Retour:^FS\n");
        sb.append("^FO400,495^BY2^BCN,60,Y,N,N^FD").append(String.format("%.0f", c.getQuantiteRetour())).append("^FS\n");

        sb.append("^XZ\n");
        return sb.toString();
    }

//    private String buildBoxLabelZpl(CncPsSession session, List<CncPsLeatherConsumption> consumptions,
//                                     List<Map<String, String>> patternPrograms) {
//        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
//        StringBuilder sb = new StringBuilder();
//        sb.append("\u0010CT~~CD,~CC^~CT~\n");
//        sb.append("^XA~TA000~JSN^LT0^MNN^MTD^PON^PMN^LH0,0^JMA^PR5,5~SD15^JUS^LRN^CI0^XZ\n");
//        sb.append("^MMT\n");
//        sb.append("^XA\n");
//        sb.append("^PW799\n");
//        sb.append("^LL799\n");
//        sb.append("^FO20,20^A0N,35,35^FDBoîte CNC-PS^FS\n");
//        sb.append("^FO20,60^GB759,2,2^FS\n");
//
//        sb.append("^FO20,70^A0N,25,25^FDID: ").append(toStr(session.getBoxId())).append("^FS\n");
//        sb.append("^FO400,70^A0N,25,25^FDSeq: ").append(toStr(session.getnSequenceImp())).append("^FS\n");
//        sb.append("^FO20,100^A0N,25,25^FDPN: ").append(toStr(session.getPartNumberImp())).append("^FS\n");
//        sb.append("^FO400,100^A0N,25,25^FDCode3: ").append(toStr(session.getCode3Imp())).append("^FS\n");
//        sb.append("^FO20,130^A0N,25,25^FDQté: ").append(toStr(session.getQuantiteImp())).append("^FS\n");
//        sb.append("^FO400,130^A0N,20,20^FDDate: ").append(date).append("^FS\n");
//        sb.append("^FO20,155^A0N,20,20^FDOp: ").append(toStr(session.getOperator())).append("^FS\n");
//        sb.append("^FO20,180^GB759,2,2^FS\n");
//
//        int y = 190;
//        if (consumptions != null && !consumptions.isEmpty()) {
//            sb.append("^FO20,").append(y).append("^A0N,22,22^FDCuirs consommés:^FS\n");
//            y += 28;
//            for (int i = 0; i < consumptions.size(); i++) {
//                CncPsLeatherConsumption lc = consumptions.get(i);
//                sb.append("^FO20,").append(y).append("^A0N,18,18^FD").append(i + 1)
//                  .append(". PN:").append(toStr(lc.getLeatherPartNumber()))
//                  .append(" S:").append(toStr(lc.getSerial()))
//                  .append(" Lot:").append(toStr(lc.getLot()))
//                  .append(" Init:").append(toStr(lc.getQuantiteInitial()))
//                  .append(" Cons:").append(toStr(lc.getQuantiteConsumed()))
//                  .append(" Ret:").append(toStr(lc.getQuantiteRetour()))
//                  .append("^FS\n");
//                y += 22;
//            }
//            sb.append("^FO20,").append(y).append("^GB759,2,2^FS\n");
//            y += 10;
//        }
//
//        sb.append("^FO20,").append(y).append("^A0N,22,22^FDPatterns CNC:^FS\n");
//        y += 28;
//        if (patternPrograms != null) {
//            for (int i = 0; i < patternPrograms.size(); i++) {
//                Map<String, String> pp = patternPrograms.get(i);
//                sb.append("^FO20,").append(y).append("^A0N,18,18^FD").append(i + 1)
//                  .append(". Pat:").append(toStr(pp.get("pattern")));
//                if (pp.get("programNumber") != null) {
//                    sb.append(" Prog:").append(toStr(pp.get("programNumber")))
//                      .append(" Cas:").append(toStr(pp.get("casette")));
//                }
//                sb.append("^FS\n");
//                y += 22;
//                if (pp.get("version") != null || pp.get("row") != null || pp.get("set") != null) {
//                    sb.append("^FO40,").append(y).append("^A0N,16,16^FD")
//                      .append("Ver:").append(toStr(pp.get("version")))
//                      .append(" Row:").append(toStr(pp.get("row")))
//                      .append(" Set:").append(toStr(pp.get("set")))
//                      .append("^FS\n");
//                    y += 20;
//                }
//                if (pp.get("coutureDecorativeCnc") != null || pp.get("blindStitch") != null) {
//                    sb.append("^FO40,").append(y).append("^A0N,16,16^FD")
//                      .append("Cout.Déco:").append(toStr(pp.get("coutureDecorativeCnc")))
//                      .append(" BlindSt:").append(toStr(pp.get("blindStitch")))
//                      .append("^FS\n");
//                    y += 20;
//                }
//            }
//        }
//
//        sb.append("^XZ\n");
//        return sb.toString();
//    }

    private String buildBoxLabelZpl(CncPsSession session,
                                    List<CncPsLeatherConsumption> consumptions,
                                    List<Map<String, String>> patternPrograms) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder sb = new StringBuilder();
        sb.append("\u0010CT~~CD,~CC^~CT~\n");
        sb.append("^XA~TA000~JSN^LT0^MNN^MTD^PON^PMN^LH0,0^JMA^PR5,5~SD15^JUS^LRN^CI0^XZ\n");
        sb.append("^MMT\n");
        sb.append("^XA\n");
        sb.append("^PW799\n");

        int y = 20; // top margin

        // ── Title ──────────────────────────────────────────────────────────────
        sb.append("^FO20,").append(y).append("^A0N,50,50^FDBoîte CNC-PS^FS\n");
        y += 60;
        sb.append("^FO20,").append(y).append("^GB759,3,3^FS\n");
        y += 10;

        // ── Box ID as barcode ──────────────────────────────────────────────────
        sb.append("^FO20,").append(y).append("^A0N,25,25^FDID Boîte:^FS\n");
        sb.append("^FO200,").append(y).append("^A0N,25,25^FD").append(toStr(session.getBoxId())).append("^FS\n");
        y += 32;
        sb.append("^FO20,").append(y).append("^BY2^BCN,70,Y,N,N^FD").append(toStr(session.getBoxId())).append("^FS\n");
        y += 100;

        sb.append("^FO20,").append(y).append("^GB759,3,3^FS\n");
        y += 10;

        // ── Box details ────────────────────────────────────────────────────────
        sb.append("^FO20,").append(y).append("^A0N,28,28^FDSeq: ").append(toStr(session.getnSequenceImp())).append("^FS\n");
        sb.append("^FO400,").append(y).append("^A0N,28,28^FDQté: ").append(toStr(session.getQuantiteImp())).append("^FS\n");
        y += 36;
        sb.append("^FO20,").append(y).append("^A0N,28,28^FDPN: ").append(toStr(session.getPartNumberImp())).append("^FS\n");
        sb.append("^FO400,").append(y).append("^A0N,28,28^FDCode3: ").append(toStr(session.getCode3Imp())).append("^FS\n");
        y += 36;
        sb.append("^FO20,").append(y).append("^A0N,25,25^FDDate: ").append(date).append("^FS\n");
        sb.append("^FO400,").append(y).append("^A0N,25,25^FDOp: ").append(toStr(session.getOperator())).append("^FS\n");
        y += 32;
        sb.append("^FO20,").append(y).append("^GB759,3,3^FS\n");
        y += 12;

        // ── Leather consumptions ───────────────────────────────────────────────
        if (consumptions != null && !consumptions.isEmpty()) {
            sb.append("^FO20,").append(y).append("^A0N,28,28^FDCuirs consommés:^FS\n");
            y += 36;
            for (int i = 0; i < consumptions.size(); i++) {
                CncPsLeatherConsumption lc = consumptions.get(i);
                sb.append("^FO20,").append(y).append("^A0N,22,22^FD").append(i + 1)
                        .append(". PN:").append(toStr(lc.getLeatherPartNumber()))
                        .append(" S:").append(toStr(lc.getSerial()))
                        .append(" Lot:").append(toStr(lc.getLot()))
                        .append(" Init:").append(toStr(lc.getQuantiteInitial()))
                        .append(" Cons:").append(toStr(lc.getQuantiteConsumed()))
                        .append(" Ret:").append(toStr(lc.getQuantiteRetour()))
                        .append("^FS\n");
                y += 28;
            }
            sb.append("^FO20,").append(y).append("^GB759,3,3^FS\n");
            y += 12;
        }

        // ── Programmes CNC (each in a big bordered rectangle / table) ──────────
        sb.append("^FO20,").append(y).append("^A0N,34,34^FDProgrammes CNC:^FS\n");
        y += 40;
        if (patternPrograms != null) {
            for (int i = 0; i < patternPrograms.size(); i++) {
                Map<String, String> pp = patternPrograms.get(i);

                int headerRowH = 52;
                int rowH = 52;
                int blockH = headerRowH + (rowH * 6) + 8;

                // Outer border rectangle
                sb.append("^FO15,").append(y).append("^GB769,").append(blockH).append(",2^FS\n");

                // Row 1 – filled header (panel + pattern title)
                sb.append("^FO15,").append(y).append("^GB769,").append(headerRowH).append(",").append(headerRowH).append(",B^FS\n");
                sb.append("^FO20,").append(y + 8).append("^FR^A0N,34,34^FB730,1,0,L,0^FD")
                        .append(i + 1).append(". Panel: ").append(toStr(pp.get("panelNumber")))
                        .append("  Pattern: ").append(toStr(pp.get("pattern"))).append("^FS\n");
                y += headerRowH;

                // Horizontal divider
                sb.append("^FO15,").append(y).append("^GB769,1,1^FS\n");

                // Row 2 – N° Programme / Set
                sb.append("^FO20,").append(y + 8).append("^A0N,34,34^FDProg:^FS\n");
                sb.append("^FO130,").append(y + 8).append("^A0N,34,34^FB260,1,0,L,0^FD")
                        .append(toStr(pp.get("programNumber"))).append("^FS\n");
                sb.append("^FO420,").append(y + 8).append("^A0N,34,34^FDSet:^FS\n");
                sb.append("^FO510,").append(y + 8).append("^A0N,34,34^FB240,1,0,L,0^FD")
                        .append(toStr(pp.get("set"))).append("^FS\n");
                y += rowH;

                // Horizontal divider
                sb.append("^FO15,").append(y).append("^GB769,1,1^FS\n");

                // Row 3 – Cassette
                sb.append("^FO20,").append(y + 8).append("^A0N,34,34^FDCassette:^FS\n");
                sb.append("^FO320,").append(y + 8).append("^A0N,34,34^FB440,1,0,L,0^FD")
                        .append(toStr(pp.get("casette"))).append("^FS\n");
                y += rowH;

                // Horizontal divider
                sb.append("^FO15,").append(y).append("^GB769,1,1^FS\n");

                // Row 4 – Fil Couture CNC
                sb.append("^FO20,").append(y + 8).append("^A0N,34,34^FDFil Couture CNC:^FS\n");
                sb.append("^FO320,").append(y + 8).append("^A0N,34,34^FB440,1,0,L,0^FD")
                        .append(toStr(pp.get("coutureDecorativeCnc"))).append("^FS\n");
                y += rowH;

                // Horizontal divider
                sb.append("^FO15,").append(y).append("^GB769,1,1^FS\n");

                // Row 5 – Cavité Press
                sb.append("^FO20,").append(y + 8).append("^A0N,34,34^FDCavité Press:^FS\n");
                sb.append("^FO320,").append(y + 8).append("^A0N,34,34^FB440,1,0,L,0^FD")
                        .append(toStr(pp.get("cavitePress"))).append("^FS\n");
                y += rowH;

                // Horizontal divider
                sb.append("^FO15,").append(y).append("^GB769,1,1^FS\n");

                // Row 6 – Fil blind
                sb.append("^FO20,").append(y + 8).append("^A0N,34,34^FDFil blind:^FS\n");
                sb.append("^FO320,").append(y + 8).append("^A0N,34,34^FB440,1,0,L,0^FD")
                        .append(toStr(pp.get("blindStitch"))).append("^FS\n");
                y += rowH;

                // Horizontal divider
                sb.append("^FO15,").append(y).append("^GB769,1,1^FS\n");

                // Row 7 – Profil / Type
                sb.append("^FO20,").append(y + 8).append("^A0N,34,34^FDProfil:^FS\n");
                sb.append("^FO160,").append(y + 8).append("^A0N,34,34^FB230,1,0,L,0^FD")
                        .append(toStr(pp.get("profil"))).append("^FS\n");
                sb.append("^FO420,").append(y + 8).append("^A0N,34,34^FDType:^FS\n");
                sb.append("^FO530,").append(y + 8).append("^A0N,34,34^FB220,1,0,L,0^FD")
                        .append(toStr(pp.get("type"))).append("^FS\n");
                y += rowH;

                // Bottom padding + gap between programmes
                y += 20;
            }
        }

        // Bottom margin
        y += 30;

        // Flexible label length
        sb.append("^LL").append(y).append("\n");
        sb.append("^XZ\n");
        return sb.toString();
    }


    // ===================== TEMPLATE HELPERS =====================

    private String loadTemplate(String filename) {
        try {
            Path path = Paths.get(templatePath, filename);
            if (java.nio.file.Files.exists(path)) {
                return new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // Template not found, fall back to hardcoded ZPL
        }
        return null;
    }

    private String applyLeatherReturnTemplate(String template, CncPsLeatherConsumption c,
                                               CncPsSession session, List<CncPsLeatherConsumption> allConsumptions) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String result = template;
        result = result.replace("@boxId", toStr(session.getBoxId()));
        result = result.replace("@nSequenceImp", toStr(session.getnSequenceImp()));
        result = result.replace("@partNumberImp", toStr(session.getPartNumberImp()));
        result = result.replace("@quantiteImp", toStr(session.getQuantiteImp()));
        result = result.replace("@operator", toStr(session.getOperator()));
        result = result.replace("@date", date);
        result = result.replace("@leatherPartNumber", toStr(c.getLeatherPartNumber()));
        result = result.replace("@serial", toStr(c.getSerial()));
        result = result.replace("@lot", toStr(c.getLot()));
        result = result.replace("@quantiteInitial", toStr(c.getQuantiteInitial()));
        result = result.replace("@quantiteConsumed", toStr(c.getQuantiteConsumed()));
        result = result.replace("@quantiteRetour", toStr(c.getQuantiteRetour()));
        result = result.replace("@quantiteRetourInt", c.getQuantiteRetour() != null ? String.format("%.0f", c.getQuantiteRetour()) : "0");
        return result;
    }

    private String applyBoxLabelTemplate(String template, CncPsSession session,
                                          List<CncPsLeatherConsumption> consumptions,
                                          List<Map<String, String>> patternPrograms) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String result = template;
        result = result.replace("@boxId", toStr(session.getBoxId()));
        result = result.replace("@nSequenceImp", toStr(session.getnSequenceImp()));
        result = result.replace("@partNumberImp", toStr(session.getPartNumberImp()));
        result = result.replace("@code3Imp", toStr(session.getCode3Imp()));
        result = result.replace("@quantiteImp", toStr(session.getQuantiteImp()));
        result = result.replace("@operator", toStr(session.getOperator()));
        result = result.replace("@date", date);
        // Cover description came from ctc Files which is no longer used; keep the
        // placeholders empty so existing external templates don't print a literal token.
        result = result.replace("@partNumberCoverDescription", "");
        result = result.replace("@partNumberCoverDesciption", "");

        // Consumptions block
        StringBuilder consBlock = new StringBuilder();
        if (consumptions != null) {
            for (int i = 0; i < consumptions.size(); i++) {
                CncPsLeatherConsumption lc = consumptions.get(i);
                consBlock.append(i + 1).append(". PN:").append(toStr(lc.getLeatherPartNumber()))
                        .append(" S:").append(toStr(lc.getSerial()))
                        .append(" Lot:").append(toStr(lc.getLot()))
                        .append(" Init:").append(toStr(lc.getQuantiteInitial()))
                        .append(" Cons:").append(toStr(lc.getQuantiteConsumed()))
                        .append(" Ret:").append(toStr(lc.getQuantiteRetour()));
                if (i < consumptions.size() - 1) consBlock.append("\n");
            }
        }
        result = result.replace("@consumptions", consBlock.toString());

        // Programs block
        StringBuilder progBlock = new StringBuilder();
        if (patternPrograms != null) {
            for (int i = 0; i < patternPrograms.size(); i++) {
                Map<String, String> pp = patternPrograms.get(i);
                progBlock.append(i + 1).append(". Panel:").append(toStr(pp.get("panelNumber")))
                        .append(" Pattern:").append(toStr(pp.get("pattern")))
                        .append(" Prog:").append(toStr(pp.get("programNumber")))
                        .append(" Cas:").append(toStr(pp.get("casette")))
                        .append(" Set:").append(toStr(pp.get("set")))
                        .append(" FilCouture:").append(toStr(pp.get("coutureDecorativeCnc")))
                        .append(" Cavité:").append(toStr(pp.get("cavitePress")))
                        .append(" FilBlind:").append(toStr(pp.get("blindStitch")))
                        .append(" Profil:").append(toStr(pp.get("profil")))
                        .append(" Type:").append(toStr(pp.get("type")));
                if (i < patternPrograms.size() - 1) progBlock.append("\n");
            }
        }
        result = result.replace("@programmes", progBlock.toString());

        return result;
    }


    private String toStr(Object value) {
        return value != null ? value.toString() : "";
    }
}
