package com.lear.MGCMS;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacement;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanData;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementData;
import com.lear.MGCMS.security.Constants;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanDataService;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanMaterialPlacementDataService;
import com.lear.MGCMS.services.QueryService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.MGCMS.utils.UtilFunctions;
import com.lear.cms.domain.SpreadingCuttingPlanCoupe;
import com.lear.cms.repositories.SpreadingCuttingPlanCoupeRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

//@Component
public class TestingTask {

    @Autowired
    private CuttingPlanMaterialPlacementDataService cuttingPlanMaterialPlacementDataService;

    @Autowired
    private SpreadingCuttingPlanCoupeRepository spreadingCuttingPlanCoupeRepository;
    @Autowired
    private ApplicationContext context;

    @Autowired
    private CuttingPlanDataService cuttingPlanDataService;

    @Autowired
    private QueryService queryService;

    @Scheduled(fixedRate = 1000 * 60 * 10)
    public void updateQuantity() {
        try{
            List<String> placementWithoutQty = queryService.getPlacementWithoutQuantity();
            for(String placement : placementWithoutQty) {
                Integer qty = ExcelHelper.getTotalEmp(placement);
                if(qty != null) {
                    queryService.updateQtyOfPlacement(placement, qty);
                    System.out.println("Updated qty " + qty + " for placement " + placement);
                } else {
                    System.out.println("NOT FOUND qty for placement " + placement);
                }
            }
        } catch(Exception e) {
            System.out.println("FATAL ERROR : " + e.getMessage());
        }
    }

//    @Scheduled(fixedRate = 1000 * 60 * 10, initialDelay = 1000 * 60 * 10)
    public void stopingCMS() {
        int hour = LocalDateTime.now().getHour();
        int minute = LocalDateTime.now().getMinute();
        if((hour == 14 ||hour == 22 || hour == 6) && minute >=0 && minute <10) {
            Constants.writeLogs("Stopping app...");
            SpringApplication.exit(context, null);
        }
    }

//    @Scheduled(fixedRate = 1000 * 60 * 10)
    public void updatePlacement() {
        try {
            FileInputStream file = new FileInputStream("C:\\Placement.xlsx");
            Workbook workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheet("Sheet1");
            Iterator<Row> rows = sheet.iterator();
            int rowNumber = 0;

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                // skip header
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }
                String oldPlacement = UtilFunctions.cellString(workbook, currentRow.getCell(0));
                String newPlacement = UtilFunctions.cellString(workbook, currentRow.getCell(1));
                if(oldPlacement ==null || newPlacement == null) {
                    continue;
                }
                Long idPlan = Long.parseLong(UtilFunctions.cellString(workbook, currentRow.getCell(2)).replace(".0", ""));
                String reftissu =  UtilFunctions.cellString(workbook, currentRow.getCell(3));
                Double longueur =  Double.parseDouble(UtilFunctions.cellString(workbook, currentRow.getCell(5)));
                Double laize = 1.55;
                try{
                    CuttingPlanMaterialPlacementData cpmp = cuttingPlanMaterialPlacementDataService.getByPlacementAndPlanCoupe(oldPlacement, idPlan);
//                    CuttingPlanData cuttingPlanData = cuttingPlanDataService.findById(idPlan);
//                    SpreadingCuttingPlanCoupe sp = spreadingCuttingPlanCoupeRepository.findByPlacementAndIdSpreadingPlanForeignPlanCoupe(oldPlacement, cuttingPlanData.getCmsId());
                    if(cpmp == null) {
                        System.out.println("CuttingPlanMaterialPlacementData NOT FOUND PLacement " +oldPlacement + " : idPlan " + idPlan + " ");
                        continue;
                    }
                    System.out.println("Deleting 1 " + oldPlacement + " " + idPlan);
                    cuttingPlanMaterialPlacementDataService.delete(cpmp);
                    System.out.println("Deleted 1 " + oldPlacement+ " " + idPlan);

//                    if(sp == null) {
//                        System.out.println("SpreadingCuttingPlanCoupe NOT FOUND PLacement " +oldPlacement + " : idPlan " + idPlan + " ");
//                        continue;
//                    }
//                    cpmp.setPlacement(newPlacement);
//                    if(longueur < 3) {
//                        cpmp.setLongueur(longueur);
//                        cpmp.setLongueurMatelas((longueur+0.02) * cpmp.getNbrCouche());
//                    } else {
//                        cpmp.setLongueur(longueur);
//                        cpmp.setLongueurMatelas((longueur+0.03) * cpmp.getNbrCouche());
//                    }
//                    cpmp.setLaize(1.55);
//                    cpmp.setCategory("A");
//                    System.out.println("Saving 1 " + newPlacement);
//                    cuttingPlanMaterialPlacementDataService.save(cpmp);
//                    System.out.println("Saved 1 " + newPlacement);
//                    sp.setPlacementPlanCoupe(newPlacement);
//                    if(longueur < 3) {
//                        sp.setLongueurPlacementPlanCoupe(longueur);
//                        sp.setLongueurMatelasPlanCoupe(longueur + 0.02);
//                    } else {
//                        sp.setLongueurPlacementPlanCoupe(longueur);
//                        sp.setLongueurMatelasPlanCoupe(longueur + 0.03);
//                    }
//                    sp.setLaizePlanCoupe(1.55);
//                    sp.setCategoryPlanCoupe("A");
//                    System.out.println("Saving 2 " + newPlacement);
//                    spreadingCuttingPlanCoupeRepository.save(sp);
//                    System.out.println("Saved 2 " + newPlacement);

                } catch(Exception e) {
                    System.out.println("FATAL ERROR : PLacement " +oldPlacement + " : " + e.getMessage());
                }

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


}
