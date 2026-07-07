package com.lear.MGCMS.repositories.CuttingPlan;

import java.time.LocalDateTime;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacement;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementId;

public interface CuttingPlanMaterialPlacementRepository extends CrudRepository<CuttingPlanMaterialPlacement, CuttingPlanMaterialPlacementId> {

	@Modifying
	@Transactional
	@Query(value = "DELETE from CuttingPlanMaterialPlacement where cuttingPlanMaterial_cuttingPlan_id = ?1", nativeQuery = true)
	void deleteByCuttingPlanId(Long id);
	
	@Modifying
	@Transactional
	@Query(value = "Update CuttingPlanMaterialPlacement "
			+ "SET maxPlie = :maxPlie "
			+ ", maxPlieDrill = :maxPlieDrill "
			+ ", maxDrill = :maxDrill "
			+ ", config = :config "
			+ ", pliesConfig = :pliesConfig "
			+ ", pliesConfigMarge = :pliesConfigMarge "
			+ "where cuttingPlanMaterial_partNumberMaterial = :partNumberMaterial" +
			" and machine = :machine and longueur >= :longueurMin  and longueur <= :longueurMax" +
			" and nbrCouche >= :minNbrCouche and nbrCouche < :maxNbrCouche"
			, nativeQuery = true)
	void update(String partNumberMaterial, String machine ,Double longueurMin , Double longueurMax, Integer minNbrCouche, Integer maxNbrCouche,
				Integer maxPlie, Integer maxPlieDrill, Integer maxDrill, String config, String pliesConfig, String pliesConfigMarge);

	/**
	 * Lightweight query: returns only the 5 fields needed for cutting-time calculation
	 * (planId, planDescription, partNumbers, perimetre, tempsDeCoupe) for active placements
	 * in a given project, without loading the full CuttingPlan object graph.
	 *
	 * Row structure: [0]=planId, [1]=planDescription, [2]=partNumbers, [3]=perimetre, [4]=tempsDeCoupe
	 */
	@Query(value =
			"SELECT cp.id, cp.description, cpmp.partNumbers, cpmp.perimetre, cpmp.tempsDeCoupe " +
			"FROM CuttingPlanMaterialPlacement cpmp " +
			"JOIN CuttingPlanMaterial cpm " +
			"  ON cpmp.cuttingPlanMaterial_partNumberMaterial = cpm.partNumberMaterial " +
			"  AND cpmp.cuttingPlanMaterial_cuttingPlan_id = cpm.cuttingPlan_id " +
			"JOIN CuttingPlan cp ON cpm.cuttingPlan_id = cp.id " +
			"WHERE cp.projet = :projet " +
			"AND (cp.enabled = 1 OR " +
			"     ((cp.startDate IS NULL OR cp.startDate <= :currentTime) " +
			"      AND (cp.endDate IS NULL OR cp.endDate >= :currentTime))) " +
			"AND cpmp.activated = 1 " +
			"AND cpmp.partNumbers IS NOT NULL " +
			"AND cpmp.perimetre IS NOT NULL",
			nativeQuery = true)
	List<Object[]> findActiveByProjet(@Param("projet") String projet, @Param("currentTime") LocalDateTime currentTime);

}

