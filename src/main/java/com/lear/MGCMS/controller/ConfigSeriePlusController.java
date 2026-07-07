package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.ConfigSeriePlus;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.ConfigSeriePlusService;
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
@RequestMapping("/api/configSeriePlus")
public class ConfigSeriePlusController {

    @Autowired
    private ConfigSeriePlusService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @GetMapping("/all")
    public Page<ConfigSeriePlus> findAll(
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

    @GetMapping("/list")
    public List<ConfigSeriePlus> findList() {
        return service.findAll();
    }

    @GetMapping("/byPartNumberMaterialArr")
    public List<ConfigSeriePlus> findByPartNumberMaterialArr(@RequestParam List<String> arr) {
        return service.findByPartNumberMaterialArr(arr);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findByCode(@PathVariable String id)  {
        ConfigSeriePlus obj = service.findById(Long.parseLong(id));
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<ConfigSeriePlus>(obj, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody ConfigSeriePlus obj, BindingResult result) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        ConfigSeriePlus newObj = service.save(obj);
        return new ResponseEntity<ConfigSeriePlus>(newObj, HttpStatus.CREATED);
    }



}
