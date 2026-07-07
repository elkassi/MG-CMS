# CMS-Prod Kiosk Integration Plan (`Form.js` / `FormCoupeNew.js` / `FormMix.js`)

> **Project:** CMS-Prod (React 17, class components, Redux installed but
> unused) — Lear Tangier TFZ.
> **Goal:** Give floor operators a **clear, pre-computed "what to work
> next"** signal that flows from Plan de Charge → Sequence Dispatcher →
> Advanced Ordonnancement → the kiosk — without breaking the current
> shift / manual-pick flow.
> **Companion plans:** `PLAN_DE_CHARGE_IMPROVEMENT_PLAN.md`,
> `SEQUENCE_DISPATCHER_PLAN.md`,
> `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`.

---

## 0. What operators need that they don't have today

From research:
- All three forms load series via
  `/api/cuttingRequestSerieData/filter?date=&shift=&machine=` on mount.
- The list is **not** paginated and is sorted client-side.
- Operators **manually pick** a serie (dropdown) — there is **no**
  "next serie" signal.
- No WebSockets, polling every 2–60 s.
- Machine identity resolved via `/api/productionTable/pc-mine`.

What's missing for the new plan to work end-to-end:
1. A **next-serie** endpoint backed by `MachineQueue` (not a static
   filter list).
2. A visible **banner** on Form.js / FormMix / FormCoupeNew telling the
   operator what's next.
3. A **"serie was reassigned to another machine"** toast — the Optimizer
   can move a Waiting serie out of this machine's queue at any time.
4. An **offline outbox** so matelassage / coupe events don't drop when
   the wifi blinks (out-of-scope here but called out).

---

## 1. The only new backend endpoint we need (kiosk-facing)

Add to an existing controller (extend
`OrdonnancementController` or a new `MachineQueueController`):

```
GET  /api/machineQueue/next?machine={machineNom}&count=3
```

Returns (projection, **never** entities):

```java
record NextSerieDto(
    int queuePosition,           // 1 = current, 2 = next, 3 = after-next
    String serie,
    String sequence,
    String partNumberMaterial,
    Double longueur,
    Integer nbrCouche,
    String placement,
    Double estimatedCuttingTime, // pre-computed by Optimizer via
                                  // CuttingTimeCalculator (contract C1)
                                  // — joins CMS-DB TimingModel by placement,
                                  // applies LASER × nbrCouche / Gerber × 2,
                                  // then × 1/efficienceTarget
    String estimatedStartTime,   // ISO
    String estimatedEndTime,
    String zone,                 // resolved zone for THIS serie:
                                  //   strict zone if its machineType is
                                  //   present there; otherwise the SHARED
                                  //   zone holding that type
                                  //   (per SEQUENCE_DISPATCHER_PLAN §3.8)
    String machineType,          // == CuttingRequestSerie.machine
                                  //   (the machine-type NAME, not a machine id)
    String rouleauHint,          // nullable — see §4
    Boolean unassignable         // true when a row exists in
                                  // UnassignableSerie for this serie
                                  // (dispatcher §3.7)
){}
```

Reads directly from `MachineQueue` where `machineNom = :m`, ordered by
`queuePosition ASC`, limit `count`. No @Entity fetch: use a dedicated
@Query projection in `MachineQueueRepository`. The CMS-DB `TimingModel`
join is already done upstream by the Optimizer when it wrote the
`MachineQueue.estimatedCuttingTime` column — the kiosk endpoint never
re-derives the time, it just reads it back. This guarantees C1
(cutting-time single source) holds end-to-end.

Why `count=3`: the banner shows current + next + after-next so the
operator can pre-stage the roll for serie #2 while serie #1 cuts.

Second endpoint for the Chef de Zone / Ordonnancement UI (already
planned in `SEQUENCE_DISPATCHER_PLAN §7`), same projection but filtered
by zone — no new work here.

---

## 2. "Next serie" banner — JSX placement

Based on research, Form.js around **line 6641–6645** is the least
invasive insertion point. Proposed component:

```jsx
{this.state.nextQueue && this.state.nextQueue.length > 0 && (
  <div className="next-serie-banner" role="status">
    <NextSerieCard serie={this.state.nextQueue[0]} tone="current" />
    {this.state.nextQueue[1] && (
      <NextSerieCard serie={this.state.nextQueue[1]} tone="upcoming" />
    )}
    {this.state.nextQueue[2] && (
      <NextSerieCard serie={this.state.nextQueue[2]} tone="later" />
    )}
  </div>
)}
```

`NextSerieCard` is a tiny stateless component in
`src/main/js/components/Layout/common/NextSerieCard.js` that renders:

```
┌────────────────────────────────────────────┐
│ SUIVANTE  #987  · séq. 2026-04-20-12       │
│ rouleau R-1234 · 612 min · fin ≈ 14:22     │
│ placement P3 · 18 couches · LECTRA-03      │
└────────────────────────────────────────────┘
```

Tone classes:
- `current` — green outline, bold. Shown whenever `queuePosition = 1`
  and the operator has not yet started it (i.e. no `dateDebutMatelassage`).
- `upcoming` — amber.
- `later` — grey, small font.

Accessibility: `role="status"` so screen readers announce changes.

### Placement in the other forms
- `FormMix.js` — same component, inserted after the mix-mode serie
  dropdown (around the render block that shows the current serie).
- `FormCoupeNew.js` — placement just above the `Section` split buttons
  so the operator sees *next* before ending current.

---

## 3. Data flow — how the banner stays live

Polling (no WebSockets yet, per research) on a 30 s interval:

```
Form.js didMount
   → GET /api/productionTable/pc-mine      // existing
   → GET /api/machineQueue/next?machine=X&count=3
      every 30 s + on any POST finishing a serie
```

On server side, `MachineQueue` is already kept fresh by the
Continuous Optimizer (see
`ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §2`). When the Optimizer
writes a new queue, it also bumps a `MachineQueue.version` counter
(new column — tiny migration).

Add a lightweight head endpoint:

```
GET /api/machineQueue/version?machine={machineNom}   -> {"v": 17}
```

Kiosk polls this every 5 s (cheap — one int read). Only refetches the
full 3-item list when `v` changes. This gives us the WebSocket
responsiveness without introducing a WS channel in CMS-Prod yet.

### Optional WebSocket (deferred)

Add to Phase 3: subscribe `/topic/machine/{machineNom}` and drop the
version polling. Reuse the STOMP config that MG-CMS already exposes.

---

## 4. Roll hint (`rouleauHint`)

Operators' biggest friction is hunting for the right roll. The
Optimizer already knows — in its feasibility check — which roll it
*assumed* to be available. Surface it:

- Before the Optimizer places a serie, it calls a shared
  `RollFeasibilityService` that reads `ScanRouleau` live (no
  pre-reservation per memory).
- That service returns a candidate roll id (the current best match).
- The optimiser does **not** reserve it, but it does **remember** the
  id on the `MachineQueue` row (new nullable column `rouleauHint`).
- The kiosk simply renders it.

If the roll has already been consumed by the time the operator scans,
the existing FormCoupeNew verification still catches the mismatch —
the hint is just a hint.

---

## 5. "Serie reassigned" toast

Flow: Operator is about to start serie 987 on LECTRA-03. The
Optimizer reshuffles — 987 is now on LECTRA-04.

1. The `/api/machineQueue/version` poll detects a bump.
2. Kiosk refetches the 3-item queue. Serie 987 is no longer in it.
3. Kiosk compares previous vs new queue → emits a toast:
   `«Série 987 a été réassignée à LECTRA-04»` with a *Comprendre*
   button that opens a modal explaining the move (reason code from
   `OptimizerRun.last_move`).

Prevents operators from starting a serie the system has already
moved away.

---

## 6. Offline outbox (brief — full design lives in `GENERAL_IMPROVEMENT_PLAN.md`)

The banner + reassignment toast are **read-only**. They degrade
gracefully: when offline, the kiosk shows a stale banner and dims it.
No new outbox work for this plan — but it is a pre-requisite for the
full kiosk reliability story; keep the `GENERAL_IMPROVEMENT_PLAN`
in sync.

---

## 7. Phases

| Phase | Scope | Priority | File(s) |
|---|---|---|---|
| **P1** | Backend: `NextSerieDto`, `/api/machineQueue/next`, `/api/machineQueue/version`, `MachineQueueRepository` projection queries | **HIGH** | `controller/MachineQueueController.java`, `repository/MachineQueueRepository.java`, `controller/dto/NextSerieDto.java` |
| **P2** | Add `MachineQueue.version` column + bumped on every optimiser write | **HIGH** | migration, `OrdonnancementService.saveQueues` |
| **P3** | `NextSerieCard` + `next-serie-banner` CSS; integrate into Form.js (L6641) | **HIGH** | `common/NextSerieCard.js`, `Form.js` |
| **P4** | Same integration in `FormMix.js` | **HIGH** | `FormMix.js` |
| **P5** | Same integration in `FormCoupeNew.js` | **MEDIUM** | `FormCoupeNew.js` |
| **P6** | Reassignment toast + *Comprendre* modal (reads `OptimizerRun.last_move`) | **MEDIUM** | `Form.js`, `FormMix.js`, `FormCoupeNew.js`, `common/ReassignmentToast.js` |
| **P7** | `rouleauHint` column on `MachineQueue` + optimiser fills it | **MEDIUM** | migration, `RollFeasibilityService`, optimiser wiring |
| **P8** | WebSocket replacement of version polling | **LOW** | MG-CMS `WebSocketConfig.java`, CMS-Prod `Form.js` init |

Ship P1–P4 together — they make the banner a real thing. P5 is same
code, just same-day follow-up. P6 onward is polish.

---

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| The banner shows the wrong serie because the optimiser hasn't written yet | Show `—` (no banner) when `MachineQueue` is empty for this machine; never guess |
| Operator starts the "current" serie before the queue is refreshed after a big re-optimise | The `POST /api/scheduling/admit` endpoint (`ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §6`) returns `409 ZONE_NOT_ACCEPTED` or `409 MACHINE_TYPE_MISMATCH` so the operator sees a hard block, not silent drift. The matelassage endpoint also extends its 409 validation to "this serie is no longer assigned to your machine". |
| Operator manually picks a serie that is on a STRICT zone the chef hasn't accepted | Same admission-control 409 path → red block modal, `[Demander à Process]` button, no Continue. Tracked in `AdmissionBlockedAudit` for the dispatcher's audit panel. |
| 30 s polling × 30 kiosks × version checks = too many requests | Version endpoint is O(1) — one `SELECT MAX(version) FROM MachineQueue WHERE machineNom=?` with index. Negligible. |
| Operator ignores the banner | It is advisory. The form's existing manual serie picker still works for overrides. A *rejet-reason* is logged when they pick a serie other than queuePosition 1 → feeds back into the optimiser weights |
| Roll hint misleads operator because roll was already consumed | Hint, not command. The current FormCoupeNew roll verification logic already catches mismatches |

---

## 9. Open questions

1. **Banner dismissible?** If the operator is confident, do they want a
   `[Cacher]` button, or should the banner always be on? (I lean
   always-on — it's information, not a modal.)
2. **Rejection reason menu.** When the operator picks a serie that
   isn't `queuePosition 1`, what options do they need? (*matière
   absente / machine bloquée / changement outil en cours / autre*.)
   This list feeds the optimiser's future weight tuning.
3. **Toast dismissal.** The "serie reassigned" toast — auto-dismiss
   after 10 s, or require a click? Operators tend to ignore
   auto-dismissing toasts, but requiring a click stops the flow.
4. **Machine type specifics.** Is the banner the same layout on
   LASER-DXF vs Lectra? I designed it generic; do LASER operators
   need additional info (camera-alignment flag, drill count)?
5. **Roll hint confidence.** If the optimiser isn't sure about the
   roll (two equally good candidates), show one + a `(+1 autre)`
   counter, or both? My draft shows one and lets the operator scan
   freely.
6. **Version-column source-of-truth.** Global `MachineQueue.version`
   (one big counter) vs per-machine counter? Per-machine avoids
   unnecessary refetches on other kiosks. I'd go per-machine.
7. **Shift handover.** At shift boundary the queue rows for the
   ending operator may still be `queuePosition = 1` with
   `statusMatelassage != Complete`. Should the banner hide these so
   the incoming operator doesn't re-do them? My draft hides any row
   whose `dateDebutMatelassage != NULL AND dateFinMatelassage = NULL`
   (in progress) — the incoming op sees the operation as in-flight
   but not as "next".
8. **Machine-type mismatch after optimiser move.** If a serie is
   re-queued on a different machine *type* (rare, but possible when a
   serie has multiple compatible types), the kiosk toast wording
   needs to be clearer. Dedicated wording, or the same generic
   message?
9. **Polling fallback.** When `/api/machineQueue/version` 500s, should
   the banner grey out immediately, or keep the last known queue for
   N cycles? My draft keeps last known for 3 cycles (90 s).
10. **Analytics.** Do we want a `KioskBannerEvent` audit table to
    measure "% of starts that followed the banner vs overrode it"?
    It would be great data for optimiser tuning but is extra surface.
