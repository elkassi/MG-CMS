package com.lear.MGCMS.controller.cms;

import com.lear.MGCMS.services.cms.ProduitFinitService;
import com.lear.cms.domain.ProduitFinit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cms/produitFinit")
public class ProduitFinitController {

    @Autowired
    private ProduitFinitService service;

    @GetMapping("/sequence/{sequence}")
    public List<ProduitFinit> findBySequence(@PathVariable String sequence) {
        return service.findBySequence(sequence);
    }

    @GetMapping("/date/{date}")
    public List<String> findAllSequenceLike(@PathVariable String date) {
        return service.findAllSequenceLike(date);
    }


    @GetMapping("/{id}")
    public ProduitFinit findById(@PathVariable String id) {
        return service.findById(Long.parseLong(id));
    }

}
