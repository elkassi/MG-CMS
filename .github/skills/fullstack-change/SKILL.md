---
name: fullstack-change
description: Use when implementing an MG-CMS feature or bug fix that touches Spring Boot backend, React frontend, and SQL Server while preserving contracts, traceability, and role-based workflows.
---

# Fullstack Change

Use this skill for bounded MG-CMS work that crosses backend, frontend, and database layers.

Ask for the business goal, impacted departments or roles, affected screens or APIs, and constraints if they are not already clear.

## Workflow

1. Map the current flow end to end.
2. Identify the frontend route or screen, backend entry point, service layer, and affected tables, queries, or scripts.
3. Identify which departments and roles are impacted.
4. Propose the smallest safe implementation slice.
5. Keep backend, frontend, and database contracts aligned.
6. Add tests or explicit manual verification steps.

## Requirements

- Preserve behavior unless requirements say otherwise.
- Avoid unrelated refactors.
- Keep controller logic thin.
- Keep UI work aligned to actual API contracts.
- Highlight scheduling, quality, logistics, and traceability risk explicitly.

## Output Expectations

1. Clear implementation plan.
2. Files or modules expected to change.
3. Contract, role, or schema impacts.
4. Verification method.