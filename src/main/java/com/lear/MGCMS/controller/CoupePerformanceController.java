package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CoupeMachineHistory;
import com.lear.MGCMS.domain.CoupePerformance;
import com.lear.MGCMS.services.CoupePerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupePerformance")
public class CoupePerformanceController {

    @Autowired
    private CoupePerformanceService service;

    // we need to create a post method to save a List of CoupePerformance come in the request body
    @PostMapping("/saveList")
    public ResponseEntity<?> saveList(@RequestBody List<CoupePerformance> coupePerformanceList) {
        service.saveAll(coupePerformanceList);
        return ResponseEntity.ok("List saved");
    }

    @GetMapping("/all")
    public Page<CoupePerformance> findAll(
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
        return service.findAll(filters, page, size, sortBy);
    }

    @GetMapping("/filter")
    public List<CoupePerformance> findFilter(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift,
            @RequestParam(value = "machines", required = false) List<String> machines) {

        return service.findFilter(date, shift, machines);
    }


}
