---
applyTo: "**/*.{js,jsx,ts,tsx}"
description: "MG-CMS React frontend standards for production, CAD, quality, logistics, scheduling, dashboard, and admin screens."
---

# React Guidance

Apply the repository-wide guidance from `../copilot-instructions.md` to all frontend work.

## Existing Frontend Shape

- This repository uses React 17, Redux, React Router 5, Axios, secured routes, and a large route table in App.js.
- Many screens are legacy class components.
- Generic CRUD and metadata-driven flows exist through EntityList, EntityForm, and metadata-based configuration.

Work with those patterns unless the task explicitly calls for structural modernization.

## Screen And Component Boundaries

- Keep page-level components focused on user workflow and composition.
- Extract reusable UI when the same logic repeats, but avoid gratuitous abstraction.
- Do not replace established repository patterns with a new state or routing approach during ordinary feature work.

## Workflow Safety

- Treat route changes as high-risk because many departments depend on stable screen paths and behaviors.
- Preserve observable workflow states for CAD, production, quality, logistics, scheduling, and admin users.
- Do not patch around backend inconsistencies in the UI unless the task explicitly asks for a temporary mitigation.

## Data And API Usage

- Keep API assumptions aligned with actual backend contracts.
- Preserve JWT and secured-route behavior.
- Be explicit about loading, empty, error, and validation states for operational screens.
- If a response shape or form payload changes, identify the backend endpoint and affected business flow.

## Generic Screens

- When working on EntityList, EntityForm, metadata, or dynamic routes, assume there may be broad cross-module impact.
- Avoid changes that silently alter field interpretation, list behavior, or form semantics across many entities.
- Confirm whether a generic UI change affects admin-only configuration screens as well as production-facing screens.

## Verification

- Prefer repository-supported frontend tests for risky UI behavior.
- Always validate the production webpack build for meaningful frontend changes.
- If automated tests are thin, provide manual verification steps that describe the business scenario and role involved.