package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.CuttingPlan.PartNumberCorrespendance;
import com.lear.MGCMS.domain.ReftissuProperty;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.ReftissuPropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reftissuProperty")
public class ReftissuPropertyController {

    @Autowired
    private ReftissuPropertyService service;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @GetMapping("/all")
    public Page<ReftissuProperty> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "reftissu,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sortBy);
    }

    @GetMapping("/list")
    public List<ReftissuProperty> findAllByReftissuAndProperty(
            @RequestParam(value = "reftissu", required = false) String reftissu,
            @RequestParam(value = "property", required = false) String property

    ) {
        return service.findAllByReftissuAndProperty(reftissu, property);
    }


    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody ReftissuProperty obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;
        ReftissuProperty objSave = service.save(obj);
        return ResponseEntity.ok(objSave);
    }

    @PostMapping("/delete")
    public void delete(@RequestBody ReftissuProperty obj) {
        service.delete(obj);
    }


}
