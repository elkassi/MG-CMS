package com.lear.MGCMS.repositories;

import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PartNumberInfo2;


public interface PartNumberInfo2Repository  extends CrudRepository<PartNumberInfo2, String> {

	PartNumberInfo2 findByPartNumber(String partNumber);

}
