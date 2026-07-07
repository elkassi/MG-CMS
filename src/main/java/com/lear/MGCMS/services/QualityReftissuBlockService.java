package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.QualityReftissuBlock;
import com.lear.MGCMS.repositories.QualityReftissuBlockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Service
public class QualityReftissuBlockService {

    @Autowired
    private QualityReftissuBlockRepository repo;

    public QualityReftissuBlock save(QualityReftissuBlock obj) {
        return repo.save(obj);
    }

    public List<QualityReftissuBlock> findAll() {
        return repo.findAll();
    }

    public void delete(QualityReftissuBlock obj) {
        repo.delete(obj);
    }

    public QualityReftissuBlock findByReftissu(String reftissu) {
        return repo.findByReftissu(reftissu);
    }
}
