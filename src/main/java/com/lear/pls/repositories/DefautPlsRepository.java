package com.lear.pls.repositories;

import com.lear.pls.domain.Defaut;
import com.lear.pls.domain.Defaut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface DefautPlsRepository extends JpaRepository<Defaut, String>, JpaSpecificationExecutor<Defaut> {

	Defaut findByCode(String code);
}
