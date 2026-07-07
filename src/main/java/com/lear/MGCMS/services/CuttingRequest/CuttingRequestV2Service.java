package com.lear.MGCMS.services.CuttingRequest;

import java.time.LocalDate;
import java.util.List;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestV2;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestV2Repository;
import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.dispatcher.ContinuousDispatchOptimizerService;
import com.lear.MGCMS.services.dispatcher.SerieStatusDateValidator;
import com.lear.MGCMS.services.dispatcher.WorkbenchCacheService;

@Service
public class CuttingRequestV2Service {

	@Autowired
	private CuttingRequestV2Repository repo;

	@Autowired(required = false)
	private SerieStatusDateValidator serieStatusDateValidator;

	@Autowired(required = false)
	private ContinuousDispatchOptimizerService optimizerService;

	@Autowired(required = false)
	private WorkbenchCacheService workbenchCacheService;

	@Autowired(required = false)
	private OrdonnancementService ordonnancementService;
	
	public CuttingRequestV2 findBySequence(String sequence) {
		return repo.findBySequence(sequence);
	}
	
	public CuttingRequestV2 save(CuttingRequestV2 obj) {
		CuttingRequestV2 saved = repo.save(obj);
		afterProductionDataChange();
		return saved;
	}

	public void deleteBySequence(String sequence) {
		repo.deleteBySequence(sequence);
		afterProductionDataChange();
	}

	public List<CuttingRequestV2> findAll(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo.findAll(date, shift);
	}

	public void deleteSeriesOther(String sequence, List<String> goodSeriesArr) {
		repo.deleteSeriesOther(sequence, goodSeriesArr);
		afterProductionDataChange();
	}

	private void afterProductionDataChange() {
		try {
			if (serieStatusDateValidator != null) {
				serieStatusDateValidator.normalizeProductionProgress();
			}
			if (ordonnancementService != null) {
				ordonnancementService.invalidateTimelineCache();
			}
			if (workbenchCacheService != null) {
				workbenchCacheService.invalidateAll();
			}
			if (optimizerService != null) {
				optimizerService.reloadActiveSnapshotFromGroundTruth();
			}
		} catch (Exception ignored) {
			// Keep CMS import/save resilient; derived production views can retry on the next poll.
		}
	}


}
