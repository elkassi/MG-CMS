package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.DefautRouleau;
import com.lear.MGCMS.services.DefautRouleauService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/defautRouleau")
public class DefautRouleauController {

    @Autowired
    private DefautRouleauService service;

    @GetMapping("/list")
    public List<DefautRouleau> findAll() {
        return service.findAllActive();
    }


}
