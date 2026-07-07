package com.lear.MGCMS.services.pls;

import com.lear.pls.domain.Projet;
import com.lear.pls.repositories.ProjetPlsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjetPlsService {

	@Autowired
	private ProjetPlsRepository projetRepository;
	
	public Projet save(Projet projet) {
		return projetRepository.save(projet);
	}
	
	public Projet findById(Long id) {
		return projetRepository.getProjetPlsById(id);
	}
	public Projet findByNom(String nom) {
		return projetRepository.findByNom(nom);
	}
	
	public Iterable<Projet> findAll() {
		return projetRepository.findAll();
	}
	
	public void deletebyId(Long id) {
		projetRepository.deleteById(id);
	}

	public List<Projet> findList() {
		return (List<Projet>) projetRepository.findAll();
	}
}
