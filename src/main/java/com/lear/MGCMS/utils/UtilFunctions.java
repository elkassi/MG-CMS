package com.lear.MGCMS.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class UtilFunctions {
	
	public static Date convertToDateViaInstant(LocalDateTime dateToConvert) {
	    return java.util.Date.from(dateToConvert.atZone(ZoneId.systemDefault()).toInstant());
	}
	
	public static LocalDateTime convertToLocalDateTime(Date dateToConvert) {
	    return LocalDateTime.ofInstant(
	      dateToConvert.toInstant(), ZoneId.systemDefault());
	}
	
	public static LocalDateTime convertTimestampToLocalDateTime(Timestamp timestampToConvert) {
	    return timestampToConvert.toLocalDateTime();
	}




	public static Double convertTwoDigit(Double num, Integer i) {
		return Double.parseDouble(String.format("%."+i+"f", num).replace(",", "."));
	}


	public static String cellString(Workbook workbook, Cell cell) {
		if(cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue();
			case NUMERIC:
				return String.valueOf(cell.getNumericCellValue());
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
				CellValue cellValue = evaluator.evaluate(cell);
				switch (cellValue.getCellType()) {
					case STRING:
						return(cellValue.getStringValue());
					case NUMERIC:
						return String.valueOf(cellValue.getNumberValue());
					case BOOLEAN:
						return String.valueOf(cellValue.getBooleanValue());
					default:
				}
				break;
			default:

		}
		return null;
	}
}
