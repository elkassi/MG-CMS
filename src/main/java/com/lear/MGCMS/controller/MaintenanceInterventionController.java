package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.MaintenanceIntervention;
import com.lear.MGCMS.services.MaintenanceInterventionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenanceIntervention")
public class MaintenanceInterventionController {

    @Autowired
    private MaintenanceInterventionService service;

    @GetMapping("/all")
    public Page<MaintenanceIntervention> findAll(
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

    @GetMapping("/listByMachine")
    public List<MaintenanceIntervention> listByMachine(
            @RequestParam(name = "machines") List<String> machines) {
        return service.listByMachine(machines);
    }
}
