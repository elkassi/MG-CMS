package com.lear.MGCMS.repositories.logistics;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lear.MGCMS.domain.logistics.LogisticsPicklist;

public interface LogisticsPicklistRepository extends JpaRepository<LogisticsPicklist, String> {

    /** Most recent persisted picklist for a (date, shift) — backs the logistics reprint trail. */
    LogisticsPicklist findFirstByReleaseDateAndShiftOrderByCreatedAtDesc(LocalDate releaseDate, Integer shift);
}
