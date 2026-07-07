package com.lear.pls.repositories;

import com.lear.pls.domain.PlsMachine;
import com.lear.pls.domain.PlsMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface PlsMachineRepository extends JpaRepository<PlsMachine, Long>, JpaSpecificationExecutor<PlsMachine> {

	PlsMachine getPlsMachineById(Long id);

	PlsMachine findByNom(String nom);
}
