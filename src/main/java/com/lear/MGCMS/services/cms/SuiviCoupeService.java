package com.lear.MGCMS.services.cms;

import com.lear.cms.domain.SuiviCoupe;
import com.lear.cms.repositories.SuiviCoupeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuiviCoupeService {

    @Autowired
    private SuiviCoupeRepository repository;

    public List<SuiviCoupe> findByNof(String nof) {
        return repository.findByNof(nof);
    }
}
