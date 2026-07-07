package com.lear.MGCMS.controller.cms;

import com.lear.MGCMS.services.cms.SuiviCoupeService;
import com.lear.cms.domain.Coupe;
import com.lear.cms.domain.SuiviCoupe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cms/suiviCoupe")
public class SuiviCoupeController {

    @Autowired
    private SuiviCoupeService service;

    @GetMapping("/sequence/{nof}")
    public List<SuiviCoupe> findCoupeByNof(@PathVariable String nof) {
        return service.findByNof(nof);
    }


}
