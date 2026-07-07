package com.lear.MGCMS.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.SystemHealthService;

/**
 * ROLE_ADMIN diagnostics: database health, expensive queries (+ login), and host
 * CPU/RAM/disk/network. {@code /report} returns a Markdown file to hand to support.
 */
@RestController
@RequestMapping("/api/admin/systemHealth")
@PreAuthorize("hasRole('ADMIN')")
public class SystemHealthController {

    private final SystemHealthService service;

    public SystemHealthController(SystemHealthService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return service.status();
    }

    @GetMapping(value = "/report", produces = "text/markdown")
    public ResponseEntity<byte[]> report(@RequestParam(defaultValue = "60") int minutes) {
        byte[] body = service.report(minutes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + service.reportFileName() + "\"")
                .contentType(MediaType.parseMediaType("text/markdown"))
                .body(body);
    }
}
