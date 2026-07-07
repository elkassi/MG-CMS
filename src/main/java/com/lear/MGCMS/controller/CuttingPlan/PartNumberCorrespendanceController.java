package com.lear.MGCMS.controller.CuttingPlan;

import com.lear.MGCMS.domain.CuttingPlan.PartNumberCorrespendance;
import com.lear.MGCMS.domain.FirstCheckConfig;
import com.lear.MGCMS.services.CuttingPlan.PartNumberCorrespendanceService;
import com.lear.MGCMS.services.FirstCheckConfigService;
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
@RequestMapping("/api/partNumberCorrespendance")
public class PartNumberCorrespendanceController {

    @Autowired
    private PartNumberCorrespendanceService service;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @GetMapping("/all")
    public Page<PartNumberCorrespendance> findAll(
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

    // find the first one by partNumber and partNumberCorrespondance
    @GetMapping("/find")
    public ResponseEntity<?> findByPartNumberAndPartNumberCorrespondance(
            @RequestParam String partNumber,
            @RequestParam String partNumberCorrespondance
    ) {
        PartNumberCorrespendance obj = service.findByPartNumberAndPartNumberCorrespondance(partNumber, partNumberCorrespondance);
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<PartNumberCorrespendance>(obj, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findByCode(@PathVariable String id)  {
        PartNumberCorrespendance obj = service.findById(Long.parseLong(id));
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<PartNumberCorrespendance>(obj, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody PartNumberCorrespendance obj, BindingResult result) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        PartNumberCorrespendance newObj = service.save(obj);
        return new ResponseEntity<PartNumberCorrespendance>(newObj, HttpStatus.CREATED);
    }




    @PostMapping("/delete")
    public void delete(@RequestBody PartNumberCorrespendance obj) {
        service.delete(obj);
    }

    @GetMapping("/byPartNumbers")
    public List<PartNumberCorrespendance> findByPartNumbers(
            @RequestParam List<String> partNumbers
    ) {
        return service.findByPartNumbers(partNumbers);
    }

}
