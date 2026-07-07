-- ============================================================================
-- V16_01__cutting_request_status_lifecycle.sql
-- Replace the CuttingRequest.sequenceStatus vocabulary with the new lifecycle
-- and add the persisted release zone.
-- ============================================================================
-- New vocabulary (see com.lear.MGCMS.domain.CuttingRequest.SequenceStatus):
--   IMPORTED          — set on import/creation (pre-release picklist candidate)
--   RELEASED          — logistics confirmed the picklist; releaseZone is fixed
--   STARTED           — a serie has begun spreading
--   COMPLETED         — every serie statusCoupe=Complete (auto), or chef-set
--   MATERIAL_MISSING  — cannot be spread completely with current stock
--   INCOMPLETE        — chef removed an unfinishable sequence from production
--
-- In-production set loaded by engine/workbench/dispatcher:
--   { RELEASED, STARTED, MATERIAL_MISSING } (+ legacy NULL).
--
-- Column naming: this project uses Hibernate's default naming strategy with NO
-- camelCase->snake_case conversion, so columns without @Column are camelCase in
-- the DB (e.g. the existing `sequenceStatus`). `releaseZone` therefore stays
-- camelCase to match the entity field (no snake_case).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Add releaseZone (camelCase, NULL-able so rollback is cheap). Idempotent.
-- ----------------------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'CuttingRequest')
      AND name = N'releaseZone'
)
BEGIN
    ALTER TABLE CuttingRequest
    ADD releaseZone VARCHAR(64) NULL;
END;

-- ----------------------------------------------------------------------------
-- 2. Data-migrate the OLD vocabulary to the new one.
--
-- CROSS-DATASOURCE LIMITATION: the authoritative lifecycle hint lives in
-- suiviplanning.Statu (joined on NSequence), but suiviplanning is on a SEPARATE
-- datasource (cms.datasource = a different SQL Server *database*, e.g.
-- LEAR_qualite) from the Flyway-managed primary DB that owns CuttingRequest.
-- A cross-database join would require hard-coding the environment-specific
-- database name (LEAR_qualite.dbo.suiviplanning), which is not portable across
-- dev/prod and unsafe to bake into a migration. We therefore perform the
-- VALUE-BASED FALLBACK MAPPING ONLY. If a suiviplanning-driven reconciliation
-- is ever needed, run it out-of-band per environment.
--
-- Each UPDATE only touches rows whose value is still in the OLD set, so this
-- migration is safe to re-run (idempotent): once a row holds a NEW value it is
-- never matched again.
-- ----------------------------------------------------------------------------

-- 'WAITING_MATERIAL' -> MATERIAL_MISSING
UPDATE CuttingRequest
SET sequenceStatus = 'MATERIAL_MISSING'
WHERE sequenceStatus = 'WAITING_MATERIAL';

-- 'PAUSED' -> INCOMPLETE (chef-removed-from-production semantics)
UPDATE CuttingRequest
SET sequenceStatus = 'INCOMPLETE'
WHERE sequenceStatus = 'PAUSED';

-- 'ACTIVE' / 'Active' / 'active' -> STARTED
UPDATE CuttingRequest
SET sequenceStatus = 'STARTED'
WHERE sequenceStatus IN ('ACTIVE', 'Active', 'active');

-- Legacy NULL rows that are still live work -> STARTED so the in-production
-- filters keep showing them. (Pre-cutover history was already back-filled to
-- 'COMPLETED' out-of-band; those rows are left untouched.)
UPDATE CuttingRequest
SET sequenceStatus = 'STARTED'
WHERE sequenceStatus IS NULL;

-- 'COMPLETED' already matches the new vocabulary verbatim — no update needed.

-- ----------------------------------------------------------------------------
-- 3. Index note: IX_CuttingRequest_active (V14_01) is a plain composite index
--    on (sequenceStatus, dueDate, dueShift) INCLUDE(...), NOT a filtered index
--    on the literal 'ACTIVE'. The new in-production values flow through it
--    unchanged, so no drop/recreate is required.
-- ----------------------------------------------------------------------------
