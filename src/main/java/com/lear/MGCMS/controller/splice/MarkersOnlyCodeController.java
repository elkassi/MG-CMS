package com.lear.MGCMS.controller.splice;

import com.lear.MGCMS.domain.CodeErreur;
import com.lear.MGCMS.domain.Consumable;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.ConsumableService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.splice.MarkersOnlyCodeService;
import com.lear.splice.domain.MarkersOnlyCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/markersOnlyCode")
public class MarkersOnlyCodeController {

    @Autowired
    private MarkersOnlyCodeService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;
    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('PROCESS') or hasRole('QUALITE') or hasRole('CHEF_EQUIPE') or hasRole('CHEF_DE_ZONE')")
    public ResponseEntity<?> save(@Valid @RequestBody MarkersOnlyCode obj, BindingResult result) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        MarkersOnlyCode newObj = service.save(obj);
        return new ResponseEntity<MarkersOnlyCode>(newObj, HttpStatus.CREATED);
    }


    @GetMapping("/all")
    public Page<MarkersOnlyCode> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "date,desc", required = false) String sortBy
    ) {
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
        MarkersOnlyCode obj = service.findByObjId(Long.parseLong(id));
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<MarkersOnlyCode>(obj, HttpStatus.OK);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('PROCESS') or hasRole('QUALITE') or hasRole('CHEF_EQUIPE') or hasRole('CHEF_DE_ZONE')")
    public ResponseEntity<?> delete(@Valid @RequestBody MarkersOnlyCode obj, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_CHEF_EQUIPE") || role.getName().equals("ROLE_CHEF_DE_ZONE")) {
                authorized = true; break;
            }
        }

        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        service.delete(obj);
        return new ResponseEntity<String>("Deleted", HttpStatus.OK);
    }


}
