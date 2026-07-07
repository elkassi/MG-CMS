package com.lear.MGCMS.controller.cms;

import com.lear.MGCMS.services.cms.SuiviMatelassageService;
import com.lear.cms.domain.SuiviMatelassage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cms/suiviMatelassage")
public class SuiviMatelassageController {
    @Autowired
    private SuiviMatelassageService service;

    @GetMapping("/sequence/{nof}")
    public List<SuiviMatelassage> findMatelassageByNof(@PathVariable String nof) {
        return service.findByNof(nof);
    }

}
