package com.lear.MGCMS.controller.splice;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.splice.CalibrationLogService;
import com.lear.splice.domain.CalibrationLog;
import com.lear.splice.domain.MarkerLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calibrationLog")
public class CalibrationLogController {

    @Autowired
    private CalibrationLogService service;

    @Autowired
    private UserService userService;

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
            if(role.getName().equals("ROLE_SPLICE_READER")) {
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
        return new ResponseEntity<Page<CalibrationLog>>(service.findAll(filters, page,size,sortBy), HttpStatus.OK);
    }

    @GetMapping("/findByDate")
    private List<CalibrationLog> findByDate(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = true) String shift
    ) {
        LocalDateTime startDate = null, endDate = null;
        if (shift.equals("2")) {
            startDate = date.atTime(05, 50);
            endDate = date.atTime(13, 50);
        } else if (shift.equals("3")) {
            startDate = date.atTime(13, 50);
            endDate = date.atTime(21, 50);
        } else {
            startDate = date.atTime(21, 50).minusDays(1);
            endDate = date.atTime(05, 50);
        }
        if (endDate.compareTo(LocalDateTime.now()) > 0) {
            endDate = LocalDateTime.now();
        }
        return service.findByDate(startDate, endDate);
    }

}
