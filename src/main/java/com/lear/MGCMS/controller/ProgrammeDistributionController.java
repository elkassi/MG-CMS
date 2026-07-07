package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.ProgrammeDistribution;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.ProgrammeDistributionService;
import com.lear.MGCMS.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/programmeDistribution")
public class ProgrammeDistributionController {

    @Autowired
    private ProgrammeDistributionService service;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> save(@RequestBody ProgrammeDistribution programmeDistribution, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_PROCESS") || role.getName().equals("ROLE_ENGINEERING")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        ProgrammeDistribution saved = service.save(programmeDistribution);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public Page<ProgrammeDistribution> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy) {
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
        Optional<ProgrammeDistribution> opt = service.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody ProgrammeDistribution programmeDistribution, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_PROCESS") || role.getName().equals("ROLE_ENGINEERING")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        service.delete(programmeDistribution);
        return new ResponseEntity<>("Deleted", HttpStatus.OK);
    }
}
