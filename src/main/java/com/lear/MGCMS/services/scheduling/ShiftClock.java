package com.lear.MGCMS.services.scheduling;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

/**
 * Single source of truth for "what shift are we in right now?" used by the
 * Phase 4+ dispatcher, the continuous optimizer, and any future code that
 * needs to scope work to the current plant shift.
 *
 * <p>Plant convention (updated to match actual shift boundaries):</p>
 * <ul>
 *   <li>Shift 1 — night,    21:50 (D-1) → 05:50 (D)</li>
 *   <li>Shift 2 — morning,  05:50 (D)   → 13:50 (D)</li>
 *   <li>Shift 3 — afternoon,13:50 (D)   → 21:50 (D)</li>
 * </ul>
 *
 * <p>Two earlier copies of this logic in {@code SequenceDispatcherService}
 * and {@code ContinuousDispatchOptimizerService} used round-hour cutoffs
 * (6/14/22) <em>and</em> the inverse mapping (1=morning, 3=night), which
 * disagreed with the plant schedule and caused
 * {@code ActiveMachineResolver} to read the wrong shift's machine
 * availability for the real-time active-sequences flow. This class
 * replaces them.</p>
 *
 * <p>The 10-minute gap between a shift's nominal end (e.g. 05:45) and the
 * next shift's nominal start (05:55) is treated as still belonging to the
 * earlier shift. This matches {@code SchedulingService.getCurrentShiftNumber}
 * and the existing {@code PlanDeChargeService} cutoffs.</p>
 */
@Component
public class ShiftClock {

    /** Result of {@link #currentSlot()} — the (date, shift) the plant is in right now. */
    public static final class ShiftSlot {
        public final LocalDate date;
        public final int shift;
        public ShiftSlot(LocalDate date, int shift) {
            this.date = date;
            this.shift = shift;
        }
    }

    private final Clock clock;

    public ShiftClock() {
        this(Clock.systemDefaultZone());
    }

    /** Visible for tests so callers can pin "now" to a fixed instant. */
    public ShiftClock(Clock clock) {
        this.clock = clock;
    }

    /** Convenience: just the shift number for the current wall-clock time. */
    public int currentShiftNumber() {
        return currentSlot().shift;
    }

    /**
     * Resolve the (planning date, shift) the plant is in <em>right now</em>.
     * For shift 1 in the small-hours of the morning, {@code date} is the
     * calendar day the shift ENDS on (matching how {@code dueDate} is set
     * for night work). For shift 1 after 21:55 the same evening, {@code date}
     * is the next calendar day (the shift will roll into).
     */
    public ShiftSlot currentSlot() {
        LocalDateTime now = LocalDateTime.now(clock);
        return slotAt(now);
    }

    /** Pure function exposed for tests + any caller that already has a timestamp. */
    public ShiftSlot slotAt(LocalDateTime now) {
        LocalTime t = now.toLocalTime();
        LocalDate today = now.toLocalDate();
        int minutes = t.getHour() * 60 + t.getMinute();

        // Plant shift boundaries: 21:50, 05:50, 13:50
        // 21:50 = 1310, 05:50 = 350, 13:50 = 830
        if (minutes >= 1310) {
            // Late evening — shift 1 of the NEXT calendar day starts at 21:50.
            return new ShiftSlot(today.plusDays(1), 1);
        }
        if (minutes < 350) {
            // Early morning — still shift 1 that started yesterday at 21:50,
            // ending at 05:50 today. Date is "today" because that's when the
            // shift ends and what dueDate uses for this work.
            return new ShiftSlot(today, 1);
        }
        if (minutes < 830) {
            // 05:50 → 13:50
            return new ShiftSlot(today, 2);
        }
        // 13:50 → 21:50
        return new ShiftSlot(today, 3);
    }
}
