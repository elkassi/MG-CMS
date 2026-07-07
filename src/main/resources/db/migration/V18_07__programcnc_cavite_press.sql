-- ProgramCNC: add the "Cavité Press" attribute (shown between Fil Couture CNC and Fil blind in the
-- Programmes CNC CRUD, /cncPs and /cncControl, and on the printed box label).
-- hibernate.ddl-auto = none, so the column must be added here. camelCase to match the entity field
-- (no naming-strategy conversion). Idempotent / catalog-safe.

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'ProgramCNC') AND name = N'cavitePress')
    ALTER TABLE ProgramCNC ADD cavitePress NVARCHAR(255) NULL;
