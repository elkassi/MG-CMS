package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.Qn;

public interface QnRepository extends CrudRepository<Qn, String> {

	@Query("SELECT t from Qn t where t.numeroQn like :numeroQn and (:of is not null)")
	Page<Qn> findByFilter(String numeroQn, PageRequest of);
	@Query("SELECT t from Qn t where t.numeroQn = :numeroQn")
	Qn findByObjId(String numeroQn);
	@Query("from Qn where reftissu in (:reftissus)")
	List<Qn> findByReftissu(List<String> reftissus);
	@Query("SELECT t.numeroQn from Qn t")
	List<String> findAllNumeroQn();
	
}
