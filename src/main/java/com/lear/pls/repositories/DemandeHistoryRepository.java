package com.lear.pls.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.pls.domain.DemandeHistory;


public interface DemandeHistoryRepository extends JpaRepository<DemandeHistory, Long>, JpaSpecificationExecutor<DemandeHistory> {

}
