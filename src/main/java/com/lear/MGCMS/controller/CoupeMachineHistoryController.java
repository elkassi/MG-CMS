package com.lear.MGCMS.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.lear.MGCMS.services.CoupeMachineHistoryService;

@RestController
@RequestMapping("/api/coupeMachineHistory")
public class CoupeMachineHistoryController {

    @Autowired
    private CoupeMachineHistoryService service;


    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CoupeMachineHistory obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        CoupeMachineHistory newObj = service.save(obj);
        return new ResponseEntity<CoupeMachineHistory>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public Page<CoupeMachineHistory> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "lineDate,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sortBy);
    }


    @GetMapping("/filter")
    public List<CoupeMachineHistory> findBetween(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift,
            @RequestParam(value = "machines", required = false) List<String> machines) {
        LocalDateTime startDate = null, endDate = null;
        if (shift.equals("2")) {
            startDate = date.atTime(05, 55);
            if (date.getDayOfWeek().getValue() == 5) {
                endDate = date.atTime(13, 30);
            } else {
                endDate = date.atTime(13, 45);
            }
        } else if (shift.equals("3")) {
            if (date.getDayOfWeek().getValue() == 5) {
                startDate = date.atTime(14, 05);
            } else {
                startDate = date.atTime(13, 55);
            }
            endDate = date.atTime(21, 45);
        } else {
            startDate = date.atTime(21, 55).minusDays(1);
            endDate = date.atTime(05, 45);
        }
        if (endDate.compareTo(LocalDateTime.now()) > 0) {
            endDate = LocalDateTime.now();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(startDate.format(formatter) + " => " + endDate.format(formatter) + " : " + machines.size());

        return service.findBetween(startDate, endDate, machines);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody CoupeMachineHistory obj, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        service.delete(obj);
        return new ResponseEntity<String>("Done", HttpStatus.OK);
    }

}
