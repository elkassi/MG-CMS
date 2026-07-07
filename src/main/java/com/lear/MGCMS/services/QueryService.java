package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.payload.*;
import com.lear.MGCMS.utils.UtilFunctions;
import org.apache.tomcat.jni.Local;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    private ApplicationContext context;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public QueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProdTicket findByLabelId(String idRouleau) {
        /*
        SELECT TOP (1000) [id]
      ,[description]
      ,[initQuantity]
      ,[labelId]
      ,[lotNr]
      ,[quantity]
      ,[reftissu]
      ,[tableName]
      ,[pls_id]
      ,[quantitePLS]
      ,[createdAt]
  FROM [MG_PLS_NEW].[dbo].[ProdTicket]
 WHERE labelId = 'S3338623'
ORDER BY quantity
         */

        DataSource cmsDataSource = (DataSource) context.getBean("plsDataSource");
        JdbcTemplate jdbcTemplatePls = new JdbcTemplate(cmsDataSource);


        String sql = "SELECT TOP (1) [id]\n" +
                "      ,[description]\n" +
                "      ,[initQuantity]\n" +
                "      ,[labelId]\n" +
                "      ,[lotNr]\n" +
                "      ,[quantity]\n" +
                "      ,[reftissu]\n" +
                "      ,[tableName]\n" +
                "      ,[pls_id]\n" +
                "      ,[quantitePLS]\n" +
                "      ,[createdAt]\n" +
                "  FROM [MG_PLS_NEW].[dbo].[ProdTicket]"
                + " WHERE labelId = '" + idRouleau + "'"
                + " AND createdAt >= DATEADD(HOUR, -72, GETDATE())"
                //and quantity > 0.01
                + " AND quantity > 0.1"
                + " ORDER BY quantity";
        //return only the first row if there are more than one
        List<ProdTicket> prodTickets = jdbcTemplatePls.query(sql, (resultSet, i) -> {
            ProdTicket prodTicket = new ProdTicket();
            prodTicket.setId(resultSet.getLong("id"));
            prodTicket.setDescription(resultSet.getString("description"));
            prodTicket.setInitQuantity(resultSet.getDouble("initQuantity"));
            prodTicket.setLabelId(resultSet.getString("labelId"));
            prodTicket.setLotNr(resultSet.getString("lotNr"));
            prodTicket.setQuantity(resultSet.getDouble("quantity"));
            prodTicket.setReftissu(resultSet.getString("reftissu"));
            prodTicket.setTableName(resultSet.getString("tableName"));
            prodTicket.setPlsId(resultSet.getString("pls_id"));
            prodTicket.setQuantitePLS(resultSet.getDouble("quantitePLS"));
            prodTicket.setCreatedAt(resultSet.getTimestamp("createdAt").toLocalDateTime());
            return prodTicket;
        });
        if (prodTickets.size() > 0) {
            return prodTickets.get(0);
        } else {
            return null;
        }
    }


    public int countCtcByPnsAndReftissu(List<String> pns, String reftissu) {
        /*
        SELECT  COUNT(*)
  FROM [dbo].[files] where (panel_number = 'PU' or panel_number = 'L')  and part_number_cover in ('L0596535AC01RKF', 'L0596535AC01RRD' , 'L0596533AD01RKF') and part_number_material = 'L0015969847RYAA'
         */
        // use the splice configuration  for jdbc
        DataSource cmsDataSource = (DataSource) context.getBean("spliceDataSource");
        JdbcTemplate jdbcTemplateSplice = new JdbcTemplate(cmsDataSource);

        String sql = "SELECT  COUNT(*)\n" +
                "  FROM [dbo].[files] where (panel_number = 'PU' or panel_number = 'LA')  and part_number_cover in (";
        for (int i = 0; i < pns.size(); i++) {
            sql += "'" + pns.get(i) + "'";
            if (i < pns.size() - 1) {
                sql += ", ";
            }
        }
        sql += ") and part_number_material = '" + reftissu + "'";
        return jdbcTemplateSplice.queryForObject(sql, Integer.class);
    }


    public List<String> getPartNumbersBySequence(String sequence) {
        /*
        SELECT[partNumber]
  FROM [dbo].[CuttingRequestPartNumber] where cuttingRequest_sequence = '010124064501'
         */
        sequence = sequence.replaceAll("[^\\d.]", "");
        String sql = "SELECT [partNumber]\n" +
                "  FROM [dbo].[CuttingRequestPartNumber] where cuttingRequest_sequence = '" + sequence + "'";
        return jdbcTemplate.query(sql, (resultSet, i) -> resultSet.getString("partNumber"));
    }


    public List<String> getReftissuAirbag() {
        /*
        SELECT [partNumberMaterial] FROM [dbo].[PartNumberMaterialConfig] where description like '%Airbag%'
         */
        String sql = "SELECT [partNumberMaterial] FROM [dbo].[PartNumberMaterialConfig] where description like '%Airbag%'";
        return jdbcTemplate.query(sql, (resultSet, i) -> resultSet.getString("partNumberMaterial"));

    }

    public String getPlsBySequenceAndReftissu(String sequence, String reftissu) {
        if (sequence != null) sequence = sequence.replaceAll("[^\\d.]", "");
        //reftissu could contain letters and numbers and special characters
        if (reftissu != null) reftissu = reftissu.replaceAll("[^a-zA-Z0-9-]", "");

        /*
SELECT [id]
  FROM [dbo].[Demande]
  where commentaire like
         */
        DataSource cmsDataSource = (DataSource) context.getBean("plsDataSource");
        JdbcTemplate jdbcTemplatePls = new JdbcTemplate(cmsDataSource);

        String sql = "SELECT [id]\n" +
                "  FROM [dbo].[Demande]\n" +
                // active is boolean must be true
                "  where active = 1 and " +
                " commentaire like '%" + sequence + "%' and commentaire like '%" + reftissu + "%' order by id desc";
        List<String> arr = jdbcTemplatePls.query(sql, (resultSet, i) -> resultSet.getString("id"));
        if (arr.size() > 0) {
            return arr.get(0);
        } else {
            return null;
        }
    }


    public Double getPrixItem(String item) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");

        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        /*
        SELECT [GL Cost] FROM [logistics].[dbo].[Rapport3_6_15_QAD] where [Item Number] = 'L001758018N1FAA'
         */
        String query = "SELECT [GL Cost] FROM [logistics].[dbo].[Rapport3_6_15_QAD] where [Item Number] = ?";
        // if not found return 0.0
//        try {
        List<Double> result = jdbcTemplateCMS.queryForList(query, new Object[]{item}, Double.class);
        if (result.size() == 0) {
            return 0.0;
        }
        return result.get(0);
//        } catch (Exception e) {
//            System.out.println("Error in QueryService.getPrixItem: " + e.getMessage());
//            return 0.0;
//        }
    }

    public List<ScheduleMachine> getScheduleMachine(LocalDate date, String shift) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");

        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        /*
        SELECT TOP (1000) [ID_Schedule_Machine]
      ,[ID_Machine_Schedule]
	  ,h.ligne
      ,[Date_Schedule_Machine]
      ,[Shift_Schedule_Machine]
      ,[Operate_Schedule_Machine]
      ,[Comment_Schedule_Machine]
      ,[CreatedDate_Schedule_Machine]
      ,[CreatedHour_Schedule_Machine]
      ,[UserName_Schedule_Machine]
      ,[HostName_Schedule_Machine]
      ,[Session_W_Schedule_Machine]
  FROM [dbo].[Schedule_Machine] as sm
  Left Join [dbo].hostname as h on h.id = sm.ID_Machine_Schedule
  where Date_Schedule_Machine = '2024-05-16' and Shift_Schedule_Machine = '1'
         */
        String query = "SELECT [ID_Schedule_Machine]\n" +
                "      ,[ID_Machine_Schedule]\n" +
                "	  ,h.ligne\n" +
                "      ,[Date_Schedule_Machine]\n" +
                "      ,[Shift_Schedule_Machine]\n" +
                "      ,[Operate_Schedule_Machine]\n" +
                "      ,[Comment_Schedule_Machine]\n" +
                "      ,[CreatedDate_Schedule_Machine]\n" +
                "      ,[CreatedHour_Schedule_Machine]\n" +
                "      ,[UserName_Schedule_Machine]\n" +
                "      ,[HostName_Schedule_Machine]\n" +
                "      ,[Session_W_Schedule_Machine]\n" +
                "  FROM [dbo].[Schedule_Machine] as sm\n" +
                "  Left Join [dbo].hostname as h on h.id = sm.ID_Machine_Schedule\n" +
                "  where Date_Schedule_Machine = ? and Shift_Schedule_Machine = ?";

        // if not found return empty list
        try {
            List<ScheduleMachine> result = jdbcTemplateCMS.query(query, new Object[]{date, shift}, (rs, rowNum) -> {
                ScheduleMachine scheduleMachine = new ScheduleMachine();
                scheduleMachine.setID_Schedule_Machine(rs.getInt("ID_Schedule_Machine"));
                scheduleMachine.setID_Machine_Schedule(rs.getInt("ID_Machine_Schedule"));
                scheduleMachine.setLigne(rs.getString("ligne"));
                scheduleMachine.setDate_Schedule_Machine(rs.getDate("Date_Schedule_Machine").toLocalDate());
                scheduleMachine.setShift_Schedule_Machine(rs.getString("Shift_Schedule_Machine"));
                scheduleMachine.setOperate_Schedule_Machine(rs.getBoolean("Operate_Schedule_Machine"));
                scheduleMachine.setComment_Schedule_Machine(rs.getString("Comment_Schedule_Machine"));
                scheduleMachine.setCreatedDate_Schedule_Machine(rs.getDate("CreatedDate_Schedule_Machine").toLocalDate());
                scheduleMachine.setCreatedHour_Schedule_Machine(rs.getTime("CreatedHour_Schedule_Machine").toLocalTime());
                scheduleMachine.setUserName_Schedule_Machine(rs.getString("UserName_Schedule_Machine"));
                scheduleMachine.setHostName_Schedule_Machine(rs.getString("HostName_Schedule_Machine"));
                scheduleMachine.setSession_W_Schedule_Machine(rs.getString("Session_W_Schedule_Machine"));
                return scheduleMachine;
            });
            return result;
        } catch (Exception e) {
            System.out.println("Error in QueryService.getScheduleMachine: " + e.getMessage());
            return Collections.emptyList();
        }

    }

    public List<Long> getCuttingPlanBySequence(List<String> pnList) {
        if (pnList == null || pnList.size() == 0) return Collections.emptyList();
        int size = pnList.size();
        /*
        SELECT cp.cuttingPlan_id
FROM [dbo].[CuttingPlanPartNumber] as cp
left Join [dbo].[CuttingPlan] as  c on c.id = cp.cuttingPlan_id
WHERE cp.partNumber IN ('L002483470NCPAC', 'L002483474NCPAC')
GROUP BY cp.cuttingPlan_id
HAVING COUNT(DISTINCT cp.partNumber) = 2
   AND COUNT(cp.partNumber) = (SELECT COUNT(*)
                               FROM [dbo].[CuttingPlanPartNumber] AS cp2
                               WHERE cp2.cuttingPlan_id = cp.cuttingPlan_id);
         */
        String sql = "SELECT cp.cuttingPlan_id\n" +
                "FROM [dbo].[CuttingPlanPartNumber] as cp\n" +
                "left Join [dbo].[CuttingPlan] as  c on c.id = cp.cuttingPlan_id\n" +
                "WHERE cp.partNumber IN (";
        for (int i = 0; i < size; i++) {
            sql += "'" + pnList.get(i) + "'";
            if (i < size - 1) {
                sql += ", ";
            }
        }
        sql += " )\n" +
                "GROUP BY cp.cuttingPlan_id\n" +
                "HAVING COUNT(DISTINCT cp.partNumber) = " + size + "\n" +
                "   AND COUNT(cp.partNumber) = (SELECT COUNT(*)\n" +
                "                               FROM [dbo].[CuttingPlanPartNumber] AS cp2\n" +
                "                               WHERE cp2.cuttingPlan_id = cp.cuttingPlan_id);";
        return jdbcTemplate.query(sql, (resultSet, i) -> resultSet.getLong("cuttingPlan_id"));
    }

    public void deleteRapportPlacementByCuttingPlanId(long cuttingPlanId) {
        DataSource cmsDataSource = (DataSource) context.getBean("plsDataSource");
        JdbcTemplate jdbcTemplatePls = new JdbcTemplate(cmsDataSource);
        //   Delete FROM [dbo].[CuttingPlanRapportPlacement] where cuttingPlan_id = 188190
        String sql = "Delete FROM [dbo].[CuttingPlanRapportPlacement] where cuttingPlan_id = ?";
        jdbcTemplatePls.update(sql, cuttingPlanId);
    }

    public Map<String, String> findMatriculeMapCMS() {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        /*
        select nomPrenom, matricule FROM [dbo].[utilisateur] where Departement = 'Ingenierie'
         */
        String sql = "select nomPrenom, matricule FROM [dbo].[utilisateur] where Departement = 'Ingenierie'";
        Map<String, String> result = new HashMap<>();
        jdbcTemplateCMS.query(sql, (resultSet, i) -> {
            result.put(resultSet.getString("nomPrenom"), resultSet.getString("matricule"));
            return null;
        });
        return result;
    }

    public List<FournisseurIMS> getNomFournisseur(List<String> lotList) {
        /*
           SELECT lot, MAX(fournisseur) AS fournisseur
FROM (
    SELECT lot, fournisseur
    FROM [dbo].[rouleau_serie]
    WHERE lot IN ('1117402', '1117403', '1117406', '1117478')
    UNION
    SELECT lot, fournisseur
    FROM [dbo].[rouleau]
    WHERE lot IN ('1117402', '1117403', '1117406', '1117478')
) AS combined
GROUP BY lot;

         */
        if (lotList == null || lotList.size() == 0) return Collections.emptyList();
        DataSource imsDataSource = (DataSource) context.getBean("imsDataSource");
        JdbcTemplate imsTemplateCMS = new JdbcTemplate(imsDataSource);

        String sql = "SELECT lot, MAX(fournisseur) AS fournisseur\n" +
                "FROM (\n" +
                "    SELECT lot, fournisseur\n" +
                "    FROM [dbo].[rouleau_serie]\n" +
                "    WHERE lot IN (";
        for (int i = 0; i < lotList.size(); i++) {
            sql += "'" + lotList.get(i) + "'";
            if (i < lotList.size() - 1) {
                sql += ", ";
            }
        }
        sql += ")\n" +
                "    UNION\n" +
                "    SELECT lot, fournisseur\n" +
                "    FROM [dbo].[rouleau]\n" +
                "    WHERE lot IN (";
        for (int i = 0; i < lotList.size(); i++) {
            sql += "'" + lotList.get(i) + "'";
            if (i < lotList.size() - 1) {
                sql += ", ";
            }
        }
        sql += ")\n" +
                ") AS combined\n" +
                "GROUP BY lot;";
        return imsTemplateCMS.query(sql, (resultSet, i) -> {
            FournisseurIMS fournisseurIMS = new FournisseurIMS();
            fournisseurIMS.setLot(resultSet.getString("lot"));
            fournisseurIMS.setNomFournisseur(resultSet.getString("fournisseur"));
            return fournisseurIMS;
        });

    }

    public Reference refDetails(String reftissu) {
        /*
        SELECT top 1
ref,
f.nom,
r.description
  FROM [dbo].[reference] as r
  Join [dbo].fournisseur as f on f.id = r.fournisseur_id
  where r.ref = '1000080022'
         */
        DataSource imsDataSource = (DataSource) context.getBean("imsDataSource");
        JdbcTemplate imsTemplateCMS = new JdbcTemplate(imsDataSource);

        String sql = "SELECT top 1 " +
                "ref, " +
                "f.nom, " +
                "r.description, " +
                "r.laize " +
                "  FROM [dbo].[reference] as r " +
                "  Join [dbo].fournisseur as f on f.id = r.fournisseur_id " +
                "  where r.ref = ?";
        List<Reference> result = imsTemplateCMS.query(sql, preparedStatement -> preparedStatement.setString(1, reftissu),
                (resultSet, i) -> {
                    Reference reference = new Reference();
                    reference.setRef(resultSet.getString("ref"));
                    reference.setFournisseur(resultSet.getString("nom"));
                    reference.setDescription(resultSet.getString("description"));
                    reference.setLaize(resultSet.getString("laize"));
                    return reference;
                });
        if (result.size() > 0) {
            return result.get(0);
        } else {
            return null;
        }

    }



    Double convertDigit(Double num, int digit) {
        return Double.parseDouble(String.format(("%."+digit+"f"), num).replace(",", "."));
    }

    /*
    SELECT [machineType] FROM [dbo].[ReftissuMachine] where partNumberMaterialConfig_partNumberMaterial = '1000050173'
     */
    public List<String> getAllowedMachinesByReftissu(String reftissu) {
        String sql = "SELECT [machineType] FROM [dbo].[ReftissuMachine] WHERE partNumberMaterialConfig_partNumberMaterial = ?";
        return jdbcTemplate.query(sql,
                new Object[]{reftissu},
                (rs, rowNum) -> rs.getString("machineType"));
    }
    /*
               SELECT
   crs.cuttingRequest_sequence,
   Min(
       CASE
           WHEN crs.dateDebutMatelassage IS NOT NULL THEN crs.dateDebutMatelassage
           ELSE NULL
       END
   ) as dateDebutMatelassage,
   MAX(crs.dateFinMatelassage) as dateFinMatelassage,
   Min(
       CASE
           WHEN crs.dateDebutCoupe IS NOT NULL THEN crs.dateDebutCoupe
           ELSE NULL
       END
   ) as dateDebutCoupe,
   MAX(crs.dateFinCoupe) as dateFinCoupe,
   confirmReftissu,
   MAX(crs.description) as description,
   Sum(crsr.nbrCouche * crsr.longueurPremierCouche + COALESCE(crsr.longueurCoucheOverlap, 0)) as totalConsommationPlan,
   SUM(
       COALESCE(overlap1, 0) +
       COALESCE(overlap2, 0) +
       COALESCE(overlap3, 0) +
       COALESCE(overlap4, 0) +
       COALESCE(overlap5, 0) +
       COALESCE(overlap6, 0) +
       COALESCE(overlap7, 0) +
       COALESCE(overlap8, 0)
   ) AS Overlap,
   Sum(COALESCE(crsr.nonUtitlse, 0)) as nonUtitlse,
   Sum(COALESCE(crsr.defaut, 0)) as defaut,
   SUM(COALESCE(crsr.totalUsage, 0)) as totalUsage,
   SUM(COALESCE(crsr.excess, 0)) as excess,
   MAX(cr.cuttingPlanId) as cuttingPlanId,
   MAX(cpm.qadUsage) as qadUsage
   FROM [dbo].CuttingRequestSerieRouleau as crsr
   JOIN MG_CMS.dbo.CuttingRequestSerie as crs on crs.serie = crsr.cuttingRequestSerie_serie
   JOIN MG_CMS.dbo.CuttingRequest as cr on cr.sequence = crs.cuttingRequest_sequence
   JOIN [dbo].[CuttingPlanMaterial] as cpm on cpm.cuttingPlan_id = cr.cuttingPlanId and cpm.[partNumberMaterial] = confirmReftissu
   WHERE crs.planningDate = '2024-10-14' and crs.shift = 2
   GROUP BY crs.cuttingRequest_sequence, confirmReftissu
            */
    public List<RapportUsage> rapportUsage(LocalDate date, String shift, String reftissu) {
        LocalDateTime dateStart = null;
        LocalDateTime dateEnd = null;
        if (shift != null) {
            switch (shift) {
                case "1":
                    dateStart = date.minusDays(1).atTime(21, 50);
                    dateEnd = date.atTime(5, 50);
                    break;
                case "2":
                    dateStart = date.atTime(5, 50);
                    dateEnd = date.atTime(13, 50);
                    break;
                default:
                    dateStart = date.atTime(13, 50);
                    dateEnd = date.atTime(21, 50);
                    break;
            }
        } else {
            dateStart = date.minusDays(1).atTime(21, 50);
            dateEnd = date.atTime(21, 50);
        }

        return queryRapportUsage(reftissu, dateStart, dateEnd);
    }

    private List<RapportUsage> queryRapportUsage(String reftissu, LocalDateTime dateStart, LocalDateTime dateEnd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS");

        String sql = "SELECT \n" +
                "crs.cuttingRequest_sequence, \n" +
                "MIN(CASE WHEN crs.dateDebutMatelassage IS NOT NULL THEN crs.dateDebutMatelassage ELSE NULL END) AS dateDebutMatelassage, \n" +
                "MAX(crs.dateFinMatelassage) AS dateFinMatelassage, \n" +
                "MIN(CASE WHEN crs.dateDebutCoupe IS NOT NULL THEN crs.dateDebutCoupe ELSE NULL END) AS dateDebutCoupe, \n" +
                "MAX(crs.dateFinCoupe) AS dateFinCoupe, \n" +
                "confirmReftissu, \n" +
                "MAX(crs.description) AS description, \n" +
                "SUM(crsr.nbrCouche * crsr.longueurPremierCouche + COALESCE(crsr.longueurCoucheOverlap, 0)) AS totalConsommationPlan, \n" +
                "SUM(COALESCE(overlap1, 0) + COALESCE(overlap2, 0) + COALESCE(overlap3, 0) + COALESCE(overlap4, 0) + \n" +
                "COALESCE(overlap5, 0) + COALESCE(overlap6, 0) + COALESCE(overlap7, 0) + COALESCE(overlap8, 0)) AS Overlap, \n" +
                "SUM(COALESCE(crsr.nonUtitlse, 0)) AS nonUtitlse, \n" +
                "SUM(COALESCE(crsr.defaut, 0)) AS defaut, \n" +
                "SUM(COALESCE(crsr.totalUsage, 0)) AS totalUsage, \n" +
                "SUM(COALESCE(crsr.excess, 0)) AS excess, \n" +
                "MAX(cr.cuttingPlanId) AS cuttingPlanId, \n" +
                "MAX(crs.statusMatelassage) AS statusMatelassage \n" +
                "FROM [dbo].CuttingRequestSerieRouleau AS crsr \n" +
                "JOIN dbo.CuttingRequestSerie AS crs ON crs.serie = crsr.cuttingRequestSerie_serie \n" +
                "JOIN dbo.CuttingRequest AS cr ON cr.sequence = crs.cuttingRequest_sequence  \n"
//                "WHERE crs.dateFinMatelassage >= '" + dateStart.format(formatter) + "' " +
//                "AND crs.dateFinMatelassage <= '" + dateEnd.format(formatter) + "'"
                ;

        if (reftissu != null && !reftissu.isEmpty()) {
            reftissu = reftissu.replaceAll("[^a-zA-Z0-9-]", "");
            sql += " WHERE confirmReftissu = '" + reftissu + "'  \n";
        }
        sql += " GROUP BY crs.cuttingRequest_sequence, confirmReftissu  \n";
        if (dateStart != null && dateEnd != null) {
            sql += " HAVING MAX(crs.dateFinMatelassage) >= '" + dateStart.format(formatter) + "' " + // MAX(crs.statusMatelassage) = 'Complete' and
                    "AND MAX(crs.dateFinMatelassage) <= '" + dateEnd.format(formatter) + "'  \n";
        }

        System.out.println(sql);
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            RapportUsage rapportUsage = new RapportUsage();
            rapportUsage.setCuttingPlanId(resultSet.getLong("cuttingPlanId"));
            rapportUsage.setCuttingRequest_sequence(resultSet.getString("cuttingRequest_sequence"));
            if (resultSet.getTimestamp("dateDebutMatelassage") != null) {
                rapportUsage.setDateDebutMatelassage(resultSet.getTimestamp("dateDebutMatelassage").toLocalDateTime());
            }
            if (resultSet.getTimestamp("dateFinMatelassage") != null) {
                rapportUsage.setDateFinMatelassage(resultSet.getTimestamp("dateFinMatelassage").toLocalDateTime());
            }
            if (resultSet.getTimestamp("dateDebutCoupe") != null) {
                rapportUsage.setDateDebutCoupe(resultSet.getTimestamp("dateDebutCoupe").toLocalDateTime());
            }
            if (resultSet.getTimestamp("dateFinCoupe") != null) {
                rapportUsage.setDateFinCoupe(resultSet.getTimestamp("dateFinCoupe").toLocalDateTime());
            }
            rapportUsage.setConfirmReftissu(resultSet.getString("confirmReftissu"));
            rapportUsage.setDescription(resultSet.getString("description"));
            rapportUsage.setTotalConsommationPlan(UtilFunctions.convertTwoDigit(resultSet.getDouble("totalConsommationPlan"), 3));
            rapportUsage.setOverlap(UtilFunctions.convertTwoDigit(resultSet.getDouble("Overlap"), 3));
            rapportUsage.setNonUtitlse(UtilFunctions.convertTwoDigit(resultSet.getDouble("nonUtitlse"), 3));
            rapportUsage.setDefaut(UtilFunctions.convertTwoDigit(resultSet.getDouble("defaut"), 3));
            rapportUsage.setTotalUsage(UtilFunctions.convertTwoDigit(resultSet.getDouble("totalUsage"), 3));
            rapportUsage.setExcess(UtilFunctions.convertTwoDigit(resultSet.getDouble("excess"), 3));
            rapportUsage.setFinalUsage(UtilFunctions.convertTwoDigit(rapportUsage.getTotalUsage() - rapportUsage.getExcess(), 3));
            rapportUsage.setStatusMatelassage(resultSet.getString("statusMatelassage"));
            return rapportUsage;
        });
    }

    public List<RapportBom> findBomByItemParent(String itemParent) {
        /*
        select Item_Parent, Component, [Quantity Per], Scrap
FROM [MATNR-APP01].[logistics].[dbo].[Rapport9_4_25_BOM_QAD]
where Item_Parent
         */
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        String sql = "select Item_Parent, Component, [Quantity Per], Scrap " +
                "FROM [MATNR-APP01].[logistics].[dbo].[Rapport9_4_25_BOM_QAD]  " +
                "where Item_Parent = ?";
        return jdbcTemplateCMS.query(sql, new Object[]{itemParent}, (resultSet, i) -> {
            RapportBom rapportBom = new RapportBom();
            rapportBom.setItemParent(resultSet.getString("Item_Parent"));
            rapportBom.setComponent(resultSet.getString("Component"));
            rapportBom.setQuantityPer(resultSet.getDouble("Quantity Per"));
            rapportBom.setScrap(resultSet.getDouble("Scrap"));
            return rapportBom;
        });

    }

    public List<QualityValidationReport> getQualityValidationReport() {
        /*
        SELECT TOP (1000) qv.serie, qv.date, qv.machine, qv.reftissu, crs.dateDebutCoupe, crs.tableCoupe
  FROM [dbo].QualityValidationHistory as qv
  JOIN  [dbo].CuttingRequestSerie as crs on crs.serie = qv.serie
  where dateDebutCoupe >= '2024-12-21 12:05:50.8900000' and dateDebutCoupe <= '2024-12-21 12:15:50.8900000'
  order by crs.dateDebutCoupe desc
         */
        LocalDateTime date1 = LocalDateTime.now();
        LocalDateTime date2 = LocalDateTime.now().minusMinutes(10);

        String sql = "SELECT qv.serie, qv.date, qv.machine, qv.reftissu, crs.dateDebutCoupe, crs.tableCoupe\n" +
                "  FROM [dbo].QualityValidationHistory as qv\n" +
                "  JOIN  [dbo].CuttingRequestSerie as crs on crs.serie = qv.serie \n" +
                "  where dateDebutCoupe >= ? and dateDebutCoupe <= ? \n" +
                "  order by crs.dateDebutCoupe desc";

        return jdbcTemplate.query(sql,
                new Object[]{date1, date2},
                (resultSet, i) -> {
                    QualityValidationReport obj = new QualityValidationReport();
                    obj.setSerie(resultSet.getString("serie"));
                    obj.setDate(resultSet.getTimestamp("date").toLocalDateTime());
                    obj.setMachine(resultSet.getString("machine"));
                    obj.setReftissu(resultSet.getString("reftissu"));
                    obj.setDateDebutCoupe(resultSet.getTimestamp("dateDebutCoupe").toLocalDateTime());
                    obj.setTableCoupe(resultSet.getString("tableCoupe"));
                    return obj;
                }
        );
    }

    public List<RapportExcess> rapportExcess(String reftissu) {
        String sql = "  select lotFrs, SUM(excess) as excess, min(createdAt) as minDate, MAX(createdAt) as maxDate" +
                " FROM [dbo].[CuttingRequestSerieRouleau]" +
                " where confirmReftissu = ?" +
                " group by confirmReftissu, lotFrs" +
                " order by SUM(excess)";
        return jdbcTemplate.query(sql,
                new Object[]{reftissu},
                (resultSet, i) -> {
                    RapportExcess obj = new RapportExcess();
                    obj.setLotFrs(resultSet.getString("lotFrs"));
                    obj.setExcess(convertDigit(resultSet.getDouble("excess"), 3));
                    obj.setMaxDate(resultSet.getTimestamp("maxDate") != null ? resultSet.getTimestamp("maxDate").toLocalDateTime() : null);
                    obj.setMinDate(resultSet.getTimestamp("minDate") != null ? resultSet.getTimestamp("minDate").toLocalDateTime() : null);
                    return obj;
                }
        );
    }

    public void deleteBySequenceCMS(String sequence) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);

        String[] sqlStatements = {
                "UPDATE dbo.Order_Schedule " +
                        "SET Status_Demande = 'F', Marker_ID = null " +
                        "FROM dbo.Order_Schedule AS ord " +
                        "JOIN dbo.Asprova_WO AS wo ON wo.[ID_Order_Schedule] = ord.ID_Demande " +
                        "JOIN dbo.suiviplanning AS suivi ON suivi.id = wo.ID_ItemNumber_Asprova_WO " +
                        "WHERE suivi.NSequence = ? ",
                "DELETE FROM [dbo].[matlassage] WHERE NOF = ?",
                "DELETE FROM [dbo].suivimatelassage WHERE NOF = ?",
                "DELETE FROM [dbo].coupe WHERE NOF = ?",
                "DELETE FROM [dbo].suivicoupe WHERE NOF = ?",
                "DELETE FROM [dbo].suiviplanning WHERE NSequence = ?",
                "DELETE FROM [dbo].produitfinit WHERE NSequence = ?",
                "UPDATE [dbo].GammeTechniqueImprimer SET NSequenceImp = '' WHERE NSequenceImp = ?",
                "with cte as ( " +
                        " Select " +
                        "      [ID_ItemNumber_Asprova_WO] ,  ID_Order_Schedule " +
                        " from dbo.suiviplanning  " +
                        " LEFT JOIn dbo.Asprova_WO " +
                        " ON Asprova_WO.[ID_ItemNumber_Asprova_WO] = suiviplanning.id " +
                        " where NSequence = ? " +
                        ") " +
                        "delete from [dbo].[Asprova_WO] " +
                        "where ID_Order_Schedule IN ( Select cte.ID_Order_Schedule from cte where cte.[ID_Order_Schedule] = [ID_Order_Schedule]) "
        };

        for (String sql : sqlStatements) {
            jdbcTemplateCMS.update(sql, sequence);
        }
    }

    public void deleteBySequenceCMSWEB(String sequence) {
        String[] sqlStatements = {
                "DELETE FROM [dbo].[CuttingRequestSerie] WHERE cuttingRequest_sequence = ?",
                "DELETE FROM [dbo].[CuttingRequestPartNumber] WHERE cuttingRequest_sequence = ?",
                "DELETE FROM [dbo].[CuttingRequestBox] WHERE cuttingRequest_sequence = ?",
                "DELETE FROM [dbo].[CuttingRequest] WHERE sequence = ?"
        };

        for (String sql : sqlStatements) {
            jdbcTemplate.update(sql, sequence);
        }
    }

    public void initialiserBySequenceCMS(String sequence) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);

        String[] sqlStatements = {
                "UPDATE dbo.suiviplanning SET [Statu] = 'Non demarre' WHERE nsequence = ? and statu = 'Released' ",
        };

        for (String sql : sqlStatements) {
            jdbcTemplateCMS.update(sql, sequence);
        }
    }

    public void deleteSerieCMS(String serie) {
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);

        String[] sqlStatements = {
                "DELETE FROM [dbo].[matlassage] WHERE NSERIE = ?",
                "DELETE FROM [dbo].suivimatelassage WHERE NSERIE = ?",
                "DELETE FROM [dbo].coupe WHERE NSERIE = ?",
                "DELETE FROM [dbo].suivicoupe WHERE NSERIE = ?",
        };

        for (String sql : sqlStatements) {
            jdbcTemplateCMS.update(sql, serie);
        }

    }

    public List<String> nonCompleted(String reftissu) {
        String sql = "select cuttingRequest_sequence+ '-'+ partNumberMaterial as ID " +
                "from dbo.CuttingRequestSerie  " +
                "where statusMatelassage != 'Complete' and statusMatelassage != 'Incomplete' ";
        if (reftissu != null && reftissu.length() > 0) {
            reftissu = reftissu.replaceAll("[^a-zA-Z0-9-]", "");
            sql += " and partNumberMaterial = '" + reftissu+ "'";
        }

        sql += "group by cuttingRequest_sequence, partNumberMaterial " +
                "order by cuttingRequest_sequence, partNumberMaterial ";
        return jdbcTemplate.query(sql,
                new Object[]{},
                (rs, rowNum) -> rs.getString("ID"));

    }

    public ResponseEntity<?> updateTolerence(String projet, String type, Double min1, Double max1, Double t2min1, Double t2max1) {
        DataSource ctcDataSource = (DataSource) context.getBean("ctcDataSource");
        JdbcTemplate jdbcTemplateSplice = new JdbcTemplate(ctcDataSource);

        List<String> whereCondition = new ArrayList<>();
        List<String> setCondition = new ArrayList<>();
        List<Object> params = new ArrayList<>();



        if (min1 != null && max1 != null) {
            setCondition.add("tol_min1 = ?");
            setCondition.add("tol_max1 = ?");
            params.add(min1);
            params.add(max1);
        }

        if (t2min1 != null && t2max1 != null) {
            setCondition.add("tol_min2 = ?");
            setCondition.add("tol_max2 = ?");
            params.add(t2min1);
            params.add(t2max1);
        }

        if (projet != null && !projet.isEmpty()) {
            whereCondition.add("projet = ?");
            params.add(projet);
        }

        if (type != null && !type.isEmpty()) {
            whereCondition.add("type = ?");
            params.add(type);
        }


        if (setCondition.isEmpty() || whereCondition.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid input parameters.");
        }

        String sql = "UPDATE [dbo].[files] SET " + String.join(", ", setCondition) + " WHERE " + String.join(" AND ", whereCondition);
        System.out.println(sql);
        try {
            int rowsAffected = jdbcTemplateSplice.update(sql, params.toArray());
            if (rowsAffected > 0) {
                return ResponseEntity.ok("Tolerance updated successfully. Rows : "+ rowsAffected);
            } else {
                return ResponseEntity.status(404).body("No matching records found.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating tolerance: " + e.getMessage());
        }

    }

    public List<CuttingRequestData> notCompletedOne(String sequence) {

        LocalDateTime now = LocalDateTime.now();
        String sql = "select crs.cuttingRequest_sequence as cuttingRequest_sequence" +
                ",max([cuttingPlanId]) as cuttingPlanId" +
                ",max([definition]) as definition" +
                ",max([modele]) as modele" +
                ",max(cr.[planningDate]) as planningDate" +
                ",max([projet]) as projet" +
                ",max(cr.[shift]) as shift" +
                ",max([version]) as version" +
                ",max(cr.[createdAt]) as createdAt " +
                ", max(wo.dueDate) as dueDate, max(wo.shift) as dueShift " +
                "FROM [dbo].[CuttingRequestSerie] as crs " +
                "Join [dbo].[CuttingRequest] as cr on cr.sequence = crs.cuttingRequest_sequence " +
                "Join [dbo].[Projet] as pj on pj.nom = cr.projet " +
                "Join [dbo].[CuttingRequestBox] as crb on cr.sequence = crb.cuttingRequest_sequence " +
                "Join [dbo].[WorkOrder] as wo on wo.wo = crb.wo " +
                "where crs.cuttingRequest_sequence = ? "+
                "group by crs.cuttingRequest_sequence";


        System.out.println(sql);
        return jdbcTemplate.query(sql,
                new Object[]{sequence},
                (resultSet, i) -> {
                    CuttingRequestData cuttingRequestData = new CuttingRequestData();
                    cuttingRequestData.setSequence(resultSet.getString("cuttingRequest_sequence"));
                    cuttingRequestData.setCuttingPlanId(resultSet.getLong("cuttingPlanId"));
                    cuttingRequestData.setDefinition(resultSet.getString("definition"));
                    cuttingRequestData.setModele(resultSet.getString("modele"));
                    cuttingRequestData.setPlanningDate(resultSet.getDate("planningDate") != null ? resultSet.getDate("planningDate").toLocalDate() : null);
                    cuttingRequestData.setProjet(resultSet.getString("projet"));
                    cuttingRequestData.setShift(resultSet.getString("shift"));
                    cuttingRequestData.setVersion(resultSet.getString("version"));
                    cuttingRequestData.setCreatedAt(resultSet.getTimestamp("createdAt") != null ? resultSet.getTimestamp("createdAt").toLocalDateTime() : null);
                    cuttingRequestData.setDueDate(resultSet.getDate("dueDate") != null ? resultSet.getDate("dueDate").toLocalDate() : null);
                    cuttingRequestData.setDueShift(resultSet.getString("dueShift"));
                    return cuttingRequestData;
                });

    }

    public List<CuttingRequestData> notCompletedSequenced(String zoneName, List<String> tables , LocalDateTime minDate) {
        /*
        select cuttingRequest_sequence
      ,max([cuttingPlanId])
      ,max([definition])
      ,max([modele])
      ,max(cr.[planningDate])
      ,max([projet])
      ,max(cr.[shift])
      ,max([version])
      ,max(cr.[createdAt])
  FROM [dbo].[CuttingRequestSerie] as crs
  Join [dbo].[CuttingRequest] as cr on cr.sequence = crs.cuttingRequest_sequence
where statusCoupe != 'Complete' and (crs.tableCoupe in ('AA9', 'AA6', 'AA7', 'AA8', 'AA5') or cr.zone_nom = 'Nejma')
  group by cuttingRequest_sequence
         */
        LocalDateTime now = LocalDateTime.now();
        String sql = "select crs.cuttingRequest_sequence as cuttingRequest_sequence" +
                ",max([cuttingPlanId]) as cuttingPlanId" +
                ",max([definition]) as definition" +
                ",max([modele]) as modele" +
                ",max(cr.[planningDate]) as planningDate" +
                ",max([projet]) as projet" +
                ",max(cr.[shift]) as shift" +
                ",max([version]) as version" +
                ",max(cr.[createdAt]) as createdAt " +
                ", max(wo.dueDate) as dueDate, max(wo.shift) as dueShift " +
                "FROM [dbo].[CuttingRequestSerie] as crs " +
                "Join [dbo].[CuttingRequest] as cr on cr.sequence = crs.cuttingRequest_sequence " +
                "Join [dbo].[Projet] as pj on pj.nom = cr.projet " +
                "Join [dbo].[CuttingRequestBox] as crb on cr.sequence = crb.cuttingRequest_sequence " +
                "Join [dbo].[WorkOrder] as wo on wo.wo = crb.wo " +
                "where (" +
                "crs.cuttingRequest_sequence like '"+now.format(DateTimeFormatter.ofPattern("ddMMyy"))+"%' or " +
                "crs.cuttingRequest_sequence like '"+now.minusDays(1).format(DateTimeFormatter.ofPattern("ddMMyy"))+"%' or " +
                "dateDebutMatelassage > '" +minDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS"))+"'" +
                ") and "+
//                "where (statusCoupe != 'Complete' or dateDebutMatelassage > '" + minDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS")) + "') and " +
                "(pj.zone_nom = ? or " +
                "crs.tableCoupe in (";
        for (int i = 0; i < tables.size(); i++) {
            sql += "'" + tables.get(i) + "'";
            if (i != tables.size() - 1) {
                sql += ", ";
            }
        }
        sql += ")) "+
                "group by crs.cuttingRequest_sequence " +
                "order by max(wo.dueDate), max(wo.shift)";


//        System.out.println(sql);
        return jdbcTemplate.query(sql,
                new Object[]{zoneName},
                (resultSet, i) -> {
                    CuttingRequestData cuttingRequestData = new CuttingRequestData();
                    cuttingRequestData.setSequence(resultSet.getString("cuttingRequest_sequence"));
                    cuttingRequestData.setCuttingPlanId(resultSet.getLong("cuttingPlanId"));
                    cuttingRequestData.setDefinition(resultSet.getString("definition"));
                    cuttingRequestData.setModele(resultSet.getString("modele"));
                    cuttingRequestData.setPlanningDate(resultSet.getDate("planningDate") != null ? resultSet.getDate("planningDate").toLocalDate() : null);
                    cuttingRequestData.setProjet(resultSet.getString("projet"));
                    cuttingRequestData.setShift(resultSet.getString("shift"));
                    cuttingRequestData.setVersion(resultSet.getString("version"));
                    cuttingRequestData.setCreatedAt(resultSet.getTimestamp("createdAt") != null ? resultSet.getTimestamp("createdAt").toLocalDateTime() : null);
                    cuttingRequestData.setDueDate(resultSet.getDate("dueDate") != null ? resultSet.getDate("dueDate").toLocalDate() : null);
                    cuttingRequestData.setDueShift(resultSet.getString("dueShift"));
                    return cuttingRequestData;
                });
    }

    public List<StatsInfo2> getStatsBySequences(List<String> sequencesArr) {
        /*
        select cuttingRequest_sequence, count(*), sum([qtyBox])
  FROM [dbo].[CuttingRequestBox] as crs
  group by cuttingRequest_sequence

  public class StatsInfo2 {

	private String info;
	private Long value1;
	private Long value2;
}
         */
        String sql = "SELECT cuttingRequest_sequence, " +
                "count(*) as count, " +
                "sum([qtyBox]) as sumQtyBox " +
                "FROM [dbo].[CuttingRequestBox] as crs " +
                "where cuttingRequest_sequence in (";
        for (int i = 0; i < sequencesArr.size(); i++) {
            sql += "'" + sequencesArr.get(i) + "'";
            if(i != sequencesArr.size() - 1) {
                sql += ", ";
            }
        }

        sql += ") " +
                "group by cuttingRequest_sequence";
        return jdbcTemplate.query(sql,
                new Object[]{},
                (resultSet, i) -> {
                    return new StatsInfo2(
                            resultSet.getString("cuttingRequest_sequence"),
                            resultSet.getLong("count"),
                            resultSet.getLong("sumQtyBox")
                    );
                });
    }

    public List<TimingPlacement> getTimingPlacement(List<String> placements) {
        if (placements == null || placements.isEmpty()) return Collections.emptyList();
        // Récupération du datasource quality
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);

        StringBuilder sql = new StringBuilder("SELECT [Placement_Timing_Model], [Validated_Cutting_time_Timing_Model], [Cutting_time_Timing_Model], [Spreading_Timing_Model] FROM [dbo].[Timing_Model] WHERE [Placement_Timing_Model] IN (");
        for (int i = 0; i < placements.size(); i++) {
            sql.append("?");
            if (i < placements.size() - 1) sql.append(",");
        }
        sql.append(")");

        return jdbcTemplateCMS.query(sql.toString(), placements.toArray(), (rs, rowNum) -> {
            TimingPlacement tp = new TimingPlacement();
            tp.setPlacementTimingModel(rs.getString("Placement_Timing_Model"));
            tp.setValidatedCuttingTimeTimingModel(rs.getDouble("Validated_Cutting_time_Timing_Model"));
            tp.setCuttingTimeTimingModel(rs.getDouble("Cutting_time_Timing_Model"));
            tp.setSpreadingTimingModel(rs.getDouble("Spreading_Timing_Model"));
            return tp;
        });
    }


    public Integer getPackaginFromCMS(String partNumber) {
                            /*
                    SELECT Packaging
  FROM [dbo].[GammeTechnique]
  where PartNumber =
  Packaging is string so convert it to Integer
                     */
        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
        String sql = "SELECT Packaging " +
                "FROM [dbo].[GammeTechnique] " +
                "where PartNumber = ?";
        return jdbcTemplateCMS.queryForObject(sql, new Object[]{partNumber}, (resultSet, i) -> {
            String packagingStr = resultSet.getString("Packaging");
            if (packagingStr != null && !packagingStr.isEmpty()) {
                try {
                    return Integer.parseInt(packagingStr);
                } catch (NumberFormatException e) {
                    // Log the error or handle it as needed
                    return null;
                }
            }
            return null;
        });
    }

    public List<String> findMaterialsOfItems(List<String> itemNumbersWithF) {
        /*
        SELECT obj2.[Component], COUNT(*), STRING_AGG(obj2.Item_Parent, ',') AS All_Item_Parents
FROM [logistics].[dbo].[Rapport9_4_25_BOM_QAD] AS obj2
WHERE obj2.Item_Parent in ('WL002476997W51AD', 'WL002476916W51AD', 'WL002905291CZKAC', 'WL002905345CXGAC', 'WL002905395CXGAC', 'WL002905293CXGAC', 'WL002905292CXGAC')
  AND EXISTS (
    SELECT 1
    FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] AS im2
    WHERE im2.[Item Number] = obj2.[Component]
      AND im2.[Unit of Measure] = 'MT'
      AND im2.[Prod_Line] = 'R100'
  )
  group by obj2.[Component]

         */

        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);

        String sql = "SELECT obj2.[Component] " +
                "FROM [logistics].[dbo].[Rapport9_4_25_BOM_QAD] AS obj2 " +
                "WHERE obj2.Item_Parent IN (";
        for (int i = 0; i < itemNumbersWithF.size(); i++) {
            sql += "'" + itemNumbersWithF.get(i) + "'";
            if (i < itemNumbersWithF.size() - 1) {
                sql += ", ";
            }
        }
        sql += ") " +
                "AND EXISTS ( " +
                "SELECT 1 " +
                "FROM [logistics].[dbo].[Rapport9_4_25_ItemMaster_QAD] AS im2 " +
                "WHERE im2.[Item Number] = obj2.[Component] " +
                "AND im2.[Unit of Measure] = 'MT' " +
                "AND im2.[Prod_Line] = 'R100' " +
                ") " +
                "GROUP BY obj2.[Component]";
        System.out.println(sql);
        return jdbcTemplateCMS.query(sql, new Object[]{}, (resultSet, i) -> {
            return resultSet.getString("Component");
        });
    }

    public List<String> getPlacementWithoutQuantity() {
        /*
        SELECT placement
  FROM [LEAR_MG_CMS].[dbo].[CuttingRequestSerie]
  where nbrPiece is null Group by placement
         */
        String sql = "SELECT placement " +
                "FROM [dbo].[CuttingRequestSerie] " +
                "where (nbrPiece is null or nbrPiece = 0) Group by placement";
        return jdbcTemplate.query(sql, new Object[]{}, (resultSet, i) -> {
            return resultSet.getString("placement");
        });
    }

    public void updateQtyOfPlacement(String placement, Integer qty) {
        try {
            String sql = "UPDATE [dbo].[CuttingRequestSerie] " +
                    // nbrPieceTotal = nbrPiece * nbrCouche
                    "SET nbrPiece = ?, nbrPieceTotal= nbrCouche * ? " +
                    "WHERE placement = ? and (nbrPiece is null or nbrPiece = 0)";
            jdbcTemplate.update(sql, qty, qty, placement);
        } catch(Exception e) {
            log.error("QueryService updateQtyOfPlacement failed for placement={}", placement, e);
        }
    }

    /**
     * Get all not-completed sequences for ALL zones in one query
     * This is optimized for OrdonnancementV3 multi-zone scheduling to reduce API calls
     * @param minDate minimum date threshold
     * @return Map<zoneName, List<CuttingRequestData>>
     */
    public Map<String, List<CuttingRequestData>> notCompletedAllZones(LocalDateTime minDate) {
        LocalDateTime now = LocalDateTime.now();
        String sql = "SELECT crs.cuttingRequest_sequence as cuttingRequest_sequence" +
                ",max([cuttingPlanId]) as cuttingPlanId" +
                ",max([definition]) as definition" +
                ",max([modele]) as modele" +
                ",max(cr.[planningDate]) as planningDate" +
                ",max([projet]) as projet" +
                ",max(cr.[shift]) as shift" +
                ",max([version]) as version" +
                ",max(cr.[createdAt]) as createdAt " +
                ",max(wo.dueDate) as dueDate, max(wo.shift) as dueShift " +
                ",max(pj.zone_nom) as zoneName " +
                "FROM [dbo].[CuttingRequestSerie] as crs " +
                "JOIN [dbo].[CuttingRequest] as cr on cr.sequence = crs.cuttingRequest_sequence " +
                "JOIN [dbo].[Projet] as pj on pj.nom = cr.projet " +
                "JOIN [dbo].[CuttingRequestBox] as crb on cr.sequence = crb.cuttingRequest_sequence " +
                "JOIN [dbo].[WorkOrder] as wo on wo.wo = crb.wo " +
                "WHERE (" +
                "crs.cuttingRequest_sequence like '"+now.format(DateTimeFormatter.ofPattern("ddMMyy"))+"%' or " +
                "crs.cuttingRequest_sequence like '"+now.minusDays(1).format(DateTimeFormatter.ofPattern("ddMMyy"))+"%' or " +
                "dateDebutMatelassage > '" +minDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS"))+"'" +
                ") " +
                "GROUP BY crs.cuttingRequest_sequence " +
                "ORDER BY max(wo.dueDate), max(wo.shift)";

        List<CuttingRequestData> allResults = jdbcTemplate.query(sql,
                new Object[]{},
                (resultSet, i) -> {
                    CuttingRequestData cuttingRequestData = new CuttingRequestData();
                    cuttingRequestData.setSequence(resultSet.getString("cuttingRequest_sequence"));
                    cuttingRequestData.setCuttingPlanId(resultSet.getLong("cuttingPlanId"));
                    cuttingRequestData.setDefinition(resultSet.getString("definition"));
                    cuttingRequestData.setModele(resultSet.getString("modele"));
                    cuttingRequestData.setPlanningDate(resultSet.getDate("planningDate") != null ? resultSet.getDate("planningDate").toLocalDate() : null);
                    cuttingRequestData.setProjet(resultSet.getString("projet"));
                    cuttingRequestData.setShift(resultSet.getString("shift"));
                    cuttingRequestData.setVersion(resultSet.getString("version"));
                    cuttingRequestData.setCreatedAt(resultSet.getTimestamp("createdAt") != null ? resultSet.getTimestamp("createdAt").toLocalDateTime() : null);
                    cuttingRequestData.setDueDate(resultSet.getDate("dueDate") != null ? resultSet.getDate("dueDate").toLocalDate() : null);
                    cuttingRequestData.setDueShift(resultSet.getString("dueShift"));
                    cuttingRequestData.setZoneName(resultSet.getString("zoneName"));
                    return cuttingRequestData;
                });

        // Group by zone name
        Map<String, List<CuttingRequestData>> result = new HashMap<>();
        for (CuttingRequestData data : allResults) {
            String zone = data.getZoneName();
            if (zone != null) {
                result.computeIfAbsent(zone, k -> new ArrayList<>()).add(data);
            }
        }
        return result;
    }

    /**
     * Rapport des matières Airbag
     * Returns data from PLS database for airbag materials
     */
    public List<Map<String, Object>> rapportMatieresAirbag(LocalDate startDate, LocalDate endDate, String partNumberMaterial) {
        DataSource plsDataSource = (DataSource) context.getBean("plsDataSource");
        JdbcTemplate jdbcTemplatePls = new JdbcTemplate(plsDataSource);

        // Default to current year if no dates provided
        if (startDate == null) {
            startDate = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        }
        if (endDate == null) {
            endDate = LocalDate.of(LocalDate.now().getYear() + 1, 1, 1);
        }

        // List of airbag part numbers
        List<String> airbagPartNumbers = List.of(
                "10000011007", "10000050033", "1000011007", "1000029036", "1000029204",
                "1000046064", "1000050033", "1000050573", "1000085026", "1000086926",
                "100029003", "1000539107", "L001761661NCPAA", "L002056277NCPAA",
                "L002849952NCPAA", "L002910010NCPAA", "L002947268NCPAA", "L002952304NCPAA",
                "L002976693NCPAA", "L0761661AA01", "LL002056277NCPAA", "L001812878NCPAA"
        );

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT demande_id, sd.sequence, partNumberMaterial, nlotfrs, d.typeDefaut, d.defaut_code, s.nom as siteNom, p.nom as projetNom, ");
        sql.append("pt.[description], [initQuantity], [labelId], [lotNr], [quantity], [reftissu], [tableName], ");
        sql.append("[quantitePLS], [prixTotal], [prixUnit], pt.[createdAt] ");
        sql.append("FROM [MG_PLS_NEW].[dbo].SubDemande as sd ");
        sql.append("LEFT JOIN [MG_PLS_NEW].[dbo].Demande as d on d.id = sd.demande_id ");
        sql.append("LEFT JOIN [MG_PLS_NEW].[dbo].Projet as p on d.projet_id = p.id ");
        sql.append("LEFT JOIN [MG_PLS_NEW].[dbo].site as s on d.site_id = s.id ");
        sql.append("LEFT JOIN [MG_PLS_NEW].[dbo].ProdTicket as pt on pt.pls_id = sd.demande_id and pt.reftissu = sd.partNumberMaterial ");
        sql.append("WHERE d.active = 1 ");
        sql.append("AND pt.createdAt >= ? ");
        sql.append("AND pt.createdAt <= ? ");

        List<Object> params = new ArrayList<>();
        params.add(startDate.atStartOfDay());
        params.add(endDate.atStartOfDay());

        // Filter by partNumberMaterial if provided, otherwise use airbag list
        if (partNumberMaterial != null && !partNumberMaterial.trim().isEmpty()) {
            sql.append("AND sd.partNumberMaterial LIKE ? ");
            params.add("%" + partNumberMaterial.trim() + "%");
        } else {
            sql.append("AND sd.partNumberMaterial IN (");
            for (int i = 0; i < airbagPartNumbers.size(); i++) {
                sql.append("?");
                if (i < airbagPartNumbers.size() - 1) {
                    sql.append(", ");
                }
                params.add(airbagPartNumbers.get(i));
            }
            sql.append(") ");
        }

        sql.append("ORDER BY pt.createdAt DESC");

        return jdbcTemplatePls.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("demandeId", rs.getObject("demande_id"));
            row.put("sequence", rs.getString("sequence"));
            row.put("partNumberMaterial", rs.getString("partNumberMaterial"));
            row.put("nlotfrs", rs.getString("nlotfrs"));
            row.put("typeDefaut", rs.getString("typeDefaut"));
            row.put("defautCode", rs.getString("defaut_code"));
            row.put("siteNom", rs.getString("siteNom"));
            row.put("projetNom", rs.getString("projetNom"));
            row.put("description", rs.getString("description"));
            row.put("initQuantity", rs.getObject("initQuantity"));
            row.put("labelId", rs.getString("labelId"));
            row.put("lotNr", rs.getString("lotNr"));
            row.put("quantity", rs.getObject("quantity"));
            row.put("reftissu", rs.getString("reftissu"));
            row.put("tableName", rs.getString("tableName"));
            row.put("quantitePLS", rs.getObject("quantitePLS"));
            row.put("prixTotal", rs.getObject("prixTotal"));
            row.put("prixUnit", rs.getObject("prixUnit"));
            if (rs.getTimestamp("createdAt") != null) {
                row.put("createdAt", rs.getTimestamp("createdAt").toLocalDateTime().toString().replace("T", " "));
            } else {
                row.put("createdAt", null);
            }
            return row;
        });
    }
}
