# Sequence Dispatcher — Zone-Level Macro Balancing

> **Project:** MG-CMS (Spring Boot + React + SQL Server) — Lear Corp.
> Trim 1, Tangier TFZ.
> **Scope:** A new page in the **Process** section that dispatches
> **sequences** (`CuttingRequest`) into **strict zones**, and a mirror
> page in the **Production** section for `ROLE_CHEF_DE_ZONE`.
> **Authoritative scheduling docs consumed:**
> [md/PLAN_DE_CHARGE.md](../md/PLAN_DE_CHARGE.md) (macro, per-shift totals)
> and [md/Advanced_Ordonancement.md](../md/Advanced_Ordonancement.md)
> (micro, serie-to-machine dispatch).
> **Sits between them:** where Plan de Charge says *"this shift has 1200 min
> of Lectra work"*, and Advanced Ordonnancement says *"serie #987 goes on
> LECTRA-03 at 14:22"*, this feature answers the missing middle question:
> *"which zone does sequence 2026-04-20-12 belong to?"*.

---

## 0. Core idea in one paragraph

A **zone** is a physical bay containing a **mix of machine types**
(e.g. the *First Article* zone holds 3 Lectra + 1 Lectra IP6; the
*Serie* zone holds its own Lectra + Lectra IP6 blend). Zones are
classified as either **STRICT** (owned by a specific area of the plant —
First Article, Serie, etc., each with its own Chef de Zone) or
**SHARED** (cross-plant pools like *LASER-DXF* and *LASER-LSR* that any
strict zone may borrow from when it lacks the matching machine type).
**`CuttingRequestSerie.machine` is the machine-type NAME** (`Lectra`,
`Lectra IP6`, `LASER-DXF`, …) — a serie can only run on a
`ProductionTable` whose `machineType.nom` equals that value.

### 0.1 Two separate engines (do not conflate)

This plan covers the **Dispatching Engine** only. There is a *second*,
unrelated engine — the **Ordonnancement Engine** — covered by
`plans/ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`. The two engines
must stay decoupled, ship in separate releases, and present **separate
control UIs**:

| Engine | Decision | Where its UI lives | Role to configure |
|---|---|---|---|
| **Dispatching Engine** (this plan) | Sequence → STRICT zone (zone-level macro balance) | `ProcessDispatcher.js` (page route `/processDispatcher`) | `ROLE_PROCESS`, `ROLE_ADMIN` |
| **Ordonnancement Engine** (other plan) | Serie → machine + start time, *within* a confirmed zone | `AdvancedOrdonnancement.js` (page route `/advancedOrdonnancement`) | `ROLE_PROCESS` only |

The Dispatching Engine writes `CuttingRequest.dispatchedZone`. The
Ordonnancement Engine reads that as a hard constraint and never
revisits it. They never share threads, schedulers, or state machines.

Sequences are dispatched **to exactly one STRICT zone**. Inside that
zone each of the sequence's series runs on the strict zone's own
machines when a matching type is present; any serie whose machine type
is absent from the strict zone automatically flows to a **SHARED** zone
that carries it. A sequence is **feasible for a strict zone** only when
*every* one of its series has at least one machine of the required type
in the candidate zone **or** in some shared zone.

The dispatcher's job is two-fold:
1. **Inter-zone balance** — no strict zone runs at 110 % while another
   sits at 40 %.
2. **Intra-zone type balance** — avoid the classic Tangier failure
   mode where a single strict zone ends up with "too many Lectra
   series and not enough Lectra IP6 series" (or the reverse), leaving
   some machines starved while the others burn.

Balancing uses the explicit formula (per zone AND per machine type
inside the zone):

```
zoneLoad%(type, zone) = Σ (cutting time of type series routed to zone)
                       ─────────────────────────────────────────
                       (num_active_machines_of_type × shiftMinutes × efficiency)
```

Capacity + efficiency come from the existing `CapaciteInstallee` table
(keyed by `dateProduction, shiftNumber, groupe` with `groupe ∈ {Coupe,
Laser}`) — **no new efficiency entity is introduced**.

Cutting time is resolved from the CMS DB via `TimingModel`
(keyed by placement): priority `Validated_Cutting_time_Timing_Model >
Real_Cutting_time_Timing_Model > CuttingRequestSerie.tempsDeCoupe`.

The dispatcher result is persisted on `CuttingRequest.dispatchedZone`;
from there, Advanced Ordonnancement and its Continuous Optimizer (see
`plans/ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`) restrict their
search to machines inside that zone plus the SHARED pool.

---

## 1. How this fits with Plan de Charge and Advanced Ordonnancement

```
┌──────────────────────────────────────────────────────────────────┐
│ PLAN DE CHARGE (macro, existing)                                 │
│  — "This shift's total Lectra work is 3480 min. Is capacity ok?" │
│  — No zone breakdown, no sequence-level decision.                │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│ SEQUENCE DISPATCHER (NEW — this plan)                            │
│  — "Sequence 2026-04-20-12 → Lectra Zone A."                     │
│  — Balances Lectra-A load% ≈ Lectra-B load% ≈ Lectra IP6 load%.  │
│  — Writes CuttingRequest.dispatchedZone.                         │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│ ADVANCED ORDONNANCEMENT + CONTINUOUS OPTIMIZER (micro, existing) │
│  — Within each zone, assigns serie to specific machine + time.   │
│  — Objective: box-duration KPI.                                  │
└──────────────────────────────────────────────────────────────────┘
```

Why this split matters:
- Zone dispatch is a **slow** decision (once per sequence, re-runnable
  at shift boundaries). A good greedy / LP balance is enough.
- Serie dispatch is a **fast** decision (continuously re-optimised). It
  shouldn't waste cycles reconsidering zone assignment.

---

## 2. What's already in place — build on, don't rebuild

| Capability | Where it lives | Verdict |
|---|---|---|
| `Zone` entity (`nom` PK, `code`, `description`, `rollLocations`, `orderInd`) | `domain/Zone.java` | Keep; add `category` (see §3.1) |
| `MachineType` entity (`name` PK, `description`) | `domain/MachineType.java` | Keep unchanged |
| `ProductionTable` with FK to `Zone` and `MachineType` | `domain/ProductionTable.java` L1–213 | Keep unchanged |
| `CuttingRequestSerie.machine` (String) = machine-type NAME | `domain/CuttingRequest/CuttingRequestSerieInfo.java` | Reused — **no** new `machineType` column on the serie |
| `TimingModel` (CMS DB) — `Validated_Cutting_time_Timing_Model` + `Real_Cutting_time_Timing_Model` keyed by placement | `com.lear.cms.domain.TimingModel` | **Sole source** for cutting time — see §2.6 |
| Machine status codes (M/A/P/O/R/MS/AD/ADM/MD/PN) | `EtatMachineHistorique`, `PLAN_DE_CHARGE.md` | Reused for "available machines" count |
| Capacity + efficiency per shift × groupe (Coupe, Laser) | `domain/CapaciteInstallee.java` | **Sole source** of efficiency — see §3.4 |
| Load-% formula with efficiency | `PlanDeChargeService.calculateShiftLoad` (already multiplies by `efficienceTarget` after PLAN_DE_CHARGE_IMPROVEMENT_PLAN P2) | Reused via `CuttingTimeCalculator` bean (§2.6) |
| Cutting-time priority: `Validated > Real > tempsDeCoupe`, LASER-DXF × `nbrCouche`, Gerber × 2 | `PLAN_DE_CHARGE.md` §Load Calculation | Reused verbatim — **must not diverge** |
| `ROLE_CHEF_DE_ZONE`, `ROLE_CHEF_EQUIPE`, `ROLE_PROCESS`, `ROLE_ADMIN` | already in security config | Reused — **no new role** |
| Sequence-level zone surfaced in `CuttingRequestData.zone` | existing projection | Gets promoted to a real persisted column (see §3.2) |

---

## 2.5 Data loading — a hard rule for a big DB

This is the non-negotiable rule applied to every endpoint, service and
scheduled job introduced by this plan (and echoed in
`PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md §2` and
`ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §4.5`). **A sequence
dispatcher that loads `@Entity` graphs on every rebalance will kill
the shift.**

### 2.5.1 Use the `data` package, never the `domain` entities

The project already has projection classes under
`com.lear.MGCMS.domain.CuttingRequest.data`:
- `CuttingRequestData`
- `CuttingRequestSerieData`
- `CuttingRequestSerieDataLight`
- `CuttingRequestBoxData`
- `CuttingRequestPartNumberData`
- `CuttingRequestSerieRouleauData`

Every query introduced here selects one of these — never
`CuttingRequest` / `CuttingRequestSerie` `@Entity`. If a field is
missing, add it to the projection, don't widen the entity fetch.

Pattern to follow (the existing repo at
`CuttingRequestSerieDataRepository:91–96` — note that
`estimatedCuttingTime` and `Validated_Cutting_time_Timing_Model` are
**not** columns on `CuttingRequestSerie` / `CuttingRequestSerieData`;
they live in the CMS database — see §2.6):

```java
@Query("SELECT new com.lear.MGCMS.domain.CuttingRequest.data." +
       "CuttingRequestSerieDataLight(s.serie, s.sequence, s.partNumberMaterial, " +
       " s.longueur, s.nbrCouche, s.placement, s.tempsDeCoupe, " +
       " s.machine, " +                                  // machine-type NAME
       " s.statusMatelassage, s.statusCoupe, s.zoneMatelassage, " +
       " s.tableMatelassage, s.zoneCoupe, s.tableCoupe, " +
       " s.dateDebutMatelassage, s.dateFinMatelassage, " +
       " s.dateDebutCoupe, s.dateFinCoupe) " +
       "FROM CuttingRequestSerieData s WHERE s.sequence IN :seqs")
List<CuttingRequestSerieDataLight> findSeriesBySequencesLight(
    @Param("seqs") Collection<String> seqs);
```

The resolved cutting time (Validated > Real > tempsDeCoupe) is
computed by joining this projection with `TimingModel` via the
`CuttingTimeCalculator` bean described in §2.6 — the join is kept out
of the main query so the placement set can be deduplicated before
hitting the CMS DB.

### 2.5.2 Load one thing at a time

Loader sequence for a **single dispatcher run** (order matters):

1. `ProductionTableRepository.findLightByZoneCategory(STRICT)` — returns
   `List<ProductionTableLight>` (id, nom, machineType.name, zone.nom,
   tableLength). **One query, all strict tables.**
2. `EtatMachineHistoriqueRepository.findLatestPerMachine()` — one
   window function query, latest status per machine.
3. `ShiftZoneConfirmationRepository.findActiveMachinesForShift(date, shift)`
   — one query, returns `(zone, machineName)` tuples.
4. `CuttingRequestSerieDataRepository.findRelevantSequences(now-8h, now)`
   — existing L80–86. One query, list of sequence ids.
5. `CuttingRequestSerieDataRepository.findSeriesBySequencesLight(seqIds)`
   — existing L91–96. One query for **all** series across the relevant
   sequences.

Five queries total, no N+1. Runs in well under 200 ms on the
production DB even during peak shift.

### 2.5.3 Never load what will not be shown

- The Process heatmap needs **zone × machineType aggregates**, not
  individual series. Use a dedicated projection
  `ZoneLoadCellDto(zone, machineType, plannedMin, capacityMin, loadPct,
  activeMachines, sequencesCount)`. The heatmap endpoint returns ~40
  rows max (8 zones × 5 machine types), not 400 series.
- The sequence list in the Process page is paginated
  (`Page<SequenceDispatchRowDto>`, 50 rows/page).
- The Chef de Zone page shows only sequences for **one** zone — a
  `zoneName` filter on the server.

### 2.5.4 Time windows by default

- Dispatcher **view horizon**: sequences whose latest serie is not
  finished within the last 7 days (the "unfinished backlog") — read-only
  historical visibility.
- Dispatcher **decision horizon**: each sequence's own `dueDate` /
  `dueShift` drives when it enters the dispatch pass; there is no single
  global cut-off.
- "Finished recently" window for Plan de Charge carryover logic:
  `now − 8h` (unchanged, matches PdC §4).
- Any query that would cross 30 days must go through a streaming
  endpoint (see `PLAN_DE_CHARGE_IMPROVEMENT_PLAN §2`).

---

## 2.6 Cutting time — where it actually comes from

**Correction to an earlier assumption.** The fields
`Validated_Cutting_time_Timing_Model`, `Real_Cutting_time_Timing_Model`,
and `Cutting_time_Timing_Model` do **not** exist on
`CuttingRequestSerie` or `CuttingRequestSerieData`. They live on the
CMS database's `TimingModel` entity
(`com.lear.cms.domain.TimingModel`), keyed by
`Placement_Timing_Model`.

A serie's **resolved cutting time** is therefore a two-step lookup:

```
resolveCuttingTime(serie) {
  tm = CMSDB.TimingModel where placementTimingModel = serie.placement
  t  = tm?.validatedCuttingtimeTimingModel
       ?? tm?.realCuttingtimeTimingModel
       ?? serie.tempsDeCoupe
  if (serie.machine == 'LASER-DXF') t *= serie.nbrCouche
  if (serie.machine == 'Gerber')    t *= 2
  return t
}
```

This algorithm is already the Plan de Charge rule (`md/PLAN_DE_CHARGE.md`
§Cutting Time Priority). To avoid every consumer re-implementing it, we
extract a **single** `CuttingTimeCalculator` bean
(contract **C1** in §8.2):

```java
@Component
public class CuttingTimeCalculator {
    @Autowired private TimingModelRepository timingModelRepo;  // CMS DB

    /** Per-serie resolved time with type multipliers. */
    public double resolveMinutes(CuttingRequestSerieData s) { ... }

    /** Batch variant: one CMS DB call for N placements. */
    public Map<String, Double> resolveMinutesBatch(
        Collection<CuttingRequestSerieData> series) { ... }
}
```

Every caller — Plan de Charge, the Sequence Dispatcher's load formula,
the Continuous Optimizer, the CMS-Prod kiosk next-serie banner — goes
through this bean so numbers cannot drift.

### 2.6.1 TimingModel lookup locality

- `TimingModelRepository` lives in the `com.lear.cms` package — it hits
  the **CMS** datasource, not the main MG-CMS datasource.
- The dispatcher must therefore batch placements: one call per
  dispatcher run, not per serie.
- `CuttingTimeCalculator.resolveMinutesBatch` implements this batch and
  caches the (placement → time) map for the dispatcher's in-memory
  scratch-space.

### 2.6.2 Missing TimingModel rows

If a placement has no matching `TimingModel`, the calculator falls back
to `tempsDeCoupe` silently. If `tempsDeCoupe` is also NULL, the serie
is added to `UnassignableSerie` with `reason = NO_TIMING` (see §3.7) —
Process sees it on the *Inassignables* drawer.

---

## 3. Data model changes (a handful of small tables)

### 3.1 `Zone.category` — the strict/shared flag
```
ALTER TABLE Zone
  ADD category VARCHAR(16) NOT NULL DEFAULT 'STRICT';   -- STRICT | SHARED
```
- **STRICT**: sequences can be *dispatched* here. A strict zone holds a
  mix of machine types (Lectra + Lectra IP6 + Gerber as configured).
  Examples for Tangier: *First Article* (3 Lectra + 1 Lectra IP6),
  *Serie* (its own Lectra + Lectra IP6 mix), *Prototype*, etc. — the
  exact set is owned by Process.
- **SHARED**: sequences are **never** dispatched here; only *overflow*
  series (whose machine type is missing from the strict zone) flow in.
  Examples: *LASER-DXF*, *LASER-LSR*.

Key invariant: a machine type present in any STRICT zone **must not**
also appear in a SHARED zone (otherwise a serie could route two ways).
The reverse is allowed and common — a SHARED zone carries the types
that strict zones don't. The admin UI enforces this on save (§6).

Consequences for the dispatcher:
- A strict zone does NOT have "only Lectra" or "only Lectra IP6"; it
  has whatever physical machines the plant laid out there.
- The load calculation keys by `(zone, machineType)` — *per machine
  type inside each zone* — so we can detect "this zone has a Lectra
  overload but its Lectra IP6 still sits idle" (the user's exact
  imbalance pain point).

### 3.2 `CuttingRequest.dispatchedZone` + acceptance columns
```
ALTER TABLE CuttingRequest
  ADD dispatchedZone VARCHAR(64) NULL,                   -- FK Zone.nom
  ADD dispatchedAt DATETIME2 NULL,
  ADD dispatchedBy BIGINT NULL,                          -- FK User.id
  ADD zoneAcceptanceStatus VARCHAR(16) NOT NULL DEFAULT 'NOT_DISPATCHED',
                                     -- NOT_DISPATCHED | PENDING | ACCEPTED | REJECTED
  ADD zoneAcceptedAt DATETIME2 NULL,
  ADD zoneAcceptedBy BIGINT NULL,                        -- FK User.id (the chef)
  ADD zoneRejectionReason NVARCHAR(512) NULL;
```
Today `CuttingRequestData.zone` is only a read-projection. Making it a
persisted column on `CuttingRequest` is essential: both the Continuous
Optimizer and the Chef de Zone screen read it constantly.

Nullable because not-yet-dispatched sequences exist (imported but not
yet shown to Process). Sequences with all series of SHARED-only machine
types stay `NULL` — they never need a strict-zone dispatch.

Acceptance columns add the **Chef-de-Zone confirmation gate** (§4.5).
Status transitions:

```
NOT_DISPATCHED ──dispatcher──►  PENDING
PENDING        ──chef accept──► ACCEPTED
PENDING        ──chef reject──► REJECTED ──auto-redispatch──► PENDING (on another zone)
ACCEPTED       ──admin only──► PENDING   (rare; requires unfreeze)
```

The optimiser only considers sequences with `zoneAcceptanceStatus = ACCEPTED`
(or `NULL dispatchedZone` for SHARED-only sequences). PENDING sequences
stay visible but are not scheduled into `MachineQueue`.

### 3.3 `UserZone` — chef-to-zone many-to-many + optional default

A `ROLE_CHEF_DE_ZONE` user may be **responsible for multiple zones**
(e.g. both First Article *and* Prototype). Conversely, one zone has
exactly one chef at a time. This is a **many-to-many** relation and
must not be collapsed onto a single `User.defaultZone` column.

```
CREATE TABLE UserZone (
    user_id       BIGINT NOT NULL,              -- FK User.id
    zone          VARCHAR(64) NOT NULL,         -- FK Zone.nom
    is_default    BIT NOT NULL DEFAULT 0,       -- the zone opened on login
    assigned_by   BIGINT NOT NULL,              -- FK User.id (chef équipe / process)
    assigned_at   DATETIME2 NOT NULL,
    revoked_at    DATETIME2 NULL,               -- soft-delete
    PRIMARY KEY (user_id, zone)
);
CREATE UNIQUE INDEX UX_UserZone_one_default_per_user
    ON UserZone(user_id) WHERE is_default = 1 AND revoked_at IS NULL;
```

Semantics:
- A `ROLE_CHEF_DE_ZONE` user's **access scope** on the Chef de Zone
  page = the set of `UserZone` rows with `revoked_at IS NULL`.
- Exactly one of those rows may carry `is_default = 1` (the landing
  zone when the chef opens the page).
- `ROLE_CHEF_EQUIPE` sees **all** zones (no `UserZone` needed — role
  alone grants visibility).
- `ROLE_PROCESS` / `ROLE_ADMIN` see all zones (same rule).

### 3.3.1 Zone-assignment management page

A new page under **Process** / **Chef d'Équipe** lets a
`ROLE_CHEF_EQUIPE` or `ROLE_PROCESS` user manage the relation:

- Left column: all zones (with category + machine-type counts).
- Right column: all users with `ROLE_CHEF_DE_ZONE`.
- Matrix checkbox: assign / revoke a (user, zone) pair.
- Per-row radio on each user to set their default zone (defaults to
  the first assigned zone).
- Audit: every change writes `(user_id, zone, assigned_by,
  assigned_at)` or `revoked_at`.

REST:

```
GET  /api/userZones                              -- list, paginated
POST /api/userZones          { user_id, zone }   -- assign
DELETE /api/userZones/{user_id}/{zone}           -- revoke (soft)
PUT  /api/userZones/{user_id}/default/{zone}     -- flip default
```

All three endpoints require `ROLE_CHEF_EQUIPE` or `ROLE_PROCESS` or
`ROLE_ADMIN`.

### 3.4 Efficiency target — reuse existing `CapaciteInstallee`

**Correction to an earlier draft.** The plan originally introduced a
new `ZoneEfficiency` table. It is redundant: the existing
`CapaciteInstallee` entity already owns the numbers we need:

```java
CapaciteInstallee {
    dateProduction     LocalDate    // per-day
    shiftNumber        Integer      // 1 | 2 | 3
    groupe             String       // "Coupe" | "Laser"
    capaciteInstallee  Integer      // installed machine count
    tempsTotalParMachine Double    // default 460
    efficienceTarget   Double       // default 90.0 %
}
```

Groupe mapping (already used in `PlanDeChargeService`):
- `Coupe` → machine types `Lectra`, `Lectra IP6`, `Gerber`
- `Laser` → machine types `LASER-DXF`, `LASER-LSR`

The dispatcher's load formula therefore reads:

```
η(type, date, shift) = CapaciteInstallee
                         .byKey(date, shift, groupeOf(type))
                         .efficienceTarget / 100.0
```

No new entity, no new CRUD page. If Process ever needs per-zone
overrides, a future `ZoneCapacityOverride(zone, date, shift, ...)`
side-table can apply on top without touching this plan's scope.

If `CapaciteInstallee` has no row for a given `(date, shift, groupe)`
combination, the dispatcher falls back to the default constants
(`460 × 0.90`) — same behaviour as Plan de Charge.

### 3.5 `ShiftZoneConfirmation` + `ShiftZoneConfirmationMachine` — the chef's declaration
```
CREATE TABLE ShiftZoneConfirmation (
    id             BIGINT IDENTITY PK,
    shift_date     DATE NOT NULL,
    shift_number   INT NOT NULL,                  -- 1, 2, 3
    zone           VARCHAR(64) NOT NULL,          -- FK Zone.nom
    confirmed_by   BIGINT NOT NULL,               -- FK User.id (the chef de zone)
    confirmed_at   DATETIME2 NOT NULL,
    reopened_at    DATETIME2 NULL,                -- if the chef edits mid-shift
    reopened_by    BIGINT NULL,
    notes          NVARCHAR(512) NULL,
    UNIQUE (shift_date, shift_number, zone)
);

CREATE TABLE ShiftZoneConfirmationMachine (
    confirmation_id   BIGINT NOT NULL,            -- FK ShiftZoneConfirmation.id
    machine           VARCHAR(64) NOT NULL,       -- FK ProductionTable.nom
    active            BIT NOT NULL DEFAULT 1,
    added_after_start BIT NOT NULL DEFAULT 0,     -- flagged for audit if added mid-shift
    remark            NVARCHAR(256) NULL,
    PRIMARY KEY (confirmation_id, machine)
);
```

Semantics:
- Before any chef confirmation for `(date, shift, zone)`, capacity falls
  back to `ProductionTable.zone = zone` filtered by **status `M` only**
  (a machine is active when `EtatMachineHistorique.status = 'M'` OR no
  `EtatMachineHistorique` row exists for that machine — *no row* is
  treated as `M` by default). This is **stricter** than Plan de Charge
  (which counts `{M, MS, MD, R}`) — the dispatcher only schedules onto
  machines that are actually running, not setting up / in maintenance
  hand-over / on standby. Plan de Charge's rule is unchanged.
- After confirmation, **the confirmation wins**: only machines with
  `active = 1` in the confirmation are counted as zone capacity,
  regardless of `EtatMachineHistorique` (the chef knows best — he sees
  the floor).
- The chef can re-open the confirmation to add a machine mid-shift
  (`added_after_start = 1` for audit). The add fires an application
  event: Plan de Charge recomputes, the Optimizer refreshes its snapshot
  (§8).

### 3.6 What we intentionally do NOT add
- **No `machineType` column on `CuttingRequestSerie`.** The existing
  `CuttingRequestSerie.machine` (String) already carries the machine-type
  NAME (e.g. `"Lectra"`, `"Lectra IP6"`, `"LASER-DXF"`). The dispatcher
  resolves `serie → ProductionTable` by joining on
  `ProductionTable.machineType.nom = serie.machine` — **never** by
  extending the serie entity.
- **No per-serie "compatible types" field.** One serie ↔ exactly one
  machine type. If CAD needs to keep the serie flexible across types,
  that choice is made upstream on `CuttingPlanMaterial.machineType` at
  plan-generation time, not here.
- **No new efficiency entity.** `CapaciteInstallee.efficienceTarget`
  already exists; reuse it (§3.4).
- **No `User.defaultZone` single column.** Chef-to-zone is many-to-many
  via `UserZone` (§3.3). A `ROLE_CHEF_DE_ZONE` user can be responsible
  for several zones; a `ROLE_CHEF_EQUIPE` sees all zones with no table
  entry.
- **No split of a sequence across strict zones.** A sequence dispatches
  to exactly one strict zone (plus shared-zone fan-out for missing
  types, §3.8). Keeps box traceability clean (see question #1).

### 3.7 Virtual per-serie table assignment — "best-fit table or flag it"

**User's ask:** *"each serie need to have assigned virtually in a table
which is the best suited for her. and if not we need to know then."*

Mechanism:

1. After the dispatcher writes `CuttingRequest.dispatchedZone`, a
   follow-up pass (`SerieTableAssigner` service) walks each serie in
   the sequence and picks the **virtual best table** using the current
   machine set (`ShiftZoneConfirmationMachine` active rows), the
   serie's machine type, table length, and existing `MachineQueue`
   load.
2. The virtual assignment is stored on **existing** columns —
   `CuttingRequestSerieData.tableCoupe` / `.zoneCoupe` — so no schema
   change. We add a small boolean `tableIsVirtual` (new column) to
   mark "this is a planning hint, not an operator confirmation".
3. If **no** suitable table exists (no active machine of the right
   type in the zone, or all candidates overloaded), write
   `tableCoupe = NULL` and raise a row in a new small table
   `UnassignableSerie`:
   ```
   CREATE TABLE UnassignableSerie (
       id          BIGINT IDENTITY PK,
       serie       VARCHAR(64) NOT NULL,
       sequence    VARCHAR(64) NOT NULL,
       reason      VARCHAR(64) NOT NULL,   -- NO_MACHINE_TYPE | ZONE_OVERLOAD | ROLL_MISSING | OTHER
       detail      NVARCHAR(512) NULL,
       detected_at DATETIME2 NOT NULL,
       resolved_at DATETIME2 NULL,
       UNIQUE (serie, sequence)
   );
   ```
4. The Process UI shows an **"Inassignables" counter** top-right of
   the dispatcher page. Clicking it opens a modal listing the series
   with their reason. Process can then either: (a) reopen
   ShiftZoneConfirmation to add a machine, (b) force-dispatch the
   sequence to another strict zone, (c) accept the miss (will ship
   next shift).

### 3.8 Complementary-zone fill is at the SERIE level, not the sequence level

Restating the rule with the precision the user asked for:

- A **sequence** dispatches to exactly **one** STRICT zone.
- Inside that sequence, **each serie** runs where its machine type
  lives:
  - If the serie's `machineType` is available in the sequence's
    `dispatchedZone` → the `SerieTableAssigner` picks the best table
    **inside that strict zone**.
  - If the serie's `machineType` is **not** available in the
    `dispatchedZone` but is available in a **SHARED** (complementary)
    zone → the serie routes to that SHARED zone's best-fit table.
  - If the machine type is in **neither** → `UnassignableSerie`
    with `reason = NO_MACHINE_TYPE`.

This is how a single dispatched sequence (say, to Lectra Zone A) ends
up with its Lectra series on Lectra Zone A's machines AND its LASER-DXF
series on the complementary LASER-DXF zone — without the dispatcher
ever "splitting" the sequence. The *sequence's ownership* stays with
the chef of Lectra Zone A (acceptance, box traceability, retard). The
*serie execution* fans out.

### 3.9 Best-fit scoring (what `SerieTableAssigner` actually computes)

For each candidate table `m` for serie `s`:

```
score(s, m) =   α · queueLoadMin(m)                      (lower = better)
              + β · (1 if s.longueur > m.tableLength - 0.5 else 0)   (length penalty)
              + γ · endStackPenalty(m, s)                (see below)
              − δ · typeSpecialisationBonus(m, s)        (LASER-DXF on LASER-DXF-only table)
```

- `queueLoadMin(m)` = sum of `estimatedCuttingTime` already in
  `MachineQueue` for `m` up to shift end.
- `endStackPenalty(m, s)` = penalises assigning one more serie to a
  machine whose queue is already past 90 % of shift capacity — this
  is the user's *"avoid stacking too much at the end of zone tables"*
  concern, made explicit in code. Penalty grows quadratically above
  90 %.
- `typeSpecialisationBonus` encourages using a specialised machine
  when one is available (keeps generic machines open for generic
  work).
- Weights `α, β, γ, δ` exposed in `application.yml` so Process can
  tune without a redeploy.

Tie-break: earliest available start time.

The assigner is called:
- by the dispatcher right after each sequence placement,
- by the Continuous Optimizer on every snapshot refresh (same bean —
  contract **C3** extended: `SerieZoneResolver` becomes the facade
  over `SerieTableAssigner`),
- on demand from the Process UI via
  `POST /api/sequenceDispatch/recomputeAssignments?date=&shift=`.

---

## 4. Load-% formula — the one calculation Process will stare at

The formula is keyed by **(machine type, zone)**. This is the level at
which the user's pain point lives: *"zones end up with too many Lectra
series and not enough Lectra IP6, or the reverse"*. Aggregating only
at zone-level would hide that mismatch.

$$
\text{zoneLoad\%}(t, z) = 100 \times \frac{\sum_{s \in S(t,z)} \text{effCutTime}(s)}{N_{\text{active}}(t, z) \times \text{shiftMinutes} \times \eta(t, \text{date}, \text{shift})}
$$

Where:
- `S(t, z)` = series in the dispatch horizon whose machine type
  (`serie.machine`, a String == `MachineType.nom`) is `t` and whose
  **resolved zone** is `z`. Resolution rule:
    1. If sequence `dispatchedZone = z_strict` AND `t` is a type whose
       machines exist in `z_strict` → `z = z_strict`.
    2. Else if `t` is only available in a SHARED zone `z_shared` → `z = z_shared`.
    3. Else → the sequence is infeasible for `z_strict` → flagged on the
       dispatcher UI + a row written to `UnassignableSerie`
       (reason = `NO_MACHINE_TYPE`).
- `effCutTime(s)` = `CuttingTimeCalculator.resolveMinutes(s)` — the
  single shared bean defined in §2.6. **Same numbers as Plan de Charge**
  by construction.
- `N_active(t, z)` = count of `ProductionTable` rows with
  `machineType.nom = t`, `zone = z`, and either (a) listed active in
  `ShiftZoneConfirmationMachine` for the target shift, or (b) when no
  confirmation exists yet, the machine's latest `EtatMachineHistorique`
  status is `'M'` **or** there is no `EtatMachineHistorique` row for
  it (no-status defaults to running). This is the dispatcher's M-only
  rule — Plan de Charge keeps its broader `{M, MS, MD, R}` rule unchanged.
- `shiftMinutes` = `CapaciteInstallee.tempsTotalParMachine` (default 460).
- `η(t, date, shift)` =
  `CapaciteInstallee.byKey(date, shift, groupeOf(t)).efficienceTarget / 100`
  (default 0.90). `groupeOf(t)` = `Coupe` for Lectra/Lectra IP6/Gerber,
  `Laser` for LASER-DXF/LSR.

### 4.1 The balance objective

The dispatcher **does not** aim for zero imbalance — it aims for
**calibrated load**. The concrete objective:

```
imbalance = max{ zoneLoad%(t, z) : N_active(t, z) > 0,  category(z) = STRICT }
          - min{ zoneLoad%(t, z) : N_active(t, z) > 0,  category(z) = STRICT }
```

SHARED zones are out of the balance set — they are overflow
receivers, not balancing targets.

Because the key includes `t`, minimising this `imbalance` pushes the
greedy to avoid the failure modes:
- "Lectra in Zone A = 120 %, Lectra IP6 in Zone A = 35 %" (single-zone
  type imbalance) → solved by routing new Lectra-heavy sequences
  elsewhere, not by squeezing another Lectra sequence into Zone A.
- "Lectra in Zone A = 95 %, Lectra in Zone B = 40 %" (inter-zone
  imbalance) → solved by the classic greedy.

Tie-break priority:
1. Earlier `dueShift` first (urgency).
2. Larger sequence first (big hammers first — reduces fragmentation).
3. Zone already carrying less of this type (favours under-loaded
   machine-types inside a zone).

### 4.2 When the balance is re-evaluated

- **At shift boundary** (pre-dispatch job in §4.5): full re-balance over
  non-started sequences.
- **Mid-shift** (continuous refresh): the optimiser re-reads
  `dispatchedZone` on every snapshot, but only re-runs the dispatcher's
  greedy if (a) Process clicks *Réquilibrer*, (b) a chef confirms their
  composition (load denominator changed), or (c) a machine is added /
  removed mid-shift.
- **Pinned sequences are immune**: see §5.4 — once the chef pins or
  accepts a sequence on their zone, the greedy cannot move it.

---

## 4.5 Shift lifecycle & confirmation gates

The dispatcher is **not a single instant**. It runs along a predictable
shift timeline, with two explicit confirmation gates from the
`ROLE_CHEF_DE_ZONE`. These gates are the single most important addition
for avoiding the "plan-looks-good-on-paper-but-doesn't-match-the-floor"
problem.

### Timeline

| Time | Actor | Action |
|---|---|---|
| **T−60 min** | Process (or auto-run at shift boundary) | **Pre-dispatch.** Dispatcher fills `dispatchedZone` and sets `zoneAcceptanceStatus = PENDING` for all non-started sequences in the horizon. Capacity uses last-known machine set (previous shift's `ShiftZoneConfirmation`, or `ProductionTable.zone` fallback). |
| **T−30 min** | Chef de Zone | **Step 1 — Confirmer composition de zone.** Sees the list of machines assigned to his zone; toggles each ACTIVE / INACTIVE for this shift; optional remark per machine. One click writes `ShiftZoneConfirmation` + child rows. Zone capacity is recomputed server-side and broadcast via WebSocket to Process. |
| **T−20 min** | Chef de Zone | **Step 2 — Accepter / Rejeter les séquences.** Sees pending sequences in his zone with their series count, machine types, estimated load impact. For each: `[Accepter]` → `zoneAcceptanceStatus = ACCEPTED`, or `[Rejeter avec raison]` → `REJECTED` + reason, which triggers re-dispatch to an alternative strict zone (§5.4). |
| **T0** | — | **Shift starts.** Optimizer's scope is exactly: confirmed machines + ACCEPTED sequences. Optimizer warm-starts within that scope and begins publishing `MachineQueue`. |
| **T0…Tend** | Chef de Zone | **Mid-shift edits** are allowed. Adding a machine → `added_after_start = 1`, optimiser snapshot refreshes, no operator disruption. Removing a machine currently running a serie is blocked until the serie completes (soft lock — can be forced with `ROLE_ADMIN` override + reason). |
| **T0…Tend** | Process | **Mid-shift re-dispatch** of non-started sequences is allowed; each move resets `zoneAcceptanceStatus = PENDING` on the affected sequence and pings the receiving chef for re-acceptance. |
| **Tend** | — | **Shift ends.** Plan de Charge reads actual execution (existing `ShiftLoadCalculation`). `ShiftZoneConfirmation` rows are preserved for retrospective analysis; series not finished carry over **with their `zoneAcceptanceStatus` intact** — the next shift's chef doesn't re-confirm unless the zone composition actually changed. |

### Fallbacks when a chef doesn't confirm

The floor must never be blocked by a missing click.

- **No `ShiftZoneConfirmation` by T−10 min** → Process dashboard shows a
  red "Zone X: non confirmée" banner. Dispatcher uses
  `ProductionTable.zone ∩ (EtatMachineHistorique.status = 'M' OR no row)`
  as the effective machine set (M-only rule).
- **No sequence acceptance by T0** → sequences with
  `zoneAcceptanceStatus = PENDING` are **auto-accepted** at T0 (written
  with `zoneAcceptedBy = SYSTEM_AUTO`), flagged on the audit page. This
  prevents deadlock without hiding the miss.
- **Chef logs in mid-shift and disagrees** with auto-accepted sequences
  → may still reject; since series may already be in progress, rejection
  only moves **not-yet-started series** out of the zone (the running
  ones finish where they are). This is the only place the dispatcher
  touches an already-started sequence — audited with `trigger = MID_SHIFT_REJECT`.

### Why this gate is worth the overhead

- The chef actually *sees* the list of machines he'll run before the
  shift starts — catches "I thought Lectra-04 was out for PM" errors.
- Sequence acceptance creates ownership: a rejected sequence carries a
  *reason* usable by Process when tuning the dispatcher's weights.
- Auto-accept fallback means the system never deadlocks if someone is
  late to login, but the miss is visible.

---

## 5. Dispatch algorithm (greedy + optional refinement)

### 5.1 Input
- All non-dispatched `CuttingRequest` with `dueDate` in the horizon
  (default: today + next 7 days, configurable).
- Current `zoneLoad%` map across all STRICT zones, keyed by
  `(machineType, zone)` per §4.
- `CapaciteInstallee` rows for each `(date, shift, groupe)` in the
  horizon — provides `efficienceTarget`, `tempsTotalParMachine`, and
  `capaciteInstallee` (§3.4). No `ZoneEfficiency` entity is used.
- Active machine set per zone from `ShiftZoneConfirmationMachine` (or
  `EtatMachineHistorique` fallback per §3.5) — resolved through
  `ActiveMachineResolver` (contract **C2**).
- Resolved cutting time per serie from `CuttingTimeCalculator` (contract
  **C1**), which batches the CMS-DB `TimingModel` lookup (§2.6).

### 5.2 Greedy pass
```
Sort sequences by:
  1. dueDate ASC, dueShift ASC     (urgency first)
  2. total strict-machine-time DESC (biggest hammers first)

For each sequence seq:
    candidates = { z ∈ STRICT zones that contain at least one machineType
                   required by seq.series that must run in a STRICT zone }
    if candidates.isEmpty():
        mark seq as "SHARED-only" → dispatchedZone stays NULL
        continue
    pick z* in candidates minimising:
        max_after_assign( zoneLoad%(t, z) across all t, z )
    seq.dispatchedZone = z*
    update zoneLoad% in memory
```
Purely deterministic. Runs in O(|sequences| × |strict zones|). With
400 series / shift and typically 30-ish sequences in the 7-day horizon,
this is sub-millisecond.

### 5.3 Optional polish pass (pairwise swap)
After greedy, do 100 random-pair swaps; keep the swap if `imbalance`
decreases. Converges quickly, gives noticeably flatter load curves on
tight shifts. This is bounded-cost — Process won't feel it.

### 5.4 Re-dispatch triggers and pinning

Triggers for running the greedy:
- New sequences imported → dispatch just the new ones (incremental).
- User clicks "Réquilibrer" → full re-dispatch of all
  **not-pinned and not-yet-started** sequences in the horizon.
- Zone `category` change or machine reassigned to another zone → full
  re-dispatch of not-pinned, not-started sequences.
- Shift boundary → auto-run (default ON, toggleable per Process).

**Frozen** (greedy must never touch):
- Any sequence with a serie in progress
  (`statusMatelassage ≠ Waiting` OR `statusCoupe ≠ Waiting`).
- Any sequence already **accepted** by a chef
  (`zoneAcceptanceStatus = ACCEPTED`) — the chef owns it.
- Any sequence **pinned** by a chef (see below).

### 5.4.1 Chef-pin — pulling a sequence into a zone

Per the user's answer to question #5, a chef must be able to **pull a
sequence that wasn't assigned to his zone**. This is a one-click action
on the Chef de Zone page that sets:

```
CuttingRequest.dispatchedZone      = myZone
CuttingRequest.zoneAcceptanceStatus = ACCEPTED
CuttingRequest.pinnedByChef         = 1        (new column — see below)
CuttingRequest.dispatchedAt         = now
CuttingRequest.dispatchedBy         = currentUser.id
```

Schema delta (additive):

```
ALTER TABLE CuttingRequest
  ADD pinnedByChef BIT NOT NULL DEFAULT 0,
  ADD pinnedAt DATETIME2 NULL,
  ADD pinnedBy BIGINT NULL;            -- FK User.id
```

Semantics:
- `pinnedByChef = 1` makes the sequence **immune to greedy
  re-dispatch** (`§5.4` frozen set).
- Only `ROLE_ADMIN` or `ROLE_PROCESS` may unpin (sets `pinnedByChef = 0`,
  logs a `DispatchAudit` entry with `trigger = UNPIN`).
- A chef can pin sequences only in zones they belong to in `UserZone`
  (§3.3). Validated server-side.
- Pinning is recorded in `DispatchAudit` with `trigger = CHEF_PIN` +
  the reason the chef typed (optional).

UI affordance (Chef de Zone Supervision mode, §7.3):
- The "Pending / rejected sequences" panel shows a globe-wide view
  (all zones' pending sequences) with a `[Pour ma zone]` button next to
  each. Clicking it asks for an optional reason and writes the delta
  above.

---

## 6. Process section — the Sequence Dispatcher page

### 6.1 Route & role
- Path: `/ordonnancement/sequenceDispatcher`
- Menu: Process > Ordonnancement Séquences
- Access: `ROLE_PROCESS` or `ROLE_ADMIN` (read-write),
  `ROLE_CHEF_DE_ZONE` (read-only — sees the whole plant view).

### 6.2 Layout (single page, 3 panels)

**Top filter bar**
- Date range (default: today → +7 days)
- Shift multi-select
- Machine type multi-select (for the load chart)
- `[Réquilibrer]` button (Process/Admin only) — triggers §5.2 + §5.3.

**Panel 1 — Zone capacity heatmap** (top third)
Rows = zones (STRICT first, SHARED below in a dimmed block).
Columns = each `(shift, machineType)` in the horizon.
Cell value = `zoneLoad%` coloured:
- < 80 % → green
- 80–100 % → amber
- > 100 % → red (overload — dispatcher must move sequences elsewhere)

Hover → tooltip with: total planned minutes, active machines, efficiency
applied, number of sequences, ETA of last serie.

**Panel 2 — Sequence list** (middle, ~50 % of the page)
Table with columns:
| Séquence | Dû | Part Numbers (count) | Séries (n/total) | Machine types | Zone actuelle | Zone recommandée | Action |
|---|---|---|---|---|---|---|---|
| 2026-04-20-12 | 2026-04-21 / S1 | 3 | 0 / 18 | Lectra, LASER-DXF | — | Serie *(STRICT — Lectra in-zone, LASER-DXF via SHARED)* | [Accepter] [Forcer…] |
- `Zone recommandée` = output of greedy dispatcher; if user already
  overrode, shows the override.
- `[Accepter]` writes `dispatchedZone`.
- `[Forcer…]` opens a modal with:
  - Dropdown of STRICT zones that have at least one compatible machine type
  - Mandatory `overrideReason` text field
  - Shows the impact: `+X % on target zone`, `−Y % on alternative`.
- Inline filters, sort, CSV export.

**Panel 3 — Audit trail** (collapsible bottom)
Latest 20 dispatch events with user, timestamp, reason. Feeds from a
new `DispatchAudit` table:
```
CREATE TABLE DispatchAudit (
    id                 BIGINT IDENTITY PK,
    sequence           VARCHAR(64) NOT NULL,
    from_zone          VARCHAR(64) NULL,
    to_zone            VARCHAR(64) NOT NULL,
    reason             NVARCHAR(512) NULL,
    trigger            VARCHAR(32) NOT NULL,    -- AUTO | MANUAL | REBALANCE
    user_id            BIGINT NOT NULL,
    created_at         DATETIME2 NOT NULL
);
```

### 6.3 Admin sub-page — zone classification
`/admin/zones` (extend existing Zone CRUD if present):
- Column `category` (STRICT / SHARED) with inline editor.
- Column `machine types present` (read-only; counts of `ProductionTable`
  rows per type in the zone).
- Column `current chef(s)` (read-only; pulled from `UserZone`).
- Column `capacity + efficiency` (read-only; link to the existing
  `CapaciteInstallee` edit page — **no new efficiency CRUD here**).
- Validation on save: warn if a machine type is present in both a
  STRICT and a SHARED zone (breaks the invariant in §3.1).

### 6.4 Chef-to-zone assignment page

`/process/zoneOwnership` (or under `/equipe/zoneOwnership` depending on
nav) — managed by `ROLE_CHEF_EQUIPE`, `ROLE_PROCESS`, or `ROLE_ADMIN`.

Purpose: enforce the mapping described in §3.3. Every zone has at least
one chef responsible; every `ROLE_CHEF_DE_ZONE` user is landed on their
default zone.

UI:

| Zone | Catégorie | Types de machines | Chef(s) actuel(s) | Actions |
|---|---|---|---|---|
| First Article | STRICT | 3 Lectra, 1 Lectra IP6 | A. Amine (défaut), K. Yassine | [Ajouter chef] [Retirer…] |
| Serie | STRICT | 4 Lectra, 2 Lectra IP6 | M. Hassan (défaut) | [Ajouter chef] [Retirer…] |
| LASER-DXF | SHARED | 2 LASER-DXF, 1 LASER-LSR | (partagé — pas de chef obligatoire) | [Ajouter chef optionnel] |

Clicking `[Ajouter chef]` opens a picker:
- Filter: users with `ROLE_CHEF_DE_ZONE`.
- Checkbox: **set as default zone** for this user (unsets any previous
  default).

A user appears in multiple rows if assigned to multiple zones — that's
expected. Revoke is soft (`revoked_at = now`), preserving audit.

Security:
- `POST /api/userZones` requires one of `{ROLE_CHEF_EQUIPE,
  ROLE_PROCESS, ROLE_ADMIN}` on the caller.
- The backend rejects an assignment when the user doesn't carry
  `ROLE_CHEF_DE_ZONE` (no "accidental" assignments).

Audit: each assign/revoke/default-flip writes to a small
`UserZoneAudit` table (or a common `SystemAudit` if one exists).

---

## 7. Production section — Chef de Zone page

### 7.1 Route & role
- Path: `/production/chefDeZone`
- Menu: Production > Chef de Zone — visible for `ROLE_CHEF_DE_ZONE`,
  `ROLE_CHEF_EQUIPE`, `ROLE_PROCESS`, `ROLE_ADMIN`.
- Default zone filter:
  - `ROLE_CHEF_DE_ZONE` → the zone in `UserZone` with `is_default = 1`.
    A dropdown at the top lets them switch to any of their other
    assigned zones (§3.3).
  - `ROLE_CHEF_EQUIPE` → "all zones" toggle (sees the full plant).
  - `ROLE_PROCESS` / `ROLE_ADMIN` → same as Chef d'Équipe (all zones).
- Server-side guard: when `ROLE_CHEF_DE_ZONE` requests a zone not in
  their `UserZone` rows, the API returns `403`.

### 7.2 Shift-start confirmation flow (the gate from §4.5)

At shift entry, the page opens in **Confirmation mode** and cannot be
dismissed until both steps are completed (or the chef explicitly clicks
"Reporter — j'ai besoin de 10 min", which lets Process see that this
zone is lagging).

**Step 1 — Confirmer composition de zone**
- Compact table of all machines where `ProductionTable.zone = my zone`:

| ☑ Actif | Machine | Type | État (EtatMachineHistorique) | Remarque |
|---|---|---|---|---|
| ☑ | LECTRA-01 | Lectra | M (Marche) | — |
| ☐ | LECTRA-02 | Lectra | P (PM aujourd'hui) | PM 06h–10h |
| ☑ | LECTRA-03 | Lectra | M (Marche) | opérateur nouveau, superviser |
| ☑ | LECTRA-IP6-01 | Lectra IP6 | M | — |

- Below the table: live load % per machine type within the zone, updated
  as the chef toggles boxes. Example:
  `Lectra: 78 % (3 machines actives)  |  Lectra IP6: 92 % (1 machine)`
- `[Confirmer composition]` → writes `ShiftZoneConfirmation` +
  `ShiftZoneConfirmationMachine` rows and advances to Step 2.

**Step 2 — Accepter / Rejeter les séquences**
- Table of sequences with `dispatchedZone = my zone` AND
  `zoneAcceptanceStatus = PENDING`:

| Séquence | Dû | Séries (total min) | Types de machines | Charge ajoutée (Lectra / IP6 / ...) | Action |
|---|---|---|---|---|---|
| 2026-04-20-12 | 2026-04-21 S1 | 18 (612 min) | Lectra, LASER-DXF | +12 % Lectra, (+3 % LASER shared) | [Accepter] [Rejeter] |
| 2026-04-20-13 | 2026-04-21 S1 | 6 (168 min) | Lectra IP6 | +25 % Lectra IP6 | [Accepter] [Rejeter] |

- Each accept/reject is a one-click + confirm (reject asks for reason
  from a fixed list: *capacité insuffisante* / *matière absente* /
  *maintenance imprévue* / *autre + texte libre*).
- Bulk actions: `[Tout accepter]`, `[Rejeter tout ce qui dépasse 100 %]`.

### 7.3 Mid-shift operations (after confirmation closes)

After step 2 the page switches to **Supervision mode** — the familiar
dashboard. It adds one persistent action at the top:
`[Modifier composition de zone]` — opens the machine-toggle table
again, lets the chef add or deactivate a machine on the fly. This is
the "he could be adding some machines to his zone at the start of the
shift" behaviour, generalised to the whole shift.

### 7.4 What the Chef sees (Supervision mode)
A single scrollable dashboard for one zone:

1. **Header strip** — zone name, category, current shift, load % per
   machine type, status dot aggregated from all machines of the zone,
   `Composition confirmée par X à HH:mm` link (opens the confirmation
   read-only, with `[Modifier]` if the chef is still on shift).
2. **Machine strip** — for each machine the chef marked active: name,
   status (reading from `EtatMachineHistorique`), current serie in
   progress, next 3 series (from `MachineQueue` — same source Advanced
   Ordonnancement uses). Machines marked INACTIVE in the confirmation
   are greyed out at the bottom of the strip.
3. **Accepted sequences panel** — list of `CuttingRequest` with
   `dispatchedZone = this zone AND zoneAcceptanceStatus = ACCEPTED`,
   progression bar, `dueShift`, estimated finish time, `[Détails]` → drill-down.
4. **Pending / rejected sequences panel** — small, collapsible; shows
   sequences still waiting for chef input mid-shift (re-dispatches from
   other zones, new imports).
5. **Alerts pane** — actionable:
   - Series with `dateFinCoupe IS NULL` older than 2 × median for this
     zone's machine type → "Fin coupe probablement oubliée".
   - Sequences predicted to miss `dueShift` (from Advanced Ordonnancement
     timeline) → "Risque de retard".
   - Machines in status P / O / PN → "Arrêt en cours".
6. **Actions** (limited — chef is read-mostly):
   - `[Signaler blocage série]` — creates a blocker ticket (reuse
     `QualityNotice` with `type = BLOCKER_ZONE`? or a new tiny entity).
     To be decided with Process owner (see question #6).
   - `[Demander redispatch]` — sends a notification to Process to
     re-balance. Does NOT change `dispatchedZone` directly — separation
     of duty.
   - `[Modifier état machine]` — only if ChefDeZone has
     `ROLE_MAINTENANCE` too; reuses `EtatMachineHistorique` CRUD.

### 7.5 Live updates
Subscribe to `/topic/zone/{zoneName}` via existing SockJS+STOMP setup.
Events: dispatched-sequence added, sequence accepted/rejected elsewhere,
serie status changed, machine status changed, confirmation re-opened.
No page reload.

---

## 8. End-to-end flow — from Plan de Charge to Coupe

This section is the **smoothness check** the user asked for: it walks
through the full life of a sequence across all three scheduling layers
and proves each handoff has a clean, single-direction contract. If any
arrow below breaks, the whole chain breaks — so these are the contracts
to unit-test.

### 8.1 The three layers and their boundaries

```
┌─────────────────────────────────────────────────────────────────────────┐
│ LAYER 1 — PLAN DE CHARGE     (existing, macro, shift-total view)        │
│    role: ROLE_PROCESS                                                   │
│    reads:  CuttingRequestSerie  (light projection)                      │
│            CMS-DB TimingModel  (Validated/Real cutting time per         │
│                                 placement — see §2.6, contract C1)      │
│            CapaciteInstallee  (capacity + efficienceTarget per          │
│                                date × shift × groupe — contract C5)     │
│            EtatMachineHistorique (machine status history)               │
│            ShiftZoneConfirmation  [NEW — feeds effective capacity]      │
│    writes: ShiftLoadCalculation (per-shift totals: Chg/Ret/SR/Cap/Rec)  │
│    outputs to Layer 2:  "this shift needs 3480 min of Lectra work"     │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ LAYER 2 — SEQUENCE DISPATCHER   (NEW, this plan)                        │
│    role: ROLE_PROCESS + ROLE_CHEF_DE_ZONE gate                          │
│    reads:  CuttingRequest (sequences), CuttingRequestSerie              │
│            Zone.category, ProductionTable.zone+machineType              │
│            ShiftZoneConfirmation  [confirmed machine set, §3.5]         │
│            CapaciteInstallee  (efficienceTarget +                       │
│                                tempsTotalParMachine — §3.4 / C5)        │
│            CMS-DB TimingModel via CuttingTimeCalculator  (§2.6 / C1)    │
│            UserZone  (chef-to-zone scope — §3.3 / C6)                   │
│    writes: CuttingRequest.dispatchedZone                                │
│            CuttingRequest.zoneAcceptanceStatus                          │
│            CuttingRequest.pinnedByChef (chef-pull — §5.4.1)             │
│            DispatchAudit                                                │
│    outputs to Layer 3: "sequence 2026-04-20-12 → Serie zone,            │
│                         accepted by chef Amine at 05:52"                │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ LAYER 3 — ADVANCED ORDONNANCEMENT + CONTINUOUS OPTIMIZER                │
│            (existing + plans/ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN)  │
│    role: ROLE_PROCESS (start/stop the engine)                           │
│    reads:  CuttingRequest (only zoneAcceptanceStatus=ACCEPTED or NULL)  │
│            ShiftZoneConfirmationMachine (active machines per zone)      │
│            EtatMachineHistorique, ScanRouleau, WorkCalendar             │
│    writes: MachineQueue (serie ↔ machine ↔ planned time)                │
│            OptimizerIndicatorSample (box-duration KPI trail)            │
│    outputs to CMS-Prod:  "next serie on LECTRA-03 is #987 at 14:22"    │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ LAYER 4 — CMS-Prod execution    (existing: Form.js / FormCoupeNew /    │
│            FormMix)                                                     │
│    writes: CuttingRequestSerie.dateDebutMatelassage / FinMatelassage /  │
│            DebutCoupe / FinCoupe                                        │
│    feedback to Layer 1 via actual durations → next shift's Plan de      │
│    Charge reads the real execution.                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Single-source-of-truth contracts

These are the lines that must **never diverge** between layers (enforce
by shared code + unit tests):

| # | Contract | Shared code | Enforced by |
|---|---|---|---|
| C1 | Cutting time of a serie = `Validated_Cutting_time_Timing_Model > Real_Cutting_time_Timing_Model > CuttingRequestSerie.tempsDeCoupe`, LASER-DXF × nbrCouche, Gerber × 2. Validated/Real live in the **CMS DB** `TimingModel`, joined by placement (§2.6). | `CuttingTimeCalculator` bean (new — §2.6), repository `TimingModelRepository` (existing CMS-DB) | unit test: compute same total from PlanDeCharge, Dispatcher, Optimizer on fixture |
| C2 | Active-machine set for (date, shift, zone) | `ActiveMachineResolver` bean — wraps `ShiftZoneConfirmation` lookup with `EtatMachineHistorique` fallback | unit test: both Dispatcher and Optimizer pull the same set |
| C3 | Zone resolution for a serie: `ProductionTable.machineType.nom = serie.machine`; serie routed to strict zone if that zone has ≥ 1 machine of its type, else routed to the SHARED zone holding that type | `SerieZoneResolver` bean — implements §4 step-by-step rule | unit test: strict sequence with LASER serie routes to shared zone; same serie under shared-only sequence stays shared |
| C4 | Scheduling scope | `SchedulableSerieFilter` bean — only series of `zoneAcceptanceStatus ∈ {ACCEPTED, NULL-for-shared}` are picked by the Optimizer | integration test: rejecting a sequence removes its series from `MachineQueue` within 5 s |
| C5 | Efficiency source = `CapaciteInstallee.efficienceTarget` keyed by `(date, shift, groupe)` — **no other source allowed** | `CapaciteInstalleeService.getEffective(date, shift, groupe)` | unit test: changing a `CapaciteInstallee` row changes the dispatcher's `zoneLoad%` denominator on the next recompute |
| C6 | Chef-to-zone mapping = `UserZone` rows with `revoked_at IS NULL` — **no other source allowed** | `UserZoneService.zonesForUser(userId)` | integration test: a `ROLE_CHEF_DE_ZONE` with no UserZone rows sees 0 zones and cannot act |

### 8.3 Event bus — the glue

Already-proposed in `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md §2.2`;
this plan adds two new event types. All events use Spring
`ApplicationEventPublisher`; the optimiser and the Chef de Zone UI
(via WebSocket relay) listen to them.

| Event | Fired by | Consumed by |
|---|---|---|
| `ShiftZoneConfirmedEvent(zone, date, shift)` | Chef clicks "Confirmer composition" | Optimizer snapshot refresh; Plan de Charge capacity recompute; Dispatcher's heatmap refresh |
| `SequenceDispatchedEvent(sequence, zone)` | Dispatcher greedy or manual override | Chef de Zone page (new pending row); Optimizer — only triggers a refresh once the sequence is accepted, not dispatched |
| `SequenceAcceptedEvent(sequence, zone)` | Chef clicks Accepter | Optimizer snapshot refresh (adds the series to its search space) |
| `SequenceRejectedEvent(sequence, zone, reason)` | Chef clicks Rejeter | Dispatcher picks the next-best zone; Process receives a notification |
| `ZoneMachineToggledEvent(zone, machine, active, whenDuringShift)` | Chef mid-shift edit | Optimizer snapshot refresh; capacity heatmap refresh; audit row |

### 8.4 Feasibility / penalty rules — exact wording for the Optimizer

From `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md §2.5`, replace "Zone
binding SOFT by default" with the precise rules below:

- **Hard (never violate)**: a serie `s` can only be placed on machine
  `m` if (a) `m ∈ ShiftZoneConfirmationMachine.active` for (date, shift),
  (b) `m.machineType` matches what `s` needs, and
  (c) one of:
    - `seq.zoneAcceptanceStatus = ACCEPTED` AND `m.zone = seq.dispatchedZone`, OR
    - `seq.zoneAcceptanceStatus = ACCEPTED` AND `m.zone.category = SHARED`
      AND `s.machineType` is **not** available in `seq.dispatchedZone`, OR
    - `seq.dispatchedZone IS NULL` AND `m.zone.category = SHARED`
      (purely SHARED sequence).
- **Soft penalty** (cost `w_zone`): applies when `m.zone.category = SHARED`
  AND `s.machineType` **is** available in `seq.dispatchedZone` — i.e. the
  optimiser is being tempted to offload a serie to the SHARED zone when
  the chef's zone can handle it. Discouraged but not forbidden.
- **Soft penalty** (cost `w_latecapacity`, new): applies when
  `ZoneMachineToggledEvent.whenDuringShift > T0 + 2h` — a machine added
  late in the shift is less useful because operators need warm-up.

This is the exact set of rules the optimiser must obey. Any deviation
is a bug.

### 8.5 Worked example (one sequence, end to end)

Sequence **2026-04-20-12** — 18 series: 14 Lectra (220 min total),
4 LASER-DXF (96 min total). Due 2026-04-21 shift 1.

1. **T−60**: Dispatcher sees `zoneLoad%(Lectra, Zone A) = 62 %`,
   `zoneLoad%(Lectra, Zone B) = 71 %`. Picks Zone A (smaller). Writes
   `dispatchedZone = Zone A`, `zoneAcceptanceStatus = PENDING`. Chef
   de Zone A is notified via WebSocket.
2. **T−30**: Chef A opens the page. Confirms Lectra-01, -03, -04 active
   (Lectra-02 in PM). `ShiftZoneConfirmation` written.
3. **T−25**: Chef A sees sequence 12 pending. Clicks Accepter. Status
   goes ACCEPTED. `SequenceAcceptedEvent` fired.
4. **T−25 + 1 s**: Optimizer refreshes snapshot. Sequence 12's Lectra
   series are now in its search space. Its LASER-DXF series routed to
   the SHARED LASER-DXF zone by the rules in §8.4. Machines assigned:
   Lectra-01 (8 series), Lectra-03 (4 series), Lectra-04 (2 series),
   LASER-DXF-01 (4 series). `MachineQueue` updated.
5. **T0**: Shift starts. `Form.js` on Lectra-01 reads `MachineQueue`,
   shows serie #1. Operator starts matelassage.
6. **T+02h15**: An operator on Lectra-03 forgets to click Fin Coupe
   (`dateFinCoupe = NULL`). `CoupeMachineHistory.reviewSerieWaiting()`
   kicks in (see `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md §5`),
   back-fills the date from the Lectra log, writes `DateInferenceAudit`.
   The Optimizer's KPI doesn't degrade.
7. **T+05h30**: Chef A adds Lectra-05 mid-shift (returned from PM).
   `ZoneMachineToggledEvent` fires. Optimizer refreshes. Remaining
   series are redistributed across 4 machines instead of 3; box-duration
   KPI visibly drops on the live chart.
8. **Tend**: Sequence 12 finishes at 13:18, 27 min before `dueShift`
   end. `Plan de Charge` next shift sees Zone A's actual retard = 0.

Each arrow in this example crosses one of the C1–C4 contracts. If any
contract breaks, the example breaks — which is why they are the
unit-test targets.

---

## 9. Phases

### Phase 1 — Data model & zone classification — **HIGH**
| Task | File(s) |
|---|---|
| Add `Zone.category` + migration + default STRICT | `domain/Zone.java`, SQL |
| Add `CuttingRequest.dispatchedZone` + `dispatchedAt` + `dispatchedBy` + `zoneAcceptanceStatus` + `zoneAcceptedAt` + `zoneAcceptedBy` + `zoneRejectionReason` + `pinnedByChef` + `pinnedAt` + `pinnedBy` | `domain/CuttingRequest/CuttingRequest.java`, SQL |
| Add `UserZone` many-to-many entity + repository (§3.3) | `domain/UserZone.java`, `repositories/UserZoneRepository.java`, SQL |
| Admin UI: zone classification with conflict warnings (machine type in both STRICT + SHARED = warning) | `src/main/js/components/Layout/ZoneAdmin.js` |
| Chef-to-zone assignment page (§6.4) + endpoints `GET/POST/DELETE /api/userZones`, `PUT /api/userZones/{user}/default/{zone}` | `controller/UserZoneController.java`, `services/UserZoneService.java`, `src/main/js/components/Layout/ZoneOwnership.js` |
| **(dropped)** ~~`ZoneEfficiency` entity~~ — reuse `CapaciteInstallee` (§3.4) | — |

### Phase 2 — Dispatch service & Process page — **HIGH**
| Task | File(s) |
|---|---|
| `CuttingTimeCalculator` bean (§2.6) — joins CMS-DB `TimingModel` by placement, applies LASER × nbrCouche + Gerber × 2, **shared with** `PlanDeChargeService` | `services/CuttingTimeCalculator.java` |
| `ZoneLoadService` — implements the `(type, zone)` formula in §4 using `CuttingTimeCalculator` + `CapaciteInstalleeService` | `services/ZoneLoadService.java` |
| `SequenceDispatchService` — greedy + pairwise polish (§5), honours pin + acceptance | `services/SequenceDispatchService.java` |
| Chef-pin endpoint `POST /api/sequenceDispatch/{seq}/pin` + `POST /api/sequenceDispatch/{seq}/unpin` (admin only) | `controller/SequenceDispatchController.java` |
| `SerieTableAssigner` — virtual per-serie best-fit table (§3.7, §3.9); feasibility uses strict zone ∪ shared zones | `services/SerieTableAssigner.java` |
| `UnassignableSerie` entity + migration (§3.7), reasons `NO_MACHINE_TYPE | ZONE_OVERLOAD | ROLL_MISSING | NO_TIMING | OTHER` | `domain/UnassignableSerie.java`, SQL |
| Light projections: `ZoneLoadCellDto`, `SequenceDispatchRowDto`, `ProductionTableLight`, `UserZoneLight` (see §2.5) | `controller/dto/dispatch/*.java` |
| REST: `GET /api/zoneLoad`, `POST /api/sequenceDispatch/rebalance`, `POST /api/sequenceDispatch/{seq}` (force), `POST /api/sequenceDispatch/{seq}/accept`, `POST /api/sequenceDispatch/{seq}/pin`, `POST /api/sequenceDispatch/recomputeAssignments`, `GET /api/sequenceDispatch/unassignable` | `controller/SequenceDispatchController.java` |
| `DispatchAudit` entity with triggers `AUTO | MANUAL | REBALANCE | CHEF_PIN | UNPIN | MID_SHIFT_REJECT` | `domain/DispatchAudit.java` |
| Process page (heatmap + list + audit + Inassignables drawer) | `src/main/js/components/Layout/SequenceDispatcher.js` |

### Phase 2.5 — Chef-de-Zone confirmation flow — **HIGH**
> This phase is the non-negotiable bridge between the dispatcher's
> *plan on paper* and the floor's *reality on the day*. It is required
> for the contracts C2 and C4 in §8.2 to hold.

| Task | File(s) |
|---|---|
| `ShiftZoneConfirmation` + `ShiftZoneConfirmationMachine` entities & JPA repos | `domain/ShiftZoneConfirmation.java`, `domain/ShiftZoneConfirmationMachine.java`, `repository/ShiftZoneConfirmationRepository.java` |
| SQL migration for the two tables (§3.5) | `SQL/2026_xx_shift_zone_confirmation.sql` |
| `ShiftZoneConfirmationService` — create / amend / reopen; validates that the acting user is the zone's chef or `ROLE_ADMIN` | `services/ShiftZoneConfirmationService.java` |
| `ActiveMachineResolver` bean — single lookup for effective machines (confirmation → fallback to `ProductionTable.zone ∩ EtatMachineHistorique`) — implements contract **C2** | `services/ActiveMachineResolver.java` |
| REST: `GET /api/shiftZoneConfirmation?date&shift&zone`, `POST /api/shiftZoneConfirmation` (create Step 1), `PUT /api/shiftZoneConfirmation/{id}/machines` (mid-shift edit), `POST /api/shiftZoneConfirmation/{id}/reopen` | `controller/ShiftZoneConfirmationController.java` |
| Acceptance endpoints: `POST /api/sequenceDispatch/{seq}/accept`, `POST /api/sequenceDispatch/{seq}/reject` — update `zoneAcceptanceStatus` + emit events | extends `SequenceDispatchController.java` from Phase 2 |
| Scheduled job — **auto-accept fallback** at T0 for `PENDING` sequences; writes `zoneAcceptedBy = SYSTEM_AUTO`; pushes a banner to Process | `services/SequenceAutoAcceptJob.java` |
| Scheduled job — **pre-dispatch at T−60** (configurable) for the upcoming shift (opt-in toggle per Process, default ON) | `services/PreDispatchJob.java` |
| Application events `ShiftZoneConfirmedEvent`, `SequenceAcceptedEvent`, `SequenceRejectedEvent`, `ZoneMachineToggledEvent` (§8.3) | `events/*.java` |
| WebSocket fan-out `/topic/zone/{zoneName}` + `/topic/sequenceDispatch` for live badge refresh on Process & Chef pages | `config/WebSocketConfig.java`, `controller/ZoneWebSocketController.java` |
| Two-step confirmation modal React component (forced at shift entry) | `src/main/js/components/Layout/ChefDeZone/ConfirmationModal.js` |
| Audit page: list of who confirmed / accepted / rejected / reopened, with SYSTEM_AUTO rows highlighted amber | `src/main/js/components/Layout/ConfirmationAudit.js` |
| Unit tests for contracts C2 and C4 (the resolver + the schedulable filter) | `services/ActiveMachineResolverTest.java`, `services/SchedulableSerieFilterTest.java` |

### Phase 3 — Chef de Zone page — **MEDIUM**
| Task | File(s) |
|---|---|
| WebSocket topic `/topic/zone/{zoneName}` with existing config | `config/WebSocketConfig.java` |
| Blocker ticket endpoint (decision per question #6) | TBD |
| Chef de Zone page (read-mostly Supervision mode + embedded ConfirmationModal from Phase 2.5) | `src/main/js/components/Layout/ChefDeZone.js` |
| Dashboard menu entry gated on `ROLE_CHEF_DE_ZONE` | `Dashboard.js` |

### Phase 4 — Optimizer integration — **MEDIUM**
| Task | File(s) |
|---|---|
| Optimizer feasibility guard updated to use `dispatchedZone` | `services/scheduling/FeasibilityGuard.java` (from ordonnancement plan) |
| Application event on `dispatchedZone` change | `services/SequenceDispatchService.java` |
| Penalty semantics update (zone-deviation) | `services/scheduling/BoxDurationObjective.java` |

### Phase 5 — Polish — **LOW**
| Task | File(s) |
|---|---|
| Auto-rebalance at shift boundary (opt-in toggle per Process) | `services/SequenceDispatchService.java` |
| CSV export on sequence list | `SequenceDispatcher.js` |
| Historical-run comparison for dispatcher imbalance | `SequenceDispatcher.js` |

---

## 10. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Formula drifts between Plan de Charge, Sequence Dispatcher, Advanced Ordonnancement | Single `CuttingTimeCalculator` bean; unit tests that assert the three callers produce identical totals on the same fixture |
| Admin misconfigures a machine type into two zones with different `category` | Save-time validation in Zone admin; daily consistency job that raises a banner for Process |
| Chef de Zone can request blocker / redispatch → noise | Require a dropdown reason from a fixed list; Process reviews in the audit panel before acting |
| Started sequences are re-dispatched silently → floor confusion | Dispatcher is forbidden from changing `dispatchedZone` once any serie has `statusMatelassage ≠ Waiting`; guard enforced server-side |
| Chef-pinned sequence accidentally moved by greedy | Pinned sequences are part of the frozen set in §5.4; only `ROLE_ADMIN` / `ROLE_PROCESS` can unpin (writes `DispatchAudit.trigger = UNPIN`) |
| Efficiency set to 0 or > 100 by mistake | Constraint already enforced on the existing `CapaciteInstallee.efficienceTarget` column (between 30 and 120) + admin UI validation. **No new `ZoneEfficiency` table.** |
| `CuttingTimeCalculator` joins CMS-DB once per serie → N+1 across the dispatcher run | Mandatory use of `resolveMinutesBatch` (§2.6) — one CMS-DB call per dispatcher run, results cached per (placement) for the run. Unit test asserts ≤ 1 CMS-DB call per dispatcher invocation. |
| `UserZone` rows accumulate revoked-but-still-shown chefs | All Chef de Zone queries filter `revoked_at IS NULL` (contract C6); a daily housekeeping job archives rows older than 12 months. |

---

## 11. Open questions — resolved

The original ten questions have been answered by the user. Summary of
the decisions (now folded into the plan; kept here as a changelog):

| # | Question | Decision |
|---|---|---|
| 1 | Split a sequence across multiple STRICT zones? | **No.** One STRICT zone per sequence. Series with unavailable types fan out to SHARED zones (§3.8). Folded into §0 + §3.6. |
| 2 | SHARED-only sequences — keep `dispatchedZone = NULL`? | **Yes.** Null dispatch means "runs entirely on SHARED zones". Folded into §3.2. |
| 3 | Ownership when multiple chefs + shared zones | Each zone has a chef; a `ROLE_CHEF_DE_ZONE` user may own multiple zones via `UserZone` (§3.3). `ROLE_CHEF_EQUIPE` sees all zones. A new page (§6.4) lets `ROLE_CHEF_EQUIPE` / `ROLE_PROCESS` assign chefs to zones. |
| 4 | Horizon width | **View**: unfinished backlog over last 7 days (read-only). **Decision**: per-sequence `dueDate` / `dueShift`. Folded into §2.5.4. |
| 5 | Auto-rebalance policy | Continuous via the scheduling engine, but **chef-accepted and chef-pinned sequences are frozen**. A chef may also **pull** (pin) a sequence that wasn't originally assigned to his zone. Folded into §5.4.1. |
| 6 | Blocker-signal feature | **Dropped.** No such feature in the plan. |
| 7 | Who sets chef-to-zone? | `ROLE_CHEF_EQUIPE` or `ROLE_PROCESS` via the new `/process/zoneOwnership` page (§6.4). |
| 8 | Efficiency source | Fixed in existing `CapaciteInstallee` entity. **No new `ZoneEfficiency` entity.** Folded into §3.4 + contract C5. |
| 9 | Balance objective | "Calibrate the charge". The pain point is the intra-zone imbalance (too many Lectra vs few Lectra IP6 or vice versa). The `(type, zone)` keyed formula + `max − min` imbalance (§4.1) addresses exactly that. At start / mid-shift the dispatcher reads: (a) sequences still to finish in the zone, (b) series left inside those sequences, (c) already-accepted sequences — then places the next sequence to reduce the observed `(type, zone)` spread. |
| 10 | KPI jump on re-dispatch | **Acceptable.** No debouncing. |

### 11.1 Confirmation-gate questions (still open — from §4.5 and §7)

11. **Gate timing.** I picked T−60 for pre-dispatch, T−30 for Step 1
    (machine confirmation), T−20 for Step 2 (sequence acceptance),
    T0 shift start. Are those offsets realistic for Tangier TFZ's
    shift rhythm (S1 21:55, S2 05:55, S3 13:55)? Should they be global
    constants, per-shift, or per-zone configurable?
    
12. **Auto-accept fallback at T0.** If the chef doesn't click by T0,
    PENDING sequences auto-accept so the floor isn't blocked. Is that
    acceptable, or should the optimiser *not* schedule non-accepted
    sequences and let their machines sit idle until the chef catches
    up? (My default: auto-accept + visible SYSTEM_AUTO audit row.)
13. **SHARED zone ownership.** Step 1 / Step 2 are clearly the strict
    zones' chef's job. Does the **SHARED** LASER-DXF zone have its own
    chef who should go through the same confirmation flow? Or is
    SHARED capacity managed centrally by Process (no gate)?
14. **Reopening an ACCEPTED sequence.** Once accepted, can the chef
    later flip it back to REJECTED if something changes (e.g. material
    arrives late)? My draft says no — only `ROLE_ADMIN` can unfreeze
    — to prevent late-shift churn. Is that too strict?
15. **Machine added mid-shift.** Does adding a machine mid-shift need
    the full two-step modal (overkill) or just a single
    `[Ajouter machine]` dialog with one `remark` field? My draft chose
    the lighter single dialog because the Step 2 sequence list is
    already accepted by that point.
16. **Removing a machine that's running a serie.** Blocked until the
    serie finishes unless `ROLE_ADMIN` forces it with a reason. Is
    that the right split of authority, or should the chef be able to
    force-remove on his own?
17. **Chef-absent shift.** If no user has a `UserZone` row for this
    zone for the shift (absent, new hire, etc.), who fills the gap?
    Fall back to any `ROLE_CHEF_EQUIPE` / `ROLE_PROCESS` user confirming
    on the chef's behalf? Or auto-confirm the full
    `ProductionTable.zone` set?
18. **Per-shift vs per-machine efficiency.** The chef can annotate
    `remark` per machine ("opérateur nouveau"). Should that remark feed
    back into `CapaciteInstallee` as a temporary-for-this-shift override
    (e.g. a new `ShiftMachineEfficiency` side table layered on top of
    `CapaciteInstallee.efficienceTarget`), or stay a note?
19. **Rejection rate alerts.** When should Process be notified that a
    zone is rejecting too much (e.g. `rejected / dispatched > 30 %`
    rolling over 3 shifts)? My draft puts it as an alert on the
    dispatcher heatmap; would you want an email / Teams notification
    as well?
20. **Mid-shift re-dispatch to an already-confirmed zone.** When
    Process re-dispatches a PENDING-because-rejected sequence to
    Zone B at T+02h, Zone B's chef has already closed the
    confirmation flow. Should the sequence land as PENDING (chef must
    reopen & accept) or auto-ACCEPTED (silent, with a WS toast to the
    chef)? My draft says PENDING but I'm open.

### 11.2 Virtual-table & data-loading questions (still open — from §2.5, §3.7–§3.9)

21. **Virtual table vs operator reality.** The assigner writes
    `tableCoupe` with `tableIsVirtual = true`. When the operator
    starts matelassage on a *different* table, should we
    (a) flip `tableIsVirtual = false` and accept the move silently,
    (b) prompt the operator for a reason, or (c) reject the mismatch
    until Process re-assigns? My draft says (a) silent accept + log,
    but (b) feeds better data back to the optimiser.
22. **Unassignable surfacing.** When an `UnassignableSerie` row
    appears mid-shift, do you want an active notification (banner +
    Teams/email), or is a badge on the dispatcher page enough?
23. **End-stack penalty threshold.** I've set the quadratic penalty
    to kick in above 90 % queue load. Is 90 % the right threshold, or
    should it be configurable per zone (Lectra often runs hot, LASER
    runs cool)?
24. **Specialisation bonus.** The `typeSpecialisationBonus` prefers
    putting LASER-DXF series on LASER-DXF-only tables. That helps
    mixed machines stay free. Is this a universally-true preference
    for Lear Tangier, or does it depend on operator skill mix?
25. **Data projection drift.** The plan mandates selecting only DTO
    fields. When CAD/Process adds a new field to
    `CuttingRequestSerieData`, the projection queries won't
    automatically include it. Would you want a **compile-time check**
    (annotation processor) to keep projection queries in sync with
    the DTO, or is a manual checklist fine?
26. **Inassignables modal UX.** Listed by serie or grouped by reason?
    Grouped-by-reason scales better when 50 series fall in the same
    "no matching machine type" bucket. My draft groups.
27. **Recompute cadence.** `POST /api/sequenceDispatch/recomputeAssignments`
    is manual today. Should it also fire on
    `ZoneMachineToggledEvent` (chef adds a machine) to auto-heal
    `UnassignableSerie` rows? My draft: yes, automatic.
28. **Light projection repo layout.** Do you want all projections in
    one `com.lear.MGCMS.domain.CuttingRequest.data` package (today's
    convention), or split per entity (e.g. `ProductionTable.data`,
    `Zone.data`)? I'd stay with today's single-package convention but
    the dispatcher adds ~6 new DTOs which may tip it over.

---

## 12. What I recommend first

Ship **Phase 1 + Phase 2 + Phase 2.5** as one coherent release. They
give you:
- The strict/shared distinction on existing `Zone` rows (Phase 1).
- The heatmap that finally shows Process which zone is overloading,
  and the persisted `dispatchedZone` the Continuous Optimizer needs
  (Phase 2).
- The shift-start confirmation gate that makes the dispatched plan
  match the real floor, with the auto-accept fallback so nothing ever
  blocks (Phase 2.5) — this is what prevents the classic
  "plan-looks-good-on-paper-but-doesn't-match-the-floor" failure.

Shipping Phase 2 without Phase 2.5 is not recommended: the optimiser
would get a `dispatchedZone` without a way to know which machines are
actually live, and contracts C2 / C4 in §8.2 would not hold.

Phase 3 (Chef de Zone Supervision mode) is the polished day-to-day
dashboard; it reuses the `ConfirmationModal` component built in
Phase 2.5, so the effort is mostly layout + live widgets.

Phase 4 (optimizer integration) should land immediately after Phase 2.5
so the optimiser stops proposing cross-zone moves that the dispatcher
already ruled out, and uses the confirmed machine set as its feasibility
space.

---

## 13. Dispatching Engine — runtime configuration & modes

The greedy in §5.2 is the *one-shot* placement. The **Dispatching Engine**
keeps that greedy running on a loop, perturbing the result, and looking
for a flatter `(machineType, zone)` load distribution. It is the
"engine of dispatching" Process triggers from `ProcessDispatcher.js`.

### 13.1 What the engine optimises (and what it does NOT)

The engine moves **only `PENDING` and not-yet-started, not-pinned
sequences** between STRICT zones. It never:
- moves a sequence with any started serie,
- moves a sequence with `zoneAcceptanceStatus = ACCEPTED`,
- moves a sequence with `pinnedByChef = 1`,
- changes serie ↔ machine assignment (that is the Ordonnancement
  Engine's job — see `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`).

The set of frozen sequences forms the **baseline charge** for each
`(t, z)` cell: their resolved cutting time goes into the numerator of
`zoneLoad%(t, z)` *before* the engine starts placing PENDING ones. The
engine's objective is therefore "given this baseline that I cannot
move, place the remaining sequences so that
`max(t,z) zoneLoad%(t,z) − min(t,z) zoneLoad%(t,z)` is minimised across
STRICT zones with `N_active(t,z) > 0`."

### 13.2 State machine

```
IDLE ──Start──► WARMING ──baseline+greedy ready──► IMPROVING
                                                   │     │
                                          Pause ◄──┘     │
                                          Stop  ◄────────┘
PAUSED ──Resume──► IMPROVING
PAUSED ──Stop──► STOPPED
IMPROVING ──FIXED_DURATION timeout──► STOPPED (best saved)
```

- `IDLE` — boot or after Stop. No threads, no scratch space.
- `WARMING` — building snapshot (active machines, baseline charge,
  PENDING set), running the §5.2 greedy as warm-start. Typically
  100–400 ms.
- `IMPROVING` — perturbation loop running, publishing throttled
  WebSocket samples to `/topic/dispatcher/engine` every 2 s.
- `PAUSED` — loop suspended; current best preserved; can resume.
- `STOPPED` — terminated. The latest accepted suggestion is persisted
  to `dispatch_engine_run` (a saved candidate the chef can later
  accept / reject sequence by sequence).

### 13.3 Two run modes

| Mode | Trigger | Termination | Behaviour |
|---|---|---|---|
| `CONTINUOUS` | Process clicks **Démarrer (continu)** | Manual Stop, or any of the §2.2 invalidating events with `restart=true` | Loop runs as long as the user wants. Every improvement immediately overwrites the published `MachineQueue`-equivalent suggestion. The Process page chart shows live improvement. |
| `FIXED_DURATION` | Process clicks **Démarrer (durée fixée)** with a duration `N seconds` (60–1800) | Engine self-stops at `T0 + N` | Loop runs for exactly `N` seconds. At `T0 + N` the best-so-far is **saved** (one row in `dispatch_engine_run` + child rows for each suggested `(sequence, zone)`); engine transitions to `STOPPED`. The saved candidate stays available for chef acceptance / re-run comparison. |

Both modes use the **same** perturbation loop and the same objective.
The only difference is the termination condition.

### 13.4 Perturbation moves (cheap → expensive)

1. **Reassign** — pick one PENDING sequence, move it to its second-best
   STRICT zone candidate. O(1).
2. **Swap** — pick two PENDING sequences in different zones and swap
   their `dispatchedZone`. O(1).
3. **Block-rotate** — rotate a triple (s1→s2→s3→s1) across three zones.
   Escapes plateaus where pairwise swaps don't help.
4. **Kick** — perform 5 random Reassigns then re-evaluate. Used when
   no improvement in the last 200 iterations.

Hard guards (rejected moves):
- Move violates the dispatchable-types rule (target zone has zero
  active machines for any of the sequence's required types and the
  shared zones do not cover the gap).
- Move targets a frozen sequence.
- Move targets a STRICT zone with `N_active = 0` for every relevant
  type (i.e. dead zone for this shift).

### 13.5 Snapshot inputs

Each WARMING refresh reads (one query each, no `@Entity` graph load):
1. Active machines per (date, shift, zone) — `ActiveMachineResolver`.
2. Baseline sequences (started OR accepted OR pinned) for the horizon.
3. PENDING / NOT_DISPATCHED sequences in the horizon.
4. Started-sequence ID set — sequences with at least one serie whose
   `statusMatelassage` or `statusCoupe` is not `Waiting`. Loaded via
   `CuttingRequestSerieDataRepository.findStartedSequenceIdsForDateShift`
   so the engine can promote those into the baseline (frozen) bucket.
5. Resolved cutting time per serie — `CuttingTimeCalculator.resolveMinutesBatch`.
6. `CapaciteInstallee` rows for the horizon's (date, shift, groupe)
   tuples.

The snapshot is a small in-memory structure; the engine perturbs it,
and only writes to `dispatched_zone` / `zone_acceptance_status` when
Process clicks **Publier**.

REJECTED sequences are skipped at snapshot build time — the engine
must not propose a new zone for a sequence the chef has already
declined; redispatch goes through the explicit Process force flow.

### 13.6 Persistence — saved runs

```
CREATE TABLE dispatch_engine_run (
    id              BIGINT IDENTITY PRIMARY KEY,
    started_at      DATETIME2 NOT NULL,
    ended_at        DATETIME2 NULL,
    mode            VARCHAR(32) NOT NULL,    -- CONTINUOUS | FIXED_DURATION
    duration_sec    INT NULL,                -- only for FIXED_DURATION
    started_by      BIGINT NOT NULL,         -- FK User.id (ROLE_PROCESS)
    final_state     VARCHAR(32) NOT NULL,    -- STOPPED | ABORTED
    iterations      INT NOT NULL DEFAULT 0,
    improvements    INT NOT NULL DEFAULT 0,
    initial_spread  DECIMAL(6,2) NULL,       -- max-min zoneLoad% at WARMING
    final_spread    DECIMAL(6,2) NULL,       -- max-min zoneLoad% at STOPPED
    notes           NVARCHAR(512) NULL
);

CREATE TABLE dispatch_engine_run_suggestion (
    run_id          BIGINT NOT NULL,         -- FK dispatch_engine_run.id
    sequence        VARCHAR(64) NOT NULL,
    suggested_zone  VARCHAR(64) NOT NULL,    -- FK Zone.nom
    previous_zone   VARCHAR(64) NULL,
    PRIMARY KEY (run_id, sequence)
);

CREATE TABLE dispatch_engine_indicator_sample (
    run_id          BIGINT NOT NULL,
    sample_at       DATETIME2 NOT NULL,
    iteration       INT NOT NULL,
    spread_pct      DECIMAL(6,2) NOT NULL,
    max_load_pct    DECIMAL(6,2) NOT NULL,
    min_load_pct    DECIMAL(6,2) NOT NULL,
    accepted        BIT NOT NULL DEFAULT 0,
    PRIMARY KEY (run_id, iteration)
);
```

### 13.7 REST surface

| Verb | Path | Role | Body |
|---|---|---|---|
| `GET` | `/api/dispatcher/engine/state` | PROCESS, ADMIN, CHEF_DE_ZONE (read) | — |
| `POST` | `/api/dispatcher/engine/start` | PROCESS, ADMIN | `{ mode: "CONTINUOUS" \| "FIXED_DURATION", durationSec?: number, date, shift }` |
| `POST` | `/api/dispatcher/engine/pause` | PROCESS, ADMIN | — |
| `POST` | `/api/dispatcher/engine/resume` | PROCESS, ADMIN | — |
| `POST` | `/api/dispatcher/engine/stop` | PROCESS, ADMIN | — |
| `GET` | `/api/dispatcher/engine/runs` | PROCESS, ADMIN | — (paginated) |
| `GET` | `/api/dispatcher/engine/runs/{id}/samples` | PROCESS, ADMIN | — |
| `POST` | `/api/dispatcher/engine/runs/{id}/publish` | PROCESS, ADMIN | promotes saved suggestion to `dispatched_zone` rows |

### 13.8 WebSocket fan-out

Topic `/topic/dispatcher/engine` — sends:
- `{ type: "STATE", state, mode, runId }` on every state transition.
- `{ type: "SAMPLE", runId, iteration, spread, maxLoad, minLoad, ts }`
  throttled to one message every 2 s.
- `{ type: "SUGGESTION", runId, sequence, fromZone, toZone }` when
  the engine accepts a perturbation that changes a sequence's zone.

### 13.9 What this engine does NOT do

- Not the serie ↔ machine ↔ time assignment (Ordonnancement Engine).
- Not the chef confirmation flow (manual UX, §4.5).
- Not the M-only feasibility decision (read-only consumer of
  `ActiveMachineResolver`).
- Does not write to `MachineQueue` (only the Ordonnancement Engine
  writes there).

---

## 14. Implementation status — Dispatching Engine

Tracked here so future passes can pick up where the last one stopped.
Tick boxes when each phase ships and is smoke-tested.

### Phase A — Harden the zone dispatcher
- [x] Verify `ZoneLoadService` keys `(machineType, zone)` and STRICT-only.
- [x] Switch `ActiveMachineResolver` to M-only rule with `null = M`.
- [x] Confirm baseline = started + accepted + pinned sequences.
- [x] Replace any `@Entity` graph fetch in dispatcher hot paths with light projections.
- [x] `GET /api/dispatcher/previewWithLoad?date&shift` returns ZoneLoadCells + candidate decisions.
- [x] `POST /api/dispatcher/publish` writes `dispatched_zone` + `PENDING`.
- [x] Smoke test: heatmap renders for today × shift 1.

### Phase B — Continuous / Fixed-duration engine
- [x] Flyway `V13_07__dispatch_engine_run.sql` — three new tables (§13.6).
- [x] `DispatchEngineRun`, `DispatchEngineRunSuggestion`, `DispatchEngineIndicatorSample` entities + repos.
- [x] `ContinuousDispatchOptimizerService` with state machine (§13.2) and modes (§13.3).
- [x] Perturbation moves Reassign, Swap, Block-rotate, Kick (§13.4).
- [x] Snapshot loader using projections only (§13.5).
- [x] Frozen-set covers pinned + accepted + **started** (statusMatelassage/statusCoupe ≠ Waiting via `findStartedSequenceIdsForDateShift`).
- [x] REJECTED sequences excluded from candidate set.
- [x] M-only fallback returns `active = 0` when no chef confirmation AND no M-status machine — closed zone gets no work.
- [x] `ActiveMachineResolver.isZoneConfirmed()` reflects chef-only signal; `hasActiveMachines()` is the relaxed predicate.
- [x] `DispatchEngineController` exposing the §13.7 endpoints.
- [x] STOMP topic `/topic/dispatcher/engine` (§13.8).
- [x] Smoke test: `ContinuousDispatchOptimizerServiceTest` — CONTINUOUS reaches IMPROVING + perturbs + stops on demand; FIXED_DURATION self-stops on the 1-second timer and persists run + suggestions. Both tests green (2/2).

### Phase C — UI on `ProcessDispatcher.js`
- [x] Engine control bar — mode toggle, duration slider, Start/Pause/Stop, current state.
- [x] Live heatmap zone × machineType, color-coded.
- [x] Imbalance gauge + Recharts time-series fed by polling `/api/dispatcher/engine/state`.
- [x] Suggested-dispatch panel (`DispatchEngineRunsPanel`) listing `dispatch_engine_run_suggestion` rows for the current run.
- [x] Saved-runs table (collapsible) reading `GET /api/dispatcher/engine/runs`, expandable per row to show the saved suggestions, with **Publier** button calling `POST /api/dispatcher/engine/runs/{id}/publish`.
- [x] Hide config from non-PROCESS / non-ADMIN users; chef sees read-only view (no Publier button).
- [ ] Smoke test (UI, manual): start engine, watch indicator drop, Stop, Publish suggestion. **Not automated — requires browser session against a live app + DB.**

### Phase D (deferred, separate plan)
- Ordonnancement Engine (serie ↔ machine ↔ time) — see
  `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`. Configured from
  `AdvancedOrdonnancement.js`, `ROLE_PROCESS` only.
