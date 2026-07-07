package com.lear.MGCMS.controller.cms;

import com.lear.MGCMS.payload.PayloadTemp;
import com.lear.MGCMS.services.cms.GammeTechniqueCMSService;
import com.lear.cms.domain.GammeTechnique;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gammeTechniqueCMS")
public class GammeTechniqueCMSController {

    @Autowired
    private GammeTechniqueCMSService service;

    @PostMapping("/getByPartNumber")
    public List<PayloadTemp> getByPartNumber(@RequestBody List<String> partNumbers) {
        return service.getByPartNumber(partNumbers);
    }

}
