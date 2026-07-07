package com.lear.MGCMS.controller.CuttingRequest.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauHistory;
import com.lear.MGCMS.payload.RouleauRapport;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieRouleauHistoryService;
import com.lear.MGCMS.services.QueryService;
import com.lear.MGCMS.services.WorkOrderService;
import com.lear.MGCMS.services.pls.ProdTicketService;
import com.lear.pls.domain.ProdTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestSerieRouleauDataService;

@RestController
@RequestMapping("/api/cuttingRequestSerieRouleauData")
public class CuttingRequestSerieRouleauDataController {

    @Autowired
    private CuttingRequestSerieRouleauDataService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;

    @Autowired
    private CuttingRequestSerieRouleauHistoryService cuttingRequestSerieRouleauHistoryService;

    @GetMapping("/badLinesInReport")
    public List<StockStatusReport> findBadLinesInReport(
            @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return service.findBadLinesInReport(startDate, endDate);
    }

    @PostMapping
//	@PreAuthorize("hasRole('ADMIN') or hasRole('QUALITE')")
    public ResponseEntity<?> save(@RequestBody CuttingRequestSerieRouleauData obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_QUALITE")) {
                authorized = true;
                break;
            }

        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

//        Double totalUsage = 0.0;
//        if(obj.getNbrCouche() > 0 && obj.getLongueurPremierCouche() != null) {
//            totalUsage += obj.getLongueurPremierCouche() * obj.getNbrCouche();
//        }
//        if(obj.getLongueurCoucheOverlap() > 0) {
//            totalUsage += obj.getLongueurCoucheOverlap();
//        }
//        // add defaut + nonUtitlse + excess + overlap1 + overlap2 + overlap3 + overlap4 + overlap5 + overlap6 + overlap7 + overlap8
//        if(obj.getDefaut() != null) {
//            totalUsage += obj.getDefaut();
//        }
//        if(obj.getNonUtitlse() != null) {
//            totalUsage += obj.getNonUtitlse();
//        }
//        if(obj.getExcess() != null) {
//            totalUsage += obj.getExcess();
//        }
//        if(obj.getOverlap1() != null) {
//            totalUsage += obj.getOverlap1();
//        }
//        if(obj.getOverlap2() != null) {
//            totalUsage += obj.getOverlap2();
//        }
//        if(obj.getOverlap3() != null) {
//            totalUsage += obj.getOverlap3();
//        }
//        if(obj.getOverlap4() != null) {
//            totalUsage += obj.getOverlap4();
//        }
//        if(obj.getOverlap5() != null) {
//            totalUsage += obj.getOverlap5();
//        }
//        if(obj.getOverlap6() != null) {
//            totalUsage += obj.getOverlap6();
//        }
//        if(obj.getOverlap7() != null) {
//            totalUsage += obj.getOverlap7();
//        }
//        if(obj.getOverlap8() != null) {
//            totalUsage += obj.getOverlap8();
//        }
//        obj.setTotalUsage(totalUsage);


        CuttingRequestSerieRouleauData newObj = service.save(obj);
        CuttingRequestSerieRouleauHistory cuttingRequestSerieRouleauHistory = new CuttingRequestSerieRouleauHistory();
        cuttingRequestSerieRouleauHistory.setSerie(newObj.getSerie());
        cuttingRequestSerieRouleauHistory.setChangeDate(LocalDateTime.now());
        cuttingRequestSerieRouleauHistory.setChangedBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        cuttingRequestSerieRouleauHistory.setContent(newObj.toString());
        cuttingRequestSerieRouleauHistoryService.save(cuttingRequestSerieRouleauHistory);
        return new ResponseEntity<CuttingRequestSerieRouleauData>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/bySeries/{listSeries}")
    public List<CuttingRequestSerieRouleauData> findBySeries(@PathVariable List<String> listSeries) {
        return service.findBySeries(listSeries);
    }

    @PostMapping("/valider/{id}")
    public ResponseEntity<?> valider(@PathVariable String id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_QUALITE")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        CuttingRequestSerieRouleauData obj = service.findById(Long.parseLong(id));

        obj.setConfirmRetour(true);
        obj.setDeblockedBy(user.getUsername());
        obj.setDeblockedDate(LocalDateTime.now());
        obj.setDeblockedMetrage(obj.getDefaut());
        obj.setRetour(obj.getRetour() + obj.getDefaut());
        obj.setDefaut(0.0);
        CuttingRequestSerieRouleauData newObj = service.save(obj);
        CuttingRequestSerieRouleauHistory cuttingRequestSerieRouleauHistory = new CuttingRequestSerieRouleauHistory();
        cuttingRequestSerieRouleauHistory.setSerie(newObj.getSerie());
        cuttingRequestSerieRouleauHistory.setChangeDate(LocalDateTime.now());
        cuttingRequestSerieRouleauHistory.setChangedBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        cuttingRequestSerieRouleauHistory.setContent("Valider : " + newObj.toString());
        cuttingRequestSerieRouleauHistoryService.save(cuttingRequestSerieRouleauHistory);

        return new ResponseEntity<CuttingRequestSerieRouleauData>(newObj, HttpStatus.CREATED);
    }

    @Autowired
    private ProdTicketService prodTicketService;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private QueryService queryService;

    @GetMapping("/rouleauRapport")
    public List<RouleauRapport> findRest(
            @RequestParam(value = "reftissu", required = false) String reftissu
    ) {
        List<RouleauRapport> arr = service.findRest(reftissu);
        List<ProdTicket> arrPLSOther = prodTicketService.findRestNotInIdRouleauInThis(
                arr.stream().map(r -> "S" + r.getIdRouleau()).toList(),
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now()
        );
        // add in arr this  arrPLSOther
        for (ProdTicket pls : arrPLSOther) {
            RouleauRapport rr = new RouleauRapport();
            rr.setIdRouleau(pls.getLabelId().replace("S", ""));
            rr.setReftissu(pls.getReftissu());
            rr.setLaize(null);
            rr.setRetour(pls.getQuantity());
            rr.setSerie(pls.getPlsId());
            rr.setCreatedAt(pls.getCreatedAt());
            rr.setTableMatelassage(pls.getTableName());
            rr.setLotFrs(pls.getLotNr().replace("H", ""));
            arr.add(rr);
//            System.out.println("Adding PLS: " + rr.getIdRouleau() + " - " + rr.getReftissu() + " - " + rr.getRetour());
        }
        List<String> refTissus = arr.stream().map(RouleauRapport::getReftissu).distinct().toList();
        java.nio.file.Path path = java.nio.file.Paths.get("\\\\matnr-fp01\\Groups\\CMS WEB\\cmsFolder\\reportsNew\\R100.prn");
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy");
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(path)) {
            String line;
            String itemNumber = "", um = "", abc = "", site = "", location = "", ref = "", status = "";
            java.time.LocalDate date = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("--------------------------")
                        || line.contains("Item Number")
                        || line.contains("Page:")
                        || line.contains("3.6.1 Stock Status Report")
                        || line.contains("Lot/Serial")
                        || line.contains("End of Report") || line.contains("TANGIER-TRIM")
                        || line.contains("Output:") || line.contains("Batch ID:")
                        || line.contains("Report Submitted") || line.contains("To:") || line.contains("Summary/Detail:") || line.contains("Include Zero Quantity:")
                        || line.trim().isEmpty()) {
                    continue;
                }
                if (line.length() < 131) {
                    line = line + String.join("", java.util.Collections.nCopies(131 - line.length(), " "));
                }
                if (line.contains("MA10TR01")) {
                    itemNumber = line.substring(0, 26).trim();
                    um = line.substring(27, 29).trim();
                    abc = line.substring(30, 32).trim();
                    site = line.substring(34, 42).trim();
                }
                if (itemNumber.isEmpty()) {
                    continue;
                }
                if (!line.substring(43, 51).trim().isEmpty() && line.substring(43, 51).trim().contains("/")) {
                    try {
                        date = java.time.LocalDate.parse(line.substring(43, 51).trim(), dateFormatter);
                    } catch (Exception e) {
                        date = null;
                    }
                }
                if (!line.substring(80, 88).trim().isEmpty()) {
                    location = line.substring(80, 88).trim();
                }
                ref = line.substring(89, 107).trim();
                String qtyStr = line.substring(108, 121).trim();
                String statusStr = line.substring(122, 130).trim();
                // Vérifier les conditions
                if (ref.isEmpty() || qtyStr.isEmpty() || !location.toUpperCase().startsWith("T0") || !um.equalsIgnoreCase("MT")) {
                    continue;
                }
                if (!statusStr.equalsIgnoreCase("AVAIL2")) {
                    continue;
                }
                if (!refTissus.contains(itemNumber)) {
                    continue;
                }
                double qty = 0.0;
                try {
                    qty = Double.parseDouble(qtyStr);
                } catch (Exception e) {
                    continue;
                }
                // Vérifier si un RouleauRapport avec le même reftissu et idRouleau existe dans arr
                for (RouleauRapport rapport : arr) {
                    if (rapport.getReftissu().equals(itemNumber) && rapport.getIdRouleau().endsWith(ref)) {
                        rapport.setLocationMP(location);
                        rapport.setQuantityMP(qty);
//                        System.out.println("R100 : " + rapport.getReftissu() + " " + rapport.getIdRouleau()  + " : " + line + " : " +ref);
                    }
                }


            }
        } catch (Exception e) {
            return null;
        }

        for (RouleauRapport rapport : arr) {
            if (rapport.getLocationMP() == null) {
                rapport.setLocationMP("Coupe");
            }
        }

        List<ProdTicket> arrPLS = prodTicketService.findObjIdRouleauInThis(
                arr.stream().map(r -> "S" + r.getIdRouleau()).toList());
        // search fr every PLS if it exists in the list arr
        for (RouleauRapport rr : arr) {
            ProdTicket pls = arrPLS.stream()
                    .filter(p -> ("S" + p.getLabelId()).equals(rr.getIdRouleau()) && p.getReftissu().equals(rr.getReftissu()))
                    .findFirst()
                    .orElse(null);
            if (pls != null && pls.getQuantity() < rr.getRetour()) {
                rr.setRetour(pls.getQuantity());
                rr.setSerie(pls.getPlsId());
                rr.setCreatedAt(pls.getCreatedAt());
                rr.setTableMatelassage(pls.getTableName());
//                System.out.println("Updating PLS: " + rr.getIdRouleau() + " - " + rr.getReftissu() + " - " + rr.getRetour());
            }
        }

        List<String> sequenceStartsWith = new ArrayList<>();
        // fill this with 120725% and 130725% and 140725%, that represent the sequence with format ddmmyy
        sequenceStartsWith.add(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy")) + "%");
        sequenceStartsWith.add(LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy")) + "%");
        sequenceStartsWith.add(LocalDate.now().minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy")) + "%");
        List<String> reftissuStillToBeUsed = service.getPartNumberMateriallToBeUsed(sequenceStartsWith);

        List<String> itemNumbersWithF = workOrderService.getItemsWithStatus("F");
        List<String> materialsOfItems = queryService.findMaterialsOfItems(itemNumbersWithF);
        // now include materialsOfItems in reftissuStillToBeUsed
        for (String material : materialsOfItems) {
            if (!reftissuStillToBeUsed.contains(material)) {
                reftissuStillToBeUsed.add(material);
            }
        }
        // for LocationMP = "Coupe" ,  if arr have reftissu in reftissuStillToBeUsed then set statut to "A utiliser" , else set to "A retourner au stock"

        for (RouleauRapport rr : arr) {
            if (rr.getLocationMP().equals("Coupe")) {
                if (reftissuStillToBeUsed.contains(rr.getReftissu())) {
                    rr.setStatut("A utiliser");
                } else {
                    rr.setStatut("A retourner au stock");
                }
            } else {
                rr.setStatut("Stock");
            }
        }
        return arr;
    }


    @GetMapping("/defaut")
    public List<CuttingRequestSerieRouleauData> findDefaut() {
        return service.findDefaut();
    }


    @GetMapping("/all")
    public Page<CuttingRequestSerieRouleauData> findAll(
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
    public ResponseEntity<?> findByCode(@PathVariable String id) {
        CuttingRequestSerieRouleauData obj = service.findById(Long.parseLong(id));
        if (obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<CuttingRequestSerieRouleauData>(obj, HttpStatus.OK);
    }


    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody CuttingRequestSerieRouleauData obj, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_QUALITE")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        service.delete(obj);
        CuttingRequestSerieRouleauHistory cuttingRequestSerieRouleauHistory = new CuttingRequestSerieRouleauHistory();
        cuttingRequestSerieRouleauHistory.setSerie(obj.getSerie());
        cuttingRequestSerieRouleauHistory.setChangeDate(LocalDateTime.now());
        cuttingRequestSerieRouleauHistory.setChangedBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        cuttingRequestSerieRouleauHistory.setContent("Supprimer : " + obj.toString());
        cuttingRequestSerieRouleauHistoryService.save(cuttingRequestSerieRouleauHistory);

        return new ResponseEntity<String>("Deleted", HttpStatus.OK);
    }


}
