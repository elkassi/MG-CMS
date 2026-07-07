package com.lear.MGCMS.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TableLengthConstraintTest {

    @Test
    @DisplayName("Empty slots always fit")
    void emptyFits() {
        assertTrue(TableLengthConstraint.fits(Collections.emptyList(), 100.0));
    }

    @Test
    @DisplayName("Non-overlapping slots fit independently of sum")
    void nonOverlappingFits() {
        List<TableLengthConstraint.TableSlot> slots = Arrays.asList(
                new TableLengthConstraint.TableSlot("S1", 80.0,
                        LocalDateTime.of(2026, 5, 1, 8, 0),
                        LocalDateTime.of(2026, 5, 1, 9, 0)),
                new TableLengthConstraint.TableSlot("S2", 80.0,
                        LocalDateTime.of(2026, 5, 1, 9, 0),
                        LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        assertTrue(TableLengthConstraint.fits(slots, 80.0));
    }

    @Test
    @DisplayName("Overlapping slots that exceed table length are rejected")
    void overlappingExceeds() {
        List<TableLengthConstraint.TableSlot> slots = Arrays.asList(
                new TableLengthConstraint.TableSlot("S1", 60.0,
                        LocalDateTime.of(2026, 5, 1, 8, 0),
                        LocalDateTime.of(2026, 5, 1, 10, 0)),
                new TableLengthConstraint.TableSlot("S2", 60.0,
                        LocalDateTime.of(2026, 5, 1, 9, 0),
                        LocalDateTime.of(2026, 5, 1, 11, 0))
        );
        assertFalse(TableLengthConstraint.fits(slots, 100.0));
    }

    @Test
    @DisplayName("Overlapping slots within table length are accepted")
    void overlappingWithinLimit() {
        List<TableLengthConstraint.TableSlot> slots = Arrays.asList(
                new TableLengthConstraint.TableSlot("S1", 40.0,
                        LocalDateTime.of(2026, 5, 1, 8, 0),
                        LocalDateTime.of(2026, 5, 1, 10, 0)),
                new TableLengthConstraint.TableSlot("S2", 50.0,
                        LocalDateTime.of(2026, 5, 1, 9, 0),
                        LocalDateTime.of(2026, 5, 1, 11, 0))
        );
        assertTrue(TableLengthConstraint.fits(slots, 100.0));
    }

    @Test
    @DisplayName("Boundary: sum exactly equals table length")
    void boundaryExact() {
        List<TableLengthConstraint.TableSlot> slots = Arrays.asList(
                new TableLengthConstraint.TableSlot("S1", 50.0,
                        LocalDateTime.of(2026, 5, 1, 8, 0),
                        LocalDateTime.of(2026, 5, 1, 10, 0)),
                new TableLengthConstraint.TableSlot("S2", 50.0,
                        LocalDateTime.of(2026, 5, 1, 9, 0),
                        LocalDateTime.of(2026, 5, 1, 11, 0))
        );
        assertTrue(TableLengthConstraint.fits(slots, 100.0));
    }

    @Test
    @DisplayName("Three-way overlapping cluster")
    void threeWayOverlap() {
        List<TableLengthConstraint.TableSlot> slots = Arrays.asList(
                new TableLengthConstraint.TableSlot("S1", 30.0,
                        LocalDateTime.of(2026, 5, 1, 8, 0),
                        LocalDateTime.of(2026, 5, 1, 12, 0)),
                new TableLengthConstraint.TableSlot("S2", 30.0,
                        LocalDateTime.of(2026, 5, 1, 9, 0),
                        LocalDateTime.of(2026, 5, 1, 10, 0)),
                new TableLengthConstraint.TableSlot("S3", 30.0,
                        LocalDateTime.of(2026, 5, 1, 10, 0),
                        LocalDateTime.of(2026, 5, 1, 11, 0))
        );
        assertFalse(TableLengthConstraint.fits(slots, 80.0));
        assertTrue(TableLengthConstraint.fits(slots, 90.0));
    }
}
