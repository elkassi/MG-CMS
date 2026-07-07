package com.lear.MGCMS.controller.ctc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.ctc.FilesHistoryService;
import com.lear.MGCMS.services.ctc.FilesService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.ctc.domain.Files;
import com.lear.ctc.domain.FilesHistory;

@RestController
@RequestMapping("/api/ctcFiles")
public class CtcFilesController {

	@Autowired
	private FilesService service;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private FilesHistoryService filesHistoryService;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	@Value("${lear.pltfolder}")
	private String pltfolder;
	@GetMapping("/all")
	private Page<Files> findAll(
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
	private Files findById(@PathVariable String id) {
		return service.findById(Long.parseLong(id));
	}
	
	@GetMapping("/pn/{partNumberCover}")
	private List<Files> findByPn(@PathVariable String partNumberCover) {
		return service.findByPartNumberCover(partNumberCover);
	}

	@GetMapping("/getPattern")
	private List<String> getPattern(
			@RequestParam(value = "partNumberCoverArray", required = false) List<String> partNumberCoverArray
			) {
		return service.getPattern(partNumberCoverArray);
	}
	
	String[] goodTypes = {
			"fabric",
			 "supplier kit leather",
			 "supplier kit fabric",
			 "CNC"
	};
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody Files obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		User user = userService.findByUsername(authentication.getName());

    	boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_ENGINEERING") || role.getName().equals("ROLE_QUALITE")) {
				authorized = true; break;
			}
		}
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
    	
		if(!obj.getType().toLowerCase().equals("cnc")){
			obj.setType(obj.getType().toLowerCase().trim());
		}
        boolean found = false;
        for (String type : goodTypes) {
            if (type.equals(obj.getType())) {
                found = true;
                break;
            }
        }
        if(!found) {
    		return new ResponseEntity<String>("can't save with the type " + obj.getType(), HttpStatus.BAD_REQUEST);
        }
    	
		if(obj.getId()== null) {
			Files oldObj = service.findByPartNumberCoverAndPanelnumber(obj.getPartNumberCover(), obj.getPanelNumber());
			if(oldObj != null) {
				obj.setId(oldObj.getId());
				obj.setCreatedAt(oldObj.getCreatedAt());
				obj.setAddedBy(oldObj.getAddedBy());
			} else {
//				Long newId = service.findMaxID() + 1;
//				System.out.println("newId "+ newId);
//				obj.setId(newId);
			}
		} 
		
		try {
			String folderPath = pltfolder+"";
			String fileName = obj.getPattern()+".plt";
	        File file = new File(folderPath, fileName);
	        if (file.exists()) {
	        	obj.setPltFound(true);
	        } else {
	        	obj.setPltFound(false);
	        }
		} catch (Exception e) {
			
		}
		
		obj.setUpdatedAt(LocalDateTime.now());
		obj.setUpdatedBy(user.getFirstName() + " " + user.getLastName());


		Files newObj = service.save(obj);
		filesHistoryService.save(new FilesHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "SAVE", newObj.toString()));
		return new ResponseEntity<Files>(newObj, HttpStatus.CREATED);
	}
	
	@GetMapping("/find/{pn}/{pattern}")
	public Files findFirstByPartNumberCoverAndPattern(@PathVariable String pn, @PathVariable String pattern) {
		return service.findFirstByPartNumberCoverAndPattern(pn, pattern);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@RequestBody Files obj, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());
		
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_ENGINEERING")|| role.getName().equals("ROLE_QUALITE")) {
				authorized = true; break;
			}
		}
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		
		service.delete(obj);
		filesHistoryService.save(new FilesHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "DELETE", obj.toString()));
		return new ResponseEntity<String>("Deleted", HttpStatus.CREATED);
	}
	
	@GetMapping(value = "/download/ctcFiles.xlsx")
	public ResponseEntity<Resource> excelCustomersReport(HttpServletResponse response,
			@RequestParam Map<String, String> filters
    		) throws IOException {
    	String filename = "ctcFiles.xlsx";
    	List<Object> arr = new ArrayList<Object>() ;
    	
    	for(Files obj : service.findList(filters)) {
    		arr.add((Object) obj);
    	}
    	System.out.println("arr size : " + arr.size());
    	ByteArrayInputStream in = ExcelHelper.listToExcel(arr, "com.lear.ctc.domain.Files");
        InputStreamResource file = new InputStreamResource(in);
    	
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .body(file);
    }
	
	@PostMapping(value = "/supprimer")
	public ResponseEntity<?> supprimer(@RequestParam Map<String, String> filters, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());

		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_ENGINEERING")|| role.getName().equals("ROLE_QUALITE")) {
				authorized = true; break;
			}
		}
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		
		List<Files> arr = service.findList(filters);
		int endIndex = Math.min(100, arr.size());
		arr= arr.subList(0, endIndex);
		service.deleteAll(arr);
		filesHistoryService.save(new FilesHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "DELETE ALL", arr.size()+" éléments : " + mapToString(filters)));
		return new ResponseEntity<String>("Deleted", HttpStatus.OK); 
	}
	
    public static String mapToString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Append key-value pair to the StringBuilder
            sb.append("\"").append(key).append("\": \"").append(value).append("\", ");
        }

        // Remove the trailing comma and space, if there is at least one entry in the map
        if (!map.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("}");

        return sb.toString();
    }

	
}
