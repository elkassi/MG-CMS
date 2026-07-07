# MG-CMS Macro Ordonnancement Plan V3

## Production Scheduling Optimization System for LEAR Corporation Cutting Department

> **Author:** Development Team  
> **Version:** 3.0  
> **Date:** January 2026  
> **Status:** Implementation Phase  
> **Component:** OrdonnancementV3.js

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State Analysis](#2-current-state-analysis)
3. [System Objectives](#3-system-objectives)
4. [Multi-Zone Architecture](#4-multi-zone-architecture)
5. [Data Model Overview](#5-data-model-overview)
6. [Algorithm Design](#6-algorithm-design)
7. [Spreading (Matelassage) Scheduling](#7-spreading-matelassage-scheduling)
8. [Machine Load Balancing](#8-machine-load-balancing)
9. [Frontend Components](#9-frontend-components)
10. [Material Availability System](#10-material-availability-system)
11. [KPI Indicators & Optimization Metrics](#11-kpi-indicators--optimization-metrics)
12. [API Design](#12-api-design)
13. [Technical Specifications](#13-technical-specifications)

---

## 1. Executive Summary

### Problem Statement
The current manual scheduling approach in the cutting department leads to:
- Suboptimal box completion times
- Unbalanced machine utilization
- Material shortages discovered too late
- Lack of visibility into future production state
- **Single zone focus** - no global view across all production zones
- **No spreading optimization** - matelassage not scheduled separately
- **Machine type imbalance** - Lectra IP6 overloaded while Lectra machines idle

### Proposed Solution
A **Multi-Zone Macro Ordonnancement System** that provides:
- **ALL ZONES & ALL MACHINES**: Load Lectra, Lectra IP6, Gerber, LASER-DXF, DIE across all zones
- **~30 machines** managed globally with zone-specific constraints
- Automatic scheduling optimization with box completion time minimization
- **Separate spreading (matelassage) scheduling** with time estimation
- **Machine type load balancing** to prevent overload
- Real-time material availability tracking and prediction
- Multi-phase scheduling respecting current work-in-progress
- Flexible constraint management (pauses, maintenance, material delays)

### Key Benefits
- **Faster Box Completion**: Minimize MAX(box duration), then optimize median, then balance
- **Global View**: See all zones and all 30+ machines in one dashboard
- **Spreading Optimization**: Schedule matelassage before cutting efficiently
- **Load Balancing**: Prevent Lectra IP6 overload, balance across machine types
- **Max Boxes Per Zone**: Limit boxes to `machines_in_zone × 16`
- **Proactive Material Management**: Know shortages before they happen
- **Real-time Visibility**: See spreading, cutting progress, and estimates
- **Flexible Planning**: Adapt to machine stops, material delays, operator availability

---

## 2. Current State Analysis

### 2.1 Existing Ordonnancement.js Implementation

The current frontend implementation in `Ordonnancement.js` provides:

| Feature | Status | Description |
|---------|--------|-------------|
| Zone/Machine Selection | ✅ Implemented | Select production zone and machines |
| Sequence Loading | ✅ Implemented | Load sequences by zone or manually |
| Cutting Time Estimation | ✅ Implemented | Via `timingPlacement` API |
| Machine Slot Management | ✅ Implemented | Track occupied time slots per machine |
| Interval Management | ✅ Implemented | Pause/Strict stop intervals |
| In-Progress Estimation | ✅ Implemented | Estimate completion for running series |
| Material Roll Tracking | ⚠️ Partial | Basic consumption calculation |
| Box Timeline | ✅ Implemented | Visual box completion graph |
| Calendar View | ✅ Implemented | Gantt-style planning view |

### 2.2 Current Scheduling Logic (Ordonnancement.js)

```
1. Load sequences not completed for selected zone/machines
2. Get series for those sequences
3. Fetch placement timing data for cutting time estimation
4. Process series by status:
   a. statusCoupe = "Complete" → Already done, show in history
   b. statusCoupe = "In progress" → Estimate completion time
   c. statusMatelassage = "Complete/In progress" & statusCoupe = "Waiting"
      → Priority scheduling based on dateDebutMatelassage
   d. Both Waiting → Schedule based on machine availability
5. Apply pause/strict interval adjustments
6. Display in calendar and box timeline
```

### 2.3 Domain Model

```
CuttingRequest (Sequence)
├── cuttingRequestPartNumbers[] → Final output quantities per part number
├── cuttingRequestSeries[] → Work items (spreading + cutting per material)
└── cuttingRequestBoxes[] → Physical boxes to fill

CuttingRequestSerie Status Flow:
┌─────────────────────────────────────────────────────────────────┐
│  statusMatelassage: Waiting → In progress → Complete            │
│  statusCoupe:       Waiting → In progress → Complete            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. System Objectives

### 3.1 Primary Objectives

#### Objective 1: Minimize Box Completion Time
**Goal**: Finish boxes as fast as possible

**Optimization Strategy (3-Phase)**:
```
Phase 1: Minimize MAX(box_duration)
         ↓ When optimal reached
Phase 2: Minimize MEDIAN(box_duration)
         ↓ When optimal reached
Phase 3: Minimize VARIANCE(box_duration) → Balance all boxes
```

**Box Duration Calculation**:
```
box_duration(box) = MAX(serie.dateFinCoupe for serie in sequence.series) 
                  - MIN(serie.dateDebutMatelassage for serie in sequence.series)

time_per_box = sequence_total_duration / count(sequence.boxes)
```

#### Objective 2: Material Availability Prediction
**Goal**: Know before we start if materials are available

**Requirements**:
- Check rolls near machines (zone locations)
- Check rolls in storage (if not near machines)
- Allow user to specify material arrival delays
- Block series if material not available until certain date

#### Objective 3: Real-time Production State
**Goal**: See what's happening now and what's coming

**Views Required**:
- Currently spreading (In progress matelassage)
- Currently cutting (In progress coupe)
- Waiting in queue (Complete matelassage, Waiting coupe)
- Estimated completion times

#### Objective 4: Multi-Zone Global Scheduling
**Goal**: Manage all ~30 machines across all zones in one view

**Requirements**:
- Load ALL zones simultaneously (not just one)
- Include ALL machine types: Lectra, Lectra IP6, Gerber, LASER-DXF, DIE
- Sequence bound to its zone (cannot move to different zone)
- Series can be assigned to specific machine within zone
- Machine start/stop control per machine
- Max boxes per zone = `machines_in_zone × 16`

#### Objective 5: Spreading (Matelassage) Optimization
**Goal**: Schedule matelassage separately to prepare matelas before cutting

**Spreading Time Estimation Formula**:
```
spreading_duration(serie) = (SPREAD_RATE × longueur × nbrCouche) + ROLL_CHANGE_TIME

Where:
  SPREAD_RATE = 15 seconds per meter (0.25 min/m)
  ROLL_CHANGE_TIME = 3 minutes (material change or new roll)
  longueur = length in meters
  nbrCouche = number of layers

Example:
  longueur = 10m, nbrCouche = 50
  spreading_duration = (0.25 × 10 × 50) + 3 = 125 + 3 = 128 minutes
```

#### Objective 6: Machine Type Load Balancing
**Goal**: Prevent overload on specific machine types

**Requirements**:
- Balance work across machine types (Lectra vs Lectra IP6 vs Gerber)
- Detect imbalance in zones (one type overloaded, another idle)
- Visual indicator for load per machine type
- Suggest redistribution when imbalance detected

#### Objective 7: Flexible Machine Management
**Goal**: High flexibility for production management

**Requirements**:
- Attribute sequence to specific machine
- Start/stop individual machines
- Series spreaded on machine X should be cut on machine X (by default)
- User can force series to different cutting machine (override)
- Real-time adjustment of zone machine configuration

---

## 4. Multi-Zone Architecture

### 4.1 Zone Configuration

```javascript
// Zone structure with machines and constraints
const zoneConfig = {
  zones: [
    {
      id: "zone-1",
      name: "Zone A",
      rollLocations: "MP1,MP2,MP3",  // For material availability
      machines: [
        { nom: "AA1", machineType: { name: "Lectra" }, active: true },
        { nom: "AA2", machineType: { name: "Lectra" }, active: true },
        { nom: "AA3", machineType: { name: "Lectra IP6" }, active: true },
        // ...
      ],
      maxBoxes: 0,  // Calculated: machines.length × 16
    },
    {
      id: "zone-2",
      name: "Zone B",
      // ...
    }
  ],
  machineTypes: ["Lectra", "Lectra IP6", "Gerber", "LASER-DXF", "DIE"]
};
```

### 4.2 Max Boxes Calculation

```
maxBoxes(zone) = count(active_machines_in_zone) × 16

Example:
  Zone A has 5 active machines
  maxBoxes = 5 × 16 = 80 boxes maximum in progress
```

### 4.3 Machine Type Distribution

| Zone | Lectra | Lectra IP6 | Gerber | LASER-DXF | DIE | Total | Max Boxes |
|------|--------|------------|--------|-----------|-----|-------|-----------|
| Zone A | 3 | 2 | 0 | 0 | 0 | 5 | 80 |
| Zone B | 4 | 1 | 2 | 0 | 0 | 7 | 112 |
| Zone C | 2 | 0 | 0 | 2 | 1 | 5 | 80 |
| **Total** | **9** | **3** | **2** | **2** | **1** | **17** | **272** |

### 4.4 Zone-Machine Mapping Rules

```
RULE 1: Sequence → Zone
  - A sequence is created for a specific zone
  - It CANNOT be moved to a different zone
  - All series of a sequence must be in same zone

RULE 2: Serie → Machine (within zone)
  - A serie can be assigned to any machine in its zone
  - User can manually attribute serie to specific machine
  - Default: auto-assign based on optimization

RULE 3: Spreading → Cutting Same Machine
  - If serie.tableMatelassage = "AA1"
  - Then serie.tableCoupe should default to "AA1"
  - User can override with "Force to machine" option

RULE 4: Machine Type Matching
  - serie.machine = "Lectra" → only Lectra machines
  - serie.placement contains "-0BF" → special rules apply
  - Fallback: any available machine in zone
```

---

## 4. Data Model Overview

### 4.1 CuttingRequestSerieData Key Fields

```java
// Identification
private String serie;              // Unique ID (e.g., "240115001")
private String sequence;           // Parent sequence ID
private String partNumberMaterial; // Material reference (reftissu)
private String placement;          // Cutting pattern file

// Physical Parameters
private Double longueur;           // Length in meters
private Integer nbrCouche;         // Number of layers
private Double laize;              // Width
private String machine;            // Required machine type (Lectra/Lectra IP6/Gerber)

// Spreading (Matelassage) Status
private String zoneMatelassage;
private String tableMatelassage;    // Spreading table assigned
private String matelasseur1;        // Operator 1
private String matelasseur2;        // Operator 2
private LocalDateTime dateDebutMatelassage;
private LocalDateTime dateFinMatelassage;
private String statusMatelassage;   // "Waiting" | "In progress" | "Complete"

// Cutting (Coupe) Status
private String zoneCoupe;
private String tableCoupe;          // Cutting machine assigned
private String coupeur1;            // Operator 1
private String coupeur2;            // Operator 2
private LocalDateTime dateDebutCoupe;
private LocalDateTime dateFinCoupe;
private String statusCoupe;         // "Waiting" | "In progress" | "Complete"

// Timing
private Double tempsDeCoupe;        // Cutting time in minutes
```

### 4.2 New Entities Required

#### MaterialAvailability
```java
@Entity
public class MaterialAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String partNumberMaterial;  // Material reference
    private String zone;                 // Production zone
    
    @Enumerated(EnumType.STRING)
    private AvailabilityStatus status;   // AVAILABLE, SHORTAGE, DELAYED
    
    private LocalDateTime availableFrom; // When material will be available
    private Double quantityNeeded;       // Meters needed
    private Double quantityAvailable;    // Meters available
    
    private String source;               // "ZONE" or "STORAGE"
    private String userNote;             // Manual notes
    private String createdBy;
    private LocalDateTime createdAt;
}

public enum AvailabilityStatus {
    AVAILABLE,    // Material is ready
    SHORTAGE,     // Not enough material
    DELAYED,      // Material coming at availableFrom
    BLOCKED       // User blocked this material
}
```

#### SchedulingConstraint
```java
@Entity
public class SchedulingConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private ConstraintType type;        // PAUSE, STRICT_STOP, MATERIAL_DELAY, MACHINE_MAINTENANCE
    
    private String machine;             // null = all machines
    private String zone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // For material delays
    private String partNumberMaterial;
    
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
```

#### OptimizedSchedule
```java
@Entity
public class OptimizedSchedule {
    @Id
    private String scheduleId;          // UUID
    
    private String zone;
    private String machineNames;         // Comma-separated
    
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;       // DRAFT, ACTIVE, COMPLETED
    
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    
    // Metrics
    private Double maxBoxDuration;
    private Double medianBoxDuration;
    private Double avgBoxDuration;
    private Double boxDurationVariance;
    
    @OneToMany(mappedBy = "schedule")
    private List<ScheduledSerieAssignment> assignments;
}

@Entity
public class ScheduledSerieAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private OptimizedSchedule schedule;
    
    private String serieId;
    private String sequenceId;
    private String assignedMachine;
    
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    
    private Integer orderOnMachine;
    private Boolean isLocked;           // Prevent re-scheduling
    
    // Material status at scheduling time
    private String materialStatus;
    private LocalDateTime materialAvailableFrom;
}
```

---

## 6. Algorithm Design

### 6.1 Main Multi-Zone Scheduling Algorithm

```
ALGORITHM: MultiZoneMacroOrdonnancement

INPUT:
  - allZones[]: List of all production zones
  - allMachines[]: All ~30 machines across all zones (Lectra, Lectra IP6, Gerber, LASER-DXF, DIE)
  - sequences[]: List of CuttingRequest (not completed) for ALL zones
  - constraints[]: List of SchedulingConstraint
  - materialAvailability[]: Material status per zone

OUTPUT:
  - globalSchedule: {
      cuttingSchedule: assignments for cutting per zone/machine
      spreadingSchedule: assignments for matelassage per zone/machine
      metrics: KPIs per zone and global
      loadBalance: machine type utilization
    }

STEPS:

1. INITIALIZATION
   ├── Load ALL zones and ALL machines
   ├── FOR EACH zone:
   │   ├── Calculate maxBoxes = activeMachines.length × 16
   │   ├── Group machines by type (Lectra, Lectra IP6, Gerber, LASER-DXF, DIE)
   │   └── Initialize machineSlots[machine] = []
   ├── Load all series with dateDebutMatelassage (ordered by date)
   ├── Fetch cutting time estimates via timingPlacement API
   └── Load all constraints (pauses, stops, material delays)

2. CLASSIFY SERIES BY STATUS (per zone)
   FOR EACH zone:
       ├── COMPLETED: statusCoupe = "Complete"
       │   → Add to history, reserve slots for reference
       │
       ├── CUTTING_IN_PROGRESS: statusCoupe = "In progress"
       │   → ESTIMATE dateFinCoupe
       │   → Reserve slot on tableCoupe machine
       │
       ├── SPREADING_IN_PROGRESS: statusMatelassage = "In progress"
       │   → ESTIMATE dateFinMatelassage
       │   → Reserve spreading slot
       │
       ├── SPREADING_DONE_WAITING_CUT: statusMatelassage = "Complete" AND statusCoupe = "Waiting"
       │   → Priority for cutting (same machine as spreading by default)
       │
       └── FULLY_WAITING: statusMatelassage = "Waiting" AND statusCoupe = "Waiting"
           → Schedule BOTH spreading and cutting

3. PHASE 1: SCHEDULE CUTTING (per zone)
   FOR EACH zone:
       // Process in-progress cutting
       FOR EACH serie WHERE statusCoupe = "In progress":
           cutTime = getCuttingTime(serie.placement)
           estimatedEnd = serie.dateDebutCoupe + cutTime + pauseAdjustments
           machineSlots[serie.tableCoupe].add({...})
       
       // Process spreading-done waiting for cut
       FOR EACH serie WHERE statusMatelassage IN ("Complete", "In progress") AND statusCoupe = "Waiting":
           // Default: use same machine as spreading
           targetMachine = serie.tableMatelassage
           IF userOverride exists:
               targetMachine = userOverride.machine
           
           schedule cutting on targetMachine
       
       // Process fully waiting (optimize)
       FOR EACH serie WHERE statusMatelassage = "Waiting":
           // Balance across machine types
           machineType = getMachineTypeForSerie(serie)
           candidateMachines = zone.machines.filter(m => m.type == machineType)
           
           // Choose machine with lowest load
           bestMachine = selectLeastLoadedMachine(candidateMachines, machineSlots)
           schedule cutting on bestMachine

4. PHASE 2: SCHEDULE SPREADING (per zone) - SEE SECTION 7
   FOR EACH zone:
       FOR EACH serie WHERE statusMatelassage = "Waiting":
           spreadTime = calculateSpreadingTime(serie)
           // Schedule spreading BEFORE cutting starts
           schedule spreading before serie.scheduledCutStart

5. LOAD BALANCING CHECK
   FOR EACH zone:
       loadByType = {}
       FOR EACH machineType IN zone.machineTypes:
           totalLoad = sum(scheduled work for machineType)
           capacity = count(machines of type) × hoursAvailable
           loadByType[machineType] = totalLoad / capacity
       
       // Detect imbalance
       maxLoad = max(loadByType.values())
       minLoad = min(loadByType.values())
       IF maxLoad - minLoad > IMBALANCE_THRESHOLD (e.g., 30%):
           WARN "Imbalance detected in zone"
           SUGGEST redistribution

6. CALCULATE METRICS
   // Per zone
   FOR EACH zone:
       calculate maxBoxDuration, medianBoxDuration, avgBoxDuration, variance
       calculate machineUtilization per machine type
       check boxCount vs maxBoxes limit
   
   // Global
   calculate global metrics across all zones

7. RETURN globalSchedule
```

### 6.2 Slot Finding Algorithm

```
FUNCTION findOptimalSlot(machines, machineSlots, earliestStart, duration, constraints, sequence, boxCount):
    bestSlot = null
    bestScore = INFINITY
    
    FOR EACH machine IN machines:
        slots = machineSlots[machine]
        constraints_for_machine = filter(constraints, machine)
        
        // Find first available time after all existing slots
        candidateStart = earliestStart
        IF slots.length > 0:
            lastSlot = max(slots, by=end)
            candidateStart = max(earliestStart, lastSlot.end)
        
        // Adjust for constraints
        (adjustedStart, adjustedEnd) = applyConstraints(candidateStart, duration, constraints_for_machine)
        
        // Calculate impact on box duration
        currentMaxEnd = getMaxEndForSequence(sequence)
        newMaxEnd = max(currentMaxEnd, adjustedEnd)
        impactScore = newMaxEnd - currentMaxEnd
        
        // Also factor in machine type balance
        machineTypeLoad = getLoadForMachineType(machine.type)
        balanceScore = machineTypeLoad * BALANCE_WEIGHT
        
        totalScore = impactScore + balanceScore
        
        IF totalScore < bestScore:
            bestScore = totalScore
            bestSlot = {machine, start: adjustedStart, end: adjustedEnd}
    
    RETURN bestSlot
```

### 6.3 Constraint Application

```
FUNCTION applyConstraints(start, duration, constraints):
    end = start + duration
    changed = true
    
    WHILE changed:
        changed = false
        
        // Check for STRICT_STOP overlaps (must shift after)
        FOR EACH constraint IN constraints WHERE type == STRICT_STOP:
            IF overlaps(start, end, constraint.start, constraint.end):
                start = constraint.end
                end = start + duration
                changed = true
                BREAK
        
        // Check for PAUSE overlaps (extend duration)
        totalPauseMinutes = 0
        FOR EACH constraint IN constraints WHERE type == PAUSE:
            IF overlaps(start, end, constraint.start, constraint.end):
                overlapStart = max(start, constraint.start)
                overlapEnd = min(end, constraint.end)
                pauseMinutes = overlapEnd - overlapStart
                totalPauseMinutes += pauseMinutes
        
        IF totalPauseMinutes > 0:
            end = end + totalPauseMinutes
            changed = true
        
        // Check for MATERIAL_DELAY
        FOR EACH constraint IN constraints WHERE type == MATERIAL_DELAY:
            IF constraint.partNumberMaterial == serie.partNumberMaterial:
                IF start < constraint.endTime:
                    start = constraint.endTime
                    end = start + duration
                    changed = true
    
    RETURN (start, end)
```

### 6.4 Machine Candidate Selection

Based on current Ordonnancement.js logic:

```
FUNCTION getMachineCandidates(serie, allMachines):
    placement = serie.placement
    config = serie.config
    partNumberMaterial = serie.partNumberMaterial
    requiredType = serie.machine OR "Lectra"
    
    // Special rules for -0BF placements
    IF placement.contains("-0BF"):
        IF partNumberMaterial == "100132940":
            RETURN filter(machines, type == "Lectra IP6")
        ELSE IF config == "A":
            RETURN filter(machines, type == "Lectra IP6")
        ELSE:
            RETURN filter(machines, type == "Lectra")
    
    // For LASER-DXF and DIE: specific machine types
    IF requiredType == "LASER-DXF":
        RETURN filter(allMachines, type == "LASER-DXF")
    IF requiredType == "DIE":
        RETURN filter(allMachines, type == "DIE")
    
    // Default: match machine type
    candidates = filter(allMachines, type == requiredType)
    IF candidates.isEmpty:
        RETURN allMachines  // Fallback to all machines
    
    RETURN candidates
```

---

## 7. Spreading (Matelassage) Scheduling

### 7.1 Spreading Time Estimation

```
CONSTANTS:
  SPREAD_RATE = 15 seconds per meter = 0.25 minutes per meter
  ROLL_CHANGE_TIME = 3 minutes (for material/roll change)

FUNCTION calculateSpreadingTime(serie):
    longueur = serie.longueur  // in meters
    nbrCouche = serie.nbrCouche  // number of layers
    
    // Base spreading time
    baseTime = SPREAD_RATE × longueur × nbrCouche
    
    // Add roll change time
    totalTime = baseTime + ROLL_CHANGE_TIME
    
    RETURN totalTime  // in minutes

EXAMPLE:
    longueur = 10m, nbrCouche = 50
    baseTime = 0.25 × 10 × 50 = 125 minutes
    totalTime = 125 + 3 = 128 minutes (2h 8m)
```

### 7.2 Spreading Schedule Algorithm

```
ALGORITHM: SpreadingScheduler

PURPOSE: Schedule matelassage operations BEFORE cutting starts

INPUT:
  - seriesForZone[]: All series in a zone needing spreading
  - machineSlots[]: Current machine occupation
  - cuttingSchedule[]: Scheduled cutting times

OUTPUT:
  - spreadingSchedule[]: {serie, machine, start, end}

STEPS:

1. FOR EACH serie WHERE statusMatelassage = "Waiting":
   
   // Calculate when this serie MUST be ready for cutting
   cutStart = serie.scheduledCutStart OR estimatedCutStart
   spreadDuration = calculateSpreadingTime(serie)
   
   // Spreading must finish BEFORE cutting starts
   latestSpreadEnd = cutStart
   latestSpreadStart = cutStart - spreadDuration
   
   // Find available slot on target machine
   targetMachine = serie.tableMatelassage OR serie.scheduledMachine
   
   // Check machine spreading slots
   spreadSlots = spreadingSlots[targetMachine]
   
   // Find slot that ends before cutting needs to start
   slot = findSpreadingSlot(targetMachine, spreadSlots, spreadDuration, latestSpreadEnd)
   
   IF slot.end > latestSpreadEnd:
       // Warning: Spreading will delay cutting
       WARN "Spreading conflict for serie {serie.id}"
       // Option 1: Accept delay
       // Option 2: Use different machine
   
   spreadingSchedule.add({
       serie: serie,
       machine: targetMachine,
       start: slot.start,
       end: slot.end,
       cutStartsAt: cutStart
   })

2. OPTIMIZE spreading order:
   // Group by material for efficiency (less roll changes)
   sortByMaterial(spreadingSchedule)
   
   // Ensure no gaps in machine utilization
   compactSlots(spreadingSchedule)

3. RETURN spreadingSchedule
```

### 7.3 Spreading View (Frontend)

```javascript
// SpreadingSchedule component state
spreadingState = {
    spreadingSeries: [],  // Series waiting for matelassage
    spreadingInProgress: [],  // Currently spreading
    spreadingComplete: [],  // Ready for cutting
    
    // Per machine spreading queue
    machineSpreadingQueue: {
        "AA1": [serie1, serie2, ...],
        "AA2": [serie3, serie4, ...],
        // ...
    },
    
    // Metrics
    spreadingMetrics: {
        totalSpreadingTime: 0,  // Total minutes of spreading work
        avgSpreadingTime: 0,
        maxSpreadingTime: 0,
        materialChanges: 0,  // Number of roll changes
    }
}

// Display spreading timeline separate from cutting
renderSpreadingTimeline() {
    return (
        <div className="spreading-timeline">
            <h4>📜 Matelassage Planning</h4>
            {machineSpreadingQueue.map((machine, queue) => (
                <div className="machine-spreading-row">
                    <span className="machine-name">{machine}</span>
                    {queue.map(serie => (
                        <div className="spreading-slot" style={{
                            left: timeToPosition(serie.spreadStart),
                            width: durationToWidth(serie.spreadDuration)
                        }}>
                            {serie.serie} - {serie.partNumberMaterial}
                        </div>
                    ))}
                </div>
            ))}
        </div>
    );
}
```

---

## 8. Machine Load Balancing

### 8.1 Load Calculation

```
FUNCTION calculateMachineTypeLoad(zone, machineType):
    machines = zone.machines.filter(m => m.type == machineType)
    
    totalScheduledMinutes = 0
    FOR EACH machine IN machines:
        FOR EACH slot IN machineSlots[machine.nom]:
            totalScheduledMinutes += slot.end - slot.start
    
    totalCapacityMinutes = machines.length × HOURS_PER_SHIFT × 60
    
    load = totalScheduledMinutes / totalCapacityMinutes
    RETURN load  // 0.0 to 1.0+
```

### 8.2 Imbalance Detection

```
FUNCTION detectImbalance(zone):
    loads = {}
    FOR EACH machineType IN ["Lectra", "Lectra IP6", "Gerber", "LASER-DXF", "DIE"]:
        IF zone has machines of this type:
            loads[machineType] = calculateMachineTypeLoad(zone, machineType)
    
    maxLoad = max(loads.values())
    minLoad = min(loads.values())
    avgLoad = avg(loads.values())
    
    imbalance = {
        isImbalanced: (maxLoad - minLoad) > 0.3,  // 30% threshold
        overloadedType: findKey(loads, maxLoad),
        underloadedType: findKey(loads, minLoad),
        loads: loads,
        recommendation: null
    }
    
    IF imbalance.isImbalanced:
        imbalance.recommendation = 
            "Move work from {overloadedType} to {underloadedType}"
    
    RETURN imbalance
```

### 8.3 Load Balancing UI

```javascript
renderLoadBalancePanel() {
    return (
        <div className="load-balance-panel">
            <h4>⚖️ Machine Type Load Balance</h4>
            
            {zones.map(zone => (
                <div className="zone-load">
                    <h5>{zone.name}</h5>
                    <div className="load-bars">
                        {machineTypes.map(type => {
                            const load = zone.loads[type];
                            return (
                                <div className="load-bar-container">
                                    <span className="type-label">{type}</span>
                                    <div className="load-bar" style={{
                                        width: `${load * 100}%`,
                                        backgroundColor: load > 0.9 ? 'red' : 
                                                        load > 0.7 ? 'orange' : 'green'
                                    }}>
                                        {(load * 100).toFixed(0)}%
                                    </div>
                                    <span className="machine-count">
                                        ({zone.machineCount[type]} machines)
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                    {zone.imbalance.isImbalanced && (
                        <div className="imbalance-warning">
                            ⚠️ {zone.imbalance.recommendation}
                        </div>
                    )}
                </div>
            ))}
        </div>
    );
}
```

---

## 6. Implementation Phases

### Phase 1: Backend Foundation (Week 1-2)

#### Tasks:
1. Create new domain entities:
   - `MaterialAvailability`
   - `SchedulingConstraint`
   - `OptimizedSchedule`
   - `ScheduledSerieAssignment`

2. Create repositories:
   - `MaterialAvailabilityRepository`
   - `SchedulingConstraintRepository`
   - `OptimizedScheduleRepository`
   - `ScheduledSerieAssignmentRepository`

3. Create core services:
   - `MacroSchedulingService` - Main scheduling logic
   - `MaterialAvailabilityService` - Material tracking
   - `SchedulingConstraintService` - Constraint management

4. Create API endpoints:
   - `/api/macro-scheduling/*`

#### Deliverables:
- [ ] Database migrations for new tables
- [ ] Entity classes with JPA mappings
- [ ] Repository interfaces
- [ ] Service layer implementation
- [ ] Unit tests for scheduling algorithm

---

### Phase 2: Material Availability System (Week 3)

#### Tasks:
1. Integrate with existing roll scanning system (`scanRouleau`)
2. Integrate with storage stock API (`stockStatusReport`)
3. Create material delay management UI
4. Implement material shortage alerts

#### Deliverables:
- [ ] Material availability checker service
- [ ] Integration with zone roll locations
- [ ] Material delay constraint management
- [ ] Real-time shortage notifications

---

### Phase 3: Frontend Dashboard (Week 4-5)

#### Tasks:
1. Create `MacroOrdonnancement.js` component
2. Implement new scheduling visualization
3. Create box duration metrics display
4. Implement material availability panel
5. Create constraint management interface

#### Deliverables:
- [ ] New dashboard component
- [ ] Interactive Gantt chart with box focus
- [ ] Material availability indicators
- [ ] KPI dashboard section
- [ ] Constraint management UI

---

### Phase 4: Optimization & Refinement (Week 6)

#### Tasks:
1. Fine-tune optimization algorithm
2. Add simulation mode (what-if analysis)
3. Performance optimization
4. User acceptance testing

#### Deliverables:
- [ ] Optimized algorithm implementation
- [ ] Simulation mode
- [ ] Performance benchmarks
- [ ] UAT sign-off

---

## 7. Backend Architecture

### 7.1 Service Layer

```java
@Service
public class MacroSchedulingService {
    
    @Autowired
    private CuttingRequestSerieDataService serieDataService;
    @Autowired
    private MaterialAvailabilityService materialService;
    @Autowired
    private SchedulingConstraintService constraintService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private OptimizedScheduleRepository scheduleRepository;
    
    /**
     * Main scheduling entry point
     */
    public OptimizedSchedule createOptimizedSchedule(
            String zone, 
            List<String> machineNames,
            SchedulingOptions options) {
        
        // 1. Load all relevant series
        List<CuttingRequestSerieData> allSeries = loadSeriesForZone(zone, machineNames);
        
        // 2. Classify by status
        Map<SerieStatus, List<CuttingRequestSerieData>> classified = classifySeries(allSeries);
        
        // 3. Load constraints
        List<SchedulingConstraint> constraints = constraintService.getActiveConstraints(zone);
        
        // 4. Check material availability
        Map<String, MaterialAvailability> materialMap = materialService.checkAvailability(
            allSeries.stream().map(s -> s.getPartNumberMaterial()).distinct().toList(),
            zone
        );
        
        // 5. Execute scheduling algorithm
        OptimizedSchedule schedule = executeScheduling(classified, machineNames, constraints, materialMap, options);
        
        // 6. Save and return
        return scheduleRepository.save(schedule);
    }
    
    /**
     * Get real-time production state
     */
    public ProductionStateDTO getProductionState(String zone, List<String> machineNames) {
        ProductionStateDTO state = new ProductionStateDTO();
        
        // Currently spreading
        state.setSpreading(serieDataService.findByStatusMatelassage("In progress", zone));
        
        // Currently cutting
        state.setCutting(serieDataService.findByStatusCoupe("In progress", zone));
        
        // Waiting for cut (matelassage done)
        state.setWaitingForCut(serieDataService.findMatelassageDoneWaitingCoupe(zone));
        
        // Scheduled estimates
        state.setEstimates(calculateEstimates(state, machineNames));
        
        return state;
    }
}
```

### 7.2 Material Service

```java
@Service
public class MaterialAvailabilityService {
    
    @Autowired
    private ScanRouleauRepository scanRouleauRepository;
    @Autowired
    private StockStatusReportService stockService;
    @Autowired
    private MaterialAvailabilityRepository availabilityRepository;
    
    /**
     * Check material availability for planning
     */
    public Map<String, MaterialAvailability> checkAvailability(
            List<String> materials, 
            String zone) {
        
        Map<String, MaterialAvailability> result = new HashMap<>();
        
        for (String material : materials) {
            MaterialAvailability availability = new MaterialAvailability();
            availability.setPartNumberMaterial(material);
            availability.setZone(zone);
            
            // Check zone rolls first
            Double zoneQuantity = getZoneRollQuantity(material, zone);
            
            // Check storage if insufficient
            Double storageQuantity = 0.0;
            if (zoneQuantity < getRequiredQuantity(material, zone)) {
                storageQuantity = getStorageQuantity(material);
            }
            
            // Check for user-defined delays
            Optional<SchedulingConstraint> delay = getDelayConstraint(material);
            
            if (zoneQuantity + storageQuantity >= getRequiredQuantity(material, zone)) {
                availability.setStatus(AvailabilityStatus.AVAILABLE);
            } else if (delay.isPresent()) {
                availability.setStatus(AvailabilityStatus.DELAYED);
                availability.setAvailableFrom(delay.get().getEndTime());
            } else {
                availability.setStatus(AvailabilityStatus.SHORTAGE);
            }
            
            availability.setQuantityAvailable(zoneQuantity + storageQuantity);
            result.put(material, availability);
        }
        
        return result;
    }
    
    /**
     * Set material delay constraint
     */
    public void setMaterialDelay(String material, String zone, LocalDateTime availableFrom, String note) {
        SchedulingConstraint constraint = new SchedulingConstraint();
        constraint.setType(ConstraintType.MATERIAL_DELAY);
        constraint.setPartNumberMaterial(material);
        constraint.setZone(zone);
        constraint.setStartTime(LocalDateTime.now());
        constraint.setEndTime(availableFrom);
        constraint.setDescription(note);
        constraintRepository.save(constraint);
    }
}
```

---

## 8. Frontend Components

### 8.1 Component Structure

```
src/main/js/components/Layout/
├── MacroOrdonnancement/
│   ├── MacroOrdonnancement.js          # Main container
│   ├── ProductionStatePanel.js          # Current state display
│   ├── SchedulingGantt.js               # Gantt chart view
│   ├── BoxMetricsPanel.js               # KPI dashboard
│   ├── MaterialAvailabilityPanel.js     # Material status
│   ├── ConstraintManager.js             # Constraint CRUD
│   └── styles/
│       └── MacroOrdonnancement.scss
```

### 8.2 Main Component State

```javascript
// MacroOrdonnancement.js
state = {
    // Zone and machines
    selectedZone: null,
    machineArr: [],
    productionTableArr: [],
    
    // Series data
    series: [],
    sequences: [],
    estimationArr: [],       // Cutting time estimates
    
    // Production state
    spreading: [],           // statusMatelassage = "In progress"
    cutting: [],             // statusCoupe = "In progress"
    waitingForCut: [],       // matelassage done, waiting cut
    fullyWaiting: [],        // both waiting
    
    // Schedule
    schedule: null,
    assignments: [],
    
    // Material
    materialAvailability: {},
    materialDelays: [],
    rollsByLocation: {},
    rollsByReftissu: {},
    storageStock: {},
    
    // Constraints
    constraints: [],
    
    // Metrics
    boxMetrics: {
        maxDuration: 0,
        medianDuration: 0,
        avgDuration: 0,
        variance: 0,
        targetMax: 0          // User-defined target
    },
    
    // UI
    isLoading: false,
    isOptimizing: false,
    optimizationProgress: 0,
    collapse: {
        productionState: false,
        schedule: false,
        materials: true,
        constraints: true,
        metrics: false
    }
}
```

### 8.3 Production State Panel

```javascript
// ProductionStatePanel.js
renderProductionState() {
    return (
        <div className="production-state-panel">
            {/* Currently Spreading */}
            <div className="section spreading">
                <h4>🔄 Matelassage en cours ({spreading.length})</h4>
                <table>
                    <thead>
                        <tr>
                            <th>Série</th>
                            <th>Table</th>
                            <th>Material</th>
                            <th>Début</th>
                            <th>Estimé Fin</th>
                        </tr>
                    </thead>
                    <tbody>
                        {spreading.map(s => (
                            <tr key={s.serie}>
                                <td>{s.serie}</td>
                                <td>{s.tableMatelassage}</td>
                                <td>{s.partNumberMaterial}</td>
                                <td>{formatDate(s.dateDebutMatelassage)}</td>
                                <td>{estimateSpreadingEnd(s)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            
            {/* Currently Cutting */}
            <div className="section cutting">
                <h4>✂️ Coupe en cours ({cutting.length})</h4>
                <table>
                    <thead>
                        <tr>
                            <th>Série</th>
                            <th>Machine</th>
                            <th>Début</th>
                            <th>Estimé Fin</th>
                            <th>Progress</th>
                        </tr>
                    </thead>
                    <tbody>
                        {cutting.map(s => (
                            <tr key={s.serie}>
                                <td>{s.serie}</td>
                                <td>{s.tableCoupe}</td>
                                <td>{formatDate(s.dateDebutCoupe)}</td>
                                <td>{s.EDdateFinCoupe}</td>
                                <td>
                                    <ProgressBar value={calculateCuttingProgress(s)} />
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            
            {/* Waiting for Cut */}
            <div className="section waiting">
                <h4>⏳ En attente de coupe ({waitingForCut.length})</h4>
                {/* Grouped by tableMatelassage, ordered by dateDebutMatelassage */}
                {groupByTable(waitingForCut).map(([table, series]) => (
                    <div key={table} className="table-group">
                        <h5>{table}</h5>
                        <ol>
                            {series.map((s, idx) => (
                                <li key={s.serie}>
                                    {s.serie} - {s.partNumberMaterial}
                                    <span className="scheduled">
                                        Prévu: {s.EDdateDebutCoupe} → {s.EDdateFinCoupe}
                                    </span>
                                </li>
                            ))}
                        </ol>
                    </div>
                ))}
            </div>
        </div>
    );
}
```

### 8.4 Box Metrics Panel

```javascript
// BoxMetricsPanel.js
renderBoxMetrics() {
    const { boxMetrics, sequences, statBox } = this.state;
    
    // Calculate per-sequence metrics
    const sequenceMetrics = sequences.map(seq => {
        const boxes = statBox.find(s => s.sequence === seq.sequence);
        const series = this.state.series.filter(s => s.sequence === seq.sequence);
        
        const minStart = Math.min(...series.map(s => 
            new Date(s.dateDebutMatelassage || s.EDdateDebutCoupe).getTime()
        ));
        const maxEnd = Math.max(...series.map(s => 
            new Date(s.dateFinCoupe || s.EDdateFinCoupe).getTime()
        ));
        
        const duration = (maxEnd - minStart) / 60000; // minutes
        const boxCount = boxes?.countBoxes || 1;
        const durationPerBox = duration / boxCount;
        
        return {
            sequence: seq.sequence,
            boxCount,
            duration,
            durationPerBox
        };
    });
    
    return (
        <div className="box-metrics-panel">
            <div className="kpi-cards">
                <div className="kpi-card max">
                    <h4>MAX Box Duration</h4>
                    <span className="value">{formatDuration(boxMetrics.maxDuration)}</span>
                    <span className="target">Target: {formatDuration(boxMetrics.targetMax)}</span>
                </div>
                <div className="kpi-card median">
                    <h4>MEDIAN Box Duration</h4>
                    <span className="value">{formatDuration(boxMetrics.medianDuration)}</span>
                </div>
                <div className="kpi-card avg">
                    <h4>AVG Box Duration</h4>
                    <span className="value">{formatDuration(boxMetrics.avgDuration)}</span>
                </div>
                <div className="kpi-card variance">
                    <h4>Variance</h4>
                    <span className="value">{boxMetrics.variance.toFixed(2)}</span>
                    <span className="indicator">
                        {boxMetrics.variance < 10 ? '✅ Balanced' : '⚠️ Unbalanced'}
                    </span>
                </div>
            </div>
            
            <div className="sequence-breakdown">
                <h4>Per-Sequence Analysis</h4>
                <table>
                    <thead>
                        <tr>
                            <th>Sequence</th>
                            <th>Boxes</th>
                            <th>Total Duration</th>
                            <th>Duration/Box</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {sequenceMetrics
                            .sort((a, b) => b.durationPerBox - a.durationPerBox)
                            .map(m => (
                                <tr key={m.sequence} className={
                                    m.durationPerBox === boxMetrics.maxDuration ? 'highlight-max' : ''
                                }>
                                    <td>{m.sequence}</td>
                                    <td>{m.boxCount}</td>
                                    <td>{formatDuration(m.duration)}</td>
                                    <td>{formatDuration(m.durationPerBox)}</td>
                                    <td>
                                        {m.durationPerBox === boxMetrics.maxDuration && '🔴 Longest'}
                                        {m.durationPerBox > boxMetrics.avgDuration && '🟡 Above Avg'}
                                        {m.durationPerBox <= boxMetrics.avgDuration && '🟢 Good'}
                                    </td>
                                </tr>
                            ))
                        }
                    </tbody>
                </table>
            </div>
        </div>
    );
}
```

---

## 9. Material Availability System

### 9.1 Data Sources

| Source | API | Data |
|--------|-----|------|
| Zone Rolls | `/api/scanRouleau/byLocations` | Rolls near machines |
| Storage Stock | `/api/stockStatusReport/currentStock` | Rolls in warehouse |
| Consumption Estimate | Calculated | nbrCouche × longueur per serie |

### 9.2 Material Check Algorithm

```
FUNCTION checkMaterialAvailability(partNumberMaterial, zone, requiredQty):
    
    // Step 1: Check zone rolls
    zoneRolls = api.get("/scanRouleau/byLocations", zone.rollLocations)
    zoneQty = sum(zoneRolls.filter(r => r.reftissu == partNumberMaterial).map(r => r.metrage))
    
    IF zoneQty >= requiredQty:
        RETURN { status: AVAILABLE, source: "ZONE", quantity: zoneQty }
    
    // Step 2: Check storage
    storageStock = api.get("/stockStatusReport/currentStock", partNumberMaterial)
    storageQty = sum(storageStock.map(s => s.qtyOnHand))
    
    totalQty = zoneQty + storageQty
    
    IF totalQty >= requiredQty:
        RETURN { 
            status: AVAILABLE, 
            source: "ZONE+STORAGE", 
            quantity: totalQty,
            needsTransfer: storageQty  // Amount to transfer from storage
        }
    
    // Step 3: Check user-defined delays
    delay = db.findMaterialDelay(partNumberMaterial, zone)
    IF delay EXISTS:
        RETURN {
            status: DELAYED,
            availableFrom: delay.endTime,
            quantity: totalQty,
            shortage: requiredQty - totalQty
        }
    
    // Step 4: Shortage
    RETURN {
        status: SHORTAGE,
        quantity: totalQty,
        shortage: requiredQty - totalQty
    }
```

### 9.3 Material Delay Interface

```javascript
// MaterialDelayModal.js
renderMaterialDelayForm() {
    return (
        <div className="material-delay-form">
            <h4>Définir un retard matière</h4>
            <form onSubmit={this.handleSetDelay}>
                <div className="form-group">
                    <label>Matière (Part Number Material)</label>
                    <Select
                        options={uniqueMaterials}
                        value={selectedMaterial}
                        onChange={this.handleMaterialChange}
                    />
                </div>
                <div className="form-group">
                    <label>Disponible à partir de</label>
                    <input 
                        type="datetime-local" 
                        value={availableFrom}
                        onChange={this.handleDateChange}
                    />
                </div>
                <div className="form-group">
                    <label>Note</label>
                    <textarea 
                        value={note}
                        onChange={this.handleNoteChange}
                        placeholder="Ex: En attente livraison fournisseur"
                    />
                </div>
                <button type="submit" className="btn btn-primary">
                    Enregistrer le retard
                </button>
            </form>
        </div>
    );
}
```

---

## 10. KPI Indicators & Optimization Metrics

### 10.1 Primary KPIs

| KPI | Formula | Target | Description |
|-----|---------|--------|-------------|
| Max Box Duration | `MAX(sequence_duration / box_count)` | Minimize | Worst-case box completion time |
| Median Box Duration | `MEDIAN(box_durations)` | Minimize after MAX | Typical box completion time |
| Avg Box Duration | `AVG(box_durations)` | Minimize | Average performance |
| Box Duration Variance | `VARIANCE(box_durations)` | Minimize | Balance indicator |
| Machine Utilization | `busy_time / available_time` | Maximize | Efficiency metric |
| Material Readiness | `available_series / total_series` | 100% | Material availability |

### 10.2 Optimization Flow

```
┌───────────────────────────────────────────────────────────────┐
│                    OPTIMIZATION PHASES                        │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  Phase 1: MINIMIZE MAX(box_duration)                          │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Focus on the sequence with longest box duration         │  │
│  │ - Prioritize its series on fastest available machines   │  │
│  │ - Split work across multiple machines if possible       │  │
│  │ - Repeat until MAX cannot be reduced                    │  │
│  └─────────────────────────────────────────────────────────┘  │
│                          ↓                                    │
│  Phase 2: MINIMIZE MEDIAN(box_duration)                       │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Focus on sequences above median                         │  │
│  │ - Reorder to reduce above-median durations              │  │
│  │ - Balance load across machines                          │  │
│  └─────────────────────────────────────────────────────────┘  │
│                          ↓                                    │
│  Phase 3: MINIMIZE VARIANCE(box_duration)                     │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Balance all box durations toward average                │  │
│  │ - Fine-tune ordering for consistency                    │  │
│  │ - Target: variance < 10%                                │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

### 10.3 Real-time Monitoring

```javascript
// Real-time metrics calculation
calculateRealTimeMetrics() {
    const { series, sequences, statBox } = this.state;
    const now = moment();
    
    const metrics = sequences.map(seq => {
        const seqSeries = series.filter(s => s.sequence === seq.sequence);
        const boxes = statBox.find(s => s.sequence === seq.sequence);
        
        // Get actual or estimated start
        const starts = seqSeries.map(s => {
            if (s.dateDebutMatelassage) return moment(s.dateDebutMatelassage);
            if (s.EDdateDebutCoupe) return moment(s.EDdateDebutCoupe);
            return null;
        }).filter(Boolean);
        
        // Get actual or estimated end
        const ends = seqSeries.map(s => {
            if (s.dateFinCoupe) return moment(s.dateFinCoupe);
            if (s.EDdateFinCoupe) return moment(s.EDdateFinCoupe);
            return null;
        }).filter(Boolean);
        
        if (starts.length === 0 || ends.length === 0) return null;
        
        const minStart = moment.min(starts);
        const maxEnd = moment.max(ends);
        const duration = maxEnd.diff(minStart, 'minutes');
        const boxCount = boxes?.countBoxes || 1;
        
        return {
            sequence: seq.sequence,
            duration,
            boxCount,
            durationPerBox: duration / boxCount,
            status: getSequenceStatus(seqSeries),
            completionPercent: calculateCompletionPercent(seqSeries)
        };
    }).filter(Boolean);
    
    return {
        maxDuration: Math.max(...metrics.map(m => m.durationPerBox)),
        medianDuration: median(metrics.map(m => m.durationPerBox)),
        avgDuration: avg(metrics.map(m => m.durationPerBox)),
        variance: variance(metrics.map(m => m.durationPerBox)),
        sequenceMetrics: metrics
    };
}
```

---

## 11. API Design

### 11.1 Macro Scheduling Endpoints

```
POST /api/macro-scheduling/optimize
    Request: { zone, machines[], options }
    Response: { scheduleId, assignments[], metrics }

GET /api/macro-scheduling/state/{zone}
    Response: { spreading[], cutting[], waitingForCut[], waiting[] }

GET /api/macro-scheduling/schedule/{scheduleId}
    Response: OptimizedSchedule with assignments

POST /api/macro-scheduling/activate/{scheduleId}
    Response: { success, activatedAt }

DELETE /api/macro-scheduling/schedule/{scheduleId}
    Response: { success }
```

### 11.2 Material Endpoints

```
GET /api/macro-scheduling/materials/availability
    Params: zone, materials[]
    Response: { material -> availability }

POST /api/macro-scheduling/materials/delay
    Request: { material, zone, availableFrom, note }
    Response: { constraintId }

DELETE /api/macro-scheduling/materials/delay/{constraintId}
    Response: { success }

GET /api/macro-scheduling/materials/consumption-forecast
    Params: zone, hoursAhead
    Response: { material -> { required, available, status } }
```

### 11.3 Constraint Endpoints

```
GET /api/macro-scheduling/constraints
    Params: zone
    Response: SchedulingConstraint[]

POST /api/macro-scheduling/constraints
    Request: SchedulingConstraint
    Response: { constraintId }

PUT /api/macro-scheduling/constraints/{id}
    Request: SchedulingConstraint
    Response: { success }

DELETE /api/macro-scheduling/constraints/{id}
    Response: { success }
```

---

## 12. Technical Specifications

### 12.1 Performance Requirements

| Metric | Requirement |
|--------|-------------|
| Schedule Generation | < 5 seconds for 100 series |
| Real-time State Update | Every 30 seconds |
| Material Check | < 2 seconds per zone |
| UI Response | < 200ms for interactions |

### 12.2 Database Indexes

```sql
-- For fast series lookup by status
CREATE INDEX idx_serie_status ON CuttingRequestSerie 
    (statusMatelassage, statusCoupe, dateDebutMatelassage);

-- For zone-based queries
CREATE INDEX idx_serie_zone ON CuttingRequestSerie 
    (zoneMatelassage, zoneCoupe);

-- For machine slot lookups
CREATE INDEX idx_serie_machine_date ON CuttingRequestSerie 
    (tableCoupe, dateDebutCoupe, dateFinCoupe);

-- Material availability
CREATE INDEX idx_material_zone ON MaterialAvailability 
    (partNumberMaterial, zone);

-- Constraints
CREATE INDEX idx_constraint_zone ON SchedulingConstraint 
    (zone, type, startTime, endTime);
```

### 12.3 Integration Points

| System | Integration | Purpose |
|--------|-------------|---------|
| Existing Ordonnancement.js | Code reuse | Leverage existing logic |
| timingPlacement API | Query service | Get cutting time estimates |
| scanRouleau | Material tracking | Zone roll availability |
| stockStatusReport | Inventory | Storage stock levels |
| WebSocket | Real-time | Push updates to UI |

### 12.4 Migration Strategy

1. **Parallel Development**: New system alongside existing Ordonnancement.js
2. **Feature Toggle**: Enable macro scheduling per zone
3. **Gradual Rollout**: Start with one zone, expand after validation
4. **Fallback**: Keep manual mode available

---

## Appendix A: Glossary

| Term | French | Description |
|------|--------|-------------|
| Sequence | Séquence | Production order (CuttingRequest) |
| Serie | Série | Single spreading+cutting work item |
| Spreading | Matelassage | Laying fabric layers |
| Cutting | Coupe | Machine cutting operation |
| Box | Boîte | Physical container for finished pieces |
| Placement | Placement | Cutting pattern file |
| Layer | Couche (nbrCouche) | Number of fabric layers |
| Roll | Rouleau | Fabric roll |

---

## Appendix B: Status State Machine

```
                    Serie Lifecycle
                    
    ┌──────────────────────────────────────────────────┐
    │                                                  │
    │  MATELASSAGE                    COUPE            │
    │  ═══════════                    ═════            │
    │                                                  │
    │  ┌─────────┐   start    ┌─────────────┐         │
    │  │ Waiting ├───────────►│ In progress │         │
    │  └─────────┘            └──────┬──────┘         │
    │       │                        │                │
    │       │                    complete             │
    │       │                        │                │
    │       │                        ▼                │
    │       │                 ┌──────────┐            │
    │       │                 │ Complete │            │
    │       │                 └────┬─────┘            │
    │       │                      │                  │
    │       │     ┌────────────────┘                  │
    │       │     │                                   │
    │       ▼     ▼                                   │
    │                                                 │
    │  ┌─────────┐   start    ┌─────────────┐         │
    │  │ Waiting ├───────────►│ In progress │         │
    │  └─────────┘            └──────┬──────┘         │
    │                                │                │
    │                            complete             │
    │                                │                │
    │                                ▼                │
    │                         ┌──────────┐            │
    │                         │ Complete │            │
    │                         └──────────┘            │
    │                                                  │
    └──────────────────────────────────────────────────┘
```

---

## Appendix C: Decision Log

| Decision | Rationale | Date |
|----------|-----------|------|
| Use existing Ordonnancement.js patterns | Proven logic, reduce risk | Jan 2026 |
| Box duration as primary KPI | Client requirement | Jan 2026 |
| 3-phase optimization (MAX→MEDIAN→VARIANCE) | Progressive improvement | Jan 2026 |
| Material delays via constraints | Unified constraint model | Jan 2026 |
| Real-time state every 30s | Balance freshness vs. load | Jan 2026 |

---

**Document Status**: Ready for Review  
**Next Steps**: 
1. Technical review with development team
2. Business validation with production managers
3. Sprint planning for Phase 1 implementation
