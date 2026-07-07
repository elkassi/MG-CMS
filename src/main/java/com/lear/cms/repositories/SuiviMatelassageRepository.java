package com.lear.cms.repositories;

import com.lear.cms.domain.SuiviCoupe;
import com.lear.cms.domain.SuiviMatelassage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SuiviMatelassageRepository extends CrudRepository<SuiviMatelassage, Long> {

	SuiviMatelassage findByNserie(String serie);

	List<SuiviMatelassage> findByNof(String nof);

	@Query("select max(id) from SuiviMatelassage")
	Long getMaxId();
	@Query("select max(nserie) from SuiviMatelassage where nserie like :prefix")
    String getMaxSerieWithPrefix(String prefix);
}
