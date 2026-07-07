package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CncMachineReport;
import com.lear.MGCMS.domain.CncMachineReportPiece;
import com.lear.MGCMS.services.CncSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cncSync")
@PreAuthorize("hasRole('ADMIN') or hasRole('CNC_PS') or hasRole('CNC_CONTROL') or hasRole('PROCESS') or hasRole('ENGINEERING')")
public class CncSyncController {

    @Autowired
    private CncSyncService syncService;

    /**
     * Export reference data (machines, programs, distributions) as JSON
     * for CMS-CNC to import.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportForCnc() {
        try {
            byte[] data = syncService.exportForCnc();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDisposition(
                    ContentDisposition.attachment().filename("mgcms-cnc-export.json").build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Import session data from CMS-CNC export file.
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROCESS') or hasRole('ENGINEERING')")
    public ResponseEntity<?> importFromCnc(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            String username = authentication.getName();
            Map<String, Object> result = syncService.importFromCnc(file, username);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Import failed: " + e.getMessage());
        }
    }

    /**
     * List imported CNC machine reports (paginated, filterable).
     */
    @GetMapping("/reports/all")
    public Page<CncMachineReport> findAllReports(
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

    /**
     * Get pieces for a specific report.
     */
    @GetMapping("/reports/{reportId}/pieces")
    public List<CncMachineReportPiece> getReportPieces(@PathVariable Long reportId) {
        return syncService.findPiecesByReport(reportId);
    }
}
