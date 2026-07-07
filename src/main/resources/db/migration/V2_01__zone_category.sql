-- V2_01: Zone.category + Zone.is_active.
--
-- STRICT zones hold fixed machine-type sets (e.g. 3x Lectra + 1x Lectra IP6
-- for FirstArticle). The SHARED zone holds LASER-DXF + LASER-LSR and spills
-- over for STRICT zones that need laser work. Every existing row defaults to
-- STRICT; ops flips the physical shared zone to SHARED via the last UPDATE
-- block below. DO NOT guess that zone's nom — confirm with ops before running
-- this script in production and edit the WHERE clause accordingly.

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'Zone')
      AND name = N'category'
)
BEGIN
    ALTER TABLE Zone
    ADD category VARCHAR(16) NOT NULL
        CONSTRAINT DF_Zone_category DEFAULT 'STRICT';
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'Zone')
      AND name = N'is_active'
)
BEGIN
    ALTER TABLE Zone
    ADD is_active BIT NOT NULL
        CONSTRAINT DF_Zone_is_active DEFAULT 1;
END;

-- Backfill: flag the zone that physically holds LASER-DXF + LASER-LSR machines
-- as SHARED. Replace 'Laser' below with the actual zone.nom in your plant.
-- If no such zone exists yet, this UPDATE is a no-op.
--
-- Wrapped in EXEC() so SQL Server defers compilation: the column is added by
-- the ALTER TABLE above in the SAME batch, and SQL Server's eager-bind parser
-- would otherwise raise "Invalid column name 'category'" at parse time.
EXEC('UPDATE Zone SET category = ''SHARED'' WHERE nom = ''Laser''');
