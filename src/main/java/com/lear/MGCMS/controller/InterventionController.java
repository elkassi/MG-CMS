package com.lear.MGCMS.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.lear.MGCMS.domain.Intervention;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.InterventionService;

@RestController
@RequestMapping("/api/intervention")
public class InterventionController {

	@Autowired 
	private InterventionService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	@Autowired
	private UserService userService;
	@GetMapping("/serie/{serie}")
	public List<Intervention> findByFilter(@PathVariable String serie) {
		return service.findBySerie(serie);
	}

	@GetMapping("/getList/{arr}")
	public List<Intervention> getList(@PathVariable List<String> arr) {
		return service.getList(arr);
	}


	@GetMapping("/all")
	public Page<Intervention> findAll(
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
		// show filters
		return service.findAll2(filters, page,size,sortBy);
	}


	
	@GetMapping("/list")
	public List<Intervention> findAll() {
		return service.findAll();
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		Intervention obj = service.findByObjId(id);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Intervention>(obj, HttpStatus.OK);
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody Intervention obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
    	
    	User user = userService.findByUsername(authentication.getName());

    	boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_ADMIN")) {
				authorized = true; break;
			}
		}
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}

		if(obj.getCodeArret() == null) {
			return new ResponseEntity<String>("Code Arret is required", HttpStatus.BAD_REQUEST);
		}
    	
		Intervention newObj = service.save(obj);
		return new ResponseEntity<Intervention>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/valider")
	public ResponseEntity<?> valider(@Valid @RequestBody Intervention obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
    	
    	User user = userService.findByUsername(authentication.getName());

    	boolean authorized = false;
		if(obj == null || user.getDepartement() == null || obj.getDepartement() == null || !user.getDepartement().equals(obj.getDepartement())) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
    	obj.setDateValidation(LocalDateTime.now());
		obj.setValiderPar(user.getLastName() + " " + user.getFirstName() + " ("+user.getMatricule()+ ")");
		Intervention newObj = service.save(obj);
		return new ResponseEntity<Intervention>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@RequestBody Intervention obj, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());

    	boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_ADMIN")) {
				authorized = true; break;
			}
		}
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		
		if(obj.getId() != null) {
			Intervention oldObj = service.findByObjId(obj.getId());
			service.delete(oldObj);
			return new ResponseEntity<String>("Deleted", HttpStatus.OK);

		}
		return new ResponseEntity<String>("Not found", HttpStatus.UNAUTHORIZED);

	}
	
	@GetMapping("/kpi-maintenance")
	public List<Intervention> findMaintenanceKpi(
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(value = "machine", required = false) String machine
			){
		return service.findMaintenanceKpi(startDate, endDate, machine);
	}

	@GetMapping("/filter")
	public List<Intervention> findStats(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift,
			@RequestParam(value = "shift", required = false) String machine
			){
		LocalDateTime startDate = null, endDate =null;
		if(shift.equals("2")) {
			startDate = date.atTime(05, 50);
			endDate = date.atTime(13, 50);
		} else if(shift.equals("3")) {
			startDate = date.atTime(13, 50);
			endDate = date.atTime(21, 50);
		} else {
			startDate = date.atTime(21, 50).minusDays(1);
			endDate = date.atTime(05, 50);
		}
		if(endDate.compareTo(LocalDateTime.now()) > 0 ) {
			endDate = LocalDateTime.now();
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		System.out.println(startDate.format(formatter) + " => "+ endDate.format(formatter));
		return service.findBetween(startDate, endDate, machine);
	}
	
}
