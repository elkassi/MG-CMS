# BoxWeight Section Review — Changes Based on New Features

## Current State Analysis

The BoxWeight system currently uses two separate data sources for weight estimation:
1. **PartNumberWeight** entity — manually managed, stores `weightUnit` (kg per unit piece)
2. **BoxTypeConfig** entity — manually managed, stores `emptyBoxWeight` per box type
3. **Historical averaging** — calculates average weight from BoxWeight history when PartNumberWeight is missing

**Formula**: `estimatedWeight = (quantity × partNumberUnitWeight) + emptyBoxWeight`

---

## Changes Recommended Based on New Features

### 1. Use CAD-Calculated Weight from PartNumberInfo

**Problem**: The current `PartNumberWeight` entity requires manual input of `weightUnit` (per-piece weight). This is separate from the CAD system and can become outdated or inconsistent.

**Solution**: The new CAD Piece Weight Calculation feature calculates and stores the **real weight per part number cover** in `PartNumberInfo.weight`. This is derived from actual CAD data (pattern areas × material density).

**Proposed Change**: Enhance the `BoxWeightController.calculateEstimatedWeight()` method to also check `PartNumberInfo.weight` as a third fallback source:

```java
// Priority order for weight estimation:
// 1. PartNumberWeight (manually configured, most reliable)
// 2. PartNumberInfo.weight (CAD-calculated, automated)
// 3. Historical average from BoxWeight records (least reliable)
```

**Impact**: This means the CAD team's work in importing piece details and calculating weights would automatically improve box weight estimation accuracy, without manual PartNumberWeight configuration for each part number.

### 2. Auto-Populate PartNumberWeight from CAD Calculations

**Problem**: Currently, `PartNumberWeight.weightUnit` must be manually entered or imported from Excel. This is labor-intensive and error-prone.

**Solution**: Add a new endpoint that syncs `PartNumberWeight` entries from `PartNumberInfo.weight`:

```
POST /api/partNumberWeight/syncFromCAD
```

This would:
1. Read all `PartNumberInfo` records that have a non-null `weight` value
2. For each, check if a corresponding `PartNumberWeight` record exists
3. If not, create one with `weightUnit = PartNumberInfo.weight / packageQty` (if `packageQty` is known)
4. Return count of synced records

**Impact**: Reduces manual data entry, keeps weight data consistent across CAD and BoxWeight systems.

### 3. Enhanced Estimation with Material-Level Weights

**Problem**: The current estimation uses a single `weightUnit` per part number, which doesn't account for different materials within the same part.

**Solution**: Now that `PartNumberMaterialConfig.weightUnit` stores kg/m² for each material, the system could provide more detailed estimation:

```
estimatedWeight = Σ (area(pattern) × quantity × weightUnit(material)) + emptyBoxWeight
```

This is already calculated by `PartNumberWeightCalculationService.calculateWeights()`. The BoxWeight system could leverage this calculation directly.

**Proposed Change**: Add to `BoxWeightController`:

```java
// If PartNumberWeight not configured, try CAD weight calculation
if (!weightConfig.isPresent()) {
    PartNumberInfo pnInfo = partNumberInfoRepository.findByPartNumber(partnumber);
    if (pnInfo != null && pnInfo.getWeight() != null) {
        Double emptyBoxWeight = boxTypeConfigService.getEmptyBoxWeight(boxType);
        // CAD weight is for entire PN, so for a box:
        // estimatedWeight = (quantity × cadWeightPerPiece) + emptyBoxWeight
        return (pnInfo.getWeight() * quantity) + emptyBoxWeight;
    }
}
```

### 4. Validation Thresholds Based on CAD Data Quality

**Problem**: The current validation uses fixed thresholds (1.0 kg for sent-received, 5%/10%/15% for estimation variance). These may not be appropriate for all part numbers.

**Solution**: Now that we have precise CAD-calculated weights, the system could dynamically adjust thresholds:
- For part numbers with **high CAD confidence** (all patterns found, all materials configured): Use tighter thresholds (e.g., 3% variance)
- For part numbers with **low CAD confidence** (missing patterns or materials): Use relaxed thresholds (e.g., 15% variance)

**Proposed Change**: Add a `confidence` field to `PartNumberInfo`:

```java
private String weightConfidence; // "HIGH", "MEDIUM", "LOW" based on calculation completeness
```

### 5. Cross-Reference Report: CAD Weight vs. Actual Box Weight

**Problem**: No way to validate if CAD-calculated weights are accurate against real-world measurements.

**Solution**: Create a new report that compares:
- `PartNumberInfo.weight` (CAD-calculated) × quantity
- vs. `BoxWeight.sentWeight - emptyBoxWeight` (actual net weight)
- vs. `BoxWeight.receivedWeight - emptyBoxWeight` (verified net weight)

This gives the CAD team feedback on whether their material weight configurations (`weightUnit` in `PartNumberMaterialConfig`) are accurate.

---

## Summary of Proposed Changes

| # | Change | Priority | Effort | Impact |
|---|--------|----------|--------|--------|
| 1 | Use PartNumberInfo.weight as fallback in estimation | HIGH | Low | Automatic estimation for all CAD-configured PNs |
| 2 | Sync PartNumberWeight from CAD calculations | MEDIUM | Medium | Reduces manual weight configuration |
| 3 | Material-level weight estimation | LOW | Medium | More precise estimation per pattern |
| 4 | Dynamic validation thresholds | LOW | Medium | Better validation for configured PNs |
| 5 | CAD vs. Actual weight comparison report | MEDIUM | Medium | CAD data quality feedback loop |

---

## Files That Would Need Changes

| File | Change |
|------|--------|
| `BoxWeightController.java` | Add PartNumberInfo as fallback in `calculateEstimatedWeight()` |
| `BoxWeightService.java` | Add new method `estimateFromCAD()` |
| `PartNumberWeightController.java` | Add `POST /syncFromCAD` endpoint |
| `BoxWeightFilling.js` | Show CAD confidence indicator |
| `BoxWeightVerifying.js` | Show CAD weight comparison in verification panel |
| `BoxWeight.md` | Update documentation with new integration points |
