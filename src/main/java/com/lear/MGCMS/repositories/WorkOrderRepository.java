package com.lear.MGCMS.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, String>, JpaSpecificationExecutor<WorkOrder>  {
	
	@Query("Select p from WorkOrder p where p.dueDate = :date and (:shift is null or p.shift = :shift)")
	List<WorkOrder> findList(LocalDate date, String shift);

	WorkOrder findByWo(String wo);

	@Query("from WorkOrder where dueDate <= :date2 and dueDate >= :date1")
    List<WorkOrder> findBetweenInterval(LocalDate date1, LocalDate date2);
}
