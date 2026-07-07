package com.lear.MGCMS.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.sql.DataSource;

import com.lear.MGCMS.domain.PartNumberMaterialConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.repositories.PartNumberBoomRepository;

@Service
public class PartNumberBoomService {

	@Autowired
	private PartNumberBoomRepository repo;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PartNumberMaterialConfigService partNumberMaterialConfigService;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PartNumberBoomService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
		if (sortDirection.equalsIgnoreCase("desc")) {
			return Sort.Direction.DESC;
		} else {
			return Sort.Direction.ASC;
		}
	}

	public Page<PartNumberBoom> findAll(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		
        Specification<PartNumberBoom> specification = (root, query, builder) -> {
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

		return repo.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
	}

	public PartNumberBoom findByObjId(String partNumber, String partNumberMaterial) {
		// TODO Auto-generated method stub
		return repo.findByObjId(partNumber, partNumberMaterial);
	}

	public PartNumberBoom save( PartNumberBoom obj) {
		// TODO Auto-generated method stub
		return repo.save(obj);
	}

	public List<PartNumberBoom> findList(String project, String version, final String partNumber, String item) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        if(item == null) {
            return new ArrayList<>();
        }

        String sql =    "SELECT " +
                "obj1.[Item Number], " +
                "[Component], " +
                "[Item_Parent], " +
                " obj1.[Description], " +
                "[GROUP], [Version] ,[Quantity Per], Scrap, [Operation] " +
                "  FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] as obj1 " +
                "left join [logistics].[dbo].[Rapport9_4_25_BOM_QAD] as obj2 " +
                "ON  obj2.Item_Parent = obj1.[Item Number] " +
                "where Item_Parent like ? " +
                " AND exists (select * FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] as im2 " +
                " WHERE im2.[Item Number] = [Component] AND [Unit of Measure] = 'MT' AND Prod_Line = 'R100') ";
        List<PartNumberBoom> list = jdbcTemplateCMS.query(sql, new Object[] { item }, (rs, rowNum) -> {
            PartNumberBoom obj = new PartNumberBoom();
            if(partNumber != null)  obj.setPartNumber(partNumber.toUpperCase().trim());
            if(rs.getString(2) != null) obj.setPartNumberMaterial(rs.getString(2).toUpperCase().trim());

            if(rs.getString(3) != null) obj.setItem(rs.getString(3).toUpperCase().trim());
            obj.setDescription(rs.getString(4));
            obj.setProject(rs.getString(5));
            obj.setVersion(rs.getString(6));
            obj.setQuantityPer(rs.getDouble(7));
            return obj;
        });
//        System.out.println(item + " = " + list.size());
        if(!list.isEmpty()) {
            for(PartNumberBoom obj : list) {
                PartNumberMaterialConfig partNumberMaterialConfig = partNumberMaterialConfigService.findByObjId(obj.getPartNumberMaterial());
                if(partNumberMaterialConfig != null) {
                    obj.setPartNumberMaterialDescription(partNumberMaterialConfig.getDescription());
                }
            }
            return list;
        }
        String newPartNumber = partNumber;
        if(partNumber != null) newPartNumber+="%";
//        System.out.println(item + " = repo.findList");
		return repo.findList(project, version, newPartNumber);

	}

	public PartNumberBoom findFirstByPartNumberMaterial(String pnMaterial) {
		// TODO Auto-generated method stub
		return repo.findFirstByPartNumberMaterial(pnMaterial);
	}

	public List<PartNumberBoom> findByPartNumberMaterial(String partNumber) {
		// TODO Auto-generated method stub
		return repo.findByPartNumberMaterial(partNumber);
	}

	public List<PartNumberBoom> findByPartNumber(String partNumber) {
		// TODO Auto-generated method stub
		return repo.findByPartNumber(partNumber);
	}

	public void deleteAll(List<PartNumberBoom> arr) {
		repo.deleteAll(arr);
	}

	public void updateProjet(String pn, String itemGroup) {
		repo.updateProjet(pn, itemGroup);
	}

    public List<PartNumberBoom> findList(String project, String version, String partNumber) {
        partNumber = "%"+ partNumber + "%";
        return repo.findList2( partNumber);
    }

    public List<PartNumberBoom> findByItems(List<String> items) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        String sql = "SELECT " +
                "obj1.[Item Number], " +
                "[Component], " +
                "[Item_Parent], " +
                " obj1.[Description], " +
                "[GROUP], [Version] ,[Quantity Per], Scrap, [Operation] " +
                "  FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] as obj1 " +
                "left join [logistics].[dbo].[Rapport9_4_25_BOM_QAD] as obj2 " +
                "ON  obj2.Item_Parent = obj1.[Item Number] " +
                "where Item_Parent in (";
        for(int i = 0; i < items.size(); i++) {
            String item = items.get(i).replaceAll("[^a-zA-Z0-9-_]", "");
            sql += "'" + item + "'";
            if(i < items.size() - 1) {
                sql += ",";
            }
        }
        sql += ") and exists (select * FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] as im2 " +
                "where im2.[Item Number] = [Component] and [Unit of Measure] = 'MT' and Prod_Line = 'R100') ";
        return jdbcTemplateCMS.query(sql, (rs, rowNum) -> {
            PartNumberBoom obj = new PartNumberBoom();
            if(rs.getString(2) != null) obj.setPartNumberMaterial(rs.getString(2).toUpperCase().trim());
            if(rs.getString(3) != null) obj.setItem(rs.getString(3).toUpperCase().trim());
            obj.setDescription(rs.getString(4));
            obj.setProject(rs.getString(5));
            obj.setVersion(rs.getString(6));
            obj.setQuantityPer(rs.getDouble(7));
            return obj;
        });
    }

    public PartNumberBoom findByItem(String item) {
        List<PartNumberBoom> arr = repo.findByItem(item);
        if(arr.size() > 0 ) {
             return arr.get(0);
        }
        return null;
    }

    /**
     * Find partNumberMaterials for a list of partNumber-item pairs
     * Returns a list of unique partNumberMaterial strings
     */
    public List<String> findPartNumberMaterialsByPartNumbersAndItems(List<Map<String, String>> partNumberItemPairs) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        
        List<String> partNumberMaterials = new ArrayList<>();
        
        for (Map<String, String> pair : partNumberItemPairs) {
            String partNumber = pair.get("partNumber");
            String item = pair.get("item");
            
            if (item == null || item.isEmpty()) {
                continue;
            }

            String sql = "SELECT " +
                    "obj1.[Item Number], " +
                    "[Component], " +
                    "[Item_Parent], " +
                    " obj1.[Description], " +
                    "[GROUP], [Version] ,[Quantity Per], Scrap, [Operation] " +
                    "  FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] as obj1 " +
                    "left join [logistics].[dbo].[Rapport9_4_25_BOM_QAD] as obj2 " +
                    "ON  obj2.Item_Parent = obj1.[Item Number] " +
                    "where Item_Parent like ? " +
                    " AND exists (select * FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] as im2 " +
                    " WHERE im2.[Item Number] = [Component] AND [Unit of Measure] = 'MT' AND Prod_Line = 'R100') ";
            
            try {
                List<String> results = jdbcTemplateCMS.query(sql, new Object[] { item }, (rs, rowNum) -> {
                    String component = rs.getString(2);
                    if (component != null) {
                        return component.toUpperCase().trim();
                    }
                    return null;
                });
                
                for (String material : results) {
                    if (material != null && !partNumberMaterials.contains(material)) {
                        partNumberMaterials.add(material);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error querying partNumberBoom for item " + item + ": " + e.getMessage());
            }
        }
        
        return partNumberMaterials;
    }
}
