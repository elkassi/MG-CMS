# MG-CMS - Database Review

_Scope:_ live inspection of the local SQL Server copy of `LEAR_MG_CMS_Prod` on `MSI\\SQLEXPRESS:1434`
_Date:_ 2026-04-25
_Method:_ `sqlcmd` profiling of schema metadata, row volumes, key coverage, empty tables, and targeted data-quality checks

---

## 0 - Executive summary

| Metric | Value |
|--------|-------|
| User tables | 140 |
| Total rows inspected from table metadata | 6,562,657 |
| Database size | 1,406.06 MB |
| Data file size | 1,365.00 MB |
| Log file size | 41.06 MB |
| Foreign keys | 77 |
| Disabled foreign keys | 0 |
| Untrusted foreign keys | 0 |
| Tables without primary key | 4 |
| Empty tables | 34 |
| History/report tables | 24 |
| Rows in history/report tables | 1,848,710 |

### Main conclusions

1. This is a real operational database, not a toy schema. The dominant domains are cutting, planning, machine history, quality, maintenance, work orders, and audit/reporting.
2. Referential integrity is better than average for a legacy manufacturing system: the schema has foreign keys and they are enabled and trusted.
3. The most important data rectification candidate is `dbo.CuttingRequest.sequenceStatus`: all 130,284 rows are `NULL`, while the application expects business values such as `ACTIVE`, `PAUSED`, `WAITING_MATERIAL`, and `COMPLETED`.
4. `dbo.WorkOrder.status` contains mixed business codes and at least one obvious typo: `Complited`. This should be normalized before more logic depends on it.
5. The scheduling feature appears implemented in the codebase but dormant in this database: `ShiftSchedule`, `SequenceSchedule`, `SerieSchedule`, `ScheduleInterval`, `MachineScheduleStatus`, `shift_load_calculation`, and `scheduling_config` are empty.
6. Four tables have no primary key. Three look like archive or transition tables and are empty; one is `sites_projets`, also empty. They should either be formalized or removed from the active schema.

---

## 1 - What the database tells us about MG-CMS

The database structure and row counts show a broad manufacturing execution scope:

- Cutting execution: `CuttingRequest`, `CuttingRequestSerie`, `CuttingRequestSerieRouleau`, `CuttingRequestBox`, `CuttingPlan`, `CuttingPlanMaterial`, `CuttingPlanMaterialPlacement`
- Machine and production traceability: `CoupeMachineHistory`, `CoupeDrill`, `CoupePerformance`, `ScanXPL`, `StockStatusReport`
- Engineering and technical master data: `GammeTechnique`, `PartNumberInfo`, `PartNumberBoom`, `GammeTechniquePartNumberMaterial`
- Quality: `AuditQualite`, `QualityNotice`, `QualityValidationHistory`
- Maintenance and incident tracking: `Intervention`, `MaintenanceIntervention`
- Planning and ordonnancement: `Planning`, plus the empty schedule-related tables mentioned later
- Security and administration: `users`, `roles`, `users_roles`

### Largest tables by row count

| Table | Rows |
|-------|------|
| `dbo.StockStatusReport` | 780,366 |
| `dbo.CoupePerformance` | 648,279 |
| `dbo.CoupeDrill` | 622,620 |
| `dbo.CoupeMachineHistory` | 381,129 |
| `dbo.GammeTechniqueImprimerHistorique` | 355,395 |
| `dbo.overlap_saving` | 343,599 |
| `dbo.CuttingPlanMaterialPlacement` | 283,175 |
| `dbo.CuttingPlanMaterial` | 258,456 |
| `dbo.CuttingRequestBox` | 235,214 |
| `dbo.CuttingRequestSerieRouleau` | 216,725 |

### Technical posture

- Compatibility level is `110`.
- Recovery model is `FULL`.
- Collation is `SQL_Latin1_General_CP1_CI_AS`.

This is consistent with a legacy SQL Server application that has grown by feature expansion rather than by periodic consolidation.

---

## 2 - Positive observations

These are the things that look healthy and should be preserved:

- `users.username` is clean: 248 rows, 248 distinct usernames, 0 blanks.
- `users.password` appears to use 60-character bcrypt-style hashes for all 248 users.
- `PartNumberInfo.partNumber` is unique across 17,272 rows.
- `QualityNotice.numeroQn` is unique across 5,479 rows.
- `Machine.code` is unique for the two machine rows present in this database.
- Foreign-key health is good: 77 foreign keys, 0 disabled, 0 untrusted.

---

## 3 - Findings and recommended actions

### H1 - `CuttingRequest.sequenceStatus` is null for every sequence

**Evidence**

- `dbo.CuttingRequest` has 130,284 rows.
- `COUNT(DISTINCT sequence) = 130,284`, so sequence identity itself is clean.
- `sequenceStatus` distribution is one value only: `NULL` for all 130,284 rows.
- In the application code, `CuttingRequestData` declares `sequenceStatus = "ACTIVE"` and the material-demand forecast filters out `COMPLETED`, `PAUSED`, and `WAITING_MATERIAL` values.

**What this probably means**

- The column exists but was never backfilled for existing production data.
- New rows may also be inserted without a database-level default.
- Any feature depending on persistent sequence status is effectively working on missing data.

**Why it matters**

- It weakens material-demand forecasting.
- It prevents reliable pause/wait/completed workflow reporting.
- It makes future scheduling logic harder to trust.

**Recommended action**

1. Confirm the intended default with business owners. Based on the code, `ACTIVE` is the best candidate.
2. Backfill nulls.
3. Add a database default for future inserts.
4. Consider making the column `NOT NULL` after the backfill and application verification.

**Safe first-step SQL**

```sql
UPDATE dbo.CuttingRequest
SET sequenceStatus = 'ACTIVE'
WHERE sequenceStatus IS NULL;
```

### H2 - `WorkOrder.status` is not normalized

**Evidence**

- `dbo.WorkOrder` has 171,053 rows.
- `wo` is effectively unique, with only 1 blank value.
- `woid` is nullable on 23,447 rows, so it should be treated as optional unless the business says otherwise.
- Status values are mixed:
  - `Wait`: 149,237
  - `C`: 14,696
  - `Complited`: 5,740
  - `Ongoing`: 975
  - `Received`: 295
  - `R`: 77
  - `A`: 33

**What this probably means**

- The column evolved over time without a strict dictionary.
- Short codes and full-text labels are mixed in the same field.
- `Complited` is almost certainly a typo rather than a valid business code.

**Why it matters**

- Reports and filters can silently miss rows.
- New integrations may hard-code the wrong status vocabulary.
- It is harder to build reliable dashboards and KPIs.

**Recommended action**

1. Define the official work-order status dictionary.
2. Map every existing value to that dictionary.
3. Correct the typo and legacy one-letter codes with a controlled migration.
4. Add either a lookup table or a constrained enum strategy at the application layer.

**Example normalization query to prepare, not run blindly**

```sql
SELECT status, COUNT(*)
FROM dbo.WorkOrder
GROUP BY status
ORDER BY COUNT(*) DESC;
```

### H3 - Scheduling schema exists, but production data is absent

**Evidence**

The following tables are present but empty:

- `dbo.ShiftSchedule`
- `dbo.SequenceSchedule`
- `dbo.SerieSchedule`
- `dbo.ScheduleInterval`
- `dbo.MachineScheduleStatus`
- `dbo.shift_load_calculation`
- `dbo.scheduling_config`

At the same time, the codebase contains a scheduling service and frontend metadata for these entities.

**What this probably means**

- The scheduling feature was developed but not activated in the production data set.
- Or the feature exists in UI/code but is not yet populated by jobs or operators.

**Why it matters**

- Users may see incomplete screens or empty dashboards.
- Developers may assume the tables are live when they are not.
- It increases maintenance cost because code and schema suggest a feature that production data does not confirm.

**Recommended action**

1. Decide whether scheduling is meant to be live in MG-CMS today.
2. If yes, define the seed path, population jobs, and data ownership.
3. If no, hide the feature from production UI and document it as dormant.

### M1 - Four tables have no primary key

**Tables identified**

- `dbo.CuttingRequestSerie2024`
- `dbo.CuttingRequestSerieRouleau2024`
- `dbo.CuttingRequestSerieRouleauHistory2024`
- `dbo.sites_projets`

**Current state**

- All four are empty in this database copy.
- The first three look like archive or year-specific transition tables.
- `sites_projets` looks like a relationship table and should probably have either a composite key or a surrogate key.

**Why it matters**

- If any of these tables start being used, duplicate rows become possible immediately.
- ORMs and migration tools behave better with explicit primary keys.

**Recommended action**

1. If these tables are still needed, add primary keys before they receive production rows.
2. If they are obsolete, deprecate and remove them from the active schema roadmap.

### M2 - History and reporting footprint is large enough to deserve an archive strategy

**Evidence**

- 24 tables match `History`, `Rapport`, `Audit`, or `Report` naming patterns.
- Together they contain 1,848,710 rows, about 28 percent of all rows counted from table metadata.
- Several of the largest tables are reporting or history-oriented rather than transactional masters.

**Why it matters**

- Backup, restore, and index maintenance costs grow faster than business value if retention is unmanaged.
- Heavy mixed-use tables can slow reporting and operational screens at the same time.

**Recommended action**

1. Split hot operational data from cold history where practical.
2. Archive by year or quarter for tables such as machine history, audit, and drill/performance history.
3. Review indexes on the largest history tables before adding new features on top of them.

### M3 - A few master-data cleanup tasks are worth doing now

**Observed issues**

- `dbo.PartNumberInfo.status` has 60 `NULL` rows.
- `dbo.GammeTechnique.partNumber` has 1 blank row.
- `dbo.WorkOrder.wo` has 1 blank row.
- `dbo.QualityNotice.decision` is `NULL` for 1,940 rows.

**Important nuance on `QualityNotice`**

- 1,897 of those 1,940 `NULL` decisions are still `active = 1`.
- That suggests most of them are open notices, not dirty closed records.

**Recommended action**

1. Backfill missing master-data keys where business owners can identify the source row.
2. For quality notices, distinguish `open` from `undecided` explicitly in reporting instead of treating null decision as an error.
3. Add data-quality checks in import routines for blank part numbers and blank work orders.

### M4 - `Planning.rowId` is not a unique identifier

**Evidence**

- `dbo.Planning` has 69,642 rows.
- It has only 2,628 distinct `rowId` values.
- Some `rowId` values repeat more than 100 times.

**Interpretation**

- `rowId` behaves like an import line identifier, batch row number, or source-system ordinal.
- It should not be treated as a unique technical key in new logic.

**Recommended action**

- If the application needs a stable unique identifier for planning records, introduce one explicitly instead of relying on `rowId`.

---

## 4 - Empty or dormant tables worth reviewing

The following empty tables deserve a business decision because they add surface area without current evidence of value:

- Scheduling and capacity: `ShiftSchedule`, `SequenceSchedule`, `SerieSchedule`, `ScheduleInterval`, `MachineScheduleStatus`, `shift_load_calculation`, `scheduling_config`
- Archive or transition: `CuttingRequestSerie2024`, `CuttingRequestSerieRouleau2024`, `CuttingRequestSerieRouleauHistory2024`, `CuttingRequestBox2024`, `CuttingRequestPartNumber2024`, `Intervention_Archive`
- Quality/config: `BlockCondition`, `BoxTypeConfig`, `QualityReftissuBlock`
- Production support: `MaterialLogistics`, `Placement`, `PlacementDetail`, `LaminationPls`

Recommendation: review each empty table with the business owner and classify it as one of these four states:

- active and waiting for data
- feature parked for later
- migration leftovers
- safe to retire

---

## 5 - Suggested rectification backlog

1. Backfill `CuttingRequest.sequenceStatus`, add a database default, and decide whether `NOT NULL` is now safe.
2. Standardize `WorkOrder.status` values and remove typo-based or one-letter variants.
3. Decide whether scheduling is live, dormant, or deprecated, then align schema, jobs, and UI.
4. Add primary keys to the four no-PK tables if they will remain in the schema.
5. Clean the small master-data defects now: blank `GammeTechnique.partNumber`, blank `WorkOrder.wo`, null `PartNumberInfo.status`.
6. Build an archival and indexing plan for the biggest history/report tables.
7. Add recurring data-quality checks to imports and nightly jobs so these issues stop reappearing.

---

## 6 - Limits of this review

- This review is based on the local SQL Server copy of the production database, not on the live production server itself.
- Backup history cannot be judged from this restored copy because SQL Server backup history lives in `msdb` on the source instance, not inside the application database backup itself.
- No live workload tracing, execution-plan capture, SQL Agent job inspection, or blocking analysis was performed here.
- Recommendations about archival, constraints, and status normalization should be validated with the business owners before rollout.

---

## 7 - Practical next move

If only one database change is made first, it should be the `CuttingRequest.sequenceStatus` cleanup because:

- the column is already expected by the application,
- the current state is uniformly missing,
- the remediation is simple,
- and it improves future scheduling and material-demand logic at the same time.