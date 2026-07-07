# Preparation & Work Order Management — Improvement Plan

> **Project:** MG-CMS  
> **Date:** March 2026  
> **Scope:** Work Order Split, Work Order Fuse, Cutting Plan Import Optimization  
> **Databases:** MG_CMS (main, `spring.datasource`) and qualite (CMS, `cms.datasource`)

---

## Table of Contents

1. [Current Architecture Overview](#1-current-architecture-overview)
2. [Problem Statement](#2-problem-statement)
3. [Feature 1: Work Order SPLIT](#3-feature-1-work-order-split)
4. [Feature 2: Work Order FUSE](#4-feature-2-work-order-fuse)
5. [Feature 3: Importation Optimization](#5-feature-3-importation-optimization)
6. [Dual Database Synchronization Strategy](#6-dual-database-synchronization-strategy)
7. [Implementation Priority & Phases](#7-implementation-priority--phases)

---

## 1. Current Architecture Overview

### Databases Involved

| Database | Connection | Purpose |
|----------|-----------|---------|
| `MG_CMS` | `spring.datasource` (matnr-app16) | Main app DB — WorkOrder, CuttingPlan, CuttingRequest, CuttingRequestSerie, etc. |
| `qualite` | `cms.datasource` (matnr-app01) | CMS DB — Order_Schedule, PlanCoupe, SuiviPlanning, SuiviCoupe, SuiviMatelassage, Coupe, AsprovaWO, ProduitFinit, Matlassage |

### Key Entities

```
WorkOrder (MG_CMS)
├── wo (PK) = Order_Schedule.ID_Demande (string of Long)
├── item, partNumber, qtyOpen, dueDate, shift
└── Synced from Order_Schedule via /api/workOrder/refresh

Order_Schedule (qualite)
├── ID_Demande (PK, Long)
├── PartNumber_Demande, Quantite_Demande, Date_Demande, Shift_Demande
├── Status_Demande (F=Free, O=Open, S=Started, E=Ended, C=Completed)
└── Remarque_Demande

CuttingRequest (MG_CMS)
├── sequence (PK, generated: ddMMyyHHmmNN)
├── cuttingPlanId → CuttingPlan.id
├── cuttingRequestPartNumbers[] → each has wo, quantity, partNumber, item
├── cuttingRequestSeries[] → material, placement, nbrCouche, longueur, machine
└── cuttingRequestBoxs[] → generated from partNumbers × packageQty
```

### Current Import Flow (`POST /api/cuttingRequest`)

1. User selects date/shift → loads WorkOrders + OrderSchedule
2. System matches WOs to CuttingPlans (by partNumber combination)
3. User opens a CuttingPlan, configures series (layers, placements, machines)
4. User clicks **"Enregistrer la séquence"** → `POST /api/cuttingRequest`
5. Backend creates: Sequence ID, Series IDs, Box IDs  
6. Backend writes to **both** databases:
   - MG_CMS: `CuttingRequest`, `CuttingRequestSerie`, `CuttingRequestBox`, `CuttingRequestPartNumber`
   - qualite: `SuiviPlanning`, `AsprovaWO`, `ProduitFinit`, `OrderSchedule` (status → "O"), `Matlassage`, `SuiviMatelassage`, `Coupe`, `SuiviCoupe`, `Sequences`

---

## 2. Problem Statement

### Verification Note (April 2026)

During code verification, the repository already contained:

- `WorkOrderService.splitWorkOrder()`
- `WorkOrderService.fuseWorkOrders()`
- `GET /api/workOrder/duplicates`
- `POST /api/workOrder/fuse`
- `POST /api/workOrder/split`
- duplicate-detection UI in `ImportationNew.js`

But the critical integration point for partial import was missing: `POST /api/cuttingRequest` did **not** call `splitWorkOrder()` after saving the sequence. That meant a partial import changed the sequence state and `OrderSchedule.Status_Demande`, but did **not** reduce the original quantity and did **not** create the remaining WO / `Order_Schedule` row.

The fix is to trigger split reconciliation from `CuttingRequestController.save()` after the cutting request and related CMS rows are written, aggregate imported quantity by WO, and return split metadata to the frontend so `/preparation` can force a full refresh when a residual WO is created.

### Problem A: Quantity Exceeds Cutting Plan Capacity (Need SPLIT)

When a work order has **quantity > what the cutting plan can produce** in one import, or when **two different cutting plans** are combined in the same work order:

- After importing one plan, the consumed quantity stays assigned to the WO
- The **remaining quantity** needs to be moved to a **new work order line** (new `ID_Demande`)
- The new line must exist in both `Order_Schedule` (qualite) and `WorkOrder` (MG_CMS)

### Problem B: Multiple Small Work Orders for Same PN (Need FUSE)

When there are **two or more work orders** for the same part number (e.g., one with qty=30 and another with qty=40):

- They should be **fused** into one work order (e.g., qty=70) and the other set to 0
- The `Remarque_Demande` column should be updated to record the fuse operation
- Same update needed in both databases

### Problem C: Import Performance

Importing multiple work orders is slow and requires manual one-by-one processing.

---

## 3. Feature 1: Work Order SPLIT (Partial Import)

### 3.1 When Split Occurs

Split is triggered **during importation** when the user imports a quantity that is **less than** the total quantity of the work order:

```
remainingQty = originalOrderSchedule.Quantite_Demande - importedQuantity
if (remainingQty > 0) → SPLIT
```

### 3.2 Algorithm — Step by Step

```
Given: Original WO with ID_Demande=500, Quantite_Demande=100, user imports 60

STEP 1: Save the cutting plan import normally (existing flow)

STEP 2: Update the ORIGINAL WorkOrder (MG_CMS DB) and OrderSchedule (qualite DB)
   - WorkOrder.qtyOpen = importedQuantity (60)
   - OrderSchedule.Quantite_Demande = importedQuantity (60)
   - OrderSchedule.Remarque_Demande += ' | SPLIT: qty reduced from 100 to 60, remaining 40 moved to ID={newId}'

STEP 3: Generate new ID_Demande
   - newIdDemande = MAX(ID_Demande from Order_Schedule) + 1
  - Local SQL Server option: create `dbo.seq_Order_Schedule_ID_Demande` and a default on `Order_Schedule.ID_Demande` so local environments without identity support can still auto-generate IDs safely

STEP 4: Create NEW OrderSchedule row in qualite DB (the remaining part)
   - ID_Demande = newIdDemande
   - Copy ALL fields from original:
     * Site_Demande, Chaine_Demande, Project_Demande, PartNumber_Demande
     * Description_PN_Demande, Leather_Kit_Demande, Textil_Kit_Demande
     * WorkCenter_Demande, UserName_Demande, HostName_Demande, SessionW_Demande
   - Quantite_Demande = remainingQty (40)
   - Date_Demande = same date as original
   - Shift_Demande = same shift as original
   - Status_Demande = 'F' (Free — ready for next import)
   - Marker_Group_ID_D = originalIdDemande (500) ← links back to the original WO
   - Import_Date_D = LocalDateTime.now() ← timestamp of when this split was created
   - Remarque_Demande = 'SPLIT from ID_Demande=500, remaining qty=40'
   - Creation_Date_Demande = LocalDate.now()
   - Creation_Hour_Demande = LocalTime.now()

STEP 5: Create NEW WorkOrder row in MG_CMS DB (the remaining part)
   - wo = String.valueOf(newIdDemande)
   - Copy: item, partNumber, description, groupName, designGroup, coverGroup, partNumberStatus
   - qtyOpen = remainingQty (40)
   - dueDate = same date
   - shift = same shift
   - status = original status
   - createdAt = LocalDateTime.now()
```

### 3.3 Key Fields Mapping

| Field | Original WO (updated) | New WO (created) |
|-------|----------------------|-------------------|
| `ID_Demande` / `wo` | unchanged (500) | MAX(ID_Demande)+1 |
| `Quantite_Demande` / `qtyOpen` | importedQty (60) | remainingQty (40) |
| `Status_Demande` | set to "O" (by import) | **"F"** (Free) |
| `Marker_Group_ID_D` | unchanged | **originalIdDemande** (500) |
| `Import_Date_D` | set by import | **LocalDateTime.now()** |
| `Remarque_Demande` | **appended**: "SPLIT: 100→60, rest→{newId}" | "SPLIT from ID=500, qty=40" |

### 3.4 Backend Implementation Plan

#### New Service Method: `WorkOrderService.splitWorkOrder()`

```java
// WorkOrderService.java — new method
@Transactional
public Map<String, Object> splitWorkOrder(String originalWo, int importedQty, User user) {
    // 1. Get originals from both DBs
    WorkOrder originalWO = repo.findByWo(originalWo);
    OrderSchedule originalOS = orderScheduleRepository.findByIdDemande(Long.parseLong(originalWo));
    
    int originalQty = originalOS.getQuantiteDemande();
    int remainingQty = originalQty - importedQty;
    
    if (remainingQty <= 0) return Map.of("split", false);
    
    // 2. Get next ID_Demande from qualite DB
    Long maxId = orderScheduleRepository.getMaxId(); // already exists!
    Long newId = maxId + 1;
    
    // 3. Create new OrderSchedule in qualite DB (remaining part)
    OrderSchedule newOS = new OrderSchedule();
    newOS.setIdDemande(newId);
    newOS.setSiteDemande(originalOS.getSiteDemande());
    newOS.setChaineDemande(originalOS.getChaineDemande());
    newOS.setProjectDemande(originalOS.getProjectDemande());
    newOS.setPartNumberDemande(originalOS.getPartNumberDemande());
    newOS.setDescriptionPNDemande(originalOS.getDescriptionPNDemande());
    newOS.setLeatherKitDemande(originalOS.getLeatherKitDemande());
    newOS.setTextilKitDemande(originalOS.getTextilKitDemande());
    newOS.setQuantiteDemande(remainingQty);
    newOS.setDateDemande(originalOS.getDateDemande());
    newOS.setShiftDemande(originalOS.getShiftDemande());
    newOS.setMatriculeDemandeurDemande(originalOS.getMatriculeDemandeurDemande());
    newOS.setNomDemandeurDemande(originalOS.getNomDemandeurDemande());
    newOS.setStatusDemande("F"); // Free — ready for next import
    newOS.setStatusPSDemande(originalOS.getStatusPSDemande());
    newOS.setStatusReceptionSewingDemande(originalOS.getStatusReceptionSewingDemande());
    newOS.setWorkCenterDemande(originalOS.getWorkCenterDemande());
    newOS.setMarkerGroupIDD(originalWo); // ← link back to the original WO
    newOS.setImportDateD(LocalDateTime.now()); // ← timestamp of the split
    newOS.setRemarqueDemande("SPLIT from ID_Demande=" + originalWo + ", remaining qty=" + remainingQty);
    newOS.setCreationDateDemande(LocalDate.now());
    newOS.setCreationHourDemande(LocalTime.now());
    newOS.setUserNameDemande(user.getFirstName() + " " + user.getLastName());
    newOS.setHostNameDemande("CMS WEB");
    newOS.setSessionWDemande("CMS WEB");
    orderScheduleRepository.save(newOS);
    
    // 4. Create new WorkOrder in MG_CMS DB (remaining part)
    WorkOrder newWO = new WorkOrder();
    newWO.setWo(String.valueOf(newId));
    newWO.setWoid(originalWO.getWoid());
    newWO.setItem(originalWO.getItem());
    newWO.setPartNumber(originalWO.getPartNumber());
    newWO.setDescription(originalWO.getDescription());
    newWO.setGroupName(originalWO.getGroupName());
    newWO.setDesignGroup(originalWO.getDesignGroup());
    newWO.setCoverGroup(originalWO.getCoverGroup());
    newWO.setPartNumberStatus(originalWO.getPartNumberStatus());
    newWO.setQtyOpen((double) remainingQty);
    newWO.setDueDate(originalWO.getDueDate());
    newWO.setShift(originalWO.getShift());
    newWO.setStatus(originalWO.getStatus());
    newWO.setCreatedAt(LocalDateTime.now());
    repo.save(newWO);
    
    // 5. Update original OrderSchedule — reduce quantity & add remark
    String existingRemark = originalOS.getRemarqueDemande() != null ? originalOS.getRemarqueDemande() : "";
    originalOS.setQuantiteDemande(importedQty);
    originalOS.setRemarqueDemande(existingRemark 
        + " | SPLIT: qty " + originalQty + "→" + importedQty 
        + ", remaining " + remainingQty + " moved to ID=" + newId);
    originalOS.setModificationDateDemande(LocalDate.now());
    originalOS.setModificationHourDemande(LocalTime.now());
    orderScheduleRepository.save(originalOS);
    
    // 6. Update original WorkOrder — reduce quantity
    originalWO.setQtyOpen((double) importedQty);
    originalWO.setUpdatedAt(LocalDateTime.now());
    repo.save(originalWO);
    
    return Map.of("split", true, "newWo", newId, "remainingQty", remainingQty);
}
```

#### Integration Point in `CuttingRequestController.save()`

Verified implementation detail:

- Save the `CuttingRequest` and related CMS rows first
- Aggregate imported quantities by `wo` across `cuttingRequestPartNumbers`
- For each WO, load `OrderSchedule`
- If `OrderSchedule.Quantite_Demande > importedQty`, call `workOrderService.splitWorkOrder(wo, importedQty, user)`
- Attach `splitInfo` / `splitInfos` to the returned `CuttingRequest` response

This avoids calling split multiple times when the same WO appears more than once in the request.

Reference logic:

```java
Map<String, Integer> importedQtyByWo = new LinkedHashMap<>();
for (CuttingRequestPartNumber crpn : newObj.getCuttingRequestPartNumbers()) {
  importedQtyByWo.merge(crpn.getWo(), crpn.getQuantity(), Integer::sum);
}

List<Map<String, Object>> splitInfos = new ArrayList<>();
for (Map.Entry<String, Integer> entry : importedQtyByWo.entrySet()) {
  OrderSchedule os = orderScheduleRepository.findByIdDemande(Long.parseLong(entry.getKey()));
  if (os != null && os.getQuantiteDemande() != null && os.getQuantiteDemande() > entry.getValue()) {
    Map<String, Object> splitResult = workOrderService.splitWorkOrder(
      entry.getKey(), entry.getValue(), user);
    if (Boolean.TRUE.equals(splitResult.get("split"))) {
      splitInfos.add(splitResult);
    }
    }
}

newObj.setSplitInfos(splitInfos);
if (splitInfos.size() == 1) {
  newObj.setSplitInfo(splitInfos.get(0));
}
```

### 3.5 Frontend Changes (ImportationNew.js)

After successful save (`POST /api/cuttingRequest`), the response includes split metadata when a partial import created residual WOs. The UI must do a full refresh in that case because `reloadStatuses()` alone cannot inject the newly created WO into `entriesList`.

```javascript
axios.post(`/api/cuttingRequest`, { ...objCR, dueDate: this.state.date, dueShift: this.state.shift })
  .then(res => {
    const splitInfos = Array.isArray(res.data.splitInfos) && res.data.splitInfos.length > 0
      ? res.data.splitInfos
      : (res.data.splitInfo && res.data.splitInfo.split ? [res.data.splitInfo] : []);

    if (splitInfos.length > 0) {
      alert(`WO divisé: ${splitInfos.map(info => `${info.remainingQty} unités vers WO ${info.newWo}`).join(', ')}`);
      this.getData(this.state.date, this.state.shift);
      return;
    }

    this.reloadStatuses();
  })
```

### 3.6 UI Indicator

- Show a **yellow warning** when a WO has `counter < total` after import
- Show a **split badge** on newly created split WOs with the remark info
- Show `Marker_Group_ID_D` value when it points to another WO (parent link)

---

## 4. Feature 2: Work Order FUSE (Duplicate Detection & Merge)

### 4.1 When Fuse Occurs

Fuse is triggered in **two steps**:

1. **Detection**: When loading work orders for a date/shift, the system detects **repeated part numbers** (same `PartNumber_Demande` / `partNumber` appearing in multiple WOs with status "F")
2. **User Confirmation**: The system shows a dialog listing the duplicates and asks the user if they want to fuse them per part number
3. **Execution**: If the user confirms, the system merges quantities into the **last WO** (highest `ID_Demande`) and zeroes out the others

### 4.2 Detection Logic — Backend

#### New Endpoint: `GET /api/workOrder/duplicates`

Returns a map of part numbers to their duplicate WOs for a given date/shift:

```java
// WorkOrderController.java
@GetMapping("/duplicates")
@PreAuthorize("hasRole('IMPORTER') or hasRole('ADMIN')")
public ResponseEntity<?> detectDuplicates(
    @RequestParam LocalDate date,
    @RequestParam(required = false) String shift) {
    
    List<WorkOrder> workOrders = service.findList(date, shift);
    
    // Group by partNumber, keep only groups with 2+ WOs that are still "Free" status
    Map<String, List<WorkOrder>> duplicates = workOrders.stream()
        .filter(wo -> wo.getQtyOpen() != null && wo.getQtyOpen() > 0)
        .collect(Collectors.groupingBy(WorkOrder::getPartNumber))
        .entrySet().stream()
        .filter(e -> e.getValue().size() >= 2)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    
    if (duplicates.isEmpty()) {
        return ResponseEntity.ok(Map.of("hasDuplicates", false));
    }
    
    // Build summary for each duplicate partNumber
    List<Map<String, Object>> duplicateGroups = new ArrayList<>();
    for (Map.Entry<String, List<WorkOrder>> entry : duplicates.entrySet()) {
        Map<String, Object> group = new HashMap<>();
        group.put("partNumber", entry.getKey());
        group.put("workOrders", entry.getValue());
        group.put("totalQty", entry.getValue().stream()
            .mapToDouble(WorkOrder::getQtyOpen).sum());
        group.put("count", entry.getValue().size());
        duplicateGroups.add(group);
    }
    
    return ResponseEntity.ok(Map.of(
        "hasDuplicates", true,
        "duplicates", duplicateGroups
    ));
}
```

### 4.3 Algorithm — Step by Step

```
Given: Same PartNumber "ABC123" appears in:
  - WO_A (ID=500, qty=30)
  - WO_B (ID=510, qty=40)
  - WO_C (ID=520, qty=20)
  
User confirms to fuse "ABC123"

STEP 1: Identify the TARGET WO = the LAST one (highest ID_Demande)
   - Target = WO_C (ID=520)
   - Sources = [WO_A (ID=500), WO_B (ID=510)]

STEP 2: Calculate total quantity
   - totalQty = 30 + 40 + 20 = 90

STEP 3: Update TARGET OrderSchedule (qualite DB)
   - Quantite_Demande = 90 (total quantity)
   - Marker_Group_ID_D = '520' (its own ID, since it's the active one)
   - Remarque_Demande += ' | FUSED: received qty from ID=500 (30) and ID=510 (40), total=90'
   - Modification_Date_Demande = LocalDate.now()
   - Modification_Hour_Demande = LocalTime.now()

STEP 4: Update TARGET WorkOrder (MG_CMS DB)
   - qtyOpen = 90
   - updatedAt = LocalDateTime.now()

STEP 5: For each SOURCE WO (WO_A, WO_B):
   a) Update SOURCE OrderSchedule (qualite DB):
      - Quantite_Demande = 0
      - Marker_Group_ID_D = '520' ← points to the WO that took the quantity
      - Remarque_Demande += ' | FUSED: qty {originalQty} transferred to ID=520'
      - Modification_Date_Demande = LocalDate.now()
      - Modification_Hour_Demande = LocalTime.now()
   b) Update SOURCE WorkOrder (MG_CMS DB):
      - qtyOpen = 0
      - updatedAt = LocalDateTime.now()
```

### 4.4 Key Fields Mapping (FUSE)

| Field | Target WO (last, receives qty) | Source WOs (zeroed out) |
|-------|-------------------------------|------------------------|
| `Quantite_Demande` / `qtyOpen` | **totalQty** (sum of all) | **0** |
| `Marker_Group_ID_D` | own ID (e.g. "520") | **targetId** (e.g. "520") ← who took the qty |
| `Remarque_Demande` | **appended**: "FUSED: received from IDs..." | **appended**: "FUSED: qty transferred to ID=520" |
| `Status_Demande` | unchanged | unchanged (but qty=0) |

### 4.5 Backend Implementation

#### New Endpoint: `POST /api/workOrder/fuse`

```java
// WorkOrderController.java
@PostMapping("/fuse")
@PreAuthorize("hasRole('IMPORTER') or hasRole('ADMIN')")
public ResponseEntity<?> fuseWorkOrders(@RequestBody List<String> woIds, Authentication auth) {
    if (woIds == null || woIds.size() < 2) {
        return ResponseEntity.badRequest().body("Need at least 2 work orders to fuse");
    }
    User user = userService.findByUsername(auth.getName());
    Map<String, Object> result = service.fuseWorkOrders(woIds, user);
    return ResponseEntity.ok(result);
}
```

#### New Service Method: `WorkOrderService.fuseWorkOrders()`

```java
@Transactional
public Map<String, Object> fuseWorkOrders(List<String> woIds, User user) {
    // Sort WO IDs to find the LAST one (highest ID_Demande = target)
    List<String> sortedIds = woIds.stream()
        .sorted(Comparator.comparingLong(id -> Long.parseLong(id)))
        .collect(Collectors.toList());
    
    String targetWo = sortedIds.get(sortedIds.size() - 1); // LAST = highest ID
    List<String> sourceWos = sortedIds.subList(0, sortedIds.size() - 1);
    
    WorkOrder targetWO = repo.findByWo(targetWo);
    OrderSchedule targetOS = orderScheduleRepository.findByIdDemande(Long.parseLong(targetWo));
    
    int totalQty = targetOS.getQuantiteDemande();
    StringBuilder targetRemark = new StringBuilder();
    targetRemark.append("FUSED: received qty from");
    
    for (String sourceWo : sourceWos) {
        WorkOrder sourceWO = repo.findByWo(sourceWo);
        OrderSchedule sourceOS = orderScheduleRepository.findByIdDemande(Long.parseLong(sourceWo));
        
        int sourceQty = sourceOS.getQuantiteDemande();
        totalQty += sourceQty;
        targetRemark.append(" ID=").append(sourceWo).append(" (").append(sourceQty).append("),");
        
        // Zero out source OrderSchedule
        String existingSourceRemark = sourceOS.getRemarqueDemande() != null 
            ? sourceOS.getRemarqueDemande() : "";
        sourceOS.setQuantiteDemande(0);
        sourceOS.setMarkerGroupIDD(targetWo); // ← points to the WO that took the qty
        sourceOS.setRemarqueDemande(existingSourceRemark 
            + " | FUSED: qty " + sourceQty + " transferred to ID=" + targetWo);
        sourceOS.setModificationDateDemande(LocalDate.now());
        sourceOS.setModificationHourDemande(LocalTime.now());
        orderScheduleRepository.save(sourceOS);
        
        // Zero out source WorkOrder
        sourceWO.setQtyOpen(0.0);
        sourceWO.setUpdatedAt(LocalDateTime.now());
        repo.save(sourceWO);
    }
    
    // Update target with total quantity
    String existingTargetRemark = targetOS.getRemarqueDemande() != null 
        ? targetOS.getRemarqueDemande() : "";
    targetOS.setQuantiteDemande(totalQty);
    targetOS.setMarkerGroupIDD(targetWo); // its own ID
    targetOS.setRemarqueDemande(existingTargetRemark 
        + " | " + targetRemark.toString() + " total=" + totalQty);
    targetOS.setModificationDateDemande(LocalDate.now());
    targetOS.setModificationHourDemande(LocalTime.now());
    orderScheduleRepository.save(targetOS);
    
    targetWO.setQtyOpen((double) totalQty);
    targetWO.setUpdatedAt(LocalDateTime.now());
    repo.save(targetWO);
    
    return Map.of(
        "success", true,
        "targetWo", targetWo,
        "totalQty", totalQty,
        "fusedWos", sortedIds,
        "zeroedWos", sourceWos
    );
}
```

### 4.6 Frontend Changes (ImportationNew.js)

#### 4.6.1 Auto-Detection on Load

After loading work orders in `getData()`, call the duplicates endpoint:

```javascript
// After loading work orders, check for duplicates
getData = async (date, shift) => {
  // ... existing WO loading logic ...
  
  // NEW: Check for duplicate part numbers
  const resDuplicates = await axios.get(
    `/api/workOrder/duplicates?date=${date}&shift=${shift}`
  );
  
  if (resDuplicates.data.hasDuplicates) {
    this.setState({ 
      showDuplicateDialog: true, 
      duplicateGroups: resDuplicates.data.duplicates 
    });
  }
};
```

#### 4.6.2 Duplicate Detection Dialog

Show a modal listing each duplicate part number with its WOs:

```javascript
// Duplicate Detection Dialog
{this.state.showDuplicateDialog && (
  <div className="modal show d-block" style={{backgroundColor: 'rgba(0,0,0,0.5)'}}>
    <div className="modal-dialog modal-lg">
      <div className="modal-content">
        <div className="modal-header bg-warning">
          <h5>Work Orders en Doublon Détectés</h5>
          <button className="close" onClick={() => this.setState({showDuplicateDialog: false})}>
            &times;
          </button>
        </div>
        <div className="modal-body">
          <p>Les Part Numbers suivants apparaissent dans plusieurs Work Orders. 
             Voulez-vous les fusionner?</p>
          {this.state.duplicateGroups.map(group => (
            <div key={group.partNumber} className="card mb-2">
              <div className="card-body">
                <h6>{group.partNumber} — {group.count} WOs, Total: {group.totalQty}</h6>
                <table className="table table-sm">
                  <thead><tr><th>WO</th><th>Qty</th><th>Status</th></tr></thead>
                  <tbody>
                    {group.workOrders.map(wo => (
                      <tr key={wo.wo}>
                        <td>{wo.wo}</td>
                        <td>{wo.qtyOpen}</td>
                        <td>{wo.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <button className="btn btn-warning btn-sm"
                  onClick={() => this.handleFuse(group.workOrders.map(wo => wo.wo))}
                >
                  Fusionner → dernière WO ({group.workOrders[group.workOrders.length-1].wo})
                </button>
              </div>
            </div>
          ))}
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary" 
            onClick={() => this.setState({showDuplicateDialog: false})}>
            Ignorer
          </button>
        </div>
      </div>
    </div>
  </div>
)}
```

#### 4.6.3 Fuse Handler

```javascript
handleFuse = (woIds) => {
  if (window.confirm(`Fusionner ${woIds.length} Work Orders?\nLa quantité totale sera dans le dernier WO (${woIds[woIds.length-1]}).\nLes autres seront mis à quantité 0.`)) {
    axios.post('/api/workOrder/fuse', woIds)
      .then(res => {
        alert(`Fusionné avec succès!\nWO ${res.data.targetWo} = ${res.data.totalQty} unités\nWOs mis à 0: ${res.data.zeroedWos.join(', ')}`);
        this.getData(this.state.date, this.state.shift);
        this.setState({ showDuplicateDialog: false });
      })
      .catch(err => alert('Erreur fusion: ' + (err.response?.data || err.message)));
  }
};
```

### 4.7 Validation Rules

- All selected WOs must have the **same partNumber**
- None of the selected WOs should have an active sequence (Status_Demande must be "F")
- WOs with qty=0 are excluded from the fuse
- Show confirmation dialog with source quantities and the target WO

---

## 5. Feature 3: Importation Optimization

### 5.1 Current Pain Points

1. **Sequential processing**: Each WO is imported one by one
2. **Multiple API calls**: getData() makes 6+ sequential axios calls
3. **Full reload after each save**: `getData()` reloads everything after each import

### 5.2 Batch Import Improvement

#### New Endpoint: `POST /api/cuttingRequest/batch`

```java
@PostMapping("/batch")
@PreAuthorize("hasRole('IMPORTER') or hasRole('ADMIN')")
public ResponseEntity<?> saveBatch(@RequestBody List<CuttingRequest> requests, 
                                    Authentication authentication) {
    User user = userService.findByUsername(authentication.getName());
    List<CuttingRequest> results = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    
    for (CuttingRequest obj : requests) {
        try {
            // Reuse existing save logic per item
            CuttingRequest saved = processSingleImport(obj, user);
            results.add(saved);
        } catch (Exception e) {
            errors.add(obj.getCuttingPlanId() + ": " + e.getMessage());
        }
    }
    
    return ResponseEntity.ok(Map.of(
        "imported", results.size(),
        "errors", errors,
        "results", results
    ));
}
```

### 5.3 Frontend: Parallel Data Loading

Replace the sequential `await` chain in `getData()` with parallel loading:

```javascript
getData = async (date, shift) => {
  this.setState({ entriesList: null, loading: true });
  const filter = `date=${date}` + (shift ? `&shift=${shift}` : '');

  try {
    // Step 1: Load WOs + OrderSchedule in parallel
    const [resWO, resOrderSchedule] = await Promise.all([
      axios.get(`/api/workOrder/filter?${filter}`),
      axios.get(`/api/orderSchedule/findByDateAndShift?dateDemande=${date}&shiftDemande=${shift}`)
    ]);

    // Merge statusDemande (existing logic)
    // ...

    // Step 2: Get sequences
    const resSequences = await axios.post(`/api/cuttingRequestPartNumberData/getSequencesByWos`, 
      resWO.data.map(e => e.wo));

    // Step 3: Load all cutting request data in parallel
    const [resCRData, resCRPN, resCRSerie, resCRBox, resGamme, resMachine] = await Promise.all([
      axios.post(`/api/cuttingRequestData/bySequences`, resSequences.data),
      axios.post(`/api/cuttingRequestPartNumberData/bySequences`, resSequences.data),
      axios.post(`/api/cuttingRequestSerieData/bySequences`, resSequences.data),
      axios.post(`/api/cuttingRequestBoxData/bySequences`, resSequences.data),
      axios.post(`/api/gammeTechniqueCMS/getByPartNumber`, resWO.data.map(e => e.partNumber)),
      axios.get(`/api/query/scheduleMachine?date=${date}&shift=${shift}`)
    ]);

    // Process results (existing logic)...
  } finally {
    this.setState({ loading: false });
  }
};
```

### 5.4 Incremental Refresh After Save

Instead of full `getData()` after save, do a targeted update:

```javascript
// After saving a sequence, only update the affected entries
.then(res => {
  // Update only the affected cutting plan in the list (existing logic works)
  // Do NOT call full getData() — just update counters
  // Only reload statuses
  this.reloadStatuses();
})
```

### 5.5 Backend: Combined Refresh Endpoint

Create a single endpoint that returns all preparation data at once:

```java
@GetMapping("/preparation-data")
public ResponseEntity<?> getPreparationData(
    @RequestParam LocalDate date, 
    @RequestParam String shift) {
    
    Map<String, Object> result = new HashMap<>();
    result.put("workOrders", workOrderService.findList(date, shift));
    result.put("orderSchedules", orderScheduleService.findByDateAndShift(date, shift));
    // ... sequences, series, boxes loaded server-side
    return ResponseEntity.ok(result);
}
```

---

## 6. Dual Database Synchronization Strategy

### Critical Rule

Every write operation that touches WorkOrder/OrderSchedule must update **BOTH** databases in a single transaction where possible.

### Tables to Keep in Sync

| Operation | MG_CMS Table | qualite Table | Fields |
|-----------|-------------|---------------|--------|
| SPLIT (original) | WorkOrder: qtyOpen=importedQty | Order_Schedule: Quantite_Demande=importedQty, Remarque_Demande appended | qtyOpen, Quantite_Demande, Remarque_Demande |
| SPLIT (new WO) | WorkOrder: wo=newId, qtyOpen=remainingQty | Order_Schedule: ID_Demande=newId, Status_Demande='F', Marker_Group_ID_D=originalId, Import_Date_D=now, Remarque_Demande set | All fields copied + new keys |
| FUSE (target=last WO) | WorkOrder: qtyOpen=totalQty | Order_Schedule: Quantite_Demande=totalQty, Marker_Group_ID_D=ownId, Remarque_Demande appended | qtyOpen, Quantite_Demande, Marker_Group_ID_D, Remarque_Demande |
| FUSE (sources=zeroed) | WorkOrder: qtyOpen=0 | Order_Schedule: Quantite_Demande=0, Marker_Group_ID_D=targetId, Remarque_Demande appended | qtyOpen, Quantite_Demande, Marker_Group_ID_D, Remarque_Demande |
| Import | CuttingRequest + children | SuiviPlanning, AsprovaWO, ProduitFinit, Matlassage, SuiviMatelassage, Coupe, SuiviCoupe, OrderSchedule |

### Transaction Note

Since MG_CMS and qualite are on **different SQL Server instances**, true distributed transactions are complex. The approach should be:

1. Write to MG_CMS first (primary)
2. Write to qualite second (CMS DB)
3. If qualite write fails, log the error but **do not rollback** MG_CMS — instead flag for manual reconciliation
4. Add a `syncStatus` field to WorkOrder to track sync state

### ID Generation Strategy

For new `ID_Demande` values:

```java
// ALWAYS use OrderScheduleRepository.getMaxId() which queries qualite DB directly
// This ensures no ID collisions even if the desktop CMS app is also creating records
Long newId = orderScheduleRepository.getMaxId() + 1;
```

---

## 7. Implementation Priority & Phases

### Phase 1 — Core Split & Fuse (Priority: HIGH)

| Task | File(s) | Effort |
|------|---------|--------|
| Add `splitWorkOrder()` to WorkOrderService | `WorkOrderService.java` | Backend |
| Integrate split into `CuttingRequestController.save()` — set `Marker_Group_ID_D`, `Import_Date_D`, `Status_Demande='F'` on new WO | `CuttingRequestController.java` | Backend |
| Add `fuseWorkOrders()` to WorkOrderService — target=last WO, update `Marker_Group_ID_D` on zeroed WOs | `WorkOrderService.java` | Backend |
| Add `POST /fuse` endpoint | `WorkOrderController.java` | Backend |
| Add `GET /duplicates` endpoint (auto-detect repeated partNumbers) | `WorkOrderController.java` | Backend |
| Add Duplicate Detection Dialog in ImportationNew | `ImportationNew.js` | Frontend |
| Add Fusionner per-partNumber button in dialog | `ImportationNew.js` | Frontend |
| Add split info display + notification after save | `ImportationNew.js` | Frontend |
| Append to `Remarque_Demande` on every split/fuse (both original & new WOs) | `WorkOrderService.java` | Backend |
| Show `Remarque_Demande` column in WO table | `ImportationNew.js` | Frontend |

### Phase 2 — Import Optimization (Priority: MEDIUM)

| Task | File(s) | Effort |
|------|---------|--------|
| Parallel data loading in `getData()` | `ImportationNew.js` | Frontend |
| Combined preparation-data endpoint | New Controller method | Backend |
| Batch import endpoint | `CuttingRequestController.java` | Backend |
| Incremental refresh after save | `ImportationNew.js` | Frontend |

### Phase 3 — Reliability & Monitoring (Priority: LOW)

| Task | File(s) | Effort |
|------|---------|--------|
| Add `syncStatus` to WorkOrder entity | `WorkOrder.java` | Backend |
| Add error logging for cross-DB writes | Services | Backend |
| Add reconciliation scheduled task | New task class | Backend |
| Show Remarque column in WO table | `ImportationNew.js` | Frontend |

---

## Quick Reference: Key Files to Modify

```
Backend:
├── services/WorkOrderService.java          → splitWorkOrder(), fuseWorkOrders()
├── controller/WorkOrderController.java     → POST /fuse, GET /duplicates endpoints
├── controller/CuttingRequest/
│   └── CuttingRequestController.java       → Integrate split into save()
├── services/cms/OrderScheduleService.java  → Helper methods for qualite DB
├── repositories/WorkOrderRepository.java   → Any new queries
└── cms/repositories/OrderScheduleRepository.java → getMaxId() already exists

Frontend:
├── components/Layout/ImportationNew.js     → Duplicate dialog, Fuse per-PN, split display, parallel loading
└── (no new files needed)
```

---

## Notes

- `OrderScheduleRepository.getMaxId()` already exists — use it for new ID generation
- `OrderScheduleRepository.findByIdDemande()` already exists — use it for lookups
- The `CuttingRequestController.save()` already writes to both databases — follow its pattern
- The `Remarque_Demande` field in `OrderSchedule` exists but is currently unused — perfect for tracking split/fuse operations. **Always append** (don't overwrite) using ` | ` separator so history is preserved
- Work orders in `state.specialElems` already track user selection — can still be used as fallback for manual fuse
- The existing `WorkOrderService` already has JdbcTemplate for raw SQL to CMS DB — can be used for complex queries
- **SPLIT**: New WO gets `Marker_Group_ID_D` = original WO's `ID_Demande` (links child → parent)
- **SPLIT**: New WO gets `Import_Date_D` = `LocalDateTime.now()` and `Status_Demande` = `"F"` (Free)
- **FUSE**: Target = **last WO** (highest `ID_Demande`), receives the total quantity
- **FUSE**: Source WOs get `Marker_Group_ID_D` = target WO's `ID_Demande` (who took the qty)
- **FUSE**: Auto-detection of duplicates via `GET /duplicates` on page load — user confirms per partNumber
