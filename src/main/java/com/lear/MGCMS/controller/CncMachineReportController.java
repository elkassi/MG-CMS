package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CncMachineReport;
import com.lear.MGCMS.services.CncSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cncMachineReport")
@PreAuthorize("hasRole('ADMIN') or hasRole('CNC_PS') or hasRole('CNC_CONTROL') or hasRole('PROCESS') or hasRole('ENGINEERING')")
public class CncMachineReportController {

    @Autowired
    private CncSyncService syncService;

    @GetMapping("/all")
    public Page<CncMachineReport> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "importedAt,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return syncService.findAllReports(filters, page, size, sortBy);
    }
}
