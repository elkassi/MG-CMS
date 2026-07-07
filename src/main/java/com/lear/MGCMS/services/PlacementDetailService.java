package com.lear.MGCMS.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PlacementDetail;
import com.lear.MGCMS.payload.EmpStat;
import com.lear.MGCMS.payload.Stat2Label1Value;
import com.lear.MGCMS.repositories.PlacementDetailRepository;
import com.lear.MGCMS.utils.UtilFunctions;


@Service
public class PlacementDetailService {
	
	@Autowired
	private PlacementDetailRepository repo;
	
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PlacementDetailService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

	
	@PersistenceContext
    private EntityManager entityManager;
	private static final int BATCH_SIZE = 100;
	
	public List<EmpStat> findSats() {
		return repo.findSats();
	}
	
	@Transactional
	public void saveAll(List<PlacementDetail> arrPlacementDetail) {
		try {
		    int totalFiles = arrPlacementDetail.size();
		    int totalBatches = (int) Math.ceil((double) totalFiles / BATCH_SIZE);
			for(int batchNumber = 0; batchNumber < totalBatches; batchNumber++) {
				System.out.println("arrPlacementDetail batchNumber : " + batchNumber);

		        int fromIndex = batchNumber * BATCH_SIZE;
		        int toIndex = Math.min(fromIndex + BATCH_SIZE, totalFiles);
		        List<PlacementDetail> batchFiles = arrPlacementDetail.subList(fromIndex, toIndex);
		        repo.saveAll(batchFiles);
		        entityManager.flush();
		        entityManager.clear();
			}
		} catch (Exception e) {
			System.out.println(LocalDateTime.now() +" :: " + e.getMessage());
		}
	}
	
	@Async("taskExecutor")
	public void saveAllProcess(List<PlacementDetail> arrPlacementDetail) {
		try {
	        repo.saveAll(arrPlacementDetail);
		} catch (Exception e) {
			System.out.println("arrPlacementDetail : " + LocalDateTime.now() +" " + e.getMessage());
		}
	}

	@Async("taskExecutor")
	public PlacementDetail process(PlacementDetail obj) {
		return repo.save(obj);
	}

	
	
	public PlacementDetail save(PlacementDetail obj) {
		return repo.save(obj);
	}
	
	public void delete(PlacementDetail obj) {
		repo.delete(obj);
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
		if (sortDirection.equalsIgnoreCase("desc")) {
			return Sort.Direction.DESC;
		} else {
			return Sort.Direction.ASC;
		}
	}

	public Page<PlacementDetail> findAll(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		
        Specification<PlacementDetail> specification = (root, query, builder) -> {
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

	public void saveAllLight(List<PlacementDetail> arrPlacementDetail) {
		repo.saveAll(arrPlacementDetail);
	}

	public List<Stat2Label1Value> returnEmpStat(String pattern) {
		String sql = "Select [placement] , [nomMedele], COUNT(*) , max(updatedAt) " + 
				"  from [dbo].[PlacementDetail]  " + 
				"  where [pattern] = '"+pattern+"'  " + 
				"  group by [placement] , [nomMedele] " + 
				"  order by MAX(updatedAt) desc";
		return jdbcTemplate.query(
	            sql,
	            (rs, rowNum) -> {
	            	Stat2Label1Value result = new Stat2Label1Value();
	                result.setLabel1(rs.getString(1));
	                result.setLabel2(rs.getString(2));
	                result.setValue(rs.getInt(3));
	                result.setDate(UtilFunctions.convertTimestampToLocalDateTime(rs.getTimestamp(4)));
	                return result;
	            }
	        );
	}

	

}
