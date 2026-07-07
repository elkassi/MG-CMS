-- ============================================================================
-- V17_08__programcnc_panel_number.sql
-- ProgramCNC: add the panelNumber column and move the natural key from
-- (partNumber, pattern) to (partNumber, panelNumber).
--
-- Columns stay camelCase to match the entity fields (no naming-strategy
-- conversion). hibernate.ddl-auto = none, so the column must be created here.
-- Uniqueness is enforced by ProgramCNCService.save (upsert-on-match), as it
-- already was; we only DROP the old (partNumber, pattern) DB constraint if one
-- is present so it can't reject the new "same pattern, different panel" rows.
-- ponytail: no new DB unique index added — parity with the prior design, and a
-- plain one would fail on the existing NULL-panel rows. Catalog-safe / idempotent.
-- ============================================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'ProgramCNC') AND name = N'panelNumber'
)
BEGIN
    ALTER TABLE ProgramCNC ADD panelNumber NVARCHAR(255) NULL;
END;

-- Drop whatever Hibernate auto-named the (partNumber, pattern) unique constraint.
-- No-op when it was never created (ddl-auto = none).
DECLARE @cn NVARCHAR(128);
SELECT @cn = kc.name
FROM sys.key_constraints kc
WHERE kc.parent_object_id = OBJECT_ID(N'ProgramCNC')
  AND kc.type = 'UQ'
  AND (SELECT COUNT(*) FROM sys.index_columns ic
       WHERE ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id) = 2
  AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c
              ON c.object_id = ic.object_id AND c.column_id = ic.column_id
              WHERE ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
                AND c.name = N'partNumber')
  AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c
              ON c.object_id = ic.object_id AND c.column_id = ic.column_id
              WHERE ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
                AND c.name = N'pattern');
IF @cn IS NOT NULL
    EXEC('ALTER TABLE ProgramCNC DROP CONSTRAINT [' + @cn + ']');
