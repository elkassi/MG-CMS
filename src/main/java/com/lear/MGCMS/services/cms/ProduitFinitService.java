package com.lear.MGCMS.services.cms;

import com.lear.cms.domain.ProduitFinit;
import com.lear.cms.repositories.ProduitFinitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProduitFinitService {

    @Autowired
    private ProduitFinitRepository repository;


    public List<ProduitFinit> findBySequence(String sequence) {
        return repository.findBySequence(sequence);
    }

    public ProduitFinit findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<String> findAllSequenceLike(String sequence) {
        return repository.findAllSequenceLike(sequence);
    }

}
