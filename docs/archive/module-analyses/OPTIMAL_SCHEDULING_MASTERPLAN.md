# Optimal Scheduling Master Plan — LEAR Corporation Trim Cutting Production

**Author:** Mouad El Ghazi  
**Created:** March 2026  
**Status:** Phase 2 In Progress  
**Context:** MG-CMS Advanced Ordonnancement Evolution  
**Goal:** Maximum flexibility and optimal ordering across all machines, series, and boxes  
**Last Updated:** Phase 1 ✅ Complete, Phase 2 🔄 Steps 2.1–2.6 implemented

---

## Table of Contents

1. [Problem Statement & Context](#1-problem-statement--context)
2. [Current System Capabilities](#2-current-system-capabilities)
3. [Core Scheduling Challenges](#3-core-scheduling-challenges)
4. [Scheduling Techniques & Algorithms](#4-scheduling-techniques--algorithms)
5. [Flexibility Mechanisms](#5-flexibility-mechanisms)
6. [Optimal Ordering Strategy](#6-optimal-ordering-strategy)
7. [Multi-Objective Optimization Framework](#7-multi-objective-optimization-framework)
8. [Real-Time Adaptive Scheduling](#8-real-time-adaptive-scheduling)
9. [Implementation Plan](#9-implementation-plan)
10. [Open Questions & Specifications Needed](#10-open-questions--specifications-needed)
11. [KPIs & Success Metrics](#11-kpis--success-metrics)
12. [Risk Matrix](#12-risk-matrix)
13. [Vue Réel — Zone Layout Redesign](#13-vue-réel--zone-layout-redesign)
14. [Roll Demand Forecast & Material Availability](#14-roll-demand-forecast--material-availability)
15. [Follow-Up Questions](#15-follow-up-questions)
16. [Consolidated 5-Phase Implementation Plan](#16-consolidated-5-phase-implementation-plan)

---

## 1. Problem Statement & Context

### The Production Pipeline
```
Material Rolls → Spreading (Matelassage) → Cutting (Coupe via Lectra/Gerber/LASER-DXF) → Parts → Boxes (PartNumbers)
```

### The Challenge
At LEAR Corporation's trim cutting department, the production pipeline involves:

1. **~30 machines** across multiple zones (Lectra, Lectra IP6, Gerber, LASER-DXF, DIE); in practice, Lectra/Lectra IP6/Gerber are usually grouped in the main production zones, while LASER-DXF and DIE may sit in other zones and must remain usable as fallback resources when compatible
2. **Sequences** (CuttingRequests) containing multiple **series** (individual spreading+cutting operations)
3. Each serie goes through: **Spreading → Cutting**, but the physical behavior depends on machine family: Lectra/Lectra IP6/Gerber/DIE use shared 14 m tables where one serie may spread while another cuts if enough free length remains, while LASER-DXF works on one serie at a time on the same machine/work area
4. The final output is **boxes** filled with part numbers — a box is only complete when ALL series of its sequence finish cutting
5. **Multiple shifts** (3 shifts/day: 21:55-05:45, 05:55-13:45, 13:55-21:45)

### Why We Need Optimal Ordering
- **Too many variables**: N machines × M series × K sequences × material types × table lengths × shift constraints
- **Competing objectives**: Fast box completion vs. machine utilization vs. material changeover minimization
- **Dynamic environment**: Machine breakdowns, material delays, operator availability, urgent orders
- **Current greedy algorithms** leave optimization opportunities on the table
- **No single ordering fits all situations** — we need a FLEXIBLE system that adapts

---

## 2. Current System Capabilities

### What We Have (Working)
| Capability | Component | Status |
|-----------|-----------|--------|
| Collision-free auto-dispatch | `OrdonnancementService.autoDispatch()` | ✅ Working |
| SCG ordering (Sequence Compaction Greedy) | `buildTimelineBlocks()` | ✅ Working |
| 4-view modes (Réel, Coupe, Matelassage, Table) | `AdvancedOrdonnancement.js` | ✅ Working |
| Machine queue management | `MachineQueue` entity | ✅ Working |
| Greedy optimization with Python backend | `SchedulingOptimizationService` | ✅ Working |
| Multi-zone macro scheduling | `OrdonnancementV3.js` | ✅ Working |
| Shift overflow detection | `OrdonnancementService` | ✅ Working |
| Sequence affinity scoring | Auto-dispatch algorithm | ✅ Working |
| Parallel spread+cut on shared 14m tables (Lectra/IP6/Gerber/DIE) | Auto-dispatch | ✅ Working |
| Single-serie layer-by-layer spread+cut on LASER-DXF | Auto-dispatch | ✅ Working |
| Incremental refresh (5-min) | Timeline API | ✅ Working |

### What's Missing for Optimal Ordering
| Gap | Impact | Priority |
|-----|--------|----------|
| No multi-algorithm selector | Users stuck with one strategy | **CRITICAL** |
| No constraint solver (OptaPlanner/OR-Tools) | Greedy ≠ optimal | **HIGH** |
| No what-if simulation | Can't compare alternatives | **HIGH** |
| No material changeover cost modeling | Unnecessary material switches waste time | **HIGH** |
| No priority weighting system | All series treated equally | **MEDIUM** |
| No drag-and-drop reordering | Manual override is clumsy | **MEDIUM** |
| No multi-shift horizon planning | Plans only current shift | **MEDIUM** |
| No ML-based time estimation | Static coefficients are inaccurate | **LOW** |
| No persistence of dispatch results | Lost on refresh | **HIGH** |
| No roll stock visibility in scheduling | Cannot detect material shortages before dispatch | **HIGH** |
| No material demand forecasting | Planners discover shortages too late | **HIGH** |
| No physical zone layout in Vue Réel | Cannot see roll locations and machine layout per zone | **MEDIUM** |

---

## 3. Core Scheduling Challenges

### Challenge 1: Box Completion Time Minimization
```
Sequence SEQ-123 has 8 series: S1, S2, S3, S4, S5, S6, S7, S8
  - 6 are on Machine AA1 (same zone)
  - 2 are on Machine AA2 (same zone)

Box is ONLY complete when ALL 8 series finish cutting.
→ We must minimize MAX(dateFinCoupe) across all series of a sequence
→ This means GROUPING series of the same sequence on fewer machines is critical
→ BUT we must balance with machine utilization
```

### Challenge 2: Multi-Machine Multi-Serie Ordering
```
3 Machines: AA1, AA2, AA3
12 Series to schedule from 4 sequences:
  SEQ-A: S1(10min), S2(15min), S3(8min)    → Box needs all 3
  SEQ-B: S4(20min), S5(12min), S6(25min)   → Box needs all 3
  SEQ-C: S7(5min), S8(30min)               → Box needs both
  SEQ-D: S9(18min), S10(10min), S11(22min), S12(7min) → Box needs all 4

Which order on which machine minimizes total box completion time?
→ This is a variant of the Job Shop Scheduling Problem (JSSP)
→ NP-hard in general, but near-optimal solutions are achievable
```

### Challenge 3: Table Physical Constraints
```
Table AA1: 14m total
Currently occupied: CUTTING 3.2m + READY_TO_CUT 5.1m = 8.3m occupied
Available: 5.7m
→ Cannot schedule a serie with longueur > 5.7m until cutting finishes
→ A new spreading operation may start in parallel with an ongoing cutting operation only if the remaining free length is enough for the new serie
→ This free-space overlap rule applies to Lectra/Lectra IP6/Gerber/DIE shared tables; LASER-DXF is scheduled as one active serie at a time
→ Must model occupied length over TIME (dynamic constraint)
```

### Challenge 4: Spreading → Cutting Dependency by Machine Family
```
For Lectra, Lectra IP6, Gerber, and DIE, each 14m table has:
  - a spreading head at the beginning of the table
  - a cutting head at the end of the table

This means the table is not a single serial resource.
Spreading for Serie S2 can run in parallel while Serie S1 is already cutting, as long as the free linear space on the table is sufficient.

Example:
  Serie S1: Spreading = 45min, Cutting = 20min, Occupied length during cutting = 6m
  Serie S2: Spreading = 30min, Cutting = 35min, Required spreading length = 5m
  Table length = 14m

Case A: Sequential model (too restrictive)
  S1 spread → S1 cut → S2 spread → S2 cut = 45 + 20 + 30 + 35 = 130min

Case B: Real table model with overlap
  - S1 spreads first
  - Once S1 reaches cutting at the table end, S1 can start cutting
  - During S1 cutting, S2 spreading can start at the table beginning
  - This is feasible because 14m - 6m = 8m free, which is enough for S2's 5m spread
  Resulting makespan = 45 + max(20, 30) + 35 = 110min

Case C: No overlap possible
  If the cutting serie occupies too much table length, or the new serie needs more free length than is available, S2 must wait.

LASER-DXF is different:
  - spreading and cutting happen on the same machine, in the same working area
  - the machine spreads one layer, cuts it, then continues with the next layer of the same serie
  - this is internal parallelism for one serie only, not true concurrency between two different series

LASER-DXF scheduling rule:
  Serie S1 can run on LASER-DXF
  Serie S2 cannot start spreading on that same LASER-DXF until S1 is fully finished
  → LASER-DXF must be treated as a single-serie resource at scheduling level

→ The real dependency is not simply "spread everything before any cutting".
→ The correct constraint depends on machine family:
    - Lectra/Lectra IP6/Gerber/DIE: a serie must finish its own spreading before its own cutting starts, but another serie may spread in parallel if enough free table length remains
    - LASER-DXF: the machine can only work on one serie at a time, even though spreading/cutting are internally coupled layer by layer
→ Optimal assignment therefore depends on machine family, machine availability, residual free table length over time, and sequence grouping.
```

### Challenge 5: Material Changeover Cost
```
Machine AA1 queue: S1(Material-A), S3(Material-B), S5(Material-A)
→ 2 material changes (A→B, B→A) × ~2min setup = ~4min wasted
  (changeover = time to find the roll in the rollLocation, carry it, and place it at the spreading head)

Better ordering: S1(Material-A), S5(Material-A), S3(Material-B)
→ 1 material change × ~2min setup = ~2min wasted
→ Save ~2 minutes by grouping same-material series
```

### Challenge 6: Material Stock Visibility & Roll Availability
```
Each Zone has a property rollLocations (e.g. "reTFZ,AR1TFZ,AR2TFZ,AR3TFZ").
These are the small physical locations near the zone where rolls are staged before production.

Roll stock is tracked in the ScanRouleau entity:
  - serialId   (String, PK, prefix "S")   — unique roll barcode
  - reftissu   (String, prefix "P")        — material reference (remove "P" prefix to match partNumberMaterial)
  - emplacement (String)                   — current rollLocation code
  - lot         (String, prefix "H")       — lot/batch identifier
  - metrage     (Double)                   — remaining length in meters
  - quantite    (String)                   — quantity label
  - matricule   (Integer)                  — operator badge number who scanned
  - date        (LocalDateTime)            — last scan timestamp

Relationship chain:
  Zone.rollLocations → split(",") → list of emplacement codes
  ScanRouleauRepository.findByLocations(emplacementCodes) → rolls currently staged in that zone
  serie.partNumberMaterial (strip "P" prefix) → match against ScanRouleau.reftissu (strip "P" prefix)

The scheduling system must:
  1. At dispatch time, load all rolls from all rollLocations of each zone
  2. Know what material is available WHERE before assigning series
  3. Flag series whose material is not currently staged in any rollLocation of the target zone
  4. Compare needed vs. available for the next N hours of planned production
  5. Generate a shortage report: which materials need to come from the central stock or from another zone
```

---

## 4. Scheduling Techniques & Algorithms

### 4.1 Algorithm Catalogue

#### Level 1: Rule-Based Dispatching (Currently Implemented)

| Algorithm | Key Idea | Best When | Implementation |
|-----------|----------|-----------|----------------|
| **SCG** (Sequence Compaction Greedy) | Close nearly-done sequences first | Many partially-done sequences | ✅ Current default |
| **SPT** (Shortest Processing Time) | Shortest cutting time first | Maximize throughput, reduce WIP | Comparator change only |
| **LPT** (Longest Processing Time) | Longest cutting time first | Balance multi-machine load | Comparator change only |
| **EDF** (Earliest Due Date First) | Earliest due date first | Meet customer deadlines | Comparator change only |
| **CR** (Critical Ratio) | `(dueDate - now) / remainingTime` lowest first | Dynamic urgency awareness | Requires due date data |
| **WSPT** (Weighted SPT) | SPT with priority weights | Mix of urgency + efficiency | Needs priority weights |
| **ATC** (Apparent Tardiness Cost) | Combines due date + processing time + current time | Best single-rule for weighted tardiness | Moderate complexity |

#### Level 2: Material-Aware Ordering (NEW)

| Algorithm | Key Idea | Best When |
|-----------|----------|-----------|
| **Material-Grouped SPT** | Group series by material, then SPT within group | Many material types, reduce changeovers |
| **Material Chain Sequencing** | Find longest chain of same-material series per machine | Material changeover is expensive |
| **Traveling Salesman on Materials** | Minimize total changeover distance across materials | Large variety of materials |

#### Level 3: Sequence-Aware Optimization (NEW)

| Algorithm | Key Idea | Best When |
|-----------|----------|-----------|
| **Sequence Compaction with Look-ahead** | SCG + peek N series ahead to avoid blocking | Many sequences competing for same machines |
| **Box Completion Priority** | Score = (series_done / total_series) × urgency_weight | Box completion is #1 priority |
| **Balanced Sequence Fan-Out** | Spread sequence series across machines to parallelize | Fast box completion needed |
| **Sequence Pipeline** | Batch all series of one sequence together, pipeline across machines | When sequence grouping > parallelization |

#### Level 4: Meta-Heuristic Optimization (NEW — Near-Optimal)

| Algorithm | Key Idea | Time Complexity | Quality |
|-----------|----------|----------------|---------|
| **Multi-start Local Search (MLS)** | Try multiple initial orderings, keep best | O(n² × starts) | Good |
| **Simulated Annealing (SA)** | Accept worse solutions probabilistically, cool down | O(n² × temperature_steps) | Very Good |
| **Genetic Algorithm (GA)** | Evolve population of schedules, crossover + mutation | O(population × generations × n) | Very Good |
| **Tabu Search** | Local search with memory of recent moves | O(n² × iterations) | Very Good |
| **Ant Colony Optimization** | Probabilistic construction with pheromone trails | O(ants × iterations × n²) | Good |

#### Level 5: Constraint Programming / Exact Solvers (NEW — Optimal)

| Solver | Integration | Computation | Quality |
|--------|-------------|-------------|---------|
| **OptaPlanner** (Java native) | Direct Spring integration | Seconds to minutes | Near-optimal |
| **Google OR-Tools** | Via gRPC or JNI | Seconds to minutes | Optimal for small/medium |
| **CP-SAT** (part of OR-Tools) | Python microservice | Fast for constraint problems | Optimal |
| **CPLEX/Gurobi** (commercial) | Via API | Very fast | Optimal (expensive license) |

### 4.2 Recommended Algorithm Selection Strategy

**Hybrid Approach — 3-Tier System:**

```
┌─────────────────────────────────────────────────────────────┐
│                    USER SELECTS MODE                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🚀 QUICK MODE (< 1 second)                                │
│  ├── Rule-based dispatching                                 │
│  ├── User picks: SCG / SPT / EDF / CR / WSPT / ATC        │
│  ├── With material grouping toggle ON/OFF                   │
│  └── With sequence affinity toggle ON/OFF                   │
│                                                             │
│  ⚡ SMART MODE (5-30 seconds)                               │
│  ├── Multi-start Local Search (MLS)                         │
│  ├── Try 10+ initial orderings (SCG, SPT, LPT, EDF, random)│
│  ├── Pairwise swap improvement on best                      │
│  ├── Material changeover optimization pass                  │
│  └── Return best found schedule                             │
│                                                             │
│  🧠 OPTIMAL MODE (30 seconds - 5 minutes)                  │
│  ├── OptaPlanner constraint solver                          │
│  ├── Define hard/soft constraints declaratively              │
│  ├── Runs async with progress bar                           │
│  ├── Returns provably near-optimal schedule                 │
│  └── Can be interrupted to use best-so-far                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 Algorithm Implementation Details

#### Quick Mode — Rule-Based Dispatcher
```
FUNCTION quickDispatch(series, machines, algorithm, options):
  
  // Step 1: Sort series based on selected algorithm
  SWITCH algorithm:
    CASE "SCG": sort by doneFraction DESC, remainingCount ASC, sequence, cuttingTime ASC
    CASE "SPT": sort by cuttingTime ASC
    CASE "LPT": sort by cuttingTime DESC
    CASE "EDF": sort by dueDate ASC, dueShift ASC
    CASE "CR":  sort by criticalRatio ASC  // (dueDate-now)/remainingTime
    CASE "WSPT": sort by (weight/cuttingTime) DESC
    CASE "ATC": sort by ATC_index DESC
      // ATC_index = (weight/cuttingTime) × exp(-max(0, dueDate-now-cuttingTime) / (K × avgCuttingTime))
  
  // Step 2: Apply material grouping (optional)
  IF options.groupByMaterial:
    reorder series to minimize material changeovers while respecting primary sort
    // Use adjacent-swap approach: for each pair (i, i+1), if swapping reduces 
    // changeovers without violating priority too much, swap
  
  // Step 3: Assign to machines with sequence affinity and zone fallback
  FOR EACH serie IN sortedSeries:
    // Search order:
    // 1. compatible active machines in the serie's preferred/origin zone
    // 2. if none exist, or that zone has no active machines (status M), search other zones
    // 3. zone is a preference, not a hard restriction, unless manually locked
    bestMachine = findBestMachine(serie, machines, options.useSequenceAffinity, options.zonePolicy)
    assign(serie, bestMachine)
    registerOccupiedSlot(bestMachine, serie)
  
  RETURN schedule
```

#### Smart Mode — Multi-Start Local Search
```
FUNCTION smartOptimize(series, machines, timeLimit=30s):
  
  bestSchedule = null
  bestScore = INFINITY
  
  // Phase 1: Generate diverse initial solutions (10+ starts)
  initialOrders = [
    quickDispatch(series, machines, "SCG"),
    quickDispatch(series, machines, "SPT"),
    quickDispatch(series, machines, "LPT"),
    quickDispatch(series, machines, "EDF"),
    quickDispatch(series, machines, "CR"),
    quickDispatch(series, machines, "SCG", {groupByMaterial: true}),
    quickDispatch(series, machines, "SPT", {groupByMaterial: true}),
    randomShuffle(series, machines) × 3  // 3 random starts
  ]
  
  FOR EACH initial IN initialOrders:
    score = evaluateSchedule(initial)
    IF score < bestScore:
      bestScore = score
      bestSchedule = initial
  
  // Phase 2: Improve best solution with local search
  improved = true
  WHILE improved AND time < timeLimit:
    improved = false
    
    // Swap moves: try all pairs of series on different machines
    FOR i = 0 TO series.length - 1:
      FOR j = i + 1 TO series.length - 1:
        IF series[i].machine != series[j].machine:
          swap(series[i].machine, series[j].machine)
          newScore = evaluateSchedule(schedule)
          IF newScore < bestScore:
            bestScore = newScore
            improved = true
          ELSE:
            swap back
    
    // Insert moves: try moving each serie to each other machine
    FOR each serie:
      FOR each otherMachine:
        move(serie, otherMachine)
        newScore = evaluateSchedule(schedule)
        IF newScore < bestScore:
          bestScore = newScore
          improved = true
        ELSE:
          move back
  
  // Phase 3: Material changeover optimization pass
  FOR each machine:
    reorderMachineQueue(machine, minimizeChangeovers=true, preserveConstraints=true)
  
  RETURN bestSchedule
```

#### Optimal Mode — Constraint Solver (OptaPlanner)
```
// Hard Constraints (MUST satisfy):
HC1: No two series overlap on same machine (cutting)
HC2: On Lectra/IP6/Gerber/DIE, concurrent spreading and cutting on the same table is allowed only when total occupied length on the 14m table stays within capacity
HC3: Serie.tableLength <= machine.tableLength
HC4: Each serie must finish its own spreading before its own cutting starts
HC5: LASER-DXF may process only one serie at a time; its internal layer-by-layer spread/cut cycle does not allow overlap between different series
HC6: All estimated dates >= now
HC7: Machine must be active (status "M")
HC8: Machine type must match serie requirement (Lectra/IP6/Gerber/LASER-DXF/DIE/etc.)
HC9: Max boxes per zone not exceeded

// Soft Constraints (MINIMIZE penalty):
SC1: Total box completion time (weight: 100)  // Primary
SC2: Max single-box completion time (weight: 80)  // Avoid outliers
SC3: Material changeover count (weight: 30)
SC4: Machine utilization variance (weight: 20)  // Balance load
SC5: Sequence spread across machines (weight: 15)  // Grouping bonus
SC6: Priority weight satisfaction (weight: 50)  // Due date respect
SC7: Shift overflow minutes (weight: 40)  // Avoid overtime
SC8: Idle time between series on same machine (weight: 10)
SC9: Material availability in zone rollLocations (weight: 25)  // Prefer series whose material is already staged

// Planning Entities:
@PlanningEntity
class SerieAssignment {
  @PlanningVariable  Machine assignedMachine;
  @PlanningVariable  Integer orderOnMachine;
}

// Score Calculator:
score = -Σ(SC_i × weight_i)  // Maximize = minimize penalties
```

---

## 5. Flexibility Mechanisms

### 5.1 User-Configurable Ordering Parameters

Every parameter that affects ordering should be adjustable by the user:

```
┌──────────────────────────────────────────────────────────┐
│              SCHEDULING CONFIGURATION PANEL               │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  📊 Algorithm Mode                                       │
│  [🚀 Quick ▼]  [⚡ Smart]  [🧠 Optimal]                 │
│                                                          │
│  Quick Algorithm:                                        │
│  [SCG ▼] SCG|SPT|LPT|EDF|CR|WSPT|ATC                   │
│                                                          │
│  ──── Priority Weights (sliders) ────                    │
│  Box Completion Speed:  ████████░░  80%                  │
│  Machine Utilization:   ██████░░░░  60%                  │
│  Material Changeover:   ████░░░░░░  40%                  │
│  Due Date Respect:      ██████████  100%                 │
│  Sequence Grouping:     ███████░░░  70%                  │
│                                                          │
│  ──── Toggles ────                                       │
│  ☑ Group by material                                     │
│  ☑ Use sequence affinity                                 │
│  ☑ Prefer sequence origin zone                           │
│  ☑ Auto fallback to other active zones                   │
│  ☑ Respect shift boundaries                              │
│  ☐ Include maintenance windows                           │
│                                                          │
│  ──── Constraints ────                                   │
│  Max boxes per zone: [Auto ▼]  (= machines × 16)        │
│  Planning horizon:   [Current Shift ▼] 1 shift/1 day/3d │
│  Material changeover time: [5] minutes                   │
│  Max machine idle gap: [10] minutes                      │
│                                                          │
│  [▶ Run Optimization]  [💾 Save as Preset]               │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 5.2 Preset Profiles

Pre-configured profiles for common scenarios:

| Profile | Box Speed | Utilization | Changeover | Due Date | Grouping | Description |
|---------|-----------|-------------|------------|----------|----------|-------------|
| **Express Boxes** | 100% | 40% | 20% | 80% | 90% | Fastest possible box completion |
| **Balanced** | 70% | 70% | 50% | 70% | 70% | Good all-around performance |
| **Machine Efficiency** | 50% | 100% | 80% | 50% | 40% | Maximum machine utilization |
| **Material Saver** | 40% | 60% | 100% | 40% | 60% | Minimize material changeovers |
| **Deadline Rush** | 60% | 30% | 10% | 100% | 50% | Meet due dates at all costs |
| **Night Shift** | 80% | 60% | 60% | 60% | 80% | Fewer operators, conservative |
| **Custom** | User | User | User | User | User | Full manual control |

### 5.3 Manual Override System

Users must be able to override any automatic decision:

```
Override Level 1: LOCK a serie to a specific machine
  → Solver treats it as a fixed assignment (not movable)
  → Other series scheduled around it

Override Level 2: LOCK a serie to a specific time slot
  → Fixed start time, solver fills around it
  → Useful for urgent orders

Override Level 3: LOCK an ordering sequence
  → "S1 must be before S3 on machine AA1"
  → Precedence constraint added to solver

Override Level 4: FORCE a sequence to specific machines or another zone
  → All series of SEQ-123 go to AA1 + AA2 only, even if another zone takes the work
  → Planner can enforce manual cross-zone takeover

Override Level 5: EXCLUDE a machine
  → "Don't use AA3 for next 2 hours" (upcoming maintenance)
  → Adds exclusion interval to machine
```

### 5.4 What-If Simulation

Before committing to a schedule, users can compare alternatives:

```
┌────────────────────────────────────────────────────────┐
│              SIMULATION COMPARISON                       │
├────────────────────────────────────────────────────────┤
│                                                        │
│  Scenario A: SCG (Current)                             │
│  ├── Max box time: 4h 20min                           │
│  ├── Avg box time: 2h 45min                           │
│  ├── Material changes: 8                               │
│  ├── Machine utilization: 72%                          │
│  └── Overtime: 15 minutes                              │
│                                                        │
│  Scenario B: Smart Optimize                            │
│  ├── Max box time: 3h 50min  ✅ -30min                │
│  ├── Avg box time: 2h 30min  ✅ -15min                │
│  ├── Material changes: 5     ✅ -3                     │
│  ├── Machine utilization: 78% ✅ +6%                   │
│  └── Overtime: 0 minutes     ✅ -15min                 │
│                                                        │
│  Scenario C: Optimal (OptaPlanner)                     │
│  ├── Max box time: 3h 35min  ✅ -45min                │
│  ├── Avg box time: 2h 20min  ✅ -25min                │
│  ├── Material changes: 4     ✅ -4                     │
│  ├── Machine utilization: 81% ✅ +9%                   │
│  └── Overtime: 0 minutes     ✅ -15min                 │
│                                                        │
│  [Apply Scenario B] [Apply Scenario C] [Run New...]   │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 5.5 Dynamic Re-Scheduling Triggers

The system should automatically suggest re-optimization when:

| Trigger | Action | Urgency |
|---------|--------|---------|
| Machine goes down (status changes from M to P/R) | Re-dispatch affected series | 🔴 Immediate |
| Origin zone has no active compatible machine (no status M) | Search other zones automatically or request manual reassignment | 🔴 Immediate |
| New urgent sequence added | Insert into existing schedule | 🔴 Immediate |
| Serie finishes earlier/later than estimated | Adjust subsequent series | 🟡 Within 5 min |
| Material shortage detected | Block affected series, re-route others | 🔴 Immediate |
| Shift change approaching | Compute carryover schedule | 🟡 30 min before |
| Operator requests manual change | Re-optimize around locked changes | 🟢 On demand |
| Estimation accuracy drifts > 15% | Recalibrate coefficients | 🟡 Background |

---

## 6. Optimal Ordering Strategy

### 6.1 The Composite Scoring Function

Every scheduling decision should be driven by a single composite score that combines all objectives:

```
Score(schedule) = 
    w1 × BoxCompletionScore(schedule)
  + w2 × MachineUtilizationScore(schedule)
  + w3 × MaterialChangeoverScore(schedule)
  + w4 × DueDateScore(schedule)
  + w5 × SequenceGroupingScore(schedule)
  + w6 × ShiftOverflowScore(schedule)
  + w7 × LoadBalanceScore(schedule)
  + w8 × OriginZonePreferenceScore(schedule)

Where:
  BoxCompletionScore = -Σ(max_box_completion_time_per_sequence)
  MachineUtilizationScore = -variance(utilization_per_machine)
  MaterialChangeoverScore = -Σ(changeover_count_per_machine × CHANGEOVER_TIME)
  DueDateScore = -Σ(max(0, completion_time - due_date) × priority_weight)
  SequenceGroupingScore = +Σ(affinity_bonus when sequence series on same machine)
  ShiftOverflowScore = -max(0, total_work - shift_capacity) × OVERTIME_PENALTY
  LoadBalanceScore = -max_machine_load / min_machine_load  // Ratio penalty
  OriginZonePreferenceScore = -Σ(cross_zone_penalty when a serie leaves its preferred zone while a compatible active machine still exists there)
```

### 6.2 Ordering Phases (Recommended Pipeline)

```
Phase 0: DATA COLLECTION
├── Load all pending series (WAITING + SPREADING + READY_TO_CUT + CUTTING)
├── Fetch cutting time estimates (TimingModel priority chain)
├── Calculate spreading times (longueur × nbrCouche × COEF)
├── Load machine states (active/inactive/maintenance)
├── Load current occupied intervals per machine
├── Load constraints (pauses, strict stops, material delays)
├── Load roll stock per zone: Zone.rollLocations → ScanRouleauRepository.findByLocations()
├── Build material availability map: Map<reftissu, List<ScanRouleau>> per zone
└── Flag series with no available roll in their target zone

Phase 1: CLASSIFICATION & PRIORITIZATION
├── Fixed: series already cutting or with locked assignments
├── High Priority: series from started sequences (close-to-done sequences)
├── Urgent: series with due date in current/next shift
├── Normal: fully waiting series
└── Deferred: series with material shortage or blocked status

Phase 2: INITIAL ORDERING (Quick Mode)
├── Apply selected dispatching rule (SCG/SPT/EDF/etc.)
├── Apply material grouping if enabled
├── Prefer compatible active machines in the sequence's current/preferred zone
├── If none are active there, search compatible machines in other zones
├── Allow manual or automatic cross-zone takeover when needed
├── Apply sequence affinity bonuses
└── Generate initial feasible schedule

Phase 3: IMPROVEMENT (Smart/Optimal Mode)
├── Smart: Multi-start + local search + swap optimization
├── Optimal: Constraint solver with full model
└── Evaluate composite score

Phase 4: MATERIAL CHANGEOVER PASS
├── For each machine queue, minimize changeovers
├── Preserve hard constraints
├── Allow minor degradation of primary objective if changeover savings are significant
└── Report changeover statistics

Phase 5: FEASIBILITY CHECK
├── Verify no collisions
├── Verify table length constraints at every instant
├── Verify each serie's spreading finishes before its own cutting starts
├── Verify allowed spread/cut overlap on the same table only when free length remains sufficient
├── Verify shift boundaries
├── Verify max boxes per zone
└── Verify material availability: flag series with no roll staged in any rollLocation of the assigned zone

Phase 6: OUTPUT & COMPARISON
├── Generate Gantt visualization data
├── Calculate all KPI metrics
├── Compare with current schedule (if exists)
├── Present to user for approval
└── Allow partial acceptance (accept some, re-optimize rest)
```

### 6.3 Series Ordering Within a Machine Queue

Once series are assigned to machines, the ORDER within each machine's queue matters significantly:

```
Strategy 1: Sequence-First Ordering
  → Group all series of same sequence together
  → Within group: shortest cutting time first
  → Between groups: close-to-done sequence first
  → ✅ Fast box completion, ✅ Good grouping, ❌ May not minimize makespan

Strategy 2: Material-First Ordering
  → Group by partNumberMaterial
  → Within group: sequence-first ordering
  → ✅ Minimize changeovers, ❌ May delay box completion

Strategy 3: Interleaved Ordering (Best of Both)
  → Score each candidate serie: combinedScore = sequenceUrgency + materialAffinity + shortJobBonus
  → Pick highest score for next slot
  → Re-evaluate scores after each assignment (dynamic)
  → ✅ Balanced, ✅ Adaptive, ❌ More complex

Strategy 4: Critical Path Ordering
  → Identify which serie, if delayed, delays the most boxes
  → Schedule critical-path series first
  → Non-critical series fill gaps
  → ✅ Optimal for makespan, ❌ Requires full dependency graph
```

### 6.4 Cross-Machine Coordination

Series of the same sequence may run on different machines. Coordination is key:

```
Technique 1: Sequence Load Balancing
  → Distribute series of a sequence across 2-3 machines max
  → Ensures parallelism without too much spread
  → Target: max 3 machines per sequence

Technique 2: Staggered Starts
  → Don't start all series of a sequence simultaneously
  → Stagger by spreading time so materials aren't all needed at once
  → Reduces material queue pressure

Technique 3: Bottleneck Machine Priority
  → Identify the machine that will finish last for each sequence
  → Give that machine's series higher priority on other machines
  → This brings forward the bottleneck completion time

Technique 4: Pipeline Scheduling
  → Table AA1: spread S1, then cut S1 at the table end while spreading S3 at the table start
  → Table AA2: spread S2, then cut S2 at the table end while spreading S4 at the table start
  → Overlap spreading of the next serie with cutting of the current serie when enough free table length remains
  → Maximizes table utilization time

Technique 5: Cross-Zone Fallback
  → Try the sequence's preferred zone first
  → If the needed compatible machine type is unavailable there, or all machines are stopped, search other zones
  → Sequence zone is a preference, not a hard rule
  → A chef de zone may manually take another zone's sequence, and the optimizer should also support this automatically when needed
```

---

## 7. Multi-Objective Optimization Framework

### 7.1 Pareto Optimization

Instead of combining all objectives into one score, offer Pareto-optimal solutions:

```
Solution A: Box time = 3h, Utilization = 85%, Changeovers = 3
Solution B: Box time = 2.5h, Utilization = 75%, Changeovers = 6
Solution C: Box time = 3.5h, Utilization = 90%, Changeovers = 2

→ No solution dominates all others
→ Present all Pareto-optimal solutions to user
→ User picks based on current priority
```

### 7.2 Constraint Hierarchy

```
LEVEL 1 — HARD CONSTRAINTS (Never violate):
  ├── No collision on same machine
  ├── Each serie must finish spreading before its own cutting starts
  ├── Spread/cut overlap on shared tables is allowed only if residual free table length stays sufficient
  ├── LASER-DXF handles only one serie at a time
  ├── Table length not exceeded
  ├── Machine type matches serie requirement
  └── Locked assignments respected

LEVEL 2 — FIRM CONSTRAINTS (Violate only with explicit user approval):
  ├── Max boxes per zone
  ├── Shift boundary respect
  ├── Machine active status
  └── Manual forced zone/machine decisions

LEVEL 3 — SOFT CONSTRAINTS (Minimize violation):
  ├── Due date respect
  ├── Material changeover minimization
  ├── Machine load balance
  ├── Sequence grouping
  ├── Priority ordering
  └── Origin zone preference
```

### 7.3 Weighted Objective Tuning

Let users adjust weights in real-time and see impact:

```
// Expose as REST API:
POST /api/scheduling/optimize
{
  "mode": "SMART",
  "algorithm": "SCG",
  "weights": {
    "boxCompletion": 80,
    "machineUtilization": 60,
    "materialChangeover": 40,
    "dueDateRespect": 100,
    "sequenceGrouping": 70,
    "shiftOverflow": 50,
    "loadBalance": 30
  },
  "constraints": {
    "maxBoxesPerZone": "auto",
    "planningHorizon": "CURRENT_SHIFT",
    "materialChangeoverMinutes": 2,
    "maxIdleGapMinutes": 10
  },
  "zonePolicy": "PREFER_ORIGIN_ZONE_FALLBACK_TO_OTHER_ACTIVE_ZONES",
  "autoCrossZoneFallback": true,
  "locks": [
    {"serieId": "S001", "machine": "AA1", "lockType": "MACHINE"},
    {"serieId": "S005", "startTime": "2026-03-30T14:00:00", "lockType": "TIME"}
  ],
  "excludedMachines": ["AA3"],
  "forceSequenceMachines": {
    "SEQ-123": ["AA1", "AA2"]
  }
}
```

---

## 8. Real-Time Adaptive Scheduling

### 8.1 Continuous Optimization Loop

```
EVERY 2 MINUTES (configurable):
  1. Detect changes since last optimization
     ├── Series status changes (new completions, new starts)
     ├── Machine status changes (breakdowns, restarts)
     ├── New sequences added
     └── Material availability changes
  
  2. IF significant changes detected:
     a. Keep locked/in-progress assignments
     b. Re-optimize remaining WAITING series only
     c. Compare new schedule with current
     d. IF improvement > THRESHOLD (5%):
        → Notify user: "Better schedule available"
        → Show comparison
        → Let user accept or reject
     e. IF critical change (machine down):
        → Auto-apply emergency re-schedule
        → Notify all affected operators
```

### 8.2 Estimation Feedback Loop

```
WHEN serie completes cutting:
  actualTime = dateFinCoupe - dateDebutCoupe
  estimatedTime = timingModel estimate
  error = actualTime - estimatedTime
  errorPercent = error / estimatedTime × 100
  
  // Store for learning
  INSERT INTO estimation_feedback (serie, placement, material, longueur, nbrCouche, 
    estimatedMinutes, actualMinutes, errorPercent, machine, timestamp)
  
  // If error is systematic, adjust coefficient
  IF avg(errorPercent for similar series in last 7 days) > 10%:
    suggestedCoefficient = avg(actual/estimated for similar series)
    NOTIFY: "Cutting time estimation for {material} on {machineType} is off by {avg_error}%. 
             Suggest coefficient adjustment: {suggestedCoefficient}"
```

### 8.3 Operator Interaction Model

```
OPERATOR VIEW (simplified mobile/tablet):
┌─────────────────────────────────────────┐
│  Machine AA1                    Shift 2  │
├─────────────────────────────────────────┤
│  NOW: S1234 (Cutting)                    │
│  ├── Material: REF-A-2345               │
│  ├── Progress: ████████░░ 80%           │
│  └── ETA: 15 min                         │
│                                          │
│  NEXT UP:                                │
│  1. S1237 | REF-A-2345 | 4.2m | ~25min  │
│  2. S1240 | REF-B-9901 | 3.1m | ~18min  │
│  3. S1242 | REF-B-9901 | 5.5m | ~32min  │
│                                          │
│  ⚠ Material change after #1 → #2       │
│     (REF-A-2345 → REF-B-9901)           │
│                                          │
│  [✓ Confirm S1234 Done]                  │
│  [⏸ Report Issue]                        │
│  [🔄 Request Reorder]                    │
└─────────────────────────────────────────┘
```

---

## 9. Implementation Plan

### Phase 1: Foundation — Multi-Algorithm Selector (2-3 weeks)

**Goal:** Allow users to choose between different dispatching rules in the existing UI.

**Tasks:**
- [ ] Add algorithm selector dropdown to AdvancedOrdonnancement controls
- [ ] Implement SPT, LPT, EDF, CR, WSPT comparators in `OrdonnancementService`
- [ ] Add material changeover cost calculation to existing `buildTimelineBlocks()`
- [ ] Add material-grouped ordering option
- [ ] Create `SchedulingConfig` entity to persist user preferences
- [ ] Add preset profiles (Express, Balanced, Efficient, etc.)
- [ ] Update auto-dispatch to accept algorithm parameter
- [ ] Unit tests for each algorithm

**Backend Changes:**
```
OrdonnancementService.java:
  + autoDispatch(String algorithm, Map<String, Integer> weights)
  + quickDispatch(List<Serie> series, List<Machine> machines, String algorithm, DispatchOptions options)
  + evaluateSchedule(Schedule schedule, Map<String, Integer> weights) → double score

New files:
  + domain/SchedulingConfig.java
  + domain/SchedulingPreset.java
  + services/DispatchAlgorithms.java (all comparators/sorters)
```

**Frontend Changes:**
```
AdvancedOrdonnancement.js:
  + Algorithm selector dropdown
  + Weight sliders panel
  + Preset profile buttons
  + Score display after dispatch
```

---

### Phase 2: Smart Optimization — MLS + Material Awareness (2-3 weeks)

**Goal:** Implement Multi-start Local Search for significantly better schedules.

**Tasks:**
- [ ] Implement Multi-start Local Search (MLS) algorithm
- [ ] Add swap and insert neighborhood moves
- [ ] Implement material changeover tracking and optimization pass
- [ ] Add async execution with progress reporting (reuse existing `@Async` pattern)
- [ ] Create schedule evaluation function with composite scoring
- [ ] Add what-if simulation (run multiple scenarios, compare results)
- [ ] Add schedule comparison UI

**Backend Changes:**
```
New files:
  + services/scheduling/SmartOptimizer.java
  + services/scheduling/ScheduleEvaluator.java
  + services/scheduling/MaterialChangeoverOptimizer.java
  + services/scheduling/NeighborhoodMoves.java
```

---

### Phase 3: Manual Override & Locking System (1-2 weeks)

**Goal:** Full manual override capabilities for operators and planners.

**Tasks:**
- [ ] Implement serie-to-machine locking (persist in database)
- [ ] Implement serie time-slot locking
- [ ] Implement sequence-to-machine-set forcing
- [ ] Implement machine exclusion intervals
- [ ] Add drag-and-drop reordering within machine queue (frontend)
- [ ] Add precedence constraint definition UI
- [ ] Respect all locks during optimization

**Backend Changes:**
```
New entity:
  + domain/SchedulingLock.java (serieId, lockType, machine, startTime, endTime)
  
Updated:
  + OrdonnancementService: respect locks in all dispatch methods
  + SmartOptimizer: treat locked series as fixed
```

---

### Phase 4: Persist & Compare Schedules (1-2 weeks)

**Goal:** Save optimization results and allow comparison between plans.

**Tasks:**
- [ ] Persist auto-dispatch results to `OptimizedPlan` entity (already exists, needs integration)
- [ ] Add "Save Plan" button that persists all estimated dates
- [ ] Add "Load Plan" to restore a previous plan
- [ ] Add "Compare Plans" view showing side-by-side metrics
- [ ] Add plan versioning (timestamp-based)
- [ ] Add plan activation (one active plan per zone)
- [ ] Show metrics delta between current and proposed plan

---

### Phase 5: Constraint Solver Integration — OptaPlanner (3-4 weeks)

**Goal:** Introduce proper constraint solver for provably near-optimal results.

**Tasks:**
- [ ] Add OptaPlanner dependency to pom.xml
- [ ] Define `@PlanningSolution`, `@PlanningEntity`, `@PlanningVariable` annotations
- [ ] Implement `ConstraintProvider` with all hard and soft constraints
- [ ] Create async solver execution with progress reporting
- [ ] Add termination conditions (time limit, score improvement threshold)
- [ ] Integrate solver results into existing Gantt/timeline views
- [ ] Benchmark: compare OptaPlanner vs Smart vs Quick on real data

**New Files:**
```
domain/solver/
  + CuttingScheduleSolution.java (@PlanningSolution)
  + SerieAssignmentEntity.java (@PlanningEntity)
  + CuttingConstraintProvider.java (constraint rules)
  + SolverConfigFactory.java (solver configuration)

services/solver/
  + OptaPlannerService.java (solver execution, async)
  + SolverResultConverter.java (convert to timeline format)
```

---

### Phase 6: Vue Réel Redesign & Roll Stock Integration (2-3 weeks)

**Goal:** Physical zone layout view with roll location visibility and material demand forecasting.

**Tasks:**
- [ ] Redesign Vue Réel to render one zone at a time (or all zones stacked, collapsible)
- [ ] Add roll location squares (left panel) from `Zone.rollLocations`, showing roll count per location
- [ ] Add click-to-open modal listing all `ScanRouleau` rows at that emplacement
- [ ] Render machines with spreading head (left) and cutting head (right) for Lectra/IP6/Gerber/DIE
- [ ] Render LASER-DXF as a single unified block with layer progress indicator
- [ ] Load roll stock at dispatch time: `ScanRouleauRepository.findByLocations(zone.rollLocations.split(","))`
- [ ] Build material availability map: `Map<reftissu, List<ScanRouleau>>` per zone
- [ ] Implement `forecastMaterialDemand(zone, horizonHours)` for 2/3/4/8h horizons
- [ ] Compare demand vs. stock, generate shortage report
- [ ] For shortages, check other zones and suggest cross-zone transfer vs. central stock request
- [ ] Add demand report UI panel with horizon selector and shortage table
- [ ] Add export: PDF and Excel for the demand/shortage report (configurable hour range)
- [ ] Flag series with no available roll in the zone and integrate with scheduling (defer/re-route)

**Backend Changes:**
```
New files:
  + services/scheduling/MaterialDemandForecastService.java
  + web/rest/MaterialDemandForecastResource.java (REST endpoint)
  + dto/MaterialDemandReportDTO.java (material, needed, available, deficit, status, suggestion)

Updated:
  + OrdonnancementService: load roll stock in Phase 0, flag series without material
  + ScanRouleauRepository: existing findByLocations() is sufficient
```

**Frontend Changes:**
```
AdvancedOrdonnancement.js / VueReel.js:
  + Zone layout component with roll location squares
  + Machine rendering: spreading head (left) + table + cutting head (right)
  + LASER-DXF single-block rendering with layer progress
  + Material demand forecast panel with horizon buttons
  + Shortage report table with export buttons
  + Roll location click → modal with ScanRouleau table
```

---

### Phase 7: Real-Time Adaptation & Estimation Feedback (2-3 weeks)

**Goal:** System automatically re-optimizes and improves over time.

**Tasks:**
- [ ] Implement continuous optimization loop (configurable interval)
- [ ] Add change detection for automatic re-optimization triggers
- [ ] Implement estimation feedback loop (actual vs predicted times)
- [ ] Add coefficient auto-adjustment suggestions
- [ ] Create operator mobile view with simplified queue display
- [ ] Add WebSocket push for real-time schedule updates
- [ ] Add estimation drift alerts

---

### Phase 8: Multi-Shift & Long Horizon Planning (2-3 weeks)

**Goal:** Plan across shift boundaries and multiple days.

**Tasks:**
- [ ] Extend planning horizon: 1 shift → 1 day → 3 days → 1 week
- [ ] Model shift transitions (breaks, handovers)
- [ ] Add operator availability per shift
- [ ] Implement rolling horizon optimization
- [ ] Add shift-level summary (what carries over, what's new)
- [ ] Add weekend/holiday awareness

---

## 10. Open Questions & Specifications Needed

> **⚠️ Please fill in these answers — they directly affect algorithm design:**

### Production Questions

| # | Question | Your Answer |
|---|----------|-------------|
| Q1 | How many series are typically pending at any time? (10? 50? 200?) | 300 to 400 |
| Q2 | How many sequences are typically active simultaneously? | 20 to 40 |
| Q3 | What is the average cutting time per serie? (minutes) | 20 |
| Q4 | What is the average spreading time per serie? (minutes) | 10 |
| Q5 | How many different materials (partNumberMaterial) are used per shift? | 30 |
| Q6 | How long does a material changeover actually take? (currently assumed 5min) | 2min which is the tame to search the roll location and find and put it in the location of speading so start using it |
| Q7 | Is there a maximum number of different materials allowed on a machine per shift? | no |
| Q8 | Do certain materials have priority over others? (e.g., leather > fabric) | no |
| Q9 | Can a serie be split across two machines? (e.g., spread on AA1, cut on AA2) |  only he it needed to be then he force it but in genral we must use the same machine for cutting as spreading   |
| Q10 | Is there a maximum number of boxes that can physically be stored near machines? | yes it is 16 and this must be configurable as we did already |

### Machine Questions

| # | Question | Your Answer |
|---|----------|-------------|
| Q11 | Are all Lectra machines identical in speed, or do some cut faster? | usually it depende on the config of the serie but the cutting time is linked to the placement and one the time is validated in TimingModel then that is fixed |
| Q12 | Can a Lectra serie be cut on a Gerber machine (or vice versa)? | no |
| Q13 | Do LASER-DXF machines have different table lengths? | could be we configue each one as we did already  |
| Q14 | What is the average machine downtime per shift? (minutes) | usually we work on efficience and our target is 90% and that what have we just implemented in PlanDeCharge feature to set the target and the total for each machine |
| Q15 | How often do machines go down unexpectedly? (times per shift) | 3 |
| Q16 | Is there a preferred machine for certain materials? | airbag matearial is work  on the table of the prodcutionTable with authorisationAirbag as true and tou can do this to get the list of it : 
    public List<String> getReftissuAirbag() {
        /*
        SELECT [partNumberMaterial] FROM [dbo].[PartNumberMaterialConfig] where description like '%Airbag%'
         */
        String sql = "SELECT [partNumberMaterial] FROM [dbo].[PartNumberMaterialConfig] where description like '%Airbag%'";
        return jdbcTemplate.query(sql, (resultSet, i) -> resultSet.getString("partNumberMaterial"));

    }

 |

### Scheduling Questions

| # | Question | Your Answer |
|---|----------|-------------|
| Q17 | What is more important: fast box completion OR high machine utilization? | both we need fast box completion but without impacting the machine and the machine of cutting need to be working non stop |
| Q18 | Are there customer-specific deadlines (e.g., "Box X must ship by 14:00")? | no |
| Q19 | How far in advance should the schedule plan? (1 shift, 1 day, 3 days?) | 1 day |
| Q20 | Should the system auto-apply optimized schedules or always require approval? | configure it on to by checking a switch to anable it |
| Q21 | How many operators are available per shift? Is this a constraint? | we have 4 opeartor working 5spreading machine 2 required in each machine (so they keep iterating) to work and 1 in cutting and 2 picking keep iteraing , and this is we suppode we have those 5 machine in a zone  , this need to be configurable|
| Q22 | Do operators have machine specializations? (e.g., only operator X can run IP6) | no |
| Q23 | Is there a priority system for sequences? (e.g., VIP customer, standard, low) | the ROLE_CHEF_EQUIPE can do that to give priority to plan, bu other than this the order must be by the shift then the next and so one. and our priority is finishing the sequence that aldready started faster with add ing the new sequence and finishing then also faster |
| Q24 | Should overtime be avoided, or is it acceptable if it improves box completion? | it is acceptable and also the a sequence could be stopped to be completed laster in case there is no material availbe and that sequence will stay incomplete until the material has arrived |

### Data & Integration Questions

| # | Question | Your Answer |
|---|----------|-------------|
| Q25 | Is TimingModel cutting time estimation generally accurate? (within 10%? 20%?) | yes |
| Q26 | Is historical actual-vs-estimated data available for ML training? | i have the table of history for each spreading (in splice database cj=heck the marker log which show the work  for each serie which is the workOrderCode and the placemetn is in the Marker column and check other column for more details of spreading) and cutting  (like CoupePrefomance) |
| Q27 | Is there an ERP/SAP system that provides due dates and priorities? | no only the CuttingRequest table that contains the dueDate and dueShift of each sequence |
| Q28 | Should the optimized plan be exportable (PDF, Excel)? | yes with also the option ot get only the a specifique number of hours |
| Q29 | How many concurrent users will view/edit the schedule? | must be only one persone at the time but flexible for more to view only |
| Q30 | Is internet available for cloud solver APIs, or must it be fully on-premise? | no we can't use any external solution , is must be all in our server |

---

## 11. KPIs & Success Metrics

### Primary KPIs

| KPI | Formula | Target | Current |
|-----|---------|--------|---------|
| **Max Box Completion Time** | `max(dateFinCoupe_last - dateDebutMatelassage_first)` per sequence | < 4 hours | ? |
| **Average Box Completion Time** | Mean of all sequence completion times | < 2.5 hours | ? |
| **Box Completion Time Variance** | Std dev of sequence completion times | < 30 min | ? |
| **Machine Utilization** | `cutting_time / available_time × 100` per machine | > 80% | ? |
| **Utilization Balance** | `max_utilization - min_utilization` across machines | < 15% | ? |
| **Material Changeover Count** | Total changeovers per shift per machine | < 5 per machine | ? |
| **Due Date Adherence** | % of sequences completed by due shift | > 95% | ? |
| **Schedule Accuracy** | `abs(estimated - actual) / estimated × 100` | < 10% | ? |
| **Shift Overflow Rate** | % of shifts with work overflowing | < 5% | ? |

### Secondary KPIs

| KPI | Formula | Target |
|-----|---------|--------|
| Optimization time (Smart mode) | Wall clock time for optimization | < 30 seconds |
| Optimization time (Optimal mode) | Wall clock time for solver | < 5 minutes |
| Re-optimization frequency | Times schedule changes per shift | < 5 |
| Manual override rate | % of series manually re-assigned | < 10% |
| WIP (Work in Progress) | Number of incomplete boxes at any time | Minimize |

### Measurement Dashboard

```
┌──────────────────────────────────────────────────────────────┐
│                   SCHEDULING KPIs                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  📦 Box Completion      ⚙️ Machine Utilization               │
│  Max:  3h 45min         AA1: ████████░░ 82%                 │
│  Avg:  2h 20min         AA2: ███████░░░ 75%                 │
│  σ:    18 min           AA3: ████████░░ 84%                 │
│                         AA4: ██████░░░░ 68%                 │
│  📅 Due Dates           AA5: ████████░░ 80%                 │
│  On time: 97%                                                │
│  Late: 1 sequence       🔧 Changeovers: 2.1/machine/shift   │
│                                                              │
│  📊 Schedule Accuracy   ⏱ Last Optimization                  │
│  Avg error: 8.5%        Mode: Smart | Time: 12s             │
│  Trend: ↗ improving     Score: 847 (vs 792 previous)        │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 12. Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| OptaPlanner adds complexity | Medium | Medium | Start with Smart mode, add OptaPlanner later |
| Users reject automated scheduling | Medium | High | Always allow manual override, show improvements |
| Estimation inaccuracy invalidates plans | High | Medium | Feedback loop + coefficient auto-adjustment |
| Too many parameters confuse users | Medium | Medium | Preset profiles, sensible defaults |
| Solver takes too long | Low | High | Time limits, best-so-far interruption |
| Machine breakdowns invalidate plan | High | Medium | Automatic re-optimization triggers |
| Material shortages not predicted | Medium | High | Material availability integration |
| Concurrent edit conflicts | Low | Medium | Optimistic locking, merge strategy |

---

## Summary of Techniques Available

```
LEVEL 1 — DISPATCHING RULES (instant, good baseline):
  SCG, SPT, LPT, EDF, CR, WSPT, ATC
  + Material grouping, sequence affinity

LEVEL 2 — LOCAL SEARCH (seconds, significant improvement):
  Multi-start Local Search, Simulated Annealing, Tabu Search
  + Swap/insert/reorder neighborhood moves
  + Material changeover optimization pass

LEVEL 3 — CONSTRAINT SOLVER (minutes, near-optimal):
  OptaPlanner, Google OR-Tools, CP-SAT
  + Full constraint model with hard/soft constraints
  + Provably near-optimal with improvement guarantees

LEVEL 4 — MACHINE LEARNING (background, continuous improvement):
  Estimation feedback loop, coefficient auto-adjustment
  + Historical data collection for future ML models

LEVEL 5 — HUMAN-IN-THE-LOOP (always available):
  Manual locks, drag-and-drop, what-if simulation
  + Preset profiles, weight sliders
  + Operator mobile view with queue management

ALL LEVELS WORK TOGETHER in a hybrid system.
The user chooses the mode, the system does the rest.
```

---

## 13. Vue Réel — Zone Layout Redesign

The "Vue Réel" must render each zone separately with a physical representation of the shop floor.

### 13.1 Zone Layout Structure

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ZONE: TFZ                                                                      │
├──────┬───────────────────────────────────────────────────────────────────────────┤
│      │                                                                          │
│ Roll │   ┌──────────────┐  Spreading head           Cutting head  ┌──────────┐ │
│ Locs │   │  ◄ SPREAD    │◄──────── 14m table ──────────►│ CUT ►    │          │ │
│      │   │   S1234      │   S1234 spreading              S1230   │ Machine  │ │
│ ┌──┐ │   │   LECTRA     │                                cutting  │ AA1      │ │
│ │re│ │   └──────────────┘                                         └──────────┘ │
│ │TF│ │                                                                          │
│ │Z │ │   ┌──────────────┐                                         ┌──────────┐ │
│ │4 │ │   │  ◄ SPREAD    │◄──────── 14m table ──────────►│ CUT ►    │          │ │
│ └──┘ │   │   S1238      │                                         │ Machine  │ │
│ ┌──┐ │   │   LECTRA IP6 │                                         │ AA2      │ │
│ │AR│ │   └──────────────┘                                         └──────────┘ │
│ │1T│ │                                                                          │
│ │FZ│ │   ┌──────────────────────────────────────────────────────────────────┐   │
│ │3 │ │   │  LASER-DXF (single work area — spread+cut same serie only)      │   │
│ └──┘ │   │   S1241 — layer 3/8                                              │   │
│ ┌──┐ │   │   Machine LX1                                                   │   │
│ │AR│ │   └──────────────────────────────────────────────────────────────────┘   │
│ │2T│ │                                                                          │
│ │FZ│ │                                                                          │
│ │2 │ │                                                                          │
│ └──┘ │                                                                          │
│      │                                                                          │
└──────┴───────────────────────────────────────────────────────────────────────────┘
```

### 13.2 Roll Location Panel (Left Side)

Each zone's `rollLocations` (from `Zone.rollLocations`, comma-separated) are shown as stacked squares on the left:

- Each square shows the **location code** and the **count of rolls** currently in it
- Clicking a square opens a **modal** with a table listing every `ScanRouleau` at that emplacement:
  | serialId | reftissu | lot | metrage | date |
  |----------|----------|-----|---------|------|
  | S00123   | PREF-A   | H45 | 12.5m   | ...  |
- Rolls are fetched via `ScanRouleauRepository.findByLocations([locationCode])`

### 13.3 Machine Rendering (Right Side)

For **Lectra / Lectra IP6 / Gerber / DIE**:
- The **spreading head** is shown on the **left** of the table
- The **cutting machine** is shown on the **right** of the table
- The 14m table is drawn between them, with occupied length visualized
- If a serie is spreading and another is cutting simultaneously, both are shown

For **LASER-DXF**:
- A single work area block represents the machine
- Only one serie is shown (the active one), with its layer progress (e.g. "layer 3/8")
- No split between spreading/cutting heads — it is a unified block

### 13.4 Zone Selector

- A zone dropdown/tabs at the top allows switching between zones
- Or show all zones stacked vertically with collapse/expand
- Each zone is self-contained with its own roll locations and machines

---

## 14. Roll Demand Forecast & Material Availability

### 14.1 Purpose

Before production starts (or during a shift), the planner needs to know:
- What materials are needed for the next **N hours** (configurable: 2, 3, 4, 8 hours)?
- How much of each material is already staged in the zone's rollLocations?
- What is missing and needs to come from the **central stock** or from **another zone**?

### 14.2 Forecast Algorithm

```
FUNCTION forecastMaterialDemand(zone, horizonHours):

  // Step 1: Get the ordered series for the next N hours
  //   These are the series that the scheduling algorithm has placed
  //   in the planning horizon for this zone's machines
  plannedSeries = getPlannedSeriesForZone(zone, now, now + horizonHours)

  // Step 2: Group by material (partNumberMaterial, strip "P" prefix)
  demandMap = {}
  FOR EACH serie IN plannedSeries:
    material = stripPrefix(serie.partNumberMaterial, "P")
    demandMap[material].totalMetrage += serie.longueur × serie.nbrCouche
    demandMap[material].serieCount += 1
    demandMap[material].serieList.add(serie)

  // Step 3: Get current roll stock in the zone's rollLocations
  locations = zone.rollLocations.split(",")
  rolls = ScanRouleauRepository.findByLocations(locations)
  stockMap = {}
  FOR EACH roll IN rolls:
    material = stripPrefix(roll.reftissu, "P")
    stockMap[material].totalMetrage += roll.metrage
    stockMap[material].rollCount += 1
    stockMap[material].rollList.add(roll)

  // Step 4: Compare demand vs. stock
  report = []
  FOR EACH material IN demandMap.keys():
    needed = demandMap[material].totalMetrage
    available = stockMap[material].totalMetrage OR 0
    deficit = max(0, needed - available)
    report.add({
      material, needed, available, deficit,
      serieCount: demandMap[material].serieCount,
      rollCount: stockMap[material].rollCount,
      status: deficit > 0 ? "SHORTAGE" : "OK"
    })

  // Step 5: For shortages, check other zones
  FOR EACH item IN report WHERE item.status == "SHORTAGE":
    FOR EACH otherZone IN allZones WHERE otherZone != zone:
      otherLocations = otherZone.rollLocations.split(",")
      otherRolls = ScanRouleauRepository.findByLocations(otherLocations)
                   .filter(r -> stripPrefix(r.reftissu, "P") == item.material)
      IF otherRolls.totalMetrage >= item.deficit:
        item.suggestion = "Transfer from zone " + otherZone.nom
        item.suggestedRolls = otherRolls
      ELSE:
        item.suggestion = "Request from central stock"

  RETURN report
```

### 14.3 Demand Report UI

```
┌────────────────────────────────────────────────────────────────────────┐
│  MATERIAL DEMAND FORECAST — Zone TFZ — Next 4 hours                   │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Horizon: [2h] [3h] [4h▼] [8h]                                        │
│                                                                        │
│  Material       │ Needed  │ In Zone │ Deficit │ Status    │ Action     │
│  ───────────────┼─────────┼─────────┼─────────┼───────────┼──────────  │
│  REF-A-2345     │ 85.0m   │ 92.3m   │ 0       │ ✅ OK     │            │
│  REF-B-9901     │ 120.0m  │ 45.0m   │ 75.0m   │ ⚠ SHORT  │ [Zone B]   │
│  REF-C-1100     │ 30.0m   │ 0       │ 30.0m   │ 🔴 NONE  │ [Stock]    │
│  REF-D-5500     │ 55.0m   │ 60.0m   │ 0       │ ✅ OK     │            │
│                                                                        │
│  Summary: 4 materials needed, 2 shortages, 1 not in any zone          │
│                                                                        │
│  [📄 Export PDF]  [📊 Export Excel]  [🔄 Refresh Stock]               │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### 14.4 Integration with Scheduling

- The demand forecast runs **after** the scheduling algorithm has ordered series
- Series flagged as `SHORTAGE` can be:
  - Deferred: moved to end of the queue until material arrives
  - Cross-zone: re-assigned to a zone that already has the material staged
  - Blocked: marked as waiting for material (status change)
- The composite scoring function can include a **material availability bonus**: prefer orderings where the next series to run already have rolls staged in the zone

---

## 15. Follow-Up Questions

> **Based on the filled-in answers, some clarifications are needed:**

| # | Question | Context |
|---|----------|---------|
| FQ1 | For `ScanRouleau.reftissu`, is the "P" prefix always present, or only sometimes? Should we always strip it before matching against `partNumberMaterial`? | Matching rolls to series | yes
| FQ2 | The `ScanRouleau.metrage` field — is this the **remaining** usable metrage on the roll, or the **original** total? | Demand forecast accuracy | remaining
| FQ3 | When a roll is consumed (fully used), is the `ScanRouleau` record deleted, or does it stay with `metrage = 0`? How do we know a roll is exhausted? | Stock calculation | deleted 
| FQ4 | The `Zone.rollLocations` format `"reTFZ,AR1TFZ,AR2TFZ,AR3TFZ"` — are these always comma-separated with no spaces? Any edge cases? | Parsing logic | always comma-separated
| FQ5 | You mentioned 4 operators for 5 spreading machines (2 required per machine). Does this mean 2–3 machines are idle at any given time due to staff shortage? Should the optimizer consider operator count as a constraint? | Operator modeling | yes because we could have absent, ingeneral we need two spreading operator so have the spreading working and 1 operator that need to be focus on a machine  of cutting and 1 who do the picking between two machines
| FQ6 | For the roll demand forecast, should the export (PDF/Excel) include only the shortage summary, or also the full list of individual rolls and series? | Export scope | yes only theserie that are waiting and way schedeuled for a spreading 
| FQ7 | `CuttingRequest.dueDate` and `dueShift` — is `dueShift` an integer (1/2/3) or a string? What format is `dueDate`? | Sorting by due date | dueDate is a localDate and dueShift is int
| FQ8 | You mentioned sequences can be **stopped** when material is unavailable and completed later. Is there a status field on CuttingRequest that tracks this (e.g., "PAUSED", "WAITING_MATERIAL")? | Deferred scheduling | there is not it is better to create one for sequence status to filter those sequence in ordering
| FQ9 | For the "Vue Réel" — should the zone layout show the real physical arrangement of machines (left-to-right, their actual positions), or is a standardized vertical stack sufficient? | UI realism vs. simplicity | could we have both , and also can we let the process user do the layout and changinf it by some grag and drop of the element and drawing and rotating element in a seperate element for configuration in the Prcess section
| FQ10 | Airbag materials can only go on tables with `autorisationAirbag = true`. Are there other material-to-machine restrictions besides airbag? | Constraint completeness | no, but we we need to be able to add restriction if we needed to on material or placement to be worked onspecifique machines
| FQ11 | The `ScanRouleau.emplacement` — can a roll move between rollLocations (e.g., from one zone's location to another)? If so, is the `emplacement` updated in real-time? | Cross-zone roll transfer | yes it is updated  but if it in use  in a machine you will find this info in the table SerieRouleauTemp, and in it are only the roll in use but when it is in use it is not deleted from the location of ScanRouleau but when done with it either will be deleted from ScanRouleau if it is finished or return to a location if the roll still have a remaning quantity
| FQ12 | For the central stock (warehouse), is there a separate table or system that tracks rolls not yet staged at any zone? Or is everything in `ScanRouleau` with different emplacement codes? | Full stock visibility | you can get the stock material with the request /api/stockStatusReport/currentStock?reftissus=PN1, PN2... and with this you can get eh stock of all, you can verify with this only the macterial that have shortage on you roll locations.

FYI : in the planning view i want to have the length of a square of a serie starting from the start date to the end date because now i still have some issue with it . it must be coorect even if the conent is big. because if the content is not seen good of the serie he can jsut clicque on it an see the detail of that square.

---

## 16. Consolidated 5-Phase Implementation Plan

> **Approach:** Implement → Test → Feedback → Fix → Next.
> Each phase has numbered steps. Each step produces testable output.

---

### PHASE 1: Foundation — Multi-Algorithm Dispatch + Configurable Weights ✅ COMPLETED

**Goal:** Replace the single hardcoded sorting logic with a pluggable algorithm system. Users choose the strategy, adjust weights, and see scores.

**Status:** All steps implemented and tested.  
**Key deliverables:**
- `DispatchAlgorithms.java` — 7 comparators (SCG, SPT, LPT, EDF, CR, WSPT, ATC) + material grouping
- `SchedulingConfig` entity with presets (Express, Balanced, Efficient, Custom)
- Algorithm selector dropdown + weight sliders in UI
- Composite score display after dispatch
- Serie square width fix in planning view (Gantt alignment corrected)
- **Bonus:** "Matelassage Incomplete" status — series without enough material are flagged and excluded from dispatch
- **Bonus:** Status change dropdown in sequence detail modal — operators can change serie statusMatelassage/statusCoupe directly

| Step | What | Backend / Frontend | Testable Output |
|------|---------|--------------------|-----------------|
| 1.1 | Create `DispatchAlgorithms.java` with 7 comparators (SCG, SPT, LPT, EDF, CR, WSPT, ATC) + material grouping | Backend | Unit tests: each comparator sorts a sample list correctly |
| 1.2 | Create `SchedulingConfig` entity + repository + default presets (Express, Balanced, Efficient, Custom) | Backend | Config saved/loaded via REST, presets visible in DB |
| 1.3 | Update `autoDispatch()` to accept `algorithm` param + `weights` map, delegate sorting to `DispatchAlgorithms` | Backend | Call `/api/ordonnancement/recommendation?algorithm=SPT` and see different orderings |
| 1.4 | Add `SchedulingConfigResource.java` REST endpoints (GET/PUT config, GET presets) | Backend | cURL/Postman: save config, load presets |
| 1.5 | Add algorithm selector dropdown + weight sliders + preset buttons to `AdvancedOrdonnancement.js` | Frontend | UI shows dropdown, selecting algorithm changes dispatch result |
| 1.6 | Add composite score display after dispatch (box completion, utilization, changeovers) | Frontend | Score card appears after dispatch with breakdown |
| 1.7 | Fix serie square width in planning view (start→end date pixel mapping) + click-for-detail | Frontend | Serie blocks render with correct width proportional to duration |

**Test Script for Phase 1:**
```
1. Open AdvancedOrdonnancement page
2. Click algorithm dropdown → select "SPT" → click Auto-Dispatch → verify series ordered by shortest processing time
3. Switch to "EDF" → re-dispatch → verify series ordered by earliest due date
4. Switch to "Material Grouping" → re-dispatch → verify consecutive series share same material
5. Open weight sliders → increase "Box Completion" to max → re-dispatch → verify started sequences come first
6. Click "Balanced" preset → verify weights reset to defaults
7. Verify serie squares have correct width (short series = short block, long series = long block)
8. Click on a serie square → verify detail popup shows all serie info
```

---

### PHASE 2: Material Stock & Demand Forecast 🔄 IN PROGRESS

**Goal:** Show roll stock per zone, forecast material demand for next N hours, detect shortages before they happen.

**Progress:**
- ✅ Step 2.1 — `sequenceStatus` field added to `CuttingRequestData` (ACTIVE, PAUSED, WAITING_MATERIAL, COMPLETED)
- ✅ Step 2.2 — `MaterialDemandForecastService.java` created (forecastDemand, deferMaterialSeries, transfer suggestions)
- ✅ Step 2.3 — `/api/material-demand/forecast`, `/defer`, `/sequence/status` REST endpoints
- ⏳ Step 2.4 — Central stock integration (depends on external `/api/stockStatusReport/currentStock`)
- ✅ Step 2.5 — Material forecast panel in UI (zone selector, horizon buttons 2h/3h/4h/8h, shortage table)
- ✅ Step 2.6 — Defer action with confirmation dialog + transfer suggestions from other zones
- ❌ Step 2.7 — Export PDF/Excel for demand report
- ❌ Step 2.8 — Material-to-machine restriction system

| Step | What | Backend / Frontend | Testable Output |
|------|---------|--------------------|-----------------|
| 2.1 | Add `sequenceStatus` field to `CuttingRequest` (ACTIVE, PAUSED, WAITING_MATERIAL, COMPLETED) | Backend | Field in DB, filter paused sequences from dispatch |
| 2.2 | Create `MaterialDemandForecastService.java` (forecast algorithm from Section 14) | Backend | Unit test: given planned series + stock → returns correct shortage report |
| 2.3 | Create `/api/material-demand/forecast?zone=TFZ&hours=4` REST endpoint | Backend | cURL returns JSON with demand/stock/deficit per material |
| 2.4 | Integrate central stock check via existing `/api/stockStatusReport/currentStock` for shortage materials only | Backend | Deficit materials show "available in warehouse: Xm" |
| 2.5 | Add material demand forecast panel to AdvancedOrdonnancement (horizon buttons, shortage table) | Frontend | UI shows demand panel with 2h/3h/4h/8h toggles |
| 2.6 | Add shortage actions: "Defer series" (pause), "Transfer suggestion" (show source zone) | Frontend | Click "Defer" → series moves to end, status=WAITING_MATERIAL |
| 2.7 | Add export PDF/Excel for demand report (only waiting/scheduled series, configurable hours) | Backend+Frontend | Click export → downloads file with shortage summary |
| 2.8 | Add material-to-machine restriction system (extensible, not just airbag) | Backend | Config table: material X → only machines [A, B, C] |

**Test Script for Phase 2:**
```
1. Navigate to zone TFZ → click "Material Forecast"
2. Select "4h" horizon → verify table shows materials needed vs. available in zone
3. Materials with deficit show ⚠ SHORT or 🔴 NONE status
4. Click "Defer" on a short material's series → verify series status changes to WAITING_MATERIAL
5. Re-dispatch → verify deferred series are excluded
6. Click "Export Excel" → verify downloaded file has shortage summary
7. Verify that sequence with all series paused shows as WAITING_MATERIAL in the queue
```

---

### PHASE 3: Smart Optimization + What-If Simulation

**Goal:** Multi-start Local Search for better schedules, what-if comparison, material changeover optimization.

| Step | What | Backend / Frontend | Testable Output |
|------|---------|--------------------|-----------------|
| 3.1 | Create `ScheduleEvaluator.java` — composite scoring function (box time, utilization, changeovers, due dates) | Backend | Unit test: two different orderings → different scores |
| 3.2 | Create `SmartOptimizer.java` — Multi-start Local Search with swap/insert moves (async, 10–30s) | Backend | Run optimizer → returns schedule with better score than greedy |
| 3.3 | Create `MaterialChangeoverOptimizer.java` — post-processing pass to group same-material series | Backend | Before: 8 changeovers/machine, After: ≤5 |
| 3.4 | Add "Optimize" button (async with progress bar) + score comparison (before vs after) | Frontend | Click Optimize → progress bar → shows "Score: 792 → 847 (+7%)" |
| 3.5 | Add what-if simulation: run N scenarios, show comparison table with KPIs | Frontend | Click "What-If" → shows 3 scenarios side by side |
| 3.6 | Add drag-and-drop manual reordering within machine queue | Frontend | Drag serie S1234 above S1235 → order updated → re-evaluate score |
| 3.7 | Add operator constraint modeling (spreading operators per zone, configurable) | Backend | If 4 operators for 5 spreaders → only 2 concurrent spreading operations |

**Test Script for Phase 3:**
```
1. Dispatch with "Quick" mode → note score
2. Click "Optimize" (Smart mode) → wait for progress bar → verify score improved
3. Check changeover count decreased after optimization
4. Click "What-If" → run 3 scenarios → compare box completion times
5. Drag a serie within a machine queue → verify new order persists and score updates
6. Set operator count to 3 for zone → verify max 1 concurrent spreading operation
```

---

### PHASE 4: Vue Réel Redesign + Zone Physical Layout

**Goal:** Physical zone view with roll locations, machine layout, configurable drag-and-drop editor.

| Step | What | Backend / Frontend | Testable Output |
|------|---------|--------------------|-----------------|
| 4.1 | Create zone layout data model: `ZoneLayoutConfig` entity with machine positions, roll location positions | Backend | Layout JSON saved per zone |
| 4.2 | Create zone layout editor in Process section (drag-and-drop machines, roll locations, rotate, resize) | Frontend | Admin user drags machines to position, saves layout |
| 4.3 | Redesign Vue Réel to render physical zone layout from saved config | Frontend | View shows machines positioned as configured, not as a flat list |
| 4.4 | Add roll location squares (left panel) with count badge, click-to-open ScanRouleau modal | Frontend | Click "reTFZ" → modal shows 12 rolls with reftissu, metrage, lot |
| 4.5 | Render dual-station machines: spreading head (left) + 14m table + cutting head (right) | Frontend | Active spread on left, active cut on right, idle areas shown |
| 4.6 | Render LASER-DXF as single block with layer progress (e.g., "layer 3/8") | Frontend | LASER machine shows unified block, layer counter |
| 4.7 | Add zone selector (tabs or stacked view) with collapse/expand | Frontend | Switch between zones via tabs, or see all zones stacked |
| 4.8 | Connect live data: show real-time serie progress on each machine, roll consumption | Frontend+Backend | As series progress, the view updates (5-min refresh or WebSocket) |

**Test Script for Phase 4:**
```
1. Go to Process section → Zone Layout Editor → drag machine AA1 to top-left, AA2 below it → save
2. Switch to Vue Réel → verify AA1 is at top-left, AA2 below
3. Click roll location "reTFZ" → modal shows rolls with correct data
4. Verify Lectra machine shows spreading head (left) and cutting head (right)
5. Verify LASER-DXF shows single block with layer progress
6. Switch zone tab → verify machines and roll locations change
7. Wait 5 minutes → verify positions of series updated
```

---

### PHASE 5: Persistence, Comparison & Advanced Features

**Goal:** Save/load/compare plans, real-time adaptation, multi-shift planning, constraint solver (optional).

| Step | What | Backend / Frontend | Testable Output |
|------|---------|--------------------|-----------------|
| 5.1 | Persist dispatch results: "Save Plan" with timestamp, "Load Plan", plan versioning | Backend+Frontend | Save a plan → reload page → Load previous plan → timeline restored |
| 5.2 | Plan comparison view: side-by-side KPIs (box time, utilization, changeovers) | Frontend | Compare Plan A vs Plan B → delta shown: "+5% utilization, -1 changeover" |
| 5.3 | Estimation feedback loop: track actual vs predicted times, auto-adjust coefficients | Backend | After 100 series: coefficient drift report, suggested adjustments |
| 5.4 | Multi-shift planning: extend horizon to 1 full day (3 shifts), shift transition modeling | Backend | Dispatch shows series across all 3 shifts with handover markers |
| 5.5 | Auto-approval toggle: if enabled, dispatch auto-applies without user confirmation | Backend+Frontend | Toggle ON → dispatch automatically assigns series every 5 min |
| 5.6 | Schedule export PDF/Excel with configurable hour range | Backend+Frontend | Export next 8 hours as PDF → file shows timeline + assignments |
| 5.7 | (Optional) OptaPlanner constraint solver integration for near-optimal results | Backend | Run OptaPlanner mode → better score than Smart mode in ≤5 min |

**Test Script for Phase 5:**
```
1. Dispatch → click "Save Plan" → name it "Plan A"
2. Change algorithm to SPT → re-dispatch → save as "Plan B"
3. Click "Compare" → select Plan A vs Plan B → verify KPI delta shown
4. Check estimation feedback: after running for 1 shift, verify drift report available
5. Toggle "Auto-Dispatch" ON → verify series auto-assigned after 5 min
6. Export next 8 hours as PDF → verify file contains timeline
```

---

### Implementation Timeline Summary

| Phase | Core Focus | Depends On | Status |
|-------|-----------|------------|--------|
| **Phase 1** | Algorithm flexibility + UI controls + serie width fix | Nothing (standalone) | ✅ **COMPLETED** |
| **Phase 2** | Material stock + demand forecast + shortages | Phase 1 (needs dispatch to forecast) | 🔄 **IN PROGRESS** (6/8 steps done) |
| **Phase 3** | Smart optimization + what-if + drag-and-drop | Phase 1 (needs algorithm framework) | ⏳ Not started |
| **Phase 4** | Vue Réel redesign + zone layout editor | Phase 2 (needs roll stock data) | ⏳ Not started |
| **Phase 5** | Persistence + comparison + multi-shift + feedback | Phase 3 (needs optimizer output) | ⏳ Not started |

---

### Change Log

| Date | Phase | Changes |
|------|-------|---------|
| Phase 1 | Phase 1 | All 7 steps completed: DispatchAlgorithms.java, SchedulingConfig entity, algorithm selector UI, weight sliders, score display, Gantt alignment fix |
| Phase 1+ | Bonus | Added "Matelassage Incomplete" status (backend filter + frontend display + SCSS), status change dropdown in sequence detail modal |
| Phase 2 | Phase 2 | Steps 2.1–2.6: sequenceStatus field, MaterialDemandForecastService, MaterialDemandController, forecast panel UI with zone selector/horizon buttons/shortage table/defer actions |

---

*Currently implementing Phase 2, Steps 2.7–2.8: Export + Material-to-machine restrictions*
