package com.lear.MGCMS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.domain.CuttingRequest.*;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData;
import com.lear.MGCMS.domain.scanCoupe.Config;
import com.lear.MGCMS.payload.QualityValidationReport;
import com.lear.MGCMS.payload.RapportShortageUrgent;
import com.lear.MGCMS.payload.Reference;
import com.lear.MGCMS.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lear.MGCMS.services.CuttingRequest.data.*;
import com.lear.MGCMS.services.pls.ProdTicketService;
import com.lear.MGCMS.services.scanCoupe.ConfigService;
import com.lear.MGCMS.services.splice.MarkerLogService;
import com.lear.splice.domain.MarkerLog;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacement;
import com.lear.MGCMS.repositories.PartNumberInfo2Repository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRepository;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestV2Service;
import com.lear.MGCMS.services.cms.OrderScheduleService;
import com.lear.MGCMS.services.ctc.FilesService;
import com.lear.MGCMS.services.ctc.GammeTechniqueImprimerService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.MGCMS.utils.UtilFunctions;
import com.lear.cms.domain.Coupe;
import com.lear.cms.domain.GammeTechniqueImprimer;
import com.lear.cms.domain.Matlassage;
import com.lear.cms.domain.OrderSchedule;
import com.lear.cms.domain.ProduitFinit;
import com.lear.cms.repositories.CoupeRepository;
import com.lear.cms.repositories.MatlassageRepository;
import com.lear.cms.repositories.ProduitFinitRepository;
import com.lear.ctc.domain.Files;

// @Component
public class WorkOrderTask {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderTask.class);

    @Autowired
    private WorkOrderService service;
    @Autowired
    private PlanningService planningService;
    @Autowired
    private PartNumberInfo2Repository partNumberInfo2Repository;
    @Autowired
    private FilesService filesService;
    @Autowired
    private OrderScheduleService orderScheduleService;
    @Autowired
    private CuttingRequestV2Service cuttingRequestV2Service;
    @Autowired
    private GammeTechniqueImprimerService gammeTechniqueImprimerService;
    @Autowired
    private ProduitFinitRepository produitFinitRepository;
    @Autowired
    private CuttingPlanRepository cuttingPlanRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private MatlassageRepository matlassageRepository;
    @Autowired
    private CoupeRepository coupeRepository;
    @Autowired
    private ReftissuPrixService reftissuPrixService;
    @Autowired
    private CuttingSpeedService cuttingSpeedService;
    @Autowired
    private ProjetService projetService;
    @Autowired
    private InterventionService interventionService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private MarkerLogService markerLogService;
    @Autowired
    private CuttingRequestSerieRouleauDataService cuttingRequestSerieRouleauDataService;
    @Autowired
    private CuttingRequestSerieDataService cuttingRequestSerieDataService;
    @Autowired
    private CuttingRequestBoxDataService cuttingRequestBoxDataService;
    @Autowired
    private CuttingRequestPartNumberDataService cuttingRequestPartNumberDataService;
    @Autowired
    private FirstCheckService firstCheckService;
    @Autowired
    private QueryService queryService;

    public WorkOrderTask() {
    }

    @Value("${lear.linkServer}")
    private String linkServer;

    @Autowired
    private PartNumberBoomService partNumberBoomService;

    @Autowired
    private ProdTicketService prodTicketService;
    @Autowired
    private ConfigService configService;
    @Value("${lear.emailsQualiteCoupe}")
    private String emailsQualiteCoupe;
    @Value("${lear.emailsLogistique}")
    private String emailsLogistique;
    @Value("${lear.emailsProcessCoupe}")
    private String emailsProcess;
    // BAD to be checked
//    @Scheduled(fixedRate = 1000 * 60 * 10, initialDelay = 1000 * 60 * 5)
    public void sendQualityValidationReport() {
        List<QualityValidationReport> arr = queryService.getQualityValidationReport();
        if (arr.size() > 0) {
            List<String> emailArr = new ArrayList<>();
            emailArr.add("sdl-tnr31@lear.com");
            emailArr.add("melghazi@lear.com");
            emailArr.add("sghailane@lear.com");
            String content = "<table style=\"border: 1px solid black;border-collapse: collapse;\">" + "<tr>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Serie</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Date</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">machine</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">reftissu</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">dateDebutCoupe</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">tableCoupe</th>"
                    + "</tr>";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (QualityValidationReport obj : arr) {
                String ligne = "<tr>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getSerie() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getDate().format(formatter) + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getMachine() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getReftissu() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getDateDebutCoupe().format(formatter) + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getTableCoupe() + "</td>";
                ligne += "</tr>";
                content += ligne;
            }
            content += "</table>";

            try {

                emailService.sendEmailAttachment(emailArr,
                        "Notification de début de coupe pour les séries validées",
                        "<html>" + "<head>" + "<style>\r\n" + "h2{text-align: center;}\r\n"
                                + "table {font-family: arial, sans-serif;border-collapse: collapse;margin: 0 auto}\r\n"
                                + "td, th {border: 1px solid #dddddd;text-align: left;padding: 4px;font-size: 12}\r\n" + "</style>"
                                + "</head>" + "<body><h2>Début de coupe pour les séries validées</h2>" + content
                                + "<br/><p>Cliquer <a href='" + linkServer + "'>ici</a> pour ouvrir l'application</p>"
                                + "</body></html>");
            } catch (Exception exp) {
                System.out.println("Email Notification 2 Error");
            }
        }

    }

    @Scheduled(fixedRate = 1000 * 60 * 15)
    public void changeStatuPlanning() {
        List<OrderSchedule> arr = orderScheduleService.findAllByStatu(new ArrayList<>(List.of("O", "S")));
        for (OrderSchedule obj : arr) {

            List<String> sequences = cuttingRequestBoxDataService.findByWo(obj.getIdDemande() + "");
            for (String sequence : sequences) {
                try {
                    Integer nonFinished = cuttingRequestSerieDataService.countNonFinishedBySequence(sequence);
                    Integer started = cuttingRequestSerieDataService.countStartedBySequence(sequence);

                    if (nonFinished > 0) {
                        if (started > 0 && obj.getStatusDemande().equals("O")) {
                            obj.setStatusDemande("S");
                            orderScheduleService.save(obj);
                        }
                    } else {
                        if (started > 0) {
                            obj.setStatusDemande("E");
                            orderScheduleService.save(obj);
                        }
                    }
                } catch (Exception e) {
                    System.out.println(obj.getIdDemande() + " : " + sequence + " Error : " + e.getMessage());
                }
            }
        }

    }

    @Scheduled(fixedRate = 1000 * 60 * 15)
    public void loadSequence() {
        int Hour = LocalDateTime.now().getHour();
        int Minute = LocalDateTime.now().getMinute();
        boolean update = false;
//        if (Hour % 2 == 0 && Minute < 15) {
//            update = true;
//        }
        if (LocalDateTime.now().getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            scanSequence(LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy")) + "%", update);
            scanSequence(LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("ddMMyy")) + "%", update);
            scanSequence(LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("ddMMyy")) + "%", update);
        } else {
            scanSequence(LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy")) + "%", update);
            scanSequence(LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("ddMMyy")) + "%", update);
        }
        System.out.println("Scan Sequence DONE.");
        //300425
//        scanSequence("300425%", true);
    }

//    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void sendNotification() {
        System.out.println("Notification : " + LocalDateTime.now());
        try {
            if (LocalDateTime.now().getHour() == 5 || LocalDateTime.now().getHour() == 13 || LocalDateTime.now().getHour() == 21) {
                List<Intervention> arr = interventionService.findNonValider();
                List<Intervention> arrToSend = new ArrayList<Intervention>();
                String departement = null;
                List<String> emailArr = new ArrayList<String>();
                for (Intervention intervention : arr) {
                    if (departement == null && intervention.getCodeArret() != null)
                        departement = intervention.getDepartement();
                    if (departement != null && !departement.equalsIgnoreCase(intervention.getDepartement())) {
                        sendEmail(arrToSend, emailArr, departement);
                        arrToSend = new ArrayList<Intervention>();
                        departement = intervention.getDepartement();
                    }

                    arrToSend.add(intervention);
                }
                sendEmail(arrToSend, emailArr, departement);
            }
        } catch (Exception e) {
            System.out.println("Email Notification 1 Error : " + e.getMessage());
        }

        try {
            if (LocalDateTime.now().getHour() == 14) {
                LocalDateTime date1 = LocalDateTime.now().minusHours(24);
                LocalDateTime date2 = LocalDateTime.now();
                List<CuttingRequestSerieRouleauData> arr = cuttingRequestSerieRouleauDataService.findExcess(-1.0, date1, date2);
                System.out.println("Rouleau Notification : " + arr.size());

                List<RapportShortageUrgent> arrUrgent = cuttingRequestSerieRouleauDataService.findShrotageUrgent(-1.0, date1, date2);
                System.out.println("arrUrgent Notification : " + arr.size());
                List<String> idRouleauArr = new ArrayList<>();
                for (RapportShortageUrgent rs : arrUrgent) {
                    if (!idRouleauArr.contains("S" + rs.getIdRouleau())) {
                        idRouleauArr.add("S" + rs.getIdRouleau());
                    }
                }
                List<String> idRouleauToRemove = prodTicketService.findIdRouleauInThis(idRouleauArr);

                arrUrgent = arrUrgent.stream()
                        .filter(rs -> !idRouleauToRemove.contains("S" + rs.getIdRouleau()))
                        .collect(Collectors.toList());
                Map<String, String> mapFournisseur = new HashMap<>();

                for (RapportShortageUrgent rs : arrUrgent) {
                    String refTissu = rs.getReftissu();
                    if (mapFournisseur.containsKey(refTissu)) {
                        rs.setFournisseur(mapFournisseur.get(refTissu));
                    } else {
                        Reference ref = queryService.refDetails(refTissu);
                        if (ref != null) {
                            String fournisseur = ref.getFournisseur();
                            mapFournisseur.put(refTissu, fournisseur);
                            rs.setFournisseur(fournisseur);
                        }
                    }
                }


//                if (arr.size() > 0) {
//                    List<String> emailArr = new ArrayList<String>();
//                    emailArr.add("SDL-TNG40@lear.com");
//                    emailArr.add("SDL-TNR31@lear.com");
//                    emailArr.add("SDL-TNR21@lear.com");
//                    emailArr.add("sdl-tnr32@lear.com");
//                    emailArr.add("afadli@lear.com");
//                    emailArr.add("RSamadi@lear.com");
//                    emailArr.add("BElhaddad@lear.com");
//                    sendRouleauNotification(arr, emailArr, date1, date2, arrUrgent);
//                }
            }
        } catch (Exception e) {
            System.out.println("Email Notification 2 Error : " + e.getMessage());
        }

        try {
            List<FirstCheck> arr = firstCheckService.getNokBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now());
            if (arr.size() > 0) {
                sendFirstCheckEmail(arr);
            }
        } catch (Exception e) {

        }

        if (LocalDateTime.now().getHour() == 14) {
            Config config = configService.findByParam("codeQualite");
            if(config == null) {
                config = new Config();
                config.setParam("codeQualite");
            }
            config.setValue(generateRandomString(4));
            configService.save(config);
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            emailService.sendEmailAttachment(new ArrayList<String>(Arrays.asList(emailsQualiteCoupe.split(";"))),
                    "Code qualité : Les codes de " + currentDateTime.format(formatter),
                    "<html><body><h2>Code qualité :</h2>"
                            + "<p>Code pour débloquer les matelasseurs : "+config.getValue()+"<br/>"
                            +"</p>"
                            +"</body></html>");
//            int day = LocalDateTime.now().getDayOfMonth();
//            if(day==15) {
//                Config configVariance = configService.findByParam("codeVariance");
//                if (configVariance == null) {
//                    configVariance = new Config();
//                    configVariance.setParam("codeVariance");
//                }
//                configVariance.setValue(generateRandomString(4));
//                configService.save(configVariance);
//                emailService.sendEmailAttachment(new ArrayList<String>(Arrays.asList(emailsLogistique.split(";"))),
//                        "Code variance : Les codes de " + currentDateTime.format(formatter),
//                        "<html><body><h2>Code variance :</h2>"
//                                + "<p>Code pour débloquer les matelasseurs : " + config.getValue() + "<br/>"
//                                + "</p>"
//                                + "</body></html>");
//                Config configProcess = configService.findByParam("codeProcess");
//                if (configProcess == null) {
//                    configProcess = new Config();
//                    configProcess.setParam("codeProcess");
//                }
//                configProcess.setValue(generateRandomString(4));
//                configService.save(configProcess);
//                emailService.sendEmailAttachment(new ArrayList<String>(Arrays.asList(emailsProcess.split(";"))),
//                        "Code process : Les codes de " + currentDateTime.format(formatter),
//                        "<html><body><h2>Code process :</h2>"
//                                + "<p>Code pour débloquer les matelasseurs : " + config.getValue() + "<br/>"
//                                + "</p>"
//                                + "</body></html>");
//            }
        }

    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void woRapportAsprova() throws IOException {

        LocalDate date1 = LocalDate.now().minusDays(1);
        LocalDate date2 = LocalDate.now().plusDays(1);
        List<OrderSchedule> arr = orderScheduleService.findBetweenInterval(date1, date2);
        List<WorkOrder> workOrders = service.findBetweenInterval(date1, date2);
        System.out.println("woRapportAsprova arr " + date1 + " => " + date2 + " : " + arr.size());
        for (
                OrderSchedule order : arr) {
            WorkOrder obj = new WorkOrder();
            boolean isNew = true;
            for (WorkOrder wo : workOrders) {
                if (wo.getWo().equals(order.getIdDemande() + "")) {
                    obj = wo;
                    isNew = false;
                    break;
                }
            }

            obj.setItem(order.getPartNumberDemande());
            obj.setWo(order.getIdDemande() + "");
            obj.setWoid(order.getMarkerID());
            try {
                obj.setQtyOpen(Double.parseDouble(order.getQuantiteDemande() + ""));
            } catch (Exception e) {
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

            obj.setDueDate(order.getDateDemande());
            obj.setShift(order.getShiftDemande());
            obj.setStatus(order.getStatusReceptionSewingDemande());

            if (obj.getItem().toUpperCase().startsWith("W")) {
                obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
            }

            PartNumberInfo2 pnInfo2 = new PartNumberInfo2();
            if (obj.getItem().toUpperCase().startsWith("WL")) {
                obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
                pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getItem().toUpperCase().substring(1));
            } else {
                PartNumberBoom pnb = partNumberBoomService.findByItem(obj.getItem());
                if (pnb != null) {
                    obj.setPartNumber(pnb.getPartNumber());
                    obj.setDescription(pnb.getDescription());
                    pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
                }
            }

            if (pnInfo2 != null) {
                obj.setGroupName(pnInfo2.getItemGroup());
                obj.setDesignGroup(pnInfo2.getDesignGroup());
                obj.setPartNumberStatus(pnInfo2.getStatus());
                obj.setCoverGroup(pnInfo2.getCovertype());
                obj.setDescription(pnInfo2.getDescription());
            } else {
                List<Files> files = filesService.findBySemiFinishedGoodPartNumber(obj.getItem());
                if (files.size() > 0) {
                    Files file = files.get(0);
                    obj.setDescription(file.getPartNumberCoverDesciption());
                    obj.setPartNumber(file.getPartNumberCover());
                    obj.setGroupName(file.getProjet());
                }
            }

            if (obj.getPartNumber() == null) {
                System.out.println("ERROR ITEM NOT FOUNC : " + obj.getItem());
            }

            if (isNew) {
                obj.setCreatedAt(LocalDateTime.now());
            }

            obj.setUpdatedAt(LocalDateTime.now());
            service.save(obj);
        }

        workOrders.removeIf(wo -> arr.stream().

                anyMatch(orderSchedule -> wo.getWo().

                        equals(orderSchedule.getIdDemande() + "")));
        for (
                WorkOrder obj : workOrders) {
            OrderSchedule order = orderScheduleService.findById(Long.parseLong(obj.getWo()));
            if (order == null) {
                service.delete(obj);
                System.out.println("Deleted WO : " + obj.getWo());
            } else {
                boolean isNew = false;
                obj.setItem(order.getPartNumberDemande());
                obj.setWo(order.getIdDemande() + "");
                obj.setWoid(order.getMarkerID());
                try {
                    obj.setQtyOpen(Double.parseDouble(order.getQuantiteDemande() + ""));
                } catch (Exception e) {
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

                obj.setDueDate(order.getDateDemande());
                obj.setShift(order.getShiftDemande());
                obj.setStatus(order.getStatusReceptionSewingDemande());

                if (obj.getItem().toUpperCase().startsWith("W")) {
                    obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
                }

                PartNumberInfo2 pnInfo2 = new PartNumberInfo2();
                if (obj.getItem().toUpperCase().startsWith("WL")) {
                    obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
                    pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getItem().toUpperCase().substring(1));
                } else {
                    PartNumberBoom pnb = partNumberBoomService.findByItem(obj.getItem());
                    if (pnb != null) {
                        obj.setPartNumber(pnb.getPartNumber());
                        obj.setDescription(pnb.getDescription());
                        pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
                    }
                }

                if (pnInfo2 != null) {
                    obj.setGroupName(pnInfo2.getItemGroup());
                    obj.setDesignGroup(pnInfo2.getDesignGroup());
                    obj.setPartNumberStatus(pnInfo2.getStatus());
                    obj.setCoverGroup(pnInfo2.getCovertype());
                    obj.setDescription(pnInfo2.getDescription());
                } else {
                    List<Files> files = filesService.findBySemiFinishedGoodPartNumber(obj.getItem());
                    if (files.size() > 0) {
                        Files file = files.get(0);
                        obj.setDescription(file.getPartNumberCoverDesciption());
                        obj.setPartNumber(file.getPartNumberCover());
                        obj.setGroupName(file.getProjet());
                    }
                }

                if (obj.getPartNumber() == null) {
                    System.out.println("ERROR ITEM NOT FOUND : " + obj.getItem());
                }

                obj.setUpdatedAt(LocalDateTime.now());
                service.save(obj);
            }
        }
        System.out.println("woRapportAsprova Done");

    }


    void sendRouleauNotification(List<CuttingRequestSerieRouleauData> arr, List<String> emailArr, LocalDateTime date1, LocalDateTime date2, List<RapportShortageUrgent> arrUrgent) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        Map<String, Double> summary = new TreeMap<>();
        for (CuttingRequestSerieRouleauData obj : arr) {
            if (summary.containsKey(obj.getConfirmReftissu())) {
                summary.put(obj.getConfirmReftissu(), summary.get(obj.getConfirmReftissu()) + obj.getExcess());
            } else {
                summary.put(obj.getConfirmReftissu(), obj.getExcess());
            }
        }
        emailArr.add("melghazi@lear.com");
        emailArr.add("sghailane@lear.com");
        emailArr.add("AZaghdoud@lear.com");
        Double total = 0.0;

        if (arr.size() > 0) {
            String urgent = "";
            if (arrUrgent.size() > 0) {
                urgent = "<table style=\"border: 1px solid black;border-collapse: collapse;\">" + "<tr>"
                        + "<th style=\"border: 1px solid black;border-collapse: collapse;\">ID Rouleau</th>"
                        + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Total Usage</th>"
                        + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Metrage Initial</th>"
                        + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Shortage</th>"
                        + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Date</th>"
                        + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Référence tissu</th>"
                        + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Fournisseur</th>"
                        + "</tr>";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (RapportShortageUrgent obj : arrUrgent) {
                    String ligne = "<tr>";
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getIdRouleau() + "</td>";
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getTotalUsage() + "</td>";
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getMetrageInitial() + "</td>";
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getShortage() + "</td>";
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getDate().format(formatter) + "</td>";
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getReftissu() + "</td>";
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getFournisseur() + "</td>";
                    ligne += "</tr>";
                    urgent += ligne;
                }
                urgent += "</table>";


            }

            String contentSummary = "<table style=\"border: 1px solid black;border-collapse: collapse;\">" + "<tr>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Reftissu</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Total Excess</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Prix unitaire</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Prix total</th>"

                    + "</tr>";
            List<Map.Entry<String, Double>> sortedSummary = new ArrayList<>(summary.entrySet());
            sortedSummary.sort(Map.Entry.comparingByValue());

            for (Map.Entry<String, Double> entry : sortedSummary) {
                Double prixUnit = 0.0;
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    Double prix = queryService.getPrixItem(entry.getKey());
                    if (prix != null && prix > 0) {
                        prixUnit = prix;
                    } else {
                        prixUnit = ExcelHelper.getPrixUnit(entry.getKey());
                    }
                }

                String ligne = "<tr>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + entry.getKey() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + UtilFunctions.convertTwoDigit(entry.getValue(), 3) + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + UtilFunctions.convertTwoDigit(prixUnit, 3) + "</td>";
                if (prixUnit > 0) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + UtilFunctions.convertTwoDigit(entry.getValue() * prixUnit, 3) + "</td>";
                    total += entry.getValue() * prixUnit;
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">0.0</td>";
                }
                ligne += "</tr>";
                contentSummary += ligne;
            }
            contentSummary += "</table>";
            String content = "<table style=\"border: 1px solid black;border-collapse: collapse;\">" + "<tr>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Serie</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Reftissu</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Id Rouleau</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Lot Frs</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Metrage</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Defaut</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Non Utitlse</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Excès/Shortage</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Total Usage</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Created At</th>"
                    + "</tr>";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (CuttingRequestSerieRouleauData obj : arr) {
                String ligne = "<tr>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getSerie() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getConfirmReftissu() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getIdRouleau() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getLotFrs() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getMetrage() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getDefaut() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getNonUtitlse() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getExcess() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getTotalUsage() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getCreatedAt().format(formatter) + "</td>";
                ligne += "</tr>";
                content += ligne;
            }
            content += "</table>";
            try {
                emailService.sendEmailAttachment(emailArr,
                        "Shortage par jour",
                        "<html>" + "<head>" + "<style>\r\n" + "h2 , h3 {text-align: center;}\r\n"
                                + "table {font-family: arial, sans-serif;border-collapse: collapse;margin: 0 auto}\r\n"
                                + "td, th {border: 1px solid #dddddd;text-align: left;padding: 4px;font-size: 12}\r\n" + "</style>"
                                + "</head>" + "<body>" +
                                "<h2>" + "Alert Rouleaux " + date1.format(dateTimeFormatter) + " => " + date2.format(dateTimeFormatter) + "</h2>"
                                + "<h3>Rouleaux mono-plan avec shortage fournisseur</h3>"
                                + urgent
                                + "<h3>" + "Référence tissu avec Shortage " + UtilFunctions.convertTwoDigit(total, 3) + " DH" + "</h3>"
                                + contentSummary
//                                + "<br/>" + content
                                + "<br/><p>Cliquer <a href='" + linkServer + "'>ici</a> pour ouvrir l'application</p>"
                                + "</body></html>");
            } catch (Exception exp) {
                System.out.println("Email Notification 2 Error : " + exp.getMessage());
            }
        }
    }

    void sendFirstCheckEmail(List<FirstCheck> arr) {
        List<String> emailArr = new ArrayList<String>();
        emailArr.add("SDL-TNR21@lear.com");
        emailArr.add("sdl-tnr31@lear.com");
        emailArr.add("melghazi@lear.com");
        emailArr.add("sghailane@lear.com");

        if (arr.size() > 0) {
			/*
			show this columns :  [category]
      ,[machine]
      ,[task]
      ,[taskNumber]
	   ,[createdAt]
			*/
            String content = "<table style=\"border: 1px solid black;border-collapse: collapse;\">" + "<tr>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Category</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Machine</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Task</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Task Number</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Created At</th>"
                    + "</tr>";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (FirstCheck obj : arr) {
                String ligne = "<tr>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getCategory() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getMachine() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getTask() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getTaskNumber() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getCreatedAt().format(formatter) + "</td>";
                ligne += "</tr>";
                content += ligne;
            }
            content += "</table>";
            try {
//                System.out.println(content);
                emailService.sendEmailAttachment(emailArr,
                        "Maintenance 1er niveau - NOK",
                        "<html>" + "<head>" + "<style>\r\n" + "h2{text-align: center;}\r\n"
                                + "table {font-family: arial, sans-serif;border-collapse: collapse;margin: 0 auto}\r\n"
                                + "td, th {border: 1px solid #dddddd;text-align: left;padding: 4px;font-size: 12}\r\n" + "</style>"
                                + "</head>" + "<body><h2>" + "Maintenance 1er niveau - NOK" + "</h2>" + content
                                + "<br/><p>Cliquer <a href=\"http://matnr-app16:8085/\">ici</a> pour ouvrir l'application</p>"
                                + "</body></html>");
            } catch (Exception exp) {
                System.out.println("Email Notification 2 Error");
            }
        }
    }

    void sendEmail(List<Intervention> arrToSend, List<String> emailArr, String departement) {
        emailArr = new ArrayList<String>();
        switch (departement) {
            case "Qualite reception":
                emailArr.add("SDL-TNG28@lear.com");
                break;
            case "Qualite coupe":
                emailArr.add("SDL-TNG28@lear.com");
                break;
            case "Maintenance":
                emailArr.add("SDL-TNR69@lear.com");
                break;
            case "HSE":
                emailArr.add("mbouzakhti@lear.com");
                break;
            case "CAD":
                emailArr.add("SDL-TNR13@lear.com");
                break;
            case "Process coupe":
                emailArr.add("SDL-TNR32@lear.com");
                break;
            case "IT":
                emailArr.add("SDL-TNR23@lear.com");
                break;
            case "Logistique":
                emailArr.add("SDL-TNG40@lear.com");
                break;
        }
        emailArr.add("SDL-TNR21@lear.com");
//        emailArr.add("SDL-TNR32@lear.com");
//        emailArr.add("SDL-TNG28@lear.com");
        emailArr.add("melghazi@lear.com");
        if (arrToSend.size() > 0) {
            String content = "<table style=\"border: 1px solid black;border-collapse: collapse;\">" + "<tr>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">ID</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Type</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Serie</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Sequence</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Début d'arrêt</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Début d'intervention</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Fin d'intervention</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Machine</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Résolu</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Code</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Emetteur</th>"
                    + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Responsable</th>" + "</tr>";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Intervention obj : arrToSend) {
                String ligne = "<tr>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getId() + "</td>";
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">";
                if (obj.getType() != null) {
                    ligne += obj.getType();
                }
                if (obj.getSousType() != null) {
                    ligne += " " + obj.getSousType();
                }
                ligne += "</td>";
                if (obj.getSerie() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getSerie()
                            + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }
                if (obj.getSequence() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getSequence()
                            + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }
                if (obj.getDebutArret() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                            + obj.getDebutArret().format(formatter) + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }
                if (obj.getDebutIntervention() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                            + obj.getDebutIntervention().format(formatter) + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }
                if (obj.getFinIntervention() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                            + obj.getFinIntervention().format(formatter) + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }
                ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getMachine()
                        + "</td>";
                if (obj.getProblemeResolu() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                            + obj.getProblemeResolu() + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }
                if (obj.getCodeArret() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                            + obj.getCodeArret().getCode() + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }
                if (obj.getMatriculeEmetteur() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                            + obj.getMatriculeEmetteur() + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }

                if (obj.getMatriculeResponsable() != null) {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                            + obj.getMatriculeResponsable() + "</td>";
                } else {
                    ligne += "<td style=\"border: 1px solid black;border-collapse: collapse;\"></td>";
                }

                ligne += "</tr>";
                content += ligne;
            }
            content += "</table>";
            try {
                System.out.println("Notification : " + departement + " : " + emailArr.size());

                emailService.sendEmailAttachment(emailArr,
                        departement + " Intervention CMS web en attente de validation",
                        "<html>" + "<head>" + "<style>\r\n" + "h2{text-align: center;}\r\n"
                                + "table {font-family: arial, sans-serif;border-collapse: collapse;margin: 0 auto}\r\n"
                                + "td, th {border: 1px solid #dddddd;text-align: left;padding: 4px;font-size: 12}\r\n" + "</style>"
                                + "</head>" + "<body><h2>" + departement
                                + " Intervention CMS web en attente de validation</h2>" + content
                                + "<br/><p>Cliquer <a href='" + linkServer + "'>ici</a> pour ouvrir l'application</p>"
                                + "</body></html>");
            } catch (Exception exp) {
                System.out.println("Email Notification 2 Error");
            }

        }
    }

    @Autowired
    private CuttingRequestDataService cuttingRequestDataService;

    void scanSequence(String date, boolean updateOld) {
        List<String> arrSequence = produitFinitRepository.findAllSequenceLike(date);
        System.out.println("Sequences " + date + " : " + arrSequence.size());
        for (String sequence : arrSequence) {
            System.out.println(sequence);
            try {
                List<ProduitFinit> arrPf = produitFinitRepository.findBySequence(sequence);
                CuttingRequestData crd = cuttingRequestDataService.findBySequence(sequence);
                if (crd == null) {
//                    CuttingRequestV2 obj = cuttingRequestV2Service.findBySequence(sequence);
                    CuttingRequestV2 obj = new CuttingRequestV2();
                    List<CuttingPlan> cpArr = cuttingPlanRepository
                            .findByCMSId(Long.parseLong(arrPf.get(0).getIdPlanProduiFinit()));
                    CuttingPlan cp = null;
                    if (!cpArr.isEmpty()) {
                        cp = cpArr.get(0);
                    }
                    Map<String, Double> tempCoupeMap = new HashedMap<String, Double>();
                    Map<String, Double> perimetreMap = new HashedMap<String, Double>();
                    Map<String, Integer> nbrPieceMap = new HashedMap<String, Integer>();

                    for (CuttingPlanMaterial cpm : cp.getCuttingPlanMaterials()) {
                        for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                            if (cpmp.getPlacement() != null) {
                                if (cpmp.getPerimetre() != null && cpmp.getTempsDeCoupe() > 0
                                        && cpmp.getPerimetre() > 0) {
                                    perimetreMap.put(cpmp.getPlacement(), cpmp.getPerimetre());
                                } else {
                                    perimetreMap.put(cpmp.getPlacement(),
                                            ExcelHelper.getPerimetre(cpmp.getPlacement()));
                                }

                                if (!nbrPieceMap.containsKey(cpmp.getPlacement())) {
                                    Integer countNbrPiece = ExcelHelper.getTotalEmp(cpmp.getPlacement());
                                    if (countNbrPiece != null) {
                                        nbrPieceMap.put(cpmp.getPlacement(), countNbrPiece);
                                    } else {
                                        nbrPieceMap.put(cpmp.getPlacement(), 0);
                                    }
                                }

                                if (cpmp.getTempsDeCoupe() != null && cpmp.getTempsDeCoupe() > 0) {
                                    tempCoupeMap.put(cpmp.getPlacement(), cpmp.getTempsDeCoupe());
                                }

                            }
                        }
                    }

                    obj.setSequence(sequence);
                    obj.setCuttingPlanId(cp.getId());
                    obj.setProjet(cp.getProjet());
                    obj.setVersion(cp.getVersion());
                    obj.setModele(cp.getDescription());
                    obj.setDefinition(cp.getDefinition());
                    Projet projet = projetService.findByObjId(cp.getProjet());
                    if (projet != null && projet.getZone() != null) {
                        obj.setZone(projet.getZone());
                    } else {
                        obj.setZone(zoneRepository.findByCode(arrPf.get(0).getAreaProduitFinit()));
                    }
                    List<CuttingRequestPartNumberV2> cppnArr = new ArrayList<CuttingRequestPartNumberV2>();
                    for (ProduitFinit pf : arrPf) {
                        CuttingRequestPartNumberV2 pn = new CuttingRequestPartNumberV2();
                        pn.setDescription(pf.getDesiProdFinit());
                        pn.setPartNumber(pf.getRefProdFinit());
                        pn.setItem(pf.getRefProdSemi());
                        pn.setQuantity(Integer.parseInt(pf.getQtyTotalPartNumber()));
                        pn.setWo(pf.getNoff());
                        pn.setWoid(pf.getWoid());
                        pn.setCuttingRequest(obj);
                        cppnArr.add(pn);
                    }
                    obj.setCuttingRequestPartNumbers(cppnArr);

                    List<CuttingRequestSerieV2> crsArr = new ArrayList<CuttingRequestSerieV2>();
                    for (Matlassage mt : matlassageRepository.findByNofOrderByNserie(sequence)) {
                        try {
                            Coupe coupe = coupeRepository.findFirstByNserie(mt.getNserie());

                            CuttingRequestSerieV2 crs = new CuttingRequestSerieV2();
                            crs.setSerie(mt.getNserie() + "");
                            crs.setPartNumberMaterial(mt.getReftissu());
                            crs.setDescription(mt.getDescription());
                            crs.setMatelassageEndroit(mt.getSens());
                            crs.setLongueur(Double.parseDouble(mt.getLongueur()));

                            crs.setQuantite(mt.getQuantite());
                            crs.setPartNumbers(mt.getModele());
                            crs.setGroupPlacement(1);
                            crs.setActivated(true);
                            crs.setMachine(mt.getMachine());
                            if (mt.getReturnMagasin() != null)
                                crs.setRetourMagasin(Double.parseDouble(mt.getReturnMagasin()));
                            crs.setMaxDrill(null);
                            crs.setMaxPlie(null);
                            crs.setMaxPlieDrill(null);
                            crs.setNbrCouche(Integer.parseInt(mt.getnCouches()));
                            crs.setPlacement(mt.getPlacement());
                            crs.setLaize(Double.parseDouble(mt.getLaLaizeDemande()));
                            crs.setConfig(coupe.getConfiguration());
                            crs.setDrill(coupe.getDrill1() + "," + coupe.getDrill2());

                            crs.setPerimetre(perimetreMap.get(crs.getPlacement()));
                            crs.setNbrPiece(nbrPieceMap.getOrDefault(crs.getPlacement(), 0));
                            if (crs.getNbrCouche() != null && crs.getNbrPiece() != null) {
                                crs.setNbrPieceTotal((double) (crs.getNbrPiece() * crs.getNbrCouche()));
                            }

                            if (tempCoupeMap.containsKey(crs.getPlacement())
                                    && tempCoupeMap.get(crs.getPlacement()) > 0) {
                                crs.setTempsDeCoupe(tempCoupeMap.get(crs.getPlacement()));
                            } else {
                                CuttingSpeed speed = cuttingSpeedService.findById(crs.getConfig());
                                if (crs.getPerimetre() != null) {
                                    if (speed != null) {
                                        crs.setTempsDeCoupe(UtilFunctions
                                                .convertTwoDigit(crs.getPerimetre() / (speed.getVitesse() * 100), 5));
                                    } else {
                                        crs.setTempsDeCoupe(UtilFunctions.convertTwoDigit(crs.getPerimetre() / 300, 5));
                                    }
                                }
                            }

                            crs.setCuttingRequest(obj);
                            if (obj.getPlanningDate() == null || obj.getShift() == null) {
                                obj.setPlanningDate(LocalDate.parse(mt.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                                LocalTime time = LocalTime.parse(mt.getHeure(), DateTimeFormatter.ofPattern("HH:mm"));
                                time.plusHours(2);
                                int hour = time.getHour();
                                if (hour < 8 && hour >= 0) {
                                    obj.setShift("1");
                                } else if (hour < 16 && hour >= 8) {
                                    obj.setShift("2");
                                } else {
                                    obj.setShift("3");
                                }
                            }
                            crs.setPlanningDate(obj.getPlanningDate());
                            crs.setShift(obj.getShift());

                            if (coupe.getDatedebut() != null && !coupe.getDatedebut().trim().isEmpty())
                                crs.setDateDebutCoupe(LocalDateTime.parse(coupe.getDatedebut(),
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                            if (coupe.getDateFin() != null && !coupe.getDateFin().trim().isEmpty())
                                crs.setDateFinCoupe(LocalDateTime.parse(coupe.getDateFin(),
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                            if (mt.getStatu().equalsIgnoreCase("complet")) {
                                crs.setStatusMatelassage("Complete");
                            } else if (mt.getStatu().equalsIgnoreCase("incomplet")) {
                                crs.setStatusMatelassage("Incomplete");
                            } else if (mt.getStatu().equalsIgnoreCase("Non demarre")) {
                                crs.setStatusMatelassage("Waiting");
                            } else {
                                crs.setStatusMatelassage("In progress");
                            }

                            List<MarkerLog> mkArr = markerLogService.findBySerie(crs.getSerie());
                            if (mkArr.size() > 0) {
                                MarkerLog mk = mkArr.get(0);
                                crs.setDateDebutMatelassage(mk.getCreatedAt());
                                crs.setDateFinMatelassage(mk.getUpdatedAt());
                            }

                            if (coupe.getStatut().equalsIgnoreCase("complet")) {
                                crs.setStatusCoupe("Complete");
                            } else if (coupe.getStatut().equalsIgnoreCase("incomplet")) {
                                crs.setStatusCoupe("Incomplete");
                            } else if (coupe.getStatut().equalsIgnoreCase("Non demarre")) {
                                if (crs.getDateDebutCoupe() != null && crs.getDateFinCoupe() == null) {
                                    crs.setStatusCoupe("In progress");
                                } else {
                                    crs.setStatusCoupe("Waiting");
                                }
                            }
                            crs.setTableMatelassage(mt.getTablee());
                            crs.setTableCoupe(coupe.getMachine());
                            crs.setMatelasseur1(mt.getMatMatlasseur1());
                            crs.setMatelasseur2(mt.getMatMatlasseur2());
                            crs.setMatelasseur3(mt.getMatMatlasseur3());
                            crs.setMatelasseur4(mt.getMatMatlasseur4());

                            crs.setCoupeur1(coupe.getMatricule());
                            crsArr.add(crs);
                        } catch (Exception e) {
                            System.out.println("Error CRS : " + mt.getNserie() + " : " + e.getMessage());
                        }
                    }
                    obj.setCuttingRequestSeries(crsArr);

                    List<CuttingRequestBoxV2> crbArr = new ArrayList<CuttingRequestBoxV2>();
                    for (GammeTechniqueImprimer gt : gammeTechniqueImprimerService.findBySequence(sequence)) {
                        CuttingRequestBoxV2 crb = new CuttingRequestBoxV2();
                        crb.setId((gt.getnSerieGammeImp()) + "");
                        crb.setPartNumber(gt.getPartNumberImp());
                        crb.setDescription(gt.getDescriptionImp());
                        crb.setItem(gt.getCode3Imp());
                        crb.setWo(gt.getNofImp());
                        crb.setWoid(gt.getWoidImp());
                        crb.setQtyBox(Integer.parseInt(gt.getQuantiteImp()));
                        crb.setCuttingRequest(obj);
                        crbArr.add(crb);
                    }
                    if (crbArr.isEmpty()) {
                        System.out.println(sequence + " Empty boxes");
                        continue;
                    }
                    obj.setCuttingRequestBoxs(crbArr);

                    cuttingRequestV2Service.save(obj);

                } else if(crd.getDueDate() == null) {
                    List<String> wo = cuttingRequestPartNumberDataService.findWoBySequence(crd.getSequence());
                    if(wo.size() > 0) {
                        OrderSchedule order = orderScheduleService.findById(Long.parseLong(wo.get(0)));
                        crd.setDueDate(order.getDateDemande());
                        crd.setDueShift(order.getShiftDemande());
                        cuttingRequestDataService.save(crd);
                    }
                } else  {
                    if (updateOld) {
                        CuttingRequestV2 obj = cuttingRequestV2Service.findBySequence(sequence);
                        List<CuttingPlan> cpArr = cuttingPlanRepository
                                .findByCMSId(Long.parseLong(arrPf.get(0).getIdPlanProduiFinit()));
                        CuttingPlan cp = null;
                        if (!cpArr.isEmpty()) {
                            cp = cpArr.get(0);
                        }
                        Map<String, Double> tempCoupeMap = new HashedMap<String, Double>();
                        Map<String, Double> perimetreMap = new HashedMap<String, Double>();
                        Map<String, Integer> nbrPieceMap = new HashedMap<String, Integer>();
                        for (CuttingPlanMaterial cpm : cp.getCuttingPlanMaterials()) {
                            for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                                if (cpmp.getPlacement() != null) {
                                    if (cpmp.getPerimetre() != null && cpmp.getPerimetre() > 0) {
                                        perimetreMap.put(cpmp.getPlacement(), cpmp.getPerimetre());
                                    } else {
                                        perimetreMap.put(cpmp.getPlacement(),
                                                ExcelHelper.getPerimetre(cpmp.getPlacement()));
                                    }

                                    if (!nbrPieceMap.containsKey(cpmp.getPlacement())) {
                                        Integer countNbrPiece = ExcelHelper.getTotalEmp(cpmp.getPlacement());
                                        if (countNbrPiece != null) {
                                            nbrPieceMap.put(cpmp.getPlacement(), countNbrPiece);
                                        } else {
                                            nbrPieceMap.put(cpmp.getPlacement(), 0);
                                        }
                                    }

                                    if (cpmp.getTempsDeCoupe() != null && cpmp.getTempsDeCoupe() > 0) {
                                        tempCoupeMap.put(cpmp.getPlacement(), cpmp.getTempsDeCoupe());
                                    }
                                }
                            }
                        }

                        obj.setSequence(sequence);
                        obj.setCuttingPlanId(cp.getId());
                        obj.setProjet(cp.getProjet());
                        obj.setVersion(cp.getVersion());
                        obj.setModele(cp.getDescription());
                        obj.setDefinition(cp.getDefinition());
                        Projet projet = projetService.findByObjId(cp.getProjet());
                        if (projet != null && projet.getZone() != null) {
                            obj.setZone(projet.getZone());
                        } else {
                            obj.setZone(zoneRepository.findByCode(arrPf.get(0).getAreaProduitFinit()));
                        }

                        List<CuttingRequestPartNumberV2> cppnArr = new ArrayList<CuttingRequestPartNumberV2>();
                        for (ProduitFinit pf : arrPf) {
                            CuttingRequestPartNumberV2 pn = new CuttingRequestPartNumberV2();
                            pn.setDescription(pf.getDesiProdFinit());
                            pn.setPartNumber(pf.getRefProdFinit());
                            pn.setItem(pf.getRefProdSemi());
                            pn.setQuantity(Integer.parseInt(pf.getQtyTotalPartNumber()));
                            pn.setWo(pf.getNoff());
                            pn.setWoid(pf.getWoid());
                            pn.setCuttingRequest(obj);
                            cppnArr.add(pn);
                        }
                        obj.setCuttingRequestPartNumbers(cppnArr);

                        List<CuttingRequestSerieV2> crsArr = new ArrayList<CuttingRequestSerieV2>();
                        List<String> goodSeriesArr = new ArrayList<String>();

                        for (Matlassage mt : matlassageRepository.findByNofOrderByNserie(sequence)) {
                            try {
                                Coupe coupe = coupeRepository.findFirstByNserie(mt.getNserie());
                                if (mt != null) {
                                    goodSeriesArr.add(mt.getNserie() + "");
                                }

                                CuttingRequestSerieV2 crs = new CuttingRequestSerieV2();
                                Optional<CuttingRequestSerieV2> crsopt = obj.getCuttingRequestSeries().stream()
                                        .filter(cuttingRequestSerie -> cuttingRequestSerie.getSerie()
                                                .equals(mt.getNserie().toString()))
                                        .findFirst();
                                if (crsopt.isPresent()) {
                                    crs = crsopt.get();
                                }
                                crs.setSerie(mt.getNserie() + "");
                                crs.setPartNumberMaterial(mt.getReftissu());
                                crs.setDescription(mt.getDescription());
                                crs.setMatelassageEndroit(mt.getSens());
                                crs.setLongueur(Double.parseDouble(mt.getLongueur()));
                                crs.setQuantite(mt.getQuantite());
                                crs.setPartNumbers(mt.getModele());
                                crs.setGroupPlacement(1);
                                crs.setActivated(true);
                                crs.setMachine(mt.getMachine());

                                if (mt.getReturnMagasin() != null)
                                    crs.setRetourMagasin(Double.parseDouble(mt.getReturnMagasin()));
                                crs.setMaxDrill(null);
                                crs.setMaxPlie(null);
                                crs.setMaxPlieDrill(null);
                                crs.setNbrCouche(Integer.parseInt(mt.getnCouches()));
                                crs.setPlacement(mt.getPlacement());
                                crs.setLaize(Double.parseDouble(mt.getLaLaizeDemande()));
                                crs.setConfig(coupe.getConfiguration());
                                crs.setDrill(coupe.getDrill1() + "," + coupe.getDrill2());
                                crs.setPerimetre(perimetreMap.get(crs.getPlacement()));
                                crs.setNbrPiece(nbrPieceMap.getOrDefault(crs.getPlacement(), 0));
                                if (crs.getNbrCouche() != null && crs.getNbrPiece() != null) {
                                    crs.setNbrPieceTotal((double) (crs.getNbrPiece() * crs.getNbrCouche()));
                                }
                                crs.setCuttingRequest(obj);
                                if (obj.getPlanningDate() == null || obj.getShift() == null) {
                                    obj.setPlanningDate(
                                            LocalDate.parse(mt.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                                    LocalTime time = LocalTime.parse(mt.getHeure(), DateTimeFormatter.ofPattern("HH:mm"));
                                    time.plusHours(2);
                                    int hour = time.getHour();
                                    if (hour < 8 && hour >= 0) {
                                        obj.setShift("1");
                                    } else if (hour < 16 && hour >= 8) {
                                        obj.setShift("2");
                                    } else {
                                        obj.setShift("3");
                                    }
                                }
                                crs.setPlanningDate(obj.getPlanningDate());
                                crs.setShift(obj.getShift());
//					if(mt.getDate() != null && !mt.getDate().trim().isEmpty() && mt.getHeure() != null && !mt.getHeure().trim().isEmpty()) {
//						if(crs.getDateDebutMatelassage() == null) crs.setDateDebutMatelassage(LocalDateTime.parse(mt.getDate() + " "+ mt.getHeure(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
//						if(crs.getDateFinMatelassage() == null) crs.setDateFinMatelassage(crs.getDateDebutMatelassage());
//					}

                                if (crs.getDateDebutCoupe() == null && coupe.getDatedebut() != null
                                        && !coupe.getDatedebut().trim().isEmpty())
                                    crs.setDateDebutCoupe(LocalDateTime.parse(coupe.getDatedebut(),
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                                if (crs.getDateFinCoupe() == null && coupe.getDateFin() != null
                                        && !coupe.getDateFin().trim().isEmpty())
                                    crs.setDateFinCoupe(LocalDateTime.parse(coupe.getDateFin(),
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                                if (mt.getStatu().equalsIgnoreCase("complet")) {
                                    crs.setStatusMatelassage("Complete");
                                } else if (mt.getStatu().equalsIgnoreCase("incomplet")) {
                                    crs.setStatusMatelassage("Incomplete");
                                } else if (mt.getStatu().equalsIgnoreCase("Non demarre")) {
                                    crs.setStatusMatelassage("Waiting");
                                } else {
                                    crs.setStatusMatelassage("In progress");
                                }

                                List<MarkerLog> mkArr = markerLogService.findBySerie(crs.getSerie());
                                if (mkArr.size() > 0) {
                                    MarkerLog mk = mkArr.get(0);
                                    crs.setDateDebutMatelassage(mk.getCreatedAt());
                                    crs.setDateFinMatelassage(mk.getUpdatedAt());
                                }


                                if (coupe.getStatut().equalsIgnoreCase("complet")) {
                                    crs.setStatusCoupe("Complete");
                                } else if (coupe.getStatut().equalsIgnoreCase("incomplet")) {
                                    crs.setStatusCoupe("Incomplete");
                                } else if (coupe.getStatut().equalsIgnoreCase("Non demarre")) {
                                    if (crs.getDateDebutCoupe() != null && crs.getDateFinCoupe() == null) {
                                        crs.setStatusCoupe("In progress");
                                    } else {
                                        crs.setStatusCoupe("Waiting");
                                    }

                                }
                                crs.setTableMatelassage(mt.getTablee());
                                crs.setTableCoupe(coupe.getMachine());
                                crs.setMatelasseur1(mt.getMatMatlasseur1());
                                crs.setMatelasseur2(mt.getMatMatlasseur2());
                                crs.setMatelasseur3(mt.getMatMatlasseur3());
                                crs.setMatelasseur4(mt.getMatMatlasseur4());

                                crs.setCoupeur1(coupe.getMatricule());
                                if (tempCoupeMap.containsKey(crs.getPlacement())
                                        && tempCoupeMap.get(crs.getPlacement()) > 0) {
                                    crs.setTempsDeCoupe(tempCoupeMap.get(crs.getPlacement()));
                                } else {
                                    if (crs.getPerimetre() != null) {
                                        CuttingSpeed speed = cuttingSpeedService.findById(crs.getConfig());
                                        if (speed != null && speed.getVitesse() != null) {
                                            crs.setTempsDeCoupe(UtilFunctions
                                                    .convertTwoDigit(crs.getPerimetre() / (speed.getVitesse() * 100), 5));
                                        } else {
                                            crs.setTempsDeCoupe(UtilFunctions.convertTwoDigit(crs.getPerimetre() / 300, 5));
                                        }

                                    }
                                }

                                crsArr.add(crs);
                            } catch (Exception e) {
                                System.out.println("Exception CRS : " + mt.getNserie() + " : " + e.getMessage());
                            }
                        }
                        obj.setCuttingRequestSeries(crsArr);
                        List<CuttingRequestBoxV2> crbArr = new ArrayList<CuttingRequestBoxV2>();
                        for (GammeTechniqueImprimer gt : gammeTechniqueImprimerService.findBySequence(sequence)) {
                            CuttingRequestBoxV2 crb = new CuttingRequestBoxV2();
                            crb.setId((gt.getnSerieGammeImp()) + "");
                            crb.setPartNumber(gt.getPartNumberImp());
                            crb.setDescription(gt.getDescriptionImp());
                            crb.setItem(gt.getCode3Imp());
                            crb.setWo(gt.getNofImp());
                            crb.setWoid(gt.getWoidImp());
                            crb.setQtyBox(Integer.parseInt(gt.getQuantiteImp()));
                            crb.setCuttingRequest(obj);
                            crbArr.add(crb);
                        }
                        obj.setCuttingRequestBoxs(crbArr);
                        cuttingRequestV2Service.save(obj);
                        cuttingRequestV2Service.deleteSeriesOther(sequence, goodSeriesArr);
                    }
                }
            } catch (Exception e) {
                log.error("WorkOrderTask sequence={} processing failed", sequence, e);
            }
        }


    }

    //************************************************************************************************************************

    //    @Scheduled(fixedRate = 1000 * 60 * 60 * 6)
    public void fillNbrPiece() {
        List<String> placementArr = cuttingRequestSerieDataService.findDistinctPlacement();
        System.out.println("fillNbrPiece : " + placementArr.size());
        int i = 0;
        for (String placement : placementArr) {
            try {
                //get nbr piece
                Integer countNbrPiece = ExcelHelper.getTotalEmp(placement);
                if (countNbrPiece != null) {
                    System.out.println(i + " : " + placementArr.size() + "Placement : " + placement + " : " + countNbrPiece);
                    cuttingRequestSerieDataService.updateNbrPiece(placement, countNbrPiece);
                }

            } catch (Exception e) {
                System.out.println("fillNbrPiece : " + placement + " : " + e.getMessage());
            }
            i++;
        }
    }


    // @Scheduled(fixedRate = 1000 * 60 * 15)
    public void woRapport() throws IOException {
        String server = "10.49.0.46";// txtServer1.getText();
        int port = 21;
        String user = "mfg";// txtUserId1.getText();
        String pass = "leartsi01";// txtPassword1.getText();
        String qadLink = "/qad/home/ftpkpitnr/";
        String[] rapports = {"16_3_2A", "16_3_2R", "16_3_2AA", "16_3_2RR", "16_3_2C"}; // 16_3_2AA.prn 16_3_2RR.prn
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            for (String rapport : rapports) {
                try {

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(ftpClient.retrieveFileStream(qadLink + rapport + ".prn")));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.length() < 130 || line.contains("Qty Completed Qty Rejected     Qty Open")
                                || line.contains("------------") || line.contains("Work Order")
                                || line.contains("TANGIER-TRIM") || line.contains("Work Order by Item Report")) {
                            continue;
                        }
                        WorkOrder obj = new WorkOrder();
                        obj.setItem(line.substring(0, 24).toUpperCase().trim());
                        obj.setWo(line.substring(25, 43).toUpperCase().trim());
                        obj.setWoid(line.substring(44, 52).toUpperCase().trim());
                        try {
                            obj.setQtyCompleted(Double.parseDouble(line.substring(53, 66).replaceAll(",", "")));
                        } catch (Exception e) {
                        }
                        try {
                            obj.setQtyRejeter(Double.parseDouble(line.substring(67, 79).replaceAll(",", "")));
                        } catch (Exception e) {
                        }
                        try {
                            obj.setQtyOpen(Double.parseDouble(line.substring(80, 92).replaceAll(",", "")));
                        } catch (Exception e) {
                        }
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

                        obj.setDueDate(LocalDate.parse(line.substring(101, 110).toUpperCase().trim(), formatter));
                        obj.setShift(line.substring(120, 128).toUpperCase().trim());
                        obj.setStatus(line.substring(129, line.length()).toUpperCase().trim());
                        if (obj.getItem().toUpperCase().startsWith("WL")) {
                            obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
                        }

                        Planning pl = planningService.findFirstByItem(obj.getItem());
                        PartNumberInfo2 pnInfo2 = new PartNumberInfo2();
                        if (pl != null) {
                            obj.setPartNumber(pl.getPartNumber());
                            pnInfo2 = partNumberInfo2Repository.findByPartNumber(pl.getPartNumber());
                            obj.setDescription(pl.getDescription());
                        } else if (obj.getItem().toUpperCase().startsWith("WL")) {
                            obj.setPartNumber(obj.getItem().toUpperCase().substring(1));
                            pnInfo2 = partNumberInfo2Repository
                                    .findByPartNumber(obj.getItem().toUpperCase().substring(1));
                        }

                        if (pnInfo2 != null) {
                            obj.setGroupName(pnInfo2.getItemGroup());
                            obj.setDesignGroup(pnInfo2.getDesignGroup());
                            obj.setPartNumberStatus(pnInfo2.getStatus());
                            obj.setCoverGroup(pnInfo2.getCovertype());
                            obj.setDescription(pnInfo2.getDescription());
                        } else {
                            List<Files> files = filesService.findBySemiFinishedGoodPartNumber(obj.getItem());
                            if (files.size() > 0) {
                                Files file = files.get(0);
                                obj.setDescription(file.getPartNumberCoverDesciption());
                                obj.setPartNumber(file.getPartNumberCover());
                                obj.setGroupName(file.getProjet());
                            }
                        }

                        WorkOrder woOld = service.findByWo(obj.getWo());
                        if (woOld != null) {
                            if (woOld.getCreatedAt() != null) {
                                obj.setCreatedAt(woOld.getCreatedAt());
                            } else {
                                obj.setCreatedAt(LocalDateTime.now());
                            }
                        }
                        obj.setUpdatedAt(LocalDateTime.now());
                        service.save(obj);
                    }
                    System.out.println("Done : " + rapport);

                    reader.close();
                    ftpClient.completePendingCommand();
                } catch (Exception e) {
                    System.out.println(rapport + " ERROR : " + e.getMessage());
                }
            }

        } finally {
            ftpClient.disconnect();
        }

    }

	/*
	@Scheduled(fixedRate = 1000 * 60 * 10)

	public void fillEmptySerie() {
		List<CuttingRequestSerieRouleauData> arr = cuttingRequestSerieRouleauDataService.findEmptySerie();

		for(CuttingRequestSerieRouleauData crsr : arr) {
			try {
				if (crsr.getRetour() != null) {
					List<Matlassage> mtArr = matlassageRepository.findByReftissuAndReurnMagasin(crsr.getConfirmReftissu(), crsr.getRetour() + "");
					if (mtArr.size() == 1) {
						System.out.println(
								crsr.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
										+ " => " + crsr.getConfirmReftissu() + " " + crsr.getRetour() + " => " + mtArr.get(0).getNserie());
						crsr.setSerie(mtArr.get(0).getNserie() + "");
						cuttingRequestSerieRouleauDataService.save(crsr);

					} else if (mtArr.size() > 1) {
						// taker only the one that has the date start with crsr.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")
						List<Matlassage> mtArrFiltered = new ArrayList<Matlassage>();
						for (Matlassage mt : mtArr) {
							if (mt.getDate().startsWith(crsr.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")))) {
								mtArrFiltered.add(mt);
							}
						}
						if (mtArrFiltered.size() == 1) {
							System.out.println("Filtered GOOD : " +
									crsr.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
									+ " => " + crsr.getConfirmReftissu() + " " + crsr.getRetour() + " => " + mtArrFiltered.get(0).getNserie());
							crsr.setSerie(mtArrFiltered.get(0).getNserie() + "");
							cuttingRequestSerieRouleauDataService.save(crsr);

						} else {
							System.out.println("Filtered " + mtArrFiltered.size() + " : " +
									crsr.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
									+ " => " + crsr.getConfirmReftissu() + " " + crsr.getRetour() + " => " + "No serie");
						}
					} else {
						System.out.println("NOT FOUND" +
								crsr.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
								+ " => " + crsr.getConfirmReftissu() + " " + crsr.getRetour() + " => " + "No serie");
					}
				}
			} catch (Exception e) {
				System.out.println("Serie :" + crsr.getSerie() + " : " + e.getMessage());
			}
		}

	}
	*/
}
