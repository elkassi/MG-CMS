package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.BoxWeight;
import com.lear.MGCMS.repositories.BoxWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BoxWeightService {

    @Autowired
    private BoxWeightRepository boxWeightRepository;

    @Autowired
    private BoxTypeConfigService boxTypeConfigService;

    public List<BoxWeight> findAll() {
        return boxWeightRepository.findAllByOrderBySentAtDesc();
    }

    public Page<BoxWeight> findAll(Pageable pageable) {
        return boxWeightRepository.findAll(pageable);
    }

    public Page<BoxWeight> findAll(Specification<BoxWeight> spec, Pageable pageable) {
        return boxWeightRepository.findAll(spec, pageable);
    }

    public Optional<BoxWeight> findById(Long id) {
        return boxWeightRepository.findById(id);
    }

    public BoxWeight save(BoxWeight boxWeight) {
        return boxWeightRepository.save(boxWeight);
    }

    public void deleteById(Long id) {
        boxWeightRepository.deleteById(id);
    }

    // Find entries by boxId
    public List<BoxWeight> findByBoxId(String boxId) {
        return boxWeightRepository.findByBoxId(boxId);
    }

    // Find entries by boxId that are not yet verified
    public List<BoxWeight> findByBoxIdNotVerified(String boxId) {
        return boxWeightRepository.findByBoxIdAndReceivedByIsNull(boxId);
    }

    // Find entries sent by a specific user
    public List<BoxWeight> findBySentBy(String sentBy) {
        return boxWeightRepository.findBySentByOrderBySentAtDesc(sentBy);
    }

    // Find entries not yet verified
    public List<BoxWeight> findNotVerified() {
        return boxWeightRepository.findByReceivedByIsNullOrderBySentAtDesc();
    }

    // Find last entry by user (for remove last functionality)
    public Optional<BoxWeight> findLastBySentBy(String sentBy) {
        return boxWeightRepository.findTopBySentByOrderByIdDesc(sentBy);
    }

    // Create new box weight entry (for filling)
    public BoxWeight createBoxWeight(String boxType, String boxId, Double sentWeight, String sentBy) {
        BoxWeight boxWeight = new BoxWeight();
        boxWeight.setBoxType(boxType);
        boxWeight.setBoxId(boxId);
        boxWeight.setSentWeight(sentWeight);
        boxWeight.setSentBy(sentBy);
        boxWeight.setSentAt(LocalDateTime.now());
        return boxWeightRepository.save(boxWeight);
    }

    // Verify box weight (for verifying role)
    public BoxWeight verifyBoxWeight(Long id, Double receivedWeight, String receivedBy) {
        Optional<BoxWeight> optionalBoxWeight = boxWeightRepository.findById(id);
        if (optionalBoxWeight.isPresent()) {
            BoxWeight boxWeight = optionalBoxWeight.get();
            boxWeight.setReceivedWeight(receivedWeight);
            boxWeight.setReceivedBy(receivedBy);
            boxWeight.setReceivedAt(LocalDateTime.now());
            
            // Calculate difference and validate
            double difference = Math.abs(boxWeight.getSentWeight() - receivedWeight);
            boxWeight.setValidated(difference <= 1.0);
            
            return boxWeightRepository.save(boxWeight);
        }
        return null;
    }

    // Remove last entry by user
    public boolean removeLastBySentBy(String sentBy) {
        Optional<BoxWeight> lastEntry = boxWeightRepository.findTopBySentByOrderByIdDesc(sentBy);
        if (lastEntry.isPresent()) {
            // Only allow removal if not yet verified
            BoxWeight boxWeight = lastEntry.get();
            if (boxWeight.getReceivedBy() == null) {
                boxWeightRepository.deleteById(boxWeight.getId());
                return true;
            }
        }
        return false;
    }

    // Find by validated status
    public List<BoxWeight> findByValidated(Boolean validated) {
        return boxWeightRepository.findByValidatedOrderBySentAtDesc(validated);
    }

    // Calculate average weight unit from history
    public Double calculateAverageWeightUnit(String partnumber, String boxType) {
        // Note: BoxWeight.boxId may contain partnumber in some cases
        // This searches historical records to calculate average weight per unit
        List<BoxWeight> history = boxWeightRepository.findByBoxId(partnumber);
        
        if (history.isEmpty()) {
            return null;
        }

        double sum = 0;
        int count = 0;
        Double emptyBoxWeight = boxTypeConfigService != null ? 
            boxTypeConfigService.getEmptyBoxWeight(boxType) : 0.0;

        for (BoxWeight bw : history) {
            if (bw.getSentWeight() != null && bw.getQuantity() != null && bw.getQuantity() > 0) {
                double netWeight = bw.getSentWeight() - emptyBoxWeight;
                double weightPerUnit = netWeight / bw.getQuantity();
                sum += weightPerUnit;
                count++;
            }
        }

        return count > 0 ? sum / count : null;
    }
}
