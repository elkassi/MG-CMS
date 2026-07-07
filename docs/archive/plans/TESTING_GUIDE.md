# MG-CMS Dispatcher — Testing Guide (Phases 0 → 11)

> **Purpose.** Walk a tester or developer through verifying every feature
> introduced by the 11-phase dispatcher plan. Each section lists exactly
> what to do, what to look for, and what a passing result looks like.
> No prior knowledge of the codebase is assumed beyond having the app
> running and access to the database.

---

## 0. Pre-flight checklist

Before starting any phase test, run through this list once.

| Check | How |
|---|---|
| App starts cleanly | `GET http://localhost:6001/` returns 200 or the login page |
| Flyway reports all scripts applied | See §0.1 |
| At least one Zone row exists with `category` set | See §0.2 |
| Test users exist with the right roles | See §0.3 |
| No leftover `dispatched_zone` from a previous test run | See §0.4 |

### 0.1 — Verify Flyway applied every migration

```sql
SELECT version, description, success
FROM   flyway_schema_history
ORDER  BY installed_rank;
```

**Expected:** rows for V1, V2_01 through V2_06, V9_01, all with `success = 1`.
Any row with `success = 0` means the migration failed — fix the SQL before
testing.

### 0.2 — Verify zone categories

```sql
SELECT nom, category, is_active
FROM   Zone
ORDER  BY category, nom;
```

**Expected:**
- At least one zone with `category = 'SHARED'` (the laser zone).
- All other zones with `category = 'STRICT'`.
- All zones you intend to test with `is_active = 1`.

If the laser zone still shows `STRICT`, run:
```sql
UPDATE Zone SET category = 'SHARED' WHERE nom = '<your laser zone nom>';
```

### 0.3 — Ensure test users exist

You need three users for the full test run:

| Variable | Role | Used in |
|---|---|---|
| `PROCESS_USER` | `ROLE_PROCESS` | Phases 4, 10 |
| `CHEF_USER` | `ROLE_CHEF_DE_ZONE` | Phases 5, 10 |
| `OPERATOR_USER` | `ROLE_USER` (or any standard role) | Phases 8, 9 |

```sql
SELECT u.matricule, r.name
FROM   users u
JOIN   users_roles ur ON ur.user_id = u.id
JOIN   roles r        ON r.id = ur.role_id
WHERE  r.name IN ('ROLE_PROCESS','ROLE_CHEF_DE_ZONE','ROLE_CHEF_EQUIPE')
ORDER  BY u.matricule;
```

### 0.4 — Clean slate for CuttingRequest dispatch columns

For a repeatable test run, wipe any previous dispatcher state on your
**test** sequences only:
```sql
UPDATE CuttingRequest
SET    dispatched_zone          = NULL,
       zone_acceptance_status   = NULL,
       pinned_by_chef           = 0
WHERE  planningDate = '<your test date>';
```

---

## Conventions used in this guide

- `BASE = http://localhost:6001` (change if your `server.port` differs).
- All `curl` commands assume a logged-in session. Pass your session cookie
  with `-b "JSESSIONID=<value>"` or use a browser / Postman with an
  established session.
- Replace `<DATE>` with an ISO date like `2026-04-24` where it appears.
- Replace `<SHIFT>` with `1`, `2`, or `3`.
- SQL blocks can be run in SQL Server Management Studio or any SQL client
  connected to `LEAR_MG_CMS`.
- **🟢 PASS** / **🔴 FAIL** markers show what a test looks like when it
  works or doesn't.

---

## Phase 0 — Regression baseline (existing features still work)

> Run this before enabling any dispatcher flag and again after enabling
> every flag. If anything breaks here, the dispatcher touched something
> it shouldn't have.

### T0-1 — Plan de Charge loads

1. Log in as any user.
2. Navigate to **Plan de Charge**.
3. Select today's date and shift 1.
4. Click **Calculer / Recalculer**.

🟢 **Pass:** shift load table populates with `totalPlannedTime` > 0 for at
least one machine group. No 500 errors in the browser console.

### T0-2 — Ordonnancement auto-dispatch still runs

```bash
curl -X POST "$BASE/api/ordonnancement/assignSerie" \
  -H "Content-Type: application/json" \
  -d '{"machineNom":"<any machine>","serieId":"<any serieId>"}'
```

🟢 **Pass:** HTTP 200 with the updated queue, OR a business-logic error
body (e.g. "série already assigned") — not a 500.

### T0-3 — Ordonnancement queue visible

```bash
curl "$BASE/api/ordonnancement/queue/all"
```

🟢 **Pass:** HTTP 200, JSON array (may be empty if no queues seeded).

---

## Phase 1 — CuttingTimeCalculator

> Goal: confirm the de-duplication didn't change PdC output, and that
> Gerber ETAs in the engine are now twice what they used to be (bug fix).

### T1-1 — PdC load % unchanged

1. Note the load % for a Gerber machine on a known shift before
   deploying Phase 1 (or on a branch without Phase 1).
2. Deploy Phase 1.
3. Reload PdC for the same shift.

🟢 **Pass:** Gerber load % is identical. LASER-DXF load % is identical.

### T1-2 — Gerber ETAs doubled in Ordonnancement

1. Pick a Gerber serie from `CuttingRequestSerie` and note its
   `tempsDeCoupe`.
2. Assign it to a Gerber machine via `/api/ordonnancement/assignSerie`.
3. Read back the queue:
   ```bash
   curl "$BASE/api/ordonnancement/queue/<machineName>"
   ```
4. Check `estimatedCuttingTime` in the response.

🟢 **Pass:** `estimatedCuttingTime = tempsDeCoupe × 2`.

### T1-3 — Unit tests pass

```bash
mvn -Dtest=CuttingTimeCalculatorTest test
```

🟢 **Pass:** `Tests run: 23, Failures: 0, Errors: 0`.

---

## Phase 2 — Dispatcher data model (Flyway)

> Goal: confirm all six migration scripts applied cleanly and the new
> columns/tables are queryable.

### T2-1 — New columns on CuttingRequest

```sql
SELECT TOP 3
    sequence,
    dispatched_zone,
    zone_acceptance_status,
    pinned_by_chef
FROM CuttingRequest;
```

🟢 **Pass:** Query runs without "invalid column name" errors. All three
columns exist (values may be NULL).

### T2-2 — Zone category column

```sql
SELECT nom, category, is_active FROM Zone;
```

🟢 **Pass:** Rows return. `category` column exists and is populated
(default `STRICT`).

### T2-3 — user_zone table exists

```sql
SELECT COUNT(*) FROM user_zone;
```

🟢 **Pass:** Returns 0 (empty) or the count of any pre-seeded rows.
No "object not found" error.

### T2-4 — shift_zone_confirmation tables exist

```sql
SELECT COUNT(*) FROM shift_zone_confirmation;
SELECT COUNT(*) FROM shift_zone_confirmation_machine;
```

🟢 **Pass:** Both return a count without error.

### T2-5 — unassignable_serie table exists

```sql
SELECT COUNT(*) FROM unassignable_serie;
```

🟢 **Pass:** Returns a count without error.

### T2-6 — machine_queue version column

```sql
SELECT TOP 3 id, machineNom, version FROM MachineQueue;
```

🟢 **Pass:** `version` column exists (may be 0 for all rows).

### T2-7 — admission_blocked_audit table (Phase 9 migration)

```sql
SELECT COUNT(*) FROM admission_blocked_audit;
```

🟢 **Pass:** Returns 0 or a count without error.

---

## Phase 3 — Service contracts (unit tests)

> Goal: confirm the four new services have green test coverage.

### T3-1 — SerieZoneResolver tests

```bash
mvn -Dtest="SerieZoneResolverTest*" test
```

🟢 **Pass:** 10 tests, 0 failures.

### T3-2 — SchedulableSerieFilter tests

```bash
mvn -Dtest="SchedulableSerieFilterTest*" test
```

🟢 **Pass:** 4 tests, 0 failures. The `laserDxfRejectedWhenNoShiftConfirmation`
case should appear in the test output.

### T3-3 — Both together

```bash
mvn -Dtest="SerieZoneResolverTest*,SchedulableSerieFilterTest*" test
```

🟢 **Pass:** 14 tests, 0 failures.

---

## Phase 4 — Dispatcher service + Process page

> All Phase 4 tests require `mgcms.dispatcher.enabled=true` in
> `application.properties` + restart. Revert after testing if needed.

### Flip the flag on
```properties
# application.properties
mgcms.dispatcher.enabled=true
```
Restart the app.

### T4-1 — Flag-off returns 404

With the flag **off** (`false`), hit either endpoint as `PROCESS_USER`:
```bash
curl -i "$BASE/api/dispatcher/preview?date=<DATE>&shift=<SHIFT>"
```

🟢 **Pass:** HTTP 404. The feature is invisible.

### T4-2 — Preview (flag on, ROLE_PROCESS)

With the flag **on**, as `PROCESS_USER`:
```bash
curl "$BASE/api/dispatcher/preview?date=<DATE>&shift=<SHIFT>"
```

🟢 **Pass:** HTTP 200, JSON body like:
```json
{
  "byZone": {
    "FirstArticle": { "sequences": ["SEQ001","SEQ002"] },
    "Laser":        { "sequences": ["SEQ003"] }
  },
  "unassignable": []
}
```
`sequences` contains the actual sequence IDs dispatched to each zone.
`unassignable` lists any rejected series with a `reasonCode`.

### T4-3 — Preview blocked for non-PROCESS

As `CHEF_USER` (ROLE_CHEF_DE_ZONE):
```bash
curl -i "$BASE/api/dispatcher/preview?date=<DATE>&shift=<SHIFT>"
```

🟢 **Pass:** HTTP 403 Forbidden.

### T4-4 — Publish writes to DB

As `PROCESS_USER`:
```bash
curl -i -X POST \
  "$BASE/api/dispatcher/publish?date=<DATE>&shift=<SHIFT>"
```

🟢 **Pass:** HTTP 200. Then verify in the DB:
```sql
SELECT sequence, dispatched_zone, zone_acceptance_status
FROM   CuttingRequest
WHERE  planningDate = '<DATE>'
  AND  shift = '<SHIFT>'
  AND  dispatched_zone IS NOT NULL;
```

Expected: rows now have `dispatched_zone` populated and
`zone_acceptance_status = 'PENDING'`.

### T4-5 — ProcessDispatcher UI renders

1. Log in as `PROCESS_USER`.
2. Navigate to `/processDispatcher`.

🟢 **Pass:** Page loads, shows the date/shift selector, a table of
zones with their sequences, and a Publish button. No 404 or blank page.

---

## Phase 5 — Chef-de-Zone confirmation gate

### Setup: assign CHEF_USER to a zone

As a `ROLE_CHEF_EQUIPE` or `ROLE_PROCESS` user:
```bash
curl -X POST "$BASE/api/userZone/assign" \
  -H "Content-Type: application/json" \
  -d '{
    "matricule": "<CHEF_USER matricule>",
    "zoneNom": "FirstArticle",
    "isDefault": true
  }'
```

🟢 **Pass:** HTTP 200. Verify:
```sql
SELECT * FROM user_zone WHERE user_id = '<CHEF_USER matricule>';
```

### T5-1 — /me returns correct zones

Log in as `CHEF_USER`:
```bash
curl "$BASE/api/userZone/me"
```

🟢 **Pass:** Response contains `defaultZone = "FirstArticle"` and the
zones list includes `"FirstArticle"`.

### T5-2 — Zone confirmation gate (no confirmation → engine rejects)

With `mgcms.engine.zoneAware=true`, before creating a confirmation row,
any preview call should return LASER-DXF series as unassignable (because
the SHARED zone has no confirmation).

```sql
-- Confirm there is no shift confirmation for this date/shift/zone yet
SELECT COUNT(*) FROM shift_zone_confirmation
WHERE date_production = '<DATE>' AND shift_number = <SHIFT>;
-- Expected: 0
```

```bash
# Preview should show unassignable series for laser-type machines
curl "$BASE/api/dispatcher/preview?date=<DATE>&shift=<SHIFT>"
```

🟢 **Pass:** `unassignable` array contains series with
`reasonCode = "ALL_ZONES_CLOSED_FOR_SHIFT"` for any LASER-DXF machines.

### T5-3 — POST /api/zone/confirm

As `CHEF_USER` (CHEF_DE_ZONE):
```bash
curl -X POST "$BASE/api/zone/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "<DATE>",
    "shift": <SHIFT>,
    "zoneNom": "FirstArticle",
    "upMachineNoms": ["L1","L2"]
  }'
```

🟢 **Pass:** HTTP 200. Verify:
```sql
SELECT szc.id, szc.zone_nom, szc.confirmed_at,
       m.machine_nom, m.is_up
FROM   shift_zone_confirmation szc
JOIN   shift_zone_confirmation_machine m ON m.confirmation_id = szc.id
WHERE  szc.date_production = '<DATE>'
  AND  szc.shift_number    = <SHIFT>
  AND  szc.zone_nom        = 'FirstArticle';
```

Expected: one `szc` row, two `m` rows with `is_up = 1`.

### T5-4 — Toggle machine mid-shift

```bash
curl -X POST "$BASE/api/zone/toggleMachine" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "<DATE>",
    "shift": <SHIFT>,
    "zoneNom": "FirstArticle",
    "machineNom": "L1"
  }'
```

🟢 **Pass:** HTTP 200. In DB, `is_up` for `L1` flips from 1 to 0
(or 0 to 1 if already down). Run the same query as T5-3 to confirm.

### T5-5 — GET /api/zone/confirmation/{zoneNom}

```bash
curl "$BASE/api/zone/confirmation/FirstArticle?date=<DATE>&shift=<SHIFT>"
```

🟢 **Pass:** HTTP 200, JSON with confirmation details and machine list.

### T5-6 — ChefDeZoneConfirm UI renders

1. Log in as `CHEF_USER`.
2. Navigate to `/chefDeZoneConfirm`.

🟢 **Pass:** Page shows the user's zones with machine checkboxes and a
Save/Confirm button. No 404 or blank page.

### T5-7 — UserZoneAdmin UI renders

1. Log in as `PROCESS_USER`.
2. Navigate to `/userZoneAdmin`.

🟢 **Pass:** Page renders the assign/revoke form. Non-PROCESS/CHEF_EQUIPE
users should be redirected or see a 403.

---

## Phase 6 — Plan de Charge polish

### T6-1 — Shift duration knob

In `application.properties`, change:
```properties
mgcms.shift.durationMinutes=480
```
Restart. Open PdC. The available-time column (denominator in load %)
should now reflect 480 minutes (minus break time).

To verify the formula:
```
effective = durationMinutes − breakMinutes = 480 − 30 = 450 min available
```

The PdC `availableTime` figure in the `ShiftLoadCalculation` table should
match:
```sql
SELECT availableTime FROM ShiftLoadCalculation
WHERE dateProduction = '<DATE>' AND shiftNumber = <SHIFT>;
```

🟢 **Pass:** `availableTime = 450` (or scaled by efficiency target).

### T6-2 — PdC actuals overlay endpoint

```bash
curl "$BASE/api/pdc/overlay?date=<DATE>&shift=<SHIFT>&machineNoms=L1,L2"
```

🟢 **Pass:** HTTP 200, JSON array of `MachineActuals` objects each with
`plannedMinutes`, `efficiencyRatio`, `earliestStart`, `latestEnd`.

### T6-3 — Role gate on Recalculer button

1. Log in as a user **without** `ROLE_PROCESS`.
2. Open Plan de Charge.

🟢 **Pass:** The Recalculer button is hidden (or disabled).

---

## Phase 7 — Zone-aware ordonnancement engine

> Requires `mgcms.engine.zoneAware=true`.

### Flip zone-aware on
```properties
mgcms.engine.zoneAware=true
```
Restart.

### T7-1 — Engine scopes by zone (LASER-DXF rejected without confirmation)

1. Ensure no `shift_zone_confirmation` exists for the SHARED (Laser) zone
   for today's shift:
   ```sql
   DELETE FROM shift_zone_confirmation
   WHERE zone_nom = '<your Laser zone nom>'
     AND date_production = '<DATE>'
     AND shift_number = <SHIFT>;
   ```

2. Publish the dispatcher (T4-4) for a shift that includes a LASER-DXF serie.

3. Check `unassignable_serie`:
   ```sql
   SELECT serie_id, reason_code, reason_detail, created_at
   FROM   unassignable_serie
   ORDER  BY created_at DESC;
   ```

🟢 **Pass:** A row appears with `reason_code = 'ALL_ZONES_CLOSED_FOR_SHIFT'`
for the LASER-DXF serie.

### T7-2 — MachineQueue version bumps

1. Note the current version for a machine:
   ```sql
   SELECT machineNom, version FROM MachineQueue WHERE machineNom = 'L1';
   ```

2. Call `saveQueues` or trigger a dispatch tick.

3. Query again.

🟢 **Pass:** `version` is incremented by at least 1.

### T7-3 — pinnedByChef survives re-dispatch

1. Pin a serie:
   ```sql
   UPDATE CuttingRequest SET pinned_by_chef = 1
   WHERE  sequence = '<SEQ>';
   ```

2. Run the dispatcher publish three times.

3. Check the MachineQueue — the pinned serie must remain at its position.

🟢 **Pass:** Queue position unchanged for the pinned serie.

### T7-4 — Auto-tick cron (optional, only if you want to see scheduled output)

```properties
mgcms.engine.autoTick.enabled=true
mgcms.engine.autoTick.cron=0/30 * * * * *   # every 30 s for testing
```
Restart, watch logs for 30 s.

🟢 **Pass:** You see log lines like `EngineTickService: auto-dispatch tick`.
Flip back to false afterwards.

---

## Phase 8 — Operator kiosk

### T8-1 — nextSerie returns head of queue

1. Ensure at least one `MachineQueue` row exists for a machine (L1):
   ```sql
   SELECT TOP 1 * FROM MachineQueue WHERE machineNom = 'L1'
   ORDER BY queuePosition;
   ```

2. Call the kiosk endpoint (no auth required):
   ```bash
   curl "$BASE/api/kiosk/nextSerie?machine=L1"
   ```

🟢 **Pass:** HTTP 200, body contains `serieId`, `machineNom`,
`estimatedCuttingTime`, and `queueVersion`.

### T8-2 — Empty queue returns 204

Remove all queue entries for a machine (or use one with no entries):
```bash
curl -i "$BASE/api/kiosk/nextSerie?machine=EMPTY_MACHINE"
```

🟢 **Pass:** HTTP 204 No Content.

### T8-3 — Version endpoint

```bash
curl "$BASE/api/kiosk/version?machine=L1"
```

🟢 **Pass:** HTTP 200, body `{"version": <N>}` where N ≥ 0.

### T8-4 — Version changes when queue changes

1. Note version: `curl "$BASE/api/kiosk/version?machine=L1"`.
2. Trigger any queue change (add or move an entry via the Ordonnancement
   API or DB directly).
3. Read version again.

🟢 **Pass:** Version number incremented.

### T8-5 — KioskBanner component renders

1. Navigate to a page that includes the KioskBanner component with a
   valid machine param.

🟢 **Pass:** Banner displays "next serie" card. After a version change
(T8-4) the banner updates within ~2 s.

---

## Phase 9 — Admission control

### T9-1 — Shadow mode (enforce=false): advisory response

Ensure `mgcms.admission.enforce=false` (default).

```bash
curl -X POST "$BASE/api/admission/check" \
  -H "Content-Type: application/json" \
  -d '{
    "serieId": "<serieId>",
    "zoneNom": "FirstArticle",
    "date": "<DATE>",
    "shift": <SHIFT>
  }'
```

For a serie dispatched to a different zone (or no zone):

🟢 **Pass:** HTTP 200 regardless.  Body has `"allowed": false` with a
reason, AND `"enforce": false`. The blocked audit row is still written:
```sql
SELECT TOP 5 * FROM admission_blocked_audit ORDER BY created_at DESC;
```

### T9-2 — Enforce mode: 409 on block

Set `mgcms.admission.enforce=true` + restart.

Send the same request as T9-1 for a serie whose zone claim doesn't match
the resolved zone:

```bash
curl -i -X POST "$BASE/api/admission/check" \
  -H "Content-Type: application/json" \
  -d '{
    "serieId": "<serieId not dispatched to Laser>",
    "zoneNom": "Laser",
    "date": "<DATE>",
    "shift": <SHIFT>
  }'
```

🟢 **Pass:** HTTP 409 Conflict, body:
```json
{
  "allowed": false,
  "reason": "OTHER",
  "detail": "serie=... claimedZone=Laser expectedZone=FirstArticle",
  "enforce": true
}
```

Verify audit row:
```sql
SELECT TOP 1 * FROM admission_blocked_audit ORDER BY created_at DESC;
```

### T9-3 — Allowed admission returns 200

Call `/api/admission/check` with the **correct** zone for a serie that has
a shift confirmation with machines up:

🟢 **Pass:** HTTP 200, `"allowed": true`. No new row in `admission_blocked_audit`.

### T9-4 — AdmissionBlockModal renders in the UI

1. On the kiosk page or operator form, trigger an admission block (T9-2 scenario).

🟢 **Pass:** A modal appears with a French error message matching the
reason code. The modal has an OK button that closes it.

---

## Phase 10 — Chef-de-Zone live page

> Requires `mgcms.dispatcher.enabled=true` and a published dispatch.

### Setup

1. Publish the dispatcher for today (T4-4).
2. Confirm that `zone_acceptance_status = 'PENDING'` for sequences in
   your CHEF_USER's zone.

### T10-1 — Acceptance endpoint (ACCEPTED)

As `CHEF_USER`:
```bash
curl -i -X POST \
  "$BASE/api/dispatcher/sequence/<SEQ>/acceptance?status=ACCEPTED"
```

🟢 **Pass:** HTTP 200. Verify in DB:
```sql
SELECT sequence, zone_acceptance_status
FROM   CuttingRequest
WHERE  sequence = '<SEQ>';
```
Expected: `zone_acceptance_status = 'ACCEPTED'`.

### T10-2 — Acceptance endpoint (REJECTED)

```bash
curl -i -X POST \
  "$BASE/api/dispatcher/sequence/<SEQ2>/acceptance?status=REJECTED"
```

🟢 **Pass:** HTTP 200. DB shows `zone_acceptance_status = 'REJECTED'`.

### T10-3 — Invalid status returns 400

```bash
curl -i -X POST \
  "$BASE/api/dispatcher/sequence/<SEQ>/acceptance?status=BANANA"
```

🟢 **Pass:** HTTP 400, body `"status must be ACCEPTED or REJECTED"`.

### T10-4 — Unknown sequence returns 404

```bash
curl -i -X POST \
  "$BASE/api/dispatcher/sequence/DOES_NOT_EXIST/acceptance?status=ACCEPTED"
```

🟢 **Pass:** HTTP 404 Not Found.

### T10-5 — Wrong role blocked

As `OPERATOR_USER` (no CHEF_DE_ZONE / PROCESS role):
```bash
curl -i -X POST \
  "$BASE/api/dispatcher/sequence/<SEQ>/acceptance?status=ACCEPTED"
```

🟢 **Pass:** HTTP 403 Forbidden.

### T10-6 — ChefDeZonePage UI renders with action buttons

1. Log in as `CHEF_USER`.
2. Navigate to `/chefDeZonePage`.

🟢 **Pass:**
- Page shows the default zone name in the heading.
- The sequences table renders with **Accepter** (green) and
  **Rejeter** (red) buttons per row.
- Clicking **Accepter** → row disappears or updates, a green info bar
  shows `"Séquence <SEQ> → ACCEPTED"`.
- Page auto-refreshes every 15 s (check the Network tab: a new
  `/preview` request fires ~15 s after page load).

---

## Phase 11 — Retention cron + self-heal

### T11-1 — Retention runs manually via purgeNow

The simplest way to test without waiting for 02:30 is to trigger the
method directly from a Spring Boot Actuator endpoint or a small test
class. Alternatively, temporarily change the cron expression to fire
soon:

```properties
mgcms.retention.enabled=true
mgcms.retention.days=7
mgcms.retention.cron=0/10 * * * * *    # every 10 s for testing
```
Restart.

Seed a stale row to be pruned:
```sql
INSERT INTO unassignable_serie (serie_id, reason_code, reason_detail, created_at)
VALUES ('TEST-OLD', 'OTHER', 'manual test', DATEADD(day, -8, SYSDATETIME()));
```

Wait ~10 s. Then:
```sql
SELECT COUNT(*) FROM unassignable_serie WHERE serie_id = 'TEST-OLD';
```

🟢 **Pass:** Row is gone (count = 0). Same test for `admission_blocked_audit`.

Revert the cron to `0 30 2 * * *` and `enabled=false`.

### T11-2 — Retention only removes rows older than `days`

Seed a **recent** row:
```sql
INSERT INTO unassignable_serie (serie_id, reason_code, reason_detail, created_at)
VALUES ('TEST-RECENT', 'OTHER', 'manual test', SYSDATETIME());
```

After the cron fires:
```sql
SELECT COUNT(*) FROM unassignable_serie WHERE serie_id = 'TEST-RECENT';
```

🟢 **Pass:** Row still exists (count = 1).

### T11-3 — Self-heal logs stuck PENDING sequences

Set:
```properties
mgcms.engine.selfHeal.enabled=true
mgcms.engine.selfHeal.cron=0/15 * * * * *       # every 15 s for testing
mgcms.engine.selfHeal.stuckPendingMinutes=0      # make everything look stuck
```
Restart.

Publish the dispatcher (so some sequences have `zone_acceptance_status = 'PENDING'`).
Wait 15 s and check the application log:

```
WARN  SelfHeal: sequence=SEQ001 dispatchedZone=FirstArticle stuck PENDING since …
WARN  SelfHeal: 2 CuttingRequest(s) stuck in PENDING > 0 min
```

🟢 **Pass:** Warnings appear, no ERROR stack traces, and **no database
writes** — the self-heal only logs.

Revert all three self-heal flags to defaults.

### T11-4 — Kill-switch: disabled means no action

```properties
mgcms.retention.enabled=false
mgcms.engine.selfHeal.enabled=false
```

Check logs — no retention or self-heal activity lines should appear,
even with a short cron expression.

🟢 **Pass:** Zero log lines from `RetentionCronService` or `SelfHealService`.

---

## End-to-end flow test (happy path)

Run this after all individual phase checks pass. It simulates the full
production workflow for one shift.

### Step 1 — Schema ready

Run T2-1 through T2-7. All pass.

### Step 2 — Roles and zones

```sql
-- assign chef1 (ROLE_CHEF_DE_ZONE) to FirstArticle as default
INSERT INTO user_zone (user_id, zone_nom, is_default, assigned_by)
VALUES ('<chef1 matricule>', 'FirstArticle', 1, '<admin matricule>');
```

### Step 3 — Chef confirms machines

As `chef1`, POST `/api/zone/confirm`:
```json
{
  "date": "<TODAY>",
  "shift": 1,
  "zoneNom": "FirstArticle",
  "upMachineNoms": ["L1","L2"]
}
```

Verify `shift_zone_confirmation` has a row and both machines are `is_up=1`.

### Step 4 — Process previews dispatch

Enable dispatcher flag. As `PROCESS_USER`:
```bash
curl "$BASE/api/dispatcher/preview?date=<TODAY>&shift=1"
```
Confirm Lectra series land in `FirstArticle`, LASER-DXF series in the
Laser zone (or `unassignable` if Laser isn't confirmed).

### Step 5 — Process publishes dispatch

```bash
curl -X POST "$BASE/api/dispatcher/publish?date=<TODAY>&shift=1"
```
Confirm DB: `dispatched_zone` populated, `zone_acceptance_status = PENDING`.

### Step 6 — Chef accepts sequences

As `chef1`, navigate to `/chefDeZonePage`. Click **Accepter** for each
sequence in the table. Verify `zone_acceptance_status = 'ACCEPTED'` in DB.

### Step 7 — Kiosk reads next serie

```bash
curl "$BASE/api/kiosk/nextSerie?machine=L1"
```
🟢 Head-of-queue serie returned with correct `estimatedCuttingTime`.

### Step 8 — Operator checks admission

```bash
curl -X POST "$BASE/api/admission/check" \
  -H "Content-Type: application/json" \
  -d '{
    "serieId": "<serie from step 7>",
    "zoneNom": "FirstArticle",
    "date": "<TODAY>",
    "shift": 1
  }'
```
🟢 HTTP 200, `"allowed": true`.

---

## Rollback verification

> Run this whenever you need to confirm a feature can be safely disabled.

### Kill all dispatcher features with one properties change

```properties
mgcms.dispatcher.enabled=false
mgcms.engine.zoneAware=false
mgcms.engine.autoTick.enabled=false
mgcms.admission.enforce=false
mgcms.retention.enabled=false
mgcms.engine.selfHeal.enabled=false
```

Restart. Then run T0-1 through T0-3 (the baseline regression tests).

🟢 **Pass:** All three baseline tests still pass. No dispatcher-specific
log lines appear. Plan de Charge and Advanced Ordonnancement work exactly
as they did before Phase 3.

### What stays in the DB (non-destructive)

| Column / Table | What it contains | Safe to leave |
|---|---|---|
| `CuttingRequest.dispatched_zone` | Previous dispatch assignments | Yes — ignored when flag is off |
| `CuttingRequest.zone_acceptance_status` | PENDING / ACCEPTED / REJECTED | Yes — ignored when flag is off |
| `shift_zone_confirmation` | Chef confirmation history | Yes |
| `user_zone` | Chef–zone assignments | Yes |
| `unassignable_serie` | Rejection audit | Yes — can be manually pruned |
| `admission_blocked_audit` | Block audit | Yes — can be manually pruned |
| `MachineQueue.version` | Monotonic counter | Yes — kiosk reads it but kiosk works without it |

No column needs to be dropped to roll back. The feature flags alone are
sufficient.

---

## Quick reference — all feature flags

| Property | Default | What it gates |
|---|---|---|
| `mgcms.dispatcher.enabled` | `false` | `/api/dispatcher/*` endpoints + ProcessDispatcher page |
| `mgcms.engine.zoneAware` | `false` | Engine filters by `dispatched_zone`, respects `pinnedByChef` |
| `mgcms.engine.autoTick.enabled` | `false` | `@Scheduled` cron that calls engine for each confirmed zone |
| `mgcms.engine.autoTick.cron` | `0 */5 * * * *` | Cron expression for the auto-tick |
| `mgcms.admission.enforce` | `false` | `/api/admission/check` returns 409 on block (vs. 200 advisory) |
| `mgcms.shift.durationMinutes` | `480` | Shift gross duration fed to PdC capacity calculation |
| `mgcms.shift.breakMinutes` | `30` | Break subtracted from gross to get effective minutes |
| `mgcms.retention.enabled` | `false` | Nightly cron prunes audit tables |
| `mgcms.retention.days` | `7` | Rows older than this many days are pruned |
| `mgcms.retention.cron` | `0 30 2 * * *` | Cron expression for the nightly retention sweep |
| `mgcms.engine.selfHeal.enabled` | `false` | Periodic sweep that logs stuck-PENDING sequences |
| `mgcms.engine.selfHeal.cron` | `0 */10 6-22 * * *` | Cron expression for the self-heal sweep |
| `mgcms.engine.selfHeal.stuckPendingMinutes` | `45` | Minutes a PENDING request must sit before it's flagged |
