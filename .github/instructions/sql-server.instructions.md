---
applyTo: "**/*.{sql,SQL}"
description: "MG-CMS SQL Server guidance for production data, scheduling logic, reporting, quality, logistics, and admin-safe database changes."
---

# SQL Server Guidance

Apply the repository-wide guidance from `../copilot-instructions.md` to all database work.

## Data Criticality

- Treat roll consumption, part number data, scheduling outputs, box traceability, quality defects, and user-access configuration as production-critical data.
- Preserve the meaning of existing status fields, quantities, timestamps, and foreign-key relationships.
- Do not make destructive or semantic changes without stating the rollout and rollback risk.

## Query Design

- Prefer explicit column lists over `SELECT *`.
- Keep joins and filters readable enough to reason about correctness and performance.
- Be careful with reporting queries used by dashboards, ordonnancement views, shortages, KPIs, and quality follow-up.

## Schema And Script Safety

- Make schema changes deliberate, reviewable, and easy to trace back to affected application flows.
- Identify impacted controllers, services, screens, and user roles before changing tables, views, or scripts.
- Preserve backward compatibility when the application cannot switch all callers at once.

## Performance

- Watch for scans, unstable plans, hidden sorts, and poor predicates on high-volume production tables.
- Call out when a change needs execution-plan review instead of guessing.
- Treat scheduling, capacity, KPI, and shortage queries as likely hot paths.

## Stored Logic And Safety

- Keep stored procedures and scripts focused and reviewable.
- Avoid burying core business rules in database changes without surfacing the application impact.
- Highlight concurrency, transaction, and data-correction risks explicitly.