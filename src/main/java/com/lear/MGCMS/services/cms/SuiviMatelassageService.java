package com.lear.MGCMS.services.cms;

import com.lear.cms.domain.SuiviMatelassage;
import com.lear.cms.repositories.SuiviMatelassageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuiviMatelassageService {

    @Autowired
    private SuiviMatelassageRepository repository;

    public List<SuiviMatelassage> findByNof(String nof) {
        return repository.findByNof(nof);
    }

    
}
