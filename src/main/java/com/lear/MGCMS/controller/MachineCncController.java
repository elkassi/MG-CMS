package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.MachineCnc;
import com.lear.MGCMS.services.MachineCncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/machineCnc")
@PreAuthorize("hasRole('ADMIN') or hasRole('CNC_CONTROL') or hasRole('QUALITE')")
public class MachineCncController {

    @Autowired
    private MachineCncService service;

    @GetMapping("/list")
    public List<MachineCnc> getAll() {
        return service.findAll();
    }

    @GetMapping("/all")
    public Page<MachineCnc> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page,size,sortBy);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<MachineCnc> opt = service.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> save(@RequestBody MachineCnc machine) {

        MachineCnc old = service.findByName(machine.getName());

        if(machine.getId() == null && old != null) {
            return new ResponseEntity<String>("Not Found", HttpStatus.BAD_REQUEST);
        }
        if(machine.getId() != null && !machine.getId().equals(old.getId())) {
            return new ResponseEntity<String>("Not Found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<MachineCnc>(service.save(machine), HttpStatus.OK);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@RequestBody MachineCnc machine) {
        service.delete(machine);
        return ResponseEntity.ok().build();
    }
}
