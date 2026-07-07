package com.lear.MGCMS.controller.CuttingRequest.data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.lear.MGCMS.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestDataService;

@RestController
@RequestMapping("/api/cuttingRequestData")
public class CuttingRequestDataController {
	
	@Autowired
	private CuttingRequestDataService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private QueryService queryService;

	@GetMapping("/notCompleted")
	public List<CuttingRequestData> notCompletedSequenced(
			@RequestParam String zoneName,
		// list of tables
		    @RequestParam(value = "tables", required = true) List<String> tables
	) {
		return queryService.notCompletedSequenced(zoneName, tables, LocalDateTime.now().minusHours(12));
	}

	/**
	 * Get all not-completed sequences for ALL zones in one call
	 * This is optimized for OrdonnancementV3 multi-zone scheduling
	 * @return Map<zoneName, List<CuttingRequestData>>
	 */
	@GetMapping("/notCompletedAllZones")
	public Map<String, List<CuttingRequestData>> notCompletedAllZones() {
		return queryService.notCompletedAllZones(LocalDateTime.now().minusHours(12));
	}

	@GetMapping("/oneSequence/{sequence}")
	public CuttingRequestData notCompletedOne(
			@PathVariable String sequence
	) {
		List<CuttingRequestData> arr = queryService.notCompletedOne(sequence);
		if(arr.size() > 0) {
			return arr.get(0);
		} else {
			return null;
		}
	}


	@PostMapping
	public ResponseEntity<?> save(@RequestBody CuttingRequestData obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
		User user = userService.findByUsername(authentication.getName());
    	boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_QUALITE")) {
				authorized = true; break;
			}
		}
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		CuttingRequestData newObj = service.save(obj);
		return new ResponseEntity<CuttingRequestData>(newObj, HttpStatus.CREATED);
	}
	@GetMapping("/all")
	public Page<CuttingRequestData> findAll(
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
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		CuttingRequestData obj = service.findById(id);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CuttingRequestData>(obj, HttpStatus.OK);
	}

	// pass a list of sequence in body and get a list of CuttingRequestData
	@PostMapping("/bySequences")
	public List<CuttingRequestData> findBySequences(@RequestBody List<String> sequences) {
		return service.findBySequences(sequences);
	}
	

}
