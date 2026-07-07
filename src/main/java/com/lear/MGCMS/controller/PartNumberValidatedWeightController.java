package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.PartNumberValidatedWeight;
import com.lear.MGCMS.services.PartNumberValidatedWeightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partNumberValidatedWeight")
public class PartNumberValidatedWeightController {

    @Autowired
    private PartNumberValidatedWeightService service;

    @GetMapping("/list")
    @PreAuthorize("hasRole('PROCESS')")
    public List<PartNumberValidatedWeight> list() {
        return service.findAll();
    }

    @GetMapping("/partnumber/{partnumber}")
    public ResponseEntity<?> findByPartnumber(@PathVariable String partnumber) {
        PartNumberValidatedWeight latest = service.findLatestByPartnumber(partnumber);
        if (latest == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("partnumber", partnumber);
            response.put("found", false);
            response.put("message", "Aucun poids validé trouvé pour ce partnumber");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(latest, HttpStatus.OK);
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('PROCESS')")
    public ResponseEntity<?> save(@RequestBody PartNumberValidatedWeight obj, Authentication authentication) {
        obj.setValidatedBy(authentication.getName());
        PartNumberValidatedWeight saved = service.save(obj);
        return new ResponseEntity<>(saved, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROCESS')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
