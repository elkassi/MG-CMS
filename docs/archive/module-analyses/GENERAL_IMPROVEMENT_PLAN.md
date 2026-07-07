# MG-CMS — General Improvement Plan

> **Project:** MG-CMS (Spring Boot + React + SQL Server) + CMS-Prod (React)
> **Scope:** Cross-cutting improvements that are **not** covered by any
> existing document in `md/` or `plans/`. This plan deliberately avoids
> duplicating the ordonnancement work (see
> [ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md](ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md))
> and the Preparation work (see
> [PREPARATION_IMPROVEMENT_PLAN.md](PREPARATION_IMPROVEMENT_PLAN.md)).

---

## 0. Why this plan exists

After scanning the codebase (177 JPA entities, 162 controllers, 93 React
pages, 6 integration databases, ~5100-line CuttingPlanForm.js, 2912-line
metadata.js), a few themes show up that no existing doc addresses. These
are the "quiet" risks — the ones that don't have a dedicated slide deck
yet but will bite production within 6–18 months.

Each section below follows the same structure:
**Why it matters → What's missing → What to do → Effort tier (S/M/L).**

---

## 1. Traceability & audit — the missing layer under *everything*

**Why it matters.** Your system drives physical production. Every hour,
decisions on quantity, quality, and scheduling are written to SQL Server.
When something goes wrong (wrong partnumber shipped, quality defect root
cause, scheduling dispute with a supplier), the first question is always:
*who changed this, when, from where?* Today, there is almost no answer.

**What's missing.**
- No JPA `@EntityListeners(AuditingEntityListener.class)`.
- `@CreatedBy` / `@LastModifiedBy` / `@CreatedDate` / `@LastModifiedDate`
  are absent from entities like `CuttingPlan`, `CuttingRequestSerie`,
  `QualityNotice`, `WorkOrder`, `PartNumberMaterialConfig`.
- Manual history tables exist only for a few entities
  (`CuttingPlanHistory`, `RouleauHistorique`, `QualityValidationHistory`)
  and require explicit service-layer calls — easy to forget.
- Login events are not persisted anywhere queryable.

**What to do.**
- Enable Spring Data JPA auditing: `@EnableJpaAuditing` + an
  `AuditorAware<User>` that reads from the security context.
- Add the four audit fields to a base `@MappedSuperclass AbstractAuditable`
  and let ~20 production-critical entities extend it.
- Add a generic `ChangeLog` table (entity_name, entity_id, action,
  diff_json, user_id, ip, ts) populated by a single Hibernate interceptor
  — one place, covers all entities, no per-service code.
- Persist `LoginEvent` (user, ts, ip, user-agent, success/failure).

**Effort:** M (2 weeks including testing on hot paths).

---

## 2. Concurrency & optimistic locking

**Why it matters.** You run 3 shifts, multiple users on the same
`CuttingPlan`, multiple operators touching the same `CuttingRequestSerie`
through MG-CMS and CMS-Prod simultaneously. Right now, last-write-wins.
I didn't find `@Version` on any of the hot entities.

**What's missing.**
- No `@Version` field → silent lost updates.
- No row-level retry strategy in services.

**What to do.**
- Add `@Version Long version` on: `CuttingPlan`, `CuttingPlanMaterial`,
  `CuttingRequestSerie`, `CuttingRequestBox`, `WorkOrder`, `QualityNotice`,
  `ProductionTable`, `Rouleau`.
- Backend returns `409 Conflict` on `OptimisticLockException`, with a
  payload containing `serverVersion` and `serverPayload`.
- Frontend shows a "Cette ligne a été modifiée par X à HH:mm, voulez-vous
  (Écraser / Recharger / Fusionner) ?" modal. Start simple: only
  Recharger (read-only refresh) is mandatory; Écraser is admin-only.

**Effort:** S backend, M frontend.

---

## 3. Database migrations — Flyway or Liquibase

**Why it matters.** Today schema changes rely on Hibernate `ddl-auto`
in non-prod and manual SQL scripts in prod. You have 177 entities and
6 integration DBs — this is a ticking bomb. When a dev adds a column,
there is no reviewable artifact, no ordered replay, no rollback.

**What's missing.** Flyway / Liquibase are not in `pom.xml`.

**What to do.**
- Pick Flyway (simpler, SQL-first, matches the team's existing SQL scripts
  in the `SQL/` folder).
- Baseline the current schema from prod (`flyway baseline`), then
  require every future change to ship as `V{yymmdd.HHmm}__{slug}.sql`.
- Add a CI check: `ddl-auto=validate` on the dev profile once baselined.
- Ship a `R__reference_data.sql` repeatable migration for seed data
  (zones, machine types, shift times, defect codes).

**Effort:** M (the baseline is the slow part).

---

## 4. Testing strategy — you have 6 test files for 162 controllers

**Why it matters.** With < 1 % of the codebase under test, every refactor
is a roll of the dice. The ordonnancement algorithm, the split/fuse logic,
the weight calculation, the quality validation — none of these are
reliably verifiable without production data.

**What's missing.**
- No unit tests on pure-logic services (e.g. `OrdonnancementService`,
  `PlanDeChargeService`, weight calculation).
- No Spring `@DataJpaTest` for repositories.
- No MockMvc integration tests.
- No E2E (Cypress/Playwright) on the 5 or 6 critical user journeys.

**What to do (pragmatic, prioritised).**
- **Round 1 — pure logic, no DB.** Unit test every method in
  `OrdonnancementService`, `PlanDeChargeService`, weight calculators,
  `WorkOrderService.split/fuse`. Target: 80 % line coverage of these
  services.
- **Round 2 — repository tests.** `@DataJpaTest` on repositories with
  non-trivial `@Query`. H2 is fine for most, but for anything that uses
  SQL Server window functions switch to Testcontainers with
  `mcr.microsoft.com/mssql/server`.
- **Round 3 — 6 E2E journeys.** (i) Importer crée une séquence,
  (ii) CAD édite un cutting plan, (iii) Process lance auto-dispatch,
  (iv) Operator ferme une série via CMS-Prod, (v) Quality ouvre un QN,
  (vi) Admin split/fuse un WO.

Skip round-trip integration tests between MG-CMS and CMS-Prod at first —
they are slow, flaky, and the payoff only comes at round 3.

**Effort:** L, but the first 30 tests (round 1) are S and already remove
most regressions.

---

## 5. Observability — you can't fix what you can't see

**Why it matters.** 400 series/shift × 24 h = 9600 state transitions per
day. When KPI numbers look odd, today you have to SSH into a box and
read logs. There is no dashboard to answer "which machine is currently
down?", "which API is slow?", "which user hit an error?".

**What's missing.**
- No Micrometer metrics beyond Actuator defaults.
- No structured logging (JSON).
- No distributed tracing between MG-CMS and CMS-Prod.
- No error aggregation (Sentry / Glitchtip / self-hosted).

**What to do.**
- Add Micrometer + Prometheus exporter; scrape with a Prometheus pod on
  the intranet; Grafana dashboards for: JVM, HTTP latency per endpoint,
  JDBC pool saturation, scheduler queue depth, reconciliation daemon
  health.
- Switch logback to JSON with `logstash-logback-encoder`; centralise in
  Loki or plain file + `lnav`.
- Add a correlation id filter (`X-Request-Id`) propagated from CMS-Prod
  into MG-CMS and into SQL via a session variable.
- Self-hosted Sentry (Glitchtip if you want minimal dependencies) for
  both Java and JS.

**Effort:** M.

---

## 6. Security hardening

**Why it matters.** You run on a plant intranet which feels safe but is
not — USB sticks, contractors, and laptop migrations happen. The
recommendations in `md/RECOMMENDATIONS.md` flag credentials in
`application.properties` and an EOL Spring Boot; this plan adds what's
still missing beyond those.

**What's missing (not yet in RECOMMENDATIONS.md).**
- WebSocket security is `permitAll()` on `/api/**` (per
  `WebSocketConfig.java`) — any authenticated (or unauthenticated?)
  client can subscribe to any topic.
- No CSRF strategy documented (JWT in localStorage is OK but exposes you
  to XSS token theft).
- JWT secret rotation policy unclear.
- Frontend loads 3rd-party libs (DevExpress Scheduler) — no CSP.
- No rate limiting on login or on the heavy `autoDispatch` endpoint.

**What to do.**
- WebSocket: require authenticated session on CONNECT, restrict topic
  subscription per role (`simpSubscribeDestMatchers(...).hasRole(...)`).
- Consider moving JWT from localStorage to an HttpOnly cookie for the
  main app (still OK to keep localStorage for CMS-Prod kiosks behind the
  plant firewall).
- Add bucket4j-based rate limiting on `/login` and `/api/scheduling/autoDispatch`.
- CSP header via a `WebSecurityConfig` filter; start report-only.
- Rotate JWT secret on every redeploy; have a documented procedure.

**Effort:** S–M depending on scope.

---

## 7. CuttingPlanForm.js at 5100 lines — a special case

`md/CAD_RECOMMENDATIONS.md` already addresses this well. This plan adds
only one thing that document doesn't: **a server-side mirror of the
client verification logic**. Today, the 7-point verification runs in
JavaScript, which means a rogue curl call can bypass it. Mirror the
checks in a `CuttingPlanValidator` Spring bean called before `save()`.
It is also a clean place to attach unit tests.

**Effort:** M.

---

## 8. Real-time operator feedback — close the UX loop

**Why it matters.** Operators on CMS-Prod screens do not currently know
when the central plan has been updated. They reload the page or refresh
the list. With a persistent plan (see ordonnancement plan Phase 1), we
can push updates.

**What's missing.** No WebSocket topic tailored to a specific machine.

**What to do.**
- Topic `/topic/machine/{machineName}/worklist` broadcasts every time
  that machine's worklist cache is rebuilt.
- CMS-Prod subscribes via existing SockJS + STOMP setup.
- A small toast "Nouvelle série #1234 planifiée — cliquer pour rafraîchir"
  is enough.

**Effort:** S.

---

## 9. Offline resilience for CMS-Prod

**Why it matters.** CMS-Prod runs on kiosk PCs next to cutting tables.
The plant Wi-Fi flaps. Today, if the backend is unreachable for 30 s,
the operator is stuck — the form cannot save `dateFinCoupe`, and later
silently forgets (this is one of the root causes of your pain points).

**What's missing.** No local queue in Form.js / FormCoupeNew.js / FormMix.js.

**What to do.**
- IndexedDB-backed outbox: every POST that fails with a network error is
  queued locally with a UUID.
- A background worker retries every 5 s with exponential back-off.
- UI shows a small badge "3 événements en attente de synchronisation".
- Server must be idempotent on client-provided UUID (add `clientEventId`
  column to `Coupe`, `Matlassage`).

This single change will measurably reduce the NULL `dateFinCoupe` rate
independently of the ordonnancement plan's reconciliation daemon.

**Effort:** M.

---

## 10. Dependency / platform hygiene (beyond what RECOMMENDATIONS.md says)

- **Node & webpack.** `webpack.config.js` exists but is likely old;
  consider a `vite` migration for the React build (faster dev loop).
- **Java upgrade path.** Spring Boot 2.5.3 → 2.7 → 3.x requires a jakarta
  migration. Stage it: 2.7 first, settle 1 month, then 3.x.
- **SQL Server driver.** Pin a specific version of `mssql-jdbc` in
  `pom.xml`; test against the prod SQL Server major version.
- **DevExpress licence & version.** The scheduler is the one 3rd-party
  lib that drives the KPI UI — keep it under licence renewal alerts.
- **Lockfile hygiene.** Commit a single lockfile per app (you currently
  have `package-lock.json` in both `MG-CMS/` and `MG-CMS/MG-CMS/`).

**Effort:** S for housekeeping, L for the Spring Boot 3 jump.

---

## 11. Disaster recovery & data lifecycle

**Why it matters.** 6 integration databases, no documented DR plan, and
history tables growing indefinitely.

**What's missing.**
- No documented RPO / RTO.
- No partitioning on `CuttingRequestSerie`, `ScanRouleauHistorique`,
  `CoupeMachineHistory`, `EtatMachineHistorique` — these will dominate
  disk in a few years.
- No export / purge job for data older than N years.

**What to do.**
- 1-page DR runbook: backup frequency, location, restore procedure,
  tested once per quarter.
- Table partitioning on the 4 high-volume history tables
  (by `YEAR(date_col)` or sliding window).
- Archival job: move rows older than 3 years to a `*_archive` table on a
  cheaper filegroup.

**Effort:** M.

---

## 12. People processes — not code, but cheap wins

- A shared `CHANGELOG.md` at the root of MG-CMS. Today there are
  `CHANGES_LOG.md` and `CHANGES_LOG_MARCH_2026.md` — promote them to a
  single canonical timeline.
- A `docs/` folder with architecture diagrams (C4 model) kept in git.
  Most of your `md/` files describe features, none describe the whole
  system. One diagram would pay itself back in every onboarding.
- Renovatebot or Dependabot on the repo for JS and Java deps. Low effort,
  constant value.
- A lightweight ADR (Architecture Decision Record) folder. Each big
  choice (scheduler algorithm, persistence layer, zone policy) gets a
  1-page `ADR-NNNN.md`. Prevents "why did we do X?" archaeology in 2028.

**Effort:** S.

---

## 13. Features you don't have yet that I'd strongly consider

These are longer-term ideas — listed without full specs because each
deserves its own plan if it resonates.

1. **Operator skill matrix & performance view.** Today `FormCoupeNew.js`
   captures `coupeur1/2` and piece counts. You have 6+ months of data
   going unused. A dashboard per operator, per machine, per material
   would unlock coaching and fair shift rotation.

2. **Predictive maintenance hook.** You already track `EtatMachineHistorique`
   with status codes and durations. Feed this into a simple mean-time-
   between-failures calculator per machine; flag a "maintenance due
   soon" amber light when MTBF_remaining < next scheduled block.

3. **Material-roll forecasting.** `Rouleau` + `ScanRouleau` + upcoming
   sequences' material needs = a 72-hour "will we run out?" view. Today
   this is tribal knowledge. A single page could replace 3 WhatsApp groups.

4. **What-if simulator for the scheduler.** Once the persistent
   `SchedulingPlan` exists, cloning a plan + mutating an input (e.g.
   "what if Lectra-03 is down all afternoon?") is almost free. Give it
   to the shift leader as a decision-support tool.

5. **Cross-plant view.** If Lear runs the same stack in other plants, a
   read-only aggregator could spot best-performing configurations per
   (material, machine, placement). Longer horizon, but the foundation
   is the `CuttingSpeedEmpirical` table proposed in the ordonnancement
   plan.

6. **In-app help powered by the LLM plan.** The
   [LOCAL_LLM_INTEGRATION_PLAN.md](LOCAL_LLM_INTEGRATION_PLAN.md) is a
   beautiful foundation. A concrete first use-case: "why is this serie
   late?" — the LLM reads the relevant rows and explains in French.
   This is far more valuable as a first LLM integration than a generic
   chat widget.

7. **A printable operator dashboard per table.** A small 7-inch
   next-to-the-table display showing the next 3 series, material, and
   a physical-position hint. Pure HTML, no login, kiosk URL.

---

## 14. Suggested sequencing (6-month view)

| Month | Focus | Deliverables |
|---|---|---|
| 1 | Foundation | Flyway baseline, `@Version`, audit superclass, JSON logs |
| 2 | Ordonnancement v1 | Phase 1+3 of the advanced ordonnancement plan |
| 3 | Observability | Prometheus + Grafana, Sentry, correlation id |
| 4 | Operator UX | Offline outbox in CMS-Prod, WebSocket push to kiosks |
| 5 | Ordonnancement v2 | Phase 2+4+5 (admission control, rolling horizon) |
| 6 | Insights | Operator skill matrix, empirical-speed dashboard, first LLM use-case |

Testing and security hardening run in parallel, not as separate months —
they are everyone's job every week.

---

## 15. What I'd ask you before starting

1. **Team size.** How many devs on MG-CMS day-to-day? (Drives whether
   Flyway / testing / audit can be done in parallel with ordonnancement
   or sequentially.)
2. **Deployment cadence.** Weekly? Monthly? Any change-window constraints
   from Lear IT?
3. **Plant IT constraints.** Is Prometheus / Grafana allowed on the
   plant network, or must observability go through Lear corporate?
4. **Compliance context.** Is this app in any IATF 16949 / TISAX audit
   scope? That changes how hard we must push on audit logging.
5. **Kiosk hardware.** What PCs run CMS-Prod? IndexedDB works on any
   modern Chromium, but old IE kiosks (I hope not) would force a
   different offline strategy.
6. **Data-retention rules.** Is there a corporate policy saying "keep
   production records for X years"? Drives §11.
