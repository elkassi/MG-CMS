package com.lear.MGCMS.repositories;

import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PartNumberInfo;

public interface PartNumberInfoRepository extends CrudRepository<PartNumberInfo, String> {

	PartNumberInfo findByPartNumber(String partNumber);

}
