package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.MachineDxfRapport;
import com.lear.MGCMS.services.MachineDxfRapportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/machineDxfRapport")
public class MachineDxfRapportController {

    @Autowired
    private MachineDxfRapportService service;

    @GetMapping("/all")
    public Page<MachineDxfRapport> findAll(
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



}
