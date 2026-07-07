package com.lear.MGCMS.services.cms;

import com.lear.cms.domain.SuiviPlanning;
import com.lear.cms.repositories.SuiviPlanningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuiviPlanningService {

    @Autowired
    private SuiviPlanningRepository repository;

    public SuiviPlanning findBySequence(String sequence) {
        List<SuiviPlanning> list = repository.findListByNSequence(sequence);
        return list.isEmpty() ? null : list.get(0);
    }
}
