# MG-CMS — Code Review Remarks

_Scope:_ `src/main/java/com/lear/MGCMS/**`, `src/main/js/**`, Flyway migrations, `application.properties`, `pom.xml`, `package.json`, `webpack.config.js`.
_Date:_ 2026-04-25
_Reviewer:_ ultra-review pass covering correctness, security, design, data model, concurrency, configuration, observability, production process fit, UI/UX, and tech debt. The Sequence Dispatcher (phases 0-11) gets focused scrutiny because it is the most recent change and therefore the most likely to harbor regressions.

---

## Sprint 1 fix log — 2026-04-26

The following findings were addressed in the current session and are
marked `✅ DONE` inline below:

- **🔴 C1** — `SecurityConfig.java`: replaced blanket `GET /api/**` permitAll with kiosk-only exemption.
- **🔴 C2** — `SelfHealService` + `CuttingRequestRepository`: added `findStuckPending(cutoff)`; sweep no longer loads the table.
- **🔴 C3** — Audit insert moved to new `AdmissionAuditService` with `Propagation.REQUIRES_NEW` + `DataAccessException` swallow.
- **🔴 C4** — `EngineTickService.autoDispatchTick` diff-checks via per-(date,shift) preview fingerprint cache.
- **🔴 C5** — New `SequenceAcceptanceChangedEvent` published from `setAcceptance`; `EngineTickService` listens and bumps zone versions.
- **🔴 C6** — `setAcceptance` lives in `SequenceDispatcherService`; controller no longer holds an `@Autowired` repository.
- **🟠 H3** — Eclipse `// TODO Auto-generated method stub` in `SecurityConfig.authenticationManager()` removed.
- **🟠 H9** — `e.printStackTrace()` removed across 13 files (controllers, tasks, services, security); all replaced with SLF4J `log.error(..., e)`.
- **🟢 L3** — `JwtAuthenticationFilter` was already on SLF4J via `GenericFilterBean#logger`; no change needed.
- **🟡 M22** — `AdmissionController.check()` is now `@PreAuthorize`-gated and rejects null `Authentication`.

---

## 0 · Executive summary

| Severity      | Count | ✅ Fixed |
|---------------|-------|---------|
| 🔴 Critical    | 6     | 6       |
| 🟠 High        | 14    | 2 (H3, H9) |
| 🟡 Medium      | 23    | 1 (M22) |
| 🟢 Low         | 14    | 1 (L3)  |
| 💡 Suggestions | 11    | 0       |
| **Total**     | **68** | **10**   |

**Top 3 things to fix this week**

1. **Tighten the API security wall** — `SecurityConfig.java:73` declares `.antMatchers(HttpMethod.GET, "/api/**").permitAll()`. Every GET endpoint is publicly readable, including dispatcher previews, user-zone assignments, and audit endpoints. Lock GETs down by role and exempt only the specific paths that legitimately need to be open (login, kiosk, static assets).
2. **Replace `findAll()` in `SelfHealService.sweep()`** — `SelfHealService.java:64` loads every `CuttingRequest` row into memory each tick. Replace with a JPA query that selects only `zoneAcceptanceStatus='PENDING' AND createdAt < :cutoff` and project the fields needed.
3. **Decouple audit writes from the admission decision** — `AdmissionService.java:60` wraps both the decision and the audit-row insert in a single `@Transactional`. If the audit table is briefly unavailable the operator gets a 500 instead of an admission decision. Use `Propagation.REQUIRES_NEW` on the audit save.

**Overall health.** The codebase has clear bones and the dispatcher feature work shows mature engineering: feature flags everywhere, Flyway-versioned migrations starting at V2_*, thoughtful Javadoc, separate properties classes per cron, and visible kill-switches. The legacy code surrounding it is older Java-7-style — many `// TODO Auto-generated method stub` (Eclipse droppings), `e.printStackTrace()` in 13 files, weak null-handling in controllers, and a few God classes. The frontend bundles 90+ top-level imports without code-splitting; the dispatcher pages on the other hand are small, focused, and easy to read. Net-net: the new work is good, the older surface area is tired but not broken, and the most pressing items are security tightening and a couple of correctness bugs in the dispatcher cron services.

---

## 1 · Findings by severity

### 🔴 Critical

#### C1. ✅ DONE — All `GET /api/**` are public

**Location:** `src/main/java/com/lear/MGCMS/security/SecurityConfig.java:73`
**What:** `.antMatchers(HttpMethod.GET, "/api/**").permitAll()` exempts every GET endpoint from authentication.
**Why it matters:** Dispatcher previews, zone confirmations, audit history, user listings, and any other GET surface are world-readable to anyone on the network. Couples with the JWT filter being installed but not enforced for these paths — confidentiality is on a name-only basis.
**Fix:** Remove that line. Keep `permitAll()` only for the explicit static-asset list and `/login`. Anywhere else, default to authenticated and add narrow exemptions like `antMatchers(HttpMethod.GET, "/api/kiosk/**").permitAll()` for the shop-floor kiosks.
**Resolution (2026-04-26):** Replaced the blanket GET permitAll with `antMatchers(HttpMethod.GET, "/api/kiosk/**").permitAll()`; everything else now falls through to `.anyRequest().authenticated()`. Dispatcher previews, user/zone assignments, audit GETs all require a valid JWT.

#### C2. ✅ DONE — `SelfHealService.sweep()` loads the entire `cutting_request` table every 10 minutes

**Location:** `src/main/java/com/lear/MGCMS/services/dispatcher/SelfHealService.java:64`
**What:** `List<CuttingRequest> all = (List<CuttingRequest>) cuttingRequestRepository.findAll();` is iterated to filter by status + age.
**Why it matters:** Production has thousands of requests. Loading all of them every 10 minutes (`mgcms.engine.selfHeal.cron=0 */10 6-22 * * *`) hammers the heap and the connection pool, and the unchecked cast hides a subtle Hibernate problem if `findAll()` ever returns an `Iterable`.
**Fix:** Add a repository query: `findByZoneAcceptanceStatusAndCreatedAtBefore(String status, LocalDateTime cutoff)` and iterate that. If Hibernate over-fetches, add `@EntityGraph` for the few columns actually read.
**Resolution (2026-04-26):** Added `findStuckPending(LocalDateTime cutoff)` to `CuttingRequestRepository` (filters `zoneAcceptanceStatus='PENDING' AND createdAt < :cutoff` server-side). Sweep now logs the warnings off that scoped result set; no more table scans, no more unchecked cast.

#### C3. ✅ DONE — Audit write inside the admission transaction blocks the operator on audit-table failure

**Location:** `src/main/java/com/lear/MGCMS/services/dispatcher/AdmissionService.java:60`, `:101-105`
**What:** `check()` is `@Transactional`; `block()` calls `auditRepository.save(...)` inside the same transaction. If the audit table is locked or the row violates a constraint, the whole admission check throws and the operator sees an HTTP 500.
**Why it matters:** Admission is on the operator's hot path. An unrelated audit issue should not stop the floor.
**Fix:** Move `block()` into an `@Transactional(propagation = Propagation.REQUIRES_NEW)` method on a separate bean (or annotate the audit save with `REQUIRES_NEW`). Catch `DataAccessException` around the audit call and degrade to a `log.warn(...)` so the decision still flows.
**Resolution (2026-04-26):** Created `AdmissionAuditService.recordBlock(...)` annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)` and wrapped in a `try/catch (DataAccessException)` that logs at WARN. `AdmissionService.block()` now delegates to it through Spring's proxy so the new propagation actually applies.

#### C4. ✅ DONE — `EngineTickService.autoDispatchTick` re-publishes every 5 minutes by default

**Location:** `src/main/java/com/lear/MGCMS/services/dispatcher/EngineTickService.java:107-115`, default cron `mgcms.engine.autoTick.cron=0 */5 * * * *` in `application.properties:59`
**What:** When `mgcms.engine.autoTick.enabled=true` and `mgcms.engine.zoneAware=true`, the cron calls `dispatcher.publish(...)` (a write) for every shift on every tick — even if nothing changed since the last tick.
**Why it matters:** Every publish stamps `dispatched_zone` and fires `SequenceAcceptedEvent`, which in turn triggers `bumpVersionsForZone(...)`. Listeners and kiosks see "the queue changed" 12 times an hour, none of which represent real change. With `autoTick.enabled=true` and a busy chef, this can cause UI churn and unneeded DB writes.
**Fix:** Compute a hash of the prior `DispatchPreview` and skip the `publish` if nothing changed. Or move the cron to `0 */15 * * * *` and put the diff check in `SequenceDispatcherService.publish` so a no-op publish writes nothing and fires no events.
**Resolution (2026-04-26):** Added a per-(date,shift) fingerprint cache (`ConcurrentHashMap<String,String>`) in `EngineTickService`. Each tick computes a stable string (sorted zones + sorted sequences + unassignable list) over `dispatcher.preview(...)` and compares it to the last published one; identical fingerprint short-circuits before any write or event publish. New `autoDispatchIfChanged(date, shift)` does the diff and only calls `autoDispatch(...)` on change.

#### C5. ✅ DONE — Chef-de-zone acceptance bypasses the event chain

**Location:** `src/main/java/com/lear/MGCMS/controller/dispatcher/DispatcherController.java:74-89`
**What:** `setAcceptance` writes `zoneAcceptanceStatus` directly via `cuttingRequestRepository.save(cr)` and never publishes a `SequenceAcceptedEvent` (or any other event). The class Javadoc at line 36 says publish "fires events" — only the `/publish` path does, the chef accept/reject path does not.
**Why it matters:** Phase 7 `EngineTickService` and the kiosk version-bump path subscribe to event signals. If a chef rejects a sequence, downstream listeners never know — the engine keeps treating the request as routable, machine queue version is not bumped, kiosks may continue showing a rejected sequence as next.
**Fix:** After save, publish a new `SequenceAcceptanceChangedEvent(zoneNom, sequence, status)` and let `EngineTickService` listen for it (or call `bumpVersionsForZone` directly). At minimum, write a regression test that asserts `MachineQueue.version` increments on reject.
**Resolution (2026-04-26):** New `SequenceAcceptanceChangedEvent` (sequence, zoneNom, status, changedByMatricule) is published from `SequenceDispatcherService.setAcceptance(...)` after the save. `EngineTickService.onSequenceAcceptanceChanged(...)` listens and calls `bumpVersionsForZone(zoneNom)` so kiosks see the change on the next 2-second poll.

#### C6. ✅ DONE — Dispatcher controller has a public `Autowired` field declared mid-class

**Location:** `src/main/java/com/lear/MGCMS/controller/dispatcher/DispatcherController.java:91-92`
**What:** Right after the controller methods, an `@Autowired private CuttingRequestRepository cuttingRequestRepository;` is declared. The Phase 10 `setAcceptance` method depends on it but is reaching into the data layer from a controller — bypassing service.
**Why it matters:** Controller code now contains direct repository writes. There is no service-level transaction boundary, no validation, no domain event publishing, and the pattern invites future drift. The same data manipulation in `SequenceDispatcherService.publish` goes through the proper layer.
**Fix:** Add `setAcceptance(String sequence, String status)` on `SequenceDispatcherService`, mark it `@Transactional`, publish the acceptance event there, and have the controller call the service.
**Resolution (2026-04-26):** Repository field deleted. New `SequenceDispatcherService.setAcceptance(sequence, status, matricule)` is `@Transactional`, validates `status` (ACCEPTED|REJECTED), saves the entity, and publishes `SequenceAcceptanceChangedEvent`. Controller now resolves the matricule from `Authentication`, calls the service, and maps `Optional.empty()` → 404 / `IllegalArgumentException` → 400.

---

### 🟠 High

#### H1. Hardcoded DB passwords in `application.properties`

**Location:** `src/main/resources/application.properties:3,8,13,18,23,28,33` (`tangier11` repeated seven times)
**What:** Plaintext passwords for every datasource and the SMTP relay.
**Fix:** Move to environment-specific Spring config (`application-dev.properties`, etc.) and inject via env vars or a credentials store. At minimum, use `${DB_PASSWORD}` placeholders and document the env var in README.

#### H2. SMTP password in cleartext

**Location:** `application.properties:102` (`spring.mail.password=Tanger10`)
**Same fix as H1.**

#### H3. ✅ DONE — `SecurityConfig.configure(AuthenticationManagerBuilder)` keeps the auto-generated stub comment

**Location:** `src/main/java/com/lear/MGCMS/security/SecurityConfig.java:87`
**What:** Eclipse generated `// TODO Auto-generated method stub` then `super.authenticationManager()`. The bean is exposed but the override is gratuitous; same call could be a single line. The TODO is one of dozens (see L7).
**Fix:** Either drop the override (Spring Boot exposes the bean automatically) or remove the comment.
**Resolution (2026-04-26):** Removed the `// TODO Auto-generated method stub` line; the `@Bean(BeanIds.AUTHENTICATION_MANAGER)` override is kept (Spring Boot 2.5 still uses it to expose the manager).

#### H4. `findAll(date, shift)` is called with `String.valueOf(shift)`

**Location:** `src/main/java/com/lear/MGCMS/services/dispatcher/SequenceDispatcherService.java:103`
**What:** Repository signature accepts `String shift`, not `int`. Stringifying an int as the input to a SQL query is a smell — if the column is numeric in the schema the compare will work but is brittle.
**Fix:** Either change the column type to varchar (cheap), or change the parameter type to `int` and use a proper JPQL/native query.

#### H5. `OrdonnancementController` and `PlanDeChargeController` predate the dispatcher and are not gated

**Location:** `src/main/java/com/lear/MGCMS/controller/OrdonnancementController.java`, `controller/PlanDeChargeController.java` (look at the class headers)
**What:** Pre-dispatcher controllers expose data that overlaps with the dispatcher. They have no `mgcms.dispatcher.enabled` check.
**Why it matters:** The dispatcher claims to be flag-gated end-to-end. Operators may see stale state from the legacy controllers when `mgcms.dispatcher.enabled=true` produces different views. Two sources of truth.
**Fix:** Decide which is canonical when both are enabled. Either retire the legacy controller behind a deprecation header, or have it delegate to dispatcher data when the flag is on.

#### H6. `ChefDeZonePage.js` polls forever after dispatcher returns 404

**Location:** `src/main/js/components/Layout/ChefDeZonePage.js:38, 50-54`
**What:** `setInterval(this.reload, 15000)` runs unconditionally; on 404 the UI shows "Dispatcher désactivé." but keeps polling every 15s.
**Why it matters:** A dozen tablets polling a 404 from the dispatcher endpoint is harmless individually, but multiplied by every chef terminal it adds noise to the access log and burns battery. Also masks a real problem if the dispatcher comes back online — the user has to refresh.
**Fix:** When the response is 404, `clearInterval(this.pollTimer)` and stop. Re-poll only on user-initiated refresh.

#### H7. `App.js` has 90+ top-level imports, no code splitting

**Location:** `src/main/js/App.js:1-100`
**What:** Every layout component is statically imported and ends up in `bundle.js`. The bundle weight is paid on every page load.
**Why it matters:** Initial paint is slower than necessary. Tablets on shopfloor Wi-Fi pay the cost daily.
**Fix:** Convert routes to `React.lazy(() => import('./components/Layout/PlanDeCharge'))` and wrap the `<Switch>` with `<Suspense fallback={...}>`.

#### H8. Dead-code imports and commented routes in `App.js`

**Location:** `src/main/js/App.js:67` (`// import OrdonnancementV2`)
**What:** Multiple commented imports and many V2/V3 components alongside originals (`Ordonnancement`, `OrdonnancementV3`, `AdvancedOrdonnancement`).
**Fix:** Pick the canonical version per page, retire the rest, and delete the dead imports. Add a section comment explaining the lineage so future devs know V3 superseded V2 superseded V1.

#### H9. ✅ DONE — `e.printStackTrace()` x31 across the controller layer

**Location:** 13 files; e.g. `controller/IppmController.java:8 occurrences`, `controller/QualityNoticeController.java:8`, `controller/CncPsController.java:8`
**What:** Stack traces written to `System.err`, never structured-logged, never alerted.
**Why it matters:** In production these errors disappear. The Phase-9 admission audit table is the dispatcher's best practice — apply that to the rest of the codebase.
**Fix:** Replace with `log.error("context", e)` using SLF4J. Add a `@ControllerAdvice` that converts `Exception` into a JSON error body so clients stop choking on HTML stack-trace pages.
**Resolution (2026-04-26):** Every `e.printStackTrace()` was deleted from `src/main/java/com/lear/MGCMS/**`. SLF4J `Logger`s were added to `IppmController`, `QualityNoticeController`, `PartNumberMaterialConfigController`, `NotificationController`, `StorageHistoryController`, `CuttingPlanningController`, `ControlTableController`, `CtcToleranceTask`, `WorkOrderTask`, `ScheduledTask`, `EmailService`, `QueryService`, and `security/Constants`. Each call site now uses `log.error("...context...", e)` with parameterized messages. `mvn compile` passes (exit 0). The `@ControllerAdvice` JSON error mapper is still pending — out of scope for this pass.

#### H10. Frontend `axios 0.21.1` is six majors behind

**Location:** `package.json:42`
**What:** Axios 0.21.1 has known SSRF (CVE-2021-3749) and prototype-pollution issues.
**Fix:** Bump to axios 1.x. Verify request interceptors and form-data uploads still pass. Major version bump but most APIs are unchanged.

#### H11. `marked@4.3.0` is two majors behind

**Location:** `package.json` (search for `"marked"`)
**What:** Outdated markdown parser (current is 12.x). Minor security fixes.
**Fix:** Bump and re-test the LLM chat widget rendering.

#### H12. `react-router-dom 5.2.0` is one major behind, no upgrade plan documented

**Location:** `package.json:54`
**Fix:** Plan a 5→6 migration; the routing API change is well-documented and your Switch/Route tree is small.

#### H13. `@mui/styled-engine-sc 6.0.0-alpha.2` in production

**Location:** `package.json` (alpha pin)
**What:** Alpha-grade dependency.
**Fix:** Either drop styled-components/MUI sc engine (plain MUI emotion is fine) or pin to a stable.

#### H14. Webpack proxy in `package.json` points at `localhost:8085`, not `:6001`

**Location:** `package.json` (`"proxy": "http://localhost:8085"`)
**What:** Default API proxy doesn't match the Spring server port (`server.port=6001`). On `npm start` API calls 404.
**Fix:** Change to `"proxy": "http://localhost:6001"` or read from an env var.

---

### 🟡 Medium

#### M1. `// TODO Auto-generated method stub` x65+

**Location:** Throughout services and a few controllers; e.g. `services/AuditQualiteConfigService.java:29,198,203`, `services/CodeArretService.java:55`, `services/CodeDefautService.java:58`, `services/ConfigSeriePlusService.java:191,196,201,207`, `services/CuttingSpeedService.java:158,167,172`, `services/cms/PlanCoupeService.java:43,63,75,83,88,93`, `controller/CuttingPlanningController.java:115,247,321`
**What:** Boilerplate from Eclipse code generation, never deleted.
**Fix:** Run a one-shot search-and-delete; they carry no information and noise up code review. Replace each with either real implementation or remove the empty override.

#### M2. `printTicket` in `CuttingRequestController.java` does role-checks manually

**Location:** `src/main/java/com/lear/MGCMS/controller/CuttingRequest/CuttingRequestController.java:160-174`
**What:** `for (Role role : user.getRoles()) { if (...) authorized = true; ... }` — duplicates Spring's authorization machinery.
**Fix:** Use `@PreAuthorize("hasAnyRole('IMPORTER','ADMIN','CHEF_DE_ZONE','CHEF_EQUIPE')")` and delete the loop. Saves 14 lines, makes the contract explicit, surfaces in Spring Security audit.

#### M3. `@placement` is replaced twice in `printTicket`

**Location:** `CuttingRequestController.java:199, 204`
**What:** Same key replaced with same value back-to-back.
**Fix:** Remove the duplicate at 204.

#### M4. `obj.getLongueur() + ""` produces "null"

**Location:** `CuttingRequestController.java:201, 202, 203, 234, 235`
**What:** When `Longueur`/`NbrCouche`/`Laize`/`Ind`/`Total` is null (DB has nullable columns), the ZPL prints "null".
**Fix:** Helper `String safeNum(Number n) { return n == null ? "" : n.toString(); }` and use everywhere.

#### M5. Resource not in try-with-resources

**Location:** `CuttingRequestController.java:176-184`
**What:** `FileReader fr = new FileReader(...)`, `BufferedReader bfr = new BufferedReader(fr)`. If `bfr` constructor throws, `fr` is leaked.
**Fix:** `try (BufferedReader bfr = new BufferedReader(new FileReader(new File(labelsFolder + "ticketSerie.prn")))) { ... }`.

#### M6. `labelsFolder + "ticketSerie.prn"` concatenates without separator

**Location:** `CuttingRequestController.java:176`
**What:** Relies on `lear.labelsFolder` ending with a backslash. The property file does (`D:\\LEAR\\tickets\\`) but a future edit could break this silently.
**Fix:** `Paths.get(labelsFolder, "ticketSerie.prn").toFile()`.

#### M7. `DispatcherProperties` not validated at startup

**Location:** `services/dispatcher/DispatcherProperties.java`
**What:** Properties bind from `application.properties` but there is no `@Validated` + `@AssertTrue`/`@Min` to catch misconfig (negative shift duration, empty cron, etc.).
**Fix:** Add `@Validated` on the class and `@Min(0)` / `@NotEmpty` on the fields.

#### M8. `RetentionCronService.purgeNow(int days)` clamps to `Math.max(1, days)` but `days <= 0` is allowed via `@Scheduled` path

**Location:** `services/dispatcher/RetentionCronService.java:43-47, 62`
**What:** Two different clamping rules between the cron and the manual API.
**Fix:** Pick one. The clamp belongs in the properties layer (`getDays() { return Math.max(1, days); }`) so all callers behave identically.

#### M9. `EngineTickService.scopeRequests` returns `findAll(date, shift)` when zone-aware is off

**Location:** `services/dispatcher/EngineTickService.java:141-147`
**What:** Comment says "centralized so the engine only has one code path" but the call still branches.
**Fix:** Always call `findByDateAndShiftAndOptionalZone(...)` with a nullable `zoneNom`, push the conditional into the repository.

#### M10. `chooseZone` ties resolved alphabetically descending instead of ascending

**Location:** `services/dispatcher/SequenceDispatcherService.java:163`
**What:** Comment says "alpha asc tiebreak (max reverses)" but the code returns `b.getKey().compareTo(a.getKey())` which sorts descending. Combined with `.max(...)` you get ascending result for the chosen zone — works, but is confusing.
**Fix:** Sort ascending, then take `.min(...)` after the count compare. Or expand the comment.

#### M11. `ZoneConfirmationController` `confirm()` has no idempotency check

**Location:** `controller/dispatcher/ZoneConfirmationController.java:50-66`
**What:** Two POSTs in quick succession from a chef double-clicking will create two `ShiftZoneConfirmation` rows.
**Fix:** Check for an existing confirmation with the same (date, shift, zoneNom) and update instead of insert.

#### M12. `UserZoneController.assign` ignores the assignedBy when authentication is null

**Location:** `controller/dispatcher/UserZoneController.java:86-88`
**What:** `String assignedBy = me == null ? null : me.getMatricule();` — passes null through.
**Why it matters:** Audit trail loses provenance.
**Fix:** Reject when `me == null` (this should be unreachable thanks to `@PreAuthorize` but defending the contract is good practice).

#### M13. `KioskController` is unauthenticated by design but has no rate-limit guard

**Location:** `controller/dispatcher/KioskController.java`
**What:** Kiosks poll `/version` every 2s. A misbehaving tablet floods the endpoint.
**Fix:** Add a simple `@RateLimit(60, TimeUnit.SECONDS)` (Bucket4j has an easy Spring integration) so a single client can't exceed N requests per minute.

#### M14. `ChefDeZonePage.js` busy-state per row is a single string

**Location:** `src/main/js/components/Layout/ChefDeZonePage.js:28, 121, 128`
**What:** `busySequence: null` — only one sequence can show "busy" at a time. Two near-simultaneous accept clicks from the same chef will visually conflict.
**Fix:** Use `Set<string>` of busy sequences.

#### M15. `ChefDeZonePage.js` uses inline `style={{ ... }}` everywhere

**Location:** `ChefDeZonePage.js:77, 79, 109, 122`
**What:** Inline objects re-allocate on every render and bypass the `Form.scss` style sheet pattern.
**Fix:** Move to a per-page SCSS module or `styled-components`.

#### M16. `ChefDeZonePage.js` reload on every shift change races

**Location:** `ChefDeZonePage.js:88` (`onChange={(e) => this.setState({ shift: parseInt(e.target.value, 10) }, this.reload)}`)
**What:** Quick toggle between shifts fires multiple in-flight reloads; whichever resolves last wins. Could show shift-1 data when the user is now on shift-2.
**Fix:** Cancel the prior request via `axios CancelToken` (or AbortController) before issuing a new one.

#### M17. `application.properties` ships LLM model URLs from HuggingFace

**Location:** `application.properties:151-154`
**What:** Model download URLs hardcoded. If HF rate-limits, the server stalls when downloading a 4 GB model on first run.
**Fix:** Pre-stage models in `mgcms.llm.model-path` during deployment and disable `download-missing-models=true` for production.

#### M18. `mgcms.llm.max-concurrent-sessions=5` with `session-timeout-minutes=30`

**Location:** `application.properties:145-146`
**What:** Five concurrent LLM sessions × 7B model each easily blows past server RAM.
**Fix:** Tune per the actual hardware. Document the relationship between RAM tier, model size, and session count.

#### M19. Flyway baseline at version 1, new migrations start at V2_*

**Location:** `application.properties:43-46`, migrations `V2_01_…sql`–`V2_06_*.sql`, `V9_01_*.sql`
**What:** Naming convention drift. V9_01 jumped because Phase 9 was the next chronological event but no V3-V8 exists. Future devs will be confused about gaps.
**Fix:** Document the convention in a `db/migration/README.md`. Either continue with phase-prefixed (`V<phase>_<seq>__name`) or switch to monotonic (`V25__name`).

#### M20. `flyway_schema_history` write happens at every startup with `baseline-on-migrate=true`

**Location:** `application.properties:44`
**What:** OK on first deploy, but if someone manually edits a baselined script later, Flyway silently re-baselines.
**Fix:** Set `baseline-on-migrate=false` after the first prod boot. Document that flag in the runbook.

#### M21. `OrdonnancementService` legacy entry points are not in this review

**Location:** `services/OrdonnancementService.java`
**What:** Phase 7 carved out `EngineTickService` to leave the monolith alone. The monolith remains. Many callers couple to its internal queue logic.
**Fix:** Plan an extract-class refactor over a quarter — split `OrdonnancementService` into `OrdonnancementQueueService`, `OrdonnancementMetricsService`, `OrdonnancementImportService`, etc., bounded by use case.

#### M22. ✅ DONE — `AdmissionController.check()` does not require authentication

**Location:** `controller/dispatcher/AdmissionController.java:38-65`
**What:** No `@PreAuthorize`, no role check.
**Why it matters:** Anyone on the LAN can call the admission endpoint with arbitrary `requestedByMatricule` (which then lands in audit). Audit integrity compromised.
**Fix:** Either authenticate via the JWT chain (and reject when `authentication == null`) or, if kiosks must remain anonymous, enforce a kiosk shared-secret header.
**Resolution (2026-04-26):** Added `@PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','IMPORTER','ADMIN')")` and an explicit null-`Authentication` check returning 401. The matricule used for the audit row is now sourced from `authentication.getName()` (resolved via `UserService`) rather than trusted client-supplied input. Combined with C1, the endpoint is no longer reachable anonymously.

#### M23. `mgcms.shift.durationMinutes=480 mgcms.shift.breakMinutes=30` is global, not per-zone

**Location:** `application.properties:62-63`
**What:** All zones share the same shift duration. Real shop-floors may have shift overlap or different break schedules per zone.
**Fix:** Add a `Zone.shiftBreakMinutes` column and read from there with the global as fallback.

---

### 🟢 Low

#### L1. `MgcmsApplication.java` schedules a top-level task

**Location:** `src/main/java/com/lear/MGCMS/MgcmsApplication.java:1`
**Fix:** Move bootstrap-time tasks into a `@PostConstruct` on a dedicated bean. Keep the application class lean.

#### L2. `SecurityConstants.java` static `H2_URL` exposed in prod

**Location:** `src/main/java/com/lear/MGCMS/security/Constants.java:1`
**What:** Even if H2 isn't running, the constant lingers.
**Fix:** Delete the constant and the `permitAll(H2_URL)`.

#### L3. ✅ DONE — `JwtAuthenticationFilter.java` printStackTrace

**Location:** `security/JwtAuthenticationFilter.java:1`
**Fix:** Replace with SLF4J error log + structured fields (`token=…`, `userPath=…`).
**Resolution (2026-04-26):** Verified — the file already uses `logger.error("Could not set user authentication in security context", ex)` via the inherited `GenericFilterBean#logger` (Apache Commons Logging facade). No `e.printStackTrace()` present. Original review remark was inaccurate for this file; no change needed.

#### L4. Mixed French/English in code comments and method names

**Location:** Throughout (e.g. `chargesParCategorie`, `getZoneNom`, `nom` vs `name`)
**Fix:** Pick one. The team uses French in DB columns and English in code; lean to that convention everywhere and document it.

#### L5. `String.format` with `%.3f` relies on default locale

**Location:** Multiple places where ZPL strings are built
**Fix:** Use `String.format(Locale.US, "%.3f", v)` to keep tickets consistent.

#### L6. `findBySequence` invoked without null guard in dispatcher controller

**Location:** `controller/dispatcher/DispatcherController.java:83-85`
**What:** `if (cr == null) return ResponseEntity.notFound().build();` is fine, but `cuttingRequestRepository.findBySequence(sequence)` could legitimately return multiple rows in older data — silent first-row-wins.
**Fix:** Make the repo signature `Optional<CuttingRequest>` and assert single-result.

#### L7. Lots of empty Eclipse-generated overrides in service classes

**Location:** Same as M1 list
**Fix:** Delete or implement.

#### L8. `Form.scss` is shared by every form variant

**Location:** `src/main/js/styles/Form.scss`
**Fix:** Split per page so unused selectors don't ship.

#### L9. `axios 0.21.1` config relies on global instance

**Location:** Throughout React components
**Fix:** When you bump to axios 1.x, create one configured instance with base URL, default headers, and a global error interceptor. Use that instance everywhere.

#### L10. `webpack 4` is two majors behind

**Location:** `package.json` devDeps
**Fix:** Upgrade to webpack 5 alongside the axios bump.

#### L11. `node-sass` deprecated in CMS-Prod, MG-CMS uses `sass` — good

**Location:** `package.json` (`"sass": "^1.83.4"`)
**No fix needed.** Keep this; just notes that CMS-Prod has not done the same.

#### L12. `EngineTickService.bumpVersionsForZone` casts row[0] to String

**Location:** `services/dispatcher/EngineTickService.java:152-158`
**What:** `Object[] row` projection from `findMachinesWithTypeInZone`. Untyped tuple is fragile.
**Fix:** Define a Spring Data interface projection (`MachineNomProjection { String getNom(); String getMachineType(); }`).

#### L13. `bumpVersionForMachine` not marked `@Modifying`

**Location:** Search `MachineQueueRepository`
**What:** If the repo method is a JPQL update, it must have `@Modifying` or it never runs.
**Fix:** Verify the annotation is present; add a unit test that asserts the version bump persists.

#### L14. `RetentionCronService` cron defaults to 02:30 every night, no jitter

**Location:** `services/dispatcher/RetentionCronService.java:40`
**What:** Multiple instances would all hit the audit tables at the same instant.
**Fix:** Add randomized jitter `(LocalDateTime.now().getNano() % 60s)` before deletion, or stagger via different cron expressions per env.

---

### 💡 Suggestions

#### S1. Wire `EngineTickService.publish` results into Micrometer counters

`@Counted("dispatcher.publish.sequences")` on the auto-tick path so ops can chart how many sequences flow through per shift.

#### S2. Add a `@TransactionalEventListener(phase = AFTER_COMMIT)` to `SequenceAcceptedEvent`

So listeners only fire after the publish actually committed. Today they fire inside the transaction.

#### S3. Wrap the LLM chat widget behind its own feature flag

`mgcms.llm.enabled` exists but the React `ChatWidget` is mounted unconditionally in `App.js:11`. Mount conditionally based on a `/api/config/flags` endpoint.

#### S4. Use JPA Specifications for `OrdonnancementService` filters

Today they're hand-written queries; specs are easier to compose for the upcoming zone-aware filtering.

#### S5. Add a `/api/version` endpoint

Returns `{commit, buildTime, dispatcherEnabled, engineZoneAware, autoTick}` so kiosks can show the deployed config in the footer.

#### S6. Generate Flyway migrations from JPA annotations

Use `flyway-maven-plugin` + `hibernate-tools` to scaffold the next migration so dev and prod schemas don't drift.

#### S7. Type the `mgcms.engine.autoTick.cron` with `@Scheduled(cron = "#{...}")` SpEL

To bail out earlier when the flag is off (today the no-op runs as a Spring task each tick).

#### S8. Add a `pom.xml` dependency-check plugin

`org.owasp:dependency-check-maven` flags vulnerable libs at build time.

#### S9. Convert `Constants.writeLogs(String)` to SLF4J

It writes to a custom file. Logback can do the same with a file appender and gives you log rotation for free.

#### S10. `application.properties` would benefit from `@ConfigurationPropertiesScan`

Today every dispatcher properties class is wired manually. Scan once and forget.

#### S11. Document the role taxonomy

Code references `ROLE_PROCESS`, `ROLE_CHEF_DE_ZONE`, `ROLE_CHEF_EQUIPE`, `ROLE_IMPORTER`, `ROLE_ADMIN`. List them all in `plans/ROLES.md` with the canonical permission set per role.

---

## 2 · Findings by axis

| Axis                              | IDs                                                            |
|-----------------------------------|----------------------------------------------------------------|
| Correctness bugs                  | C2, C4, C5, H4, H6, M3, M4, M5, M10, M11, M16                  |
| Security                          | C1, H1, H2, H10, H11, M22, L2                                  |
| Design / architecture             | C6, H5, H7, H8, M2, M9, M21, S2, S4                            |
| Data model / persistence          | C2, C3, H4, M19, M20, L13                                      |
| Concurrency / transactions        | C3, M11, S2                                                    |
| Configuration                     | H1, H2, M7, M8, M17, M18, M20, M23, S1, S5, S6, S10            |
| Observability                     | H9, M22, S1, S5, S9                                            |
| Production process fit            | C4, C5, H5, M11, M22, M23                                      |
| UI / UX (frontend)                | H6, H7, H8, M14, M15, M16                                      |
| Tech debt / dead code             | H3, H8, M1, L1, L7, L8, L9                                     |
| Dependencies                      | H10, H11, H12, H13, H14, L9, L10                               |

---

## 3 · Dispatcher (Phase 0-11) specific notes

The dispatcher landed cleanly as 11 small phases each behind its own flag. That itself is excellent; many teams ship a feature in one shot and own the regressions for months. With that said, the focused notes are:

- **Phase 4 (`SequenceDispatcherService.publish`)** — synchronous event publishing inside a `@Transactional` write. See S2 (use `AFTER_COMMIT` phase).
- **Phase 7 (`EngineTickService.autoDispatchTick`)** — too aggressive cron + no diff check. See C4.
- **Phase 9 (`AdmissionService.check`)** — audit write inside the operator's hot transaction. See C3. Also the controller is unauthenticated; see M22.
- **Phase 10 (`DispatcherController.setAcceptance`)** — bypasses event chain. See C5. Should live in the service layer. See C6.
- **Phase 11 retention (`RetentionCronService`)** — clamping inconsistent between cron and manual call. See M8. No cron jitter. See L14.
- **Phase 11 self-heal (`SelfHealService.sweep`)** — `findAll()` is a fairness problem at scale. See C2.
- **Phase 8 (`KioskController`)** — by design unauthenticated. The no-rate-limit gap is the only concern. See M13.

Across all phases the property naming and Javadoc are consistent. Nice work. A `plans/PHASES_RUNBOOK.md` summarizing what flag enables what is the only meta-doc still missing — `application.properties:85-93` already has the inventory comment, just promote it to its own file.

---

## 4 · Positive notes — keep doing this

1. **Feature flags are everywhere.** Every dispatcher entry point (controller and service) checks `mgcms.dispatcher.enabled`. The runbook in `application.properties:85-93` is exactly the doc ops needs to roll back.
2. **Properties classes per concern.** `DispatcherProperties`, `EngineProperties`, `AdmissionProperties`, `RetentionProperties`, `SelfHealProperties`, `ShiftProperties` — each file is short, typed, and obvious. Future devs can find what they need fast.
3. **Audit tables fire even in shadow mode.** `AdmissionService.block()` writes regardless of `enforce`. That's the right shape for measuring impact before flipping a flag.
4. **Phase Javadoc.** Every dispatcher class has a paragraph explaining what it owns and which phase introduced it. Keep this discipline elsewhere.
5. **Flyway-versioned schema.** `V2_01..V2_06`, `V9_01` — consistent prefixes, clearly tied to plan phases.
6. **Defensive `safe()` helpers.** `SequenceDispatcherService.safe(List)` is a tiny pattern but it eliminates a whole class of NPE bugs.

---

## 5 · Next steps

A reasonable two-sprint plan to land the bulk of the critical + high items:

**Sprint 1 (security + dispatcher correctness)**
- C1: tighten SecurityConfig
- C2: replace SelfHealService.sweep with a query
- C3: split admission audit into REQUIRES_NEW
- C4: diff-check in autoDispatchTick
- C5: publish acceptance event from setAcceptance
- C6: move setAcceptance into the service layer
- M22: authenticate AdmissionController

**Sprint 2 (frontend + tech debt)**
- H6: stop polling on dispatcher 404
- H7: code-split App.js
- H8: delete commented routes
- H10/H11/H12/H13: dependency bumps + smoke tests
- H14: fix dev proxy
- M1: bulk-delete Eclipse TODO stubs
- L7: collapse empty service overrides

Everything else is opportunistic — pick from the list when you're already in the file for another reason.
