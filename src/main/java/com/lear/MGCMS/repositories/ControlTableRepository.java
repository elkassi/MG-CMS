package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ControlTable;
import org.springframework.data.repository.CrudRepository;

public interface ControlTableRepository extends CrudRepository<ControlTable, String> {

	ControlTable findFirstByPcName(String hostName);

}
