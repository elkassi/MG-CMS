package com.lear.MGCMS.controller;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Planning;
import com.lear.MGCMS.utils.UtilFunctions;

@RestController
@RequestMapping("/api/cuttingPlanning")
public class CuttingPlanningController {

	private static final Logger log = LoggerFactory.getLogger(CuttingPlanningController.class);

	@GetMapping("/header")
	public List<PlanningHeader> getHeader() {
		List<PlanningHeader> arr = new ArrayList<PlanningHeader>();

		try {
			ZipSecureFile.setMinInflateRatio(0);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : openning excel");
			FileInputStream file = new FileInputStream(new File("C:\\Cutting Planning.xlsx"));
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
					+ " : Create a new workbook from the file");
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : Get the first sheet");
			XSSFSheet sheet = workbook.getSheetAt(0);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
					+ " : Iterate through each row in the sheet");

			// header 1
			XSSFRow rowHeader1 = sheet.getRow(9);
			for (int j = 0; j < rowHeader1.getLastCellNum(); j++) {
				String strHeader1 = j + " : ";
				XSSFCell cell = rowHeader1.getCell(j);
				CellType cellType = cell.getCellType();
				switch (cellType) {
				case NUMERIC:
					if (DateUtil.isCellDateFormatted(cell)) {
						Date date = cell.getDateCellValue();
						PlanningHeader obj = new PlanningHeader();
						obj.setColumn(j);
						obj.setDate(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()));
						arr.add(obj);
					}
					break;
				case FORMULA:
					FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
					CellValue cellValue = evaluator.evaluate(cell);
					switch (cellValue.getCellType()) {
					case NUMERIC:
						if (DateUtil.isCellDateFormatted(cell)) {
							Date date = cell.getDateCellValue();
							PlanningHeader obj = new PlanningHeader();
							obj.setColumn(j);
							obj.setDate(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()));
							arr.add(obj);
						}
						break;
					default:
					}
					break;
				default:
				}
				System.out.println(strHeader1);
			}

			// header 2

			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : end");

			// Close the workbook
			workbook.close();

		} catch (EncryptedDocumentException | IOException e) {
			log.error("CuttingPlanningController workbook read failed", e);
		}
		return arr;
	}

	@GetMapping("/{date}/{shift}")
	public ResponseEntity<?> findByShift(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PathVariable String shift) {

		try {
			List<Planning> arr = new ArrayList<Planning>();
			ZipSecureFile.setMinInflateRatio(0);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : openning excel");
			FileInputStream file = new FileInputStream(new File("C:\\Cutting Planning.xlsx"));
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
					+ " : Create a new workbook from the file");
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : Get the first sheet");
			XSSFSheet sheet = workbook.getSheetAt(0);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : Searching for date");

			Integer col = 12;

			XSSFRow rowHeader1 = sheet.getRow(9);
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

				if (date1 != null && date1.equals(date)) {
					col = j;
					System.out.println("date found " + j + date.toString());
					break;
				}

			}

			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
					+ " : Iterate through each row in the sheet");

			switch (shift) {
			case "1":
				col = col + 1;
				break;
			case "2":
				col = col + 4;
				break;
			case "3":
				col = col + 7;
				break;
			}

			for (int i = 16; i <= sheet.getLastRowNum(); i++) {
				try {
					XSSFRow row = sheet.getRow(i);
					if (row == null || row.getCell(col) == null) {
						continue;
					}
					if (row.getCell(col).getCellType() != null
							&& !row.getCell(col).getCellType().equals(CellType.NUMERIC)) {
						continue;
					}
					if (row.getCell(4).getStringCellValue() == null || row.getCell(4).getStringCellValue().trim().isEmpty()) {
						continue;
					}
					Planning obj = new Planning();
					obj.setQuantity((int) row.getCell(col).getNumericCellValue());
					if (obj.getQuantity() == 0) {
						continue;
					}
					CellStyle style = row.getCell(col).getCellStyle();

					if (style.getFillForegroundColorColor() instanceof XSSFColor) {
						XSSFColor fillForegroundColor = (XSSFColor) style.getFillForegroundColorColor();
						String color = fillForegroundColor.getARGBHex();
						obj.setColor(color);
					}
					obj.setRowId(i);
					obj.setPartNumber(row.getCell(4).getStringCellValue());
					obj.setDescription(row.getCell(5).getStringCellValue());

					Cell cellItem = row.getCell(6);
					CellType cellType = cellItem.getCellType();
					if (cellType != null) {
						if (cellType.equals(CellType.NUMERIC)) {
							obj.setItem(((int) row.getCell(6).getNumericCellValue()) + "");
						} else if (cellType.equals(CellType.STRING)) {
							obj.setItem(row.getCell(6).getStringCellValue());
						}
					}

					obj.setGroupName(row.getCell(7).getStringCellValue());
					obj.setDesignGroup(row.getCell(8).getStringCellValue());
					obj.setCoverGroup(row.getCell(9).getStringCellValue());
					obj.setStatus(row.getCell(10).getStringCellValue());
//	                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
//	                CellValue cellValue = evaluator.evaluate(row.getCell(11));
//	                if(cellValue ==null) {
//	    	            obj.setPackageQty(null);
//	                } else {
//	    	            obj.setPackageQty((int) cellValue.getNumberValue());
//	                }
					arr.add(obj);
				} catch (Exception e) {
					System.out.println(i + " : " + e.getMessage());
				}
			}
			workbook.close();
			return new ResponseEntity<List<Planning>>(arr, HttpStatus.OK);
		} catch (EncryptedDocumentException | IOException e) {
			// TODO Auto-generated catch block
			return new ResponseEntity<String>("Error : " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping
	public void findAll() {
		try {
			ZipSecureFile.setMinInflateRatio(0);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : openning excel");
			FileInputStream file = new FileInputStream(new File("C:\\Cutting Planning.xlsx"));
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
					+ " : Create a new workbook from the file");
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : Get the first sheet");
			XSSFSheet sheet = workbook.getSheetAt(0);
			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
					+ " : Iterate through each row in the sheet");

			// header 1
			XSSFRow rowHeader1 = sheet.getRow(9);

			for (int j = 0; j < rowHeader1.getLastCellNum(); j++) {
				String strHeader1 = j + " : ";
				XSSFCell cell = rowHeader1.getCell(j);
				CellType cellType = cell.getCellType();
				switch (cellType) {
				case STRING:
					strHeader1 += "|" + cell.getStringCellValue();
					break;
				case NUMERIC:
					if (DateUtil.isCellDateFormatted(cell)) {
						Date date = cell.getDateCellValue();
						strHeader1 += "|" + date.toString();
					} else {
						strHeader1 += "|" + cell.getNumericCellValue();
					}
					break;
				case BOOLEAN:
					strHeader1 += "|" + cell.getNumericCellValue();
					break;
				case FORMULA:
					FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
					CellValue cellValue = evaluator.evaluate(cell);

					switch (cellValue.getCellType()) {
					case STRING:
						strHeader1 += "|" + cellValue.getStringValue();
						break;
					case NUMERIC:
						if (DateUtil.isCellDateFormatted(cell)) {
							Date date = cell.getDateCellValue();
							strHeader1 += "|" + date.toString();
						} else {
							strHeader1 += "|" + cellValue.getNumberValue();
						}
						break;
					case BOOLEAN:
						strHeader1 += "|" + cellValue.getBooleanValue();
						break;
					default:
						System.out.println();
					}
					break;
				default:
				}
				System.out.println(strHeader1);
			}

			System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + " : end");

			// Close the workbook
			workbook.close();
		} catch (EncryptedDocumentException | IOException e) {
			log.error("CuttingPlanningController workbook read failed", e);
		}
	}

	@GetMapping("/wo")
	public List<WoItem> getAllWO() throws IOException {
		List<WoItem> arr = new ArrayList<WoItem>();

		System.out.println("Starting import (Component) ....");
		String server = "10.49.0.46";// txtServer1.getText();
		int port = 21;
		String user = "mfg";// txtUserId1.getText();
		String pass = "leartsi01";// txtPassword1.getText();
		String qadLink = "/qad/home/ftpkpitnr/";
		String[] rapports = { "16_3_2A.prn", "16_3_2AA.prn", "16_3_2R.prn", "16_3_2RR.prn"};

		FTPClient ftpClient = new FTPClient();
		
		try {
			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();
			for(String rapport : rapports) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(ftpClient.retrieveFileStream(qadLink+rapport)));
				String line; int i = 0;
				while ((line = reader.readLine()) != null) {
					if(line.contains("-------------") 
							|| line.contains("Qty Completed")
							|| line.contains("TANGIER-TRIM")
							|| line.contains("16.3.2")) {
						continue;
					}
					if(line.length() > 129 ) {
						WoItem obj = new WoItem();
						obj.setItem(line.substring(0,24).trim().toUpperCase());
						obj.setWo(line.substring(25,43).trim().toUpperCase());
						obj.setWoId(line.substring(44,52).trim().toUpperCase());
						try {
							obj.setQtyOpen(Double.parseDouble(line.substring(67,79).trim().toUpperCase()));
						}catch(Exception e) {
						}
						try {
							obj.setQtyOpen(Double.parseDouble(line.substring(80,92).trim().toUpperCase()));
						}catch(Exception e) {
						}
						try {
							obj.setQtyOpen(Double.parseDouble(line.substring(44,52).trim().toUpperCase()));
						}catch(Exception e) {
						}
						try {
							obj.setQtyOpen(Double.parseDouble(line.substring(44,52).trim().toUpperCase()));
						}catch(Exception e) {
						}
						obj.setShift(line.substring(44,52).trim().toUpperCase());
						obj.setSt(line.substring(44,52).trim().toUpperCase());

//						System.out.println(line);
						arr.add(obj);
					}
				}

				reader.close();
				ftpClient.completePendingCommand();
			}
		} finally {
			ftpClient.disconnect();
		}

		return arr;
	}

}

class WoItem {
	private String item;
	private String wo;
	private String woId;
	private Double qtyOpen;
	private Double qtyRejected;
	private Double qtyCompleted;
	private LocalDate dueDate;
	private String shift;
	private String st;

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public String getWo() {
		return wo;
	}

	public void setWo(String wo) {
		this.wo = wo;
	}

	public String getWoId() {
		return woId;
	}

	public void setWoId(String woId) {
		this.woId = woId;
	}

	public Double getQtyOpen() {
		return qtyOpen;
	}

	public void setQtyOpen(Double qtyOpen) {
		this.qtyOpen = qtyOpen;
	}

	public Double getQtyRejected() {
		return qtyRejected;
	}

	public void setQtyRejected(Double qtyRejected) {
		this.qtyRejected = qtyRejected;
	}

	public Double getQtyCompleted() {
		return qtyCompleted;
	}

	public void setQtyCompleted(Double qtyCompleted) {
		this.qtyCompleted = qtyCompleted;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getShift() {
		return shift;
	}

	public void setShift(String shift) {
		this.shift = shift;
	}

	public String getSt() {
		return st;
	}

	public void setSt(String st) {
		this.st = st;
	}
}

class PlanningHeader {
	private LocalDate date;
	private Integer column;

//	private List<PlanningHeaderShift> planningHeaderShifts = new ArrayList<PlanningHeaderShift>();
	public PlanningHeader() {
		super();
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public Integer getColumn() {
		return column;
	}

	public void setColumn(Integer column) {
		this.column = column;
	}
}

