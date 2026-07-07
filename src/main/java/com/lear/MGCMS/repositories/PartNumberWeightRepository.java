package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.PartNumberWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartNumberWeightRepository extends JpaRepository<PartNumberWeight, Long>, JpaSpecificationExecutor<PartNumberWeight> {

    Optional<PartNumberWeight> findByPartnumber(String partnumber);

    boolean existsByPartnumber(String partnumber);
}
