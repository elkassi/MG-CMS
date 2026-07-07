package com.lear.MGCMS.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.HardwareConfig;
import com.lear.MGCMS.repositories.HardwareConfigRepository;

@Service
public class HardwareConfigService {

    @Autowired
    private HardwareConfigRepository repo;

    public HardwareConfig findById(Long id) {
        Optional<HardwareConfig> obj = repo.findById(id);
        if(!obj.isPresent()) {
            return null;
        }
        return obj.get();
    }

    public HardwareConfig save(HardwareConfig obj) {
        return repo.save(obj);
    }

    public void delete(HardwareConfig obj) {
        repo.delete(obj);
    }

    public List<HardwareConfig> findAll() {
        return repo.findAll();
    }

    public List<HardwareConfig> findByMachine(String machine) {
        return repo.findByMachine(machine);
    }

    public List<HardwareConfig> findByType(String type) {
        return repo.findByType(type);
    }

    public List<HardwareConfig> findByMachineAndType(String machine, String type) {
        return repo.findByMachineAndType(machine, type);
    }

    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

    public Page<HardwareConfig> findAll(Map<String, String> filters, int page, int size, String sortBy) {
        String[] sortParams = sortBy.split(",");
        String sortField = sortParams[0];
        String sortDirection = sortParams.length > 1 ? sortParams[1] : "asc";

        PageRequest pageRequest = PageRequest.of(page, size,
            Sort.by(replaceOrderStringThroughDirection(sortDirection), sortField));

        Specification<HardwareConfig> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (value != null && !value.isEmpty()) {
                    Path<String> fieldPath = root.get(key);
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(fieldPath),
                        "%" + value.toLowerCase() + "%"
                    ));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(spec, pageRequest);
    }
}
