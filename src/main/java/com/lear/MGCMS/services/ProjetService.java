package com.lear.MGCMS.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Projet;
import com.lear.MGCMS.repositories.ProjetRepository;

import javax.sql.DataSource;

@Service
public class ProjetService {

	@Autowired
	private ProjetRepository repo;

	@Autowired
	private ApplicationContext context;

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public ProjetService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}


	public Page<Projet> findAll(String nom, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
        
		return repo.findByFilter(nom+"%",PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

	public Projet findByObjId(String id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	public Projet save( Projet obj) {
		// TODO Auto-generated method stub
		return repo.save(obj);
	}

	public List<Projet> findAll() {
		// TODO Auto-generated method stub
		return (List<Projet>) repo.findAll();
	}

	public List<String> findProjetsFromCMS() {
		DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
		JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
		/*
		SELECT [GROUP]
  FROM [logistics].[dbo].[Rapport9_4_25_Asprova_ItemMaster]
  Group by [GROUP]
		 */
		String sql = "SELECT [GROUP] FROM [logistics].[dbo].[Rapport9_4_25_Asprova_ItemMaster] Group by [GROUP]";
		List<String> list = new ArrayList<String>();
		list = jdbcTemplateCMS.queryForList(sql, String.class);
		return list;
	}

	public void delete(Projet oldObj) {
		repo.delete(oldObj);
	}
	
}
