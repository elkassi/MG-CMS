package com.lear.MGCMS.services.cms;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.cms.domain.CategoryLaizePlanCoupe;
import com.lear.cms.domain.IntervalItemMachinePlanCoupe;
import com.lear.cms.domain.IntervalSeuilPlanCoupe;
import com.lear.cms.domain.ItemMachinePlanCoupe;
import com.lear.cms.domain.ItemPlanCoupe;
import com.lear.cms.domain.SeuilLongueurPlanCoupe;
import com.lear.cms.repositories.CategoryLaizePlanCoupeRepository;
import com.lear.cms.repositories.IntervalItemMachinePlanCoupeRepository;
import com.lear.cms.repositories.IntervalSeuilPlanCoupeRepository;
import com.lear.cms.repositories.ItemMachinePlanCoupeRepository;
import com.lear.cms.repositories.ItemPlanCoupeRepository;
import com.lear.cms.repositories.SeuilLongueurPlanCoupeRepository;
import com.lear.MGCMS.domain.PartNumberMaterialConfig;
import com.lear.MGCMS.domain.ReftissuCategory;
import com.lear.MGCMS.domain.ReftissuMachine;
import com.lear.MGCMS.domain.ReftissuMargin;

@Service
public class ItemPlanCoupeService {
	
	@Autowired
	private ItemPlanCoupeRepository repo;
	
	@Autowired
	private CategoryLaizePlanCoupeRepository categoryRepository;
	
	@Autowired
	private SeuilLongueurPlanCoupeRepository seuilRepository;
	
	@Autowired
	private IntervalSeuilPlanCoupeRepository intervalSeuilPlanCoupeRepository;
	
	@Autowired
	private ItemMachinePlanCoupeRepository itemMachinePlanCoupeRepository;
	
	@Autowired
	private IntervalItemMachinePlanCoupeRepository intervalItemMachinePlanCoupeRepository;
	
	public List<ItemPlanCoupe> findAll() {
		return (List<ItemPlanCoupe>) repo.findAll();
	}
	
	public ItemPlanCoupe findByIdItemPlan(Integer id) {
		return repo.findByIdItemPlan(id);
	}
	
	public List<CategoryLaizePlanCoupe> findCategories(Integer id) {
		return categoryRepository.findByIdItemForeignPlan(id);
	}
	
	public List<SeuilLongueurPlanCoupe> findByIdItemForeign1Plan(Integer idItemForeign1Plan) {
		return seuilRepository.findByIdItemForeign1Plan(idItemForeign1Plan);
	}
	
	public List<IntervalSeuilPlanCoupe> findByIdSeuilForeignPlan(Integer idSeuilForeignPlan) {
		return intervalSeuilPlanCoupeRepository.findByIdSeuilForeignPlan(idSeuilForeignPlan);
	}
	
	public List<ItemMachinePlanCoupe> findByIdItemForeignPlan(Integer idItemForeignPlan) {
		return itemMachinePlanCoupeRepository.findByIdItemForeignPlan(idItemForeignPlan);
	}
	
	public List<IntervalItemMachinePlanCoupe> findByIdItemMachineForeignPlan(Integer idItemMachineForeignPlan) {
		return intervalItemMachinePlanCoupeRepository.findByIdItemMachineForeignPlan(idItemMachineForeignPlan);
	}


    public List<ItemPlanCoupe> findByItemNumberPlanIn(List<String> pns) {
		return repo.findByItemNumberPlanIn(pns);
    }

    public ItemPlanCoupe findByItemNumberPlan(String partNumberMaterial) {
		return repo.findByItemNumberPlan(partNumberMaterial);
    }

    public ItemPlanCoupe save(ItemPlanCoupe itemPlanCoupe) {
		return repo.save(itemPlanCoupe);
    }

	public Integer findMaxId() {
		Integer max = repo.findMaxId();
		if (max == null) {
			return 0;
		} else {
			return max;
		}
	}

	/**
	 * Machine type name to CMS idMachineForeignPlan mapping.
	 */
	private static final Map<String, Integer> MACHINE_TYPE_TO_CMS_ID = new HashMap<>();
	static {
		MACHINE_TYPE_TO_CMS_ID.put("Lectra", 1);
		MACHINE_TYPE_TO_CMS_ID.put("Gerber", 2);
		MACHINE_TYPE_TO_CMS_ID.put("DIE", 3);
		MACHINE_TYPE_TO_CMS_ID.put("LASER-LSR", 4);
		MACHINE_TYPE_TO_CMS_ID.put("Lectra IP6", 5);
		MACHINE_TYPE_TO_CMS_ID.put("LASER-DXF", 6);
	}

	/**
	 * Save PartNumberMaterialConfig data back to CMS database.
	 * Checks existing IDs and reuses them when possible; uses MAX+1 for new entries.
	 * Fills maxPliePlan = next interval's minPlie - 1 (or 100 if last interval).
	 * Only saves ReftissuMargin entries where machine is null (general margins).
	 * Returns the saved ItemPlanCoupe.
	 */
	@Transactional("cmsTransactionManager")
	public ItemPlanCoupe saveToCms(PartNumberMaterialConfig config, String username) {
		// Check if item already exists in CMS
		ItemPlanCoupe existing = repo.findByItemNumberPlan(config.getPartNumberMaterial());
		
		int itemId;
		// Collect existing child IDs so we can reuse them
		List<Integer> existingSeuilIds = new ArrayList<>();
		List<Integer> existingIntervalSeuilIds = new ArrayList<>();
		List<Integer> existingMachineIds = new ArrayList<>();
		List<Integer> existingIntervalMachineIds = new ArrayList<>();
		List<Integer> existingCategoryIds = new ArrayList<>();

		if (existing != null) {
			itemId = existing.getIdItemPlan();
			// Collect existing child IDs before deleting
			List<SeuilLongueurPlanCoupe> existingSeuils = seuilRepository.findByIdItemForeign1Plan(itemId);
			for (SeuilLongueurPlanCoupe seuil : existingSeuils) {
				existingSeuilIds.add(seuil.getIdSeuil_Plan());
				List<IntervalSeuilPlanCoupe> existingIntervals = intervalSeuilPlanCoupeRepository.findByIdSeuilForeignPlan(seuil.getIdSeuil_Plan());
				for (IntervalSeuilPlanCoupe interval : existingIntervals) {
					existingIntervalSeuilIds.add(interval.getIdIntervalSeuilPlan());
				}
				intervalSeuilPlanCoupeRepository.deleteByIdSeuilForeignPlan(seuil.getIdSeuil_Plan());
			}
			seuilRepository.deleteByIdItemForeign1Plan(itemId);
			
			List<ItemMachinePlanCoupe> existingMachines = itemMachinePlanCoupeRepository.findByIdItemForeignPlan(itemId);
			for (ItemMachinePlanCoupe machine : existingMachines) {
				existingMachineIds.add(machine.getIdItemMachinePlan());
				List<IntervalItemMachinePlanCoupe> existingIntervals = intervalItemMachinePlanCoupeRepository.findByIdItemMachineForeignPlan(machine.getIdItemMachinePlan());
				for (IntervalItemMachinePlanCoupe interval : existingIntervals) {
					existingIntervalMachineIds.add(interval.getIdIntervalItemMachinePlan());
				}
				intervalItemMachinePlanCoupeRepository.deleteByIdItemMachineForeignPlan(machine.getIdItemMachinePlan());
			}
			itemMachinePlanCoupeRepository.deleteByIdItemForeignPlan(itemId);
			
			List<CategoryLaizePlanCoupe> existingCategories = categoryRepository.findByIdItemForeignPlan(itemId);
			for (CategoryLaizePlanCoupe cat : existingCategories) {
				existingCategoryIds.add(cat.getIdCategoryPlan());
			}
			categoryRepository.deleteByIdItemForeignPlan(itemId);
		} else {
			itemId = findMaxId() + 1;
		}
		
		// Save/update ItemPlanCoupe
		ItemPlanCoupe item = existing != null ? existing : new ItemPlanCoupe();
		item.setIdItemPlan(itemId);
		item.setItemNumberPlan(config.getPartNumberMaterial());
		item.setDescriptionPlan(config.getDescription());
		item.setVitesseCoupePlan(config.getVitesse() != null ? String.valueOf(config.getVitesse().intValue()) : "300");
		item.setRotationPlan(config.getRotation());
		item.setPlaquePlan(config.getPlaque() != null ? String.valueOf(config.getPlaque()) : "");
		item.setTauxScrapPlan(config.getTauxScrap() != null ? String.valueOf(config.getTauxScrap()) : "3");
		item.setCommentPlan(config.getCommentaire());
		item.setUserPlan(username);
		item.setOperationDatePlan(LocalDate.now());
		item.setOperationHourPlan(LocalTime.now());
		
		// Find default machine ID
		if (config.getReftissuMachines() != null) {
			for (ReftissuMachine machine : config.getReftissuMachines()) {
				if (Boolean.TRUE.equals(machine.getDefaultValue())) {
					Integer cmsId = MACHINE_TYPE_TO_CMS_ID.get(machine.getMachineType());
					if (cmsId != null) {
						item.setIdDefaultMachinePlan(cmsId);
					}
					break;
				}
			}
		}
		
		item = repo.save(item);
		
		// Save Categories - reuse existing IDs first, then use max+1
		if (config.getReftissuCategories() != null) {
			int catIdIndex = 0;
			Integer catMaxId = categoryRepository.findMaxId();
			int nextCatId = catMaxId != null ? catMaxId + 1 : 1;
			
			for (ReftissuCategory cat : config.getReftissuCategories()) {
				CategoryLaizePlanCoupe cmsCat = new CategoryLaizePlanCoupe();
				int catId = catIdIndex < existingCategoryIds.size() ? existingCategoryIds.get(catIdIndex) : nextCatId++;
				catIdIndex++;
				cmsCat.setIdCategoryPlan(catId);
				cmsCat.setIdItemForeignPlan(itemId);
				cmsCat.setCategoryNamePlan(cat.getCategory());
				cmsCat.setDescriptionCategoryPlan(cat.getDescription());
				cmsCat.setBorneMinCategoryPlan(cat.getBorneMin());
				cmsCat.setBorneMaxCategoryPlan(cat.getBorneMax());
				cmsCat.setDefaultCategoryPlan(cat.getDefaultValue());
				categoryRepository.save(cmsCat);
			}
		}
		
		// Save Seuils (Margins) - only save ReftissuMargin where machine is null (general margins)
		if (config.getReftissuMargins() != null) {
			// Filter to only margins with machine = null
			List<ReftissuMargin> generalMargins = new ArrayList<>();
			for (ReftissuMargin margin : config.getReftissuMargins()) {
				if (margin.getMachine() == null || margin.getMachine().isEmpty()) {
					generalMargins.add(margin);
				}
			}

			int seuilIdIndex = 0;
			Integer seuilMaxId = seuilRepository.findMaxId();
			int nextSeuilId = seuilMaxId != null ? seuilMaxId + 1 : 1;
			int intervalSeuilIdIndex = 0;
			Integer intervalSeuilMaxId = intervalSeuilPlanCoupeRepository.findMaxId();
			int nextIntervalSeuilId = intervalSeuilMaxId != null ? intervalSeuilMaxId + 1 : 1;
			
			for (ReftissuMargin margin : generalMargins) {
				SeuilLongueurPlanCoupe cmsSeuil = new SeuilLongueurPlanCoupe();
				int currentSeuilId = seuilIdIndex < existingSeuilIds.size() ? existingSeuilIds.get(seuilIdIndex) : nextSeuilId++;
				seuilIdIndex++;
				cmsSeuil.setIdSeuil_Plan(currentSeuilId);
				cmsSeuil.setIdItemForeign1Plan(itemId);
				cmsSeuil.setSeuilMinPlan(margin.getLongueurMin());
				cmsSeuil.setSeuilMaxPlan(margin.getLongueurMax());
				cmsSeuil.setLongueurPlusPlan(0.0);
				cmsSeuil.setCommentSeuilPlan("");
				seuilRepository.save(cmsSeuil);
				
				// Save interval seuils with maxPliePlan computation
				if (margin.getPliesConfig() != null && !margin.getPliesConfig().isEmpty()) {
					String[] plies = margin.getPliesConfig().split("\\|");
					// Parse and sort by minPlie ascending
					List<double[]> sortedPlies = new ArrayList<>();
					for (String plie : plies) {
						String[] parts = plie.split(";");
						if (parts.length >= 2) {
							try {
								double minPlie = Double.parseDouble(parts[0]);
								double longueurPlus = Double.parseDouble(parts[1]);
								sortedPlies.add(new double[]{minPlie, longueurPlus});
							} catch (NumberFormatException e) {
								// Skip invalid entries
							}
						}
					}
					sortedPlies.sort((a, b) -> Double.compare(a[0], b[0]));

					for (int i = 0; i < sortedPlies.size(); i++) {
						IntervalSeuilPlanCoupe cmsInterval = new IntervalSeuilPlanCoupe();
						int intervalId = intervalSeuilIdIndex < existingIntervalSeuilIds.size()
								? existingIntervalSeuilIds.get(intervalSeuilIdIndex) : nextIntervalSeuilId++;
						intervalSeuilIdIndex++;
						cmsInterval.setIdIntervalSeuilPlan(intervalId);
						cmsInterval.setIdSeuilForeignPlan(currentSeuilId);
						cmsInterval.setMinPlieSeuilPlan(sortedPlies.get(i)[0]);
						cmsInterval.setLongueurPlusSeuilPlan(sortedPlies.get(i)[1]);
						// maxPliePlan = next interval's minPlie - 1, or 100 if last interval
						if (i + 1 < sortedPlies.size()) {
							cmsInterval.setMaxPlieSeuilPlan(sortedPlies.get(i + 1)[0] - 1);
						} else {
							cmsInterval.setMaxPlieSeuilPlan(100.0);
						}
						intervalSeuilPlanCoupeRepository.save(cmsInterval);
					}
				}
			}
		}
		
		// Save Machines - reuse existing IDs first, then use max+1
		if (config.getReftissuMachines() != null) {
			int machineIdIndex = 0;
			Integer machineMaxId = itemMachinePlanCoupeRepository.findMaxId();
			int nextMachineId = machineMaxId != null ? machineMaxId + 1 : 1;
			int intervalMachineIdIndex = 0;
			Integer intervalMachineMaxId = intervalItemMachinePlanCoupeRepository.findMaxId();
			int nextIntervalMachineId = intervalMachineMaxId != null ? intervalMachineMaxId + 1 : 1;
			
			for (ReftissuMachine machine : config.getReftissuMachines()) {
				ItemMachinePlanCoupe cmsMachine = new ItemMachinePlanCoupe();
				int currentMachineId = machineIdIndex < existingMachineIds.size() ? existingMachineIds.get(machineIdIndex) : nextMachineId++;
				machineIdIndex++;
				cmsMachine.setIdItemMachinePlan(currentMachineId);
				cmsMachine.setIdItemForeignPlan(itemId);
				
				Integer cmsId = MACHINE_TYPE_TO_CMS_ID.get(machine.getMachineType());
				cmsMachine.setIdMachineForeignPlan(cmsId != null ? cmsId : 1);
				cmsMachine.setMaxPlieTotalPlan(machine.getMaxPlie());
				cmsMachine.setMaxPlieDrillPlan(machine.getMaxPlieDrill());
				cmsMachine.setSeuilDrillPlan(machine.getMaxDrill() != null ? machine.getMaxDrill().doubleValue() : 0.0);
				cmsMachine.setDefaultItemMachinePlan(machine.getDefaultValue());
				cmsMachine.setRemarqueItemMachinePlan("");
				itemMachinePlanCoupeRepository.save(cmsMachine);
				
				// Save interval machines with maxPliePlan computation
				if (machine.getPliesConfig() != null && !machine.getPliesConfig().isEmpty()) {
					String[] plies = machine.getPliesConfig().split("\\|");
					// Parse and sort by minPlie ascending
					List<int[]> sortedPlies = new ArrayList<>();
					List<String> sortedConfigs = new ArrayList<>();
					for (String plie : plies) {
						String[] parts = plie.split(";");
						if (parts.length >= 2) {
							try {
								int minPlie = Integer.parseInt(parts[0]);
								sortedPlies.add(new int[]{minPlie});
								sortedConfigs.add(parts[1]);
							} catch (NumberFormatException e) {
								// Skip invalid entries
							}
						}
					}
					// Sort by minPlie ascending
					Integer[] indices = new Integer[sortedPlies.size()];
					for (int i = 0; i < indices.length; i++) indices[i] = i;
					java.util.Arrays.sort(indices, (a, b) -> Integer.compare(sortedPlies.get(a)[0], sortedPlies.get(b)[0]));

					for (int idx = 0; idx < indices.length; idx++) {
						int i = indices[idx];
						IntervalItemMachinePlanCoupe cmsInterval = new IntervalItemMachinePlanCoupe();
						int intervalId = intervalMachineIdIndex < existingIntervalMachineIds.size()
								? existingIntervalMachineIds.get(intervalMachineIdIndex) : nextIntervalMachineId++;
						intervalMachineIdIndex++;
						cmsInterval.setIdIntervalItemMachinePlan(intervalId);
						cmsInterval.setIdItemMachineForeignPlan(currentMachineId);
						cmsInterval.setMinPliePlan(sortedPlies.get(i)[0]);
						cmsInterval.setConfigurationPlan(sortedConfigs.get(i));
						cmsInterval.setMatelassageEndroitPlan(config.getMatelassageEndroit());
						// maxPliePlan = next interval's minPlie - 1, or 100 if last interval
						if (idx + 1 < indices.length) {
							int nextIdx = indices[idx + 1];
							cmsInterval.setMaxPliePlan(sortedPlies.get(nextIdx)[0] - 1);
						} else {
							cmsInterval.setMaxPliePlan(100);
						}
						intervalItemMachinePlanCoupeRepository.save(cmsInterval);
					}
				}
			}
		}
		
		return item;
	}
}
