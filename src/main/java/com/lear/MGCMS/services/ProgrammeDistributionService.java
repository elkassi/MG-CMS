package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.ProgrammeDistribution;
import com.lear.MGCMS.repositories.ProgrammeDistributionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProgrammeDistributionService {

    @Autowired
    private ProgrammeDistributionRepository repository;

    public ProgrammeDistribution save(ProgrammeDistribution programmeDistribution) {
        // If saving a new record, check if machine+programmeNumber already exists
        if (programmeDistribution.getId() == null || programmeDistribution.getId() == 0) {
            Long machineId = programmeDistribution.getMachine() != null ? programmeDistribution.getMachine().getId() : null;
            if (machineId != null && programmeDistribution.getProgrammeNumber() != null) {
                ProgrammeDistribution existing = repository.findFirstByMachineIdAndProgrammeNumber(
                        machineId, programmeDistribution.getProgrammeNumber());
                if (existing != null) {
                    programmeDistribution.setId(existing.getId());
                }
            }
        }
        return repository.save(programmeDistribution);
    }

    public Optional<ProgrammeDistribution> findById(Long id) {
        return repository.findById(id);
    }

    public void delete(ProgrammeDistribution programmeDistribution) {
        repository.delete(programmeDistribution);
    }

    public List<ProgrammeDistribution> findAll() {
        return repository.findAll();
    }

    public Page<ProgrammeDistribution> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr.length > 1 ? sortArr[1] : "desc";
        Sort.Direction evalDirection = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<ProgrammeDistribution> specification = buildSpecification(filters);
        return repository.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
    }

    private Specification<ProgrammeDistribution> buildSpecification(Map<String, String> filters) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String[] strArr = entry.getKey().split("\\.");
                if (strArr.length >= 2) {
                    String operation = strArr[0];
                    Path<String> path = root.get(strArr[1]);
                    for (int i = 2; i < strArr.length; i++) {
                        path = path.get(strArr[i]);
                    }
                    String value = entry.getValue();

                    switch (operation) {
                        case "contains":
                            predicates.add(builder.like(builder.lower(path), "%" + value.toLowerCase() + "%"));
                            break;
                        case "startWith":
                            predicates.add(builder.like(builder.lower(path), value.toLowerCase() + "%"));
                            break;
                        case "endWith":
                            predicates.add(builder.like(builder.lower(path), "%" + value.toLowerCase()));
                            break;
                        case "equal":
                            predicates.add(builder.equal(builder.lower(path), value.toLowerCase()));
                            break;
                        case "notEqual":
                            predicates.add(builder.notEqual(builder.lower(path), value.toLowerCase()));
                            break;
                        case "isNull":
                            predicates.add(builder.isNull(path));
                            break;
                        case "isNotNull":
                            predicates.add(builder.isNotNull(path));
                            break;
                        default:
                            break;
                    }
                }
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
