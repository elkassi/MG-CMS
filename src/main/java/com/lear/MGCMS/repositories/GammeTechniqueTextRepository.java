package com.lear.MGCMS.repositories;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.GammeTechniqueText;

public interface GammeTechniqueTextRepository extends JpaRepository<GammeTechniqueText, Long> {

	@Query("select t from GammeTechniqueText t where upper(t.partNumber) = upper(:partNumber) and (t.applyToPattern = false or t.applyToPattern is null) order by t.id")
	List<GammeTechniqueText> findExactByPartNumber(@Param("partNumber") String partNumber);

	@Query("select t from GammeTechniqueText t where (upper(t.partNumber) = upper(:partNumber) and (t.applyToPattern = false or t.applyToPattern is null)) or (t.applyToPattern = true and upper(t.pattern) in :patterns) order by t.applyToPattern, t.id")
	List<GammeTechniqueText> resolveForPartNumberAndPatterns(@Param("partNumber") String partNumber, @Param("patterns") List<String> patterns);

	@Modifying
	@Transactional
	@Query("delete from GammeTechniqueText t where upper(t.partNumber) = upper(:partNumber) and (t.applyToPattern = false or t.applyToPattern is null)")
	void deleteExactByPartNumber(@Param("partNumber") String partNumber);

	@Modifying
	@Transactional
	@Query("delete from GammeTechniqueText t where t.id in :ids")
	void deleteByIds(@Param("ids") List<Long> ids);
}
