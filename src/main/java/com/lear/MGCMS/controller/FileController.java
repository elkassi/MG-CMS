package com.lear.MGCMS.controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lear.MGCMS.storage.StorageService;
import com.sun.mail.iap.Response;


@RestController
@RequestMapping("/api/file")
public class FileController {
	
	private final StorageService storageService;
	
	@Autowired
	public FileController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/{filename:.+}")
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}
	
	@PostMapping("/store")
	public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file, Authentication authentication) {
		String name = authentication.getName()+ "-" + (new SimpleDateFormat("dd_MM_yyyy,hh_mm_ss")).format(new Date())+ "-"+file.getOriginalFilename();
		try {
			storageService.store(file, name);
			return new ResponseEntity<String>(name, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<String>("file error : " + e.getMessage(), HttpStatus.UNAUTHORIZED); 
		}
		//		if(file.getOriginalFilename().endsWith(".png") || file.getOriginalFilename().endsWith(".xlsx") || file.getOriginalFilename().endsWith(".pdf") || file.getOriginalFilename().endsWith(".jpg")) {

//		}
		
	}
	
	
	
}
