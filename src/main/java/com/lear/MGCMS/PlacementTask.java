package com.lear.MGCMS;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.domain.CuttingPlan.*;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanData;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementData;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanHistoryRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLight2Repository;
import com.lear.MGCMS.services.*;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanDataService;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanMaterialPlacementDataService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.MGCMS.utils.UtilFunctions;
import com.lear.cms.domain.*;
import com.lear.cms.domain.GammeTechnique;
import com.lear.cms.repositories.SpreadingCuttingPlanCoupeRepository;
import com.lear.cms.repositories.TimingModelRepository;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lear.MGCMS.payload.EmpStat;
import com.lear.MGCMS.repositories.PlacementFolderRepository;
import com.lear.MGCMS.repositories.PlanningRepository;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanMaterialPlacementService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanMaterialService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanPartNumberService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanService;
import com.lear.MGCMS.services.cms.PlanCoupeService;
import com.lear.MGCMS.services.ctc.FilesService;
import com.lear.cms.repositories.GammeTechniqueCMSRepository;
import com.lear.ctc.domain.Files;

//@Component
public class PlacementTask {

    @Autowired
    private PlacementDetailService placementDetailService;
    @Autowired
    private PlacementService placementService;
    @Autowired
    private PlacementFolderRepository placementFolderRepository;
    @Autowired
    private FilesService filesService;
    @Autowired
    private GammeTechniqueCMSRepository gammeTechniqueRepository;
    @Autowired
    private PartNumberInfo2Service partNumberInfo2Service;
    @Autowired
    private PartNumberBoomService partNumberBoomService;
    @Autowired
    private PlanningRepository planningRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private PlanCoupeService planCoupeService;
    @Autowired
    private CuttingPlanLight2Repository cuttingPlanLight2Repository;
    @Autowired
    private UserService userService;
    @Autowired
    private CuttingPlanPartNumberService cuttingPlanPartNumberService;
    @Autowired
    private CuttingPlanMaterialService cuttingPlanMaterialService;
    @Autowired
    private CuttingPlanMaterialPlacementService cuttingPlanMaterialPlacementService;
    @Value("${lear.pltfolder}")
    private String pltfolder;
    @Autowired
    private CuttingPlanService cuttingPlanService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private CuttingPlanDataService cuttingPlanDataService;
    @Autowired
    private CuttingPlanHistoryRepository cuttingPlanHistoryRepository;
    @Autowired
    private StockStatusReportService stockStatusReportService;

    @Scheduled(fixedRate = 1000 * 60)
    private void verifyNotFound() {
        List<Files> files = filesService.findNotFound();
//		System.out.println("verifyNotFound arr : " + files.size() + " " + LocalDateTime.now());
        String folderPath = pltfolder + "";
        int i = 0, percentage = 0;
        for (Files obj : files) {
            i++;
            String fileName = obj.getPattern() + ".plt";
            File file = new File(folderPath, fileName);
            try {
                if (file.exists()) {
                    obj.setPltFound(true);
                    filesService.save(obj);
                } else {
                    if (obj.getPltFound() == null) {
                        obj.setPltFound(false);
                        filesService.save(obj);
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    @Scheduled(fixedRate = 1000 * 60 * 10)
    public void loadPlanCoupe() {
        User user = userService.findByUsername("melghazi");
        int percentage = 0;
//		Long id = Long.parseLong("2001");
//		Long max = planCoupeService.maxId();

        List<PlanCoupe> arrPc = planCoupeService.findAllLightBetween(LocalDate.now().minusDays(3), LocalDate.now());
        Map<String, User> mapUsers = new HashMap<>();
        mapUsers.put(user.getMatricule(), user);
        Map<String, String> matriculeMap = queryService.findMatriculeMapCMS();
        for (PlanCoupe pcLight : arrPc) {
            while (percentage < (int) (100 * ((float) (arrPc.size() - pcLight.getIdPlanCoupe()) / arrPc.size()))) {
                percentage++;
                System.out.println("Loading CMS CP : " + pcLight.getIdPlanCoupe() + "  " + percentage + "/100 : ");
            }

            if (pcLight.getCreatedByHostaNamePlanCoupe().isEmpty() || Objects.equals(pcLight.getCreatedByHostaNamePlanCoupe(), "CMS WEB")) {
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
                    System.out.println("RENEW id : " + pcLight.getIdPlanCoupe() + " / " + (cpObj != null ? cpObj.getId() : "null"));
//					if (pcLight.getStatusPlanCoupe() == false)
//						continue;
//					System.out.println("plan coupe id : " + id);
                    PlanCoupe pc = planCoupeService.findById(pcLight.getIdPlanCoupe());
                    CuttingPlan obj = new CuttingPlan();

                    if (cpObj != null && cpObj.getId() != null) {
                        cuttingPlanService.deleteByPlanCoupeId(cpObj.getId());
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
                    if (pc.getCreatedByUserNamePlanCoupe() != null) {
                        if (matriculeMap.containsKey(pc.getCreatedByUserNamePlanCoupe())) {
                            String matricule = matriculeMap.get(pc.getCreatedByUserNamePlanCoupe());
                            if (mapUsers.containsKey(matricule)) {
                                obj.setCreatedBy(mapUsers.get(pc.getCreatedByUserNamePlanCoupe()));
                            } else {
                                User userObj = userService.findByMatricule(matricule);
                                if (userObj != null) {
                                    obj.setCreatedBy(userObj);
                                    mapUsers.put(matricule, userObj);
                                }
                            }
                        }
                    }
                    if (pc.getModifiedByUserNamePlanCoupe() != null) {
                        if (matriculeMap.containsKey(pc.getModifiedByUserNamePlanCoupe())) {
                            String matricule = matriculeMap.get(pc.getModifiedByUserNamePlanCoupe());
                            if (mapUsers.containsKey(matricule)) {
                                obj.setUpdatedBy(mapUsers.get(pc.getModifiedByUserNamePlanCoupe()));
                            } else {
                                User userObj = userService.findByMatricule(matricule);
                                if (userObj != null) {
                                    obj.setUpdatedBy(userObj);
                                    mapUsers.put(matricule, userObj);
                                }
                            }
                        }
                    }
                    if (pc.getEnabledByUserNamePlanCoupe() != null) {
                        if (matriculeMap.containsKey(pc.getEnabledByUserNamePlanCoupe())) {
                            String matricule = matriculeMap.get(pc.getEnabledByUserNamePlanCoupe());
                            if (mapUsers.containsKey(matricule)) {
                                obj.setEnabledBy(mapUsers.get(pc.getEnabledByUserNamePlanCoupe()));
                            } else {
                                User userObj = userService.findByMatricule(matricule);
                                if (userObj != null) {
                                    obj.setEnabledBy(userObj);
                                    mapUsers.put(matricule, userObj);
                                }
                            }
                        }
                    }
                    if (pc.getDisabledByUserNamePlanCoupe() != null) {
                        if (matriculeMap.containsKey(pc.getDisabledByUserNamePlanCoupe())) {
                            String matricule = matriculeMap.get(pc.getDisabledByUserNamePlanCoupe());
                            if (mapUsers.containsKey(matricule)) {
                                obj.setDisabledBy(mapUsers.get(pc.getDisabledByUserNamePlanCoupe()));
                            } else {
                                User userObj = userService.findByMatricule(matricule);
                                if (userObj != null) {
                                    obj.setDisabledBy(userObj);
                                    mapUsers.put(matricule, userObj);
                                }
                            }
                        }
                    }
                    if (pc.getHourCreatedPlanCoupe() != null && pc.getDateCreatedPlanCoupe() != null) {
                        obj.setCreatedAt(pc.getDateCreatedPlanCoupe().atTime(pc.getHourCreatedPlanCoupe()));
                    }
                    if (pc.getDateModifiedPlanCoupe() != null && pc.getHourModifiedPlanCoupe() != null) {
                        obj.setUpdatedAt(pc.getDateModifiedPlanCoupe().atTime(pc.getHourModifiedPlanCoupe()));
                    }
                    if (pc.getDateEnabledPlanCoupe() != null && pc.getHourEnabledPlanCoupe() != null) {
                        obj.setEnabledAt(pc.getDateEnabledPlanCoupe().atTime(pc.getHourEnabledPlanCoupe()));
                    }
                    if (pc.getDateDisabledPlanCoupe() != null && pc.getHourDisabledPlanCoupe() != null) {
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
                                    (int) Math.ceil(Double.parseDouble(pc.getQuantityPlanCoupe() + "") / Double.parseDouble(scpc.getQuantityPerLayerPlanCoupe() + "")));
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
                    cuttingPlanService.save(obj, user);
//					System.out.println("SAVED id : " + pcLight.getIdPlanCoupe());
                }
            } catch (Exception e) {
                System.out.println("error id : " + pcLight.getIdPlanCoupe() + " : " + e.getMessage());
            }
        }
    }

    Double convertTwoDigit(Double num) {
        return Double.parseDouble(String.format("%.4f", num).replace(",", "."));
    }

    @Scheduled(fixedRate = 1000 * 60 * 4)
    public void loadR100Report() {
        String reportName = "R100";
        String server = "10.49.0.46";// txtServer1.getText();
        int port = 21;
        String user = "mfg";// txtUserId1.getText();
        String pass = "leartsi01";// txtPassword1.getText();
        String qadLink = "/qad/home/batchtnr/" + reportName + ".prn";
        FTPClient ftpClient = new FTPClient();
        String line = "";
        int i = 0;
        int percentage = 0;
        int countSaved = 0;
        System.out.println("Current time: " + LocalDateTime.now());

        try {
            ftpClient.connect(server, port);
            if (ftpClient.login(user, pass)) {
                // System.out.println("Login et password ok");
            } else {
                System.out.println("login error (Component)");
            }
            ftpClient.enterLocalPassiveMode();
            if (ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
//                System.out.println("Connecter (Component)");
            } else {
                System.out.println("ftp type error 2 (Component)");
            }
            String remoteFile1 = qadLink;
            File downloadFile1 = new File("C:\\R100-MP\\R100.prn");// new File(jTextField3.getText());
            boolean success;
            try (OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1))) {
                success = ftpClient.retrieveFile(remoteFile1, outputStream1);
                outputStream1.close();
            }
            if (success) {
                System.out.println("Date End : " + LocalDateTime.now());

                Path sourcePath = downloadFile1.toPath();
                Path destinationPath = Paths.get("\\\\matnr-fp01\\Groups\\CMS WEB\\cmsFolder\\reportsNew\\R100.prn");
                java.nio.file.Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File copied to the new directory successfully.");

            } else {
                System.out.println("File has not been  downloaded (Component)");
            }

            

        } catch (IOException ex) {
            System.out.println("IOException (Component) : " + ex.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {

            }
        }


        // in C:\R100-MP get the lestedt updated file that his name start with R- and get the BufferedReader
        File folder = new File("C:\\R100-MP");
        File[] listOfFiles = folder.listFiles();
        Arrays.sort(listOfFiles, Comparator.comparingLong(File::lastModified).reversed());
        String fileName = "";
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().startsWith("R-")) {
                fileName = file.getName();
                break;
            }
        }
        if (fileName.isEmpty()) {
            return;
        }
        String[] liste = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("C:\\R100-MP\\" + fileName)));
            liste = br.lines().collect(Collectors.toList())
                    .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
        } catch (Exception e) {
            System.out.println("ERROR : loadR100Report : " + e.getMessage());
        }
        if (liste == null) {
            return;
        }

        List<String> newList = new ArrayList<String>();
        PrintWriter pw = null;
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(qadLink)));
            String itemNumber = "", um = "", abc = "", site = "",
                    location = "", ref = "";
            LocalDate date = null;
            System.out.println("Current time: " + LocalDateTime.now());
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("C:\\R100-MP\\R-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".txt"), "windows-1252")));
            while ((line = reader.readLine()) != null) {
                try {
                    ref = "";
                    i++;

                    while (percentage < (int) (10 * ((float) i / 71700))) {
                        percentage++;
//                        System.out.println("Loading R100 : " + i + "  " + percentage + "/10 : " + countSaved);
                    }

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
                        // fill line with spaces until it reaches 131
                        line = line + String.join("", Collections.nCopies(131 - line.length(), " "));
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
                        date = LocalDate.parse(line.substring(43, 51).trim(), DateTimeFormatter.ofPattern("MM/dd/yy"));
                    }
                    if (!line.substring(80, 88).trim().isEmpty()) {
                        location = line.substring(80, 88).trim();
                    }
                    ref = line.substring(89, 107).trim();
                    if (ref.isEmpty() || line.substring(108, 121).trim().isEmpty() || !location.toUpperCase().startsWith("T0") || !um.equalsIgnoreCase("MT")) {
                        continue;
                    }
                    pw.println(itemNumber + " " + ref + " " + location + " " + Double.parseDouble(line.substring(108, 121).trim()) + " " + line.substring(122, 130).trim() + " " + date);
                    newList.add(itemNumber + " " + ref + " " + location + " " + Double.parseDouble(line.substring(108, 121).trim()) + " " + line.substring(122, 130).trim() + " " + date);
                    //                    stockStatusReportService.save(new StockStatusReport(
//                            itemNumber,
//                            um,
//                            abc,
//                            site,
//                            date,
//                            location,
//                            ref,
//                            Double.parseDouble(line.substring(108, 121).trim()),
//                            line.substring(122, 130).trim(),
//                            LocalDateTime.now()
//                    ));
                    countSaved++;
                } catch (Exception e) {
                    itemNumber = "";
                    um = "";
                    abc = "";
                    site = "";
                    location = "";
                    ref = "";
                    date = null;
                    System.out.println("ERROR2 : loadR100Report : " + e.getMessage());
                }
            }
            System.out.println("Last date : " + LocalDateTime.now());
        } catch (Exception e) {
            System.out.println("ERROR : loadR100Report : " + e.getMessage());
        } finally {
            try {
                System.out.println("DONE : "  + LocalDateTime.now());
                pw.close();
            } catch (Throwable e) {
            }
        }

        PrintWriter pwDiff = null;

        try {
            pwDiff = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("C:\\R100-MP\\DF-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".txt"), "windows-1252")));
            for (String line1 : liste) {
                if (!newList.contains(line1)) {
                    pwDiff.println("REMOVED : " + line1);
                    stockStatusReportService.save(
                            new StockStatusReport(
                                    line1.split(" ")[0],
                                    "MT",
                                    null,
                                    "MA10TR01",
                                    LocalDate.parse(line1.split(" ")[5], DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                    line1.split(" ")[2],
                                    line1.split(" ")[1],
                                    Double.parseDouble(line1.split(" ")[3]),
                                    line1.split(" ")[4],
                                    LocalDateTime.now(),
                                    true
                            )
                    );
                }
            }

            for (String line1 : newList) {
                if (!Arrays.asList(liste).contains(line1)) {
                    pwDiff.println("NEW : " + line1);
                    stockStatusReportService.save(
                            //    public StockStatusReport(String itemNumber, String um, String abc, String site, LocalDate lastCnt, String location, String ref, Double qtyOnHand, String status, LocalDateTime lastUpdated)
                            new StockStatusReport(
                                    line1.split(" ")[0],
                                    "MT",
                                    null,
                                    "MA10TR01",
                                    LocalDate.parse(line1.split(" ")[5], DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                    line1.split(" ")[2],
                                    line1.split(" ")[1],
                                    Double.parseDouble(line1.split(" ")[3]),
                                    line1.split(" ")[4],
                                    LocalDateTime.now(),
                                    false
                            )
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("ERROR : loadR100Report : " + e.getMessage());
        } finally {
            try {
                System.out.println("DONE Saving : "  + LocalDateTime.now());
                pwDiff.close();
            } catch (Throwable e) {
            }
        }


    }

    //************************************************************************************************************************

    @Autowired
    private CuttingPlanMaterialPlacementDataService cuttingPlanMaterialPlacementDataService;
    @Autowired
    private SpreadingCuttingPlanCoupeRepository spreadingCuttingPlanCoupeRepository;

    //    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void refreshLongueurCuttingPlan() {
        System.out.println("Current time: " + LocalDateTime.now());
        List<String> arr = new ArrayList<String>(Arrays.asList("1CGC387-195", "1CGA447-195", "1CGO447-0BF-195", "1CGC447-195", "1CGO385-0BF-195", "1CGC385-195", "1CIA420-195", "1CIA420-195", "1CIA420-195", "1CHO697-0BF-195", "1CIO434-195", "1CGO386-0bf-195", "1CGC386-195", "1CGC376-195", "1CIA481-195", "1CIC329-195", "1CIA329-195", "1cia236-195", "1CIO236-195", "1cia236-195", "1CIO236-195", "1CIO236-195", "1cia236-195", "1CGC377-195", "1CIO481-195", "1CGO377-0BF-195", "1CIO368-195", "1cia368-195", "1CIO495-0BF-195", "1CIO495-0BF-195", "1CGO398-0BF-195", "1CGC398-195", "1CIA430-195", "1CGO376-0BF-195", "1CGA356-0BF-195", "1CGO356-0BF-195", "1CGA376A-0BF-195", "1CGA376A-0BF-195", "1CGC356-195", "1CGO337-0BF-195", "1CGA337-0BF-195", "1CHO739-0BF-195", "1CIA582-0BF-195", "1CIO582-195", "1CGC337-195", "1CHA739A-195", "1CHO711-195", "1CKA561-195", "1CIA495-195", "1CKA561-195", "1CKA334-195", "1CIA476-195", "1CGO375-0BF-195", "1CIA468-0BF-195", "1CIC468-195", "1cka354-195", "1CGC375-195", "1CHI663-195", "1CIO476-195", "1CIO468-0BF-195", "1CKA506-195", "1CIO584-0BF-195", "1CIO584-0BF-195", "1CIA480-195", "1CJA104-0BF-195", "1CIO480-0bf-195", "1CKA335-0BF-195", "1CHA687-0BF-195", "1CGO208-0BF-195", "1CGA208-0BF-195", "1CLA115-0BF-195", "1CLA171-0BF-195", "1CGA449-195", "1CGO449-195", "1CGC449-195", "1CIO618-195", "1CHO705-195", "1CKA616-0BF-195", "1CIA495-0BF-195", "1CHO558-0bf-195", "1CHA558-0bf-195", "1CKA564-195", "1CIA461-195", "1CII461-195", "1CHO699-0BF-195", "1CIO632-195", "1CKA332-195", "1CIO518-0BF-195", "1CIA518-0BF-195", "1CHA699-0BF-195", "1CKO626-195", "1CKA607-195", "1CIA562-0BF-195", "1CIO562-0BF-195", "1CIC562-195", "1CHO559-0bf-195", "1CHA559-195", "1CMA242-195", "1CHA685-195", "1CKA690-195", "1CKA441-195", "1CIC456-195", "1CIO606-195", "1CGC378-195", "1CKA488-195", "1CKA618-195", "1CIO631-0BF-195", "1CKA618-195", "1CGO378-0BF-195", "1CKO610-0bf-195", "1CKA610-0bf-195", "1CHO680-195", "1CHA680-195", "1CKA603-195", "1CMA217-195", "1CM4217-195", "1CIO529-0BF-195", "1CIA529-0BF-195", "1CHA549-195", "1CMA194-195", "1CGC227-195", "1CGO227-0BF-195", "__1CLA186-ET-195", "1CGC455-195", "1CGO453-195", "1CHO704-0BF-195", "1CKA612-0BF-195", "1CGO380-0BF-195", "1CGC380-195", "1CMA219-195", "1CKA537-0BF-195", "1CIO519-195", "1CHO549-195", "1CMA223-0BF-195", "1CM4201-195", "1CMA197-195", "1CKA537-0BF-195", "1CKA534-0BF-195", "1CHA684-0BF-195", "1CKA625-195", "1CKA627-195", "1CKA629A-0BF-195", "1CKA629A-0BF-195", "1CHO710-195", "1CMA226-0BF-195", "1CHA684-0BF-195", "1CHO684-0BF-195", "1CIO586-0BF-195", "1CIA586-0BF-195", "1CKA602-195", "1CGA319-0BF-195", "1CGO319-0BF-195", "1CMA202-0bf-195", "1CHA543-195", "1CHO543-195", "1CIA491-195", "1CIO491-195", "1CIA487-195", "1CIO487-0BF-195", "1CHA537-195", "1CHO537-0BF-195", "1CHA536-195", "1CHO536-0BF-195", "1CHA531-195", "1CGO319-0BF-195", "1CGA319-0BF-195", "1CHA485-195", "1CGC234-195", "1CGO234-0BF-195", "1CHO894-195", "1CHO797-0BF-195", "1CHA681-0BF-195", "1CHA606-0BF-195", "1CIA684-0BF-195", "1CIO684-0BF-195", "__1CLA184-ET-195", "1CHA785-195", "1CIA683-195", "1CIO683-195", "1CHO776-0BF-195", "1CIA679-195", "1CHA733-0BF-195", "1CIA678-195", "1CHA683-0BF-195", "1CKA697-195", "1CJA026-195", "1CGC483-195", "1CGC474-195", "1CGA464-195", "1CGO461-195", "1CGC461-195", "1CHO738-0BF-195", "1CHO709-195", "1CGO451-195", "1CGC451-195", "1CIA609-195", "1CGO446-0BF-195", "1CGC446-195", "1CGO445-0BF-195", "1CGC445-195", "1CGA445-195", "1CIO600-0BF-195", "1CHA698-195", "1CHC698-195", "1CHO698-195", "1CKA614-195", "1CIA585-0BF-195", "1CIO585-0BF-195", "1CIA583-0BF-195", "1CIO583-0BF-195", "1CHA607-0BF-195", "1CHC607-195", "1CHO607-195", "1CIA580-0BF-195", "1CIO580-0bf-195", "1CHA678-195", "1CHO678-195", "1CMA225-0BF-195", "1CMA221-195", "1CM4221-195", "1CMA216-195", "1CHA660-0BF-195", "1CKA582-0BF-195", "1CLA120-195", "1CMA212-0BF-195", "1CHA660-0BF-195", "1CIO537-0BF-195", "1CMA205-195", "1CGC345-195", "1CKA563-0BF-195", "1CKA549-195", "1cka440-195", "1CKO440-195", "1CHO534-195", "1CHA534-195", "1CHA530-195", "1CLA105-195", "1CGC319-195", "1CLA111-195", "__1CLA182-ET-195", "1CIA288-195", "1CLA105-195", "1CLA104-195", "1cka452-195", "1cka452-195", "1CIA288-195", "1CII288-195", "1cko411"));
        List<CuttingPlanMaterialPlacementData> cpArr = cuttingPlanMaterialPlacementDataService.getByPlacements(arr);
        for (CuttingPlanMaterialPlacementData cp : cpArr) {
            Double marge = 0.02;
            if (cp.getPliesConfigMarge().endsWith("0.02")) {
                marge = 0.02;
            }
            if (cp.getPliesConfigMarge().endsWith("0.03")) {
                marge = 0.03;
            }
            Double newLongueur = ExcelHelper.getLongueur(cp.getPlacement());
            System.out.println(cp.getPlacement() + " " + marge + " " + cp.getLongueur() + " => " + newLongueur);
            if (newLongueur == null || newLongueur == 0) {
                continue;
            }
            cp.setLongueur(newLongueur);
            cp.setLongueurMatelas(UtilFunctions.convertTwoDigit((newLongueur + marge) * cp.getNbrCouche(), 3));
            cuttingPlanMaterialPlacementDataService.save(cp);
            spreadingCuttingPlanCoupeRepository.updatePlacement(cp.getPlacement(), newLongueur, newLongueur + marge);

        }
        System.out.println("Last date : " + LocalDateTime.now());
    }

    @Value("${lear.cutfilesFolder}")
    private String cutfilesFolder;
//    @Value("${lear.cutfilesArchiveFolder}")
    private String cutfilesArchiveFolder;
    @Value("${lear.cutfilesAblLaserFolder}")
    private String cutfilesAblLaserFolder;

    //    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    public void verifyAllPlansDrill() {
        System.out.println("Loading all placements...");
        List<String> arr = cuttingPlanMaterialPlacementDataService.findVerifyDrill();
//        List<String> arrPlacement = new ArrayList<String>(Arrays.asList("1CGC387-195","1CGA447-195","1CGO447-0BF-195","1CGC447-195","1CGO385-0BF-195","1CGC385-195","1CIA420-195","1CIA420-195","1CIA420-195","1CHO697-0BF-195","1CIO434-195","1CGO386-0bf-195","1CGC386-195","1CGC376-195","1CIA481-195","1CIC329-195","1CIA329-195","1cia236-195","1CIO236-195","1cia236-195","1CIO236-195","1CIO236-195","1cia236-195","1CGC377-195","1CIO481-195","1CGO377-0BF-195","1CIO368-195","1cia368-195","1CIO495-0BF-195","1CIO495-0BF-195","1CGO398-0BF-195","1CGC398-195","1CIA430-195","1CGO376-0BF-195","1CGA356-0BF-195","1CGO356-0BF-195","1CGA376A-0BF-195","1CGA376A-0BF-195","1CGC356-195","1CGO337-0BF-195","1CGA337-0BF-195","1CHO739-0BF-195","1CIA582-0BF-195","1CIO582-195","1CGC337-195","1CHA739A-195","1CHO711-195","1CKA561-195","1CIA495-195","1CKA561-195","1CKA334-195","1CIA476-195","1CGO375-0BF-195","1CIA468-0BF-195","1CIC468-195","1cka354-195","1CGC375-195","1CHI663-195","1CIO476-195","1CIO468-0BF-195","1CKA506-195","1CIO584-0BF-195","1CIO584-0BF-195","1CIA480-195","1CJA104-0BF-195","1CIO480-0bf-195","1CKA335-0BF-195","1CHA687-0BF-195","1CGO208-0BF-195","1CGA208-0BF-195","1CLA115-0BF-195","1CLA171-0BF-195","1CGA449-195","1CGO449-195","1CGC449-195","1CIO618-195","1CHO705-195","1CKA616-0BF-195","1CIA495-0BF-195","1CHO558-0bf-195","1CHA558-0bf-195","1CKA564-195","1CIA461-195","1CII461-195","1CHO699-0BF-195","1CIO632-195","1CKA332-195","1CIO518-0BF-195","1CIA518-0BF-195","1CHA699-0BF-195","1CKO626-195","1CKA607-195","1CIA562-0BF-195","1CIO562-0BF-195","1CIC562-195","1CHO559-0bf-195","1CHA559-195","1CMA242-195","1CHA685-195","1CKA690-195","1CKA441-195","1CIC456-195","1CIO606-195","1CGC378-195","1CKA488-195","1CKA618-195","1CIO631-0BF-195","1CKA618-195","1CGO378-0BF-195","1CKO610-0bf-195","1CKA610-0bf-195","1CHO680-195","1CHA680-195","1CKA603-195","1CMA217-195","1CM4217-195","1CIO529-0BF-195","1CIA529-0BF-195","1CHA549-195","1CMA194-195","1CGC227-195","1CGO227-0BF-195","__1CLA186-ET-195","1CGC455-195","1CGO453-195","1CHO704-0BF-195","1CKA612-0BF-195","1CGO380-0BF-195","1CGC380-195","1CMA219-195","1CKA537-0BF-195","1CIO519-195","1CHO549-195","1CMA223-0BF-195","1CM4201-195","1CMA197-195","1CKA537-0BF-195","1CKA534-0BF-195","1CHA684-0BF-195","1CKA625-195","1CKA627-195","1CKA629A-0BF-195","1CKA629A-0BF-195","1CHO710-195","1CMA226-0BF-195","1CHA684-0BF-195","1CHO684-0BF-195","1CIO586-0BF-195","1CIA586-0BF-195","1CKA602-195","1CGA319-0BF-195","1CGO319-0BF-195","1CMA202-0bf-195","1CHA543-195","1CHO543-195","1CIA491-195","1CIO491-195","1CIA487-195","1CIO487-0BF-195","1CHA537-195","1CHO537-0BF-195","1CHA536-195","1CHO536-0BF-195","1CHA531-195","1CGO319-0BF-195","1CGA319-0BF-195","1CHA485-195","1CGC234-195","1CGO234-0BF-195","1CHO894-195","1CHO797-0BF-195","1CHA681-0BF-195","1CHA606-0BF-195","1CIA684-0BF-195","1CIO684-0BF-195","__1CLA184-ET-195","1CHA785-195","1CIA683-195","1CIO683-195","1CHO776-0BF-195","1CIA679-195","1CHA733-0BF-195","1CIA678-195","1CHA683-0BF-195","1CKA697-195","1CJA026-195","1CGC483-195","1CGC474-195","1CGA464-195","1CGO461-195","1CGC461-195","1CHO738-0BF-195","1CHO709-195","1CGO451-195","1CGC451-195","1CIA609-195","1CGO446-0BF-195","1CGC446-195","1CGO445-0BF-195","1CGC445-195","1CGA445-195","1CIO600-0BF-195","1CHA698-195","1CHC698-195","1CHO698-195","1CKA614-195","1CIA585-0BF-195","1CIO585-0BF-195","1CIA583-0BF-195","1CIO583-0BF-195","1CHA607-0BF-195","1CHC607-195","1CHO607-195","1CIA580-0BF-195","1CIO580-0bf-195","1CHA678-195","1CHO678-195","1CMA225-0BF-195","1CMA221-195","1CM4221-195","1CMA216-195","1CHA660-0BF-195","1CKA582-0BF-195","1CLA120-195","1CMA212-0BF-195","1CHA660-0BF-195","1CIO537-0BF-195","1CMA205-195","1CGC345-195","1CKA563-0BF-195","1CKA549-195","1cka440-195","1CKO440-195","1CHO534-195","1CHA534-195","1CHA530-195","1CLA105-195","1CGC319-195","1CLA111-195","__1CLA182-ET-195","1CIA288-195","1CLA105-195","1CLA104-195","1cka452-195","1cka452-195","1CIA288-195","1CII288-195","1cko411"));
//        List<CuttingPlanMaterialPlacementData> arr = cuttingPlanMaterialPlacementDataService.getByPlacements(arrPlacement);

        System.out.println("Total placements : " + arr.size());
        for (String cp : arr) {
            if (cp == null || cp.isEmpty()) {
                continue;
            }

            String[] cpSplit = cp.split("/");
            String placement = cpSplit[0];
            try {
                int drill1 = 0;
                int drill2 = 0;
                if (cpSplit[1] != null && cpSplit[1].contains(",")) {
                    String[] drills = cpSplit[1].split(",");
                    if (drills.length > 0 && !drills[0].isEmpty()) {
                        drill1 = Integer.parseInt(drills[0]);
                    }
                    if (drills.length > 1 && !drills[1].isEmpty()) {
                        drill2 = Integer.parseInt(drills[1]);
                    }
                }
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(
                            new FileInputStream(cutfilesFolder + "" + placement),
                            "windows-1252"));
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    try {
                        br = new BufferedReader(new InputStreamReader(
                                new FileInputStream(
                                        cutfilesAblLaserFolder + "" + placement),
                                "windows-1252"));
                    } catch (UnsupportedEncodingException | FileNotFoundException e12) {
                        try {
                            br = new BufferedReader(new InputStreamReader(
                                    new FileInputStream(
                                            cutfilesFolder + "IP6\\" + placement),
                                    "windows-1252"));
                        } catch (UnsupportedEncodingException | FileNotFoundException e1) {
                            try {
                                br = new BufferedReader(new InputStreamReader(new FileInputStream(
                                        cutfilesFolder + "Archive\\" + placement),
                                        "windows-1252"));
                            } catch (UnsupportedEncodingException | FileNotFoundException e2) {
                                try {
                                    br = new BufferedReader(new InputStreamReader(
                                            new FileInputStream(
                                                    cutfilesArchiveFolder + "" + placement),
                                            "windows-1252"));
                                } catch (UnsupportedEncodingException | FileNotFoundException e3) {
                                    System.out.println("Placement not found : " + placement + " : " + cpSplit[2]);
                                }
                            }
                        }
                    }

                } finally {
                    if (br != null) {
                        String[] liste = br.lines().collect(Collectors.toList())
                                .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                        if (liste[0].contains("M43*")) {
                            if (drill1 == 0) {
                                System.out.println("Drill 1 is missing : " + placement + " : " + cpSplit[2]);
                            }
                        } else {
                            if (drill1 > 0) {
                                System.out.println("Drill 1 is not needed : " + placement + " : " + cpSplit[2]);
                            }
                        }
                        if (liste[0].contains("M44*")) {
                            if (drill2 == 0) {
                                System.out.println("Drill 2 is missing : " + placement + " : " + cpSplit[2]);
                            }
                        } else {
                            if (drill2 > 0) {
                                System.out.println("Drill 2 is not needed : " + placement + " : " + cpSplit[2]);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("ERROR : " + placement + " : " + cpSplit[2] + " : " + e.getMessage());
            }
        }
        System.out.println("Last date : " + LocalDateTime.now());
    }
    //************************************************************************************************************************

//        @Scheduled(fixedRate = 1000 * 60 * 60) // initialDelay = 1000 * 60 * 20
    private void loadPlacements() {
        List<PlacementFolder> folders = placementFolderRepository.findAll();
        List<PlacementDetail> filesFound = new ArrayList<>();

        for (PlacementFolder pf : placementFolderRepository.findAll()) {
            System.out.println("Folder : " + pf.getFolderLink());
            List<Placement> arrPlacement = new ArrayList<Placement>();
            File folder = new File(pf.getFolderLink());
            File[] listOfFiles = folder.listFiles();
//			Arrays.sort(listOfFiles, Comparator.comparingLong(File::lastModified).reversed());
            for (int j = 0; j < listOfFiles.length; j++) {
                if(!listOfFiles[j].getName().startsWith("1NCN") && !listOfFiles[j].getName().startsWith("__1NCN")) {
                    continue;
                }
                List<PlacementDetail> arrPlacementDetail = new ArrayList<PlacementDetail>();
                System.out.println("File " + listOfFiles[j].getName() + " : " + j + " / " + listOfFiles.length);
                Placement placement = new Placement();
                if (listOfFiles[j].isFile()) {
                    long lastModifiedTimestamp = listOfFiles[j].lastModified();
                    // convert lastModifiedTimestamp ot LocalDateTime
                    LocalDateTime lastModified = Instant.ofEpochMilli(lastModifiedTimestamp).atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
//                    if(lastModified.isBefore(LocalDateTime.of(2024, 11, 25, 3, 13, 11))) {
//                        continue;
//                    }

//                    Placement oldplacement = placementService.findByfilter(listOfFiles[j].getName(),
//                            pf.getFolderLink());
//
//                    if (oldplacement != null && oldplacement.getLastModified() != null
//                            && oldplacement.getLastModified() == lastModifiedTimestamp) {
//                        continue;
//                    }

                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(
                                new InputStreamReader(new FileInputStream(listOfFiles[j]), "windows-1252"));
                        if (br != null) {

                            placement.setPlacement(listOfFiles[j].getName());
//                            placement.setFolder(pf.getFolderLink());
//                            placement.setLastModified(lastModifiedTimestamp);
                            String[] liste = br.lines().collect(Collectors.toList())
                                    .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
//
//                            if (liste[0].contains("/LO") && liste[0].contains("/l")) {
//                                placement.setLongueur(convertTwoDigit(Double.parseDouble(
//                                        liste[0].subSequence(liste[0].indexOf("/LO") + 4, liste[0].indexOf("/l"))
//                                                .toString().replace("CM", ""))
//                                        * 0.01));
//                                placement.setLargeur(convertTwoDigit(Double.parseDouble(
//                                        liste[0].subSequence(liste[0].indexOf("/l") + 3, liste[0].indexOf("*N1"))
//                                                .toString().replace("CM", ""))
//                                        * 0.01));
//                            } else if (liste[0].contains("/L") && liste[0].contains("/W")) {
//                                placement.setLongueur(convertTwoDigit(Double.parseDouble(
//                                        liste[0].subSequence(liste[0].indexOf("/L") + 3, liste[0].indexOf("/W"))
//                                                .toString().replace("CM", ""))
//                                        * 0.01));
//                                placement.setLargeur(convertTwoDigit(Double.parseDouble(
//                                        liste[0].subSequence(liste[0].indexOf("/W") + 3, liste[0].indexOf("*N1"))
//                                                .toString().replace("CM", ""))
//                                        * 0.01));
//                            }

                            String digit = null;
                            Integer empInd = null;
                            String partNumberMaterial = null;
                            String description = null, categoriePiece = null, taille = null, idPaquet = null,
                                    nomMedele = null;
                            int nbrPieces = 0;
                            for (int i = 1; i < liste.length; i++) {
                                if (partNumberMaterial == null && liste[i].startsWith("M,NUMERO")) {
                                    partNumberMaterial = liste[i].split(",")[2];
                                    placement.setPartNumberMaterial(partNumberMaterial);
                                }
                                if (liste[i].startsWith("M,Efficience plct")) {
                                    placement.setEfficience(Double.parseDouble(liste[i].split(",")[2]));
                                }

                                if (liste[i].startsWith("L,")) {
                                    empInd = Integer.parseInt(liste[i].split(",")[1]);
                                    digit = null;
                                } else if (liste[i].startsWith("D,1,")) {
                                    digit = liste[i].split(",")[2];
                                } else if (liste[i].startsWith("D,2,")) {
                                    description = liste[i].split(",")[2];
                                } else if (liste[i].startsWith("D,3,")) {
                                    categoriePiece = liste[i].split(",")[2];
                                } else if (liste[i].startsWith("D,4,")) {
                                    taille = liste[i].split(",")[2];
                                } else if (liste[i].startsWith("D,5,")) {
                                    idPaquet = liste[i].split(",")[2];
                                } else if (liste[i].startsWith("D,6,")) {
                                    nomMedele = liste[i].split(",")[2];
                                } else if (liste[i].startsWith("D,7,")) {
                                    if (digit != null) {
                                        PlacementDetail placementDetail = new PlacementDetail();
                                        placementDetail.setPlacement(placement.getPlacement());
//                                        placementDetail.setFolder(placement.getFolder());
//                                        placementDetail.setInd(empInd);
//                                        placementDetail.setIdPaquet(idPaquet);
                                        placementDetail.setNomMedele(nomMedele);
//                                        placementDetail.setGaucheDroite(liste[i].split(",")[2]);
                                        digit.replace("-LSR", "");
                                        placementDetail.setPattern(digit);
                                        placementDetail.setUpdatedAt(lastModified);
                                        nbrPieces++;
//                                        arrPlacementDetail.add(placementDetail);
//                                        try {
//                                            placementDetailService.save(placementDetail);
//                                        } catch (Exception e) {
//                                            System.out.println("placementDetailService : " + e.getMessage());
//                                        }
                                        if(digit.trim().toUpperCase().equalsIgnoreCase("HCB-CAV-P1011-12A")) {
                                            filesFound.add(placementDetail);
                                        }
                                        digit = null;
                                    }
                                }
                            }
                            placement.setNbrPieces(nbrPieces);
//                            placementService.save(placement);
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            System.out.println("DONE Folder : " + pf.getFolderLink() + " : " + arrPlacement.size());
        }
        for(PlacementDetail pd : filesFound) {
            System.out.println(pd.getPlacement() + "      "+ pd.getNomMedele() + "     "+ pd.getUpdatedAt() );
        }

    }


    //	@Scheduled(fixedRate = 1000 * 60 * 60)
    public void verifyPlanCoupeEnable() {
        List<PlanCoupe> arr = planCoupeService.findAllLight();
        int i = 0, percentage = 0;
        User user = userService.findByUsername("aennaji");
        for (PlanCoupe pc : arr) {
            i++;
            while (percentage < (int) (100 * ((float) i / arr.size()))) {
                percentage++;
                System.out.println("verifyPlanCoupeEnable : " + i + "  " + percentage + "/100");
            }
            CuttingPlanData obj = cuttingPlanDataService.findFirstByCmsId(pc.getIdPlanCoupe());
            // check if the two boolean are different, obj.getEnabled() != pc.getStatusPlanCoupe()
            if (obj == null) {
                System.out.println("NOT FOUND : " + pc.getIdPlanCoupe() + "  " + "null");
                continue;
            }
            if (obj.getEnabled().equals(pc.getStatusPlanCoupe())) {
                continue;
            }
            System.out.println("Save PlanCoupeEnable : " + pc.getIdPlanCoupe() + "  " + obj.getEnabled() + " = " + pc.getStatusPlanCoupe());

            obj.setEnabled(pc.getStatusPlanCoupe());
            if (pc.getStatusPlanCoupe()) {
                obj.setEnabledAt(LocalDateTime.now());
                obj.setEnabledBy(user);
            } else {
                obj.setDisabledAt(LocalDateTime.now());
                obj.setDisabledBy(user);
            }

            CuttingPlanData newObj = cuttingPlanDataService.save(obj, user);
            try {
                CuttingPlanHistory cph = new CuttingPlanHistory();
                cph.setCuttingPlan(obj.getId());
                cph.setCreatedAt(LocalDateTime.now());
                cph.setUpdatedBy(user);
                if (pc.getStatusPlanCoupe()) {
                    cph.setChanges("Enabled");
                } else {
                    cph.setChanges("Disabled");
                }
                cuttingPlanHistoryRepository.save(cph);
            } catch (Exception e) {
                System.out.println("CuttingPlan History ERROR : " + e.getMessage());
            }
        }
    }


    //	@Scheduled(fixedRate = 1000 * 60 * 60)
    public void verifyingQty() {
        List<EmpStat> arr = placementDetailService.findSats();
        List<String> arrError = new ArrayList<String>();
        int i = 0, percentage = 0;
        for (EmpStat obj : arr) {
            i++;

            while (percentage < (int) (1000 * ((float) i / arr.size()))) {
                percentage++;
                System.out.println("Loading Qty CTC : " + i + "  " + percentage + "/1000");
            }
            Files file = filesService.findFirstByPartNumberCoverAndPattern(obj.getNomModele(), obj.getPattern());
            if (file == null) {
                arrError.add("NO FOUND CTC : " + obj.getPlacement() + " => " + obj.getPattern() + ":"
                        + obj.getNomModele() + ":" + obj.getTotal() + " n'est pas trouvé");
                continue;
            }
            if (file.getQuantity() == null) {
                file.setQuantity(Integer.parseInt(obj.getTotal() + ""));
                filesService.save(file);
//				System.out.println("saving : " +obj.getPattern() + ":"+ obj.getNomModele() + ":"+ obj.getTotal());
            } else if (file.getQuantity() != Integer.parseInt(obj.getTotal() + "")) {
                arrError.add("BAD CTC : " + obj.getPlacement() + " => " + file.getPattern() + ":"
                        + file.getPartNumberCover() + ":" + file.getQuantity() + " n'est pas le même que "
                        + obj.getPattern() + ":" + obj.getNomModele() + ":" + obj.getTotal());
                // fix it
            } else {
//				System.out.println("Good "+ obj.getPattern() + ":"+ obj.getNomModele() + ":"+ obj.getTotal() );
            }

        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\cmsFolder\\errors.txt"));
            for (String error : arrError) {
                writer.write(error);
                writer.newLine();
            }
            writer.close();
            System.out.println("File saved successfully!");
        } catch (IOException e) {
            System.out.println("An error occurred while saving the file: " + e.getMessage());
        }
        System.out.println("arrError : " + arrError.size());
    }

    //	@Scheduled(fixedRate = 1000 * 60 * 60)
    private void loadECN() {
        List<String> arr = filesService.findPartNumbersList();
        int i = 0, percentage = 0;
        for (String pn : arr) {
            i++;
            while (percentage < (int) (100 * ((float) i / arr.size()))) {
                percentage++;
                System.out.println("Loading ECN : " + i + "  " + percentage + "/100");
            }
            GammeTechnique obj = gammeTechniqueRepository.findFirstByPartNumber(pn);
            if (obj != null && obj.getEcn() != null && !obj.getEcn().trim().isEmpty()) {
//				System.out.println(pn + " : "+ obj.getEcn());
                filesService.updateEcn(pn, obj.getEcn());
            }
        }
    }

    //	@Scheduled(fixedRate = 1000 * 60 * 60)
    private void loadProjet() {
        List<String> arr = filesService.findPartNumbersList();
        int i = 0, percentage = 0;
        for (String pn : arr) {
            i++;
            while (percentage < (int) (100 * ((float) i / arr.size()))) {
                percentage++;
                System.out.println("Loading Projet : " + i + "  " + percentage + "/100");
            }
            PartNumberInfo2 obj = partNumberInfo2Service.findByPartNumber(pn);
            if (obj != null && obj.getPartNumber() != null && !obj.getPartNumber().trim().isEmpty()) {
                filesService.updateProjet(pn, obj.getItemGroup());
                partNumberBoomService.updateProjet(pn, obj.getItemGroup());
            }
        }
    }
    @Value("${lear.linkServer}")
    private String linkServer;


    //	@Scheduled(fixedRate = 1000 * 60 * 60 * 2)
    public void verifyCTCWithPlananing() {

        List<Planning> arr = planningRepository.findNotFoundCtc();
//		System.out.println("verifyCTCWithPlananing : " + arr.size());

        List<String> emailArr = new ArrayList<String>();
        emailArr.add("melghazi@lear.com");
        String content = "";
        if (arr.size() > 0) {
            content += "<br/>" + returnTables(arr);
        }
        if (content.length() > 0) {
            try {
                emailService.sendEmailAttachment(emailArr, "CTC Not Found Alert", "<html>" + "<head>" + "<style>\r\n"
                        + "h2{text-align: center;}\r\n"
                        + "table {font-family: arial, sans-serif;border-collapse: collapse;margin: 0 auto}\r\n"
                        + "td, th {border: 1px solid #dddddd;text-align: left;padding: 4px;font-size: 12}\r\n" + "</style>"
                        + "</head>" + "<body><h2>CTC Not Found Alert</h2>" + content
                        + "<br/><p>Cliquer <a href='"+linkServer+"'>ici</a> pour ouvrir l'application</p>"
                        + "</body></html>");
            } catch (Exception exp) {
                System.out.println("Email Notification 2 Error");
            }
        }
    }

    String returnTables(List<Planning> arr) {
        String message = "<table style=\"border: 1px solid black;border-collapse: collapse;\">" + "<tr>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Part Number</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Description</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Item</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Group</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Design Group</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Cover Group</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Status</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Commentaire</th>"
                + "<th style=\"border: 1px solid black;border-collapse: collapse;\">Quantité</th>" + "</tr>";

        for (Planning obj : arr) {
            message += "<tr>" + "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                    + obj.getPartNumber() + "</td>"
                    + "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getDescription()
                    + "</td>" + "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getItem()
                    + "</td>" + "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getGroupName()
                    + "</td>" + "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                    + obj.getDesignGroup() + "</td>"
                    + "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getCoverGroup()
                    + "</td>" + "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getStatus()
                    + "</td>" + "<td style=\"border: 1px solid black;border-collapse: collapse;\">"
                    + obj.getCommentaire() + "</td>"
                    + "<td style=\"border: 1px solid black;border-collapse: collapse;\">" + obj.getQuantity() + "</td>"
                    + "</tr>";
        }

        message += "</table>";
//		System.out.println("message : " + message);
        return message;
    }

    @Autowired
    private TimingModelRepository timingModelRepository;

//        @Scheduled(fixedRate = 1000 * 60 * 60 * 60)
    public void loadTimingModel() {
        List<PlanCoupe> planWithNoTimingModel = planCoupeService.findPlanWithNoTimingModel();
        System.out.println("planWithNoTimingModel : " + planWithNoTimingModel.size());
        int i = 0, percentage = 0;
        for (PlanCoupe pc : planWithNoTimingModel) {
            i++;
            while (percentage < (int) (100 * ((float) i / planWithNoTimingModel.size()))) {
                percentage++;
                System.out.println("Loading Timing Model : " + i + "  " + percentage + "/100");
            }
            List<CuttingPlan> objArr = cuttingPlanService.findByCmsId(pc.getIdPlanCoupe());
            CuttingPlan obj = null;
            if (objArr.size() > 0) {
                obj = objArr.get(0);
            }
            if (obj == null) {
                System.out.println("Error Plan Coupe : " + pc.getIdPlanCoupe());
                continue;
            }
            for (CuttingPlanMaterial cpm : obj.getCuttingPlanMaterials()) {

                for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {

                    try {
                        List<SpreadingCuttingPlanCoupe> scpcArr = spreadingCuttingPlanCoupeRepository.findByIdSpreadingPlanForeignPlanCoupeAndPlacementPlanCoupe(pc.getIdPlanCoupe(), cpmp.getPlacement());
                        if (scpcArr.size() == 0) {
                            continue;
                        }
                        int maxQty = 0;
                        if (cpmp.getPartNumbers() != null) {
                            String partNumbers = cpmp.getPartNumbers(); // like L002483470NCPAC:8, L002483474NCPAC:11 and we need to get the max like 11 here
                            String[] arrPartNumbers = partNumbers.split(", ");

                            for (String partNumber : arrPartNumbers) {
                                String[] arrPartNumber = partNumber.split(":");
                                maxQty = Math.max(maxQty, Integer.parseInt(arrPartNumber[1]));
                            }
                        }
                        SpreadingCuttingPlanCoupe scpc = scpcArr.get(0);
                        TimingModel tm = new TimingModel();
                        tm.setIdPlanCoupeTimingModel(pc.getIdPlanCoupe());
                        tm.setIdSpreadingTimingModel(scpc.getIdSpreadingCuttingPlanCoupe());
                        tm.setItemNumberTimingModel(cpm.getPartNumberMaterial());
                        tm.setDescriptionItemNumberTimingModel(cpm.getDescription());
                        tm.setPlacementTimingModel(cpmp.getPlacement());
                        tm.setTypeItemTimingModel("");
                        if (cpmp.getPerimetre() != null && cpmp.getPerimetre() > 0) {
                            tm.setPerimeterTimingModel(cpmp.getPerimetre());
                        } else {
                            tm.setPerimeterTimingModel(scpc.getPerimiterPlanCoupe());
                        }
                        tm.setQtyPlanTimingModel(pc.getQuantityPlanCoupe());


                        tm.setQtyPerLayerTimingModel(scpc.getQuantityPerLayerPlanCoupe());
                        tm.setMaxPlieTimingModel(scpc.getMaxPliePlanCoupe());
                        tm.setMachineTimingModel(scpc.getMachinePlanCoupe());
                        tm.setLayersTimingModel(cpmp.getNbrCouche());
                        tm.setLongueurMatelasTimingModel(cpmp.getLongueurMatelas() / cpmp.getNbrCouche());
                        tm.setLongueurPlacementTimingModel(cpmp.getLongueur());
                        tm.setSeuilLongueurTimingModel(UtilFunctions.convertTwoDigit(tm.getLongueurMatelasTimingModel() - tm.getLongueurPlacementTimingModel(), 3));
                        if (cpmp.getTempsDeCoupe() == null || cpmp.getTempsDeCoupe() == 0) {
                            cpmp.setTempsDeCoupe(2.8);
                        } else {
                            tm.setSpeedMMinTimingModel(UtilFunctions.convertTwoDigit(cpmp.getPerimetre() * 0.01 / cpmp.getTempsDeCoupe(), 4));
                        }
                        if(tm.getSpeedMMinTimingModel() == null) {
                            tm.setSpeedMMinTimingModel(2.8);
                        }
                        tm.setDrillingMiscTimingModel(9.0);
                        tm.setPrepTimeMinTimingModel(6.2);
                        tm.setCuttingTimeStopperPerlayerTimingModel(0.3);
                        tm.setSpreadTimePerLayerMMinTimingModel(0.1);
//                    tm.setSpreadingTimingModel(tm.getPrepTimeMinTimingModel() + (tm.getCuttingTimeStopperPerlayerTimingModel() * tm.getLayersTimingModel()) + (tm.getSpreadTimePerLayerMMinTimingModel() * tm.getLayersTimingModel() * tm.getLongueurMatelasTimingModel()));
//                    tm.setCuttingTimeTimingModel(cpmp.getTempsDeCoupe());
                        timingModelRepository.save(tm);
                    } catch (Exception e) {
                        System.out.println("Error Plan Coupe : " + pc.getIdPlanCoupe() + " " + cpmp.getPlacement() + " Error timing model : " + e.getMessage());
                    }
                }
            }
        }
    }

}
