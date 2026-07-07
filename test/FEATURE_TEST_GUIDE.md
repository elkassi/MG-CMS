# Feature Test Guide — MG-CMS (Commit 48bee5d)

> **Date:** April 2026
> **Scope:** CAD Piece Weight Calculation, BoxWeight Integration, Preparation Split/Fuse
> **Author:** Auto-generated from code review

---

## Table of Contents

1. [Code Review Summary](#1-code-review-summary)
2. [CAD Piece Weight Calculation — Tests](#2-cad-piece-weight-calculation--tests)
3. [BoxWeight Integration — Tests](#3-boxweight-integration--tests)
4. [Preparation Split — Tests](#4-preparation-split--tests)
5. [Preparation Fuse — Tests](#5-preparation-fuse--tests)
6. [Duplicate Detection — Tests](#6-duplicate-detection--tests)
7. [Cross-Feature Integration Tests](#7-cross-feature-integration-tests)
8. [Issues Found During Review](#8-issues-found-during-review)

---

## 1. Code Review Summary

### Feature Completeness

| Feature | Status | Files Reviewed |
|---------|--------|----------------|
| CAD Piece Weight Calculation | ✅ Implemented | `PartNumberWeightCalculationService`, `PieceDetailController`, `WeightCalculation.js` |
| BoxWeight Integration | ✅ Implemented | `BoxWeightController`, `BoxWeightService`, `BoxWeightFilling.js`, `BoxWeightVerifying.js` |
| Work Order SPLIT | ✅ Implemented | `WorkOrderService.splitWorkOrder()`, `WorkOrderController /split` |
| Work Order FUSE | ✅ Implemented | `WorkOrderService.fuseWorkOrders()`, `WorkOrderController /fuse` |
| Duplicate Detection | ✅ Implemented | `WorkOrderService.detectDuplicates()`, `WorkOrderController /duplicates` |

### Key Concerns (see Section 8)

- ⚠️ Dual-database `@Transactional` only covers primary datasource — split/fuse can leave DBs inconsistent if one fails
- ⚠️ Missing input validations on split (negative qty, zero qty) and fuse (same PN check, status check)
- ⚠️ N+1 query pattern in weight calculation loop
- ⚠️ Hardcoded 1.0 kg tolerance for BoxWeight verification

---

## 2. CAD Piece Weight Calculation — Tests

**Endpoint:** `POST /api/pieceDetail/calculateWeight`
**Service:** `PartNumberWeightCalculationService.calculateWeights()`
**Frontend:** `WeightCalculation.js`

### Test 2.1 — Basic Weight Calculation (Happy Path)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure DB has `PartNumberMaterialConfig` with `objId = "MAT-001"` and `weightUnit = 0.35` (kg/m²) | Config exists |
| 2 | Ensure DB has `PieceDetail` with `descrip` containing pattern name, `area = 2500` (cm²), `perimeter = 200` (cm) | PieceDetail exists |
| 3 | Ensure `Files` table has entry with `type = "fabric"`, `partNumberCover = "PN-COVER-001"`, `pattern = "CUSH_HAB01"`, `partNumberMaterial = "MAT-001"`, `quantity = 2` | Files entry exists |
| 4 | Open **WeightCalculation** page (or call API directly) | Page loads |
| 5 | Enter `PN-COVER-001` in the part number textarea | Text entered |
| 6 | Click **"Calculer"** | API call `POST /api/pieceDetail/calculateWeight` with `{ partNumberCovers: ["PN-COVER-001"] }` |
| 7 | Verify response: `totalWeight = (2500/10000) × 2 × 0.35 = 0.175 kg` | Weight = **0.175 kg** |
| 8 | Verify `PartNumberInfo` table: `partNumber = "PN-COVER-001"` has `weight = 0.175` | Saved in DB |
| 9 | Verify `totalPerimeter` is saved if > 0 | Perimeter = 200 |

### Test 2.2 — Multiple Patterns Per Part Number

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Setup 3 `Files` entries for same `partNumberCover = "PN-MULTI"` with different patterns | 3 files |
| 2 | Each has: area=1000, qty=1, weightUnit=0.5 | Per pattern weight = 0.05 |
| 3 | Call calculate for `"PN-MULTI"` | `totalWeight = 3 × 0.05 = 0.15 kg` |
| 4 | Verify all 3 patterns appear in `patterns[]` response | 3 pattern details |

### Test 2.3 — Missing Fabric Files

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call calculate for `"PN-NONEXISTENT"` (no Files entry) | |
| 2 | Verify response has error: "Aucun fichier tissu trouvé pour: PN-NONEXISTENT" | Error returned |
| 3 | Verify `totalWeight = null` | Not saved |
| 4 | Verify `saved = false` | Not persisted |

### Test 2.4 — Missing PieceDetail (Pattern Not Found)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Setup Files entry but no matching PieceDetail | |
| 2 | Call calculate | |
| 3 | Verify error: "Pattern non trouvé: {pattern}" | Error for missing pattern |
| 4 | Verify `totalWeight = null` when ANY pattern is missing | Null |
| 5 | Verify `calculatedWeight` still has partial calculation | Partial sum |

### Test 2.5 — Missing Material Config (WeightUnit Not Configured)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Setup Files + PieceDetail, but no `PartNumberMaterialConfig` for the material | |
| 2 | Call calculate | |
| 3 | Verify error: "Poids unitaire non configuré pour le matériau: {material}" | Error |
| 4 | Pattern detail shows `weightUnit = null`, `weightContribution = null` | Nulls |

### Test 2.6 — Multiple Part Numbers in Single Request

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter 3 part numbers (comma or newline separated) | |
| 2 | Click Calculer | |
| 3 | Verify results array has 3 entries | 3 results |
| 4 | Each result independent — one error doesn't block others | Partial success OK |

### Test 2.7 — Area = 0 Edge Case

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | PieceDetail with `area = 0` | |
| 2 | Calculate | `weightContribution = 0` (valid, no crash) |

### Test 2.8 — Already Existing PartNumberInfo

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | PartNumberInfo for "PN-EXISTS" already has weight = 5.0 | |
| 2 | Calculate with new data → totalWeight = 0.3 | |
| 3 | Verify PartNumberInfo.weight UPDATED to 0.3 (not appended) | Overwritten |

---

## 3. BoxWeight Integration — Tests

### 3A. BoxWeight Filling

**Endpoint:** `POST /api/boxWeight/fill`
**Role Required:** `ROLE_FILLING_WEIGHT`
**Frontend:** `BoxWeightFilling.js`

#### Test 3A.1 — Fill Box Weight (Happy Path)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as user with `ROLE_FILLING_WEIGHT` | Authenticated |
| 2 | Open BoxWeightFilling page | Page loads, "Mes entrées" section visible |
| 3 | Select boxType = "gray" | BoxType selected |
| 4 | Enter boxId = "BOX-001" | |
| 5 | Enter sentWeight = 15.5 | |
| 6 | Enter partnumber = "PN-001", quantity = 20 | |
| 7 | Click Submit | `POST /api/boxWeight/fill` called |
| 8 | Verify response: sentBy = current user, sentAt = now | Metadata set |
| 9 | Verify estimatedWeight calculated (if PN config exists) | Estimation populated |
| 10 | Verify entry appears in "Mes entrées" list | List updated |

#### Test 3A.2 — Fill Without partnumber/quantity

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit with only boxType, boxId, sentWeight (no partnumber) | |
| 2 | Verify save succeeds | OK — partnumber is optional |
| 3 | Verify `estimatedWeight = null` | Skipped |

#### Test 3A.3 — Missing Required Fields

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit with boxType = null | 400 Bad Request |
| 2 | Submit with boxId = null | 400 Bad Request |
| 3 | Submit with sentWeight = null | 400 Bad Request |

#### Test 3A.4 — Estimate Weight (3-Priority Fallback)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | **Priority 1:** Create `PartNumberWeight` for `PN-PRI1` with `weightUnit = 0.5` | |
| 2 | Call `GET /api/boxWeight/estimateWeight?partnumber=PN-PRI1&quantity=20&boxType=gray` | |
| 3 | Verify: estimated = `(0.5 × 20) + 0.5` = **10.5 kg** (gray box = 0.5 kg empty) | Priority 1 used |
| 4 | **Priority 2:** No PartNumberWeight, but PartNumberInfo has `weight = 0.3` for `PN-PRI2` | |
| 5 | Call estimateWeight for PN-PRI2 | estimated = `(0.3 × 20) + 0.5` = **6.5 kg** |
| 6 | **Priority 3:** No PartNumberWeight, no PartNumberInfo, but BoxWeight history exists for `PN-PRI3` | |
| 7 | Call estimateWeight for PN-PRI3 | Uses average historical weight per unit |
| 8 | **No data:** No config/CAD/history for `PN-NONE` | `estimatedWeight = null` |

#### Test 3A.5 — Box Type Empty Weight

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Verify `BoxTypeConfig` has "gray" = 0.5 kg, "black" = 0.8 kg | Configs exist |
| 2 | Same PN and qty, boxType gray vs black | Different estimates (0.3 kg difference) |

#### Test 3A.6 — Delete Last Entry

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fill 2 entries | 2 entries in list |
| 2 | Click "Delete Last" | Last entry removed |
| 3 | Verify only latest unverified entry is deleted | Older entry untouched |
| 4 | Try delete when last entry already verified | Fails / returns false |

#### Test 3A.7 — Gamme Lookup

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter a valid boxId (matching a cutting request serie) | |
| 2 | Click "Lookup Gamme" | `GET /api/gammeTechniqueImprimer/serie/{boxId}` |
| 3 | Verify partnumber and quantity auto-filled from gamme data | Auto-populated |

### 3B. BoxWeight Verification

**Endpoint:** `POST /api/boxWeight/verify/{id}`
**Role Required:** `ROLE_VERIFYING_WEIGHT`
**Frontend:** `BoxWeightVerifying.js`

#### Test 3B.1 — Verify Box Weight (Validated — diff ≤ 1.0 kg)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as user with `ROLE_VERIFYING_WEIGHT` | |
| 2 | Search for boxId "BOX-001" (previously filled) | |
| 3 | Verify unverified entry appears | Entry found |
| 4 | Enter receivedWeight = 15.8 (diff = 0.3 from sentWeight 15.5) | |
| 5 | Click "Verify" | `POST /api/boxWeight/verify/{id}` |
| 6 | Verify response: `validated = true`, `difference = 0.3` | ✅ Validated |
| 7 | Verify receivedBy = current user, receivedAt = now | Metadata set |
| 8 | Entry disappears from unverified list | Verified |

#### Test 3B.2 — Verify Box Weight (Not Validated — diff > 1.0 kg)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Box with sentWeight = 15.5 | |
| 2 | Enter receivedWeight = 17.0 (diff = 1.5) | |
| 3 | Click Verify | |
| 4 | Verify: `validated = false`, `difference = 1.5` | ❌ Not validated |
| 5 | UI shows red alert with the difference | Warning displayed |

#### Test 3B.3 — Verify Exact Match

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | sentWeight = 15.5, receivedWeight = 15.5 | |
| 2 | Verify | `validated = true`, `difference = 0.0` |

#### Test 3B.4 — Verify Boundary (diff = exactly 1.0)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | sentWeight = 15.5, receivedWeight = 16.5 | |
| 2 | Verify | `validated = true`, `difference = 1.0` (threshold is ≤ 1.0) |

#### Test 3B.5 — Verify Already-Verified Box

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Try to verify a box that already has receivedWeight set | |
| 2 | Verify behavior: should it reject or overwrite? | Depends on implementation — verify no double-counting |

#### Test 3B.6 — Missing receivedWeight

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST verify with `receivedWeight = null` | 400 Bad Request |

#### Test 3B.7 — Invalid Box ID

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST verify with non-existent ID | 404 Not Found |

---

## 4. Preparation Split — Tests

**Endpoint:** `POST /api/workOrder/split`
**Service:** `WorkOrderService.splitWorkOrder()`
**Role Required:** `ROLE_IMPORTER` or `ROLE_ADMIN`

### Test 4.1 — Split Happy Path (remainingQty > 0)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure WorkOrder "500" exists with `qtyOpen = 100` | WO in MG_CMS DB |
| 2 | Ensure OrderSchedule `ID_Demande = 500` exists with `Quantite_Demande = 100` | OS in qualite DB |
| 3 | Call `POST /api/workOrder/split` with `{ wo: "500", importedQty: 60 }` | |
| 4 | Verify response: `{ split: true, newWo: <MAX+1>, remainingQty: 40, importedQty: 60 }` | Split occurred |
| 5 | **Original WO (MG_CMS):** `qtyOpen = 60`, `updatedAt = now` | Updated |
| 6 | **Original OS (qualite):** `Quantite_Demande = 60` | Updated |
| 7 | **Original OS Remarque:** Contains `"SPLIT: qty 100→60, remaining 40 moved to ID=<newId>"` | Audit trail |
| 8 | **New WO (MG_CMS):** `wo = <newId>`, `qtyOpen = 40`, `partNumber = same`, `createdAt = now` | Created |
| 9 | **New OS (qualite):** `ID_Demande = <newId>`, `Quantite_Demande = 40`, `Status_Demande = "F"` | Created |
| 10 | **New OS:** `Marker_Group_ID_D = "500"` (links back to original) | Linked |
| 11 | **New OS Remarque:** Contains `"SPLIT from ID_Demande=500, remaining qty=40"` | Audit |
| 12 | **New WO field copy:** Verify item, partNumber, description, groupName, designGroup, coverGroup are copied | All fields cloned |

### Test 4.2 — No Split When importedQty = originalQty

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO "500" with qty = 100, call split with importedQty = 100 | |
| 2 | Verify: `{ split: false }` | No split |
| 3 | Verify no new WO or OS created | Nothing added |
| 4 | Original WO/OS unchanged | No modifications |

### Test 4.3 — No Split When importedQty > originalQty

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO "500" with qty = 100, call split with importedQty = 150 | |
| 2 | Verify: `{ split: false }` (remainingQty = -50 ≤ 0) | No split |

### Test 4.4 — Split When importedQty = 1 (Almost Full Import)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO qty = 100, importedQty = 1 | |
| 2 | Verify split: original=1, new WO=99 | Large remainder |

### Test 4.5 — Split When importedQty = originalQty - 1

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO qty = 100, importedQty = 99 | |
| 2 | Verify split: original=99, new WO=1 | Tiny remainder |

### Test 4.6 — Work Order Not Found

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call split with `wo = "999999"` (non-existent) | |
| 2 | Verify: `{ split: false, error: "Work Order not found" }` | Error returned |

### Test 4.7 — Missing Required Fields

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST split with `wo = null` | 400 Bad Request |
| 2 | POST split with `importedQty = null` | 400 Bad Request |

### Test 4.8 — New ID Generation (MAX+1)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Note current MAX(ID_Demande) in qualite DB | e.g., 5000 |
| 2 | Perform split | |
| 3 | Verify new ID = 5001 | Incremented |
| 4 | Perform another split immediately | New ID = 5002 |
| 5 | No ID collision | Unique IDs |

### Test 4.9 — Concurrent Splits (Race Condition)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | From 2 different browsers, simultaneously split different WOs | |
| 2 | Verify both succeed with different new IDs | No ID collision |
| 3 | ⚠️ **Risk:** `MAX(ID_Demande) + 1` is not atomic — could collide | Watch for errors |

### Test 4.10 — Authorization

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call split as `ROLE_IMPORTER` | ✅ Allowed |
| 2 | Call split as `ROLE_ADMIN` | ✅ Allowed |
| 3 | Call split as `ROLE_CAD` | ❌ 403 Forbidden |
| 4 | Call split as `ROLE_FILLING_WEIGHT` | ❌ 403 Forbidden |

---

## 5. Preparation Fuse — Tests

**Endpoint:** `POST /api/workOrder/fuse`
**Service:** `WorkOrderService.fuseWorkOrders()`
**Role Required:** `ROLE_IMPORTER` or `ROLE_ADMIN`

### Test 5.1 — Fuse 3 WOs (Happy Path)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create 3 WOs for same partNumber: WO 500 (qty=30), WO 510 (qty=40), WO 520 (qty=20) | In both DBs |
| 2 | Call `POST /api/workOrder/fuse` with `["500", "510", "520"]` | |
| 3 | Verify response: `{ success: true, targetWo: "520", totalQty: 90, zeroedWos: ["500","510"] }` | Fused |
| 4 | **Target WO 520 (MG_CMS):** `qtyOpen = 90` | Updated |
| 5 | **Target OS 520 (qualite):** `Quantite_Demande = 90`, `Marker_Group_ID_D = "520"` | Updated |
| 6 | **Target OS Remarque:** Contains "FUSED: received qty from ID=500 (30), ID=510 (40), total=90" | Audit |
| 7 | **Source WO 500 (MG_CMS):** `qtyOpen = 0` | Zeroed |
| 8 | **Source OS 500 (qualite):** `Quantite_Demande = 0`, `Marker_Group_ID_D = "520"` | Zeroed + linked |
| 9 | **Source OS 500 Remarque:** Contains "FUSED: qty 30 transferred to ID=520" | Audit |
| 10 | Same verification for WO 510 | Zeroed + linked |

### Test 5.2 — Fuse 2 WOs

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO 100 (qty=50), WO 200 (qty=25) | |
| 2 | Fuse ["100", "200"] | Target = 200 (highest), totalQty = 75 |

### Test 5.3 — Fuse Requires at Least 2 WOs

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST fuse with `["500"]` | 400 Bad Request: "Need at least 2 work orders to fuse" |
| 2 | POST fuse with `[]` | 400 Bad Request |
| 3 | POST fuse with `null` | 400 Bad Request |

### Test 5.4 — Target WO Not Found

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fuse `["500", "999999"]` where 999999 is the target (highest) but doesn't exist | |
| 2 | Verify: `{ success: false, error: "Target Work Order not found" }` | Error |

### Test 5.5 — Source WO Not Found (Skipped)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fuse `["888888", "500"]` where 888888 doesn't exist, target=500 exists | |
| 2 | Verify: source is skipped (`continue` in code), target qty = original qty only | No crash |
| 3 | ⚠️ **Concern:** Silent skip with no error reported — user won't know a source was missing | Verify behavior |

### Test 5.6 — Fuse with Zero Quantity Sources

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO 500 (qty=0), WO 510 (qty=40) | |
| 2 | Fuse ["500", "510"] | Target=510, totalQty=40 (0+40) |
| 3 | WO 500 stays at qty=0 | Already zero |

### Test 5.7 — Authorization

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fuse as `ROLE_IMPORTER` | ✅ Allowed |
| 2 | Fuse as `ROLE_ADMIN` | ✅ Allowed |
| 3 | Fuse as `ROLE_CAD` | ❌ 403 Forbidden |

---

## 6. Duplicate Detection — Tests

**Endpoint:** `GET /api/workOrder/duplicates?date=YYYY-MM-DD&shift=X`
**Service:** `WorkOrderService.detectDuplicates()`
**Role Required:** `ROLE_IMPORTER`, `ROLE_ADMIN`, or `ROLE_CAD`

### Test 6.1 — Duplicates Found

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | For a date/shift, create WOs: PN-A (qty=30), PN-A (qty=40), PN-B (qty=50) | |
| 2 | Call `GET /api/workOrder/duplicates?date=2026-04-01&shift=A` | |
| 3 | Verify: `{ hasDuplicates: true, duplicates: [{ partNumber: "PN-A", count: 2, totalQty: 70 }] }` | Detected |
| 4 | PN-B not in duplicates (only 1 entry) | Not flagged |

### Test 6.2 — No Duplicates

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | All WOs have different partNumbers | |
| 2 | Call detectDuplicates | `{ hasDuplicates: false }` |

### Test 6.3 — Zero-Qty WOs Excluded

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WOs: PN-A (qty=30), PN-A (qty=0) | |
| 2 | Detect | `hasDuplicates: false` — only 1 with qty > 0 |

### Test 6.4 — Null PartNumber Excluded

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO with `partNumber = null` and qty > 0 | |
| 2 | Detect | Entry excluded — no NPE |

### Test 6.5 — Multiple Duplicate Groups

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WOs: PN-A×2, PN-B×3, PN-C×1 | |
| 2 | Detect | 2 groups: PN-A (count=2), PN-B (count=3) |

### Test 6.6 — Shift Parameter Optional

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call with `shift` omitted | Should work — check if shift=null handled in `findList()` |

---

## 7. Cross-Feature Integration Tests

### Test 7.1 — CAD Weight → BoxWeight Estimation (Priority 2)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Calculate CAD weight for "PN-CAD" → saved to PartNumberInfo.weight = 0.25 kg | |
| 2 | No PartNumberWeight entry for "PN-CAD" | |
| 3 | Call `GET /estimateWeight?partnumber=PN-CAD&quantity=20&boxType=gray` | |
| 4 | Verify: Priority 2 used → `(0.25 × 20) + 0.5 = 5.5 kg` | CAD feeds BoxWeight |

### Test 7.2 — Manual Weight Override (Priority 1 > CAD Priority 2)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | PartNumberInfo.weight = 0.25 (from CAD) | |
| 2 | Create PartNumberWeight with `weightUnit = 0.30` (manual override) | |
| 3 | Call estimateWeight | `(0.30 × 20) + 0.5 = 6.5 kg` — uses Priority 1, NOT CAD |

### Test 7.3 — Split Then Re-import

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO 500: qty=100 | |
| 2 | Import 60 → split creates WO 501 (qty=40) with status "F" | |
| 3 | Refresh work orders | WO 501 visible in the list |
| 4 | Import cutting plan for WO 501 with qty=40 | Full import, no split |
| 5 | Verify WO 501: status changes from "F" → import completed | |

### Test 7.4 — Detect Duplicates Then Fuse

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Load work orders for date/shift | |
| 2 | Call detectDuplicates | Returns list of duplicates |
| 3 | User selects a duplicate group | WO IDs collected |
| 4 | Call fuse with those IDs | Merged into one |
| 5 | Call detectDuplicates again | That partNumber no longer duplicated |

### Test 7.5 — Split Creates Potential Duplicate

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | WO 500 for PN-A (qty=100) | |
| 2 | Another WO 600 for PN-A (qty=50) already exists | |
| 3 | Split WO 500 → creates WO 501 for PN-A (qty=40) | |
| 4 | Now 3 WOs for PN-A: 500(60), 501(40), 600(50) | |
| 5 | Detect duplicates → 3 entries for PN-A | Detected |

### Test 7.6 — Weight Validated Then Used in Estimation

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Calculate CAD weight → 0.25 kg | |
| 2 | Validate weight via PartNumberValidatedWeight | Audit entry created |
| 3 | Use in BoxWeight estimation | Still uses PartNumberInfo.weight (validation is audit only) |

---

## 8. Issues Found During Review

### 8.1 Critical — Dual Database Transaction Risk

**Location:** `WorkOrderService.splitWorkOrder()` and `fuseWorkOrders()`

```
@Transactional only covers the primary datasource (MG_CMS).
If the qualite DB write succeeds but MG_CMS write fails,
quantities become inconsistent across databases.
```

**Impact:** High — data corruption possible
**Recommendation:** Implement ChainedTransactionManager or Saga pattern with compensation
**Test to run:** Kill the app between the two DB writes (or simulate DB timeout) and verify state

### 8.2 Medium — Missing Split Validations

**Location:** `WorkOrderService.splitWorkOrder()`

- No check for `importedQty <= 0` (negative/zero import)
- No check for `importedQty` being a valid integer (not a decimal)
- Controller handles null `wo`/`importedQty` but service doesn't validate importedQty > 0

**Test:** Send `importedQty = 0` → creates split with `remainingQty = 100` (the full original qty) — likely unintended

### 8.3 Medium — Fuse Same-PartNumber Not Verified

**Location:** `WorkOrderService.fuseWorkOrders()`

- Service fuses ANY WO IDs together without verifying they share the same `partNumber`
- User could accidentally fuse WOs for different part numbers

**Test:** Fuse WOs with different partNumbers → succeeds silently → data corruption

### 8.4 Medium — MAX+1 ID Race Condition

**Location:** `WorkOrderService.splitWorkOrder()` — `Long maxId = orderScheduleRepository.getMaxId();`

- Not atomic — two concurrent splits could generate the same newId
- No DB-level sequence or unique constraint enforcement

**Test:** Trigger 2 splits simultaneously → potential duplicate ID_Demande

### 8.5 Low — Hardcoded Verification Tolerance

**Location:** `BoxWeightService.verifyBoxWeight()` — `validated = (difference ≤ 1.0)`

- 1.0 kg tolerance is hardcoded, not configurable per box type or material
- Gray boxes (0.5 kg empty) have proportionally higher tolerance than black boxes (0.8 kg)

### 8.6 Low — N+1 Query in Weight Calculation

**Location:** `PartNumberWeightCalculationService.calculateWeights()`

- For each file: queries PieceDetail + PartNumberMaterialConfig individually
- Could batch-load all patterns up front

### 8.7 Low — Fuse Silent Skip on Missing Source

**Location:** `WorkOrderService.fuseWorkOrders()` — `if (sourceWO == null || sourceOS == null) continue;`

- If a source WO doesn't exist, it's silently skipped
- User gets `success: true` but some WOs weren't actually fused
- Should add warning to response

---

## Smoke Test Checklist (Quick Validation)

Use this for a quick pass to confirm features work end-to-end:

- [ ] **CAD Weight:** Enter 1 known part number → Calculate → verify weight in DB
- [ ] **BoxWeight Fill:** Login as filler → fill 1 box → verify in DB
- [ ] **BoxWeight Verify:** Login as verifier → verify that box → check validated=true/false
- [ ] **BoxWeight Estimate:** Call estimateWeight API → verify 3-priority cascade
- [ ] **Split:** Create WO with qty=100 → split with importedQty=60 → verify new WO with qty=40
- [ ] **Fuse:** Create 2 WOs for same PN → fuse → verify target has total qty
- [ ] **Detect Duplicates:** Load date/shift with duplicate PNs → verify detection
- [ ] **Authorization:** Verify split/fuse blocked for non-IMPORTER/ADMIN roles
- [ ] **Both DBs synced:** After split/fuse, check BOTH MG_CMS and qualite databases
