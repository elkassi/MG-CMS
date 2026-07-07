# CuttingPlanForm - Technical Documentation

## Overview

The `CuttingPlanForm.js` is a complex React component (~5022 lines) responsible for managing cutting plans in the MG-CMS manufacturing system. It handles the complete lifecycle of cutting plan creation, modification, validation, and submission to the CMS (Cutting Management System).

**Path**: `src/main/js/components/Layout/CuttingPlanForm.js`

---

## Component Structure

### 1. State Management

The component uses extensive local state with numerous properties:

```javascript
state = {
  // Core Data
  modalObj: {},                    // Main cutting plan object
  optionsList: {                   // Dropdown options
    projet: [],
    version: [],
    partNumberBoom: [],
    machineType: [],
    zone: [],
    wo: []
  },
  partNumberMaterialConfigs: {},   // Material configurations (machine, category, margins)
  
  // Verification Data
  ctcData: {},                     // CTC file data per part number
  digits: [],                      // Pattern list for drill verification
  placementsInfo: {},              // Placement details from CMS
  drillEmpArr: [],                 // Drill information
  
  // UI State
  loading: false,
  loadingTest: false,
  loadingConfirmer: false,
  error: null,
  hideColumn: false,
  
  // History
  histories: [],
  selectedHistory: null,
  
  // Modals
  showDrillModal: false,
  drillModalData: { ctcData: null, drillEmpData: null, partNumbers: null },
  importModal: null,
  
  // Similar Plans
  cpSimilarList: [],
  searchSimilarCuttingPlan: false
}
```

### 2. Main Object Structure (modalObj)

The cutting plan object has the following structure:

```javascript
modalObj = {
  id: Long,
  cmsId: Long,                          // CMS system ID
  projet: String,                        // Project name
  version: String,                       // Project version
  description: String,                   // Plan description
  definition: String,                    // Plan definition
  enabled: Boolean,                      // Active status
  foam: Boolean,                         // Is foam material
  type: String,                          // "Normal" or "Special Plan"
  
  // Part Numbers
  cuttingPlanPartNumbers: [{
    partNumber: String,
    description: String,
    item: String,                        // Kit textile
    quantity: Integer,
    quantityPer: Integer
  }],
  
  // Materials
  cuttingPlanMaterials: [{
    partNumberMaterial: String,          // Reference tissu
    description: String,
    partNumbers: String,                 // Comma-separated PNs
    tauxScrap: Double,
    rotation: String,
    plaque: Double,                      // Fixed length for plaque materials
    vitesse: Double,                     // Cutting speed
    matelassageEndroit: Boolean,
    qadUsage: Double,                    // QAD usage calculation
    cuttingPlanMaterialPlacement: [{
      groupPlacement: Integer,
      activated: Boolean,
      placement: String,                 // Placement file name
      machine: String,                   // Machine type
      drill: String,                     // "drill1,drill2" format
      category: String,
      laize: Double,                     // Width
      longueur: Double,                  // Length
      longueurMatelas: Double,           // Mattress length
      nbrCouche: Integer,                // Number of layers
      config: String,                    // Plies configuration
      maxPlie: Integer,
      maxPlieDrill: Integer,
      maxDrill: Integer,
      espaceRelarge: String,             // Buffer space
      rotation: String,
      verifEndroit: String,
      perimetre: Double,
      tempsDeCoupe: Double,              // Cutting time
      partNumbers: String                // PN:Qty format
    }]
  }],
  
  // Rapport Data (from Lectra reports)
  cuttingPlanRapportPlacements: [],
  cuttingPlanRapportModels: [],
  cuttingPlanRapportDrills: [],
  
  // Verification Results
  verification1: String,
  verification2: String,
  verification3: String,
  verification4: String,
  verification5: String,
  verification6: String,
  verification7: String,
  verification7Plaque: String,
  
  // Metadata
  alertMessages: String,
  commentaire: String
}
```

---

## Data Flow & API Endpoints

### Input Data Loading

| API Endpoint | Method | Purpose |
|--------------|--------|---------|
| `/api/projet/list` | GET | Load project list |
| `/api/projetVersion` | GET | Load project versions |
| `/api/partNumberBoom/list` | GET | Load BOM data for part numbers |
| `/api/partNumberMaterialConfig/pns/{pns}` | GET | Load material configurations |
| `/api/zone/list` | GET | Load zone options |
| `/api/machineType` | GET | Load machine type options |
| `/api/cuttingSpeed/list` | GET | Load cutting speed configurations |
| `/api/cuttingPlan/{id}` | GET | Load existing cutting plan |

### CTC & Drill Verification

| API Endpoint | Method | Purpose |
|--------------|--------|---------|
| `/api/ctcFiles/pn/{partNumber}` | GET | Load CTC data for part number |
| `/api/drillEmp/list` | POST | Load drill information for patterns |
| `/api/plt/exist` | POST | Check PLT file existence |

### CMS Integration

| API Endpoint | Method | Purpose |
|--------------|--------|---------|
| `/api/cuttingPlan/cms/searchPnMaterial` | GET | Search CMS for existing placements |
| `/api/cuttingPlan/cms` | POST | Submit plan to CMS |
| `/api/cuttingPlan/alert` | POST | Send alert emails for errors |

### Plan Management

| API Endpoint | Method | Purpose |
|--------------|--------|---------|
| `/api/cuttingPlan` | POST | Save cutting plan |
| `/api/cuttingPlan/enable/{id}` | POST | Enable cutting plan |
| `/api/cuttingPlan/disable/{id}` | POST | Disable cutting plan |
| `/api/cuttingPlanHistory/cuttingPlan/{id}` | GET | Load plan history |

---

## Verification System

The component has **8 verification checks** that must pass before saving:

### Verification 1: PNs vs Placements
- Compares requested part numbers with placement models
- Validates quantities match

### Verification 2: Laize (Width)
- Checks material width against placement requirements
- Uses `partNumberMaterialConfigs` for reference

### Verification 3: Drills
- Validates drill configurations
- Compares with CTC data and drill reports
- Checks drill1/drill2 requirements

### Verification 4: Placement Rules
- Length within configured intervals
- Buffer/Espace relarge (ESP00, etc.)
- Rotation settings
- Machine-specific naming (IP6, LSR, DXF suffixes)
- **Enhanced 0BF Validation**: For materials with `validated0BF = true`:
  - Placement name must contain `-0BF` **OR** `-MBF` suffix (for Lectra machines)
  - Espace relarge must be `ESP00`

### Verification 5: Material vs Placement Definition
- Verifies `numeroDefPlct` matches `partNumberMaterial`

### Verification 6: Code Prefix
- Validates placement names start with project+version code

### Verification 7: Plaque Length
- For plaque materials, compares length tolerances (5%)

### Verification 7+ (Quantity)
- Validates quantities per model
- Checks for duplicate part numbers
- Validates placement completeness

---

## Render Methods

### Main Sections

| Method | Description |
|--------|-------------|
| `renderForm()` | Main form with project selection, part numbers table |
| `renderReftissus()` | Materials and placements table |
| `renderRapportPlacement()` | Lectra placement report parsing |
| `renderRapportModel()` | Lectra model report parsing |
| `renderRapportDrill()` | Drill report parsing |
| `renderConsommation()` | Material consumption calculations |
| `renderHistorique()` | Plan history display |
| `renderTest()` | Verification results display |
| `renderSimilarCuttingPlan()` | Similar plans lookup |

### Modals

| Method | Description |
|--------|-------------|
| `renderReftissuModal()` | Material configuration display |
| `renderConfirmModal()` | CTC validation before save |
| `renderDetailModal()` | Detailed placement view |
| `renderImportModal()` | Import from CMS modal |
| `renderDrillModal()` | Drill overview modal |

---

## Key Business Logic

### QAD Usage Calculation
```javascript
// 1.03 multiplier for non-plaque materials
qadUsage = !cpmElem.plaque ? total * 1.03 : total
```

### Margin Calculation (Enhanced)
The `getMarge` function calculates appropriate margins using `ReftissuMargin` configuration.

```javascript
getMarge(longueur, nbrCouche, partNumberMaterial) {
    // Get material configuration
    const config = this.state.partNumberMaterialConfigs[partNumberMaterial];
    if (!config || !config.reftissuMargins) return 0;
    
    // Find margin interval matching the length
    const marginConfig = config.reftissuMargins.find(rm => 
        longueur >= rm.minLength && longueur <= rm.maxLength
    );
    
    if (!marginConfig) return 0;
    
    // Parse pliesConfig: "10;margin1|20;margin2|50;margin3"
    // Each segment: maxLayers;marginValue
    const pliesConfigs = marginConfig.pliesConfig.split('|');
    
    for (let pc of pliesConfigs) {
        const [maxLayers, margin] = pc.split(';');
        if (nbrCouche <= parseInt(maxLayers)) {
            return parseFloat(margin);
        }
    }
    
    return 0;
}
```

**Example Usage**:
```javascript
// Given reftissuMargins configuration:
[
    {
        minLength: 0,
        maxLength: 1000,
        pliesConfig: "10;50|20;75|50;100"
        // Means: ≤10 layers → 50mm margin
        //        ≤20 layers → 75mm margin
        //        ≤50 layers → 100mm margin
    },
    {
        minLength: 1001,
        maxLength: 2000,
        pliesConfig: "10;75|20;100|50;150"
    }
]

// Calculate margin for length=800, layers=15
const margin = getMarge(800, 15, "MATERIAL-001");
// Returns: 75 (matches first interval, 10 < 15 ≤ 20)

// Calculate margin for length=1500, layers=8
const margin = getMarge(1500, 8, "MATERIAL-001");
// Returns: 75 (matches second interval, 8 ≤ 10)
```

**Key Features**:
- Dynamic margin based on length intervals
- Layer-specific margin values
- Fallback to 0 if no configuration found
- Flexible configuration via `ReftissuMargin` entity

### Machine Type Conversion
```javascript
convertToMachine(machineId) {
  // 1: Lectra, 2: Gerber, 3: DIE, 4: LASER-LSR, 5: Lectra IP6, 6: LASER-DXF
}
```

### Layer Configuration
```javascript
// PliesConfig format: "10;config1|20;config2|50;config3"
// Finds appropriate config based on nbrCouche
```

---

## Recent Enhancements (v3.59)

### 1. Enhanced 0BF Validation
**Update**: Verification 4 now accepts both `-0BF` and `-MBF` suffixes for validated 0BF materials.

#### Implementation Details
```javascript
// CuttingPlanForm.js - Verification 4
if (configObj.validated0BF === true && cpmp.machine === "Lectra"
    && !cpmp.placement.includes("-0BF") && !cpmp.placement.includes("-MBF")) {
    error.push(cpmp.placement + ": validated 0BF: placement name must contain -0BF or -MBF")
}

// Additional check for espace relarge
if (configObj.validated0BF === true && cpmp.espaceRelarge !== "ESP00") {
    error.push(cpmp.placement + ": validated 0BF: buffer space must be ESP00")
}
```

#### Business Rules
- **Applies to**: Materials with `validated0BF = true` flag
- **Machine Type**: Lectra machines only
- **Naming Convention**: Placement name must include:
  - `-0BF` suffix (zero buffer/margin) **OR**
  - `-MBF` suffix (minimal buffer/margin)
- **Buffer Space**: Must use `ESP00` (zero buffer space)

#### Examples
| Placement Name | Valid? | Reason |
|----------------|--------|--------|
| `ABC-V1-001-0BF` | ✅ Yes | Contains `-0BF` suffix |
| `ABC-V1-002-MBF` | ✅ Yes | Contains `-MBF` suffix |
| `ABC-V1-003-LECTRA` | ❌ No | Missing required suffix |
| `ABC-V1-004-0BF` (ESP10) | ❌ No | Wrong buffer space |

---

### 2. ReftissuMargin Integration in getMarge
**Update**: Margin calculation now uses configurable `ReftissuMargin` intervals instead of hardcoded values.

#### Previous Behavior
- Fixed margins based on simple conditions
- No length-based variation
- Limited flexibility

#### New Behavior
- Dynamic margins based on length intervals
- Layer-count specific configurations
- Fully configurable via database

#### Configuration Structure
```java
@Entity
public class ReftissuMargin {
    private Long id;
    private Double minLength;        // Minimum length for this interval (mm)
    private Double maxLength;        // Maximum length for this interval (mm)
    private String pliesConfig;      // Format: "layers1;margin1|layers2;margin2|..."
    
    @ManyToOne
    private PartNumberMaterialConfig partNumberMaterialConfig;
}
```

#### Configuration Examples

**Example 1: Standard Material**
```json
{
    "partNumberMaterial": "FABRIC-001",
    "reftissuMargins": [
        {
            "minLength": 0,
            "maxLength": 1000,
            "pliesConfig": "10;50|20;75|50;100"
        },
        {
            "minLength": 1001,
            "maxLength": 2000,
            "pliesConfig": "10;75|20;100|50;150"
        }
    ]
}
```

**Example 2: High-Precision Material**
```json
{
    "partNumberMaterial": "LEATHER-PREMIUM",
    "reftissuMargins": [
        {
            "minLength": 0,
            "maxLength": 500,
            "pliesConfig": "5;30|10;40|20;50"
        },
        {
            "minLength": 501,
            "maxLength": 1500,
            "pliesConfig": "5;40|10;60|20;80"
        }
    ]
}
```

#### Usage in Calculation
```javascript
// Material: FABRIC-001
// Length: 800mm
// Layers: 15

Step 1: Find matching length interval
- 800 is between 0 and 1000 ✓
- Use pliesConfig: "10;50|20;75|50;100"

Step 2: Find matching layer configuration
- 15 layers > 10, so skip "10;50"
- 15 layers ≤ 20, so use "20;75" ✓

Result: Margin = 75mm
```

#### Benefits
1. **Flexibility**: Easy to adjust margins without code changes
2. **Precision**: Different margins for different lengths and layer counts
3. **Material-Specific**: Each material can have unique margin rules
4. **Scalability**: Add new intervals without modifying code
5. **Auditability**: Configuration changes tracked in database

#### Migration Notes
- Existing placements remain valid
- New calculations use ReftissuMargin if configured
- Falls back to 0 if no configuration exists
- Recommend reviewing and configuring margins for all active materials

---

### 3. Machine-Specific Margin Selection (February 2026)
**Update**: The margin lookup now supports machine-specific margins. Each `ReftissuMargin` can optionally specify a `machine` field.

#### How it Works
When computing `pliesConfigMarge` for a placement:
1. Get the placement's `machine` value
2. If `machine` is set, first try to find a `ReftissuMargin` matching both the length range **AND** the machine
3. If no machine-specific margin is found (or machine is null), fall back to a margin where `machine` is null/empty
4. This applies in both pliesConfigMarge assignment locations (~line 2283 and ~line 2426 in CuttingPlanForm.js)

#### Frontend Logic (CuttingPlanForm.js)
```javascript
let machine = arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].machine;
let margesConfig = null;
if (machine) {
    margesConfig = ...reftissuMargins.find(e =>
        e.longueurMin <= lg && e.longueurMax >= lg && e.machine === machine
    );
}
if (!margesConfig) {
    margesConfig = ...reftissuMargins.find(e =>
        e.longueurMin <= lg && e.longueurMax >= lg && !e.machine
    );
}
```

#### ReftissuMargin Entity Update
- Added `machine` (String, nullable) field to `ReftissuMargin`
- When `machine` is null → default margin for all machines
- When `machine` is set → margin specific to that machine type

#### Reftissu Margin Display in Modal
- Added "Machine" column to the renderReftissuModal margin table
- Shows `elem.machine || '-'` for each margin entry

### 4. PartNumberMaterialConfigForm - Type Machine in Seuil
**Update**: Added "Type Machine" Select column as the first column in the `renderSeuil()` method of `PartNumberMaterialConfigForm.js`.

- Uses the same Select pattern as `renderMachineForm()` with `machineType` options
- `isClearable={true}` (machine is optional for margins)
- Stores value on `arrMargin[ind].machine`

### 5. ChangementReftissu - Per-Row Convert Button
**Update**: Added per-row "Convert" button in `ChangementReftissu.js` renderData table.
- Each row in the results table has an "Action" column with a "Convert" button
- Clicking Convert on a single row executes the conversion for that specific row only

### 6. MachineTypeSwap - Complete Redesign (February 2026)
**Update**: `MachineTypeSwap.js` completely rewritten with new grouped tree structure.

#### New Features
- **Simplified Search**: Only Part Number Material (required) + Cutting Plan ID (optional)
- **Hierarchical Tree View**: Results grouped by Cutting Plan → Group Placement → rows
- **Sorted Display**: cuttingPlan asc, groupPlacement asc, activated desc
- **Machine Selector**: Select target machine from distinct machines found in results
- **Color-Coded Results**:
  - **Green**: Successfully swapped to target machine
  - **Yellow**: Already has target machine active
  - **Red**: Target machine not found in group
- **Results Modal**: Summary with changed/already/notfound/error counts

### 7. Reftissu Data Entities (February 2026)
**Update**: Created lightweight Data entities for read-only access to Reftissu tables.

| Entity | Table | Fields |
|--------|-------|--------|
| `ReftissuMachineData` | ReftissuMachine | partNumberMaterialConfig, machineType, maxPlie, maxPlieDrill, maxDrill, defaultValue, pliesConfig |
| `ReftissuCategoryData` | ReftissuCategory | partNumberMaterialConfig, category, description, borneMin, borneMax, defaultValue |
| `ReftissuMarginData` | ReftissuMargin | partNumberMaterialConfig, intervalId, longueurMin, longueurMax, machine, pliesConfig |

- Located in `domain/CuttingPlan/data/`
- Use `@Column(name = "partNumberMaterialConfig_partNumberMaterial")` String instead of `@ManyToOne`
- Follow same pattern as `CuttingPlanMaterialData.java`

---

## Backend Components

### Controller
**Path**: `src/main/java/.../controller/CuttingPlan/CuttingPlanController.java`
- ~1344 lines
- Handles CRUD operations, CMS synchronization, enable/disable

### Service
**Path**: `src/main/java/.../services/CuttingPlan/CuttingPlanService.java`
- ~370 lines
- Business logic, filtering, pagination

### Related Entities
- `CuttingPlan.java`
- `CuttingPlanPartNumber.java`
- `CuttingPlanMaterial.java`
- `CuttingPlanMaterialPlacement.java`
- `CuttingPlanRapportPlacement.java`
- `CuttingPlanRapportModel.java`
- `CuttingPlanRapportDrill.java`
- `CuttingPlanHistory.java`

---

## Recommendations

### 🔴 Critical Issues

#### 1. **File Size - Needs Decomposition**
The component is **5022 lines** which is extremely large. This makes it:
- Difficult to maintain
- Hard to test
- Slow to load and parse

**Recommendation**: Split into multiple components:
```
CuttingPlanForm/
├── index.js                      (Main container, ~300 lines)
├── CuttingPlanHeader.js          (Title, metadata)
├── PartNumbersTable.js           (Part numbers section)
├── MaterialsTable.js             (Materials & placements)
├── RapportSection.js             (Lectra reports parsing)
├── VerificationPanel.js          (All 8 verifications)
├── HistorySection.js             (Plan history)
├── modals/
│   ├── ConfirmModal.js
│   ├── DrillModal.js
│   ├── ImportModal.js
│   └── DetailModal.js
├── hooks/
│   ├── useCuttingPlanData.js     (Data loading)
│   ├── useVerification.js        (Verification logic)
│   └── useMaterialConfig.js      (Material configs)
└── utils/
    ├── calculations.js           (getMarge, qadUsage, etc.)
    └── validations.js            (Input validations)
```

#### 2. **Class Component - Convert to Functional**
Using React class components is outdated. 

**Recommendation**: Convert to functional component with hooks:
- `useState` for state management
- `useEffect` for lifecycle
- `useCallback` for memoized callbacks
- `useMemo` for expensive calculations
- Custom hooks for reusable logic

#### 3. **No TypeScript**
Complex objects like `modalObj` need type safety.

**Recommendation**: Add TypeScript interfaces:
```typescript
interface CuttingPlan {
  id: number;
  projet: string;
  version: string;
  cuttingPlanPartNumbers: CuttingPlanPartNumber[];
  cuttingPlanMaterials: CuttingPlanMaterial[];
  // ...
}
```

### 🟠 High Priority

#### 4. **Inline Styles**
Many inline styles throughout the component.

**Recommendation**: Move to SCSS file:
```scss
.verification-header {
  padding: 8px;
  border-radius: 19px;
  margin: 5px;
  
  &.loading { background-color: grey; }
  &.good { background-color: rgb(206, 255, 179); }
  &.bad { background-color: rgb(255, 181, 181); }
}
```

#### 5. **Magic Numbers & Strings**
Hardcoded values like:
- `1.03` multiplier
- `1.0` kg tolerance
- Machine type mappings
- Color values

**Recommendation**: Create constants file:
```javascript
// constants.js
export const SCRAP_MULTIPLIER = 1.03;
export const MACHINE_TYPES = {
  '1': 'Lectra',
  '2': 'Gerber',
  '3': 'DIE',
  '4': 'LASER-LSR',
  '5': 'Lectra IP6',
  '6': 'LASER-DXF'
};
export const VALIDATION_COLORS = {
  SUCCESS: '#9dff8c',
  ERROR: '#ffa3a3',
  WARNING: '#ffffc0'
};
```

#### 6. **Error Handling**
Errors are caught but often just logged or stored in state.

**Recommendation**: 
- Implement centralized error handling
- Add user-friendly error messages
- Implement retry logic for API calls
- Add toast notifications for errors

### 🟡 Medium Priority

#### 7. **API Calls Management**
Multiple scattered axios calls without centralization.

**Recommendation**: Create service layer:
```javascript
// services/cuttingPlanApi.js
export const cuttingPlanApi = {
  load: (id) => axios.get(`/api/cuttingPlan/${id}`),
  save: (data) => axios.post('/api/cuttingPlan', data),
  saveToCms: (data) => axios.post('/api/cuttingPlan/cms', data),
  enable: (id) => axios.post(`/api/cuttingPlan/enable/${id}`),
  disable: (id) => axios.post(`/api/cuttingPlan/disable/${id}`),
  // ...
};
```

#### 8. **Complex Nested Loops**
Multiple nested loops for data processing are hard to follow.

**Recommendation**: 
- Extract to named functions
- Use array methods (map, filter, reduce)
- Consider functional programming patterns

#### 9. **Duplicate Code**
Similar logic repeated in multiple verification methods.

**Recommendation**: Create shared validation utilities:
```javascript
// utils/validations.js
export const validatePlacement = (placement, config) => {
  const errors = [];
  if (!placement.name) errors.push('Placement name required');
  if (!placement.longueur) errors.push('Length required');
  // ...
  return errors;
};
```

#### 10. **State Updates**
Complex nested state updates with spread operators.

**Recommendation**: 
- Use Immer for immutable updates
- Consider using useReducer for complex state
```javascript
import produce from 'immer';

const updatePlacement = (materialIndex, placementIndex, updates) => {
  setState(produce(draft => {
    Object.assign(
      draft.modalObj.cuttingPlanMaterials[materialIndex]
           .cuttingPlanMaterialPlacement[placementIndex],
      updates
    );
  }));
};
```

### 🟢 Improvements

#### 11. **Loading States**
Multiple loading flags are scattered.

**Recommendation**: Centralize loading states:
```javascript
const [loadingStates, setLoadingStates] = useState({
  initial: true,
  saving: false,
  verifying: false,
  searchingCMS: false
});
```

#### 12. **Form Validation**
Implement proper form validation library.

**Recommendation**: Use react-hook-form or Formik:
```javascript
import { useForm } from 'react-hook-form';
const { register, handleSubmit, errors } = useForm();
```

#### 13. **Performance**
Large tables and complex calculations may cause lag.

**Recommendation**:
- Implement virtualization for large tables (react-window)
- Memoize expensive calculations
- Use React.memo for child components
- Debounce search inputs

#### 14. **Testing**
Component is likely untestable due to size.

**Recommendation**: After decomposition:
- Unit tests for utility functions
- Integration tests for API calls
- Component tests for each sub-component

---

## Priority Implementation Order

1. **Extract utility functions** (calculations, validations) - Low risk
2. **Create API service layer** - Low risk, immediate benefit
3. **Extract modal components** - Medium risk
4. **Split main sections into components** - Medium risk
5. **Convert to functional components** - High effort
6. **Add TypeScript** - High effort, highest long-term benefit

---

## Summary

| Metric | Current | Recommended |
|--------|---------|-------------|
| File Size | 5022 lines | <500 lines per file |
| Components | 1 | 10-15 |
| API Calls | Scattered | Centralized service |
| State Management | Complex local | Hooks + Reducer |
| Type Safety | None | TypeScript |
| Tests | None | Unit + Integration |
