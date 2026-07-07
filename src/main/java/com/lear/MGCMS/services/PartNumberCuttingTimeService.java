package com.lear.MGCMS.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.repositories.PartNumberInfoRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialPlacementRepository;

@Service
public class PartNumberCuttingTimeService {

	@Autowired
	private CuttingPlanMaterialPlacementRepository placementRepository;

	@Autowired
	private PartNumberInfoRepository partNumberInfoRepository;

	/**
	 * Calculate cutting time per partNumber based on perimeter proportion
	 * within active cutting plans for a given project.
	 * Uses a lightweight query to avoid loading the full CuttingPlan object graph.
	 */
	public List<Map<String, Object>> calculateCuttingTimeByProject(String project, LocalDate startDate, LocalDate endDate) {
		System.out.println("[CuttingTime] Starting calculation for project: " + project
				+ " | startDate=" + startDate + " | endDate=" + endDate);

		List<Map<String, Object>> results = new ArrayList<>();

		// Step 1: Load only needed fields from active placements (lightweight query)
		System.out.println("[CuttingTime] Loading active placements (lightweight)...");
		List<Object[]> rows = placementRepository.findActiveByProjet(project, LocalDateTime.now());

		if (rows == null || rows.isEmpty()) {
			System.out.println("[CuttingTime] No active placements found for project: " + project);
			Map<String, Object> error = new HashMap<>();
			error.put("error", "Aucun plan de coupe actif pour le projet: " + project);
			results.add(error);
			return results;
		}
		System.out.println("[CuttingTime] Found " + rows.size() + " active placement rows.");

		// Step 2: Group rows by cuttingPlanId
		Map<Long, String> planDescriptions = new HashMap<>();
		// planId -> (partNumber -> perimetre)
		Map<Long, Map<String, Double>> planPerimetrePerPN = new HashMap<>();
		// planId -> totalTempsDeCoupe
		Map<Long, Double> planTempsDeCoupe = new HashMap<>();

		for (Object[] row : rows) {
			Long planId = ((Number) row[0]).longValue();
			String planDesc = row[1] != null ? row[1].toString() : "";
			String partNumbersRaw = row[2] != null ? row[2].toString() : "";
			double perimetre = row[3] != null ? ((Number) row[3]).doubleValue() : 0;
			double tempsDeCoupe = row[4] != null ? ((Number) row[4]).doubleValue() : 0;

			planDescriptions.put(planId, planDesc);
			planTempsDeCoupe.merge(planId, tempsDeCoupe, Double::sum);

			if (!partNumbersRaw.isEmpty()) {
				String[] pnList = partNumbersRaw.split(",");
				for (String pn : pnList) {
					String trimmedPN = pn.trim();
					if (!trimmedPN.isEmpty()) {
						planPerimetrePerPN
								.computeIfAbsent(planId, k -> new HashMap<>())
								.merge(trimmedPN, perimetre, Double::sum);
					}
				}
			}
		}

		System.out.println("[CuttingTime] Found " + planPerimetrePerPN.size() + " active plan(s) to process.");

		// Step 3: For each plan, compute and save cutting time per PN
		int planIndex = 0;
		for (Map.Entry<Long, Map<String, Double>> planEntry : planPerimetrePerPN.entrySet()) {
			planIndex++;
			Long planId = planEntry.getKey();
			Map<String, Double> perimetrePerPN = planEntry.getValue();
			String planDesc = planDescriptions.getOrDefault(planId, "");
			double totalTempsDeCoupe = planTempsDeCoupe.getOrDefault(planId, 0.0);

			System.out.println("[CuttingTime] Processing plan " + planIndex + "/" + planPerimetrePerPN.size()
					+ " | id=" + planId + " | desc=" + planDesc
					+ " | partNumbers=" + perimetrePerPN.size());

			Map<String, Object> planResult = new HashMap<>();
			planResult.put("cuttingPlanId", planId);
			planResult.put("cuttingPlanDescription", planDesc);
			List<String> errors = new ArrayList<>();

			double totalPerimetre = perimetrePerPN.values().stream().mapToDouble(Double::doubleValue).sum();
			planResult.put("totalPerimetre", totalPerimetre);
			planResult.put("totalTempsDeCoupe", totalTempsDeCoupe);

			if (totalPerimetre == 0) {
				errors.add("Pas de données périmètre pour le plan: " + planId);
			}

			List<Map<String, Object>> pnResults = new ArrayList<>();
			int pnIndex = 0;
			for (Map.Entry<String, Double> entry : perimetrePerPN.entrySet()) {
				pnIndex++;
				String partNumber = entry.getKey();
				double perimetrePN = entry.getValue();
				double percentage = totalPerimetre > 0 ? (perimetrePN / totalPerimetre) * 100 : 0;
				double tempsCoupePN = totalPerimetre > 0 ? totalTempsDeCoupe * (perimetrePN / totalPerimetre) : 0;

				Map<String, Object> pnResult = new HashMap<>();
				pnResult.put("partNumber", partNumber);
				pnResult.put("perimetrePN", perimetrePN);
				pnResult.put("percentagePerimetre", Math.round(percentage * 100.0) / 100.0);
				pnResult.put("tempsCoupePN", Math.round(tempsCoupePN * 100.0) / 100.0);

				// Save to PartNumberInfo
				if (totalPerimetre > 0) {
					PartNumberInfo info = partNumberInfoRepository.findByPartNumber(partNumber);
					if (info == null) {
						info = new PartNumberInfo();
						info.setPartNumber(partNumber);
					}
					info.setTotalPerimetre(perimetrePN);
					info.setTempsDeCoupe(tempsCoupePN);
					partNumberInfoRepository.save(info);
					pnResult.put("saved", true);
				} else {
					pnResult.put("saved", false);
				}

				if (pnIndex % 10 == 0 || pnIndex == perimetrePerPN.size()) {
					System.out.println("[CuttingTime]   Saved " + pnIndex + "/" + perimetrePerPN.size()
							+ " partNumbers for plan " + planId);
				}

				pnResults.add(pnResult);
			}

			planResult.put("partNumbers", pnResults);
			planResult.put("errors", errors);
			results.add(planResult);

			System.out.println("[CuttingTime] Plan " + planId + " done: "
					+ pnResults.size() + " partNumbers processed.");
		}

		System.out.println("[CuttingTime] Calculation complete. " + results.size() + " plan(s) returned.");
		return results;
	}
}
