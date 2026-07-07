package com.lear.MGCMS.controller.CuttingRequest.data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.CuttingPlan.PartNumberCorrespendance;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauHistory;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieDataLight;
import com.lear.MGCMS.domain.FirstCheckConfig;
import com.lear.MGCMS.payload.SerieReport;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieRouleauHistoryService;
import com.lear.MGCMS.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestSerieDataService;

@RestController
@RequestMapping("/api/cuttingRequestSerieData")
public class CuttingRequestSerieDataController {

	@Autowired
	private CuttingRequestSerieDataService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private CuttingRequestSerieRouleauHistoryService cuttingRequestSerieRouleauHistoryService;

	@PostMapping
	public ResponseEntity<?> save(@RequestBody CuttingRequestSerieData obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
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
		CuttingRequestSerieRouleauHistory cuttingRequestSerieRouleauHistory = new CuttingRequestSerieRouleauHistory();
		cuttingRequestSerieRouleauHistory.setSerie(obj.getSerie());
		cuttingRequestSerieRouleauHistory.setChangeDate(LocalDateTime.now());
		cuttingRequestSerieRouleauHistory.setChangedBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule()+")");
		cuttingRequestSerieRouleauHistory.setContent("Serie : " + obj.toString());
		cuttingRequestSerieRouleauHistoryService.save(cuttingRequestSerieRouleauHistory);

		CuttingRequestSerieData newObj = service.save(obj);
		return new ResponseEntity<CuttingRequestSerieData>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/seriesArr")
	public List<CuttingRequestSerieData> getAll(
			@RequestParam(value = "series", required = true) List<String> series
	) {
		return service.findSeries(series);
	}

	@GetMapping("/sequencesArr")
	public List<CuttingRequestSerieData> findBySequencesArr(
			@RequestParam(value = "sequences", required = true) List<String> sequencesArr
	) {
		return service.findBySequencesArr(sequencesArr);
	}

	@GetMapping("/getSeries/{sequence}/{partNumberMaterial}")
	public List<String> findSeries(@PathVariable String sequence, @PathVariable String partNumberMaterial) {
		return service.findSeries(sequence, partNumberMaterial);
	}
	@GetMapping("/bySequence/{sequence}")
	public List<CuttingRequestSerieData> findBySequence(@PathVariable String sequence) {
		return service.findBySequence(sequence);
	}


	@GetMapping("/all")
	public Page<CuttingRequestSerieData> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "serie,desc", required = false) String sortBy
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
		CuttingRequestSerieData obj = service.findById(id);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CuttingRequestSerieData>(obj, HttpStatus.OK);
	}

	@Autowired
	private QueryService queryService;

	@PostMapping("/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public void delete(@RequestBody CuttingRequestSerieData obj) {
		service.delete(obj);
		queryService.deleteSerieCMS(obj.getSerie());
	}

	@PostMapping("/bySequences")
	public List<CuttingRequestSerieDataLight> findBySequences(@RequestBody List<String> sequences) {
		return service.findBySequences(sequences);
	}

	@PostMapping("/getReportByPartNumberMaterial")
	public List<SerieReport> getReportByPartNumberMaterial(@RequestBody List<String> partNumberMaterials) {
		return service.getReportByPartNumberMaterial(partNumberMaterials);
	}

}
