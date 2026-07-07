package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.PartNumberValidatedWeight;
import com.lear.MGCMS.repositories.PartNumberValidatedWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartNumberValidatedWeightService {

    @Autowired
    private PartNumberValidatedWeightRepository repo;

    public List<PartNumberValidatedWeight> findAll() {
        return (List<PartNumberValidatedWeight>) repo.findAll();
    }

    public List<PartNumberValidatedWeight> findByPartnumber(String partnumber) {
        return repo.findByPartnumber(partnumber);
    }

    public PartNumberValidatedWeight findLatestByPartnumber(String partnumber) {
        return repo.findFirstByPartnumberOrderByValidatedAtDesc(partnumber);
    }

    public PartNumberValidatedWeight save(PartNumberValidatedWeight obj) {
        return repo.save(obj);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
