package com.lear.MGCMS.services.CuttingRequest.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieDataLight;
import com.lear.MGCMS.payload.SerieReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.dispatcher.ContinuousDispatchOptimizerService;
import com.lear.MGCMS.services.dispatcher.SerieStatusDateValidator;
import com.lear.MGCMS.services.dispatcher.WorkbenchCacheService;

@Service
public class CuttingRequestSerieDataService {

    @Autowired
    private ApplicationContext context;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CuttingRequestSerieDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Autowired
    private CuttingRequestSerieDataRepository repo;

    @Autowired(required = false)
    private SerieStatusDateValidator serieStatusDateValidator;

    @Autowired(required = false)
    private ContinuousDispatchOptimizerService optimizerService;

    @Autowired(required = false)
    private WorkbenchCacheService workbenchCacheService;

    @Autowired(required = false)
    private OrdonnancementService ordonnancementService;

    public CuttingRequestSerieData findById(String sequence) {
        Optional<CuttingRequestSerieData> obj = repo.findById(sequence);
        if (!obj.isPresent()) {
            return null;
        }
        return obj.get();
    }

    public CuttingRequestSerieData save(CuttingRequestSerieData obj) {
        CuttingRequestSerieData saved = repo.save(obj);
        afterProductionDataChange();
        return saved;
    }

    public void delete(CuttingRequestSerieData obj) {
        repo.delete(obj);
        afterProductionDataChange();
    }

    private void afterProductionDataChange() {
        try {
            if (serieStatusDateValidator != null) {
                serieStatusDateValidator.normalizeProductionProgress();
            }
            if (ordonnancementService != null) {
                ordonnancementService.invalidateTimelineCache();
            }
            if (workbenchCacheService != null) {
                workbenchCacheService.invalidateAll();
            }
            if (optimizerService != null) {
                optimizerService.reloadActiveSnapshotFromGroundTruth();
            }
        } catch (Exception ignored) {
            // Production imports must not fail because a derived cache refresh failed.
        }
    }

    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

    public Page<CuttingRequestSerieData> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<CuttingRequestSerieData> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add filters based on the key-value pairs in the 'filters' map
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
                String[] strArr = entry.getKey().split("\\.");

                if (strArr.length >= 2) {
                    Path<String> path = root.get(strArr[1]);
                    for (int i = 2; i < strArr.length; i++) {
                        path = path.get(strArr[i]);
                    }

                    // Handle different data types
                    if (path.getJavaType().equals(String.class)) {
                        System.out.println("String");
                        if (entry.getKey().startsWith("startWith.")) {
                            predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("endWith.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue()));
                        } else if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("contains.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
                        }
                    } else if (path.getJavaType().equals(Boolean.class)) {
                        String valueEntry = entry.getValue();
                        if (valueEntry.equalsIgnoreCase("1")) {
                            valueEntry = "TRUE";
                        }
                        if (valueEntry.equalsIgnoreCase("0")) {
                            valueEntry = "FALSE";
                        }
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
                        }
                    } else if (path.getJavaType().equals(LocalDate.class)) {
                        System.out.println("LocalDate");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        }
//                        else if (entry.getKey().startsWith("startWith.")) {
//                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS".substring(0, entry.getValue().length())));
//                            Integer lg = entry.getValue().length() ;
//                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
//                            switch(lg) {
//                            	case 1 :
//                            		endDateTime = startDateTime.plusYears(1000).minusNanos(1);break;
//                            	case 2 :
//                            		endDateTime = startDateTime.plusYears(100).minusNanos(1);break;
//                            	case 3:
//                            		endDateTime = startDateTime.plusYears(10).minusNanos(1);break;
//
//                            }
//                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
//                        }
                    } else if (path.getJavaType().equals(LocalDateTime.class)) {
                        System.out.println("LocalDateTime");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("startWith.")) {
                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue());
                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
                        }
                    } else if (path.getJavaType().equals(Integer.class)) {
                        // Handle Integer conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Double.class)) {
                        // Handle Double conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Long.class)) {
                        // Handle Long conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        }
                    }

                    if (entry.getKey().startsWith("isNull.")) {
                        predicates.add(builder.isNull(path));
                    } else if (entry.getKey().startsWith("isNotNull.")) {
                        predicates.add(builder.isNotNull(path));
                    }

                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
    }

    public Integer countStartedBySequence(String sequence) {
        return repo.countStartedBySequence(sequence);
    }

    public Integer countNonFinishedBySequence(String sequence) {
        return repo.countNonFinishedBySequence(sequence);
    }

    public List<CuttingRequestSerieData> findSeries(List<String> series) {
        return repo.findSeries(series);
    }

    public List<String> findDistinctPlacement() {
        return repo.findDistinctPlacement();
    }

    public void updateNbrPiece(String placement, Integer countNbrPiece) {
        repo.updateNbrPiece(placement, countNbrPiece);
    }

    public List<String> findSeries(String sequence, String partNumberMaterial) {
        return repo.findSeries(sequence, partNumberMaterial);
    }

    public List<CuttingRequestSerieData> findBySequence(String sequence) {
        return repo.findBySequence(sequence);
    }

    public List<CuttingRequestSerieData> findBySequencesArr(List<String> sequencesArr) {
        return repo.findBySequencesArr(sequencesArr);
    }

    public Long getMaxNserie() {
        Long maxNserie = repo.getMaxNserie();
        if (maxNserie == null) {
            return 0L;
        }
        return maxNserie;
    }

    public List<CuttingRequestSerieDataLight> findBySequences(List<String> sequences) {
        if (sequences == null || sequences.isEmpty()) {
            return new ArrayList<>();
        }
        return repo.findBySequences(sequences);
    }

    public List<SerieReport> getReportByPartNumberMaterial(List<String> partNumberMaterials) {
        // Utiliser le nom réel de la table de la base de données au lieu du nom de la classe
        String sql = "select serie, placement, partNumberMaterial, longueur, laize, nbrCouche, perimetre, " +
                "longueur * nbrCouche as 'Longueur'" +
               // ", (longueur * nbrCouche) * 1000 / perimetre as 'Indicateur' " +
                "from [dbo].[CuttingRequestSerie] where statusMatelassage = 'Waiting'  and partNumberMaterial in (";
               // " and perimetre is not null and perimetre > 0 " +


        for (int i = 0; i < partNumberMaterials.size(); i++) {
            sql += "'" + partNumberMaterials.get(i) + "'";
            if (i < partNumberMaterials.size() - 1) {
                sql += ", ";
            }
        }
        sql += ") order by partNumberMaterial";

        List<SerieReport> reports = new ArrayList<>();
        try {
            List<SerieReport> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
                String serie = rs.getString("serie");
                String placement = rs.getString("placement");
                String partNumberMaterial = rs.getString("partNumberMaterial");
                Double longueur = rs.getDouble("longueur");
                Double laize = rs.getDouble("laize");
                Integer nbrCouche = rs.getInt("nbrCouche");
                Double perimetre = rs.getDouble("perimetre");
                Double longueurTotal = rs.getDouble("Longueur");
//                Double indicateur = rs.getDouble("Indicateur");

                return new SerieReport(serie, placement, partNumberMaterial, longueur, laize, nbrCouche, perimetre, longueurTotal, (perimetre != null && perimetre > 0) ? (longueur * nbrCouche)*1000/perimetre : 0.0 );
            });
            reports.addAll(result);

        } catch (DeadlockLoserDataAccessException e) {
            System.err.println("Deadlock occurred: " + e.getMessage());
        }
        return reports;
    }
}
