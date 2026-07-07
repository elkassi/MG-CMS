package com.lear.MGCMS.services.CuttingPlan.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementData;
import com.lear.MGCMS.payload.MachineTypeSwapPlacementDto;
import com.lear.MGCMS.payload.MachineTypeSwapPlacementSearchResponse;
import com.lear.MGCMS.repositories.CuttingPlan.data.CuttingPlanMaterialPlacementDataRepository;

@Service
public class CuttingPlanMaterialPlacementDataService {

	@Autowired
	private CuttingPlanMaterialPlacementDataRepository repo;

    // save
    public CuttingPlanMaterialPlacementData save(CuttingPlanMaterialPlacementData cuttingPlanMaterialPlacementData) {
        return repo.save(cuttingPlanMaterialPlacementData);
    }
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
		if (sortDirection.equalsIgnoreCase("desc")) {
			return Sort.Direction.DESC;
		} else {
			return Sort.Direction.ASC;
		}
	}

	public Page<CuttingPlanMaterialPlacementData> findAll(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		
        Specification<CuttingPlanMaterialPlacementData> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add filters based on the key-value pairs in the 'filters' map
            for (Map.Entry<String, String> entry : filters.entrySet()) {
            	System.out.println(entry.getKey() + " : "+ entry.getValue());
            	String[] strArr = entry.getKey().split("\\.");
            	
            	if(strArr.length >= 2) {
            		Path<String> path = root.get(strArr[1]);
            		for(int i = 2; i < strArr.length; i++) {
            			path = path.get(strArr[i]);
            		}
            		
            		// Handle different data types
                    if (path.getJavaType().equals(String.class)) {
                    	System.out.println("String");
                        if (entry.getKey().startsWith("startWith.")) {
                            predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("endWith.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue()));
                        } else if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("contains.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
                        }
                    } else if (path.getJavaType().equals(Boolean.class)) {
                        String valueEntry = entry.getValue();
                        if(valueEntry.equalsIgnoreCase("1")) {
                            valueEntry = "TRUE";
                        }
                        if(valueEntry.equalsIgnoreCase("0")) {
                            valueEntry = "FALSE";
                        }
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
                        }
                    } else if (path.getJavaType().equals(LocalDate.class)) {
                    	System.out.println("LocalDate");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } 
//                        else if (entry.getKey().startsWith("startWith.")) {
//                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS".substring(0, entry.getValue().length())));
//                            Integer lg = entry.getValue().length() ;
//                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
//                            switch(lg) {
//                            	case 1 : 
//                            		endDateTime = startDateTime.plusYears(1000).minusNanos(1);break;
//                            	case 2 :
//                            		endDateTime = startDateTime.plusYears(100).minusNanos(1);break;
//                            	case 3:
//                            		endDateTime = startDateTime.plusYears(10).minusNanos(1);break;
//                            		
//                            }
//                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
//                        }
                    } else if (path.getJavaType().equals(LocalDateTime.class)) {
                    	System.out.println("LocalDateTime");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("startWith.")) {
                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue());
                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
                        }
                    } else if (path.getJavaType().equals(Integer.class)) {
                        // Handle Integer conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } 
                    } else if (path.getJavaType().equals(Double.class)) {
                        // Handle Double conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Long.class)) {
                        // Handle Long conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        }
                    }
                                        
                    if(entry.getKey().startsWith("isNull.")) {
                        predicates.add(builder.isNull(path));
                	} else if(entry.getKey().startsWith("isNotNull.")) {
                        predicates.add(builder.isNotNull(path));
                	} 
            		
            	}
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

		return repo.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
	}

    public MachineTypeSwapPlacementSearchResponse findMachineTypeSwapPlacements(
            String partNumberMaterial,
            String placement,
            String machine,
            String projet,
            Long cuttingPlan,
            int page,
            int size) {
        String materialFilter = normalizeFilter(partNumberMaterial);
        if (materialFilter == null) {
            throw new IllegalArgumentException("partNumberMaterial is required");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        Page<Long> planPage = repo.findMachineTypeSwapCuttingPlans(
            materialFilter,
            normalizeOptionalFilter(placement),
            normalizeOptionalFilter(machine),
            normalizeOptionalFilter(projet),
            cuttingPlan,
            PageRequest.of(safePage, safeSize)
        );

        List<Long> planIds = planPage.getContent();
        long totalPlacements = repo.countMachineTypeSwapPlacements(
            materialFilter,
            normalizeOptionalFilter(placement),
            normalizeOptionalFilter(machine),
            normalizeOptionalFilter(projet),
            cuttingPlan
        );

        if (planIds.isEmpty()) {
            return new MachineTypeSwapPlacementSearchResponse(
                Collections.emptyList(),
                safePage,
                safeSize,
                planPage.getTotalPages(),
                planPage.getTotalElements(),
                totalPlacements,
                planPage.isFirst(),
                planPage.isLast()
            );
        }

        List<MachineTypeSwapPlacementDto> placements =
            repo.findMachineTypeSwapPlacementsByPlans(planIds, materialFilter);
        placements.sort(Comparator
            .comparing(MachineTypeSwapPlacementDto::getCuttingPlan, Comparator.nullsLast(Long::compareTo))
            .thenComparing(dto -> dto.getGroupPlacement() == null ? Integer.MAX_VALUE : dto.getGroupPlacement())
            .thenComparing(dto -> Boolean.TRUE.equals(dto.getActivated()) ? 0 : 1)
            .thenComparing(dto -> dto.getPlacement() == null ? "" : dto.getPlacement()));

        return new MachineTypeSwapPlacementSearchResponse(
            placements,
            planPage.getNumber(),
            planPage.getSize(),
            planPage.getTotalPages(),
            planPage.getTotalElements(),
            totalPlacements,
            planPage.isFirst(),
            planPage.isLast()
        );
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOptionalFilter(String value) {
        String normalized = normalizeFilter(value);
        return normalized == null ? "" : normalized;
    }

    public List<CuttingPlanMaterialPlacementData> getByPlacement(String placement) {
            return repo.getByPlacement(placement);
    }
    public List<CuttingPlanMaterialPlacementData> getByPlacements(List<String> placements) {
        return repo.getByPlacements(placements);
    }

    public List<CuttingPlanMaterialPlacementData> findList() {
        return repo.findList();
    }

    public List<String> findVerifyDrill() {
        return repo.findVerifyDrill();
    }

    public CuttingPlanMaterialPlacementData getByPlacementAndPlanCoupe(String placement, Long idPlan) {
        return repo.getByPlacementAndPlanCoupe(placement, idPlan);
    }

    public void delete(CuttingPlanMaterialPlacementData cpmp) {
        repo.delete(cpmp);
    }

    public List<CuttingPlanMaterialPlacementData> findByCuttingPlan(long id) {
        return repo.findByCuttingPlan(id);
    }

    public List<String> getPlacementsByCuttingPlanId(Long id) {
        return repo.getPlacementsByCuttingPlanId(id);
    }
    
    public void deleteByCuttingPlanIdAndPlacementIn(Long cuttingPlanId, List<String> placements) {
        if (placements != null && !placements.isEmpty()) {
            repo.deleteByCuttingPlanIdAndPlacementIn(cuttingPlanId, placements);
        }
    }

    public CuttingPlanMaterialPlacementData updateMachine(Long cuttingPlan, String placement, String partNumberMaterial, String newMachine) {
        CuttingPlanMaterialPlacementData data = repo.getByPlacementAndPlanCoupe(placement, cuttingPlan);
        if (data == null) {
            throw new RuntimeException("Placement not found: " + placement + " for cutting plan: " + cuttingPlan);
        }
        data.setMachine(newMachine);
        return repo.save(data);
    }

    public CuttingPlanMaterialPlacementData toggleActivation(Long cuttingPlan, String placement) {
        CuttingPlanMaterialPlacementData data = repo.getByPlacementAndPlanCoupe(placement, cuttingPlan);
        if (data == null) {
            throw new RuntimeException("Placement not found: " + placement + " for cutting plan: " + cuttingPlan);
        }
        data.setActivated(!data.getActivated());
        return repo.save(data);
    }

    public int bulkActivateByMachine(Long cuttingPlan, String partNumberMaterial, String groupPlacement, String machine) {
        List<CuttingPlanMaterialPlacementData> items = repo.findByCuttingPlanAndPartNumberMaterialAndGroupPlacementAndMachineAndActivated(
            cuttingPlan, partNumberMaterial, groupPlacement, machine, false
        );
        
        for (CuttingPlanMaterialPlacementData item : items) {
            item.setActivated(true);
        }
        
        repo.saveAll(items);
        return items.size();
    }

//	public List<StatsInfo> findStatsByMachine(List<Long> ids) {
//		// TODO Auto-generated method stub
//		return repo.findStatsByMachine(ids);
//	}
	
}
