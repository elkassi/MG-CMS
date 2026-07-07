# Advanced Ordonnancement — Micro Production Planning

**Feature:** Advanced Ordonnancement (Dispatching & Scheduling)  
**Module:** Process  
**Access:** ROLE_PROCESS  
**Author:** Mouad El Ghazi  
**Created:** February 2026  
**Last Updated:** June 2026

---

## Table of Contents
1. [Overview](#overview)
2. [Concepts](#concepts)
3. [Data Model](#data-model)
4. [Four View Modes](#four-view-modes)
5. [Auto-Dispatch Algorithm](#auto-dispatch-algorithm)
6. [Sequence Detail & Management](#sequence-detail--management)
7. [API Endpoints](#api-endpoints)
8. [Frontend Component](#frontend-component)
9. [Configuration Constants](#configuration-constants)
10. [Performance Optimization](#performance-optimization)
11. [Shift Overflow Detection](#shift-overflow-detection)

---

## Overview

The Advanced Ordonnancement is a **micro production planning** tool that extends Plan de Charge (macro planning) into real-time dispatching. It provides:

- **4 view modes:** Réel (table physical view), Planifié Coupe (cutting timeline), Planifié Matelassage (spreading timeline), Table de données (sortable/filterable data table)
- **Collision-free Auto-Dispatch** with LASER-DXF parallel support (spreading + cutting simultaneous)
- **Sequence color coding**: each sequence gets a unique background color on Gantt bars
- **Sequence detail modal**: click a sequence to see all its series with all dates (real + estimated)
- **15-minute time ruler ticks** always visible, with configurable zoom (8h/4h/2h/1h/30min), default 2h
- **"Maintenant" button**: auto-scrolls and centers on the red now-line (with render timing fix)
- **Multi-select machine filter**: select multiple machines via checkbox dropdown
- **View mode select dropdown**: compact dropdown instead of buttons
- **Data table view**: all series with sort-by-header and filter-by-column, shows estimated dates when real dates are null
- **Physical table constraints** (length limits) in the Réel view
- **Manual override** to assign series/zones to different machines
- **Save next 3 series** per machine into a database entity for operators
- **Smart data loading**: only loads relevant sequences (12h activity window + current/previous/before-previous shift)
- **Incremental refresh**: checks for changes every 5 minutes
- **Manual sequence addition**: add sequences by ID with removable tags
- **Shift overflow detection**: overflow % calculated against total capacity of all active machines
- **Machine zone reassignment**: change a machine's zone directly from the UI
- **White/light theme**: clean white background for production screens

### Relationship to Plan de Charge
```
Plan de Charge (Macro)          →  Advanced Ordonnancement (Micro)
────────────────────────        ────────────────────────────────
Per-shift aggregated view        Per-minute timeline per machine
"What is the total charge?"     "Which serie goes where and when?"
Shift-level retard calculation   Collision-free scheduling with all 4 dates
Machine type grouping            Individual machine columns (filterable)
                                 Physical table position view (Réel)
```

---

## Concepts

### Series Lifecycle
```
CuttingRequest (Sequence)
  └── CuttingRequestSerie (Serie)
       ├── statusMatelassage: Waiting → In progress → Complete
       ├── statusCoupe:       Waiting → In progress → Complete
       └── Physical flow:
            1. Spreading (matelassage) on tableMatelassage
            2. Cutting (coupe) on tableCoupe
            3. Quality control
            4. Box completion
```

### 4 Date Fields Per Serie
Each serie has 4 date fields representing the full production timeline:
```
dateDebutMatelassage → dateFinMatelassage → dateDebutCoupe → dateFinCoupe
```
If a date is null, the system **estimates** it using the Auto-Dispatch algorithm. Estimated dates are shown in the UI with a `*` or `(est.)` marker.

### Table Physical Constraint
Each `ProductionTable` has a physical `tableLength` (default: 14 metres). At any point in time, the sum of `longueur` of all series physically present on the table must not exceed this limit.

### Occupied Space Calculation
```
For a machine M at time T:
  occupiedLength = 0
  
  For each serie assigned to M (tableCoupe = M.nom):
    if serie.statusCoupe == "In progress":
      elapsed = T - dateDebutCoupe
      progress = elapsed / estimatedCuttingTime
      remainingLength = serie.longueur * (1 - progress)
      occupiedLength += remainingLength
      
    else if serie.statusCoupe == "Waiting" AND serie.statusMatelassage == "Complete":
      occupiedLength += serie.longueur
      
  availableLength = M.tableLength - occupiedLength
```

### Box Completion Goal
Each `CuttingRequest` (sequence) generates a set of boxes. Boxes are only complete when **all series** of the sequence have `statusCoupe = "Complete"`.
- **Goal:** Minimize concurrent incomplete boxes by finishing sequences before starting new ones

### Zone Constraints
- Each `ProductionTable` belongs to a `Zone`
- **Lectra, Lectra IP6:** Zone-restricted (+50 score bonus for same zone)
- **LASER-DXF, GERBER, DIR:** No zone restriction, work in parallel

### Spreading Time Estimation
```
spreadingDuration = (longueur × nbrCouche × COEF_SPREADING_PER_METRE) + COEF_SETUP_TIME
Where:
  COEF_SPREADING_PER_METRE = 0.5 min per metre per layer
  COEF_SETUP_TIME = 2 minutes
```

### Cutting Time Estimation
Priority: Validated_Cutting_time_Timing_Model > Real_Cutting_time_Timing_Model > tempsDeCoupe.
For LASER-DXF: multiply by nbrCouche.

---

## Data Model

### Entity: MachineQueue
```sql
CREATE TABLE machine_queue (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_nom VARCHAR(50) NOT NULL,
    queue_position INT NOT NULL,
    serie VARCHAR(100) NOT NULL,
    sequence_id VARCHAR(100),
    part_number_material VARCHAR(200),
    longueur DECIMAL(10,2),
    estimated_cutting_time DECIMAL(10,2),
    estimated_start_time DATETIME,
    estimated_end_time DATETIME,
    assigned_by VARCHAR(100),
    assigned_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT UK_machine_queue UNIQUE (machine_nom, queue_position)
);
```

### CuttingRequestData (Sequence Header)
Used by `getSequenceDetail()` to load sequence-level info:
- `dueDate`, `dueShift`, `zone`, `projet`, `modele`, `definition`, `planningDate`, `createdAt`

---

## Four View Modes

### 1. Réel (Real View) — Physical Table Position
Shows machines grouped by zone with a **physical table visualization**. Only shows series currently active on the table:
- **SPREADING** (far left): cloth being spread, full length
- **READY_TO_CUT** (middle): matelassage done, waiting for cut, full length
- **CUTTING** (far right): currently being cut, shows **remaining length** = `longueur × ((estFinCoupe − now) / estimatedCuttingTime)`

```
[Machine Header: Status dot + Name + Status + lastFinCoupe estimated]  [Zone btn]
[====== Table Bar 14m ====================================================]
  [SPREADING 3.2m]  [READY_TO_CUT 5.1m]  [CUTTING 2.1m remaining]
[Ruler: 0m ... 7m ... 14m]
[Total occupé: 10.4m / 14m]
[Detail rows per serie: ID | Status | Ref | Length info | Time | Seq link]
```

Features:
- Physical positions on the bar reflect the actual table layout (SPREADING left, CUTTING right)
- CUTTING block width shrinks in real-time as the serie is cut (remaining proportion)
- Total occupied length shown below the ruler
- **Last estimated `dateFinCoupe`** shown in the machine header (blue text)
- Click a serie to open detail modal
- Zones collapsible with chevron

### 2. Planifié Coupe (Cutting Timeline)
Gantt-like timeline using **strictly coupe dates** as block edges:
- Block start = `dateDebutCoupe` (real) or `estimatedDebutCoupe`
- Block end = `dateFinCoupe` (real) or `estimatedFinCoupe`
- Only blocks that have coupe date fields are rendered (no matelassage-date fallback)
- Blocks shown per machine determined by `tableCoupe` or `assignedMachine`
- **Sequence color as block background**
- 15-minute ruler ticks always visible with zoom control
- Red now-line with “Maintenant” button to center
- Machine occupancy bar + **last estimated `dateFinCoupe`** (blue text)
- **Sticky columns**: Machine, État, Occupation stick when scrolling horizontally

### 3. Planifié Matelassage (Spreading Timeline)
Same Gantt layout but using **strictly matelassage dates**:
- Block start = `dateDebutMatelassage` (real) or `estimatedDebutMatelassage`
- Block end = `dateFinMatelassage` (real) or `estimatedFinMatelassage`
- Only blocks that have matelassage date fields are rendered (no coupe-date fallback)
- Blocks shown per machine determined by `tableMatelassage` or `assignedMachine`
- Same **sticky columns** and machine-level **lastFinCoupe** display as Planifié Coupe

### 4. Table de données (Data Table View)
Full data table showing **all series** from all sources (timeline, unassigned, dispatched):
```
| Série | Séquence | Réf. Tissu | Long. | Couches | Statut | Table Mat. | Table Coupe | Début Mat. | Fin Mat. | Temps Mat. | Début Coupe | Fin Coupe | Temps Coupe |
|-------|----------|------------|-------|---------|--------|------------|-------------|------------|----------|------------|-------------|-----------|-------------|
```
- **Temps Mat.** (`estimatedSpreadingTime`): estimated spreading duration in minutes (formula-based)
- **Temps Coupe** (`estimatedCuttingTime`): validated/real cutting time priority — TimingModel > tempsDeCoupe

Features:
- **Sortable headers**: click any column header to sort ascending/descending
- **Date sorting with estimated fallback**: when sorting by date columns, estimated dates are used when real dates are null, so estimated entries sort by their datetime value instead of appearing at top/bottom
- **Filter row**: input row below headers for per-column text filtering
- **Machine filter applied**: the multi-select machine filter on top also filters the data table rows by `tableCoupe` or `tableMatelassage`
- Shows `tableMatelassage` and `tableCoupe` columns (with estimated values from dispatch marked as `(rec.)`)
- If real dates are null, shows estimated dates (marked with `*` and italic blue)
- Click a row to open the serie detail modal
- Sequence column has colored left border matching sequence color

### View Mode Selector
Compact `<select>` dropdown instead of buttons:
```
[Réel ▼]  →  Réel | Planifié Coupe | Planifié Matelassage | Table de données
```

### Time Ruler (Planifié views only)
- **15-minute interval ticks** always visible on the ruler (pixel-based widths for accurate alignment)
- Hour ticks are bold, quarter-hour ticks are lighter
- Zoom controls change how wide each tick appears (8h/4h/2h/1h/30min)
- **Default: 2h** selected on load
- "Maintenant" button scrolls to center on the red now-line (finds the DOM element directly, with 150ms render delay fix)

### Multi-Select Machine Filter
Dropdown with checkboxes allowing selection of **multiple machines** simultaneously.
Applied to all views: Réel, Planifié Coupe, Planifié Matelassage, **and Table de données** (filters rows by `tableCoupe` or `tableMatelassage`):
```
[Machines: 3 sélectionnée(s) ▼]
  [Toutes] [Sélect. tout]
  ☑ CNC-01
  ☑ CNC-02
  ☐ CNC-03
  ☑ CNC-04
```
- "Toutes" button clears selection (shows all)
- "Sélect. tout" checks all machines

---

## Collision-Free Timeline Estimation

### Purpose
The `buildTimelineBlocks()` method estimates all future dates with **per-machine collision detection**, ensuring no two series overlap in cutting time on the same machine. This applies to the regular timeline/table views (not just Auto-Dispatch).

### Algorithm
1. **Phase 1 — Register occupied intervals**: Scan COMPLETED and CUTTING series to build per-machine occupation maps for both cutting and spreading.
2. **Phase 2 — CUTTING blocks**: Estimate `dateFinCoupe = dateDebutCoupe + estimatedCuttingTime`. The `estimatedFinCoupe` field is exposed to the table view. The interval is registered for collision tracking.
3. **Phase 3 — READY_TO_CUT** (sorted by `dateDebutMatelassage`, oldest first):
   - Find the earliest non-colliding cutting slot on the assigned machine using `findEarliestSlot()`.
   - Each new slot is registered immediately → subsequent series on the same machine are pushed after it.
   - Sets `estimatedDebutCoupe` and `estimatedFinCoupe` on the block.
4. **Phase 4 — SPREADING**: Estimate spreading end, then find collision-free coupe slot after spreading finishes.
5. **Phase 5 — WAITING (Two-Pass)**:
   - **Pass 1 (Coupe)**: Infer machine (active machines only, status "M"), SCG ordering, schedule collision-free cutting dates.
   - **Pass 2 (Matelassage)**: Sort by estimated `dateDebutCoupe`, chain matelassage on the same table using `findEarliestSlot(spreadingOccupied, ...)`.
   - Only machines with status **"M"** are eligible for WAITING dispatch.

### 3a. READY_TO_CUT Machine Resolution Rule
- If `tableCoupe` is **not null and not empty** → use `tableCoupe` (serie has been physically moved to a different cutting machine)
- Else if `tableMatelassage` is **not null** → use `tableMatelassage` (serie stays on the same table where it was spread — most common case)
- `block.tableCoupe` is always set to the resolved machine so it appears in the Planifié Coupe view

### WAITING Series: Machine Inference
WAITING series have `tableCoupe = null` and `tableMatelassage = null` (nothing started yet).
Machine is inferred using **sequence affinity** — **only active machines (status "M")** are eligible:
1. `s.tableCoupe` → use directly (rare, only if manually assigned)
2. `s.tableMatelassage` → use directly
3. **Sequence affinity**: look at other series of the same sequence that are COMPLETED/CUTTING/READY_TO_CUT/SPREADING → use their machine
4. **Least-loaded active machine**: if no affinity, assign to the active machine with the earliest available cutting slot

Once a machine is inferred for a WAITING serie, it becomes the affinity for sibling series of the same sequence (tracked via `seqMachineAffinity` map).

### WAITING Series: Optimal Ordering (Sequence Compaction Greedy — SCG)

**Metric to minimize**: `max(m) × median(m) × variance(m)` where `m(seq) = sequence_span / num_series`

**Ordering strategy (currently implemented: SCG)**:

| Priority | Criterion | Rationale |
|----------|-----------|-----------|
| 1 | Higher done fraction first | Close nearly-completed sequences quickly → reduces `max(m)` |
| 2 | Fewer remaining WAITING first | Close them in fewer steps → reduces overall latency |
| 3 | Same sequence grouped together | Minimizes span per sequence → directly reduces `m(seq)` |
| 4 | Shorter cutting time first within group | Minimizes wasted time within a sequence's cutting window |

### Alternative Scheduling Algorithms

Six candidate algorithms for WAITING ordering, each with different tradeoffs:

| # | Algorithm | Description | Strengths | Weaknesses |
|---|-----------|-------------|-----------|------------|
| 1 | **SCG** (Sequence Compaction Greedy) ✅ | Group by sequence, close-to-done first | Minimizes max(span/boxes), good for variance | May not optimize median if many sequences |
| 2 | **SPT** (Shortest Processing Time) | Shortest cutting time first globally | Minimizes average completion time | Ignores sequence grouping → increases span |
| 3 | **LPT** (Longest Processing Time) | Longest cutting time first globally | Balances multi-machine load | On single machine, delays short jobs |
| 4 | **EDF** (Earliest Due Date) | Sort by sequence due date ascending | Meets deadlines, constraint-aware | Ignores sequence completion proximity |
| 5 | **CR** (Critical Ratio) | Sort by `(dueDate - now) / remaining_time` | Urgency-aware, adapts dynamically | Requires accurate due dates |
| 6 | **MLS** (Multi-start Local Search) | Try SCG + SPT + LPT + random, evaluate metric, keep best. Optionally do pairwise swaps. | Finds near-optimal solution | Higher computation time (O(n² × S)) |

**Mathematical formulation** of the composite metric:

$$
\text{Objective} = \max_{s \in S}\left(\frac{T_s}{N_s}\right) \times \text{median}_{s \in S}\left(\frac{T_s}{N_s}\right) \times \text{Var}_{s \in S}\left(\frac{T_s}{N_s}\right)
$$

Where:
- $T_s$ = `max(dateFinCoupe) - min(dateDebutCoupe)` for all series in sequence $s$
- $N_s$ = number of series in sequence $s$
- $S$ = set of all sequences with at least one assigned serie

**To switch algorithms**: modify the `waitingList.sort(...)` comparator in `buildTimelineBlocks()`.

### Key Rules
- `dateDebutCoupe` ≥ `now` (always)
- `dateDebutCoupe` ≥ `dateFinMatelassage` (sequential constraint, except LASER-DXF parallel)
- No two cutting intervals overlap on the same machine
- WAITING estimation continues from last READY_TO_CUT/SPREADING end on the machine (never goes backward)
- **Only active machines** (status "M") are eligible for WAITING dispatch — machines with status P, R, etc. are excluded
- `findEarliestSlot(occupied, earliest, durationMs)` — finds the first gap in occupied intervals where the duration fits

### WAITING Two-Pass Algorithm Detail
```
PASS 1 — COUPE:
  1. Build activeMachines = { m ∈ allMachines | machineStatuses[m] == "M" }
  2. Order WAITING by SCG (done fraction → remaining count → sequence → cutting time)
  3. For each WAITING serie:
     a. Infer machine: tableCoupe > tableMatelassage > seqAffinity > least-loaded active
     b. Find collision-free cutting slot: findEarliestSlot(cuttingOccupied[machine], now, cuttingMs)
     c. Register interval → next series on same machine starts after
     d. Store [coupeStart, coupeEnd] and machine for Pass 2

PASS 2 — MATELASSAGE:
  1. Sort WAITING by estimated coupeStart ascending
  2. For each WAITING serie (in coupeStart order):
     a. If LASER-DXF: spreadStart = coupeStart (parallel)
     b. Else: spreadStart = findEarliestSlot(spreadingOccupied[machine], now, spreadMs)
     c. Register spreading interval
     d. Build block with all 6 estimated dates
```

### Per-Machine lastFinCoupe
After building all timeline blocks, the backend computes the **last estimated `dateFinCoupe`** for each machine.
This is the maximum of all `estimatedFinCoupe` or `dateFinCoupe` values across all blocks assigned to that machine.
It is injected into the `machinesByZone` response and displayed below the occupancy bar in Planifié views.

### Block Fields Added
| Field | Status | Description |
|-------|--------|-------------|
| `estimatedFinCoupe` | CUTTING | Estimated end of cutting (shown in table as `*`) |
| `estimatedDebutCoupe` | READY_TO_CUT, SPREADING, WAITING | Collision-free estimated start of cutting |
| `estimatedFinCoupe` | READY_TO_CUT, SPREADING, WAITING | Collision-free estimated end of cutting |
| `estimatedDebutMatelassage` | WAITING | Estimated start of spreading (scheduled or backward-calculated) |
| `estimatedFinMatelassage` | WAITING | Estimated end of spreading |
| `estimatedSpreadingTime` | ALL | Estimated spreading duration in minutes |
| `assignedMachine` | WAITING | Inferred machine from sequence affinity or least-loaded active |
| `tableCoupe` | WAITING | Set to the inferred machine (same as `assignedMachine`) |
| `tableMatelassage` | WAITING | Set to the inferred machine (same table for spreading) |

### Gantt Bar Rendering
Each bar in Planifié views:
- Uses **sequence color** as background
- Positioned using coupe dates (Planifié Coupe) or matelassage dates (Planifié Matelassage)
- Only rendered when the relevant date pair is available — no fallback to `blockStart`
- **No PLANNED bars**: dispatch results are informational only (shown in the Dispatch panel), not drawn on the Gantt

---

## Auto-Dispatch Algorithm

### Purpose
Estimates all 4 dates for unassigned series with **zero collision guarantee**:
- No two spreading operations overlap on the same table
- No two cutting operations overlap on the same machine
- All estimated dates ≥ now
- Order always respected: dateDebutMatelassage → dateFinMatelassage → dateDebutCoupe → dateFinCoupe
- Optimal sequence-affinity scoring
- **Started sequences prioritized**: sequences with some series already in progress are dispatched first to finish them quickly

### Machine Selection Rules
1. If serie already has `tableCoupe` → not dispatched (machine already determined)
2. If serie has `tableMatelassage` and is NOT Waiting → use `tableMatelassage` as cutting machine (serie stays on same table)
3. If serie is fully Waiting (no tableMatelassage) → search all active machines for best fit

### Algorithm: `autoDispatch()`

```
Input:
  - All series with statusCoupe != "Complete"
  - All active machines (status "M")
  - TimingModel cutting times

Process:
  1. Load all relevant series
  2. Build occupied interval maps per machine:
     - spreadingOccupied: Map<tableName, List<[startMs, endMs]>>
     - cuttingOccupied: Map<tableName, List<[startMs, endMs]>>
  3. Register all existing (non-null) date intervals as occupied
  4. Sort unassigned series by: **started sequences first**, then planningDate ASC, sequence ASC
  5. For each unassigned serie:
     a. Compute spreadDuration, cutDuration
     b. For each eligible machine (availableLength >= longueur):
        - If LASER-DXF machine: spreading in parallel with cutting
          → spreadStart = cutStart (simultaneous)
          → Only cutting slot matters for collision detection
        - Else: Find earliest non-colliding spread slot
          → spreadEnd = spreadStart + spreadDuration
          → Find earliest non-colliding cut slot AFTER spreadEnd
        - cutEnd = cutStart + cutDuration
        - Total span = cutEnd - now
        - Score = sequenceAffinityBonus - totalSpan
     c. Pick machine with best (highest) score
     d. Register new intervals as occupied
     e. Emit dispatched record with all 4 estimated dates

findEarliestSlot(occupied, earliest, duration):
  Sort occupied intervals by start time
  candidate = max(earliest, now)
  For each interval [s, e] in occupied:
    if candidate + duration <= s: return candidate  // fits before this interval
    if candidate < e: candidate = e                 // shift past this interval
  return candidate  // fits after all intervals
```

### Output
```json
{
  "dispatched": [
    {
      "serie": "S001",
      "sequence": "SEQ-123",
      "recommendedMachine": "CNC-01",
      "estimatedDebutMatelassage": "2026-02-21T08:30:00",
      "estimatedFinMatelassage": "2026-02-21T09:15:00",
      "estimatedDebutCoupe": "2026-02-21T09:15:00",
      "estimatedFinCoupe": "2026-02-21T09:45:00",
      "spreadTable": "CNC-01",
      "score": 85.3
    }
  ],
  "totalDispatched": 12,
  "totalUnassigned": 5,
  "generatedAt": "2026-02-21T06:15:00"
}
```

### Sequence Affinity
When a serie's sequence already has other series assigned to the same machine (in-progress OR previously dispatched in same batch), a **-60s affinity bonus** is applied to that machine's effective start time, keeping sequences together. The dispatch also tracks which machine was chosen for each sequence (`dispatchedMachineBySequence` map) to reinforce grouping.

### LASER-DXF Parallel Handling
For machines identified as LASER-DXF (from `ProductionTable.laserDxf` flag):
- **Cutting time**: base cutting time × nbrCouche (each layer cut individually)
- **Spreading**: happens **in parallel** with cutting (not sequential)
- In auto-dispatch: `estimatedDebutMatelassage = estimatedDebutCoupe` (same start time)
- Only the cutting interval is checked for collision detection
- This reflects the physical reality where LASER-DXF machines cut and spread simultaneously

---

## Sequence Detail & Management

### Loading All Series of a Sequence
When sequences are found (via timeline or manual addition), **all series** of each sequence are loaded. This ensures visibility into:
- All 4 date fields (real or estimated) for every serie
- Status distribution (completed, cutting, spreading, waiting)
- Machine assignments

### Sequence Detail Modal
Click any sequence link to open a full detail view:
```
┌─────────────────────────────────────────────────────────┐
│ Séquence: SEQ-123                      [Retirer]        │
│─────────────────────────────────────────────────────────│
│ Due Date: 2026-02-21    Due Shift: 1    Zone: Zone A    │
│ Projet: PRJ-01          Modèle: MDL-X                  │
│─────────────────────────────────────────────────────────│
│ [4 terminées] [2 en coupe] [1 matelassage] [3 attente] │
│ Temps restant coupe: 2h30  Temps restant mat.: 1h15   │
│ Fin estimée: 21/02 15:30                               │
│─────────────────────────────────────────────────────────│
│ Série | Réf. | Long | Table Mat. | Table Coupe |      │
│       | Couches | Mat. | Coupe | Déb.M | Fin M |     │
│       | Déb.C | Fin C |                              │
│ S001  | RF-A | 4.2m | CNC-01 | CNC-01 | ...         │
│ S002  | RF-B | 3.1m | CNC-02 | -      | ...         │
│ * = date estimée                                        │
└─────────────────────────────────────────────────────────┘
```

Features:
- **Total time display**: shows remaining cutting time, remaining spreading time, and estimated completion date for the whole sequence
- **Table columns**: Série, Réf., Long., Couches, Status Mat., Status Coupe, **Table Mat.**, **Table Coupe**, Début Mat., Fin Mat., Début Coupe, Fin Coupe
- Estimated dates marked with `*`
- Backend provides: `totalRemainingCuttingTime`, `totalRemainingSpreadingTime`, `estimatedCompletion`

### Sequence Colors
Each unique sequence gets a color from a 20-color palette:
```javascript
const SEQ_COLORS = [
    '#e6194B', '#3cb44b', '#4363d8', '#f58231', '#911eb4',
    '#42d4f4', '#f032e6', '#bfef45', '#fabed4', '#469990',
    '#dcbeff', '#9A6324', '#800000', '#aaffc3', '#808000',
    '#ffd8b1', '#000075', '#a9a9a9', '#e6beff', '#fffac8'
];
```
Colors appear as:
- Left border on Gantt bars (Planifié views)
- Left border on unassigned group headers
- Left border on dispatch group headers
- Badge in serie detail modal
- Left accent on sequence detail modal title

### Remove Sequence
From the sequence detail modal, users can remove an additional sequence (ones added via search bar). This removes the tag and reloads the timeline without that sequence.

---

## API Endpoints

### Controller: `/api/ordonnancement`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/timeline?hoursBack=12&hoursForward=12&additionalSequences=SEQ1,SEQ2` | Full timeline data with sequenceInfo map |
| GET | `/timeline/refresh?sinceMinutes=5&hoursBack=12&hoursForward=12` | Incremental refresh |
| GET | `/timeline/sequence/{sequence}` | Validate and load a specific sequence |
| GET | `/recommendation` | **Auto-Dispatch** — collision-free scheduling with all 4 estimated dates |
| GET | `/sequence/detail/{sequence}` | Full sequence detail: header + all series with all dates |
| GET | `/machineState` | Current state of all machines |
| GET | `/zones` | All available zone names |
| POST | `/assignSerie` | Manually assign a serie to a machine |
| POST | `/assignSequenceZone` | Move a full sequence to a different zone |
| POST | `/changeMachineZone` | Change a machine's zone assignment |
| POST | `/saveQueue` | Save next 3 series per machine |
| GET | `/queue/{machineNom}` | Get saved queue for a machine |
| GET | `/queue/all` | Get saved queues for all machines |

### New Endpoints (v2)

#### `GET /recommendation` (Updated)
Returns collision-free auto-dispatch result instead of simple recommendation list:
```json
{
  "dispatched": [...],
  "totalDispatched": 12,
  "totalUnassigned": 5,
  "generatedAt": "2026-02-21T06:15:00"
}
```

#### `GET /sequence/detail/{sequence}`
Returns full sequence information:
```json
{
  "found": true,
  "sequence": "SEQ-123",
  "dueDate": "2026-02-21",
  "dueShift": "1",
  "zone": "Zone A",
  "projet": "PRJ-01",
  "modele": "MDL-X",
  "planningDate": "2026-02-20",
  "completedCount": 4,
  "cuttingCount": 2,
  "spreadingCount": 1,
  "waitingCount": 3,
  "totalRemainingCuttingTime": 150.0,
  "totalRemainingSpreadingTime": 75.0,
  "estimatedCompletion": "2026-02-21T15:30:00",
  "series": [
    {
      "serie": "S001",
      "partNumberMaterial": "RF-A",
      "longueur": 4.2,
      "nbrCouche": 8,
      "statusMatelassage": "Complete",
      "statusCoupe": "In progress",
      "dateDebutMatelassage": "2026-02-21T07:00:00",
      "dateFinMatelassage": "2026-02-21T07:45:00",
      "dateDebutCoupe": "2026-02-21T08:00:00",
      "dateFinCoupe": null,
      "estimatedDebutMatelassage": null,
      "estimatedFinMatelassage": null,
      "estimatedDebutCoupe": null,
      "estimatedFinCoupe": "2026-02-21T08:30:00",
      "tableCoupe": "CNC-01",
      "tableMatelassage": "CNC-01"
    }
  ]
}
```

### Timeline Response (Updated)
The `/timeline` response now includes a `sequenceInfo` map:
```json
{
  "machinesByZone": {...},
  "timelineBlocks": [...],
  "unassignedSeries": [...],
  "metrics": {...},
  "shiftOverflow": {...},
  "sequenceInfo": {
    "SEQ-123": { "zone": "Zone A", "dueDate": "2026-02-21", "dueShift": "1" },
    "SEQ-456": { "zone": "Zone B", "dueDate": "2026-02-22", "dueShift": "2" }
  },
  "currentTime": "...",
  "windowStart": "...",
  "windowEnd": "..."
}
```

---

## Frontend Component

### Route
```javascript
<SecuredRoute exact path="/advancedOrdonnancement" component={AdvancedOrdonnancement} />
```

### Component Structure
```
AdvancedOrdonnancement.js
├── Header: "Ordonnancement Avancé"
├── Controls Bar
│   ├── Actualiser button
│   ├── View mode <select>: Réel | Planifié Coupe | Planifié Matelassage | Table de données
│   ├── Auto-Dispatch button
│   ├── Sauvegarder File button
│   ├── File d'attente toggle button
│   ├── Interval selector (8h/4h/2h/1h/30min) — Planifié views only
│   ├── Maintenant button — Planifié views only
│   └── Multi-select machine filter (checkbox dropdown)
├── Sequence Search Bar
│   ├── Text input + "Ajouter" button
│   └── Sequence tags (colored, clickable, removable)
├── Metrics Bar (5 cards)
├── Shift Overflow Panel
├── Main Area
│   ├── Timeline / View Area (left, wide)
│   │   ├── [Réel] → renderReelView() — table position visual
│   │   ├── [Planifié Coupe] → renderPlanifieView() — Gantt with coupe dates
│   │   ├── [Planifié Matel.] → renderPlanifieView() — Gantt with matel. dates
│   │   └── [Table de données] → renderDataTableView() — sortable/filterable table
│   └── Unassigned Panel (right) — collapsible, hidden by default, toggle on header click
├── Dispatch Panel — grouped by sequence with estimated dates table
├── Queue Section — per-machine queue summary
├── Legend
└── Modals
    ├── Serie detail (block click)
    ├── Sequence detail (sequence click) — all series with all 4 dates
    ├── Assign dialog
    └── Zone change dialog
```

### State Variables
```javascript
{
  // View mode
  viewMode: 'reel' | 'planifie_coupe' | 'planifie_matelassage' | 'table_donnees',
  
  // Timeline interval (minutes)
  timelineInterval: 120, // 2h default, options: 480/240/120/60/30
  
  // Collapsible unassigned panel
  showUnassignedContent: false, // hidden by default, toggle on header click
  
  // Multi-select machine filter
  filterMachines: [], // empty = all machines shown (applied to Réel, Planifié, Table de données)
  showMachineFilterDropdown: false,
  
  // Data table view
  tableSortField: 'serie',
  tableSortDir: 'asc',
  tableFilters: {}, // { columnKey: filterText }
  
  // Auto-dispatch results
  dispatchResult: null | { dispatched: [...], ... },
  loadingDispatch: false,
  
  // Sequence management
  additionalSequences: [],
  sequenceColorMap: {}, // { sequence: colorHex }
  
  // Sequence detail
  showSequenceDetail: false,
  sequenceDetailData: null,
  
  // Collapsed zones (Réel view)
  collapsedZones: {},
  
  // ... (timeline, queue, modal, filter states)
}
```

---

## Configuration Constants

| Constant | Default | Description |
|----------|---------|-------------|
| `TABLE_LENGTH_DEFAULT` | 14.0 | Default table length in metres |
| `COEF_SPREADING_PER_METRE` | 0.5 | Minutes per metre per layer for spreading |
| `COEF_SETUP_TIME` | 2.0 | Minutes setup before starting spreading |
| `BOXES_PER_SEQUENCE` | 16 | Incomplete boxes per concurrent sequence |
| `SHIFT_DURATION_MINUTES` | 460 | Effective work time per shift |
| `SQL_BATCH_SIZE` | 2000 | Max parameters per SQL IN clause |
| `TIMELINE_HOURS_BACK` | 12 | Default hours to show in the past |
| `TIMELINE_HOURS_FORWARD` | 12 | Default hours to show in the future |

---

## File Structure

```
src/main/java/com/lear/MGCMS/
├── domain/
│   ├── MachineQueue.java
│   └── ProductionTable.java                 (+tableLength)
├── repositories/
│   ├── MachineQueueRepository.java
│   ├── ProductionTableRepository.java       (+findAllMachinesLight, +findLaserDxfMachineNames)
│   ├── EtatMachineHistoriqueRepository.java (+findAllCurrentStatuses)
│   └── CuttingRequest/data/
│       ├── CuttingRequestSerieDataRepository.java (+lightweight projections)
│       └── CuttingRequestDataRepository.java      (+findSequenceInfoLight, +findBySequence)
├── services/
│   ├── OrdonnancementService.java           (autoDispatch, getSequenceDetail, getTimelineData)
│   └── EtatMachineHistoriqueService.java    (+getAllCurrentStatusCodes)
└── controller/
    └── OrdonnancementController.java        (/recommendation, /sequence/detail)

src/main/js/
├── components/Layout/
│   └── AdvancedOrdonnancement.js             (4 view modes, dispatch, sequence detail, data table)
└── styles/
    └── AdvancedOrdonnancement.scss            (all ordonnancement styles: views, multi-filter, data table)
```

---

## Performance Optimization

### Smart Data Loading
Only loads series from **relevant sequences**:
1. Sequences with activity in 12h bounded window
2. Sequences due for current/previous/before-previous shifts  
3. Manually added sequences (additionalSequences param)

### Lightweight Projections
19-column JPQL projections instead of full 50+ column entities with eager joins.

### Incremental Refresh
5-minute cycle: counts changes first → if 0, returns `noChanges: true` (instant).

### SQL Server Batching
All IN-clause queries batched in chunks of 2000 (SQL Server limit: 2100).

### Batch Operations
- Machine status: 1 query instead of N
- TimingModel cutting times: 1 query instead of N
- Sequence info: batch-loaded for all unique sequences in timeline

### Frontend HTTP Deduplication
All API methods have loading-state guards to prevent concurrent duplicate requests.

---

## Shift Overflow Detection

Determines whether current shift's remaining work can complete before shift end.

### Overflow Percentage Formula
The overflow percentage is calculated against the **total capacity of all active machines** in the next shift:
```
nextShiftCapacity = SHIFT_DURATION_MINUTES × activeMachineCount
overflowPercentage = (overflowMinutes / nextShiftCapacity) × 100
```
This matches the Plan de Charge calculation where capacity is the sum of all machine-minutes available.

### Response Format
```json
{
  "currentShiftDate": "2026-02-21",
  "currentShiftNumber": "1",
  "shiftEnd": "2026-02-21T14:00:00",
  "minutesUntilShiftEnd": 187,
  "totalWorkMinutes": 215,
  "canFinishBeforeShiftEnd": false,
  "overflowMinutes": 28,
  "overflowPercentageNextShift": 0.6
}
```

Frontend shows green panel (✓ OK) or red panel (⚠ overflow with minutes and % of next shift).
