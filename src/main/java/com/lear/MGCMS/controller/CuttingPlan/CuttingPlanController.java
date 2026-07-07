package com.lear.MGCMS.controller.CuttingPlan;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanData;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialData;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementData;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanPartNumberData;
import com.lear.MGCMS.domain.Projet;
import com.lear.MGCMS.domain.ProjetVersion;
import com.lear.MGCMS.payload.AlertCuttingPlan;
import com.lear.MGCMS.services.*;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanDataService;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanMaterialDataService;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanMaterialPlacementDataService;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanPartNumberDataService;
import com.lear.MGCMS.services.cms.PlanCoupeService;
import com.lear.MGCMS.utils.UtilFunctions;
import com.lear.cms.domain.*;
import com.lear.cms.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanHistory;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacement;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumber;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanRapportDrill;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanRapportModel;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanRapportPlacement;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanHistoryRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLight2Repository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialInfoRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialPlacementInfoRepository;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanMaterialPlacementService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanMaterialService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanPartNumberService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanService;
import com.lear.MGCMS.services.ctc.FilesService;
import com.lear.ctc.domain.Files;

@RestController
@RequestMapping("/api/cuttingPlan")
public class CuttingPlanController {

    @Autowired
    private CuttingPlanService service;
    @Autowired
    private com.lear.MGCMS.services.PerimetreService perimetreService;
    @Autowired
    private CuttingPlanPartNumberService cuttingPlanPartNumberService;
    @Autowired
    private CuttingPlanMaterialService cuttingPlanMaterialService;
    @Autowired
    private CuttingPlanMaterialPlacementService cuttingPlanMaterialPlacementService;
    @Autowired
    private UserService userService;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;
    @Autowired
    private FilesService filesService;
    @Autowired
    private CuttingPlanLight2Repository cuttingPlanLight2Repository;
    @Autowired
    private CuttingPlanHistoryRepository cuttingPlanHistoryRepository;
    @Autowired
    private PlanCoupeService planCoupeService;
    @Autowired
    private PlanCoupeRepository planCoupeRepository;
    @Autowired
    private PartNumberPlanCoupeRepository partNumberPlanCoupeRepository;
    @Autowired
    private SpreadingCuttingPlanCoupeRepository spreadingCuttingPlanCoupeRepository;
    @Autowired
    private DrillPlanCoupeRepository drillPlanCoupeRepository;
    @Autowired
    private QueryService queryService;
    @Autowired
    private ProjetService projetService;
    @Autowired
    private ProjetVersionService projetVersionService;
    @Autowired
    private TimingModelRepository timingModelRepository;

    @GetMapping("/all")
    public Page<CuttingPlanLight2> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy,
            Authentication authentication
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        
        // If user has ROLE_CAD_FOAM but not ROLE_CAD, filter to only show foam=true plans
        if (authentication != null) {
            boolean hasRoleCadFoam = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
            boolean hasRoleCad = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));
            
            if (hasRoleCadFoam && !hasRoleCad) {
                // Force foam filter for CAD_FOAM users without CAD role
                filters.put("equal.foam.1000", "TRUE");
            }
            if (!hasRoleCadFoam && hasRoleCad) {
                // Force foam filter for CAD_FOAM users without CAD role
                filters.put("equal.foam.1000", "FALSE");
            }
        }
        
        return service.findAll2(filters, page, size, sortBy);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findByCode(@PathVariable String id, Authentication authentication) {
        CuttingPlan obj = service.findByObjId(Long.parseLong(id));
        if (obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        if (authentication != null) {
            boolean hasRoleCadFoam = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
            boolean hasRoleCad = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));
            boolean isFoam = Boolean.TRUE.equals(obj.getFoam());

            // CAD_FOAM-only users can only access foam plans; CAD-only users can
            // only access non-foam plans (foam = false or null). Boolean.TRUE.equals
            // is null-safe, so legacy null-foam rows don't NPE.
            if (hasRoleCadFoam && !hasRoleCad && !isFoam) {
                return new ResponseEntity<String>("Access denied", HttpStatus.BAD_REQUEST);
            }
            if (hasRoleCad && !hasRoleCadFoam && isFoam) {
                return new ResponseEntity<String>("Access denied", HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<CuttingPlan>(obj, HttpStatus.OK);
    }

    public void checkProjet() {
        List<Projet> arr = projetService.findAll();
        List<String> projets = new ArrayList<>();
        for (Projet obj : arr) {
            projets.add(obj.getNom());
        }

        List<String> projetsFromCMS = projetService.findProjetsFromCMS();
        for (String projet : projetsFromCMS) {
            if (!projets.contains(projet)) {
                Projet obj = new Projet();
                obj.setNom(projet);
                projetService.save(obj);
            }
        }
    }

    public void checkVersion() {
        List<Projet> arr = projetService.findAll();
        for (Projet obj : arr) {
            List<String> versionsExisted = projetVersionService.findVersionByProjetNom(obj.getNom());
            List<String> versionsFromCMS = projetVersionService.findVersionFromCMS(obj.getNom());
            List<ProjetVersion> versions = new ArrayList<>();
            for(String version : versionsFromCMS) {
                if(!versionsExisted.contains(version)) {
                    versions.add(new ProjetVersion(obj, version));
                }
            }
            projetVersionService.saveAll(versions);
        }
    }

    @Autowired
    private CuttingPlanDataService cuttingPlanDataService;

    @PostMapping("/enable/{id}")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> enable(@PathVariable String id, Authentication authentication) {
        CuttingPlanData obj = cuttingPlanDataService.findById(Long.parseLong(id));
//        if(obj.getAlertMessages() != null && !obj.getAlertMessages().isEmpty()) {
//            return new ResponseEntity<String>("Vous ne pouvez pas activer ce plan de coupe car il contient des alertes : " + obj.getAlertMessages(), HttpStatus.BAD_REQUEST);
//        }
        User user = userService.findByUsername(authentication.getName());
        obj.setEnabled(true);
        obj.setEnabledAt(LocalDateTime.now());
        obj.setEnabledBy(user);
        CuttingPlanData newObj = cuttingPlanDataService.save(obj, user);
        try {
            CuttingPlanHistory cph = new CuttingPlanHistory();
            cph.setCuttingPlan(newObj.getId());
            cph.setCreatedAt(LocalDateTime.now());
            cph.setUpdatedBy(user);
            cph.setChanges("Enabled");
            cuttingPlanHistoryRepository.save(cph);
        } catch (Exception e) {
            System.out.println("CuttingPlan History ERROR : " + e.getMessage());
        }

        PlanCoupe pc = planCoupeService.findByIdLight(obj.getCmsId());
        if(pc != null) {
            pc.setStatusPlanCoupe(true);
            pc.setDateEnabledPlanCoupe(obj.getEnabledAt().toLocalDate());
            pc.setHourEnabledPlanCoupe(obj.getEnabledAt().toLocalTime());
            pc.setEnabledBySessionWPlanCoupe(obj.getEnabledBy().getUsername());
            pc.setEnabledByUserNamePlanCoupe(obj.getEnabledBy().getLastName() + " " + obj.getEnabledBy().getFirstName());
            pc.setEnabledByHostaNamePlanCoupe("CMS WEB");
            planCoupeService.save(pc);
        }

        return new ResponseEntity<String>("enable", HttpStatus.OK);
    }

    @PostMapping("/disable/{id}")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> disable(@PathVariable String id, Authentication authentication) {
        CuttingPlanData obj = cuttingPlanDataService.findById(Long.parseLong(id));

        User user = userService.findByUsername(authentication.getName());
        obj.setEnabled(false);
        obj.setDisabledAt(LocalDateTime.now());
        obj.setDisabledBy(user);
        CuttingPlanData newObj = cuttingPlanDataService.save(obj, user);
        try {
            CuttingPlanHistory cph = new CuttingPlanHistory();
            cph.setCuttingPlan(newObj.getId());
            cph.setCreatedAt(LocalDateTime.now());
            cph.setUpdatedBy(user);
            cph.setChanges("Disabled");
            cuttingPlanHistoryRepository.save(cph);
        } catch (Exception e) {
            System.out.println("CuttingPlan History ERROR : " + e.getMessage());
        }

        PlanCoupe pc = planCoupeService.findByIdLight(obj.getCmsId());
        if(pc != null) {
            pc.setStatusPlanCoupe(false);
            pc.setDateDisabledPlanCoupe(obj.getDisabledAt().toLocalDate());
            pc.setHourDisabledPlanCoupe(obj.getDisabledAt().toLocalTime());
            pc.setDisabledBySessionWPlanCoupe(obj.getDisabledBy().getUsername());
            pc.setDisabledByUserNamePlanCoupe(obj.getDisabledBy().getLastName() + " " + obj.getDisabledBy().getFirstName());
            pc.setDisabledByHostaNamePlanCoupe("CMS WEB");
            planCoupeService.save(pc);
        }

        return new ResponseEntity<String>("disable", HttpStatus.OK);
    }

    @PostMapping("/enable-consommation/{id}")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> enableconsommation(@PathVariable String id, Authentication authentication) {
        CuttingPlanData obj = cuttingPlanDataService.findById(Long.parseLong(id));
        User user = userService.findByUsername(authentication.getName());
        obj.setConsommation(true);
        CuttingPlanData newObj = cuttingPlanDataService.save(obj, user);
        try {
            CuttingPlanHistory cph = new CuttingPlanHistory();
            cph.setCuttingPlan(newObj.getId());
            cph.setCreatedAt(LocalDateTime.now());
            cph.setUpdatedBy(user);
            cph.setChanges("Consommation activée");
            cuttingPlanHistoryRepository.save(cph);
        } catch (Exception e) {
            System.out.println("CuttingPlan History ERROR : " + e.getMessage());
        }

        return new ResponseEntity<String>("Consommation activée", HttpStatus.OK);
    }

    @PostMapping("/disable-consommation/{id}")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> disableconsommation(@PathVariable String id, Authentication authentication) {
        CuttingPlanData obj = cuttingPlanDataService.findById(Long.parseLong(id));
        User user = userService.findByUsername(authentication.getName());
        obj.setConsommation(false);
        CuttingPlanData newObj = cuttingPlanDataService.save(obj, user);
        try {
            CuttingPlanHistory cph = new CuttingPlanHistory();
            cph.setCuttingPlan(newObj.getId());
            cph.setCreatedAt(LocalDateTime.now());
            cph.setUpdatedBy(user);
            cph.setChanges("Consommation désactivée");
            cuttingPlanHistoryRepository.save(cph);
        } catch (Exception e) {
            System.out.println("CuttingPlan History ERROR : " + e.getMessage());
        }

        return new ResponseEntity<String>("Consommation désactivée", HttpStatus.OK);
    }

    @Autowired
    private EmailService emailService;
    @Value("${lear.emailsCAD}")
    private String emailsCAD;

    @PostMapping("/alert")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> alert(@RequestBody AlertCuttingPlan obj, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<String> emailArr = new ArrayList<>();
        for(String responsable : obj.getResponsableList()) {
            User userObj = userService.findFirstByFullName(responsable);
            if(userObj != null) {
                emailArr.add(userObj.getEmail());
            }
        }
        emailArr.add(emailsCAD);
        emailArr.add("melghazi@lear.com");
        emailArr.add("sghailane@lear.com");
        try {

            emailService.sendEmailAttachment(emailArr,
                    "Plan de coupe CAD "+obj.getCuttingPlan().getId()+" non compatible avec CTC : " + obj.getCuttingPlan().getDescription(),
                    "<html>" + "<head>" + "<style>\r\n" + "h2{text-align: center;}\r\n"
                            + "table {font-family: arial, sans-serif;border-collapse: collapse;margin: 0 auto}\r\n"
                            + "td, th {border: 1px solid #dddddd;text-align: left;padding: 4px;font-size: 12}\r\n" + "</style>"
                            + "</head>" + "<body>"
                            + obj.getContent()
                            + "</body></html>");
        } catch (Exception exp) {
            System.out.println("Email Notification 2 Error");
        }
        return new ResponseEntity<String>("alert", HttpStatus.OK);
    }


    @PostMapping("/refresh-cms")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> loadPlanCoupe() {
        User user = userService.findByUsername("melghazi");
        int percentage = 0;

        checkProjet();
        checkVersion();

        List<PlanCoupe> arrPc = planCoupeService.findAllLightBetween(LocalDate.now().minusDays(3), LocalDate.now());
        Map<String, User> mapUsers = new HashMap<>();
        mapUsers.put(user.getMatricule(), user);
        Map<String, String> matriculeMap = queryService.findMatriculeMapCMS();
        for (PlanCoupe pcLight : arrPc) {
            while(percentage< (int)(100*((float)(arrPc.size()-pcLight.getIdPlanCoupe())/arrPc.size()))) {
                percentage++;
                System.out.println("Loading CMS CP : "+ pcLight.getIdPlanCoupe() + "  " + percentage+"/100 : " );
            }

            if(pcLight.getCreatedByHostaNamePlanCoupe().isEmpty() || Objects.equals(pcLight.getCreatedByHostaNamePlanCoupe(), "CMS WEB")) {
                continue;
            }

            try {
                CuttingPlanLight2 cpObj = cuttingPlanLight2Repository.findByCmsId(pcLight.getIdPlanCoupe());
                if (cpObj == null || (
                        (pcLight.getHourCreatedPlanCoupe() != null && pcLight.getDateCreatedPlanCoupe() != null && cpObj.getCreatedAt() != null && cpObj.getCreatedAt().compareTo(pcLight.getDateCreatedPlanCoupe().atTime(pcLight.getHourCreatedPlanCoupe())) != 0)
                                || (pcLight.getHourModifiedPlanCoupe() != null && pcLight.getDateModifiedPlanCoupe() != null && cpObj.getUpdatedAt() != null && cpObj.getUpdatedAt().compareTo(pcLight.getDateModifiedPlanCoupe().atTime(pcLight.getHourModifiedPlanCoupe())) != 0)
                                || (pcLight.getHourModifiedPlanCoupe() != null && cpObj.getUpdatedAt() == null)
                                || (pcLight.getHourEnabledPlanCoupe() != null && pcLight.getDateEnabledPlanCoupe() != null && cpObj.getEnabledAt() != null && cpObj.getEnabledAt().compareTo(pcLight.getDateEnabledPlanCoupe().atTime(pcLight.getHourEnabledPlanCoupe())) != 0)
                                || (pcLight.getHourEnabledPlanCoupe() != null && cpObj.getEnabledAt() == null)
                                || (pcLight.getHourDisabledPlanCoupe() != null && pcLight.getDateDisabledPlanCoupe() != null && cpObj.getDisabledAt() != null && cpObj.getDisabledAt().compareTo(pcLight.getDateDisabledPlanCoupe().atTime(pcLight.getHourDisabledPlanCoupe())) != 0)
                                || (pcLight.getHourDisabledPlanCoupe() != null && cpObj.getDisabledAt() == null)
                )) {
//					if (pcLight.getStatusPlanCoupe() == false)
//						continue;
//					System.out.println("plan coupe id : " + id);
                    PlanCoupe pc = planCoupeService.findById(pcLight.getIdPlanCoupe());
                    CuttingPlan obj = new CuttingPlan();

                    if(cpObj != null && cpObj.getId() != null) {
                        service.deleteByPlanCoupeId(cpObj.getId());
                        obj.setId(cpObj.getId());
//						System.out.println("RENEW id : " + id + " / "+ cpObj.getId());
                    }

                    List<String> arrPn = new ArrayList<String>();
                    List<String> arrPnQte = new ArrayList<String>();

                    obj.setCmsId(pc.getIdPlanCoupe());
                    obj.setProjet(pc.getGroupPlanCoupe());
                    obj.setVersion(pc.getVersionPlanCoupe());
                    obj.setDefinition(pc.getDefinitionPlanCoupe());
                    obj.setEnabled(pc.getStatusPlanCoupe());
                    obj.setStartDate(pc.getStartDateFromPlanCoupe());
                    obj.setEndDate(pc.getEndDateToPlanCoupe());
                    obj.setCommentaire(pc.getCommentplanCoupe());
                    obj.setType(pc.getTypePlanCoupe());
                    if(pc.getCreatedByUserNamePlanCoupe() != null) {
                        if(matriculeMap.containsKey(pc.getCreatedByUserNamePlanCoupe())) {
                            String matricule = matriculeMap.get(pc.getCreatedByUserNamePlanCoupe());
                            if (mapUsers.containsKey(matricule)) {
                                obj.setCreatedBy(mapUsers.get(pc.getCreatedByUserNamePlanCoupe()));
                            } else {
                                User userObj = userService.findByUsername(matricule);
                                if (userObj != null) {
                                    obj.setCreatedBy(userObj);
                                    mapUsers.put(matricule, userObj);
                                }
                            }
                        } else {

                        }
                    }
                    if(pc.getHourCreatedPlanCoupe() != null && pc.getDateCreatedPlanCoupe() != null) {
                        obj.setCreatedAt(pc.getDateCreatedPlanCoupe().atTime(pc.getHourCreatedPlanCoupe()));
                    }
                    if(pc.getDateModifiedPlanCoupe() != null && pc.getHourModifiedPlanCoupe() != null) {
                        obj.setUpdatedAt(pc.getDateModifiedPlanCoupe().atTime(pc.getHourModifiedPlanCoupe()));
                    }
                    if(pc.getDateEnabledPlanCoupe() != null && pc.getHourEnabledPlanCoupe() != null) {
                        obj.setEnabledAt(pc.getDateEnabledPlanCoupe().atTime(pc.getHourEnabledPlanCoupe()));
                    }
                    if(pc.getDateDisabledPlanCoupe() != null && pc.getHourDisabledPlanCoupe() != null) {
                        obj.setDisabledAt(pc.getDateDisabledPlanCoupe().atTime(pc.getHourDisabledPlanCoupe()));
                    }
                    List<CuttingPlanPartNumber> cppnArr = new ArrayList<CuttingPlanPartNumber>();
                    for (PartNumberPlanCoupe pnpc : pc.getPartNumberPlanCoupes()) {
                        CuttingPlanPartNumber pn = new CuttingPlanPartNumber();
                        pn.setPartNumber(pnpc.getPartNumberPlanCoupe());
                        pn.setItem(pnpc.getKitTextilPlanCoupe());
                        pn.setDescription(pnpc.getDescriptionPartNumberPlanCoupe());
                        pn.setQuantity(pnpc.getQuantityPartNumberPlanCoupe());
                        pn.setCuttingPlan(obj);
                        cppnArr.add(pn);
                        arrPn.add(pn.getPartNumber());
                        arrPnQte.add(pn.getPartNumber() + "(" + pn.getQuantity() + ")");
                    }
                    obj.setCuttingPlanPartNumbers(cppnArr);

                    String description = String.join("_", arrPnQte);
                    if (obj.getVersion() != null) {
                        description = obj.getVersion() + " " + description;
                    }
                    if (obj.getProjet() != null) {
                        description = obj.getProjet() + " " + description;
                    }
                    obj.setDescription(description);
                    List<SpreadingCuttingPlanCoupe> arr = pc.getSpreadingCuttingPlanCoupes();

                    Collections.sort(arr, new Comparator<SpreadingCuttingPlanCoupe>() {
                        @Override
                        public int compare(SpreadingCuttingPlanCoupe o1, SpreadingCuttingPlanCoupe o2) {
                            int compareId = Long.compare(o1.getIdSpreadingCuttingParentPlanCoupe(),
                                    o2.getIdSpreadingCuttingParentPlanCoupe());
                            if (compareId != 0) {
                                return compareId;
                            }
                            return Boolean.compare(!o1.getDefaultSpreadingCuttingPlanCoupe(),
                                    !o2.getDefaultSpreadingCuttingPlanCoupe());
                        }
                    });

                    Set<String> reftissuArr = new HashSet<String>();
                    for (SpreadingCuttingPlanCoupe scpc : arr) {
                        reftissuArr.add(scpc.getItemNumberPlanCoupe());
                    }
                    List<CuttingPlanMaterial> cpmArr = new ArrayList<CuttingPlanMaterial>();
                    for (String reftissu : reftissuArr) {
                        CuttingPlanMaterial cpm = new CuttingPlanMaterial();
                        cpm.setPartNumberMaterial(reftissu);
                        Set<String> placements = new HashSet<String>();
                        List<CuttingPlanMaterialPlacement> cpmpArr = new ArrayList<CuttingPlanMaterialPlacement>();
                        for (SpreadingCuttingPlanCoupe scpc : arr) {
                            if (!scpc.getItemNumberPlanCoupe().equals(reftissu) || scpc.getPlacementPlanCoupe() == null || scpc.getQuantityPerLayerPlanCoupe() == null || scpc.getQuantityPerLayerPlanCoupe() <= 0
                                    || scpc.getPlacementPlanCoupe().trim().isEmpty()
                                    || placements.contains(scpc.getPlacementPlanCoupe().trim().toUpperCase())) {
                                continue;
                            }
                            placements.add(scpc.getPlacementPlanCoupe().trim().toUpperCase());
                            if (scpc.getDefaultSpreadingCuttingPlanCoupe()) {
                                cpm.setPartNumberMaterial(scpc.getItemNumberPlanCoupe());
                                cpm.setDescription(scpc.getDescriptionItemNumberPlanCoupe());
                                cpm.setMatelassageEndroit(scpc.getMatelassageEndroitPlanCoupe());
                                cpm.setTauxScrap(scpc.getTauxScrapPlanCoupe().toString());
                                cpm.setVitesse(300);
                                cpm.setQadUsage(scpc.getUsageQADPlanCoupe());
                                cpm.setCuttingPlan(obj);
                            }
                            CuttingPlanMaterialPlacement cpmp = new CuttingPlanMaterialPlacement();
                            cpmp.setPlacement(scpc.getPlacementPlanCoupe());
                            List<String> drills = new ArrayList<String>();
                            for (DrillPlanCoupe dpc : scpc.getDrillPlanCoupes()) {
                                drills.add(dpc.getDrillPlan());
                            }
                            if (drills.size() == 1) {
                                drills.add("");
                            }
                            if (drills.size() == 0) {
                                drills.add("");
                                drills.add("");
                            }
                            cpmp.setDrill(String.join(",", drills));
                            cpmp.setCategory(scpc.getCategoryPlanCoupe());
                            cpmp.setLongueur(scpc.getLongueurPlacementPlanCoupe());
                            switch (scpc.getMachinePlanCoupe()) {
                                case "1":
                                    cpmp.setMachine("Lectra");
                                    break;
                                case "2":
                                    cpmp.setMachine("Gerber");
                                    break;
                                case "3":
                                    cpmp.setMachine("DIE");
                                    break;
                                case "4":
                                    cpmp.setMachine("LASER-LSR");
                                    break;
                                case "5":
                                    cpmp.setMachine("Lectra IP6");
                                    break;
                                case "6":
                                    cpmp.setMachine("LASER-DXF");
                                    break;
                            }
                            cpmp.setNbrCouche(
                                    (int) Math.ceil(Double.parseDouble(pc.getQuantityPlanCoupe()+"") / Double.parseDouble(scpc.getQuantityPerLayerPlanCoupe()+"")));
                            cpmp.setLongueurMatelas(scpc.getLongueurMatelasPlanCoupe() * cpmp.getNbrCouche());
                            cpmp.setConfig(scpc.getConfigurationPlanCoupe());
                            cpmp.setLaize(scpc.getLaizePlanCoupe());
                            cpmp.setMaxDrill(30);
                            cpmp.setMaxPlie(scpc.getMaxPliePlanCoupe());
                            cpmp.setMaxPlieDrill(scpc.getMaxPliePlanCoupe());
                            cpmp.setGroupPlacement(Integer.parseInt(scpc.getIdSpreadingCuttingParentPlanCoupe() + ""));
                            cpmp.setActivated(scpc.getDefaultSpreadingCuttingPlanCoupe());
                            cpmp.setTempsDeCoupe(scpc.getTempsCoupeTheoriquePlanCoupe());
                            cpmp.setPliesConfig("1," + scpc.getConfigurationPlanCoupe());
                            cpmp.setPliesConfigMarge(
                                    "1," + (scpc.getLongueurMatelasPlanCoupe() - scpc.getLongueurPlacementPlanCoupe()));
                            cpmp.setPerimetre(scpc.getPerimiterPlanCoupe());
                            cpmp.setCuttingPlanMaterial(cpm);
                            cpmpArr.add(cpmp);
                        }
                        cpm.setCuttingPlanMaterialPlacement(cpmpArr);
                        cpmArr.add(cpm);
                    }
                    obj.setCuttingPlanMaterials(cpmArr);
                    service.save(obj, user);
                }
            }catch(Exception e) {
                System.out.println("error id : " + pcLight.getIdPlanCoupe() + " : "+ e.getMessage());
            }
        }
        return new ResponseEntity<String>("Done", HttpStatus.OK);
    }


    @PostMapping("/{id}/convertReftissu/{reftissus}")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public void convertReftissu(@PathVariable String id, @PathVariable List<String> reftissus, Authentication authentication) {
        CuttingPlan obj = service.findByObjId(Long.parseLong(id));
        User user = userService.findByUsername(authentication.getName());

        for (String reftissu : reftissus) {
            String[] arrReftissu = reftissu.split(":");
            for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {
                if (cpm.getPartNumberMaterial().equalsIgnoreCase(arrReftissu[0])) {
                    service.updatePartNumberMaterial(Long.parseLong(id), arrReftissu[0], arrReftissu[1]);
                    break;
                }
            }
        }

        // Update CMS tables if cmsId exists
        if (obj.getCmsId() != null) {
            for (String reftissu : reftissus) {
                String[] arrReftissu = reftissu.split(":");
                String oldReftissu = arrReftissu[0];
                String newReftissu = arrReftissu[1];
                
                // Update SpreadingCuttingPlanCoupe
                List<SpreadingCuttingPlanCoupe> spreadingList = spreadingCuttingPlanCoupeRepository
                    .findByIdSpreadingPlanForeignPlanCoupe(obj.getCmsId());
                for (SpreadingCuttingPlanCoupe scpc : spreadingList) {
                    if (scpc.getItemNumberPlanCoupe() != null && 
                        scpc.getItemNumberPlanCoupe().equalsIgnoreCase(oldReftissu)) {
                        scpc.setItemNumberPlanCoupe(newReftissu);
                        spreadingCuttingPlanCoupeRepository.save(scpc);
                    }
                }
                
                // Update TimingModel
                List<TimingModel> timingList = timingModelRepository
                    .findByIdPlanCoupeTimingModel(obj.getCmsId());
                for (TimingModel tm : timingList) {
                    if (tm.getItemNumberTimingModel() != null && 
                        tm.getItemNumberTimingModel().equalsIgnoreCase(oldReftissu)) {
                        tm.setItemNumberTimingModel(newReftissu);
                        timingModelRepository.save(tm);
                    }
                }
            }
        }

        try {
            CuttingPlanHistory cph = new CuttingPlanHistory();
            cph.setCuttingPlan(Long.parseLong(id));
            cph.setCreatedAt(LocalDateTime.now());
            cph.setUpdatedBy(user);
            cph.setChanges("Changement de reftissu : " + String.join(" / ", reftissus));
            cuttingPlanHistoryRepository.save(cph);
        } catch (Exception e) {
        }
    }


    @PostMapping("/save-bulk")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> saveBulk(@Valid @RequestBody CuttingPlan obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;
        User user = userService.findByUsername(authentication.getName());
        obj.setType("Excel Import");
        List<String> arrPn = new ArrayList<String>();
        List<String> arrPnQte = new ArrayList<String>();
        for (CuttingPlanPartNumber cppn : obj.getCuttingPlanPartNumbers()) {
            cppn.setCuttingPlan(obj);
            arrPn.add(cppn.getPartNumber());
            arrPnQte.add(cppn.getPartNumber() + "(" + cppn.getQuantity() + ")");
        }
        for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {
            cpm.setCuttingPlan(obj);
            for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                cpmp.setCuttingPlanMaterial(cpm);
            }
        }

        String description = String.join("_", arrPnQte);
        if (obj.getVersion() != null) {
            description = obj.getVersion() + " " + description;
        }
        if (obj.getProjet() != null) {
            description = obj.getProjet() + " " + description;
        }
        obj.setDescription(description);
        if (authentication != null && obj.getFoam() == null) {
            boolean hasRoleCadFoam = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
            boolean hasRoleCad = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));

            if (hasRoleCadFoam && !hasRoleCad) {
                obj.setFoam(true);
            } else {
                obj.setFoam(false);
            }
        }
        if (obj.getId() == null) {
            if (obj.getCreatedBy() == null) obj.setCreatedBy(user);
            if (obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());

            CuttingPlan newObj = service.save(obj, user);
            return new ResponseEntity<CuttingPlan>(newObj, HttpStatus.CREATED);
        }
        CuttingPlan oldObj = service.findByObjId(obj.getId());
        if (obj.getCreatedBy() == null) obj.setCreatedBy(user);
        if (obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());
        obj.setUpdatedBy(user);
        obj.setUpdatedAt(LocalDateTime.now());
        if (!oldObj.getEnabled().equals(obj.getEnabled())) {
            obj.setEnabledAt(LocalDateTime.now());
            obj.setEnabledBy(user);
        }
        CuttingPlan newObj = service.save(obj, user);
        return new ResponseEntity<CuttingPlan>(newObj, HttpStatus.CREATED);
    }


    @Autowired
    private CuttingPlanPartNumberDataService cuttingPlanPartNumberDataService;
    @Autowired
    private CuttingPlanMaterialDataService cuttingPlanMaterialDataService;
    @Autowired
    private CuttingPlanMaterialPlacementDataService cuttingPlanMaterialPlacementDataService;

    @PostMapping("/cms")
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> saveCMS(@Valid @RequestBody CuttingPlan obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        User user = userService.findByUsername(authentication.getName());



        if (obj.getCmsId() != null) {
            CuttingPlanLight2 cp = cuttingPlanLight2Repository.findByCmsIdAndEnable(obj.getCmsId());
            if (cp != null && !cp.getId().equals(obj.getId())) {
                return new ResponseEntity<String>("Plan CMS " + obj.getCmsId() + " est déjà utilisé dans cette application avec l'id " + cp.getId(), HttpStatus.BAD_REQUEST);
            }
        }

        List<String> arrPn = new ArrayList<String>();
        List<String> arrPnQte = new ArrayList<String>();
        Boolean sameQuantity = true;
        int quantity = obj.getCuttingPlanPartNumbers().get(0).getQuantity();
        for (CuttingPlanPartNumber cppn : obj.getCuttingPlanPartNumbers()) {
            cppn.setCuttingPlan(obj);
            arrPn.add(cppn.getPartNumber());
            arrPnQte.add(cppn.getPartNumber() + "(" + cppn.getQuantity() + ")");
            if (quantity != cppn.getQuantity()) {
                sameQuantity = false;
                quantity = Math.max(quantity, cppn.getQuantity());
            }
        }
        for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {
            cpm.setCuttingPlan(obj);
            for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                cpmp.setCuttingPlanMaterial(cpm);
            }
        }
        // Refresh cached per-part-number perimeters from the placement cut files
        // (Plan de Charge part-number report). Best-effort; never blocks the save.
        perimetreService.applyToPlan(obj);
        for (CuttingPlanRapportPlacement cprp : obj.getCuttingPlanRapportPlacements()) {
            cprp.setCuttingPlan(obj);
        }
        for (CuttingPlanRapportModel cprm : obj.getCuttingPlanRapportModels()) {
            cprm.setCuttingPlan(obj);
        }
        for (CuttingPlanRapportDrill cprd : obj.getCuttingPlanRapportDrills()) {
            cprd.setCuttingPlan(obj);
        }

        obj.setVersion2(String.join("_", arrPn));
        obj.setQuantity(quantity);

        String description = "";
        if (sameQuantity) {
            description = String.join("_", arrPn) + " " + quantity;
        } else {
            description = String.join("_", arrPnQte);
        }
        if (obj.getVersion() != null) {
            description = obj.getVersion() + " " + description;
        }
        if (obj.getProjet() != null) {
            description = obj.getProjet() + " " + description;
        }
        obj.setDescription(description);
        if (authentication != null && obj.getFoam() == null) {
            boolean hasRoleCadFoam = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
            boolean hasRoleCad = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));

            if (hasRoleCadFoam && !hasRoleCad) {
                obj.setFoam(true);
            } else {
                obj.setFoam(false);
            }
        }
        if (obj.getId() == null) {
            if (obj.getCreatedBy() == null) obj.setCreatedBy(user);
            if (obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());
            if (obj.getEnabled()) {
                obj.setEnabledAt(LocalDateTime.now());
                obj.setEnabledBy(user);
            } else {
                obj.setDisabledAt(LocalDateTime.now());
                obj.setDisabledBy(user);
            }
        } else {
            CuttingPlan oldObj = service.findByObjId(obj.getId());
            if (obj.getCreatedBy() == null) obj.setCreatedBy(user);
            if (obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());
            obj.setUpdatedBy(user);
            obj.setUpdatedAt(LocalDateTime.now());
            if (!oldObj.getEnabled().equals(obj.getEnabled())) {
                if (obj.getEnabled()) {
                    obj.setEnabledAt(LocalDateTime.now());
                    obj.setEnabledBy(user);
                } else {
                    obj.setDisabledAt(LocalDateTime.now());
                    obj.setDisabledBy(user);
                }
            }
        }

        if(obj.getStartDate() == null) {
            obj.setStartDate(LocalDateTime.now());
        }
        if(obj.getDefinition() == null) {
            obj.setDefinition("");
        }
        if(obj.getCommentaire() == null){
            obj.setCommentaire("");
        }

        // let verify if description exist in other cutting plan if the obj id is null , and if it is not null , then just check that the description has not changed from last time
//        if (obj.getId() == null) {
//            CuttingPlanData cpData = cuttingPlanDataService.findByDescription(obj.getDescription());
//            if (cpData != null) {
//                return new ResponseEntity<String>("Description " + obj.getDescription() + " est déjà utilisé dans cette application avec l'id " + cpData.getId(), HttpStatus.BAD_REQUEST);
//            }
//        } else {
//            CuttingPlanData cpData = cuttingPlanDataService.findById(obj.getId());
//            if (cpData != null && !cpData.getDescription().equals(obj.getDescription())) {
//                // we just directly the error without checking in the db if the new description exist or not
//                return new ResponseEntity<String>("Vous ne pouvez pas modifier la description d'un plan de coupe existant", HttpStatus.BAD_REQUEST);
//            }
//        }


        if (obj.getId() != null){
            // 1. Handle PartNumber deletions
            List<String> partNumbersOld = cuttingPlanPartNumberDataService.getPartNumbersByCuttingPlanId(obj.getId());
            List<String> partNumbersNew = new ArrayList<>();
            for (CuttingPlanPartNumber cppn : obj.getCuttingPlanPartNumbers()) {
                partNumbersNew.add(cppn.getPartNumber());
            }
            
            // Find part numbers to delete (old ones not in new list)
            List<String> partNumbersToDelete = new ArrayList<>();
            for (String pnOld : partNumbersOld) {
                if (!partNumbersNew.contains(pnOld)) {
                    partNumbersToDelete.add(pnOld);
                }
            }
            
            // Batch delete part numbers
            if (!partNumbersToDelete.isEmpty()) {
                cuttingPlanPartNumberDataService.deleteByCuttingPlanIdAndPartNumberIn(obj.getId(), partNumbersToDelete);
            }

            // 2. Handle Material deletions
            List<String> partNumberMaterialsOld = cuttingPlanMaterialDataService.getPartNumberMaterialsByCuttingPlanId(obj.getId());
            List<String> partNumberMaterialsNew = new ArrayList<>();
            for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {
                partNumberMaterialsNew.add(cpm.getPartNumberMaterial());
            }
            
            // Find materials to delete (old ones not in new list)
            List<String> materialsToDelete = new ArrayList<>();
            for (String materialOld : partNumberMaterialsOld) {
                if (!partNumberMaterialsNew.contains(materialOld)) {
                    materialsToDelete.add(materialOld);
                }
            }
            
            // Batch delete materials (this will handle related placements through cascade if configured)
            if (!materialsToDelete.isEmpty()) {
                cuttingPlanMaterialDataService.deleteByCuttingPlanIdAndPartNumberMaterialIn(obj.getId(), materialsToDelete);
            }

            // 3. Handle Placement deletions
            List<String> placementsOld = cuttingPlanMaterialPlacementDataService.getPlacementsByCuttingPlanId(obj.getId());
            List<String> placementsNew = new ArrayList<>();
            for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {
                for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                    placementsNew.add(cpmp.getPlacement());
                }
            }
            
            // Find placements to delete (old ones not in new list)
            List<String> placementsToDelete = new ArrayList<>();
            for (String placementOld : placementsOld) {
                if (!placementsNew.contains(placementOld)) {
                    placementsToDelete.add(placementOld);
                }
            }
            
            // Batch delete placements - must be done BEFORE material deletion to avoid FK constraint issues
            if (!placementsToDelete.isEmpty()) {
                cuttingPlanMaterialPlacementDataService.deleteByCuttingPlanIdAndPlacementIn(obj.getId(), placementsToDelete);
            }
        }


        // plan coupe
        PlanCoupe pc = new PlanCoupe();
        try {

            if (obj.getCmsId() != null) {
                pc = planCoupeService.findByIdLight(obj.getCmsId());
                drillPlanCoupeRepository.deletePlanById(obj.getCmsId());
                spreadingCuttingPlanCoupeRepository.deletePlanById(obj.getCmsId());
                partNumberPlanCoupeRepository.deletePlanById(obj.getCmsId());
                timingModelRepository.deletePlanById(obj.getCmsId());
            } else {
                Long newId = planCoupeService.maxId() + 1;
                obj.setCmsId(newId);
                pc.setIdPlanCoupe(newId);
            }
            pc.setIndexPlanCoupe(obj.getDescription());
            if (obj.getProjet() != null) {
                pc.setGroupPlanCoupe(obj.getProjet());
            } else {
                pc.setGroupPlanCoupe("");
            }
            if (obj.getVersion() != null) {
                pc.setVersionPlanCoupe(obj.getVersion());
            } else {
                pc.setVersionPlanCoupe("");
            }
            pc.setDefinitionPlanCoupe(obj.getDefinition());
            pc.setVersion2PlanCoupe(obj.getVersion2());
            if (obj.getCreatedAt() != null) {
                pc.setDateCreatedPlanCoupe(obj.getCreatedAt().toLocalDate());
                pc.setHourCreatedPlanCoupe(obj.getCreatedAt().toLocalTime());
            }
            if (obj.getUpdatedAt() != null) {
                pc.setDateModifiedPlanCoupe(obj.getUpdatedAt().toLocalDate());
                pc.setHourModifiedPlanCoupe(obj.getUpdatedAt().toLocalTime());
            }
            if (obj.getEnabledAt() != null) {
                pc.setDateEnabledPlanCoupe(obj.getEnabledAt().toLocalDate());
                pc.setHourEnabledPlanCoupe(obj.getEnabledAt().toLocalTime());
            }
            if (obj.getDisabledAt() != null) {
                pc.setDateDisabledPlanCoupe(obj.getDisabledAt().toLocalDate());
                pc.setHourDisabledPlanCoupe(obj.getDisabledAt().toLocalTime());
            }
            if (obj.getCopyId() != null) {
                pc.setLibelleplanCoupe("Copy");
                Optional<CuttingPlanLight2> cp = cuttingPlanLight2Repository.findById(obj.getCopyId());
                if (cp.isPresent()) {
                    pc.setCopyFromPlanCoupe(cp.get().getCmsId().intValue());
                }
            } else {
                pc.setLibelleplanCoupe("Original");
            }
            if (obj.getCreatedBy() != null) {
                pc.setCreatedBySessionWPlanCoupe(obj.getCreatedBy().getUsername());
                pc.setCreatedByUserNamePlanCoupe(obj.getCreatedBy().getLastName() + " " + obj.getCreatedBy().getFirstName());
                pc.setCreatedByHostaNamePlanCoupe("CMS WEB");
            } else {
                pc.setCreatedBySessionWPlanCoupe("");
                pc.setCreatedByUserNamePlanCoupe("");
                pc.setCreatedByHostaNamePlanCoupe("");
            }
            if (obj.getUpdatedBy() != null) {
                pc.setModifiedBySessionWPlanCoupe(obj.getUpdatedBy().getUsername());
                pc.setModifiedByUserNamePlanCoupe(obj.getUpdatedBy().getLastName() + " " + obj.getUpdatedBy().getFirstName());
                pc.setModifiedByHostaNamePlanCoupe("CMS WEB");
            } else {
                pc.setModifiedByUserNamePlanCoupe("");
                pc.setModifiedBySessionWPlanCoupe("");
                pc.setModifiedByHostaNamePlanCoupe("");
            }
            if (obj.getEnabledBy() != null) {
                pc.setEnabledBySessionWPlanCoupe(obj.getEnabledBy().getUsername());
                pc.setEnabledByUserNamePlanCoupe(obj.getEnabledBy().getLastName() + " " + obj.getEnabledBy().getFirstName());
                pc.setEnabledByHostaNamePlanCoupe("CMS WEB");
            } else {
                pc.setEnabledByUserNamePlanCoupe("");
                pc.setEnabledBySessionWPlanCoupe("");
                pc.setEnabledByHostaNamePlanCoupe("");
            }
            if (obj.getDisabledBy() != null) {
                pc.setDisabledBySessionWPlanCoupe(obj.getDisabledBy().getUsername());
                pc.setDisabledByUserNamePlanCoupe(obj.getDisabledBy().getLastName() + " " + obj.getDisabledBy().getFirstName());
                pc.setDisabledByHostaNamePlanCoupe("CMS WEB");
            } else {
                pc.setDisabledByUserNamePlanCoupe("");
                pc.setDisabledBySessionWPlanCoupe("");
                pc.setDisabledByHostaNamePlanCoupe("");
            }
            pc.setQuantityPlanCoupe(obj.getQuantity());
            pc.setStatusPlanCoupe(obj.getEnabled());
            pc.setTypePlanCoupe(obj.getType());
            pc.setStartDateFromPlanCoupe(obj.getStartDate());
            pc.setEndDateToPlanCoupe(obj.getEndDate());
            pc.setCommentplanCoupe(obj.getCommentaire());
            planCoupeService.save(pc);
        } catch (Exception e) {

        }
        CuttingPlan newObj = service.save(obj, user);
        try {
            Long maxIdPartNumber = partNumberPlanCoupeRepository.maxId();
            if (maxIdPartNumber == null) {
                maxIdPartNumber = 0L;
            }
            for (CuttingPlanPartNumber cppn : obj.getCuttingPlanPartNumbers()) {
                maxIdPartNumber++;
                PartNumberPlanCoupe pnpc = new PartNumberPlanCoupe();
                pnpc.setIdPartNumberPlanCoupe(maxIdPartNumber);
                pnpc.setPartNumberPlanCoupe(cppn.getPartNumber());
                pnpc.setKitTextilPlanCoupe(cppn.getItem());
                pnpc.setDescriptionPartNumberPlanCoupe(cppn.getDescription());
                pnpc.setQuantityPartNumberPlanCoupe(cppn.getQuantity());
                pnpc.setIdPartNumberPlanForeignPlanCoupe(pc.getIdPlanCoupe());
                partNumberPlanCoupeRepository.save(pnpc);
            }
            Long maxIdSpreading = spreadingCuttingPlanCoupeRepository.maxId();
            if (maxIdSpreading == null) {
                maxIdSpreading = 0L;
            }
            Long maxIdSpreadingParent = 0L;
            Long maxIdDrill = drillPlanCoupeRepository.maxId();
            if (maxIdDrill == null) {
                maxIdDrill = 0L;
            }

            for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {
                Double usage = 0.0;
                for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {

                    if (cpmp.getActivated()) {
                        usage += cpmp.getLongueurMatelas();
                    }
                }
                int group = cpm.getCuttingPlanMaterialPlacement().get(0).getGroupPlacement();
                for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                    maxIdSpreading++;
                    if (group != cpmp.getGroupPlacement()) {
                        group = cpmp.getGroupPlacement();
                    }
                    if (cpmp.getActivated()) {
                        maxIdSpreadingParent = maxIdSpreading;
                    }
                    SpreadingCuttingPlanCoupe scpc = new SpreadingCuttingPlanCoupe();
                    scpc.setIdSpreadingCuttingPlanCoupe(maxIdSpreading);
                    scpc.setIdSpreadingPlanForeignPlanCoupe(pc.getIdPlanCoupe());
                    scpc.setItemNumberPlanCoupe(cpm.getPartNumberMaterial());
                    scpc.setDescriptionItemNumberPlanCoupe(cpm.getDescription());
                    scpc.setLaizePlanCoupe(cpmp.getLaize());
                    scpc.setCategoryPlanCoupe(cpmp.getCategory());
                    scpc.setMaxPliePlanCoupe(Math.min(cpmp.getMaxPlie(), cpmp.getNbrCouche()));
                    scpc.setMatelassageEndroitPlanCoupe(cpm.getMatelassageEndroit());
                    switch (cpmp.getMachine()) {
                        case "Lectra":
                            scpc.setMachinePlanCoupe("1");
                            break;
                        case "Gerber":
                            scpc.setMachinePlanCoupe("2");
                            break;
                        case "DIE":
                            scpc.setMachinePlanCoupe("3");
                            break;
                        case "LASER-LSR":
                            scpc.setMachinePlanCoupe("4");
                            break;
                        case "Lectra IP6":
                            scpc.setMachinePlanCoupe("5");
                            break;
                        case "LASER-DXF":
                            scpc.setMachinePlanCoupe("6");
                            break;
                    }
                    String partNumbers = cpmp.getPartNumbers(); // like L002483470NCPAC:8, L002483474NCPAC:11 and we need to get the max like 11 here
                    String[] arrPartNumbers = partNumbers.split(", ");
                    int maxQty = 0;
                    for (String partNumber : arrPartNumbers) {
                        String[] arrPartNumber = partNumber.split(":");
                        maxQty = Math.max(maxQty, Integer.parseInt(arrPartNumber[1]));
                    }
                    scpc.setPlacementPlanCoupe(cpmp.getPlacement());
                    scpc.setLongueurPlacementPlanCoupe(cpmp.getLongueur());
                    if (cpm.getPlaque() != null && cpm.getPlaque() > 0) {
                        scpc.setTypePlaquePlanCoupe("Plaque");
                        scpc.setLongueurMatelasPlanCoupe(cpm.getPlaque());
                    } else {
                        scpc.setTypePlaquePlanCoupe("");
                        scpc.setLongueurMatelasPlanCoupe(cpmp.getLongueurMatelas() / cpmp.getNbrCouche());
                    }
                    scpc.setQuantityPerLayerPlanCoupe((int) Math.ceil((double) obj.getQuantity() / cpmp.getNbrCouche()));
                    scpc.setOverlapPlanCoupe("");
                    scpc.setConfigurationPlanCoupe(cpmp.getConfig());
                    scpc.setPerimiterPlanCoupe(cpmp.getPerimetre());
                    scpc.setTempsCoupeTheoriquePlanCoupe(cpmp.getTempsDeCoupe());
                    scpc.setUsageTheoriquePlanCoupe(usage);
                    if (cpm.getQadUsage() == null) {
                        cpm.setQadUsage(0.0);
                    }
                    scpc.setUsageQADPlanCoupe(cpm.getQadUsage());
                    scpc.setGapQADPlanCoupe(usage - cpm.getQadUsage());
                    scpc.setPercentGapPlanCoupe(100 * (usage - cpm.getQadUsage()) / usage);
                    scpc.setTauxScrapPlanCoupe(Double.parseDouble(cpm.getTauxScrap()));
                    scpc.setDefaultSpreadingCuttingPlanCoupe(cpmp.getActivated());
                    scpc.setIdSpreadingCuttingParentPlanCoupe(maxIdSpreadingParent);
                    if (cpmp.getMaxDrill() != null) {
                        scpc.setSeuilDrillPlanCoupe((double) cpmp.getMaxDrill());
                    }
                    spreadingCuttingPlanCoupeRepository.save(scpc);
                    if (cpmp.getDrill() == null || cpmp.getDrill().isEmpty() || cpmp.getDrill().equals(",")) {
                        cpmp.setDrill("0,0");
                    }
                    if (cpmp.getDrill().endsWith(",")) {
                        cpmp.setDrill(cpmp.getDrill() + "0");
                    }
                    if (cpmp.getDrill().startsWith(",")) {
                        cpmp.setDrill("0" + cpmp.getDrill());
                    }
                    for (String drill : cpmp.getDrill().split(",")) {
                        maxIdDrill++;
                        DrillPlanCoupe dpc = new DrillPlanCoupe();
                        dpc.setIdDrillPlanCoupe(maxIdDrill);
                        if (drill.isBlank()) {
                            dpc.setDrillPlan("0");
                        } else {
                            dpc.setDrillPlan(drill);
                        }
                        dpc.setIdCuttingForeignPlanCoupe(maxIdSpreading);
                        drillPlanCoupeRepository.save(dpc);
                    }
                    try {
                        TimingModel tm = new TimingModel();
                        tm.setIdPlanCoupeTimingModel(pc.getIdPlanCoupe());
                        tm.setIdSpreadingTimingModel(maxIdSpreading);
                        tm.setItemNumberTimingModel(cpm.getPartNumberMaterial());
                        tm.setDescriptionItemNumberTimingModel(cpm.getDescription());
                        tm.setPlacementTimingModel(cpmp.getPlacement());
                        tm.setTypeItemTimingModel("");
                        tm.setPerimeterTimingModel(cpmp.getPerimetre());
                        tm.setQtyPlanTimingModel(pc.getQuantityPlanCoupe());
                        tm.setQtyPerLayerTimingModel(maxQty);
                        tm.setMaxPlieTimingModel(cpmp.getMaxPlie());
                        tm.setMachineTimingModel(scpc.getMachinePlanCoupe());
                        tm.setLayersTimingModel(cpmp.getNbrCouche());
                        tm.setLongueurMatelasTimingModel(cpmp.getLongueurMatelas() / cpmp.getNbrCouche());
                        tm.setLongueurPlacementTimingModel(cpmp.getLongueur());
                        tm.setSeuilLongueurTimingModel(UtilFunctions.convertTwoDigit(tm.getLongueurMatelasTimingModel() - tm.getLongueurPlacementTimingModel(), 3));
                        tm.setSpeedMMinTimingModel(UtilFunctions.convertTwoDigit(cpmp.getPerimetre() * 0.01 / cpmp.getTempsDeCoupe(), 4));
                        tm.setDrillingMiscTimingModel(9.0);
                        tm.setPrepTimeMinTimingModel(6.2);
                        tm.setCuttingTimeStopperPerlayerTimingModel(0.3);
                        tm.setSpreadTimePerLayerMMinTimingModel(0.1);
                        tm.setValidatedCuttingtimeTimingModel(0.0);
//                    tm.setSpreadingTimingModel(tm.getPrepTimeMinTimingModel() + (tm.getCuttingTimeStopperPerlayerTimingModel() * tm.getLayersTimingModel()) + (tm.getSpreadTimePerLayerMMinTimingModel() * tm.getLayersTimingModel() * tm.getLongueurMatelasTimingModel()));
//                    tm.setCuttingTimeTimingModel(cpmp.getTempsDeCoupe());
                        tm.setRealCuttingtimeTimingModel(0.0);
                        timingModelRepository.save(tm);
                    } catch (Exception e) {
                        System.out.println("Error timing model : " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {

        }

        return new ResponseEntity<CuttingPlan>(newObj, HttpStatus.CREATED);
    }


    @PostMapping
    @PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
    public ResponseEntity<?> save(@Valid @RequestBody CuttingPlan obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        User user = userService.findByUsername(authentication.getName());


        if (obj.getCmsId() != null) {
            CuttingPlanLight2 cp = cuttingPlanLight2Repository.findByCmsId(obj.getCmsId());
            if (cp != null && !cp.getId().equals(obj.getId())) {
                return new ResponseEntity<String>("Plan CMS " + obj.getCmsId() + " est déjà utilisé dans cette application avec l'id " + cp.getId(), HttpStatus.BAD_REQUEST);
            }
        }

        if (obj.getId() != null) {
            cuttingPlanMaterialPlacementService.deleteByCuttingPlanId(obj.getId());
            cuttingPlanMaterialService.deleteByCuttingPlanId(obj.getId());
            cuttingPlanPartNumberService.deleteByCuttingPlanId(obj.getId());
        }
        List<String> arrPn = new ArrayList<String>();
        List<String> arrPnQte = new ArrayList<String>();
        Boolean sameQuantity = true;
        int quantity = obj.getCuttingPlanPartNumbers().get(0).getQuantity();
        for (CuttingPlanPartNumber cppn : obj.getCuttingPlanPartNumbers()) {
            cppn.setCuttingPlan(obj);
            arrPn.add(cppn.getPartNumber());
            arrPnQte.add(cppn.getPartNumber() + "(" + cppn.getQuantity() + ")");
            if (quantity != cppn.getQuantity()) {
                sameQuantity = false;
                quantity = Math.max(quantity, cppn.getQuantity());
            }
        }
        for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {
            cpm.setCuttingPlan(obj);
            for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                cpmp.setCuttingPlanMaterial(cpm);
            }
        }
        // Refresh cached per-part-number perimeters from the placement cut files
        // (Plan de Charge part-number report). Best-effort; never blocks the save.
        perimetreService.applyToPlan(obj);
        for (CuttingPlanRapportPlacement cprp : obj.getCuttingPlanRapportPlacements()) {
            cprp.setCuttingPlan(obj);
        }
        for (CuttingPlanRapportModel cprm : obj.getCuttingPlanRapportModels()) {
            cprm.setCuttingPlan(obj);
        }
        for (CuttingPlanRapportDrill cprd : obj.getCuttingPlanRapportDrills()) {
            cprd.setCuttingPlan(obj);
        }

        obj.setVersion2(String.join("_", arrPn));
        obj.setQuantity(quantity);

        String description = "";
        if (sameQuantity) {
            description = String.join("_", arrPn) + " " + quantity;
        } else {
            description = String.join("_", arrPnQte);
        }
        if (obj.getVersion() != null) {
            description = obj.getVersion() + " " + description;
        }
        if (obj.getProjet() != null) {
            description = obj.getProjet() + " " + description;
        }
        obj.setDescription(description);
        if (authentication != null && obj.getFoam() == null) {
            boolean hasRoleCadFoam = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
            boolean hasRoleCad = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));

            if (hasRoleCadFoam && !hasRoleCad ) {
                obj.setFoam(true);
            } else {
                obj.setFoam(false);
            }
        }
        if (obj.getId() == null) {

            if (obj.getCreatedBy() == null) obj.setCreatedBy(user);
            if (obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());
            if (obj.getEnabled()) {
                obj.setEnabledAt(LocalDateTime.now());
                obj.setEnabledBy(user);
            } else {
                obj.setDisabledAt(LocalDateTime.now());
                obj.setDisabledBy(user);
            }
        } else {
            CuttingPlan oldObj = service.findByObjId(obj.getId());
            if (obj.getCreatedBy() == null) obj.setCreatedBy(user);
            if (obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());
            obj.setUpdatedBy(user);
            obj.setUpdatedAt(LocalDateTime.now());
            if (!oldObj.getEnabled().equals(obj.getEnabled())) {
                if (obj.getEnabled()) {
                    obj.setEnabledAt(LocalDateTime.now());
                    obj.setEnabledBy(user);
                } else {
                    obj.setDisabledAt(LocalDateTime.now());
                    obj.setDisabledBy(user);
                }
            }
        }

        if(obj.getStartDate() == null) {
            obj.setStartDate(LocalDateTime.now());
        }

        CuttingPlan newObj = service.save(obj, user);

        return new ResponseEntity<CuttingPlan>(newObj, HttpStatus.CREATED);
    }

    @Value("${lear.pltfolder}")
    private String pltfolder;

    @GetMapping("/ctc-info/{pn}")
    public List<DigitCtc> findByCtc(@PathVariable String pn) {
        List<DigitCtc> arr = new ArrayList<DigitCtc>();
        List<Files> arrFiles = filesService.findByPartNumberCover(pn);
        for (Files file : arrFiles) {
            DigitCtc obj = new DigitCtc();
            obj.setFile(file);
            List<String> arrText = new ArrayList<String>();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pltfolder + "" + file.getPattern() + ".plt"), "windows-1252"));
                String[] liste = br.lines().collect(Collectors.toList()).toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                for (int i = 1; i < liste.length; i++) {
                    arrText.add(liste[i]);
                }
            } catch (FileNotFoundException | UnsupportedEncodingException e) {

            }
            obj.setGraphContent(arrText);
            arr.add(obj);
        }
        return arr;
    }

    @GetMapping("/ctc-info/{pn}/{type}")
    public List<DigitCtc> findByCtcType(@PathVariable String pn, @PathVariable String type) {
        if (type.equals("fabric")) {
            List<DigitCtc> arr = new ArrayList<DigitCtc>();
            List<Files> arrFiles = filesService.findByPartNumberCover(pn);
            for (Files file : arrFiles) {
                DigitCtc obj = new DigitCtc();
                if(file.getType().equals("supplier kit leather") && file.getPartNumberMaterial().toUpperCase().startsWith("LEATHER")) {
                    file.setPartNumberMaterial("Leather");
                }
                if(file.getType().equals("CNC")) {
                    file.setPartNumberMaterial("CNC");
                }
                obj.setFile(file);
                List<String> arrText = new ArrayList<String>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(pltfolder + "" + file.getPattern() + ".plt"), "windows-1252"));
                    String[] liste = br.lines().collect(Collectors.toList()).toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                    for (int i = 1; i < liste.length; i++) {
                        arrText.add(liste[i]);
                    }
                } catch (FileNotFoundException | UnsupportedEncodingException e) {

                }
                obj.setGraphContent(arrText);
                arr.add(obj);
            }
            return arr;
        } else {
            List<DigitCtc> arr = new ArrayList<DigitCtc>();
            List<Files> arrFiles = filesService.findBySemiFinishedGoodPartNumber(pn);
            System.out.println("arrFiles : " + arrFiles.size());
            for (Files file : arrFiles) {
                DigitCtc obj = new DigitCtc();
                obj.setFile(file);
                List<String> arrText = new ArrayList<String>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(pltfolder + "" + file.getPattern() + ".plt"), "windows-1252"));
                    String[] liste = br.lines().collect(Collectors.toList()).toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                    for (int i = 1; i < liste.length; i++) {
                        arrText.add(liste[i]);
                    }
                } catch (FileNotFoundException | UnsupportedEncodingException e) {

                }
                obj.setGraphContent(arrText);
                arr.add(obj);
            }
            return arr;
        }
    }

    @GetMapping("/projets/{projets}")
    public List<CuttingPlanLight> findAllActiveInProjets(@PathVariable List<String> projets) {
        return service.findAllActiveInProjets(projets);
    }

    @GetMapping
    public List<CuttingPlanLight> findAllActive() {
        return service.findAllActive(LocalDateTime.now());
    }


}

class DigitCtc {
    private Files file;
    private List<String> graphContent;

    public DigitCtc() {
        super();
    }

    public Files getFile() {
        return file;
    }

    public void setFile(Files file) {
        this.file = file;
    }

    public List<String> getGraphContent() {
        return graphContent;
    }

    public void setGraphContent(List<String> graphContent) {
        this.graphContent = graphContent;
    }


}
