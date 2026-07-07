package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.ProgramCNC;
import com.lear.MGCMS.domain.ProgramCNCHistory;
import com.lear.MGCMS.repositories.ProgramCNCHistoryRepository;
import com.lear.MGCMS.repositories.ProgramCNCRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProgramCNCService {

    @Autowired
    private ProgramCNCRepository programCNCRepository;

    @Autowired
    private ProgramCNCHistoryRepository programCNCHistoryRepository;

    /** Save (create or update) and record the change in the history. Duplicates are allowed. */
    public ProgramCNC save(ProgramCNC programCNC, String username) {
        boolean isUpdate = programCNC.getId() != null && programCNC.getId() != 0;
        programCNC.setUpdatedAt(LocalDateTime.now());
        programCNC.setUpdatedBy(username);
        ProgramCNC saved = programCNCRepository.save(programCNC);
        recordHistory(username, isUpdate ? "UPDATE" : "CREATION", saved.toString());
        return saved;
    }

    public ProgramCNC findById(Long id) {
        Optional<ProgramCNC> opt = programCNCRepository.findById(id);
        return opt.orElse(null);
    }

    public void delete(ProgramCNC programCNC, String username) {
        // Snapshot the persisted row so the history reflects the full deleted record.
        ProgramCNC existing = programCNC.getId() != null ? findById(programCNC.getId()) : null;
        String snapshot = (existing != null ? existing : programCNC).toString();
        programCNCRepository.delete(programCNC);
        recordHistory(username, "DELETE", snapshot);
    }

    /** Bulk delete (one history row per deleted record). */
    public void deleteAll(List<ProgramCNC> list, String username) {
        for (ProgramCNC p : list) {
            recordHistory(username, "DELETE", p.toString());
        }
        programCNCRepository.deleteAll(list);
    }

    private void recordHistory(String username, String operation, String snapshot) {
        programCNCHistoryRepository.save(new ProgramCNCHistory(LocalDateTime.now(), username, operation, snapshot));
    }

    public List<ProgramCNC> findList(Map<String, String> filters) {
        Specification<ProgramCNC> specification = buildSpecification(filters);
        return programCNCRepository.findAll(specification);
    }

    public Page<ProgramCNC> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr.length > 1 ? sortArr[1] : "desc";
        Sort.Direction evalDirection = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<ProgramCNC> specification = buildSpecification(filters);

        return programCNCRepository.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
    }

    private Specification<ProgramCNC> buildSpecification(Map<String, String> filters) {
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
