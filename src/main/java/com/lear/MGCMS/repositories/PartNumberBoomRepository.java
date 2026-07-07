package com.lear.MGCMS.repositories;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.PartNumberBoomId;
import com.lear.MGCMS.domain.Placement;
import com.lear.MGCMS.domain.PlacementId;

public interface PartNumberBoomRepository extends JpaRepository<PartNumberBoom, PartNumberBoomId>, JpaSpecificationExecutor<PartNumberBoom> {

//	@Query("SELECT t from PartNumberBoom t where "
//			+ " (:partNumber is null or t.partNumber like :partNumber) "
//			+ "and (:partNumberMaterial is null or t.partNumberMaterial like :partNumberMaterial) "
//			+ "and (:project is null or t.project like :project) "
//			+ "and (:version is null or t.version like :version) "
//			+ "and (:item is null or t.item like :item) "
//			+ "and :of is not null"
//			)
//	Page<PartNumberBoom> findAll(String partNumber, String partNumberMaterial, String project, String version, String item, PageRequest of);
	@Query("SELECT t from PartNumberBoom t where t.partNumber = :partNumber and t.partNumberMaterial = :partNumberMaterial")
	PartNumberBoom findByObjId(String partNumber, String partNumberMaterial);
	
	@Query(value = "SELECT TOP 100 * from PartNumberBoom as t where "
			+ " (:partNumber is null or t.partNumber like :partNumber) "
			+ "and (:project is null or t.project is null or t.project = :project) "
			+ "and (:version is null or t.version is null or t.version = :version) "
			, nativeQuery = true)
	List<PartNumberBoom> findList(String project, String version, String partNumber);
	
	PartNumberBoom findFirstByPartNumberMaterial(String pnMaterial);
	
	List<PartNumberBoom> findByPartNumberMaterial(String partNumber);
	List<PartNumberBoom> findByPartNumber(String partNumber);

	@Modifying
	@Transactional
	@Query(value = "UPDATE PartNumberBoom " + 
			"   SET project = :project " + 
			" WHERE partNumber = :partNumber", nativeQuery = true)
	void updateProjet(String partNumber, String project);

	@Query("select t from PartNumberBoom t where t.partNumber like :partNumber")
	List<PartNumberBoom> findList2(String partNumber);

	@Query("select t from PartNumberBoom t where t.item = :item")
	List<PartNumberBoom> findByItem(String item);
}
