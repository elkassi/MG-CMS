# GAP_ANALYSIS.md — fallow code-intelligence sweep (results)

**Date:** 2026-06-05
**Tool:** `fallow@2.88.3` (deterministic JS/TS code intelligence) on `src/main/js`
**Raw report:** `../fallow-report.md`
**Verification:** frontend `npm run prod` green after every change; backend `mvn -o compile` green.

fallow found **109 dead-code issues, 726 clone groups (39.5% duplication), 980 high-complexity functions** (JS only — the Java backend is not covered). Every action below was driven by grep/config/git evidence, not assumption.

---

## ✅ Applied & verified

| # | Change | Evidence | Verification |
|---|--------|----------|--------------|
| 1 | **Declared `prop-types@^15.8.1`** in `package.json` deps | imported by 31 files, was undeclared (transitive-only → latent break) | additive; build green |
| 2 | **Deleted 15 stale dead files** (~7,700 lines) | unreachable from entry, 0 inbound imports (grep-verified), last touched 2022→2025-12, no dynamic imports anywhere | `npm run prod` green (3.7s) |
| 3 | **Removed 11 unused packages** (9 deps + 2 devDeps) | 0 source references (grep) | `npm uninstall` + `npm run prod` green; lock upgraded v1→v3 |

**Deleted files (#2):** `automate.js`, `actions/userAction.js`, `Layout/CtcFiles.js`, `Layout/CtcFilesForm.js`, `Layout/QnForm.js`, `Layout/StatutQN.js`, `Layout/OrdonnancementV2.js`, `Layout/ordonnancement/` (6 files — superseded by V3/AdvancedOrdonnancement), `styles/SerieOrganisation.scss`, `styles/ordonnancement.scss`.

**Removed packages (#3):** `jspdf`, `html2canvas`, `react-google-login`, `react-icons`, `react-country-flag`, `react-json-formatter`, `rc-input-number`, `@fontsource/roboto`, `web-vitals`, `url-loader`, `copy-webpack-plugin`.
> Residual risk: a green build proves no *static* import broke; it cannot prove a *dynamic/runtime* use isn't affected. Smoke-test the running app on your Node 20 toolchain to fully confirm. All changes are git-reversible.

---

## 🟡 KEPT by decision — 15 recent files (likely staged WIP, NOT wired into routes)

fallow flags these "unused" because nothing imports them yet — but they were committed in the **last 4-7 weeks** and map to active features. Treated as in-progress, **not deleted**. Either wire them into `App.js` routes or delete deliberately when each feature lands.

| File | Last commit | Likely purpose |
|------|-------------|----------------|
| `Layout/WorkbenchSection.js` | 2026-05-07 | ↔ `WORKBENCH_SPEC.md` |
| `Layout/ShiftCompletionView.js` | 2026-05-14 | shift-completion screen |
| `Layout/MaterialForecastView.js` | 2026-05-14 | logistics forecast |
| `Layout/BoxStatusView.js` | 2026-05-13 | box tracking |
| `Layout/GanttView.js` | 2026-05-11 | dispatcher gantt (export also in AdvancedOrdonnancement) |
| `Layout/SequenceFlowIndicator.js` | 2026-05-11 | dispatcher UI |
| `Layout/AuditView.js` | 2026-05-08 | quality audit |
| `Layout/MaterialPreviewView.js` | 2026-05-08 | material preview |
| `Layout/StockAvailabilityView.js` | 2026-05-08 | logistics stock |
| `Layout/PlanDeChargeView.js` | 2026-05-07 | scheduling (vs active `PlanDeCharge.js`) |
| `Layout/DispatchEngineChart.js` | 2026-05-05 | dispatcher chart |
| `Layout/AdmissionBlockModal.js` | 2026-04-24 | modal |
| `Layout/KioskBanner.js` | 2026-04-24 | kiosk UI |
| `components/Dashboard-Tunisie.js` | 2026-04-24 | plant dashboard variant |
| `components/Dashboard-tanger.js` | 2026-05-19 | plant dashboard variant |

> The 39.5%-duplication "Dashboard consolidation" item collapses into this: `Dashboard-tanger`/`Dashboard-Tunisie` duplicate `Dashboard.js` (613-line clone). If they're abandoned, delete; if they're the intended per-plant split, finish wiring them. Your call per-file.

---

## ❌ Confirmed false positives — correctly NOT touched

fallow flagged these "unused devDependencies"; they're used as **string config values** (not `import`ed), proven by `webpack.config.js:39-43` + jest config:
`@babel/preset-env`, `@babel/preset-react`, `@babel/plugin-transform-runtime`, `@babel/plugin-proposal-class-properties`. Deleting them would break `npm run prod` + `npm test`.

---

## 🚫 Risky deps — KEPT (interconnected; need runtime check before removal)

`styled-components` + `@mui/styled-engine-sc` (MUI styled engine), `@devexpress/dx-react-chart-material-ui` (DevExpress peer cluster), `@testing-library/jest-dom`/`react`/`user-event` (test infra), `react-ipgeolocation`. fallow flags them unused, but each carries removal risk; verify with a test run before pruning.

---

## 📋 Remaining (not actioned — needs deliberate, scoped work)

- **20 unused exports / 32 unused class members** (e.g. `default.getCR`, `convertToMachine`, `chargerReftissuConfig`) — dead methods; medium risk (possible dynamic dispatch). Remove per-file when touching each component.
- **39.5% duplication (726 clone groups)** — `EntityForm`/`EntityList`/`CtcFilesForm`/`PartNumberMaterialConfigForm` family + Dashboard triplication. High-leverage but high-risk refactor.
- **980 high-complexity functions** — worst: `Dashboard.js render` (cyclo 141 / 1033 lines / CRAP 20022), `Form.js render` (127), `SchedulingDashboard.js` (1654 lines), `CuttingPlanForm.js`. Extract sub-renders incrementally, with tests, when each screen is next touched.
- **Backend (Java)** — uncovered by fallow; `pom.xml` has no static-analysis plugins and the `code-review-graph` MCP isn't connected to this session. Compiles clean. For a real audit: install JDK 17, connect the graph MCP, or add spotbugs/PMD.
