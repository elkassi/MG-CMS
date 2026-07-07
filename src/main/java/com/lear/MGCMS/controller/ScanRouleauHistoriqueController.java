package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.ScanRouleauHistorique;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.ScanRouleauHistoriqueService;
import com.lear.MGCMS.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scanRouleauHistorique")
public class ScanRouleauHistoriqueController {

    @Autowired
    private ScanRouleauHistoriqueService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;

    @GetMapping("/bySerialId")
    public List<ScanRouleauHistorique> findBySerialId(
            @RequestParam(value = "serialId", required = true) String serialId
    ) {
        return service.findBySerialId(serialId);
    }

    @GetMapping("/byDateRange")
    public List<ScanRouleauHistorique> findByDateBetween(
            @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return service.findByDateBetween(startDate, endDate);
    }

    @GetMapping("/bySerialIdAndDateRange")
    public List<ScanRouleauHistorique> findBySerialIdAndDateBetween(
            @RequestParam(value = "serialId", required = true) String serialId,
            @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return service.findBySerialIdAndDateBetween(serialId, startDate, endDate);
    }

//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> save(@RequestBody ScanRouleauHistorique obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        ScanRouleauHistorique newObj = service.save(obj);
        return new ResponseEntity<ScanRouleauHistorique>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public List<ScanRouleauHistorique> findAll() {
        return service.findAll();
    }

    @GetMapping("/all")
    public Page<ScanRouleauHistorique> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "date,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sortBy);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Integer id) {
        ScanRouleauHistorique obj = service.findById(id);
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<ScanRouleauHistorique>(obj, HttpStatus.OK);
    }

//    @PostMapping("/delete")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@Valid @RequestBody ScanRouleauHistorique obj, Authentication authentication) {
        service.delete(obj);
        return new ResponseEntity<String>("Deleted", HttpStatus.OK);
    }
}
