# Post-Claude / Codex Summary - Release Logistics, Workbench, CMS-Prod

*Date: 2026-06-01*

This file summarizes the work Codex completed after the Claude Code handoff in the conversation around **"Implement release logistics allocation system"**. It is meant as the single handoff/reference file for what changed, why, and what was verified.

---

## Goal

Finish the remaining release-logistics tasks, align the production strategy document with the new flow, and make both MG-CMS `/processWorkbench` and CMS-Prod show the best 3 series to work on per machine.

The hard recommendation gates are:

- `statusCoupe = Waiting`
- `statusMatelassage = Waiting`
- parent `sequenceStatus IN (RELEASED, STARTED)`
- never recommend `IMPORTED`, `COMPLETED`, `INCOMPLETE`, or `MATERIAL_MISSING`

---

## Main Flow Now

The intended operator/logistics flow is:

1. Logistics uses MG-CMS release advice to choose sequences to release.
2. The release is committed, not only previewed:
   - `suiviplanning.Statu` moves to `Released`
   - MG-CMS sequence lifecycle mirrors to `RELEASED`
   - active roll allocations are written
   - a printable picklist snapshot is saved
3. `/processWorkbench` becomes the process action board:
   - first tab: top 3 series per machine
   - second tab: sequences and material focus
4. CMS-Prod shows the same top 3 MG-CMS recommendations on each machine/table form.
5. When CMS-Prod starts a recommended sequence, MG-CMS can be called through the secured integration path so the lifecycle moves to `STARTED`.

---

## MG-CMS Backend Changes

### Public recommendation endpoint

`GET /api/public/next-series?machine=AA2&limit=3` now uses the same ranked table feed as `/processWorkbench`.

Key files:

- `src/main/java/com/lear/MGCMS/controller/dispatcher/PublicRecommendationController.java`
- `src/main/java/com/lear/MGCMS/services/dispatcher/TableFeedRankingService.java`
- `src/main/java/com/lear/MGCMS/services/dispatcher/TableFeedDto.java`

The response now includes the fields CMS-Prod needs:

- `serie`
- `sequence`
- `machine`
- `machineType`
- `zone`
- `phase = MATELASSAGE`
- `partNumberMaterial`
- `longueur`
- `nbrCouche`
- `requiredLength`
- `validatedMinutes`
- `status`
- `statusCoupe`
- `statusMatelassage`
- `sequenceStatus`
- `lifecycle = WAITING`
- `score`
- `materialInZone`
- `sameRefTissuMounted`
- `fitsTableLength`
- `reasons`

Important ranking behavior:

- Candidates are only built from Waiting/Waiting series.
- Parent sequences must be `RELEASED` or `STARTED`.
- `STARTED` sequences get a strong boost so opened work is finished before new work is opened.
- Same mounted fabric is boosted to reduce heavy roll changes.
- Material on rack in the target zone is boosted.
- Table-fit logic now uses the physical lay length (`longueur`), while `requiredLength` remains total fabric need (`longueur x nbrCouche`).

### Material availability

`MaterialAvailabilityChecker` now checks true rack availability more honestly:

- starts from `ScanRouleau` rack stock by zone
- subtracts active released/advised allocation reservations
- subtracts rolls already mounted on tables when their serial exists in rack stock
- normalizes fabric references by stripping leading `P` and uppercasing

This keeps the recommendation material signal aligned with the release allocation ledger.

### Workbench cache

`WorkbenchCacheService` now includes `tableFeed` in `/api/workbench/data`, so `/processWorkbench` can render the same top-3 recommendation contract without making a separate page-specific call.

### Query/status tightening

`EngineScheduleEntryRepository.findCurrentCoupeQueueForMachine` now filters queue context to `sequenceStatus IN ('RELEASED','STARTED')`.

`CuttingRequestRepository.findActiveDueOnOrBeforeLight` exposes `sequenceStatus` to the table feed ranking projection. It still loads the broader active set for other workbench/live-charge needs, and the strict recommendation filter is applied in `TableFeedRankingService`.

---

## MG-CMS Frontend Changes

### `/processWorkbench`

Key files:

- `src/main/js/components/Layout/Workbench.js`
- `src/main/js/components/Layout/WorkbenchHeader.js`
- `src/main/js/components/Layout/MachineRecommendationsView.js`
- `src/main/js/components/Layout/SequenceFocusView.js`
- `src/main/js/styles/Workbench.scss`

The page was simplified from a control-room style screen into an action board.

Visible tabs are now:

- `Top 3 par machine`
- `Séquences & matière`

Removed from the visible operator path:

- engine control panel
- Gantt view
- shift completion view
- material forecast view
- surplus dispatch controls

The new machine recommendation panel shows:

- machines ordered by soonest idle
- top 3 series per machine
- serie and sequence
- sequence status
- fabric reference
- `longueur`
- `nbrCouche`
- `Coupe Waiting`
- `Matelassage Waiting`
- score
- top reasons
- empty state when no Waiting/Waiting `RELEASED` or `STARTED` candidate exists

### `/tableFeed`

The standalone table feed text was aligned with the same top-3-per-machine vocabulary and hard-gate empty state.

---

## CMS-Prod Changes

Key files:

- `D:\work\LEAR\CMS-Prod\src\main\js\components\Layout\Form.js`
- `D:\work\LEAR\CMS-Prod\src\main\js\components\Layout\FormMix.js`
- `D:\work\LEAR\CMS-Prod\src\main\js\styles\Form.scss`

CMS-Prod now fetches:

```text
{MGCMS_SERVER_URL}/api/public/next-series?machine={table}&limit=3
```

The recommendation panel now shows:

- title: `Top 3 MG-CMS à lancer`
- subtitle: `Machine {table} · Waiting/Waiting · RELEASED/STARTED`
- serie rank
- sequence
- sequence lifecycle
- material
- length and layers
- `Coupe Waiting`
- `Mat. Waiting`
- score
- top reasons

The visible actions were kept intentionally small:

- `Charger`
- `Imprimer` / `Réimprimer`

The previous print-status and quality-code flow remains intact.

---

## Release Logistics / Picklist Work Completed

The remaining logistics release tasks from the handoff were completed:

- `[MG-CMS] Picklist: select -> commit -> printable`
- `[MG-CMS] Surface materialMissingSuggested in /processWorkbench`
- `[Integration] Auth for CMS-Prod -> MG-CMS /sequence/{seq}/start`

The release flow now stores a picklist snapshot and exposes the printable picklist after commit.

`materialMissingSuggested` is visible in both the logistics release context and `/processWorkbench`, so production can identify sequences that should not be opened silently when material is unavailable.

CMS-Prod can call the MG-CMS sequence start integration using the configured integration authentication instead of relying on the public endpoint for mutations.

---

## Strategy Document Updated

`docs/production-flow-and-strategy.md` was updated to match the implemented flow:

- release commit and picklist snapshot are now described as implemented
- allocation ledger and true rack stock are described as current baseline
- `materialMissingSuggested` is documented as surfaced in release and workbench views
- CMS-Prod `STARTED` integration auth is documented as in place
- the current strategic target is the shared best-3-per-machine recommendation contract

The document now names the open items clearly:

- workbench cleanup/action-board focus
- R100 replenishment signal
- close-loop consumption vs allocation
- logistics menu gate decision

---

## ScanCoupe / CMS-Prod Context

Claude had already made related `scanCoupe` changes around rack visibility/delete behavior. Codex checked those changes and verified the scanCoupe project with offline Maven `test-compile` earlier in the conversation.

CMS-Prod `Form.js` and `FormMix.js` were updated after that so both normal and mix production forms show the same MG-CMS top-3 recommendations.

---

## Final Finish Pass

Codex made one additional logic fix during the final pass:

- table-fit scoring no longer compares `longueur x nbrCouche` to the physical table length
- table-fit scoring now compares only `longueur` to `ProductionTable.tableLength`
- the API still returns `requiredLength = longueur x nbrCouche` as total fabric need
- a regression test was added to protect this distinction

Files:

- `src/main/java/com/lear/MGCMS/services/dispatcher/TableFeedRankingService.java`
- `src/main/java/com/lear/MGCMS/services/dispatcher/TableFeedDto.java`
- `src/test/java/com/lear/MGCMS/services/dispatcher/TableFeedRankingServiceTest.java`

---

## Verification Run

MG-CMS:

```text
.\mvnw.cmd -o test -Dtest="PublicRecommendationControllerTest,MaterialAvailabilityCheckerTest,TableFeedRankingServiceTest" -DfailIfNoTests=false
```

Result: passed, 8 tests, 0 failures.

```text
npm run prod
```

Result: webpack compiled successfully.

CMS-Prod:

```text
.\mvnw.cmd -o test-compile
```

Result: build success.

```text
$env:NODE_OPTIONS='--openssl-legacy-provider'; npx webpack --mode production
```

Result: webpack compiled successfully. Warnings were pre-existing/normal for this project: stale Browserslist data, Sass legacy JS API, and large bundle size.

Diff hygiene:

```text
git diff --check
```

Result: clean in MG-CMS and CMS-Prod.

---

## Not Yet Live-Verified

No live browser/database smoke test was completed in this final pass. The next high-value manual check is:

1. Start MG-CMS against the target database.
2. Open `/processWorkbench`.
3. Confirm each active machine shows only Waiting/Waiting candidates from `RELEASED` or `STARTED` sequences.
4. Call `/api/public/next-series?machine=AA2&limit=3` and confirm the same candidates appear in the same order.
5. Open CMS-Prod on machine `AA2` and confirm the top 3 panel matches the public endpoint.
6. Use `Charger` and `Imprimer` on one recommendation.
7. Start the serie/sequence from CMS-Prod and confirm MG-CMS lifecycle moves to `STARTED`.

---

## Known Follow-Ups

- The recommendation material signal currently confirms positive rack availability after reservations/on-table deductions. It does not yet require remaining rack meters to be greater than or equal to `requiredLength`; the logistics release advisor has the stronger quantity logic.
- R100 should be exposed separately as a replenishment signal: "not on rack but exists in magasin" versus "not in magasin either".
- Decide whether the dashboard `Logistique` menu gate should include `ROLE_VALID_QN_LOGISTIQUE`, matching the route permission.
- Add a live endpoint/UI smoke test once a representative production database is available.
