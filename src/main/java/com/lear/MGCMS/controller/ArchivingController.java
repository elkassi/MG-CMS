package com.lear.MGCMS.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.ArchivingService;

/**
 * ROLE_ADMIN archiving of aged production rows into same-DB {@code <table>_archive}
 * copies. {@code /preview} is a no-op dry-run; {@code /run} performs the move.
 */
@RestController
@RequestMapping("/api/admin/archiving")
@PreAuthorize("hasRole('ADMIN')")
public class ArchivingController {

    private final ArchivingService service;

    public ArchivingController(ArchivingService service) {
        this.service = service;
    }

    @GetMapping("/inventory")
    public Map<String, Object> inventory() {
        return service.inventory();
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(@RequestParam String table, @RequestParam String beforeDate) {
        return service.preview(table, beforeDate);
    }

    @PostMapping("/run")
    public Map<String, Object> run(@RequestParam String table, @RequestParam String beforeDate) {
        return service.run(table, beforeDate);
    }
}
