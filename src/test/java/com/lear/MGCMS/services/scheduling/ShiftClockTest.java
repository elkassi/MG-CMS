package com.lear.MGCMS.services.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ShiftClock}. Pins the cutoffs to the plant schedule so a
 * future refactor can't silently regress to the old wrong (round-hour) values
 * the dispatcher used to ship with.
 *
 * <p>Plant convention:</p>
 * <ul>
 *   <li>Shift 1 — 21:55 (D-1) → 05:45 (D), date = D</li>
 *   <li>Shift 2 — 05:55 → 13:45, date = D</li>
 *   <li>Shift 3 — 13:55 → 21:45, date = D</li>
 * </ul>
 */
class ShiftClockTest {

    private final ShiftClock clock = new ShiftClock();
    private static final LocalDate D = LocalDate.of(2026, 5, 7);

    @Test
    @DisplayName("Mid-night returns shift 1 with current calendar date")
    void earlyMorning_returnsShift1() {
        ShiftClock.ShiftSlot slot = clock.slotAt(LocalDateTime.of(D, LocalTime.of(2, 30)));
        assertEquals(1, slot.shift);
        assertEquals(D, slot.date);
    }

    @Test
    @DisplayName("Just before shift 2 start (05:50) is still shift 1")
    void handoverBeforeShift2_returnsShift1() {
        ShiftClock.ShiftSlot slot = clock.slotAt(LocalDateTime.of(D, LocalTime.of(5, 50)));
        assertEquals(1, slot.shift);
        assertEquals(D, slot.date);
    }

    @Test
    @DisplayName("Morning hour returns shift 2 (NOT shift 1 like the old dispatcher claimed)")
    void morning_returnsShift2() {
        ShiftClock.ShiftSlot slot = clock.slotAt(LocalDateTime.of(D, LocalTime.of(10, 0)));
        assertEquals(2, slot.shift);
        assertEquals(D, slot.date);
    }

    @Test
    @DisplayName("Just before shift 3 start (13:50) is still shift 2")
    void handoverBeforeShift3_returnsShift2() {
        ShiftClock.ShiftSlot slot = clock.slotAt(LocalDateTime.of(D, LocalTime.of(13, 50)));
        assertEquals(2, slot.shift);
    }

    @Test
    @DisplayName("Afternoon hour returns shift 3 (NOT shift 2 like the old dispatcher claimed)")
    void afternoon_returnsShift3() {
        ShiftClock.ShiftSlot slot = clock.slotAt(LocalDateTime.of(D, LocalTime.of(16, 0)));
        assertEquals(3, slot.shift);
        assertEquals(D, slot.date);
    }

    @Test
    @DisplayName("Late evening (after 21:55) rolls into shift 1 of NEXT day")
    void lateEvening_rollsToNextDayShift1() {
        ShiftClock.ShiftSlot slot = clock.slotAt(LocalDateTime.of(D, LocalTime.of(22, 30)));
        assertEquals(1, slot.shift);
        assertEquals(D.plusDays(1), slot.date);
    }

    @Test
    @DisplayName("Just before next-day rollover (21:50) is still shift 3 of today")
    void justBeforeRollover_returnsShift3() {
        ShiftClock.ShiftSlot slot = clock.slotAt(LocalDateTime.of(D, LocalTime.of(21, 50)));
        assertEquals(3, slot.shift);
        assertEquals(D, slot.date);
    }
}
