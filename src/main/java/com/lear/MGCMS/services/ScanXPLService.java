package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.ScanXPL;
import com.lear.MGCMS.repositories.ScanXPLRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ScanXPLService {

    @Autowired
    private ScanXPLRepository repo;

    public ScanXPL findById(Long id) {
        Optional<ScanXPL> obj = repo.findById(id);
        return obj.orElse(null);
    }

    public ScanXPL save(ScanXPL obj) {
        return repo.save(obj);
    }

    public void delete(ScanXPL obj) {
        repo.delete(obj);
    }

    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

    public List<ScanXPL> findAll() {
        return repo.findAll();
    }

    public List<ScanXPL> findBySerie(String serie) {
        return repo.findBySerie(serie);
    }

    public List<ScanXPL> findByMachine(String machine) {
        return repo.findByMachine(machine);
    }

    public List<ScanXPL> findByMachineAndDateRange(String machine, LocalDateTime startDate, LocalDateTime endDate) {
        return repo.findByMachineAndDateRange(machine, startDate, endDate);
    }

    public List<ScanXPL> findByMachinesAndDateRange(List<String> machines, LocalDateTime startDate, LocalDateTime endDate) {
        return repo.findByMachinesAndDateRange(machines, startDate, endDate);
    }

    public List<ScanXPL> findBySerieIn(List<String> series) {
        return repo.findBySerieIn(series);
    }

    public List<String> findDistinctSeriesByMachinesAndDateRange(List<String> machines, LocalDateTime startDate, LocalDateTime endDate) {
        return repo.findDistinctSeriesByMachinesAndDateRange(machines, startDate, endDate);
    }

    public Page<ScanXPL> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        PageRequest pageRequest = PageRequest.of(page, size, evalDirection, evalSort);

        Specification<ScanXPL> specification = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    String[] keyParts = key.split("\\.");
                    String filterType = keyParts[0];
                    String fieldName = keyParts.length > 1 ? keyParts[1] : null;

                    if (fieldName != null) {
                        Path<?> path = root.get(fieldName);
                        Class<?> javaType = path.getJavaType();

                        switch (filterType) {
                            case "equal":
                                if (javaType == Boolean.class || javaType == boolean.class) {
                                    predicates.add(criteriaBuilder.equal(path, Boolean.parseBoolean(value)));
                                } else if (javaType == Long.class || javaType == long.class) {
                                    predicates.add(criteriaBuilder.equal(path, Long.parseLong(value)));
                                } else if (javaType == Integer.class || javaType == int.class) {
                                    predicates.add(criteriaBuilder.equal(path, Integer.parseInt(value)));
                                } else {
                                    predicates.add(criteriaBuilder.equal(path, value));
                                }
                                break;
                            case "like":
                                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(path.as(String.class)),
                                        "%" + value.toLowerCase() + "%"));
                                break;
                            case "startswith":
                                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(path.as(String.class)),
                                        value.toLowerCase() + "%"));
                                break;
                            case "date":
                                LocalDate localDate = LocalDate.parse(value);
                                LocalDateTime startOfDay = localDate.atStartOfDay();
                                LocalDateTime endOfDay = localDate.atTime(23, 59, 59);
                                predicates.add(criteriaBuilder.between(
                                        root.get(fieldName).as(LocalDateTime.class),
                                        startOfDay, endOfDay));
                                break;
                            case "dategte":
                                LocalDate gteDate = LocalDate.parse(value);
                                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                        root.get(fieldName).as(LocalDateTime.class),
                                        gteDate.atStartOfDay()));
                                break;
                            case "datelte":
                                LocalDate lteDate = LocalDate.parse(value);
                                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                        root.get(fieldName).as(LocalDateTime.class),
                                        lteDate.atTime(23, 59, 59)));
                                break;
                        }
                    }
                }
            });

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(specification, pageRequest);
    }
}
