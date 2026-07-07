package com.lear.MGCMS.controller;

import com.lear.MGCMS.services.cms.OrderScheduleService;
import com.lear.cms.domain.OrderSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orderSchedule")
public class OrderScheduleController {

    @Autowired
    private OrderScheduleService service;

    @GetMapping("/findByDateAndShift")
    public List<OrderSchedule> findByDateAndShift(
            @RequestParam("dateDemande") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDemande,
            @RequestParam("shiftDemande") String shiftDemande) {
        return service.findByDateAndShift(dateDemande, shiftDemande);
    }

}
