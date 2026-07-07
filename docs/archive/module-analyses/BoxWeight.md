# Box Weight Control Section

## Overview

The Box Weight Control section is a quality assurance feature in the MG-CMS application that manages the weight verification process for shipped boxes. It implements an enhanced two-phase workflow with optional weight estimation:

1. **Weight Filling (Enregistrement Poids)**: Recording the sent weight of boxes with optional estimation
2. **Weight Verification (Vérification Poids)**: Validating received weight against sent weight and estimated weight

## Recent Updates (v3.59)

### New Features
- **Weight Estimation**: Calculate expected box weight based on part numbers and quantities
- **Quantity Tracking**: Record number of pieces in each box
- **Empty Box Weight Configuration**: Configurable empty box weights by box type
- **Part Number Weight Database**: Unit weights for accurate estimation

---

## Menu Structure

Located in **CAD Section** of the Dashboard:
- **Enregistrement Poids** → `/boxWeightFilling`
- **Vérification Poids** → `/boxWeightVerifying`
- **Historique Poids** → `/boxWeight`

---

## Frontend Components

### 1. BoxWeightFilling.js
**Path**: `src/main/js/components/Layout/BoxWeightFilling.js`  
**Lines**: ~265 lines

#### Purpose
Entry form for recording box weight when sending boxes.

#### Features
- **Box Type Selection**: Dropdown to select type of box (loaded from `/api/cadConfig/boxType`)
- **Box ID Input**: Unique identifier for the box
- **Sent Weight Input**: Weight in kg with validation
- **User's Entries Table**: Shows entries created by current user with ability to delete last unverified entry

#### State Structure
```javascript
{
  list: [],           // User's own entries
  boxType: null,      // Selected box type
  boxId: "",          // Box identifier
  sentWeight: "",     // Weight being sent
  error: null,        // Error message
  success: null       // Success message
}
```

#### API Calls
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cadConfig/boxType` | Load box type options |
| GET | `/api/boxWeight/mySent` | Get current user's entries |
| POST | `/api/boxWeight/fill` | Submit new weight entry |
| DELETE | `/api/boxWeight/removeLast` | Delete last unverified entry |

#### Key UI Elements
- Form with box type dropdown, box ID input, and weight input
- Submit button with loading state
- Table showing user's entries with delete button for last unverified item

---

### 2. BoxWeightVerifying.js
**Path**: `src/main/js/components/Layout/BoxWeightVerifying.js`  
**Lines**: ~389 lines

#### Purpose
Interface for verifying received box weights against sent weights.

#### Features
- **Search by Box ID**: Quick lookup to find boxes for verification
- **Verification Panel**: Shows sent weight and allows entering received weight
- **Difference Calculation**: Automatically calculates weight difference
- **Pending Entries Tab**: Shows all boxes awaiting verification
- **All Entries Tab**: Complete history of all entries

#### State Structure
```javascript
{
  list: [],                    // Verified/all entries list
  notVerifiedList: [],         // Pending verification entries
  search: "",                  // Search input
  selectedEntry: null,         // Entry being verified
  receivedWeight: "",          // Weight input for verification
  activeTab: "pending",        // Current tab
  filters: { boxType: null, boxId: "", startDate: null, endDate: null }
}
```

#### API Calls
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/boxWeight/notVerified` | Get all unverified entries |
| GET | `/api/boxWeight/byBoxIdNotVerified/{id}` | Search by box ID |
| POST | `/api/boxWeight/verify/{id}` | Submit verification |
| GET | `/api/boxWeight/all` | Get all entries (with filters) |

#### Validation Logic
- Weight difference is calculated: `|receivedWeight - sentWeight|`
- Visual indicator: 
  - ≤ 1.0 kg → **Green** (validated = true)
  - > 1.0 kg → **Red** (validated = false)

---

### 3. Styling (BoxWeight.scss)
**Path**: `src/main/js/components/Layout/BoxWeight.scss`  
**Lines**: ~225 lines

#### Design Theme
Uses Lear corporate colors:
- **Primary Red**: `#bf3030`
- **Background**: White with grey accents
- **Text**: Black/dark grey

#### Key Classes
```scss
.box-weight-container        // Main container
.box-weight-form             // Form styling
.box-weight-table            // Table styling with sticky header
.verification-panel          // Verification section styling
.difference-indicator        // Weight difference display
  .valid                     // Green for ≤ 1kg
  .invalid                   // Red for > 1kg
```

---

## Backend Components

### 1. BoxWeight.java (Entity)
**Path**: `src/main/java/com/lear/MGCMS/domain/BoxWeight.java`  
**Lines**: ~135 lines

#### Entity Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key (auto-generated) |
| `boxType` | String | Type of box |
| `boxId` | String | Unique box identifier |
| `sentWeight` | Double | Weight when sent |
| `sentBy` | String | User who sent |
| `sentAt` | LocalDateTime | Timestamp of sending |
| `receivedWeight` | Double | Weight when received |
| `receivedBy` | String | User who verified |
| `receivedAt` | LocalDateTime | Timestamp of verification |
| `validated` | Boolean | Validation result |
| `quantity` | Integer | **NEW**: Number of pieces in box |
| `estimatedWeight` | Double | **NEW**: Calculated estimated weight |

#### JPA Annotations
- `@Entity`, `@Table(name = "box_weight")`
- `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`

---

### 2. BoxWeightRepository.java
**Path**: `src/main/java/com/lear/MGCMS/repositories/BoxWeightRepository.java`  
**Lines**: ~40 lines

#### Query Methods
```java
// Find by box ID
List<BoxWeight> findByBoxId(String boxId);

// Find unverified entries
List<BoxWeight> findByReceivedWeightIsNull();

// Find unverified by box ID
List<BoxWeight> findByBoxIdAndReceivedWeightIsNull(String boxId);

// Find user's sent entries
List<BoxWeight> findBySentByOrderBySentAtDesc(String username);

// Find last entry by user (for deletion)
Optional<BoxWeight> findFirstBySentByAndReceivedWeightIsNullOrderBySentAtDesc(String username);
```

---

### 3. BoxWeightService.java
**Path**: `src/main/java/com/lear/MGCMS/services/BoxWeightService.java`  
**Lines**: ~119 lines

#### Key Methods

##### `createBoxWeight(BoxWeight boxWeight, String username)`
- Sets `sentBy` to current user
- Sets `sentAt` to current timestamp
- Saves and returns the entity

##### `verifyBoxWeight(Long id, Double receivedWeight, String username)`
- Retrieves box weight by ID
- Sets `receivedWeight`, `receivedBy`, `receivedAt`
- **Calculates validation**: `validated = Math.abs(receivedWeight - sentWeight) <= 1.0`
- Returns updated entity

##### `removeLastBySentBy(String username)`
- Finds the last unverified entry by user
- Deletes it if found
- Returns boolean success status

##### `findNotVerified()`
- Returns all entries where `receivedWeight` is null

##### `findMySent(String username)`
- Returns all entries sent by current user

---

### 4. BoxWeightController.java
**Path**: `src/main/java/com/lear/MGCMS/controller/BoxWeightController.java`  
**Lines**: ~167 lines

#### REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/boxWeight/list` | Get paginated list |
| GET | `/api/boxWeight/all` | Get all entries with filters |
| GET | `/api/boxWeight/byBoxId/{id}` | Find by box ID |
| GET | `/api/boxWeight/byBoxIdNotVerified/{id}` | Find unverified by box ID |
| GET | `/api/boxWeight/mySent` | Get current user's entries |
| GET | `/api/boxWeight/notVerified` | Get all pending verifications |
| POST | `/api/boxWeight/fill` | Create new entry |
| POST | `/api/boxWeight/verify/{id}` | Verify an entry |
| DELETE | `/api/boxWeight/removeLast` | Remove user's last entry |

---

## Business Logic Flow

### Phase 1: Weight Filling (Sending)
```
1. User selects box type from dropdown
2. User enters box ID (unique identifier)
3. User enters sent weight in kg
4. System validates input
5. POST /api/boxWeight/fill with { boxType, boxId, sentWeight }
6. Backend sets sentBy=currentUser, sentAt=now()
7. Entry saved to database
8. User sees entry in "My Entries" table
```

### Phase 2: Weight Verification (Receiving)
```
1. Verifier searches by box ID or views pending list
2. Selects entry to verify
3. Enters received weight
4. System calculates difference: |received - sent|
5. Visual indicator shows:
   - Green (≤1kg): Will be marked as validated
   - Red (>1kg): Will be marked as NOT validated
6. POST /api/boxWeight/verify/{id} with receivedWeight
7. Backend calculates: validated = (difference <= 1.0)
8. Entry updated with receivedWeight, receivedBy, receivedAt, validated
```

---

## Validation Rules

### Weight Difference Tolerance
- **Threshold**: 1.0 kg
- **Rule**: `|receivedWeight - sentWeight| <= 1.0`
- **Result**:
  - `validated = true` → Weight is within acceptable range
  - `validated = false` → Weight discrepancy detected

### Input Validation
- Box ID: Required, non-empty string
- Weight: Positive number, formatted to 2 decimal places
- Box Type: Required selection from dropdown

---

## Database Schema

```sql
CREATE TABLE box_weight (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    box_type VARCHAR(255),
    box_id VARCHAR(255) NOT NULL,
    sent_weight DOUBLE NOT NULL,
    sent_by VARCHAR(255) NOT NULL,
    sent_at DATETIME NOT NULL,
    received_weight DOUBLE,
    received_by VARCHAR(255),
    received_at DATETIME,
    validated BOOLEAN
);
```

---

## Weight Estimation System

### Overview
The weight estimation feature helps predict box weight based on:
- Part number unit weights
- Quantity of pieces
- Empty box weight configuration

### Components

#### 1. PartNumberWeight Entity
**Purpose**: Store unit weight for each part number.

**Structure**:
```java
@Entity
@Table(name = "PartNumberWeight")
public class PartNumberWeight {
    private Long id;
    private String partnumber;      // Part number identifier
    private Double weightUnit;      // Weight per unit (kg)
}
```

**API Endpoints**:
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/partNumberWeight/list` | Get all weights |
| GET | `/api/partNumberWeight/byPartnumber/{pn}` | Get weight for part number |
| POST | `/api/partNumberWeight` | Create/update weight |
| POST | `/api/partNumberWeight/import` | Bulk import from Excel |

**Excel Import Format**:
```
Column A: Part Number (string)
Column B: Weight Unit (numeric, kg)
```

---

#### 2. BoxTypeConfig Entity
**Purpose**: Configure empty box weights by type.

**Structure**:
```java
@Entity
@Table(name = "BoxTypeConfig")
public class BoxTypeConfig {
    private Long id;
    private String boxType;           // e.g., "gray", "black"
    private Double emptyBoxWeight;    // Empty box weight (kg)
}
```

**API Endpoints**:
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/boxTypeConfig/list` | Get all configurations |
| GET | `/api/boxTypeConfig/byBoxType/{type}` | Get config by type |
| POST | `/api/boxTypeConfig` | Create/update config |

---

### Estimation Logic

#### Formula
```
estimatedWeight = (quantity × partNumberUnitWeight) + emptyBoxWeight
```

#### Example Calculation
```
Given:
- Part Number: "PN-12345"
- Quantity: 100 pieces
- Unit Weight: 0.25 kg (from PartNumberWeight)
- Box Type: "gray"
- Empty Box Weight: 2.5 kg (from BoxTypeConfig)

Calculation:
estimatedWeight = (100 × 0.25) + 2.5
estimatedWeight = 25 + 2.5
estimatedWeight = 27.5 kg
```

#### Implementation (Service Layer)
```java
public BoxWeight createWithEstimation(BoxWeight boxWeight, String partNumber) {
    // Get part number weight
    Optional<PartNumberWeight> pnWeight = 
        partNumberWeightRepository.findByPartnumber(partNumber);
    
    // Get empty box weight
    Optional<BoxTypeConfig> boxConfig = 
        boxTypeConfigRepository.findByBoxType(boxWeight.getBoxType());
    
    if (pnWeight.isPresent() && boxConfig.isPresent() 
        && boxWeight.getQuantity() != null) {
        
        double estimated = (boxWeight.getQuantity() * pnWeight.get().getWeightUnit()) 
                         + boxConfig.get().getEmptyBoxWeight();
        
        boxWeight.setEstimatedWeight(estimated);
    }
    
    return boxWeightRepository.save(boxWeight);
}
```

---

### Enhanced Validation Workflow

#### Multi-Level Validation
The system now supports three-way validation:

1. **Sent vs. Received Weight** (Original)
   - Threshold: 1.0 kg
   - Rule: `|receivedWeight - sentWeight| ≤ 1.0`

2. **Estimated vs. Sent Weight** (New - Optional)
   - Threshold: 5% variance
   - Rule: `|sentWeight - estimatedWeight| / estimatedWeight ≤ 0.05`

3. **Estimated vs. Received Weight** (New - Optional)
   - Threshold: 5% variance
   - Rule: `|receivedWeight - estimatedWeight| / estimatedWeight ≤ 0.05`

#### Validation Status Indicators

| Status | Condition | Color | Action |
|--------|-----------|-------|--------|
| **Excellent** | All weights within 5% | Green | Auto-approve |
| **Good** | Sent-received ≤ 1kg, estimate variance 5-10% | Yellow | Manual review |
| **Warning** | Estimate variance > 10% | Orange | Investigation required |
| **Failed** | Sent-received > 1kg | Red | Rejection or investigation |

---

## Business Logic Flow (Updated)

### Phase 1: Weight Filling with Estimation (Sending)
```
1. User selects box type from dropdown
2. User enters box ID (unique identifier)
3. (Optional) User enters part number
4. (Optional) User enters quantity
5. IF part number AND quantity provided:
   a. System retrieves unit weight from PartNumberWeight
   b. System retrieves empty box weight from BoxTypeConfig
   c. System calculates: estimatedWeight = (quantity × unitWeight) + emptyBoxWeight
   d. System displays estimated weight to user
6. User enters actual sent weight
7. IF estimated weight exists:
   a. System calculates variance: |sentWeight - estimatedWeight| / estimatedWeight
   b. IF variance > 5%: Display warning
   c. IF variance > 10%: Require comment/justification
8. System validates input
9. POST /api/boxWeight/fill with { boxType, boxId, sentWeight, quantity, estimatedWeight }
10. Backend sets sentBy=currentUser, sentAt=now()
11. Entry saved to database
12. User sees entry in "My Entries" table with estimation status
```

### Phase 2: Weight Verification with Estimation (Receiving)
```
1. Verifier searches by box ID or views pending list
2. Selects entry to verify
3. IF estimated weight exists:
   a. System displays: Estimated, Sent, and Expected variance
4. Verifier enters received weight
5. System calculates differences:
   a. Sent-Received: |receivedWeight - sentWeight|
   b. (If estimated) Estimated-Received: |receivedWeight - estimatedWeight|
6. Visual indicator shows:
   a. Green: All validations pass (≤1kg and ≤5% variance)
   b. Yellow: Sent-received OK, but estimate variance 5-10%
   c. Orange: Estimate variance >10%
   d. Red: Sent-received >1kg OR estimate variance >15%
7. IF variance is high: System prompts for investigation note
8. POST /api/boxWeight/verify/{id} with { receivedWeight, investigationNote }
9. Backend calculates: 
   a. validated = (|receivedWeight - sentWeight| <= 1.0)
   b. estimateAccurate = (variance <= 0.05) [if estimated]
10. Entry updated with receivedWeight, receivedBy, receivedAt, validated
```

---

## Enhanced Validation Rules

### Weight Difference Tolerance (Original)
- **Threshold**: 1.0 kg
- **Rule**: `|receivedWeight - sentWeight| <= 1.0`
- **Result**:
  - `validated = true` → Weight is within acceptable range
  - `validated = false` → Weight discrepancy detected

### Estimation Variance Tolerance (New)
- **Good Threshold**: 5%
- **Warning Threshold**: 10%
- **Critical Threshold**: 15%
- **Rule**: `variance = |actualWeight - estimatedWeight| / estimatedWeight`
- **Results**:
  - variance ≤ 5% → **Excellent** estimation accuracy
  - 5% < variance ≤ 10% → **Acceptable** with review
  - 10% < variance ≤ 15% → **Poor** requires investigation
  - variance > 15% → **Critical** discrepancy

### Input Validation
- Box ID: Required, non-empty string
- Weight: Positive number, formatted to 2 decimal places
- Box Type: Required selection from dropdown
- Quantity: Optional, positive integer
- Part Number: Optional, must exist in PartNumberWeight if provided

---

## Database Schema (Updated)

```sql
CREATE TABLE box_weight (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    box_type VARCHAR(255),
    box_id VARCHAR(255) NOT NULL,
    sent_weight DOUBLE NOT NULL,
    sent_by VARCHAR(255) NOT NULL,
    sent_at DATETIME NOT NULL,
    received_weight DOUBLE,
    received_by VARCHAR(255),
    received_at DATETIME,
    validated BOOLEAN,
    quantity INT,                    -- NEW
    estimated_weight DOUBLE          -- NEW
);

CREATE TABLE part_number_weight (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partnumber VARCHAR(255) NOT NULL UNIQUE,
    weight_unit DOUBLE NOT NULL
);

CREATE TABLE box_type_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    box_type VARCHAR(255) NOT NULL UNIQUE,
    empty_box_weight DOUBLE NOT NULL
);
```

---

## User Permissions

The Box Weight section is integrated with the application's authentication system:
- **Filling**: Available to users with access to the CAD section
- **Verification**: Available to users with verification rights
- **Configuration Management**: Available to ADMIN users only
  - Part Number Weight configuration
  - Box Type Config management
- User actions are tracked via `sentBy` and `receivedBy` fields

---

## Integration Points

### CAD Configuration
- Box types are loaded from `/api/cadConfig/boxType`
- Part number weights from `/api/partNumberWeight`
- Empty box weights from `/api/boxTypeConfig`

### ERP/PLM Integration (Recommended)
- Automatic sync of part number weights from ERP
- Update weight data when part numbers change
- Alert when new part numbers lack weight configuration

### User Management
- Current user's username is automatically captured from JWT token
- User-specific queries for "My Entries" functionality

### Quality Dashboard Integration
- Track estimation accuracy trends
- Identify part numbers with consistent variance
- Generate reports on weight discrepancies

---

## Reporting & Analytics

### Key Metrics
1. **Estimation Accuracy Rate**: Percentage of boxes within 5% variance
2. **Validation Pass Rate**: Percentage of boxes validated successfully
3. **Average Variance**: Mean variance between estimated and actual weights
4. **Part Number Accuracy**: Per-PN estimation accuracy

### Recommended Reports
```sql
-- Part Numbers with Poor Estimation Accuracy
SELECT 
    pn.partnumber,
    COUNT(*) as total_boxes,
    AVG(ABS(bw.sent_weight - bw.estimated_weight) / bw.estimated_weight * 100) as avg_variance_pct
FROM box_weight bw
LEFT JOIN part_number_weight pn ON bw.box_id LIKE CONCAT('%', pn.partnumber, '%')
WHERE bw.estimated_weight IS NOT NULL
GROUP BY pn.partnumber
HAVING avg_variance_pct > 10
ORDER BY avg_variance_pct DESC;

-- Daily Box Weight Summary
SELECT 
    CAST(sent_at AS DATE) as date,
    COUNT(*) as total_boxes,
    SUM(CASE WHEN validated = 1 THEN 1 ELSE 0 END) as validated_boxes,
    AVG(CASE 
        WHEN estimated_weight IS NOT NULL 
        THEN ABS(sent_weight - estimated_weight) / estimated_weight * 100 
        ELSE NULL 
    END) as avg_estimation_variance_pct
FROM box_weight
WHERE sent_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY CAST(sent_at AS DATE)
ORDER BY date DESC;
```

---

## Related Files Summary

| File | Type | Location |
|------|------|----------|
| BoxWeightFilling.js | React Component | `src/main/js/components/Layout/` |
| BoxWeightVerifying.js | React Component | `src/main/js/components/Layout/` |
| BoxWeight.scss | Styles | `src/main/js/components/Layout/` |
| BoxWeight.java | Entity | `src/main/java/.../domain/` |
| BoxWeightRepository.java | Repository | `src/main/java/.../repositories/` |
| BoxWeightService.java | Service | `src/main/java/.../services/` |
| BoxWeightController.java | Controller | `src/main/java/.../controller/` |
| PartNumberValidatedWeight.java | Entity | `src/main/java/.../domain/` |
| PartNumberValidatedWeightRepository.java | Repository | `src/main/java/.../repositories/` |
| PartNumberValidatedWeightService.java | Service | `src/main/java/.../services/` |
| PartNumberValidatedWeightController.java | Controller | `src/main/java/.../controller/` |

---

## Updates (February 2026)

### 1. GammeTechniqueImprimer Box ID Lookup

**Feature**: When the user types a Box ID (série) and presses Enter, the system looks up the `GammeTechniqueImprimer` table to auto-populate box details.

#### New Endpoint
```
GET /api/gammeTechniqueImprimer/serie/{serie}
```
- Calls `GammeTechniqueImprimerService.findByNSerieGammeImp(serie)`
- Returns the first matching record or 400 if not found

#### Frontend Changes (BoxWeightFilling.js)
- Added `handleBoxIdLookup()` method triggered on Enter key in Box ID input
- On match: auto-populates `partnumber` (from `partNumberImp`) and `quantity` (from `quantiteImp`)
- State additions: `partNumberWeight`, `gammeLoading`, `gammeInfo`

### 2. Empty Weight by Box Type

**Feature**: Predefined empty box weights based on box color.

| Box Type | Empty Weight (kg) |
|----------|-------------------|
| Gray (Gris) | 0.5 |
| Black (Noir) | 0.8 |

- Implemented via `getEmptyWeight()` method in BoxWeightFilling.js
- Box type options updated with emptyWeight display

### 3. Part Number Weight Calculation

**Feature**: Calculate weight per part number piece from sent weight.

#### Formula
```
partNumberWeight = (sentWeight - emptyWeight) / quantity
```

#### Display
- Shown as info alert below the estimated weight section
- Only appears when sentWeight, quantity, and emptyWeight are all available
- Included in the save payload as `partNumberWeight`

### 4. Process Validated Weight (ROLE_PROCESS)

**Feature**: Process team can validate/record a reference weight for each part number.

#### Entity: PartNumberValidatedWeight
```java
@Entity
@Table(name = "PartNumberValidatedWeight")
public class PartNumberValidatedWeight {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String partnumber;
    private Double validatedWeight;
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String comment;
}
```

#### API Endpoints
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/partNumberValidatedWeight/list` | ROLE_PROCESS | List all validated weights |
| POST | `/api/partNumberValidatedWeight` | ROLE_PROCESS | Save validated weight |
| GET | `/api/partNumberValidatedWeight/partnumber/{pn}` | ROLE_PROCESS | Get latest weight for PN |
| POST | `/api/partNumberValidatedWeight/delete` | ROLE_PROCESS | Delete record |

#### Security
- All endpoints require `@PreAuthorize("hasRole('PROCESS')")`
