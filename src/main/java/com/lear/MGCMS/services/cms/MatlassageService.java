package com.lear.MGCMS.services.cms;

import com.lear.cms.domain.Matlassage;
import com.lear.cms.repositories.MatlassageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatlassageService {

    @Autowired
    private MatlassageRepository repository;


    public List<Matlassage> findBySequence(String sequence) {
        return repository.findByNofOrderByNserie(sequence);
    }
}
