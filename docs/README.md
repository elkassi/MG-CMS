# MG-CMS Documentation

## Current documents

| Document | Purpose |
|----------|---------|
| [schema.md](schema.md) | **Database & workflow reference** — all six databases, every table/column meaning, which page/production step uses it, status lifecycles, id conventions, known hot spots. Start here for any data question. |
| [recommendations.md](recommendations.md) | Improvement backlog (2026-06-11): indexing plan, batch import, roll tracking, design/flexibility guidance across MG-CMS / CMS-Prod / scanCoupe. |
| [picklist-logistics-master-plan.md](picklist-logistics-master-plan.md) | **Active plan** — logistics release tool over the `suiviplanning` Non démarrée → Released flow (direction set 2026-05-31). |
| [guide-rectification-chef.md](guide-rectification-chef.md) | **Guide chef d'équipe (FR)** — page Rectification Séquences : corriger statut/zone, clôture en masse, write-through suiviplanning, zones auto-déduites de la table de coupe (sources log/chef/auto), kill-switches `mgcms.sequence.rectify.enabled` / `mgcms.sequence.zoneAutofix.enabled`. |
| [production-flow-and-strategy.md](production-flow-and-strategy.md) | End-to-end production flow (CAD → planning → dispatch → cutting → logistics) and product strategy. |
| [../AGENTS.md](../AGENTS.md) | Complete project guide: stack, packages, build, conventions. Start here. |
| [../CLAUDE.md](../CLAUDE.md) | Claude Code entry point: build commands + toolchain gotchas. |
| [../.github/instructions/](../.github/instructions/) | Layer-specific working standards (Java/Spring, React, SQL Server, testing). |

## Current top-level features

The live floor flow (routes in `../src/main/js/App.js`, menu in
`../src/main/js/components/Dashboard.js`):

- **Logistics Release** (`/logisticsRelease`) — release `suiviplanning` rows Non démarrée → Released over the non-XA `cms` datasource (commit compensates the flip on local failure).
- **Process Workbench / Table Feed / next-series** (`/processWorkbench`, `/tableFeed`) — feed the matelassage; banded-lexicographic "best series" ranking per table.
- **Production Floor** (`/productionFloor`) — machine/table state probed at a shift-correct hour.
- **CNC quality** (`/cncPs`, `/cncControl`, `/cncQualite`, `/cncQualiteMachine`) — per-programme leather control, ProgramCNC audit trail, per-machine quality report (`/cncQualiteMachine`, commit ea62f60), expandable PN-Cuir/Fil-Couture pattern reference images (commit 35c38ed), and box-ID-must-start-with-'S' validation (commit 757998e).
- **Plan de Charge** (`/planDeCharge`) — load planning with efficiency/capacity rules and the part-number perimeter report.
- **System Health / Archiving** (`/systemHealth`, `/archiving`) — admin DB probes and whitelisted archiving (ADMIN-only).
- **Chef rectification & zone confirmation** (`/chefDeZoneConfirm`, Rectification Séquences) — confirm machines/tables and correct sequence status/zone with write-through to `suiviplanning`.

The continuous dispatch optimizer was **shelved 2026-05-31** and the Ordonnancement /
scheduling UI (advanced ordonnancement, scheduling dashboard, chef-de-zone supervision /
affectation pages) was **removed 2026-06-30**; treat any archive doc describing them as current
with that caveat.

## Archive

[archive/](archive/) holds historical plans, analyses, and session reports (Dec 2025 – Jun 2026).
They describe the codebase **as it was when written** and are kept for context only — do not
treat them as current. See [archive/README.md](archive/README.md).
