package com.lear.MGCMS.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.HardwareConfig;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.HardwareConfigService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/hardwareConfig")
public class HardwareConfigController {

    @Autowired
    private HardwareConfigService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody HardwareConfig obj, BindingResult result, Authentication authentication) {
        // Check admin role for create/update operations
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_PROCESS") ) {
                authorized = true;
                break;
            }
        }

        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED - Admin role required for modification", HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        HardwareConfig newObj = service.save(obj);
        return new ResponseEntity<HardwareConfig>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public List<HardwareConfig> findAll() {
        return service.findAll();
    }

    @GetMapping("/all")
    public Page<HardwareConfig> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy
            ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sortBy);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        HardwareConfig obj = service.findById(id);
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<HardwareConfig>(obj, HttpStatus.OK);
    }

    @GetMapping("/machine/{machine}")
    public ResponseEntity<?> findByMachine(@PathVariable String machine) {
        List<HardwareConfig> configs = service.findByMachine(machine);
        return new ResponseEntity<List<HardwareConfig>>(configs, HttpStatus.OK);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> findByType(@PathVariable String type) {
        List<HardwareConfig> configs = service.findByType(type);
        return new ResponseEntity<List<HardwareConfig>>(configs, HttpStatus.OK);
    }

    @GetMapping("/machine/{machine}/type/{type}")
    public ResponseEntity<?> findByMachineAndType(@PathVariable String machine, @PathVariable String type) {
        List<HardwareConfig> configs = service.findByMachineAndType(machine, type);
        return new ResponseEntity<List<HardwareConfig>>(configs, HttpStatus.OK);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@Valid @RequestBody HardwareConfig obj, Authentication authentication) {
        // Check admin role for delete operations
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for(Role role : user.getRoles()) {
            if(role.getName().equals("ROLE_ADMIN")|| role.getName().equals("ROLE_PROCESS")) {
                authorized = true;
                break;
            }
        }

        if(!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED - Admin role required for deletion", HttpStatus.UNAUTHORIZED);
        }

        HardwareConfig existingObj = service.findById(obj.getId());
        if(existingObj == null) {
            return new ResponseEntity<String>("Hardware configuration not found", HttpStatus.NOT_FOUND);
        }

        service.delete(existingObj);
        return new ResponseEntity<String>("Hardware configuration deleted successfully", HttpStatus.OK);
    }
}
