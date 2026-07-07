-- ============================================================================
-- V16_04__cutting_plan_foam_backfill.sql
-- Backfill CuttingPlan.foam: legacy rows created before the foam flag existed
-- have foam IS NULL. The role model treats ROLE_CAD as the owner of non-foam
-- plans (foam = false) and ROLE_CAD_FOAM as the owner of foam plans
-- (foam = true). NULL therefore behaves like "non-foam" everywhere except the
-- generic list filter (equal.foam = 0), which excludes NULL rows and hides them
-- from ROLE_CAD users. Normalise NULL -> false so the filter and the foam flag
-- agree.
--
-- Column naming: this project uses Hibernate's default naming strategy with NO
-- camelCase->snake_case conversion, so the column is the camelCase `foam`
-- (no @Column on the entity field).
--
-- Idempotent: only rows still holding NULL are touched, so re-running is a no-op.
-- ============================================================================

UPDATE CuttingPlan
SET foam = 0
WHERE foam IS NULL;
