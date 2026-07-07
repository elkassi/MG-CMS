package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.LaminationPls;
import com.lear.MGCMS.services.LaminationPlsService;
import com.lear.MGCMS.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lamination-pls")
public class LaminationPlsController {

    @Autowired
    private LaminationPlsService service;

    @Autowired
    private QueryService queryService;

    @GetMapping("/filter")
    public ResponseEntity<?> filter(
            @RequestParam(value = "partNumberMaterial", required = false) String partNumberMaterial,
            @RequestParam(value = "sequence", required = false) String sequence
    ) {
        List<String> pns = queryService.getPartNumbersBySequence(sequence);
        int countCtc = queryService.countCtcByPnsAndReftissu(pns, partNumberMaterial);
        if(countCtc == 0) {
            return new ResponseEntity<String>("No data found", HttpStatus.NOT_FOUND);
        }
        LaminationPls obj = service.filter(partNumberMaterial);
        if(obj == null) {
            return new ResponseEntity<String>("No data found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<LaminationPls>(obj, HttpStatus.OK);
    }

}
