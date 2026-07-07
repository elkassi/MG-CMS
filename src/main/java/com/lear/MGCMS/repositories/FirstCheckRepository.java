package com.lear.MGCMS.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.FirstCheck;

public interface FirstCheckRepository extends JpaRepository<FirstCheck, Long>, JpaSpecificationExecutor<FirstCheck> {

	@Query("from FirstCheck where date = :date and shift = :shift and machine = :machine")
	List<FirstCheck> findList(LocalDate date, String shift, String machine);

	@Query("from FirstCheck where createdAt >= :date1 and createdAt <= :date2 and decision = 'NOK'")
    List<FirstCheck> getNokBetween(LocalDateTime date1, LocalDateTime date2);

	@Query("from FirstCheck where date = :date and shift = :shift and category in (:categories)")
    List<FirstCheck> getList(LocalDate date, List<String> categories, String shift);
}
