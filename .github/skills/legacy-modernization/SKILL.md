---
name: legacy-modernization
description: Use when auditing or modernizing MG-CMS in staged, low-risk slices with attention to production continuity, architecture debt, tests, performance, and maintainability.
---

# Legacy Modernization

Use this skill when working on MG-CMS as an already-built production repository that needs cleanup, structure, safety, or performance improvements.

Ask for the target repository area, current pain points, impacted departments, and tolerance for refactoring if they are not already clear.

## Workflow

1. Map the existing architecture and high-risk workflows.
2. Separate quick wins from structural changes.
3. Propose modernization in phases.
4. Prefer vertical slices over subsystem-wide rewrites.
5. Tie each phase to verification and rollback thinking.

## Requirements

- Preserve business behavior.
- Avoid large speculative rewrites.
- Surface missing tests as a first-class risk.
- Identify backend, frontend, database, and role dependencies before planning changes.
- Protect production continuity and traceability.

## Output Expectations

1. Short architecture summary.
2. Ranked technical debt areas.
3. Suggested implementation phases.
4. Risks, dependencies, and testing needs.