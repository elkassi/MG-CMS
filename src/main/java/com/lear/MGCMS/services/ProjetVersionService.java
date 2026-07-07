package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.ProjetVersion;
import com.lear.MGCMS.repositories.ProjetVersionRepository;

import javax.sql.DataSource;

@Service
public class ProjetVersionService {

	@Autowired
	private ProjetVersionRepository repo;

	@Autowired
	private ApplicationContext context;

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public ProjetVersionService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Page<ProjetVersion> findAll(String projet, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
        if(projet != null) projet += "%";
		return repo.findByFilter(projet,PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

	public ProjetVersion findByObjId(Long id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	public ProjetVersion save( ProjetVersion obj) {
		// TODO Auto-generated method stub
		return repo.save(obj);
	}

	public List<ProjetVersion> findByProjet(String projet) {
		// TODO Auto-generated method stub
		return repo.findByProjet(projet);
	}

	public void delete(ProjetVersion oldObj) {
		repo.delete(oldObj);
	}

	public List<String> findVersionFromCMS(String group) {
		DataSource cmsDataSource = (DataSource) context.getBean("cmsDataSource");
		JdbcTemplate jdbcTemplateCMS = new JdbcTemplate(cmsDataSource);
		/*
SELECT  [Version]
  FROM [logistics].[dbo].[Rapport9_4_25_Asprova_ItemMaster] where [GROUP] = 'K9' and Version is not null
  Group by [Version]
  		 */
		String sql = "SELECT  [Version] FROM [logistics].[dbo].[Rapport9_4_25_Asprova_ItemMaster] where [GROUP] = '"+group+"' and Version is not null Group by [Version]";
		List<String> list = jdbcTemplateCMS.queryForList(sql, String.class);
		return list;
	}

	public void saveAll(List<ProjetVersion> versions) {
		repo.saveAll(versions);
	}

    public List<String> findVersionByProjetNom(String nom) {
		return repo.findVersionByProjetNom(nom);
    }
}
