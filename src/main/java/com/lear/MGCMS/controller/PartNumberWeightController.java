package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.PartNumberWeight;
import com.lear.MGCMS.services.PartNumberWeightService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/partNumberWeight")
public class PartNumberWeightController {

    @Autowired
    private PartNumberWeightService partNumberWeightService;

    @GetMapping("/list")
    public List<PartNumberWeight> list() {
        return partNumberWeightService.findAll();
    }

    @GetMapping("/all")
    public Page<PartNumberWeight> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        Pageable pageable = PageRequest.of(page, size,
                dir.equals("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());
        return partNumberWeightService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartNumberWeight> findById(@PathVariable Long id) {
        Optional<PartNumberWeight> partNumberWeight = partNumberWeightService.findById(id);
        return partNumberWeight.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/byPartnumber/{partnumber}")
    public ResponseEntity<PartNumberWeight> findByPartnumber(@PathVariable String partnumber) {
        Optional<PartNumberWeight> partNumberWeight = partNumberWeightService.findByPartnumber(partnumber);
        return partNumberWeight.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PartNumberWeight> create(@RequestBody PartNumberWeight partNumberWeight) {
        if (partNumberWeight.getPartnumber() == null || partNumberWeight.getWeightUnit() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(partNumberWeightService.save(partNumberWeight));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartNumberWeight> update(@PathVariable Long id, @RequestBody PartNumberWeight partNumberWeight) {
        Optional<PartNumberWeight> existing = partNumberWeightService.findById(id);
        if (existing.isPresent()) {
            partNumberWeight.setId(id);
            return ResponseEntity.ok(partNumberWeightService.save(partNumberWeight));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<PartNumberWeight> existing = partNumberWeightService.findById(id);
        if (existing.isPresent()) {
            partNumberWeightService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }

        try {
            List<PartNumberWeight> importedRecords = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter dataFormatter = new DataFormatter();
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    try {
                        Cell partnumberCell = row.getCell(0);
                        Cell weightUnitCell = row.getCell(1);

                        if (partnumberCell == null || weightUnitCell == null) {
                            errors.add("Row " + (i + 1) + ": Missing data");
                            errorCount++;
                            continue;
                        }

                        String partnumber = getCellValueAsString(partnumberCell, dataFormatter, formulaEvaluator);
                        Double weightUnit = getCellValueAsDouble(weightUnitCell, formulaEvaluator);

                        if (partnumber == null || partnumber.trim().isEmpty() || weightUnit == null) {
                            errors.add("Row " + (i + 1) + ": Invalid data");
                            errorCount++;
                            continue;
                        }

                        PartNumberWeight entity = new PartNumberWeight();
                        entity.setPartnumber(partnumber.trim());
                        entity.setWeightUnit(weightUnit);

                        partNumberWeightService.save(entity);
                        importedRecords.add(entity);
                        successCount++;

                    } catch (Exception e) {
                        errors.add("Row " + (i + 1) + ": " + e.getMessage());
                        errorCount++;
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("errors", errors);
            response.put("message", "Import completed: " + successCount + " successful, " + errorCount + " errors");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing file: " + e.getMessage()));
        }
    }

    private String getCellValueAsString(Cell cell, DataFormatter dataFormatter, FormulaEvaluator formulaEvaluator) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.FORMULA) {
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            if (cellValue == null) return null;
            switch (cellValue.getCellType()) {
                case NUMERIC:
                    return dataFormatter.formatCellValue(cell, formulaEvaluator);
                case STRING:
                    return cellValue.getStringValue();
                default:
                    return dataFormatter.formatCellValue(cell, formulaEvaluator);
            }
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return dataFormatter.formatCellValue(cell);
        }

        return dataFormatter.formatCellValue(cell, formulaEvaluator);
    }

    private Double getCellValueAsDouble(Cell cell, FormulaEvaluator formulaEvaluator) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.FORMULA) {
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            if (cellValue == null) return null;
            if (cellValue.getCellType() == CellType.NUMERIC) {
                return cellValue.getNumberValue();
            }
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
