package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.FirstCheck;
import com.lear.MGCMS.domain.FirstCheckMachineStopped;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.FirstCheckMachineStoppedService;
import com.lear.MGCMS.services.FirstCheckService;
import com.lear.MGCMS.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/firstCheckMachineStopped")
public class FirstCheckMachineStoppedController {

    @Autowired
    private FirstCheckMachineStoppedService service;

    @Autowired
    private UserService userService;

    @PostMapping
    private ResponseEntity<?> save(@RequestBody FirstCheckMachineStopped obj, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_PROCESS")) {
                authorized = true; break;
            }
        }
        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        obj.setCreatedAt(LocalDateTime.now());
        obj.setCreatedBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        return new ResponseEntity<FirstCheckMachineStopped>(service.save(obj), HttpStatus.CREATED);
    }

    @GetMapping("/filtre")
    private List<FirstCheckMachineStopped> findList(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = true) String shift,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "machine", required = false) String machine
    ) {
        return service.findList(date, shift, machine, category);
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_PROCESS")) {
                authorized = true; break;
            }
        }
        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        service.delete(id);
        return new ResponseEntity<String>("Deleted", HttpStatus.OK);
    }

}
