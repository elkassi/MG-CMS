package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.DefautRouleau;
import com.lear.MGCMS.repositories.DefautRouleauRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefautRouleauService {

    @Autowired
    private DefautRouleauRepository repo;

    public List<DefautRouleau> findAll() {
        // TODO Auto-generated method stub
        return (List<DefautRouleau>) repo.findAll();
    }

    public List<DefautRouleau> findAllActive() {
        // TODO Auto-generated method stub
        return repo.findAllActive();
    }
}
