package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MachineQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MachineQueueRepository extends JpaRepository<MachineQueue, Long> {

    List<MachineQueue> findByMachineNomOrderByQueuePosition(String machineNom);

    @Query("SELECT mq FROM MachineQueue mq ORDER BY mq.machineNom, mq.queuePosition")
    List<MachineQueue> findAllOrdered();

    @Modifying
    @Query("DELETE FROM MachineQueue mq WHERE mq.machineNom = :machineNom")
    void deleteByMachineNom(String machineNom);

    @Modifying
    @Query("DELETE FROM MachineQueue mq")
    void deleteAllQueues();

    // ===== Phase 7 version tracking =====

    /**
     * Atomically bump every row's {@code version} for the named machine.
     * Called at the end of every {@code OrdonnancementService.saveQueues}
     * commit (Phase 7) so the kiosk's {@code /api/kiosk/version} endpoint
     * can serve a cheap monotonic counter.
     */
    @Modifying
    @Transactional
    @Query("UPDATE MachineQueue mq SET mq.version = COALESCE(mq.version, 0) + 1 "
         + "WHERE mq.machineNom = :machineNom")
    int bumpVersionForMachine(String machineNom);

    /**
     * Highest current version across any row for the machine. Zero when the
     * queue is empty — the kiosk treats that as "nothing to do".
     */
    @Query("SELECT MAX(mq.version) FROM MachineQueue mq WHERE mq.machineNom = :machineNom")
    Long maxVersionForMachine(String machineNom);
}
