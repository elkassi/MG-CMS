package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.EngineScheduleEntryRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.logistics.AllocationService;

class WorkbenchSequenceFocusServiceTest {

    @Mock private EngineScheduleEntryRepository scheduleEntryRepository;
    @Mock private CuttingRequestBoxInfoRepository boxInfoRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private ProductionTableRepository productionTableRepository;
    @Mock private AllocationService allocationService;
    @Mock private SerieRouleauTempService serieRouleauTempService;

    @InjectMocks private WorkbenchSequenceFocusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(allocationService.reservedMetersByMaterialZone()).thenReturn(Collections.emptyMap());
        when(serieRouleauTempService.getAll()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("focus prioritizes open sequence and exposes upcoming boxes plus material transfer")
    @SuppressWarnings("unchecked")
    void build_prioritizesOpenSequence_andBuildsChefAndLogisticsActions() {
        LocalDate today = LocalDate.of(2026, 5, 22);
        LocalDateTime now = LocalDateTime.now();

        Zone z1 = zone("Z1", "R1");
        Zone z2 = zone("Z2", "R2");
        when(zoneRepository.findAllActive()).thenReturn(Arrays.asList(z1, z2));
        when(productionTableRepository.findMachinesWithTypeInZones(any()))
                .thenReturn(Collections.singletonList(new Object[]{"Z1", "L1", "Lectra"}));

        EngineScheduleEntry readyEntry = new EngineScheduleEntry(
                "ser-ready", EngineScheduleEntry.Phase.COUPE, "L1", "seq-ready", "Z1",
                now.plusMinutes(25), now.plusMinutes(45), 1L, now.minusMinutes(5));
        when(scheduleEntryRepository.findCoupeEntriesForSequencesWithinHorizon(any(), any()))
                .thenReturn(Collections.singletonList(readyEntry));
        when(boxInfoRepository.countBoxesBySequences(any()))
                .thenReturn(Arrays.asList(
                        new Object[]{"seq-open", 6L},
                        new Object[]{"seq-ready", 3L}
                ));

        LiveChargeDto.SequenceDto open = sequence("seq-open", true, "ser-open", "PAA", 20.0);
        LiveChargeDto.SequenceDto ready = sequence("seq-ready", false, "ser-ready", "PBB", 10.0);
        LiveChargeDto liveCharge = new LiveChargeDto(
                now, today, 2, 480,
                new LiveChargeDto.TotalsDto(2, 1, 1, 0, 70.0, 480.0),
                Collections.singletonList(new LiveChargeDto.ZoneChargeDto(
                        "Z1", "STRICT", Collections.singletonList(
                                new LiveChargeDto.MachineTypeChargeDto(
                                        "Lectra", "Coupe", 1, 480, 90.0, 432.0,
                                        20.0, 50.0, 70.0, 16.2)),
                        Collections.singletonList(open), Collections.singletonList(ready),
                        70.0, 480.0, 14.5)),
                Collections.emptyList());

        Map<String, Object> result = service.build(today, 2, liveCharge, stock(), EngineState.IDLE);
        List<Map<String, Object>> zones = (List<Map<String, Object>>) result.get("zones");
        Map<String, Object> zone = zones.get(0);
        List<Map<String, Object>> focusSequences = (List<Map<String, Object>>) zone.get("focusSequences");
        List<Map<String, Object>> chefAlerts = (List<Map<String, Object>>) zone.get("chefAlerts");
        Map<String, Object> logistics = (Map<String, Object>) zone.get("logistics");
        List<Map<String, Object>> transfers = (List<Map<String, Object>>) logistics.get("transferSuggestions");
        Map<String, Object> boxOccupancy = (Map<String, Object>) zone.get("boxOccupancy");

        assertEquals("seq-open", focusSequences.get(0).get("sequence"));
        assertEquals("OPEN", focusSequences.get(0).get("state"));
        assertEquals(6, boxOccupancy.get("occupiedBoxes"));
        assertEquals(16, boxOccupancy.get("maxBoxes"));
        assertFalse(chefAlerts.isEmpty(), "upcoming balanced sequence should alert the chef");
        assertEquals("seq-ready", chefAlerts.get(0).get("sequence"));
        assertEquals("AA", transfers.get(0).get("material"));
        assertEquals("OUT_OF_ZONE", transfers.get(0).get("status"));
    }

    @Test
    @DisplayName("sequence focus exposes materialMissingSuggested after ledger reservations make stock unavailable")
    @SuppressWarnings("unchecked")
    void build_exposesMaterialMissingSuggested_fromTrueStock() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        LocalDateTime now = LocalDateTime.now();

        when(zoneRepository.findAllActive()).thenReturn(Collections.singletonList(zone("Z1", "R1")));
        when(productionTableRepository.findMachinesWithTypeInZones(any()))
                .thenReturn(Collections.singletonList(new Object[]{"Z1", "L1", "Lectra"}));
        when(scheduleEntryRepository.findCoupeEntriesForSequencesWithinHorizon(any(), any()))
                .thenReturn(Collections.emptyList());
        when(boxInfoRepository.countBoxesBySequences(any())).thenReturn(Collections.emptyList());
        Map<String, Double> reserved = new LinkedHashMap<>();
        reserved.put("AA|Z1", 100.0);
        when(allocationService.reservedMetersByMaterialZone()).thenReturn(reserved);

        LiveChargeDto.SequenceDto blocked = sequence("seq-blocked", true, "ser-blocked", "PAA", 50.0);
        LiveChargeDto liveCharge = new LiveChargeDto(
                now, today, 2, 480,
                new LiveChargeDto.TotalsDto(1, 0, 1, 0, 20.0, 480.0),
                Collections.singletonList(new LiveChargeDto.ZoneChargeDto(
                        "Z1", "STRICT", Collections.singletonList(
                                new LiveChargeDto.MachineTypeChargeDto(
                                        "Lectra", "Coupe", 1, 480, 90.0, 432.0,
                                        20.0, 50.0, 70.0, 16.2)),
                        Collections.emptyList(), Collections.singletonList(blocked),
                        20.0, 480.0, 4.2)),
                Collections.emptyList());

        Map<String, Object> result = service.build(today, 2, liveCharge,
                Collections.singletonList(rack("PAA", "R1", 100.0)), EngineState.IDLE);

        List<Map<String, Object>> zones = (List<Map<String, Object>>) result.get("zones");
        Map<String, Object> zone = zones.get(0);
        List<Map<String, Object>> focusSequences = (List<Map<String, Object>>) zone.get("focusSequences");
        assertEquals(Boolean.TRUE, focusSequences.get(0).get("materialMissingSuggested"));
        assertEquals("NONE", focusSequences.get(0).get("materialStatus"));
    }

    private Zone zone(String nom, String locations) {
        Zone z = new Zone();
        z.setNom(nom);
        z.setCategory(Zone.Category.STRICT);
        z.setRollLocations(locations);
        z.setActive(true);
        return z;
    }

    private LiveChargeDto.SequenceDto sequence(String id,
                                               boolean started,
                                               String serieId,
                                               String material,
                                               double meters) {
        LiveChargeDto.SerieDto serie = new LiveChargeDto.SerieDto(
                serieId, "Lectra", 20.0, 20.0, "TEMPS_DE_COUPE",
                started ? "In progress" : "Waiting",
                started ? "In progress" : "Waiting",
                started ? "L1" : null,
                started ? "L1" : null,
                started ? LocalDateTime.now().minusMinutes(5) : null,
                null,
                started ? LocalDateTime.now().minusMinutes(10) : null,
                null,
                started ? 5.0 : 0.0,
                20.0,
                "Z1",
                "STRICT",
                material,
                null,
                meters);
        return new LiveChargeDto.SequenceDto(
                id, "Z1", "Z1", "Z1", "DISPATCHED",
                false, started, started ? "IMPLICIT_TABLE_STRICT" : null,
                started ? serieId : null, started ? "L1" : null,
                started ? "In progress" : null,
                false, "ACCEPTED", 20.0,
                Collections.singletonList(serie),
                LocalDate.of(2026, 5, 22), 20.0, null);
    }

    private List<Map<String, Object>> stock() {
        Map<String, Object> outOfZone = rack("PAA", "R2", 50.0);
        Map<String, Object> inZone = rack("PBB", "R1", 40.0);
        Map<String, Object> returnCandidate = rack("PCC", "R1", 30.0);
        return Arrays.asList(outOfZone, inZone, returnCandidate);
    }

    private Map<String, Object> rack(String material, String location, double meters) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("reftissu", material);
        row.put("emplacement", location);
        row.put("metrage", meters);
        return row;
    }
}
