# MG-CMS Feature Implementation Summary

## Date: Implementation Session

This document summarizes all features implemented during this session.

---

## 1. KPI Maintenance Page (KPI Réactivité Maintenance)

### Location
- **Frontend**: `src/main/js/components/Layout/KpiMaintenance.js`
- **Backend**: 
  - `InterventionController.java` - New endpoint `/api/intervention/kpi-maintenance`
  - `InterventionService.java` - New method `findMaintenanceKpi()`
  - `InterventionRepository.java` - New query for maintenance interventions

### Features
- **Dashboard View** with summary cards:
  - Nombre total d'interventions
  - Temps de réaction moyen
  - Temps de réparation moyen
  - Taux de résolution

- **Data Grouping Options**:
  - Par Machine
  - Par Code Arrêt
  - Par Shift (Matin/Après-midi/Nuit)
  - Par Jour
  - Par Semaine

- **Visualizations**:
  - Bar charts for time metrics
  - Pie charts for distribution
  - Collapsible sections for each grouping

- **Filtering**:
  - Date range (start/end date)
  - Machine selection

- **Export**:
  - CSV export functionality

### KPI Calculations
- **Temps de réaction**: `debutIntervention - debutArret` (in minutes)
- **Temps de réparation**: `finIntervention - debutIntervention` (in minutes)
- **Temps d'arrêt total**: `finIntervention - debutArret` (in minutes)
- **Taux de résolution**: Percentage of interventions with `problemeResolu = true`

### Access
- Menu: Dashboard → KPI → KPI Maintenance
- Route: `/kpiMaintenance`

---

## 2. IPPM KPI Report Improvements

### Location
- **Frontend**: `src/main/js/components/Layout/IppmReport.js`

### Features Added

#### Sortable Columns
- Click on any column header to sort ascending/descending
- Sort indicator icons (↑ ↓) show current sort state
- Supports both numeric and string sorting

#### Filter Row
- Second row below headers with text inputs
- Filter by any column value
- Real-time filtering as you type
- Number fields: exact match
- Text fields: contains match

#### Double-Click Detail Modal
- Double-click on any row to see full details
- Modal displays all row fields in readable format
- Field names are formatted for readability (camelCase → Title Case)

### Technical Implementation
- New state: `sortConfig`, `searchFilters`, `selectedRow`, `showDetailModal`
- Methods: `handleSort()`, `getSortedData()`, `handleSearchChange()`, `getFilteredData()`, `handleRowDoubleClick()`, `renderDetailModal()`

---

## 3. Intervention Admin Edit/Delete

### Location
- **Frontend**: `src/main/js/components/Layout/ValidationIntervention.js`

### Features Added

#### Admin Role Check
- Uses Redux to access `security.user.roles`
- `isAdmin()` method checks for `ROLE_ADMIN`

#### Edit Button
- Visible only for admin users
- Allows editing intervention data
- Sends updated data via `POST /api/intervention`

#### Delete Button
- Visible only for admin users
- Shows confirmation dialog before deletion
- Calls `POST /api/intervention/delete`
- Red button with trash icon

### Technical Implementation
- Added Redux `connect()` integration
- New state: `editMode`, `deleteConfirmId`
- Icon imports: `faTrash`, `faEdit`

---

## 4. MachineLog.js Enhancements

### Location
- **Frontend**: `src/main/js/components/Layout/MachineLog.js`
- **Backend**: `src/main/java/com/lear/MGCMS/controller/MachineLogController.java`

### Features Added

#### Pagination
- 100 files per page (configurable)
- Page navigation controls
- Display: "Affichage X - Y sur Z fichiers"

#### Search/Filter
- Search by filename (contains)
- Filter by prefix (startsWith)
- Press Enter or click button to search

#### File Download
- Download button per file row
- Uses blob response for proper file download

#### File Content Viewer
- View file content in modal (max 5MB)
- Ctrl+F style search within file:
  - Search box in modal header
  - Highlights all matches
  - Current match in orange, others in yellow
  - Navigate between matches with up/down buttons
  - Shows match count: "X/Y"

### Backend Endpoints Added
- `GET /api/machineLog/folder/{folderName}` - Enhanced with pagination params:
  - `page` (default: 0)
  - `size` (default: 100)
  - `fileName` (optional, contains filter)
  - `prefix` (optional, startsWith filter)
- `GET /api/machineLog/read/{folderName}/{fileName}` - Read file content
- `GET /api/machineLog/download/{folderName}/{fileName}` - Download file

---

## 5. Machine Type Swap (CAD Section)

### Location
- **Frontend**: `src/main/js/components/Layout/MachineTypeSwap.js`
- **Backend**: 
  - `CuttingPlanMaterialPlacementDataController.java` - New endpoint
  - `CuttingPlanMaterialPlacementDataService.java` - New method

### Features

#### Search Placements
- Filter by placement prefix
- Filter by part number material
- Filter by cutting plan ID
- Results sorted by cutting plan (desc)

#### Multi-Select
- Checkbox per row
- Select All checkbox
- Visual selection feedback (blue highlight)

#### Machine Type Selection
- Dropdown with available machine types
- Loaded from `/api/machineType/all`

#### Preview Before Swap
- Modal showing all changes
- From → To machine display
- Indicates which items will change
- Count of changes vs no-change items

#### Execute Swap
- Batch update with progress
- Results modal showing success/failure per item
- Auto-refresh search results after swap

### Backend Endpoint Added
```java
POST /api/cuttingPlanMaterialPlacementData/updateMachine
Body: {
    "cuttingPlan": Long,
    "placement": String,
    "partNumberMaterial": String,
    "newMachine": String
}
```

### Access
- Menu: CAD → Changement Type Machine
- Route: `/machineTypeSwap`

---

## File Changes Summary

### New Files Created
1. `src/main/js/components/Layout/KpiMaintenance.js` (~700 lines)
2. `src/main/js/components/Layout/MachineTypeSwap.js` (~500 lines)
3. `KPI_Maintenance.md` (this file)

### Modified Files

#### Frontend
- `src/main/js/App.js` - Added imports and routes
- `src/main/js/components/Dashboard.js` - Added menu links
- `src/main/js/components/Layout/IppmReport.js` - Sorting, filtering, detail modal
- `src/main/js/components/Layout/ValidationIntervention.js` - Admin edit/delete
- `src/main/js/components/Layout/MachineLog.js` - Pagination, search, viewer

#### Backend
- `InterventionController.java` - KPI maintenance endpoint
- `InterventionService.java` - KPI maintenance method
- `InterventionRepository.java` - KPI maintenance query
- `MachineLogController.java` - Pagination, read, download endpoints
- `CuttingPlanMaterialPlacementDataController.java` - Update machine endpoint
- `CuttingPlanMaterialPlacementDataService.java` - Update machine method

---

## Routes Summary

| Route | Component | Description |
|-------|-----------|-------------|
| `/kpiMaintenance` | KpiMaintenance | Maintenance KPI Dashboard |
| `/machineTypeSwap` | MachineTypeSwap | Machine Type Swap Tool |
| `/ippmReport` | IppmReport | IPPM Report (enhanced) |
| `/validationIntervention` | ValidationIntervention | Intervention validation (enhanced) |
| `/machineLog` | MachineLog | Machine Log Viewer (enhanced) |

---

## Menu Structure Changes

### Dashboard → KPI
- Added: **KPI Maintenance**
- Added: **KPI Charge Machines**

### CAD
- Added: **Changement Type Machine**

---

## 6. KPI Charge Machines (February 2026)

### Location
- **Frontend**: `src/main/js/components/Layout/KpiChargeMachine.js`
- **Backend**: Reuses existing Plan de Charge API endpoints

### Features
- **Stacked Bar Chart**: Visualizes machine load per date/shift with three segments:
  - **Retard** (red) - Carryover from previous shift (notCut time)
  - **Charge** (green/blue/yellow) - Current shift planned load
  - **Capacité restante** (gray) - Remaining capacity up to 100%
- **Color-Coding by Time**:
  - Past shifts: Green charge (actual), solid red retard
  - Current shift: Yellow/amber charge (mixed), orange retard
  - Future shifts: Blue charge (estimated), faded red retard
- **100% Reference Line**: Dashed red line at 100% capacity
- **Machine Type Filter**: View all machines or filter by specific type (Lectra, Gerber, LASER-DXF, DIE, etc.)
- **Per-Type Breakdown**: When "All" selected, shows individual mini-charts for each machine type
- **Table View**: Toggle between chart and tabular data with columns for Retard, Charge, Sum, Capacité, cutting time
- **Tooltip Details**: Hover over bars to see total %, machine counts, cutting time in minutes

### API Endpoints Used
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/planDeCharge/machines` | Machine list with types/zones |
| GET | `/api/planDeCharge/currentShift` | Current shift identification |
| GET | `/api/planDeCharge/search` | Status grid for machine availability |
| GET | `/api/planDeCharge/aggregatedCuttingTime` | Cutting time by machine/date/shift |
| GET | `/api/planDeCharge/aggregatedCuttingTimeWithStatus` | Cut/notCut breakdown for retard |

### Access
- Menu: Dashboard → KPI → KPI Charge Machines
- Route: `/kpiChargeMachine`

### Technical Details
- Uses `chart.js` + `react-chartjs-2` (Bar component)
- Registers: BarElement, BarController, CategoryScale, LinearScale, Title, Tooltip, Legend
- Shift duration constant: 460 minutes (~7h40)
- Charge % = (totalCuttingTime / availableTime) × 100
- Retard % = (notCutTimeFromPrevShift / availableTime) × 100

---

## Dependencies Used
- React Bootstrap (Modal)
- FontAwesome icons
- Axios for HTTP requests
- Redux for state management (ValidationIntervention)
- chart.js + react-chartjs-2 (KpiChargeMachine)

---

## Notes
- All features follow existing code patterns in the project
- French language used for UI text to match existing application
- Role-based access control maintained
- Error handling implemented with user-friendly messages
