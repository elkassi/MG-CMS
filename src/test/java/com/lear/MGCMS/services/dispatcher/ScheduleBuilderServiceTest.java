package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScheduleBuilderServiceTest {

    @Test
    @DisplayName("pending schedule order respects dueDate then dueShift before sequence id")
    void build_ordersPendingSeriesByDueDateThenDueShift() {
        ScheduleBuilderService builder = new ScheduleBuilderService();
        LocalDate dueDate = LocalDate.of(2026, 5, 5);
        LocalDateTime horizon = LocalDateTime.of(2026, 5, 5, 6, 0);

        List<ScheduleBuilderService.SerieInput> series = Arrays.asList(
                input("S-current", "A-current", dueDate, "2"),
                input("S-old", "Z-old", dueDate, "1")
        );
        Map<String, String> sequenceToZone = new HashMap<>();
        sequenceToZone.put("A-current", "ZA");
        sequenceToZone.put("Z-old", "ZA");

        Map<String, Map<String, Set<String>>> machinesByZoneByType = new HashMap<>();
        Map<String, Set<String>> byType = new HashMap<>();
        byType.put("Lectra", new LinkedHashSet<>(Arrays.asList("L-A1")));
        machinesByZoneByType.put("ZA", byType);

        ScheduleSnapshot snapshot = builder.build(
                horizon, series, sequenceToZone, machinesByZoneByType);

        List<ScheduleSnapshot.PlannedSlot> coupe = snapshot.slotsForMachine("L-A1").stream()
                .filter(s -> s.getPhase() == ScheduleSnapshot.Phase.COUPE)
                .collect(Collectors.toList());

        assertEquals(2, coupe.size());
        assertEquals("S-old", coupe.get(0).getSerieId(),
                "shift 1 sequence should remain ahead of shift 2 even when its sequence id sorts later");
        assertEquals("S-current", coupe.get(1).getSerieId());
    }

    @Test
    @DisplayName("ready-to-cut frozen rows still receive coupe slots")
    void build_readyToCutStillReceivesCoupeSlot() {
        ScheduleBuilderService builder = new ScheduleBuilderService();
        LocalDateTime horizon = LocalDateTime.of(2026, 5, 22, 1, 0);
        LocalDateTime matStart = horizon.minusMinutes(20);
        LocalDateTime matEnd = horizon.minusMinutes(5);

        List<ScheduleBuilderService.SerieInput> series = java.util.Collections.singletonList(
                new ScheduleBuilderService.SerieInput(
                        "S-ready",
                        "SEQ-open",
                        "Lectra",
                        7.0,
                        1.0,
                        1,
                        "MAT",
                        LocalDate.of(2026, 5, 22),
                        "1",
                        "L-A1",
                        null,
                        true,
                        matStart,
                        matEnd,
                        null,
                        null)
        );
        Map<String, String> sequenceToZone = new HashMap<>();
        sequenceToZone.put("SEQ-open", "ZA");
        Map<String, Map<String, Set<String>>> machinesByZoneByType = machineMap("ZA", "Lectra", "L-A1");

        ScheduleSnapshot snapshot = builder.build(horizon, series, sequenceToZone, machinesByZoneByType);

        List<ScheduleSnapshot.PlannedSlot> slots = snapshot.slotsForMachine("L-A1");
        assertEquals(2, slots.size(), "matelassage is anchored and coupe is planned");
        assertTrue(slots.stream().anyMatch(s -> s.getPhase() == ScheduleSnapshot.Phase.COUPE
                && "S-ready".equals(s.getSerieId())));
    }

    @Test
    @DisplayName("opened sequence pending rows are planned before unopened new sequence")
    void build_openedSequencePendingRowsComeBeforeNewSequence() {
        ScheduleBuilderService builder = new ScheduleBuilderService();
        LocalDateTime horizon = LocalDateTime.of(2026, 5, 22, 1, 0);

        List<ScheduleBuilderService.SerieInput> series = Arrays.asList(
                input("S-new", "A-new", LocalDate.of(2026, 5, 22), "1"),
                new ScheduleBuilderService.SerieInput(
                        "S-open",
                        "Z-open",
                        "Lectra",
                        7.0,
                        1.0,
                        1,
                        "MAT-open",
                        LocalDate.of(2026, 5, 22),
                        "1",
                        "L-A1",
                        null,
                        true,
                        horizon.minusMinutes(10),
                        horizon.minusMinutes(5),
                        null,
                        null)
        );
        Map<String, String> sequenceToZone = new HashMap<>();
        sequenceToZone.put("A-new", "ZA");
        sequenceToZone.put("Z-open", "ZA");

        ScheduleSnapshot snapshot = builder.build(
                horizon, series, sequenceToZone, machineMap("ZA", "Lectra", "L-A1"));

        List<ScheduleSnapshot.PlannedSlot> coupe = snapshot.slotsForMachine("L-A1").stream()
                .filter(s -> s.getPhase() == ScheduleSnapshot.Phase.COUPE)
                .collect(Collectors.toList());

        assertEquals(2, coupe.size());
        assertEquals("S-open", coupe.get(0).getSerieId(),
                "opened sequence should close before a fresh unopened sequence");
        assertEquals("S-new", coupe.get(1).getSerieId());
    }

    @Test
    @DisplayName("coupe queue respects physical lifecycle before movable waiting work")
    void build_respectsLifecycleBeforeWaitingRows() {
        ScheduleBuilderService builder = new ScheduleBuilderService();
        LocalDateTime horizon = LocalDateTime.of(2026, 5, 22, 1, 0);

        List<ScheduleBuilderService.SerieInput> series = Arrays.asList(
                waitingInput("S-wait", "SEQ-wait", "MAT-D", 0.4),
                lifecycleInput("S-spread", "SEQ-spread", "Waiting", "In progress",
                        "L-A1", null, horizon.minusMinutes(5), null, null, null),
                lifecycleInput("S-ready", "SEQ-ready", "Waiting", "Complete",
                        "L-A1", null, null, null, null, null),
                lifecycleInput("S-cut", "SEQ-cut", "In progress", "Complete",
                        "L-A1", "L-A1", horizon.minusMinutes(20), horizon.minusMinutes(15),
                        horizon.minusMinutes(10), null)
        );
        Map<String, String> sequenceToZone = new HashMap<>();
        sequenceToZone.put("SEQ-wait", "ZA");
        sequenceToZone.put("SEQ-spread", "ZA");
        sequenceToZone.put("SEQ-ready", "ZA");
        sequenceToZone.put("SEQ-cut", "ZA");

        ScheduleSnapshot snapshot = builder.build(
                horizon, series, sequenceToZone, machineMap("ZA", "Lectra", "L-A1"));

        List<String> coupeOrder = snapshot.slotsForMachine("L-A1").stream()
                .filter(s -> s.getPhase() == ScheduleSnapshot.Phase.COUPE)
                .map(ScheduleSnapshot.PlannedSlot::getSerieId)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("S-cut", "S-ready", "S-spread", "S-wait"), coupeOrder);
    }

    @Test
    @DisplayName("short waiting series of same material are grouped consecutively")
    void build_groupsSmallWaitingRowsByMaterial() {
        ScheduleBuilderService builder = new ScheduleBuilderService();
        LocalDateTime horizon = LocalDateTime.of(2026, 5, 22, 1, 0);

        List<ScheduleBuilderService.SerieInput> series = Arrays.asList(
                waitingInput("S-a-long", "SEQ-4", "MAT-C", 2.0),
                waitingInput("S-a1", "SEQ-1", "MAT-A", 0.9),
                waitingInput("S-b1", "SEQ-2", "MAT-B", 0.2),
                waitingInput("S-a2", "SEQ-3", "MAT-A", 0.3)
        );
        Map<String, String> sequenceToZone = new HashMap<>();
        sequenceToZone.put("SEQ-1", "ZA");
        sequenceToZone.put("SEQ-2", "ZA");
        sequenceToZone.put("SEQ-3", "ZA");
        sequenceToZone.put("SEQ-4", "ZA");

        ScheduleSnapshot snapshot = builder.build(
                horizon, series, sequenceToZone, machineMap("ZA", "Lectra", "L-A1"));

        List<String> coupeOrder = snapshot.slotsForMachine("L-A1").stream()
                .filter(s -> s.getPhase() == ScheduleSnapshot.Phase.COUPE)
                .map(ScheduleSnapshot.PlannedSlot::getSerieId)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("S-a2", "S-a1", "S-b1", "S-a-long"), coupeOrder);
    }

    private static ScheduleBuilderService.SerieInput input(
            String serie, String sequence, LocalDate dueDate, String dueShift) {
        return new ScheduleBuilderService.SerieInput(
                serie,
                sequence,
                "Lectra",
                10.0,
                1.0,
                1,
                "MAT-" + serie,
                dueDate,
                dueShift,
                null,
                null,
                false,
                null,
                null,
                null,
                null);
    }

    private static ScheduleBuilderService.SerieInput waitingInput(
            String serie, String sequence, String material, double longueur) {
        return new ScheduleBuilderService.SerieInput(
                serie,
                sequence,
                "Lectra",
                7.0,
                longueur,
                1,
                material,
                "Waiting",
                "Waiting",
                LocalDate.of(2026, 5, 22),
                "1",
                null,
                null,
                false,
                null,
                null,
                null,
                null);
    }

    private static ScheduleBuilderService.SerieInput lifecycleInput(
            String serie,
            String sequence,
            String statusCoupe,
            String statusMatelassage,
            String tableMatelassage,
            String tableCoupe,
            LocalDateTime matStart,
            LocalDateTime matEnd,
            LocalDateTime coupeStart,
            LocalDateTime coupeEnd) {
        return new ScheduleBuilderService.SerieInput(
                serie,
                sequence,
                "Lectra",
                7.0,
                0.4,
                1,
                "MAT-" + serie,
                statusCoupe,
                statusMatelassage,
                LocalDate.of(2026, 5, 22),
                "1",
                tableMatelassage,
                tableCoupe,
                true,
                matStart,
                matEnd,
                coupeStart,
                coupeEnd);
    }

    private static Map<String, Map<String, Set<String>>> machineMap(
            String zone, String machineType, String machine) {
        Map<String, Map<String, Set<String>>> machinesByZoneByType = new HashMap<>();
        Map<String, Set<String>> byType = new HashMap<>();
        byType.put(machineType, new LinkedHashSet<>(Arrays.asList(machine)));
        machinesByZoneByType.put(zone, byType);
        return machinesByZoneByType;
    }
}
