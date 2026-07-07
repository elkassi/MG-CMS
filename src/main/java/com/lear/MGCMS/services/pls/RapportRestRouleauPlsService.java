package com.lear.MGCMS.services.pls;

import com.lear.pls.domain.RapportRestRouleau;
import com.lear.pls.repositories.RapportRestRouleauPlsRepository;
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

@Service
public class RapportRestRouleauPlsService {

	@Autowired
	private RapportRestRouleauPlsRepository repo;

	public RapportRestRouleau save(RapportRestRouleau obj) {
		return repo.save(obj);
	}

	public Page<RapportRestRouleau> findAll(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

		Specification<RapportRestRouleau> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			for (Map.Entry<String, String> entry : filters.entrySet()) {
				String[] strArr = entry.getKey().split("\\.");
				if (strArr.length >= 2) {
					Path<String> path = root.get(strArr[1]);
					for (int i = 2; i < strArr.length; i++) {
						path = path.get(strArr[i]);
					}
					if (path.getJavaType().equals(String.class)) {
						if (entry.getKey().startsWith("contains.")) {
							predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
						} else if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(String.class), entry.getValue()));
						}
					} else if (path.getJavaType().equals(Double.class)) {
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(Double.class), Double.parseDouble(entry.getValue())));
						}
					}
				}
			}
			return builder.and(predicates.toArray(new Predicate[0]));
		};

		return repo.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
	}

	public RapportRestRouleau findById(Long id) {
		return repo.findById(id).orElse(null);
	}
}
