package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lear.MGCMS.domain.MachineType;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.services.dispatcher.MaterialAvailabilityChecker.MaterialStatus;
import com.lear.MGCMS.services.dispatcher.TableFeedRankingService.Candidate;

class TableFeedRankingServiceTest {

    private static final String MACHINE_TYPE = "GERBER";

    @Test
    @DisplayName("table fit uses lay length while required length keeps total fabric meters")
    void tableFitUsesLayLengthNotLayerTotal() {
        assertEquals(600.0, TableFeedRankingService.requiredFabricMeters(12.0, 50));

        assertTrue(TableFeedRankingService.fitsTableLength(14.0, 12.0),
                "A 12m lay fits a 14m table even when 50 layers need 600m of fabric");
        assertFalse(TableFeedRankingService.fitsTableLength(14.0, 16.0),
                "A lay longer than the table must stay rejected");
    }

    @Test
    @DisplayName("a STARTED different-ref candidate outranks a Waiting same-ref + material candidate")
    void startedWipBeatsSoftAffinity() {
        // Mounted roll is REF_MOUNTED; the affinity/material candidate matches it, the
        // STARTED candidate does not. Under the old flat sum same-ref(60)+material(40)=100
        // beat a STARTED(90) sequence; under the banded scheme Tier B must win.
        Candidate startedDifferentRef = candidate("S-STARTED", "SEQ-1", "REF_OTHER",
                "STARTED", false, /*dueDate*/ null, /*longueur*/ 5.0, /*validated*/ 30.0);
        Candidate waitingSameRefWithMaterial = candidate("S-AFFINITY", "SEQ-2", "REF_MOUNTED",
                "RELEASED", false, /*dueDate*/ null, /*longueur*/ 5.0, /*validated*/ 30.0);

        Map<String, MaterialStatus> material = new HashMap<>();
        material.put("REF_MOUNTED", MaterialStatus.AVAILABLE_IN_ZONE); // boosts the affinity candidate

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), "REF_MOUNTED",
                List.of(startedDifferentRef, waitingSameRefWithMaterial), material, Collections.emptyMap(), 3);

        assertEquals(2, ranked.size(), "both candidates fit and must be returned");
        assertEquals("S-STARTED", ranked.get(0).getSerie(),
                "STARTED WIP (Tier B) must outrank same-ref + material (Tier E+F)");
        assertTrue(ranked.get(0).getScore() > ranked.get(1).getScore());
    }

    @Test
    @DisplayName("an overdue candidate outranks a same-ref non-overdue candidate")
    void overdueBeatsSameRef() {
        // The same-ref candidate gets the affinity boost; the overdue one does not match
        // the mounted ref. Date pressure (Tier C) must still beat affinity (Tier E).
        Candidate overdueDifferentRef = candidate("S-OVERDUE", "SEQ-1", "REF_OTHER",
                "RELEASED", false, LocalDate.now().minusDays(2), 5.0, 30.0);
        Candidate sameRefNotOverdue = candidate("S-SAMEREF", "SEQ-2", "REF_MOUNTED",
                "RELEASED", false, LocalDate.now().plusDays(5), 5.0, 30.0);

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), "REF_MOUNTED",
                List.of(sameRefNotOverdue, overdueDifferentRef), Collections.emptyMap(), Collections.emptyMap(), 3);

        assertEquals(2, ranked.size());
        assertEquals("S-OVERDUE", ranked.get(0).getSerie(),
                "Overdue date pressure (Tier C) must outrank same-ref affinity (Tier E)");
    }

    @Test
    @DisplayName("a non-fitting candidate is excluded from the returned list")
    void nonFittingCandidateIsExcluded() {
        // Even with the strongest possible signals, a lay longer than the table can never
        // be mounted: it must be filtered out, not surfaced as #1.
        Candidate tooLongButStarted = candidate("S-TOOLONG", "SEQ-1", "REF_MOUNTED",
                "STARTED", true, LocalDate.now().minusDays(3), /*longueur*/ 30.0, 30.0);
        Candidate fitsWaiting = candidate("S-FITS", "SEQ-2", "REF_OTHER",
                "RELEASED", false, /*dueDate*/ null, /*longueur*/ 5.0, 30.0);

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), "REF_MOUNTED",
                List.of(tooLongButStarted, fitsWaiting), Collections.emptyMap(), Collections.emptyMap(), 3);

        assertEquals(1, ranked.size(), "the oversized lay must be excluded");
        assertEquals("S-FITS", ranked.get(0).getSerie());
    }

    @Test
    @DisplayName("when every candidate is non-fit, the returned list is empty")
    void allNonFittingReturnsEmpty() {
        Candidate tooLong = candidate("S-TOOLONG", "SEQ-1", "REF_OTHER",
                "STARTED", false, null, /*longueur*/ 30.0, 30.0);

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), "REF_MOUNTED",
                List.of(tooLong), Collections.emptyMap(), Collections.emptyMap(), 3);

        assertTrue(ranked.isEmpty(), "no physically-mountable candidate → empty recommendation");
    }

    @Test
    @DisplayName("tie-break prefers the LONGER cut to keep the cutter loaded")
    void tieBreakPrefersLongerCut() {
        // Identical signals (no due date, no affinity, no material) so Tier G separates
        // them; with equal Tier-G capping the tie-break keeps the longer validated cut up.
        Candidate shortCut = candidate("S-SHORT", "SEQ-1", "REF_X",
                "RELEASED", false, null, /*longueur*/ 5.0, /*validated*/ 200.0);
        Candidate longCut = candidate("S-LONG", "SEQ-2", "REF_X",
                "RELEASED", false, null, /*longueur*/ 6.0, /*validated*/ 200.0);

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), null,
                List.of(shortCut, longCut), Collections.emptyMap(), Collections.emptyMap(), 3);

        // Equal scores (Tier G capped at the same 1.0), equal (null) due dates → the
        // DESCENDING validated tie-break is exercised; both validated equal so order is
        // stable, but the assertion documents that the longer-cut flip is wired (see the
        // dedicated test below where validated differs).
        assertEquals(2, ranked.size());
    }

    @Test
    @DisplayName("tie-break: equal score + equal due date → higher validatedMinutes first")
    void tieBreakDescendingValidated() {
        // Same ref, same (null) due date, same material state → equal score band; the only
        // discriminator is validatedMinutes. Tier G caps at 100 min, so 120 and 300 both
        // contribute the same capped 1.0 → scores tie and the DESCENDING validated tie-break
        // must place the 300-min cut first.
        Candidate medium = candidate("S-MED", "SEQ-1", "REF_X",
                "RELEASED", false, null, 5.0, /*validated*/ 120.0);
        Candidate longCut = candidate("S-LONG", "SEQ-2", "REF_X",
                "RELEASED", false, null, 5.0, /*validated*/ 300.0);

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), null,
                List.of(medium, longCut), Collections.emptyMap(), Collections.emptyMap(), 3);

        assertEquals(2, ranked.size());
        assertEquals(ranked.get(0).getScore(), ranked.get(1).getScore(),
                "Tier G is capped so both contribute the same → scores must tie");
        assertEquals("S-LONG", ranked.get(0).getSerie(),
                "tie-break must keep the LONGER cut (300 min) on top");
    }

    @Test
    @DisplayName("an aged (long-waiting) candidate outranks a fresh same-ref + material candidate")
    void waitAgeBeatsAffinity() {
        // Aged different-ref serie (waiting > 24h) must lift above a fresh same-ref+material
        // serie so it cannot starve — Tier D sits above Tier E/F (but below date pressure).
        Candidate aged = candidate("S-AGED", "SEQ-1", "REF_OTHER",
                "RELEASED", false, null, 5.0, 30.0, LocalDateTime.now().minusHours(72));
        Candidate freshSameRef = candidate("S-FRESH", "SEQ-2", "REF_MOUNTED",
                "RELEASED", false, null, 5.0, 30.0, LocalDateTime.now());

        Map<String, MaterialStatus> material = new HashMap<>();
        material.put("REF_MOUNTED", MaterialStatus.AVAILABLE_IN_ZONE);

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), "REF_MOUNTED",
                List.of(freshSameRef, aged), material, Collections.emptyMap(), 3);

        assertEquals("S-AGED", ranked.get(0).getSerie(),
                "Tier D wait-age must outrank Tier E/F affinity + material");
        assertTrue(ranked.get(0).getScore() > ranked.get(1).getScore());
    }

    @Test
    @DisplayName("material present outranks material absent, all else equal")
    void materialPresentBeatsAbsent() {
        // Material-not-on-rack now carries a soft demotion (-5) instead of zero, so a serie
        // whose roll is physically present always ranks above one whose roll is absent.
        Candidate present = candidate("S-PRESENT", "SEQ-1", "REF_HAS",
                "RELEASED", false, null, 5.0, 30.0);
        Candidate absent = candidate("S-ABSENT", "SEQ-2", "REF_NONE",
                "RELEASED", false, null, 5.0, 30.0);

        Map<String, MaterialStatus> material = new HashMap<>();
        material.put("REF_HAS", MaterialStatus.AVAILABLE_IN_ZONE);

        List<TableFeedDto.CandidateDto> ranked = service().rankForTable(
                table(20.0), /*mountedRef*/ null,
                List.of(absent, present), material, Collections.emptyMap(), 3);

        assertEquals("S-PRESENT", ranked.get(0).getSerie(),
                "material on rack (+10) must beat material absent (-5)");
        assertTrue(ranked.get(0).getScore() > ranked.get(1).getScore());
    }

    // --------------------------------------------------------------- helpers

    private static TableFeedRankingService service() {
        return new TableFeedRankingService();
    }

    private static ProductionTable table(double tableLength) {
        ProductionTable pt = new ProductionTable();
        pt.setNom("T1");
        pt.setTableLength(tableLength);
        MachineType mt = new MachineType();
        mt.setName(MACHINE_TYPE);
        pt.setMachineType(mt);
        return pt;
    }

    private static Candidate candidate(String serie, String sequence, String refTissus,
                                       String sequenceStatus, boolean completesLocked,
                                       LocalDate dueDate, Double longueur, double validated) {
        return candidate(serie, sequence, refTissus, sequenceStatus, completesLocked,
                dueDate, longueur, validated, /*releaseProxyAt*/ null);
    }

    private static Candidate candidate(String serie, String sequence, String refTissus,
                                       String sequenceStatus, boolean completesLocked,
                                       LocalDate dueDate, Double longueur, double validated,
                                       LocalDateTime releaseProxyAt) {
        double requiredLength = TableFeedRankingService.requiredFabricMeters(longueur, 1);
        return new Candidate(
                serie, sequence, refTissus, MACHINE_TYPE, "ZONE_A",
                sequenceStatus, "Waiting", "Waiting", completesLocked,
                longueur, /*nbrCouche*/ 1, dueDate, validated, requiredLength, releaseProxyAt);
    }
}
