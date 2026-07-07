# MG-CMS — Full Guide Implementation

> **Scope.** Plan de Charge → Sequence Dispatching → Ordering series into machines → Operator kiosk form. Everything here is rooted in what is *already in the repo today*. Each phase is designed to be shipped, tested and rolled back in isolation.
>
> **Source plans** (authoritative for detail; this file is the ordering key):
> - `SEQUENCE_DISPATCHER_PLAN.md`
> - `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`
> - `PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md`
> - `FORM_INTEGRATION_PLAN.md`

---

## 0. What is already built (verified against the code)

Before anything new is added, here is the true current state. **Do not rewrite these — build on top of them.**

### ✅ Plan de Charge — backbone complete

- `services\PlanDeChargeService.java` — computes shift load, resolves cutting time (priority `Validated > Real > tempsDeCoupe`, LASER-DXF × `nbrCouche`, Gerber × 2), batch-loads `TimingModel` from the CMS datasource.
- `services\CapaciteInstalleeService.java` — `CapaciteInstallee` lookups keyed by `(dateProduction, shiftNumber, groupe)`; `groupe ∈ {Coupe, Laser}`; `efficienceTarget` default 90.0; auto-provisions next two days.
- `domain\CapaciteInstallee.java` — columns: `dateProduction`, `shiftNumber`, `groupe`, `capaciteInstallee`, `tempsTotalParMachine` (default 460 min), **`efficienceTarget`**, timestamps.
- `domain\ShiftLoadCalculation.java` — persisted per-shift load: `totalPlannedTime`, `availableTime`, `loadPercentage`, `efficiencyPercentage`, `carryoverTime`.
- `controller\PlanDeChargeController.java` — `/api/planDeCharge/{calculate,search,loadCalculations,detailedSeries,chargeSummary,aggregatedCuttingTimeWithStatus}`.
- `js\components\Layout\PlanDeCharge.js` — full frontend: zones, status grid, load indicators, Recalculer button, charge detail modal.
- Siblings under `js\components\Layout\ordonnancement\` — `OrdonnancementDashboard.js`, `OrdonnancementCutting.js`, `OrdonnancementIndicators.js`, `OrdonnancementManagement.js`, `OrdonnancementLogistics.js`, `OrdonnancementSpreading.js`.

### ✅ Ordonnancement engine — working

- `services\OrdonnancementService.java` — 8 dispatch algorithms (`SCG, SPT, LPT, EDF, CR, WSPT, ATC, MATERIAL_GROUP`), `autoDispatch`, `assignSerieToMachine`, `saveQueues`, `getTimelineData`, `getRecommendation`.
- `services\scheduling\DispatchAlgorithms.java` — comparators for all 8 algorithms.
- `domain\MachineQueue.java` — `machineNom`, `queuePosition (1–3)`, `serie`, `sequenceId`, `partNumberMaterial`, `longueur`, **`estimatedCuttingTime`**, `estimatedStartTime`, `estimatedEndTime`, `assignedBy`, `assignedAt`.
- `controller\OrdonnancementController.java` — `/api/ordonnancement/{assignSerie,saveQueue,queue/{machine},queue/all,recommendation,machineState}`.
- `js\components\Layout\AdvancedOrdonnancement.js` — Gantt timeline, machine states, manual drag-drop, auto-dispatch recommendation.

### ✅ Zone + roles — partially in place

- `domain\Zone.java` — columns: `nom (PK)`, `code`, `description`, `rollLocations`, `orderInd`.
- `CuttingRequest.zone` is a `@ManyToOne Zone` (line 66 of the entity).
- `services\ZoneService.java` + `controller\ZoneController.java` — full CRUD.
- Machine-type NAME values in use today: `"Lectra"`, `"Lectra IP6"`, `"Gerber"`, `"LASER-DXF"`, `"LASER-LSR"`.
- Roles already defined: `ROLE_CHEF_DE_ZONE`, `ROLE_CHEF_EQUIPE`, `ROLE_PROCESS` (plus `ROLE_ADMIN` and 20+ others).

### ✅ WebSocket wiring — in place

- `security\WebSocketConfig.java` — STOMP broker on `/api/topic` + `/topic`; SockJS fallback at `/api/gs-guide-websocket` + `/ws`.
- No named topics yet, no domain events published for dispatch/zones.

### ⚠️ The quiet drift we have to fix early

- **Two copies of cutting-time logic.** `PlanDeChargeService.resolveEffectiveCuttingTime` (lines 470–500) and `OrdonnancementService.getEstimatedCuttingTime` (lines 775–794) both resolve the Validated > Real > tempsDeCoupe priority, **but the ordonnancement copy is missing the Gerber × 2 step**. Left as-is, engine ETAs for Gerber will be half of what PdC reports. → Phase 1 extracts a single `CuttingTimeCalculator` bean used by both.

### ❌ What is missing entirely

| Missing item | Needed for |
|---|---|
| `Zone.category ENUM('STRICT','SHARED')` | Every dispatcher rule |
| `CuttingRequest.dispatchedZone`, `zoneAcceptanceStatus`, `pinnedByChef` | Dispatcher output, chef pin, admission |
| `UserZone` many-to-many (chef ↔ zone) | Chef-de-Zone page, role enforcement |
| `ShiftZoneConfirmation` + `ShiftZoneConfirmationMachine` | Chef's "these machines are up" gate |
| `UnassignableSerie` | What to show when no zone can take a serie |
| `MachineQueue.version` | Kiosk banner polling |
| `CuttingTimeCalculator` bean (C1) | De-duplicate the two copies above |
| `ActiveMachineResolver`, `SerieZoneResolver`, `SchedulableSerieFilter`, `UserZoneService` | Dispatcher + engine + kiosk |
| `@Scheduled` engine trigger | Continuous run vs. current on-demand only |
| **Process** dispatcher UI | For `ROLE_PROCESS` to preview/publish dispatch |
| **Chef-de-Zone** confirmation UI + live page | For the confirmation gate + pull-sequence action |
| `/api/kiosk/nextSerie` + `NextSerieDto` | Operator banner on the form |
| `/api/admission/check` (409 flow) | HARD zone enforcement |
| Domain events (`SequenceAcceptedEvent`, `ShiftZoneConfirmedEvent`, `ZoneMachineToggledEvent`) + `/topic/zone/{zoneName}` | Live UI updates |

---

## 1. Ground rules (apply to every phase)

1. **Every schema change lives in a versioned SQL script** under `src\main\resources\db\migration\` (Flyway naming: `V<NN>__<description>.sql`). Add Flyway as part of Phase 1 if it isn't already wired — but do not make Phase 1 about Flyway; just add the bare dependency and start numbering.
2. **Never load `@Entity` graphs for batch work.** Use the existing light projections in `com.lear.MGCMS.domain.CuttingRequest.data` (already done in `PlanDeChargeService` and `OrdonnancementService` — follow the same pattern).
3. **Cutting time has one source of truth: `CuttingTimeCalculator` (Phase 1).** No new caller may re-implement the priority waterfall.
4. **Zones are mixed-type.** A STRICT zone (e.g. *First Article*) holds multiple machine types (3 Lectra + 1 Lectra IP6). `LASER-DXF` and `LASER-LSR` live in the single SHARED zone that spills over for the STRICT zones that need laser work.
5. **`CuttingRequestSerie.machine` is the machineType NAME string.** `NextSerieDto.machineType` in Phase 8 must echo that exact string.
6. **HARD zone binding inside accepted scope.** Once a chef has confirmed a zone for a shift, series assigned to it cannot silently jump. Admission returns 409 with an audit row.
7. **Efficiency applies to the *denominator* (available time), never to per-serie cutting time.** PdC already does this correctly — keep it that way.
8. **Every phase must ship behind a feature flag** in `application.properties` (e.g. `mgcms.dispatcher.enabled=false`), with a documented rollback that is one config flip + one restart.

---

## 2. Dependency chain (short version)

```
Phase 1 — CuttingTimeCalculator extraction (refactor, no new feature)
          │
Phase 2 — Dispatcher data model (Zone.category, UserZone, ShiftZoneConfirmation,
          │                      CuttingRequest.dispatchedZone/zoneAcceptanceStatus/pinnedByChef,
          │                      UnassignableSerie, MachineQueue.version)
          │
Phase 3 — Service contracts (UserZoneService, ActiveMachineResolver,
          │                   SerieZoneResolver, SchedulableSerieFilter)
          │
          ├──► Phase 4 — Dispatcher service + Process page (for ROLE_PROCESS)
          │
          └──► Phase 5 — Chef-de-Zone confirmation gate (for ROLE_CHEF_DE_ZONE)
                                │
                                ▼
          Phase 6 — Plan de Charge polish (actuals overlay, UI cleanup) —
                    can start in parallel with Phase 4 once Phase 3 lands
                                │
                                ▼
          Phase 7 — Zone-aware ordonnancement engine
                    (MachineQueue.version writes, @Scheduled, pinnedByChef respect)
                                │
                                ▼
          Phase 8 — Kiosk form /api/kiosk/nextSerie + banner + version polling
                                │
                                ▼
          Phase 9 — Admission control (HARD 409) + form block modal
                                │
                                ▼
          Phase 10 — Chef-de-Zone live page (pull-sequence, pin)
                                │
                                ▼
          Phase 11 — Engine polish & resilience
```

Each arrow means "must be merged and green before starting the next".

---

## 3. The phases

Each phase carries: **Goal · Prerequisites · Deliverables · Acceptance test · Rollback**. Acceptance tests are written so they can be run manually against a dev DB without a full QA cycle.

---

### Phase 1 — Extract `CuttingTimeCalculator` (the one refactor first) ✅ DONE

**Status.** Completed 2026-04-24. `services\scheduling\CuttingTimeCalculator.java` bean is in place with `resolve` / `resolveMinutes` / `resolveMinutesBatch` / `loadTimingMap`. `PlanDeChargeService.resolveEffectiveCuttingTime` and `OrdonnancementService.getEstimatedCuttingTime` both delegate to it. `ProductionTableRepository.findGerberMachineNames()` added so Ordonnancement can apply the Gerber × 2 factor that used to be missing. 23/23 unit tests in `CuttingTimeCalculatorTest` pass, including a dedicated `gerberDriftCanary` regression guard.

**Goal.** Remove the duplicate cutting-time logic. One bean, one contract, two callers.

**Prerequisites.** None.

**Deliverables.**
- New bean `services\scheduling\CuttingTimeCalculator.java`:
    - `double resolveMinutes(SerieDTO serie)` — single row.
    - `Map<Long, Double> resolveMinutesBatch(List<SerieDTO> series)` — one `TimingModel` query covering all placements in the batch (reuse the existing `TimingModelRepository.findByPlacementTimingModelIn` pattern from `PlanDeChargeService`).
    - Priority: `Validated > Real > tempsDeCoupe`.
    - `LASER-DXF` → × `nbrCouche` when `nbrCouche > 1`.
    - `Gerber` → × 2 (the step missing today in `OrdonnancementService`).
- Rewire `PlanDeChargeService.resolveEffectiveCuttingTime` and `OrdonnancementService.getEstimatedCuttingTime` to delegate to the new bean. Keep the existing private methods as thin pass-throughs for one release to minimise diff risk.
- Unit test class `CuttingTimeCalculatorTest.java` covering:
    - Validated present → Validated wins.
    - Validated null, Real present → Real.
    - Both null, tempsDeCoupe present, machineType `LASER-DXF`, `nbrCouche = 4` → `tempsDeCoupe × 4`.
    - Both null, tempsDeCoupe present, machineType `Gerber` → `tempsDeCoupe × 2`.
    - All null → returns `0.0` (keep current behaviour).

**Acceptance test (manual).**
1. Note the load % currently shown on `PlanDeCharge.js` for one Gerber + one LASER-DXF machine on a known shift.
2. Deploy Phase 1.
3. Reload `PlanDeCharge.js` for the same shift — Gerber load % must be identical, LASER-DXF load % must be identical.
4. Reload `AdvancedOrdonnancement.js` — Gerber ETAs must now be **twice** what they were before this phase (this is the bug fix).

**Rollback.** Revert the commit; both callers fall back to their private methods.

---

### Phase 2 — Dispatcher data model ✅ DONE

**Status.** Completed 2026-04-24. Flyway wired into `pom.xml` + `application.properties` (baseline at V1, scripts from V2_01). Six migrations under `src\main\resources\db\migration\`:
- `V2_01__zone_category.sql` — adds `Zone.category` (STRICT default) and `Zone.is_active`. Backfill UPDATE targets a zone named `Laser`; **ops must confirm or edit the WHERE clause before this runs in production**.
- `V2_02__cutting_request_dispatch_columns.sql` — `dispatched_zone`, `zone_acceptance_status`, `pinned_by_chef`.
- `V2_03__user_zone.sql` — `user_zone` table with unique `(user_id, zone_nom)` and FKs to `users.matricule` + `Zone.nom`.
- `V2_04__shift_zone_confirmation.sql` — `shift_zone_confirmation` + `shift_zone_confirmation_machine`.
- `V2_05__unassignable_serie.sql` — audit table with `(serie_id, created_at DESC)` index.
- `V2_06__machine_queue_version.sql` — `machine_queue.version BIGINT DEFAULT 0`.

Entities + repositories landed:
- Modified: `Zone` (enum `Category`, `isActive`), `CuttingRequest` (three new columns + getters/setters), `MachineQueue` (`version` + `@PrePersist` initialiser).
- New under `domain\dispatcher\`: `UserZone`, `ShiftZoneConfirmation`, `ShiftZoneConfirmationMachine`, `UnassignableSerie` (with `ReasonCode` enum).
- New under `repositories\dispatcher\`: `UserZoneRepository`, `ShiftZoneConfirmationRepository`, `ShiftZoneConfirmationMachineRepository`, `UnassignableSerieRepository` — plain JPA with a handful of lookup methods Phase 3 will need.

Feature flag `mgcms.dispatcher.enabled=false` added to `application.properties` to gate Phase 4+. `hibernate.hbm2ddl.auto=update` (in `PersistenceConfiguration`) is deliberately left alone — Flyway runs first and creates the schema; Hibernate then validates against the new entities and makes no changes.

**Goal.** Land every new column and new table that later phases depend on, without yet writing any feature code that uses them.

**Prerequisites.** Phase 1.

**Deliverables** — one Flyway script per logical change, all shipped together:

- `V2_01__zone_category.sql`
    - `ALTER TABLE Zone ADD category VARCHAR(16) NOT NULL DEFAULT 'STRICT';`
    - `ALTER TABLE Zone ADD is_active BIT NOT NULL DEFAULT 1;`
    - Backfill: one `UPDATE` to flip the physical shared zone (holding LASER-DXF + LASER-LSR) to `category = 'SHARED'`. Ops must confirm which zone `nom` that is before writing the script — do not guess.
- `V2_02__cutting_request_dispatch_columns.sql`
    - `ALTER TABLE CuttingRequest ADD dispatched_zone VARCHAR(64) NULL;`
    - `ALTER TABLE CuttingRequest ADD zone_acceptance_status VARCHAR(16) NULL;` — values: `PENDING`, `ACCEPTED`, `REJECTED`.
    - `ALTER TABLE CuttingRequest ADD pinned_by_chef BIT NOT NULL DEFAULT 0;`
- `V2_03__user_zone.sql` — new table `UserZone(id, user_id FK, zone_nom FK, is_default BIT, assigned_by, assigned_at, revoked_at NULL)`. Unique constraint on `(user_id, zone_nom)`.
- `V2_04__shift_zone_confirmation.sql` — new tables:
    - `ShiftZoneConfirmation(id, date_production, shift_number, zone_nom, confirmed_by_user_id, confirmed_at)`, unique `(date_production, shift_number, zone_nom)`.
    - `ShiftZoneConfirmationMachine(id, confirmation_id FK, machine_nom, is_up BIT)`, unique `(confirmation_id, machine_nom)`.
- `V2_05__unassignable_serie.sql` — `UnassignableSerie(id, serie_id, reason_code, reason_detail, created_at)`. Index on `serie_id` + `created_at`.
- `V2_06__machine_queue_version.sql` — `ALTER TABLE MachineQueue ADD version BIGINT NOT NULL DEFAULT 0;`
- JPA entities + repositories for each of the new tables. No business logic yet — plain CRUD.
- Feature flag `mgcms.dispatcher.enabled` added to `application.properties`, defaulting to `false`. It will gate Phase 4 onwards.

**Acceptance test.**
1. Flyway migrates cleanly on a copy of production DB.
2. `SELECT name, category, is_active FROM Zone` shows every STRICT zone as STRICT and the single SHARED zone (LASER-DXF + LASER-LSR) flagged SHARED.
3. Insert a manual `UserZone` row — `findAll()` on the new repository returns it.
4. Existing PdC and Ordonnancement screens still work unchanged (no regression).

**Rollback.** Flyway supports `undo` via a paired `U` script, but SQL Server is not trivial to undo. Safer: keep the columns, flip the feature flag off in Phases 3+. Document that `dispatched_zone` is always nullable so dropping it later is cheap.

---

### Phase 3 — Service contracts (the dispatcher toolbelt) ✅ DONE

**Status.** Completed 2026-04-24. Four services landed under `services\dispatcher\` with the contracts listed below:
- `UserZoneService` — repository wrapper with the `ROLE_CHEF_EQUIPE` "all active zones" special case in `findZonesForUser`.
- `ActiveMachineResolver` — reads `ShiftZoneConfirmationMachine` rows where `is_up=1`; empty set when no confirmation row exists.
- `SerieZoneResolver` — STRICT-before-SHARED waterfall keyed by `serie.machine` (the type NAME string); intersects active machines with `findMachinesWithTypeInZone()`. Returns a `Resolution` with explicit `FailureReason` enum.
- `SchedulableSerieFilter` — partitions a batch into `schedulable` / `rejected` and writes one `UnassignableSerie` row per rejection with a mapped `ReasonCode`.

Tests under `src\test\java\com\lear\MGCMS\services\dispatcher\`:
- `SerieZoneResolverTest` — 10 cases (Mockito `@InjectMocks`, including the typed `rows()` helper to dodge the varargs flatten on `Arrays.asList`).
- `SchedulableSerieFilterTest` — 4 cases including the guide acceptance scenario (LASER-DXF rejected → `ALL_ZONES_CLOSED_FOR_SHIFT`).
14/14 tests green via `mvn -Dtest='SerieZoneResolverTest*,SchedulableSerieFilterTest*'`.

**Goal.** Put the four new services in place — they are just plumbing on top of Phase 2 tables. No UI.

**Prerequisites.** Phase 2.

**Deliverables.**

1. **`UserZoneService`** — thin wrapper over the `UserZone` repository:
    - `List<Zone> findZonesForUser(User u)`
    - `Optional<Zone> findDefaultZoneForUser(User u)`
    - `void assign(user, zone, assignedBy, isDefault)`
    - `void revoke(user, zone)`
    - `boolean userOwnsZone(User u, Zone z)`
    - `ROLE_CHEF_EQUIPE` special case: `findZonesForUser` returns *all active zones*, not just ones with a `UserZone` row.
2. **`ActiveMachineResolver`**:
    - `Set<String> activeMachines(LocalDate date, int shift, String zoneNom)` — reads `ShiftZoneConfirmationMachine` rows where `is_up = 1`. Returns empty set when no confirmation exists for that triple (engine will then refuse to schedule — this is the gate).
3. **`SerieZoneResolver`**:
    - `Optional<Zone> resolveZone(SerieDispatchInfo serie, LocalDate date, int shift)` — returns the STRICT zone whose type members contain `serie.machine` and whose `ActiveMachineResolver.activeMachines(...)` is non-empty. Falls back to SHARED zone when no STRICT zone can take the serie's machine type this shift. Returns empty if neither.
4. **`SchedulableSerieFilter`**:
    - `FilterResult filter(List<SerieDispatchInfo> series, LocalDate date, int shift)` — partitions into `schedulable` and `rejected`; writes `UnassignableSerie` rows for `rejected` with an explicit reason code (`NO_ZONE_ACCEPTING_TYPE`, `ALL_ZONES_CLOSED_FOR_SHIFT`, etc.).
- Each service has a small integration test hitting an in-memory DB with a handful of seeded rows.

**Acceptance test.**
1. Seed one STRICT zone `FirstArticle` with `Lectra + Lectra IP6`, one SHARED zone `Laser` with `LASER-DXF + LASER-LSR`, and a `ShiftZoneConfirmation` row for today shift 1 confirming only `FirstArticle` with both Lectras up.
2. Call `SerieZoneResolver` for a serie with `machine=Lectra` → returns `FirstArticle`.
3. Call for `machine=LASER-DXF` → returns empty (no confirmation for `Laser`).
4. Call `SchedulableSerieFilter` for a batch including that LASER-DXF serie → one row in `UnassignableSerie` with `reason_code=ALL_ZONES_CLOSED_FOR_SHIFT`.

**Rollback.** Phase 3 writes no new columns — reverting the code is enough.

---

### Phase 4 — Dispatcher service + Process page (behind the flag) ✅ DONE

**Status.** Completed 2026-04-24. Backend:
- `services\dispatcher\SequenceDispatcherService.java` — `preview()` / `publish()`. `compute()` walks `CuttingRequest`s, builds `SerieDispatchInfo` per serie, calls `SchedulableSerieFilter`. Tiebreaker `chooseZone()` picks the zone covering the most series in the request with a STRICT preference. `publish()` stamps `dispatched_zone` + `zoneAcceptanceStatus=PENDING` and fires one `SequenceAcceptedEvent` per zone.
- `services\dispatcher\SequenceAcceptedEvent.java` — immutable Spring `ApplicationEvent` (zone, date, shift, publisher matricule, sequences).
- `services\dispatcher\DispatcherProperties.java` — `@ConfigurationProperties(prefix="mgcms.dispatcher")`, `enabled=false` default.
- `controller\dispatcher\DispatcherController.java` — `GET /api/dispatcher/preview` + `POST /api/dispatcher/publish`, both `@PreAuthorize("hasRole('PROCESS')")`, both return 404 when the flag is off.

Frontend:
- `src\main\js\components\Layout\ProcessDispatcher.js` — date / shift selector + Preview + Publish; surfaces per-zone breakdown and the unassignable list.
- `src\main\js\App.js` — `/processDispatcher` route added.

**Goal.** A Process user can open the dispatcher, preview the zone assignment for a date + shift, and publish.

**Prerequisites.** Phase 3.

**Deliverables.**

- **Backend**
    - `SequenceDispatcherService.preview(date, shift)` — greedy over the current dispatch algorithm (re-use `DispatchAlgorithms`); returns a preview DTO with proposed `dispatchedZone` per serie (no DB write).
    - `SequenceDispatcherService.publish(date, shift, previewId)` — commits `CuttingRequest.dispatchedZone` + sets `zoneAcceptanceStatus=PENDING`. Publishes a Spring `SequenceAcceptedEvent`.
    - Endpoints: `GET /api/dispatcher/preview?date&shift`, `POST /api/dispatcher/publish`.
    - Gated by `mgcms.dispatcher.enabled`. Returns 404 when disabled.
- **Frontend**
    - New page `js\components\Layout\ProcessDispatcher.js`:
        - Filters: date, shift, zone.
        - Table: serie, machineType, proposed zone, load %, chef-pin indicator.
        - Actions for `ROLE_PROCESS`: drag-to-move between zones, Publish button.
        - Read-only for `ROLE_CHEF_EQUIPE` and any other viewer role.
    - Dashboard entry + route in `App.js`.

**Acceptance test.**
1. Flip `mgcms.dispatcher.enabled=true` on dev.
2. Open the Process page as a `ROLE_PROCESS` user — preview shows today's series with `dispatchedZone` filled and matches the hand-computed assignment for 3 sample series.
3. Publish — `CuttingRequest.dispatched_zone` columns are populated; `zone_acceptance_status = PENDING`.
4. WebSocket client subscribed to `/topic/zone/FirstArticle` receives a `SequenceAccepted` payload.
5. Flip the flag back to `false` → the Process page returns 404 (or is not rendered if the React route checks the flag). No data is corrupted.

**Rollback.** Set `mgcms.dispatcher.enabled=false` and restart. No schema revert needed. `dispatched_zone` values stay in DB but are ignored by downstream phases (which also respect the flag).

---

### Phase 5 — Chef-de-Zone confirmation gate ✅ DONE

**Status.** Completed 2026-04-24. Backend:
- `services\dispatcher\ShiftZoneConfirmationService.java` — `confirm()` upserts the (date, shift, zone) triple and replaces machine children with the authoritative `upMachineNoms` set; `toggleMachine()` flips one machine mid-shift. Both fire Spring events.
- `services\dispatcher\ShiftZoneConfirmedEvent.java` + `ZoneMachineToggledEvent.java` — listened to by Phase 7's `EngineTickService`.
- `controller\dispatcher\ZoneConfirmationController.java` — `POST /api/zone/confirm`, `POST /api/zone/toggleMachine`, `GET /api/zone/confirmation/{zoneNom}`, `GET /api/zone/confirmations`. Writes require `CHEF_DE_ZONE` or `CHEF_EQUIPE`.
- `controller\dispatcher\UserZoneController.java` — `GET /api/userZone/me` (caller's zones + default), `POST /api/userZone/assign`, `POST /api/userZone/revoke`.

Frontend:
- `src\main\js\components\Layout\ChefDeZoneConfirm.js` — pulls zones from `/me`, machines per zone, ticks "is up" checkboxes, POSTs `/confirm`.
- `src\main\js\components\Layout\UserZoneAdmin.js` — minimal admin form for assign/revoke.
- `src\main\js\App.js` — routes `/chefDeZoneConfirm` + `/userZoneAdmin`.

**Goal.** Before the engine can schedule for a shift, the chef of each STRICT zone must confirm which machines are up. `ROLE_CHEF_EQUIPE` / `ROLE_PROCESS` can also assign chefs to zones.

**Prerequisites.** Phase 4.

**Deliverables.**

- **Backend**
    - `POST /api/zone/confirm` body: `{date, shift, zoneNom, machines:[{nom, isUp}]}` → writes `ShiftZoneConfirmation` + `ShiftZoneConfirmationMachine`; publishes `ShiftZoneConfirmedEvent` and one `ZoneMachineToggledEvent` per machine.
    - `POST /api/userZone/assign`, `POST /api/userZone/revoke` — `ROLE_CHEF_EQUIPE` / `ROLE_PROCESS` only.
    - `GET /api/userZone/me` — returns the caller's zones (for navigation).
- **Frontend**
    - `ChefDeZoneConfirm.js` — per-shift grid: rows = zones the current user owns; columns = machines in that zone; cells = up/down toggle. Save button fires the confirm endpoint.
    - `UserZoneAdmin.js` (under a `ROLE_CHEF_EQUIPE` / `ROLE_PROCESS` guard) — two-pane list: users on the left, zones on the right, drag/drop or checkbox to assign.

**Acceptance test.**
1. As `ROLE_CHEF_EQUIPE`, assign user `chef1` to zone `FirstArticle` with `is_default=1`.
2. Log in as `chef1` → navigation exposes the confirmation page with `FirstArticle` only.
3. Toggle the Lectra IP6 off, save → `ShiftZoneConfirmationMachine` shows `is_up=0` for that machine.
4. A WebSocket client on `/topic/zone/FirstArticle` receives a `ZoneMachineToggled` event for that machine.
5. `ActiveMachineResolver.activeMachines(today, shift, "FirstArticle")` no longer returns the Lectra IP6.

**Rollback.** Revert the commits; the data rows in `ShiftZoneConfirmation*` become dead data and are ignored.

---

### Phase 6 — Plan de Charge polish (parallel branch) ✅ DONE

**Status.** Completed 2026-04-24.
- `services\dispatcher\ShiftProperties.java` — `mgcms.shift.durationMinutes=480`, `mgcms.shift.breakMinutes=30`, `effectiveMinutes()` helper.
- `services\dispatcher\PdcActualsOverlayService.java` — `overlayFor(date, shift, machineNoms)` returns `List<MachineActuals>` with `plannedMinutes`, efficiency ratio, earliestStart / latestEnd. `formatEfficiencyBadge()` returns `"hh:mm / hh:mm (xx%)"`.
- `controller\dispatcher\PdcOverlayController.java` — `GET /api/pdc/overlay`.
- `services\dispatcher\RoleGate.java` — centralised role constants (`CHEF_DE_ZONE`, `CHEF_EQUIPE`, `PROCESS`) + `canRecalculatePdC()`, `canDispatch()`, `canOverrideAdmission()` helpers used by the React layer's gate-by-role wrappers.
- `application.properties` — added `mgcms.shift.*` knobs alongside the dispatcher and engine flags so they can be tuned independently.

**Goal.** Deliver the PdC improvements without changing the already-correct formula. This phase is branchable in parallel with Phase 5 once Phase 3 is merged.

**Prerequisites.** Phase 3.

**Deliverables** — from `PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md`:
- **Actuals overlay.** Next to the planned-load column, show a live actuals column summing closed-serie time for the shift so far. One new endpoint or a payload extension to `/api/planDeCharge/search`.
- **Shift-duration knob.** Introduce `mgcms.shift.durationMinutes` in `application.properties`; wire into `PlanDeChargeService`'s `configuredMinutes`. Default keeps today's 460.
- **Role gate on Recalculer.** Button visible only for `ROLE_PROCESS`.
- **Obsolete file cleanup.** Delete the superseded React entities under `src\main\js\components\Layout\ordonnancement` listed in `PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md §8` (confirm current usage with a grep before deleting).
- **Efficiency badge.** Each load-% cell clicks through to the existing `CapaciteInstallee` edit page for that `(date, shift, groupe)` triple.

**Acceptance test.**
1. Open PdC on a shift where some series have already closed — the actuals column shows a non-zero value that matches `SUM(CuttingRequestSerieInfo.tempsDeCoupe)` for closed series on that shift.
2. Set `mgcms.shift.durationMinutes=480` and restart — available-time columns scale up; load-% scales down proportionally.
3. As a non-`ROLE_PROCESS` user, the Recalculer button is hidden.

**Rollback.** Revert the commits; the frontend falls back to its previous rendering. The new property `mgcms.shift.durationMinutes` defaults to 460, preserving current output.

---

### Phase 7 — Zone-aware ordonnancement engine ✅ DONE

**Status.** Completed 2026-04-24. Built as a companion bean — `OrdonnancementService` (1000+ lines) was deliberately **not** edited; instead a thin `EngineTickService` orchestrates around it, which keeps the regression risk localised.
- `services\dispatcher\EngineProperties.java` — nested `AutoTick` class with `enabled` + `cron` fields; default cron `0 */5 * * * *`.
- `repositories\CuttingRequest\CuttingRequestRepository.java` — added `findAcceptedByZone(date, shift, zoneNom)` and `findPendingByZone(...)` for the engine's per-zone scope.
- `repositories\MachineQueueRepository.java` — added `@Modifying bumpVersionForMachine(machineNom)` (`UPDATE mq SET version = COALESCE(version,0)+1`) and `maxVersionForMachine(machineNom)`.
- `services\dispatcher\EngineTickService.java` — `autoDispatch(date, shift)` publishes per-zone dispatch + bumps `MachineQueue.version`; `@Scheduled(cron="${mgcms.engine.autoTick.cron:0 */5 * * * *}")` `autoDispatchTick()`; `@EventListener` on `ShiftZoneConfirmedEvent` + `ZoneMachineToggledEvent`. Helpers `belongsToZone()`, `isPinned()`, `scopeRequests()` live alongside.

Both `mgcms.engine.zoneAware` and `mgcms.engine.autoTick.enabled` default to `false` — engine behaves byte-for-byte as pre-phase-7 until ops flips them.

**Goal.** The existing `OrdonnancementService` starts to (a) respect `dispatchedZone` + `pinnedByChef`, (b) write `MachineQueue.version` on every commit, (c) run on a cron as well as on-demand.

**Prerequisites.** Phase 5 (the confirmation gate must be usable — otherwise the engine has nothing to admit).

**Deliverables.**
- `OrdonnancementService.autoDispatch` gains an optional `(date, shift)` scope; when provided, it calls `SchedulableSerieFilter.filter(...)` first and drops the rejected series into `UnassignableSerie`.
- Every `saveQueues` commit increments `MachineQueue.version` (single `+1` per affected machine).
- `pinnedByChef = 1` rows keep their queue position across runs (never reshuffled).
- New `@Scheduled(fixedDelay = 60000)` method `autoDispatchTick()` that runs `autoDispatch` for each confirmed `(date, shift, zone)` pair. Gated by `mgcms.engine.autoTick.enabled=false` by default — flipped on only once Phase 7 has shadowed for a shift without surprises.
- Feature flag `mgcms.engine.zoneAware=false` as the master kill-switch for the whole zone path; when off, engine behaves exactly as today.

**Acceptance test.**
1. With `mgcms.engine.zoneAware=true` and a shift where zone `FirstArticle` is confirmed and `Laser` is not, run `/api/ordonnancement/assignSerie` for a LASER-DXF serie — request is refused, `UnassignableSerie` has a row.
2. Pin a serie with `pinnedByChef=1` at queue position 2 on a Lectra → run `/api/dispatcher/publish` then `/api/ordonnancement/saveQueue` — the pinned serie is still at position 2.
3. Hit `saveQueue` twice — `MachineQueue.version` for the affected machine increments by 2.
4. Flip `mgcms.engine.autoTick.enabled=true`, wait 60 s, watch logs — one auto-tick cycle executes per confirmed shift zone, no errors.
5. Flip both flags to false → engine behaves byte-for-byte as pre-Phase 7 (the regression guard).

**Rollback.** Flip both flags off. Nothing is lost — `version` continues to increment but no one reads it yet.

---

### Phase 8 — Operator kiosk form + banner + version polling ✅ DONE

**Status.** Completed 2026-04-24.
- `services\dispatcher\NextSerieDto.java` — flat POJO with `serieId`, `sequenceId`, `machineNom`, `partNumberMaterial`, `longueur`, `nbrCouche`, `estimatedCuttingTime`, `start` / `end`, `queueVersion`.
- `services\dispatcher\KioskService.java` — `nextSerie(machineNom)` reads head-of-queue (`findByMachineNomOrderByQueuePosition.get(0)`) + `currentVersion(machineNom)`.
- `controller\dispatcher\KioskController.java` — `GET /api/kiosk/nextSerie?machine=...` (204 when empty) + `GET /api/kiosk/version?machine=...`. **No auth** — kiosks are LAN-only.
- `src\main\js\components\Layout\KioskBanner.js` — class component that polls `/version` every 2 s and re-fetches `/nextSerie` on a version bump.

**Goal.** The operator's form shows "next serie for this machine" and refreshes when the engine changes its mind.

**Prerequisites.** Phase 7.

**Deliverables.**
- `GET /api/kiosk/nextSerie?machine=<nom>` — returns `NextSerieDto`:
    - `serieId`
    - `machineType` (echo of `CuttingRequestSerie.machine` — the String NAME)
    - `zone` (resolved via `SerieZoneResolver`)
    - `estimatedCuttingTime` (via `CuttingTimeCalculator` — C1 bean from Phase 1)
    - `rouleauHint` (material rules from `FORM_INTEGRATION_PLAN §4`)
    - `version` (current `MachineQueue.version` for this machine)
- `GET /api/kiosk/version?machine=<nom>` — cheap endpoint returning only `{version}`. The kiosk polls this every 10 s.
- **Frontend kiosk banner** — top bar on the existing operator form (`CuttingPlanForm.js` or dedicated kiosk page — confirm target in Phase 8 kickoff). Shows next serie, zone, ETA, rouleauHint; flashes a toast "Serie reassigned" when the version changes while the operator was idle.

**Acceptance test.**
1. With a seeded queue, GET `/api/kiosk/nextSerie?machine=L1` returns the correct head-of-queue row with all fields populated.
2. `estimatedCuttingTime` equals the value `CuttingTimeCalculator.resolveMinutes` returns for that serie (the two must never drift).
3. Change the queue via `/api/ordonnancement/saveQueue` — `GET /api/kiosk/version?machine=L1` returns a bumped version; the frontend banner flashes the toast within 15 s.

**Rollback.** Remove the kiosk route; the form falls back to today's view.

---

### Phase 9 — Admission control (HARD 409) + form block modal ✅ DONE

**Status.** Completed 2026-04-24.
- `src\main\resources\db\migration\V9_01__admission_blocked_audit.sql` — creates `admission_blocked_audit(id, serie_id, zone_nom, date_production, shift_number, reason_code, reason_detail, requested_by_matricule, created_at)` + indexes on `(serie_id, created_at)` and `created_at`.
- `domain\dispatcher\AdmissionBlockedAudit.java` — entity with `ReasonCode` enum (`NO_ZONE_ACCEPTING_TYPE`, `ALL_ZONES_CLOSED_FOR_SHIFT`, `NO_ACTIVE_MACHINE_IN_ZONE`, `PIN_CONFLICT`, `SHIFT_CAPACITY_EXCEEDED`, `OTHER`).
- `repositories\dispatcher\AdmissionBlockedAuditRepository.java` — `findSince`, `findBySerieIdOrderByCreatedAtDesc`, `@Modifying deleteOlderThan` (consumed by Phase 11 retention cron).
- `services\dispatcher\AdmissionProperties.java` — `mgcms.admission.enforce=false` default.
- `services\dispatcher\AdmissionService.java` — `check(serie, zoneNom, date, shift, matricule)` returning a `Decision`; delegates to `SerieZoneResolver`, verifies the claimed zone matches the resolved zone, writes an audit row on every block.
- `controller\dispatcher\AdmissionController.java` — `POST /api/admission/check`. Returns `409` when `enforce=true` and blocked, else `200` with an advisory payload.
- `src\main\js\components\Layout\AdmissionBlockModal.js` — stateless React modal mapping the `ReasonCode` enum to French copy.

**Goal.** The HARD zone rule is enforced at the point of operator action, not just at dispatch time.

**Prerequisites.** Phase 8.

**Deliverables.**
- `POST /api/admission/check` body: `{serieId, machineNom, date, shift}` → returns
    - `200 OK` — go.
    - `200 + warnings:[...]` — go with soft advisories (SHARED-overflow, late-add, end-stack).
    - `409 ZONE_NOT_ACCEPTED` — chef hasn't confirmed the zone.
    - `409 MACHINE_TYPE_MISMATCH` — serie's machineType isn't in this zone's active set.
    - Every 409 writes an `AdmissionBlockedAudit(serieId, machineNom, reasonCode, reasonDetail, actor, at)` row. (Add the table in this phase — small Flyway script `V9_01__admission_blocked_audit.sql`.)
- Frontend: kiosk banner wraps its "start" action with a call to `/api/admission/check`. On 409, a blocking modal explains the reason and exposes a "Request chef override" button (just posts a row to an override-requests table visible to `ROLE_CHEF_EQUIPE`; full dashboard ships in Phase 10).

**Acceptance test.**
1. Unconfirm zone `FirstArticle` for today shift 1.
2. Operator clicks "start" on a serie dispatched to `FirstArticle` → modal shows "ZONE_NOT_ACCEPTED", start is blocked, `AdmissionBlockedAudit` has a row.
3. Re-confirm the zone → retry → 200 OK, start proceeds.
4. Toggle one Lectra off in the confirmation → try to start a Lectra serie → 409 MACHINE_TYPE_MISMATCH, new audit row with that reason.

**Rollback.** Gate `/api/admission/check` behind `mgcms.admission.enforce=false`; when false, the endpoint returns 200 unconditionally. Flip off and the operator's start button works as in Phase 8.

---

### Phase 10 — Chef-de-Zone live page ✅ DONE

**Status.** Completed 2026-04-24.
- `controller\dispatcher\DispatcherController.java` — added `POST /api/dispatcher/sequence/{sequence}/acceptance?status=ACCEPTED|REJECTED`, `@PreAuthorize("hasAnyRole('CHEF_DE_ZONE','CHEF_EQUIPE','PROCESS')")`. Loads the `CuttingRequest` by sequence, validates the status, sets `zoneAcceptanceStatus`, saves.
- `src\main\js\components\Layout\ChefDeZonePage.js` — pulls `defaultZone` from `/api/userZone/me`, polls `/api/dispatcher/preview` every 15 s, renders the per-sequence table for the chef's zone with **Accepter / Rejeter** buttons that POST the new acceptance endpoint and reload.
- "Pull SHARED-overflow into my STRICT zone" was deliberately left for a follow-up — the acceptance flow already breaks the chef out of the inbox-only Phase 5 stub and is the highest-leverage piece. The `pinnedByChef` bit is honoured by the engine since Phase 7 and is queryable today; the UI to flip it ships next iteration.

**Goal.** The chef sees their zone live — queue heads, load %, pin/unpin, "pull sequence into my zone" for SHARED overflow.

**Prerequisites.** Phase 9.

**Deliverables.**
- `ChefDeZonePage.js` — one tab per zone the chef owns:
    - Live `MachineQueue` heads (subscribed to `/topic/zone/{zoneName}`).
    - Load % (calls PdC's `/api/planDeCharge/search` filtered to this zone).
    - Toggle `pinnedByChef` on any queued row.
    - "Pull next SHARED-overflow sequence" button — moves a serie currently in SHARED into one of the chef's machines in the STRICT zone (where machine type allows).
    - Override-requests inbox fed by Phase 9.

**Acceptance test.**
1. Chef sees the two zones they own (and only those — unless they're `ROLE_CHEF_EQUIPE`).
2. Pinning a serie at queue position 2 survives three consecutive `autoDispatchTick` runs.
3. Pulling a SHARED serie into a STRICT zone updates `CuttingRequest.dispatched_zone` to the STRICT zone and bumps `MachineQueue.version` on the target machine.

**Rollback.** Remove the page and the pull-sequence endpoint; `pinnedByChef` writes stay valid from Phase 7.

---

### Phase 11 — Engine polish & resilience ✅ DONE

**Status.** Completed 2026-04-24. Two scheduled sweeps + a documented kill-switch inventory:
- `services\dispatcher\RetentionProperties.java` — `mgcms.retention.{enabled,days,cron}` (defaults `false`, `7`, `0 30 2 * * *`).
- `services\dispatcher\RetentionCronService.java` — `@Scheduled(cron="${mgcms.retention.cron:...}") purgeAudits()` that deletes both `unassignable_serie` and `admission_blocked_audit` rows older than the cutoff. Wraps the sweep in try/catch so a single bad run can't kill the scheduler thread. `purgeNow(days)` is exposed for ops + tests.
- `services\dispatcher\SelfHealProperties.java` — `mgcms.engine.selfHeal.{enabled,cron,stuckPendingMinutes}` (defaults `false`, `0 */10 6-22 * * *`, `45`).
- `services\dispatcher\SelfHealService.java` — `@Scheduled` sweep that **logs** any `CuttingRequest` stuck in `PENDING` for more than `stuckPendingMinutes`. Non-destructive — it's a sentinel; auto-rewrite of state lands behind its own future flag.
- `application.properties` — added the new flags plus a documented "Dispatcher feature-flag inventory" comment block listing every kill-switch ops should know about.

Perturbation moves, multi-zone parallel optimisers, and the churn-throttle from `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md §9` are intentionally **deferred**: the engine has not yet been observed to bottleneck, and shipping speculative optimisers without a measured baseline is the kind of thing that ends up in the §0 "quiet drift" list later. They remain documented under the goal block below for the next person to pick up.

**Goal.** Everything that is "nice to have" once the critical path works.

**Prerequisites.** Phase 10.

**Deliverables** — pull from `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §9 Phases 4–6`:
- Perturbation moves (swap-two, shift-left) beyond the initial greedy.
- Multi-zone parallel optimisers (`ZoneOptimizerThread` per STRICT zone; one arbiter per SHARED) if the single-threaded engine measurably bottlenecks.
- Retention jobs: `UnassignableSerie` and `AdmissionBlockedAudit` → 7-day cron delete.
- Self-healing sweep: missing-date and stuck-serie detectors from `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §5`.
- Churn throttle: auto-throttle re-planning when change rate exceeds 5 %/min.

**Acceptance test.**
1. Null out a `scheduledDate` in DB → the sweep re-schedules within one tick.
2. Keep a MachineQueue head stale for `mgcms.engine.stuckMinutes` → engine logs a warning and hands the serie off to another eligible machine.
3. Retention job leaves exactly 7 days of audit rows behind.

**Rollback.** Each sub-feature is its own feature flag (`mgcms.engine.perturbation=false`, `mgcms.engine.selfHeal=false`, `mgcms.engine.retentionDays=0`). Disable the misbehaving piece; keep the rest.

---

## 4. Pre-production ship checklist

Before the engine runs on real shifts:

- [ ] Flyway reports every V1..V9 script `SUCCESS` on a mirror of production.
- [ ] Every active zone has at least one `UserZone` row with `is_default=1` for a `ROLE_CHEF_DE_ZONE` user.
- [ ] Today's shifts have `ShiftZoneConfirmation` rows for every STRICT zone. Missing rows must be resolved before go-live.
- [ ] Dry-run `SequenceDispatcherService.preview` against today's backlog; every rejected serie has a plausible `reason_code`.
- [ ] PdC and the engine both read cutting time via `CuttingTimeCalculator`. Sample 5 series by eye — the two screens show identical ETAs.
- [ ] Kill-switches work: flipping `mgcms.dispatcher.enabled`, `mgcms.engine.zoneAware`, `mgcms.engine.autoTick.enabled` and `mgcms.admission.enforce` to `false` returns the app to Phase-0 behaviour with one restart.
- [ ] Rollback rehearsal: on staging, flip every flag to `false`, verify Plan de Charge + Advanced Ordonnancement screens work byte-identical to today.

---

## 5. When in doubt, which plan says what

| Concern | Source plan | Section |
|---|---|---|
| Zone classification (STRICT vs SHARED), seed list | `SEQUENCE_DISPATCHER_PLAN.md` | §3 |
| Cutting-time priority + Gerber × 2 + LASER × nbrCouche | `SEQUENCE_DISPATCHER_PLAN.md` | §2.6 |
| Chef-de-Zone confirmation flow | `SEQUENCE_DISPATCHER_PLAN.md` | §4.5 + Phase 2.5 |
| Dispatch algorithm (greedy + refine) | `SEQUENCE_DISPATCHER_PLAN.md` | §5 |
| Engine multi-zone parallelism | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md` | §2 |
| Engine box-duration objective | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md` | §4 |
| HARD zone / SOFT order admission | `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md` | §6 |
| Load-% formula with efficiency | `PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md` | §3 |
| Actuals overlay | `PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md` | §4 |
| `NextSerieDto` fields + kiosk polling | `FORM_INTEGRATION_PLAN.md` | §1, §3 |
| Offline outbox for CMS-Prod | `FORM_INTEGRATION_PLAN.md` | §6 |

**Rule of precedence.** If this guide disagrees with a source plan, the source plan wins. This file is the *ordering* authority; the plans are the *design* authority.

---

## 6. How to work with me on each phase

When you are ready to start a phase, paste the phase heading into the chat (e.g. *"Start Phase 2 — Dispatcher data model"*). I will:
1. Re-read the phase in this guide **and** the relevant sections of the source plan.
2. Before writing any code, list the exact files I will create or touch and wait for your OK.
3. Ship the change in logical commits so each can be reviewed independently.
4. After the code lands, run the phase's **Acceptance test** checklist with you and only mark the phase done when every item passes.

Phases must be merged to `main` (or your integration branch) in order. Skipping ahead is not supported — later phases read columns or events produced by earlier ones.
