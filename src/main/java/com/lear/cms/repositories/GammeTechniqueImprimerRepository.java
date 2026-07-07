package com.lear.cms.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.GammeTechniqueImprimer;

public interface GammeTechniqueImprimerRepository extends CrudRepository<GammeTechniqueImprimer, Long> {

	@Query("select gt from GammeTechniqueImprimer gt where gt.nSerieGammeImp = :nSerieGammeImp order by idGammeImp desc")
	List<GammeTechniqueImprimer> findFirstByNSerieGammeImp(Integer nSerieGammeImp);
	
	GammeTechniqueImprimer findFirstByPartNumberImp(String partNumberImp);

	@Query("from GammeTechniqueImprimer where nSequenceImp = :sequence ")// and nbrImprissionImp = 1
	List<GammeTechniqueImprimer> findByNSequenceImp(String sequence);

	List<GammeTechniqueImprimer> findByNofImp(String wo);

	@Query("select nSequenceImp from GammeTechniqueImprimer gamme "
			+ "where nofImp in (:woArr) and "
			+ "not exists(select gamme2 from GammeTechniqueImprimer gamme2 where gamme.nSequenceImp = gamme2.nSequenceImp and gamme2.nofImp not in (:woArr))")
	List<String> findSequenceByWOs(List<String> woArr);
	
	@Query("from GammeTechniqueImprimer where nofImp = :wo and nSequenceImp = :sequence")
	List<GammeTechniqueImprimer> findByWOAndSequence(String wo, String sequence);
	
	@Query("select max(nSerieGammeImp) from GammeTechniqueImprimer")
	Integer findMaxSerie();
	@Query("select max(idGammeImp) from GammeTechniqueImprimer")
	Long findMaxId();
	@Query("from GammeTechniqueImprimer where nSerieGammeImp = :serie")
	GammeTechniqueImprimer findByNSerieGammeImp(int serie);
	@Query("from GammeTechniqueImprimer where dateRechercheImp >= :date order by nSequenceImp")
	List<GammeTechniqueImprimer> findAllAfter(LocalDate date);
	@Query("select max(nSerieGammeImp) from GammeTechniqueImprimer")
    String getMaxNSerieGammeImp();
	@Query("select  max(nSequenceImp) FROM GammeTechniqueImprimer where nSequenceImp like :sequence")
	String getMaxNSerieGammeImpLike(String sequence);

}
