# Preparation Improvement — Feature Documentation

## Overview

This feature adds three major improvements to the Work Order preparation workflow:
1. **Work Order SPLIT** — When importing a quantity less than the total WO quantity, split the remainder into a new WO
2. **Work Order FUSE** — Detect and merge duplicate part numbers across multiple WOs
3. **Import Optimization** — Parallel data loading and duplicate detection

---

## Feature 1: Work Order SPLIT

### When it occurs
Split is triggered when the imported quantity is less than the original WO quantity:
```
remainingQty = originalQty - importedQty
if (remainingQty > 0) → SPLIT
```

### Algorithm
1. Save the cutting plan import normally (existing flow)
2. Generate new `ID_Demande` via `MAX(ID_Demande) + 1`
3. Create new OrderSchedule in qualite DB with remaining quantity and Status='F' (Free)
4. Create new WorkOrder in MG_CMS DB with remaining quantity
5. Update original OrderSchedule — reduce quantity, append to `Remarque_Demande`
6. Update original WorkOrder — reduce quantity

### Key fields
| Field | Original WO (updated) | New WO (created) |
|-------|----------------------|-------------------|
| `ID_Demande` / `wo` | unchanged | MAX(ID_Demande)+1 |
| `Quantite_Demande` / `qtyOpen` | importedQty | remainingQty |
| `Status_Demande` | "O" (by import) | "F" (Free) |
| `Marker_Group_ID_D` | unchanged | originalWo (link back) |
| `Remarque_Demande` | appended with split info | "SPLIT from ID=..." |

### API Endpoint
```
POST /api/workOrder/split
Body: { "wo": "500", "importedQty": 60 }
Response: { "split": true, "newWo": 501, "remainingQty": 40 }
```

---

## Feature 2: Work Order FUSE

### When it occurs
Fuse is auto-detected when loading work orders for a date/shift — the system detects repeated part numbers appearing in multiple WOs with quantity > 0.

### Algorithm
1. Identify the TARGET WO = the LAST one (highest ID_Demande)
2. Sum all quantities
3. Update TARGET: quantity = total, append to Remarque_Demande
4. Update SOURCES: quantity = 0, Marker_Group_ID_D = target WO, append to Remarque_Demande

### API Endpoints
```
GET /api/workOrder/duplicates?date=2026-03-31&shift=1
Response: { "hasDuplicates": true, "duplicates": [...] }

POST /api/workOrder/fuse
Body: ["500", "510", "520"]
Response: { "success": true, "targetWo": "520", "totalQty": 90 }
```

---

## Feature 3: Duplicate Detection Dialog (Frontend)

### Auto-detection
After loading work orders in `getData()`, the system calls `/api/workOrder/duplicates`. If duplicates are found, a modal dialog is displayed showing:
- Each duplicate part number with its WOs
- Total quantity for each duplicate group
- "Fusionner" button to merge per part number

### User flow
1. User loads work orders for a date/shift
2. System auto-detects duplicate part numbers
3. Yellow dialog appears showing duplicates
4. User can click "Fusionner" to merge, or "Ignorer" to dismiss
5. After fuse, data is refreshed automatically

---

## Dual Database Synchronization

Both MG_CMS and qualite databases are updated for every split/fuse operation:

| Operation | MG_CMS (WorkOrder) | qualite (OrderSchedule) |
|-----------|-------------------|------------------------|
| SPLIT (original) | qtyOpen = importedQty | Quantite_Demande = importedQty |
| SPLIT (new) | new WO with remainingQty | new record with Status='F' |
| FUSE (target) | qtyOpen = totalQty | Quantite_Demande = totalQty |
| FUSE (sources) | qtyOpen = 0 | Quantite_Demande = 0, Marker_Group_ID_D = targetWo |

---

## Files Modified

### Backend
- `services/WorkOrderService.java` — Added `splitWorkOrder()`, `fuseWorkOrders()`, `detectDuplicates()`
- `controller/WorkOrderController.java` — Added `POST /fuse`, `POST /split`, `GET /duplicates`

### Frontend
- `components/Layout/ImportationNew.js` — Added duplicate dialog, fuse handler, auto-detection on load
