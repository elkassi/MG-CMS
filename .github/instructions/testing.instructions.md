---
applyTo: "**/*"
description: "MG-CMS testing expectations for production-safe backend, frontend, database, and workflow changes."
---

# Testing Guidance

Apply the repository-wide guidance from `../copilot-instructions.md` when proposing or implementing verification.

## General Standard

- Risky changes need verification.
- Prefer automated tests when the repository already supports them.
- If automated tests are not practical, provide explicit manual verification steps tied to the affected workflow.

## What Counts As Risky In MG-CMS

- changes to cutting plans, placements, part number rules, or material configuration
- changes to demand preparation, production status, boxes, or machine execution
- changes to quality notices, defects, scrap, or verification workflows
- changes to roll consumption, shortages, or logistics reconciliation
- changes to scheduling, ordonnancement, plan de charge, or KPI calculations
- changes to user roles, security, admin settings, or integrations

## Expected Verification Shape

- Frontend changes: production build plus relevant Jest coverage when available.
- Backend changes: targeted unit or integration tests when practical.
- Database changes: verification that includes affected application behavior, not just SQL syntax.
- Cross-layer changes: cover the full user flow, not only one layer.

## Review Standard

- Do not mark work complete just because it compiles.
- Missing tests for risky changes are a finding.
- Weak tests that miss the business outcome are also a finding.
- When using manual verification, name the role, screen, action, and expected outcome.