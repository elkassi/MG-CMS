package com.lear.MGCMS.repositories;

import java.util.List;
import java.util.Optional;

import com.lear.MGCMS.domain.ConfigSeriePlus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lear.MGCMS.domain.PieceDetail;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PieceDetailRepository extends JpaRepository<PieceDetail, String>, JpaSpecificationExecutor<PieceDetail> {

	List<PieceDetail> findByDescripContaining(String descrip);

	Optional<PieceDetail> findByPieceName(String pieceName);
}
