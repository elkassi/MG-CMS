package com.lear.MGCMS.services.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.domain.MachineQueue;
import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.EtatMachineHistoriqueRepository;
import com.lear.MGCMS.repositories.MachineQueueRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.dispatcher.ActiveMachineResolver;
import com.lear.MGCMS.services.dispatcher.LiveChargeDto;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

class FloorStateServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);
    private static final int SHIFT = 2;

    @Mock private ZoneRepository zoneRepository;
    @Mock private ScanRouleauRepository scanRouleauRepository;
    @Mock private ProductionTableRepository productionTableRepository;
    @Mock private ActiveMachineResolver activeMachineResolver;
    @Mock private SerieRouleauTempService serieRouleauTempService;
    @Mock private MachineQueueRepository machineQueueRepository;
    @Mock private LiveChargeService liveChargeService;
    @Mock private CuttingRequestBoxInfoRepository boxInfoRepository;
    @Mock private EtatMachineHistoriqueRepository etatMachineRepository;
    @Mock private CuttingRequestRepository cuttingRequestRepository;
    @Mock private ShiftClock shiftClock;

    @InjectMocks private FloorStateService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(shiftClock.currentSlot()).thenReturn(new ShiftClock.ShiftSlot(TODAY, SHIFT));
        when(cuttingRequestRepository.findStatusBySequences(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("aggregates one zone: a CUTTING machine with its current serie + queue, the rack rolls, the spreading roll, and box occupancy")
    @SuppressWarnings("unchecked")
    void getFloorState_oneZone() {
        // One active zone whose rack location is R1.
        when(zoneRepository.findAllActive()).thenReturn(Collections.singletonList(zone("Z1", "R1")));

        // Two rolls in rack R1 (in Z1).
        when(scanRouleauRepository.findRackLight()).thenReturn(Arrays.asList(
                rack("roll-aa", "PAA", "R1", "lot1", 50.0),
                rack("roll-bb", "PBB", "R1", "lot2", 30.0)));

        // One machine M1 (Lectra) physically in Z1.
        when(productionTableRepository.findMachinesWithTypeInZones(any()))
                .thenReturn(Collections.singletonList(new Object[]{"Z1", "M1", "Lectra"}));

        // M1 is UP this shift.
        when(activeMachineResolver.activeMachines(eq(TODAY), anyInt(), eq("Z1")))
                .thenReturn(new java.util.LinkedHashSet<>(Collections.singletonList("M1")));

        // codeEtat 'M' (running) for M1.
        when(etatMachineRepository.findAllCurrentStatuses(any()))
                .thenReturn(Collections.singletonList(new Object[]{"M1", "M"}));

        // A roll currently spread on table M1.
        when(serieRouleauTempService.getAll()).thenReturn(Collections.singletonList(
                spreadRoll("M1", "roll-aa", "PAA", "lot1", 12.5)));

        // M1's cut queue: one waiting serie, 40 min.
        when(machineQueueRepository.findAllOrdered()).thenReturn(Collections.singletonList(
                queue("M1", 1, "ser-next", "seq-2", "PBB", 18.0, 40.0)));

        // LiveCharge: seq-1 has an In-progress serie on table M1 (=> M1 is CUTTING) with 25 min remaining.
        // The locked sequence also drives this zone's box occupancy.
        LiveChargeDto.SerieDto inProgress = serie("ser-cur", "Lectra", "In progress", "M1", "PAA", 25.0);
        LiveChargeDto.SequenceDto seq1 = sequence("seq-1", Collections.singletonList(inProgress));
        LiveChargeDto.ZoneChargeDto z1 = zoneCharge("Z1",
                Collections.singletonList(seq1), Collections.emptyList());
        when(liveChargeService.compute()).thenReturn(new LiveChargeDto(
                LocalDateTime.now(), TODAY, SHIFT, 480,
                new LiveChargeDto.TotalsDto(1, 1, 0, 0, 25.0, 432.0),
                Collections.singletonList(z1), Collections.emptyList()));

        // Box occupancy: seq-1 has 5 boxes.
        when(boxInfoRepository.countBoxesBySequences(any()))
                .thenReturn(Collections.singletonList(new Object[]{"seq-1", 5L}));

        Map<String, Object> out = service.getFloorState(TODAY, SHIFT);

        // --- top-level shape ---
        assertEquals(TODAY.toString(), out.get("date"));
        assertEquals(SHIFT, out.get("shift"));
        assertNotNull(out.get("generatedAt"));

        List<Map<String, Object>> zones = (List<Map<String, Object>>) out.get("zones");
        assertEquals(1, zones.size());
        Map<String, Object> zone = zones.get(0);
        assertEquals("Z1", zone.get("zone"));
        assertEquals("STRICT", zone.get("category"));

        // --- machine ---
        List<Map<String, Object>> machines = (List<Map<String, Object>>) zone.get("machines");
        assertEquals(1, machines.size());
        Map<String, Object> m1 = machines.get(0);
        assertEquals("M1", m1.get("nom"));
        assertEquals("Lectra", m1.get("machineType"));
        assertEquals("Coupe", m1.get("groupe"));
        assertEquals(Boolean.TRUE, m1.get("up"));
        assertEquals("CUTTING", m1.get("status"));
        assertEquals("ser-cur", m1.get("currentSerie"));
        assertEquals("seq-1", m1.get("currentSequence"));
        assertEquals("PAA", m1.get("currentMaterial"));
        // ETA = 25 (in-progress remaining) + 40 (queued) = 65 min.
        assertEquals(65L, ((Number) m1.get("finishEtaMinutes")).longValue());
        assertNotNull(m1.get("finishEtaClock"));

        List<Map<String, Object>> queue = (List<Map<String, Object>>) m1.get("queue");
        assertEquals(1, queue.size());
        assertEquals("ser-next", queue.get(0).get("serie"));
        assertEquals("PBB", queue.get(0).get("material"));
        assertEquals(1, queue.get(0).get("position"));

        // --- spreading ---
        List<Map<String, Object>> spreading = (List<Map<String, Object>>) zone.get("spreadingTables");
        assertEquals(1, spreading.size());
        assertEquals("M1", spreading.get(0).get("table"));
        assertEquals("roll-aa", spreading.get(0).get("idRouleau"));
        assertEquals(12.5, ((Number) spreading.get(0).get("estimationRest")).doubleValue(), 0.001);
        // current serie on the table comes from the In-progress cut on that machine.
        assertEquals("ser-cur", spreading.get(0).get("serie"));

        // --- racks ---
        List<Map<String, Object>> racks = (List<Map<String, Object>>) zone.get("racks");
        assertEquals(1, racks.size());
        Map<String, Object> rack = racks.get(0);
        assertEquals("R1", rack.get("rack"));
        assertEquals(2, rack.get("rollCount"));
        assertEquals(80.0, ((Number) rack.get("meters")).doubleValue(), 0.001);
        List<Map<String, Object>> rolls = (List<Map<String, Object>>) rack.get("rolls");
        assertEquals(2, rolls.size());
        assertEquals("roll-aa", rolls.get(0).get("serialId"));
        assertEquals("PAA", rolls.get(0).get("reftissu"));

        // --- boxes: occupied=5, capacity=1 machine × 16 = 16 ---
        Map<String, Object> boxes = (Map<String, Object>) zone.get("boxes");
        assertEquals(5, boxes.get("occupied"));
        assertEquals(16, boxes.get("capacity"));
        assertEquals(31.25, ((Number) boxes.get("pct")).doubleValue(), 0.001);

        // --- totals ---
        Map<String, Object> totals = (Map<String, Object>) out.get("totals");
        assertEquals(1, totals.get("zoneCount"));
        assertEquals(1, totals.get("machineCount"));
        assertEquals(1, totals.get("cuttingCount"));
        assertEquals(0, totals.get("stoppedCount"));
        assertEquals(1, totals.get("spreadingCount"));
        assertEquals(2, totals.get("rollsInRacks"));
    }

    @Test
    @DisplayName("a machine that is not UP this shift is reported DOWN with no current cut")
    @SuppressWarnings("unchecked")
    void getFloorState_downMachine() {
        when(zoneRepository.findAllActive()).thenReturn(Collections.singletonList(zone("Z1", "R1")));
        when(scanRouleauRepository.findRackLight()).thenReturn(Collections.emptyList());
        when(productionTableRepository.findMachinesWithTypeInZones(any()))
                .thenReturn(Collections.singletonList(new Object[]{"Z1", "M1", "Lectra"}));
        // Not UP.
        when(activeMachineResolver.activeMachines(any(), anyInt(), any()))
                .thenReturn(Collections.emptySet());
        when(etatMachineRepository.findAllCurrentStatuses(any())).thenReturn(Collections.emptyList());
        when(serieRouleauTempService.getAll()).thenReturn(Collections.emptyList());
        when(machineQueueRepository.findAllOrdered()).thenReturn(Collections.emptyList());
        when(liveChargeService.compute()).thenReturn(new LiveChargeDto(
                LocalDateTime.now(), TODAY, SHIFT, 480,
                new LiveChargeDto.TotalsDto(0, 0, 0, 0, 0.0, 0.0),
                Collections.emptyList(), Collections.emptyList()));

        Map<String, Object> out = service.getFloorState(TODAY, SHIFT);

        List<Map<String, Object>> zones = (List<Map<String, Object>>) out.get("zones");
        Map<String, Object> m1 = ((List<Map<String, Object>>) zones.get(0).get("machines")).get(0);
        assertEquals("DOWN", m1.get("status"));
        assertEquals(Boolean.FALSE, m1.get("up"));
        assertFalse(m1.containsKey("currentSerie") && m1.get("currentSerie") != null);
        assertTrue(((List<?>) m1.get("queue")).isEmpty());

        // capacity = 0 active machines × 16 = 0, no boxes.
        Map<String, Object> boxes = (Map<String, Object>) zones.get(0).get("boxes");
        assertEquals(0, boxes.get("capacity"));
        assertEquals(0.0, ((Number) boxes.get("pct")).doubleValue(), 0.001);
    }

    // ------------------------------------------------------------------ helpers

    private Zone zone(String nom, String locations) {
        Zone z = new Zone();
        z.setNom(nom);
        z.setCategory(Zone.Category.STRICT);
        z.setRollLocations(locations);
        z.setActive(true);
        return z;
    }

    /** ScanRouleau light projection: 0=serialId, 1=reftissu, 2=quantite, 3=emplacement, 4=lot, 5=metrage. */
    private Object[] rack(String serialId, String reftissu, String emplacement, String lot, double metrage) {
        return new Object[]{serialId, reftissu, 0.0, emplacement, lot, metrage};
    }

    private SerieRouleauTemp spreadRoll(String table, String idRouleau, String reftissu, String lot, double rest) {
        SerieRouleauTemp s = new SerieRouleauTemp();
        s.setTableMatelassage(table);
        s.setIdRouleau(idRouleau);
        s.setReftissu(reftissu);
        s.setLot(lot);
        s.setEstimationRest(rest);
        s.setDate(LocalDateTime.of(TODAY, java.time.LocalTime.of(8, 30)));
        return s;
    }

    private MachineQueue queue(String machine, int pos, String serie, String sequence,
                               String material, Double longueur, Double estCut) {
        MachineQueue q = new MachineQueue(machine, pos, serie, sequence, material, longueur, estCut);
        return q;
    }

    private LiveChargeDto.SerieDto serie(String serie, String machineType, String statusCoupe,
                                         String tableCoupe, String refTissus, double remaining) {
        return new LiveChargeDto.SerieDto(
                serie, machineType, 20.0, 20.0, "TEMPS_DE_COUPE",
                statusCoupe, "Waiting",
                tableCoupe, null,
                null, null, null, null,
                0.0, remaining,
                "Z1", "STRICT",
                refTissus, null, 0.0);
    }

    private LiveChargeDto.SequenceDto sequence(String sequence, List<LiveChargeDto.SerieDto> series) {
        return new LiveChargeDto.SequenceDto(
                sequence, "Z1", "Z1", "Z1", "LOCKED",
                false, true, null, null, null, null,
                false, null, 25.0, series,
                TODAY, 0.0, null);
    }

    private LiveChargeDto.ZoneChargeDto zoneCharge(String nom,
                                                   List<LiveChargeDto.SequenceDto> locked,
                                                   List<LiveChargeDto.SequenceDto> pending) {
        return new LiveChargeDto.ZoneChargeDto(
                nom, "STRICT", Collections.emptyList(),
                locked, pending, 25.0, 432.0, 6.0);
    }
}
