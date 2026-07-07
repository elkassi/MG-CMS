# Process Workbench Sequence Focus Update

## Goal

The Process Workbench must stop treating boxes as independent flow units. In the real zone flow, a sequence opens all of its end-of-line boxes when the sequence starts. Those boxes only leave the zone when every `CuttingRequestSerie` in the sequence is complete. The workbench therefore has to help the team choose which sequence to open or finish next, because the fastest result is not "one box finished quickly"; it is "the whole opened sequence released quickly".

This update adds a lightweight sequence-focus layer on top of the balanced dispatcher result. It does not replace the dispatch balancing engine. The engine still decides the best zone/machine spread first. After that, the workbench turns the current balanced schedule into operational guidance for:

- Chef de zone: which sequence is open, just added, or about to be added so the end-of-line boxes can be prepared.
- Logistics: which fabrics are needed in the next 2 hours, which ones are missing from the zone rack, and which rack materials can be removed because they are not needed soon.

## Business Rules

1. Load only the current due bucket and older work:
   - Include active sequences where `dueDate < current dueDate`.
   - Include active sequences where `dueDate = current dueDate` and `dueShift <= current dueShift`.
   - Exclude upcoming due shifts, even if the sequence exists in the database or a stale schedule table.
   - Include only `sequenceStatus` equal to `ACTIVE`, `Active`, `active`, or `null`.

2. Sequence lifecycle:
   - The physical serie lifecycle is `WAITING -> SPREADING -> READY_TO_CUT -> CUTTING -> COMPLETED`.
   - `WAITING`: the serie has not started yet. This is the only state the engine may reorder for future ordonnancement.
   - `SPREADING`: matelassage is currently working. The serie is already on its matelassage table and must not be moved.
   - `READY_TO_CUT`: matelassage is complete. The serie is waiting on the same table unless `tableCoupe` has explicitly changed.
   - `CUTTING`: the cutting machine is working on the serie. It is fully anchored.
   - `COMPLETED`: historical/completed work. It remains before active/future work when sorting by `dateDebutCoupe`.
   - A sequence is "open" when any serie has started matelassage/coupe or has a non-waiting production status.
   - When a sequence opens, all boxes at the end of the line are considered opened.
   - Boxes are not released one by one.
   - The sequence releases all boxes only when all series in that sequence are complete.
   - In the ordonnancement table, filtering one machine and sorting by `dateDebutCoupe` should show the physical order: `COMPLETED -> CUTTING -> READY_TO_CUT -> SPREADING -> WAITING`.

3. Operational priority:
   - Open sequences have priority because their boxes are already occupying the zone.
   - Unopened sequences are candidates only when they appear in the balanced schedule and are near the next start window.
   - A new unopened sequence should be prepared before its first planned serie starts, but should not be physically treated as final while the engine is still balancing.
   - Material shortages should not block the dispatcher from balancing, but they must reduce confidence and raise logistics actions.
   - For future `WAITING` rows, short-length series of the same `partNumberMaterial` should be grouped consecutively inside the same due bucket to reduce spread setup churn.

## Sequence Focus Score

The focus score is a simple in-memory ranking, not a second optimizer.

Priority factors:

- `openBoost`: opened sequences get a large boost so the zone closes work already occupying boxes.
- `boxReleaseValue`: `boxCount / predictedCloseMinutes`; this favors sequences that release many boxes quickly.
- `dueUrgency`: older due dates and earlier due shifts stay ahead of newer work.
- `startWindow`: a sequence scheduled to start soon becomes visible to the chef before boxes are needed.
- `materialPenalty`: missing or out-of-zone material lowers confidence and pushes logistics actions.

The output is intentionally small: only the top focus items and the material summary per zone are sent to the UI.

## Backend Shape

The workbench response gets a new `sequenceFocus` object:

```json
{
  "generatedAt": "2026-05-22T10:00:00",
  "horizonMinutes": 120,
  "dispatchBalanced": true,
  "zones": [
    {
      "zone": "ZA",
      "openSequenceCount": 2,
      "aboutToStartCount": 1,
      "materialIssueCount": 1,
      "boxOccupancy": {
        "activeMachines": 2,
        "maxBoxes": 32,
        "occupiedBoxes": 14,
        "occupancyPct": 43.75,
        "occupiedSequences": []
      },
      "focusSequences": [
        {
          "sequence": "12345",
          "state": "OPEN",
          "action": "FINISH_SEQUENCE",
          "boxCount": 8,
          "remainingSeries": 3,
          "totalSeries": 7,
          "predictedCloseMinutes": 42,
          "firstStart": "2026-05-22T10:05:00",
          "lastEnd": "2026-05-22T10:42:00",
          "materialStatus": "OK"
        }
      ],
      "chefAlerts": [
        {
          "sequence": "12346",
          "state": "READY_TO_START",
          "action": "PREPARE_BOXES",
          "minutesToStart": 18
        }
      ],
      "logistics": {
        "status": "ATTENTION",
        "materials": [],
        "shortages": [],
        "transferSuggestions": [],
        "returnCandidates": []
      }
    }
  ]
}
```

## Material Logic

The logistics view uses the same 2-hour horizon as the sequence-focus view.

Demand source:

- Prefer the persisted engine schedule entries after balance.
- Restrict to active current-or-overdue sequences already present in the workbench live charge.
- Use each serie's `refTissus` and `tableLengthRequired` as the lightweight demand estimate.
- If no schedule row exists, fall back to open/near sequence series from `LiveChargeDto`.

Stock source:

- Use the existing `ScanRouleau` light projection already loaded by `WorkbenchCacheService`.
- Map `ScanRouleau.emplacement` to a zone using `Zone.rollLocations`.
- Compare needed meters to available meters in the target zone.
- If the material exists outside the zone, show transfer options.
- If material is in a zone rack but not needed in the next 2 hours, show it as a return/remove candidate.

Status values:

- `OK`: enough material in the zone rack.
- `OUT_OF_ZONE`: enough total material exists, but some must be brought from another zone.
- `SHORTAGE`: some material exists, but total production stock is not enough.
- `NONE`: no scanned production stock exists for that material.
- `RETURN_TO_STOCK`: material is present in the zone rack and not needed in the 2-hour horizon.

## UI Update

`/processWorkbench` changes from a chart-first page into an operation board:

1. New default tab: `Focus Séquence`
   - Chef de zone cards show:
     - open sequences to finish,
     - sequences about to start,
     - sequences just added by the balanced schedule,
     - box count and remaining series,
     - material confidence.
   - Logistics panel shows:
     - 2-hour material status by zone,
     - shortages and out-of-zone transfers,
     - material to remove from racks when not needed soon.

2. Existing tabs remain for deeper inspection:
   - Shift completion
   - Dispatching of sequence
   - Ordonnancement of serie
   - Prévision Matière

3. The old box status chart can be removed from the main tab flow because it does not match the real sequence-level box release rule.

## Performance Guardrails

This feature must stay light because the server has shown long-run freezes during tests.

Rules:

- No new continuous optimizer loop.
- No per-client SQL polling for focus data.
- Compute focus inside the existing workbench cache refresh.
- Use already-loaded `LiveChargeDto` and `ScanRouleau` rack data.
- Query schedule entries in one batch per cache refresh and filter them by current active sequences.
- Throttle engine preload while the workbench is idle.
- Poll the frontend at cache-friendly intervals instead of every second.
- Do not refresh upcoming shift work in the scheduled workbench cache.

## Implementation Notes

Backend:

- Add `WorkbenchSequenceFocusService`.
- Add a batched repository method for current active schedule entries.
- Attach `sequenceFocus` in full and incremental workbench cache responses.
- Add preload throttling in `WorkbenchCacheService`.
- Classify every schedule row by lifecycle so `WAITING` is the only movable status.
- Guard engine Level-2/Level-3 schedule moves so they cannot relocate or swap `SPREADING`, `READY_TO_CUT`, or `CUTTING` slots.
- Add per-zone box occupation: occupied open-sequence boxes over `16 * activeMachines`.

Frontend:

- Add `SequenceFocusView`.
- Make it the default `/processWorkbench` tab.
- Remove the old `Box status` tab from the main tab list.
- Keep the existing material forecast tab for deep process/admin analysis, but use the new focus logistics panel for the daily 2-hour rack action view.
- Show each zone's occupied sequences and box load against its physical box capacity.

## Expected Shopfloor Behavior

Chef de zone opens `/processWorkbench` and immediately sees:

- which opened sequence should be finished first,
- which sequence is about to enter the zone,
- how many boxes are already occupying the zone versus the zone limit,
- which opened sequences are consuming those boxes,
- whether the engine is still balancing or the plan is stable enough to prepare physically.

Logistics opens the same page and sees:

- material needed in the next 2 hours by zone,
- what must be brought from another zone,
- what is not needed again soon and can be removed from the rack.
