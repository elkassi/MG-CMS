# Improvement Recommendations — MG-CMS / CMS-Prod / scanCoupe

> **For the current cross-repo improvement backlog** (ambiguous findings, open product questions,
> deferred items from the 2026-07-02 4-repo investigation), see [`improvements.md`](improvements.md).
> This file remains the historical DONE/PARTIAL/OPEN tracker for the 2026-06-11 review.

*Written 2026-06-11 after a full schema + code + restored-DB review (see `schema.md`).
Status reviewed 2026-06-30, refreshed 2026-07-02 against the live codebase — each item is now
tagged **[DONE]** (with the migration/feature that resolved it), **[PARTIAL]**, or **[OPEN]**.
The 2026-07-02 pass folds in commits that shipped after the 2026-06-30 review: 250cc88 (dispatch
graded-material consolidation), 35c38ed (CNC reference images), 3b24191/1252dc9 (cncQualite
filters/sort), ea62f60 (cncQualiteMachine report), 757998e (box-ID validation), and f1f407d
(Tier-D wait-age anti-starvation ranking, documented in `CLAUDE.md` and
`production-flow-and-strategy.md` — this file's scope is indexing/import/roll-tracking, not the
ranking algorithm, so it isn't itemized separately here).
Ordered by impact. SQL verified against the 2026-04-25 prod restore; always re-check
`flyway_schema_history` and `sys.indexes` in prod before running anything.*

---

## 1. Indexing plan (database speed)

### 1.0 First: verify what prod actually has
The 2026-04-25 restore had **only primary keys** on every hot table. Two batches of fixes
postdate it: Flyway `V14_01` (2026-05-12, workbench indexes) and the manual prod script of
2026-06-09 (`IX_CoupeMachineHistory_ind`, `IX_CoupeDrill_ind`, `IX_StockStatusReport_qtyOnHand`,
`IX_CuttingRequestSerieRouleau_serie`). Before adding anything:

```sql
SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;
SELECT t.name tbl, i.name ix FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id
WHERE i.type=2 AND t.name LIKE 'Cutting%' OR t.name IN ('WorkOrder','Scan_Rouleau','ScanXPL','overlap_saving');
```

**Action: codify the 2026-06-09 prod-only indexes as a Flyway migration (V18_01)** so dev/test
get them and a prod rebuild can't lose them.

**[DONE]** — `V18_01__perf_indexes.sql` (2026-06-25) and `V18_02__system_health_hot_path_indexes.sql`
both shipped, verified against a live import of the prod DB. The June-9 prod-only indexes plus
the gaps below are now Flyway-managed on the primary DB.

### 1.1 Missing indexes (new Flyway migration, P0)
These cover query patterns found in the repositories that V14_01/June script do **not** cover.
**Status (2026-06-30, all in `V18_01`):** WorkOrder due, CuttingRequestSerieRouleau idRouleau,
ScanXPL serie, overlap_saving serie are **[DONE]** (V18_01 shipped them, some with slightly
different INCLUDE lists than proposed below). CoupeDrill and FirstCheck also got date indexes in
V18_01. The two satellite-DB indexes (GammeTechniqueImprimer_NSerie on `qualite`,
ProdTicket_labelId on `MG_PLS_NEW`) are documented in V18_01's footer as **apply-by-hand**
(Flyway only manages the primary DB) — verify they exist in prod. **[OPEN]:** the three
CuttingRequest/CuttingRequestSerie planning indexes (IX_CuttingRequest_planning,
IX_CuttingRequestSerie_planning, IX_CuttingRequestSerie_machine_dates) are NOT yet shipped.

```sql
-- /preparation filters CuttingRequest by planningDate+shift (V14 only covers dueDate/sequenceStatus)
CREATE NONCLUSTERED INDEX IX_CuttingRequest_planning
    ON CuttingRequest (planningDate, shift)
    INCLUDE (cuttingPlanId, modele, definition, dueDate, dueShift, sequenceStatus);

-- daily production views + ordonnancement read séries by planningDate/shift
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_planning
    ON CuttingRequestSerie (planningDate, shift)
    INCLUDE (cuttingRequest_sequence, statusMatelassage, statusCoupe, machine, partNumberMaterial);

-- machine history windows: findBetween(dateDebutCoupe, machine)
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_machine_dates
    ON CuttingRequestSerie (machine, dateDebutCoupe)
    INCLUDE (statusCoupe, dateFinCoupe, tempsDeCoupe, placement);

-- /preparation WO list (dueDate+shift) — today a full scan of 171k rows
CREATE NONCLUSTERED INDEX IX_WorkOrder_due
    ON WorkOrder (dueDate, shift)
    INCLUDE (woid, item, partNumber, qtyOpen, qtyCompleted, status, deactivated);

-- roll lifecycle: scanCoupe Contrôle tab + logistics roll tracking query by roll serial
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerieRouleau_idRouleau
    ON CuttingRequestSerieRouleau (idRouleau)
    INCLUDE (cuttingRequestSerie_serie, sequence, metrage, retour, confirmRetour, createdAt);

-- CMS-Prod: per-série lookups
CREATE NONCLUSTERED INDEX IX_ScanXPL_serie ON ScanXPL (serie) INCLUDE (machine, scanDate);
CREATE NONCLUSTERED INDEX IX_overlap_saving_serie ON overlap_saving (serie) INCLUDE (created_at, couche_number);

-- qualite DB: import calls MAX(NSerieGammeImp) on 130k rows per imported sequence
CREATE NONCLUSTERED INDEX IX_GammeTechniqueImprimer_NSerie ON GammeTechniqueImprimer (NSerieGammeImp);

-- MG_PLS_NEW: scanCoupe queries ProdTicket by labelId (verify in prod first — local copy is empty)
CREATE NONCLUSTERED INDEX IX_ProdTicket_labelId ON ProdTicket (labelId) INCLUDE (reftissu, quantity, createdAt);
```

P1 (add if their pages feel slow, after measuring): `Intervention (machine, debutArret)`,
`FirstCheck (date, shift, machine)`, `CoupePerformance (machine, date)`,
`Scan_Rouleau (reftissu)` INCLUDE (emplacement, metrage), `StockStatusReport (ref, isDeleted)`.

### 1.2 "Read the end, not the whole table" (the long-history problem)
**Status (2026-06-30):** Rule 1's filtered active indexes (IX_CRS_active, IX_CR_active) are **[OPEN]**
— V18_02 added two *timeline* filtered indexes on CuttingRequestSerie
(IX_CuttingRequestSerie_coupe_timeline / _matelassage_timeline, both `WHERE … IS NOT NULL`),
but the proposed status-filtered active-slice indexes are not yet shipped.
Rule 2's CoupeMachineHistory date key (the "read backwards instead of scan") shipped as
IX_CoupeMachineHistory_lineDate in V18_01; the CuttingRequestBox `idNum` computed-column trick is
still **[OPEN]**. Rule 3's archive policy is **[PARTIAL]**: the `/archiving` admin tool shipped
(atomic batch DELETE…OUTPUT over a SAFE_LEAF whitelist) but a scheduled yearly archive job is not.

The tables that grow forever (CuttingRequestSerie 324k, SerieRouleau 217k, CoupeMachineHistory,
CoupePerformance 648k, overlap_saving…) are *active only in their last few days*. Three rules:

1. **Every floor/dashboard query must be bounded** by date or status — never `findAll()`.
   The biggest wins are filtered indexes on the *active* slice, which stay tiny no matter how
   big the table gets:
   ```sql
   -- active séries are <1% of the table; this index stays a few MB forever
   CREATE NONCLUSTERED INDEX IX_CRS_active
       ON CuttingRequestSerie (statusCoupe, statusMatelassage)
       INCLUDE (cuttingRequest_sequence, machine, planningDate, tableCoupe, tableMatelassage)
       WHERE statusCoupe <> 'Complete';

   CREATE NONCLUSTERED INDEX IX_CR_active
       ON CuttingRequest (sequenceStatus)
       INCLUDE (planningDate, shift, dueDate, dueShift, dispatched_zone)
       WHERE sequenceStatus IN ('IMPORTED','RELEASED','STARTED','MATERIAL_MISSING');
   ```
   (Caveat: filtered indexes are used only when the query predicate provably implies the
   filter — keep the literals identical to the repository queries.)
2. **Latest-row lookups**: replace `MAX(CAST(col AS INT))` scans with `TOP 1 … ORDER BY` over
   an indexed/computed column, or better a counter table (§2).
   For `CuttingRequestBox`: `ALTER TABLE CuttingRequestBox ADD idNum AS TRY_CAST(id AS INT) PERSISTED;`
   `CREATE INDEX IX_Box_idNum ON CuttingRequestBox (idNum DESC);` — turns the per-import scan
   into an instant seek (backwards-compatible, no code change required).
3. **Archive policy** (yearly job, after validation): move rows older than ~13 months from
   `CuttingPlanHistory` (326 MB!), `GammeTechniqueImprimerHistorique`, `CoupePerformance`,
   `CoupeDrill`, `CoupeMachineHistory`, `overlap_saving`, `FirstCheck`, `AuditQualite` to
   `<table>_archive`. Keep the lifecycle tables (`CuttingRequest*`) un-archived until the
   roll-traceability requirement is settled — they are the traceability record. Note: SQL
   Server **Standard edition = offline index builds**; schedule DDL in the night shift gap.

### 1.3 Server settings
`cost threshold for parallelism` was already raised 5→50 (June fix) **[DONE]**. Separately, the
JDBC unicode-parameter scan was fixed by adding `;sendStringParametersAsUnicode=false` to the
**prod** datasource URL (`application-tanger.properties`) — note the committed properties file
does NOT carry this flag, so confirm it on the live prod connection string. After the new indexes,
update statistics on the touched tables and re-check the top offenders:
`sys.dm_exec_query_stats` ordered by `total_logical_reads`.

---

## 2. Make import fast and batch (the `/preparation` pain)

**Status (2026-06-30): [OPEN]** — none of this section has shipped. No `IdCounter` table
(no migration defines one), no `POST /api/cuttingRequest/batch` endpoint, no JPA batching
properties (`hibernate.jdbc.batch_size` / `order_inserts` are absent from every
`application*.properties`), and no `LegacyMirrorService`. Still the importer's daily pain.

Today: one POST per sequence; each runs ~10 MAX()/LIKE counter scans + row-by-row inserts
(sequence, PNs, séries, **one row per box** — often hundreds) + 5 mirror inserts per PN into
qualite. That is why importing a full shift takes so long.

1. **Counter table instead of MAX() scans** (also fixes cross-system uniqueness):
   ```sql
   CREATE TABLE IdCounter (name VARCHAR(40) PRIMARY KEY, nextVal BIGINT NOT NULL);
   -- seed once: boxId = max(box.idNum, qualite NSerieGammeImp)+1 ; serie = max(...)+1
   UPDATE IdCounter WITH (UPDLOCK) SET @v = nextVal = nextVal + @count WHERE name='boxId'; -- range grab
   ```
   One atomic range grab per import replaces 6 scans. The legacy CMS keeps writing its own
   ids — keep the seed comfortably above and re-sync nightly, or (better) bump the counter
   to `max(legacy)` inside the same grab while qualite still writes.
2. **`POST /api/cuttingRequest/batch`**: accept `List<CuttingRequestImportDto>`, validate all
   first (gamme exists, packaging present — return per-item errors), then persist in ONE
   transaction with ranges pre-allocated. Frontend: "Importer tout" button on `/preparation`
   that sends the already-validated combinaison in one call with a per-line result table.
3. **JPA batching** (one-line win for every cascade save):
   ```properties
   spring.jpa.properties.hibernate.jdbc.batch_size=50
   spring.jpa.properties.hibernate.order_inserts=true
   spring.jpa.properties.hibernate.order_updates=true
   ```
4. **Qualite mirror writes**: keep them synchronous (same transaction) for now — the floor
   reads them within minutes — but move the 5-table mirror block out of the controller into a
   `LegacyMirrorService.mirrorImport(...)`. That gives one place to batch it, and one class to
   delete on the day qualite is decommissioned.
5. Target: importing 20 sequences ≤ a few seconds (vs minutes today). Measure before/after
   with the same shift's data.

---

## 3. Roll out/return tracking (logistics "100% sure")

**Status (2026-06-30): [PARTIAL]** — the allocation ledger now exists
(`V16_02__logistics_allocation.sql` table `logistics_allocation` + `AllocationService` +
`LogisticsReleaseService`), so "don't invent a new table" is satisfied. Its lifecycle is
`ADVISED → RELEASED → CONSUMED` with `RETURNED / CANCELLED` off-ramps — NOT the richer
`PICKED / ON_TABLE / RETURN_PENDING` state machine proposed in #1 below, and the #2
exception report / daily missing-roll mail is **not yet built**. Items #1 (extra states wired to
scanCoupe Sortie / SerieRouleauTemp / confirmRetour) and #2 (exception report) remain **[OPEN]**.

Data already exists but nothing closes the loop ("3 rolls × 100m out, 85m needed — did the
rest come back?"). Build on the allocation ledger (don't invent a new table):

1. **State machine on `logistics_allocation.status`**:
   `ALLOCATED → PICKED → ON_TABLE → CONSUMED / RETURN_PENDING → RETURNED (+ CANCELLED)`.
   - scanCoupe **Sortie** scan sets PICKED (operator scans rack + roll; match on serialId).
   - CMS-Prod mounting the roll (`SerieRouleauTemp` insert) sets ON_TABLE.
   - `CuttingRequestSerieRouleau.confirmRetour=true` or a remnant re-scanned into
     `Scan_Rouleau` sets RETURNED; full consumption sets CONSUMED.
2. **Exception report (the actual guarantee)**: a small page + daily mail —
   *allocations PICKED/ON_TABLE > N hours with no consumption row*, and *rolls whose
   `metrage − totalUsage − retour` ≠ remnant re-entered*. That diff IS the missing-roll alarm.
3. **Shortage messages must say which state failed**: "not on rack but in magasin (bring it
   down)" vs "not in magasin (procurement)" — R100/`StockStatusReport` vs `Scan_Rouleau`
   distinction, already specified in `production-flow-and-strategy.md` §4.1.

---

## 4. WorkOrder entry for ROLE_PLANIFICATEUR (requested optional feature)

**Status (2026-06-30): [OPEN]** — not built. No `ROLE_PLANIFICATEUR` in the codebase, and no
migration adds an `origin`/`createdBy` column to WorkOrder.

Low-risk to add: the table + refresh already exist.
- Add `origin` column to `WorkOrder` (`'ERP'` default | `'MANUAL'`) and `createdBy`; the QAD
  refresh must only upsert `origin='ERP'` rows so it never clobbers manual ones.
- New role `ROLE_PLANIFICATEUR` (roles table insert + route gate); page = EntityList/EntityForm
  pattern on `/workOrders` with fields wo (generated `MAN-yyMMdd-n`), item (WL…), partNumber,
  quantity, dueDate, dueShift.
- `/preparation` already reads WorkOrder by dueDate+shift — manual WOs appear automatically.

---

## 5. Combinaison: cover the need first, then minimal cutting time

**Status (2026-06-30): [OPEN]** — no covering/scoring logic was added; the `to-work` suggestion
still doesn't optimize. (This ties into the §2 batch import, which is also unbuilt.)

Current `to-work` suggestion doesn't optimize. Keep it a transparent greedy (the optimizer
lesson from the dispatcher applies here too):
1. Hard goal: chosen plans × quantities **cover every WO's open qty** (no uncovered PN).
2. Among covering options prefer: fewer over-production kits → lower Σ `tempsDeCoupe`
   (`CuttingPlanMaterialPlacement.tempsDeCoupe × nbrCouche`) → fewer distinct sequences →
   fewer distinct materials (batch heavy rolls).
3. Show the math per suggestion (covered / over / est. cutting time) so the importer can
   override — same trust principle as the release advisor.
4. Plans that *already exactly match* a WO set (the common repeat case) should short-circuit
   to a one-click "import all" (ties into §2 batch import).

---

## 6. Design & professionalism (app-wide)

1. **One mental model everywhere**: sequence → séries → boxes with the §3 lifecycle from
   `schema.md`. Every screen should display the same status chips/colors for
   sequenceStatus and statusMatelassage/statusCoupe (shared React component, not per-page
   string switches).
2. **Stop the entity-clone pattern**: CuttingRequest exists as 4 mapped classes
   (`CuttingRequest`, `V2`, `Info`, `Data`) — same table, different cascades. Two of them in
   one session can silently overwrite each other's writes. Rule going forward: **`data/`
   classes + interface projections only**; freeze the others (no new fields), migrate
   read-paths opportunistically.
3. **Status literals**: serie statuses are scattered string literals (`'Waiting'`,
   `'In progress'`, even lowercase `'waiting'` in two native queries — works only because the
   collation is case-insensitive). Add a `SerieStatus` constants class like `SequenceStatus`
   and reference it everywhere. **[OPEN]** (2026-06-30) — no `SerieStatus` constants class exists;
   literals are still scattered (`SerieStatusDateValidator` is unrelated).
4. **Monolith components**: `ImportationNew.js` (2.5k lines), CMS-Prod `Form.js` (7k) /
   `FormCoupeNew.js` (9k) — never big-bang refactor; extract the API calls into a
   `services/` module per screen first, then split tabs into child components when touched.
5. **Default filters**: list pages on hot tables must default to *today + active* (and
   paginate server-side), never load-all-then-filter-client-side.
6. **Menu vs route gates**: `/logisticsRelease` reachable by URL for `ROLE_VALID_QN_LOGISTIQUE`
   but invisible in the menu (known gap) — decide and align all department sections once.
7. **Ship a deploy checklist**: lockfile check (`npm ci` desync gotcha), bundle version bump
   (CMS-Prod `index.html` cache-busting), Flyway pending migrations, never commit local
   `application.properties`.

---

## 7. Flexibility for future change

1. **Everything through Flyway** — no manual prod DDL without a paired migration (the June 9
   indexes are the standing counter-example to fix).
2. **Isolate the legacy bridge**: all qualite reads/writes behind `LegacyMirrorService` +
   the 20-min sync. When the old CMS dies, delete one service and the `com.lear.cms` package.
3. **Document as you change**: `schema.md` is now the source of truth — update it in the same
   PR as any schema change (CLAUDE.md already points to it).
4. **One shift convention at the boundary**: new code uses plant numbering (1 night / 2
   morning / 3 afternoon) and converts at the qualite/Ordonnancement boundary only.
5. **Measure first**: keep `SQL_HealthCheck_MG-CMS.sql` runs (top reads / missing-index DMVs /
   fragmentation) as a monthly routine; add indexes from evidence, not by habit — every index
   slows the floor's writes a little.

## Suggested order of execution

| # | Item | Effort | Impact | Status (2026-06-30) |
|---|---|---|---|---|
| 1 | Verify V14 in prod + Flyway-ify June indexes (V18_01) | hours | locks in June gains | **DONE** (V18_01 + V18_02) |
| 2 | §1.1 missing indexes + §1.2 filtered actives + Box `idNum` | hours | /preparation, WO list, roll history, dashboards | **PARTIAL** (V18_01/02 indexes done; CR/CRS planning indexes, status-filtered actives, Box `idNum` open) |
| 3 | §2 batch import (counter table + batch endpoint + JPA batching) | 2-4 days | the importer's daily pain | **OPEN** |
| 4 | §3 allocation state machine + exception report | 3-5 days | logistics traceability goal | **PARTIAL** (ledger V16_02 shipped; extra states + exception report open) |
| 5 | §4 planner WO entry | 1-2 days | closes the workflow's front door | **OPEN** |
| 6 | §5 combinaison scoring | 2-3 days | better plans, less over-cut | **OPEN** |
| 7 | §6/§7 hygiene items | continuous | long-term clarity | ongoing |
