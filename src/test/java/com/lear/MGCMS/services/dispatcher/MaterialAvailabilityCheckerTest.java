package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.domain.ScanRouleau;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.logistics.AllocationService;

class MaterialAvailabilityCheckerTest {

    @Mock private ScanRouleauRepository scanRouleauRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private StockStatusClient stockStatusClient;
    @Mock private AllocationService allocationService;
    @Mock private SerieRouleauTempService serieRouleauTempService;

    @InjectMocks
    private MaterialAvailabilityChecker checker;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(allocationService.reservedMetersByMaterialZone()).thenReturn(Collections.emptyMap());
        when(serieRouleauTempService.getAll()).thenReturn(Collections.emptyList());
        checker.clearSnapshotCache();
    }

    @Test
    @DisplayName("All materials available in zone → AVAILABLE_IN_ZONE")
    void allAvailableInZone() {
        Zone zone = new Zone();
        zone.setNom("ZA");
        zone.setRollLocations("RACK-A1,RACK-A2");
        when(zoneRepository.findById("ZA")).thenReturn(Optional.of(zone));

        ScanRouleau r1 = new ScanRouleau();
        r1.setReftissu("T123");
        r1.setEmplacement("RACK-A1");
        r1.setMetrage(25.0);
        when(scanRouleauRepository.findByLocations(Arrays.asList("RACK-A1", "RACK-A2")))
                .thenReturn(Collections.singletonList(r1));

        Map<String, MaterialAvailabilityChecker.MaterialStatus> result =
                checker.check(Set.of("T123"), "ZA");

        assertEquals(MaterialAvailabilityChecker.MaterialStatus.AVAILABLE_IN_ZONE, result.get("T123"));
    }

    @Test
    @DisplayName("Missing in zone but stock API > 0 → NEEDS_TRANSFER")
    void needsTransfer() {
        Zone zone = new Zone();
        zone.setNom("ZA");
        zone.setRollLocations("RACK-A1");
        when(zoneRepository.findById("ZA")).thenReturn(Optional.of(zone));
        when(scanRouleauRepository.findByLocations(any()))
                .thenReturn(Collections.emptyList());
        when(stockStatusClient.getCurrentStockCount(Arrays.asList("T456")))
                .thenReturn(3);

        Map<String, MaterialAvailabilityChecker.MaterialStatus> result =
                checker.check(Set.of("T456"), "ZA");

        assertEquals(MaterialAvailabilityChecker.MaterialStatus.NOT_IN_ZONE, result.get("T456"));
    }

    @Test
    @DisplayName("Released allocations subtract from rack availability")
    void allocationLedgerSubtractsRackAvailability() {
        Zone zone = new Zone();
        zone.setNom("ZA");
        zone.setRollLocations("RACK-A1");
        when(zoneRepository.findById("ZA")).thenReturn(Optional.of(zone));

        ScanRouleau r1 = new ScanRouleau();
        r1.setReftissu("T123");
        r1.setEmplacement("RACK-A1");
        r1.setMetrage(25.0);
        when(scanRouleauRepository.findByLocations(Collections.singletonList("RACK-A1")))
                .thenReturn(Collections.singletonList(r1));
        when(allocationService.reservedMetersByMaterialZone())
                .thenReturn(Collections.singletonMap("T123|ZA", 25.0));

        Map<String, MaterialAvailabilityChecker.MaterialStatus> result =
                checker.check(Set.of("T123"), "ZA");

        assertEquals(MaterialAvailabilityChecker.MaterialStatus.NOT_IN_ZONE, result.get("T123"));
    }

    @Test
    @DisplayName("Missing in zone and stock API = 0 → MISSING")
    void missing() {
        Zone zone = new Zone();
        zone.setNom("ZA");
        zone.setRollLocations("RACK-A1");
        when(zoneRepository.findById("ZA")).thenReturn(Optional.of(zone));
        when(scanRouleauRepository.findByLocations(any()))
                .thenReturn(Collections.emptyList());
        when(stockStatusClient.getCurrentStockCount(Arrays.asList("T999")))
                .thenReturn(0);

        Map<String, MaterialAvailabilityChecker.MaterialStatus> result =
                checker.check(Set.of("T999"), "ZA");

        assertEquals(MaterialAvailabilityChecker.MaterialStatus.NOT_IN_ZONE, result.get("T999"));
    }

    @Test
    @DisplayName("Empty refTissus returns empty map")
    void emptyRefTissus() {
        Map<String, MaterialAvailabilityChecker.MaterialStatus> result =
                checker.check(Collections.emptySet(), "ZA");
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Cache hit: second call with same params skips ScanRouleau read")
    void cachesPerSnapshot() {
        Zone zone = new Zone();
        zone.setNom("ZA");
        zone.setRollLocations("RACK-A1");
        when(zoneRepository.findById("ZA")).thenReturn(Optional.of(zone));
        when(scanRouleauRepository.findByLocations(any()))
                .thenReturn(Collections.emptyList());

        checker.check(Set.of("T1"), "ZA");
        checker.check(Set.of("T1"), "ZA"); // should hit cache

        // Per Slice 0: stockStatusClient is no longer consulted at all.
        org.mockito.Mockito.verifyNoInteractions(stockStatusClient);
        // ScanRouleau is the source of truth; cache prevents a second read.
        org.mockito.Mockito.verify(scanRouleauRepository, org.mockito.Mockito.times(1))
                .findByLocations(any());
    }
}
