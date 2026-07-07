---
name: Audit
description: Diagnose-first feature/area audit — map requirements to code, write findings, fix only low-risk items, verify the build
---

## Audit

Run a structured, diagnose-first audit of a feature, screen, or area. Surface root
causes before touching code, write findings down, then fix only the low-risk items
and prove the build still works. Favors deep diagnosis over fast patching.

### Steps

1. Scope: restate the target feature/screen/area and the user role(s) that hit it.
2. Map the flow with the graph (NOT Grep first): `get_minimal_context`, then
   `semantic_search_nodes` / `query_graph` to trace route → component → controller →
   service → repository → entity. Record exact DTO field names and DB column names —
   do NOT assume snake_case or a unified array (see CLAUDE.md naming strategy).
3. Detect risk: `detect_changes` + `get_impact_radius` on the touched nodes to see
   blast radius and which departments/flows are affected.
4. Write findings to `GAP_ANALYSIS.md` grouped by risk (high/medium/low): what's
   wrong, root cause, affected layer, and the smallest correct fix.
5. Fix ONLY low-risk items in this pass. Leave high-risk (status transitions,
   quantities, traceability, scheduling, quality, access) as written proposals.
6. Verify after EACH fix, do not batch:
   - Backend: `mvn -q compile` (or `mvn test -Dtest=...` for touched service/controller)
   - Frontend: `npm run prod` (and `npm test -- <file>.test.js` for risky UI)
7. Report a checklist: what passed, what changed file-by-file, what still needs manual
   verification tied to the business workflow.

### Tips

- Diagnose the root cause before editing — name the layer (DTO shape, query, render
  pipeline) rather than guessing and iterating.
- Verify field/column names against the source before binding UI or writing native SQL;
  this is the #1 source of repeat-iteration bugs in this codebase.
- Never declare done on an unverified compile. Green build + named manual steps = done.

## Token Efficiency Rules
- ALWAYS start with `get_minimal_context(task="<your task>")` before any other graph tool.
- Use `detail_level="minimal"` on all calls. Only escalate to "standard" when minimal is insufficient.
- Target: complete the diagnosis in ≤6 graph tool calls; reserve the rest for fixes and verification.
