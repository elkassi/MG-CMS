package com.lear.MGCMS.services.CuttingRequest;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.payload.RapportOverlap;
import com.lear.MGCMS.payload.StatsInfo;
import com.lear.MGCMS.utils.StatusQualite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestSerieInfoRepository;

@Service
public class CuttingRequestSerieInfoService {

    @Autowired
    private CuttingRequestSerieInfoRepository repo;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CuttingRequestSerieInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getStatusCoupeBySequence(String sequence) {
        return repo.getStatusCoupeBySequence(sequence);
    }

    public List<String> getStatusMatelassageBySequence(String sequence) {
        return repo.getStatusMatelassageBySequence(sequence);
    }

    public List<CuttingRequestSerieInfo> findAll(LocalDate date, String shift) {
        // TODO Auto-generated method stub
        return repo.findAll(date, shift);
    }

    public List<CuttingRequestSerieInfo> findBetween(LocalDateTime startDate, LocalDateTime endDate, String machine) {
        // TODO Auto-generated method stub
        return repo.findBetween(startDate, endDate, machine);
    }

    public CuttingRequestSerieInfo findBySerie(String serie) {
        // TODO Auto-generated method stub
        return repo.findBySerie(serie);
    }

    public List<CuttingRequestSerieInfo> findAllNotYet() {
        // TODO Auto-generated method stub
        return repo.findAllNotYet();
    }

    public List<CuttingRequestSerieInfo> findAllInProgress() {
        // TODO Auto-generated method stub
        return repo.findAllInProgress();
    }

    public List<CuttingRequestSerieInfo> historique(LocalDate date) {

        return repo.getHistorique(LocalDateTime.of(date, LocalTime.of(0, 0)), LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 0)));
    }
    /*
            SELECT TOP (1000)
        crs.cuttingRequest_sequence
           ,crsr.cuttingRequestSerie_serie
           --kit
            ,crs.partNumbers
            ,crs.placement
            ,crsr.longueurPremierCouche
            ,crsr.nbrCouche
            ,crsr.laize
            ,crsr.confirmReftissu
            ,crs.description
            ,crsr.createdAt
            ,crs.tableMatelassage
            ,crs.tableCoupe
            ,crs.matelasseur1 + '/' +crs.matelasseur2
            ,(select SUM(cast(crs2.nbrCouche as int)) from [dbo].[CuttingRequestSerie] as crs2 where crs2.cuttingRequest_sequence = crs.cuttingRequest_sequence and crs2.placement = crs.placement )
          ,[overlap1]
                ,[overlap2]
                ,[overlap3]
                ,[overlap4]
                ,[overlap5]
                ,[overlap6]
                ,[overlap7]
                ,[overlap8]
                ,[excess]
                ,[retour]
                ,[totalUsage]

        FROM [dbo].[CuttingRequestSerieRouleau] crsr
        Join [dbo].[CuttingRequestSerie] crs on crs.serie = crsr.cuttingRequestSerie_serie

        order by serie desc
             */
    //"(select SUM(cast(crs2.nbrCouche as int)) from [dbo].[CuttingRequestSerie] as crs2 where crs2.cuttingRequest_sequence = crs.cuttingRequest_sequence and crs2.placement = crs.placement ) as nbrCoucheTotal, " +
//                "crsr.nbrCouche as nbrCoucheTotal, " +

    public List<RapportOverlap> rapportOverlap(LocalDate date1, LocalDate date2, List<String> prodList) {
        String sql = "SELECT " +
                "crs.cuttingRequest_sequence, " +
                "crsr.cuttingRequestSerie_serie, " +
                "(SELECT MAX(cppn.quantity) " +
                " FROM [dbo].[CuttingPlanPartNumber] cppn " +
                " WHERE cppn.cuttingPlan_id = cp.id) AS maxPartQuantity, " +
                "cr.modele AS modele, " +
                "crs.placement, " +
                "crsr.longueurPremierCouche, " +
                "crsr.nbrCouche, " +
                "crsr.laize AS laizeMesure, " +
                "crs.laize AS laize, " +
                "crsr.confirmReftissu, " +
                "crs.description, " +
                "crsr.createdAt, " +
                "crs.tableMatelassage, " +
                "crs.tableCoupe, " +
                "crs.drill, " +
                "crs.matelassageEndroit, " +
                "crs.matelasseur1, " +
                "crs.matelasseur2, " +
                "crs.coupeur1, " +
                "cp.id AS cuttingPlanId, " +
                "cp.cmsId AS cmsId, " +
                "[overlap1], [overlap2], [overlap3], [overlap4], " +
                "[overlap5], [overlap6], [overlap7], [overlap8], " +
                "[excess], [retour], [totalUsage] " +
                "FROM [dbo].[CuttingRequestSerieRouleau] crsr " +
                "JOIN [dbo].[CuttingRequestSerie] crs ON crs.serie = crsr.cuttingRequestSerie_serie " +
                "JOIN [dbo].[CuttingRequest] cr ON cr.sequence = crs.cuttingRequest_sequence " +
                "JOIN [dbo].[CuttingPlan] cp ON cp.id = cr.cuttingPlanId " +
                "WHERE crsr.createdAt BETWEEN ? AND ? " +
                "ORDER BY crs.serie DESC";

        final String[] minSerie = {null};
        final String[] maxSerie = {null};

        List<RapportOverlap> arr = jdbcTemplate.query(
                sql,
                new Object[] {
                        LocalDateTime.of(date1, LocalTime.of(0, 0)),
                        LocalDateTime.of(date2.plusDays(1), LocalTime.of(0, 0))
                },
                (rs, rowNum) -> {
                    RapportOverlap ro = new RapportOverlap();

                    ro.setCuttingRequest_sequence(rs.getString("cuttingRequest_sequence"));
                    ro.setCuttingRequestSerie_serie(rs.getString("cuttingRequestSerie_serie"));

                    // track min/max series
                    String serie = rs.getString("cuttingRequestSerie_serie");
                    if (minSerie[0] == null || minSerie[0].compareTo(serie) > 0) {
                        minSerie[0] = serie;
                    }
                    if (maxSerie[0] == null || maxSerie[0].compareTo(serie) < 0) {
                        maxSerie[0] = serie;
                    }

                    // new fields from the updated query
                    // assuming RapportOverlap has matching setters: setMaxPartQuantity(Double) and setModele(String)
                    int maxPartQty = rs.getInt("maxPartQuantity");
                    if (!rs.wasNull()) {
                        ro.setQuantite(maxPartQty + "");
                    }

//                    ro.setQuantite(rs.getString("quantite"));
                    ro.setPartNumbers(rs.getString("modele"));
                    ro.setPlacement(rs.getString("placement"));
                    ro.setLongueurPremierCouche(rs.getDouble("longueurPremierCouche"));
                    ro.setNbrCouche(rs.getInt("nbrCouche"));
                    ro.setLaizeMesure(rs.getDouble("laizeMesure"));
                    ro.setLaize(rs.getDouble("laize"));
                    ro.setConfirmReftissu(rs.getString("confirmReftissu"));
                    ro.setDescription(rs.getString("description"));
                    Timestamp ts = rs.getTimestamp("createdAt");
                    if (ts != null) {
                        ro.setCreatedAt(ts.toLocalDateTime());
                    }

                    ro.setTableMatelassage(rs.getString("tableMatelassage"));
                    ro.setTableCoupe(rs.getString("tableCoupe"));

                    // Handle prodList
                    String str1 = rs.getString("matelasseur1");
                    String str2 = rs.getString("matelasseur2");
                    String str3 = rs.getString("coupeur1");
                    if (prodList != null) {
                        for (String prod : prodList) {
                            if (str1 != null && prod.endsWith(":" + rs.getString("matelasseur1"))) {
                                str1 = prod;
                            }
                            if (str2 != null && prod.endsWith(":" + rs.getString("matelasseur2"))) {
                                str2 = prod;
                            }
                            if (str3 != null && prod.endsWith(":" + rs.getString("coupeur1"))) {
                                str3 = prod;
                            }
                        }
                    }
                    ro.setMatelasseur((str1 != null ? str1 : "") + " " + (str2 != null ? str2 : "") + " / " + (str3 != null ? str3 : ""));

                    // Handle drill
                    String drill = rs.getString("drill");
                    if (drill != null) {
                        String[] drillArr = drill.split(",");
                        if (drillArr.length > 0) {
                            ro.setDrill1(drillArr[0]);
                        }
                        if (drillArr.length > 1) {
                            ro.setDrill2(drillArr[1]);
                        }
                    }

                    ro.setMatelassageEndroit(rs.getString("matelassageEndroit"));
                    long cuttingPlanIdVal = rs.getLong("cuttingPlanId");
                    if (!rs.wasNull()) {
                        ro.setCuttingPlanId(cuttingPlanIdVal);
                    }
                    long cmsIdVal = rs.getLong("cmsId");
                    if (!rs.wasNull()) {
                        ro.setCmsId(cmsIdVal);
                    }
                    ro.setOverlap1(rs.getDouble("overlap1"));
                    ro.setOverlap2(rs.getDouble("overlap2"));
                    ro.setOverlap3(rs.getDouble("overlap3"));
                    ro.setOverlap4(rs.getDouble("overlap4"));
                    ro.setOverlap5(rs.getDouble("overlap5"));
                    ro.setOverlap6(rs.getDouble("overlap6"));
                    ro.setOverlap7(rs.getDouble("overlap7"));
                    ro.setOverlap8(rs.getDouble("overlap8"));
                    ro.setExcess(rs.getDouble("excess"));
                    ro.setRetour(rs.getDouble("retour"));
                    ro.setTotalUsage(rs.getDouble("totalUsage"));

                    return ro;
                }
        );



        String sqlSerie = "SELECT cuttingRequest_sequence + '-' + placement AS serie, SUM(nbrCouche) AS nbrCoucheTotal " +
                "FROM [dbo].[CuttingRequestSerie] " +
                "WHERE serie >= ? AND serie <= ? " +
                "GROUP BY cuttingRequest_sequence, placement";

        Map<String, Long> serieMap = jdbcTemplate.query(
                sqlSerie,
                new Object[]{minSerie[0], maxSerie[0]},
                (rs, rowNum) -> new StatsInfo(rs.getString("serie"), rs.getLong("nbrCoucheTotal"))
        ).stream().collect(
                java.util.stream.Collectors.toMap(StatsInfo::getInfo, StatsInfo::getValue)
        );

        System.out.println("serieMap Size = " + serieMap.size());

        for (RapportOverlap ro : arr) {
            ro.setNbrCoucheTotal(serieMap.get(ro.getCuttingRequest_sequence() + "-" + ro.getPlacement()));
        }

        return arr;
    }

    public List<CuttingRequestSerieInfo> findSeries(List<String> series) {
        // TODO Auto-generated method stub
        return repo.findSeries(series);
    }

    public List<CuttingRequestSerieInfo> findBetween2(LocalDateTime startDate, LocalDateTime endDate, String machine) {
        // TODO Auto-generated method stub
        return repo.findBetween2(startDate, endDate, machine);
    }

    public List<StatusQualite> statusQualite(LocalDate date, LocalDate date2, String machine, Boolean valide) {
         /*
    SELECT TOP 1000
  cuttingRequest_sequence,
  serie,
  partNumberMaterial,
  placement,
  tableMatelassage,
  tableCoupe,
  tableQualite,
  nbrPiece,
  nbrCouche,
  nbrPieceTotal,
  qteNonConforme,
  crs.codeDefaut_code,
  cd.description AS codeDefaut_description, -- Add the description column
  lieuDetection,
  qteScrap,
  crs.codeScrap_code,
  cs.description AS codeScrap_description, -- Add the description column,
  dateDebutCoupe
FROM [dbo].[CuttingRequestSerie] crs
LEFT JOIN [dbo].[CodeDefaut] cd ON crs.codeDefaut_code = cd.code
LEFT JOIN [dbo].[CodeScrap] cs ON crs.codeScrap_code = cs.code
WHERE statusCoupe = 'Complete'
ORDER BY dateDebutCoupe DESC;
     */
        String sql = "SELECT cuttingRequest_sequence, serie, partNumberMaterial, placement, tableMatelassage, tableCoupe, tableQualite, " +
                "nbrPiece, nbrCouche, nbrPieceTotal, qteNonConforme, crs.codeDefaut_code, cd.description AS codeDefaut_description, lieuDetection, " +
                "qteScrap, crs.codeScrap_code, cs.description AS codeScrap_description, dateDebutCoupe, dateValidationQualite, " +
                "      premierPaquet, " +
                "  milieuPaquet, " +
                "  dernierPaquet, " +
                "  verificationDrill, " +
                "  verificationDrill2 " +
                "FROM [dbo].[CuttingRequestSerie] crs " +
                "LEFT JOIN [dbo].[CodeDefaut] cd ON crs.codeDefaut_code = cd.code " +
                "LEFT JOIN [dbo].[CodeScrap] cs ON crs.codeScrap_code = cs.code " +
                "WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ? ";
        if (valide != null) {
            if (valide) {
                sql += "AND (crs.codeDefaut_code IS NOT NULL or crs.codeScrap_code IS NOT NULL) ";
            } else {
                sql += "AND (crs.codeDefaut_code IS NULL and crs.codeScrap_code IS NULL) ";
            }
        }
        if(machine != null) {
            sql += "AND tableCoupe = ? ";
        }
        sql += "ORDER BY dateDebutCoupe DESC";
        if(machine != null) {
            return jdbcTemplate.query(
                    sql,
                    new Object[]{
                            LocalDateTime.of(date, LocalTime.of(0, 0)),
                            LocalDateTime.of(date2.plusDays(1), LocalTime.of(0, 0)),
                            machine
                    },
                    (rs, rowNum) -> {
                        StatusQualite sq = new StatusQualite();
                        sq.setCuttingRequestSequence(rs.getString("cuttingRequest_sequence"));
                        sq.setSerie(rs.getString("serie"));
                        sq.setPartNumberMaterial(rs.getString("partNumberMaterial"));
                        sq.setPlacement(rs.getString("placement"));
                        sq.setTableMatelassage(rs.getString("tableMatelassage"));
                        sq.setTableCoupe(rs.getString("tableCoupe"));
                        sq.setTableQualite(rs.getString("tableQualite"));
                        sq.setNbrPiece(rs.getInt("nbrPiece"));
                        sq.setNbrCouche(rs.getInt("nbrCouche"));
                        sq.setNbrPieceTotal(rs.getInt("nbrPieceTotal"));
                        sq.setQteNonConforme(rs.getInt("qteNonConforme"));
                        sq.setCodeDefautCode(rs.getString("codeDefaut_code"));
                        sq.setCodeDefautDescription(rs.getString("codeDefaut_description"));
                        sq.setLieuDetection(rs.getString("lieuDetection"));
                        sq.setQteScrap(rs.getInt("qteScrap"));
                        sq.setCodeScrapCode(rs.getString("codeScrap_code"));
                        sq.setCodeScrapDescription(rs.getString("codeScrap_description"));
                        if (rs.getTimestamp("dateValidationQualite") != null) {
                            sq.setDateDebutCoupe(rs.getTimestamp("dateDebutCoupe").toLocalDateTime());
                        }
                        // rs.getTimestamp("dateValidationQualite") can be null
                        if (rs.getTimestamp("dateValidationQualite") != null) {
                            sq.setDateValidationQualite(rs.getTimestamp("dateValidationQualite").toLocalDateTime());
                        }
                        sq.setPremierPaquet(rs.getString("premierPaquet"));
                        sq.setMilieuPaquet(rs.getString("milieuPaquet"));
                        sq.setDernierPaquet(rs.getString("dernierPaquet"));
                        sq.setVerificationDrill(rs.getString("verificationDrill"));
                        sq.setVerificationDrill2(rs.getString("verificationDrill2"));

                        return sq;
                    }
            );
        } else {
            return jdbcTemplate.query(
                    sql,
                    new Object[]{
                            LocalDateTime.of(date, LocalTime.of(0, 0)),
                            LocalDateTime.of(date2.plusDays(1), LocalTime.of(0, 0))
                    },
                    (rs, rowNum) -> {
                        StatusQualite sq = new StatusQualite();
                        sq.setCuttingRequestSequence(rs.getString("cuttingRequest_sequence"));
                        sq.setSerie(rs.getString("serie"));
                        sq.setPartNumberMaterial(rs.getString("partNumberMaterial"));
                        sq.setPlacement(rs.getString("placement"));
                        sq.setTableMatelassage(rs.getString("tableMatelassage"));
                        sq.setTableCoupe(rs.getString("tableCoupe"));
                        sq.setTableQualite(rs.getString("tableQualite"));
                        sq.setNbrPiece(rs.getInt("nbrPiece"));
                        sq.setNbrCouche(rs.getInt("nbrCouche"));
                        sq.setNbrPieceTotal(rs.getInt("nbrPieceTotal"));
                        sq.setQteNonConforme(rs.getInt("qteNonConforme"));
                        sq.setCodeDefautCode(rs.getString("codeDefaut_code"));
                        sq.setCodeDefautDescription(rs.getString("codeDefaut_description"));
                        sq.setLieuDetection(rs.getString("lieuDetection"));
                        sq.setQteScrap(rs.getInt("qteScrap"));
                        sq.setCodeScrapCode(rs.getString("codeScrap_code"));
                        sq.setCodeScrapDescription(rs.getString("codeScrap_description"));
                        if (rs.getTimestamp("dateValidationQualite") != null) {
                            sq.setDateDebutCoupe(rs.getTimestamp("dateDebutCoupe").toLocalDateTime());
                        }
                        // rs.getTimestamp("dateValidationQualite") can be null
                        if (rs.getTimestamp("dateValidationQualite") != null) {
                            sq.setDateValidationQualite(rs.getTimestamp("dateValidationQualite").toLocalDateTime());
                        }
                        sq.setPremierPaquet(rs.getString("premierPaquet"));
                        sq.setMilieuPaquet(rs.getString("milieuPaquet"));
                        sq.setDernierPaquet(rs.getString("dernierPaquet"));
                        sq.setVerificationDrill(rs.getString("verificationDrill"));
                        sq.setVerificationDrill2(rs.getString("verificationDrill2"));

                        return sq;
                    }
            );
        }
    }

    public CuttingRequestSerieInfo save(CuttingRequestSerieInfo obj) {
        // TODO Auto-generated method stub
        return repo.save(obj);
    }

    public List<String> findMachines(String sequence, List<String> reftissuList) {
        // TODO Auto-generated method stub
        return repo.findMachines(sequence, reftissuList);
    }
}
