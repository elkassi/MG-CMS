package com.lear.MGCMS.repositories.CuttingPlan.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementData;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementDataId;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.payload.MachineTypeSwapPlacementDto;
import com.lear.MGCMS.payload.StatsInfo;

public interface CuttingPlanMaterialPlacementDataRepository extends JpaRepository<CuttingPlanMaterialPlacementData, CuttingPlanMaterialPlacementDataId>, JpaSpecificationExecutor<CuttingPlanMaterialPlacementData> {

    @Query("FROM CuttingPlanMaterialPlacementData cpm WHERE placement = :placement")
    List<CuttingPlanMaterialPlacementData> getByPlacement(String placement);

    @Query("FROM CuttingPlanMaterialPlacementData cpm WHERE placement in (:placements)")
    List<CuttingPlanMaterialPlacementData> getByPlacements(List<String> placements);

    @Query("FROM CuttingPlanMaterialPlacementData cpm order by cpm.cuttingPlan desc")
    List<CuttingPlanMaterialPlacementData> findList();

    // return a string like placement + '/' + drill + '/' + cuttingPlan
    @Query("SELECT CONCAT(cpm.placement, '/', cpm.drill, '/', CAST(cpm.cuttingPlan AS string))" +
            " FROM CuttingPlanMaterialPlacementData cpm" +
            " WHERE machine like 'Lectra%'" +
            " ORDER BY cpm.cuttingPlan DESC")
    List<String> findVerifyDrill();

    @Query("FROM CuttingPlanMaterialPlacementData cpm WHERE placement = :placement and cuttingPlan = :idPlan ")
    CuttingPlanMaterialPlacementData getByPlacementAndPlanCoupe(String placement, Long idPlan);

    @Query("FROM CuttingPlanMaterialPlacementData cpm WHERE cuttingPlan = :id ")
    List<CuttingPlanMaterialPlacementData> findByCuttingPlan(long id);

    @Query("SELECT placement FROM CuttingPlanMaterialPlacementData WHERE cuttingPlan = :id")
    List<String> getPlacementsByCuttingPlanId(Long id);
    
    @org.springframework.data.jpa.repository.Modifying
    @javax.transaction.Transactional
    @Query("DELETE FROM CuttingPlanMaterialPlacementData WHERE cuttingPlan = :cuttingPlanId AND placement IN :placements")
    void deleteByCuttingPlanIdAndPlacementIn(Long cuttingPlanId, List<String> placements);

    @Query("FROM CuttingPlanMaterialPlacementData cpm WHERE cpm.cuttingPlan = :cuttingPlan " +
           "AND cpm.partNumberMaterial = :partNumberMaterial " +
           "AND cpm.groupPlacement = :groupPlacement " +
           "AND cpm.machine = :machine " +
           "AND cpm.activated = :activated")
    List<CuttingPlanMaterialPlacementData> findByCuttingPlanAndPartNumberMaterialAndGroupPlacementAndMachineAndActivated(
        Long cuttingPlan, String partNumberMaterial, String groupPlacement, String machine, Boolean activated);

    @Query(
        value = "SELECT DISTINCT cpm.cuttingPlan " +
                "FROM CuttingPlanMaterialPlacementData cpm, CuttingPlanData cp " +
                "WHERE cp.id = cpm.cuttingPlan " +
                "AND LOWER(cpm.partNumberMaterial) LIKE LOWER(CONCAT(CONCAT('%', :partNumberMaterial), '%')) " +
                "AND (:cuttingPlan IS NULL OR cpm.cuttingPlan = :cuttingPlan) " +
                "AND (:placement = '' OR LOWER(cpm.placement) LIKE LOWER(CONCAT(CONCAT('%', :placement), '%'))) " +
                "AND (:machine = '' OR cpm.machine = :machine) " +
                "AND (:projet = '' OR LOWER(cp.projet) LIKE LOWER(CONCAT(CONCAT('%', :projet), '%'))) " +
                "ORDER BY cpm.cuttingPlan ASC",
        countQuery = "SELECT COUNT(DISTINCT cpm.cuttingPlan) " +
                "FROM CuttingPlanMaterialPlacementData cpm, CuttingPlanData cp " +
                "WHERE cp.id = cpm.cuttingPlan " +
                "AND LOWER(cpm.partNumberMaterial) LIKE LOWER(CONCAT(CONCAT('%', :partNumberMaterial), '%')) " +
                "AND (:cuttingPlan IS NULL OR cpm.cuttingPlan = :cuttingPlan) " +
                "AND (:placement = '' OR LOWER(cpm.placement) LIKE LOWER(CONCAT(CONCAT('%', :placement), '%'))) " +
                "AND (:machine = '' OR cpm.machine = :machine) " +
                "AND (:projet = '' OR LOWER(cp.projet) LIKE LOWER(CONCAT(CONCAT('%', :projet), '%')))"
    )
    Page<Long> findMachineTypeSwapCuttingPlans(
        @Param("partNumberMaterial") String partNumberMaterial,
        @Param("placement") String placement,
        @Param("machine") String machine,
        @Param("projet") String projet,
        @Param("cuttingPlan") Long cuttingPlan,
        Pageable pageable);

    @Query("SELECT COUNT(cpm) " +
            "FROM CuttingPlanMaterialPlacementData cpm, CuttingPlanData cp " +
            "WHERE cp.id = cpm.cuttingPlan " +
            "AND LOWER(cpm.partNumberMaterial) LIKE LOWER(CONCAT(CONCAT('%', :partNumberMaterial), '%')) " +
            "AND (:cuttingPlan IS NULL OR cpm.cuttingPlan = :cuttingPlan) " +
            "AND (:placement = '' OR LOWER(cpm.placement) LIKE LOWER(CONCAT(CONCAT('%', :placement), '%'))) " +
            "AND (:machine = '' OR cpm.machine = :machine) " +
            "AND (:projet = '' OR LOWER(cp.projet) LIKE LOWER(CONCAT(CONCAT('%', :projet), '%')))")
    long countMachineTypeSwapPlacements(
        @Param("partNumberMaterial") String partNumberMaterial,
        @Param("placement") String placement,
        @Param("machine") String machine,
        @Param("projet") String projet,
        @Param("cuttingPlan") Long cuttingPlan);

    @Query("SELECT new com.lear.MGCMS.payload.MachineTypeSwapPlacementDto(" +
            "cpm.placement, cpm.cuttingPlan, cp.projet, cpm.partNumberMaterial, cpm.partNumbers, " +
            "cpm.groupPlacement, cpm.activated, cpm.machine, cpm.category, cpm.laize, cpm.longueur) " +
            "FROM CuttingPlanMaterialPlacementData cpm, CuttingPlanData cp " +
            "WHERE cp.id = cpm.cuttingPlan " +
            "AND cpm.cuttingPlan IN :cuttingPlans " +
            "AND LOWER(cpm.partNumberMaterial) LIKE LOWER(CONCAT(CONCAT('%', :partNumberMaterial), '%')) " +
            "ORDER BY cpm.cuttingPlan ASC, cpm.groupPlacement ASC, cpm.placement ASC")
    List<MachineTypeSwapPlacementDto> findMachineTypeSwapPlacementsByPlans(
        @Param("cuttingPlans") List<Long> cuttingPlans,
        @Param("partNumberMaterial") String partNumberMaterial);

//	@Query("select st from CuttingPlanMaterialPlacementData cpm "
//			+ "where cpm.cuttingPlan in (:ids) "
//			+ "and cpm.activated = true "
//			+ "GROUP BY ")
//	List<StatsInfo> findStatsByMachine(List<Long> ids);

}
