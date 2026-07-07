# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Read `AGENTS.md` for the full project guide and `docs/README.md` for the documentation index.
For any question about databases, tables, columns, status lifecycles or which page writes
what, read **`docs/schema.md`** first — it maps all six databases and the full production flow.

## Toolchain Reality (this dev machine)

- **JDK**: only JDK 19 is installed. `mvn compile` works, but Spring Boot 2.5.3 tests/run
  expect JDK 17 — `mvn test` / `spring-boot:run` may fail until JDK 17 is installed.
- **Node**: Node 25 is installed although `package.json` engines pin Node 20; the build works.
- **npm 11 gotcha**: letting npm 11 regenerate `package-lock.json` can desync it so that
  `npm ci` fails in prod (dev is unaffected). Fix with `npm install --package-lock-only`.
  This actually broke `/logisticsRelease` once — check the lockfile diff before committing it.
- **Memory**: do NOT run two Maven builds in parallel — the machine OOMs (pagefile error 1455).
- Frontend bundles to `target/classes/static/` which is **gitignored**; prod deploys build it fresh.

## Critical Domain Gotchas

- **Hibernate naming**: the project uses a no-op physical naming strategy — there is **no**
  camelCase→snake_case conversion. Columns without `@Column` are camelCase in SQL Server,
  and **native queries must match the camelCase names**.
- **Shift numbering**: plant convention is 1=night, 2=morning, 3=afternoon.
  `OrdonnancementService` uses the INVERSE mapping (legacy). Do not "unify" them naively —
  flows depend on each side's existing convention.
- **`CuttingRequest.sequenceStatus`**: lifecycle is IMPORTED → RELEASED → STARTED →
  COMPLETED, plus the off-path states MATERIAL_MISSING and INCOMPLETE (see
  `SequenceStatusService`). Logistics release sets RELEASED, the CMS-Prod spreading app
  sets STARTED, and chef actions set COMPLETED/INCOMPLETE/MATERIAL_MISSING.
- **suiviplanning → sequenceStatus sync**: a 20-min scheduled job mirrors
  `suiviplanning.Statu` → `CuttingRequest.sequenceStatus` (Non demarre/Released/En cours/
  Complet → IMPORTED/RELEASED/STARTED/COMPLETED). It deliberately scans **all** suiviplanning
  rows (a date-window optimisation failed in prod). Backward transitions are allowed, but it
  never overwrites MATERIAL_MISSING/INCOMPLETE.

## Current Feature Map (verify in code; these ARE current as of 2026-06-30)

- **Live production flow is logistics release, not the optimizer.** The continuous dispatch
  optimizer was SHELVED 2026-05-31. Treat `/api/scheduling/**` (`SchedulingController`,
  `SchedulingOptimizationService`, the `optimized_plan`/`optimized_series_assignment` tables)
  as DEAD/legacy — its only UI was removed. The live flow is **logistics release**
  (`/logisticsRelease`, `LogisticsRelease.js`) driving the `suiviplanning` table on the
  second, non-XA `cms` datasource. `LogisticsReleaseService.commit()` compensates a non-XA
  `suiviplanning` flip on local failure (reverts only rows it flipped, `setRollbackOnly` +
  rethrow; `SuiviPlanningRepository.revertReleasedToNonDemarreBySequences`).
- **KEPT despite the optimizer being shelved:** `OrdonnancementService` is still called live
  (timeline-cache invalidation from `WorkbenchCacheService`, `SuiviPlanningStatusSyncService`,
  `SequenceStatusService`, `CuttingRequestV2Service`; `SerieDTO` used by ordering strategies),
  and `OrdonnancementController` `/api/ordonnancement/**` still backs `Form.js`.
- **REMOVED 2026-06-30 (commit 834568d) — do not reference these, they are deleted:**
  `/advancedOrdonnancement` (`AdvancedOrdonnancement.js`), the "Ordonnancement" dashboard
  section `/schedulingDashboard` (`SchedulingDashboard.js`), "Supervision Zone"
  `/chefDeZonePage` (`ChefDeZonePage.js`), and "Affectation Chef↔Zone" `/userZoneAdmin`
  (`UserZoneAdmin.js`).
- **Zone confirmation is live:** `/chefDeZoneConfirm` ("Confirmation Machines",
  `ChefDeZoneConfirm.js`) → `/api/zone/confirm` writes `ShiftZoneConfirmationMachine`, read by
  `ActiveMachineResolver` to decide "up" machines/tables for next-series / table-feed /
  production-floor.
- **Next-series ranking** (`TableFeedRankingService.rankForTable`) is BANDED LEXICOGRAPHIC:
  WIP > date (overdue/today) > wait-age (Tier D anti-starvation/FIFO, commit f1f407d,
  2026-06-30, kill-switch `mgcms.nextserie.waitage.enabled`) > same-ref > material-in-zone
  (plus a -5 soft demotion when material is not on a rack in the zone) > keep-busy/longer-cut.
  Non-fitting candidates are EXCLUDED from the top-N. Hard gates: `statusCoupe=Waiting` AND
  `statusMatelassage=Waiting` AND parent `sequenceStatus IN (RELEASED, STARTED)`, not REJECTED.
- **productionFloor** (`FloorStateService`) probes machine `codeEtat` at a shift-correct hour
  (or `now()`), not a hardcoded 12:00; rack lookup via `ScanRouleauRepository.findRackLight()`
  (`emplacement IS NOT NULL`).
- **CNC quality rework:** `/cncControl` (`CncControl.js`) and `/cncQualite` (`CncQualite.js`),
  `ProgramCNCHistory` audit trail, `ProgramCNC.cavitePress` field, ProgramCNC duplicates now
  allowed.
- **Admin tools:** `/systemHealth` (`SystemHealth.js`, ADMIN-only) and `/archiving`
  (`Archiving.js`, ADMIN-only); V18_01 perf indexes.
- **Other current features:** Qn (Flash qualité) `appliquerSur` field (Matelassage/Coupe/Les
  deux) routing which questions show in CMS-Prod (V18_08); Plan de Charge efficiency/capacity
  rules + part-number perimeter report; prod URL sets `sendStringParametersAsUnicode=false`.

## Build and Test Commands

### Backend (Java/Spring Boot)

```bash
# Full build with frontend bundling
mvn clean package

# Build backend only (skips frontend)
mvn clean install -DskipTests

# Run tests
mvn test

# Run specific test
mvn test -Dtest=TestClassName

# Run the application (auto-rebuilds on file changes with devtools)
mvn spring-boot:run

# Check for vulnerabilities
mvn dependency-check:check
```

### Frontend (React/Webpack)

```bash
# Install dependencies (also run this after npm version changes)
npm install

# Development mode with watch (serves on http://localhost:3000 via proxy to :8085)
npm run watch
# or
npm start

# Production build
npm run prod

# Build and bundle resources for Spring (outputs to src/main/resources/static/)
npm run bundle:resources

# Run Jest tests
npm test

# Run a single test file
npm test -- MyComponent.test.js
```

### Node.js Version

This project requires Node.js 20.x and npm 10.x (see `.nvmrc` and `.node-version`).

```bash
# Check current version
node --version
npm --version

# Use nvm if available
nvm use
```

## Architecture Overview

### High-Level Stack

- **Backend**: Spring Boot 2.5.3 on Java 17, with Spring Security (JWT), JPA/Hibernate, WebSocket support
- **Frontend**: React 17 with Redux state management, React Router 5 for navigation, Webpack for bundling
- **Database**: Microsoft SQL Server (primary) with additional configured data sources and Flyway migrations
- **Authentication**: JWT-based with role-based access control (RBAC)
- **UI Framework**: Material-UI with custom components, Font Awesome icons, charts via Chart.js and Recharts

### Layering

```
React Frontend (src/main/js/)
    ↓
Redux Actions/Reducers (src/main/js/actions/, src/main/js/reducers/)
    ↓
REST API via Axios
    ↓
Spring Controllers (src/main/java/com/lear/MGCMS/controller/)
    ↓
Spring Services (src/main/java/com/lear/MGCMS/services/)
    ↓
JPA Repositories (src/main/java/com/lear/MGCMS/repositories/)
    ↓
SQL Server Database
```

### Backend Package Structure

| Package | Purpose |
|---------|---------|
| `com.lear.MGCMS.controller.*` | HTTP endpoints organized by domain (CuttingPlan, CuttingRequest, logistics, scanCoupe, cms, etc.) |
| `com.lear.MGCMS.services.*` | Business logic organized by domain |
| `com.lear.MGCMS.repositories.*` | JPA repositories and custom SQL queries organized by domain |
| `com.lear.MGCMS.domain.*` | JPA entities and domain models organized by domain |
| `com.lear.MGCMS.payload.*` | DTOs and request/response objects |
| `com.lear.MGCMS.security` | Authentication, JWT token handling, role-based access |
| `com.lear.MGCMS.validator` | Business rule validation for risky workflows |
| `com.lear.MGCMS.utils` | Shared utilities (Excel export, file handling, etc.) |
| `com.lear.cms.*`, `com.lear.ctc.*`, `com.lear.pls.*`, `com.lear.splice.*` | Domain-specific entities and repositories for external systems |

### Frontend Structure

| Directory | Purpose |
|-----------|---------|
| `src/main/js/components/` | React components organized by feature/screen |
| `src/main/js/actions/` | Redux action creators |
| `src/main/js/reducers/` | Redux reducers |
| `src/main/js/securityUtils/` | JWT/auth helpers |
| `src/main/js/styles/` | Global SCSS styles |
| `src/main/js/assets/` | Images and static assets |

## Domain Context

MG-CMS is a production-critical manufacturing system for Lear Trim Tangier that manages cutting operations from CAD planning through production execution and logistics tracking.

### Key Workflows by Department

- **CAD**: Cutting plans, pattern placement, material configuration, speed/drill settings
- **Engineering**: Part number specs, material rules, weight, cutting time
- **Production**: Demand prep, cutting execution, box tracking, machine management
- **Quality**: Defect notices, audits, verification, scrap tracking
- **Logistics**: Roll consumption, stock visibility, shortage alerts, material allocation
- **Scheduling**: Plan de charge (load planning), ordonnancement (sequencing), capacity tracking
- **Admin**: User management, security, configuration, integrations

### Critical Data Integrity

- **Traceability**: Roll IDs, part numbers, box contents, status progression, historical records
- **Quantities**: Material consumption, scrap, shortages must be accurate
- **Status transitions**: Must follow valid sequences without gaps
- **Role-based access**: User permissions determine what each department can see/modify

## Important Documentation

Before making changes, read:

1. **`.github/copilot-instructions.md`** — Global priorities, department workflows, verification expectations
2. **`.github/instructions/java-spring.instructions.md`** — Backend layering, contracts, data sources, transactions
3. **`.github/instructions/react.instructions.md`** — Frontend patterns, route safety, component guidelines
4. **`.github/instructions/sql-server.instructions.md`** — Query design, schema safety, performance concerns
5. **`.github/instructions/testing.instructions.md`** — What counts as risky, expected verification shape

These files define the working standards for this codebase and override generic practices.

## Database Migrations

Flyway manages SQL Server schema migrations automatically on startup. Migration scripts are in:

```
src/main/resources/db/migration/
```

Naming convention: `V<version>__<description>.sql` (e.g., `V2__add_dispatcher_tables.sql`)

The baseline is set to V1 since the pre-dispatcher schema predates Flyway.

## Key Patterns to Preserve

- **Controllers**: Thin, focused on request validation and security boundaries
- **Services**: House business rules; call repositories explicitly
- **Repositories**: Keep query behavior reviewable and testable
- **DTOs**: Separate from entities; used for API contracts
- **Redux**: Centralized state management; avoid local component state for shared data
- **Routes**: Defined in `App.js`; stable route paths are critical for department workflows
- **Generic UI**: EntityList/EntityForm patterns used for many CRUD flows — changes here can have broad impact

## Risk Indicators

Work on these areas requires extra care and explicit verification:

- **Scheduling**: Plan de charge, capacity calculations, KPI outputs
- **Quality**: Defect workflows, verification processes, audit trails
- **Logistics**: Roll consumption, shortage calculations, stock reconciliation
- **Admin/Security**: User roles, access rules, integrations
- **Status workflows**: Any change to valid status transitions
- **Cross-layer**: Changes affecting multiple departments or systems

## Testing Expectations

- **Frontend**: Production build validation + Jest tests for risky UI behavior
- **Backend**: Targeted service/controller tests for business rules; integration tests for risky persistence
- **Database**: Verification includes affected application behavior, not only SQL syntax
- **Cross-layer**: Cover the full user flow across backend, frontend, and database

If automated coverage is weak, provide explicit manual verification steps tied to business workflows.

## Workflow for Code Changes

1. **Map the flow**: Identify the affected screen/route, backend endpoint, service, repository, and user role
2. **Check criticality**: Does it touch status transitions, quantities, traceability, scheduling, quality, or access?
3. **Review patterns**: Check existing code before introducing new ones
4. **Implement**: Preserve public contracts and business semantics
5. **Verify**: Build + test + manual steps if needed, mentioning the business scenario
6. **Document**: Note affected departments in the commit message if it impacts workflows

## Common Issues and Solutions

- **N+1 queries**: Use projections in repository queries for large result sets (especially scheduling/reporting)
- **Index missing**: Check query plans for table scans on high-volume tables (production, scheduling)
- **Frontend/backend contract drift**: Verify API response shapes match component expectations
- **Status inconsistency**: Ensure status transitions follow valid sequences defined in services
- **Lazy-loading surprises**: Be aware of Hibernate lazy-loading within transactions
