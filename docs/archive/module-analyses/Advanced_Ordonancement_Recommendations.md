# Advanced Ordonnancement — Recommendations & Future Improvements

**Author:** Mouad El Ghazi  
**Created:** June 2026

---

## Table of Contents
1. [Current Architecture Assessment](#current-architecture-assessment)
2. [Short-Term Recommendations](#short-term-recommendations)
3. [Medium-Term Recommendations](#medium-term-recommendations)
4. [Long-Term Recommendations](#long-term-recommendations)
5. [Performance Recommendations](#performance-recommendations)
6. [UX/UI Recommendations](#uxui-recommendations)
7. [Auto-Dispatch Algorithm Improvements](#auto-dispatch-algorithm-improvements)

---

## Current Architecture Assessment

### Strengths
- **Smart data loading**: Only relevant sequences loaded, avoiding stale data
- **Collision-free scheduling**: Auto-dispatch guarantees no overlapping intervals on same machine
- **Lightweight projections**: 19-column JPQL instead of 50+ column entities with eager joins
- **Incremental refresh**: 5-minute cycle with fast change-detection query
- **Three view modes**: Physical table view (Réel) + two Gantt views (Coupe/Matelassage)
- **Sequence color coding**: Visual correlation across all views

### Areas for Improvement
- Auto-dispatch does not persist estimated dates (in-memory only)
- No drag-and-drop for manual prioritization
- No WebSocket for real-time push updates
- No historical analytics or trend tracking
- Single-user scheduling (no concurrent edit locking)

---

## Short-Term Recommendations

### 1. Persist Auto-Dispatch Results
**Priority:** HIGH  
Currently, auto-dispatch results exist only in memory and are lost on page refresh.

**Recommendation:**
- Create an `OrdonnancementPlan` entity to store dispatch results
- Add a "Sauvegarder Plan" button that persists all estimated dates
- Show last saved plan timestamp
- Allow operators to see the plan even without re-running dispatch

### 2. Drag-and-Drop Prioritization
**Priority:** MEDIUM  
Allow users to drag series in the unassigned panel to reorder priority.

**Recommendation:**
- Add `react-beautiful-dnd` or `@dnd-kit/core` for drag support
- Persist manual priority order in a `seriePriority` field
- Auto-dispatch should respect manual priority when set

### 3. Notification on Shift Overflow
**Priority:** MEDIUM  
When shift overflow is detected, notify relevant operators.

**Recommendation:**
- Send email/SMS alert when `overflowPercentageNextShift > 20%`
- Add a notification bell icon in the header with overflow alerts
- Log overflow events for trend analysis

### 4. Machine Downtime Awareness
**Priority:** HIGH  
Auto-dispatch should account for planned machine downtime (PM status).

**Recommendation:**
- Query `EtatMachineHistorique` for upcoming PM/OFF status changes
- Register PM intervals as occupied (non-schedulable) in the dispatch algorithm
- Show PM periods as gray blocks in the Planifié timeline

---

## Medium-Term Recommendations

### 5. WebSocket Real-Time Updates
**Priority:** MEDIUM  
Replace 5-minute polling with WebSocket push notifications.

**Recommendation:**
- Use Spring WebSocket (STOMP over SockJS)
- Backend publishes events on: status change, date update, assignment change
- Frontend subscribes and patches state incrementally
- Reduces server load from polling and improves responsiveness

### 6. Multi-Table Spreading Visualization
**Priority:** LOW  
Some series spread on one table and cut on another. The Réel view should show both.

**Recommendation:**
- Show a "spreading ghost" block on the matelassage table
- Use a different hatching pattern to distinguish from cutting occupancy
- Link the two blocks visually (same color, connected line)

### 7. Historical Analytics Dashboard
**Priority:** MEDIUM  
Track scheduling efficiency over time.

**Recommendation:**
- Log dispatch accuracy: how close were estimated dates to actual dates?
- Track average sequence completion time
- Show trends: overtime frequency, machine utilization percentages
- Create a `/analytics` sub-page with charts (Chart.js or Recharts)

### 8. Operator Mobile View
**Priority:** LOW  
Operators at machines need a simplified view on tablets/phones.

**Recommendation:**
- Create a responsive `/ordonnancement/operator` view
- Show only: current machine queue, next 3 series, estimated times
- Large touch-friendly buttons for status updates
- Auto-refresh every 30 seconds

---

## Long-Term Recommendations

### 9. Machine Learning for Time Estimation
**Priority:** LOW  
Replace static coefficient-based estimation with ML predictions.

**Recommendation:**
- Collect historical data: actual cutting/spreading times vs estimates
- Train regression model on: material type, longueur, nbrCouche, machine type
- Deploy as a microservice or embedded model (ONNX Runtime in Java)
- Gradually improve estimation accuracy over time

### 10. Multi-Shift Planning
**Priority:** MEDIUM  
Currently plans one shift at a time. Extend to plan across shifts.

**Recommendation:**
- Allow selecting a planning horizon (1 shift / 1 day / 1 week)
- Auto-dispatch across shift boundaries with break periods
- Show shift transitions in the Gantt timeline
- Consider operator availability per shift

### 11. Constraint Satisfaction Solver
**Priority:** LOW  
Replace greedy auto-dispatch with optimal solver.

**Recommendation:**
- Use OptaPlanner (Java-based constraint solver) or Google OR-Tools
- Define hard constraints: no overlaps, table length, zone limits
- Define soft constraints: minimize sequence completion time, maximize affinity
- Provides provably optimal or near-optimal schedules
- Trade-off: slower computation but better results

---

## Performance Recommendations

### 12. Redis Caching for Timeline
**Priority:** MEDIUM  
Cache timeline data in Redis with 30-second TTL.

**Recommendation:**
- Cache key: `ord:timeline:{hoursBack}:{hoursForward}:{additionalSequences}`
- Invalidate on serie status change (via ApplicationEvent)
- Reduces database load from concurrent users viewing the same page

### 13. Database Index Optimization
**Priority:** HIGH  
Ensure proper indexes for ordonnancement queries.

**Recommendation:**
```sql
-- For smart loading queries
CREATE INDEX IX_CRS_Sequence_Status ON CuttingRequestSerieData (sequence, statusCoupe, statusMatelassage);
CREATE INDEX IX_CRS_Dates ON CuttingRequestSerieData (dateDebutCoupe, dateFinCoupe, dateDebutMatelassage, dateFinMatelassage);
CREATE INDEX IX_CRS_Planning ON CuttingRequestSerieData (planningDate, sequence);

-- For Auto-Dispatch
CREATE INDEX IX_CRS_TableCoupe ON CuttingRequestSerieData (tableCoupe, statusCoupe);
CREATE INDEX IX_CRS_TableMatel ON CuttingRequestSerieData (tableMatelassage, statusMatelassage);

-- For Sequence Detail
CREATE INDEX IX_CRD_Sequence ON CuttingRequestData (sequence);
```

### 14. Async Auto-Dispatch
**Priority:** MEDIUM  
For large datasets, auto-dispatch may take >2 seconds.

**Recommendation:**
- Run auto-dispatch asynchronously with `@Async` + `CompletableFuture`
- Frontend shows progress indicator
- Return result via polling or WebSocket callback

---

## UX/UI Recommendations

### 15. Keyboard Shortcuts
**Priority:** LOW  
Add keyboard navigation for power users.

| Shortcut | Action |
|----------|--------|
| `R` | Switch to Réel view |
| `C` | Switch to Planifié Coupe view |
| `M` | Switch to Planifié Matelassage view |
| `N` | Scroll to Now |
| `D` | Run Auto-Dispatch |
| `F5` | Refresh timeline |
| `Esc` | Close modal |

### 16. Print-Friendly Layout
**Priority:** LOW  
Allow printing the Réel view for posting on the production floor.

**Recommendation:**
- Add `@media print` styles
- Hide controls, show only machines and series
- Include timestamp and shift info in header
- A4 landscape format

### 17. Dark Mode Support
**Priority:** LOW  
For operators working night shifts (Shift 3: 22h-06h).

---

## Auto-Dispatch Algorithm Improvements

### 18. Batch Optimization (Current: Sequential)
**Current**: Series are dispatched one-by-one. The first serie gets the best slot, potentially blocking better global assignments.

**Improvement**: Use look-ahead optimization:
- Phase 1: Greedy initial assignment (current algorithm)
- Phase 2: Local search improvement
  - Swap assignments between machines
  - Accept swaps that reduce total completion time
  - Iterate until no improvement found

### 19. Priority-Weighted Scoring
**Current**: All series treated equally. Urgent sequences should be prioritized.

**Improvement**: Add priority multiplier:
```
score = baseMachineScore × priorityWeight
Where:
  priorityWeight = 2.0 if dueShift is current shift (urgent)
  priorityWeight = 1.5 if dueShift is next shift
  priorityWeight = 1.0 otherwise
```

### 20. Setup Time Between Different Materials
**Current**: No penalty for switching materials on a machine.

**Improvement**: Add material changeover time:
```
If previousSerie.partNumberMaterial != currentSerie.partNumberMaterial:
  addSetupTime(MATERIAL_CHANGEOVER_MINUTES)  // e.g., 5 minutes
```
This would make the algorithm prefer keeping same-material series on the same machine.
