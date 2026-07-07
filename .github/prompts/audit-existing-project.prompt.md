---
description: "Audit MG-CMS as an existing production repository and return a prioritized improvement plan grounded in real modules, workflows, and risks."
---

Audit this repository as a production manufacturing system used by CAD, engineering, production, quality, logistics, process, and admin teams.

Requirements:

1. Start by mapping the backend, frontend, database, build, security, and test entry points.
2. Do not propose a full rewrite.
3. Prefer findings tied to real files, workflows, or modules.
4. Focus on regression risk, maintainability, architecture debt, test gaps, performance risk, and role-based behavior drift.
5. Call out where production continuity or traceability could be affected.

Return:

1. A short architecture map.
2. The top findings ordered by severity.
3. Quick wins that can be done in 1 to 2 days.
4. Medium refactors that should be split into slices.
5. Missing tests, missing validation, or missing verification paths.
6. Suggested repository customizations or docs that would improve future AI work.

For each finding, include:

- why it matters
- affected area
- departments or roles impacted when relevant
- recommended fix shape
- expected verification method