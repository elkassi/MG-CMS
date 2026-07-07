package com.lear.MGCMS.services.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieLight;
import com.lear.MGCMS.payload.SequenceStatus;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestSerieLightRepository;
import com.lear.MGCMS.utils.UtilFunctions;

@Service
public class CuttingRequestSerieLightService {

	@Autowired
	private CuttingRequestSerieLightRepository repo;
	
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CuttingRequestSerieLightService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<SequenceStatus> getStatusSequence() {
        String sql = "SELECT  " + 
        		"    crs.cuttingRequest_sequence,  " + 
        		"	MIN(crs.statusMatelassage) as statusMatelassage , " + 
        		"    MIN(crs.dateDebutCoupe) as dateDebutCoupe,  " + 
        		"	CASE  " + 
        		"        WHEN COUNT(crs.serie) = COUNT(CASE WHEN crs.dateFinCoupe IS NOT NULL THEN 1 END)  " + 
        		"        THEN MAX(crs.dateFinCoupe) " + 
        		"        ELSE NULL  " + 
        		"    END as dateFinCoupe, " + 
        		"    COUNT(crs.serie) as total, " + 
        		"    COUNT(CASE WHEN crs.dateFinCoupe IS NOT NULL THEN 1 END) as notNullCount,  " + 
        		"	(SELECT COUNT(*) from [dbo].CuttingRequestBox as crb where crb.cuttingRequest_sequence = crs.cuttingRequest_sequence) as totalBoxes " + 
        		"FROM [dbo].[CuttingRequestSerie] as crs " + 
        		"JOIN [dbo].[CuttingRequest] as cr ON cr.sequence = crs.cuttingRequest_sequence " + 
        		"GROUP BY crs.cuttingRequest_sequence " + 
        		"HAVING   MIN(crs.dateDebutCoupe) is not null and (MIN(crs.dateDebutCoupe) > '2023-10-20 00:00:00.0000000' or MAX(crs.dateFinCoupe)  is null or MAX(crs.dateFinCoupe) > '2023-10-20 00:00:00.0000000') " + 
        		"ORDER BY dateDebutCoupe DESC;";

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
            	SequenceStatus result = new SequenceStatus();
            	result.setSequence(rs.getString(1));
            	result.setStatusMatelassage(rs.getString(2));
            	if (rs.getTimestamp(3) != null) result.setDateDebutCoupe(UtilFunctions.convertTimestampToLocalDateTime(rs.getTimestamp(3)));
            	if (rs.getTimestamp(4) != null) result.setDateFinCoupe(UtilFunctions.convertTimestampToLocalDateTime(rs.getTimestamp(4)));
            	result.setTotal(rs.getInt(5));
            	result.setNotNullCount(rs.getInt(6));
            	result.setNotNullCount(rs.getInt(6));
            	result.setTotalBoxes(rs.getInt(7));
                return result;
            }
        );
    }


	
	public List<CuttingRequestSerieLight> findAll(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo.findAll(date, shift);
	}

	public List<CuttingRequestSerieLight> findBetween(LocalDateTime startDate, LocalDateTime endDate, String machine) {
		// TODO Auto-generated method stub
		return repo.findBetween(startDate, endDate, machine);
	}

	public CuttingRequestSerieLight findBySerie(String serie) {
		// TODO Auto-generated method stub
		return repo.findBySerie(serie);
	}

	public List<CuttingRequestSerieLight> findAllNotYet() {
		// TODO Auto-generated method stub
		return repo.findAllNotYet();
	}
		
	public List<CuttingRequestSerieLight> findAllInProgress() {
		// TODO Auto-generated method stub
		return repo.findAllInProgress();
	}

	public List<CuttingRequestSerieLight> historique(LocalDate date, List<String> machines) {
		return repo.getHistorique(LocalDateTime.of(date, LocalTime.of(0, 0)), LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 0)), machines);
	}

	
}
