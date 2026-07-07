package com.lear.MGCMS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Service
public class OptitimeService {

    @Autowired
    private ApplicationContext context;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OptitimeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getListNames(String sec) {
        DataSource spliceDataSource = (DataSource) context.getBean("spliceDataSource");
        JdbcTemplate jdbcTemplateSplice = new JdbcTemplate(spliceDataSource);
        try{
        String sql = "SELECT [P_NOM] + ' ' + [P_PRE] +':' + [P_MAT] FROM [LEAR].[dbo].[TGPPER] " +
                " where P_sec = ? and P_DATSOR is null  and P_UN2 = '5' " +
                " order by P_DATENT";
        return jdbcTemplateSplice.queryForList(sql, String.class, sec);
        } catch (Exception e) {
            // Handle exception
            System.out.println("Error: " + e.getMessage());
        }
        return new ArrayList<>();
    }


    public String getFullName(String code) {
        DataSource spliceDataSource = (DataSource) context.getBean("spliceDataSource");
        JdbcTemplate jdbcTemplateSplice = new JdbcTemplate(spliceDataSource);
        /*
        SELECT [P_NOM] + ' ' + [P_PRE] +' : ' + [P_MAT]
  FROM [LEAR].[dbo].[TGPPER] where P_CODBAD2 = '0F00501490'
         */
        try {
            String sql = "SELECT [P_NOM] + ' ' + [P_PRE] + ' : ' + [P_MAT] + ' : ' + [P_SEC] FROM [LEAR].[dbo].[TGPPER] " +
                    " where P_CODBAD2 = ?";
            // return could be null
            List<String> list = jdbcTemplateSplice.queryForList(sql, String.class, code);
            if (list.size() > 0) {
                return list.get(0);
            }
        } catch (Exception e) {
            // Handle exception
            System.out.println("Error: " + e.getMessage());
        }
        return "";
    }

    public String getFullNameByMatricule(String matricule) {
        DataSource spliceDataSource = (DataSource) context.getBean("spliceDataSource");
        JdbcTemplate jdbcTemplateSplice = new JdbcTemplate(spliceDataSource);
        /*
        SELECT [P_NOM] + ' ' + [P_PRE] +' : ' + [P_MAT]
  FROM [LEAR].[dbo].[TGPPER] where P_CODBAD2 = '0F00501490'
         */
        try {
            String sql = "SELECT [P_NOM] + ' ' + [P_PRE] + ' : ' + [P_MAT] FROM [LEAR].[dbo].[TGPPER] " +
                    " where P_MAT = ?";
            // return could be null
            List<String> list = jdbcTemplateSplice.queryForList(sql, String.class, matricule);
            if (list.size() > 0) {
                return list.get(0);
            }
        } catch (Exception e) {
            // Handle exception
            System.out.println("Error: " + e.getMessage());
        }
        return "";
    }


}
