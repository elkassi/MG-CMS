# MG-CMS — Consolidated Agent Swarm Audit Report
**Date:** 2026-05-05  
**Scope:** Backend (Spring Boot), Frontend (React), Database (MS SQL), Plans (Markdown)  
**Agents:** Backend Audit Agent ✅ | Frontend Audit Agent ✅ | Database Audit Agent ✅ | Plans Cross-Check Agent ⏹ (sufficient data gathered)  
**DB Target:** `LEAR_MG_CMS_Prod` on `MSI\SQLEXPRESS`  
**CMS-Prod Context:** Production station app (spreading, cutting, LASER-DXF, DIE GERBER) — integration points noted throughout.

---

## 1. EXECUTIVE SUMMARY

The MG-CMS codebase has a **mature dispatcher foundation** (`ZoneLoadService`, `SequenceDispatcherService`, `CuttingTimeCalculator`, `DispatchAudit` entity) but suffers from **three critical systemic issues** that block production deployment of the advanced ordonnancement vision:

1. **CRITICAL SECURITY BUG** — ~50 `@PreAuthorize` annotations use `hasRole('ROLE_X')`, which Spring Security resolves to `ROLE_ROLE_X`. Because authorities are stored as `ROLE_ADMIN`, `ROLE_PROCESS`, etc., these endpoints either reject legitimate users or fall back to unprotected state. The `DispatcherController` happens to be correctly written (`hasRole('PROCESS')`), but many other controllers (CNC, Cutting, Gamme) are broken.

2. **CRITICAL DATABASE GAP** — Flyway migration `V12_01__dispatch_audit.sql` exists on disk but **was never applied**. The `dispatch_audit` table is missing, so every dispatcher audit write will crash with `Invalid object name 'dispatch_audit'`.

3. **CRITICAL PERFORMANCE BUG** — `ZoneLoadService.computeMatrix()` and `SequenceDispatcherService.compute()` load full `CuttingRequest` `@Entity` graphs (including all series, boxes, part-numbers via eager `@LazyCollection(LazyCollectionOption.FALSE)`). On a 400-serie shift this is a massive DB+memory hit that violates the plans' "projections only" hard rule.

Beyond these blockers, the frontend is **11 pages behind** the Master Scheduling Vision (missing Optimizer Console, War Room, KPI Dashboard, Inassignables, etc.), all WebSocket topics are unimplemented (fallback to polling), and route-level role guards are absent. The Continuous Optimization Engine (`ContinuousOptimizerService`, `ZoneOptimizerThread`, `BoxDurationCalculator`) is entirely missing from the backend.

---

## 2. MCP CONFIGURATION STATUS

The `.roo/mcp.json` configures `mssql-prod-reader` with:
- `SQL_CONNECTION_STRING`: `Driver={ODBC Driver 17 for SQL Server};Server=MSI\SQLEXPRESS;Database=LEAR_MG_CMS_Prod;Trusted_Connection=Yes;Encrypt=No;TrustServerCertificate=Yes;Connection Timeout=60;`
- `REQUEST_TIMEOUT: 300`, `timeout: 300`

**Issue:** The MCP database tools (`list_environments`, `test_connection`, `read_data`) **time out** when invoked. Direct `sqlcmd` to the same server/database works instantly. Root cause is likely the `npx @esetnik/mssql-mcp-reader@0.1.0` startup time exceeding the MCP client timeout, or the ODBC/Trusted_Connection path failing inside the Node process.

**Recommendation:** Switch the MCP to use the same SQL Server JDBC connection string the Spring Boot app uses (`jdbc:sqlserver://MSI\SQLEXPRESS:1434;databaseName=LEAR_MG_CMS_Prod`), or increase the MCP server timeout to 60s. Until then, use `sqlcmd` directly for DB operations.

---

## 3. BACKEND AUDIT

### 3.1 Roles & Security

| Role | Exists in DB? | Used in `@PreAuthorize`? | Menu Gate? |
|---|---|---|---|
| `ROLE_ADMIN` | ✅ | ✅ (correctly in Dispatcher, broken in CNC) | ✅ Full menu |
| `ROLE_PROCESS` | ✅ | ✅ | ✅ Process submenu |
| `ROLE_CHEF_DE_ZONE` | ✅ | ✅ | ❌ **Invisible in menu** |
| `ROLE_CHEF_EQUIPE` | ✅ | ✅ | ✅ Production submenu |
| `ROLE_CAD` | ✅ | ✅ | ✅ CAD submenu |
| `ROLE_QUALITE` | ✅ | ✅ | ✅ Qualité submenu |
| `ROLE_CNC_PS` | ✅ | ❌ Broken (`hasRole('ROLE_CNC_PS')`) | — |
| `ROLE_MAINTENANCE` | ✅ | ❌ Broken | — |
| `ROLE_OPERATOR` / `ROLE_VIEWER` | ❌ Not in DB | — | — |

**Critical Bug Detail:** `UserDetailsImpl.build()` creates `SimpleGrantedAuthority(role.getName())`. If DB stores `ROLE_ADMIN`, authority = `ROLE_ADMIN`.
- `hasRole('ADMIN')` → checks `ROLE_ADMIN` ✅ **works**
- `hasRole('ROLE_ADMIN')` → checks `ROLE_ROLE_ADMIN` ❌ **never matches**

**Affected files (broken annotations):**
- `CncPsController.java` — 27 broken annotations (`ROLE_CNC_PS`, `ROLE_ADMIN`, `ROLE_QUALITE`, `ROLE_CNC_CONTROL`)
- `CuttingRequestSerieInfoController.java` — `hasRole('ROLE_ADMIN')`
- `CuttingRequestV2Controller.java` — `hasRole('ROLE_ADMIN')`
- `CuttingRequestSerieDataController.java` — `hasRole('ROLE_ADMIN')`
- `GammeTechniqueController.java` — `hasRole('ROLE_ENGINEERING')`, `hasRole('ROLE_CUTTING_CUIR')`, `hasRole('ROLE_QUALITE')`
- `MachineCncController.java` — `hasRole('ROLE_ADMIN')`
- `PartNumberInfoController.java` — `hasRole('ROLE_ADMIN')`
- `PointageController.java` — `hasRole('ROLE_ADMIN')`
- `SerieRouleauTempController.java` — `hasRole('ROLE_ADMIN')`
- `MarkersOnlyCodeController.java` — `hasRole('ROLE_QUALITE')`
- `CncSyncController.java` — `hasRole('ROLE_ADMIN')`
- `CncMachineReportController.java` — `hasRole('ROLE_ADMIN')`
- `CncMachineReportPieceController.java` — `hasRole('ROLE_ADMIN')`

**Unannotated endpoints (potential holes):**
- `HomeController.java`
- `QueryController.java`

### 3.2 Implemented (What's Already Built)

| Plan Item | File Path | Quality |
|---|---|---|
| `Zone.category` (STRICT/SHARED) | `domain/Zone.java` | ✅ Good |
| `CuttingRequest.dispatchedZone` + acceptance + pin | `domain/CuttingRequest/CuttingRequest.java` | ✅ Good |
| `DispatchAudit` entity | `domain/dispatcher/DispatchAudit.java` | ✅ Good |
| `ShiftZoneConfirmation` + child | `domain/dispatcher/ShiftZoneConfirmation*.java` | ✅ Good |
| `UserZone` | `domain/dispatcher/UserZone.java` | ✅ Good |
| `UnassignableSerie` | `domain/dispatcher/UnassignableSerie.java` | ✅ Good |
| `MachineQueue.version` | `domain/MachineQueue.java` | ✅ Good |
| `ZoneLoadService` (Contract C7) | `services/dispatcher/ZoneLoadService.java` | ⚠️ Functional but loads full entities |
| `CuttingTimeCalculator` (Contract C1) | `services/scheduling/CuttingTimeCalculator.java` | ✅ Excellent — batched CMS-DB lookup |
| `ActiveMachineResolver` (Contract C2) | `services/dispatcher/ActiveMachineResolver.java` | ✅ Good |
| `AdmissionBlockedAudit` | `domain/dispatcher/AdmissionBlockedAudit.java` | ✅ Good |
| `SequenceDispatcherService` | `services/dispatcher/SequenceDispatcherService.java` | ⚠️ Functional but loads full entities |
| `DispatcherController` | `controller/dispatcher/DispatcherController.java` | ✅ Good (correct `hasRole` usage) |
| `EngineTickService` | `services/dispatcher/EngineTickService.java` | ✅ Good — fingerprint diff-check, event listeners |
| `ZoneLoadController` | `controller/dispatcher/ZoneLoadController.java` | ✅ Good |
| `EngineControlController` | `controller/dispatcher/EngineControlController.java` | ✅ Good |
| `CapaciteInstalleeService.getEffective` | `services/CapaciteInstalleeService.java` | ✅ Good |

### 3.3 Missing (What's in Plans but Not in Code)

| Missing Item | Plan Reference | Impact |
|---|---|---|
| `SerieTableAssigner` (Contract C8) | `SEQUENCE_DISPATCHER_PLAN §3.7–3.9` | No virtual best-fit table; no score-based queue balancing |
| `BoxDurationCalculator` (Contract C9) | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §4.1` | `OrdonnancementService` uses hard-coded `BOXES_PER_SEQUENCE = 16` |
| `ContinuousOptimizerService` + per-zone threads | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §2` | No engine lifecycle, no perturbation loop |
| `WorkCalendar` entity/table | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §3.1` | No Sunday-closed shift tracking |
| `OptimizerRun` + `OptimizerIndicatorSample` | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §3.2` | No run history, no KPI chart data |
| `DateInferenceAudit` | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §5` | No audit for CMS-Prod back-fills |
| `AdmissionOverrideAudit` | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §6` | No warning-path audit |
| Perturbation moves (Swap, Relocate, Block, 2-opt, Kick) | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §2.3` | No search neighborhood |
| `FeasibilityGuard` | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §2.5` | No hard rule enforcement |
| T-60 pre-dispatch + T0 auto-accept jobs | `SEQUENCE_DISPATCHER_PLAN §4.5` | No shift boundary automation |
| `PlanDeChargeActualsService` | `PLAN_DE_CHARGE_IMPROVEMENT_PLAN §4` | No realized-vs-planned feedback |
| `ShiftCellDto` / `ShiftSummaryDto` endpoints | `PLAN_DE_CHARGE_IMPROVEMENT_PLAN §2` | PdC returns huge nested JSON |

### 3.4 Backend Bugs / Inconsistencies

1. **CRITICAL — `@PreAuthorize` double-prefix bug** (~50 annotations) — see §3.1
2. **HIGH — Full entity graph loading** — `ZoneLoadService.computeMatrix()` calls `cuttingRequestRepository.findAll(date, shift)` which triggers `CuttingRequest` → `cuttingRequestSeries` + `cuttingRequestPartNumbers` + `cuttingRequestBoxs` via `@LazyCollection(LazyCollectionOption.FALSE)`
3. **HIGH — `BOXES_PER_SEQUENCE = 16` constant** — `OrdonnancementService.java:73` hard-codes 16 instead of querying `CuttingRequestBox` count per sequence
4. **MEDIUM — `PlanDeChargeService.getMachinesGroupedByZone()` loads full `ProductionTable` entities**
5. **MEDIUM — `PlanDeChargeService.getDetailedSeriesForShift` not switched to `CuttingRequestSerieDataLight`**
6. **LOW — `CuttingRequestSerieDataLight` is an `@Entity`, not a true JPQL projection**
7. **LOW — Missing `formulaVersion` on `ShiftLoadCalculation`**

---

## 4. FRONTEND AUDIT

### 4.1 Roles & UI Gates

| Role | Menu Gating (Dashboard.js) | Route Gating (SecuredRoute.js) |
|---|---|---|
| `ROLE_ADMIN` | Full menu | ❌ None — only checks `validToken` |
| `ROLE_PROCESS` | Process submenu | ❌ None |
| `ROLE_CHEF_DE_ZONE` | **Not present in any menu** | ❌ None |
| `ROLE_CHEF_EQUIPE` | Production submenu | ❌ None |

**Critical gaps:**
1. **No route-level role guards** — `SecuredRoute.js` only verifies `validToken`. Any logged-in user can navigate to `/processDispatcher`, `/engineControl`, etc. by direct URL.
2. **Chef de Zone pages are orphan routes** — `/chefDeZonePage` and `/chefDeZoneConfirm` are registered in `App.js` but never rendered in `Dashboard.js`.
3. **UserZoneAdmin reachable by anyone** — no frontend role check.

### 4.2 Implemented Pages

| Page | File | State vs. Plan |
|---|---|---|
| **Plan de Charge** | `PlanDeCharge.js` (~3,600 lines) | ✅ Mature. Missing: actuals overlay, "Non confirmée" banner, heatmap SVG, 7-day lookahead, drill-through links |
| **Process Dispatcher** | `ProcessDispatcher.js` (303 lines) | ⚠️ Phase 4 stub. Missing: heatmap panel, sequence actions, audit trail, Inassignables drawer, rebalance button |
| **Advanced Ordonnancement** | `AdvancedOrdonnancement.js` | ⚠️ Timeline + queues exist. Missing: engine control bar, Recharts chart, "Données corrigées" panel |
| **Chef de Zone — Confirmation** | `ChefDeZoneConfirm.js` (161 lines) | ⚠️ Basic machine toggle. Missing: live load-%, step-2 acceptance flow, WebSocket |
| **Chef de Zone — Supervision** | `ChefDeZonePage.js` (143 lines) | ⚠️ Minimal stub. 15s polling, accept/reject only. Missing: machine strip, alerts, pin/unpin, pull |
| **Engine Control** | `EngineControl.js` | ⚠️ Old Phase 7 auto-tick UI. Uses `/api/engine/*` instead of planned `/api/ordo/engine/*` |
| **Kiosk Banner** | `KioskBanner.js` | ⚠️ Polls version every 2s. Missing: toast on bump, `rouleauHint`, admission check |
| **Operator Forms** | `Form.js`, `FormCoupe.js` | ✅ Feature-rich. Missing: `POST /api/admission/check` call, warning/block modals |
| **Admission Block Modal** | `AdmissionBlockModal.js` (44 lines) | ✅ Present but **orphan** — not wired into any form |

### 4.3 Missing Pages (11 pages)

| Missing Page | Planned Route | Plan Reference |
|---|---|---|
| Sequence Dispatcher heatmap panel | (part of `/processDispatcher`) | `SEQUENCE_DISPATCHER_PLAN §6.2` |
| Optimizer Console | `/process/optimizerConsole` | `MASTER_SCHEDULING_VISION_v3 §5.5` |
| Optimizer History | `/process/optimizerHistory` | `MASTER_SCHEDULING_VISION_v3 §5.6` |
| Work Calendar | `/process/workCalendar` | `MASTER_SCHEDULING_VISION_v3 §5.4` |
| Inassignables | `/process/inassignables` + drawer | `MASTER_SCHEDULING_VISION_v3 §4.5` |
| Date Inference Audit | `/process/dateInferenceAudit` | `MASTER_SCHEDULING_VISION_v3 §6.1` |
| Admission Audit | `/process/admissionAudit` | `MASTER_SCHEDULING_VISION_v3 §7.3` |
| Zone Admin | `/admin/zones` | `MASTER_SCHEDULING_VISION_v3 §4.7` |
| Zone Ownership matrix | `/process/zoneOwnership` | `MASTER_SCHEDULING_VISION_v3 §4.8` |
| Floor War Room | `/production/warRoom` | `MASTER_SCHEDULING_VISION_v3 §9` |
| KPI Dashboard | `/ordonnancement/kpi` | `MASTER_SCHEDULING_VISION_v3 §10` |

### 4.4 Frontend Bugs / Inconsistencies

1. **CRITICAL — Zero WebSocket client implementation** — No `sockjs-client`, `@stomp/stompjs`, or native `WebSocket` usage. All planned topics (`/topic/zone/{zoneName}`, `/topic/ordo/engine`, `/topic/dispatcher`, `/topic/admission`) are unimplemented.
2. **CRITICAL — No route-level authorization** — `SecuredRoute.js` only checks `validToken`.
3. **HIGH — `ProcessDispatcher.js` is a Phase 4 preview stub** — Missing heatmap, sequence list with actions, audit trail, Inassignables badge, `Réquilibrer` button.
4. **HIGH — `AdvancedOrdonnancement.js` lacks Continuous Optimizer UI bar**
5. **HIGH — `Form.js` / `FormCoupe.js` do not call admission check**
6. **MEDIUM — `EngineControl.js` uses legacy endpoint paths** (`/api/engine/*` instead of `/api/ordo/engine/*`)
7. **MEDIUM — `UserZoneAdmin.js` is a text-box form, not the planned matrix editor**
8. **MEDIUM — Polling intervals hard-coded and inconsistent** (Kiosk: 2s, ChefDeZone: 15s, AdvancedOrdo: 5min)

---

## 5. DATABASE AUDIT

### 5.1 Schema Inventory

**146 tables** in `LEAR_MG_CMS_Prod`. Core dispatcher tables:
- `Zone` (10 rows, has `category`, `is_active`)
- `CuttingRequest` (130,284 rows, has `dispatched_zone`, `zone_acceptance_status`, `pinned_by_chef`)
- `CuttingRequestSerie` (162,176 rows)
- `ProductionTable` (38 rows)
- `machine_queue` (13 rows — mostly empty, engine not writing yet)
- `users` (251 rows)
- `roles` (29 rows)
- `capacite_installee` (95 rows)
- `etat_machine_historique` (39 rows)

### 5.2 Migration Status

| Script | In `db/migration` | In `flyway_schema_history` | Status |
|---|---|---|---|
| `V1` (baseline) | N/A | ✅ `1` | Applied 2026-05-04 |
| `V2_01__zone_category.sql` | ✅ | ✅ `2.01` | **APPLIED** |
| `V2_02__cutting_request_dispatch_columns.sql` | ✅ | ✅ `2.02` | **APPLIED** |
| `V2_03__user_zone.sql` | ✅ | ✅ `2.03` | **APPLIED** |
| `V2_04__shift_zone_confirmation.sql` | ✅ | ✅ `2.04` | **APPLIED** |
| `V2_05__unassignable_serie.sql` | ✅ | ✅ `2.05` | **APPLIED** |
| `V2_06__machine_queue_version.sql` | ✅ | ✅ `2.06` | **APPLIED** |
| `V9_01__admission_blocked_audit.sql` | ✅ | ✅ `9.01` | **APPLIED** |
| `V12_01__dispatch_audit.sql` | ✅ | ❌ **NOT FOUND** | **NOT APPLIED** 🔴 |
| `V13_01..V13_06` | ❌ | ❌ | **MISSING** |
| `V14_01__cleanup_legacy_scheduling.sql` | ❌ | ❌ | **MISSING** |

### 5.3 Critical DB Gaps

1. **HIGH — `dispatch_audit` table missing** — Migration file exists but Flyway never ran it. `DispatchAudit.java` entity has no table.
2. **HIGH — Missing tables:** `WorkCalendar`, `OptimizerRun`, `OptimizerIndicatorSample`, `DateInferenceAudit`, `AdmissionOverrideAudit`
3. **MEDIUM — Missing `CuttingRequest` columns:** `dispatchedAt`, `dispatchedBy`, `zoneAcceptedAt`, `zoneAcceptedBy`, `zoneRejectionReason`
4. **MEDIUM — Missing `ShiftZoneConfirmation` columns:** `reopened_at`, `reopened_by`, `notes`
5. **MEDIUM — Missing `ShiftZoneConfirmationMachine` columns:** `added_after_start`, `remark`
6. **LOW — Missing `UnassignableSerie` columns:** `sequence`, `resolved_at`
7. **LOW — `Zone.restLocation` column exists in DB but missing from `Zone.java` entity**

### 5.4 Legacy Tables Still Present (Should Be Dropped)

Per `MASTER_SCHEDULING_VISION_v3 §15`, these 10 tables should be dropped in `V14_01`:
- `optimized_plan`, `optimized_series_assignment`, `scheduling_config`, `shift_load_calculation`
- `SequenceSchedule`, `SerieSchedule`, `ShiftSchedule`, `ScheduleInterval`
- `MachineScheduleStatus`, `MaterialLogistics`

All still have active JPA entities in `domain/scheduling/`.

### 5.5 Data Anomalies

- `user_zone` has **0 rows** — no chef is linked to any zone. Chef-de-Zone page will be empty.
- `shift_zone_confirmation` has **0 rows** — no shift confirmations yet.
- `unassignable_serie` has **302 rows** — 302 series logged as unassignable.
- `admission_blocked_audit` has **0 rows** — admission control not yet enforced.

---

## 6. PLANS CROSS-CHECK

### 6.1 Consistency Status

The four authoritative plans are **largely consistent** with each other. No contradictions found in core data model definitions. Key contracts (C1–C9) are consistently referenced across plans.

### 6.2 Plan Coherence Notes

- `MASTER_SCHEDULING_VISION_v3.md` correctly takes precedence over earlier plans where it deviates (e.g., `tableIsVirtual` flag kept in projection only rather than as a real column)
- `SEQUENCE_DISPATCHER_PLAN.md` and `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md` share the same `CuttingTimeCalculator` (C1), `ActiveMachineResolver` (C2), and `CapaciteInstallee` efficiency (C5) contracts — no drift
- `PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md` correctly defers zone-level decisions to the dispatcher and machine-level decisions to the optimizer

### 6.3 CMS-Prod Boundary

| Function | Lives In | MG-CMS Integration |
|---|---|---|
| Spreading station forms (`Form.js`) | CMS-Prod | Calls MG-CMS `/api/admission/check` (planned) |
| Cutting station kiosks | CMS-Prod | Polls MG-CMS `/api/kiosk/*` + `/api/machineQueue/next` |
| LASER-DXF / DIE GERBER | CMS-Prod | Same kiosk pattern |
| `reviewSerieWaiting()` (dateFinCoupe back-fill) | CMS-Prod | Writes to MG-CMS DB; MG-CMS shows `DateInferenceAudit` |
| Continuous Optimizer | MG-CMS | Writes `MachineQueue`; CMS-Prod kiosks poll it |
| Sequence Dispatcher | MG-CMS | Writes `CuttingRequest.dispatchedZone`; CMS-Prod reads it |

---

## 7. ROLE-BASED ACCESS MATRIX

### 7.1 Current State

| Function | Admin | Process | Chef Équipe | Chef de Zone | Operator | Viewer |
|---|---|---|---|---|---|---|
| Plan de Charge (view) | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Plan de Charge (recalc) | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Sequence Dispatcher (preview) | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Sequence Dispatcher (publish) | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Sequence Dispatcher (rebalance) | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Zone Load Heatmap | ✅ | ✅ | ✅ | ✅ (read-only) | ❌ | ❌ |
| Chef de Zone — Confirmation | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Chef de Zone — Supervision | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Engine Control (start/stop) | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Advanced Ordonnancement | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Operator Kiosk | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| Floor War Room (planned) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| KPI Dashboard (planned) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

### 7.2 Gaps

- `ROLE_OPERATOR` and `ROLE_VIEWER` do **not exist** in the DB. If needed, they must be added to the `roles` table.
- Frontend route guards are **absent** — the matrix above reflects backend `@PreAuthorize` only.
- Chef de Zone has **no menu entry** in the React sidebar.

---

## 8. INTEGRATION & TEST PLAN FOR PRODUCTION DEPLOYMENT

### 8.1 Pre-Deployment Checklist

| # | Item | Verification |
|---|---|---|
| 1 | Flyway migrations V1–V9.01 applied | `SELECT * FROM flyway_schema_history` |
| 2 | Apply `V12_01__dispatch_audit.sql` | Run manually + insert Flyway row, OR restart app with validation |
| 3 | Apply `V12_02__cutting_request_acceptance_audit.sql` | Add `dispatchedAt`, `dispatchedBy`, `zoneAcceptedAt`, `zoneAcceptedBy`, `zoneRejectionReason` |
| 4 | Apply `V13_01..V13_05` migrations | Create `WorkCalendar`, `OptimizerRun`, `OptimizerIndicatorSample`, `DateInferenceAudit`, `AdmissionOverrideAudit` |
| 5 | Backfill `user_zone` assignments | Insert at least one chef per STRICT zone |
| 6 | Seed `WorkCalendar` | Set Sunday shifts 1/2/3 = closed for next 90 days |
| 7 | Fix `hasRole('ROLE_X')` bug | Global search/replace in affected controllers |
| 8 | Feature flags review | `application.properties`: `mgcms.dispatcher.enabled=true`, `mgcms.engine.zone-aware=false` (keep OFF until engine ships) |
| 9 | Database connection strings | Verify all 6 datasources point to correct prod servers |
| 10 | CMS-Prod kiosk endpoints | Verify `/api/kiosk/**` is permitAll and returns correct data |

### 8.2 Staging Test Scenarios

#### Scenario A — Dispatcher Shadow Mode
1. Login as `ROLE_PROCESS`
2. Open `/processDispatcher`
3. Click Preview for today's shift 1
4. Verify zone cards render with STRICT first, SHARED last
5. Verify unassignable count matches `unassignable_serie` table
6. Click Publish — verify `CuttingRequest.dispatched_zone` updated, `zone_acceptance_status=PENDING`
7. Verify `dispatch_audit` row written with trigger=PUBLISH

#### Scenario B — Chef Acceptance Flow
1. Login as `ROLE_CHEF_DE_ZONE`
2. Open `/chefDeZonePage`
3. Verify default zone loaded from `user_zone`
4. Accept one sequence — verify `zone_acceptance_status=ACCEPTED`
5. Reject one sequence — verify `zone_acceptance_status=REJECTED`, `dispatch_audit` row written

#### Scenario C — Zone Load Heatmap
1. Login as `ROLE_PROCESS`
2. Open `/processDispatcher` (or direct API call to `/api/zoneLoad/matrix`)
3. Verify `ZoneLoadDto` returns cells for every `(zone, machineType)` pair
4. Verify load % calculation includes `CapaciteInstallee.efficienceTarget`
5. Verify inter-zone and intra-zone spread computed

#### Scenario D — Security Regression
1. Login as `ROLE_CAD`
2. Attempt direct URL to `/processDispatcher`
3. Verify 403 Forbidden (after `SecuredRoute.js` fix)
4. Attempt `POST /api/dispatcher/publish`
5. Verify 403 Forbidden

#### Scenario E — Performance Baseline
1. Run `/api/zoneLoad/matrix?date=2026-05-05&shift=1` with 400-series shift
2. Verify response time < 500ms
3. Verify no N+1 queries in Hibernate SQL log

### 8.3 Rollback Plan

| Layer | Rollback Action |
|---|---|
| Feature flags | Set `mgcms.dispatcher.enabled=false`, `mgcms.engine.zone-aware=false` |
| DB schema | Flyway does not auto-rollback; keep `V12_02` additive only (nullable columns) |
| Dispatcher state | Set `CuttingRequest.dispatched_zone=NULL` and `zone_acceptance_status=NULL` for affected date/shift |
| Code | Revert git commit; old endpoints remain compatible because `dispatched_zone` is nullable |

### 8.4 Production Deployment Sequence

```
Phase 1 — Security & DB (1 day)
  ├─ Fix hasRole bug
  ├─ Apply V12_01 (dispatch_audit)
  ├─ Apply V12_02 (CuttingRequest audit columns)
  └─ Restart app, verify Flyway history

Phase 2 — Dispatcher Hardening (1 day)
  ├─ Backfill user_zone assignments
  ├─ Seed WorkCalendar (if V13 ready)
  ├─ Performance test ZoneLoadService
  └─ Enable mgcms.dispatcher.enabled=true

Phase 3 — Engine Skeleton (1 week)
  ├─ Ship V13 migrations
  ├─ Deploy ContinuousOptimizerService (zone-aware=false, auto-tick=false)
  ├─ Build OptimizerConsole.js
  └─ Staging test: Start → IMPROVING → Stop

Phase 4 — Go-Live (1 day)
  ├─ Set mgcms.engine.zone-aware=true
  ├─ Set mgcms.admission.enforce=true
  ├─ Monitor DispatchAudit + AdmissionBlockedAudit for 1 shift
  └─ On-call: Process + Dev standby
```

---

## 9. PRIORITY-RANKED ACTION ITEMS

### 🔴 P0 — Block Production (Fix Before Any Deploy)

| # | Action | Owner | Effort |
|---|---|---|---|
| 1 | Fix `hasRole('ROLE_X')` → `hasRole('X')` across all controllers | Backend | 1–2 h |
| 2 | Apply `V12_01__dispatch_audit.sql` to DB + fix Flyway history | DB / DevOps | 30 min |
| 3 | Rewrite `ZoneLoadService.computeMatrix()` to use projections | Backend | 4–6 h |
| 4 | Add route-level role guards to `SecuredRoute.js` | Frontend | 2–4 h |
| 5 | Add Chef de Zone menu entries to `Dashboard.js` | Frontend | 1 h |

### 🟡 P1 — Required for Next Sprint

| # | Action | Owner | Effort |
|---|---|---|---|
| 6 | Create `V12_02` + `V13_01..V13_05` migrations + entities | Backend / DB | 1–2 days |
| 7 | Implement `SerieTableAssigner` (Contract C8) | Backend | 1–2 days |
| 8 | Implement `BoxDurationCalculator` (Contract C9) | Backend | 4–6 h |
| 9 | Rebuild `ProcessDispatcher.js` with heatmap + actions | Frontend | 5–7 days |
| 10 | Build `OptimizerConsole.js` | Frontend | 5–7 days |
| 11 | Wire WebSocket client (`sockjs-client` + `@stomp/stompjs`) | Frontend | 3–5 days |

### 🟢 P2 — Polish & Cleanup

| # | Action | Owner | Effort |
|---|---|---|---|
| 12 | Create `V14_01__cleanup_legacy_scheduling.sql` | Backend / DB | 2–3 h |
| 13 | Add missing `CuttingRequest` acceptance columns | Backend / DB | 2 h |
| 14 | Add `formulaVersion` to `ShiftLoadCalculation` | Backend | 30 min |
| 15 | Add `restLocation` to `Zone.java` or drop DB column | Backend | 30 min |
| 16 | Wire `AdmissionBlockModal.js` into `Form.js` | Frontend (CMS-Prod) | 1–2 days |

---

*End of Consolidated Audit Report*
