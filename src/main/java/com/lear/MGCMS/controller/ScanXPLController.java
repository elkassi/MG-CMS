package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.ScanXPL;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.ScanXPLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scanXPL")
public class ScanXPLController {

    @Autowired
    private ScanXPLService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @GetMapping("/list")
    public List<ScanXPL> findAll() {
        return service.findAll();
    }

    @GetMapping("/all")
    public Page<ScanXPL> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "scanDate,desc", required = false) String sortBy
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
    public ResponseEntity<?> findById(@PathVariable Long id) {
        ScanXPL obj = service.findById(id);
        if (obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<ScanXPL>(obj, HttpStatus.OK);
    }

    @GetMapping("/serie/{serie}")
    public List<ScanXPL> findBySerie(@PathVariable String serie) {
        return service.findBySerie(serie);
    }

    @GetMapping("/machine/{machine}")
    public List<ScanXPL> findByMachine(@PathVariable String machine) {
        return service.findByMachine(machine);
    }

    @GetMapping("/bySerieIn")
    public List<ScanXPL> findBySerieIn(@RequestParam List<String> series) {
        return service.findBySerieIn(series);
    }

    @GetMapping("/byMachinesAndDateRange")
    public List<ScanXPL> findByMachinesAndDateRange(
            @RequestParam List<String> machines,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return service.findByMachinesAndDateRange(machines, start, end);
    }

    @GetMapping("/distinctSeriesByMachinesAndDateRange")
    public List<String> findDistinctSeriesByMachinesAndDateRange(
            @RequestParam List<String> machines,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return service.findDistinctSeriesByMachinesAndDateRange(machines, start, end);
    }
}
