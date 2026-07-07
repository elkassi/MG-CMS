package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.Consumable;
import com.lear.MGCMS.payload.ConsumableStat;
import com.lear.MGCMS.repositories.ConsumableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConsumableService {

    @Autowired
    private ConsumableRepository repo;

    @Autowired
    private ApplicationContext context;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ConsumableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

//    public Page<Consumable> findAll(String id, int page, int size, String sort) {
//        String[] sortArr = sort.split(",");
//        String evalSort = sortArr[0];
//        String sortDirection = sortArr[1];
//        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
//        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
//
//        return repo.findByFilter(id+"%", PageRequest.of(page, size, sortOrderIgnoreCase));
//    }

    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

    public Consumable findByObjId(String id) {
        // TODO Auto-generated method stub
        return repo.findByObjId(id);
    }

    public Consumable save(Consumable obj) {
        // TODO Auto-generated method stub
        return repo.save(obj);
    }


    public void delete(Consumable oldObj) {
        repo.delete(oldObj);
    }


    public Page<Consumable> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<Consumable> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add filters based on the key-value pairs in the 'filters' map
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String[] strArr = entry.getKey().split("\\.");


                if (strArr.length >= 2) {
                    Path<String> path = root.get(strArr[1]);
                    for (int i = 2; i <= strArr.length - 1; i++) {
                        path = path.get(strArr[i]);
                    }

                    // Handle different data types
                    if (path.getJavaType().equals(String.class)) {
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
                            predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(LocalDate.class)) {
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

                    if (entry.getKey().startsWith("isNull.")) {
                        predicates.add(builder.isNull(path));
                    } else if (entry.getKey().startsWith("isNotNull.")) {
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


    public List<ConsumableStat> getStat(String type) {
        String sql = "select machine, YEAR(date), DATEPART(week, date), count(*), " +
                "   ROUND(AVG(count), 2) AS avg_count, " +
                "    ROUND(AVG(value), 2) AS avg_value, " +
                "    ROUND(AVG(value1), 2) AS avg_value1, " +
                "    ROUND(AVG(value2), 2) AS avg_value2, " +
                "    ROUND(AVG(value3), 2) AS avg_value3, " +
                "    ROUND(AVG(value4), 2) AS avg_value4 " +
                " from consumable" +
                " where type = ? AND date >= DATEADD(month, -6, GETDATE())  " +
                " group by machine, YEAR(date), DATEPART(week, date)" +
                " order by machine, YEAR(date), DATEPART(week, date)";
        return jdbcTemplate.query(sql, new Object[]{type}, (rs, rowNum) -> {
            ConsumableStat stat = new ConsumableStat();
            stat.setMachine(rs.getString(1));
            stat.setYear(rs.getInt(2));
            stat.setWeekNumber(rs.getInt(3));
            stat.setTotal(rs.getInt(4));
            stat.setCount(rs.getDouble(5));
            stat.setValue(rs.getDouble(6));
            stat.setValue1(rs.getDouble(7));
            stat.setValue2(rs.getDouble(8));
            stat.setValue3(rs.getDouble(9));
            stat.setValue4(rs.getDouble(10));
            return stat;
        });

    }

}
