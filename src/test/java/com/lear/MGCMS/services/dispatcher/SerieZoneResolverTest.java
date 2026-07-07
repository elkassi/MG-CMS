package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;

/**
 * Unit tests for {@link SerieZoneResolver} exercising the STRICT-before-SHARED
 * waterfall and the three failure-reason branches.
 */
class SerieZoneResolverTest {

    @Mock private ProductionTableRepository productionTableRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private ActiveMachineResolver activeMachineResolver;
    @Mock private DispatcherProperties dispatcherProperties;

    @InjectMocks private SerieZoneResolver resolver;

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 24);

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(dispatcherProperties.isAllowUnconfirmedZones()).thenReturn(false);
    }

    private static Zone zone(String nom, Zone.Category cat, boolean active) {
        Zone z = new Zone();
        z.setNom(nom);
        z.setCategory(cat);
        z.setActive(active);
        return z;
    }

    private static SerieDispatchInfo serie(String id, String machineType) {
        return new SerieDispatchInfo(id, "1", machineType, 5.0, 10, "P1");
    }

    private static SerieDispatchInfo serie(String id, String machineType, String preferredZoneNom) {
        return new SerieDispatchInfo(id, "1", machineType, 5.0, 10, "P1", preferredZoneNom);
    }

    /** Typed {@code List<Object[]>} helper — {@code Arrays.asList(Object[]...)} flattens. */
    private static List<Object[]> rows(Object[]... rows) {
        List<Object[]> out = new ArrayList<>(rows.length);
        for (Object[] r : rows) out.add(r);
        return out;
    }

    // ------------------------------------------------------------------
    // Happy paths
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("happy path: serie placed in its STRICT zone")
    class HappyStrict {

        @Test
        @DisplayName("Lectra serie → FirstArticle (STRICT)")
        void lectraToFirstArticle() {
            Zone firstArticle = zone("FirstArticle", Zone.Category.STRICT, true);
            when(productionTableRepository.findZonesHostingMachineType("Lectra"))
                    .thenReturn(Collections.singletonList("FirstArticle"));
            when(zoneRepository.findByObjId("FirstArticle")).thenReturn(firstArticle);
            when(activeMachineResolver.activeMachines(TODAY, 1, "FirstArticle"))
                    .thenReturn(Set.of("LEC-01"));
            when(productionTableRepository.findMachinesWithTypeInZone("FirstArticle"))
                    .thenReturn(rows(new Object[]{"LEC-01", "Lectra"}));

            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "Lectra"), TODAY, 1);

            assertTrue(r.isAccepted());
            assertEquals("FirstArticle", r.getZone().getNom());
        }
    }

    @Nested
    @DisplayName("STRICT preferred over SHARED when both host the type")
    class StrictBeforeShared {

        @Test
        @DisplayName("when both FirstArticle (STRICT) and Laser (SHARED) host LASER-DXF, STRICT wins")
        void strictWins() {
            Zone strict = zone("FirstArticle", Zone.Category.STRICT, true);
            Zone shared = zone("Laser",        Zone.Category.SHARED, true);

            when(productionTableRepository.findZonesHostingMachineType("LASER-DXF"))
                    .thenReturn(Arrays.asList("FirstArticle", "Laser"));
            when(zoneRepository.findByObjId("FirstArticle")).thenReturn(strict);
            when(zoneRepository.findByObjId("Laser")).thenReturn(shared);

            when(activeMachineResolver.activeMachines(TODAY, 1, "FirstArticle"))
                    .thenReturn(Set.of("DXF-01"));
            when(productionTableRepository.findMachinesWithTypeInZone("FirstArticle"))
                    .thenReturn(rows(new Object[]{"DXF-01", "LASER-DXF"}));

            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "LASER-DXF"), TODAY, 1);

            assertTrue(r.isAccepted());
            assertEquals("FirstArticle", r.getZone().getNom());
        }

        @Test
        @DisplayName("STRICT zone closed, SHARED open → SHARED wins")
        void sharedFallback() {
            Zone strict = zone("FirstArticle", Zone.Category.STRICT, true);
            Zone shared = zone("Laser",        Zone.Category.SHARED, true);

            when(productionTableRepository.findZonesHostingMachineType("LASER-DXF"))
                    .thenReturn(Arrays.asList("FirstArticle", "Laser"));
            when(zoneRepository.findByObjId("FirstArticle")).thenReturn(strict);
            when(zoneRepository.findByObjId("Laser")).thenReturn(shared);

            when(activeMachineResolver.activeMachines(TODAY, 1, "FirstArticle"))
                    .thenReturn(Collections.emptySet());
            when(activeMachineResolver.activeMachines(TODAY, 1, "Laser"))
                    .thenReturn(Set.of("DXF-07"));
            when(productionTableRepository.findMachinesWithTypeInZone("Laser"))
                    .thenReturn(rows(new Object[]{"DXF-07", "LASER-DXF"}));

            SerieZoneResolver.Resolution r = resolver.resolve(serie("S2", "LASER-DXF"), TODAY, 1);

            assertTrue(r.isAccepted());
            assertEquals("Laser", r.getZone().getNom());
        }
    }

    // ------------------------------------------------------------------
    // Failure modes
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("failure modes → mapped FailureReason")
    class Failures {

        @Test
        @DisplayName("no zone hosts the type → NO_ZONE_ACCEPTING_TYPE")
        void noZoneAccepting() {
            when(productionTableRepository.findZonesHostingMachineType("FOO"))
                    .thenReturn(Collections.emptyList());
            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "FOO"), TODAY, 1);
            assertFalse(r.isAccepted());
            assertEquals(SerieZoneResolver.FailureReason.NO_ZONE_ACCEPTING_TYPE,
                         r.getFailureReason());
        }

        @Test
        @DisplayName("zone exists but no shift confirmation → ALL_ZONES_CLOSED_FOR_SHIFT")
        void allClosed() {
            Zone laser = zone("Laser", Zone.Category.SHARED, true);
            when(productionTableRepository.findZonesHostingMachineType("LASER-DXF"))
                    .thenReturn(Collections.singletonList("Laser"));
            when(zoneRepository.findByObjId("Laser")).thenReturn(laser);
            when(activeMachineResolver.activeMachines(eq(TODAY), anyInt(), eq("Laser")))
                    .thenReturn(Collections.emptySet());

            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "LASER-DXF"), TODAY, 1);

            assertFalse(r.isAccepted());
            assertEquals(SerieZoneResolver.FailureReason.ALL_ZONES_CLOSED_FOR_SHIFT,
                         r.getFailureReason());
        }

        @Test
        @DisplayName("zone confirmed but no up-machine of requested type → NO_ACTIVE_MACHINE_IN_ZONE")
        void confirmedButNoMatchingMachine() {
            Zone fa = zone("FirstArticle", Zone.Category.STRICT, true);
            when(productionTableRepository.findZonesHostingMachineType("Lectra"))
                    .thenReturn(Collections.singletonList("FirstArticle"));
            when(zoneRepository.findByObjId("FirstArticle")).thenReturn(fa);
            when(activeMachineResolver.activeMachines(TODAY, 1, "FirstArticle"))
                    .thenReturn(Set.of("GRB-01")); // wrong machine up
            when(productionTableRepository.findMachinesWithTypeInZone("FirstArticle"))
                    .thenReturn(rows(
                            new Object[]{"LEC-01", "Lectra"},
                            new Object[]{"GRB-01", "Gerber"}));

            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "Lectra"), TODAY, 1);

            assertFalse(r.isAccepted());
            assertEquals(SerieZoneResolver.FailureReason.NO_ACTIVE_MACHINE_IN_ZONE,
                         r.getFailureReason());
        }

        @Test
        @DisplayName("every candidate zone is inactive → NO_ZONE_ACCEPTING_TYPE")
        void allCandidatesInactive() {
            Zone inactive = zone("OldZone", Zone.Category.STRICT, false);
            when(productionTableRepository.findZonesHostingMachineType("Lectra"))
                    .thenReturn(Collections.singletonList("OldZone"));
            when(zoneRepository.findByObjId("OldZone")).thenReturn(inactive);

            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "Lectra"), TODAY, 1);

            assertFalse(r.isAccepted());
            assertEquals(SerieZoneResolver.FailureReason.NO_ZONE_ACCEPTING_TYPE,
                         r.getFailureReason());
        }

                @Test
                @DisplayName("legacy LASER-LSR falls back to LASER-DXF zone hosting")
                void laserLsrAlias() {
                    Zone laser = zone("Laser", Zone.Category.SHARED, true);
                    when(productionTableRepository.findZonesHostingMachineType("LASER-LSR"))
                        .thenReturn(Collections.emptyList());
                    when(productionTableRepository.findZonesHostingMachineType("LASER-DXF"))
                        .thenReturn(Collections.singletonList("Laser"));
                    when(zoneRepository.findByObjId("Laser")).thenReturn(laser);
                    when(activeMachineResolver.activeMachines(TODAY, 1, "Laser"))
                        .thenReturn(Set.of("DXF-07"));
                    when(productionTableRepository.findMachinesWithTypeInZone("Laser"))
                        .thenReturn(rows(new Object[]{"DXF-07", "LASER-DXF"}));

                    SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "LASER-LSR"), TODAY, 1);

                    assertTrue(r.isAccepted());
                    assertEquals("Laser", r.getZone().getNom());
                }

                @Test
                @DisplayName("shadow routing can use zone hosting when confirmations are missing")
                void shadowRoutingAllowsUnconfirmedZone() {
                    Zone laser = zone("Laser", Zone.Category.SHARED, true);
                    when(dispatcherProperties.isAllowUnconfirmedZones()).thenReturn(true);
                    when(productionTableRepository.findZonesHostingMachineType("LASER-DXF"))
                        .thenReturn(Collections.singletonList("Laser"));
                    when(zoneRepository.findByObjId("Laser")).thenReturn(laser);
                    when(activeMachineResolver.activeMachines(eq(TODAY), anyInt(), eq("Laser")))
                        .thenReturn(Collections.emptySet());
                    when(productionTableRepository.findMachinesWithTypeInZone("Laser"))
                        .thenReturn(rows(new Object[]{"DXF-07", "LASER-DXF"}));

                    SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "LASER-DXF"), TODAY, 1);

                    assertTrue(r.isAccepted());
                    assertEquals("Laser", r.getZone().getNom());
                }

                    @Test
                    @DisplayName("preferred legacy zone is tried first during shadow routing")
                    void preferredZoneWinsWhenCompatible() {
                        Zone california = zone("CALIFORNIA", Zone.Category.STRICT, true);
                        Zone nejma = zone("NEJMA", Zone.Category.STRICT, true);
                        when(dispatcherProperties.isAllowUnconfirmedZones()).thenReturn(true);
                        when(productionTableRepository.findZonesHostingMachineType("Lectra"))
                            .thenReturn(Arrays.asList("CALIFORNIA", "NEJMA"));
                        when(zoneRepository.findByObjId("CALIFORNIA")).thenReturn(california);
                        when(zoneRepository.findByObjId("NEJMA")).thenReturn(nejma);
                        when(activeMachineResolver.activeMachines(eq(TODAY), anyInt(), eq("CALIFORNIA")))
                            .thenReturn(Collections.emptySet());
                        when(activeMachineResolver.activeMachines(eq(TODAY), anyInt(), eq("NEJMA")))
                            .thenReturn(Collections.emptySet());
                        when(productionTableRepository.findMachinesWithTypeInZone("CALIFORNIA"))
                            .thenReturn(rows(new Object[]{"CAL-01", "Lectra"}));
                        when(productionTableRepository.findMachinesWithTypeInZone("NEJMA"))
                            .thenReturn(rows(new Object[]{"NEJ-01", "Lectra"}));

                        SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "Lectra", "NEJMA"), TODAY, 1);

                        assertTrue(r.isAccepted());
                        assertEquals("NEJMA", r.getZone().getNom());
                    }
    }

    @Nested
    @DisplayName("null safety")
    class NullSafety {

        @Test
        void nullSerie() {
            SerieZoneResolver.Resolution r = resolver.resolve(null, TODAY, 1);
            assertFalse(r.isAccepted());
        }

        @Test
        void nullMachine() {
            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", null), TODAY, 1);
            assertFalse(r.isAccepted());
        }

        @Test
        void nullDate() {
            SerieZoneResolver.Resolution r = resolver.resolve(serie("S1", "Lectra"), null, 1);
            assertFalse(r.isAccepted());
        }
    }
}
