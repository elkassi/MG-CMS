# MG-CMS Agent Guide

MG-CMS (Cutting Management System) is a production-critical manufacturing application used at Lear Trim Tangier. It manages cutting operations from engineering and CAD preparation through production execution, quality follow-up, logistics consumption tracking, process scheduling, and application administration.

This guide is written for AI coding agents. Expect the reader to know nothing about the project. All information below is derived from the actual codebase.

---

## Project Overview

- **Name:** MG-CMS (CMS-web)
- **Version:** 3.69.1
- **Author:** Mouad El Ghazi
- **Description:** Cutting Management System for Lear Corporation
- **Language of codebase:** Primarily English with extensive French business terminology

### Business Context

The system is operationally sensitive. Rolls of material are planned, allocated, cut, and consumed. Part numbers, cutting plans, and machine assignments must stay consistent. Produced pieces are grouped into boxes and sent downstream to sewing. Quality, scrap, shortage, maintenance, and KPI data must remain traceable. User roles and permissions affect what each department can see or change.

### Department Modules

- **CAD:** cutting plans, placements, pattern search, material placement, speed, drill, and box configuration
- **Engineering:** part number specifications, material configuration, weight, cutting time, and technical rules
- **Production:** demand preparation, matelassage, cutting status, machine execution, and box tracking
- **Quality:** quality notices, defect validation, audits, verification, scrap, and follow-up dashboards
- **Logistics:** roll consumption, shortage visibility, stock verification, and material allocation accuracy
- **Process and Logistics release:** plan de charge, logistics release over `suiviplanning`, next-series / table feed, and capacity visibility (the continuous dispatch optimizer was shelved 2026-05-31)
- **Admin and IT:** user management, security, configuration, integrations, and operational settings

---

## Technology Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 16 | Core programming language |
| Spring Boot | 2.5.3 | Application framework |
| Spring Security | (bundled) | JWT-based authentication |
| Spring Data JPA | (bundled) | Database access layer |
| Hibernate | (bundled) | ORM with SQL Server dialect |
| WebSocket/STOMP | (bundled) | Real-time communication |
| Apache POI | 5.2.3 | Excel file processing |
| Gson | 2.8.5 | JSON serialization |
| Flyway | 7.7 (bundled) | Database migrations (dispatcher data model, Phase 2+) |
| llama.cpp Java bindings (de.kherud:llama) | 4.2.0 | Embedded LLM inference |

### Frontend

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 17.0.1 | UI framework |
| Redux | 4.0.5 | State management |
| React Router | 5.2.0 | Client-side routing |
| Material UI (MUI) | 5.14.14 | UI components |
| Bootstrap | 4.6.0 | CSS framework |
| Axios | 0.21.1 | HTTP client |
| Recharts / Chart.js | 3.9.1 / 4.3.1 | Data visualization |
| DevExpress Scheduler | 4.0.5 | Scheduling components |
| Webpack | 5.98.0 | Module bundler |
| Babel | 7.26.0 | JavaScript transpiler |
| Sass | 1.83.4 | CSS preprocessor |
| Jest | 29.7.0 | Testing framework |

### Database

- **Primary:** Microsoft SQL Server (main MG-CMS database)
- **Multi-datasource setup:** The application connects to 7 separate SQL Server databases:
  1. `LEAR_CMS_V5` — primary application database
  2. `LEAR_plt_viewer` — PLT file viewer data (CTC module)
  3. `LEAR_qualite` — quality management data (CMS module)
  4. `LEAR_MG_PLS_NEW` — PLS system data
  5. `LEAR_splice` — splice management data
  6. `LEAR_LearPokaYoke` — poka-yoke system data
  7. `LEAR_IMS_NEWAPP` — IMS application data

---

## Project Structure

```
MG-CMS/
├── pom.xml                          # Maven build configuration
├── package.json                     # NPM dependencies and scripts
├── webpack.config.js                # Frontend bundler configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/lear/
│   │   │       ├── MGCMS/           # Main application package
│   │   │       │   ├── MgcmsApplication.java
│   │   │       │   ├── controller/  # REST controllers (thin, validation-focused)
│   │   │       │   │   ├── cms/     # CMS (quality) controllers
│   │   │       │   │   ├── ctc/     # CTC (PLT viewer) controllers
│   │   │       │   │   ├── CuttingPlan/      # CAD cutting plan controllers
│   │   │       │   │   ├── CuttingRequest/   # Production demand controllers
│   │   │       │   │   ├── dispatcher/       # Process dispatcher controllers
│   │   │       │   │   ├── pls/     # PLS module controllers
│   │   │       │   │   ├── scanCoupe/
│   │   │       │   │   ├── scheduling/      # orphaned/legacy (optimizer UI removed)
│   │   │       │   │   └── splice/
│   │   │       │   ├── domain/      # JPA entities
│   │   │       │   ├── repositories/# Spring Data JPA repositories
│   │   │       │   ├── services/    # Business logic
│   │   │       │   ├── security/    # JWT, WebSocket, multi-datasource config
│   │   │       │   ├── payload/     # DTOs / request/response payloads
│   │   │       │   ├── exceptions/
│   │   │       │   ├── utils/
│   │   │       │   ├── validator/
│   │   │       │   └── storage/     # File storage properties
│   │   │       ├── cms/             # CMS (quality) domain + repositories
│   │   │       ├── ctc/             # CTC domain + repositories
│   │   │       ├── pls/             # PLS domain + repositories
│   │   │       ├── splice/          # Splice domain + repositories
│   │   │       └── learpokeyoke/    # PokaYoke domain + repositories
│   │   ├── resources/
│   │   │   ├── application.properties         # Main config (local dev defaults)
│   │   │   ├── application-tanger.properties  # Tanger production profile
│   │   │   ├── application-tunisie.properties # Tunisie production profile
│   │   │   ├── db/migration/        # Flyway SQL migrations
│   │   │   ├── templates/index.html # Thymeleaf root template (loads bundle.js)
│   │   │   └── static/              # Static assets + compiled bundle.js
│   │   └── js/                      # React frontend source (~190 files)
│   │       ├── index.js             # React entry point
│   │       ├── App.js               # Route table (large, department-driven)
│   │       ├── store.js             # Redux store
│   │       ├── history.js           # Browser history
│   │       ├── actions/             # Redux actions
│   │       ├── reducers/            # Redux reducers
│   │       ├── components/
│   │       │   ├── EntityList.js    # Generic metadata-driven list
│   │       │   ├── EntityForm.js    # Generic metadata-driven form
│   │       │   ├── Layout/          # Page-level components (one per screen)
│   │       │   │   ├── Home.js
│   │       │   │   ├── Landing.js
│   │       │   │   ├── LogisticsRelease.js
│   │       │   │   ├── CuttingPlan.js
│   │       │   │   ├── DemandeDeCoupe.js
│   │       │   │   ├── ChefDeZoneConfirm.js
│   │       │   │   ├── CncControl.js
│   │       │   │   ├── ... (80+ department screens)
│   │       │   ├── LlmChat/         # Embedded LLM chat widget
│   │       │   ├── styles/
│   │       │   └── utils/
│   │       ├── securityUtils/       # JWT handling, secured routes
│   │       └── assets/images/
│   └── test/java/com/lear/MGCMS/    # Backend tests
├── SQL/                             # Ad-hoc SQL scripts (not Flyway)
├── docs/                            # Current docs (docs/README.md) + docs/archive/ (historical)
├── scripts/                         # Utility scripts
└── target/                          # Maven build output
```

---

## Build and Run Commands

### Prerequisites

- **Java:** 16+ (configured in `pom.xml`)
- **Node.js:** 20.20.2 (exact version pinned in `.nvmrc` and `.node-version`)
- **NPM:** 10.8.2 (pinned as packageManager)
- **Maven:** 3.6+ (wrapper available: `./mvnw` or `mvnw.cmd`)
- **Database:** Microsoft SQL Server (local dev defaults to `MSI\SQLEXPRESS:1434`)

### Backend Build

```bash
# Full build (Java + frontend bundle via frontend-maven-plugin)
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Run Spring Boot application directly
./mvnw spring-boot:run
```

The `frontend-maven-plugin` is configured in `pom.xml` but does not define explicit execution bindings in the provided POM. In practice, the frontend build is often run separately via NPM.

### Frontend Build

```bash
# Install dependencies
npm install

# Development watch mode (outputs to target/classes/static/)
npm run watch

# Production build (outputs to target/classes/static/)
npm run prod

# Production build with custom output directory (e.g., for embedding in JAR)
npm run bundle:resources
```

Webpack outputs `bundle.js` to `target/classes/static/` by default. For packaging into the JAR, use `bundle:resources` which writes to `src/main/resources/static/`.

### Test Commands

```bash
# Backend tests
./mvnw test

# Frontend tests
npm test
# or
npx jest --runInBand
```

---

## Development Conventions

### Backend (Java / Spring Boot)

- **Layering:** Keep controllers thin (request handling, validation, security boundaries, response shaping). Business rules belong in services. Repository access must be explicit and reviewable.
- **Multi-datasource awareness:** Do not assume a single data source. Each module (CTC, CMS, PLS, splice, LearPokaYoke, IMS) has its own `Persistence*Configuration` in `security/`.
- **Entity locations:** Entities for the main app live in `com.lear.MGCMS.domain`. Cross-module entities live in their own top-level packages (`com.lear.cms.domain`, `com.lear.ctc.domain`, etc.).
- **Repositories:** Follow Spring Data JPA patterns. Watch for N+1 queries, hidden lazy-loading, and native query correctness.
- **DTO separation:** Keep DTOs, entities, payloads, and response contracts clearly separated when the code already distinguishes them.
- **Transactions:** Keep transaction boundaries explicit and as small as practical. Call out side effects (scheduled jobs, WebSocket updates, emails, file exports, integrations).
- **JWT security:** `JwtAuthenticationFilter` validates tokens on every request. `SecurityConfig` permits public access to `/api/kiosk/**` and `/api/public/**` GET endpoints for shopfloor kiosks. All other endpoints require authentication.
- **Feature flags:** Dispatcher/engine behavior is controlled by properties like `mgcms.dispatcher.enabled`, `mgcms.engine.zone-aware`, etc.

### Frontend (React / Redux)

- **Framework versions:** React 17 class components are common. Do not modernize to functional components + hooks unless explicitly requested.
- **Routing:** All routes are declared in `App.js`. Secured routes use `SecuredRoute` wrapper.
- **Generic screens:** `EntityList` and `EntityForm` provide metadata-driven CRUD. Changes to these affect many modules—treat as high-risk.
- **State management:** Redux with thunk middleware. Actions live in `actions/`, reducers in `reducers/`.
- **Styling:** Mix of Bootstrap 4, Material-UI 5, and SCSS. `styles/` folders contain component-specific SCSS.
- **API calls:** Axios is used throughout. Base URL is proxied to `http://localhost:8085` in development.
- **Build artifact:** The entire frontend compiles to a single `bundle.js` loaded by `index.html`.

### Database

- **Schema migrations:** Flyway is enabled (`spring.flyway.enabled=true`) and baselined at V1. Migration files are in `src/main/resources/db/migration/`.
- **Ad-hoc scripts:** One-off scripts live in `SQL/` and are **not** managed by Flyway.
- **Query safety:** Prefer explicit column lists over `SELECT *`. Be careful with reporting queries used by dashboards, ordonnancement views, shortages, KPIs, and quality follow-up.
- **Production data:** Treat roll consumption, part number data, scheduling outputs, box traceability, quality defects, and user-access configuration as production-critical.

---

## Testing Strategy

### Backend Tests

- **Framework:** JUnit 5 (via `spring-boot-starter-test`), with Awaitility for async assertions.
- **Test location:** `src/test/java/com/lear/MGCMS/`
- **Existing coverage includes:**
  - Dispatcher services: `ContinuousDispatchOptimizerServiceTest`, `LockResolverTest`, `MaterialAvailabilityCheckerTest`, `SchedulableSerieFilterTest`, `SeriesOrderingStrategyTest`, `SerieZoneResolverTest`
  - Controllers: `UserZoneControllerTest`, `WorkbenchControllerTest`
  - Scheduling: `CuttingTimeCalculatorTest`, `ShiftClockTest`
  - Domain services: `PartNumberCuttingTimeServiceTest`, `PartNumberWeightCalculationServiceTest`, `PieceDetailServiceTest`
  - LLM: `LlmServiceTest`
  - Utilities: `TableLengthConstraintTest`, `WorkOrderSplitFuseTest`

### Frontend Tests

- **Framework:** Jest 29 with `babel-jest`.
- **Config in `package.json`:**
  - Roots: `src/main/js`
  - Test match: `**/*.test.js`
  - Environment: `node`
- **Current coverage is minimal.** Most components do not have tests. The only existing frontend test is `chatStreamUtils.test.js`.

### Verification Expectations

- Frontend-only changes must pass the webpack production build (`npm run prod`).
- Backend changes should include targeted service/controller tests when practical.
- Cross-layer changes need explicit manual verification steps when automated coverage is weak.
- For production-sensitive flows, verification must describe the business scenario, not only compilation.
- High-value manual verification examples:
  - Creating or editing a cutting plan
  - Progressing a production or box status
  - Validating roll consumption or shortage calculations
  - Creating or validating a quality notice or defect path
  - Checking scheduling or plan de charge output
  - Confirming role-based access after admin or security edits

---

## Security Considerations

- **Authentication:** JWT-based stateless authentication. Tokens are generated on login and validated by `JwtAuthenticationFilter`.
- **Passwords:** BCrypt-encoded. `CustomUserDetailsService` loads users from the main database.
- **Authorization:** Role-based access control with `@PreAuthorize`, `@Secured`, and JSR-250 annotations enabled. Roles are seeded at startup by `MgcmsApplication` if missing.
- **Roles:** 25 distinct roles including `ROLE_ADMIN`, `ROLE_CAD`, `ROLE_ENGINEERING`, `ROLE_COUPEUR`, `ROLE_QUALITE`, `ROLE_CHEF_DE_ZONE`, `ROLE_MAINTENANCE`, `ROLE_QN_SUPERVISOR`, etc.
- **Public endpoints:** `/api/kiosk/**` and `/api/public/**` are intentionally open for shopfloor kiosk usage.
- **File uploads:** `multipart` limits are set to 2000MB to support large CAD files.
- **CORS/CSRF:** CSRF is disabled (`http.csrf().disable()`) because the API is stateless JWT.
- **Admin seeding:** If no admin user exists on startup, `admin` / `123456` is created with all roles. This is development-only behavior.

---

## Deployment and Runtime

- **Default port:** 8086 (hardcoded in `MgcmsApplication.java`)
- **Packaging:** Spring Boot executable JAR (`spring-boot-maven-plugin`)
- **Profile-specific configs:** `application-tanger.properties`, `application-tunisie.properties`
- **Static assets:** `bundle.js` and images are served from `src/main/resources/static/` (or `target/classes/static/`)
- **Scheduled tasks:** Enabled (`@EnableScheduling`). Thread pool size defaults to 10.
- **WebSocket:** Configured for real-time updates (STOMP over SockJS).
- **Email:** SMTP relay configured for internal Lear addresses.
- **File system paths:** Multiple hardcoded Windows paths for PLT files, cut files, labels, archives, and report folders.
- **LLM integration:** Optional embedded inference via llama.cpp. Disabled by default (`mgcms.llm.enabled=false`).

---

## AI-Assisted Workflows in This Repo

This repository has additional tooling for AI-assisted development:

1. **code-review-graph** — Local knowledge graph for structural context and impact analysis.
   - Commands: `code-review-graph status`, `code-review-graph build`
   - Prefer graph tools before broad file scanning when available.

2. **Continue** — Local review automation.
   - Config in `.continue/checks/fullstack-regression.md` and `.continue/agents/mg-cms-review.md`
   - Run: `cn review --base HEAD`

3. **GitHub Copilot instructions** — Department-aware, safety-first guidance in `.github/copilot-instructions.md`.

4. **Per-technology instructions:**
   - `.github/instructions/java-spring.instructions.md`
   - `.github/instructions/react.instructions.md`
   - `.github/instructions/sql-server.instructions.md`
   - `.github/instructions/testing.instructions.md`

When working on this codebase, read the relevant instruction file before making changes.

---

## Working Rules Summary

1. **Preserve working production behavior** unless requirements explicitly change it.
2. **Protect traceability** for rolls, part numbers, boxes, statuses, and historical records.
3. **Keep backend, frontend, and database contracts aligned.**
4. **Prefer small, reviewable changes** over broad rewrites.
5. **Highlight role or department impact** for every non-trivial change.
6. **Add or update verification** for risky changes.
7. **Do not rewrite stable modules** without a clear migration path.
8. **Treat scheduling, quality, logistics, and admin changes as high-risk** by default.
9. **For risky changes, include tests or explicit manual verification steps.**
