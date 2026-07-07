package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.LaminationPls;
import com.lear.MGCMS.repositories.LaminationPlsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LaminationPlsService {

    @Autowired
    private LaminationPlsRepository repo;

    public LaminationPls filter(String reftissu) {
        return repo.findByReftissu(reftissu);
    }
}
