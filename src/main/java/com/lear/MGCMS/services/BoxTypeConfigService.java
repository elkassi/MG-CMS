package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.BoxTypeConfig;
import com.lear.MGCMS.repositories.BoxTypeConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BoxTypeConfigService {

    @Autowired
    private BoxTypeConfigRepository boxTypeConfigRepository;

    public List<BoxTypeConfig> findAll() {
        return boxTypeConfigRepository.findAll();
    }

    public Optional<BoxTypeConfig> findById(Long id) {
        return boxTypeConfigRepository.findById(id);
    }

    public Optional<BoxTypeConfig> findByBoxType(String boxType) {
        return boxTypeConfigRepository.findByBoxType(boxType);
    }

    public BoxTypeConfig save(BoxTypeConfig boxTypeConfig) {
        return boxTypeConfigRepository.save(boxTypeConfig);
    }

    public void deleteById(Long id) {
        boxTypeConfigRepository.deleteById(id);
    }

    public Double getEmptyBoxWeight(String boxType) {
        return findByBoxType(boxType)
                .map(BoxTypeConfig::getEmptyBoxWeight)
                .orElse(0.0);
    }
}
