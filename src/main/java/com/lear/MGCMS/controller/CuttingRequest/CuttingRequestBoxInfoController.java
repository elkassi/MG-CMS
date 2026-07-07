package com.lear.MGCMS.controller.CuttingRequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestBoxInfo;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestBoxInfoService;

@RestController
@RequestMapping("/api/cuttingRequestBoxInfo")
public class CuttingRequestBoxInfoController {

	@Autowired
	private CuttingRequestBoxInfoService service;
	
	@GetMapping
	public List<CuttingRequestBoxInfo> findAll(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift
			){
		return service.findAll(date, shift);
	}
	
}
