package com.lear.MGCMS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.repositories.*;
import com.lear.MGCMS.services.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumber;
import com.lear.MGCMS.payload.WorkOrderElem;
import com.lear.MGCMS.services.cms.ItemPlanCoupeService;
import com.lear.MGCMS.services.cms.PlanCoupeService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.cms.domain.CategoryLaizePlanCoupe;
import com.lear.cms.domain.IntervalItemMachinePlanCoupe;
import com.lear.cms.domain.IntervalSeuilPlanCoupe;
import com.lear.cms.domain.ItemMachinePlanCoupe;
import com.lear.cms.domain.ItemPlanCoupe;
import com.lear.cms.domain.PartNumberPlanCoupe;
import com.lear.cms.domain.PlanCoupe;
import com.lear.cms.domain.SeuilLongueurPlanCoupe;
import com.lear.cms.domain.SpreadingCuttingPlanCoupe;
import com.lear.cms.repositories.PlanCoupeRepository;


// @Component
public class ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTask.class);

    @Autowired
    private PlanningRepository planningRepository;

    @Autowired
    private PartNumberInfoRepository partNumberInfoRepository;

    @Autowired
    private PartNumberInfo2Repository partNumberInfo2Repository;

    @Autowired
    private ItemPlanCoupeService itemPlanCoupeService;
    @Autowired
    private PartNumberMaterialConfigService partNumberMaterialConfigService;
    @Autowired
    private PartNumberBoomLightRepository partNumberBoomLightRepository;
    @Autowired
    private PartNumberBoomRepository partNumberBoomRepository;

    @Autowired
    private PlanCoupeService planCoupeService;

    @Autowired
    private ProjetService projetService;

    @Autowired
    private ProjetVersionService projetVersionService;
    @Autowired
    private ReftissuMachineRepository reftissuMachineRepository;
    @Autowired
    private ReftissuCategoryRepository reftissuCategoryRepository;
    @Autowired
    private ReftissuMarginRepository reftissuMarginRepository;
    @Autowired
    private CapaciteInstalleeService capaciteInstalleeService;

    @Autowired
    private PartNumberMaterialConfigDataService partNumberMaterialConfigDataService;

    @Scheduled(
            fixedRate = 1000 * 60 * 60 * 6
//            , initialDelay = 1000 * 60 * 5
    )
    public void ensureCapaciteInstallee() {
        try {
            capaciteInstalleeService.ensureNextTwoDays();
            System.out.println("ensureCapaciteInstallee: OK");
        } catch (Exception e) {
            System.out.println("ensureCapaciteInstallee ERROR: " + e.getMessage());
        }
    }

//    @Scheduled(fixedRate = 1000 * 60 * 60 * 6, initialDelay = 1000 * 60 * 30)
    public void loadItemConfig() {
        List<ItemPlanCoupe> arr = itemPlanCoupeService.findAll();
        System.out.println("loadItemConfig Size : " + arr.size());
        int percentage = 0, i = 0;
        for (ItemPlanCoupe objItem : arr) {

            i++;
            while(percentage< (int)(100*((float)i/arr.size()))) {
                percentage++;
                System.out.println("Loading ItemConfig : "+ percentage+"/100 : " );
            }
            System.out.println(objItem.getItemNumberPlan());
            try {
                reftissuMachineRepository.deleteByPartnumber(objItem.getItemNumberPlan());
                reftissuCategoryRepository.deleteByPartnumber(objItem.getItemNumberPlan());
                reftissuMarginRepository.deleteByPartnumber(objItem.getItemNumberPlan());
            } catch (Exception e) {
                System.out.println("loadItemConfig deleteByPartnumber "+objItem.getItemNumberPlan()+" : " + e.getMessage());
            }
            PartNumberMaterialConfig obj = new PartNumberMaterialConfig();
            obj.setPartNumberMaterial(objItem.getItemNumberPlan());
            if(objItem.getDescriptionPlan() != null) {
                obj.setDescription(objItem.getDescriptionPlan().replaceAll("[\\n\\t]", " "));
            }
            obj.setVitesse(300);
            obj.setRotation(objItem.getRotationPlan());
            if (objItem.getPlaquePlan() != null && !objItem.getPlaquePlan().trim().isEmpty()) {
                try {
                    obj.setPlaque(Double.parseDouble(objItem.getPlaquePlan()));
                } catch (Exception e) {
                    System.out.println("Plaque " + objItem.getItemNumberPlan() + " : " + e.getMessage());
                }
            }
            obj.setTauxScrap(3.0);
            obj.setCommentaire(objItem.getCommentPlan());
            obj.setPartNumberMaterial(objItem.getItemNumberPlan());
            obj.setCreatedAt(LocalDateTime.now());
            List<ReftissuCategory> reftissuCategoryArr = new ArrayList<ReftissuCategory>();
            List<CategoryLaizePlanCoupe> arrCetegory = itemPlanCoupeService.findCategories(objItem.getIdItemPlan());
            for (CategoryLaizePlanCoupe category : arrCetegory) {
                ReftissuCategory reftissuCategory = new ReftissuCategory();
                reftissuCategory.setCategory(category.getCategoryNamePlan());
                reftissuCategory.setBorneMin(category.getBorneMinCategoryPlan());
                reftissuCategory.setBorneMax(category.getBorneMaxCategoryPlan());
                reftissuCategory.setDefaultValue(category.getDefaultCategoryPlan());
                reftissuCategory.setDescription(category.getDescriptionCategoryPlan());
                reftissuCategory.setPartNumberMaterialConfig(obj);
                reftissuCategoryArr.add(reftissuCategory);
            }
            obj.setReftissuCategories(reftissuCategoryArr);

            List<ReftissuMargin> reftissuMarginArr = new ArrayList<ReftissuMargin>();
            List<SeuilLongueurPlanCoupe> arrSeuil = itemPlanCoupeService.findByIdItemForeign1Plan(objItem.getIdItemPlan());
            int intervalId = 1;
            for (SeuilLongueurPlanCoupe seuil : arrSeuil) {
                ReftissuMargin reftissuMargin = new ReftissuMargin();
                reftissuMargin.setIntervalId(intervalId);
                reftissuMargin.setLongueurMin(seuil.getSeuilMinPlan());
                reftissuMargin.setLongueurMax(seuil.getSeuilMaxPlan());
                List<String> pliesConfigs = new ArrayList<String>();
                List<IntervalSeuilPlanCoupe> intervalSeuilPlanCoupes = itemPlanCoupeService.findByIdSeuilForeignPlan(seuil.getIdSeuil_Plan());
                for (IntervalSeuilPlanCoupe intervalSeuil : intervalSeuilPlanCoupes) {
                    pliesConfigs.add(intervalSeuil.getMinPlieSeuilPlan().intValue() + ";" + intervalSeuil.getLongueurPlusSeuilPlan());
                }
                reftissuMargin.setPliesConfig(String.join("|", pliesConfigs));
                reftissuMargin.setPartNumberMaterialConfig(obj);
                reftissuMarginArr.add(reftissuMargin);
                intervalId++;
            }
            obj.setReftissuMargins(reftissuMarginArr);

            List<ReftissuMachine> reftissuMachineArr = new ArrayList<ReftissuMachine>();
            List<ItemMachinePlanCoupe> arrMachine = itemPlanCoupeService.findByIdItemForeignPlan(objItem.getIdItemPlan());
            for (ItemMachinePlanCoupe machine : arrMachine) {
                ReftissuMachine reftissuMachine = new ReftissuMachine();
                reftissuMachine.setMaxPlie(machine.getMaxPlieTotalPlan());
                reftissuMachine.setMaxPlieDrill(machine.getMaxPlieDrillPlan());
                reftissuMachine.setMaxDrill(machine.getSeuilDrillPlan().intValue());
                switch (machine.getIdMachineForeignPlan()) {
                    case 1:
                        reftissuMachine.setMachineType("Lectra");
                        break;
                    case 2:
                        reftissuMachine.setMachineType("Gerber");
                        break;
                    case 3:
                        reftissuMachine.setMachineType("DIE");
                        break;
                    case 4:
                        reftissuMachine.setMachineType("LASER-LSR");
                        break;
                    case 5:
                        reftissuMachine.setMachineType("Lectra IP6");
                        break;
                    case 6:
                        reftissuMachine.setMachineType("LASER-DXF");
                        break;
                }
                reftissuMachine.setDefaultValue(machine.getDefaultItemMachinePlan());
                List<String> pliesConfigs = new ArrayList<String>();
                List<IntervalItemMachinePlanCoupe> intervalItemMachines = itemPlanCoupeService.findByIdItemMachineForeignPlan(machine.getIdItemMachinePlan());
                for (IntervalItemMachinePlanCoupe intervalItemMachine : intervalItemMachines) {
                    pliesConfigs.add(intervalItemMachine.getMinPliePlan().intValue() + ";" + intervalItemMachine.getConfigurationPlan());
                    obj.setMatelassageEndroit(intervalItemMachine.getMatelassageEndroitPlan());
                }
                reftissuMachine.setPliesConfig(String.join("|", pliesConfigs));
                reftissuMachine.setPartNumberMaterialConfig(obj);
                reftissuMachineArr.add(reftissuMachine);
            }
            obj.setReftissuMachines(reftissuMachineArr);
            PartNumberMaterialConfigData oldObj = partNumberMaterialConfigDataService.findById(obj.getPartNumberMaterial());
            obj.setMargeLaizeMin(oldObj.getMargeLaizeMin());
            obj.setMargeLaizeMax(oldObj.getMargeLaizeMax());
            obj.setValidated0BF(oldObj.getValidated0BF());
            obj.setBuffer1IP6(oldObj.getBuffer1IP6());
            obj.setBuffer2IP6(oldObj.getBuffer2IP6());
            obj.setWeightUnit(oldObj.getWeightUnit());
            partNumberMaterialConfigService.save(obj, null);

        }
        System.out.println("loadItemConfig DONE.");
    }

//    @Scheduled(fixedRate = 1000 * 60 * 60 * 24, initialDelay = 1000 * 60 * 60 * 3)
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

//    @Scheduled(fixedRate = 1000 * 60 * 60 * 24, initialDelay = 1000 * 60 * 60 * 4)
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



    //************************************************************************************************************************

    //@Scheduled(fixedRate = 1000 * 60 * 10)
    public void copyConfigTissu() throws IOException {
        List<ItemPlanCoupe> arr = itemPlanCoupeService.findAll();
        try {
            // Load the Excel file
//            FileInputStream file = new FileInputStream("\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\TCAD\\FORMULAIRE\\BD_TEXTILE FOAM & 3D-test.xlsx");
            FileInputStream file = new FileInputStream("C:\\Users\\melghazi\\Desktop\\config.xlsx");

            Workbook workbook = WorkbookFactory.create(file);

            // Get the desired sheet
            Sheet sheet = workbook.getSheet("PARAMETRE"); // Replace "Sheet1" with your sheet name            
            int i = 1;
            for (ItemPlanCoupe objItem : arr) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(objItem.getItemNumberPlan());
                row.createCell(1).setCellValue(objItem.getDescriptionPlan());
                row.createCell(18).setCellValue(objItem.getVitesseCoupePlan());
                row.createCell(20).setCellValue(objItem.getRotationPlan());
                row.createCell(21).setCellValue(objItem.getPlaquePlan());
                row.createCell(22).setCellValue("TEAM CAD");
                row.createCell(23).setCellValue(objItem.getCommentPlan());
                List<CategoryLaizePlanCoupe> arrCetegory = itemPlanCoupeService.findCategories(objItem.getIdItemPlan());
                for (CategoryLaizePlanCoupe category : arrCetegory) {
                    if (category.getDefaultCategoryPlan()) {
                        row.createCell(2).setCellValue(category.getBorneMinCategoryPlan());
                        break;
                    }
                }

                List<SeuilLongueurPlanCoupe> arrSeuil = itemPlanCoupeService.findByIdItemForeign1Plan(objItem.getIdItemPlan());

                if (arrSeuil.size() == 1) {
                    List<IntervalSeuilPlanCoupe> intervalSeuilPlanCoupes = itemPlanCoupeService.findByIdSeuilForeignPlan(arrSeuil.get(0).getIdSeuil_Plan());

                    row.createCell(15).setCellValue(arrSeuil.get(0).getSeuilMaxPlan());
                    if (intervalSeuilPlanCoupes.size() > 0) {
                        row.createCell(16).setCellValue(intervalSeuilPlanCoupes.get(0).getLongueurPlusSeuilPlan());
                    }
                }
                if (arrSeuil.size() >= 2) {
                    List<IntervalSeuilPlanCoupe> intervalSeuilPlanCoupes = itemPlanCoupeService.findByIdSeuilForeignPlan(arrSeuil.get(0).getIdSeuil_Plan());
                    List<IntervalSeuilPlanCoupe> intervalSeuilPlanCoupes2 = itemPlanCoupeService.findByIdSeuilForeignPlan(arrSeuil.get(1).getIdSeuil_Plan());

                    row.createCell(15).setCellValue(arrSeuil.get(1).getSeuilMinPlan());
                    if (intervalSeuilPlanCoupes.size() > 0) {
                        row.createCell(16).setCellValue(intervalSeuilPlanCoupes.get(0).getLongueurPlusSeuilPlan());
                    }
                    if (intervalSeuilPlanCoupes2.size() > 0) {
                        row.createCell(17).setCellValue(intervalSeuilPlanCoupes2.get(0).getLongueurPlusSeuilPlan());
                    }
                }


                List<ItemMachinePlanCoupe> arrMachine = itemPlanCoupeService.findByIdItemForeignPlan(objItem.getIdItemPlan());
                String matellasage = "";
                for (ItemMachinePlanCoupe machine : arrMachine) {
                    List<IntervalItemMachinePlanCoupe> intervalItemMachines = itemPlanCoupeService.findByIdItemMachineForeignPlan(machine.getIdItemMachinePlan());


                    switch (machine.getIdMachineForeignPlan()) {
                        case 2://Gerber
                            row.createCell(3).setCellValue(machine.getMaxPlieTotalPlan());
                            if (intervalItemMachines.size() > 0) {
                                row.createCell(9).setCellValue(intervalItemMachines.get(0).getConfigurationPlan());
                                if (intervalItemMachines.get(0).getMatelassageEndroitPlan() != null && !intervalItemMachines.get(0).getMatelassageEndroitPlan().trim().isEmpty())
                                    matellasage = intervalItemMachines.get(0).getMatelassageEndroitPlan();
                            }
                            break;
                        case 1: //Lectra
                            row.createCell(4).setCellValue(machine.getMaxPlieTotalPlan());
                            if (intervalItemMachines.size() > 0) {
                                row.createCell(10).setCellValue(intervalItemMachines.get(0).getConfigurationPlan());
                                if (intervalItemMachines.get(0).getMatelassageEndroitPlan() != null && !intervalItemMachines.get(0).getMatelassageEndroitPlan().trim().isEmpty())
                                    matellasage = intervalItemMachines.get(0).getMatelassageEndroitPlan();
                            }
                            break;
                        case 3: // DIE
                            row.createCell(5).setCellValue(machine.getMaxPlieTotalPlan());
                            if (intervalItemMachines.size() > 0) {
                                row.createCell(11).setCellValue(intervalItemMachines.get(0).getConfigurationPlan());
                                if (intervalItemMachines.get(0).getMatelassageEndroitPlan() != null && !intervalItemMachines.get(0).getMatelassageEndroitPlan().trim().isEmpty())
                                    matellasage = intervalItemMachines.get(0).getMatelassageEndroitPlan();
                            }
                            break;
                        case 4://LASER-LSR
                            row.createCell(6).setCellValue(machine.getMaxPlieTotalPlan());
                            if (intervalItemMachines.size() > 0) {
                                row.createCell(12).setCellValue(intervalItemMachines.get(0).getConfigurationPlan());
                                if (intervalItemMachines.get(0).getMatelassageEndroitPlan() != null && !intervalItemMachines.get(0).getMatelassageEndroitPlan().trim().isEmpty())
                                    matellasage = intervalItemMachines.get(0).getMatelassageEndroitPlan();
                            }
                            break;

                        case 5://Lectra IP6
                            row.createCell(7).setCellValue(machine.getMaxPlieTotalPlan());
                            if (intervalItemMachines.size() > 0) {
                                row.createCell(13).setCellValue(intervalItemMachines.get(0).getConfigurationPlan());
                                if (intervalItemMachines.get(0).getMatelassageEndroitPlan() != null && !intervalItemMachines.get(0).getMatelassageEndroitPlan().trim().isEmpty())
                                    matellasage = intervalItemMachines.get(0).getMatelassageEndroitPlan();
                            }
                            break;

                        case 6://LASER-DXF
                            row.createCell(8).setCellValue(machine.getMaxPlieTotalPlan());
                            if (intervalItemMachines.size() > 0) {
                                row.createCell(14).setCellValue(intervalItemMachines.get(0).getConfigurationPlan());
                                if (intervalItemMachines.get(0).getMatelassageEndroitPlan() != null && !intervalItemMachines.get(0).getMatelassageEndroitPlan().trim().isEmpty())
                                    matellasage = intervalItemMachines.get(0).getMatelassageEndroitPlan();
                            }
                            break;
                    }

                }
                row.createCell(19).setCellValue(matellasage);

                i++;
            }
//            System.out.println("excel config saving...");

            // Save the changes
            FileOutputStream outFile = new FileOutputStream("C:\\Users\\melghazi\\Desktop\\config.xlsx");
            workbook.write(outFile);
            outFile.close();
            System.out.println("excel config done");
            // Close the workbook
            workbook.close();

            System.out.println("Excel file updated successfully.");
        } catch (Exception e) {
            log.error("ScheduledTask Excel update failed", e);
        }
    }

    //	@Scheduled(fixedRate = 1000 * 60 * 60 * 8)
    public void copyPlanDeCoupe() {
        Long id = Long.parseLong("2272");
        PlanCoupe pc = planCoupeService.findById(id);
        CuttingPlan obj = new CuttingPlan();
        obj.setProjet(pc.getGroupPlanCoupe());
        obj.setVersion(pc.getVersionPlanCoupe());
        obj.setDefinition(pc.getDefinitionPlanCoupe());
        obj.setEnabled(pc.getStatusPlanCoupe());
        obj.setCommentaire(pc.getCommentplanCoupe());
        obj.setCmsId(id);
        List<CuttingPlanPartNumber> cppnArr = new ArrayList<CuttingPlanPartNumber>();
        for (PartNumberPlanCoupe pnpc : pc.getPartNumberPlanCoupes()) {
            CuttingPlanPartNumber cppn = new CuttingPlanPartNumber();
            cppn.setPartNumber(pnpc.getPartNumberPlanCoupe());
            cppn.setItem(pnpc.getKitTextilPlanCoupe());
            cppn.setDescription(pnpc.getDescriptionPartNumberPlanCoupe());
            cppn.setQuantity(pnpc.getQuantityPartNumberPlanCoupe());
            cppn.setCuttingPlan(obj);
//			System.out.println(pnpc.getPartNumberPlanCoupe() + " : " + pnpc.getQuantityPartNumberPlanCoupe());
        }

        List<SpreadingCuttingPlanCoupe> spreadingCuttingPlanCoupes = pc.getSpreadingCuttingPlanCoupes();

        // Define a custom comparator to sort by idSpreadingCuttingParentPlanCoupe and then by defaultSpreadingCuttingPlanCoupe
        Comparator<SpreadingCuttingPlanCoupe> comparator = Comparator
                .comparing(SpreadingCuttingPlanCoupe::getIdSpreadingCuttingParentPlanCoupe, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(SpreadingCuttingPlanCoupe::getDefaultSpreadingCuttingPlanCoupe, Comparator.reverseOrder());

        // Sort the list using the comparator
        Collections.sort(spreadingCuttingPlanCoupes, comparator);

        for (SpreadingCuttingPlanCoupe sc : spreadingCuttingPlanCoupes) {
//			System.out.println(sc.getItemNumberPlanCoupe() + " => " +sc.getIdSpreadingCuttingParentPlanCoupe() );
        }

    }

    //Good
//	@Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    private void loadPnInfo() {
        String server = "10.49.0.46";// txtServer1.getText();
        int port = 21;
        String user = "mfg";// txtUserId1.getText();
        String pass = "leartsi01";// txtPassword1.getText();
        String remoteFilePath = "/QADSPA_SEATS/home/ftpkpitnr/analysis.csv";
        String csvDelimiter = ";";

        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            List<String> pnList = new ArrayList<String>();

            String line;
            int i = 0;
            int percentage = 0;
            while ((line = br.readLine()) != null) {
                i++;
                if (i == 0) {
                    continue;
                }
                if (i < 130991) {
                    continue;
                }
                while (percentage < (int) (1000 * ((float) i / 132850))) {
                    percentage++;
                    System.out.println("Loading analysis.csv : " + i + "  " + percentage + "/1000 : " + pnList.size());
                }
                if(pnList.size() > 10000) {
                    pnList = new ArrayList<>();
                }
                try {
                    String[] fields = line.split(csvDelimiter);
                    if (fields[8].contains("TRIM2") || fields[8].contains("TRIM4") || fields[8].contains("TRIM3") || fields[8].contains("FIP") || !pnList.contains(fields[2])) {
                        for (int j = 0; j < fields.length; j++) {
                            fields[j] = fields[j].replaceAll("^\"|\"$", "");
//                            System.out.println(j + " : " + fields[j]);
                        }
                        PartNumberInfo2 pn = new PartNumberInfo2();
                        pn.setPartNumber(fields[2]);
                        pn.setDescription(fields[3] + " " + fields[4]);
                        pn.setStatus(fields[6]);
                        pn.setProdLine(fields[7]);
                        pn.setItemType(fields[8]);
                        pn.setDesignGroup(fields[9]);
                        pn.setItemGroup(fields[10]);
                        pn.setCovertype(fields[11]);
                        pn.setApd(fields[27]);
                        pnList.add(fields[2]);
//                        System.out.println(pn.toString());
                        partNumberInfo2Repository.save(pn);
                    }
                } catch (Exception e) {
            		System.out.println("loadPnInfo : " + e.getMessage());
                }

            }

            inputStream.close();
            br.close();
            ftpClient.disconnect();
        } catch (IOException e) {
//            System.out.println("loadPnInfo General : " + e.getMessage());
        }

    }

    //	@Scheduled(fixedRate = 1000 * 60 * 60 * 12)
    public void loadBomPn() {
        String reportName = "13-8-8";
        String server = "10.49.0.46";// txtServer1.getText();
        int port = 21;
        String user = "mfg";// txtUserId1.getText();
        String pass = "leartsi01";// txtPassword1.getText();
        String qadLink = "/qad/home/batchtnr/" + reportName + ".prn";
        FTPClient ftpClient = new FTPClient();
        String line = "", lastLine = "";
        int i = 0;
        int percentage = 0;
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(qadLink)));


            String charging = null;
            PartNumberBoom obj = new PartNumberBoom();
            PartNumberInfo2 pnInfo2 = new PartNumberInfo2();
            List<PartNumberBoom> arr = new ArrayList<PartNumberBoom>();

            while ((line = reader.readLine()) != null) {
                i++;
                while (percentage < (int) (1000 * ((float) i / 2095000))) {
                    percentage++;
//					System.out.println("Loading BOM : "+ i + "  " + percentage+"/1000");
                }
//				if(i < 700000) continue;

                if (line.contains("Quantity Per")
                        || line.contains("------------")
                        || line.contains("13.8.8 Item-Site Structure Report")
                        || line.contains("TANGIER-TRIM")
                        || line.contains("Site:")
                        || line.contains("End of Report")
                        || line.contains("Report Submitted By")
                        || line.contains("Parent Item")
                        || line.contains("Start:")
                        || line.contains("Levels")
                        || line.contains("New Page Each Parent")
                        || line.contains("Sort by Reference")
                        || line.contains("Batch ID")
                        || line.trim().isEmpty()) {
                    continue;
                }
                try {
                    if (line.startsWith("PARENT")) {
                        if (obj.getPartNumberMaterial() != null && obj.getPartNumber() != null) {
                            if (pnInfo2 != null) {
                                obj.setProject(pnInfo2.getItemGroup());
                                obj.setVersion(pnInfo2.getDesignGroup());
                            }
                            String reftissu = obj.getPartNumberMaterial();

                            partNumberBoomRepository.save(obj);
                            arr.removeIf(elem -> elem.getPartNumberMaterial().equals(reftissu));
                        }
                        obj = new PartNumberBoom();
                        charging = null;
                        obj.setPartNumber(line.substring(11, 29).trim().toUpperCase());
                        if (arr.size() > 0) partNumberBoomRepository.deleteAll(arr);
                        arr = partNumberBoomRepository.findByPartNumber(obj.getPartNumber());
                        obj.setDescription(line.substring(43, Math.min(67, line.length())).trim().toUpperCase());
                        pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
                    }
                    if (obj.getPartNumber() != null && !line.startsWith("PARENT")) {
                        if (lastLine.startsWith("PARENT") && line.length() > 43 && !line.startsWith("1")) {
                            obj.setDescription(obj.getDescription() + " " + line.substring(43, Math.min(67, line.length())).trim());
                        } else if (line.length() >= 83 &&
                                (line.substring(11, 29).trim().toUpperCase().startsWith("W") || line.substring(11, 29).trim().startsWith("3"))
                        ) {
                            obj.setItem(line.substring(11, 29).trim());
                            if (line.substring(0, 4).trim().equals("1")) {
                                charging = ".2";
                            } else if (line.substring(0, 4).trim().equals(".2")) {
                                charging = "..3";
                            } else if (line.substring(0, 4).trim().equals("..3")) {
                                charging = "...4";
                            }
                        } else if (charging != null && line.startsWith(charging) && line.substring(81, 84).trim().toUpperCase().trim().equalsIgnoreCase("MT") && line.length() > 84
                                && line.substring(84, Math.min(90, line.length())).trim().equals("5")) {//&& lastLine.length() > 126
                            if (obj.getPartNumberMaterial() != null && obj.getPartNumber() != null) {
                                if (pnInfo2 != null) {
                                    obj.setProject(pnInfo2.getItemGroup());
                                    obj.setVersion(pnInfo2.getDesignGroup());
                                }
                                String reftissu = obj.getPartNumberMaterial();
                                arr.removeIf(elem -> elem.getPartNumberMaterial().equals(reftissu));
                                partNumberBoomRepository.save(obj);
                            }
                            obj.setQuantityPer(Double.parseDouble(line.substring(68, 80).trim()));
                            obj.setPartNumberMaterial(line.substring(11, 29).trim().toUpperCase());
                            obj.setPartNumberMaterialDescription(line.substring(43, 67).trim().toUpperCase());
                        } else if (charging != null
                                && lastLine.startsWith(charging)
                                && line.length() > 43
                                && !line.startsWith("1") && !line.startsWith(".2") && !line.startsWith("..3") && !line.startsWith("...4")) {//&& lastLine.length() > 126
                            obj.setPartNumberMaterialDescription(obj.getPartNumberMaterialDescription() + " " + line.substring(43, Math.min(67, line.length())).trim().toUpperCase());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("loadBomPn BOM : " + e.getMessage());
                }
                lastLine = line;
            }
//			System.out.println("loadBomPn DONE.");
        } catch (IOException e) {
            System.out.println("loadBomPn BOM : " + e.getMessage());
        }

    }


    //	@Scheduled(fixedRate = 1000 * 60 * 60 * 2)
    public void loadingPlanning() throws IOException {
//		LocalDateTime currentDateTime = LocalDateTime.now().plusHours(-6);
        XSSFWorkbook workbook = null;
        try {
            List<Planning> arr = new ArrayList<Planning>();
            ZipSecureFile.setMinInflateRatio(0);
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : openning excel");

//			FileInputStream file = new FileInputStream("\\\\MATNR-APP02\\Planning\\Coupe\\Copy of Cutting planning 20-04.xlsx");
//			XSSFWorkbook workbook = new XSSFWorkbook(file);

            String directoryPath = "\\\\MATNR-APP02\\Planning\\Coupe";
            Path directory = Paths.get(directoryPath);
            File[] files = directory.toFile().listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().toLowerCase().endsWith(".xlsx") && !pathname.getName().toLowerCase().contains("~$");
                }
            });
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            File latestFile = files[0];
            String latestFilePath = latestFile.getAbsolutePath();
            System.out.println("latestFilePath : " + latestFilePath);
            workbook = new XSSFWorkbook(new FileInputStream(latestFilePath));


            XSSFSheet sheetTemps = workbook.getSheet("Temps");
            for (int i = 0; i < sheetTemps.getLastRowNum(); i++) {
                try {

                    XSSFRow rowTemp1 = sheetTemps.getRow(i);
                    if (rowTemp1 == null || rowTemp1.getCell(1) == null) continue;
                    String pn = ExcelHelper.cellString(workbook, rowTemp1.getCell(1));
                    if (pn == null || pn.trim().isEmpty()) {
                        continue;
                    }
                    PartNumberInfo obj = new PartNumberInfo();
                    obj.setPartNumber(pn);
//					obj.setDescription(ExcelHelper.cellString(workbook,rowTemp1.getCell(2)));
//					obj.setStatus(ExcelHelper.cellString(workbook,rowTemp1.getCell(0)));
                    obj.setPerimetre(ExcelHelper.getNumericValueFromCell(workbook, rowTemp1.getCell(121)));
                    obj.setPackageQty(Integer.parseInt(ExcelHelper.cellString(workbook, rowTemp1.getCell(143))));
                    partNumberInfoRepository.save(obj);
                } catch (Exception e) {
//					System.out.println("Partnumberinfo error : " + e.getMessage());
                }
            }
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : Get the first sheet");
            XSSFSheet sheet = workbook.getSheet("Planning");

            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : Searching for date");

            Integer col = 12;

            XSSFRow rowHeader1 = sheet.getRow(8);
            Integer rowHeaderLength = (int) rowHeader1.getLastCellNum();
            for (int j = rowHeaderLength - 1; j >= 0; j--) {
                XSSFCell cell = rowHeader1.getCell(j);
                CellType cellType = cell.getCellType();
                LocalDate date1 = null;
                switch (cellType) {
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            date1 = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        }
                        break;
                    case FORMULA:
                        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);
                        switch (cellValue.getCellType()) {
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    date1 = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                }
                                break;
                            default:
                        }
                        break;
                    default:
                }

                if (date1 != null) {
                    List<PlanningDetails> arrDB = planningRepository.findAllWaiting(date1, "1");

                    col = j;
//					System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
//							+ " : Iterate through each row in the sheet");

                    // shift 1
                    col = col + 1;
                    for (int i = 16; i <= sheet.getLastRowNum(); i++) {
                        try {
                            XSSFRow row = sheet.getRow(i);
                            if (row == null || row.getCell(col) == null) {
                                continue;
                            }
                            if (row.getCell(col).getCellType() == null
//									&& !row.getCell(col).getCellType().equals(CellType.NUMERIC)
                            ) {
                                continue;
                            }
                            if (row.getCell(7).getStringCellValue() == null || row.getCell(7).getStringCellValue().trim().isEmpty()) {
                                continue;
                            }
                            Planning obj = new Planning();
                            Double quantity = ExcelHelper.getNumericValueFromCell(workbook, row.getCell(col));
                            if (quantity != null) {
                                obj.setQuantity(quantity.intValue());
                            } else {
                                continue;
                            }

                            if (obj.getQuantity() == 0) {
                                continue;
                            }

                            Comment comment = row.getCell(col).getCellComment();
                            if (comment != null) {
                                RichTextString commentText = comment.getString();
                                String commentString = commentText.getString();
                                obj.setCommentaire(commentString);
                            }

                            obj.setShift("1");
                            obj.setPlanningDate(date1);
                            CellStyle style = row.getCell(col).getCellStyle();

                            if (style.getFillForegroundColorColor() instanceof XSSFColor) {
                                XSSFColor fillForegroundColor = (XSSFColor) style.getFillForegroundColorColor();
                                String color = fillForegroundColor.getARGBHex();
                                obj.setColor(color);
                            }
                            obj.setRowId(i);
                            obj.setPartNumber(ExcelHelper.cellString(workbook, row.getCell(7)));
                            obj.setDescription(ExcelHelper.cellString(workbook, row.getCell(8)));
                            obj.setItem(ExcelHelper.cellString(workbook, row.getCell(9)));
//							obj.setGroupName(ExcelHelper.cellString(workbook,row.getCell(7)));
//							obj.setDesignGroup(ExcelHelper.cellString(workbook,row.getCell(8)));
//							obj.setCoverGroup(ExcelHelper.cellString(workbook,row.getCell(9)));
//							obj.setStatus(ExcelHelper.cellString(workbook,row.getCell(10)));
//							System.out.println(date1.toString() + " - 1:"+obj.getRowId() + " " + obj.getPartNumber() + " " + obj.getItem() + " " + obj.getQuantity());
                            PartNumberInfo2 pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
                            if (pnInfo2 != null) {
                                obj.setGroupName(pnInfo2.getItemGroup());
                                obj.setDesignGroup(pnInfo2.getDesignGroup());
                                obj.setStatus(pnInfo2.getStatus());
                                obj.setCoverGroup(pnInfo2.getCovertype());
                            }
                            arr.add(obj);
                            planningRepository.save(obj);
                            arrDB.removeIf(elem -> elem.getPartNumber().equals(obj.getPartNumber()));
                        } catch (Exception e) {
//							System.out.println(i + " : " + e.getMessage());
                        }
                    }

//					System.out.println(date1.toString() + " - 1:"+"arrDB1 : " + arrDB.size());
                    if (arrDB.size() > 0) planningRepository.deleteAll(arrDB);
                    // shift 2
                    List<PlanningDetails> arrDB2 = planningRepository.findAllWaiting(date1, "2");
                    col = col + 3;
                    for (int i = 16; i <= sheet.getLastRowNum(); i++) {
                        try {
                            XSSFRow row = sheet.getRow(i);
                            if (row == null || row.getCell(col) == null) {
                                continue;
                            }
                            if (row.getCell(col).getCellType() == null
//									&& !row.getCell(col).getCellType().equals(CellType.NUMERIC)
                            ) {
                                continue;
                            }
                            if (row.getCell(7).getStringCellValue() == null || row.getCell(7).getStringCellValue().trim().isEmpty()) {
                                continue;
                            }
                            Planning obj = new Planning();
                            Double quantity = ExcelHelper.getNumericValueFromCell(workbook, row.getCell(col));
                            if (quantity != null) {
                                obj.setQuantity(quantity.intValue());
                            } else {
                                continue;
                            }

                            if (obj.getQuantity() == 0) {
                                continue;
                            }
                            Comment comment = row.getCell(col).getCellComment();
                            if (comment != null) {
                                RichTextString commentText = comment.getString();
                                String commentString = commentText.getString();
                                obj.setCommentaire(commentString);
                            }
                            obj.setShift("2");
                            obj.setPlanningDate(date1);
                            CellStyle style = row.getCell(col).getCellStyle();

                            if (style.getFillForegroundColorColor() instanceof XSSFColor) {
                                XSSFColor fillForegroundColor = (XSSFColor) style.getFillForegroundColorColor();
                                String color = fillForegroundColor.getARGBHex();
                                obj.setColor(color);
                            }
                            obj.setRowId(i);
                            obj.setPartNumber(ExcelHelper.cellString(workbook, row.getCell(7)));
                            obj.setDescription(ExcelHelper.cellString(workbook, row.getCell(8)));
                            obj.setItem(ExcelHelper.cellString(workbook, row.getCell(9)));
//							obj.setGroupName(ExcelHelper.cellString(workbook,row.getCell(7)));
//							obj.setDesignGroup(ExcelHelper.cellString(workbook,row.getCell(8)));
//							obj.setCoverGroup(ExcelHelper.cellString(workbook,row.getCell(9)));
//							obj.setStatus(ExcelHelper.cellString(workbook,row.getCell(10)));
//							System.out.println(date1.toString() + " - 2:"+obj.getRowId() + " " + obj.getPartNumber() + " " + obj.getItem() + " " + obj.getQuantity());
                            PartNumberInfo2 pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
                            if (pnInfo2 != null) {
                                obj.setGroupName(pnInfo2.getItemGroup());
                                obj.setDesignGroup(pnInfo2.getDesignGroup());
                                obj.setStatus(pnInfo2.getStatus());
                                obj.setCoverGroup(pnInfo2.getCovertype());
                            }
                            arr.add(obj);
                            planningRepository.save(obj);
                            arrDB2.removeIf(elem -> elem.getPartNumber().equals(obj.getPartNumber()));
                        } catch (Exception e) {
//							System.out.println(i + " : " + e.getMessage());
                        }
                    }
//					System.out.println(date1.toString() + " - 2:"+"arrDB2 : " + arrDB2.size());
                    if (arrDB2.size() > 0) planningRepository.deleteAll(arrDB2);

                    // shift 3
                    List<PlanningDetails> arrDB3 = planningRepository.findAllWaiting(date1, "3");
                    col = col + 3;
                    for (int i = 16; i <= sheet.getLastRowNum(); i++) {
                        try {
                            XSSFRow row = sheet.getRow(i);
                            if (row == null || row.getCell(col) == null) {
                                continue;
                            }
                            if (row.getCell(col).getCellType() == null
//									&& !row.getCell(col).getCellType().equals(CellType.NUMERIC)
                            ) {
                                continue;
                            }
                            if (row.getCell(7).getStringCellValue() == null || row.getCell(7).getStringCellValue().trim().isEmpty()) {
                                continue;
                            }
                            Planning obj = new Planning();
                            Double quantity = ExcelHelper.getNumericValueFromCell(workbook, row.getCell(col));
                            if (quantity != null) {
                                obj.setQuantity(quantity.intValue());
                            } else {
                                continue;
                            }

                            if (obj.getQuantity() == 0) {
                                continue;
                            }
                            Comment comment = row.getCell(col).getCellComment();
                            if (comment != null) {
                                RichTextString commentText = comment.getString();
                                String commentString = commentText.getString();
                                obj.setCommentaire(commentString);
                            }
                            obj.setShift("3");
                            obj.setPlanningDate(date1);
                            CellStyle style = row.getCell(col).getCellStyle();

                            if (style.getFillForegroundColorColor() instanceof XSSFColor) {
                                XSSFColor fillForegroundColor = (XSSFColor) style.getFillForegroundColorColor();
                                String color = fillForegroundColor.getARGBHex();
                                obj.setColor(color);
                            }
                            obj.setRowId(i);
                            obj.setPartNumber(ExcelHelper.cellString(workbook, row.getCell(7)));
                            obj.setDescription(ExcelHelper.cellString(workbook, row.getCell(8)));
                            obj.setItem(ExcelHelper.cellString(workbook, row.getCell(9)));
//							obj.setGroupName(ExcelHelper.cellString(workbook,row.getCell(7)));
//							obj.setDesignGroup(ExcelHelper.cellString(workbook,row.getCell(8)));
//							obj.setCoverGroup(ExcelHelper.cellString(workbook,row.getCell(9)));
//							obj.setStatus(ExcelHelper.cellString(workbook,row.getCell(10)));
//							System.out.println(date1.toString() + " - 3:"+obj.getRowId() + " " + obj.getPartNumber() + " " + obj.getItem() + " " + obj.getQuantity());
                            PartNumberInfo2 pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
                            if (pnInfo2 != null) {
                                obj.setGroupName(pnInfo2.getItemGroup());
                                obj.setDesignGroup(pnInfo2.getDesignGroup());
                                obj.setStatus(pnInfo2.getStatus());
                                obj.setCoverGroup(pnInfo2.getCovertype());
                            }
                            arr.add(obj);
                            planningRepository.save(obj);
                            arrDB3.removeIf(elem -> elem.getPartNumber().equals(obj.getPartNumber()));
                        } catch (Exception e) {
//							System.out.println(i + " : " + e.getMessage());
                        }
                    }
//					System.out.println(date1.toString() + " - 3:"+ "arrDB3 : " + arrDB3.size());
                    if (arrDB3.size() > 0) planningRepository.deleteAll(arrDB3);
                }

            }

            workbook.close();
        } catch (EncryptedDocumentException | IOException e) {
            if (workbook != null) {
                workbook.close();
            }
//			System.out.println("Error: "+e.getMessage());
        }

    }

//	@Scheduled(fixedRate = 1000 * 60 * 60)
//	public void fillingWO() throws IOException {
//		
//		List<WorkOrderElem> arr = new ArrayList<WorkOrderElem>();
//		Map<String, List<WorkOrderElem>> map = new HashMap<String, List<WorkOrderElem>>();
//		String server = "10.49.0.154";// txtServer1.getText();
//		int port = 21;
//		String user = "rguenda";// txtUserId1.getText();
//		String pass = "Tanger.2022";// txtPassword1.getText();
//		String qadLink = "/QADSPA_SEATS/home/ftpkpitnr/";
//		String[] rapports = { "16_3_2A", "16_3_2R", "16_3_2C"}; // 16_3_2AA.prn 16_3_2RR.prn
//		FTPClient ftpClient = new FTPClient();
//		try {
//			ftpClient.connect(server, port);
//			ftpClient.login(user, pass);
//			ftpClient.enterLocalPassiveMode();
//
//			for(String rapport : rapports) {
//				try {
//					
//				
//				BufferedReader reader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(qadLink+rapport+".prn")));
//				String line;
//				while ((line = reader.readLine()) != null) {
//					if(line.length() < 130 || line.contains("Qty Completed Qty Rejected     Qty Open") 
//							|| line.contains("------------") 
//							|| line.contains("Work Order") 
//							|| line.contains("TANGIER-TRIM") 
//							|| line.contains("Work Order by Item Report")) {
//						continue;
//					}
//					WorkOrderElem obj = new WorkOrderElem();
//					obj.setItem(line.substring(0,24).toUpperCase().trim());
//					obj.setWo(line.substring(25,43).toUpperCase().trim());
//					obj.setWoid(line.substring(44,52).toUpperCase().trim());
//					try {
//						obj.setQtyCompleted(Double.parseDouble(line.substring(53, 66)));
//					} catch (Exception e) {
//					}
//					try {
//						obj.setQtyRejeter(Double.parseDouble(line.substring(67,79)));
//					} catch (Exception e) {
//					}
//					try {
//						obj.setQtyOpen(Double.parseDouble(line.substring(80,92)));
//					} catch (Exception e) {
//					}
//			        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
//			        
//
//					obj.setDueDate(LocalDate.parse(line.substring(101,110).toUpperCase().trim(), formatter));
//					obj.setShift(line.substring(120,128).toUpperCase().trim());
//					obj.setSt(line.substring(129, line.length()).toUpperCase().trim());
//					List<WorkOrderElem> newArr = map.get(obj.getItem());
//					if(newArr == null) {
//						newArr.get
//					}
//					newArr.add(obj);
//					arr.add(obj);
//				}
//				
//				reader.close();
//				ftpClient.completePendingCommand();
//				} catch (Exception e) {
//					System.out.println(rapport + " ERROR : "+ e.getMessage());
//				}
//			}
//			
//		} finally {
//				ftpClient.disconnect();
//		}
//
//		return arr;
//		
//	}


}
