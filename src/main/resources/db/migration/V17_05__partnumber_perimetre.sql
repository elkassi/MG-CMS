-- ============================================================================
-- V17_05__partnumber_perimetre.sql
-- Cached per-part-number cut perimeter (sum of the D,6 piece perimeters across
-- a plan's / a request's placements). Feeds the Plan de Charge part-number
-- cutting-time report (perimeter share -> % of plan -> cutting time).
--
-- Columns stay camelCase to match the entity fields (no naming-strategy
-- conversion). hibernate.ddl-auto = none, so they must be created here.
-- Catalog-safe / idempotent.
-- ============================================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'CuttingPlanPartNumber') AND name = N'perimetre'
)
BEGIN
    ALTER TABLE CuttingPlanPartNumber ADD perimetre FLOAT NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'CuttingRequestPartNumber') AND name = N'perimetre'
)
BEGIN
    ALTER TABLE CuttingRequestPartNumber ADD perimetre FLOAT NULL;
END;
