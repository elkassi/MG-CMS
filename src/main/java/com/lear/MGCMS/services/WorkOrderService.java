package com.lear.MGCMS.services;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.WorkOrder;
import com.lear.MGCMS.repositories.WorkOrderRepository;
import com.lear.MGCMS.services.cms.OrderScheduleService;
import com.lear.cms.domain.OrderSchedule;
import com.lear.cms.repositories.OrderScheduleRepository;

@Service
public class WorkOrderService {
	
	@Autowired
	private WorkOrderRepository repo;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private OrderScheduleService orderScheduleService;

    @Autowired
    private OrderScheduleRepository orderScheduleRepository;


    @Autowired
    public WorkOrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WorkOrder> findList(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo.findList(date, shift);
	}

	public WorkOrder save(WorkOrder obj) {
		return repo.save(obj);
	}
	
	public WorkOrder findByWo(String wo) {
		return repo.findByWo(wo);
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
		if (sortDirection.equalsIgnoreCase("desc")) {
			return Sort.Direction.DESC;
		} else {
			return Sort.Direction.ASC;
		}
	}

	public Page<WorkOrder> findAll(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());
		
        Specification<WorkOrder> specification = (root, query, builder) -> {
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


//	public WorkOrder findByWo(String wo) {
//		// TODO Auto-generated method stub
//		return repo.findByWo(wo);
//	}


    public List<WorkOrder> findBetweenInterval(LocalDate date1, LocalDate date2) {
        return repo.findBetweenInterval(date1,date2);
    }

    public void delete(WorkOrder obj) {
        repo.delete(obj);
    }

    public List<String> getItemsWithStatus(String f) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");

        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);

        /*
          select PartNumber_Demande FROM qualite.dbo.Order_Schedule
  where Site_Demande = 'CUT-KIT' and Status_Demande = 'F' and Quantite_Demande > 0  (Date_Demande =  '2025-07-14' and Shift_Demande = '2'))
  order by Date_Demande desc

         */
        //for the condition Date_Demande it will depende on which shift we are in
        String shift = null;
        LocalDate date = LocalDate.now();
        LocalDate dateMin = LocalDate.now().minusDays(5);
        // add two hours and womapre if it between 0 and 8 or 8 to 16 or 16 to 24
        LocalDateTime dateTime = LocalDateTime.now().plusHours(2);
        if (dateTime.getHour() >= 0 && dateTime.getHour() < 8) {
            shift = "1";
        } else if (dateTime.getHour() >= 8 && dateTime.getHour() < 16) {
            shift = "2";
        } else if (dateTime.getHour() >= 16 && dateTime.getHour() < 24) {
            shift = "3";
        }
        // if we are on shift 3 then we need just before the next day shift 1 or (after dateMin and before the current date)
        // if we are on shift 2 then we need (after dateMin and before the current date)
        // and if we are on shift 1 then we need just shift 1 or shift 2 of the current date or (after dateMin and before the day previous)
        String query = "select PartNumber_Demande FROM dbo.Order_Schedule " +
                "where Site_Demande = 'CUT-KIT' and Status_Demande = '" + f + "' and Quantite_Demande > 0 ";
        if (shift.equals("1")) {
            query += " and ((Date_Demande = '" + date + "' and Shift_Demande in ('1', '2'))" +
                    " OR (Date_Demande <= '" + date.minusDays(1) + "' and Date_Demande >= '" + dateMin + "'))";
        } else if (shift.equals("2")) {
            query += " and Date_Demande >= '" + dateMin + "' and Date_Demande <= '" + date + "'";
        } else if (shift.equals("3")) {
            query += " and ((Date_Demande >= '" + dateMin + "' and Date_Demande <= '" + date + "') or (Date_Demande = '" + date.plusDays(1) + "' and Shift_Demande = '1')) ";
        }
        query += " Group by PartNumber_Demande";

        return jdbcTemplateCMS.queryForList(query, String.class);
    }

    /**
     * SPLIT: When imported quantity is less than original WO quantity,
     * split remaining quantity into a new WO in both databases.
     */
    @Transactional
    public Map<String, Object> splitWorkOrder(String originalWo, int importedQty, User user) {
        if (originalWo == null || importedQty <= 0) {
            return Map.of("split", false, "error", "Invalid split request");
        }

        WorkOrder originalWO = repo.findByWo(originalWo);
        OrderSchedule originalOS = orderScheduleService.findById(Long.parseLong(originalWo));

        if (originalWO == null || originalOS == null) {
            return Map.of("split", false, "error", "Work Order not found");
        }

        int originalOrderScheduleQty = originalOS.getQuantiteDemande() != null ? originalOS.getQuantiteDemande() : 0;
        int originalWorkOrderQty = toInt(originalWO.getQtyOpen())
            + toInt(originalWO.getQtyRejeter())
            + toInt(originalWO.getQtyCompleted());
        int originalQty = Math.max(originalOrderScheduleQty, originalWorkOrderQty);
        int remainingQty = originalQty - importedQty;

        if (remainingQty <= 0) {
            return Map.of("split", false);
        }

        Long newId = createSplitOrderSchedule(originalOS, remainingQty, originalWo, user);

        // Create new WorkOrder in MG_CMS DB (remaining part)
        WorkOrder newWO = new WorkOrder();
        newWO.setWo(String.valueOf(newId));
        newWO.setWoid(originalWO.getWoid());
        newWO.setItem(originalWO.getItem());
        newWO.setPartNumber(originalWO.getPartNumber());
        newWO.setDescription(originalWO.getDescription());
        newWO.setGroupName(originalWO.getGroupName());
        newWO.setDesignGroup(originalWO.getDesignGroup());
        newWO.setCoverGroup(originalWO.getCoverGroup());
        newWO.setPartNumberStatus(originalWO.getPartNumberStatus());
        newWO.setQtyOpen((double) remainingQty);
        newWO.setDueDate(originalWO.getDueDate());
        newWO.setShift(originalWO.getShift());
        newWO.setStatus(originalWO.getStatus());
        newWO.setCreatedAt(LocalDateTime.now());
        repo.save(newWO);

        // Update original OrderSchedule — reduce quantity & add remark
        String existingRemark = originalOS.getRemarqueDemande();
        originalOS.setQuantiteDemande(importedQty);
        originalOS.setRemarqueDemande(appendRemark(existingRemark,
            "SPLIT: qty " + originalQty + "→" + importedQty
                + ", remaining " + remainingQty + " moved to ID=" + newId));
        originalOS.setModificationDateDemande(LocalDate.now());
        originalOS.setModificationHourDemande(LocalTime.now());
        orderScheduleRepository.save(originalOS);

        // Update original WorkOrder — reduce quantity
        int importedOpenQty = Math.max(0,
            importedQty - toInt(originalWO.getQtyRejeter()) - toInt(originalWO.getQtyCompleted()));
        originalWO.setQtyOpen((double) importedOpenQty);
        originalWO.setUpdatedAt(LocalDateTime.now());
        repo.save(originalWO);

        Map<String, Object> result = new HashMap<>();
        result.put("split", true);
        result.put("newWo", newId);
        result.put("originalQty", originalQty);
        result.put("remainingQty", remainingQty);
        result.put("importedQty", importedQty);
        return result;
    }

    private Long createSplitOrderSchedule(OrderSchedule originalOS, int remainingQty, String originalWo, User user) {
        try {
            Long generatedId = insertSplitOrderScheduleWithGeneratedId(originalOS, remainingQty, originalWo, user);
            if (generatedId != null) {
                return generatedId;
            }
        } catch (Exception e) {
            // Fall back to explicit IDs when the target database does not auto-generate ID_Demande.
        }

        Long fallbackId = getNextOrderScheduleId();
        OrderSchedule newOS = buildSplitOrderSchedule(originalOS, remainingQty, originalWo, user);
        newOS.setIdDemande(fallbackId);
        orderScheduleRepository.save(newOS);
        return fallbackId;
    }

    private Long insertSplitOrderScheduleWithGeneratedId(OrderSchedule originalOS, int remainingQty, String originalWo, User user) {
        JdbcTemplate jdbcTemplateCMS = getCmsJdbcTemplate();
        OrderSchedule newOS = buildSplitOrderSchedule(originalOS, remainingQty, originalWo, user);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int insertedRows = jdbcTemplateCMS.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbo.Order_Schedule (" +
                    "Site_Demande, Chaine_Demande, Project_Demande, PartNumber_Demande, Description_PN_Demande, " +
                    "Leather_Kit_Demande, Textil_Kit_Demande, Quantite_Demande, Date_Demande, Shift_Demande, " +
                    "Matricule_Demandeur_Demande, Nom_Demandeur_Demande, Status_Demande, Remarque_Demande, " +
                    "Status_PS_Demande, Status_Reception_Sewing_Demande, Creation_Date_Demande, Creation_Hour_Demande, " +
                    "Modification_Date_Demande, Modification_Hour_Demande, UserName_Demande, HostName_Demande, " +
                    "SessionW_Demande, WorkCenter_Demande, Marker_Group_ID_D, Import_Date_D) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );

            int index = 1;
            statement.setObject(index++, newOS.getSiteDemande());
            statement.setObject(index++, newOS.getChaineDemande());
            statement.setObject(index++, newOS.getProjectDemande());
            statement.setObject(index++, newOS.getPartNumberDemande());
            statement.setObject(index++, newOS.getDescriptionPNDemande());
            statement.setObject(index++, newOS.getLeatherKitDemande());
            statement.setObject(index++, newOS.getTextilKitDemande());
            statement.setObject(index++, newOS.getQuantiteDemande());
            statement.setObject(index++, newOS.getDateDemande());
            statement.setObject(index++, newOS.getShiftDemande());
            statement.setObject(index++, newOS.getMatriculeDemandeurDemande());
            statement.setObject(index++, newOS.getNomDemandeurDemande());
            statement.setObject(index++, newOS.getStatusDemande());
            statement.setObject(index++, newOS.getRemarqueDemande());
            statement.setObject(index++, newOS.getStatusPSDemande());
            statement.setObject(index++, newOS.getStatusReceptionSewingDemande());
            statement.setObject(index++, newOS.getCreationDateDemande());
            statement.setObject(index++, newOS.getCreationHourDemande());
            statement.setObject(index++, newOS.getModificationDateDemande());
            statement.setObject(index++, newOS.getModificationHourDemande());
            statement.setObject(index++, newOS.getUserNameDemande());
            statement.setObject(index++, newOS.getHostNameDemande());
            statement.setObject(index++, newOS.getSessionWDemande());
            statement.setObject(index++, newOS.getWorkCenterDemande());
            statement.setObject(index++, newOS.getMarkerGroupIDD());
            statement.setObject(index++, newOS.getImportDateD());
            return statement;
        }, keyHolder);

        if (insertedRows != 1 || keyHolder.getKey() == null) {
            throw new IllegalStateException("Unable to generate ID_Demande for split work order");
        }

        return keyHolder.getKey().longValue();
    }

    private OrderSchedule buildSplitOrderSchedule(OrderSchedule originalOS, int remainingQty, String originalWo, User user) {
        OrderSchedule newOS = new OrderSchedule();
        newOS.setSiteDemande(originalOS.getSiteDemande());
        newOS.setChaineDemande(originalOS.getChaineDemande());
        newOS.setProjectDemande(originalOS.getProjectDemande());
        newOS.setPartNumberDemande(originalOS.getPartNumberDemande());
        newOS.setDescriptionPNDemande(originalOS.getDescriptionPNDemande());
        newOS.setLeatherKitDemande(originalOS.getLeatherKitDemande());
        newOS.setTextilKitDemande(originalOS.getTextilKitDemande());
        newOS.setQuantiteDemande(remainingQty);
        newOS.setDateDemande(originalOS.getDateDemande());
        newOS.setShiftDemande(originalOS.getShiftDemande());
        newOS.setMatriculeDemandeurDemande(originalOS.getMatriculeDemandeurDemande());
        newOS.setNomDemandeurDemande(originalOS.getNomDemandeurDemande());
        newOS.setStatusDemande("F");
        newOS.setStatusPSDemande(originalOS.getStatusPSDemande());
        newOS.setStatusReceptionSewingDemande(originalOS.getStatusReceptionSewingDemande());
        newOS.setWorkCenterDemande(originalOS.getWorkCenterDemande());
        newOS.setMarkerGroupIDD(originalWo);
        newOS.setImportDateD(LocalDateTime.now());
        newOS.setRemarqueDemande("SPLIT from ID_Demande=" + originalWo + ", remaining qty=" + remainingQty);
        newOS.setCreationDateDemande(LocalDate.now());
        newOS.setCreationHourDemande(LocalTime.now());
        newOS.setModificationDateDemande(LocalDate.now());
        newOS.setModificationHourDemande(LocalTime.now());
        if (user != null) {
            newOS.setUserNameDemande(user.getFirstName() + " " + user.getLastName());
        } else {
            newOS.setUserNameDemande(originalOS.getUserNameDemande());
        }
        newOS.setHostNameDemande("CMS WEB");
        newOS.setSessionWDemande("CMS WEB");
        return newOS;
    }

    private JdbcTemplate getCmsJdbcTemplate() {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        return new JdbcTemplate(cmsDataSource);
    }

    private Long getNextOrderScheduleId() {
        Long maxId = orderScheduleRepository.getMaxId();
        Long fallbackId = maxId == null ? 1L : maxId + 1L;

        try {
            Long sequenceId = orderScheduleRepository.getNextIdFromSequence();
            if (sequenceId != null && sequenceId >= fallbackId) {
                return sequenceId;
            }
        } catch (Exception e) {
            // Fall back to MAX(ID_Demande)+1 when the local DB has not been migrated yet.
        }

        return fallbackId;
    }

    private int toInt(Double value) {
        if (value == null) {
            return 0;
        }
        return (int) Math.round(value);
    }

    private String appendRemark(String existingRemark, String newRemark) {
        if (newRemark == null || newRemark.isBlank()) {
            return existingRemark;
        }
        if (existingRemark == null || existingRemark.isBlank()) {
            return newRemark;
        }
        return existingRemark + " | " + newRemark;
    }

    /**
     * FUSE: Merge multiple WOs for the same partNumber into the last one (highest ID_Demande).
     * Zeroes out all source WOs and transfers total quantity to the target.
     */
    @Transactional
    public Map<String, Object> fuseWorkOrders(List<String> woIds, User user) {
        // Sort WO IDs to find the LAST one (highest ID_Demande = target)
        List<String> sortedIds = woIds.stream()
            .sorted(Comparator.comparingLong(id -> Long.parseLong(id)))
            .collect(Collectors.toList());

        String targetWo = sortedIds.get(sortedIds.size() - 1);
        List<String> sourceWos = sortedIds.subList(0, sortedIds.size() - 1);

        WorkOrder targetWO = repo.findByWo(targetWo);
        OrderSchedule targetOS = orderScheduleService.findById(Long.parseLong(targetWo));

        if (targetWO == null || targetOS == null) {
            return Map.of("success", false, "error", "Target Work Order not found");
        }

        int totalQty = targetOS.getQuantiteDemande() != null ? targetOS.getQuantiteDemande() : 0;
        StringBuilder targetRemark = new StringBuilder();
        targetRemark.append("FUSED: received qty from");

        for (String sourceWo : sourceWos) {
            WorkOrder sourceWO = repo.findByWo(sourceWo);
            OrderSchedule sourceOS = orderScheduleService.findById(Long.parseLong(sourceWo));

            if (sourceWO == null || sourceOS == null) continue;

            int sourceQty = sourceOS.getQuantiteDemande() != null ? sourceOS.getQuantiteDemande() : 0;
            totalQty += sourceQty;
            targetRemark.append(" ID=").append(sourceWo).append(" (").append(sourceQty).append("),");

            // Zero out source OrderSchedule
            String existingSourceRemark = sourceOS.getRemarqueDemande() != null
                ? sourceOS.getRemarqueDemande() : "";
            sourceOS.setQuantiteDemande(0);
            sourceOS.setStatusDemande("C");
            sourceOS.setMarkerGroupIDD(targetWo);
            sourceOS.setRemarqueDemande(existingSourceRemark
                + " | FUSED: qty " + sourceQty + " transferred to ID=" + targetWo);
            sourceOS.setModificationDateDemande(LocalDate.now());
            sourceOS.setModificationHourDemande(LocalTime.now());
            orderScheduleRepository.save(sourceOS);

            // Zero out source WorkOrder
            sourceWO.setQtyOpen(0.0);
            sourceWO.setStatus("C");
            sourceWO.setUpdatedAt(LocalDateTime.now());
            repo.save(sourceWO);
        }

        // Update target with total quantity
        String existingTargetRemark = targetOS.getRemarqueDemande() != null
            ? targetOS.getRemarqueDemande() : "";
        targetOS.setQuantiteDemande(totalQty);
        targetOS.setMarkerGroupIDD(targetWo);
        targetOS.setRemarqueDemande(existingTargetRemark
            + " | " + targetRemark.toString() + " total=" + totalQty);
        targetOS.setModificationDateDemande(LocalDate.now());
        targetOS.setModificationHourDemande(LocalTime.now());
        orderScheduleRepository.save(targetOS);

        targetWO.setQtyOpen((double) totalQty);
        targetWO.setUpdatedAt(LocalDateTime.now());
        repo.save(targetWO);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("targetWo", targetWo);
        result.put("totalQty", totalQty);
        result.put("fusedWos", sortedIds);
        result.put("zeroedWos", sourceWos);
        return result;
    }

    /**
     * Detect duplicate partNumbers in work orders for a given date/shift.
     */
    public Map<String, Object> detectDuplicates(LocalDate date, String shift) {
        List<WorkOrder> workOrders = repo.findList(date, shift);
        Map<String, OrderSchedule> orderScheduleByWo = new HashMap<>();

        for (WorkOrder workOrder : workOrders) {
            try {
                OrderSchedule orderSchedule = orderScheduleService.findById(Long.parseLong(workOrder.getWo()));
                if (orderSchedule != null) {
                    orderScheduleByWo.put(workOrder.getWo(), orderSchedule);
                }
            } catch (NumberFormatException e) {
                // Ignore work orders with non-numeric identifiers in duplicate detection.
            }
        }

        Map<String, List<WorkOrder>> duplicates = workOrders.stream()
            .filter(wo -> wo.getQtyOpen() != null && wo.getQtyOpen() > 0)
            .filter(wo -> wo.getPartNumber() != null)
            .filter(wo -> {
                OrderSchedule orderSchedule = orderScheduleByWo.get(wo.getWo());
                return orderSchedule != null && "F".equalsIgnoreCase(orderSchedule.getStatusDemande());
            })
            .collect(Collectors.groupingBy(WorkOrder::getPartNumber))
            .entrySet().stream()
            .filter(e -> e.getValue().size() >= 2)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                    .sorted(Comparator.comparingLong(wo -> Long.parseLong(wo.getWo())))
                    .collect(Collectors.toList())
            ));

        if (duplicates.isEmpty()) {
            return Map.of("hasDuplicates", false);
        }

        List<Map<String, Object>> duplicateGroups = new ArrayList<>();
        for (Map.Entry<String, List<WorkOrder>> entry : duplicates.entrySet()) {
            Map<String, Object> group = new HashMap<>();
            group.put("partNumber", entry.getKey());
            group.put("workOrders", entry.getValue());
            group.put("totalQty", entry.getValue().stream()
                .mapToDouble(WorkOrder::getQtyOpen).sum());
            group.put("count", entry.getValue().size());
            duplicateGroups.add(group);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasDuplicates", true);
        result.put("duplicates", duplicateGroups);
        return result;
    }
}
