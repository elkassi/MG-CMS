package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.Matlassage;

public interface MatlassageRepository extends CrudRepository<Matlassage, Long>  {
	
	List<Matlassage> findByNofOrderByNserie(String sq);
    @Query("from Matlassage where reftissu = :reftissu and returnMagasin = :retour")
    List<Matlassage> findByReftissuAndReurnMagasin(String reftissu, String retour);

    Matlassage findByNserie(Long nserie);

    List<Matlassage> findByNof(String sequence);

    @Query("select max(nserie) from Matlassage")
    Long getMaxNserie();
}
