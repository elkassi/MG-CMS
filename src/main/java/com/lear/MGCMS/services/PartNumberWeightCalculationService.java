package com.lear.MGCMS.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.domain.PartNumberMaterialConfig;
import com.lear.MGCMS.domain.PieceDetail;
import com.lear.MGCMS.repositories.PartNumberInfoRepository;
import com.lear.MGCMS.repositories.PartNumberMaterialConfigRepository;
import com.lear.MGCMS.repositories.PieceDetailRepository;
import com.lear.ctc.domain.Files;
import com.lear.ctc.repositories.FilesRepository;

@Service
public class PartNumberWeightCalculationService {

	@Autowired
	private FilesRepository filesRepository;

	@Autowired
	private PieceDetailRepository pieceDetailRepository;

	@Autowired
	private PartNumberMaterialConfigRepository partNumberMaterialConfigRepository;

	@Autowired
	private PartNumberInfoRepository partNumberInfoRepository;

	/**
	 * Calculate weight for a list of partNumberCover values.
	 * For each partNumberCover:
	 *   1. Get fabric files from Files entity
	 *   2. For each file, look up PieceDetail for the area and PartNumberMaterialConfig for weightUnit
	 *   3. Calculate: area(cm²)/10000 * quantity * weightUnit(kg/m²) = weight in kg
	 *   4. Save total weight to PartNumberInfo
	 */
	public List<Map<String, Object>> calculateWeights(List<String> partNumberCovers) {
		List<Map<String, Object>> results = new ArrayList<>();

		for (String partNumberCover : partNumberCovers) {
			Map<String, Object> result = new HashMap<>();
			result.put("partNumberCover", partNumberCover);
			List<String> errors = new ArrayList<>();
			List<Map<String, Object>> patterns = new ArrayList<>();

			// Step 1: Get fabric files
			List<Files> files = filesRepository.findByTypeAndPartNumberCover("fabric", partNumberCover);
			if (files == null || files.isEmpty()) {
				errors.add("Aucun fichier tissu trouvé pour: " + partNumberCover);
				result.put("totalWeight", null);
				result.put("patterns", patterns);
				result.put("errors", errors);
				results.add(result);
				continue;
			}

			double totalWeight = 0;
			double totalPerimeter = 0;

			for (Files file : files) {
				Map<String, Object> patternDetail = new HashMap<>();
				patternDetail.put("pattern", file.getPattern());
				patternDetail.put("partNumberMaterial", file.getPartNumberMaterial());
				patternDetail.put("quantity", file.getQuantity());

				// Step 2a: Look up PieceDetail by pattern
				List<PieceDetail> pieceDetails = pieceDetailRepository.findByDescripContaining(file.getPattern());
				PieceDetail pieceDetail = null;
				if (pieceDetails != null && !pieceDetails.isEmpty()) {
					pieceDetail = pieceDetails.get(0);
				} else {
					// Try exact match by pieceName
					Optional<PieceDetail> opt = pieceDetailRepository.findByPieceName(file.getPattern());
					if (opt.isPresent()) {
						pieceDetail = opt.get();
					}
				}

				if (pieceDetail == null) {
					errors.add("Pattern non trouvé: " + file.getPattern());
					patternDetail.put("area", null);
					patternDetail.put("weightUnit", null);
					patternDetail.put("weightContribution", null);
					patterns.add(patternDetail);
					continue;
				}

				patternDetail.put("area", pieceDetail.getArea());
				if (pieceDetail.getPerimeter() != null) {
					totalPerimeter += pieceDetail.getPerimeter();
				}

				// Step 2b: Look up PartNumberMaterialConfig for weightUnit
				PartNumberMaterialConfig materialConfig = partNumberMaterialConfigRepository.findByObjId(file.getPartNumberMaterial());
				if (materialConfig == null || materialConfig.getWeightUnit() == null) {
					errors.add("Poids unitaire non configuré pour le matériau: " + file.getPartNumberMaterial());
					patternDetail.put("weightUnit", null);
					patternDetail.put("weightContribution", null);
					patterns.add(patternDetail);
					continue;
				}

				patternDetail.put("weightUnit", materialConfig.getWeightUnit());

				// Step 3: Calculate weight contribution
				double area = pieceDetail.getArea() != null ? pieceDetail.getArea() : 0;
				int quantity = file.getQuantity() != null ? file.getQuantity() : 1;
				double weightUnit = materialConfig.getWeightUnit();
				double weightContribution = (area / 10000.0) * quantity * weightUnit;

				patternDetail.put("weightContribution", weightContribution);
				totalWeight += weightContribution;
				patterns.add(patternDetail);
			}

			result.put("totalWeight", errors.isEmpty() ? totalWeight : null);
			result.put("calculatedWeight", totalWeight);
			result.put("totalPerimeter", totalPerimeter);
			result.put("patterns", patterns);
			result.put("errors", errors);

			// Step 4: Save to PartNumberInfo if no errors
			if (errors.isEmpty()) {
				PartNumberInfo info = partNumberInfoRepository.findByPartNumber(partNumberCover);
				if (info == null) {
					info = new PartNumberInfo();
					info.setPartNumber(partNumberCover);
				}
				info.setWeight(totalWeight);
				if (totalPerimeter > 0) {
					info.setTotalPerimetre(totalPerimeter);
				}
				partNumberInfoRepository.save(info);
				result.put("saved", true);
			} else {
				result.put("saved", false);
			}

			results.add(result);
		}

		return results;
	}
}
