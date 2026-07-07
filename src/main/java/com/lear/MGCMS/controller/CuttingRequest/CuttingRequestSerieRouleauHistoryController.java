package com.lear.MGCMS.controller.CuttingRequest;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauHistory;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauInfo;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieRouleauHistoryService;
import com.lear.MGCMS.services.MapValidationErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cuttingRequestSerieRouleauHistory")
public class CuttingRequestSerieRouleauHistoryController {

    @Autowired
    private CuttingRequestSerieRouleauHistoryService service;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @GetMapping("/all")
    public Page<CuttingRequestSerieRouleauHistory> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "changeDate,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page,size,sortBy);
    }

    @PostMapping("/saveSerieRouleau")
    public ResponseEntity<?> saveSerieRouleau(@RequestBody CuttingRequestSerieRouleauInfo crsri,
                                              @RequestParam(value = "serie", required = true) String serie,
                                              @RequestParam(value = "matricule", required = true) String matricule
    ) {
        CuttingRequestSerieRouleauHistory crsrh = new CuttingRequestSerieRouleauHistory();
        crsrh.setSerie(serie);
        crsrh.setChangeDate(LocalDateTime.now());
        crsrh.setChangedBy(matricule);
        crsrh.setContent(crsri.toString());
        return new ResponseEntity<CuttingRequestSerieRouleauHistory>(service.save(crsrh), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody CuttingRequestSerieRouleauHistory obj, BindingResult result) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if(errorMap != null) return errorMap;
        if(obj.getChangeDate() == null) {
            obj.setChangeDate(LocalDateTime.now());
        }
        CuttingRequestSerieRouleauHistory newObj = service.save(obj);
        return new ResponseEntity<CuttingRequestSerieRouleauHistory>(newObj, HttpStatus.CREATED);
    }

}
