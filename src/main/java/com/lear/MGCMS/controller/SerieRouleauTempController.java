package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.services.SerieRouleauTempService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/serieRouleauTemp")
public class SerieRouleauTempController {

    @Autowired
    private SerieRouleauTempService service;

    @GetMapping("/en-cours")
    public List<SerieRouleauTemp> getEnCours(
            @RequestParam(value = "reftissu", required = false) String reftissu
    ) {
        return service.getEnCours(reftissu);
    }

    @GetMapping("/all")
    public List<SerieRouleauTemp> getAll() {
        return service.getAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SerieRouleauTemp> save(@RequestBody SerieRouleauTemp serieRouleauTemp) {
        serieRouleauTemp.setDate(LocalDateTime.now());
        return new ResponseEntity<SerieRouleauTemp>(service.save(serieRouleauTemp), HttpStatus.CREATED);
    }

    @DeleteMapping("/{tableMatelassage}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String tableMatelassage) {
        service.deleteByid(tableMatelassage);
    }





}
