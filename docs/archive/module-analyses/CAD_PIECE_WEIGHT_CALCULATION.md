# CAD Piece Weight Calculation — Implementation Plan

## Objective

Enable the CAD team to import a CSV file containing piece details (from CAD software export), store it in a new database table, and calculate the **real weight** of each part number cover based on:
- The patterns (pieces) composing each part number and their areas (from the CSV)
- The fabric material used for each pattern (from `Files` entity in CTC)
- The weight per m² of each material (new `weightUnit` field on `PartNumberMaterialConfig`)

---

## CSV Structure (Source: CAD Export)

| Column | Type | Description |
|--------|------|-------------|
| `Piece Name` | String (PK) | Unique piece identifier (e.g. `AA-00226771_A_1`) |
| `DESCRIP` | String | Pattern description (e.g. `CUSH_HAB01_AC1_RH`) |
| `CATEGORY` | String | Piece category (e.g. `REAR INSERT`, `FRONT INSERT`) |
| `COMMENT` | String | Optional comment |
| `RULE TABLE` | String | Rule table code (e.g. `AC1`, `AC2`, `RULIB`) |
| `BYTE SIZE` | Integer | Byte size of the piece data |
| `Area` | Double | Piece area (cm²) |
| `Total Area` | Double | Total area (cm²) |
| `Perimeter` | Double | Perimeter (cm) |
| `Base Size` | Integer | Base size |
| `Smallest Size` | Integer | Smallest size |
| `NUM INT` | Integer | Number of internal points |
| `NUM NCH` | Integer | Number of notches |
| `NUM GP` | Integer | Number of grade points |
| `NUM CRN` | Integer | Number of corners |
| `PIECE X` | Double | Piece X dimension |
| `PIECE Y` | Double | Piece Y dimension |
| `Shrink/Stretch X` | String | Shrink/stretch X value |
| `Shrink/Stretch Y` | String | Shrink/stretch Y value |
| `Fabric Code` | String | Fabric code letter |
| `DATE` | String | Last modification date |
| `User Last Mod` | String | Last modification user |
| `Created Time` | String | Creation timestamp |
| `User Created` | String | Created by user |
| `Prev Mod Time` | String | Previous modification time |
| `User Prev Mod` | String | Previous modification user |

---

## Weight Calculation Logic

### Formula

For a given **partNumberCover**:

$$
\text{Weight}_{PN} = \sum_{\text{pattern}} \left( \text{Area}_{\text{pattern}} \times \text{quantity}_{\text{pattern}} \times \text{weightUnit}_{\text{material}} \right)
$$

Where:
- **Area** comes from the `PieceDetail` table (imported CSV), field `area` in cm² → converted to m²
- **quantity** comes from the `Files` entity (CTC), for each pattern of type `fabric` under the given partNumberCover
- **weightUnit** comes from the `PartNumberMaterialConfig` entity, the weight in kg/m² of the material

### Step-by-Step Algorithm

1. User provides a list of **partNumberCover** values
2. For each partNumberCover:
   a. Query `Files` entity: `findByTypeAndPartNumberCover("fabric", partNumberCover)` → get list of patterns with their `partNumberMaterial` and `quantity`
   b. For each pattern found:
      - Look up the `PieceDetail` record by matching the `pattern` field (from Files) to identify the piece and get its `area`
      - Look up `PartNumberMaterialConfig` by `partNumberMaterial` to get the `weightUnit` (kg/m²)
      - Calculate: `area (cm²) / 10000 * quantity * weightUnit (kg/m²)` = weight contribution in kg
   c. Sum all pattern contributions → **total weight for the partNumberCover**
3. Return results table with errors for:
   - **Pattern not found**: pattern from `Files` not found in `PieceDetail` table
   - **Weight unit not found**: `partNumberMaterial` has no `weightUnit` configured in `PartNumberMaterialConfig`

---

## Storage: Save Results in `PartNumberInfo`

Instead of a separate weight results table, we store the calculated **weight** and **total perimeter** directly in the existing `PartNumberInfo` entity (PK: `partNumber`). Two new fields will be added:

| Field | Type | Description |
|-------|------|-------------|
| `weight` | Double | Calculated total weight (kg) for this partNumber |
| `totalPerimetre` | Double | Sum of perimeters of all patterns composing this partNumber (cm) |
| `tempsDeCoupe` | Double | Estimated cutting time in **minutes** for this partNumber (from Process interface) |

Current `PartNumberInfo` fields: `partNumber` (PK), `perimetre`, `packageQty`

After calculation, `PartNumberInfo` is updated/saved with the new values so results persist and are reusable across the application.

---

## Phase 1: Database — New Table `PieceDetail`

### 1.1 Entity: `PieceDetail.java`

**Location**: `src/main/java/com/lear/MGCMS/domain/PieceDetail.java`

```java
@Entity
@Table(name = "PieceDetail")
public class PieceDetail {
    @Id
    private String pieceName;        // "AA-00226771_A_1" — primary key
    private String descrip;
    private String category;
    private String comment;
    private String ruleTable;
    private Integer byteSize;
    private Double area;             // cm²
    private Double totalArea;        // cm²
    private Double perimeter;
    private Double baseSize;
    private Double smallestSize;
    private Integer numInt;
    private Integer numNch;
    private Integer numGp;
    private Integer numCrn;
    private Double pieceX;
    private Double pieceY;
    private String shrinkStretchX;
    private String shrinkStretchY;
    private String fabricCode;
    private String date;
    private String userLastMod;
    private String createdTime;
    private String userCreated;
    private String prevModTime;
    private String userPrevMod;
    // + importedAt (LocalDateTime), importedBy (String)
}
```

### 1.2 Repository: `PieceDetailRepository.java`

**Location**: `src/main/java/com/lear/MGCMS/repositories/PieceDetailRepository.java`

```java
public interface PieceDetailRepository extends JpaRepository<PieceDetail, String> {
    List<PieceDetail> findByDescripContaining(String descrip);
    Optional<PieceDetail> findByPieceName(String pieceName);
}
```

---

## Phase 2: Modify `PartNumberMaterialConfig` — Add `weightUnit`

### 2.1 Entity Change

**File**: `src/main/java/com/lear/MGCMS/domain/PartNumberMaterialConfig.java`

Add new field:
```java
@Column(name = "weight_unit")
private Double weightUnit; // kg per m² of this material
```
+ getter/setter

### 2.2 Database Migration

```sql
ALTER TABLE PartNumberMaterialConfig ADD weight_unit FLOAT NULL;
```

### 2.3 Frontend — metadata.js

Add `weightUnit` field to the `partNumberMaterialConfig` metadata definition so CAD users can set/view it in the existing CRUD form.

---

## Phase 3: Backend — CSV Import & Weight Calculation

### 3.1 Service: `PieceDetailService.java`

**Location**: `src/main/java/com/lear/MGCMS/services/PieceDetailService.java`

**Methods**:
- `importCsv(MultipartFile file, String importedBy)` — parse CSV, map to `PieceDetail` entities, save/update by `pieceName`
- `findAll()`, `findById(String pieceName)` — basic lookups

### 3.2 Service: `PartNumberWeightCalculationService.java`

**Location**: `src/main/java/com/lear/MGCMS/services/PartNumberWeightCalculationService.java`

**Method**: `calculateWeights(List<String> partNumberCovers)`

**Returns**: List of result objects:
```java
public class WeightCalculationResult {
    String partNumberCover;
    Double totalWeight;            // kg (null if errors)
    List<PatternDetail> patterns;  // breakdown per pattern
    List<String> errors;           // error messages
}

public class PatternDetail {
    String pattern;
    String partNumberMaterial;
    Double area;                   // cm²
    Integer quantity;
    Double weightUnit;             // kg/m²
    Double weightContribution;     // kg
}
```

**Algorithm**:
```
for each partNumberCover:
  files = filesRepository.findByTypeAndPartNumberCover("fabric", partNumberCover)
  if files is empty → error "No fabric files found for {partNumberCover}"
  
  totalWeight = 0
  for each file in files:
    pieceDetail = pieceDetailRepository.findByDescripContaining(file.pattern)
                  OR match by pattern name logic
    if pieceDetail is null → error "Pattern not found: {file.pattern}"
    
    materialConfig = partNumberMaterialConfigRepository.findById(file.partNumberMaterial)
    if materialConfig is null OR materialConfig.weightUnit is null
      → error "Weight unit not found for material: {file.partNumberMaterial}"
    
    weightContribution = (pieceDetail.area / 10000) * file.quantity * materialConfig.weightUnit
    totalWeight += weightContribution
  
  result.totalWeight = totalWeight
  
  // Save to PartNumberInfo
  partNumberInfo = partNumberInfoRepository.findByPartNumber(partNumberCover)
  if partNumberInfo == null:
      partNumberInfo = new PartNumberInfo(partNumberCover)
  partNumberInfo.setWeight(totalWeight)
  partNumberInfoRepository.save(partNumberInfo)
```

### 3.3 Controller: `PieceDetailController.java`

**Location**: `src/main/java/com/lear/MGCMS/controller/PieceDetailController.java`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/pieceDetail/import` | POST | Upload CSV file, parse and store in `PieceDetail` table |
| `/api/pieceDetail/all` | GET | List all pieces (paginated) |
| `/api/pieceDetail/{pieceName}` | GET | Get single piece detail |
| `/api/pieceDetail/calculateWeight` | POST | Body: `{ "partNumberCovers": ["PN1","PN2",...] }` → returns weight calculations |

---

## Phase 4: Frontend — CAD Team UI

### 4.1 metadata.js — Add `pieceDetail` Entity

```javascript
pieceDetail: {
  displayName: "Détails Pièces (CAD)",
  operation: ["search", "delete"],
  firstOrderProperty: "pieceName",
  firstOrderDirection: "asc",
  fields: [
    { name: "pieceName", displayName: "Nom Pièce", type: "text" },
    { name: "descrip", displayName: "Description", type: "text" },
    { name: "category", displayName: "Catégorie", type: "text" },
    { name: "ruleTable", displayName: "Rule Table", type: "text" },
    { name: "area", displayName: "Surface (cm²)", type: "number" },
    { name: "totalArea", displayName: "Surface Totale (cm²)", type: "number" },
    { name: "perimeter", displayName: "Périmètre (cm)", type: "number" },
    { name: "pieceX", displayName: "Dimension X", type: "number" },
    { name: "pieceY", displayName: "Dimension Y", type: "number" },
    { name: "fabricCode", displayName: "Code Tissu", type: "text" },
    { name: "importedAt", displayName: "Date Import", type: "datetime", hideForm: true },
    { name: "importedBy", displayName: "Importé Par", type: "text", hideForm: true },
  ],
  fieldsFilter: [
    { name: "pieceName", displayName: "Nom Pièce", type: "text" },
    { name: "descrip", displayName: "Description", type: "text" },
    { name: "category", displayName: "Catégorie", type: "text" },
    { name: "fabricCode", displayName: "Code Tissu", type: "text" },
  ],
}
```

### 4.2 CSV Import Component

**File**: `src/main/js/components/Layout/PieceDetailImport.js`

- File upload input (accept `.csv`)
- Upload button → POST to `/api/pieceDetail/import`
- Display import result: number of records imported, any parsing errors

### 4.3 Weight Calculation Component

**File**: `src/main/js/components/Layout/WeightCalculation.js`

- Textarea or tag-input for multiple partNumberCover values
- "Calculate" button → POST to `/api/pieceDetail/calculateWeight`
- Results table:

| Part Number Cover | Total Weight (kg) | Status |
|---|---|---|
| PN-001 | 2.45 | ✅ OK |
| PN-002 | — | ❌ Pattern not found: PATTERN_X |
| PN-003 | — | ❌ Weight unit not found for material: MAT_Y |

- Expandable rows showing per-pattern breakdown (pattern, material, area, quantity, weightUnit, contribution)

### 4.4 Dashboard Integration

Add links in `Dashboard.js` under the CAD section:
- "Import Pièces CSV" → `/pieceDetailImport`
- "Calcul Poids" → `/weightCalculation`
- "Détails Pièces" → entity list for `pieceDetail`

### 4.5 App.js Routes

```jsx
<SecuredRoute exact path="/pieceDetailImport" component={PieceDetailImport} />
<SecuredRoute exact path="/weightCalculation" component={WeightCalculation} />
```

---

## Phase 5: Update `PartNumberMaterialConfig` UI

### 5.1 metadata.js

Add to `partNumberMaterialConfig` fields array:
```javascript
{ name: "weightUnit", displayName: "Poids/m² (kg)", type: "number" }
```

This allows CAD team to set the weight per m² for each material directly from the existing material config form.

---

## Error Handling Summary

| Error | When | Message |
|-------|------|---------|
| Pattern not found | Pattern from `Files` doesn't match any `PieceDetail.pieceName` or `PieceDetail.descrip` | `"Pattern non trouvé: {pattern}"` |
| Weight unit not found | `PartNumberMaterialConfig.weightUnit` is null for the material | `"Poids unitaire non configuré pour le matériau: {partNumberMaterial}"` |
| No fabric files | No `Files` records with type `fabric` for the partNumberCover | `"Aucun fichier tissu trouvé pour: {partNumberCover}"` |
| CSV parse error | Malformed CSV row during import | `"Erreur ligne {n}: {details}"` |

---

## Phase 6: Process Interface — Perimeter & Cutting Time per PartNumber

### 6.1 Objective

New screen for the **Process team** to calculate the **cutting time per partNumber** based on:
- The **perimeter proportion** of each partNumber within its active cutting plan
- The **estimated cutting time** of each activated placement in that cutting plan

Given a **Period** (date range) and a **Project**, the system:
1. Gets all partNumbers in that project
2. For each partNumber, finds the **latest active CuttingPlan** that includes it
3. Computes the **perimeter percentage** of that partNumber within the cutting plan
4. Determines the **total estimated cutting time** of that cutting plan (sum of `tempsDeCoupe` of all activated placements)
5. Attributes cutting time to each partNumber proportionally to its perimeter share
6. Saves the result (`tempsDeCoupe` in minutes) in `PartNumberInfo`

### 6.2 Data Flow & Algorithm

#### Step 1 — Inputs
- **Period**: `startDate` → `endDate` (date range filter)
- **Project**: project name (e.g. `HAB01`)

#### Step 2 — Find Active CuttingPlans for the Project

Using existing repository method:
```java
// CuttingPlanRepository.java — already exists
List<CuttingPlanLight> findAllActiveInProjets(List<String> projets, LocalDateTime currentTime);
```

This returns all cutting plans where `enabled = true` OR within `startDate`/`endDate` range.

#### Step 3 — For each CuttingPlan, compute perimeter per partNumber

**CuttingPlan structure recap:**
- `CuttingPlan` → has many `CuttingPlanPartNumber` (partNumber, quantity)
- `CuttingPlan` → has many `CuttingPlanMaterial` → each has many `CuttingPlanMaterialPlacement`

Each `CuttingPlanMaterialPlacement` has:
- `placement` — placement name
- `partNumbers` — comma-separated list of partNumbers in this placement
- `activated` — Boolean, only count activated ones
- `perimetre` — perimeter of this placement
- `tempsDeCoupe` — estimated cutting time of this placement (minutes)

**Algorithm for perimeter per partNumber:**
```
for each activeCuttingPlan:
    perimetrePerPN = {}  // Map<String partNumber, Double totalPerimetre>
    
    for each CuttingPlanMaterial in cuttingPlan.cuttingPlanMaterials:
        for each CuttingPlanMaterialPlacement in material.cuttingPlanMaterialPlacement:
            if placement.activated != true → skip
            
            // Parse partNumbers (comma-separated)
            pnList = placement.partNumbers.split(",")
            
            for each pn in pnList:
                perimetrePerPN[pn] += placement.perimetre  // accumulate
    
    // Total perimeter of the plan
    totalPerimetre = sum(perimetrePerPN.values())
    
    // Percentage of each partNumber
    for each pn in perimetrePerPN:
        percentagePN = perimetrePerPN[pn] / totalPerimetre
```

#### Step 4 — Compute total cutting time of the CuttingPlan

```
totalTempsDeCoupe = 0  // minutes

for each CuttingPlanMaterial in cuttingPlan.cuttingPlanMaterials:
    for each CuttingPlanMaterialPlacement in material.cuttingPlanMaterialPlacement:
        if placement.activated != true → skip
        totalTempsDeCoupe += placement.tempsDeCoupe
```

> **Note**: This mirrors the Plan de Charge logic where `tempsDeCoupe` from `CuttingRequestSerieInfo` is summed per serie. Here we sum it at the CuttingPlan level from the activated placements.

#### Step 5 — Attribute cutting time to each partNumber

```
for each pn in perimetrePerPN:
    tempsCoupePN = totalTempsDeCoupe * (perimetrePerPN[pn] / totalPerimetre)
    
    // Save in PartNumberInfo
    partNumberInfo = partNumberInfoRepository.findByPartNumber(pn)
    if partNumberInfo == null:
        partNumberInfo = new PartNumberInfo(pn)
    partNumberInfo.setTempsDeCoupe(tempsCoupePN)           // in minutes
    partNumberInfo.setTotalPerimetre(perimetrePerPN[pn])    // in cm
    partNumberInfoRepository.save(partNumberInfo)
```

### 6.3 Entity Changes — `PartNumberInfo`

**File**: `src/main/java/com/lear/MGCMS/domain/PartNumberInfo.java`

**Current fields**: `partNumber` (PK), `perimetre`, `packageQty`

**Add fields**:
```java
private Double weight;           // kg — from Phase 3 weight calculation
private Double totalPerimetre;   // cm — sum of perimeters of all activated placements containing this PN
private Double tempsDeCoupe;     // minutes — attributed cutting time from CuttingPlan
```

**Database migration**:
```sql
ALTER TABLE PartNumberInfo ADD weight FLOAT NULL;
ALTER TABLE PartNumberInfo ADD totalPerimetre FLOAT NULL;
ALTER TABLE PartNumberInfo ADD tempsDeCoupe FLOAT NULL;
```

### 6.4 Backend Service — `PartNumberCuttingTimeService.java`

**Location**: `src/main/java/com/lear/MGCMS/services/PartNumberCuttingTimeService.java`

**Method**: `calculateCuttingTimeByProject(String project, LocalDate startDate, LocalDate endDate)`

**Returns**: List of result objects:
```java
public class CuttingTimeResult {
    String partNumber;
    Long cuttingPlanId;
    String cuttingPlanDescription;
    Double perimetrePN;           // cm
    Double totalPerimetrePlan;    // cm
    Double percentagePerimetre;   // 0-100%
    Double totalTempsCoupePlan;   // minutes (sum of activated placements)
    Double tempsCoupePN;          // minutes (attributed to this PN)
    List<String> errors;
}
```

**Full algorithm**:
```
Input: project, startDate, endDate
Results: List<CuttingTimeResult>

1. activePlans = cuttingPlanRepository.findAllActiveInProjets([project], now)
   If using period filter: filter plans where plan.enabledAt or plan.createdAt falls within [startDate, endDate]

2. For each plan in activePlans:
   a. Build perimetrePerPN map:
      Map<String, Double> perimetrePerPN = {}
      for each material in plan.cuttingPlanMaterials:
          for each placement in material.cuttingPlanMaterialPlacement:
              if placement.activated != true → skip
              pnList = placement.partNumbers.split(",").map(trim)
              for each pn in pnList:
                  perimetrePerPN[pn] += placement.perimetre ?: 0

   b. Total perimeter:
      totalPerimetre = sum(perimetrePerPN.values())
      if totalPerimetre == 0 → error "No perimeter data for plan {planId}"

   c. Total cutting time (only activated placements):
      totalTempsDeCoupe = 0
      for each material in plan.cuttingPlanMaterials:
          for each placement in material.cuttingPlanMaterialPlacement:
              if placement.activated != true → skip
              totalTempsDeCoupe += placement.tempsDeCoupe ?: 0

   d. For each partNumber:
      percentage = perimetrePerPN[pn] / totalPerimetre * 100
      tempsCoupePN = totalTempsDeCoupe * (perimetrePerPN[pn] / totalPerimetre)
      
      → save to PartNumberInfo:
         partNumberInfo.totalPerimetre = perimetrePerPN[pn]
         partNumberInfo.tempsDeCoupe = tempsCoupePN  (minutes)
         
      → add to results with breakdown
```

### 6.5 Backend Controller — `PartNumberCuttingTimeController.java`

**Location**: `src/main/java/com/lear/MGCMS/controller/PartNumberCuttingTimeController.java`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/partNumberCuttingTime/calculate` | POST | Body: `{ "project": "HAB01", "startDate": "2026-01-01", "endDate": "2026-03-31" }` → returns cutting time per PN |
| `/api/partNumberCuttingTime/results/{project}` | GET | Get last saved results for a project |

### 6.6 Frontend Component — `CuttingTimePerPartNumber.js`

**File**: `src/main/js/components/Layout/CuttingTimePerPartNumber.js`

**UI Layout**:

```
┌─────────────────────────────────────────────────────────┐
│  TEMPS DE COUPE PAR PART NUMBER                         │
│                                                         │
│  Projet: [Dropdown HAB01 ▾]                             │
│  Période: [Start Date] → [End Date]                     │
│  [Calculer]                                             │
│                                                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │ Cutting Plan: #1234 - HAB01 v2.1 (Active)         │ │
│  │ Total Périmètre: 1245.6 cm                        │ │
│  │ Total Temps de Coupe: 180.5 min                   │ │
│  ├──────────────┬────────┬──────┬────────┬───────────┤ │
│  │ Part Number  │ Périm. │  %   │ Temps  │ Status    │ │
│  ├──────────────┼────────┼──────┼────────┼───────────┤ │
│  │ PN-001       │ 450.2  │ 36.1 │ 65.2   │ ✅ Saved  │ │
│  │ PN-002       │ 380.1  │ 30.5 │ 55.0   │ ✅ Saved  │ │
│  │ PN-003       │ 415.3  │ 33.4 │ 60.3   │ ✅ Saved  │ │
│  └──────────────┴────────┴──────┴────────┴───────────┘ │
│                                                         │
│  [Sauvegarder dans PartNumberInfo]                      │
└─────────────────────────────────────────────────────────┘
```

**Columns**:
| Column | Source | Description |
|--------|--------|-------------|
| Part Number | `CuttingPlanPartNumber.partNumber` | PN from the cutting plan |
| Périmètre (cm) | Calculated | Sum of perimeters of activated placements containing this PN |
| % | Calculated | `(perimetrePN / totalPerimetrePlan) * 100` |
| Temps de Coupe (min) | Calculated | `totalTempsCoupePlan * percentage / 100` |
| Status | — | Saved or error |

**Dashboard entry**: Add under Process section (or CAD section) in `Dashboard.js`:
- "Temps de Coupe / PN" → `/cuttingTimePerPartNumber`

**Route in App.js**:
```jsx
<SecuredRoute exact path="/cuttingTimePerPartNumber" component={CuttingTimePerPartNumber} />
```

### 6.7 Relationship to Plan de Charge

The Plan de Charge calculates total load per machine per shift using `tempsDeCoupe` from `CuttingRequestSerieInfo`. This new feature works at a **higher level**:

| Aspect | Plan de Charge | Cutting Time per PN (new) |
|--------|----------------|---------------------------|
| Level | Per serie (execution) | Per CuttingPlan (definition) |
| Source | `CuttingRequestSerieInfo.tempsDeCoupe` | `CuttingPlanMaterialPlacement.tempsDeCoupe` |
| Breakdown | By machine, by shift | By partNumber (perimeter proportion) |
| Purpose | Load balancing machines | Cost attribution to partNumbers |

Both use the same `tempsDeCoupe` concept but at different pipeline stages.

---

## Error Handling Summary (All Phases)

| Error | Phase | When | Message |
|-------|-------|------|---------|
| Pattern not found | 3 | Pattern from `Files` doesn't match any `PieceDetail` | `"Pattern non trouvé: {pattern}"` |
| Weight unit not found | 3 | `PartNumberMaterialConfig.weightUnit` is null | `"Poids unitaire non configuré pour: {partNumberMaterial}"` |
| No fabric files | 3 | No `Files` with type `fabric` for the PN | `"Aucun fichier tissu trouvé pour: {partNumberCover}"` |
| CSV parse error | 1 | Malformed CSV row during import | `"Erreur ligne {n}: {details}"` |
| No active cutting plan | 6 | No enabled CuttingPlan for project | `"Aucun plan de coupe actif pour le projet: {project}"` |
| No perimeter data | 6 | All activated placements have null perimeter | `"Pas de données périmètre pour le plan: {planId}"` |
| No cutting time data | 6 | All activated placements have null tempsDeCoupe | `"Pas de temps de coupe pour le plan: {planId}"` |

---

## File Changes Summary

### New Files
| File | Type | Description |
|------|------|-------------|
| `domain/PieceDetail.java` | Entity | New table for CSV piece data |
| `repositories/PieceDetailRepository.java` | Repository | JPA repository for PieceDetail |
| `services/PieceDetailService.java` | Service | CSV import + CRUD |
| `services/PartNumberWeightCalculationService.java` | Service | Weight calculation logic |
| `services/PartNumberCuttingTimeService.java` | Service | Cutting time per PN calculation |
| `controller/PieceDetailController.java` | Controller | REST endpoints for CSV & weight |
| `controller/PartNumberCuttingTimeController.java` | Controller | REST endpoints for cutting time |
| `components/Layout/PieceDetailImport.js` | React | CSV upload UI |
| `components/Layout/WeightCalculation.js` | React | Weight calculation UI |
| `components/Layout/CuttingTimePerPartNumber.js` | React | Process: cutting time per PN UI |

### Modified Files
| File | Change |
|------|--------|
| `domain/PartNumberMaterialConfig.java` | Add `weightUnit` field (Double) |
| `domain/PartNumberInfo.java` | Add `weight`, `totalPerimetre`, `tempsDeCoupe` fields (Double) |
| `metadata.js` | Add `pieceDetail` entity + `weightUnit` to `partNumberMaterialConfig` |
| `Dashboard.js` | Add CAD menu entries + Process "Temps de Coupe / PN" |
| `App.js` | Add routes for new components (3 routes) |

### Database
```sql
-- New table (auto-created by JPA)
-- PieceDetail with pieceName as PK

-- Alter existing tables
ALTER TABLE PartNumberMaterialConfig ADD weight_unit FLOAT NULL;
ALTER TABLE PartNumberInfo ADD weight FLOAT NULL;
ALTER TABLE PartNumberInfo ADD totalPerimetre FLOAT NULL;
ALTER TABLE PartNumberInfo ADD tempsDeCoupe FLOAT NULL;
```

---

## Implementation Order

1. **DB + Entity**: Create `PieceDetail` entity → let JPA auto-create table
2. **PartNumberMaterialConfig**: Add `weightUnit` field + run ALTER
3. **PartNumberInfo**: Add `weight`, `totalPerimetre`, `tempsDeCoupe` fields + run ALTERs
4. **Backend Services (CAD)**: `PieceDetailService` (CSV import) → `PartNumberWeightCalculationService` → save weight to `PartNumberInfo`
5. **Backend Controller (CAD)**: `PieceDetailController` with CSV import + weight calculation endpoints
6. **Backend Services (Process)**: `PartNumberCuttingTimeService` → reads CuttingPlan placements → computes perimeter % → attributes cutting time → saves `tempsDeCoupe` + `totalPerimetre` to `PartNumberInfo`
7. **Backend Controller (Process)**: `PartNumberCuttingTimeController` with calculation + results endpoints
8. **Frontend metadata.js**: Add `pieceDetail` entity + update `partNumberMaterialConfig` with `weightUnit`
9. **Frontend Components (CAD)**: Import CSV page → Weight Calculation page
10. **Frontend Components (Process)**: Cutting Time per PartNumber page (Period + Project → results table)
11. **Dashboard + Routes**: Wire all 3 new pages + menu entries
12. **Testing**:
    - Import sample CSV → configure weightUnit → calculate weights → verify `PartNumberInfo.weight`
    - Select project + period → verify perimeter % and cutting time → verify `PartNumberInfo.tempsDeCoupe`
