package com.lear.MGCMS.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialPlacementRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialRepository;

@Service
public class PartNumberMaterialConfigService {

	@Autowired
	private PartNumberMaterialConfigRepository repo;

	@Autowired
	private ReftissuMachineRepository reftissuMachineRepository;

	@Autowired
	private ReftissuCategoryRepository reftissuCategoryRepository;

	@Autowired
	private ReftissuMarginRepository reftissuMarginRepository;

	@Autowired
	private CuttingPlanMaterialPlacementRepository cuttingPlanMaterialPlacementRepository;

	@Autowired
	private CuttingPlanMaterialRepository cuttingPlanMaterialRepository;
	
	@Autowired
	private PartNumberMaterialConfigHistoryRepository partNumberMaterialConfigHistoryRepository;

	public Page<PartNumberMaterialConfigAll> findAll(String partNumberMaterial, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		return repo.findAll(partNumberMaterial + "%", PageRequest.of(page, size, sortOrderIgnoreCase));
	}

	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
		if (sortDirection.equalsIgnoreCase("desc")) {
			return Sort.Direction.DESC;
		} else {
			return Sort.Direction.ASC;
		}
	}

	public PartNumberMaterialConfig findByObjId(String id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	@Autowired
	private PlieConfigRepository plieConfigRepository;

	public PartNumberMaterialConfig save(PartNumberMaterialConfig obj, User user) {
		// TODO Auto-generated method stub
		PartNumberMaterialConfig oldObj = repo.findByObjId(obj.getPartNumberMaterial());

		if (oldObj != null) {
			try {
				cuttingPlanMaterialRepository.update(obj.getPartNumberMaterial(), obj.getVitesse(), obj.getRotation(),
						obj.getTauxScrap().toString(), obj.getMatelassageEndroit());
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			List<ReftissuMachine> arrReftissuMachines = oldObj.getReftissuMachines();
			List<ReftissuCategory> arrReftissuCategorys = oldObj.getReftissuCategories();
			List<ReftissuMargin> arrReftissuMargins = oldObj.getReftissuMargins();

//			if (oldObj.getReftissuMachines() != null && oldObj.getReftissuMachines().size() > 0) {
//				reftissuMachineRepository.deleteAll(oldObj.getReftissuMachines());
//			}
//			if (oldObj.getReftissuCategories() != null && oldObj.getReftissuCategories().size() > 0) {
//				reftissuCategoryRepository.deleteAll(oldObj.getReftissuCategories());
//			}
//			if (oldObj.getReftissuMargins() != null && oldObj.getReftissuMargins().size() > 0) {
//				reftissuMarginRepository.deleteAll(oldObj.getReftissuMargins());
//			}
//			repo.deleteByPartNumberMaterial(obj.getPartNumberMaterial());
			for (ReftissuMachine rt : obj.getReftissuMachines()) {
				rt.setPartNumberMaterialConfig(obj);
				arrReftissuMachines.removeIf(elem -> elem.getMachineType().equals(rt.getMachineType()));
			}
			for (ReftissuCategory rc : obj.getReftissuCategories()) {
				rc.setPartNumberMaterialConfig(obj);
				arrReftissuCategorys.removeIf(elem -> elem.getCategory().equals(rc.getCategory()));
			}
			int i = 1;

			List<PlieConfig> arr = plieConfigRepository.findByPartNumberMaterial(obj.getPartNumberMaterial());

			for (ReftissuMargin rm : obj.getReftissuMargins()) {
				arrReftissuMargins.removeIf(elem -> elem.getIntervalId().equals(rm.getIntervalId()));

				for (ReftissuMachine rt : obj.getReftissuMachines()) {
						List<Integer> sections = new ArrayList<>();
						String[] pliesConfig = rt.getPliesConfig().split("\\|");
						for(int ic =0; ic < pliesConfig.length; ic++) {
							String config = "";
							if(pliesConfig[ic].split(";").length > 1) {
								config = pliesConfig[ic].split(";")[1];
							}
							Integer minPlie = Integer.parseInt(pliesConfig[ic].split(";")[0]);
							Integer maxPlie = 999;
							if(ic < pliesConfig.length - 1) {
								maxPlie = Integer.parseInt(pliesConfig[ic + 1].split(";")[0]);
							}
							if(arr.size() == 0) {
								cuttingPlanMaterialPlacementRepository.update(
										obj.getPartNumberMaterial(), rt.getMachineType(),
										rm.getLongueurMin(), rm.getLongueurMax(),
										minPlie, maxPlie,
										rt.getMaxPlie(), rt.getMaxPlieDrill(),
										rt.getMaxDrill(),
										config,
										rt.getPliesConfig(),
										rm.getPliesConfig());
							}
						}

				}
				rm.setPartNumberMaterialConfig(obj);
				rm.setIntervalId(i);
				i++;
			}
			if (!arrReftissuMachines.isEmpty()) {
				for(ReftissuMachine rt : arrReftissuMachines) {
					rt.setPartNumberMaterialConfig(obj);
				}
			    reftissuMachineRepository.deleteAll(arrReftissuMachines);
			}
			if(arrReftissuCategorys.size() > 0) reftissuCategoryRepository.deleteAll(arrReftissuCategorys);
			if(arrReftissuMargins.size() > 0) reftissuMarginRepository.deleteAll(arrReftissuMargins);
		} else {
			for (ReftissuMachine rt : obj.getReftissuMachines()) {
				rt.setPartNumberMaterialConfig(obj);
			}
			for (ReftissuCategory rc : obj.getReftissuCategories()) {
				rc.setPartNumberMaterialConfig(obj);
			}
			int i = 1;
			for (ReftissuMargin rm : obj.getReftissuMargins()) {
				rm.setPartNumberMaterialConfig(obj);
				rm.setIntervalId(i);
				i++;
			}
		}
		try {
			PartNumberMaterialConfigHistory pnmcHistory = new PartNumberMaterialConfigHistory();
			pnmcHistory.setChanges(obj.getChanges());
			pnmcHistory.setCreatedAt(LocalDateTime.now());
			pnmcHistory.setPartNumberMaterial(obj.getPartNumberMaterial());
			pnmcHistory.setUpdatedBy(user);
			partNumberMaterialConfigHistoryRepository.save(pnmcHistory);
		}catch(Exception e) {
			System.out.println("History ERROR : "+ e.getMessage());
		}
		return repo.save(obj);
	}

	public List<PartNumberMaterialConfigAll> findByPns(List<String> pns) {
		// TODO Auto-generated method stub
		return repo.findByPns(pns);
	}

	public void delete(@Valid PartNumberMaterialConfig obj) {
		if (obj != null && obj.getReftissuMachines() != null && obj.getReftissuMachines().size() > 0) {
			reftissuMachineRepository.deleteAll(obj.getReftissuMachines());
		}
		if (obj != null && obj.getReftissuCategories() != null && obj.getReftissuCategories().size() > 0) {
			reftissuCategoryRepository.deleteAll(obj.getReftissuCategories());
		}
		if (obj != null && obj.getReftissuMargins() != null && obj.getReftissuMargins().size() > 0) {
			reftissuMarginRepository.deleteAll(obj.getReftissuMargins());
		}
		repo.delete(obj);
	}

}
