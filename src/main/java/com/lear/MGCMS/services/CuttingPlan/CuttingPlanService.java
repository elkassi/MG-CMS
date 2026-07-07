package com.lear.MGCMS.services.CuttingPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.ReftissuCategory;
import com.lear.MGCMS.domain.ReftissuMachine;
import com.lear.MGCMS.domain.ReftissuMargin;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanHistory;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialId;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialInfo;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacement;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementInfo;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanHistoryRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLight2Repository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLightRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialInfoRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialPlacementInfoRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRapportPlacementRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRepository;

import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Path;

@Service
public class CuttingPlanService {

	@Autowired
	private CuttingPlanRepository repo;
	
	@Autowired
	private CuttingPlanLight2Repository cuttingPlanLightRepository;
	@Autowired
	private CuttingPlanHistoryRepository cuttingPlanHistoryRepository;

	public Page<CuttingPlanLight> findAll(Long id, String projet, String version, List<String> pns,
			List<String> pnsMaterials, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		if (projet != null)
			projet += "%";
		if (version != null)
			version += "%";

		String pn1 = null, pn2 = null, pn3 = null, pn4 = null;
		if (pns != null) {
			if (pns.size() > 0 && pns.get(0) != null)
				pn1 = pns.get(0);
			if (pns.size() > 1 && pns.get(1) != null)
				pn2 = pns.get(1);
			if (pns.size() > 2 && pns.get(2) != null)
				pn3 = pns.get(2);
			if (pns.size() > 3 && pns.get(3) != null)
				pn4 = pns.get(3);
		}
		String pnsMaterial1 = null, pnsMaterial2 = null, pnsMaterial3 = null, pnsMaterial4 = null;
		if (pnsMaterials != null) {
			if (pnsMaterials.size() > 0 && pnsMaterials.get(0) != null)
				pnsMaterial1 = pnsMaterials.get(0);
			if (pnsMaterials.size() > 1 && pnsMaterials.get(1) != null)
				pnsMaterial2 = pnsMaterials.get(1);
			if (pnsMaterials.size() > 2 && pnsMaterials.get(2) != null)
				pnsMaterial3 = pnsMaterials.get(2);
			if (pnsMaterials.size() > 3 && pnsMaterials.get(3) != null)
				pnsMaterial4 = pnsMaterials.get(3);
		}
		return repo.findAll(id, projet, version, pn1, pn2, pn3, pn4, pnsMaterial1, pnsMaterial2, pnsMaterial3,
				pnsMaterial4, PageRequest.of(page, size, sortOrderIgnoreCase));
	}

	public Page<CuttingPlanLight2> findAll2(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		
        Specification<CuttingPlanLight2> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add filters based on the key-value pairs in the 'filters' map
            for (Map.Entry<String, String> entry : filters.entrySet()) {
            	System.out.println(entry.getKey() + " : "+ entry.getValue());
            	String[] strArr = entry.getKey().split("\\.");
            	
            	if(strArr.length >= 2) {
            		Path<String> path = root.get(strArr[1]);
            		for(int i = 2; i < strArr.length-1; i++) {
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
                    	System.out.println("Boolean");
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
                    
//                    if (entry.getKey().startsWith("equal.")) {
//                        predicates.add(builder.equal(path, entry.getValue()));
//                    } else if (entry.getKey().startsWith("notEqual.")) {
//                        predicates.add(builder.notEqual(path, entry.getValue()));
//                    } else if (entry.getKey().startsWith("contains.")) {
//                        predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
//                    }
                    
                    if(entry.getKey().startsWith("isNull.")) {
                        predicates.add(builder.isNull(path));
                	} else if(entry.getKey().startsWith("isNotNull.")) {
                        predicates.add(builder.isNotNull(path));
                	} 
            		
//                	if(entry.getKey().startsWith("startWith.")) {
//                        predicates.add(builder.like(path, entry.getValue()+"%"));
//                	} else if(entry.getKey().startsWith("endWith.")) {
//                        predicates.add(builder.like(path, "%"+entry.getValue()));
//                	} else if(entry.getKey().startsWith("equal.")) {
//                        predicates.add(builder.equal(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("notEqual.")) {
//                        predicates.add(builder.notEqual(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("contains.")) {
//                        predicates.add(builder.like(path, "%"+entry.getValue()+"%"));
//                	} else if(entry.getKey().startsWith("greaterThan.")) {
//                        predicates.add(builder.greaterThan(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("lessThan.")) {
//                        predicates.add(builder.lessThan(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("isNull.")) {
//                        predicates.add(builder.isNull(path));
//                	} else if(entry.getKey().startsWith("isNotNull.")) {
//                        predicates.add(builder.isNotNull(path));
//                	} 
            	}
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

		return cuttingPlanLightRepository.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
	}

	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
		if (sortDirection.equalsIgnoreCase("desc")) {
			return Sort.Direction.DESC;
		} else {
			return Sort.Direction.ASC;
		}
	}

	public CuttingPlan findByObjId(Long id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	public List<CuttingPlan> findByCmsId(Long id) {
		// TODO Auto-generated method stub
		return repo.findByCMSId(id);
	}

	public CuttingPlan save(CuttingPlan obj, User user) {
		CuttingPlan newObj = repo.save(obj);
		try {
			CuttingPlanHistory cph = new CuttingPlanHistory();
			cph.setCuttingPlan(newObj.getId());
			cph.setCreatedAt(LocalDateTime.now());
			cph.setUpdatedBy(user);
			cph.setChanges(obj.getChanges());
			cuttingPlanHistoryRepository.save(cph);
		} catch (Exception e) {
			System.out.println("CuttingPlan History ERROR : " + e.getMessage());
		}
		return newObj;
	}

	@Autowired
	private CuttingPlanRapportPlacementRepository cuttingPlanRapportPlacementRepository;

	public int deleteRapportPlacementById(Long id) {
		return cuttingPlanRapportPlacementRepository.deleteID(id);
	}

	public List<CuttingPlanLight> findAllActiveInProjets(List<String> projets) {
		return repo.findAllActiveInProjets(projets, LocalDateTime.now());
	}

	public List<CuttingPlanLight> findAllActive(LocalDateTime currentTime) {
		return repo.findAllActive(currentTime);
	}
	
	@Autowired
	private CuttingPlanMaterialInfoRepository cuttingPlanMaterialInfoRepository;
	@Autowired
	private CuttingPlanMaterialPlacementInfoRepository cuttingPlanMaterialPlacementInfoRepository;

	
	@Transactional
	public void updatePartNumberMaterial(Long cuttingPlan, String partNumberMaterial, String newPartNumber) {
		System.out.println("cuttingPlan : "+ cuttingPlan + " partNumberMaterial : " + partNumberMaterial + " => " + newPartNumber);
		CuttingPlanMaterialId idObj = new CuttingPlanMaterialId(cuttingPlan, partNumberMaterial);
		Optional<CuttingPlanMaterialInfo> oldObjOptional = cuttingPlanMaterialInfoRepository.findById(idObj);		
	    List<CuttingPlanMaterialPlacementInfo> oldArr = cuttingPlanMaterialPlacementInfoRepository.findByCuttingPlanAndCuttingPlanMaterial(cuttingPlan, partNumberMaterial);
	    
	    
	    if(oldObjOptional.isPresent()) {
		    CuttingPlanMaterialInfo newObj = new CuttingPlanMaterialInfo();
//		    List<CuttingPlanMaterialPlacementInfo> newArr = new ArrayList<CuttingPlanMaterialPlacementInfo>();
	    	CuttingPlanMaterialInfo  oldObj = oldObjOptional.get();
	    	newObj.setCuttingPlan(oldObj.getCuttingPlan());
	    	newObj.setPartNumberMaterial(newPartNumber);
	    	newObj.setDescription(oldObj.getDescription());
	    	newObj.setVitesse(oldObj.getVitesse());
	    	newObj.setRotation(oldObj.getRotation());
	    	newObj.setPlaque(oldObj.getPlaque());
	    	newObj.setTauxScrap(newObj.getTauxScrap());
	    	newObj.setMatelassageEndroit(oldObj.getMatelassageEndroit());
	    	newObj.setPartNumbers(oldObj.getPartNumbers());
	    	newObj.setQadUsage(oldObj.getQadUsage());
	    	cuttingPlanMaterialInfoRepository.save(newObj);
	    	for(CuttingPlanMaterialPlacementInfo cpmp : oldArr) {
	    		CuttingPlanMaterialPlacementInfo newCpmp = new CuttingPlanMaterialPlacementInfo();
	    		newCpmp.setCuttingPlanMaterial(newPartNumber);
	    		newCpmp.setCuttingPlan(cuttingPlan);
	    		newCpmp.setPlacement(cpmp.getPlacement());
	    		newCpmp.setPartNumbers(cpmp.getPartNumbers());
	    		newCpmp.setGroupPlacement(cpmp.getGroupPlacement());
	    		newCpmp.setActivated(cpmp.getActivated());
	    		newCpmp.setMachine(cpmp.getMachine());
	    		newCpmp.setMaxPlie(cpmp.getMaxPlie());
	    		newCpmp.setMaxDrill(cpmp.getMaxDrill());
	    		newCpmp.setMaxPlieDrill(cpmp.getMaxPlieDrill());
	    		newCpmp.setNbrCouche(cpmp.getNbrCouche());
	    		newCpmp.setConfig(cpmp.getConfig());
	    		newCpmp.setDrill(cpmp.getDrill());
	    		newCpmp.setCategory(cpmp.getCategory());
	    		newCpmp.setLaize(cpmp.getLaize());
	    		newCpmp.setLongueur(cpmp.getLongueur());
	    		newCpmp.setLongueurMatelas(cpmp.getLongueurMatelas());
	    		newCpmp.setTempsDeCoupe(cpmp.getTempsDeCoupe());
	    		newCpmp.setPliesConfig(cpmp.getPliesConfig());
	    		newCpmp.setPliesConfigMarge(cpmp.getPliesConfigMarge());
	    		cuttingPlanMaterialPlacementInfoRepository.save(newCpmp);
//	    		newArr.add(newCpmp);
	    	}
		    cuttingPlanMaterialPlacementInfoRepository.deleteAll(oldArr);
		    cuttingPlanMaterialInfoRepository.delete(oldObj);
		    
	    }

	   
	    
	}

	public void deleteByPlanCoupeId(Long id) {
		repo.deleteByPlanCoupeId(id);
	}




}
