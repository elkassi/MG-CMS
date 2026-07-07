package com.lear.MGCMS.controller.scanCoupe;

import com.lear.MGCMS.domain.scanCoupe.Config;
import com.lear.MGCMS.services.scanCoupe.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/code")
    @PreAuthorize("hasRole('QUALITE')")
    public ResponseEntity<?> getCode() {
        Config config = configService.findByParam("codeQualite");
        if(config == null) {
            return ResponseEntity.badRequest().body("Not found");
        }
        return ResponseEntity.ok(config.getValue());
    }

}
