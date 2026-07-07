---
applyTo: "**/*.java"
description: "MG-CMS Spring Boot backend standards for production, CAD, quality, logistics, scheduling, integration, and admin changes."
---

# Java Spring Guidance

Apply the repository-wide guidance from `../copilot-instructions.md` to all backend work.

## Layering

- Keep controller classes focused on request handling, validation, security boundaries, and response shaping.
- Keep business rules in services, not in controllers.
- Keep repository access explicit and reviewable, especially for scheduling, reporting, and consumption logic.

## Business Safety

- Preserve status transitions, traceability fields, and quantity calculations.
- Treat changes to cutting plans, roll consumption, box tracking, scheduling, quality flows, or user access as high-risk.
- When editing logic that affects more than one department, name the impacted departments in your reasoning or verification.

## Persistence And Data Sources

- Do not assume a single data source or a single integration boundary.
- Be careful with native queries, custom repository methods, and cross-database configuration.
- Watch for N+1 query patterns, hidden lazy-loading behavior, and service methods that trigger more database work than expected.

## Contracts

- Keep DTOs, entities, payloads, and response contracts clearly separated when the code already distinguishes them.
- Do not silently change response shapes used by React screens, generic list views, exports, or dashboards.
- When a backend contract changes, identify the affected frontend route and verification path before editing.

## Transactions And Side Effects

- Keep transaction boundaries explicit and as small as practical.
- Call out side effects that can affect scheduled jobs, WebSocket updates, emails, file exports, or integrations.
- Avoid packing multiple independent workflow transitions into one large method unless the existing design requires it.

## Security

- Preserve JWT authentication and role-based access behavior.
- If an endpoint access rule changes, state which user roles gain or lose access.
- Treat admin and security changes as production-sensitive by default.

## Testing

- Prefer targeted service and controller tests for business-rule changes.
- Add integration coverage for risky persistence, transaction, or multi-step workflow changes when practical.
- If automated coverage is limited, provide exact manual verification steps by business flow.