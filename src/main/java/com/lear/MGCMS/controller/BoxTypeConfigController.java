package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.BoxTypeConfig;
import com.lear.MGCMS.services.BoxTypeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/boxTypeConfig")
public class BoxTypeConfigController {

    @Autowired
    private BoxTypeConfigService boxTypeConfigService;

    @GetMapping("/list")
    public List<BoxTypeConfig> list() {
        return boxTypeConfigService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoxTypeConfig> findById(@PathVariable Long id) {
        Optional<BoxTypeConfig> boxTypeConfig = boxTypeConfigService.findById(id);
        return boxTypeConfig.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/byBoxType/{boxType}")
    public ResponseEntity<BoxTypeConfig> findByBoxType(@PathVariable String boxType) {
        Optional<BoxTypeConfig> boxTypeConfig = boxTypeConfigService.findByBoxType(boxType);
        return boxTypeConfig.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BoxTypeConfig> create(@RequestBody BoxTypeConfig boxTypeConfig) {
        if (boxTypeConfig.getBoxType() == null || boxTypeConfig.getEmptyBoxWeight() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(boxTypeConfigService.save(boxTypeConfig));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoxTypeConfig> update(@PathVariable Long id, @RequestBody BoxTypeConfig boxTypeConfig) {
        Optional<BoxTypeConfig> existing = boxTypeConfigService.findById(id);
        if (existing.isPresent()) {
            boxTypeConfig.setId(id);
            return ResponseEntity.ok(boxTypeConfigService.save(boxTypeConfig));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<BoxTypeConfig> existing = boxTypeConfigService.findById(id);
        if (existing.isPresent()) {
            boxTypeConfigService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
