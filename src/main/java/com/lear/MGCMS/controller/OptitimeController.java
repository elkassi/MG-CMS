package com.lear.MGCMS.controller;

import com.lear.MGCMS.services.OptitimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/optitime")
public class OptitimeController {

    @Autowired
    private OptitimeService service;


    @GetMapping("/listNames")
    public List<String> getListNames(
            @RequestParam("sec") String sec) {
        return service.getListNames(sec);
    }

    @GetMapping("/code")
    public String getFullName(
            @RequestParam("code") String code) {
        return service.getFullName(code);
    }

    @GetMapping("/matricule")
    public String getFullNameByMatricule(
            @RequestParam("matricule") String matricule) {
        return service.getFullNameByMatricule(matricule);
    }


}
