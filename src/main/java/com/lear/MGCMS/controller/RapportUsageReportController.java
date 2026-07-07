package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.RapportUsageReport;
import com.lear.MGCMS.services.RapportUsageReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rapportUsageReport")
public class RapportUsageReportController {

    @Autowired
    private RapportUsageReportService service;

    @GetMapping("/all")
    public Page<RapportUsageReport> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "cuttingRequest_sequence,asc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sortBy);
    }

    /** Persist the date/shift rows the /rapportUsage page just received and enriched. */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody List<RapportUsageReport> rows) {
        int count = service.saveBatch(rows);
        Map<String, Object> res = new HashMap<>();
        res.put("count", count);
        return res;
    }
}
