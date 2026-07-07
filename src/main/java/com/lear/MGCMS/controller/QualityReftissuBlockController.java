package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.AuditQualiteConfig;
import com.lear.MGCMS.domain.QualityReftissuBlock;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.QualityReftissuBlockService;
import com.lear.MGCMS.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/qualityReftissuBlock")
public class QualityReftissuBlockController {

    @Autowired
    private QualityReftissuBlockService service;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;
    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('QUALITE')")
    public ResponseEntity<?> save(@Valid @RequestBody QualityReftissuBlock obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        User user = userService.findByUsername(authentication.getName());

//        QualityReftissuBlock oldObj = service.findByReftissu(obj.getReftissu());
//        if(oldObj != null) {
//            return new ResponseEntity<String>("Reftissu already exists", HttpStatus.BAD_REQUEST);
//        }
        obj.setDate(LocalDateTime.now());
        obj.setCreatedBy(user.getLastName() + " " + user.getFirstName() + " ("+user.getMatricule()+ ")");
        QualityReftissuBlock newObj = service.save(obj);
        return new ResponseEntity<QualityReftissuBlock>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public List<QualityReftissuBlock> findAll() {
        return service.findAll();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('QUALITE')")
    public void delete(@RequestBody QualityReftissuBlock obj) {
        service.delete(obj);
    }

}
