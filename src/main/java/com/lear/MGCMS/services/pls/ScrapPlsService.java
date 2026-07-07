package com.lear.MGCMS.services.pls;

import com.lear.pls.domain.Scrap;
import com.lear.pls.repositories.ScrapPlsRepository;
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

@Service
public class ScrapPlsService {

	@Autowired
	private ScrapPlsRepository repo;

	public Scrap save(Scrap obj) {
		return repo.save(obj);
	}

	public Page<Scrap> findAll(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

		Specification<Scrap> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			for (Map.Entry<String, String> entry : filters.entrySet()) {
				String[] strArr = entry.getKey().split("\\.");
				if (strArr.length >= 2) {
					Path<String> path = root.get(strArr[1]);
					for (int i = 2; i < strArr.length; i++) {
						path = path.get(strArr[i]);
					}
					if (path.getJavaType().equals(String.class)) {
						if (entry.getKey().startsWith("startWith.")) {
							predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
						} else if (entry.getKey().startsWith("contains.")) {
							predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
						} else if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(String.class), entry.getValue()));
						}
					} else if (path.getJavaType().equals(Boolean.class)) {
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(Boolean.class),
									entry.getValue().equals("True") || entry.getValue().equals("1")));
						}
					} else if (path.getJavaType().equals(LocalDateTime.class)) {
						if (entry.getKey().startsWith("greaterThan.")) {
							predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
						} else if (entry.getKey().startsWith("lessThan.")) {
							predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
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

	public Scrap findById(String id) {
		return repo.findScrapById(id);
	}
}
