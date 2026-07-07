package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.GammeTechniqueImprimerHistorique;

public interface GammeTechniqueImprimerHistoriqueRepository extends JpaRepository<GammeTechniqueImprimerHistorique, Long>, JpaSpecificationExecutor<GammeTechniqueImprimerHistorique> {

}
