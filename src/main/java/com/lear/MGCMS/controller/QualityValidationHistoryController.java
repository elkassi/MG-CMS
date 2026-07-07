package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CodeScrap;
import com.lear.MGCMS.domain.QualityValidationHistory;
import com.lear.MGCMS.services.QualityValidationHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/qualityValidationHistory")
public class QualityValidationHistoryController {

    @Autowired
    private QualityValidationHistoryService service;

    @GetMapping("/all")
    public Page<QualityValidationHistory> findAll(
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
        return service.findAll(filters, page,size,sortBy);
    }
}
