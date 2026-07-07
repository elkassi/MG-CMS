package com.lear.MGCMS.repositories.CuttingPlan;

import java.time.LocalDateTime;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;

public interface CuttingPlanRepository extends JpaRepository<CuttingPlan, Long> {
	
	@Query("SELECT t from CuttingPlanLight t where (:id is null or t.id = :id) "
			+ "and (:projet is null or t.projet like :projet) "
			+ "and (:version is null or t.version like :version) "
			+ "and (:pn1 is null or exists (select cppn from CuttingPlanPartNumber cppn where cppn.cuttingPlan.id = t.id and cppn.partNumber = :pn1)) "
			+ "and (:pn2 is null or exists (select cppn from CuttingPlanPartNumber cppn where cppn.cuttingPlan.id = t.id and cppn.partNumber = :pn2)) "
			+ "and (:pn3 is null or exists (select cppn from CuttingPlanPartNumber cppn where cppn.cuttingPlan.id = t.id and cppn.partNumber = :pn3)) "
			+ "and (:pn4 is null or exists (select cppn from CuttingPlanPartNumber cppn where cppn.cuttingPlan.id = t.id and cppn.partNumber = :pn4)) "
			+ "and (:pnsMaterial1 is null or exists (select cpm from CuttingPlanMaterial cpm where cpm.cuttingPlan.id = t.id and cpm.partNumberMaterial = :pnsMaterial1)) "
			+ "and (:pnsMaterial2 is null or exists (select cpm from CuttingPlanMaterial cpm where cpm.cuttingPlan.id = t.id and cpm.partNumberMaterial = :pnsMaterial2)) "
			+ "and (:pnsMaterial3 is null or exists (select cpm from CuttingPlanMaterial cpm where cpm.cuttingPlan.id = t.id and cpm.partNumberMaterial = :pnsMaterial3)) "
			+ "and (:pnsMaterial4 is null or exists (select cpm from CuttingPlanMaterial cpm where cpm.cuttingPlan.id = t.id and cpm.partNumberMaterial = :pnsMaterial4)) "
			+ "and :of is not null ")
	Page<CuttingPlanLight> findAll(Long id, String projet, String version, String pn1,String pn2,String pn3,String pn4,String pnsMaterial1,String pnsMaterial2,String pnsMaterial3,String pnsMaterial4 , PageRequest of);
	@Query("SELECT t from CuttingPlan t where t.id = :id")
	CuttingPlan findByObjId(Long id);
	
	
	@Query("SELECT t from CuttingPlanLight t where t.projet in (:projets) and "
			+ "(t.enabled = 1 "
			+ " or ((t.startDate is null or t.startDate <= :currentTime) and (t.endDate is null or t.endDate >= :currentTime))"
			+ ")")
	List<CuttingPlanLight> findAllActiveInProjets(List<String> projets, LocalDateTime currentTime);
	
	@Query("SELECT t from CuttingPlanLight t where "
			+ "(t.enabled = 1 "
			+ " or ((t.startDate is null or t.startDate <= :currentTime) and (t.endDate is null or t.endDate >= :currentTime))"
			+ ")")
	List<CuttingPlanLight> findAllActive(LocalDateTime currentTime);

	@Modifying
	@Transactional
	@Query(value = "DELETE FROM CuttingPlanMaterialPlacement WHERE cuttingPlanMaterial_cuttingPlan_id = :id ;"
			+ "DELETE FROM CuttingPlanPartNumber WHERE cuttingPlan_id = :id ;"
			+ "DELETE FROM CuttingPlanMaterial WHERE cuttingPlan_id = :id ;"
			+ "DELETE FROM CuttingPlanRapportDrill WHERE cuttingPlan_id = :id ;" + 
			"DELETE FROM CuttingPlanRapportModel WHERE cuttingPlan_id = :id ;" + 
			"DELETE FROM CuttingPlanRapportPlacement WHERE cuttingPlan_id = :id ;"
			, nativeQuery = true)
	void deleteByPlanCoupeId(Long id);
	@Query("SELECT t from CuttingPlan t where t.cmsId = :cmsId")
	List<CuttingPlan> findByCMSId(Long cmsId);

	/**
	 * Light projection: returns [id, cmsId] pairs for a batch of plan ids.
	 * Avoids loading the whole CuttingPlan graph just to resolve cmsId.
	 */
	@Query("SELECT t.id, t.cmsId FROM CuttingPlanLight t WHERE t.id IN :ids")
	List<Object[]> findIdAndCmsIdByIdIn(List<Long> ids);

}
