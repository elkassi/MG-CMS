package com.lear.ctc.repositories;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.lear.ctc.domain.Files;

public interface FilesRepository extends JpaRepository<Files, Long>, JpaSpecificationExecutor<Files> {
	
	Files getFilesById(Long id);
		
	void deleteById(Long id);

	Files findFirstByPartNumberCoverAndPanelNumber(String partNumberCover, String panelNumber);
			
	Files findFirstByPartNumberCover(String partNumberCover);
//	@Query("SELECT t from Files t where (:partNumberCover is null or t.partNumberCover like :partNumberCover) ")
	List<Files> findByPartNumberCover(String partNumberCover);
	List<Files> findBySemiFinishedGoodPartNumber(String semiFinishedGoodPartNumber);
	Files findFirstByPattern(String pattern);

	@Query("FROM Files WHERE panelNumber LIKE CONCAT('%', :suffix) AND partNumberCover = :partNumberCover")
	List<Files> findByPanelNumberEndingWithAndPartNumberCover(String suffix, String partNumberCover);

	@Query("FROM Files WHERE type = :type AND partNumberCover = :partNumberCover")
	List<Files> findByTypeAndPartNumberCover(String type, String partNumberCover);

	@Query(value = "SELECT max(id) from Files", nativeQuery = true)
	Long findMaxID();

	Files findFirstByPartNumberCoverAndPattern(String pn, String pattern);
	
	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " + 
			"   SET ecn_number = :ecnNumber " + 
			" WHERE part_number_cover = :partNumberCover", nativeQuery = true)
	void updateEcn(String partNumberCover, String ecnNumber);

	@Query("Select partNumberCover from Files group by partNumberCover")
	List<String> findPartNumbersList();

	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " + 
			"   SET projet = :projet " + 
			" WHERE part_number_cover = :partNumberCover", nativeQuery = true)
	void updateProjet(String partNumberCover, String projet);
	@Query("from Files where pltFound is null or pltFound = false")
	List<Files> findNotFound();

	/*
	  select part_number_material + ' ; '+ pattern
  FROM [dbo].[files] where part_number_cover in ('L001790122PVJAM' , 'L001790123PVJAM' , 'L002193363PVJAH', 'L002193364PVJAH')
  group by part_number_material, pattern
	 */
	@Query(value = "select UPPER(TRIM(part_number_material)) + ' : ' + UPPER(TRIM(pattern)) FROM files " +
			"where part_number_cover in (:partNumberCoverArray) and part_number_material is not null and pattern is not null " +
			"group by part_number_material, pattern", nativeQuery = true)
    List<String> getPattern(List<String> partNumberCoverArray);

	@Query("FROM Files where partNumberCover in (:partNumberCoverArray) and partNumberMaterial is not null and pattern is not null")
	List<Files> getFilesPattern(List<String> partNumberCoverArray);

	@Query("select pattern from Files where projet = :projet and type = :type group by pattern")
	List<String> findPatternByProjetAndType(String projet, String type);

	@Query("select pattern from Files where projet = :projet and type = :type and partNumberMaterial not like '%Leather SIN Hacoflex%' group by pattern")
	List<String> findPatternByProjetAndTypeAsLaminated(String projet, String type);

	@Query("select pattern from Files where projet = :projet and type = :type and partNumberMaterial like '%Leather SIN Hacoflex%' group by pattern")
	List<String> findPatternByProjetAndTypeAsNotLaminated(String projet, String type);

	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " +
			"   SET tol_min1 = :min1, tol_max1= :max1 " +
			" WHERE projet = :projet and type = :type and pattern = :pattern", nativeQuery = true)
	void updatePatternByProjetAndType(String projet, String type, String pattern,
									  Double min1, Double max1);

	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " +
			"   SET tol_min1 = :min1, tol_max1= :max1 " +
			" WHERE projet = :projet and type = :type and pattern = :pattern and part_number_material not like '%Leather SIN Hacoflex%'", nativeQuery = true)
	void updatePatternByProjetAndTypeAsLaminated(String projet, String type, String pattern,
												 Double min1, Double max1);

 	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " +
			"   SET tol_min1 = :min1, tol_max1= :max1 " +
			" WHERE projet = :projet and type = :type and pattern = :pattern and part_number_material like '%Leather SIN Hacoflex%'", nativeQuery = true)
	void updatePatternByProjetAndTypeAsNotLaminated(String projet, String type, String pattern,
													Double min1, Double max1);


	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " +
			"   SET tol_min2 = :min2, tol_max2= :max2 " +
			" WHERE projet = :projet and type = :type and pattern = :pattern", nativeQuery = true)
	void updatePatternByProjetAndType2(String projet, String type, String pattern,
									  Double min2, Double max2);

	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " +
			"   SET tol_min2 = :min2, tol_max2= :max2 " +
			" WHERE projet = :projet and type = :type and pattern = :pattern and part_number_material not like '%Leather SIN Hacoflex%'", nativeQuery = true)
	void updatePatternByProjetAndTypeAsLaminated2(String projet, String type, String pattern,
												 Double min2, Double max2);

	@Modifying
	@Transactional
	@Query(value = "UPDATE Files " +
			"   SET tol_min2 = :min2, tol_max2= :max2 " +
			" WHERE projet = :projet and type = :type and pattern = :pattern and part_number_material like '%Leather SIN Hacoflex%'", nativeQuery = true)
	void updatePatternByProjetAndTypeAsNotLaminated2(String projet, String type, String pattern,
													Double min2, Double max2);


}

