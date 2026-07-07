package com.lear.MGCMS.controller.CuttingPlan.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Positive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.payload.MachineTypeSwapPlacementSearchResponse;
import com.lear.MGCMS.payload.StatsInfo;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanMaterialPlacementDataService;

@RestController
@RequestMapping("/api/cuttingPlanMaterialPlacementData")
public class CuttingPlanMaterialPlacementDataController {

	@Autowired
	private CuttingPlanMaterialPlacementDataService service;
	
	@GetMapping("/all")
	public Page<CuttingPlanMaterialPlacementData> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
			) {
		if (filters == null) {
            filters = new HashMap<>();
        }
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page,size,sortBy);
	}

	@GetMapping("/machineTypeSwap")
	public MachineTypeSwapPlacementSearchResponse findMachineTypeSwap(
			@RequestParam(value = "partNumberMaterial") String partNumberMaterial,
			@RequestParam(value = "placement", required = false) String placement,
			@RequestParam(value = "machine", required = false) String machine,
			@RequestParam(value = "project", required = false) String project,
			@RequestParam(value = "projet", required = false) String projet,
			@RequestParam(value = "cuttingPlan", required = false) Long cuttingPlan,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
			@RequestParam(value = "size", defaultValue = "20", required = false) int size) {
		String projectFilter = project != null ? project : projet;
		return service.findMachineTypeSwapPlacements(
			partNumberMaterial,
			placement,
			machine,
			projectFilter,
			cuttingPlan,
			page,
			size
		);
	}

	@GetMapping("/cuttingPlan/{cuttingPlanId}")
	public List<CuttingPlanMaterialPlacementData> findByCuttingPlan(@PathVariable String cuttingPlanId) {
		return service.findByCuttingPlan(Long.parseLong(cuttingPlanId));
	}

	@PostMapping("/updateMachine")
	public CuttingPlanMaterialPlacementData updateMachine(@RequestBody Map<String, Object> request) {
		Long cuttingPlan = Long.parseLong(request.get("cuttingPlan").toString());
		String placement = request.get("placement").toString();
		String partNumberMaterial = request.get("partNumberMaterial").toString();
		String newMachine = request.get("newMachine").toString();
		return service.updateMachine(cuttingPlan, placement, partNumberMaterial, newMachine);
	}

	@PostMapping("/toggleActivation")
	public CuttingPlanMaterialPlacementData toggleActivation(@RequestBody Map<String, Object> request) {
		Long cuttingPlan = Long.parseLong(request.get("cuttingPlan").toString());
		String placement = request.get("placement").toString();
		return service.toggleActivation(cuttingPlan, placement);
	}

	@PostMapping("/bulkActivateByMachine")
	public Map<String, Object> bulkActivateByMachine(@RequestBody Map<String, Object> request) {
		Long cuttingPlan = Long.parseLong(request.get("cuttingPlan").toString());
		String partNumberMaterial = request.get("partNumberMaterial").toString();
		String groupPlacement = request.get("groupPlacement").toString();
		String machine = request.get("machine").toString();
		
		int count = service.bulkActivateByMachine(cuttingPlan, partNumberMaterial, groupPlacement, machine);
		
		Map<String, Object> response = new HashMap<>();
		response.put("count", count);
		response.put("message", count + " placement(s) activated");
		return response;
	}
	
//	@PostMapping("/stats")
//	private List<StatsInfo> getCuttingStats(@RequestBody List<Long> ids) {
//		return service.findStatsByMachine(ids);
//	}
	
}
