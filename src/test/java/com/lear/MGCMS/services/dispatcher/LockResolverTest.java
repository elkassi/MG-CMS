package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lear.MGCMS.domain.Zone;

/**
 * Unit tests for {@link LockResolver} — the lock decision logic shared by
 * {@code LiveChargeService} and {@code ContinuousDispatchOptimizerService}.
 *
 * <p>Two lock conditions, in priority order:</p>
 * <ol>
 *   <li>EXPLICIT: zoneAcceptanceStatus = ACCEPTED → dispatchedZone</li>
 *   <li>IMPLICIT: any serie statusCoupe != Waiting + tableCoupe → STRICT zone
 *       (tie-break by most recent dateDebutCoupe)</li>
 * </ol>
 */
class LockResolverTest {

    private static LockResolver.SerieLockInput serie(String id, String status,
                                                     String tableCoupe, LocalDateTime started) {
        return new LockResolver.SerieLockInput(id, status, tableCoupe, started);
    }

    private static Map<String, LockResolver.TableZoneInfo> tableMap(Object... pairs) {
        Map<String, LockResolver.TableZoneInfo> m = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 3) {
            String table = (String) pairs[i];
            String zone = (String) pairs[i + 1];
            Zone.Category cat = (Zone.Category) pairs[i + 2];
            m.put(table, new LockResolver.TableZoneInfo(zone, cat));
        }
        return m;
    }

    // ============================================================ EXPLICIT lock

    @Test
    @DisplayName("ACCEPTED + dispatchedZone → EXPLICIT_ACCEPTED locked to dispatchedZone")
    void explicit_accepted_locks_to_dispatched_zone() {
        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                "FirstArticle", "ACCEPTED",
                Collections.emptyList(), Collections.emptyMap());

        assertTrue(res.isPresent());
        assertEquals("FirstArticle", res.get().getLockZoneNom());
        assertEquals(LockResolver.LockReason.EXPLICIT_ACCEPTED, res.get().getReason());
        assertNull(res.get().getLockingSerieId());
    }

    @Test
    @DisplayName("ACCEPTED with null dispatchedZone → not locked (no zone to lock to)")
    void explicit_accepted_without_dispatched_zone_is_not_locked() {
        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, "ACCEPTED",
                Collections.emptyList(), Collections.emptyMap());
        assertFalse(res.isPresent());
    }

    @Test
    @DisplayName("ACCEPTED is case-insensitive")
    void explicit_accepted_case_insensitive() {
        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                "Z1", "accepted",
                Collections.emptyList(), Collections.emptyMap());
        assertTrue(res.isPresent());
    }

    // ============================================================ IMPLICIT lock

    @Test
    @DisplayName("In-progress serie on STRICT-zone table → IMPLICIT_TABLE_STRICT locked to that zone")
    void implicit_in_progress_on_strict_table_locks() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-12", "FirstArticle", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                "OtherZone", "PENDING",
                Arrays.asList(serie("S1", "In progress", "T-12",
                        LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        assertTrue(res.isPresent());
        assertEquals("FirstArticle", res.get().getLockZoneNom());
        assertEquals(LockResolver.LockReason.IMPLICIT_TABLE_STRICT, res.get().getReason());
        assertEquals("S1", res.get().getLockingSerieId());
        assertEquals("T-12", res.get().getLockingTableNom());
        assertEquals("In progress", res.get().getLockingStatusCoupe());
    }

    @Test
    @DisplayName("Complete serie on STRICT-zone table → still implicit-locks (statusCoupe != Waiting)")
    void implicit_complete_on_strict_table_locks() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-12", "FirstArticle", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(serie("S1", "Complete", "T-12", null)),
                tables);

        assertTrue(res.isPresent());
        assertEquals("FirstArticle", res.get().getLockZoneNom());
    }

    @Test
    @DisplayName("Waiting serie on STRICT-zone table → not locked (work hasn't started)")
    void implicit_waiting_on_strict_table_does_not_lock() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-12", "FirstArticle", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(serie("S1", "Waiting", "T-12",
                        LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        assertFalse(res.isPresent());
    }

    @Test
    @DisplayName("In-progress serie on SHARED-zone table → ignored, not locked")
    void implicit_shared_zone_table_is_ignored() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-S1", "SharedLaser", Zone.Category.SHARED);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(serie("S1", "In progress", "T-S1",
                        LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        assertFalse(res.isPresent());
    }

    @Test
    @DisplayName("In-progress serie with unknown table → not locked")
    void implicit_unknown_table_is_not_locked() {
        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(serie("S1", "In progress", "T-Unknown",
                        LocalDateTime.of(2026, 5, 7, 14, 0))),
                Collections.emptyMap());

        assertFalse(res.isPresent());
    }

    @Test
    @DisplayName("Mix: SHARED serie ignored, STRICT serie wins")
    void implicit_mix_shared_then_strict_locks_to_strict() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-S1", "SharedLaser", Zone.Category.SHARED,
                "T-12", "FirstArticle", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(
                        serie("S1", "In progress", "T-S1",
                                LocalDateTime.of(2026, 5, 7, 15, 0)),
                        serie("S2", "In progress", "T-12",
                                LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        assertTrue(res.isPresent());
        assertEquals("FirstArticle", res.get().getLockZoneNom());
        assertEquals("S2", res.get().getLockingSerieId());
    }

    // ============================================================ Tie-break

    @Test
    @DisplayName("Multi-zone tie-break: most recent dateDebutCoupe wins")
    void tie_break_most_recent_start_wins() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-A", "ZoneA", Zone.Category.STRICT,
                "T-B", "ZoneB", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                "OtherZone", "PENDING",
                Arrays.asList(
                        serie("S1", "In progress", "T-A",
                                LocalDateTime.of(2026, 5, 7, 10, 0)),     // older
                        serie("S2", "In progress", "T-B",
                                LocalDateTime.of(2026, 5, 7, 14, 0))),    // newer wins
                tables);

        assertTrue(res.isPresent());
        assertEquals("ZoneB", res.get().getLockZoneNom());
        assertEquals("S2", res.get().getLockingSerieId());
    }

    @Test
    @DisplayName("Tie-break: null dateDebutCoupe loses to non-null")
    void tie_break_null_start_loses() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-A", "ZoneA", Zone.Category.STRICT,
                "T-B", "ZoneB", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(
                        serie("S1", "Complete", "T-A", null),
                        serie("S2", "In progress", "T-B",
                                LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        // S1 is found first (no start) but S2 has a start so wins.
        assertTrue(res.isPresent());
        assertEquals("ZoneB", res.get().getLockZoneNom());
    }

    @Test
    @DisplayName("Tie-break: both nulls — first found stays")
    void tie_break_both_nulls_first_wins() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-A", "ZoneA", Zone.Category.STRICT,
                "T-B", "ZoneB", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(
                        serie("S1", "Complete", "T-A", null),
                        serie("S2", "Complete", "T-B", null)),
                tables);

        assertTrue(res.isPresent());
        assertEquals("ZoneA", res.get().getLockZoneNom());
        assertEquals("S1", res.get().getLockingSerieId());
    }

    // ============================================================ Precedence

    @Test
    @DisplayName("EXPLICIT takes precedence over IMPLICIT")
    void explicit_beats_implicit() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-12", "PhysicalZone", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                "AcceptedZone", "ACCEPTED",
                Arrays.asList(serie("S1", "In progress", "T-12",
                        LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        assertTrue(res.isPresent());
        assertEquals(LockResolver.LockReason.EXPLICIT_ACCEPTED, res.get().getReason());
        assertEquals("AcceptedZone", res.get().getLockZoneNom());
    }

    // ============================================================ Edge cases

    @Test
    @DisplayName("Empty series + no acceptance → not locked")
    void empty_series_not_locked() {
        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                "Z1", "PENDING",
                Collections.emptyList(),
                Collections.emptyMap());
        assertFalse(res.isPresent());
    }

    @Test
    @DisplayName("Null tableCoupe → ignored (cannot resolve to zone)")
    void null_table_is_ignored() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-12", "ZoneA", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(serie("S1", "In progress", null,
                        LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        assertFalse(res.isPresent());
    }

    @Test
    @DisplayName("Null statusCoupe → ignored")
    void null_status_is_ignored() {
        Map<String, LockResolver.TableZoneInfo> tables = tableMap(
                "T-12", "ZoneA", Zone.Category.STRICT);

        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                null, null,
                Arrays.asList(serie("S1", null, "T-12",
                        LocalDateTime.of(2026, 5, 7, 14, 0))),
                tables);

        assertFalse(res.isPresent());
    }

    @Test
    @DisplayName("REJECTED acceptance status does not lock (only ACCEPTED does)")
    void rejected_does_not_lock() {
        Optional<LockResolver.LockResult> res = LockResolver.resolve(
                "Z1", "REJECTED",
                Collections.emptyList(),
                Collections.emptyMap());
        assertFalse(res.isPresent());
    }
}
