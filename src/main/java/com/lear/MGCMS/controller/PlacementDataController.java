package com.lear.MGCMS.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.lear.MGCMS.domain.CuttingPlan.PartNumberCorrespendance;
import com.lear.MGCMS.services.CuttingPlan.PartNumberCorrespendanceService;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanRapportPlacement;
import com.lear.MGCMS.services.ctc.FilesService;
import com.lear.ctc.domain.Files;

import ch.qos.logback.core.joran.event.InPlayListener;

@RestController
@RequestMapping("/api/placementData")
public class PlacementDataController {

    @Autowired
    private FilesService filesService;

    @Value("${lear.cutfilesFolder}")
    private String cutfilesFolder;
    @Value("${lear.cutfilesArchiveFolder}")
    private String cutfilesArchiveFolder;
    @Value("${lear.pltfolder}")
    private String pltfolder;
    @Value("${lear.cutfilesAblLaserFolder}")
    private String cutfilesAblLaserFolder;

    @Autowired
    private PartNumberCorrespendanceService partNumberCorrespendanceService;

    @PostMapping("/plt-check")
    public List<DigitInfo> checkPlt(@RequestBody List<String> digits) {
        List<DigitInfo> arr = new ArrayList<DigitInfo>();
        for (String digit : digits) {
            DigitInfo obj = new DigitInfo();
            obj.setPattern(digit);
            try {
                String folderPath = pltfolder;
                String fileName = digit + ".plt";
                File file = new File(folderPath, fileName);
                if (file.exists()) {
                    obj.setExist(true);
                } else {
                    obj.setExist(false);
                }
            } catch (Exception e) {

            }
            arr.add(obj);
        }
        return arr;
    }

    @GetMapping("/rapport")
    public ResponseEntity<?> findRapportList(
            //List<String> placements
            @RequestParam(value = "placements", required = false) List<String> placements,
            @RequestParam(value = "partNumbers", required = false) List<String> partNumbers
    ) {
        List<CuttingPlanRapportPlacement> arr = new ArrayList<CuttingPlanRapportPlacement>();

        for (String placement : placements) {
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
                                continue;
                            }
                        }
                    }
                }

            } finally {
                if (br != null) {
                    CuttingPlanRapportPlacement obj = new CuttingPlanRapportPlacement();
                    obj.setNomPlct(placement);
                    obj.setNomDefPlct(placement);
                    String[] liste = br.lines().collect(Collectors.toList())
                            .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                    String digit = null;
                    String idPaquet = null;
                    Integer empInd = null;
                    String partNumberMaterial = null;
                    String partNumber = null, mode = null;
                    Double perimetre = 0.0;
                    // calculate perimetre
                    String pointsData = liste[0].substring(liste[0].indexOf("*N1") + 3, liste[0].indexOf("*Q"));
                    if (pointsData.contains("M43*")) {
                        obj.setDrill1(true);
                    } else {
                        obj.setDrill1(false);
                    }
                    if (pointsData.contains("M44*")) {
                        obj.setDrill2(true);
                    } else {
                        obj.setDrill2(false);
                    }

                    for (String points : pointsData.split("\\*N")) {
//						int minX = 999999999;
//						int maxX = 0;

                        for (String pointsXY : points.split("\\*M15\\*")) {
                            String pointerType = "", drillType = "";
                            try {
                                Integer lastX = null, lastY = null;
                                for (String elem : ("*M15*" + pointsXY + "*M15*").split("\\*")) {
                                    if (elem.startsWith("M")) {
                                        pointerType = elem;
                                        drillType = "";
                                    }
                                    if (elem.startsWith("D")) {
                                        drillType = elem;
                                    }
                                    if (elem.startsWith("X")) {
                                        if (drillType == "" || !elem.contains("M")) {
                                            if (lastX != null && lastY != null) {
                                                perimetre += Math.hypot(
                                                        Integer.parseInt(elem.replace("X", "").split("Y")[0]) - lastX,
                                                        Integer.parseInt(elem.replace("X", "").split("Y")[1]) - lastY);
                                            }
                                            lastX = Integer.parseInt(elem.replace("X", "").split("Y")[0]);
                                            lastY = Integer.parseInt(elem.replace("X", "").split("Y")[1]);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            }

                        }
                    }
                    obj.setPerimetreTotal(convertTwoDigit(perimetre / 42, 3) + "");

                    if (liste[0].contains("/LO=") && liste[0].contains("/l=")) {
                        obj.setLongueur(convertTwoDigit(Double
                                .parseDouble(liste[0].subSequence(liste[0].indexOf("/LO") + 4, liste[0].indexOf("/l"))
                                        .toString().replace("CM", ""))
                                * 0.01, 3) + "");
                        obj.setLargeur(liste[0].subSequence(liste[0].indexOf("/l") + 3, liste[0].indexOf("*N1"))
                                .toString().replace("CM", ""));
                    } else if (liste[0].contains("/L=") && liste[0].contains("/W=")) {
                        obj.setLongueur(convertTwoDigit(Double
                                .parseDouble(liste[0].subSequence(liste[0].indexOf("/L") + 3, liste[0].indexOf("/W"))
                                        .toString().replace("CM", ""))
                                * 0.01, 3) + "");
                        obj.setLargeur(liste[0].subSequence(liste[0].indexOf("/W") + 3, liste[0].indexOf("*N1"))
                                .toString().replace("CM", ""));
                    }
                    Map<String, Integer> map = new HashMap<String, Integer>();
                    Map<String, Set<String>> mapPaquet = new HashMap<String, Set<String>>();

                    //let count how many "LFL " or "GRE " in the text
                    int countGRE = 0, count = 0;
                    boolean stillOnHeader = true;
                    for (int i = 1; i < liste.length; i++) {
                        //a text could countain "LFL " or "GRE " more than one time
                        if (stillOnHeader && (liste[i].toUpperCase().contains("LFL ")
                                || liste[i].toUpperCase().contains("RFL ")
                                || liste[i].toUpperCase().contains("GRE ")
                                || liste[i].toUpperCase().contains("DRE "))) {
                            String text = liste[i].toUpperCase();
                            Pattern pattern = Pattern.compile("LFL |RFL |GRE |DRE ");
                            Matcher matcher = pattern.matcher(text);
                            while (matcher.find()) {
                                countGRE++;
                            }
                        }

                        if (stillOnHeader && (liste[i].toUpperCase().contains("CH ") || liste[i].toUpperCase().contains("OV "))) {
                            obj.setPertePercentage("");
                            String[] str = liste[i].split("\u0000");
                            for(String s : str) {
                                if(s != null && !s.isEmpty() && (s.contains("CH ") || s.contains("OV "))) {
                                    obj.setPertePercentage(s.split("CH ")[0] + " " + obj.getPertePercentage());
                                }
                            }


                        }
                        if (obj.getContraintes() == null && liste[i].toUpperCase().contains(placement.toUpperCase())) {
                            String subString = liste[i];
                            if (subString.contains("ESP00")) {
                                obj.setContraintes("ESP00");
                            } else if (subString.contains("ESP01")) {
                                obj.setContraintes("ESP01");
                            } else if (subString.contains("ESP02")) {
                                obj.setContraintes("ESP02");
                            } else if (subString.contains("ESP03")) {
                                obj.setContraintes("ESP03");
                            } else if (subString.contains("ESP04")) {
                                obj.setContraintes("ESP04");
                            } else if (subString.contains("ESP06")) {
                                obj.setContraintes("ESP06");
                            } else if (subString.contains("ESP10")) {
                                obj.setContraintes("ESP10");
                            } else if (subString.contains("ESP16")) {
                                obj.setContraintes("ESP16");
                            }
                            if (subString.contains("ROTA-180-4D") && obj.getDescription() == null) {
                                obj.setDescription("ROTA-180-4D");
                            } else if (subString.contains("ROTA-180-3D") && obj.getDescription() == null) {
                                obj.setDescription("ROTA-180-3D");
                            } else if (subString.contains("ROTA-180-2D") && obj.getDescription() == null) {
                                obj.setDescription("ROTA-180-2D");
                            } else if (subString.contains("ROTA-45") && obj.getDescription() == null) {
                                obj.setDescription("ROTA-45");
                            } else if (subString.contains("ROTA-180") && obj.getDescription() == null) {
                                obj.setDescription("ROTA-180");
                            } else if (subString.contains("ROTA-90") && obj.getDescription() == null) {
                                obj.setDescription("ROTA-90");
                            } else if (subString.contains("ROTA-FIXE") && obj.getDescription() == null) {
                                obj.setDescription("ROTA-FIXE");
                            }

                        }
                        if (partNumberMaterial == null && (liste[i].toUpperCase().startsWith("M,NUMERO")
                                || liste[i].toUpperCase().startsWith("M,ORDER NUM"))) {
                            stillOnHeader = false;
                            partNumberMaterial = liste[i].split(",")[2];
                            obj.setNumeroDefPlct(partNumberMaterial);
                        }
                        if (liste[i].toUpperCase().startsWith("M,Efficience plct".toUpperCase())
                                || liste[i].toUpperCase().startsWith("M,MARKER UTILTIZATION")) {
                            obj.setEfficience(liste[i].split(",")[2]);
                        }
                        if (liste[i].startsWith("D,1,")) {
                            digit = liste[i].split(",")[2];
                            count++;
                        }
                        if (liste[i].startsWith("D,5,")) {
                            idPaquet = liste[i].split(",")[2];
                        }
                        if (liste[i].startsWith("D,6,")) {
                            partNumber = liste[i].split(",")[2];
                            if (map.get(partNumber + ":" + digit) != null) {
                                map.put(partNumber + ":" + digit, map.get(partNumber + ":" + digit) + 1);
                                Set<String> arrPaquet = mapPaquet.get(partNumber + ":" + digit);
                                arrPaquet.add(idPaquet);
                                mapPaquet.put(partNumber + ":" + digit, arrPaquet);
                            } else {
                                map.put(partNumber + ":" + digit, 1);
                                Set<String> arrPaquet = new HashSet<String>();
                                arrPaquet.add(idPaquet);
                                mapPaquet.put(partNumber + ":" + digit, arrPaquet);
                            }
                        }
                    }
                    Map<String, Integer> mapMinQty = new HashMap<String, Integer>();
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        String key = entry.getKey();
                        Integer value = entry.getValue();
//						Files file = filesService.findFirstByPartNumberCoverAndPattern(key.split(":")[0].trim(), key.split(":")[1].trim());
//						if(file != null && file.getQuantity() != null && file.getQuantity() > 1) {
//							value = entry.getValue() / (file.getQuantity());
//						} else {
                        value = mapPaquet.get(key).size();
//						}
                        Integer qte = value;

                        if (mapMinQty.get(key.split(":")[0]) != null) {
                            qte = mapMinQty.get(key.split(":")[0]);
                        }
                        mapMinQty.put(key.split(":")[0], Math.min(qte, value));
                        // do something with the key-value pair
                    }

                    List<String> models = new ArrayList<String>();
                    List<String> quantiteTailles = new ArrayList<String>();

                    for (Map.Entry<String, Integer> entry : mapMinQty.entrySet()) {
                        String key = entry.getKey();
                        Integer value = entry.getValue();
                        models.add(key);
                        quantiteTailles.add("1(" + value + ")");
                    }
                    List<String> newModels = new ArrayList<String>();
                    for (String model : models) {
                        boolean added = false;
                        for (String pn : partNumbers) {
                            PartNumberCorrespendance crObj = partNumberCorrespendanceService.findByPartNumberAndPartNumberCorrespondanceAndPlacement(model, pn, placement);
                            if(crObj == null) {
                                crObj = partNumberCorrespendanceService.findByPartNumberAndPartNumberCorrespondanceAndPlacementNull(model, pn);
                            }
                            if (crObj != null) {
                                newModels.add(crObj.getPartNumberCorrespondance());
                                added = true;
                                break;
                            }
                        }
                        if (!added) {
                            newModels.add(model);
                        }
                    }
                    obj.setModeles(String.join(", ", newModels));
                    obj.setPlaceTailleQt(String.join(" | ", quantiteTailles));
                    if (countGRE > 0) {
                        obj.setNa(countGRE + "/" + count);
                    }
                    arr.add(obj);
                }
            }
        }
        return new ResponseEntity<List<CuttingPlanRapportPlacement>>(arr, HttpStatus.OK);

    }

    @GetMapping("/nbrPiece/{placements}")
    public List<PlacementCounter> getTotalEmp(@PathVariable List<String> placements) {
        List<PlacementCounter> arr = new ArrayList<PlacementCounter>();
        for (String placement : placements) {
            PlacementCounter obj = new PlacementCounter();
            Integer count = 0;
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
//							return new ResponseEntity<String>("Bad request" , HttpStatus.BAD_REQUEST);
                            }
                        }
                    }
                }

            } finally {
                try {
                    if (br != null) {
                        String[] liste = br.lines().collect(Collectors.toList())
                                .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                        if (liste[0].contains("/LO=") && liste[0].contains("/l=")) {
                            obj.setLongueur(convertTwoDigit(Double
                                    .parseDouble(liste[0].subSequence(liste[0].indexOf("/LO") + 4, liste[0].indexOf("/l"))
                                            .toString().replace("CM", ""))
                                    * 0.01, 3));
                            obj.setLargeur(convertTwoDigit(Double.parseDouble(liste[0].subSequence(liste[0].indexOf("/l") + 3, liste[0].indexOf("*N1"))
                                    .toString().replace("CM", "")) * 0.01, 3));
                        } else if (liste[0].contains("/L=") && liste[0].contains("/W=")) {
                            obj.setLongueur(convertTwoDigit(Double
                                    .parseDouble(liste[0].subSequence(liste[0].indexOf("/L") + 3, liste[0].indexOf("/W"))
                                            .toString().replace("CM", ""))
                                    * 0.01, 3));
                            obj.setLargeur(convertTwoDigit(Double.parseDouble(liste[0].subSequence(liste[0].indexOf("/W") + 3, liste[0].indexOf("*N1"))
                                    .toString().replace("CM", "")) * 0.01, 3));
                        }
                        String pointsData = liste[0].substring(liste[0].indexOf("*N1") + 2, liste[0].indexOf("*Q"));
                        if (pointsData.contains("M43*")) {
                            obj.setDrill1(true);
                        }
                        if (pointsData.contains("M44*")) {
                            obj.setDrill2(true);
                        }
                        String digit = null;
                        for (int i = 1; i < liste.length; i++) {
                            if (liste[i].startsWith("D,1,")) {
                                count++;
                            }
                        }
                    }
                } catch (Exception e2) {
                    System.out.println(e2.getMessage());
                }
            }
            obj.setPlacement(placement);
            obj.setNbrPiece(count);
            if (count > 0) {
                arr.add(obj);
            }
        }
        return arr;
    }

    static int firstNumber(String elem) {
        String number = "";
        for (int i = 0; i < elem.length(); i++) {
            Boolean flag = Character.isDigit(elem.charAt(i));
            if (flag) {
                number += elem.charAt(i);
            } else {
                break;
            }
            // Print current character
        }
        return Integer.parseInt(number);
    }

    Double convertTwoDigit(Double num, Integer i) {
        return Double.parseDouble(String.format("%." + i + "f", num).replace(",", "."));
    }


    @GetMapping("/info-list")
    public ResponseEntity<?> findInfoList(
            @RequestParam(value = "placements", required = false) List<String> placements,
            @RequestParam(value = "partNumbers", required = false) List<String> partNumbers
    ) {
        List<PlacementInfo> arr = new ArrayList<PlacementInfo>();
        Map<String, List<PartNumberCorrespendance>> mapCache1 = new HashMap<>();
        Map<String, List<PartNumberCorrespendance>> mapCache2 = new HashMap<>();
        List<Files> filesArr = filesService.getFilesPattern(partNumbers);

        for (String placement : placements) {
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
                            }
                        }
                    }
                }

            } finally {
                if (br != null) {
                    // we need to cache the first and the second partNumberCorrespendanceService functions

                    String[] liste = br.lines().collect(Collectors.toList())
                            .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                    String digit = null;
                    Integer empInd = null;
                    String partNumberMaterial = null;
                    String partNumber = null, mode = null;

                    String pointsData = liste[0].substring(liste[0].indexOf("*N1") + 2, liste[0].indexOf("*Q"));
                    Map<Integer, Integer> drillMap1 = new HashedMap<Integer, Integer>();
                    Map<Integer, Integer> drillMap2 = new HashedMap<Integer, Integer>();
                    Map<Integer, Integer> graphNumberMap = new HashedMap<Integer, Integer>();
                    for (String points : pointsData.split("\\*N")) {
                        String target1 = "M43*";
                        int count1 = 0;
                        int index1 = points.indexOf(target1);
                        while (index1 != -1) {
                            count1++;
                            index1 = points.indexOf(target1, index1 + 1);
                        }
                        String target2 = "M44*";
                        int count2 = 0;
                        int index2 = points.indexOf(target2);
                        while (index2 != -1) {
                            count2++;
                            index2 = points.indexOf(target2, index2 + 1);
                        }
                        int endIndex = points.indexOf("*");
                        String numberStr = points.substring(0, endIndex);
                        int number = Integer.parseInt(numberStr);
                        drillMap1.put(number, count1);
                        drillMap2.put(number, count2);
                        Integer graphNumber = 0;
                        Map<Integer, List<String>> graphs = new HashMap<Integer, List<String>>();
                        for (String pointsXY : points.split("\\*M15\\*")) {
                            List<String> pointsArr = new ArrayList<String>();
                            try {
                                String lastPoint = null;
                                for (String elem : ("*M15*" + pointsXY + "*M15*").split("\\*")) {
                                    if(elem.contains("M43") || elem.contains("M44") || elem.startsWith("M")) {
                                        continue;
                                    }
                                    if (elem.startsWith("X")) {
                                        lastPoint = Integer.parseInt(elem.replace("X", "").split("Y")[0]) +","+ Integer.parseInt(elem.replace("X", "").split("Y")[1]);
                                        pointsArr.add(lastPoint);
                                    }
                                }
                            } catch (Exception e) {
                            }
                            //we need to go through every value in graphs and check if there is at least one point in pointsArr
                            // if it does then add it to that value in graph
                            // if not  we add a new value in graph
                            boolean added = false;
                            for (Map.Entry<Integer, List<String>> entry : graphs.entrySet()) {
                                Integer key = entry.getKey();
                                List<String> value = entry.getValue();
                                for(String point : pointsArr) {
                                    if(value.contains(point)) {
                                        value.addAll(pointsArr);
                                        added = true;
                                        break;
                                    }
                                }
                                if(added) {
                                    break;
                                }
                            }
                            if(!added && pointsArr.size() > 2) {
                                graphNumber++;
                                graphs.put(graphNumber, pointsArr);
                            }
                        }

//                        for (Map.Entry<Integer, List<String>> entry : graphs.entrySet()) {
//                            Integer key = entry.getKey();
//                            List<String> value = entry.getValue();
//                            System.out.println(String.join(",", value)+",");
//                        }

                        graphNumberMap.put(number, graphNumber);
                    }

                    for (int i = 1; i < liste.length; i++) {
                        if (partNumberMaterial == null && (liste[i].toUpperCase().startsWith("M,NUMERO")
                                || liste[i].toUpperCase().startsWith("M,ORDER NUM"))) {
                            partNumberMaterial = liste[i].split(",")[2];
                        }

                        if (liste[i].startsWith("L,")) {
                            empInd = Integer.parseInt(liste[i].split(",")[1]);
                            digit = null;
                        } else if (liste[i].startsWith("D,1,")) {
                            digit = liste[i].split(",")[2];
                        } else if (liste[i].startsWith("D,5,")) {
                            mode = liste[i].split(",")[2];
                        } else if (liste[i].startsWith("D,6,")) {
                            partNumber = liste[i].split(",")[2];
                        } else if (liste[i].startsWith("D,7,")) {
                            if (digit != null) {
                                PlacementInfo obj = new PlacementInfo();
                                obj.setPlacement(placement);
                                digit.replace("-LSR", "");
                                obj.setDigit(digit);
                                obj.setPartNumberMaterial(partNumberMaterial);
                                if(partNumbers.contains(partNumber)) {
                                    obj.setPartNumber(partNumber);
                                } else {

                                    List<PartNumberCorrespendance> crArr = null;
                                    // verify mapCache1
                                    if(mapCache1.get(partNumber) != null) {
                                        crArr = mapCache1.get(partNumber);
                                        System.out.println("crArr Cached partNumber "+partNumber+" : " + crArr.size());
                                    } else {
                                        crArr = partNumberCorrespendanceService.findByPartNumberAndPartNumberCorrespondance(partNumber, partNumbers, placements);
                                        System.out.println("crArr partNumber "+partNumber+" : " + crArr.size());
                                        mapCache1.put(partNumber, crArr);
                                    }

                                    for(PartNumberCorrespendance crObj : crArr) {
                                        if (crObj.getPartNumberCorrespondance() == null || crObj.getPartNumberCorrespondance().equalsIgnoreCase(partNumber)) {
                                            continue;
                                        }
                                        if (crObj.getPlacement() != null && !crObj.getPlacement().isEmpty() && !crObj.getPlacement().equalsIgnoreCase(placement)) {
                                            continue;
                                        }
                                        obj.setPartNumber(crObj.getPartNumberCorrespondance());
                                        // If pattern correspondance is also filled, change the digit too
                                        if (crObj.getPatternCorrespondance() != null && !crObj.getPatternCorrespondance().isEmpty()
                                                && crObj.getPattern() != null && !crObj.getPattern().isEmpty()
                                                && crObj.getPattern().equalsIgnoreCase(digit.replace("-LSR", ""))) {
                                            digit = crObj.getPatternCorrespondance();
                                            obj.setDigit(digit);
                                        }
                                    }

                                    if (obj.getPlacement() == null) {
                                        obj.setPartNumber(partNumber);
                                    }
                                }
                                obj.setMode(mode);
                                obj.setEmpInd(empInd);
                                obj.setCounterDrill1(drillMap1.get(empInd));
                                obj.setCounterDrill2(drillMap2.get(empInd));
                                obj.setGraphNumber(graphNumberMap.get(empInd));
                                obj.setSens(liste[i].split(",")[2]);
//                              Instead of this  Files file = filesService.findFirstByPattern(digit.replace("-LSR", "")); we will search for File in filesArr
                                Files file = null;
                                for (Files f : filesArr) {
                                    if (f.getPattern().equalsIgnoreCase(digit.replace("-LSR", "")) && f.getPartNumberCover().equalsIgnoreCase(partNumber)) {
                                        file = f;
                                        break;
                                    }
                                }
                                if(file == null) {
                                    List<PartNumberCorrespendance> arrCpDigit = null;
                                    // verify mapCache2 by digit
                                    if(mapCache2.get(digit.replace("-LSR", "")) != null) {
                                        arrCpDigit = mapCache2.get(digit.replace("-LSR", ""));
                                        System.out.println("arrCpDigit CACHED "+digit.replace("-LSR", "")+" : " + arrCpDigit.size());
                                    } else {
                                        arrCpDigit = partNumberCorrespendanceService.findOnePatternToChange(digit.replace("-LSR", ""), partNumbers, placements);
                                        System.out.println("arrCpDigit "+digit.replace("-LSR", "")+" : " + arrCpDigit.size());
                                        mapCache2.put(digit.replace("-LSR", ""), arrCpDigit);
                                    }
                                    // we need to find if we have a new digit name to change it with
                                    for (PartNumberCorrespendance c : arrCpDigit) {
                                        if (
                                                (c.getPartNumberCorrespondance() == null || c.getPartNumberCorrespondance().equalsIgnoreCase(partNumber))
                                                        && (c.getPlacement() == null || c.getPlacement().equalsIgnoreCase(placement))
                                        ) {
                                            System.out.println("Found new digit name : " + c.getPatternCorrespondance() + " for " + digit.replace("-LSR", ""));
                                            digit = c.getPatternCorrespondance();
                                            obj.setDigit(digit);
                                            break;
                                        }
                                    }
                                }
                                if (file != null) {
                                    obj.setEmpNumber(file.getPanelNumber());
                                }
//							int nextInd = liste[0].indexOf("N"+(empInd+1));
//							if(nextInd != -1) {
//								obj.setContent(liste[0].subSequence(liste[0].indexOf("N"+empInd+"*")+3, liste[0].indexOf("N"+(empInd+1)+"*")).toString());
//							} else {
//								obj.setContent(liste[0].subSequence(liste[0].indexOf("N"+empInd+"*")+3, liste[0].indexOf("*Q")).toString());
//							}
                                arr.add(obj);
                                digit = null;
                            }
                        }

                    }
                }
            }
        }
        return new ResponseEntity<List<PlacementInfo>>(arr, HttpStatus.OK);
    }

    @GetMapping("/info-content/{pn}/{placements}")
    public ResponseEntity<?> findInfoList(@PathVariable String pn, @PathVariable List<String> placements) {
        List<PlacementInfo> arr = new ArrayList<PlacementInfo>();
        for (String placement : placements) {
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
                            }
                        }
                    }
                }

            } finally {

                String[] liste = br.lines().collect(Collectors.toList())
                        .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                String digit = null;
                Integer empInd = null;
                String partNumberMaterial = null;
                String partNumber = null, mode = null;
                for (int i = 1; i < liste.length; i++) {
                    if (partNumberMaterial == null && (liste[i].toUpperCase().startsWith("M,NUMERO")
                            || liste[i].toUpperCase().startsWith("M,ORDER NUM"))) {
                        partNumberMaterial = liste[i].split(",")[2];
                    }

                    if (liste[i].startsWith("L,")) {
                        empInd = Integer.parseInt(liste[i].split(",")[1]);
                        digit = null;
                    } else if (liste[i].startsWith("D,1,")) {
                        digit = liste[i].split(",")[2];
                    } else if (liste[i].startsWith("D,5,")) {
                        mode = liste[i].split(",")[2];
                    } else if (liste[i].startsWith("D,6,")) {
                        partNumber = liste[i].split(",")[2];
                    } else if (liste[i].startsWith("D,7,")) {
                        if (digit != null) {
                            if (pn.equalsIgnoreCase(partNumber)) {
                                PlacementInfo obj = new PlacementInfo();
                                obj.setPlacement(placement);
                                digit.replace("-LSR", "");
                                obj.setDigit(digit);
                                obj.setPartNumberMaterial(partNumberMaterial);
                                obj.setPartNumber(partNumber);
                                obj.setMode(mode);
                                obj.setEmpInd(empInd);
                                obj.setSens(liste[i].split(",")[2]);
                                Files file = filesService.findFirstByPattern(digit.replace("-LSR", ""));
                                if (file != null) {
                                    obj.setEmpNumber(file.getPanelNumber());
                                }
                                int nextInd = liste[0].indexOf("N" + (empInd + 1));
                                if (nextInd != -1) {
                                    obj.setContent(liste[0].subSequence(liste[0].indexOf("N" + empInd + "*") + 3,
                                            liste[0].indexOf("N" + (empInd + 1) + "*")).toString());
                                } else {
                                    obj.setContent(liste[0].subSequence(liste[0].indexOf("N" + empInd + "*") + 3,
                                            liste[0].indexOf("*Q")).toString());
                                }

                                arr.add(obj);
                            }

                            digit = null;
                        }
                    }

                }
            }
        }
        return new ResponseEntity<List<PlacementInfo>>(arr, HttpStatus.OK);
    }

}

class PlacementCounter {
    private String placement;
    private Integer nbrPiece;
    private Double longueur;
    private Double largeur;
    private boolean drill1 = false;
    private boolean drill2 = false;

    public boolean isDrill1() {
        return drill1;
    }

    public void setDrill1(boolean drill1) {
        this.drill1 = drill1;
    }

    public boolean isDrill2() {
        return drill2;
    }

    public void setDrill2(boolean drill2) {
        this.drill2 = drill2;
    }

    public Double getLongueur() {
        return longueur;
    }

    public void setLongueur(Double longueur) {
        this.longueur = longueur;
    }

    public Double getLargeur() {
        return largeur;
    }

    public void setLargeur(Double largeur) {
        this.largeur = largeur;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public Integer getNbrPiece() {
        return nbrPiece;
    }

    public void setNbrPiece(Integer nbrPiece) {
        this.nbrPiece = nbrPiece;
    }

}

class DigitInfo {
    private String pattern;
    private Boolean exist;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Boolean getExist() {
        return exist;
    }

    public void setExist(Boolean exist) {
        this.exist = exist;
    }

}

class PlacementInfo {
    private String placement;
    private String partNumberMaterial;
    private String empNumber;

    private Integer empInd;
    private String digit;
    private String mode;
    private String partNumber;
    private String sens;

    private Integer counterDrill1;
    private Integer counterDrill2;
    private Integer graphNumber;
    private String content;

    public Integer getGraphNumber() {
        return graphNumber;
    }

    public void setGraphNumber(Integer graphNumber) {
        this.graphNumber = graphNumber;
    }

    public PlacementInfo() {
        super();
    }

    public Integer getCounterDrill1() {
        return counterDrill1;
    }

    public void setCounterDrill1(Integer counterDrill1) {
        this.counterDrill1 = counterDrill1;
    }

    public Integer getCounterDrill2() {
        return counterDrill2;
    }

    public void setCounterDrill2(Integer counterDrill2) {
        this.counterDrill2 = counterDrill2;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSens() {
        return sens;
    }

    public void setSens(String sens) {
        this.sens = sens;
    }

    public String getPartNumberMaterial() {
        return partNumberMaterial;
    }

    public void setPartNumberMaterial(String partNumberMaterial) {
        this.partNumberMaterial = partNumberMaterial;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getDigit() {
        return digit;
    }

    public void setDigit(String digit) {
        this.digit = digit;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public Integer getEmpInd() {
        return empInd;
    }

    public void setEmpInd(Integer empInd) {
        this.empInd = empInd;
    }

    public String getEmpNumber() {
        return empNumber;
    }

    public void setEmpNumber(String empNumber) {
        this.empNumber = empNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
