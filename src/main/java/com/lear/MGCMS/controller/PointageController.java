package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.PartNumberMaterialConfigData;
import com.lear.MGCMS.domain.Pointage;
import com.lear.MGCMS.services.OptitimeService;
import com.lear.MGCMS.services.PointageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pointage")
public class PointageController {

    @Autowired
    private PointageService service;

    @Autowired
    private OptitimeService optitimeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('QUALITE')")
    public ResponseEntity<?> save(@RequestBody Pointage pointage){
        Pointage obj  = service.save(pointage);
        return ResponseEntity.ok(obj);
    }

//    @GetMapping("/scan")
//    public ResponseEntity<?> scanPointage(
//            @RequestParam(value = "code", required = true) String code,
//            @RequestParam(value = "machine", required = true) String machine,
//            @RequestParam(value = "type", required = false) String type
//    ){
//
//        String str = optitimeService.getFullName(code);
//        if(str == null) {
//            return ResponseEntity.ok("Code not found");
//        }
//        if(type == null) {
//            type = "Intervention";
//        }
//        Pointage oldObj = service.getLastPointage(LocalDateTime.now().minusHours(4), LocalDateTime.now(), machine, type, null);
//        if(oldObj != null && oldObj.getDateFin() == null) {
//            if(!oldObj.getMatricule().equals(str.split(":")[1].trim())) {
//                return ResponseEntity.ok("Matricule not match");
//            }
//            oldObj.setDateFin(LocalDateTime.now());
//            service.save(oldObj);
//            return ResponseEntity.ok(oldObj);
//        }
//        String[] arr = str.split(":");
//        Pointage obj = new Pointage();
//        obj.setMatricule(arr[1].trim());
//        obj.setPoste(machine);
//        obj.setType(type);
//        obj.setDepartement(arr[2].trim());
//        obj.setDate(LocalDateTime.now());
//        obj.setFullName(arr[0].trim());
//        Pointage newObj = service.save(obj);
//        return ResponseEntity.ok(newObj);
//    }

    @GetMapping("/list")
    public ResponseEntity<?> getListPointage(
            @RequestParam(value = "date1", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date1,
            @RequestParam(value = "date2", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date2,
            @RequestParam(value = "poste", required = true) String poste,
            @RequestParam(value = "type", required = true) String type,
            @RequestParam(value = "departement", required = false) String departement
    ){
        return ResponseEntity.ok(service.getListPointage(date1, date2, poste, type, departement));
    }

    @GetMapping("/last")
    public ResponseEntity<?> getLastPointage(
            @RequestParam(value = "date1", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date1,
            @RequestParam(value = "date2", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date2,
            @RequestParam(value = "poste", required = true) String poste,
            @RequestParam(value = "type", required = true) String type,
            @RequestParam(value = "departement", required = false) String departement
            ){
        Pointage obj  = service.getLastPointage(date1, date2, poste, type, departement);
        return ResponseEntity.ok(obj);
    }

    @GetMapping("/all")
    public Page<Pointage> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
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
        Pointage obj = service.findById(id);
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Pointage>(obj, HttpStatus.OK);
    }


}
