package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CncPsLeatherConsumption;
import com.lear.MGCMS.repositories.CncPsLeatherConsumptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cncPsLeatherConsumption")
public class CncPsLeatherConsumptionController {

    @Autowired
    private CncPsLeatherConsumptionRepository repository;

    @GetMapping("/all")
    public Page<CncPsLeatherConsumption> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] sortParams = sort.split(",");
        String sortProp = sortParams[0];
        String sortDir = sortParams.length > 1 ? sortParams[1] : "desc";
        Pageable pageable = PageRequest.of(page, size,
                sortDir.equals("asc") ? Sort.by(sortProp).ascending() : Sort.by(sortProp).descending());
        return repository.findAll(pageable);
    }
}
