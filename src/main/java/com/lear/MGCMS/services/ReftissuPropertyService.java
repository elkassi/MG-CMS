package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.ReftissuProperty;
import com.lear.MGCMS.repositories.ReftissuPropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReftissuPropertyService {

    @Autowired
    private ReftissuPropertyRepository repo;


    public Page<ReftissuProperty> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<ReftissuProperty> specification = (root, query, builder) -> {
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
                        System.out.println("Boolean");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
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

        return repo.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
    }

    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }
    public void delete(ReftissuProperty obj) {
        repo.delete(obj);
    }
    public ReftissuProperty save(ReftissuProperty obj) {
        // TODO Auto-generated method stub
        return repo.save(obj);
    }

    public List<ReftissuProperty> findAllByReftissuAndProperty(String reftissu, String property) {
        return repo.findAllByReftissuAndProperty(reftissu, property);
    }
}
