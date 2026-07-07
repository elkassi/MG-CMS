package com.lear.cms.repositories;

import com.lear.cms.domain.SuiviCoupe;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SuiviCoupeRepository extends CrudRepository<SuiviCoupe, Long> {

	SuiviCoupe findByNserie(String serie);

	List<SuiviCoupe> findByNof(String nof);

	@Query("SELECT MAX(id) FROM SuiviCoupe")
    Long getMaxId();

	@Query("select max(nserie) from SuiviCoupe where nserie like :prefix")
	String getMaxSerieWithPrefix(String prefix);
}
