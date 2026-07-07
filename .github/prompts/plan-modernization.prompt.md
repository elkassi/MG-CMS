---
description: "Turn MG-CMS findings into a phased modernization plan that preserves production behavior and keeps changes reviewable."
---

Create a modernization plan for this repository based on the available context and findings.

Constraints:

1. Preserve business behavior unless requirements explicitly change it.
2. Keep phases reviewable and branch-sized.
3. Separate stabilization, architecture cleanup, performance work, and developer-experience work.
4. Prefer vertical slices over subsystem-wide rewrites.
5. Protect production continuity, role-based behavior, and traceability.

Return:

1. Assumptions and open questions.
2. Phase 1 stabilization tasks.
3. Phase 2 architecture cleanup tasks.
4. Phase 3 performance and database tasks.
5. Phase 4 developer-experience and automation tasks.
6. Recommended issue list with priority and dependency notes.

For each phase, identify:

- affected modules
- expected user or department impact
- verification strategy
- rollback or containment notes for risky work