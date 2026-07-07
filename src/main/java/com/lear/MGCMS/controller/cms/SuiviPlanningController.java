package com.lear.MGCMS.controller.cms;

import com.lear.MGCMS.services.cms.SuiviPlanningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cms/suiviPlanning")
public class SuiviPlanningController {

    @Autowired
    private SuiviPlanningService service;

    @GetMapping("/{sequence}")
    public ResponseEntity<?> findBySequence(@PathVariable String sequence) {
        return ResponseEntity.ok(service.findBySequence(sequence));
    }

}
