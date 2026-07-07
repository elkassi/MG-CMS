package com.lear.MGCMS.services.ctc;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.GammeTechniqueImprimerHistorique;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.repositories.GammeTechniqueImprimerHistoriqueRepository;
import com.lear.cms.domain.GammeTechniqueImprimer;
import com.lear.cms.repositories.GammeTechniqueImprimerRepository;

@Service
public class GammeTechniqueImprimerService {

	@Autowired
	private GammeTechniqueImprimerRepository repo;
	
	@Autowired
	private GammeTechniqueImprimerHistoriqueRepository gammeTechniqueImprimerHistoriqueRepository;

	public List<GammeTechniqueImprimer> findBySequence(String sequence) {
		// TODO Auto-generated method stub
		return repo.findByNSequenceImp(sequence);
	}

	public List<GammeTechniqueImprimer> findByNSerieGammeImp(String nSerieGammeImp) {
		return repo.findFirstByNSerieGammeImp(Integer.parseInt(nSerieGammeImp));
	}

	public List<GammeTechniqueImprimer> findByWO(String wo) {
		// TODO Auto-generated method stub
		return repo.findByNofImp(wo);
	}
	
	public List<String> findSequenceByWOs(List<String> woArr) {
		return repo.findSequenceByWOs(woArr);
	}

	public List<GammeTechniqueImprimer> findByWOAndSequence(String wo, String sequenceCMS) {
		// TODO Auto-generated method stub
		return repo.findByWOAndSequence(wo, sequenceCMS);
	}

	public Integer findMaxSerie() {
		// TODO Auto-generated method stub
		return repo.findMaxSerie();
	}
	
	public Long findMaxId() {
		// TODO Auto-generated method stub
		return repo.findMaxId();
	}

	public GammeTechniqueImprimer save(GammeTechniqueImprimer gti, User user) {
		try {
			gammeTechniqueImprimerHistoriqueRepository.save(new GammeTechniqueImprimerHistorique(gti.toString(),user));
		}catch (Exception e) {
			
		}
		return repo.save(gti);
	}


	public GammeTechniqueImprimer findByNSerieGammeImp(int serie) {
		// TODO Auto-generated method stub
		return repo.findByNSerieGammeImp(serie);
	}

	public List<GammeTechniqueImprimer> findAllAfter(LocalDate date) {
		// TODO Auto-generated method stub
		return repo.findAllAfter(date);
	}

	public List<GammeTechniqueImprimer> findList() {
		return (List<GammeTechniqueImprimer>) repo.findAll();
	}

    public Long getMaxId() {
		Long maxId = repo.findMaxId();
		if (maxId == null) {
			return 0L; // Return 0 if no records found
		}
		return maxId;
    }

	public String getMaxNSerieGammeImp() {
		return repo.getMaxNSerieGammeImp();
	}
	public String getMaxNSerieGammeImpLike(String sequence) {
		return repo.getMaxNSerieGammeImpLike(sequence);
	}
}
