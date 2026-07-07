# Changes Log — February 2026 Session

## 1. MachineTypeSwap.js Fixes

### File: `src/main/js/components/Layout/MachineTypeSwap.js`

#### Fix: Machine Select Options
- **Before:** Select dropdown showed all available machines from API (`availableMachines`)
- **After:** Select dropdown shows only distinct machines from the results table (`distinctMachines`)
- This prevents users from selecting machines that have no placements in the results

#### Feature: Per-Row Activation Toggle
- Added "Actions" column header in results table
- Added toggle button per row that calls `POST /api/cuttingPlanMaterialPlacementData/toggleActivation`
- Button styled conditionally: `btn-outline-danger` (active → click to deactivate) / `btn-outline-success` (inactive → click to activate)
- Method: `toggleRowActivation(row)`

---

## 2. PartNumberMaterialConfig — Save to CMS

### Overview
Added the ability to save a PartNumberMaterialConfig from the MG-CMS database back to the CMS database (qualite DB on matnr-app01). This is the reverse of the existing "refresh" operation (CMS → MG-CMS).

### Machine Type → CMS ID Mapping
| Machine Type | CMS ID |
|-------------|--------|
| Lectra | 1 |
| Gerber | 2 |
| DIE | 3 |
| LASER-LSR | 4 |
| Lectra IP6 | 5 |
| LASER-DXF | 6 |

### Backend Changes

#### ItemPlanCoupeService.java
- Added `MACHINE_TYPE_TO_CMS_ID` static map
- Added `saveToCms(PartNumberMaterialConfig config, String username)` method:
  - `@Transactional("cmsTransactionManager")` — operates on CMS database
  - **Strategy:** Delete-and-recreate children, update or create parent item
  - Checks if item exists by `itemNumberPlan`
  - If exists: uses same ID, deletes all children (categories, seuils → intervalSeuils, machines → intervalMachines)
  - If new: generates `MAX(id) + 1` for the new item
  - Saves full hierarchy: Item → Categories, Seuils → IntervalSeuils, Machines → IntervalMachines
  - Each child entity gets `MAX(id) + 1` for its own ID
  - Parses pliesConfig format: `"minPlie;value|minPlie;value"`

#### CMS Repository Changes (5 repositories modified)
Added `findMaxId()` @Query and `deleteBy*` methods:
- `CategoryLaizePlanCoupeRepository`: `findMaxId()`, `deleteByIdItemForeignPlan()`
- `SeuilLongueurPlanCoupeRepository`: `findMaxId()`, `deleteByIdItemForeign1Plan()`
- `IntervalSeuilPlanCoupeRepository`: `findMaxId()`, `deleteByIdSeuilForeignPlan()`
- `ItemMachinePlanCoupeRepository`: `findMaxId()`, `deleteByIdItemForeignPlan()`
- `IntervalItemMachinePlanCoupeRepository`: `findMaxId()`, `deleteByIdItemMachineForeignPlan()`

#### PartNumberMaterialConfigController.java
- Added `POST /api/partNumberMaterialConfig/saveToCms/{id}` endpoint
- Uses `Authentication` to get current username
- Loads config by ID, calls `itemPlanCoupeService.saveToCms(config, username)`
- Returns success/error response

### Frontend Changes

#### PartNumberMaterialConfigForm.js
- Added `faUpload` icon import
- Added `savingToCms` state variable (loading indicator)
- Added "Sauvegarder vers CMS" button (yellow/warning style) next to existing "Enregistrer" button
- Button only visible when editing an existing entity (`entityId` not null)
- Calls `POST /api/partNumberMaterialConfig/saveToCms/{entityId}`
- Shows loading text "Sauvegarde..." while in progress
- Shows success/error alert on completion

---

## 3. MachineLog — Lazy Loading

### Problem
The MachineLog page was slow to load because it scanned all files in every machine folder on initial page load.

### Solution
Changed to a lazy loading approach: initial load fetches only folder names, files are loaded only when a folder is selected.

### Backend Changes

#### MachineLogController.java
- Added `GET /api/machineLog/folderNames` endpoint
- Returns only `{ name: folderName }` objects without scanning files inside subfolders
- Sorted alphabetically

### Frontend Changes

#### MachineLog.js
- `loadFolders()` now calls `/api/machineLog/folderNames` (lightweight, no file scanning)
- Folder list table simplified: removed "Dernier Log" and "Il y a" columns
- Shows only machine name in the folder list
- Layout changed from `col-md-4` / `col-md-8` to `col-md-3` / `col-md-9` (more space for file content)

---

## 4. Plan de Charge — See PLAN_DE_CHARGE.md

All Plan de Charge changes (nbrCouche LASER-DXF fix, retard redesign, Unknown machine type fix, UI improvements) are documented in the "Updates (February 2026)" section of `PLAN_DE_CHARGE.md`.

---

## Files Modified Summary

| File | Changes |
|------|---------|
| `MachineTypeSwap.js` | distinctMachines select, toggleRowActivation, Actions column |
| `CuttingRequestSerieInfoRepository.java` | CASE WHEN for LASER-DXF, findSeriesWithTimestampsForRetard |
| `PlanDeChargeService.java` | isLaserDxf check, rewritten retard calc, Unknown fallback |
| `PlanDeCharge.js` | Machine type filter, retard column, partial retard display |
| `PlanDeCharge.scss` | pdc-row-partial-retard, pdc-status-partial styles |
| `MachineLog.js` | Lazy loading, simplified folder table, col-md-3/9 layout |
| `MachineLogController.java` | New /folderNames endpoint |
| `CategoryLaizePlanCoupeRepository.java` | findMaxId(), deleteByIdItemForeignPlan() |
| `SeuilLongueurPlanCoupeRepository.java` | findMaxId(), deleteByIdItemForeign1Plan() |
| `IntervalSeuilPlanCoupeRepository.java` | findMaxId(), deleteByIdSeuilForeignPlan() |
| `ItemMachinePlanCoupeRepository.java` | findMaxId(), deleteByIdItemForeignPlan() |
| `IntervalItemMachinePlanCoupeRepository.java` | findMaxId(), deleteByIdItemMachineForeignPlan() |
| `ItemPlanCoupeService.java` | saveToCms() method with full CMS hierarchy save |
| `PartNumberMaterialConfigController.java` | POST /saveToCms/{id} endpoint |
| `PartNumberMaterialConfigForm.js` | "Sauvegarder vers CMS" button |
