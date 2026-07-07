package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.ProgramCNC;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.ProgramCNCService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.utils.ExcelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/programCNC")
public class ProgramCNCController {

    @Autowired
    private ProgramCNCService programCNCService;

    @Autowired
    private UserService userService;

    /**
     * Save or update a ProgramCNC entry.
     * Allowed for ROLE_ADMIN, ROLE_PROCESS, and ROLE_ENGINEERING.
     */
    @PostMapping
    public ResponseEntity<?> save(@RequestBody ProgramCNC programCNC, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        if (!isAuthorized(user)) {
            return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        ProgramCNC saved = programCNCService.save(programCNC, displayName(user));
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    /**
     * Get all ProgramCNC entries (paginated, filterable) for EntityList.
     */
    @GetMapping("/all")
    public Page<ProgramCNC> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy) {
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return programCNCService.findAll(filters, page, size, sortBy);
    }

    /**
     * Get a single ProgramCNC by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        ProgramCNC obj = programCNCService.findById(id);
        if (obj == null) {
            return new ResponseEntity<>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(obj, HttpStatus.OK);
    }

    /**
     * Export ProgramCNC entries to Excel.
     */
    @GetMapping(value = "/download/programCNC.xlsx")
    public ResponseEntity<Resource> downloadExcel(@RequestParam Map<String, String> filters) throws IOException {
        String filename = "programCNC.xlsx";
        List<Object> arr = new ArrayList<>();
        for (ProgramCNC obj : programCNCService.findList(filters)) {
            arr.add(obj);
        }
        ByteArrayInputStream in = ExcelHelper.listToExcel(arr, "com.lear.MGCMS.domain.ProgramCNC");
        InputStreamResource file = new InputStreamResource(in);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

    /**
     * Delete a ProgramCNC entry.
     * Allowed for ROLE_ADMIN, ROLE_PROCESS, and ROLE_ENGINEERING.
     */
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody ProgramCNC programCNC, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        if (!isAuthorized(user)) {
            return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        programCNCService.delete(programCNC, displayName(user));
        return new ResponseEntity<>("Deleted", HttpStatus.OK);
    }

    /**
     * Bulk delete ProgramCNC rows matching the dependency filters (EntityList "Supprimer" button).
     * Allowed for ROLE_ADMIN, ROLE_PROCESS, and ROLE_ENGINEERING.
     */
    @PostMapping("/supprimer")
    public ResponseEntity<?> supprimer(@RequestParam Map<String, String> filters, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        if (!isAuthorized(user)) {
            return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        // Safety: never allow an unfiltered call to wipe the whole table.
        if (filters == null || filters.isEmpty()) {
            return new ResponseEntity<>("Aucun filtre fourni", HttpStatus.BAD_REQUEST);
        }
        List<ProgramCNC> arr = programCNCService.findList(filters);
        programCNCService.deleteAll(arr, displayName(user));
        return new ResponseEntity<>("Deleted " + arr.size(), HttpStatus.OK);
    }

    private boolean isAuthorized(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        for (Role role : user.getRoles()) {
            String name = role.getName();
            if ("ROLE_ADMIN".equals(name) || "ROLE_PROCESS".equals(name) || "ROLE_ENGINEERING".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String displayName(User user) {
        String last = user.getLastName() != null ? user.getLastName() : "";
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        return (last + " " + first).trim();
    }
}
