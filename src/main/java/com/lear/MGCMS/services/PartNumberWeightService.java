package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.PartNumberWeight;
import com.lear.MGCMS.repositories.PartNumberWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PartNumberWeightService {

    @Autowired
    private PartNumberWeightRepository partNumberWeightRepository;

    public List<PartNumberWeight> findAll() {
        return partNumberWeightRepository.findAll();
    }

    public Page<PartNumberWeight> findAll(Pageable pageable) {
        return partNumberWeightRepository.findAll(pageable);
    }

    public Page<PartNumberWeight> findAll(Specification<PartNumberWeight> spec, Pageable pageable) {
        return partNumberWeightRepository.findAll(spec, pageable);
    }

    public Optional<PartNumberWeight> findById(Long id) {
        return partNumberWeightRepository.findById(id);
    }

    public Optional<PartNumberWeight> findByPartnumber(String partnumber) {
        return partNumberWeightRepository.findByPartnumber(partnumber);
    }

    public PartNumberWeight save(PartNumberWeight partNumberWeight) {
        return partNumberWeightRepository.save(partNumberWeight);
    }

    public void deleteById(Long id) {
        partNumberWeightRepository.deleteById(id);
    }

    public boolean existsByPartnumber(String partnumber) {
        return partNumberWeightRepository.existsByPartnumber(partnumber);
    }
}
