package com.lear.MGCMS.services.CuttingPlan;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialRepository;

@Service
public class CuttingPlanMaterialService {

	@Autowired
	private CuttingPlanMaterialRepository repo;

	public void deleteAll(List<CuttingPlanMaterial> cuttingPlanPartMaterials) {
		repo.deleteAll(cuttingPlanPartMaterials);
	}

	public void saveAll(List<CuttingPlanMaterial> cuttingPlanMaterials) {
		// TODO Auto-generated method stub
		repo.saveAll(cuttingPlanMaterials);
	}

	public void deleteByCuttingPlanId(Long id) {
		// TODO Auto-generated method stub
		repo.deleteByCuttingPlanId(id);
	}

	public void deleteByCuttingPlanIdAndPartNumberMaterial(long id, String partNumberMaterial) {
		repo.deleteByCuttingPlanIdAndPartNumberMaterial(id, partNumberMaterial);
	}
	
}
