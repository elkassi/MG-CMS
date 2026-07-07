package com.lear.MGCMS.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.GammeTechniqueText;
import com.lear.MGCMS.repositories.GammeTechniqueTextRepository;

@Service
public class GammeTechniqueTextService {

	@Autowired
	private GammeTechniqueTextRepository repository;

	public List<GammeTechniqueText> resolve(String partNumber, List<String> patterns) {
		List<String> normalizedPatterns = normalizePatterns(patterns);
		if (normalizedPatterns.isEmpty()) {
			return repository.findExactByPartNumber(partNumber);
		}
		return repository.resolveForPartNumberAndPatterns(partNumber, normalizedPatterns);
	}

	@Transactional
	public List<GammeTechniqueText> replaceForPartNumber(String partNumber, List<GammeTechniqueText> texts, List<Long> deletedIds, String username) {
		if (deletedIds != null && !deletedIds.isEmpty()) {
			repository.deleteByIds(deletedIds);
		}

		repository.deleteExactByPartNumber(partNumber);

		List<GammeTechniqueText> saved = new ArrayList<>();
		if (texts != null) {
			for (GammeTechniqueText text : texts) {
				if (text == null || text.getContent() == null || text.getContent().trim().isEmpty()) {
					continue;
				}

				if (Boolean.TRUE.equals(text.getApplyToPattern())) {
					saved.add(savePatternText(partNumber, text, username));
				} else {
					GammeTechniqueText exactText = new GammeTechniqueText();
					copyEditableFields(text, exactText);
					exactText.setId(null);
					exactText.setPartNumber(partNumber);
					exactText.setApplyToPattern(false);
					exactText.setCreatedBy(username);
					exactText.setCreatedAt(LocalDateTime.now());
					saved.add(repository.save(exactText));
				}
			}
		}

		List<String> savedPatterns = saved.stream()
			.map(GammeTechniqueText::getPattern)
			.filter(pattern -> pattern != null && !pattern.trim().isEmpty())
			.collect(Collectors.toList());

		return resolve(partNumber, savedPatterns);
	}

	public void deleteExactByPartNumber(String partNumber) {
		repository.deleteExactByPartNumber(partNumber);
	}

	private GammeTechniqueText savePatternText(String partNumber, GammeTechniqueText source, String username) {
		GammeTechniqueText target = new GammeTechniqueText();
		if (source.getId() != null) {
			Optional<GammeTechniqueText> existing = repository.findById(source.getId());
			if (existing.isPresent()) {
				target = existing.get();
			}
		}

		boolean existingRecord = target.getId() != null;
		copyEditableFields(source, target);
		target.setApplyToPattern(true);
		if (target.getPartNumber() == null || target.getPartNumber().trim().isEmpty()) {
			target.setPartNumber(partNumber);
		}
		if (existingRecord) {
			target.setUpdatedBy(username);
			target.setUpdatedAt(LocalDateTime.now());
		} else {
			target.setCreatedBy(username);
			target.setCreatedAt(LocalDateTime.now());
		}
		return repository.save(target);
	}

	private void copyEditableFields(GammeTechniqueText source, GammeTechniqueText target) {
		target.setPartNumber(source.getPartNumber());
		target.setPanelNumber(trimToNull(source.getPanelNumber()));
		target.setPartNumberMaterial(trimToNull(source.getPartNumberMaterial()));
		target.setPattern(trimToNull(source.getPattern()));
		target.setContent(source.getContent().trim());
		target.setLabelX(source.getLabelX());
		target.setLabelY(source.getLabelY());
		target.setLabelSize(source.getLabelSize());
		target.setFontFamily(trimToNull(source.getFontFamily()));
		target.setFontWeight(trimToNull(source.getFontWeight()));
		target.setFontStyle(trimToNull(source.getFontStyle()));
		target.setFillColor(trimToNull(source.getFillColor()));
		target.setRotation(source.getRotation());
	}

	private List<String> normalizePatterns(List<String> patterns) {
		Set<String> result = new LinkedHashSet<>();
		if (patterns != null) {
			for (String pattern : patterns) {
				String normalized = trimToNull(pattern);
				if (normalized != null) {
					result.add(normalized.toUpperCase());
				}
			}
		}
		return new ArrayList<>(result);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
