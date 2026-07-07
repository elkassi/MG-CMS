package com.lear.MGCMS.controller.CuttingRequest.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestPartNumberData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.payload.StatsInfo2;
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
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestBoxData;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestBoxDataService;

@RestController
@RequestMapping("/api/cuttingRequestBoxData")
public class CuttingRequestBoxDataController {

	@Autowired
	private CuttingRequestBoxDataService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private QueryService queryService;

	@GetMapping("/getStatsBySequences")
	public List<StatsInfo2> getStatsBySequences(
			@RequestParam(value = "sequences", required = true) List<String> sequencesArr
	) {
		return queryService.getStatsBySequences(sequencesArr);
	}

	/**
	 * POST version of getStatsBySequences to avoid URL length limits
	 * for large sequence lists (used by OrdonnancementV3)
	 */
	@PostMapping("/getStatsBySequences")
	public List<StatsInfo2> getStatsBySequencesPost(@RequestBody List<String> sequencesArr) {
		return queryService.getStatsBySequences(sequencesArr);
	}


	@PostMapping
	public ResponseEntity<?> save(@RequestBody CuttingRequestBoxData obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;

		User user = userService.findByUsername(authentication.getName());
    	boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_IMPORTER") || role.getName().equals("ROLE_ADMIN")) {
				authorized = true; break;
			}
		}
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		
		CuttingRequestBoxData newObj = service.save(obj);
		return new ResponseEntity<CuttingRequestBoxData>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/bySequence/{sequence}")
	public List<CuttingRequestBoxData> findBySequence(@PathVariable String sequence) {
		return service.findBySequence(sequence);
	}

	@GetMapping("/all")
	public Page<CuttingRequestBoxData> findAll(
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
		CuttingRequestBoxData obj = service.findById(id);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CuttingRequestBoxData>(obj, HttpStatus.OK);
	}

	@GetMapping("/wo/{wo}")
	public ResponseEntity<?> findByWo(@PathVariable String wo)  {
		CuttingRequestBoxData obj = service.findFirstByWo(wo);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CuttingRequestBoxData>(obj, HttpStatus.OK);
	}

	@PostMapping("/bySequences")
	public List<CuttingRequestBoxData> findBySequences(@RequestBody List<String> sequences) {
		return service.findBySequences(sequences);
	}
	
}
