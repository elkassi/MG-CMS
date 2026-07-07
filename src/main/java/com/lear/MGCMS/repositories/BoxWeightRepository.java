package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.BoxWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoxWeightRepository extends JpaRepository<BoxWeight, Long>, JpaSpecificationExecutor<BoxWeight> {

    // Find by boxId
    List<BoxWeight> findByBoxId(String boxId);

    // Find by boxId and not yet received (for verification)
    List<BoxWeight> findByBoxIdAndReceivedByIsNull(String boxId);

    // Find last entry by sentBy (for remove last functionality)
    Optional<BoxWeight> findTopBySentByOrderByIdDesc(String sentBy);

    // Find all sent by a specific user
    List<BoxWeight> findBySentByOrderBySentAtDesc(String sentBy);

    // Find entries sent between dates
    List<BoxWeight> findBySentAtBetween(LocalDateTime start, LocalDateTime end);

    // Find entries not yet verified
    List<BoxWeight> findByReceivedByIsNullOrderBySentAtDesc();

    // Find entries by validated status
    List<BoxWeight> findByValidatedOrderBySentAtDesc(Boolean validated);

    // Find all ordered by sentAt desc
    List<BoxWeight> findAllByOrderBySentAtDesc();
}
