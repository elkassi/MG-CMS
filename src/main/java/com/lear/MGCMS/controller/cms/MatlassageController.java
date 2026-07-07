package com.lear.MGCMS.controller.cms;

import com.lear.MGCMS.services.cms.MatlassageService;
import com.lear.cms.domain.Matlassage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cms/matlassage")
public class MatlassageController {

    @Autowired
    private MatlassageService service;

    @GetMapping("/sequence/{sequence}")
    public List<Matlassage> findBySequence(@PathVariable String sequence) {
        return service.findBySequence(sequence);
    }

}
