package com.lear.MGCMS.services.CuttingRequest;

import java.time.LocalDate;
import java.util.List;

import com.lear.MGCMS.payload.StatCoupe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;

@Service
public class CuttingRequestService {

    @Autowired
    private CuttingRequestRepository repo;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CuttingRequestService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CuttingRequest findBySequence(String sequence) {
        return repo.findBySequence(sequence);
    }

    public CuttingRequest save(CuttingRequest obj) {
        return repo.save(obj);
    }

    public void deleteBySequence(String sequence) {
        repo.deleteBySequence(sequence);
    }

    public List<CuttingRequest> findAll(LocalDate date, String shift) {
        // TODO Auto-generated method stub
        return repo.findAll(date, shift);
    }

    public List<StatCoupe> findAllStat(LocalDate date, String shift, String zone) {
		/*
		here is the Query :
		SELECT TOP (1000) cr.[sequence]
      ,cr.[createdAt]
      ,cr.[cuttingPlanId]
      ,cr.[definition]
      ,cr.[modele]
      ,cr.[planningDate]
      ,cr.[projet]
      ,cr.[shift]
      ,cr.[version]
      ,cr.[zone_nom]
      ,cr.[createdBy_matricule]
	  ,MIN(dateDebutMatelassage) as dateDebutMatelassage
	  ,MAX(dateFinMatelassage) as dateFinMatelassage
      ,SUM(CASE WHEN crs.statusMatelassage = 'Waiting' THEN 1 ELSE 0 END) AS WaitingMatelassage
      ,SUM(CASE WHEN crs.statusMatelassage = 'In progress' THEN 1 ELSE 0 END) AS InProgressMatelassage
      ,SUM(CASE WHEN crs.statusMatelassage = 'Complete' THEN 1 ELSE 0 END) AS CompleteMatelassage
      ,SUM(CASE WHEN crs.statusMatelassage = 'Incomplete' THEN 1 ELSE 0 END) AS IncompleteMatelassage
	  ,MIN(dateDebutCoupe) as dateDebutCoupe
	  ,MAX(dateFinCoupe) as dateFinCoupe
      ,SUM(CASE WHEN crs.statusCoupe = 'Waiting' THEN 1 ELSE 0 END) AS WaitingCoupe
      ,SUM(CASE WHEN crs.statusCoupe = 'In progress' THEN 1 ELSE 0 END) AS InProgressCoupe
      ,SUM(CASE WHEN crs.statusCoupe = 'Complete' THEN 1 ELSE 0 END) AS CompleteCoupe
      ,SUM(CASE WHEN crs.statusCoupe = 'Incomplete' THEN 1 ELSE 0 END) AS IncompleteCoupe
FROM [dbo].[CuttingRequest] as cr
LEFT JOIN [dbo].[CuttingRequestSerie] as crs ON crs.cuttingRequest_sequence = cr.sequence
GROUP BY cr.[sequence]
      ,cr.[createdAt]
      ,cr.[cuttingPlanId]
      ,cr.[definition]
      ,cr.[modele]
      ,cr.[planningDate]
      ,cr.[projet]
      ,cr.[shift]
      ,cr.[version]
      ,cr.[zone_nom]
      ,cr.[createdBy_matricule]
ORDER BY cr.[sequence] DESC;
add to the query : shift and date
		 */
        String sql = "SELECT TOP (1000) cr.[sequence]\n" +
                "      ,cr.[cuttingPlanId]\n" +
                "      ,cr.[modele]\n" +
                "      ,cr.[zone_nom]\n" +
                "      ,cr.[planningDate]\n" +
                "      ,cr.[shift]\n" +
                "	  ,MIN(dateDebutMatelassage) as dateDebutMatelassage\n" +
                "	  ,MAX(dateFinMatelassage) as dateFinMatelassage\n" +
                "      ,SUM(CASE WHEN crs.statusMatelassage = 'Waiting' THEN 1 ELSE 0 END) AS WaitingMatelassage\n" +
                "      ,SUM(CASE WHEN crs.statusMatelassage = 'In progress' THEN 1 ELSE 0 END) AS InProgressMatelassage\n" +
                "      ,SUM(CASE WHEN crs.statusMatelassage = 'Complete' THEN 1 ELSE 0 END) AS CompleteMatelassage\n" +
                "      ,SUM(CASE WHEN crs.statusMatelassage = 'Incomplete' THEN 1 ELSE 0 END) AS IncompleteMatelassage\n" +
                "	  ,MIN(dateDebutCoupe) as dateDebutCoupe\n" +
                "	  ,MAX(dateFinCoupe) as dateFinCoupe\n" +
                "      ,SUM(CASE WHEN crs.statusCoupe = 'Waiting' THEN 1 ELSE 0 END) AS WaitingCoupe\n" +
                "      ,SUM(CASE WHEN crs.statusCoupe = 'In progress' THEN 1 ELSE 0 END) AS InProgressCoupe\n" +
                "      ,SUM(CASE WHEN crs.statusCoupe = 'Complete' THEN 1 ELSE 0 END) AS CompleteCoupe\n" +
                "	  ,SUM(CASE WHEN crs.statusCoupe = 'Incomplete' THEN 1 ELSE 0 END) AS IncompleteCoupe\n" +
                "FROM [dbo].[CuttingRequest] as cr\n" +
                "LEFT JOIN [dbo].[CuttingRequestSerie] as crs ON crs.cuttingRequest_sequence = cr.sequence\n" +
                "WHERE cr.planningDate = ?\n";
        if (shift != null && !shift.isEmpty())
            sql += "AND cr.shift = ?\n";
        if (zone != null && !zone.isEmpty())
            sql += "AND cr.zone_nom = ?\n";
        sql += "	GROUP BY cr.[sequence]\n" +
                "      ,cr.[cuttingPlanId]\n" +
                "      ,cr.[modele]\n" +
                "      ,cr.[zone_nom]\n" +
                "      ,cr.[planningDate]\n" +
                "      ,cr.[shift]\n" +
                "ORDER BY cr.[sequence] DESC;";
        return jdbcTemplate.query(sql,
                preparedStatement -> {
                    preparedStatement.setDate(1, java.sql.Date.valueOf(date));
                    if (shift != null && !shift.isEmpty())
                        preparedStatement.setString(2, shift);
                    if (zone != null && !zone.isEmpty())
                        preparedStatement.setString(3, zone);
                },
                (rs, rowNum) ->
                        new StatCoupe(
                                rs.getString("sequence"),
                                //createdAt can be null
                                rs.getLong("cuttingPlanId"),
                                rs.getString("modele"),
                                rs.getString("zone_nom"),
                                rs.getTimestamp("planningDate") != null ? rs.getTimestamp("planningDate").toLocalDateTime().toLocalDate() : null,
                                rs.getString("shift"),
                                rs.getTimestamp("dateDebutMatelassage") != null ? rs.getTimestamp("dateDebutMatelassage").toLocalDateTime() : null,
                                rs.getTimestamp("dateFinMatelassage") != null ? rs.getTimestamp("dateFinMatelassage").toLocalDateTime() : null,
                                rs.getInt("WaitingMatelassage"),
                                rs.getInt("InProgressMatelassage"),
                                rs.getInt("CompleteMatelassage"),
                                rs.getInt("IncompleteMatelassage"),
                                rs.getTimestamp("dateDebutCoupe") != null ? rs.getTimestamp("dateDebutCoupe").toLocalDateTime() : null,
                                rs.getTimestamp("dateFinCoupe") != null ? rs.getTimestamp("dateFinCoupe").toLocalDateTime() : null,
                                rs.getInt("WaitingCoupe"),
                                rs.getInt("InProgressCoupe"),
                                rs.getInt("CompleteCoupe"),
                                rs.getInt("IncompleteCoupe")
                        )
        );


    }


    public Integer totalSequencesWithPrefix(String prefix) {
        String sql = "SELECT COUNT(*) FROM CuttingRequest WHERE sequence LIKE ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, prefix + "%");
    }

    public Integer getMaxIdBox() {
        //SELECT max(id) FROM [dbo].[CuttingRequestBox]
        // but id is a string so convert it to number
        String sql = "SELECT MAX(CAST(id AS INT)) FROM [dbo].[CuttingRequestBox]";
        Integer maxId = jdbcTemplate.queryForObject(sql, Integer.class);
        if (maxId == null) {
            return 75000000; // if no records found, return 0
        }
        return maxId;
    }

    public Integer getMaxSerieWithPrefix(String prefix) {
        //SELECT max(serie) FROM [dbo].[CuttingRequestSerie]
        // but serie is a string so convert it to number
        String sql = "SELECT MAX(CAST(serie AS INT)) FROM [dbo].[CuttingRequestSerie] WHERE serie LIKE ?";
        Integer maxSerie = jdbcTemplate.queryForObject(sql, Integer.class, prefix + "%");
        if (maxSerie == null) {
            return Integer.parseInt(prefix + "000000"); // if no records found, return prefix + 00000000
        }
        return maxSerie;
    }

    public String maxSequencesWithPrefix(String prefix) {
        String sql = "SELECT max(sequence) FROM CuttingRequest WHERE sequence LIKE ?";

        try {
            return jdbcTemplate.queryForObject(sql, String.class, prefix + "%");
        } catch (Exception e) {
            return null;
        }
    }
}
