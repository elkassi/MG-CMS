package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.Intervention;
import com.lear.MGCMS.domain.QualityNotice;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.payload.StatsInfo;
import com.lear.MGCMS.services.EmailService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.QualityNoticeService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.storage.StorageService;
import com.lear.ctc.domain.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qualityNotice")
public class QualityNoticeController {

    private static final Logger log = LoggerFactory.getLogger(QualityNoticeController.class);

    @Autowired
    private QualityNoticeService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    private final StorageService storageService;

    @Autowired
    public QualityNoticeController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Value("${lear.emailsQualiteCoupe}")
    private String emailsQualiteCoupe;
    @Value("${lear.emailsQualiteReception}")
    private String emailsQualiteReception;
    @Value("${lear.emailsLogistique}")
    private String emailsLogistique;
    @Value("${lear.linkServer}")
    private String linkServer;
    //lear.emailCutting
    @Value("${lear.emailCutting}")
    private String emailCutting;

    @GetMapping("/all")
    public Page<QualityNotice> findAll(
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
        return service.findAll2(filters, page, size, sortBy);
    }

    @GetMapping("/stats")
    public List<StatsInfo> stats() {
        return service.getStatsBySites();
    }

    @GetMapping("/statsByTypeDefaut")
    public List<StatsInfo> statsByType() {
        return service.getStatsByTypeDefaut();
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> findByCode(@PathVariable String id) {
        QualityNotice obj = service.findByObjId(id);
        if (obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<QualityNotice>(obj, HttpStatus.OK);
    }


    @PostMapping("/qnSave")
    public ResponseEntity<?> qnSave(@Valid @RequestBody QualityNotice obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;
        List<String> listEmailTo = new ArrayList<>();
        List<String> listEmailCC = new ArrayList<>();

        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_QN")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        if (obj.getNumeroQn() == null) {

            if((obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC")) && (obj.getNumEmp() == null || obj.getNumEmp().isEmpty())) {
                return new ResponseEntity<String>("il faut choisir les empiècements pour les défauts coupes", HttpStatus.BAD_REQUEST);
            }

            obj.setActive(true);
            Integer maxInd = service.getMaxIndBySite(obj.getSite());
            if (maxInd == null) maxInd = 0;
            obj.setNumeroQn(obj.getSite().toUpperCase() + "-N°" + (maxInd + 1));
            obj.setInd(maxInd + 1);
            obj.setCreatedAt(LocalDateTime.now());
            obj.setCreatedBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
            if(obj.getImage1() == null) {
                return new ResponseEntity<String>("Image1 est obligatoire", HttpStatus.BAD_REQUEST);
            }
            obj.setReponse("En attente de la validation qualité coupe");
        } else {
            // return bad request if trying to update
            return new ResponseEntity<String>("BAD REQUEST", HttpStatus.BAD_REQUEST);
        }
        if (obj.getExtraEmails() != null) {
            String[] emails = obj.getExtraEmails().split(";");
            for (String email : emails) {
                if (email.length() > 0 && !listEmailCC.contains(email)) {
                    listEmailCC.add(email);
                }
            }
            obj.setExtraEmails(obj.getExtraEmails()+";"+user.getEmail());
        } else {
            obj.setExtraEmails(user.getEmail());
            listEmailCC.add(user.getEmail());
        }

        QualityNotice newObj = service.save(obj);
        if(obj.getTypeDefaut()!= null && (obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC"))){
            for(String str : emailsQualiteCoupe.split(";")) {
                listEmailTo.add(str);
            }
        }
        if(obj.getTypeDefaut()!= null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")){
            for(String str : emailsQualiteReception.split(";")) {
                listEmailTo.add(str);
            }
        }
        if(obj.getTypeDefaut()!= null && obj.getTypeDefaut().equalsIgnoreCase("Défaut logistique")){
            for(String str : emailsLogistique.split(";")) {
                listEmailTo.add(str);
            }
        }

        // add in cc the emailCutting
        String[] emailCuttingArr = emailCutting.split(";");
        for(String email : emailCuttingArr) {
            if(!listEmailCC.contains(email)) {
                listEmailCC.add(email);
            }
        }

        if (obj.getSite().equalsIgnoreCase("TRIM2")) {
            listEmailCC = new ArrayList<>(List.of("SDL-TNR38@lear.com", "SDL-TNR42@lear.com"));
        } else if (obj.getSite().equalsIgnoreCase("TRIM4")) {
            listEmailCC = new ArrayList<>(List.of("SDL-TNR59@lear.com", "SDL-TNR40@lear.com"));
        } else if (obj.getSite().equalsIgnoreCase("TRIM1")) {
            if(obj.getTypeDefaut()!= null && (obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC"))) {
                listEmailCC = new ArrayList<>();
            } else {
                listEmailCC = new ArrayList<>(List.of("SDL-TNR31@lear.com", "SDL-TNR21@lear.com"));
            }
        } else if (obj.getSite().equalsIgnoreCase("FOAM")) {
            listEmailCC = new ArrayList<>(List.of("SDL-TNR36@lear.com", "NElhamraoui@lear.com"));
        }
        if(!listEmailTo.contains("SDL-TNR31@lear.com") && !listEmailTo.contains("SDL-TNR31@lear.com")) {
            listEmailTo.add("SDL-TNR31@lear.com");
        }


    


        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Nouveau QN " + obj.getNumeroQn() + " par " + user.getFirstName() + " " + user.getLastName() + "<br/>";

        emailContent += "<ul>";
        emailContent += "<li>Site : " + obj.getSite() + "</li>";
        emailContent += "<li>WO : " + obj.getWo() + "</li>";
        emailContent += "<li>Coordinateur : " + obj.getCoordinateur() + "</li>";
        emailContent += "<li>Created by : " + obj.getCreatedBy() + "</li>";
        emailContent += "<li>Created at : " + obj.getCreatedAt() + "</li>";
        emailContent += "<li>Sequence : " + obj.getSequence() + "</li>";
        emailContent += "<li>Part Number : " + obj.getPartnumber() + "</li>";
        emailContent += "<li>Projet : " + obj.getProjet() + "</li>";
        emailContent += "<li>Num Emp : " + obj.getNumEmp() + "</li>";
        emailContent += "<li>Quantite : " + obj.getQuantite() + "</li>";
        emailContent += "<li>Ref Tissu : " + obj.getReftissu() + "</li>";
        emailContent += "<li>Ref Tissu Description : " + obj.getReftissuDescription() + "</li>";
        emailContent += "<li>Fournisseur : " + obj.getNomFournisseur() + "</li>";
        emailContent += "<li>Type Defaut : " + obj.getTypeDefaut() + "</li>";
        if(obj.getCodeDefaut() != null) {
            emailContent += "<li>Code Defaut : " + obj.getCodeDefaut().getCode() + " : " + obj.getCodeDefaut().getDescription() + "</li>";
        }

        if(obj.getTypeDefaut() != null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")) {
            // add idRouleau , lotFrs, dateCoupe
            emailContent += "<li>Id Rouleau : " + obj.getIdRouleau() + "</li>";
            emailContent += "<li>Lot Fournisseur : " + obj.getLotFrs() + "</li>";
            emailContent += "<li>Date Coupe : " + obj.getDateCoupe() + "</li>";
        }

        emailContent += "<li>Description : " + obj.getDescription() + "</li>";

        emailContent += "</ul>";
        emailContent += "<br/>";
        emailContent += "Pour ouvrir le QN cliquez <a href='"+linkServer+"/qualityNoticeValidation?numeroQn=" + obj.getNumeroQn() + "'>ici</a>";
        emailContent += "</p>";
        emailContent += "</body>";
        emailContent += "</html>";
        List<File> arrFiles = new ArrayList<>();
        if(obj.getImage1() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage1());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(obj.getImage2() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage2());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        emailService.sendEmailAttachment(listEmailTo, listEmailCC, "Lear Quality Notice : " + obj.getNumeroQn(), emailContent, arrFiles);

        return new ResponseEntity<QualityNotice>(newObj, HttpStatus.CREATED);
    }


    @PostMapping("/valider")
    public ResponseEntity<?> valider(@Valid @RequestBody QualityNotice obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        if(obj.getReponse() == null || obj.getReponse().isEmpty() || obj.getReponse().startsWith("En attente")) {
            return new ResponseEntity<String>("Il faut choisir votre réponse", HttpStatus.NOT_FOUND);
        }

        User user = userService.findByUsername(authentication.getName());
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equalsIgnoreCase("ROLE_QUALITE") && (obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC"))) {
                authorized = true;
                break;
            }
            if (role.getName().equalsIgnoreCase("ROLE_VALID_QN_FOURNISSEUR") && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")) {
                authorized = true;
                break;
            }
            if (role.getName().equalsIgnoreCase("ROLE_VALID_QN_LOGISTIQUE") && obj.getTypeDefaut().equalsIgnoreCase("Défaut logistique")) {
                authorized = true;
                break;
            }

        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        QualityNotice oldObj = service.findByObjId(obj.getNumeroQn());
        if (oldObj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        oldObj.setReftissuDescription(obj.getReftissuDescription());
        //Nom Fournisseur, ID Rouleau, Lot Frs, dateCoupe
        oldObj.setNomFournisseur(obj.getNomFournisseur());
        oldObj.setIdRouleau(obj.getIdRouleau());
        oldObj.setLotFrs(obj.getLotFrs());
        oldObj.setDateCoupe(obj.getDateCoupe());

        oldObj.setMachine(obj.getMachine());
        oldObj.setCorrectDefaut(obj.getCorrectDefaut());
        oldObj.setQteRecu(obj.getQteRecu());
        oldObj.setQteRecuCoiffe(obj.getQteRecuCoiffe());
        oldObj.setQteRecuMetrage(obj.getQteRecuMetrage());
        //qrqc
        oldObj.setQrqc(obj.getQrqc());
        oldObj.setReponse(obj.getReponse());
        oldObj.setDecision(obj.getDecision());
        oldObj.setSecurisation(obj.getSecurisation());
        oldObj.setRemarque(obj.getRemarque());
        oldObj.setTraiterPar(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        oldObj.setDateTraitement(LocalDateTime.now());
        oldObj.setExtraEmailsReponse(obj.getExtraEmailsReponse());
        oldObj.setFichier(obj.getFichier());
        QualityNotice newObj = service.save(oldObj);
        // list of String emails initial by SDL-TNR31@lear.com, SDL-TNR21@lear.com , SDL-TNG28@lear.com, SDL-TNG26@lear.com
        List<String> listEmailCC = new ArrayList<>();
        if(obj.getTypeDefaut()!= null && (obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC"))){
            for(String str : emailsQualiteCoupe.split(";")) {
                listEmailCC.add(str);
            }
        }
        if(obj.getTypeDefaut()!= null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")){
            for(String str : emailsQualiteReception.split(";")) {
                listEmailCC.add(str);
            }
        }
        if(obj.getTypeDefaut()!= null && obj.getTypeDefaut().equalsIgnoreCase("Défaut logistique")){
            for(String str : emailsLogistique.split(";")) {
                listEmailCC.add(str);
            }
        }

        // add in cc the emailCutting
        String[] emailCuttingArr = emailCutting.split(";");
        for(String email : emailCuttingArr) {
            if(!listEmailCC.contains(email)) {
                listEmailCC.add(email);
            }
        }

        List<String> listEmailTo = new ArrayList<>();
        if (oldObj.getSite().equalsIgnoreCase("TRIM2")) {
            listEmailTo = new ArrayList<>(List.of("SDL-TNR38@lear.com", "SDL-TNR42@lear.com"));
        } else if (oldObj.getSite().equalsIgnoreCase("TRIM4")) {
            listEmailTo = new ArrayList<>(List.of("SDL-TNR59@lear.com", "SDL-TNR40@lear.com"));
        } else if (oldObj.getSite().equalsIgnoreCase("TRIM1")) {
            if(obj.getTypeDefaut()!= null && (obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC"))) {
                listEmailTo = new ArrayList<>(List.of("SDL-TNG28@lear.com", "SDL-TNG26@lear.com", "SDL-TNG40@lear.com"));
            }else {
                listEmailTo = new ArrayList<>(List.of("SDL-TNG28@lear.com","SDL-TNR31@lear.com", "SDL-TNR21@lear.com"));
            }
            listEmailCC = new ArrayList<>();
        } else if (oldObj.getSite().equalsIgnoreCase("FOAM")) {
            listEmailTo = new ArrayList<>(List.of("SDL-TNR36@lear.com", "NElhamraoui@lear.com"));
        }
        // add oldObj.getExtraEmails() to listEmail by spliting by ;
        try {
            if (oldObj.getExtraEmails() != null) {
                String[] emails = oldObj.getExtraEmails().split(";");
                for (String email : emails) {
                    if (email != null && email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }
        try {
            if (oldObj.getExtraEmailsReponse() != null) {
                String[] emails = oldObj.getExtraEmailsReponse().split(";");
                for (String email : emails) {
                    if (email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Validation du QN " + oldObj.getNumeroQn() + " par " + user.getFirstName() + " " + user.getLastName() + "<br/>"
                // show the details of the QN

                + "<ul>";
        emailContent += "<li>Site : " + obj.getSite() + "</li>";
        emailContent += "<li>WO : " + obj.getWo() + "</li>";
        emailContent += "<li>Coordinateur : " + obj.getCoordinateur() + "</li>";
        emailContent += "<li>Created by : " + obj.getCreatedBy() + "</li>";
        emailContent += "<li>Created at : " + obj.getCreatedAt() + "</li>";
        emailContent += "<li>Sequence : " + obj.getSequence() + "</li>";
        emailContent += "<li>Part Number : " + obj.getPartnumber() + "</li>";
        emailContent += "<li>Projet : " + obj.getProjet() + "</li>";
        emailContent += "<li>Num Emp : " + obj.getNumEmp() + "</li>";
        emailContent += "<li>Quantite : " + obj.getQuantite() + "</li>";
        emailContent += "<li>Ref Tissu : " + obj.getReftissu() + "</li>";
        emailContent += "<li>Ref Tissu Description : " + obj.getReftissuDescription() + "</li>";
        emailContent += "<li>Fournisseur : " + obj.getNomFournisseur() + "</li>";
        emailContent += "<li>Type Defaut : " + obj.getTypeDefaut() + "</li>";
        if(obj.getCodeDefaut() != null) {
            emailContent += "<li>Code Defaut : " + obj.getCodeDefaut().getCode() + " : " + obj.getCodeDefaut().getDescription() + "</li>";
        }

        if(obj.getTypeDefaut() != null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")) {
            // add idRouleau , lotFrs, dateCoupe
            emailContent += "<li>Id Rouleau : " + obj.getIdRouleau() + "</li>";
            emailContent += "<li>Lot Fournisseur : " + obj.getLotFrs() + "</li>";
            emailContent += "<li>Date Coupe : " + obj.getDateCoupe() + "</li>";
        }

        emailContent += "<li>Description : " + obj.getDescription() + "</li>";        emailContent += "<hr/>"
                + "<li>Machine : " + oldObj.getMachine() + "</li>"
                + "<li>Correct Defaut : " + oldObj.getCorrectDefaut().getCode() + " : " + oldObj.getCorrectDefaut().getDescription() + "</li>"
                + "<li>Quantité reçu : " + oldObj.getQteRecu() + "</li>"
                + "<li>Quantité reçu coiffes : " + oldObj.getQteRecu() + "</li>"
                + "<li>Quantité reçu métrage : " + oldObj.getQteRecuMetrage() + "</li>"
                // qrqc
                + "<li>QRQC : " + (oldObj.getQrqc() == true ? "OUI" : "NON") + "</li>"
                + "<li>Reponse : " + oldObj.getReponse() + "</li>"
                + "<li>Decision : " + oldObj.getDecision() + "</li>"
                + "<li>Securisation : " + oldObj.getSecurisation() + "</li>"
                + "<li>Remarque / CAUSE POTENTIEL : " + oldObj.getRemarque() + "</li>"
                + "<li>Traité Par : " + oldObj.getTraiterPar() + "</li>"
                + "<li>Date de traitement : " + oldObj.getDateTraitement().format(formatter) + "</li>"

                + "</ul>"
                + "<br/>"
                + "Pour ouvrir le QN cliquez <a href='"+linkServer+"/qualityNoticeValidation?numeroQn=" + oldObj.getNumeroQn() + "'>ici</a>"
                + "</p>"
                + "</body>"
                + "</html>";
        List<File> arrFiles = new ArrayList<>();
        if(oldObj.getFichier() != null) {
            Resource rc = storageService.loadAsResource(oldObj.getFichier());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(oldObj.getImage1() != null) {
            Resource rc = storageService.loadAsResource(oldObj.getImage1());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(oldObj.getImage2() != null) {
            Resource rc = storageService.loadAsResource(oldObj.getImage2());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
//        if(!listEmailCC.contains("melghazi@lear.com")) {
//            listEmailCC.add("melghazi@lear.com");
//        }
//        if(!listEmailCC.contains("sghailane@lear.com")) {
//            listEmailCC.add("sghailane@lear.com");
//        }
        listEmailCC.add(user.getEmail());

        emailService.sendEmailAttachment(listEmailTo, listEmailCC, (oldObj.getQrqc() == true ? "Demande QRQC : " : "") + "Lear Quality Notice : Validation du " + oldObj.getNumeroQn() , emailContent,arrFiles);

        return new ResponseEntity<QualityNotice>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/sendNotification/{numeroQn}")
    public ResponseEntity<?> sendNotification(@PathVariable String numeroQn,Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_QUALITE")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        QualityNotice obj = service.findByObjId(numeroQn);
        if (obj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        if (obj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        // list of String emails initial by SDL-TNR31@lear.com, SDL-TNR21@lear.com , SDL-TNG28@lear.com, SDL-TNG26@lear.com
        List<String> listEmailCC = new ArrayList<>();
        if(obj.getTypeDefaut()!= null && (obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC"))){
            for(String str : emailsQualiteCoupe.split(";")) {
                listEmailCC.add(str);
            }
        }
        if(obj.getTypeDefaut()!= null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")){
            for(String str : emailsQualiteReception.split(";")) {
                listEmailCC.add(str);
            }
        }
        if(obj.getTypeDefaut()!= null && obj.getTypeDefaut().equalsIgnoreCase("Défaut logistique")){
            for(String str : emailsLogistique.split(";")) {
                listEmailCC.add(str);
            }
        }

        // add in cc the emailCutting
        String[] emailCuttingArr = emailCutting.split(";");
        for(String email : emailCuttingArr) {
            if(!listEmailCC.contains(email)) {
                listEmailCC.add(email);
            }
        }

        List<String> listEmailTo = new ArrayList<>();
        if (obj.getSite().equalsIgnoreCase("TRIM2")) {
            listEmailTo = new ArrayList<>(List.of("SDL-TNR38@lear.com", "SDL-TNR42@lear.com"));
        } else if (obj.getSite().equalsIgnoreCase("TRIM4")) {
            listEmailTo = new ArrayList<>(List.of("SDL-TNR59@lear.com", "SDL-TNR40@lear.com"));
        } else if (obj.getSite().equalsIgnoreCase("TRIM1")) {
            if(obj.getTypeDefaut()!= null && (obj.getTypeDefaut().equalsIgnoreCase("Défaut coupe") || obj.getTypeDefaut().equalsIgnoreCase("Défaut CNC"))) {
                listEmailTo = new ArrayList<>(List.of( "SDL-TNG28@lear.com", "SDL-TNG26@lear.com", "SDL-TNG40@lear.com"));
            }else {
                listEmailTo = new ArrayList<>(List.of("SDL-TNR31@lear.com", "SDL-TNR21@lear.com"));
            }
            listEmailCC = new ArrayList<>();
        } else if (obj.getSite().equalsIgnoreCase("FOAM")) {
            listEmailTo = new ArrayList<>(List.of("SDL-TNR36@lear.com", "NElhamraoui@lear.com"));
        }
        // add obj.getExtraEmails() to listEmail by spliting by ;
        try {
            if (obj.getExtraEmails() != null) {
                String[] emails = obj.getExtraEmails().split(";");
                for (String email : emails) {
                    if (email != null && email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }
        try {
            if (obj.getExtraEmailsReponse() != null) {
                String[] emails = obj.getExtraEmailsReponse().split(";");
                for (String email : emails) {
                    if (email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Pièces non reçues du QN " + obj.getNumeroQn() + " : " + user.getFirstName() + " " + user.getLastName() + "<br/>"
                // show the details of the QN

                + "<ul>";
        emailContent += "<li>Site : " + obj.getSite() + "</li>";
        emailContent += "<li>WO : " + obj.getWo() + "</li>";
        emailContent += "<li>Coordinateur : " + obj.getCoordinateur() + "</li>";
        emailContent += "<li>Created by : " + obj.getCreatedBy() + "</li>";
        emailContent += "<li>Created at : " + obj.getCreatedAt() + "</li>";
        emailContent += "<li>Sequence : " + obj.getSequence() + "</li>";
        emailContent += "<li>Part Number : " + obj.getPartnumber() + "</li>";
        emailContent += "<li>Projet : " + obj.getProjet() + "</li>";
        emailContent += "<li>Num Emp : " + obj.getNumEmp() + "</li>";
        emailContent += "<li>Quantite : " + obj.getQuantite() + "</li>";
        emailContent += "<li>Ref Tissu : " + obj.getReftissu() + "</li>";
        emailContent += "<li>Ref Tissu Description : " + obj.getReftissuDescription() + "</li>";
        emailContent += "<li>Fournisseur : " + obj.getNomFournisseur() + "</li>";
        emailContent += "<li>Type Defaut : " + obj.getTypeDefaut() + "</li>";
        if(obj.getCodeDefaut() != null) {
            emailContent += "<li>Code Defaut : " + obj.getCodeDefaut().getCode() + " : " + obj.getCodeDefaut().getDescription() + "</li>";
        }

        if(obj.getTypeDefaut() != null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")) {
            // add idRouleau , lotFrs, dateCoupe
            emailContent += "<li>Id Rouleau : " + obj.getIdRouleau() + "</li>";
            emailContent += "<li>Lot Fournisseur : " + obj.getLotFrs() + "</li>";
            emailContent += "<li>Date Coupe : " + obj.getDateCoupe() + "</li>";
        }

        emailContent += "<li>Description : " + obj.getDescription() + "</li>";

        emailContent += "</ul>"
                + "<br/>"
                + "Pour ouvrir le QN cliquez <a href='"+linkServer+"/qualityNoticeValidation?numeroQn=" + obj.getNumeroQn() + "'>ici</a>"
                + "</p>"
                + "</body>"
                + "</html>";
        List<File> arrFiles = new ArrayList<>();
        if(obj.getFichier() != null) {
            Resource rc = storageService.loadAsResource(obj.getFichier());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(obj.getImage1() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage1());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(obj.getImage2() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage2());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        listEmailCC.add(user.getEmail());

        emailService.sendEmailAttachment(listEmailTo, listEmailCC, "Lear Quality Notice : Pièces non reçues ! " + obj.getNumeroQn(), emailContent,arrFiles);
        obj.setSendNotificationDate(LocalDateTime.now());
        obj.setNotificationBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");

        QualityNotice newObj = service.save(obj);
        return new ResponseEntity<QualityNotice>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/transfertQR/{numeroQn}")
    public ResponseEntity<?> transfertQR(@PathVariable String numeroQn,Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_QUALITE")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        QualityNotice obj = service.findByObjId(numeroQn);
        obj.setTypeDefaut("Défaut fournisseur");
        if (obj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        if (obj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        // list of String emails initial by SDL-TNR31@lear.com, SDL-TNR21@lear.com , SDL-TNG28@lear.com, SDL-TNG26@lear.com
        List<String> listEmailCC = new ArrayList<>();
        for(String str : emailsQualiteCoupe.split(";")) {
            listEmailCC.add(str);
        }

        // add in cc the emailCutting
        String[] emailCuttingArr = emailCutting.split(";");
        for(String email : emailCuttingArr) {
            if(!listEmailCC.contains(email)) {
                listEmailCC.add(email);
            }
        }

        List<String> listEmailTo = new ArrayList<>();
        for(String str : emailsQualiteReception.split(";")) {
            listEmailTo.add(str);
        }

        // add obj.getExtraEmails() to listEmail by spliting by ;
        try {
            if (obj.getExtraEmails() != null) {
                String[] emails = obj.getExtraEmails().split(";");
                for (String email : emails) {
                    if (email != null && email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }
        try {
            if (obj.getExtraEmailsReponse() != null) {
                String[] emails = obj.getExtraEmailsReponse().split(";");
                for (String email : emails) {
                    if (email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Transfert à Qualité réception : QN " + obj.getNumeroQn() + " : " + user.getFirstName() + " " + user.getLastName() + "<br/>"
                // show the details of the QN

                + "<ul>";
        emailContent += "<li>Site : " + obj.getSite() + "</li>";
        emailContent += "<li>WO : " + obj.getWo() + "</li>";
        emailContent += "<li>Coordinateur : " + obj.getCoordinateur() + "</li>";
        emailContent += "<li>Created by : " + obj.getCreatedBy() + "</li>";
        emailContent += "<li>Created at : " + obj.getCreatedAt() + "</li>";
        emailContent += "<li>Sequence : " + obj.getSequence() + "</li>";
        emailContent += "<li>Part Number : " + obj.getPartnumber() + "</li>";
        emailContent += "<li>Projet : " + obj.getProjet() + "</li>";
        emailContent += "<li>Num Emp : " + obj.getNumEmp() + "</li>";
        emailContent += "<li>Quantite : " + obj.getQuantite() + "</li>";
        emailContent += "<li>Ref Tissu : " + obj.getReftissu() + "</li>";
        emailContent += "<li>Ref Tissu Description : " + obj.getReftissuDescription() + "</li>";
        emailContent += "<li>Fournisseur : " + obj.getNomFournisseur() + "</li>";
        emailContent += "<li>Type Defaut : " + obj.getTypeDefaut() + "</li>";
        if(obj.getCodeDefaut() != null) {
            emailContent += "<li>Code Defaut : " + obj.getCodeDefaut().getCode() + " : " + obj.getCodeDefaut().getDescription() + "</li>";
        }

        if(obj.getTypeDefaut() != null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")) {
            // add idRouleau , lotFrs, dateCoupe
            emailContent += "<li>Id Rouleau : " + obj.getIdRouleau() + "</li>";
            emailContent += "<li>Lot Fournisseur : " + obj.getLotFrs() + "</li>";
            emailContent += "<li>Date Coupe : " + obj.getDateCoupe() + "</li>";
        }

        emailContent += "<li>Description : " + obj.getDescription() + "</li>";

        emailContent += "</ul>"
                + "<br/>"
                + "Pour ouvrir le QN cliquez <a href='"+linkServer+"/qualityNoticeValidation?numeroQn=" + obj.getNumeroQn() + "'>ici</a>"
                + "</p>"
                + "</body>"
                + "</html>";
        List<File> arrFiles = new ArrayList<>();
        if(obj.getFichier() != null) {
            Resource rc = storageService.loadAsResource(obj.getFichier());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(obj.getImage1() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage1());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(obj.getImage2() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage2());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        emailService.sendEmailAttachment(listEmailTo, listEmailCC, "Lear Quality Notice : Validation Qualité Coupe " + obj.getNumeroQn(), emailContent,arrFiles);
        obj.setCoupeValidationDate(LocalDateTime.now());
        obj.setCoupeValidationBY(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        obj.setReponse("En attente de la validation qualité réception");
        QualityNotice newObj = service.save(obj);
        return new ResponseEntity<QualityNotice>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/transfertLogistique/{numeroQn}")
    public ResponseEntity<?> transfertLogistique(@PathVariable String numeroQn,Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_QUALITE")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        QualityNotice obj = service.findByObjId(numeroQn);
        obj.setTypeDefaut("Défaut logistique");
        if (obj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        if (obj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        // list of String emails initial by SDL-TNR31@lear.com, SDL-TNR21@lear.com , SDL-TNG28@lear.com, SDL-TNG26@lear.com
        List<String> listEmailCC = new ArrayList<>();
        for(String str : emailsQualiteCoupe.split(";")) {
            listEmailCC.add(str);
        }
        // add in cc the emailCutting
        String[] emailCuttingArr = emailCutting.split(";");
        for(String email : emailCuttingArr) {
            if(!listEmailCC.contains(email)) {
                listEmailCC.add(email);
            }
        }

        List<String> listEmailTo = new ArrayList<>();
        for(String str : emailsLogistique.split(";")) {
            listEmailTo.add(str);
        }
        // add obj.getExtraEmails() to listEmail by spliting by ;
        try {
            if (obj.getExtraEmails() != null) {
                String[] emails = obj.getExtraEmails().split(";");
                for (String email : emails) {
                    if (email != null && email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }
        try {
            if (obj.getExtraEmailsReponse() != null) {
                String[] emails = obj.getExtraEmailsReponse().split(";");
                for (String email : emails) {
                    if (email.length() > 0 && !listEmailCC.contains(email)) {
                        listEmailCC.add(email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("QualityNoticeController operation failed", e);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String emailContent = "<html>"
                + "<body>"
                + "<p>"
                + "Transfert aux logistiques : QN " + obj.getNumeroQn() + " : " + user.getFirstName() + " " + user.getLastName() + "<br/>"
                // show the details of the QN

                + "<ul>";
        emailContent += "<li>Site : " + obj.getSite() + "</li>";
        emailContent += "<li>WO : " + obj.getWo() + "</li>";
        emailContent += "<li>Coordinateur : " + obj.getCoordinateur() + "</li>";
        emailContent += "<li>Created by : " + obj.getCreatedBy() + "</li>";
        emailContent += "<li>Created at : " + obj.getCreatedAt() + "</li>";
        emailContent += "<li>Sequence : " + obj.getSequence() + "</li>";
        emailContent += "<li>Part Number : " + obj.getPartnumber() + "</li>";
        emailContent += "<li>Projet : " + obj.getProjet() + "</li>";
        emailContent += "<li>Num Emp : " + obj.getNumEmp() + "</li>";
        emailContent += "<li>Quantite : " + obj.getQuantite() + "</li>";
        emailContent += "<li>Ref Tissu : " + obj.getReftissu() + "</li>";
        emailContent += "<li>Ref Tissu Description : " + obj.getReftissuDescription() + "</li>";
        emailContent += "<li>Fournisseur : " + obj.getNomFournisseur() + "</li>";
        emailContent += "<li>Type Defaut : " + obj.getTypeDefaut() + "</li>";
        if(obj.getCodeDefaut() != null) {
            emailContent += "<li>Code Defaut : " + obj.getCodeDefaut().getCode() + " : " + obj.getCodeDefaut().getDescription() + "</li>";
        }

        if(obj.getTypeDefaut() != null && obj.getTypeDefaut().equalsIgnoreCase("Défaut fournisseur")) {
            // add idRouleau , lotFrs, dateCoupe
            emailContent += "<li>Id Rouleau : " + obj.getIdRouleau() + "</li>";
            emailContent += "<li>Lot Fournisseur : " + obj.getLotFrs() + "</li>";
            emailContent += "<li>Date Coupe : " + obj.getDateCoupe() + "</li>";
        }

        emailContent += "<li>Description : " + obj.getDescription() + "</li>";

        emailContent += "</ul>"
                + "<br/>"
                + "Pour ouvrir le QN cliquez <a href='"+linkServer+"/qualityNoticeValidation?numeroQn=" + obj.getNumeroQn() + "'>ici</a>"
                + "</p>"
                + "</body>"
                + "</html>";
        List<File> arrFiles = new ArrayList<>();
        if(obj.getFichier() != null) {
            Resource rc = storageService.loadAsResource(obj.getFichier());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(obj.getImage1() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage1());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(obj.getImage2() != null) {
            Resource rc = storageService.loadAsResource(obj.getImage2());
            try {
                arrFiles.add(rc.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        emailService.sendEmailAttachment(listEmailTo, listEmailCC, "Lear Quality Notice : Validation Qualité Coupe " + obj.getNumeroQn(), emailContent,arrFiles);
        obj.setCoupeValidationDate(LocalDateTime.now());
        obj.setCoupeValidationBY(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        obj.setReponse("En attente de la validation logistique");
        QualityNotice newObj = service.save(obj);
        return new ResponseEntity<QualityNotice>(newObj, HttpStatus.CREATED);
    }

    @GetMapping("/confirmationSuperviseur/{numeroQn}")
    public ResponseEntity<?> confirmationSuperviseur(@PathVariable String numeroQn,Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_QN_SUPERVISOR")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        QualityNotice obj = service.findByObjId(numeroQn);
        if (obj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        // list of String emails initial by SDL-TNR31@lear.com, SDL-TNR21@lear.com , SDL-TNG28@lear.com, SDL-TNG26@lear.com
        obj.setSuperviseurConfirmationDate(LocalDateTime.now());
        obj.setSuperviseurConfirmationBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");

        QualityNotice newObj = service.save(obj);
        return new ResponseEntity<QualityNotice>(newObj, HttpStatus.CREATED);
    }


    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody QualityNotice obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_QUALITE")) {//
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        if (obj.getNumeroQn() == null) {
            Integer maxInd = service.getMaxIndBySite(obj.getSite());
            if (maxInd == null) maxInd = 0;
            obj.setNumeroQn(obj.getSite().toUpperCase() + "-N°" + (maxInd + 1));
            obj.setInd(maxInd + 1);
            obj.setCreatedAt(LocalDateTime.now());
            obj.setCreatedBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
        }

        QualityNotice newObj = service.save(obj);

        return new ResponseEntity<QualityNotice>(newObj, HttpStatus.CREATED);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@Valid @RequestBody QualityNotice obj, BindingResult result, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        if(obj.getNumeroQn() == null) {
            return new ResponseEntity<String>("BAD REQUEST", HttpStatus.BAD_REQUEST);
        }

        QualityNotice oldObj = service.findByObjId(obj.getNumeroQn());
        if (oldObj == null) {
            return new ResponseEntity<String>("NOT FOUND", HttpStatus.NOT_FOUND);
        }
        oldObj.setActive(false);
        service.save(oldObj);

        return new ResponseEntity<String>("DELETED", HttpStatus.OK);
    }

}
