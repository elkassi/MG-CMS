-- ProgramCNC: duplicates are now allowed (the upsert-on-(partNumber,panelNumber) was removed
-- from ProgramCNCService). Add audit columns + a change-history table (creation / update / delete).
-- hibernate.ddl-auto = none, so columns/tables must be created here. Columns stay camelCase to
-- match the entity fields (no naming-strategy conversion). Idempotent / catalog-safe.

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'ProgramCNC') AND name = N'updatedAt')
    ALTER TABLE ProgramCNC ADD updatedAt DATETIME2 NULL;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'ProgramCNC') AND name = N'updatedBy')
    ALTER TABLE ProgramCNC ADD updatedBy NVARCHAR(255) NULL;

IF OBJECT_ID(N'ProgramCNCHistory', N'U') IS NULL
BEGIN
    CREATE TABLE ProgramCNCHistory (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        operationDate DATETIME2 NULL,
        username NVARCHAR(255) NULL,
        operation NVARCHAR(20) NULL,
        snapshot NVARCHAR(2000) NULL
    );
END;
