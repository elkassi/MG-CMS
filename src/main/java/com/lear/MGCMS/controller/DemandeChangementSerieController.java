package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.DemandeChangementSerie;
import com.lear.MGCMS.domain.QualityNotice;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.payload.Reference;
import com.lear.MGCMS.payload.StatsInfo;
import com.lear.MGCMS.services.*;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestDataService;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestSerieDataService;
import com.lear.MGCMS.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/demandeChangementSerie")
public class DemandeChangementSerieController {

    @Autowired
    private DemandeChangementSerieService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    private final StorageService storageService;

    @Value("${lear.emailCutting}")
    private String emailCutting;

    @Autowired
    public DemandeChangementSerieController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/listSeries")
    public ResponseEntity<?> listSeries(
            @RequestParam List<String> series
    ) {
        if (series == null || series.isEmpty()) {
            return new ResponseEntity<>("Series list is empty", HttpStatus.BAD_REQUEST);
        }
        List<DemandeChangementSerie> list = service.findBySerieIn(series);
//        if (list == null || list.isEmpty()) {
//            return new ResponseEntity<>("No series found", HttpStatus.NOT_FOUND);
//        }
        return new ResponseEntity<List<DemandeChangementSerie>>(list, HttpStatus.OK);
    }


    @GetMapping("/all")
    public Page<DemandeChangementSerie> findAll(
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
        filters.put("equal.active", "TRUE");
        return service.findAll(filters, page, size, sortBy);
    }

//    @GetMapping("/stats")
//    public List<StatsInfo> stats() {
//        return service.getStatsByType();
//    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findByCode(@PathVariable String id) {
        DemandeChangementSerie obj = service.findByObjId(id);
        if (obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<DemandeChangementSerie>(obj, HttpStatus.OK);
    }


    @PostMapping("/delete")
    public ResponseEntity<?> delete(@Valid @RequestBody DemandeChangementSerie obj, BindingResult result, Authentication authentication) {
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
        if (obj.getId() == null) {
            return new ResponseEntity<String>("BAD REQUEST", HttpStatus.BAD_REQUEST);
        }

        DemandeChangementSerie oldObj = service.findByObjId(obj.getId());
        if (oldObj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        oldObj.setActive(false);
        service.save(oldObj);

        return new ResponseEntity<String>("DELETED", HttpStatus.OK);
    }

    @Autowired
    private CuttingRequestSerieDataService cuttingRequestSerieDataService;
    //cuttingRequestDataService
    @Autowired
    private CuttingRequestDataService cuttingRequestDataService;
    @Value("${lear.linkServer}")
    private String linkServer;

    @Autowired
    private QueryService queryService;
    @Value("${lear.emailsProcessCoupe}")
    private String emailsProcessCoupe;
    //lear.emailsQualiteCoupe
    @Value("${lear.emailsQualiteCoupe}")
    private String emailsQualiteCoupe;
    //lear.emailsLogistique
    @Value("${lear.emailsLogistique}")
    private String emailsLogistique;

    @Value("${lear.emailsCAD}")
    private String emailsCAD;

    public static String extractMatricule(String creePar) {
        // Define the regex pattern to match the matricule inside parentheses
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(creePar);

        if (matcher.find()) {
            // Return the matched group inside parentheses
            return matcher.group(1);
        }
        return null; // Return null if no match is found
    }


    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody DemandeChangementSerie obj,
                                  BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        User user = userService.findByUsername(authentication.getName());

//        boolean authorized = false;
//        for (Role role : user.getRoles()) {
//            if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_ADMIN")) {
//                authorized = true;
//                break;
//            }
//        }
//        if (!authorized) {
//            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
//        }

        if(obj.getId() == null) {

            DemandeChangementSerie obj2 = new DemandeChangementSerie();
            List<String> listEmailTo = new ArrayList<>();
            List<String> listEmailCC = new ArrayList<>();

            obj2.setSerie(obj.getSerie());
            obj2.setTypeDemande(obj.getTypeDemande());
            obj2.setLaize(obj.getLaize());
            obj2.setMachine(obj.getMachine());
            obj2.setAutreChangement(obj.getAutreChangement());
            obj2.setDescription(obj.getDescription());
            CuttingRequestSerieData crs = cuttingRequestSerieDataService.findById(obj.getSerie());
            CuttingRequestData cr = cuttingRequestDataService.findBySequence(crs.getSequence());
            if(crs == null) {
                return new ResponseEntity<String>("Serie Not Found", HttpStatus.BAD_REQUEST);
            }
            obj2.setSequence(crs.getSequence());
            obj2.setPartNumberMaterial(crs.getPartNumberMaterial());
            obj2.setPartNumbers(crs.getPartNumbers());
            obj2.setPlacement(crs.getPlacement());
            obj2.setProjet(cr.getProjet());
            obj2.setLaizeOld(crs.getLaize());
            Reference ref = queryService.refDetails(crs.getPartNumberMaterial());
            if(ref != null) {
                try{
                    obj2.setLaizeContracuelle(Double.parseDouble(String.format("%.3f", Double.parseDouble(ref.getLaize()) / 1000)));
                }catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            Integer maxInd = service.getMaxInd(LocalDateTime.now().getYear());
            if (maxInd == null) {
                maxInd = 1;
            } else {
                maxInd++;
            }
            obj2.setId("D"+ String.format("%ty", LocalDateTime.now()) + String.format("%06d", maxInd));
            obj2.setInd(maxInd);
            obj2.setDateCreation(LocalDateTime.now());
            obj2.setCreePar(user.getLastName() + " "+ user.getFirstName()+ " ("+user.getMatricule()+")");
            boolean skipPrecess = false;
//            if(obj2.getMachine() != null
//                    && obj2.getMachine().getName().equalsIgnoreCase("Lectra IP6")
//                    && !obj2.getMachine().getName().equalsIgnoreCase(crs.getMachine())
//            ) {
//                List<String> allowedMachines = queryService.getAllowedMachinesByReftissu(obj2.getPartNumberMaterial());
//                if(allowedMachines.contains("Lectra IP6") && obj.getLaize() == null) {
//                    skipPrecess = true;
//                }
//            }
            if(skipPrecess) {
                obj2.setStatut("En attente de traitement du CAD");
                obj2.setReponseDepartement("NA");
                obj2.setDepartementValidation("NA");
                obj2.setConfirmeParDepartement("NA");
                obj2.setDateConfirmationDepartement(LocalDateTime.now());
                obj2.setCause("Machine déjà validée pour cette matière");
                obj2.setReponse("En attente");
                listEmailTo.add(emailsCAD);

            } else if(obj.getTypeDemande() != null
                    && obj.getTypeDemande().equalsIgnoreCase("Machine")
            ) {
                obj2.setDepartementValidation("Process");
                obj2.setStatut("En attente de la valication Process");
                obj2.setReponseDepartement("En attente");
                listEmailTo.add(emailsProcessCoupe);
            } else if(obj.getTypeDemande() != null
                    && (obj.getTypeDemande().startsWith("Diviser Matelas"))
            ) {
                obj2.setDepartementValidation("Qualité coupe");
                obj2.setStatut("En attente de la valication Qualité coupe");
                obj2.setReponseDepartement("En attente");

                obj2.setReponse("NA");
                obj2.setConfirmePar("NA");


                listEmailTo.add(emailsQualiteCoupe);
            } else if(obj.getTypeDemande() != null
                    && obj.getTypeDemande().startsWith("QLaize dérogé")
            ) {
                obj2.setDepartementValidation("Variance");
                obj2.setReponse("En attente");
                obj2.setStatut("En attente de traitement du CAD");
                obj2.setReponseDepartement("Validée");
                obj2.setConfirmeParDepartement(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
                obj2.setDateConfirmationDepartement(LocalDateTime.now());

                listEmailTo.add(emailsCAD);
            } else if(obj.getTypeDemande() != null && obj.getTypeDemande().startsWith("QLaize non dérogé")) {
                obj2.setDepartementValidation("Variance");
                obj2.setStatut("En attente de la valication Variance");
                obj2.setReponseDepartement("En attente");
                listEmailTo.add(emailsLogistique);
            } else if(obj.getTypeDemande() != null && obj.getTypeDemande().startsWith("Overlaize")) {
                obj2.setDepartementValidation("NA");
                obj2.setReponse("En attente");
                obj2.setStatut("En attente de traitement du CAD");
                obj2.setReponseDepartement("NA");
                obj2.setConfirmeParDepartement(null);
                obj2.setDateConfirmationDepartement(null);
                listEmailTo.add(emailsCAD);
            } else if(obj.getTypeDemande() != null && obj.getTypeDemande().equalsIgnoreCase("Changement de config")) {
                // This branch was unreachable before (duplicate Overlaize
                // condition): a Changement-de-config demande matched no branch,
                // saved with null statut and the config was never applied.
                if(obj.getConfig() == null || obj.getConfig().isEmpty()) {
                    return new ResponseEntity<String>("Config is required for Changement de config request", HttpStatus.BAD_REQUEST);
                }
                // verify that obj.getConfig() is different than crs.getConfig()
                if(obj.getConfig().equals(crs.getConfig())) {
                    return new ResponseEntity<String>("Config should be different than the current one", HttpStatus.BAD_REQUEST);
                }
                obj2.setDepartementValidation("NA");
                obj2.setReponse("NA");
                obj2.setStatut("Traitée");
                obj2.setReponseDepartement("NA");
                obj2.setConfirmeParDepartement("NA");
                obj2.setDateConfirmationDepartement(null);
                crs.setConfig(obj.getConfig());
                cuttingRequestSerieDataService.save(crs);
                listEmailTo.add(emailsCAD);
            } else if(obj.getTypeDemande() != null && obj.getTypeDemande().equalsIgnoreCase("Erreur métrage")) {
                // Metrage error found on the floor: the NOK length travels in
                // the laize field; Variance validates, then CAD treats.
                obj2.setDepartementValidation("Variance");
                obj2.setStatut("En attente de la valication Variance");
                obj2.setReponseDepartement("En attente");
                listEmailTo.add(emailsLogistique);
            }

            DemandeChangementSerie newObj = service.save(obj2);

            listEmailCC.add("melghazi@lear.com");
            listEmailCC.add("SGhailane@lear.com");
            List<File> arrFiles = new ArrayList<>();
            String emailContent = "<html>"
                    + "<body>"
                    + "<p>"
                    + "Demande de changement serie " + obj.getId() + " par " + user.getFirstName() + " " + user.getLastName() + "<br/>";
            emailContent += "<ul>";
            emailContent += "<li>Serie : " + obj.getSerie() + "</li>";
            emailContent += "<li>Sequence : " + (obj.getSequence() != null ? obj.getSequence() : "") + "</li>";
            emailContent += "<li>Part Number Material : " + (obj.getPartNumberMaterial()  != null ? obj.getPartNumberMaterial() : "") + "</li>";
            emailContent += "<li>Part Numbers : " + (obj.getPartNumbers() !=null ? obj.getPartNumbers() : "" )+ "</li>";
            emailContent += "<li>Laize : " + (obj.getLaize()  != null ? obj.getLaize() : "" )+ "</li>";
            emailContent += "<li>Machine : " + (obj.getMachine() != null ? obj.getMachine().getName(): "") + "</li>";
            emailContent += "<li>Description : " + (obj.getDescription() != null ? obj.getDescription() : "") + "</li>";
            emailContent += "</ul>";
            emailContent += "<br/>";
            emailContent += "Pour ouvrir le CMS WEB : <br/><a href='"+linkServer+"/demandeChangementSerie'>Cliquez ici pour ourir le tableau des demandes</a>";
            emailContent += "<br/>Cliquez <a href='"+linkServer+"/demandeChangementSerieValidation?id=" + obj.getId() + "'>ici pour ouvrir la demande "+obj.getId()+"</a>";
            emailContent += "</p>";
            emailContent += "</body>";
            emailContent += "</html>";


            emailService.sendEmailAttachment(listEmailTo, listEmailCC, newObj.getProjet() + " Demande de changement serie : " + obj.getId(), emailContent, arrFiles);

            return new ResponseEntity<DemandeChangementSerie>(newObj, HttpStatus.CREATED);
        }
        return new ResponseEntity<String>("can't change", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/departementValider")
    public ResponseEntity<?> departementValider(@Valid @RequestBody DemandeChangementSerie obj, BindingResult result, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        DemandeChangementSerie oldObj = service.findByObjId(obj.getId());

        if(oldObj.getReponseDepartement() == null || !oldObj.getReponseDepartement().equalsIgnoreCase("En attente")) {
            return new ResponseEntity<>("Demande n'est pas en attente", HttpStatus.BAD_REQUEST);
        }
        String roleToBeSearched = null;
         if(oldObj.getTypeDemande() == null) {
            return new ResponseEntity<>("Type de demande est obligatoire", HttpStatus.BAD_REQUEST);
        }
         if(oldObj.getTypeDemande().equalsIgnoreCase("Machine")) {
            roleToBeSearched = "ROLE_PROCESS";
        } else if(oldObj.getTypeDemande().startsWith("Diviser Matelas")) {
            roleToBeSearched = "ROLE_QUALITE";
        } else if(oldObj.getTypeDemande().startsWith("QLaize")) {
            roleToBeSearched = "ROLE_VARIANCE";
        } else {
            return new ResponseEntity<>("Type de demande non supporté", HttpStatus.BAD_REQUEST);
        }

         // search user.getRoles
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals(roleToBeSearched)) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        if(oldObj.getTypeDemande() != null
                && (obj.getTypeDemande().startsWith("Diviser Matelas"))
        ) {
            oldObj.setReponse("NA");
            oldObj.setConfirmePar("NA");
            oldObj.setStatut("Traitée");
        } else {
            oldObj.setReponse("En attente");
            oldObj.setStatut("En attente de traitement du CAD");
        }

        oldObj.setReponseDepartement("Validée");
        oldObj.setConfirmeParDepartement(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        oldObj.setDateConfirmationDepartement(LocalDateTime.now());
        oldObj.setCause(obj.getCause());
        DemandeChangementSerie newObj = service.save(oldObj);
        List<String> listEmailTo = new ArrayList<>();
        List<String> listEmailCC = new ArrayList<>();
        listEmailTo.add(emailsCAD);
        try {
            User creeBy = userService.findByUsername(extractMatricule(newObj.getCreePar()));
            listEmailCC.add(creeBy.getEmail());
        } catch (Exception e) {

        }
        listEmailCC.add("melghazi@lear.com");
        listEmailCC.add("SGhailane@lear.com");
        List<File> arrFiles = new ArrayList<>();
        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Demande de changement serie " + newObj.getId() + " est validée par " + user.getFirstName() + " " + user.getLastName() + "<br/>";
        emailContent += "<ul>";
        emailContent += "<li>Serie : " + newObj.getSerie() + "</li>";
        emailContent += "<li>Sequence : " + (newObj.getSequence() != null ? newObj.getSequence() : "") + "</li>";
        emailContent += "<li>Part Number Material : " + (newObj.getPartNumberMaterial()  != null ? newObj.getPartNumberMaterial() : "") + "</li>";
        emailContent += "<li>Part Numbers : " + (newObj.getPartNumbers() !=null ? newObj.getPartNumbers() : "" )+ "</li>";
        emailContent += "<li>Laize : " + (newObj.getLaize()  != null ? newObj.getLaize() : "" )+ "</li>";
        emailContent += "<li>Machine : " + (newObj.getMachine() != null ? newObj.getMachine().getName(): "") + "</li>";
        emailContent += "<li>Description : " + (newObj.getDescription() != null ? newObj.getDescription() : "") + "</li>";
        emailContent += "</ul>";
        emailContent += "<br/>";
        emailContent += "Pour ouvrir le CMS WEB : <br/><a href='"+linkServer+"/demandeChangementSerie'>Cliquez ici pour ourir le tableau des demandes</a>";
        emailContent += "<br/>Cliquez <a href='"+linkServer+"/demandeChangementSerieValidation?id=" + obj.getId() + "'>ici pour ouvrir la demande "+obj.getId()+"</a>";
        emailContent += "</p>";
        emailContent += "</body>";
        emailContent += "</html>";

        emailService.sendEmailAttachment(listEmailTo, listEmailCC, newObj.getProjet() + " Demande de changement serie validée : " + newObj.getId(), emailContent, arrFiles);

        return  new ResponseEntity<DemandeChangementSerie>(newObj, HttpStatus.OK);
    }

    @PostMapping("/departementRefuser")
    public ResponseEntity<?> departementRefuser(@Valid @RequestBody DemandeChangementSerie obj, BindingResult result, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        DemandeChangementSerie oldObj = service.findByObjId(obj.getId());
        if(oldObj.getReponseDepartement() == null || !oldObj.getReponseDepartement().equalsIgnoreCase("En attente")) {
            return new ResponseEntity<>("Demande n'est pas en attente", HttpStatus.BAD_REQUEST);
        }

        if(oldObj.getReponseDepartement() == null || !oldObj.getReponseDepartement().equalsIgnoreCase("En attente")) {
            return new ResponseEntity<>("Demande n'est pas en attente", HttpStatus.BAD_REQUEST);
        }
        String roleToBeSearched = null;
        if(oldObj.getTypeDemande() == null) {
            return new ResponseEntity<>("Type de demande est obligatoire", HttpStatus.BAD_REQUEST);
        }
        if(oldObj.getTypeDemande().equalsIgnoreCase("Machine")) {
            roleToBeSearched = "ROLE_PROCESS";
        } else if(oldObj.getTypeDemande().startsWith("Diviser Matelas")) {
            roleToBeSearched = "ROLE_QUALITE";
        } else if(oldObj.getTypeDemande().startsWith("QLaize")) {
            roleToBeSearched = "ROLE_VARIANCE";
        } else {
            return new ResponseEntity<>("Type de demande non supporté", HttpStatus.BAD_REQUEST);
        }

        // search user.getRoles
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals(roleToBeSearched)) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        oldObj.setReponseDepartement("Refusée");
        oldObj.setConfirmeParDepartement(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        oldObj.setDateConfirmationDepartement(LocalDateTime.now());
        oldObj.setStatut("Refusée");
        oldObj.setCause(obj.getCause());
        DemandeChangementSerie newObj = service.save(oldObj);
        List<String> listEmailTo = new ArrayList<>();
        List<String> listEmailCC = new ArrayList<>();
        listEmailTo.add(emailsCAD);
        try {
            User creeBy = userService.findByUsername(extractMatricule(newObj.getCreePar()));
            listEmailCC.add(creeBy.getEmail());
        } catch (Exception e) {

        }
        listEmailCC.add("M");
        listEmailCC.add("SGhailane@lear.com");
        List<File> arrFiles = new ArrayList<>();
        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Demande de changement serie " + newObj.getId() + " est refusée par " + user.getFirstName() + " " + user.getLastName() + "<br/>";
        emailContent += "<ul>";
        emailContent += "<li>Serie : " + newObj.getSerie() + "</li>";
        emailContent += "<li>Sequence : " + (newObj.getSequence() != null ? newObj.getSequence() : "") + "</li>";
        emailContent += "<li>Part Number Material : " + (newObj.getPartNumberMaterial()  != null ? newObj.getPartNumberMaterial() : "") + "</li>";
        emailContent += "<li>Part Numbers : " + (newObj.getPartNumbers() !=null ? newObj.getPartNumbers() : "" )+ "</li>";
        emailContent += "<li>Laize : " + (newObj.getLaize()  != null ? newObj.getLaize() : "" )+ "</li>";
        emailContent += "<li>Machine : " + (newObj.getMachine() != null ? newObj.getMachine().getName(): "") + "</li>";
        emailContent += "<li>Description : " + (newObj.getDescription() != null ? newObj.getDescription() : "") + "</li>";
        emailContent += "</ul>";
        emailContent += "<br/>";
        emailContent += "Pour ouvrir le CMS WEB : <br/><a href='"+linkServer+"/demandeChangementSerie'>Cliquez ici pour ourir le tableau des demandes</a>";
        emailContent += "<br/>Cliquez <a href='"+linkServer+"/demandeChangementSerieValidation?id=" + obj.getId() + "'>ici pour ouvrir la demande "+obj.getId()+"</a>";
        emailContent += "</p>";
        emailContent += "</body>";
        emailContent += "</html>";

        emailService.sendEmailAttachment(listEmailTo, listEmailCC, newObj.getProjet() + " Demande de changement serie refusée : " + newObj.getId(), emailContent, arrFiles);
        return  new ResponseEntity<DemandeChangementSerie>(newObj, HttpStatus.OK);
    }

    @PostMapping("/cadValider")
    @PreAuthorize("hasRole('CAD')")
    public ResponseEntity<?> cadValider(
            @Valid @RequestBody DemandeChangementSerie obj,
            BindingResult result, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        DemandeChangementSerie oldObj = service.findByObjId(obj.getId());
        if(oldObj.getReponse() == null || !oldObj.getReponse().equalsIgnoreCase("En attente")) {
            return new ResponseEntity<>("Demande n'est pas en attente", HttpStatus.BAD_REQUEST);
        }
        oldObj.setConfirmePar(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        oldObj.setDateConfirmation(LocalDateTime.now());
        oldObj.setReponse("Traitée");
        oldObj.setStatut("Traitée");
        oldObj.setNewPlacement(obj.getNewPlacement());
        DemandeChangementSerie newObj = service.save(oldObj);

        List<String> listEmailTo = new ArrayList<>();
        List<String> listEmailCC = new ArrayList<>();
        try {
            User creeBy = userService.findByUsername(extractMatricule(newObj.getCreePar()));
            listEmailTo.add(creeBy.getEmail());
        } catch (Exception e) {

        }
        String[] emails = emailCutting.split(";");
        for (String email : emails) {
            if (!email.isEmpty() && !listEmailTo.contains(email)) {
                listEmailTo.add(email);
            }
        }

        listEmailCC.add("melghazi@lear.com");
        listEmailCC.add("SGhailane@lear.com");
        List<File> arrFiles = new ArrayList<>();
        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Demande de changement serie " + newObj.getId() + " est traitée par " + user.getFirstName() + " " + user.getLastName() + "<br/>";
        emailContent += "<ul>";
        emailContent += "<li>Serie : " + newObj.getSerie() + "</li>";
        emailContent += "<li>Sequence : " + (newObj.getSequence() != null ? newObj.getSequence() : "") + "</li>";
        emailContent += "<li>Part Number Material : " + (newObj.getPartNumberMaterial()  != null ? newObj.getPartNumberMaterial() : "") + "</li>";
        emailContent += "<li>Part Numbers : " + (newObj.getPartNumbers() !=null ? newObj.getPartNumbers() : "" )+ "</li>";
        emailContent += "<li>Laize : " + (newObj.getLaize()  != null ? newObj.getLaize() : "" )+ "</li>";
        emailContent += "<li>Machine : " + (newObj.getMachine() != null ? newObj.getMachine().getName(): "") + "</li>";
        emailContent += "<li>Autre Changement : " + (newObj.getAutreChangement() != null ? newObj.getAutreChangement() : "") + "</li>";
        emailContent += "<li>Description : " + (newObj.getDescription() != null ? newObj.getDescription() : "") + "</li>";
        emailContent += "<li>Placement : " + (newObj.getPlacement() != null ? newObj.getPlacement() : "") + "</li>";
        emailContent += "<li>Reponse Departement : " + (newObj.getReponseDepartement() != null ? newObj.getReponseDepartement() : "") + "</li>";
        emailContent += "<li>Confirme Par Departement : " + (newObj.getConfirmeParDepartement() != null ? newObj.getConfirmeParDepartement() : "") + "</li>";
        emailContent += "<li>Date Confirmation Departement : " + (newObj.getDateConfirmationDepartement() != null ? newObj.getDateConfirmationDepartement().toString() : "") + "</li>";
        emailContent += "<li>Cause : " + (newObj.getCause() != null ? newObj.getCause() : "") + "</li>";
        emailContent += "<li>New Placement : " + (newObj.getNewPlacement() != null ? newObj.getNewPlacement() : "") + "</li>";
        emailContent += "<li>Reponse : " + (newObj.getReponse() != null ? newObj.getReponse() : "") + "</li>";
        emailContent += "<li>Confirme Par : " + (newObj.getConfirmePar() != null ? newObj.getConfirmePar() : "") + "</li>";
        emailContent += "<li>Date Confirmation : " + (newObj.getDateConfirmation() != null ? newObj.getDateConfirmation().toString() : "") + "</li>";

        emailContent += "</ul>";
        emailContent += "<br/>";
        emailContent += "Pour ouvrir le CMS WEB : <br/><a href='"+linkServer+"/demandeChangementSerie'>Cliquez ici pour ourir le tableau des demandes</a>";
        emailContent += "<br/>Cliquez <a href='"+linkServer+"/demandeChangementSerieValidation?id=" + obj.getId() + "'>ici pour ouvrir la demande "+obj.getId()+"</a>";
        emailContent += "</p>";
        emailContent += "</body>";
        emailContent += "</html>";

        emailService.sendEmailAttachment(listEmailTo, listEmailCC, newObj.getProjet() + " Demande de changement serie est traitée : " + newObj.getId(), emailContent, arrFiles);
        return  new ResponseEntity<DemandeChangementSerie>(newObj, HttpStatus.OK);
    }

    @PostMapping("/cadRefuser")
    @PreAuthorize("hasRole('CAD')")
    public ResponseEntity<?> cadRefuser(@Valid @RequestBody DemandeChangementSerie obj, BindingResult result, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        DemandeChangementSerie oldObj = service.findByObjId(obj.getId());
        if(oldObj.getReponse() == null || !oldObj.getReponse().equalsIgnoreCase("En attente")) {
            return new ResponseEntity<>("Demande n'est pas en attente", HttpStatus.BAD_REQUEST);
        }
        oldObj.setConfirmePar(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        oldObj.setDateConfirmation(LocalDateTime.now());
        oldObj.setReponse("Refusée");
        oldObj.setStatut("Refusée");
        DemandeChangementSerie newObj = service.save(oldObj);

        List<String> listEmailTo = new ArrayList<>();
        List<String> listEmailCC = new ArrayList<>();
        try {
            User creeBy = userService.findByUsername(extractMatricule(newObj.getCreePar()));
            listEmailTo.add(creeBy.getEmail());
        } catch (Exception e) {

        }
        listEmailCC.add("melghazi@lear.com");
        listEmailCC.add("SGhailane@lear.com");
        List<File> arrFiles = new ArrayList<>();
        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Demande de changement serie " + newObj.getId() + " est traitée par " + user.getFirstName() + " " + user.getLastName() + "<br/>";
        emailContent += "<ul>";
        emailContent += "<li>Serie : " + newObj.getSerie() + "</li>";
        emailContent += "<li>Sequence : " + (newObj.getSequence() != null ? newObj.getSequence() : "") + "</li>";
        emailContent += "<li>Part Number Material : " + (newObj.getPartNumberMaterial()  != null ? newObj.getPartNumberMaterial() : "") + "</li>";
        emailContent += "<li>Part Numbers : " + (newObj.getPartNumbers() !=null ? newObj.getPartNumbers() : "" )+ "</li>";
        emailContent += "<li>Laize : " + (newObj.getLaize()  != null ? newObj.getLaize() : "" )+ "</li>";
        emailContent += "<li>Machine : " + (newObj.getMachine() != null ? newObj.getMachine().getName(): "") + "</li>";
        emailContent += "<li>Autre Changement : " + (newObj.getAutreChangement() != null ? newObj.getAutreChangement() : "") + "</li>";
        emailContent += "<li>Description : " + (newObj.getDescription() != null ? newObj.getDescription() : "") + "</li>";
        emailContent += "<li>Placement : " + (newObj.getPlacement() != null ? newObj.getPlacement() : "") + "</li>";
        emailContent += "<li>Reponse Departement : " + (newObj.getReponseDepartement() != null ? newObj.getReponseDepartement() : "") + "</li>";
        emailContent += "<li>Confirme Par Departement : " + (newObj.getConfirmeParDepartement() != null ? newObj.getConfirmeParDepartement() : "") + "</li>";
        emailContent += "<li>Date Confirmation Departement : " + (newObj.getDateConfirmationDepartement() != null ? newObj.getDateConfirmationDepartement().toString() : "") + "</li>";
        emailContent += "<li>Cause : " + (newObj.getCause() != null ? newObj.getCause() : "") + "</li>";
        emailContent += "<li>New Placement : " + (newObj.getNewPlacement() != null ? newObj.getNewPlacement() : "") + "</li>";
        emailContent += "<li>Reponse : " + (newObj.getReponse() != null ? newObj.getReponse() : "") + "</li>";
        emailContent += "<li>Confirme Par : " + (newObj.getConfirmePar() != null ? newObj.getConfirmePar() : "") + "</li>";
        emailContent += "<li>Date Confirmation : " + (newObj.getDateConfirmation() != null ? newObj.getDateConfirmation().toString() : "") + "</li>";

        emailContent += "</ul>";
        emailContent += "<br/>";
        emailContent += "Pour ouvrir le CMS WEB : <br/><a href='"+linkServer+"/demandeChangementSerie'>Cliquez ici pour ourir le tableau des demandes</a>";
        emailContent += "<br/>Cliquez <a href='"+linkServer+"/demandeChangementSerieValidation?id=" + obj.getId() + "'>ici pour ouvrir la demande "+obj.getId()+"</a>";
        emailContent += "</p>";
        emailContent += "</body>";
        emailContent += "</html>";

        emailService.sendEmailAttachment(listEmailTo, listEmailCC, newObj.getProjet() + " Demande de changement serie est refusée par CAD : " + newObj.getId(), emailContent, arrFiles);


        return  new ResponseEntity<DemandeChangementSerie>(newObj, HttpStatus.OK);
    }

}
