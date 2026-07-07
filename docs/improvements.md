# Cross-Repo Improvement Backlog

*Written 2026-07-02, consolidating a 4-repo code investigation (MG-CMS, CMS-Prod, scanCoupe, PLS)
that combined static reading, a second-pass verdict check on every finding, and a documentation
audit. This file is the current, actively-referenced improvement backlog — it collects every
ambiguous finding and improvement suggestion the sweep surfaced, in one place, organized by repo
and area. `docs/recommendations.md` remains the historical DONE/PARTIAL/OPEN tracker for the
2026-06-11 indexing/import/roll-tracking review and is not superseded by this file — the two cover
different scopes. Confirmed bugs and the clearly-fixable disputed findings from this sweep are
being fixed in code as this document is written; see "Fixed in this pass" near the bottom so you
don't re-report them. Everything else here is either a product/architecture decision or a
lower-priority improvement that needs a human call before code should change.*

---

## MG-CMS

### Logistics Release (`mgcms-logistics`)

`/logisticsRelease` (`LogisticsRelease.js` + `LogisticsReleaseController`/`LogisticsReleaseService`)
is a well-built staged advisor+commit flow — zone charge balancing, material-availability advice,
an allocation ledger, and a printable picklist. Its biggest adoption blocker isn't the page itself:
the Dashboard's "Logistique" menu section (the only place `/logisticsRelease` is linked anywhere in
the app) is gated to `ROLE_ADMIN`/`ROLE_VARIANCE` only, while the route and every backend endpoint
also authorize `PROCESS`, `CHEF_DE_ZONE`, `CHEF_EQUIPE` and `VALID_QN_LOGISTIQUE` — those shop-floor
roles have zero navigational path to the tool (already flagged as a known, unresolved gap in
`docs/production-flow-and-strategy.md` and `docs/recommendations.md`, and now also confirmed as a
clear fix — see "Fixed in this pass"). Once on the page, the operator flow is reasonably lean, but
the WAIT/zone-saturation recommendation is purely advisory: nothing in the backend `commit()` path
or the frontend checkbox blocks committing a WAIT-recommended sequence, so the charge-balancing the
tool exists for can be silently bypassed. The picklist snapshot persisted specifically for
reprinting is unreachable (no GET endpoint), and the frontend's own refresh/date-change logic wipes
the only in-memory copy, so any interruption after a successful release permanently loses the
printable deliverable. Beyond the confirmed bugs, there's no history/audit view of past
releases/picklists anywhere in the app, and the zone recommendation's dependence on chef
confirmation vs. a silent machine-status fallback is invisible to the operator.

#### Feature flag `mgcms.dispatcher.enabled` silently gates the entire live floor stack, not just the shelved optimizer
*Impact: medium · Effort: S*

`DispatcherProperties.java` documents `mgcms.dispatcher.enabled` as "master switch for the whole
dispatcher subsystem", implying the continuous-dispatch optimizer that `AGENTS.md`/`CLAUDE.md` say
was shelved 2026-05-31. In practice it also gates `LogisticsReleaseController`, `TableFeedController`,
`ZoneConfirmationController`, `FloorStateController` and `WorkbenchController` — i.e. every
currently-live production-floor screen including `/logisticsRelease`. It's enabled in
`application-tanger.properties`/`application.properties` but absent (defaults false) from
`application-tunisie.properties`. Anyone toggling this flag off believing they're disabling
dead/shelved code would silently 404 the actively-used logistics release, table feed and production
floor pages. Rename/split the flag or update its javadoc to make the scope explicit.

#### No escalation path for material-missing sequences from within `/logisticsRelease`
*Impact: medium · Effort: M*

`LogisticsRelease.js:496-503` disables the row checkbox for `materialMissingSuggested` rows with
only a tooltip ("Matière insuffisante: à passer MATERIAL_MISSING") — there is no button on this
screen to actually perform that transition. The operator must navigate elsewhere (e.g.
`SequenceRectification.js`) to act on exactly the situation this tool surfaces, which is a dead-end
in the primary workflow.

#### Released sequences vanish from the page with no recent-history view
*Impact: high · Effort: M*

`LogisticsReleaseService.build()` only returns IMPORTED candidates, so the instant a sequence is
committed it disappears from `/logisticsRelease` entirely. There is no "recently released, awaiting
CMS-Prod start" panel and no way to confirm from this screen that what was just committed actually
reflects reality in `suiviplanning`/`CuttingRequestData`. Combined with the missing
picklist-history endpoint (see "Fixed in this pass"), logistics has effectively zero
after-the-fact visibility into what it released. Add a small recent-activity strip (last N
picklists/releases for the shift) directly on the page.

#### Zone recommendation quietly depends on chef zone confirmation with no UI indication of which data source was used
*Impact: medium · Effort: S*

`LiveChargeService` (`src/main/java/com/lear/MGCMS/services/dispatcher/LiveChargeService.java:521-539`)
and `ActiveMachineResolver.activeMachines()` fall back to `EtatMachineHistorique` M-status when no
`ShiftZoneConfirmationMachine` confirmation exists for the (date, shift, zone) triple.
`/logisticsRelease`'s zone chips (`LogisticsRelease.js` `renderZones()`) never indicate whether a
zone's `activeMachines` count came from an explicit chef confirmation
(`ActiveMachineResolver.isZoneConfirmed()`) or the M-status fallback, so a logistics user has no way
to judge whether a recommendation rests on fresh, deliberate input or on stale machine-status data.
Surface a small "confirmed"/"inferred" badge per zone.

#### `build()` is recomputed a third time inside `commit()` purely to guard against stale UI
*Impact: low · Effort: M*

`LogisticsReleaseService.commit()` (line 732) calls the full, expensive `build(date, shift)` again
from scratch to revalidate the selection, on top of the same call already made once by the stage-2
GET `/candidates` that populated the page. On an already 3-stage sequential page load
(`LogisticsRelease.js` `LOAD_STEPS`), this adds visible latency at the exact moment (clicking
"Confirmer la sortie") the operator is trying to finish. Consider a short-TTL cache of `build()` per
(date, shift) shared between the last GET and the following commit, invalidated by any intervening
write.

#### Magasin shortage detection depends on a hardcoded R100 status literal with no override path
*Impact: medium · Effort: M*

The recap/overview magasin lookup is filtered to rolls flagged status `'AVAIL2'` in R100 (message
surfaced at `LogisticsRelease.js:836`, "Aucun rouleau AVAIL2 en magasin"). If warehouse stock-status
data entry lags — a recurring theme in this codebase per the roll-lifecycle docs (magasin → rack →
spreading → consumed) — physically present rolls not yet flagged AVAIL2 make recap show
"Insuffisant" and disable "Confirmer la sortie" (`LogisticsRelease.js:981-996`) with no manual
override or escalation path inside the tool, forcing users to bypass it entirely for that release.

---

### Process Workbench / Next-Series Ranking (`mgcms-workbench`)

The `/api/public/next-series` path (`PublicRecommendationController` → `TableFeedRankingService`
`recommendForMachine` → `compute` → `rankForTable`) and the `/processWorkbench` frontend
(`Workbench.js` + `WorkbenchContext.js`, served by `WorkbenchCacheService`) are internally consistent
and largely correct. The banded-lexicographic score is mathematically sound (verified: no
cross-tier leak, e.g. max lower-tier sum 651 < 5000, 111 < 500), the negative penalties only lower
scores so they cannot invert tier order, and the final comparator is a clean, transitive
lexicographic chain (no "Comparison method violates its general contract" risk). The kill-switch
`mgcms.nextserie.waitage.enabled=false` cleanly degrades to prior behavior. Fit exclusion works
correctly and frontend contracts match. The remaining gaps are: type-matching is bypassed when a
serie's `machine` field is null (now fixed — see "Fixed in this pass"), a routing/type-match
tolerance mismatch that can silently drop dirty-string series, and stale docs/comments claiming
there is no wait-age tier (the Tier D anti-starvation term shipped in commit f1f407d, 2026-06-30).

#### Routing uses strict machine-type match while the per-table filter is tolerant — dirty strings are silently dropped
*Impact: medium · Effort: S*

`LiveChargeService.resolveTargetZone` (`D:/work/LEAR/MG-CMS/MG-CMS/src/main/java/com/lear/MGCMS/services/dispatcher/LiveChargeService.java:817` and `:823`)
matches the serie's machine type with exact `mbt.containsKey(machineType)` (no trim,
case-sensitive), whereas `TableFeedRankingService.rankForTable` (`TableFeedRankingService.java:434`,
`sameMachineType`) matches with trim + case-insensitive on purpose "so a stray trailing space or
casing difference must not silently drop every candidate." Because routing runs first and returns
null (candidate skipped) on any casing/whitespace mismatch, the tolerant per-table filter can never
see those series — the tolerance is partly defeated. Current: a serie with machine `'lectra '` or
`'Lectra'` vs table type `'LECTRA'` is dropped at routing. Proposed: normalize (trim + upper) both
sides of `resolveTargetZone`'s lookup, or key `machinesByZoneByType` by a normalized type, so routing
and per-table matching agree.

#### Tier C (date pressure) compares dueDate to `LocalDate.now()` instead of the requested `date`
*Impact: low · Effort: S*

In `TableFeedRankingService.rankForTable`, overdue/due-today are computed against
`LocalDate.now()` (`TableFeedRankingService.java:454` and `:457`), but the candidate set is filtered
relative to the requested plant bucket via `findActiveDueOnOrBeforeLight(date, shift)`. The Process
Workbench header (`WorkbenchHeader.js`) exposes a date picker, so a chef planning tomorrow's shift
gets candidates due up to tomorrow, yet "due today" is measured against wall-clock today —
tomorrow's due series never receive the +5000 due-today boost and today's become "overdue." On the
live path (date=today) this is correct and is what the unit tests assume. Proposed: pass the compute
`date` into `rankForTable` and compare `dueDate` against it instead of `LocalDate.now()`, so date
pressure is relative to the viewed bucket.

#### Stale in-code comment claims there is no wait-age tier
*Impact: low · Effort: S*

`TableFeedRankingService.rankForTable` opens with a "ponytail:" comment
(`TableFeedRankingService.java:419-423`) stating "no wait-age tier — dueDate is the only starvation
proxy … a serie that keeps losing on affinity can starve indefinitely", immediately above the
now-live Tier D anti-starvation block (lines 463-474) fed by `releaseProxyAt`. The comment directly
contradicts the code. Proposed: delete or rewrite the comment to describe the shipped Tier D (floor
500 + capped ramp, threshold 24h, kill-switch `mgcms.nextserie.waitage.enabled`).

#### Tier D ramp is computed on total wait-age, not hours beyond the threshold
*Impact: low · Effort: S*

The constant comment (`TableFeedRankingService.java:93`) says `W_AGE_RAMP_PER_HOUR` is "+ per hour
waited beyond threshold", but the ramp uses total `ageHours`: `ramp = min(ageHours, 400) * 0.1`
(lines 469-471). At exactly 24h a serie gets 500 + 2.4 rather than 500 + 0. It is still monotonic and
bounded (≤540), so no cross-tier leak, but it does not match the stated "beyond threshold" semantics.
Proposed: ramp on `(ageHours - AGE_THRESHOLD_HOURS)` if the beyond-threshold semantics are intended,
or fix the comment.

#### Public next-series returns empty for an operator at a table that was never shift-confirmed and is currently empty
*Impact: medium · Effort: M*

`compute()` only emits a table when it is in `ActiveMachineResolver.activeMachines` OR physically in
use (a roll mounted or a cut queued) — `TableFeedRankingService.java:334-336`.
`recommendForMachine` (and thus `/api/public/next-series`) finds nothing for that table, so a
CMS-Prod operator standing at an idle, unconfirmed table sees zero recommendations even when valid
Waiting/Waiting released series exist in the zone. This is intentional today. **Open question:** add
a fallback in `recommendForMachine` that, when the requested machine/table is not present in the
DTO, still ranks the zone's candidates for that table (looked up by name/type) so the public
contract never starves a real operator? (see "Open product questions" below)

#### Next ranking upgrades toward "the best serie" (setup-change cost, material continuity, deadline gradation)
*Impact: high · Effort: L*

Open questions for the next iteration of `TableFeedRankingService.rankForTable`, all anchored to
existing signals: (1) Setup-change cost — Tier E currently rewards only same `mountedRef`
(`TableFeedRankingService.java:477-483`). Consider a graded penalty for a roll change proportional to
changeover effort (e.g. different `refTissus` width/colour family) using
`MaterialAvailabilityChecker` data, rather than binary same/none. (2) Material continuity with what
is already staged — the candidate already knows `targetZone` material status (`materialByZone`, line
306-309); consider boosting series whose full `requiredLength` (`longueur × nbrCouche`) is
satisfiable by rolls already on-rack, not just presence of the ref. (3) Deadline gradation — Tier C
is binary overdue/today (lines 452-461); consider a bounded continuous term inside the date band
scaled by days-until-due (still capped below the tier gap) so "due today" vs "due in 1 day" vs "due
in 3 days" differentiate without leaking into Tier B. (4) Cutter continuity — Tier G (lines 497-502)
only rewards raw minutes; consider preferring series that keep the SAME downstream CNC/cutter fed
(via `MachineQueue` lookahead in `cutQueueEndByMachine`, lines 292-297) to reduce cutter idle between
series.

---

### Plan de Charge (`mgcms-plandecharge`)

Plan de Charge's shift-mapping (`getShiftTimes`, `PlanDeChargeService.java:136-160`) correctly
follows the plant convention (1=night 21:55-05:45, 2=morning, 3=afternoon), unlike
`OrdonnancementService`'s known inverse legacy mapping, and division-by-zero is consistently
guarded throughout both the Java service and `PlanDeCharge.js`. `CapaciteInstalleeService.getEffective()`'s
explicit→rule→default precedence and rule specificity/tie-break logic look correct. The
part-number report's perimeter-share math is sound and falls back to an equal split when no
perimeter is cached, keeping the sequence total intact. The one serious defect found (see "Known
issues deferred" — it needs a product decision, not just a code fix) is that the load-percentage
numbers actually shown to planners apply no efficiency correction anywhere, contradicting the
on-screen tooltips. On the design side, the Détails de charge table is reasonably explainable
(per-cell tooltips, click-to-filter drill-down, CSV/Excel export), but the tooltip copy is currently
misleading given that defect, and the KpiChargeMachine dashboard duplicates the same metric with a
different, non-configurable shift-length assumption, creating a second source of disagreement.

#### `PlanDeChargeService.calculateShiftLoad()`/`ShiftLoadCalculation` persistence is orphaned and has drifted from the live grid
*Impact: medium · Effort: M*

`PlanDeChargeController` `POST /api/planDeCharge/calculate` (wired to the "Recalculer" button,
`PlanDeCharge.js:1560-1575`) computes and saves `ShiftLoadCalculation` rows using a DIFFERENT formula
(`availableTime = rendementSum × configuredMinutes × efficiencyTargetPct/100`,
`PlanDeChargeService.java:1024-1059`) than the live grid the user is looking at
(`getShiftProductiveCapacityMinutes`, raw, no efficiency at all). Nothing in the frontend reads these
persisted rows back (`loadCalculations` state at `PlanDeCharge.js:51` stays empty; grep across
`src/main/js` confirms `/api/planDeCharge/search`, `/loadCalculations`, `/chargeSummary`,
`/detailedSeries`, `/aggregatedCuttingTime`, `/aggregatedCuttingTimeWithStatus` are never called).
Clicking "Recalculer" therefore silently writes to a table that has no consumer besides itself, and
its formula will keep drifting further from the live one. Either delete the dead
calculate/`ShiftLoadCalculation` path or make it the single source of truth the frontend reads.

#### `KpiChargeMachine.js` hardcodes shift duration instead of using CapaciteInstallee
*Impact: medium · Effort: S*

`KpiChargeMachine.js:20,187` uses a fixed `SHIFT_DURATION_MINUTES=460` for its "Chg %" denominator
(`availableTime = machinesAvailable * SHIFT_DURATION_MINUTES`) and never fetches
`/api/capaciteInstellee/...` at all (its `loadData()` only calls
machines/currentShift/search/aggregatedCuttingTime/aggregatedCuttingTimeWithStatus).
`PlanDeCharge.js`, by contrast, resolves the configured per-shift minutes from `CapaciteInstallee`
(`getConfiguredShiftMinutes`/`capEntry`). For any (date, shift, groupe) where an admin has configured
a non-default `tempsTotalParMachine` via the Capacité Installée editor (`PlanDeCharge.js` ~row 960),
the KpiChargeMachine dashboard and the main Plan de Charge screen will silently show different Chg%
numbers for the exact same shift.

#### `PerimetreService.ensureRequestPerimetre()` permanently caches a 0.0 perimeter on first (possibly transient) resolution failure
*Impact: low · Effort: S*

`PerimetreService.java:261-291`: `ensureRequestPerimetre()` only recomputes when `anyNull` is true
(some `CuttingRequestPartNumber.perimetre` is still null). If `perimetrePerPartNumber()`/`aggregate()`
fails to resolve a part number on first try (cut file temporarily unreachable, D,6 token not yet
matched via `PartNumberCorrespendance`, etc.), the code writes `0.0` via `per.getOrDefault(pn, 0.0)`
(line 286) — which is not null — so the `anyNull` guard will never re-trigger a recompute for that
request, and `getPartNumberCuttingTimeReport`'s per-part-number split
(`PlanDeChargeService.java:695-712`) will permanently attribute 0% of that sequence's cutting time to
that part number even after the underlying file/mapping issue is fixed.

#### `calculateShiftLoad`'s denominator compounds two different "efficiency" concepts
*Impact: low · Effort: S*

`PlanDeChargeService.java:1024-1059`: `availableTime = rendementSum` (Σ per-machine
`ProductionTable.efficience`/100) `× configuredMinutes × (CapaciteInstallee.efficienceTarget/100)`.
This multiplies a per-machine mechanical efficiency and a separate global "efficience global
coupe/laser" target together, so a machine at 90% efficience combined with a 90% efficienceTarget
yields an effective 81% capacity reduction. If/when this formula is wired back into the UI (see the
orphaned-persistence item above), clarify with the process team whether these two factors are meant
to compound multiplicatively or whether one should supersede the other.

---

## CMS-Prod

No `improvements`-backlog items or ambiguous findings were surfaced for CMS-Prod as its own repo in
this sweep pass (the CMS-Prod code referenced elsewhere — e.g. `Form.js`'s roll-inventory lookup — is
discussed under PLS's roll-consumption comparison below, since it's the model PLS is being compared
against). CMS-Prod's own findings from this pass were purely documentation staleness (blade-wear
alerts, cut-section capture, sensor tolerance-interval alert, QN per-station routing,
material-readiness warnings, top-3 recommendation freshness), which have already been folded into
`docs/COUPE.md` and `docs/MATELASSAGE.md` directly — see the docIssue fixes noted in this repo's
commit history for 2026-07-02.

---

## scanCoupe

No `improvements`-backlog items or disputed findings were surfaced for scanCoupe as its own repo in
this sweep pass beyond documentation staleness (the Retour/Audit tabs and in-flight save indicators,
already folded into `CLAUDE.md` and `docs/schema.md`). The Audit tab's T0M\*/T0R\* magasin-location
filter, previously flagged as an unverified domain assumption, is now RESOLVED — see
**"scanCoupe T0M\*/T0R\* magasin-location filter"** under "Open product questions" below.

---

## PLS

### Placement Generation (`pls-placement`)

PLS generates cutting placements one PIECE per file. The path: each SubDemande line auto-resolves a
source marker file `placement` + pattern digit `pnEmp` + cover `pn` + `empNumb` via CMS
matlassage/CTC lookups (`DemandeDetail.js:190-290`). Recut (non-CAD) or CAD then clicks "générer
placement", which GETs `/api/cutfile-generate/{placement}/{pnEmp}/{plsId}/{ind}`
(`DemandeDetail.js:1328` and `:1358`). That endpoint is `TestController.generatePlacement` →
`generateFile` (`TestController.java:248-368`): it opens the source marker, locates the single
matching empiècement (`iEmp`), reconstructs the header geometry for that one emp, and copies ONLY
that one piece's body block relabeled "L,1" into a new windows-1252 cut file, returning the filename
stored into `subDemande.placementEmp`. So the single-piece limit is structural: `generateFile` emits
exactly one `L,1` piece, is keyed per line index `ind`, and the UI calls it per line — there is no
batch/"generate all" action. The verification sibling `CadVerificationController` does a per-piece
(per line) parse of the same grammar, confirming the one-piece model end to end. The parsing/format
code works in production but is brittle (string-index arithmetic, prefix matching, `Math.log`
digit-counting), and `generateFile` lacked the not-found guard its read-only twin `getInfoPlacement`
has — which is now fixed (see "Fixed in this pass"). The cut-file grammar does support multiple
pieces per marker (real production markers contain many), so multi-piece generation is feasible but
requires renumbering the N-blocks and L-blocks and recomputing base/laize when merging.

#### Multi-piece placement generation (core capability)
*Impact: high · Effort: L*

Files: `TestController.java` `generateFile`/`generatePlacement`
(`D:/work/LEAR/PLS/src/main/java/com/lear/PLS/controller/TestController.java:248-368`) and
`DemandeDetail.js` buttons (`D:/work/LEAR/PLS/src/main/js/components/demande/DemandeDetail.js:1327-1364`).
CURRENT: `generateFile` copies exactly ONE empiècement into the output marker (writes 'L,1' + that
single piece's D/P/s lines at lines 350-357) and rebuilds the header to hold only that one N-block
(lines 336-343). PROPOSED: accept a LIST of pieces (each = {placement, pnEmp, pn, empNumb}) that
share the SAME source marker file, and emit one cut file containing all of them. Implementation: (1)
run the existing D,1/D,6/D,7 match loop once per requested piece to collect each iEmp; (2) in the
body copy loop, for each matched L,<iEmp> block emit sequential labels L,1, L,2, … instead of always
L,1, and copy each block's P,/D,/s, lines; (3) rebuild the header by concatenating the *N<iEmp>*
geometry segment of each included piece under renumbered *N1*,*N2*,… between *N1 and *Q (the header
today only splices one segment). Base pixel/cm dims and laize stay from the shared source marker so
they are valid for all pieces. Constraint to enforce: only merge pieces from the SAME source
`placement` file (same coordinate base/laize) — cross-marker merging needs coordinate re-basing and
is out of scope for v1. This removes the structural one-piece limit.

#### 'Générer tous les placements' batch action for a whole Demande
*Impact: high · Effort: L*

Files: new endpoint in `TestController.java` + a button in `DemandeDetail.js` near the per-line
"générer placement" (`D:/work/LEAR/PLS/src/main/js/components/demande/DemandeDetail.js:1326-1337`)
and/or `DemandeTable.js` recut section (~:966). CURRENT: operator clicks "générer placement" once per
sub-demande line; there is no whole-demande action. PROPOSED: add
`POST /api/cutfile-generate-all/{plsId}` taking the Demande, filtering accepted "Not found" lines
needing a placement, GROUPING them by source `placement` file (plus material/laize as a safety key),
calling the new multi-piece generator once per group, and writing `subDemande.placementEmp` for
every line in the group to the group's output filename. Frontend adds a single "générer tous les
placements" button that calls it and refreshes the lines. Naming: current per-line filename
`'__'+ind+last5(plsId)+'-NS'` (`TestController.java:257`) must change to a per-group name to avoid
overwrites (e.g. include the source placement or a group index).

#### cutfile-generate is an unauthenticated GET with no path-traversal guard
*Impact: medium · Effort: S*

File: `TestController.java:248-291`. `SecurityConfig` permits all `GET /api/**` (`BACKEND.md` shows
`antMatchers(HttpMethod.GET, "/api/**").permitAll()`), so `/api/cutfile-generate` is reachable
unauthenticated. `placement` is used directly in `FileInputStream` paths and `ind`+`plsId` are used
to build the `FileOutputStream` write path, with NO validation. The sibling
`CadVerificationController` deliberately added `isSafePlacementName` (rejects '/','\\','..',':') for
exactly this reason. PROPOSED: apply the same `isSafePlacementName` check to `placement`, and
sanitize `ind`/`plsId` (must be numeric/whitelisted) before composing the output filename, to prevent
arbitrary file read/write via path traversal.

#### CAD regenerate button is disabled for grouped lines
*Impact: low · Effort: S*

File: `DemandeDetail.js:1357` — the CAD refresh/regenerate button only renders when
`concatElem[2] === 1` (a group of exactly one line sharing the same `placementEmp`). CURRENT: when
several lines already share a `placementEmp` (a group), CAD cannot regenerate. Once multi-piece
generation exists this gate should be revisited so a group can be regenerated as a single
multi-piece file rather than blocking it.

#### Piece match uses `startsWith("D,1")` without trailing comma
*Impact: low · Effort: S*

File: `TestController.java:142` and `:303` use `liste[i].startsWith("D,1")` which also matches
`D,10`..`D,19` lines, whereas `CadVerificationController.java:240` uses the stricter
`startsWith("D,1,")`. Today D-codes appear to be single digit (D,6/D,7) so it is latent, but it
should be tightened to `"D,1,"` to match the intended grammar and the verifier.

#### Header geometry extraction relies on `Math.log` digit-counting and prefix `indexOf`
*Impact: medium · Effort: M*

File: `TestController.java:204-208` (`getInfoPlacement`) and `:338-343` (`generateFile`). The code
skips past `'*N<iEmp>*'` using `+3+(int)(Math.log(iEmp)/Math.log(10))` (digits-1), which is off by
one on exact powers of ten (`log10(100)` can floor to 1). It is currently absorbed downstream (points
are re-split on `'*'` and only `'X..'` tokens are kept), but it is fragile. Also the existence check
`indexOf("*N"+(iEmp+1))` (no trailing '*') differs from the extraction
`indexOf("*N"+(iEmp+1)+"*")`: if e.g. `*N20*` exists but `*N2*` does not, the check passes yet the
extraction returns -1 → substring throws. PROPOSED: replace digit-count arithmetic with
`String.valueOf(iEmp).length()` and make the existence check use the same '*'-delimited token as the
extraction.

#### Open product questions for multi-piece placements
*Impact: medium · Effort: S* — *see "Open product questions" rollup below*

Explicit decisions needed before building: (1) Grouping key — merge only pieces from the same source
marker `placement`, or also across markers of identical laize/material? (2) Quantity — today
quantity is satisfied by `nbrCouche` (layers), not by repeating a piece; does "multiple pieces per
placement" mean distinct empiècements only, or also repeating one piece to meet `quantite`? Lines in
a group may have different `nbrCouche`, which a single marker cannot express per-piece — decide
whether differing `nbrCouche` forces separate placements. (3) Machine limits — max marker width
(laize) and length constraints that cap how many pieces fit. (4) Whether CAD (`envoyerCAD`) flow and
recut flow should both get multi-piece, or recut only.

---

### Roll Consumption (`pls-roll`)

CMS-Prod's `Form.js` (`D:/work/LEAR/CMS-Prod/src/main/js/components/Layout/Form.js`) is the real
roll-consumption engine: the operator scans a roll id (`idRouleau`, must start with "S"),
`handleRouleauSearch` (line 6554) calls `GET /api/cuttingRequestSerieRouleauInfo/idRouleau` which
returns the roll's supplier lot, available metrage (retour) and laize; a `strictUsingRestRollFirst`
config gate (`getRestRouleau`, line 6494) forces operators to consume leftover "rest" rolls of the
same material before mounting a fresh one (overridable by quality validation or a `ROLE_VARIANCE`
roll validation); mounting a roll writes `SerieRouleauTemp` (roll-on-table) and deletes it from the
rack (`ScanRouleau`). Consumption is modeled per roll as `nbrCouche × longueur` + overlaps across a
list (`cuttingRequestSerieRouleaus`), and on save (`/api/cuttingRequestSerieInfo/matelassage`,
controller line 457) the leftover (`retour + confirmRetour`) is written back to `ScanRouleau` as a
new rest-roll label at the zone's rest location, or the roll is deleted if fully consumed. **PLS has
NO equivalent traceable consumption.** In PLS the "consumption" is declarative: on a SubDemande the
recut operator sets `stock` (Found/Not found), `resteRouleau` (Oui/Non), `total` (meters) and
`placementEmp`; `startWorking` (`DemandeController` line 738) routes `resteRouleau=Non` lines to
Variance (which sets `demandeVariance` + `zoneRouleau`) then Matelassage; there is no roll id, no
metrage lookup against inventory, no decrement, no `SerieRouleauTemp` mount, and no write-back to
`ScanRouleau`/`StockStatusReport`. The nearest PLS analog is `Production.js`
(`D:/work/LEAR/PLS/src/main/js/components/production/Production.js`), which prints a rest ticket:
the operator manually types `labelId` (S...), `lotNr` (H...), `initQuantity` (roll length) and the
app derives `quantity`=leftover and `quantitePLS`=consumed, then POSTs a single `ProdTicket` per
(pls, reftissu). This is why consumption is "still bad": no inventory linkage, no "use rests first"
enforcement, only one roll per material line (`ProdTicket` dedups on pls+reftissu and locks the form
via `oldInfo`), no partial/multi-roll support, and `total` is a free string. `RapportRestRouleau` is
only a cost/quantity report generated at transport-close, not a real remnant record.

#### Adopt Form.js roll-inventory lookup in PLS (replace manual initQuantity typing)
*Impact: high · Effort: L*

Files: PLS `Production.js` (labelId/initQuantity inputs, lines 419-525) and a new PLS backend
endpoint mirroring CMS-Prod `CuttingRequestSerieRouleauInfoController`
`/api/cuttingRequestSerieRouleauInfo/idRouleau`. Current behavior: the operator manually types the
roll's Label ID (S...), Lot Nr (H...) and Quantite initiale; nothing verifies the roll exists or how
much material it actually has, and `quantity`(rest)=`initQuantity`-`quantitePLS` is computed off the
typed number. Proposed: on scanning `labelId`, call a lookup (against the same CMS roll/rest source
used by `Form.js` `getRestRouleau`: `cuttingRequestSerieRouleauInfo/rouleauRapport` +
`query/plsRest` + `serieRouleauTemp` + `stockStatusReport/verifyInLocation`) to auto-fill `lotNr`,
laize and available metrage, so `initQuantity` is validated not typed. This gives PLS the same
traceability CMS-Prod has.

#### Enforce 'use rest rolls first' in PLS like strictUsingRestRollFirst
*Impact: high · Effort: M*

Files: PLS `Production.js` reftissu/labelId handlers and `DemandeDetail.js` recut section
(`resteRouleau` toggle, lines 1300-1318). CMS-Prod `Form.js` `getRestRouleau` (line 6494) blocks
mounting a fresh roll while same-material rests (retour≥1, not currently mounted, not in magasin)
exist, listing which table/how many meters, gated by an `appConfiguration` flag (line 6331). PLS
today only asks a yes/no `resteRouleau` with no verification that an actual rest exists or that it
was used. Proposed: when `resteRouleau=Oui` require selecting a concrete rest roll (id + available
meters) from the PLS rest inventory; when the operator tries `resteRouleau=Non` while rests exist,
warn/block with the same list, behind a PLS `appConfiguration` flag so it can be relaxed per site.
**Open product question** (see rollup below): should this be a hard block (like CMS-Prod) or an
advisory warning for PLS, and who may override (variance role vs supervisor)?

#### Write real remnant records instead of only a cost report
*Impact: high · Effort: L*

Files: PLS `ProdTicketController` (save, line 47), `RapportRestRouleauController`,
`DemandeController.transport` (line 1163). CMS-Prod, on save, writes the leftover back to
`ScanRouleau` as a new rest-roll label at `zone.getRestLocation`
(`CuttingRequestSerieInfoController` line 502-515) so the remnant re-enters inventory and can be
found again. PLS only prints a barcode ticket (`Production.js` modal) and, at close, writes a
`RapportRestRouleau` row that is purely a quantity/price report (`RapportRestRouleau.java` has no
roll id, no lot, no location). Proposed: when a PLS roll is consumed with a leftover, persist the
remnant into the shared roll/rest inventory (`ScanRouleau` or the `plsRest` source `Form.js` already
reads) keyed by `labelId`+lot+meters+location, so PLS rests become consumable by both PLS and
CMS-Prod. **Open product question** (see rollup below): should PLS remnants live in the same
`ScanRouleau`/`plsRest` tables CMS-Prod uses, or a PLS-only rest pool?

#### Allow multiple rolls (and partial consumption) per material line
*Impact: high · Effort: L*

Files: PLS `Production.js` (`ProdTicket` is one-per-pls+reftissu; backend
`ProdTicketController.save` line 54-57 rejects a second ticket for the same pls+reftissu, and the
frontend loads it back as `oldInfo=true` disabling all fields at lines 373-377/423/449/484).
CMS-Prod models `cuttingRequestSerieRouleaus` as a LIST, letting one serie be spread across many
rolls, each with its own `nbrCouche`/overlaps, and computes remaining need vs consumed across rolls
(`Form.js` `calculateSerieStatistics` ~line 419+). Proposed: let a PLS material line accept N rolls
until `quantitePLS` is satisfied, tracking cumulative consumed vs required and showing the shortfall,
instead of one locked ticket. This is the primary "more flexible" axis. **Open product questions**
(see rollup below): (a) partial consumption — can one PLS line be fulfilled by 2+ rolls scanned over
time; (b) multiple rests — can more than one leftover be produced/recorded per line; (c) manual
override — may an operator/variance force-close a line with a shortage (CMS-Prod has a
commented-out shortage/confirmRetour path at `Form.js` line 1910-1917) or edit an already-printed
ticket.

#### Make PLS total a validated number end-to-end
*Impact: medium · Effort: S*

Files: PLS `DemandeDetail.js` total input (lines 1254-1268) and validation (lines 1773-1783),
`DemandeController.transport` `Double.parseDouble(sd.getTotal())` (line 1184), `SubDemande.total` is
a String. Current behavior: `total` is a free-form string; the transport `RapportRestRouleau` save
wraps `parseDouble` in a try/catch that silently drops the record on any non-numeric value, and the
dead `returnTotal` helper (`DemandeDetail.js:1666` — see "Known issues deferred" below) would store
it with a "cm" suffix. Proposed: normalize `total` to a plain numeric string on input, validate
server-side, and log (not swallow) parse failures so missing rest reports are visible.

#### Give roll-consumption its own screen/flow rather than overloading SubDemande flags
*Impact: medium · Effort: L*

Files: PLS `SubDemande.java` (`resteRouleau`/`demandeVariance`/`zoneRouleau`/`total` as loose
strings), `DemandeController.startWorking` (line 738 routing on these strings), `Production.js`.
Current behavior: consumption state is smeared across free-text SubDemande fields interpreted by
string-equality routing ('Oui'/'Non'/'ok'/'Manque matiere'), which is brittle (many
null/equalsIgnoreCase checks, e.g. `startWorking` lines 765-766, 832-844). Proposed: model a
first-class `RollConsumption` entity per SubDemande (rollId, lot, laize, requiredMeters,
consumedMeters, remnantMeters, location, status) mirroring `CuttingRequestSerieRouleauInfo`, and
drive routing off typed status rather than parsing strings. **Open product question** (see rollup
below): how tightly should PLS reuse the CMS-Prod `CuttingRequestSerieRouleau` schema vs a
PLS-native one.

---

## Documentation & Housekeeping (cross-repo) (`docs-audit`)

Enumerated `.md` files across all four repos via `git ls-files`. MG-CMS has 79 tracked `.md` files
(mostly agent/skill/instruction configs plus a large `docs/archive/` of historical plans, kept out
of the live index); CMS-Prod has 15 (`docs/` module guides + `plans/` + `docs/archive/`); scanCoupe
has 2 (`CLAUDE.md` + `docs/schema.md`); PLS has 9 (root `ARCHITECTURE`/`BACKEND`/`FRONTEND`/
`DEMANDE`/`RECOMMENDATIONS`/`Scrap.md`, previously duplicated verbatim under `md/`). MG-CMS's live
docs were refreshed together in commit `4000cbb` (2026-06-30), but 7 more commits shipped
same-day/next-day and weren't reflected — now corrected via this pass's docIssue fixes. CMS-Prod's
`docs/` (COUPE.md, MATELASSAGE.md, etc.) dated to 2026-06-10 and were silent on ~3 weeks of
subsequent floor work — also now corrected. scanCoupe's `CLAUDE.md`/`schema.md` never mentioned the
Retour and Audit tabs — corrected. PLS's `DEMANDE.md` and `Scrap.md` are current and thorough;
`ARCHITECTURE.md`/`BACKEND.md`/`FRONTEND.md` (Feb/Mar 2026) are generic skeletons that missed the
entire `CadVerificationController` and the fuller Scrap lifecycle — `BACKEND.md`'s controller
inventory is now corrected as part of this pass; `ARCHITECTURE.md`/`FRONTEND.md` remain open (see
below). No bugs were found in this area (a documentation audit, not a code audit).

#### Consolidate the many improvement/recommendation-tracking .md files into one backlog
*Impact: medium · Effort: L* — **actioned by this file.**

There are at least 20+ separate backlog/recommendation files across the 4 repos with overlapping or
superseded content. This document (`docs/improvements.md`) is that consolidation for the current
sweep's ambiguous findings and improvement suggestions. Live/current trackers kept as-is:
MG-CMS `docs/recommendations.md` (2026-06-11 review, DONE/PARTIAL/OPEN tags, refreshed 2026-07-02),
PLS `RECOMMENDATIONS.md` (2026-02-09 tech-debt review, security items re-verified 2026-07-02).
Archived/historical files already sit under each repo's `docs/archive/`/`plans/`/`module-analyses/`
and are kept for context only — they were not further consolidated in this pass (that remains a
separate, larger cleanup: MG-CMS alone has ~25 archived plan/analysis files, and CMS-Prod's
`docs/archive/` has 6 more). If a future pass wants to fully merge those, keep MG-CMS
`docs/recommendations.md`'s DONE/PARTIAL/OPEN tagging convention as the template and pull forward
only genuinely still-open items — much of the rest is superseded (e.g. CMS-Prod's now-archived
`ORDONNANCEMENT_INTEGRATION_PLAN.md` concerns UI removed 2026-06-30).

#### De-duplicate PLS's ARCHITECTURE.md/BACKEND.md/FRONTEND.md between root and md/
*Impact: low · Effort: S* — **done in this pass.**

`D:/work/LEAR/PLS/ARCHITECTURE.md` was byte-identical to `D:/work/LEAR/PLS/md/ARCHITECTURE.md`
(confirmed via `diff`), and likewise `BACKEND.md`/`FRONTEND.md`. The root copies were kept (matching
`DEMANDE.md`/`Scrap.md`/`RECOMMENDATIONS.md` placement) and the `md/` folder was removed via
`git rm -r md` in this pass.

#### Update PLS ARCHITECTURE.md/BACKEND.md/FRONTEND.md for CAD verification and full Scrap workflow
*Impact: medium · Effort: M* — **partially done: BACKEND.md fixed in this pass; ARCHITECTURE.md/FRONTEND.md still open.**

These three files (last substantively touched 2026-02-09/03-28) predate the `CadVerificationController`
(`ca3b2ab`, 2026-06-22) and the fuller `ScrapController` lifecycle (`0eb93f7`, 2026-06-23) that
`Scrap.md` and `DEMANDE.md` now document in detail. `BACKEND.md`'s controller inventory now includes
`CadVerificationController` and a corrected `ScrapController` endpoint table (this pass).
`FRONTEND.md`'s component tree still needs a pass to reference the CAD verification UI and the newer,
more accurate `Scrap.md`/`DEMANDE.md` docs rather than duplicating a stale summary; `ARCHITECTURE.md`
likewise still needs a high-level mention of the CAD-verification flow.

#### Mark MG-CMS test/FEATURE_TEST_GUIDE.md as historical
*Impact: low · Effort: S* — **still open.**

`D:/work/LEAR/MG-CMS/MG-CMS/test/FEATURE_TEST_GUIDE.md` is pinned to "Commit 48bee5d, April 2026" and
was last edited 2026-04-01 — three months of feature work (CNC quality rework, next-series ranking,
admin health/archiving, dispatcher changes) predate it. It sits outside `docs/archive/` so a reader
following `docs/README.md`'s index would not know it's a point-in-time snapshot. Either move it into
`docs/archive/` (matching the convention used for other point-in-time docs) or add a staleness banner
like `archive/README.md` does.

---

## Known issues deferred (need a product decision)

These are real, confirmed findings where the fix itself is not mechanical — a person needs to choose
between two or more valid directions before code changes. They are NOT being fixed in this pass.

### PLS — `DemandeDetail.js:1666` (`returnTotal` dead code with a latent format conflict)

`returnTotal` (and its twin at `ScrapDetail.js:601`) has zero call sites anywhere in the repo — it's
genuinely dead code. If it were ever wired up to render the total column, it would persist
`subDemande.total` WITH a unit suffix (e.g. "120cm"), which `DemandeController.transport`'s
`Double.parseDouble(sd.getTotal())` (line 1184) would then throw on — silently swallowed by a
`catch` that only prints to console, silently dropping the `RapportRestRouleau` cost record. The
live/working path that actually sets `total` today (the `<input id="total">` handler,
`DemandeDetail.js:1254-1267`) enforces a plain-numeric value via regex, confirming the real
total-format contract is a bare number — `returnTotal` encodes a conflicting contract.

**Why deferred:** deleting `returnTotal` is a low-risk mechanical cleanup, but there are two
different valid product intents on the table — delete the dead code, or finish wiring it up (and fix
its output format to match the plain-numeric contract) — and there's a duplicate copy in
`ScrapDetail.js` with the identical pattern. A human should decide whether this dead code was meant
to be finished/wired in before either fix is applied.

**Decision needed:** was `returnTotal` meant to be wired up (in which case: fix its format to plain
numeric, in both `DemandeDetail.js` and `ScrapDetail.js`), or is it truly dead and safe to delete
(both copies)?

### MG-CMS — `SequenceStatusService.java:196` (`transition()` unconditional suiviplanning write)

`transition()` calls `suiviPlanningRepository.updateStatuBySequence(sequence, statu)` unconditionally
for every row of a sequence (no status filter), unlike the guarded
`releaseNonDemarreBySequences`/`revertReleasedToNonDemarreBySequences` used elsewhere in the same
`commit()` flow. Because `suiviplanning` is a separate non-XA datasource written independently by
CMS-Prod, there is a real window between the guarded release flip and this unguarded per-sequence
call in which a row that raced ahead to 'En cours'/'Complet' would be silently stomped back to
'Released' — regressing floor progress until the next 20-minute sync corrects it.

**Why deferred:** the same `updateStatuBySequence` method is also the documented, intentionally
unguarded write-through used by chef rectification (repo javadoc: "force every row … unguarded — the
chef is the authority here"). Guarding the shared method directly would silently break that
deliberate override behavior, so a correct fix needs a new guarded query variant scoped specifically
to `transition()`'s RELEASED-mirroring call — that's a design choice, not a mechanical edit.

**Decision needed:** should a new guarded-query variant be added specifically for the
release-mirroring path (leaving chef rectification's unguarded override untouched), and what
ordering rule defines "already advanced" (e.g. does STARTED/COMPLETED always beat a RELEASED
write)?

### MG-CMS — `PlanDeChargeService.java:534` (effective cutting time hardcodes `efficiencePct=100.0`)

`resolveEffectiveCuttingTime()` always calls `cuttingTimeCalculator.resolve(...)` with a hardcoded
`efficiencePct=100.0`, discarding the real per-machine efficience value it just resolved and skipping
the legacy "Gerber × 2" correction the calculator otherwise applies universally. Result: Gerber-machine
cutting-time estimates in every Plan de Charge view (Chg%/Chg% cap.inst., the part-number report,
`KpiChargeMachine.js`) are silently ~2x understated, and `ProductionTable.efficience` (the editable
"Efficience %" admin field) has zero effect anywhere in Plan de Charge. Meanwhile the frontend's
denominator (`getShiftProductiveCapacityMinutes`) is explicitly "raw"/no-efficiency by its own
comment, and the one place that does apply the intended correction
(`calculateShiftLoad`/`ShiftLoadCalculation`) is dead code the frontend never reads — both the backend
comment and the frontend comment assume the OTHER side applies the correction, but neither does on the
path the UI actually renders, directly contradicting the on-screen tooltips.

**Why deferred:** two architecturally different valid fixes exist — apply the correction at the
numerator (cutting-time side, matching the tooltip wording) or the denominator (capacity side,
matching `calculateShiftLoad`'s existing formula) — plus cleanup of the orphaned frontend state/dead
endpoints. Picking between them is a scheduling-semantics decision, and this touches an area (Plan de
charge / capacity KPIs) this repo's own `CLAUDE.md` explicitly flags as high-risk requiring extra
care.

**Decision needed:** should per-machine efficience + the legacy Gerber×2 correction be applied at the
numerator or the denominator — and should the orphaned `calculateShiftLoad`/`ShiftLoadCalculation`
persistence be wired into the frontend, or deleted along with the unread "Recalculer" round trip?

---

## Fixed in this pass

The confirmed bugs and the clearly-fixable disputed findings from this sweep are being fixed in code
concurrently with this documentation pass — do not re-report them.

- **PLS** — `TestController.java:338` — `generateFile` throws an uncaught `StringIndexOutOfBoundsException`
  (and leaves a garbage cut file on disk) when the requested piece isn't found in the source marker;
  needed the same not-found guard its read-only twin `getInfoPlacement` already has.
- **PLS** — `Production.js:368` — the `reftissu` onChange handler dereferences
  `state.demande.subDemandes` with no null guard, crashing the screen whenever `demande` is null
  (no-`plsId` mount or a failed PLS search).
- **PLS** — `Production.js:229` — `onAfterPrint`'s `subDemandes.map` crashes on a null
  `partNumberMaterial`, silently losing both the demande save and the subsequent `ProdTicket` POST
  after a successful print.
- **MG-CMS** — `Dashboard.js:390` — the "Logistique" sidebar section (the only in-app link to
  `/logisticsRelease`) was gated to `ROLE_ADMIN`/`ROLE_VARIANCE` only, hiding the tool from
  `PROCESS`/`CHEF_DE_ZONE`/`CHEF_EQUIPE`/`VALID_QN_LOGISTIQUE` users who are fully authorized to use
  it at the route and API level.
- **MG-CMS** — `LogisticsReleaseService.java:744` — `commit()` never blocked a WAIT-recommended
  sequence from being released, silently bypassing the zone-saturation advisory the tool exists to
  enforce.
- **MG-CMS** — `LogisticsReleaseService.java:760` — `flippedByUs` tracked whole sequences instead of
  individual rows, so a compensating revert after a failed commit could wrongly un-release a row of a
  multi-row sequence that the failed call never touched.
- **MG-CMS** — `LogisticsPicklistRepository.java:1` — no GET endpoint (or repository query) existed
  to retrieve a saved picklist snapshot, so an operator interrupted after a successful release could
  never reprint it despite the snapshot being persisted specifically for that purpose.
- **MG-CMS** — `LogisticsReleaseService.java:797` — `commit()`'s try/catch didn't cover
  Hibernate flush-timing failures that surface only at the outer `@Transactional` commit boundary
  (after `commit()` already returned), which could still split-brain `suiviplanning=Released` against
  the local MG-CMS mirror.
- **MG-CMS** — `TableFeedRankingService.java:434` — the machine-type gate was skipped whenever a
  candidate serie's `machine` field was null, letting an unrunnable serie be recommended on every
  table type (Lectra/Gerber/CNC) in a zone.

---

## Open product questions

### ✅ RESOLVED (2026-07-02, product owner) — scanCoupe T0M\*/T0R\* magasin-location filter

`R100AuditService.isMagasinLocation()`
(`D:/work/LEAR/scanCoupe/src/main/java/com/lear/scanCoupe/services/R100AuditService.java:151-157`),
which backs the Audit tab's "deleted from R100 but never scanned into a rack" traceability check,
treats **only** locations starting with `T0M` or `T0R` as "real magasin storage locations." The
product owner has confirmed the semantics: **`T0M*`** = NEW rolls in the magasin, not yet consumed
by CMS or PLS (a `T0M` roll that already has consumption records is a data anomaly whose position
should be rectified to `T0R`); **`T0R*`** = rolls RETURNED to the magasin after consumption (rest
rolls); **any other location** (`T0D*`, `T0V*`, ...) is **BLOCKED** — production cannot use those
rolls, and they are correctly excluded from the "real magasin exit" set the Audit tab flags. The
filter logic in `isMagasinLocation()` was already correct; only the comments claiming this was an
unverified assumption have been updated (class javadoc + method javadoc) to state these confirmed
semantics. Same StockStatusReport location semantics are now documented in
`D:/work/LEAR/scanCoupe/docs/schema.md` and `D:/work/LEAR/scanCoupe/CLAUDE.md`.

**Follow-on improvement idea:** add a data-quality check that flags `T0M*` rolls which already have
consumption records (in `CuttingRequestSerieRouleau` and/or PLS `ProdTicket`) — per the confirmed
semantics, a `T0M` roll should never have been consumed, so any hit is a data anomaly whose
`StockStatusReport.location` should be rectified to `T0R`.

### PLS — multi-piece placement grouping rules

(From `pls-placement` → "Open product questions for multi-piece placements" above.) (1) Grouping key
— merge only pieces from the same source marker `placement`, or also across markers of identical
laize/material? (2) Quantity semantics — does "multiple pieces per placement" mean distinct
empiècements only, or also repeating one piece to meet `quantite`, given lines in a group may have
differing `nbrCouche` that a single marker can't express per-piece? (3) Machine limits — what marker
width/length constraints cap how many pieces fit? (4) Should both the CAD (`envoyerCAD`) flow and the
recut flow get multi-piece generation, or recut only?

### PLS — roll-consumption flexibility axes

(From `pls-roll` items above.)
- **Rest-first enforcement**: hard block (like CMS-Prod's `strictUsingRestRollFirst`) or an advisory
  warning for PLS — and who may override it (variance role vs supervisor)?
- **Remnant record location**: should PLS remnants live in the same `ScanRouleau`/`plsRest` tables
  CMS-Prod uses, or a PLS-only rest pool?
- **Multi-roll / partial consumption**: can one PLS material line be fulfilled by 2+ rolls scanned
  over time; can more than one leftover be recorded per line; may an operator/variance force-close a
  line with a shortage, or edit an already-printed ticket?
- **Schema reuse**: how tightly should a first-class PLS `RollConsumption` entity reuse the CMS-Prod
  `CuttingRequestSerieRouleau` schema vs. a PLS-native one?

### MG-CMS — next-series fallback for unconfirmed/idle tables

(From `mgcms-workbench` → "Public next-series returns empty for an operator at a table that was never
shift-confirmed and is currently empty" above.) Should `recommendForMachine` fall back to ranking a
zone's candidates for a requested machine/table that isn't present in the workbench DTO (i.e., never
shift-confirmed and currently idle), so the public `/api/public/next-series` contract never returns
empty for a real operator standing at a real table? Today this is intentional behavior, not a bug —
but it's worth a product call on whether "no data" is the right response for that case.

### MG-CMS — Plan de Charge efficiency correction placement

Already detailed under "Known issues deferred" above (`PlanDeChargeService.java:534`) — repeated here
because it's as much a product question as a bug: should efficiency (per-machine + legacy Gerber×2)
be applied at the numerator or the denominator of the load-% calculation, and should the orphaned
`calculateShiftLoad`/`ShiftLoadCalculation` persistence be wired into the frontend or removed?
