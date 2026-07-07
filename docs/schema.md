# MG-CMS — Database & Workflow Schema

*The single source of truth for what each database, table and column means, and which
production step / page touches it. Covers the whole cutting ecosystem (MG-CMS is the master;
CMS-Prod and scanCoupe have their own shorter `docs/schema.md` that reference this one).*

*Sizes/row counts below are from the 2026-04-25 prod restore (`LEAR_MG_CMS_Prod` on the dev
machine) — treat as orders of magnitude, not live numbers.*

---

## 1. The six databases

| Datasource prefix | Java package | DB (prod) | Role |
|---|---|---|---|
| `spring.datasource` | `com.lear.MGCMS` | **MG_CMS** | Primary. All new development. Shared with CMS-Prod (writes) and scanCoupe (writes `Scan_*`). |
| `cms.datasource` | `com.lear.cms` | **qualite** | The *old* CMS (legacy Windows app). MG-CMS **mirrors imports into it** so both systems show the same data; the 20-min sync job reads `suiviplanning.Statu` back. Legacy tables are purged regularly (suiviplanning ≈ 600 rows) — except `GammeTechniqueImprimer` (~130k) and `Timing_Model` (~280k) which grow. |
| `ctc.datasource` | `com.lear.ctc` | **plt_viewer** | Engineering piece specs: `files`, `sequences`, `sequence_details` (CTC = control table check / PLT viewer). |
| `pls.datasource` | `com.lear.pls` | **MG_PLS_NEW** | PLS = re-cut/defect/scrap workflow (Demande → SubDemande → ProdTicket) + scrap reporting. |
| `splice.datasource` | `com.lear.splice` | **splice** | Supplier spreader app DB (read-mostly): sensor calibration + per-layer spreading log. |
| `learpokayoke.datasource` | `com.lear.learpokayoke` | **LearPokaYoke** | `PokaYokeMain` — material↔marker poka-yoke list. |

**Naming gotcha (applies everywhere):** Hibernate uses a no-op physical naming strategy.
A field without `@Column` keeps its **camelCase** name in SQL Server. Native queries must
match camelCase column names exactly.

**Shift conventions:** plant = 1 night, 2 morning, 3 afternoon. The legacy `qualite` DB uses
letters (A/B/C) and `OrdonnancementService` uses an inverse mapping — never "unify" blindly.

---

## 2. End-to-end production flow → tables

Each step lists: who acts, on which page, and what is written/read.

### Step 0 — Demand: work orders arrive
*Actor: logistics planner (today: imported from QAD/refresh, not created in the app).*

| Table (MG_CMS) | Meaning |
|---|---|
| **WorkOrder** (~171k rows) | One row per WO from the ERP. `wo` (PK), `woid`, `item` (WL… = semi-finished cut kit), `partNumber` (finished cover, sewing side), `qtyOpen/qtyRejeter/qtyCompleted`, `dueDate` + `shift` (when it must be cut), `status`, `deactivated`. The `/preparation` page lists WOs by `dueDate`+`shift`, can `fuse` duplicates. |
| **Planning** / `PlanningDetails` (~70k) | Manual planning grid per `planningDate`+`shift`+`partNumber`: quantity, group/design-group, color, comment; `PlanningDetails` adds the WO + chosen cutting plan + `sequenceCoupe` once planned. Used by `/planning` and `/cuttingPlanning`. |

> Note: items that start with **WL** are the *cut kit* (output of cutting); `partNumber` is the
> final sewn cover. Cutting works in WL/item space; sewing consumption is out of scope.

### Step 1 — Engineering ("ingénierie"): define the pieces
*Actor: product engineer. Pages: `/ctcFiles` (Files), `/gammesTechnique`, `/gammeCMS`.*

| Table | DB | Meaning |
|---|---|---|
| **files** | plt_viewer | One row per **piece** of a cover PN: `part_number_cover` (+description), `panel_number` (piece number), `pattern` (PLT file name = the piece geometry), `part_number_material` (fabric it is cut from), `semi_finished_good_part_number`, `quantity` (pieces per cover), `type`, `ecn_number`, CTC tolerances (`tol_min1/max1/min2/max2`, `toleranceDrill`), `plt_found`. |
| **FilesHistory** | plt_viewer | Audit of changes to `files` (`changement` LOB). |
| **sequences** / **sequence_details** | plt_viewer | Link a cutting sequence to cover PN / serial / marker / material — written at import so the PLT viewer can resolve what a sequence contains. |
| **GammeTechnique** (+`GammeTechniqueEmp`, `GammeTechniquePartNumberMaterial`, `GammeTechniqueText`, `GammeTechniqueImprimerHistorique`) | MG_CMS | The printable **gamme** (A4 sheet glued on each box): per `partNumber` the image, label positions per `panelNumber` (Emp), zoom per material, free text labels. |
| **GammeTechnique** / **GammeTechniqueImprimer** | qualite | Legacy copies; `GammeTechniqueImprimer` (~130k) is the legacy **print log** — one row per printed box label (`NSerieGammeImp` is the box id counter, `NSequenceImp`, `NOFImp`=WO, `QuantiteImp`); still written/read at import & print time. |
| **PartNumberBoom** (~190k) | MG_CMS | The **BOM extract from QAD**: `partNumber` × `partNumberMaterial` with `quantityPer`. Used to validate plans/imports against ERP. |
| **PartNumberInfo** | MG_CMS | PN master data from ERP: status, prodLine, item type/groups, `packageQty` (box size), weight, perimeter, cutting time. |

### Step 2 — CAD: cutting plans
*Actor: CAD team. Pages: `/cuttingPlan`, `/cuttingPlanCombination`, `/cuttingPlanFoam`, `/partNumberMaterialConfig`, `/placement-view`, `/patternSearch`.*

| Table (MG_CMS) | Meaning |
|---|---|
| **CuttingPlan** (~137k) | A reusable plan: "cut these PNs together". `projet`, `version`, `definition`, `quantity` (kits), `enabled` window (`startDate/endDate`, enabled/disabled by+at), `type` (Normal/…), `foam`, six `verificationN` LOBs (CAD validation checklist snapshots), `consommation`, `alertMessages`, `cmsId` (id of the mirror row in qualite `PlanCoupe`). |
| **CuttingPlanPartNumber** | (plan, partNumber) → `quantity` of covers the plan yields, `quantityPer`, `item`. |
| **CuttingPlanMaterial** | (plan, partNumberMaterial) → cutting parameters for that fabric inside the plan: `vitesse`, `rotation`, `plaque`, `tauxScrap`, `matelassageEndroit` (face up/down), `qadUsage` (theoretical m/kit from QAD). |
| **CuttingPlanMaterialPlacement** (~283k) | The **placements** (markers): per (plan, material, `placement` name) → `machine`, `nbrCouche` (layers to spread), `maxPlie`, `longueur` (marker length), `laize` (width), `config`, `drill`, `perimetre`, `tempsDeCoupe` (estimated cutting time), `pliesConfig`, group. This is what becomes a **série** at import. |
| **CuttingPlanCombination** (+`…PartNumber`) | Named PN groupings ("combinaisons") used by the Combinaison helper on `/preparation` to propose which plans cover a WO set. |
| **CuttingPlanHistory** (~263k, 326MB — biggest table) | LOB audit of every plan edit. |
| **CuttingPlanRapportModel/Placement/Drill** | Parsed Lectra Diamino reports attached to a plan (models per placement, lengths, efficiency, drills) for validation. |
| **PartNumberMaterialConfig** (+`ReftissuCategory`, `ReftissuMachine`, `ReftissuMargin`, history) | Per-fabric master config: speed, rotation, plaque, scrap rate, spreading face, laize margins, validated flags per machine (0BF/IP6), `weight_unit` (kg/m²); category (laize buckets), per-machine-type max plies, margin tables. |
| **Placement** / **PlacementDetail** / **PlacementFolder** | Index of PLT placement files found in the CAD folders: per `placement`+`folder` the material, length/width/efficiency/nbrPieces; detail = pieces (patterns) inside the placement. Drives `/placement-view`, pattern search and import-time piece counting. |
| **DrillEmp** | Pattern → drill1/drill2 counts (laser drill verification reference). |
| **PartNumberCorrespendance** | PN/pattern ↔ correspondence mapping for shared placements. |
| Mirrors in **qualite**: `PlanCoupe`, `Spreading_Cutting_PlanCoupe`, `PartNumber_PlanCoupe`, `Item_PlanCoupe`, `ItemMachine_PlanCoupe`, `CategoryLaize_PlanCoupe`, `SeuilLongueur_PlanCoupe`, `Interval_*`, `Drill_PlanCoupe` | The same plan data written for the legacy CMS (`cmsId` links them). `Timing_Model` (~284k) = per-placement timing rows the legacy system uses for plan-de-charge math. |

### Step 3 — Import / préparation: plans become cutting requests
*Actor: `ROLE_IMPORTER` (a.k.a. importateur). Page: `/preparation` (`ImportationNew.js`). Endpoint: `POST /api/cuttingRequest`.*

The importer picks `planningDate`+`shift`, sees the WOs due, asks **Combinaison**
(`/api/cuttingPlanPartNumberInfo/to-work`) for plans covering the demand, then imports the
chosen plans **one by one** (one POST per sequence — see §5 performance notes).

| Table (MG_CMS) | Meaning |
|---|---|
| **CuttingRequest** (~130k) | One **sequence** = one imported plan execution. PK `sequence` (format `ddMMyyHHmm` + counter). `cuttingPlanId`, `projet/version/modele/definition`, `planningDate`+`shift` (when it was planned), `dueDate`+`dueShift` (when it must be done), `cmsId`, **dispatch fields** (`zone_nom` FK, `dispatchedZone`, `zoneAcceptanceStatus`, `pinnedByChef`, dispatched/accepted by+at, `releaseZone` V16_01, `releaseZoneSource` V17_02 — LOGISTICS/CHEF = locked, AUTO/NULL = re-inferable by `SequenceZoneAutoCorrectService`), **`sequenceStatus`** lifecycle (§3). |
| **CuttingRequestSerie** (~324k) | One **série** = one placement to spread+cut. PK `serie` (yyyy + counter, plant-unique). FK `cuttingRequest_sequence`. Copy of placement params (material, placement, `nbrCouche`, `longueur`, `laize`, `config`, `drill`, `maxPlie*`, `perimetre`, `tempsDeCoupe`, `machine`) + **execution tracking**: matelassage (zone/table, matelasseur1-4, dateDebut/Fin, `statusMatelassage`), coupe (zone/table, coupeur1-2, dateDebut/Fin, `statusCoupe`, `autoCoupe`), quality (tableQualite, contrôleur, qteNonConforme + codeDefaut, qteScrap + codeScrap, lieuDetection, paquet checks, verificationDrill1/2), picking (`matriculePicking`), `nbrPiece`/`nbrPieceTotal`, `retourMagasin`. **This is the hottest table in the system** — written by CMS-Prod at every floor event, read by every dashboard. |
| **CuttingRequestPartNumber** | (sequence, partNumber) → quantity of covers this sequence produces, `wo`/`woid` link back to the WorkOrder, `packageQty`, `gammePrinted`. |
| **CuttingRequestBox** (~235k) | One row per **physical box** to fill. `id` = numeric string (global counter ≥ 75,000,000 shared with legacy `NSerieGammeImp`!), partNumber, `qtyBox`, wo/woid, `gammePrinted`, `nbrImpression`. Labels/A4 gammes are printed from here. |
| **CuttingRequestSerieRouleau** (~217k) | **Consumption ledger**: one row per roll mounted on a série. FK `cuttingRequestSerie_serie`, `idRouleau` (roll serial), `confirmReftissu`, `lotFrs`, `metrage` (length taken), `nbrCouche`, `longueurPremierCouche`, `longueurCoucheOverlap`, `defaut`, `nonUtitlse`, `retour` (returned meters), `excess`, `overlap1..8` (overlap positions), `totalUsage`, `confirmRetour`, deblock fields, `machine`, `location`, `sequence` (denormalized). Written by CMS-Prod during spreading. |
| **CuttingRequestSerieRouleauHistory** | LOB audit per série of roll-row edits. |
| Mirror writes into **qualite** at import | `suiviplanning` (one row per PN: `nSequence`, `Statu='Non demarre'`, WOID, qty), `produitfinit`, `Asprova_WO`, `Order_Schedule`, `sequences` (plt_viewer). Box ids reuse the legacy `GammeTechniqueImprimer` counter. |

### Step 4 — Logistics release & picklist
*Actor: logistics. Pages: `/logisticsRelease`, `/rapportShortage`, `/stockReportVerification`, `/rollQlaize`. Docs: `picklist-logistics-master-plan.md`.*

| Table | DB | Meaning |
|---|---|---|
| **suiviplanning** | qualite | Legacy lifecycle board. `Statu`: `Non demarre` → `Released` → `En cours` → `Complet`. The release tool flips Non démarrée→Released (`SuiviPlanningRepository.releaseNonDemarreBySequences`); a 20-min job mirrors `Statu` → `CuttingRequest.sequenceStatus` (scans ALL rows — table is tiny; date-window version failed in prod). Because this `cms` datasource is **non-XA**, the logistics `commit()` compensates a local failure by reverting only the rows it flipped (`revertReleasedToNonDemarreBySequences`), then `setRollbackOnly` + rethrow. |
| **logistics_allocation** | MG_CMS | Soft **reservation ledger**: (sequence, serie, refTissus, serialId, sourceRack/Zone, targetZone, allocatedMeters, `status` enum, picklistId). Prevents double-allocation of rolls between released sequences. |
| **logistics_picklist** | MG_CMS | Printable picklist snapshot per release (`snapshotJson` LOB, releaseDate, shift, sequenceCount). |
| **StockStatusReport** (~780k) | MG_CMS | **Magasin (R100)** stock: rows loaded from the R100.prn report — `itemNumber`, `ref` (fabric), `location`, `qtyOnHand`, `status`, `lastUpdated`, `isDeleted` (diff-loader flag). Location-movement history of every roll in the warehouse. ⚠ PK is (itemNumber, location, ref, qtyOnHand) — qtyOnHand inside the PK is a design wart. |
| **Scan_Rouleau** (`ScanRouleau`) | MG_CMS | **Rolls on production racks** (scanned by scanCoupe): `serialId` PK, `reftissu`, `quantite` (string!) + `metrage` (number), `emplacement` (rack → zone via `Zone.rollLocations`), `lot`, `matricule`, `date`. The physically-pickable stock layer. |
| **Scan_RouleauHistorique** | MG_CMS | Roll movement audit (`content` text per event). |
| **SerieRouleauTemp** | MG_CMS | Rolls **currently on a spreading table**: PK `tableMatelassage` (one row per table), `idRouleau`, `reftissu`, `lot`, `quantiteInitiale`, `estimationRest` (live remaining meters). Remnant flow: row deleted here → re-saved into `Scan_Rouleau` with the rest. |
| **MaterialLogistics** | MG_CMS | Material move requests per zone (required/available/deficit, neededBy, assignedRolls, status) — scheduling-era table, partially superseded by the allocation ledger. |
| **ValidationQLaize** | MG_CMS | Quality check of received rolls' laize (contractual vs real width) per roll. |
| **LaminationPls** | MG_CMS | reftissu → PLS id mapping for laminated fabrics. |

Roll lifecycle (the four states): magasin (`StockStatusReport`) → rack (`Scan_Rouleau`) →
spreading (`SerieRouleauTemp`) → consumed (`CuttingRequestSerieRouleau` / PLS `ProdTicket`);
remnants go back to `Scan_Rouleau`.

### Step 5 — Dispatch, ordonnancement & plan de charge
*Actors: process team & chefs de zone. Pages: `/processDispatcher`, `/processWorkbench`, `/chefDeZoneConfirm` ("Confirmation Machines"), `/tableFeed`, `/planDeCharge`, `/engineControl`, `/kpiChargeMachine`.*
> Removed 2026-06-30 (commit 834568d): the "Ordonnancement" menu (`/advancedOrdonnancement`, `/schedulingDashboard`) and the Chef-de-Zone "Supervision Zone" (`/chefDeZonePage`) + "Affectation Chef↔Zone" (`/userZoneAdmin`) front-ends were deleted. `OrdonnancementService` and `OrdonnancementController` (`/api/ordonnancement/**`, used by `Form.js`) are KEPT and still called live.

| Table (MG_CMS) | Meaning |
|---|---|
| **Zone** | Cutting zones. PK `nom`, `code`, `rollLocations` (CSV of racks belonging to the zone), `orderInd`, `category` (`STRICT` = machine-bound zone (Lectra/IP6) vs shared C/CAP SPARTEL/Presse), `is_active`. |
| **ProductionTable** | Physical machines/tables: `nom`, machineType, zone, PC names (matelassage/coupe), printer IP, drill calibration values, `tableLength`, CMS versions installed, `forPls`, `autorisationAirbag`. |
| **Machine** / **MachineType** | Cutting machine specs (`maxLaize`, `maxLength`) per type. |
| **machine_queue** | The "next 3" per machine: (machineNom, queuePosition 1-3) → serie/sequence + estimated times, optimistic-locked (`version`). |
| **UserZone** (`user_zone`, V2_03) | Chef ↔ zone assignment (`is_default`, `assigned_by/at`, `revoked_at` soft-delete). Its admin UI (`/userZoneAdmin`) was removed 2026-06-30; the table/entity remain. |
| **ShiftZoneConfirmation** (+`…Machine`, V2_04) | Chef's shift-start confirmation: one row per (date, shift, zone) + N machine child rows (`is_up`). LIVE: written by `/api/zone/confirm` from `/chefDeZoneConfirm`, read by `ActiveMachineResolver` to decide which machines/tables are "up" for next-series / tableFeed / productionFloor. |
| **DispatchAudit / AdmissionBlockedAudit / UnassignableSerie** | Audit trails of dispatcher decisions: sequence moved from/to zone + trigger; séries refused admission (reason enum); séries impossible to place. |
| **dispatch_engine_run / …_suggestion / …_indicator_sample** ⚠ DEAD/legacy | History of the shelved continuous-dispatch optimizer (2026-05-31 pivot): mode, iterations, spread before/after; per-run suggested zone per sequence; load-spread samples. No longer driven by the live flow. |
| **engine_schedule_entry** ⚠ DEAD/legacy | Optimizer-era engine plan: (serie, phase MATELASSAGE/COUPE) → machine + planned start/end per run. |
| **SequenceSchedule / SerieSchedule / ShiftSchedule / MachineScheduleStatus / ScheduleInterval / SchedulingConfig / optimized_plan / optimized_series_assignment** ⚠ DEAD/legacy | The older scheduling module (shelved optimizer, 2026-05-31 pivot): per-sequence zone/priority/status, per-série machine+times, personnel per shift, machine availability per shift, blocked intervals, scoring weights, saved optimizer plans. ORPHANED 2026-06-30: `SchedulingController` (`/api/scheduling/**`) + `SchedulingOptimizationService` + `OptimizedPlan`/`OptimizedSeriesAssignment` still compile but their only UI (`/schedulingDashboard`) was removed. |
| **shift_load_calculation** | Plan-de-charge result rows per (shift_date, shift_number, machine_type): planned/actual/available time, load %, efficiency, carryover. |
| **capacite_installee** | Installed capacity per date/shift/groupe (minutes per machine, efficiency target). |
| **etat_machine_historique** | Machine state intervals (code_etat, cause, action, start/end) — downtime tracking for plan de charge. |
| **Timing_Model** (qualite, ~284k) | Legacy per-placement timing matrix (speed, prep time, per-layer times) used to compute theoretical cutting/spreading times. |
| **ShiftLoadCalculation / CoupePerformance / suivicoupe / suivimatelassage** | Realized-performance views (see step 7). |

### Step 6 — Spreading (matelassage) — CMS-Prod
*Actor: matelasseurs at the table PC. CMS-Prod screens `/formMatelassage`, `/formMix`. See CMS-Prod `docs/MATELASSAGE.md`.*

Writes into MG_CMS: `CuttingRequestSerie` (status/dates/operators), `SerieRouleauTemp`,
`CuttingRequestSerieRouleau` (+History), `overlap_saving`, `QualityValidationHistory`,
`ScanXPL`, plus legacy `qualite` mirrors (`matlassage`, `suivimatelassage`, `suiviplanning`).

| Table | DB | Meaning |
|---|---|---|
| **overlap_saving** (~344k) | MG_CMS | Every **overlap** done during spreading: serie, placement, machine, reftissu, start/end position (mm), width, sensor raw+calibrated values, matelasseur, shift, couche number, roll id/lot. An overlap = re-spreading a partial layer over a defect/splice so the affected pieces are still cut complete. |
| **CalibrageSensor** | MG_CMS | Per-table length-sensor calibration (left/right links, reference vs measured values, margins). |
| **calibration_log** | splice | Supplier app's own calibration log per station. |
| **marker** | splice | Marker catalog as the splice app knows it (lengths net/brut, layers, drills, width, pieces/layer). |
| **marker_log** | splice | Per-spreading-session log: marker, lengths, splices, layers to do/done, defect/splice lengths, margins, badge/user, station, state — the supplier-side record of the spreading work. |
| **markers_only_code** | splice | Work queue rows (order_code → marker, layers, multiply, status). |
| **QualityValidationHistory / QualityValidationPattern / QualityPatternValidationHistory** | MG_CMS | First-piece pattern validation: which (machine, placement/pattern, roll) was validated by quality, when, and the rules that require it. |

### Step 7 — Cutting (coupe) — CMS-Prod on the Lectra PC
*Actor: coupeurs. CMS-Prod screens `/formCoupe`, `/formCoupeLaser`, `/formCoupeSimple`, `/formMix`. See CMS-Prod `docs/COUPE.md`.*

| Table (MG_CMS) | Meaning |
|---|---|
| **CoupeMachineHistory** (~381k+) | Parsed Lectra log lines: (machine, fileReport, ind) PK, lineDate, placement, errorCode, type, extra. Drives the cutting-history view & auto status. ⚠ was the #1 CPU killer until `IX_CoupeMachineHistory_ind` (prod, 2026-06-09). |
| **CoupeMachineHistoryLoading** | Read-cursor per (machine, fileReport): lastUpdate + lineNumber — incremental log ingestion state. |
| **CoupeSection** | One row per cut **section** from the Lectra report: marker name, qty (articles/pieces/layers), marker length/width/efficiency, speeds, vacuum, notches/drills qty, material length/width, perimeters, running time, user. The "what actually got cut" record. |
| **CoupeDrill** (~623k) / **CoupeDrillHistory** | Laser drill verification lines (environment/requirement/material per placement & ind) and the per-shift drill-size memory. |
| **ToolWearHistory** | Blade wear samples per (machine, fileReport, ind): bladeName, wearValue/max/percent. The "outil" section check. |
| **ScanXPL** (~116k) | Each scan of a série at the cutting machine to generate the **CPL/XPL file**: serie, machine, operator, scanDate, isFirstScan, placement, autoUpdatePerformed. |
| **CoupePerformance** (~648k) | Per (machine, placement, dateDebut): counters from the machine — running/interruption/repérage/hors-cycle minutes, nbrPieces, coupeur. Feeds `/kpiChargeMachine` & IPPM. |
| **CoupeError** | Machine error events (etat, machine, placement, error text). |
| **Intervention** (~191k) | Downtime/intervention tickets created at the machine: serie/sequence, machine, codeArret (department-routed), codeErreur, cause/action, debut/fin, emetteur/responsable, validation. Page `/validationIntervention`. |
| **CuttingRequestSerieDerogation(+Demande)** | "Cut on another machine" derogation: request + approval to move a série off its planned machine. |
| **cuttingRequestSeriePrinting** | Log of série label (re)prints at the machine incl. quality validation of the reprint. |
| **MachineDxfRapportV2 / MachineLsrRapport** | Gerber DXF job reports / laser machine reports (for the LASER & DXF flows). |
| **Consumable / MaintenanceIntervention(+Config) / FirstCheck(+Config, MachineStopped) / Pointage** | Consumables mounted on machines (blades…), maintenance acts vs frequency plan, shift-start first-check checklists, operator clock-in per poste. |

### Step 8 — Quality
*Pages: `/qualityNoticeForm`, `/qualityNoticeValidation`, `/verificationQualite`, `/suiviQulite`, `/auditQualite` (EntityList), `/validationDefautRouleau`, `/qualityCode`.*

| Table (MG_CMS) | Meaning |
|---|---|
| **QualityNotice** | Full QN workflow against a fabric/roll defect: WO/sequence/PN, reftissu + supplier, defect type+code, roll id/lot, metrageEcarte, photos, response/decision/securisation, validation chain (coupe → superviseur), QRQC flag. |
| **Qn** | Lightweight QN record / "Flash qualité" (numeroQn, projet, reftissu, defect, image, resultat). `appliquerSur` (V18_08): Matelassage / Coupe / Les deux — CMS-Prod uses it to decide whether to show the QN at the spreading vs cutting station (null = show everywhere). |
| **AuditQualite(+Config)** | Layered process audit per date/shift/tableControle: task results + actions. |
| **DefautRouleau** | Catalog of roll defect types (titre, active). |
| **QualityReftissuBlock** | Fabric refs blocked by quality (no release while present). |
| **CodeDefaut / CodeScrap / CodeArret / CodeErreur / Defaut / CauseScrap** | Code catalogs: defect codes (dept-routed emails), scrap codes, downtime codes, Lectra error codes → root cause / action. |
| **DemandeChangementSerie** | Request to change a série's parameters (laize, machine, placement…) with department validation chain. Page `/demandeChangementSerieValidation`. |

### Step 9 — Picking & boxes (end of line)
| Table (MG_CMS) | Meaning |
|---|---|
| **CuttingRequestBox** | (step 3) box fill state; a sequence's boxes ship only when **all** its séries are `Complete` — this is why stuck séries pile boxes at zone end. |
| **BoxWeight / BoxTypeConfig / PartNumberWeight / PartNumberValidatedWeight** | Box-weighing poka-yoke: expected box weight = Σ piece weights (+ empty box weight per type), sent vs received weight, validation. Pages `/boxWeightFilling`, `/boxWeightVerifying`, `/weightCalculation`. |
| **PieceDetail** | Per-pattern piece measurements imported from CAD (area, perimeter…) — feeds weight estimation (`/pieceDetailImport`). |

### Step 10 — CNC / PS line (leather CNC perforation/sewing prep)
*Pages: `/cncPs`, `/cncControl`, `/cncQualite`, `/cncQualiteMachine` (per-machine quality report,
commit ea62f60, 2026-07-01), `/cncKpi`, `/cncSync`, `/cncShiftStatus`. `/cncPs` and `/cncControl`
also show expandable PN-Cuir/Fil-Couture pattern reference images (commit 35c38ed).*

| Table (MG_CMS) | Meaning |
|---|---|
| **CncPsSession** | One session per scanned **box** (`boxId` unique, must start with `'S'` — validated client-side, commit 757998e): PN, code1/3, qty, machineCnc, production status/dates, label printed; quality fields `qualiteStatus`, `userQualite`, `startDateControl`/`endDateControl` (V18_03). |
| **CncProduction / CncControl / CncPsLeatherConsumption** | Per-session piece runs (program, pattern, panel, machine, status), quality control rows (OK/NOK + CNC defect/scrap codes; `programNumber` V18_03, `machineId` V18_06 replacing per-session machinePress/machineBlind), leather roll consumption (serial/lot, init/consumed/retour). |
| **MachineCnc / ProgramCNC / ProgrammeDistribution** | CNC machine list; PN+pattern → program number, casette, stitch params, `cavitePress` (V18_07); program-number distribution per machine. **ProgramCNC duplicates are now ALLOWED** (the upsert-on-(partNumber,panelNumber) was removed from `ProgramCNCService`); +`updatedAt`/`updatedBy` (V18_05). |
| **ProgramCNCHistory** (V18_05) | Audit of ProgramCNC changes: `operationDate`, `username`, `operation` (CREATION/UPDATE/DELETE), `snapshot` (ProgramCNC.toString()). Persist-only — no viewing page. |
| **CncMachineReport(+Piece)** | Imported per-machine production reports + per-piece status (for KPI/sync). |
| **PokaYokeMain** (LearPokaYoke DB) | Material ↔ marker authorization list checked before cutting. |

### Cross-cutting / admin
| Table | Meaning |
|---|---|
| **users / roles** (MG_CMS) | App users (PK `matricule`) + role list (ROLE_ADMIN, ROLE_IMPORTER, ROLE_CAD, ROLE_QUALITY, ROLE_LOGISTIQUE, ROLE_VARIANCE, chef roles…). `/user` (the `/userZoneAdmin` chef↔zone admin page was removed 2026-06-30). |
| **Projet / ProjetVersion / ProjetReftissu / Site** | Project catalog (JLR, …) with versions and fabric refs per project. |
| **ReftissuPrix / ReftissuProperty** | Fabric price (scrap valuation) and free-form properties. |
| **HardwareConfig / ControlTable / Scan_Config** | PC/hardware links per machine, poste→PC mapping, scanCoupe config params. |
| **PartNumberMaterialUpdate** | Old→new material PN renames applied across data. |
| **ConfigSeriePlus** | Template series ("série plus") parameters for quick re-cuts. |
| **DemandeHistory / …History tables** | LOB audits (one per functional area). |
| **Asprova_WO / Order_Schedule** (qualite) | Legacy scheduling interface rows (Asprova era), still fed at import. |

### PLS (MG_PLS_NEW) — re-cut / defect / scrap workflow
*Pages: `/projetPLS`, `/rapportRestRouleau`, `/rapportShortage`, `/plsDemande` (EntityList), CMS-Prod `/formPLS`.*

| Table | Meaning |
|---|---|
| **Demande** | A re-cut request (defect found downstream): project/chain/lieu, defect, CAD/variance/recut/transport approval chain (`wait*` fields), files, urgent flag. |
| **SubDemande** | The pieces to re-cut: PN, empNumb (panel), quantity, material, placement, laize, drills, roll/zone info, nbrCouche, printed flag. |
| **ProdTicket** | Roll consumption tickets on the PLS side: `labelId` (roll serial), reftissu, quantity used, `initQuantity`, lot, table, prices, `quantitePLS`. scanCoupe's Contrôle tab joins this with `CuttingRequestSerieRouleau` for full roll history. |
| **Scrap / SubScrap / ScrapRapport / RapportRestRouleau** | Scrap declaration workflow + valuation reports; rest-of-roll reports. |
| **users, Projet, Site, Chaine, LieuDetection, Defaut, CauseScrap, machine** | PLS-side catalogs (separate user table!). |

---

## 3. Status lifecycles (the state machines)

**`CuttingRequest.sequenceStatus`** (mirrored from `suiviplanning.Statu` every 20 min; never
overwrites MATERIAL_MISSING/INCOMPLETE; backward transitions allowed):

```
IMPORTED ──release──▶ RELEASED ──first série starts──▶ STARTED ──all séries complete──▶ COMPLETED
   (Non demarre)        (Released)                      (En cours)                       (Complet)
side states: MATERIAL_MISSING (logistics), INCOMPLETE (production)   null = legacy row (treated as active by dispatcher)
```

**`CuttingRequestSerie.statusMatelassage` / `statusCoupe`** (set by CMS-Prod):
`Waiting` → `In progress` → `Complete` (+ `Incomplete` when stopped mid-way).
A série is *done* only when `statusCoupe='Complete'`; a sequence's boxes release only when all
its séries are Complete.

**`suiviplanning.Statu`** (legacy): `Non demarre` → `Released` → `En cours` → `Complet`.

**WorkOrder.status**: ERP open/closed states + `deactivated` soft-delete; coverage is judged
by qtyOpen vs imported quantities on `/preparation`.

**Zone admission** (dispatcher): `CuttingRequest.dispatchedZone` + `zoneAcceptanceStatus`
(PENDING/ACCEPTED/REJECTED + `pinnedByChef`); STRICT zones freeze a sequence once a série
started there.

---

## 4. ID conventions

| Id | Format | Generator |
|---|---|---|
| `CuttingRequest.sequence` | `ddMMyyHHmm` + 2-digit counter | MAX(LIKE prefix) over MG_CMS **and** qualite `coupe.nof` |
| `CuttingRequestSerie.serie` | `yyyy` + counter (numeric string) | MAX over MG_CMS + qualite `suivicoupe`/`suivimatelassage` |
| `CuttingRequestBox.id` | numeric string ≥ 75,000,000 | MAX(CAST(id AS INT)) over boxes **and** qualite `GammeTechniqueImprimer.NSerieGammeImp` |
| `users.matricule` | plant badge number | manual |
| Roll `serialId` / `idRouleau` | supplier label barcode | scanned |

These MAX() lookups exist because ids must stay unique across MG-CMS **and** the legacy CMS
writing the same tables. See §5 for the cost.

---

## 5. Known hot spots (verified on the restore + code)

1. **Bare tables**: as of the 2026-04-25 restore, CuttingRequest/Serie/Rouleau/Box, WorkOrder,
   Scan_Rouleau, StockStatusReport, ScanXPL, overlap_saving had **only their PKs** — every
   filtered query is a full scan. `V14_01` (2026-05-12) adds the workbench set; the 2026-06-09
   prod script added `IX_CoupeMachineHistory_ind`, `IX_CoupeDrill_ind`,
   `IX_StockStatusReport_qtyOnHand`, `IX_CuttingRequestSerieRouleau_serie` (prod-only, not in
   Flyway). `V18_01` (2026-06-25) adds the remaining gaps to Flyway (WorkOrder due-date,
   CoupeMachineHistory/CoupeDrill `lineDate`, overlap_saving/ScanXPL `serie`,
   CuttingRequestSerieRouleau `idRouleau`, FirstCheck date); `V18_02` adds /systemHealth
   hot-path indexes (log-ingestion cursor, CuttingPlanMaterial(Placement) bulk-update,
   CuttingRequestBox(wo), CuttingRequestSerie matelassage/coupe timelines). Two satellite-DB
   indexes (qualite, MG_PLS_NEW) are noted in V18_01 but applied by hand. Remaining gaps: see
   `docs/recommendations.md`.
2. **Import is serial and chatty**: one POST per sequence; each runs ~10 MAX()/LIKE scans
   (incl. `MAX(CAST(id AS INT))` on 235k boxes and `MAX(NSerieGammeImp)` on 130k legacy rows)
   plus 5+ mirror inserts into qualite per part number, all row-by-row.
3. **History tables grow without bound**: CuttingPlanHistory 326MB, GammeTechniqueImprimerHistorique,
   CoupePerformance/CoupeDrill 600k+ — queries must always be date- or key-bounded.
4. **String-typed numerics** in legacy mirrors (`suiviplanning.*`, `Scan_Rouleau.quantite`,
   box ids) force CASTs and prevent index seeks when filtered numerically.
