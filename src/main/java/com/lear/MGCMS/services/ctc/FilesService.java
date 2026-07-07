package com.lear.MGCMS.services.ctc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;
import com.lear.ctc.domain.Files;
import com.lear.ctc.repositories.FilesRepository;


@Service
public class FilesService {

	@Autowired
	private FilesRepository repository;
	
	public Files save(Files obj) {
		return repository.save(obj);
	}
	
	public Files findById(Long id) {
		return repository.getFilesById(id);
	}
	
	public Iterable<Files> findAll() {
		return repository.findAll();
	}
	
	public void deletebyId(Long id) {
		repository.deleteById(id);
	}
	
	public Files findFirstByPattern(String pattern) {
		return repository.findFirstByPattern(pattern);
	}

	public Files findFirstByPartNumberCoverAndPanelNumber(String partNumberCover, String panelNumber) {
		// TODO Auto-generated method stub
		
		return repository.findFirstByPartNumberCoverAndPanelNumber(partNumberCover, panelNumber);
	}
	
	public Files findFirstByPartNumberCover(String partNumberCover) {
		// TODO Auto-generated method stub
		
		return repository.findFirstByPartNumberCover(partNumberCover);
	}
	
	public List<Files> findByPartNumberCover(String partNumberCover) {
		// TODO Auto-generated method stub
		return repository.findByPartNumberCover(partNumberCover);
	}
	
	public List<Files> findBySemiFinishedGoodPartNumber(String semiFinishedGoodPartNumber) {
		// TODO Auto-generated method stub
		return repository.findBySemiFinishedGoodPartNumber(semiFinishedGoodPartNumber);
	}

	public Page<Files> findAll(Map<String, String> filters,int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		
        Specification<Files> specification = (root, query, builder) -> {
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
                        if (entry.getKey().startsWith("equal.")) {
                        	if(entry.getValue().equals("True") || entry.getValue().equals("1") || entry.getValue().equals("yes")) {
                                predicates.add(builder.equal(path.as(Boolean.class), true));
                        	} else {
                                predicates.add(builder.equal(path.as(Boolean.class), false));
                        	}
                        } else if (entry.getKey().startsWith("notEqual.")) {
                        	if(entry.getValue().equals("True") || entry.getValue().equals("1") || entry.getValue().equals("yes")) {
                                predicates.add(builder.notEqual(path.as(Boolean.class), true));
                        	} else {
                                predicates.add(builder.notEqual(path.as(Boolean.class), false));
                        	}
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

		return repository.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }
	
	public void updateEcn(String partNumberCover, String ecnNumber) {
		repository.updateEcn(partNumberCover, ecnNumber);
	}

	public Files findByPartNumberCoverAndPanelnumber(String partNumberCover, String panelNumber) {
		// TODO Auto-generated method stub
		return repository.findFirstByPartNumberCoverAndPanelNumber(partNumberCover, panelNumber);
	}

	public Long findMaxID() {
		// TODO Auto-generated method stub
		return repository.findMaxID();
	}

	public Files findFirstByPartNumberCoverAndPattern(String pn, String pattern) {
		// TODO Auto-generated method stub
		return repository.findFirstByPartNumberCoverAndPattern(pn, pattern);
	}

	public List<String> findPartNumbersList() {
		// TODO Auto-generated method stub
		return repository.findPartNumbersList();
	}

	public void delete(Files obj) {
		repository.delete(obj);
	}

	public List<Files> findList(Map<String, String> filters) {
		Specification<Files> specification = (root, query, builder) -> {
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
		return repository.findAll(specification);
	}

	public void deleteAll(List<Files> arr) {
		repository.deleteAll(arr);
	}

	public void updateProjet(String pn, String projet) {
		repository.updateProjet(pn, projet);
	}

	public List<Files> findNotFound() {
		return repository.findNotFound();
	}

    public List<String> getPattern(List<String> partNumberCoverArray) {
        return repository.getPattern(partNumberCoverArray);
    }


    public List<Files> getFilesPattern(List<String> partNumberCoverArray) {
            return repository.getFilesPattern(partNumberCoverArray);
    }

    public List<String> findPatternByProjetAndType(String projet, String type) {
    return repository.findPatternByProjetAndType(projet, type );
    }
    public List<String> findPatternByProjetAndTypeAsLaminated(String projet, String type) {
        return repository.findPatternByProjetAndTypeAsLaminated(projet, type );
    }

    public List<String> findPatternByProjetAndTypeAsNotLaminated(String projet, String type) {
        return repository.findPatternByProjetAndTypeAsNotLaminated(projet, type );
    }


    public void updatePatternByProjetAndType(String projet, String type, String pattern, Double min1, Double max1) {
        repository.updatePatternByProjetAndType(projet, type, pattern, min1, max1);
    }
    public void updatePatternByProjetAndTypeAsLaminated(String projet, String type, String pattern, Double min1, Double max1) {
        repository.updatePatternByProjetAndTypeAsLaminated(projet, type, pattern, min1, max1);
    }
    public void updatePatternByProjetAndTypeAsNotLaminated(String projet, String type, String pattern, Double min1, Double max1) {
        repository.updatePatternByProjetAndTypeAsNotLaminated(projet, type, pattern, min1, max1);
    }

    public void updatePatternByProjetAndType2(String projet, String type, String pattern, Double min1, Double max1) {
        repository.updatePatternByProjetAndType2(projet, type, pattern, min1, max1);
    }
    public void updatePatternByProjetAndTypeAsLaminated2(String projet, String type, String pattern, Double min1, Double max1) {
        repository.updatePatternByProjetAndTypeAsLaminated2(projet, type, pattern, min1, max1);
    }
    public void updatePatternByProjetAndTypeAsNotLaminated2(String projet, String type, String pattern, Double min1, Double max1) {
        repository.updatePatternByProjetAndTypeAsNotLaminated2(projet, type, pattern, min1, max1);
    }
}
