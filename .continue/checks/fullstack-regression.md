---
name: Fullstack Regression Review
description: Review MG-CMS changes for backend, frontend, database, role-access, and traceability regressions before production rollout.
---

Review this pull request with a bug-finding mindset.

Check for:

1. Backend response changes that are not reflected in the frontend.
2. Frontend assumptions that no longer match API, authorization, or database behavior.
3. SQL or schema changes without corresponding application updates.
4. Missing validation, transaction safety, or error-handling coverage.
5. Missing tests or missing manual verification for risky business flows.
6. Traceability regressions involving rolls, part numbers, boxes, scheduling states, or quality records.

Return:

1. Findings ordered by severity.
2. Clear reasoning for each finding.
3. A suggested focused fix when practical.

Do not spend effort on style-only comments unless they affect correctness, maintainability, or review safety.