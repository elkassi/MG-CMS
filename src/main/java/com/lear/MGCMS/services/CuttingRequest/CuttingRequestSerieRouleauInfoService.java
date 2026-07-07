package com.lear.MGCMS.services.CuttingRequest;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauInfo;
import com.lear.MGCMS.payload.RouleauRapport;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestSerieRouleauInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CuttingRequestSerieRouleauInfoService {

    @Autowired
    private CuttingRequestSerieRouleauInfoRepository repo;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CuttingRequestSerieRouleauInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CuttingRequestSerieRouleauInfo> findByIdRouleau(String idRouleau) {
        return repo.findByIdRouleau(idRouleau);
    }

    public List<RouleauRapport> findRest(String reftissu) {
        String sql = "WITH RankedRows AS ( " +
                "    SELECT  " +
                "        crsr.[idRouleau], " +
                "        crsr.[laize], " +
                "        crsr.[lotFrs], " +
                "        crsr.[metrage], " +
                "        crsr.[nbrCouche], " +
                "        crsr.[retour], " +
                "        crsr.[cuttingRequestSerie_serie], " +
                "        crsr.[createdAt], " +
                "        crs.tableMatelassage, " +
                "		 crsr.[confirmRetour], " +
                "        ROW_NUMBER() OVER (PARTITION BY crsr.[lotFrs], crsr.[idRouleau] ORDER BY crsr.[metrage] ASC) AS RowNum " +
                "    FROM  " +
                "        [dbo].[CuttingRequestSerieRouleau] AS crsr " +
                "        JOIN [dbo].[CuttingRequestSerie] AS crs ON crs.serie = crsr.cuttingRequestSerie_serie " +
                "    WHERE  " +
                "        crsr.[confirmReftissu] = ? " +
                "        AND crsr.[createdAt] >= DATEADD(HOUR, -48, GETDATE()) " + // Add this condition
                ") " +
                "SELECT  " +
                "    [lotFrs], " +
                "    [idRouleau], " +
                "    [laize], " +
                "    [retour], " +
                "    [cuttingRequestSerie_serie], " +
                "    [createdAt], " +
                "    tableMatelassage " +
                "FROM  " +
                "    RankedRows " +
                "WHERE  " +
                "    RowNum = 1 and confirmRetour = 1 " +
                "ORDER BY  " +
                "    [createdAt] DESC;";
        return jdbcTemplate.query(sql,
                new Object[]{reftissu},
                (rs, rowNum) -> {
                    RouleauRapport result = new RouleauRapport();
                    result.setLotFrs(rs.getString(1));
                    result.setIdRouleau(rs.getString(2));
                    result.setLaize(rs.getDouble(3));
                    result.setRetour(rs.getDouble(4));
                    result.setSerie(rs.getString(5));
                    result.setCreatedAt(convertTimestampToLocalDateTime(rs.getTimestamp(6)));
                    result.setTableMatelassage(rs.getString(7));
                    return result;
                }
        );
    }

    public static LocalDateTime convertTimestampToLocalDateTime(Timestamp timestampToConvert) {
        if (timestampToConvert != null) {
            return timestampToConvert.toLocalDateTime();
        } else {
            return null;
        }
    }


    public List<String> getArrLabelIdWithRetour(List<String> arrStr) {
        return repo.getArrLabelIdWithRetour(arrStr);
    }
}
