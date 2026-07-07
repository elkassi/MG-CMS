package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.ScanRouleau;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.ScanRouleau;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.ScanRouleauService;
import com.lear.MGCMS.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scanRouleau")
public class ScanRouleauController {

    @Autowired
    private ScanRouleauService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;


    // pass in param list of locations and then get the list
    @GetMapping("/byLocations")
    public List<ScanRouleau> findByLocations(
            @RequestParam(value = "locations", required = true) List<String> locations
    ) {
        return service.findByLocations(locations);
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody ScanRouleau obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        ScanRouleau newObj = service.save(obj);
        return new ResponseEntity<ScanRouleau>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public List<ScanRouleau> findAll() {
        return service.findAll();
    }

    @GetMapping("/all")
    public Page<ScanRouleau> findAll(
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
        ScanRouleau obj = service.findById(id);
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<ScanRouleau>(obj, HttpStatus.OK);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@Valid @RequestBody ScanRouleau obj, Authentication authentication) {
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
        service.delete(obj);
        return new ResponseEntity<String>("Deleted", HttpStatus.OK);
    }



}
