package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.payload.*;
import com.lear.MGCMS.services.QueryService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.ctc.FilesHistoryService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.ctc.domain.FilesHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;
    @Autowired
    private UserService userService;

    @GetMapping("/listCuttingPlan")
    public List<Long> getCuttingPlanBySequence(@RequestParam(name = "pnList" , required = true) List<String> pnList) {
        return queryService.getCuttingPlanBySequence(pnList);
    }

    @GetMapping("/reftissu-airbag")
    public List<String> getReftissuAirbag() {
        return queryService.getReftissuAirbag();
    }


    @GetMapping("/prixItem")
    public Double getPrixItem(@RequestParam(name = "item", required = true) String item) {
        return queryService.getPrixItem(item);
    }

    @GetMapping("/pls-verification")
    public String getPlsBySequenceAndReftissu(
            @RequestParam(name = "sequence", required = true) String sequence,
            @RequestParam(name = "partNumberMaterial", required = true) String partNumberMaterial
    ) {
        return queryService.getPlsBySequenceAndReftissu(sequence, partNumberMaterial);
    }


    @GetMapping("/scheduleMachine")
    private List<ScheduleMachine> getScheduleMachine(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift
    ) {
        return queryService.getScheduleMachine(date, shift);
    }

    @GetMapping("/nomFournisseur")
    private List<FournisseurIMS> getNomFournisseur(
            @RequestParam(value = "lotList", required = true) List<String> lotList
    ) {
        return queryService.getNomFournisseur(lotList);
    }

    @GetMapping("/refDetails")
    private ResponseEntity<?> refDetails(
            @RequestParam(value = "reftissu", required = true) String reftissu
    ) {
        Reference ref = queryService.refDetails(reftissu);
        if(ref == null) {
            return ResponseEntity.badRequest().body("Not Found");
        }
        return ResponseEntity.ok(ref);
    }




    ///rapportUsage?date=${this.state.date}&shift=${this.state.shift}&reftissu=${this.state.reftissu}
    @GetMapping("/rapportUsage")
    private List<RapportUsage> rapportUsage(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift,
            @RequestParam(value = "reftissu", required = false) String reftissu
    ) {
        List<RapportUsage> newArr = new ArrayList<>();
        List<RapportUsage> arr = queryService.rapportUsage(date, shift, reftissu);
        System.out.println("arr : " + arr.size());

        List<String> nonCompleted = queryService.nonCompleted(reftissu);
        System.out.println("nonCompleted : " + nonCompleted.size());
        for(RapportUsage obj : arr) {
            String id = obj.getCuttingRequest_sequence() + "-"+obj.getConfirmReftissu();
            if(!nonCompleted.contains(id)) {
                newArr.add(obj);
            } else {
                System.out.println(id);
            }
        }
        System.out.println("newArr : " + newArr.size());

        return newArr;
    }

    /*
      select lotFrs, SUM(excess) as excess, min(createdAt) as minDate, MAX(createdAt) as maxDate
      FROM [dbo].[CuttingRequestSerieRouleau]
      where confirmReftissu = 'L003015067NCPAA'
      group by confirmReftissu, lotFrs order by SUM(excess)
     */
    @GetMapping("/rapportExcess")
    private List<RapportExcess> rapportUsage(
            @RequestParam(value = "reftissu", required = false) String reftissu
    ) {
        return queryService.rapportExcess(reftissu);
    }


    @GetMapping("/getBom")
    private List<RapportBom> findBomByItemParent(
            @RequestParam(value = "itemParent", required = true) String itemParent
    ) {
        return queryService.findBomByItemParent(itemParent);
    }

    @Autowired
    private FilesHistoryService filesHistoryService;

    @PostMapping("/updateTolerence")
    private ResponseEntity<?> updateTolerence(
            @RequestParam(value = "projet", required = true) String projet,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "min1", required = false) Double min1,
            @RequestParam(value = "max1", required = false) Double max1,
            @RequestParam(value = "t2min1", required = false) Double t2min1,
            @RequestParam(value = "t2max1", required = false) Double t2max1,
            Authentication authentication
            ) {
        User user = userService.findByUsername(authentication.getName());
        String changement = "";
        if(projet != null) {
            changement += "projet="+ projet +",";
        }
        if(type != null) {
            changement += "type="+ type +",";
        }
        if(min1 != null) {
            changement += "min1="+ min1 +",";
        }
        if(max1 != null) {
            changement += "max1="+ max1 +",";
        }
        if(t2min1 != null) {
            changement += "t2min1="+ t2min1 +",";
        }
        if(t2max1 != null) {
            changement += "t2max1="+ t2max1 +",";
        }
        try{
            filesHistoryService.save(new FilesHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "Tolerence Form", changement));
        } catch (Exception e) {

        }
        return queryService.updateTolerence(projet,type, min1, max1, t2min1, t2max1);
    }


    @PostMapping("/initialiserSequenceCMS")
    public ResponseEntity<?> initialiserSequenceCMS(
            @RequestParam(value = "sequence", required = true) String sequence,
            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_IMPORTER") || role.getName().equals("ROLE_ADMIN")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        queryService.initialiserBySequenceCMS(sequence);

        return new ResponseEntity<String>("initialiser", HttpStatus.OK);

    }

    @DeleteMapping("/deleteSequenceCMS")
    public ResponseEntity<?> deleteBySequence(
            @RequestParam(value = "sequence", required = true) String sequence,
            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_IMPORTER") || role.getName().equals("ROLE_ADMIN")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        queryService.deleteBySequenceCMSWEB(sequence);
        queryService.deleteBySequenceCMS(sequence);

        return new ResponseEntity<String>("DELETED", HttpStatus.OK);

    }

    @PostMapping("/timingPlacement")
    public List<TimingPlacement> getTimingPlacement(@RequestBody List<String> placements) {
        // Utilisation du body pour supporter un grand nombre d'éléments (plus fiable que query param)
        List<TimingPlacement> arr = queryService.getTimingPlacement(placements);
        // verify if there is some placement not found, wo we can calculate the missing time
        List<String> placementAlreadyFound = new ArrayList<>();
        for(TimingPlacement obj : arr) {
            placementAlreadyFound.add(obj.getPlacementTimingModel());
        }

        for(String placement : placements) {
            if(!placementAlreadyFound.contains(placement)) {
                TimingPlacement tp = new TimingPlacement();
                tp.setPlacementTimingModel(placement);
                Double perimetre = ExcelHelper.getPerimetre(placement);
                if(perimetre == null) {
                    perimetre = 0.0;
                }
                tp.setValidatedCuttingTimeTimingModel(perimetre / 280); // Assuming a default value for validated cutting time

                arr.add(tp);
            }
        }
        return arr;
    }

    /**
     * Rapport des matières Airbag
     * Returns data from PLS database for airbag materials
     */
    @GetMapping("/rapportMatieresAirbag")
    public List<Map<String, Object>> rapportMatieresAirbag(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "partNumberMaterial", required = false) String partNumberMaterial
    ) {
        return queryService.rapportMatieresAirbag(startDate, endDate, partNumberMaterial);
    }

}
