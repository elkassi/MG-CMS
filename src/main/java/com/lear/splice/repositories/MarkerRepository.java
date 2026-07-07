package com.lear.splice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.splice.domain.Marker;

import java.util.List;


public interface MarkerRepository extends JpaRepository<Marker, Long>, JpaSpecificationExecutor<Marker> {

    List<Marker> findByMarker(String placement);
}
