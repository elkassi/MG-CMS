package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.MaintenanceIntervention;
import com.lear.MGCMS.domain.MaintenanceInterventionConfig;
import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.services.MaintenanceInterventionConfigService;
import com.lear.MGCMS.services.MapValidationErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenanceInterventionConfig")
public class MaintenanceInterventionConfigController {

    @Autowired
    private MaintenanceInterventionConfigService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @GetMapping("/all")
    public Page<MaintenanceInterventionConfig> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sort) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sort);
    }

    @GetMapping("/list")
    public List<MaintenanceInterventionConfig> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        MaintenanceInterventionConfig obj = service.findById(id);
        return new ResponseEntity<MaintenanceInterventionConfig>(obj, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody MaintenanceInterventionConfig obj, BindingResult result) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) {
            return errorMap;
        }
        MaintenanceInterventionConfig newObj = service.save(obj);
        return new ResponseEntity<MaintenanceInterventionConfig>(newObj, HttpStatus.CREATED);
    }

}
