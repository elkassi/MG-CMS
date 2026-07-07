package com.lear.MGCMS.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanRapportPlacement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;

public class ExcelHelper {

       private static String cutfilesFolder= "\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\CAD\\Cut Files\\";
       private static String cutfilesArchiveFolder= "\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\CAD\\Archive\\";
       private static String pltfolder= "\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\CAD\\PLT Files\\";
       private static String cutfilesAblLaserFolder = "\\\\matnr-fp01\\groups\\Dep\\Ingénierie\\CAD\\ABL LASER\\Cutfile\\";
    /*
        private static String cutfilesFolder= "D:\\LEAR\\Cut Files\\";
        private static String cutfilesArchiveFolder=  "D:\\LEAR\\Archive\\";
        private static String pltfolder=  "D:\\LEAR\\PLT Files\\";
        private static String cutfilesAblLaserFolder =  "D:\\LEAR\\ABL LASER\\Cutfile\\";
     */
    public static Integer getTotalEmp(String placement) {
        Integer count = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream("\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\CAD\\Cut Files\\" + placement),
                    "windows-1252"));
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            try {
                br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(
                                "\\\\matnr-fp01\\groups\\Dep\\Ingénierie\\CAD\\ABL LASER\\Cutfile\\" + placement),
                        "windows-1252"));
            } catch (UnsupportedEncodingException | FileNotFoundException e12) {
                try {
                    br = new BufferedReader(new InputStreamReader(
                            new FileInputStream(
                                    "\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\CAD\\Cut Files\\IP6\\" + placement),
                            "windows-1252"));
                } catch (UnsupportedEncodingException | FileNotFoundException e1) {
                    try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(
                                "\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\CAD\\Cut Files\\Archive\\" + placement),
                                "windows-1252"));
                    } catch (UnsupportedEncodingException | FileNotFoundException e2) {
                        try {
                            br = new BufferedReader(new InputStreamReader(
                                    new FileInputStream(
                                            "\\\\MATNR-FP01\\Groups\\Dep\\Ingénierie\\CAD\\Archive\\" + placement),
                                    "windows-1252"));
                        } catch (UnsupportedEncodingException | FileNotFoundException e3) {
                            System.out.println(placement + " not found");
                            return null;
                        }
                    }
                }
            }

        } finally {
            if (br != null) {
                String[] liste = br.lines().collect(Collectors.toList())
                        .toArray(new String[br.lines().collect(Collectors.toList()).size()]);
                for (int i = 1; i < liste.length; i++) {
                    if (liste[i].startsWith("D,1,")) {
                        count++;
                    }
                }

                if(count == 0) {
                    String pointsData = liste[0].substring(liste[0].indexOf("*N1") + 2, liste[0].indexOf("*Q"));
                    count = pointsData.split("\\*N").length;
                }
            }
        }
        return count;
    }


    public static void doawnloadRapportPrix() {
        String server = "10.49.0.46";// txtServer1.getText();
        int port = 21;
        String user = "mfg";// txtUserId1.getText();
        String pass = "leartsi01";// txtPassword1.getText();
        String qadLink = "/qad/home/batchtnr/3-6-15.prn";
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(server, port);
            if (ftpClient.login(user, pass)) {
                // System.out.println("Login et password ok");
            } else {
                System.out.println("login error (Component)");
            }
            ftpClient.enterLocalPassiveMode();
            if (ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
                System.out.println("Connecter (Component)");
            } else {
                System.out.println("ftp type error 2 (Component)");
            }
            String remoteFile1 = qadLink;
            File downloadFile1 = new File("C:\\pbsimport\\fichier3-6-15.prn");// new File(jTextField3.getText());
            boolean success;
            try (OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1))) {
                success = ftpClient.retrieveFile(remoteFile1, outputStream1);
                outputStream1.close();
                if (success) {
                    System.out.println("File has been downloaded successfully (Component)");
                } else {
                    System.out.println("File has not been  downloaded (Component)");
                }
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
    }

    public static Double getPrixUnit(String reftissu) {
        String server = "10.49.0.46";// txtServer1.getText();
        int port = 21;
        String user = "mfg";// txtUserId1.getText();
        String pass = "leartsi01";// txtPassword1.getText();
        String qadLink = "/qad/home/batchtnr/3-6-15.prn";
        FTPClient ftpClient = new FTPClient();

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
            File downloadFile1 = new File("C:\\pbsimport\\fichier3-6-15.prn");// new File(jTextField3.getText());
            boolean success;
            try (OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1))) {
                success = ftpClient.retrieveFile(remoteFile1, outputStream1);
                outputStream1.close();
                if (success) {
//                    System.out.println("File has been downloaded successfully (Component)");
                } else {
                    System.out.println("File has not been  downloaded (Component)");
                }
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
        return getPrixFromFile(reftissu);
    }

    public static Double getPrixFromFile(String reftissu) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new InputStreamReader(new FileInputStream("C:\\pbsimport\\fichier3-6-15.prn"), "windows-1252"));
            List<String> liste = br.lines().collect(Collectors.toList());
            String ref = "";
            Double prix = 0.0;
            for (String ligne : liste) {

                if (ligne.length() > 93 && !ligne.startsWith("Item Number        Description              Sit")
                        && !ligne.startsWith("-----------------") && !ligne.startsWith("Product Line:")
                        && !ligne.contains("TANGIER-TRIM ") && !ligne.contains("3.6.15 Inventory Valuation as of Date")
                        && ligne.substring(0, 18).trim().equalsIgnoreCase(reftissu.trim())) {
                    ref = ligne.substring(0, 18).trim();
                    prix = Double.parseDouble(ligne.substring(76, 93).trim().replace(",", ""));
                    System.out.println("QAD ref : " + ref + " prix : " + prix);
                    break;
                }
            }
            return prix;
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println("Error : " + e.getMessage());
        } finally {
            try {
            } catch (Throwable e) {
            }
            try {
                br.close();
            } catch (Throwable e) {
            }
        }

        return null;
    }

    public static List<Object> convertObjectToList(Object obj) {
        List<Object> list = new ArrayList<>();
        if (obj.getClass().isArray()) {
            list = Arrays.asList((Object[]) obj);
        } else if (obj instanceof Collection<?>) {
            list = new ArrayList<>((Collection<?>) obj);
        }
        return list;
    }



    public static Double getLongueur(String placement) {
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
                            System.out.println(placement + " not found");
                            return null;
                        }
                    }
                }
            }

        }
        List<String> lines = br.lines().toList();
        String[] liste = lines.toArray(new String[0]);

        if (liste.length > 0) {
            if (liste[0].contains("/LO=") && liste[0].contains("/l=")) {
                return convertTwoDigit(Double
                        .parseDouble(liste[0].substring(liste[0].indexOf("/LO=") + 4, liste[0].indexOf("/l="))
                                .replace("CM", ""))
                        * 0.01, 3);
            } else if (liste[0].contains("/L=") && liste[0].contains("/W=")) {
                return convertTwoDigit(Double
                        .parseDouble(liste[0].substring(liste[0].indexOf("/L=") + 3, liste[0].indexOf("/W="))
                                .replace("CM", ""))
                        * 0.01, 3);
            }
        }
        return null;
    }


    public static Double convertTwoDigit(Double num, Integer i) {
        return Double.parseDouble(String.format("%." + i + "f", num).replace(",", "."));
    }


    public static Double getPerimetre(String placement) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(cutfilesFolder + "" + placement),
                    "windows-1252"));
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            try {
                br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(
                                cutfilesFolder + placement),
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
                            System.out.println(placement + " not found");
                            return null;
                        }
                    }
                }
            }

        } finally {
            if (br != null) {
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

                for (String points : pointsData.split("\\*N")) {
//					int minX = 999999999;
//					int maxX = 0;

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
                                            perimetre += Math.hypot(Integer.parseInt(elem.replace("X", "").split("Y")[0]) - lastX, Integer.parseInt(elem.replace("X", "").split("Y")[1]) - lastY);
                                        }
                                        lastX = Integer.parseInt(elem.replace("X", "").split("Y")[0]);
                                        lastY = Integer.parseInt(elem.replace("X", "").split("Y")[1]);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }

                    }
                }
                return Double.parseDouble(String.format("%." + 3 + "f", perimetre / 42).replace(",", "."));
            }
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public static ByteArrayInputStream listToExcel(List<Object> objs, String classPath) {
        try {
            Class<?> cs = Class.forName(classPath);
            Method methods[] = cs.getDeclaredMethods();
            Field fields[] = cs.getDeclaredFields();
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {

                Sheet sheet = workbook.createSheet("CTC Files");
                // Header
                Row headerRow = sheet.createRow(0);
                CellStyle style = workbook.createCellStyle();
                style.setFillBackgroundColor(HSSFColor.HSSFColorPredefined.AQUA.getIndex());
                Font font = workbook.createFont();
                font.setColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
                font.setFontName("Arial");
                font.setBold(true);
                style.setFont(font);

                for (int col = 0; col < fields.length; col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(fields[col].getName());
                    cell.setCellStyle(style);
                }

                int rowIdx = 1;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                for (Object obj : objs) {
                    //Ppap newObj = gson.fromJson(gson.toJson(obj), Ppap.class);
                    String sites = "";
                    String projets = "";
                    Row row = sheet.createRow(rowIdx++);
                    for (int j = 0; j < fields.length; j++) {
                        try {
                            Object newObj = cs.getDeclaredConstructor().newInstance();
                            //String gsonString = gson.toJson(obj);
                            //newObj = gson.fromJson(gsonString, cs);
                            newObj = cs.cast(obj);
                            //System.out.println(fields[j].getName() + " / " + fields[j].getType() + " / " + fields[j].getGenericType()+ " / " + fields[j].getAnnotations().length + " / "+ fields[j].getDeclaredAnnotations().length);
                            if (fields[j].getType().toString().contains("interface java.util.List")) {
                                Method method = cs.getDeclaredMethod("get" + (fields[j].getName().charAt(0) + "").toUpperCase()
                                        + fields[j].getName().substring(1));
                                Class<?> csMin = Class.forName(fields[j].getGenericType().getTypeName().substring(15, fields[j].getGenericType().getTypeName().length() - 1));
                                Field[] fieldsMin = csMin.getDeclaredFields();
                                if (method.invoke(newObj) != null) {
                                    List<Object> objArr = convertObjectToList(method.invoke(newObj));
                                    for (Field fieldMin : fieldsMin) {
                                        if (!fieldMin.getName().equals("id")) {
                                            Method methodMin = csMin.getDeclaredMethod("get" + (fieldMin.getName().charAt(0) + "").toUpperCase()
                                                    + fieldMin.getName().substring(1));
                                            List<String> result = new ArrayList<String>();
                                            for (Object objElem : objArr) {
                                                result.add(methodMin.invoke(csMin.cast(objElem)).toString());
                                            }
                                            row.createCell(j).setCellValue(String.join(",", result));
                                            break;

                                        }
                                    }
                                }
                            } else if (fields[j].getType().toString().startsWith("class com.lear.learweb")) {
                                Class<?> csMin = Class.forName(fields[j].getType().getName());
                                Field[] fieldsMin = csMin.getDeclaredFields();
                                Method method = cs.getDeclaredMethod("get" + (fields[j].getName().charAt(0) + "").toUpperCase()
                                        + fields[j].getName().substring(1));
                                if (method.invoke(newObj) != null) {
                                    for (Field fieldMin : fieldsMin) {
                                        if (!fieldMin.getName().equals("id")) {
                                            Method methodMin = csMin.getDeclaredMethod("get" + (fieldMin.getName().charAt(0) + "").toUpperCase()
                                                    + fieldMin.getName().substring(1));
                                            if (methodMin.invoke(csMin.cast(method.invoke(newObj))) != null) {
                                                row.createCell(j).setCellValue(methodMin.invoke(csMin.cast(method.invoke(newObj))).toString());
                                                break;
                                            }
                                        }
                                    }
                                }

                            } else if (fields[j].getType().toString().equals("boolean")) {
                                Method method = cs.getDeclaredMethod("is" + (fields[j].getName().charAt(0) + "").toUpperCase()
                                        + fields[j].getName().substring(1));
                                if (method.invoke(newObj) != null) {
                                    row.createCell(j).setCellValue((boolean) method.invoke(newObj));
                                }
                            } else if (fields[j].getType().toString().equals("class java.util.Date")) {
                                Method method = cs.getDeclaredMethod("get" + (fields[j].getName().charAt(0) + "").toUpperCase()
                                        + fields[j].getName().substring(1));
                                if (method.invoke(newObj) != null) {
                                    row.createCell(j).setCellValue(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format((Date) method.invoke(newObj)).toString());
                                }
                            } else {
                                Method method = cs.getDeclaredMethod("get" + (fields[j].getName().charAt(0) + "").toUpperCase()
                                        + fields[j].getName().substring(1));
                                if (method.invoke(newObj) != null) {
                                    row.createCell(j).setCellValue(method.invoke(newObj).toString());
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
                        }
                    }
                    // Autosize columns
                    for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                        sheet.autoSizeColumn(columnIndex);
                    }
                }


                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            } catch (IOException e1) {
                throw new RuntimeException("IOException: " + e1.getMessage());
            } /*catch (InstantiationException | IllegalAccessException e2) {
				throw new RuntimeException("InstantiationException | IllegalAccessException: " + e2.getMessage());
			}*/

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("ClassNotFoundException: " + e.getMessage());
        }

    }

    public static LocalDate getLocalDateFromCell(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            String dateString = df.format(DateUtil.getJavaDate(cell.getNumericCellValue()));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dateString, formatter);
        } else if (cell.getCellType() == CellType.STRING) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(cell.getStringCellValue(), formatter);
        } else {
//            throw new IllegalArgumentException("Cell is not a date type");
            return null;
        }
    }

    public static LocalDateTime getLocalDateTimeFromCell(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String dateString = df.format(DateUtil.getJavaDate(cell.getNumericCellValue()));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return LocalDateTime.parse(dateString, formatter);
        } else if (cell.getCellType() == CellType.STRING) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return LocalDateTime.parse(cell.getStringCellValue(), formatter);
        } else {
//            throw new IllegalArgumentException("Cell is not a date time type");
            return null;
        }
    }

    public static String cellString(Workbook workbook, Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return new DecimalFormat("#").format(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                switch (cellValue.getCellType()) {
                    case STRING:
                        return (cellValue.getStringValue());

                    case NUMERIC:
                        return new DecimalFormat("#").format(cellValue.getNumberValue());
                    case BOOLEAN:
                        return String.valueOf(cellValue.getBooleanValue());
                    default:
                }
                break;
            default:

        }
        return null;
    }

    public static Double getNumericValueFromCell(Workbook workbook, Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    try {
                        DataFormatter formatter = new DataFormatter();
                        String strValue = formatter.formatCellValue(cell);
                        return Double.parseDouble(strValue);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                case FORMULA:
                    FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case STRING:
                            String strValue = cellValue.getStringValue();
                            return Double.parseDouble(strValue);
                        case NUMERIC:
                            return cellValue.getNumberValue();
                        default:
                            break;
                    }
                    break;
                default:
                    return null;
            }
        } catch (Exception e) {
            // Handle any exceptions here
        } catch (NoClassDefFoundError e) {

        }
        return null;
    }


}