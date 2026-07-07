# MG-CMS - Copilot Instructions

## Project Overview

MG-CMS is a production-critical manufacturing application used at Lear Trim Tangier to manage cutting operations from engineering and CAD preparation through production execution, quality follow-up, logistics consumption tracking, process scheduling, and application administration.

The business flow is operationally sensitive:

- rolls of material are planned, allocated, cut, and consumed
- part numbers, cutting plans, and machine assignments must stay consistent
- produced pieces are grouped into boxes and sent downstream to sewing
- quality, scrap, shortage, maintenance, and KPI data must remain traceable
- user roles and permissions affect what each department can see or change

## Current Stack

- Spring Boot 2.5.3 backend on Java 17
- React 17 frontend with Redux and React Router 5
- Webpack-based frontend build
- Microsoft SQL Server as the primary production database, plus additional configured data sources and integrations
- JWT-based authentication, secured routes, WebSocket support, and Excel import/export flows

## AI Context Workflow

This repository has a local `code-review-graph` knowledge graph and a Continue review workflow.

- For large exploration, impact analysis, and refactor planning, prefer the graph workflow when it is available instead of broad manual scanning.
- For local review of changes, prefer `cn review --base HEAD` with the MG-CMS review agent before assuming a change is safe.
- Treat these tools as force multipliers for repository context and review quality, not as replacements for tests or domain reasoning.

## Global Priorities

1. Preserve working production behavior unless requirements explicitly change it.
2. Protect traceability for rolls, part numbers, boxes, statuses, and historical records.
3. Keep backend, frontend, and database contracts aligned.
4. Prefer small, reviewable changes over broad rewrites.
5. Highlight role or department impact for every non-trivial change.
6. Add or update verification for risky changes.

## Department And Workflow Awareness

When evaluating a change, consider which departments are affected:

- CAD: cutting plans, placements, pattern search, material placement, speed, drill, and box configuration
- Engineering: part number specifications, material configuration, weight, cutting time, and technical rules
- Production: demand preparation, matelassage, cutting status, machine execution, and box tracking
- Quality: quality notices, defect validation, audits, verification, scrap, and follow-up dashboards
- Logistics: roll consumption, shortage visibility, stock verification, and material allocation accuracy
- Process and ordonnancement: scheduling, plan de charge, optimized sequencing, and capacity visibility
- Admin and IT: user management, security, configuration, integrations, and operational settings

If a change affects one of these areas, call out the impacted workflows instead of treating it as a generic CRUD change.

## Architecture Expectations

- Keep controllers thin and focused on request validation, response composition, and authorization boundaries.
- Keep business rules in services or domain-oriented code.
- Keep repositories and SQL behavior explicit enough to reason about performance and correctness.
- Preserve the existing generic UI patterns built around metadata, EntityList, EntityForm, and route-driven screens when those patterns already own the flow.
- Preserve the established security model based on JWT, secured routes, and role-driven access.
- Be careful with multi-datasource, integration, scheduled-task, and WebSocket behavior.

## Working Style

Before changing code:

1. Map the current flow end to end.
2. Identify the impacted screen or route, backend endpoint, service, repository or SQL object, and user role.
3. Check whether the change touches status transitions, planning logic, quantities, or traceability keys.
4. Check for existing repository patterns before introducing a new one.

When implementing:

1. Avoid unrelated refactors.
2. Preserve public contracts unless the task includes a contract change.
3. Do not silently change route names, endpoint shapes, status semantics, or database meanings.
4. Treat scheduling, quality, material consumption, and admin changes as high-risk unless proven otherwise.

## Verification Expectations

- Frontend-only changes should at least pass the webpack production build and relevant Jest tests.
- Backend changes should use targeted Spring or service tests when practical.
- Cross-layer changes should include explicit manual verification steps when automated coverage is weak.
- For production-sensitive flows, verification should mention the business scenario, not only compilation.

Examples of high-value manual verification:

- creating or editing a cutting plan
- progressing a production or box status
- validating roll consumption or shortage calculations
- creating or validating a quality notice or defect path
- checking scheduling or plan de charge output
- confirming role-based access after admin or security edits

## What To Avoid

1. Full rewrites of stable modules.
2. Introducing a new frontend architecture during routine feature work.
3. Hiding SQL behavior changes inside service refactors.
4. Renaming large route or API surfaces without an explicit migration plan.
5. Assuming a change is safe because one department screen still renders.