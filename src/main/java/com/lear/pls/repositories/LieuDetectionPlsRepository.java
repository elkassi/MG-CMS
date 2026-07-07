package com.lear.pls.repositories;

import com.lear.pls.domain.Demande;
import com.lear.pls.domain.LieuDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface LieuDetectionPlsRepository extends JpaRepository<LieuDetection, Long>, JpaSpecificationExecutor<LieuDetection> {

	LieuDetection getLieuDetectionPlsById(Long id);

	LieuDetection findByNom(String nom);
}
