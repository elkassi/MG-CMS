# CAD Modifications Summary

## Overview

This document summarizes all the CAD (Cutting Plan) modifications made to the MG-CMS system, including backend services, frontend components, and data models.

---

## 1. saveToCms - ID Reuse & maxPliePlan Fix

**File**: `src/main/java/com/lear/MGCMS/services/cms/ItemPlanCoupeService.java`

### Problem
- The `saveToCms` method deleted all children entities and recreated them with `max(id)+1` every time, causing ID inflation in the CMS database.
- `maxPliePlan` fields on `IntervalSeuilPlanCoupe` and `IntervalItemMachinePlanCoupe` were never populated (always null).
- All `ReftissuMargin` entries were saved to CMS, including machine-specific ones that shouldn't be there.

### Solution
- **ID Reuse**: Before deleting children, collect all existing IDs into arrays (`existingSeuilIds`, `existingIntervalSeuilIds`, `existingMachineIds`, `existingIntervalMachineIds`, `existingCategoryIds`). When re-inserting, reuse these IDs first. Only fall back to `max(id)+1` for new entries beyond the existing count.
- **maxPliePlan Computation**:
  - For `IntervalSeuilPlanCoupe`: Parse `pliesConfig` intervals, sort by min plie, set `maxPlieSeuilPlan = nextInterval.minPlie - 1` (or `100.0` for the last interval).
  - For `IntervalItemMachinePlanCoupe`: Same logic, `maxPliePlan = nextInterval.minPlie - 1` (or `100` for the last interval).
- **Machine Filter**: Only save `ReftissuMargin` entries where `machine == null || machine.isEmpty()`. Machine-specific margins are excluded from CMS sync.

### PliesConfig Format
```
"10;config1|20;config2|50;config3"
```
- Pipe-separated intervals
- Semicolon-separated key-value pairs: `minPlie;configName`
- Sorted by minPlie ascending when processing

---

## 2. PartNumberCorrespondance Integration

### Backend
**Files Modified**:
- `PartNumberCorrespendanceController.java` - Added `GET /api/partNumberCorrespendance/byPartNumbers?partNumbers=...` endpoint
- `PartNumberCorrespendanceService.java` - Added `findByPartNumbers(List<String>)` method
- `PartNumberCorrespendanceRepository.java` - Added `findByPartNumberIn(List<String>)` Spring Data query

### Frontend
**File**: `CuttingPlanForm.js`

### Problem
The `PartNumberCorrespendance` entity existed but was never loaded or used in `CuttingPlanForm.js`. Material comparison in the verification modal flagged false positives when materials had known correspondances (e.g., different naming conventions for the same material).

### Solution
- **Loading**: After `partNumberMaterialConfig` data is loaded in `componentDidMount`, fetch correspondances via the new endpoint using all part numbers.
- **Verification Modal (renderConfirmModal)**: When CTC material doesn't match placement material, check if a `PartNumberCorrespondance` exists for the same `partNumber` with a matching `pattern` or `patternCorrespondance`. If found, the mismatch is NOT flagged as an error.
- **Visual**: Material cells use a `materialMismatch` flag that accounts for correspondances before applying red background highlighting.

### PartNumberCorrespendance Entity Fields
| Field | Type | Description |
|-------|------|-------------|
| partNumber | String | The part number this correspondance applies to |
| partNumberCorrespondance | String | The corresponding part number/material |
| pattern | String | The original pattern/digit identifier |
| patternCorrespondance | String | The corresponding pattern in the other system |
| placement | String | Related placement name |

---

## 3. maxPlie Fix for ALL Placements

**File**: `CuttingPlanForm.js` (componentDidMount)

### Problem
When opening an existing cutting plan, only the first placement (`cuttingPlanMaterialPlacement[0]`) had its `maxPlie`, `maxPlieDrill`, and `maxDrill` values set from the material configuration. Additional placements (alternative machines, optional placements) kept null values.

### Solution
- Loop through ALL placements of each material, not just index 0.
- For each placement, first try to find a machine config matching `placement.machine`. If not found, fall back to the default machine config.
- Set `maxPlie`, `maxPlieDrill`, `maxDrill`, and `pliesConfig` for each placement from the matched (or default) machine config.
- Set `category` and `laize` from the default category config if not already assigned.

---

## 4. Optional Placement Verification (verification4)

**File**: `CuttingPlanForm.js` (verification4 function)

### Problem
No validation existed to ensure that optional placements (`activated=false`) in a `groupPlacement` matched the active placement's digit composition and quantity.

### Solution
After the main placement loop in verification4, added a new check:
- Group all placements by `groupPlacement` within each material.
- For each group, compare optional placements (`activated=false`) against the active one (`activated=true`).
- Verify that `partNumbers` (digits) match between active and optional placements.
- Verify that `nbrCouche` matches between active and optional placements.
- Push error messages for any mismatches.

---

## 5. Detail-Only View Mode

**File**: `CuttingPlanForm.js` (render method)

### Problem
Opening `/cuttingPlan/{id}` immediately loaded the full edit form with all interactive elements, which was slow and confusing for users who only wanted to review a plan.

### Solution
- Added `viewMode` state, initialized to `true` when `entityId` is present.
- When `viewMode` is true, render a read-only summary view showing:
  - Basic plan info (project, version, definition, CMS ID, type, status, dates, created/updated by)
  - Part numbers table (read-only)
  - Materials & placements table (read-only, with all key columns)
  - Verification results (renderTest)
  - Alert messages
- A **"Modifier"** button switches `viewMode` to `false`, revealing the full edit form.
- A **"Retour"** button navigates back to the list.

---

## 6. LASER-DXF Consumption Check (verification4)

**File**: `CuttingPlanForm.js` (verification4 function)

### Problem
No validation existed for LASER-DXF placements with very small material consumption. When the mattress length (`longueurMatelas`) is below 1.1m, the number of layers should be exactly 1 (single-layer cutting).

### Solution
Added in verification4, inside the per-placement check:
```javascript
if (cpmp.machine === "LASER-DXF" && cpmp.longueurMatelas && cpmp.longueurMatelas < 1.1 && cpmp.nbrCouche !== 1) {
    error.push(placement + " : LASER-DXF avec consommation < 1.1, le nombre de couche doit être 1")
}
```

---

## Data Flow Summary

```
CuttingPlan (DB)
  ├── CuttingPlanPartNumbers[] (part numbers + quantities)
  ├── CuttingPlanMaterials[] (materials/reftissus)
  │     └── CuttingPlanMaterialPlacement[] (individual placements)
  │           ├── machine, category, laize
  │           ├── nbrCouche, config, drill
  │           ├── longueur, longueurMatelas
  │           ├── groupPlacement, activated
  │           └── pliesConfig, pliesConfigMarge
  └── CuttingPlanRapportPlacements[] (rapport data)

PartNumberMaterialConfig (per material)
  ├── reftissuMachines[] (machine types, maxPlie values, pliesConfig)
  ├── reftissuCategories[] (laize categories)
  └── reftissuMargins[] (length intervals, machine-specific margins)

CMS Database (ItemPlanCoupe sync)
  ├── SeuilLongueurPlanCoupe → IntervalSeuilPlanCoupe[] (margin intervals)
  ├── ItemMachinePlanCoupe → IntervalItemMachinePlanCoupe[] (machine plie intervals)
  └── CategoryLaizePlanCoupe[] (laize categories)
```

## Files Modified

| File | Type | Changes |
|------|------|---------|
| `ItemPlanCoupeService.java` | Backend | Complete rewrite of saveToCms |
| `PartNumberCorrespendanceController.java` | Backend | New `/byPartNumbers` endpoint |
| `PartNumberCorrespendanceService.java` | Backend | New `findByPartNumbers` method |
| `PartNumberCorrespendanceRepository.java` | Backend | New `findByPartNumberIn` query |
| `CuttingPlanForm.js` | Frontend | Multiple changes (all 6 features) |
