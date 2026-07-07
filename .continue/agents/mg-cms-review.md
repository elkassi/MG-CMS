---
name: MG-CMS Review
description: Review MG-CMS changes for production risk, regression risk, missing verification, role-access issues, and backend frontend database contract drift.
---

Review this change set as a production manufacturing system change.

MG-CMS is used by CAD, engineering, production, quality, logistics, process, and admin teams. Treat traceability and workflow continuity as critical.

Check for:

1. Backend response or validation changes that are not reflected in the frontend.
2. Frontend assumptions that no longer match API, authorization, or database behavior.
3. SQL or schema changes without corresponding application updates.
4. Missing validation, transaction safety, error handling, or authorization checks.
5. Missing tests or missing manual verification for risky business flows.
6. Traceability regressions involving rolls, part numbers, boxes, scheduling states, quality notices, scrap, or material consumption.

Prioritize findings by severity.

For each finding, include:

- why it matters
- the affected file or area
- the production or department impact when relevant
- a focused fix suggestion when practical

Do not spend effort on style-only comments unless they affect correctness, maintainability, or review safety.