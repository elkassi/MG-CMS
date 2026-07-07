package com.lear.MGCMS.services.dispatcher;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.MachineQueue;
import com.lear.MGCMS.repositories.MachineQueueRepository;

/**
 * Thin service backing the Phase 8 operator-kiosk endpoints.
 *
 * <p>Two responsibilities:
 * <ul>
 *   <li>{@link #nextSerie(String)} — return the head-of-queue row on a
 *       machine as a {@link NextSerieDto}.</li>
 *   <li>{@link #currentVersion(String)} — return the max queue version
 *       on the machine, so the kiosk can cheaply detect changes.</li>
 * </ul>
 *
 * <p>The kiosk tablet typically polls {@code /version} every 2 seconds
 * and only re-fetches {@code /nextSerie} when the number moves; this
 * caps the server load even with dozens of tablets connected.</p>
 */
@Service
public class KioskService {

    @Autowired
    private MachineQueueRepository machineQueueRepository;

    @Transactional(readOnly = true)
    public Optional<NextSerieDto> nextSerie(String machineNom) {
        if (machineNom == null) return Optional.empty();
        List<MachineQueue> queue = machineQueueRepository.findByMachineNomOrderByQueuePosition(machineNom);
        if (queue.isEmpty()) return Optional.empty();
        MachineQueue head = queue.get(0);
        NextSerieDto dto = new NextSerieDto();
        dto.setSerieId(head.getSerie());
        dto.setSequenceId(head.getSequenceId());
        dto.setMachineNom(head.getMachineNom());
        dto.setPartNumberMaterial(head.getPartNumberMaterial());
        dto.setLongueur(head.getLongueur());
        dto.setEstimatedCuttingTime(head.getEstimatedCuttingTime());
        dto.setEstimatedStartTime(head.getEstimatedStartTime());
        dto.setEstimatedEndTime(head.getEstimatedEndTime());
        Long v = currentVersion(machineNom);
        dto.setQueueVersion(v == null ? 0L : v);
        return Optional.of(dto);
    }

    @Transactional(readOnly = true)
    public Long currentVersion(String machineNom) {
        if (machineNom == null) return 0L;
        Long v = machineQueueRepository.maxVersionForMachine(machineNom);
        return v == null ? 0L : v;
    }
}
