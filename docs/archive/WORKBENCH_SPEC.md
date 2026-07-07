# WORKBENCH_SPEC.md — MG-CMS Process Workbench Specification

> Produced by Agent A (Architect). All other agents MUST read this file before writing code.

---

## 1. TypeScript-style Interface for `WorkbenchData`

```typescript
// Shared context shape — loaded once per mount / header change, passed to sections.
interface WorkbenchData {
  asOf: string; // ISO-8601 LocalDateTime
  date: string; // YYYY-MM-DD
  shift: number; // 1=night, 2=morning, 3=afternoon
  shiftMinutes: number;

  liveCharge: LiveChargeDto;        // GET /api/dispatcher/liveCharge
  zoneLoad: ZoneLoadMatrix;         // GET /api/zoneLoad/matrix?date=&shift=
  gantt: GanttTimeline;             // GET /api/ordonnancement/timeline?hoursBack=&hoursForward=
  engineState: EngineStateSnapshot; // GET /api/dispatcher/engine/state

  // Phase 8 stubs (filled by Agent C, consumed by Agent D)
  materialForecast?: MaterialForecastDto;
}

interface EngineStateSnapshot {
  state: 'IDLE' | 'WARMING' | 'IMPROVING' | 'PAUSED' | 'STOPPED';
  mode?: 'CONTINUOUS' | 'FIXED_DURATION';
  runId?: number;
  iteration?: number;
  spread?: number;
  rawSpread?: number;
  stdDev?: number;
  median?: number;
  initialSpread?: number;
  lastImprovement?: number;
}

interface LiveChargeDto {
  asOf: string;
  date: string;
  shift: number;
  shiftMinutes: number;
  totals: {
    totalSequences: number;
    lockedSequences: number;
    pendingSequences: number;
    unassignedSequences: number;
    totalRemainingMinutes: number;
    totalCapacityMinutes: number;
  };
  zones: ZoneChargeDto[];
  unassigned: SequenceDto[];
}

interface ZoneChargeDto {
  zoneNom: string;
  category: 'STRICT' | 'SHARED';
  byMachineType: MachineTypeChargeDto[];
  lockedSequences: SequenceDto[];
  pendingSequences: SequenceDto[];
  totalRemainingMinutes: number;
  totalCapacityMinutes: number;
  overallLoadPct: number;
}

interface MachineTypeChargeDto {
  machineType: string;
  groupe: string;
  activeMachines: number;
  shiftMinutes: number;
  efficiencePct: number;
  capacityMinutes: number;
  lockedRemainingMinutes: number;
  pendingRemainingMinutes: number;
  totalRemainingMinutes: number;
  loadPct: number;
}

// Phase 8 additions (stubs in Phase 1–4, filled by Agent C):
// SequenceDto additions: dueDate, boxCycleTimeMinutes, materialStatus
// SerieDto additions: refTissus, materialStatus, tableLengthRequired
interface SequenceDto {
  sequence: string;
  zoneFix: string | null;
  dispatchedZone: string | null;
  effectiveZone: string | null;
  zoneSource: 'LOCKED' | 'ENGINE_PROPOSED' | 'DISPATCHED' | 'PREFERRED' | 'NONE';
  zoneMismatch: boolean;
  locked: boolean;
  lockReason: 'EXPLICIT_ACCEPTED' | 'IMPLICIT_TABLE_STRICT' | null;
  lockingSerieId: string | null;
  lockingTableNom: string | null;
  lockingStatusCoupe: string | null;
  pinnedByChef: boolean;
  zoneAcceptanceStatus: string | null;
  totalRemainingMinutes: number;
  series: SerieDto[];
  // Phase 8 fields (nullable/optional until backend ships):
  dueDate?: string | null;          // YYYY-MM-DD from CuttingRequest.dueDate
  boxCycleTimeMinutes?: number;     // computed by engine
  materialStatus?: 'AVAILABLE_IN_ZONE' | 'NEEDS_TRANSFER' | 'MISSING' | null;
}

interface SerieDto {
  serie: string;
  machine: string;
  tempsDeCoupe: number | null;
  validatedMinutes: number;
  timeSource: 'VALIDATED' | 'REAL' | 'TEMPS_DE_COUPE' | 'NONE';
  statusCoupe: string | null;
  statusMatelassage: string | null;
  tableCoupe: string | null;
  tableMatelassage: string | null;
  dateDebutCoupe: string | null;
  dateFinCoupe: string | null;
  dateDebutMatelassage: string | null;
  dateFinMatelassage: string | null;
  elapsedMinutes: number;
  remainingMinutes: number;
  targetZoneNom: string | null;
  targetZoneCategory: 'STRICT' | 'SHARED' | null;
  // Phase 8 fields:
  refTissus?: string | null;
  materialStatus?: 'AVAILABLE_IN_ZONE' | 'NEEDS_TRANSFER' | 'MISSING' | null;
  tableLengthRequired?: number; // longueur * nbrCouche
}

// Placeholder — actual shape defined by existing PlanDeCharge backend
interface ZoneLoadMatrix {
  // opaque passthrough from /api/zoneLoad/matrix
  [key: string]: any;
}

// Placeholder — actual shape defined by existing OrdonnancementService timeline
interface GanttTimeline {
  // opaque passthrough from /api/ordonnancement/timeline
  [key: string]: any;
}

// Phase 8 placeholder
interface MaterialForecastDto {
  // to be defined by Agent C
  [key: string]: any;
}
```

---

## 2. Component Tree for `Workbench.js`

```
App.js
└─ SecuredRoute /processWorkbench
   └─ Workbench (components/Layout/Workbench.js)
      ├─ WorkbenchHeader (components/Layout/WorkbenchHeader.js)
      │  ├─ date picker
      │  ├─ shift selector (1|2|3)
      │  ├─ zone filter dropdown (optional — "All zones" default)
      │  ├─ engine state badge (color-coded)
      │  └─ global refresh button
      │
      ├─ WorkbenchContext.Provider (React Context — src/main/js/components/Layout/WorkbenchContext.js)
      │  └─ WorkbenchSection[] (components/Layout/WorkbenchSection.js)
      │     ├─ Section 1: Plan de Charge
      │     │  └─ PlanDeChargeView (data prop, no self-fetch)
      │     ├─ Section 2: Dispatching de Séquence
      │     │  ├─ LiveChargeView (data prop, read-only)
      │     │  ├─ DispatchHeatmap (data prop)
      │     │  └─ DispatchEngineControl (role-gated)
      │     ├─ Section 3: Ordonnancement (Gantt)
      │     │  └─ GanttView (readOnly={true}, data prop)
      │     ├─ Section 4: Audit / Historique
      │     │  └─ AuditTable (data from /api/dispatcher/audit)
      │     └─ Section 5: Prévision Matière
      │        └─ MaterialForecastPanel (Phase 8 — stub for now)
      │
      └─ (old route redirects handled in App.js)
```

---

## 3. Section-by-Section Data Flow

| Section | Data Source | Endpoint (aggregator) | Who Renders | Notes |
|---------|-------------|----------------------|-------------|-------|
| Header | Context | `GET /api/workbench/data?date=&shift=` | WorkbenchHeader | Date/shift change triggers full reload |
| Plan de Charge | Context.zoneLoad | same | PlanDeChargeView | Refactored to accept `data` prop |
| Dispatching | Context.liveCharge | same | LiveChargeView + DispatchHeatmap + DispatchEngineControl | EngineControl only for PROCESS/ADMIN |
| Gantt | Context.gantt | same | GanttView | Read-only in Workbench; no algo dropdown |
| Audit | Own fetch (lightweight) | `GET /api/dispatcher/audit?limit=50` | AuditTable | Only fetched when section expanded |
| Prévision Matière | Phase 8 | TBD | MaterialForecastPanel | Stub in Phase 1–4 |

**Aggregator contract** (`/api/workbench/data`):
- Input: `?date=YYYY-MM-DD&shift=1|2|3`
- Output: `{ liveCharge, zoneLoad, gantt, engineState }`
- Role: `PROCESS | CHEF_DE_ZONE | CHEF_EQUIPE | ADMIN`
- Internally delegates to existing services; zero duplication of business logic.

---

## 4. Role × Section Visibility Matrix

| Section | PROCESS | ADMIN | CHEF_DE_ZONE | CHEF_EQUIPE | CAD | Other |
|---------|:-------:|:-----:|:------------:|:-----------:|:---:|:-----:|
| Plan de Charge | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Dispatching | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Engine Controls | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Gantt | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Audit | ✅ | ✅ | ✅ (own zones) | ✅ | ❌ | ❌ |
| Prévision Matière | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |

**Front route guard** (`App.js`):
```
/processWorkbench → allowedRoles: ["ROLE_PROCESS","ROLE_CHEF_DE_ZONE","ROLE_CHEF_EQUIPE","ROLE_ADMIN"]
```

**Dashboard menu**: Add "Process Workbench" link under the existing Process/Scheduling area, visible to `PROCESS | CHEF_DE_ZONE | CHEF_EQUIPE | ADMIN`.

---

## 5. Polling Contract

| Endpoint | Cadence | Trigger Refresh |
|----------|---------|-----------------|
| `GET /api/workbench/data` | 10 s default; 2 s when `engineState.state ∈ {WARMING, IMPROVING}` | Mount, date/shift change, manual refresh button |
| `GET /api/dispatcher/engine/state` | Same cycle as above (bundled in aggregator response, but header badge may want direct poll) | Bundled in aggregator; no separate poll needed if aggregator is fast (< 500 ms) |
| `GET /api/dispatcher/audit` | 10 s when Audit section is expanded | Section expand, manual refresh |

**Implementation note**: The Workbench context manages ONE timer. When engine is running, timer drops to 2 s. When stopped, returns to 10 s. Sections do NOT run their own polling loops.

---

## 6. URL Parameter Contract

| Route | Query Param | Deep-link Behavior |
|-------|-------------|-------------------|
| `/processWorkbench` | none | Opens with all sections collapsed (or last localStorage state) |
| `/processWorkbench?section=planDeCharge` | `section=planDeCharge` | Expands Plan de Charge section |
| `/processWorkbench?section=dispatching` | `section=dispatching` | Expands Dispatching section |
| `/processWorkbench?section=gantt` | `section=gantt` | Expands Gantt section |
| `/processWorkbench?section=audit` | `section=audit` | Expands Audit section |
| `/processWorkbench?section=material` | `section=material` | Expands Prévision Matière (Phase 8) |

**Redirects** (in `App.js` using `<Redirect>`):
```
/planDeCharge         → /processWorkbench?section=planDeCharge
/processDispatcher    → /processWorkbench?section=dispatching
/advancedOrdonnancement → /processWorkbench?section=gantt
```

**localStorage key**: `mgcms.workbench.sections.{matricule}` → JSON array of expanded section IDs.

---

## 7. Dependencies on Phase 8 Fields

Phase 1–4 agents MUST stub these fields so the UI compiles and renders without NPEs. Agent C will fill the backend implementation.

### Backend stubs (Agent C)

In `LiveChargeDto.java`:
- Add `dueDate` (LocalDate), `boxCycleTimeMinutes` (double), `materialStatus` (String) to `SequenceDto`
- Add `refTissus` (String), `materialStatus` (String), `tableLengthRequired` (double) to `SerieDto`

In `LiveChargeService.java`:
- Surface `dueDate` from `CuttingRequest.dueDate` in `compute()`
- Set `boxCycleTimeMinutes = 0.0` and `materialStatus = null` as temporary defaults until engine logic ships

### Frontend stubs (Agent D)

In `LiveChargeView.js`:
- Render `dueDate` and `boxCycleTimeMinutes` only when present (conditional rendering)
- Render `refTissus` and `materialStatus` per-serie only when present
- Use a neutral "—" or hidden when Phase 8 fields are absent

In `Workbench.js`:
- Section 5 (Prévision Matière) renders a placeholder card: "Prévision Matière — Phase 8"

---

## 8. File Ownership by Agent

| File(s) | Owner Agent |
|---------|-------------|
| `WorkbenchController.java` | B |
| `UserZoneController.java` (additions) | B |
| `UserZoneRepository.java` (additions) | B |
| `MaterialAvailabilityChecker.java` | C |
| `TableLengthConstraint.java` | C |
| `ContinuousDispatchOptimizerService.java` (modifications) | C |
| `LiveChargeDto.java` (modifications) | C |
| `LiveChargeService.java` (modifications) | C |
| `Workbench.js`, `WorkbenchContext.js`, `WorkbenchHeader.js`, `WorkbenchSection.js` | D |
| `Workbench.scss` | D |
| `PlanDeCharge.js` (refactor) | D |
| `ProcessDispatcher.js` (refactor) | D |
| `AdvancedOrdonnancement.js` (refactor) | D |
| `App.js` (additions) | D |
| `Dashboard.js` (additions) | D |
| `UserZoneAdmin.js` (rewrite) | E |
| `UserZoneAdmin.scss` | E |
| `VERIFICATION.md` | F |

**Coordination rule**: If two agents need to touch the same file, the earlier phase agent owns it; the later agent reads the file after the earlier agent claims completion and appends their changes.

---

## 9. Critical Conventions to Preserve

1. **Hibernate naming**: Columns are camelCase in DB (project bypasses SpringPhysicalNamingStrategy). Prefer JPQL; if native query required, document exact column names.
2. **SQL Server IN-clause limit**: Batch at `SQL_BATCH_SIZE = 2000`. Pattern exists in `CuttingRequestRepository` and `ContinuousDispatchOptimizerService`.
3. **Shift mapping**: Plant shift = 1 night, 2 morning, 3 afternoon. Legacy `OrdonnancementService` uses inverse mapping — translate explicitly when crossing.
4. **Active machine resolution**: Use `ActiveMachineResolver.activeMachines(date, shift, zoneNom)` — do NOT reimplement.
5. **Lock resolution**: Use `LockResolver.resolve()` — do NOT reimplement.
6. **Per-serie zone routing**: Use `LiveChargeService.resolveTargetZone()` — engine and live view must agree.
7. **Auth role strings**: Exact match required — `ROLE_PROCESS`, `ROLE_ADMIN`, `ROLE_CHEF_DE_ZONE`, `ROLE_CHEF_EQUIPE`, etc.

---

## 10. Test Expectations

| Agent | Test Deliverables |
|-------|-------------------|
| B | `WorkbenchControllerTest` (MockMvc), `UserZoneControllerTest` (MockMvc for new endpoints) |
| C | `MaterialAvailabilityCheckerTest`, `TableLengthConstraintTest`, extend `ContinuousDispatchOptimizerServiceTest` |
| D | Frontend build: `npx webpack --mode development` must succeed |
| E | Frontend build: `npx webpack --mode development` must succeed |
| F | `VERIFICATION.md` log + backend compile + backend test + webpack build |

---

## 11. Open Questions / Decisions

1. **PlanDeCharge self-fetch preservation**: The refactor extracts `<PlanDeChargeView data={...}/>` but keeps a top-level wrapper at `/planDeCharge` that fetches and forwards. The wrapper is a thin component in the same file or a separate `PlanDeChargePage.js` — Agent D decides based on file size (3619 lines; recommend separate wrapper file).

2. **AdvancedOrdonnancement read-only mode**: The existing component has heavy Redux coupling. Agent D should extract a pure `<GanttView readOnly={true} data={...}/>` subcomponent and keep the existing page as a wrapper that passes `readOnly={false}` when accessed directly. If Redux coupling makes extraction hard, Agent D may create a parallel read-only render path.

3. **UserZoneAdmin `/assign` role gate**: Currently `CHEF_EQUIPE | PROCESS`. The brief says "check if ADMIN should be added". Decision: keep existing gate unless security team clarifies. The new `/all` and `/setDefault` endpoints use `ADMIN | PROCESS | CHEF_EQUIPE` per the brief.

4. **Material stock API client**: `/api/stockStatusReport/currentStock?refTissus=...` is an existing endpoint. Agent C should inject `RestTemplate` or use an existing HTTP client bean. If none exists, create a lightweight `@Service` wrapper `StockStatusClient` in `services/dispatcher/`.

5. **TableLengthConstraint pure function**: Input is `List<TableSlot>` where `TableSlot = {serieId, tableLengthRequired, startTime, endTime}`. Output: `boolean fits`. The utility has no Spring dependencies.

---

*Spec version: 1.0 — ready for implementation.*
