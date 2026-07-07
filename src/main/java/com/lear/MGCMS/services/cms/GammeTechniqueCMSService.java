package com.lear.MGCMS.services.cms;

import com.lear.MGCMS.payload.PayloadTemp;
import com.lear.cms.domain.GammeTechnique;
import com.lear.cms.repositories.GammeTechniqueCMSRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GammeTechniqueCMSService {

    @Autowired
    private GammeTechniqueCMSRepository repository;

    @Autowired
    private ApplicationContext context;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GammeTechniqueCMSService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public List<PayloadTemp> getByPartNumber(List<String> partNumbers) {
        if (partNumbers == null || partNumbers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> sanitizedPartNumbers = new ArrayList<>();
        for (String partNumber : partNumbers) {
            if (partNumber == null) {
                continue;
            }

            String trimmedPartNumber = partNumber.trim();
            if (!trimmedPartNumber.isEmpty() && !sanitizedPartNumbers.contains(trimmedPartNumber)) {
                sanitizedPartNumbers.add(trimmedPartNumber);
            }
        }

        if (sanitizedPartNumbers.isEmpty()) {
            return Collections.emptyList();
        }

        DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
        JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);

        // Construct the SQL query
        String sql = "SELECT PartNumber, Site, Packaging FROM dbo.GammeTechnique WHERE PartNumber IN (";
        StringBuilder partNumbersInClause = new StringBuilder();
        for (int i = 0; i < sanitizedPartNumbers.size(); i++) {
            partNumbersInClause.append("?");
            if (i < sanitizedPartNumbers.size() - 1) {
                partNumbersInClause.append(",");
            }
        }
        sql += partNumbersInClause.toString() + ")";

        // Execute the query and map the results
        return jdbcTemplateCMS.query(sql, sanitizedPartNumbers.toArray(), (rs, rowNum) -> {
            PayloadTemp payload = new PayloadTemp();
            payload.setStr1(rs.getString("PartNumber"));
            payload.setStr2(rs.getString("Site"));
            payload.setNum1(rs.getDouble("Packaging"));
            return payload;
        });
    }
}
