package com.lear.MGCMS.controller.dispatcher;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lear.MGCMS.services.dispatcher.TableFeedDto;
import com.lear.MGCMS.services.dispatcher.TableFeedRankingService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicRecommendationController {

    @Autowired
    private TableFeedRankingService tableFeedRankingService;

    @Autowired
    private ShiftClock shiftClock;

    /**
     * Returns the best next matelassage series for a given machine/table from
     * the same per-table ranking used by the Process Workbench. This public
     * contract is deliberately strict for CMS-Prod: only series whose
     * statusCoupe/statusMatelassage are both Waiting and whose parent sequence
     * is RELEASED or STARTED can be recommended.
     */
    @GetMapping("/next-series")
    public ResponseEntity<List<Map<String, Object>>> getNextSeries(
            @RequestParam String machine,
            @RequestParam(required = false, defaultValue = "3") int limit) {

        List<Map<String, Object>> result = new ArrayList<>();
        int max = Math.max(1, Math.min(limit, 50));
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();

        List<TableFeedDto.CandidateDto> candidates =
                tableFeedRankingService.recommendForMachine(slot.date, slot.shift, machine, max);
        for (TableFeedDto.CandidateDto candidate : candidates) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("serie", candidate.getSerie());
            item.put("sequence", candidate.getSequence());
            item.put("machine", machine);
            item.put("machineType", candidate.getMachine());
            item.put("zone", candidate.getEffectiveZone());
            item.put("phase", "MATELASSAGE");
            item.put("partNumberMaterial", candidate.getRefTissus());
            item.put("longueur", candidate.getLongueur());
            item.put("nbrCouche", candidate.getNbrCouche());
            item.put("requiredLength", candidate.getRequiredLength());
            item.put("validatedMinutes", candidate.getValidatedMinutes());
            item.put("status", candidate.getStatusCoupe() == null ? "Waiting" : candidate.getStatusCoupe());
            item.put("statusCoupe", candidate.getStatusCoupe() == null ? "Waiting" : candidate.getStatusCoupe());
            item.put("statusMatelassage", candidate.getStatusMatelassage() == null ? "Waiting" : candidate.getStatusMatelassage());
            item.put("sequenceStatus", candidate.getSequenceStatus());
            item.put("lifecycle", "WAITING");
            item.put("score", candidate.getScore());
            item.put("materialInZone", candidate.isMaterialInZone());
            item.put("sameRefTissuMounted", candidate.isSameRefTissuMounted());
            item.put("fitsTableLength", candidate.isFitsTableLength());
            item.put("reasons", candidate.getReasons());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }
}
