package com.lear.cms.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.Coupe;

import java.util.List;


public interface CoupeRepository extends CrudRepository<Coupe, Long> {
	
	Coupe getCoupeById(Long id);
		
	void deleteById(Long id);

	Iterable<Coupe> findAll();
	
	//Coupe findFirstByNofAndPlacement(String nof, String placement);
	Coupe findFirstByNserie(Long nserie);
	Coupe findFirstByNofAndNserie(String nof, Long nserie);


    List<Coupe> findByNof(String nof);

	@Query("SELECT MAX(id) FROM Coupe")
    Long getMaxId();

	@Query("SELECT MAX(nof) FROM Coupe where nof like :nof")
	String getMaxNofLike(String nof);
}
