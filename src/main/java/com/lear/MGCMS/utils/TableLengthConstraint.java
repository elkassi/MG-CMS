package com.lear.MGCMS.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure utility: verifies that the total table length required by overlapping
 * series does not exceed the physical table length.
 */
public class TableLengthConstraint {

    /**
     * @param slots       scheduled slots on one table
     * @param tableLength physical table length
     * @return {@code true} if every overlapping cluster fits
     */
    public static boolean fits(List<TableSlot> slots, double tableLength) {
        if (slots == null || slots.isEmpty()) {
            return true;
        }
        List<TableSlot> sorted = new ArrayList<>(slots);
        sorted.sort(Comparator.comparing(TableSlot::start));

        for (int i = 0; i < sorted.size(); i++) {
            double total = sorted.get(i).lengthRequired();
            LocalDateTime clusterEnd = sorted.get(i).end();
            for (int j = i + 1; j < sorted.size(); j++) {
                TableSlot other = sorted.get(j);
                if (other.start().isBefore(clusterEnd)) {
                    total += other.lengthRequired();
                    if (other.end().isAfter(clusterEnd)) {
                        clusterEnd = other.end();
                    }
                } else {
                    break;
                }
            }
            if (total > tableLength + 1e-9) {
                return false;
            }
        }
        return true;
    }

    public record TableSlot(String serieId, double lengthRequired,
                            LocalDateTime start, LocalDateTime end) {
    }
}
