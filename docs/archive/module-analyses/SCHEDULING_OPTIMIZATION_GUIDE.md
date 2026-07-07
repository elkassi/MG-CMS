# MG-CMS Scheduling Optimization Module

## Overview

The Scheduling Optimization module allows users to optimize cutting sequences across multiple machines using a greedy algorithm with async processing.

---

## Quick Start

1. **Open Scheduling Dashboard** from the main menu
2. **Select Zone** and **select machines** (e.g., AA1, AA2, AA3)
3. Click **"Charger"** to load sequences
4. **Select sequences** to optimize
5. Click **"Optimiser"** to run optimization
6. View results in **Gantt chart** and **Sequence Summary table**

---

## Features

### Sequence Loading
- Load sequences for selected zone and machines
- Display sequence details (model, due date, status, estimated duration)
- Select specific sequences for optimization

### Optimization
- Greedy algorithm assigns series to machines
- Respects machine capacity (max boxes constraint)
- Groups by material for efficiency
- Async processing with real-time status updates

### Status Monitoring
- Progress bar shows optimization progress (0-100%)
- Status indicator (Running/Completed/Failed)
- Auto-polling during optimization (every 2 seconds)
- Change detection alerts when data is modified

### Gantt Visualization
- Interactive Gantt chart with time on vertical axis
- Color-coded series by sequence
- Machine columns show assigned work
- Side table with sequence summaries

### Plan Management
- Save and retrieve optimization plans
- Delete outdated plans
- Re-optimize with updated parameters
- Global view of all active plans

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/scheduling/loadSequences` | Load sequences for zone/machines |
| POST | `/api/scheduling/optimize` | Run optimization |
| GET | `/api/scheduling/status/{planId}` | Get optimization status |
| POST | `/api/scheduling/stop/{planId}` | Stop running optimization |
| GET | `/api/scheduling/checkChanges/{planId}` | Check for data changes |
| GET | `/api/scheduling/globalPlans` | Get all plans |
| GET | `/api/scheduling/plan/{planId}` | Get specific plan |
| DELETE | `/api/scheduling/plan/{planId}` | Delete a plan |
| POST | `/api/scheduling/reoptimize/{planId}` | Re-optimize with new params |

---

## Optimization Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| Max Boxes | 16 × machines | Maximum boxes per run |
| Timeout | 30 seconds | Maximum optimization time |
| Group by Material | true | Group series by material |
| Priority | balanced | balanced / speed / efficiency |

---

## Database Entities

### OptimizedPlan
- `planId` - Unique identifier (UUID)
- `zoneName` - Target zone
- `machineNames` - Assigned machines
- `status` - RUNNING / COMPLETED / FAILED
- `progress` - 0-100 percentage
- `maxDurationMinutes` - Total duration
- `minStartDate`, `maxEndDate` - Time bounds
- `isActive` - Currently active plan

### OptimizedSeriesAssignment
- `serieId`, `sequenceId` - Series identifiers
- `machineName` - Assigned machine
- `scheduledStart`, `scheduledEnd` - Scheduled times
- `cuttingDurationMinutes` - Cutting duration
- `orderOnMachine` - Order in machine queue
- `isLocked` - Lock status

---

## File Structure

```
Backend:
├── controller/scheduling/
│   └── SchedulingController.java      # REST endpoints
├── services/scheduling/
│   └── SchedulingOptimizationService.java  # Business logic
├── domain/scheduling/
│   ├── OptimizedPlan.java             # Plan entity
│   └── OptimizedSeriesAssignment.java # Assignment entity
├── repositories/scheduling/
│   └── OptimizedPlanRepository.java   # Data access
└── payload/scheduling/
    ├── OptimizationRequest.java       # Request DTO
    ├── OptimizationResponse.java      # Response DTO
    └── SequenceLoadResponse.java      # Sequence DTO

Frontend:
└── components/Layout/
    ├── SchedulingDashboard.js         # Main dashboard
    ├── SchedulingGanttChart.js        # Gantt visualization
    └── SequenceSummaryTable.js        # Summary table
```

---

## Usage Tips

1. **Start small** - Test with 1-2 machines first
2. **Monitor progress** - Watch the status panel during optimization
3. **Check changes** - Use "Vérifier Changements" if data might have changed
4. **Re-optimize** - Click "Ré-optimiser" when changes are detected
5. **Global view** - Use "Vue Globale" to see all active plans

---

## Technical Notes

- Optimization runs asynchronously using Spring `@Async`
- Status polling occurs every 2 seconds during optimization
- Auto-refresh (when enabled) polls every 30 seconds for plan updates
- All plans are persisted to the database with full audit trail
