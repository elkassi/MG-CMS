package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.BoxWeight;
import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.repositories.PartNumberInfoRepository;
import com.lear.MGCMS.services.BoxWeightService;
import com.lear.MGCMS.services.PartNumberWeightService;
import com.lear.MGCMS.services.BoxTypeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/boxWeight")
public class BoxWeightController {

    @Autowired
    private BoxWeightService boxWeightService;

    @Autowired
    private PartNumberWeightService partNumberWeightService;

    @Autowired
    private BoxTypeConfigService boxTypeConfigService;

    @Autowired
    private PartNumberInfoRepository partNumberInfoRepository;

    // Get all box weights
    @GetMapping("/list")
    public List<BoxWeight> list() {
        return boxWeightService.findAll();
    }

    // Get paginated list
    @GetMapping("/all")
    public Page<BoxWeight> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sentAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageRequest.of(page, size,
                dir.equals("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());
        return boxWeightService.findAll(pageable);
    }

    // Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<BoxWeight> findById(@PathVariable Long id) {
        Optional<BoxWeight> boxWeight = boxWeightService.findById(id);
        return boxWeight.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get entries by boxId
    @GetMapping("/byBoxId/{boxId}")
    public List<BoxWeight> findByBoxId(@PathVariable String boxId) {
        return boxWeightService.findByBoxId(boxId);
    }

    // Get entries by boxId that are not yet verified
    @GetMapping("/byBoxIdNotVerified/{boxId}")
    public List<BoxWeight> findByBoxIdNotVerified(@PathVariable String boxId) {
        return boxWeightService.findByBoxIdNotVerified(boxId);
    }

    // Get entries sent by current user
    @GetMapping("/mySent")
    public List<BoxWeight> mySent(Authentication authentication) {
        String username = authentication.getName();
        return boxWeightService.findBySentBy(username);
    }

    // Get entries not yet verified
    @GetMapping("/notVerified")
    public List<BoxWeight> notVerified() {
        return boxWeightService.findNotVerified();
    }

    // Get entries by validated status
    @GetMapping("/byValidated/{validated}")
    public List<BoxWeight> byValidated(@PathVariable Boolean validated) {
        return boxWeightService.findByValidated(validated);
    }

    // Create new box weight entry (for ROLE_FILLING_WEIGHT)
    @PostMapping("/fill")
    public ResponseEntity<BoxWeight> fill(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String username = authentication.getName();
        String boxType = (String) payload.get("boxType");
        String boxId = (String) payload.get("boxId");
        Double sentWeight = payload.get("sentWeight") instanceof Integer 
            ? ((Integer) payload.get("sentWeight")).doubleValue() 
            : (Double) payload.get("sentWeight");
        Integer quantity = payload.get("quantity") instanceof Integer
            ? (Integer) payload.get("quantity")
            : (payload.get("quantity") != null ? ((Number) payload.get("quantity")).intValue() : null);
        String partnumber = (String) payload.get("partnumber");

        if (boxType == null || boxId == null || sentWeight == null) {
            return ResponseEntity.badRequest().build();
        }

        BoxWeight boxWeight = new BoxWeight();
        boxWeight.setBoxType(boxType);
        boxWeight.setBoxId(boxId);
        boxWeight.setSentWeight(sentWeight);
        boxWeight.setSentBy(username);
        boxWeight.setSentAt(LocalDateTime.now());
        boxWeight.setQuantity(quantity);

        // Calculate estimated weight if partnumber and quantity are provided
        if (partnumber != null && quantity != null && quantity > 0) {
            Double estimatedWeight = calculateEstimatedWeight(partnumber, quantity, boxType);
            boxWeight.setEstimatedWeight(estimatedWeight);
        }

        boxWeight = boxWeightService.save(boxWeight);
        return ResponseEntity.ok(boxWeight);
    }

    // Calculate estimated weight based on partnumber, quantity, and boxType
    private Double calculateEstimatedWeight(String partnumber, Integer quantity, String boxType) {
        // Priority 1: Try to get weightUnit from PartNumberWeight (manually configured)
        Optional<com.lear.MGCMS.domain.PartNumberWeight> weightConfig = 
            partNumberWeightService.findByPartnumber(partnumber);
        
        if (weightConfig.isPresent() && weightConfig.get().getWeightUnit() != null) {
            Double weightUnit = weightConfig.get().getWeightUnit();
            Double emptyBoxWeight = boxTypeConfigService.getEmptyBoxWeight(boxType);
            return (weightUnit * quantity) + emptyBoxWeight;
        }

        // Priority 2: Try to use CAD-calculated weight from PartNumberInfo
        PartNumberInfo pnInfo = partNumberInfoRepository.findByPartNumber(partnumber);
        if (pnInfo != null && pnInfo.getWeight() != null) {
            Double cadWeight = pnInfo.getWeight(); // weight per piece from CAD calculation
            Double emptyBoxWeight = boxTypeConfigService.getEmptyBoxWeight(boxType);
            return (cadWeight * quantity) + emptyBoxWeight;
        }

        // Priority 3: Calculate average from BoxWeight history (least reliable)
        Double averageWeight = boxWeightService.calculateAverageWeightUnit(partnumber, boxType);
        if (averageWeight != null) {
            Double emptyBoxWeight = boxTypeConfigService.getEmptyBoxWeight(boxType);
            return (averageWeight * quantity) + emptyBoxWeight;
        }

        return null;
    }

    // Get estimated weight for a partnumber
    @GetMapping("/estimateWeight")
    public ResponseEntity<?> estimateWeight(
            @RequestParam String partnumber,
            @RequestParam Integer quantity,
            @RequestParam String boxType) {
        
        if (partnumber == null || quantity == null || quantity <= 0 || boxType == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid parameters"));
        }

        Double estimatedWeight = calculateEstimatedWeight(partnumber, quantity, boxType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("partnumber", partnumber);
        response.put("quantity", quantity);
        response.put("boxType", boxType);
        response.put("estimatedWeight", estimatedWeight);
        
        return ResponseEntity.ok(response);
    }

    // Verify box weight (for ROLE_VERIFYING_WEIGHT)
    @PostMapping("/verify/{id}")
    public ResponseEntity<?> verify(@PathVariable Long id, @RequestBody Map<String, Object> payload, Authentication authentication) {
        String username = authentication.getName();
        Double receivedWeight = payload.get("receivedWeight") instanceof Integer
            ? ((Integer) payload.get("receivedWeight")).doubleValue()
            : (Double) payload.get("receivedWeight");

        if (receivedWeight == null) {
            return ResponseEntity.badRequest().body("receivedWeight is required");
        }

        BoxWeight boxWeight = boxWeightService.verifyBoxWeight(id, receivedWeight, username);
        if (boxWeight == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("boxWeight", boxWeight);
        response.put("difference", Math.abs(boxWeight.getSentWeight() - receivedWeight));
        response.put("validated", boxWeight.getValidated());

        return ResponseEntity.ok(response);
    }

    // Remove last entry (for ROLE_FILLING_WEIGHT)
    @DeleteMapping("/removeLast")
    public ResponseEntity<?> removeLast(Authentication authentication) {
        String username = authentication.getName();
        boolean removed = boxWeightService.removeLastBySentBy(username);
        if (removed) {
            return ResponseEntity.ok().body(Map.of("message", "Last entry removed successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No entry to remove or entry already verified"));
        }
    }

    // Standard CRUD for admin
    @PostMapping
    public BoxWeight create(@RequestBody BoxWeight boxWeight, Authentication authentication) {
        boxWeight.setSentBy(authentication.getName());
        return boxWeightService.save(boxWeight);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoxWeight> update(@PathVariable Long id, @RequestBody BoxWeight boxWeight) {
        Optional<BoxWeight> existing = boxWeightService.findById(id);
        if (existing.isPresent()) {
            boxWeight.setId(id);
            return ResponseEntity.ok(boxWeightService.save(boxWeight));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<BoxWeight> existing = boxWeightService.findById(id);
        if (existing.isPresent()) {
            boxWeightService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
