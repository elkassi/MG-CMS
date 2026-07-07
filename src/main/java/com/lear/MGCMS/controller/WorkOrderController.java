package com.lear.MGCMS.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.PartNumberInfo2;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.repositories.PartNumberInfo2Repository;
import com.lear.MGCMS.services.PartNumberBoomService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.cms.OrderScheduleService;
import com.lear.MGCMS.services.ctc.FilesService;
import com.lear.cms.domain.OrderSchedule;
import com.lear.ctc.domain.Files;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.domain.WorkOrder;
import com.lear.MGCMS.payload.WorkOrderElem;
import com.lear.MGCMS.services.WorkOrderService;

@RestController
@RequestMapping("/api/workOrder")
public class WorkOrderController {
	
	@Autowired
	private WorkOrderService service;
	
	@GetMapping("/filter")
	public List<WorkOrder> findList(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift
			){
		return service.findList(date, shift);
	}
	
	@GetMapping("/all")
	public Page<WorkOrder> findAll(@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
			@RequestParam(value = "size", defaultValue = "20", required = false) int size,
			@RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy) {
		if (filters == null) {
			filters = new HashMap<>();
		}
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page, size, sortBy);
	}

//	@GetMapping("/{reportName}")
	public List<WorkOrderElem> findRapport(@PathVariable String reportName) throws IOException {
		List<WorkOrderElem> arr = new ArrayList<WorkOrderElem>();

		String server = "10.49.0.46";// txtServer1.getText();
		int port = 21;
		String user = "mfg";// txtUserId1.getText();
		String pass = "leartsi01";// txtPassword1.getText();
		String qadLink = "/qad/home/ftpkpitnr/"+reportName+".prn";
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();

			BufferedReader reader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(qadLink)));

			String line;
			while ((line = reader.readLine()) != null) {
				if(line.length() < 130 || line.contains("Qty Completed Qty Rejected     Qty Open") 
						|| line.contains("------------") 
						|| line.contains("Work Order") 
						|| line.contains("TANGIER-TRIM") 
						|| line.contains("Work Order by Item Report")) {
					continue;
				}
				WorkOrderElem obj = new WorkOrderElem();
				obj.setItem(line.substring(0,24).toUpperCase().trim());
				obj.setWo(line.substring(25,43).toUpperCase().trim());
				obj.setWoid(line.substring(44,52).toUpperCase().trim());
				try {
					obj.setQtyCompleted(Double.parseDouble(line.substring(53, 66)));
				} catch (Exception e) {
				}
				try {
					obj.setQtyRejeter(Double.parseDouble(line.substring(67,79)));
				} catch (Exception e) {
				}
				try {
					obj.setQtyOpen(Double.parseDouble(line.substring(80,92)));
				} catch (Exception e) {
				}
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
		        

				obj.setDueDate(LocalDate.parse(line.substring(101,110).toUpperCase().trim(), formatter));
				obj.setShift(line.substring(120,128).toUpperCase().trim());
				obj.setSt(line.substring(129, line.length()).toUpperCase().trim());
				arr.add(obj);
			}
			
			reader.close();
			ftpClient.completePendingCommand();
		} finally {
			ftpClient.disconnect();
		}

		return arr;
	}
	
//	@GetMapping
	public List<WorkOrderElem> findAll() throws IOException {
		List<WorkOrderElem> arr = new ArrayList<WorkOrderElem>();

		String server = "10.49.0.46";// txtServer1.getText();
		int port = 21;
		String user = "mfg";// txtUserId1.getText();
		String pass = "leartsi01";// txtPassword1.getText();
		String qadLink = "/qad/home/ftpkpitnr/";
		String[] rapports = { "16_3_2A", "16_3_2R", "16_3_2C"}; // 16_3_2AA.prn 16_3_2RR.prn
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();

			for(String rapport : rapports) {
				try {
					
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(qadLink+rapport+".prn")));
				String line;
				while ((line = reader.readLine()) != null) {
					if(line.length() < 130 || line.contains("Qty Completed Qty Rejected     Qty Open") 
							|| line.contains("------------") 
							|| line.contains("Work Order") 
							|| line.contains("TANGIER-TRIM") 
							|| line.contains("Work Order by Item Report")) {
						continue;
					}
					WorkOrderElem obj = new WorkOrderElem();
					obj.setItem(line.substring(0,24).toUpperCase().trim());
					obj.setWo(line.substring(25,43).toUpperCase().trim());
					obj.setWoid(line.substring(44,52).toUpperCase().trim());
					try {
						obj.setQtyCompleted(Double.parseDouble(line.substring(53, 66)));
					} catch (Exception e) {
					}
					try {
						obj.setQtyRejeter(Double.parseDouble(line.substring(67,79)));
					} catch (Exception e) {
					}
					try {
						obj.setQtyOpen(Double.parseDouble(line.substring(80,92)));
					} catch (Exception e) {
					}
			        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
			        

					obj.setDueDate(LocalDate.parse(line.substring(101,110).toUpperCase().trim(), formatter));
					obj.setShift(line.substring(120,128).toUpperCase().trim());
					obj.setSt(line.substring(129, line.length()).toUpperCase().trim());
					arr.add(obj);
				}
				
				reader.close();
				ftpClient.completePendingCommand();
				} catch (Exception e) {
					System.out.println(rapport + " ERROR : "+ e.getMessage());
				}
			}
			
		} finally {
				ftpClient.disconnect();
		}

		return arr;
	}

	@Autowired
	private OrderScheduleService orderScheduleService;
	@Autowired
	private PartNumberInfo2Repository partNumberInfo2Repository;
	@Autowired
	private PartNumberBoomService partNumberBoomService;
	@Autowired
	private FilesService filesService;

	@Autowired
	private UserService userService;


	@GetMapping("/refresh")
	public ResponseEntity<?> woRapportAsprova(
			//date, shift
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift
	) throws IOException {

		List<OrderSchedule> arr = orderScheduleService.findByDateAndShift(date, shift);
		List<WorkOrder> workOrders = service.findList(date, shift);
		System.out.println("refresh Wo arr " + date + " => " + shift + " : " + arr.size());
		for (
				OrderSchedule order : arr) {
			WorkOrder obj = new WorkOrder();
			boolean isNew = true;
			for (WorkOrder wo : workOrders) {
				if (wo.getWo().equals(order.getIdDemande() + "")) {
					obj = wo;
					isNew = false;
					break;
				}
			}

			obj.setItem(order.getPartNumberDemande());
			obj.setWo(order.getIdDemande() + "");
			obj.setWoid(order.getMarkerID());
			try {
				obj.setQtyOpen(Double.parseDouble(order.getQuantiteDemande() + ""));
			} catch (Exception e) {
			}
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

			obj.setDueDate(order.getDateDemande());
			obj.setShift(order.getShiftDemande());
			obj.setStatus(order.getStatusReceptionSewingDemande());

			if (obj.getItem().toUpperCase().startsWith("W")) {
				obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
			}

			PartNumberInfo2 pnInfo2 = new PartNumberInfo2();
			if (obj.getItem().toUpperCase().startsWith("WL")) {
				obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
				pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getItem().toUpperCase().substring(1));
			} else {
				PartNumberBoom pnb = partNumberBoomService.findByItem(obj.getItem());
				if (pnb != null) {
					obj.setPartNumber(pnb.getPartNumber());
					obj.setDescription(pnb.getDescription());
					pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
				}
			}

			if (pnInfo2 != null) {
				obj.setGroupName(pnInfo2.getItemGroup());
				obj.setDesignGroup(pnInfo2.getDesignGroup());
				obj.setPartNumberStatus(pnInfo2.getStatus());
				obj.setCoverGroup(pnInfo2.getCovertype());
				obj.setDescription(pnInfo2.getDescription());
			} else {
				List<Files> files = filesService.findBySemiFinishedGoodPartNumber(obj.getItem());
				if (files.size() > 0) {
					Files file = files.get(0);
					obj.setDescription(file.getPartNumberCoverDesciption());
					obj.setPartNumber(file.getPartNumberCover());
					obj.setGroupName(file.getProjet());
				}
			}

			if (obj.getPartNumber() == null) {
				System.out.println("ERROR ITEM NOT FOUND : " + obj.getItem());
			}

			if (isNew) {
				obj.setCreatedAt(LocalDateTime.now());
			}

			obj.setUpdatedAt(LocalDateTime.now());
			service.save(obj);
		}

		workOrders.removeIf(wo -> arr.stream().

				anyMatch(orderSchedule -> wo.getWo().

						equals(orderSchedule.getIdDemande() + "")));
		for (WorkOrder obj : workOrders) {
			OrderSchedule order = orderScheduleService.findById(Long.parseLong(obj.getWo()));
			if (order == null) {
				service.delete(obj);
				System.out.println("Deleted WO : " + obj.getWo());
			} else {
				boolean isNew = false;
				obj.setItem(order.getPartNumberDemande());
				obj.setWo(order.getIdDemande() + "");
				obj.setWoid(order.getMarkerID());
				try {
					obj.setQtyOpen(Double.parseDouble(order.getQuantiteDemande() + ""));
				} catch (Exception e) {
				}
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

				obj.setDueDate(order.getDateDemande());
				obj.setShift(order.getShiftDemande());
				obj.setStatus(order.getStatusReceptionSewingDemande());

				if (obj.getItem().toUpperCase().startsWith("W")) {
					obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
				}

				PartNumberInfo2 pnInfo2 = new PartNumberInfo2();
				if (obj.getItem().toUpperCase().startsWith("WL")) {
					obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
					pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getItem().toUpperCase().substring(1));
				} else {
					PartNumberBoom pnb = partNumberBoomService.findByItem(obj.getItem());
					if (pnb != null) {
						obj.setPartNumber(pnb.getPartNumber());
						obj.setDescription(pnb.getDescription());
						pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
					}
				}

				if (pnInfo2 != null) {
					obj.setGroupName(pnInfo2.getItemGroup());
					obj.setDesignGroup(pnInfo2.getDesignGroup());
					obj.setPartNumberStatus(pnInfo2.getStatus());
					obj.setCoverGroup(pnInfo2.getCovertype());
					obj.setDescription(pnInfo2.getDescription());
				} else {
					List<Files> files = filesService.findBySemiFinishedGoodPartNumber(obj.getItem());
					if (files.size() > 0) {
						Files file = files.get(0);
						obj.setDescription(file.getPartNumberCoverDesciption());
						obj.setPartNumber(file.getPartNumberCover());
						obj.setGroupName(file.getProjet());
					}
				}

				if (obj.getPartNumber() == null) {
					System.out.println("ERROR ITEM NOT FOUND : " + obj.getItem());
				}

				obj.setUpdatedAt(LocalDateTime.now());
				service.save(obj);
			}
		}
//		System.out.println("woRapportAsprova Done");
		return  ResponseEntity.ok().body("OK" + arr.size() + " WO Processed" );
	}

	@PostMapping("/{wo}/toggleDeactivation")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('CAD')")
	public ResponseEntity<?> toggleDeactivation(@PathVariable String wo) {
		WorkOrder workOrder = service.findByWo(wo);
		if (workOrder == null) {
			return ResponseEntity.notFound().build();
		}
		workOrder.setDeactivated(workOrder.getDeactivated() == null || !workOrder.getDeactivated());
		workOrder.setUpdatedAt(LocalDateTime.now());
		service.save(workOrder);
		return ResponseEntity.ok().body(workOrder);
	}

	// Detect duplicate partNumbers in work orders for a given date/shift
	@GetMapping("/duplicates")
	@PreAuthorize("hasRole('IMPORTER') or hasRole('ADMIN') or hasRole('CAD')")
	public ResponseEntity<?> detectDuplicates(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) String shift) {
		Map<String, Object> result = service.detectDuplicates(date, shift);
		return ResponseEntity.ok(result);
	}

	// Fuse multiple work orders for the same partNumber
	@PostMapping("/fuse")
	@PreAuthorize("hasRole('IMPORTER') or hasRole('ADMIN')")
	public ResponseEntity<?> fuseWorkOrders(@RequestBody List<String> woIds, Authentication auth) {
		if (woIds == null || woIds.size() < 2) {
			return ResponseEntity.badRequest().body(Map.of("message", "Need at least 2 work orders to fuse"));
		}
		User user = userService.findByUsername(auth.getName());
		Map<String, Object> result = service.fuseWorkOrders(woIds, user);
		return ResponseEntity.ok(result);
	}

	// Split a work order manually
	@PostMapping("/split")
	@PreAuthorize("hasRole('IMPORTER') or hasRole('ADMIN')")
	public ResponseEntity<?> splitWorkOrder(@RequestBody Map<String, Object> body, Authentication auth) {
		String wo = (String) body.get("wo");
		Integer importedQty = body.get("importedQty") instanceof Integer
			? (Integer) body.get("importedQty")
			: ((Number) body.get("importedQty")).intValue();

		if (wo == null || importedQty == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "wo and importedQty are required"));
		}

		User user = userService.findByUsername(auth.getName());
		Map<String, Object> result = service.splitWorkOrder(wo, importedQty, user);
		return ResponseEntity.ok(result);
	}

}

