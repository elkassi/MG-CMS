# Plan de Charge - Cutting Machines Load Plan

**Feature:** Plan de Charge (Machine Load Planning)  
**Module:** Process  
**Access:** ROLE_PROCESS  
**Author:** Mouad El Ghazi  
**Created:** February 2026

---

## Table of Contents
1. [Overview](#overview)
2. [Entity: EtatMachineHistorique](#entity-etatmachinehistorique)
3. [Entity: ShiftLoadCalculation](#entity-shiftloadcalculation)
4. [API Endpoints](#api-endpoints)
5. [Frontend Component](#frontend-component)
6. [Business Logic](#business-logic)
7. [Status Codes](#status-codes)
8. [Machine Type Colors](#machine-type-colors)

---

## Overview

The Plan de Charge feature provides a comprehensive view of cutting machine status, load, and efficiency across shifts. Process team members can:

- View machine status by date range and shift
- Track machine availability (Marche, Arrêt, PM, etc.)
- Calculate shift load based on cutting request timing
- Monitor efficiency and carryover between shifts
- Update machine status in real-time

### Shift Schedule
| Shift | Start Time | End Time | Notes |
|-------|------------|----------|-------|
| Shift 1 | 21:55 (previous day) | 05:45 | Night shift |
| Shift 2 | 05:55 | 13:45 | Morning shift |
| Shift 3 | 13:55 | 21:45 | Afternoon shift |

---

## Entity: EtatMachineHistorique

Tracks the historical status of each machine over time.

### Database Schema
```sql
CREATE TABLE etat_machine_historique (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine VARCHAR(50) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NULL,
    code_etat VARCHAR(10) NOT NULL,
    cause VARCHAR(500) NULL,
    action VARCHAR(500) NULL,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT GETDATE(),
    updated_by VARCHAR(100),
    updated_at DATETIME,
    closed_by VARCHAR(100),
    closed_at DATETIME
);

CREATE INDEX idx_etat_machine_dates ON etat_machine_historique(machine, start_date, end_date);
```

### Java Entity
```java
@Entity
@Table(name = "etat_machine_historique")
public class EtatMachineHistorique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String machine;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String codeEtat;
    private String cause;
    private String action;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String closedBy;
    private LocalDateTime closedAt;
}
```

---

## Entity: ShiftLoadCalculation

Stores calculated load and efficiency data for each shift to avoid recalculation.

### Database Schema
```sql
CREATE TABLE shift_load_calculation (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shift_date DATE NOT NULL,
    shift_number INT NOT NULL,
    machine_type VARCHAR(50),
    total_planned_time DECIMAL(10,2),
    total_actual_time DECIMAL(10,2),
    available_time DECIMAL(10,2),
    load_percentage DECIMAL(5,2),
    efficiency_percentage DECIMAL(5,2),
    carryover_time DECIMAL(10,2),
    machines_count INT,
    calculated_at DATETIME,
    calculated_by VARCHAR(100),
    CONSTRAINT UK_shift_calculation UNIQUE (shift_date, shift_number, machine_type)
);
```

---

## API Endpoints

### EtatMachineHistorique Controller (`/api/etatMachineHistorique`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/list` | Get all records |
| GET | `/{id}` | Get record by ID |
| GET | `/machine/{machine}` | Get records for a specific machine |
| GET | `/byDateRange?startDate=&endDate=` | Get records by date range |
| GET | `/machine/{machine}/byDateRange?startDate=&endDate=` | Get records for machine in date range |
| GET | `/machine/{machine}/active` | Get active (no end date) records for machine |
| GET | `/machine/{machine}/status?dateTime=` | Get status code at a specific time |
| GET | `/distinctMachines` | Get list of distinct machine names |
| GET | `/activeBreakdowns/count` | Count of active breakdowns |
| POST | `/` | Create new record |
| PUT | `/{id}` | Update record |
| PUT | `/{id}/close` | Close status (set end date to now) |
| PUT | `/{id}/closeWithDate?endDate=` | Close status with specific end date |
| DELETE | `/{id}` | Delete record |

### Plan de Charge Controller (`/api/planDeCharge`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/search?startDate=&endDate=` | Get status grid + load calculations for date range (legacy, no longer used by frontend) |
| GET | `/machines` | Get all machines grouped by zone |
| GET | `/shiftTimes?date=&shiftNumber=` | Get shift start/end times |
| GET | `/currentShift` | Get current shift info (date, shift number, times) |
| POST | `/calculate?date=&shiftNumber=` | Calculate and save shift load |
| GET | `/loadCalculations?startDate=&endDate=` | Get saved load calculations |
| GET | `/aggregatedCuttingTime?startDate=&endDate=` | Get cutting time by machine/date/shift (legacy, no longer used by frontend) |
| GET | `/aggregatedCuttingTimeWithStatus?startDate=&endDate=` | Get cutting time split by cut/notCut status (legacy, no longer used by frontend) |
| POST | `/cuttingTimesByPlacements` | Get cutting times from TimingModel for placements |
| GET | `/distinctPlacements?startDate=&endDate=` | Get distinct placements in date range |
| GET | `/detailedSeries?dueDate=&dueShift=` | Get detailed series for shift with cutting times |
| GET | `/chargeSummary?dueDate=&dueShift=` | Get charge summary with breakdown by machine type |
| GET | `/seriesForDateRange?startDate=&endDate=` | **NEW** Get all series with resolved effective cutting times for frontend indicator calculations. Returns: serie, machine, placement, tableCoupe, dueDate (ISO string), dueShift, effectiveCuttingTime, dateDebutCoupe (ISO string), dateFinCoupe (ISO string), nbrCouche |

---

## Frontend Component

### Route
```javascript
<SecuredRoute exact path="/planDeCharge" component={PlanDeCharge} />
```

### Dashboard Menu Entry
```javascript
// In Dashboard.js - Process section
{ to: "/planDeCharge", title: "Plan de Charge" }
```

### Component Structure
```
PlanDeCharge.js
├── Date Range Selector (dateDebut, dateFin)
├── Search Button + Export CSV + Toggle Charge Indicators
├── Historique des États Machines (collapsible list of all EtatMachineHistorique)
├── Legend Panel (status codes + machine type colors)
├── Load Summary Panel (computed from frontend-aggregated seriesData)
│   ├── Date / Shift / Cap / Chg (min) / Chg % / SR (min) / SR % / Ret prev (min) / Ret prev % / Sum % / Ret→next brut (min) / Ret→next brut % / T. Rest (min) / Ret adj (min) / Actions
│   ├── Color coding: red >100%, orange >80%, green otherwise
│   └── Clickable rows → opens Charge Details overlay (derived from seriesData, no backend call)
├── Machine Status Grid
│   ├── Header: Zone > Machines
│   ├── Indicator Columns (per machine type): Ret | Chg | SR | Sum | Cap | Rec
│   ├── Coupe Group (Lectra + Lectra IP6 combined): Ret | Chg | SR | Sum | Cap | Rec
│   ├── Global Charge column
│   ├── Data Rows: Date > Shift > Status cells (clickable)
│   └── Current shift row highlighted with orange border
├── Next Shift Preparation Section
│   ├── Per machine type: Charge Prévue, Retard Ajusté issu du shift courant, Travail Total
│   ├── Machines Dispo vs Recommandées, Surplus/Déficit
│   ├── Temps restant du shift courant et retard ajusté affichés par type
│   ├── Action recommendations (Arrêter/Démarrer machines)
│   ├── "Charger Séries" button → filters series from loaded seriesData (no backend call)
│   ├── Series table with temps changement série column
│   └── Export buttons (CSV/Excel) for next shift series
├── Edit Modal (for updating machine status)
│   ├── Machine history list with edit/delete/close actions
│   ├── Single datetime-local input for Date Début
│   ├── Date Fin with X button to clear
│   └── Code Status, Cause, Action fields
└── Charge Details Overlay (triggered from Load Summary)
    ├── Global Indicators Summary (9 stat cards)
    │   ├── Séries (count) / Chg (min + %) / SR (min + %) / Ret prev (min + %)
    │   └── Sum (Chg - SR + Ret prev) % / Ret→next brut (min + %) / Temps restant / Ret→next ajusté / Cap
    ├── Per Machine Type Indicators Table (15 columns)
    │   ├── Type / Séries / Cap / Chg min + % / SR min + % / Ret prev min + %
    │   └── Sum % / Ret→next brut min + % / Temps restant / Ret ajusté min + % / GLOBAL footer
    ├── Capacité Installée editor (ROLE_PROCESS / ROLE_ADMIN)
    │   └── Edit `capaciteInstallee` and `efficienceTarget` for Coupe / Laser per shift
    ├── Detailed series list (simplified 10 columns, no Source/Statut Matelassage)
    ├── Formula explanation section
    └── Export buttons (CSV/Excel) for series list
```

---

## Business Logic

### Status Determination Logic
```
For each cell (machine, date, shift):
1. Query EtatMachineHistorique where:
   - machine = currentMachine
   - startDate <= shiftEndTime
   - (endDate IS NULL OR endDate >= shiftStartTime)
2. If found, display codeEtat
3. If not found, display default status (M - Marche)
```

### Shift Time Calculation
```javascript
function getShiftTimes(date, shiftNumber) {
    switch(shiftNumber) {
        case 1:
            return {
                start: previousDay(date) + " 21:55",
                end: date + " 05:45"
            };
        case 2:
            return {
                start: date + " 05:55",
                end: date + " 13:45"
            };
        case 3:
            return {
                start: date + " 13:55",
                end: date + " 21:45"
            };
    }
}
```

### Load Calculation
```
For each shift:
1. Get all CuttingRequestSerie for that shift (dueDate + dueShift)
2. For each serie:
    a. Use the serie machine field as the machine key
    b. Resolve machine type from ProductionTable using the machine name
    c. Get placement from TimingModel
    d. Determine cutting time (priority order):
      - Validated_Cutting_time_Timing_Model (if > 0)
      - Real_Cutting_time_Timing_Model (if validated is null/0)
      - tempsDeCoupe from CuttingRequestSerieInfo (fallback)
    e. Apply multipliers:
      - LASER-DXF: multiply by nbrCouche (cuts per layer)
      - Gerber: multiply by 2 (Gerber needs double the estimated time)
3. Check cutting status:
   - dateFinCoupe != null → Cut (completed)
   - dateDebutCoupe != null && dateFinCoupe == null → Cutting (in progress)
   - dateDebutCoupe/dateFinCoupe outside shift interval or null → Retard (carryover)
4. Calculate per machine type:
   - Chg (Charge) = Sum of estimated cutting times (without dateDebut/dateFin check) / available time × 100
    - Ret (Retard) = adjusted carryover from previous shift / available time × 100
   - SR (Surpassant) = Current shift's cut time from previous shift / available time × 100
   - Sum = Ret + Chg - SR
   - Cap = Available machines (M/MS/MD/R) / total machines
   - Rec = Recommended machines = ceil(totalWork / 460)
5. Coupe Group: combines Lectra + Lectra IP6 machines for a grouped indicator
6. Global: combines all machine types
```

### Aggregation Alignment Rules

The frontend status grid, next-shift preparation, detailed series export, and backend charge summary must all use the same source-of-truth rules:

- Group by `CuttingRequestSerieInfo.machine`
- Resolve machine type from `ProductionTable.nom = machine`
- Use the same cutting-time priority for every calculation:
    - validated TimingModel time
    - real TimingModel time
    - `tempsDeCoupe`
- Apply the same type-specific multipliers after the priority decision

This guarantees that:

- the sum of exported series `Temps (min)` for one machine type
- divided by `(available machines of that type * 460)`

matches the `Chg %` displayed in the grid for the same shift and machine type.

### TimingModel Lookup Batching

SQL Server limits `IN` predicates to 2100 parameters. Plan de Charge can request more than 2100 distinct placements over a date range, so TimingModel lookups are batched in chunks before querying.

This batching is required for:

- `/aggregatedCuttingTime`
- `/aggregatedCuttingTimeWithStatus`
- any bulk TimingModel lookup used by Plan de Charge

### Retard (Carryover) Logic — Redesigned
```
Retard is calculated per-serie based on 3 distinct cases:

Case (a) - Non coupé:
  dateDebutCoupe IS NULL → Series was never started
  Full retard: entirety of effectiveCuttingTime counts as retard

Case (b) - Début coupe after shift end:
  dateDebutCoupe > shiftEnd → Cutting started after the shift ended
  Full retard: entirety of effectiveCuttingTime counts as retard

Case (c) - Partial retard:
  dateFinCoupe > shiftEnd → Cutting completed after the shift ended
  Partial retard = Duration.between(shiftEnd, dateFinCoupe) in minutes
  Only the overflow portion beyond the shift counts as retard

If none of the above → Series was completed within the shift → No retard

The backend query findSeriesWithTimestampsForRetard returns per-serie data
with dateDebutCoupe, dateFinCoupe timestamps for individual analysis.

Carryover propagated to the NEXT shift is not always the raw `notCut` value.

If the source shift is the currently active shift, the frontend computes the
remaining productive capacity before deciding what is really carried forward:

  configuredShiftMinutes = capaciteInstallee.tempsTotalParMachine || 460
  efficienceTarget = capaciteInstallee.efficienceTarget || 90
  elapsedMinutes = ceil(minutesSinceShiftStart / 5) * 5
  remainingMinutesPerMachine = max(0, configuredShiftMinutes - elapsedMinutes)
  remainingCapacity = remainingMinutesPerMachine * activeMachines * (efficienceTarget / 100)
  adjustedCarryover = max(0, rawNotCut - remainingCapacity)

This means:
- the current shift can still absorb part or all of its raw retard
- if `adjustedCarryover = 0`, the next shift receives `0` retard
- the same adjusted value is used by the status grid, load summary, charge details,
  and next-shift preparation
```

### Next Shift Preparation Logic
```
For each machine type:
1. Get planned cutting time for next shift from aggregatedCuttingTime
2. Get raw current shift retard from aggregatedCuttingTimeWithStatus (notCut)
3. Compute remaining productive capacity from Capacité Installée:
  - configuredShiftMinutes = tempsTotalParMachine || 460
  - elapsedMinutes = ceil(minutesSinceShiftStart / 5) * 5
  - remainingMinutesPerMachine = max(0, configuredShiftMinutes - elapsedMinutes)
  - remainingCapacity = remainingMinutesPerMachine × currentShiftActiveMachines × (efficienceTarget || 90) / 100
4. Adjust the retard that is really passed to next shift:
  - currentShiftRetard = max(0, rawNotCut - remainingCapacity)
5. Total Work = next shift cutting + current shift retard
6. Recommended Machines = ceil(Total Work / 460)
7. Surplus/Deficit = Available Machines - Recommended Machines
8. Action:
   - Surplus > 0 → "Arrêter N machine(s)" (can turn OFF excess machines)
   - Surplus < 0 → "Démarrer N machine(s)" (need to activate more)
   - Surplus = 0 → "OK"
9. "Charger Séries" button:
   - Filters from already-loaded seriesData (no backend call)
   - Shows series table with temps changement série column
   - Export to CSV/Excel available
Goal: Activate machines so each type approaches 100% load without exceeding it.
```

### Query for Series Data
```sql
SELECT serie, cuttingRequest_sequence, cr.dueDate, cr.dueShift, machine,
       placement, partNumberMaterial, crs.description,
       dateDebutMatelassage, dateFinMatelassage, statusMatelassage, tableMatelassage,
       dateDebutCoupe, dateFinCoupe, statusCoupe, tableCoupe,
       tm.Validated_Cutting_time_Timing_Model,
       tm.Real_Cutting_time_Timing_Model,
       tempsDeCoupe
FROM CuttingRequestSerie AS crs
JOIN CuttingRequest AS cr ON cr.sequence = crs.cuttingRequest_sequence
LEFT JOIN Timing_Model AS tm ON tm.Placement_Timing_Model = crs.placement
WHERE cr.dueDate = '2026-02-05' AND cr.dueShift = '1'
ORDER BY serie;
```

### Carryover Calculation
```
Carryover = series NOT completed within the shift interval.

For each serie in the shift:
  - If dateFinCoupe IS NULL → NOT cut → counts as carryover
  - If dateFinCoupe IS NOT NULL → IS cut → completed
Carryover Time = Sum of effectiveCuttingTime for all non-cut series.
If the source shift is still active, only the adjusted carryover
`max(0, rawCarryover - remainingCapacity)` is added to the next shift's retard.
```

### Indicator Columns
The status grid shows per machine type and per date/shift:
| Column | Description |
|--------|-------------|
| **Ret** | Retard: adjusted carryover % from previous shift (`adjustedCarryover / available time × 100`) |
| **Chg** | Charge: planned load % based on estimated cutting times (without dateDebut/dateFin check) |
| **SR** | Surpassant: % of series planned in current shift but worked (cut) in a previous shift |
| **Sum** | Sum: Ret + Chg - SR |
| **Cap** | Capacité: available machines (status M/MS/MD/R) / total machines |
| **Rec** | Recommandé: recommended number of machines to handle the load |

Additional groups:
| Group | Description |
|-------|-------------|
| **Coupe** | Combined Lectra + Lectra IP6 machines (same 6 columns: Ret/Chg/SR/Sum/Cap/Rec) |
| **Global** | Combined charge % across all machine types |

### Cutting Time Priority
The cutting time for each serie is determined in this order:
1. **Validated_Cutting_time_Timing_Model** (from TimingModel) - if > 0
2. **Real_Cutting_time_Timing_Model** (from TimingModel) - if validated is null/0
3. **tempsDeCoupe** (from CuttingRequestSerieInfo) - fallback

---

## Status Codes

| Code | Name | Color (Hex) | Description |
|------|------|-------------|-------------|
| M | Marche | #00b050 | Machine running |
| A | Arrêt | #a5a5a5 | Machine stopped |
| P | PM | #ffff00 | Preventive Maintenance |
| O | OFF | #000000 | Machine off |
| R | Récupération | #e2efda | Recovery machine |
| MS | Machine Spéciale | #00da63 | Special machine |
| AD | Arrêt Prod | #ff0000 | Stop requested by production |
| ADM | Arrêt Maintenance | #ed7d31 | Stop requested by maintenance |
| MD | Démarrée sur demande | #c00000 | Started on demand |
| PN | En Panne | #c00000 | Machine breakdown |

---

## Machine Type Colors

| Machine Type | Background Color |
|--------------|------------------|
| Lectra | #a9d08e |
| Lectra IP6 | #92d050 |
| Gerber | #e2efda |
| LASER-DXF | #ffc000 |
| DIE | #0077ff |

---

## File Structure

```
src/main/java/com/lear/MGCMS/
├── domain/
│   ├── EtatMachineHistorique.java
│   └── ShiftLoadCalculation.java
├── repositories/
│   ├── EtatMachineHistoriqueRepository.java
│   ├── ShiftLoadCalculationRepository.java
│   └── CuttingRequest/
│       └── CuttingRequestSerieInfoRepository.java  (queries for Plan de Charge)
├── services/
│   ├── EtatMachineHistoriqueService.java
│   └── PlanDeChargeService.java
└── controller/
    ├── EtatMachineHistoriqueController.java
    └── PlanDeChargeController.java

src/main/js/components/Layout/
├── PlanDeCharge.js
└── styles/
    └── PlanDeCharge.scss

Cross-server reference:
└── [MATNR-APP01].[qualite].[dbo].[Timing_Model]  (TimingModel entity via TimingModelRepository)
```

---

## Key Constants

| Constant | Value | Description |
|----------|-------|-------------|
| SHIFT_DURATION_MINUTES | 460 | Effective work time per shift (~7h40) |
| Available statuses for machines | M, MS, MD, R | Statuses counted as "available" for load calculation |

---

## Data Flow

```
1. loadInitialData() on mount (once):
   - GET /api/productionTable/list → machines grouped by zone (frontend sorting)
   - GET /api/planDeCharge/currentShift → current shift info
2. loadData() on search (parallel):
   - GET /api/etatMachineHistorique/listBetweenDate → allHistoryList
   - GET /api/planDeCharge/seriesForDateRange?startDate&endDate → seriesData
     (each entry: serie, machine, placement, tableCoupe, dueDate (ISO string),
      dueShift, effectiveCuttingTime, dateDebutCoupe (ISO string),
      dateFinCoupe (ISO string), nbrCouche — cutting time already resolved by backend
      using validated > real > tempsDeCoupe priority + LASER-DXF nbrCouche + Gerber x2.
      Dates are explicitly converted to strings to avoid Jackson LocalDate array serialization)
3. buildStatusGridFromHistory() (frontend):
   - For each machine, for each date/shift, find if any history record 
     covers the shift midpoint → use codeEtat, else default 'M'
4. computeAggregationsFromSeries() (frontend):
   - Builds aggregatedCuttingTime: machine → date → shift → totalCuttingTime
   - Builds aggregatedCuttingTimeWithStatus: machine → date → shift → { total, cut, notCut, sr }
   - Retard 3-case logic and SR calculation done on frontend
5. getLoadIndicator() calculates per machineType:
   - Chg: sum of aggregatedCuttingTime / availableTime × 100
  - Ret: adjusted carryover from the previous shift
   - SR: current shift's sr from aggregatedCuttingTimeWithStatus
   - Available machines: count M/MS/MD/R statuses from statusGrid
6. getCoupeGroupIndicator():
  - Same logic as getLoadIndicator but combines Lectra + Lectra IP6 + Gerber machines
7. loadChargeDetails() (frontend only, no backend call):
   - Filters seriesData by dueDate/dueShift
   - Computes per-serie: retardMinutes, srMinutes, isPartialRetard
  - Computes previous shift retard with the same adjusted carryover rule used by the grid
   - Builds machineTypeIndicators: per-type breakdown with:
     seriesCount, chargeMin, chgPct, srMin, srPct, retardPrevMin, retardPrevPct,
    sumPct (Chg - SR + RetPrev), retardThisShiftMin, retardThisShiftPct,
    tempsRestantMin, retardAdjustedMin, retardAdjustedPct,
     machinesAvailable, totalMachines
  - Builds global totals (same indicators aggregated across all machine types)
8. renderNextShiftPreparation():
  - Uses aggregatedCuttingTime for next shift's planned charge
  - Uses adjusted carryover from the current shift for the retard part
  - Uses Capacité Installée (`tempsTotalParMachine`, `efficienceTarget`) to compute remaining capacity
   - "Charger Séries" button: filters from loaded seriesData (no backend call)
9. Capacité Installée editing:
  - Available directly from the charge details modal for ROLE_PROCESS / ROLE_ADMIN
  - Supports updating `capaciteInstallee` and `efficienceTarget` per shift and group
9. Export:
   - exportSeriesCSV(): exports series to CSV with BOM, semicolon separator
   - exportSeriesExcel(): exports series to HTML table as .xls file
```

### API Calls Reduction (Refactored)

| Before (5+ calls per search) | After (2 calls per search) |
|------|------|
| GET /planDeCharge/currentShift (duplicate) | Removed (loaded once on mount) |
| GET /planDeCharge/search | Removed (statusGrid built from listBetweenDate data) |
| GET /planDeCharge/aggregatedCuttingTime | Removed (computed on frontend from seriesData) |
| GET /planDeCharge/aggregatedCuttingTimeWithStatus | Removed (computed on frontend from seriesData) |
| GET /planDeCharge/detailedSeries (for charge details) | Removed (filtered from loaded seriesData) |
| GET /planDeCharge/chargeSummary (for charge details) | Removed (computed on frontend) |
| GET /planDeCharge/detailedSeries (for next shift) | Removed (filtered from loaded seriesData) |
| **New: GET /etatMachineHistorique/listBetweenDate** | Single call for history |
| **New: GET /planDeCharge/seriesForDateRange** | Single call for all series with resolved cutting times |

---

## Updates (February 2026)

### LASER-DXF nbrCouche Multiplier (Fixed)

For **LASER-DXF machines only**, the estimated cutting time (`tempsDeCoupe`) must be multiplied by the number of layers (`nbrCouche`) because LASER-DXF cuts one layer at a time (unlike Lectra/Gerber which cut multiple layers at once).

**Important:** This multiplication is **only applied for LASER-DXF** machines. Other machine types (Lectra, Gerber, DIE, etc.) should NOT multiply by nbrCouche.

#### Backend Changes

##### CuttingRequestSerieInfoRepository.java
1. **`findAggregatedCuttingTimeByMachineAndShiftWithNbrCouche`**: Uses CASE WHEN subquery to conditionally multiply:
   ```sql
   SUM(CASE WHEN crs.tableCoupe IN (
       SELECT pt.nom FROM ProductionTable pt WHERE pt.machineType.name = 'LASER-DXF'
   ) THEN crs.tempsDeCoupe * COALESCE(crs.nbrCouche, 1) 
   ELSE crs.tempsDeCoupe END)
   ```
2. **`findAggregatedCuttingTimeWithStatus`**: Same CASE WHEN fix applied
3. **New query `findSeriesWithTimestampsForRetard`**: Returns per-serie data with timestamps (dateDebutCoupe, dateFinCoupe) for retard analysis. Also applies CASE WHEN for LASER-DXF nbrCouche.

##### PlanDeChargeService.java
1. **`getDetailedSeriesForShift`**: Added machine type resolution via `getMachineTypeMap()` to check `isLaserDxf` before nbrCouche multiplication. Only multiplies when machine type is LASER-DXF AND source is "TempsDeCoupe".
2. **`getAggregatedCuttingTimeWithStatus`**: Completely rewritten to use per-serie `findSeriesWithTimestampsForRetard` query for 3-case retard analysis.
3. **`calculateShiftChargeSummary`**: Added Unknown machine type fallback — if `tableCoupe` doesn't resolve to a machine type, tries the `machine` field as fallback.

#### Calculation Logic
```
For each serie:
  effectiveCuttingTime = tempsDeCoupe  (base value)

  If machineType == "LASER-DXF" AND source == "TempsDeCoupe" AND nbrCouche > 1:
      effectiveCuttingTime = tempsDeCoupe × nbrCouche

  // TimingModel values (Validated/Real) already account for nbrCouche
  // so multiplication is only needed for the tempsDeCoupe fallback
  // and ONLY for LASER-DXF machines
```

### Retard Calculation Redesign

The retard (carryover) calculation has been completely redesigned from a simple "cut/not cut" binary to a 3-case per-serie analysis:

| Case | Condition | Retard | Type |
|------|-----------|--------|------|
| (a) Non coupé | dateDebutCoupe IS NULL | Full retard (entire effectiveCuttingTime) | non_coupe |
| (b) Début après shift | dateDebutCoupe > shiftEnd | Full retard (entire effectiveCuttingTime) | debut_apres_shift |
| (c) Partiel | dateFinCoupe > shiftEnd | Partial retard (dateFinCoupe - shiftEnd) | partiel |

New fields in chargeSummary response:
- `partialRetardCount`: Number of series with partial retard
- `totalRetardMinutes`: Sum of all retard minutes across series
- Per-serie: `retardType`, `retardMinutes`, `isPartialRetard` fields

### Unknown Machine Type Fix

In `calculateShiftChargeSummary()`, when resolving machine type from `tableCoupe`:
- First attempts: lookup tableCoupe in ProductionTable → machineType
- Fallback: if not found, uses the `machine` field from CuttingRequestSerieInfo
- This prevents "Unknown" entries in the breakdown table

### UI Improvements

#### Charge Details Overlay
- **Global Indicators Summary**: 7 stat cards (Séries, Chg, SR, Ret prev, Sum, Ret→next, Cap)
- **Per Machine Type Indicators Table**: Breakdown with 12 columns and GLOBAL footer row
- **Machine type filter**: Click a machine type row in the breakdown table to filter the series list by that type. Click "Afficher tout" to clear the filter.
- **"Type Machine" column**: Replaces the plain "Machine" column with a color-coded badge showing the resolved machine type.
- **"Retard (min)" column**: Shows per-serie retard time in minutes.
- **Partial retard status**: Displayed with ◐ symbol and orange styling.
- **Formula explanation section**: Documents Chg, SR, Ret prev, Sum, Ret→next formulas and the LASER-DXF nbrCouche note.

### Gerber x2 Multiplier

For **Gerber machines**, all cutting times are multiplied by 2 because Gerber requires double the estimated time. This multiplier is applied:

- **Backend**: `PlanDeChargeService.java` in `getAggregatedCuttingTimeByMachine()`, `getAggregatedCuttingTimeWithStatus()`, and `getDetailedSeriesForShift()`
- **Frontend**: reads pre-multiplied values from backend API (no frontend multiplication needed)

```
Multiplier logic:
  If machineType == "Gerber":
      effectiveCuttingTime = effectiveCuttingTime × 2
  
  Applied after all other calculations (tempsDeCoupe, nbrCouche for LASER-DXF, etc.)
```

### SR% (Surpassant) — New Indicator

The **SR%** (Surpassant percentage) represents series that were planned in the current shift but were already worked (cut) in a previous shift.

| Column | Formula |
|--------|---------|
| SR% | (sum of sr time — minutes worked BEFORE shiftStart) / available time × 100 |

- Displayed in blue (#2196f3) in the indicator columns
- Subtracted from Sum calculation: **Sum = Ret + Chg - SR**

### Coupe Group — New Grouped Indicator

A **"Coupe"** group indicator combines Lectra and Lectra IP6 machines into a single grouped column set before the Global column.

- Background color: green (#7cb342)
- Columns: Ret | Chg | SR | Sum | Cap | Rec (same as per-type indicators)
- Calculated via `getCoupeGroupIndicator()` method

### Export Functionality

Series tables can be exported to CSV or Excel format:

- **CSV**: UTF-8 with BOM, semicolon separator (`;`), includes temps changement série column
- **Excel**: HTML table exported as `.xls` file for Excel compatibility
- Available in both Charge Details overlay and Next Shift Preparation section

### Next Shift Series Loading

The "Préparation Shift Suivant" section now includes:
- **"Charger Séries" button**: Loads detailed series for the next shift via API
- **Series table**: Shows série, placement, type machine, table coupe, temps (min), source, temps changement série, statut
- **Export buttons**: CSV and Excel export for loaded series

### White Theme

The UI has been converted from a dark theme to a white/light theme:
- Backgrounds: white (#ffffff) and light gray (#f8f9fa, #f5f5f5, #e9ecef)
- Text: dark (#222222, #333333, #555555)
- Borders: light gray (#dee2e6)
- Improved readability and consistency

### Date Serialization Fix

**Problem:** `LocalDate` and `LocalDateTime` objects stored in `Map<String, Object>` were serialized by Jackson as arrays (e.g., `[2026, 3, 14]` instead of `"2026-03-14"`) because no `JavaTimeModule` or `WRITE_DATES_AS_TIMESTAMPS=false` config was present. This caused frontend aggregation keys to never match, resulting in "all 0" indicators.

**Fix:** In `PlanDeChargeService.getSeriesForDateRange()`, all date fields are now explicitly converted to strings using `.toString()`:
- `dueDate` → `planningDate.toString()` (ISO format `"yyyy-MM-dd"`)
- `dateDebutCoupe` → `dateDebutCoupe.toString()` (ISO format `"yyyy-MM-ddTHH:mm:ss"`)
- `dateFinCoupe` → `dateFinCoupe.toString()` (ISO format `"yyyy-MM-ddTHH:mm:ss"`)

### seriesForDateRange Endpoint — Additional Fields

The `getSeriesForDateRange()` service method now uses `findSeriesForDateRange` query (instead of `findSeriesForAggregation`) to include additional fields needed by frontend:
- `serie` — serie number for display and identification
- `tableCoupe` — used for machine type resolution on frontend
- `nbrCouche` — used for LASER-DXF multiplier verification

### Load Summary — Comprehensive Indicators

`renderLoadSummary()` now shows 13 columns instead of 6:

| Column | Source | Formula |
|--------|--------|---------|
| Date | shift date | — |
| Shift | shift number | — |
| Cap | statusGrid | activeMachines / totalMachines |
| Chg (min) | aggregatedCuttingTime | sum of cutting time minutes |
| Chg % | aggregatedCuttingTime | totalCuttingTime / availableTime × 100 |
| SR (min) | aggregatedCuttingTimeWithStatus | sum of sr minutes |
| SR % | aggregatedCuttingTimeWithStatus | srTime / availableTime × 100 |
| Ret prev (min) | aggregatedCuttingTimeWithStatus (prev shift) | sum of notCut from previous shift |
| Ret prev % | aggregatedCuttingTimeWithStatus (prev shift) | retPrevTime / availableTime × 100 |
| Sum % | computed | Chg% - SR% + RetPrev% |
| Ret→next (min) | aggregatedCuttingTimeWithStatus | sum of notCut from current shift |
| Ret→next % | aggregatedCuttingTimeWithStatus | retThisTime / availableTime × 100 |
| Actions | — | Click to open Charge Details |

Color coding for percentage columns: red if >100%, orange if >80%, green otherwise.

### Charge Details — Per Machine Type Indicators

`renderChargeDetails()` now shows a comprehensive indicator layout:

1. **Global Indicators Summary**: 7 stat cards showing Séries, Chg (min+%), SR (min+%), Ret prev (min+%), Sum%, Ret→next (min+%), Cap
2. **Per Machine Type Table**: 12 columns (Type, Séries, Cap, Chg min/%, SR min/%, Ret prev min/%, Sum%, Ret→next min/%) with a GLOBAL footer row
3. **Detailed Series List**: Simplified 10 columns (Serie, Placement, Machine, Type Machine, Table Coupe, Temps (min), Source, Statut, Retard, SR)
4. **Formula Explanation Section**: Documents Chg, SR, Ret prev, Sum, Ret→next formulas

---

## Usage Example

1. Navigate to Process > Plan de Charge
2. Select date range (e.g., 2026-02-01 to 2026-02-07)
3. Click "Rechercher"
4. View machine status grid with color-coded cells
5. Check indicator columns (Ret/Chg/SR/Sum/Cap/Rec) per machine type
6. View "Coupe" group (Lectra + Lectra IP6 combined) indicators
7. Current shift row is highlighted with an orange border
8. Click on a cell to update machine status (opens modal)
9. View load summary for each shift (click row for detailed series)
10. Click "Recalculer" to update load calculations
11. In "Préparation Shift Suivant", click "Charger Séries" to view and export next shift series
12. Use Export CSV/Excel buttons on series tables to download data
