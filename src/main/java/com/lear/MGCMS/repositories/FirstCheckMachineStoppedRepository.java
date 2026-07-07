package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.FirstCheck;
import com.lear.MGCMS.domain.FirstCheckMachineStopped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface FirstCheckMachineStoppedRepository extends JpaRepository<FirstCheckMachineStopped, Long>, JpaSpecificationExecutor<FirstCheckMachineStopped> {

    @Query("from FirstCheckMachineStopped where date = :date and shift = :shift" +
            " and (:machine is null or machine = :machine) and (:category is null or category = :category)")
    List<FirstCheckMachineStopped> findList(LocalDate date, String shift, String machine, String category);




}
