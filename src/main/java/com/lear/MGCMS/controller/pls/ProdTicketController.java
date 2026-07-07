package com.lear.MGCMS.controller.pls;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.pls.DemandeHistoryService;
import com.lear.MGCMS.services.pls.ProdTicketService;
import com.lear.pls.domain.DemandeHistory;
import com.lear.pls.domain.ProdTicket;
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
import java.util.Map;

@RestController
@RequestMapping("/api/plsProdTicket")
public class ProdTicketController {

    @Autowired
    private ProdTicketService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;
    @Autowired
    private DemandeHistoryService demandeHistoryService;
    @GetMapping("/all")
    private ResponseEntity<?> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy,
            Authentication authentication
    ) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_PLS_READER") || role.getName().equals("ROLE_PLS_ADMIN")) {
                authorized = true; break;
            }
        }

        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return new ResponseEntity<Page<ProdTicket>>(service.findAll(filters, page,size,sortBy), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    private ResponseEntity<?> findById(@PathVariable String id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_PLS_READER") || role.getName().equals("ROLE_PLS_ADMIN")) {
                authorized = true; break;
            }
        }

        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<ProdTicket>(service.findById(Long.parseLong(id)), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody ProdTicket obj, BindingResult result, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_PLS_ADMIN")) {
                authorized = true; break;
            }
        }

        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        ProdTicket newObj = service.save(obj);
        demandeHistoryService.save(new DemandeHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "SAVE", "ProdTicket", newObj.toString()));
        return new ResponseEntity<ProdTicket>(newObj, HttpStatus.CREATED);
    }

    @PostMapping("/delete")
    ResponseEntity<?> delete(@Valid @RequestBody ProdTicket obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_PLS_ADMIN")) {
                authorized = true; break;
            }
        }

        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        service.delete(obj);
        demandeHistoryService.save(new DemandeHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "DELETE", "ProdTicket", obj.toString()));
        return new ResponseEntity<String>("Deleted", HttpStatus.CREATED);
    }

}
