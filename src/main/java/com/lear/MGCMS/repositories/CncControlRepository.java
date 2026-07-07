package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CncControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CncControlRepository extends JpaRepository<CncControl, Long> {

    List<CncControl> findBySessionId(Long sessionId);

    // Controls performed in a time window, with their session eagerly loaded (machine is already EAGER).
    @Query("select c from CncControl c join fetch c.session s where c.createdAt between :start and :end")
    List<CncControl> findControlsWithSessionBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
