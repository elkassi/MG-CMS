package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.ValidationQLaize;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.ValidationQLaizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/validationQLaize")
public class ValidationQLaizeController {

    @Autowired
    private ValidationQLaizeService service;

    @Autowired
    private UserService userService;

    @GetMapping("/all")
    public Page<ValidationQLaize> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sortBy);
    }

    @GetMapping("/list")
    public List<ValidationQLaize> findAllList() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ValidationQLaize> findById(@PathVariable Long id) {
        Optional<ValidationQLaize> validation = service.findById(id);
        if (validation.isPresent()) {
            return ResponseEntity.ok(validation.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ValidationQLaize validationQLaize, Authentication authentication) {
        if(validationQLaize.getLaizeReel() == null || validationQLaize.getLaizeReel() <= 0 || validationQLaize.getLaizeReel() >= 3) {
            return ResponseEntity.badRequest().body("Laize Reel must be between 0 and 3.");
        }

        User user = userService.findByUsername(authentication.getName());
        validationQLaize.setValidatedBy(user.getLastName() + " " + user.getFirstName() + " : " + user.getMatricule());
        if (validationQLaize.getValidationDate() == null) {
            validationQLaize.setValidationDate(LocalDateTime.now());
        }
        return ResponseEntity.ok(service.save(validationQLaize));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ValidationQLaize> update(
            @PathVariable Long id,
            @RequestBody ValidationQLaize validationQLaize) {
        Optional<ValidationQLaize> existing = service.findById(id);
        if (existing.isPresent()) {
            validationQLaize.setId(id);
            return ResponseEntity.ok(service.save(validationQLaize));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<ValidationQLaize> existing = service.findById(id);
        if (existing.isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // List<ValidationQLaize> getStockQLaize()
    @GetMapping("/stockQLaize")
    public List<ValidationQLaize> getStockQLaize() {
        return service.getStockQLaize();
    }
}
