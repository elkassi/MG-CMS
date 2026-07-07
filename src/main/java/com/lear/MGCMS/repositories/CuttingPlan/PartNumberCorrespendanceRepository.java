package com.lear.MGCMS.repositories.CuttingPlan;

import com.lear.MGCMS.domain.CuttingPlan.PartNumberCorrespendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartNumberCorrespendanceRepository extends JpaRepository<PartNumberCorrespendance, Long>, JpaSpecificationExecutor<PartNumberCorrespendance> {

    @Query("from PartNumberCorrespendance where id = :id")
    PartNumberCorrespendance findObjById(Long id);


    PartNumberCorrespendance findFirstByPartNumberAndPartNumberCorrespondance(String partNumber, String partNumberCorrespondance);

    PartNumberCorrespendance findFirstByPartNumberAndPartNumberCorrespondanceAndPlacement(String model, String pn, String placement);

    @Query("from PartNumberCorrespendance where partNumber = :partNumber and partNumberCorrespondance = :partNumberCorrespondance and (placement is null or placement = '')")
    List<PartNumberCorrespendance> findByPartNumberAndPartNumberCorrespondanceAndPlacementIsNull(String partNumber, String partNumberCorrespondance);

    @Query("from PartNumberCorrespendance where pattern in (:patterns) and "+
    "(partNumberCorrespondance is null and partNumberCorrespondance in (:partNumbers)) and "+
    "(placement is null or placement in (:placements))")
    List<PartNumberCorrespendance> findPatternToChange(List<String> patterns, List<String> partNumbers, List<String> placements);
    @Query("from PartNumberCorrespendance where pattern = :pattern and "+
            "(partNumberCorrespondance is null or partNumberCorrespondance in (:partNumbers)) and "+
            "(placement is null or placement in (:placements))")
    List<PartNumberCorrespendance> findOnePatternToChange(String pattern, List<String> partNumbers, List<String> placements);
    @Query("from PartNumberCorrespendance where partNumber = :partNumber and partNumberCorrespondance in (:partNumbers) and (placement is null or placement in (:placements))")
    List<PartNumberCorrespendance> findByPartNumberAndPartNumberCorrespondance(String partNumber, List<String> partNumbers, List<String> placements);

    List<PartNumberCorrespendance> findByPartNumberIn(List<String> partNumbers);

}
