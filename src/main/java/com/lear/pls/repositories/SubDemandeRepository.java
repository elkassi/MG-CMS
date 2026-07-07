package com.lear.pls.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.pls.domain.SubDemande;

public interface SubDemandeRepository extends JpaRepository<SubDemande, Long>, JpaSpecificationExecutor<SubDemande> {

	SubDemande getSubDemandeById(long id);

}
