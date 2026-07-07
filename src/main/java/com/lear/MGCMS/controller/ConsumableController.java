package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.Consumable;
import com.lear.MGCMS.domain.Intervention;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.ConsumableService;
import com.lear.MGCMS.services.InterventionService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consumable")
public class ConsumableController {

    @Autowired
    private ConsumableService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;
    @Autowired
    private UserService userService;

    @GetMapping("/all")
    public Page<Consumable> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "date,desc", required = false) String sortBy
    ) {
        System.out.println("ConsumableController.findAll");
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page,size,sortBy);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> findByCode(@PathVariable String id)  {
        Consumable obj = service.findByObjId(id);
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Consumable>(obj, HttpStatus.OK);
    }

    @GetMapping("/stat")
    public List<?> getStat(
            @RequestParam(value = "type", required = true) String type
    ) {
        return service.getStat(type);
    }



}
