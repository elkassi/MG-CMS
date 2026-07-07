-- ============================================================================
-- V17_03__production_table_efficience.sql
-- Per-machine expected efficiency (%). PlanDeCharge uses it to scale cutting
-- time: effectiveCuttingTime = base / (efficience / 100). This replaces the
-- hardcoded "Gerber x2" for PlanDeCharge; OrdonnancementService keeps the
-- legacy x2 (it calls the calculator's legacy overload).
--
-- Seeding preserves today's numbers once PlanDeCharge's capacity denominator
-- goes raw (no group efficiency factor anymore):
--   * Gerber machines -> 50  (1 / 0.50 = x2, identical to the old multiplier)
--   * all other machines -> 90 (1 / 0.90, which compensates for dropping the
--     old 90% group factor that used to sit in the denominator)
--
-- Column stays camelCase to match the entity field (no naming-strategy
-- conversion in this project). hibernate.ddl-auto = none, so this column must
-- be created here.
-- ============================================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'ProductionTable') AND name = N'efficience'
)
BEGIN
    ALTER TABLE ProductionTable
        ADD efficience FLOAT NOT NULL CONSTRAINT DF_ProductionTable_efficience DEFAULT 90;
END;

-- Seed Gerber machines to 50%. The MachineType FK column name is resolved from
-- the catalog so the migration cannot fail on a name guess (it is machineType_name
-- under JPA-compliant naming, but we never hard-code it). MachineType's PK is its
-- 'name', so the FK column holds the machine-type name directly.
DECLARE @fkCol sysname;

SELECT @fkCol = pc.name
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id
                   AND pc.column_id = fkc.parent_column_id
WHERE fk.parent_object_id = OBJECT_ID(N'ProductionTable')
  AND fk.referenced_object_id = OBJECT_ID(N'MachineType');

IF @fkCol IS NULL AND EXISTS (
    SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'ProductionTable') AND name = N'machineType_name')
    SET @fkCol = N'machineType_name';

IF @fkCol IS NULL AND EXISTS (
    SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'ProductionTable') AND name = N'machineType_id')
    SET @fkCol = N'machineType_id';

IF @fkCol IS NOT NULL
BEGIN
    DECLARE @sql nvarchar(max) = N'
        UPDATE pt SET pt.efficience = 50
        FROM ProductionTable pt
        JOIN MachineType mt ON mt.name = pt.' + QUOTENAME(@fkCol) + N'
        WHERE mt.name = ''Gerber''';
    EXEC sp_executesql @sql;
END;
