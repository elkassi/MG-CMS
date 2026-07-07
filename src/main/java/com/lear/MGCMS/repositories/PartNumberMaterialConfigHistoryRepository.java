package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PartNumberMaterialConfigHistory;

public interface PartNumberMaterialConfigHistoryRepository extends CrudRepository<PartNumberMaterialConfigHistory, Long> {

	List<PartNumberMaterialConfigHistory> findByPartNumberMaterial(String partNumberMaterial);

}
