package com.lear.MGCMS.controller.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieLight;
import com.lear.MGCMS.payload.SequenceStatus;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieLightService;

@RestController
@RequestMapping("/api/cuttingRequestSerieLight")
public class CuttingRequestSerieLightController {
	
	@Autowired 
	private CuttingRequestSerieLightService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@PostMapping("/getSequences")
	public List<SequenceStatus> getStatusSequence() {
		return service.getStatusSequence();
	}
	
	
	
	@GetMapping
	public List<CuttingRequestSerieLight> findAll(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift
			){
		return service.findAll(date, shift);
	}
	
	@GetMapping("/historique")
	public List<CuttingRequestSerieLight> historique(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "machines", required = false) List<String> machines
			){
		return service.historique(date, machines);
	}
	
	@GetMapping("/notYet")
	public List<CuttingRequestSerieLight> findAllNotYet(){
		return service.findAllNotYet();
	}

	@GetMapping("/inProgress")
	public List<CuttingRequestSerieLight> findAllInProgress(){
		return service.findAllInProgress();
	}
	
	@GetMapping("/filter")
	public List<CuttingRequestSerieLight> findStats(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift,
			@RequestParam(value = "machine", required = false) String machine
			){
		LocalDateTime startDate = null, endDate =null;
		if(shift.equals("2")) {
			startDate = date.atTime(05, 55);
			endDate = date.atTime(13, 45);
		} else if(shift.equals("3")) {
			startDate = date.atTime(13, 55);
			endDate = date.atTime(21, 45);
		} else {
			startDate = date.atTime(21, 55).minusDays(1);
			endDate = date.atTime(05, 45);
		}
		if(endDate.compareTo(LocalDateTime.now()) > 0 ) {
			endDate = LocalDateTime.now();
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		System.out.println(startDate.format(formatter) + " => "+ endDate.format(formatter) + " : " + machine);
		return service.findBetween(startDate, endDate, machine);
	}


	
	@GetMapping("/{serie}")
	public ResponseEntity<?> findBySerie(@PathVariable String serie) {
		CuttingRequestSerieLight obj = service.findBySerie(serie);

		if (obj != null) {
			return new ResponseEntity<CuttingRequestSerieLight>(obj, HttpStatus.OK);
		}
		return new ResponseEntity<String>(serie + " not found", HttpStatus.BAD_REQUEST);
	}


}
