-- V16_02__logistics_allocation.sql
-- Logistics allocation / reservation ledger.
--
-- One row per advised roll reservation. Makes rack stock TRUE: once a roll is
-- ADVISED (or RELEASED) for a target zone, its meters are deducted from rack
-- availability so two zones are never told to use the same roll. Soft/advisory
-- — it records intent, it does not lock the physical stock.
--
-- Status lifecycle: ADVISED -> RELEASED -> CONSUMED, with RETURNED / CANCELLED
-- as off-ramps. Rows in (ADVISED, RELEASED) count against availability.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'logistics_allocation')
BEGIN
    CREATE TABLE logistics_allocation (
        id              BIGINT IDENTITY PRIMARY KEY,
        sequence        VARCHAR(64)  NOT NULL,
        serie           VARCHAR(64)  NOT NULL,
        refTissus       VARCHAR(64)  NOT NULL,
        serialId        VARCHAR(64)  NOT NULL,   -- the roll
        sourceRack      VARCHAR(64)  NULL,
        sourceZone      VARCHAR(64)  NULL,
        targetZone      VARCHAR(64)  NOT NULL,
        allocatedMeters FLOAT        NOT NULL,
        status          VARCHAR(16)  NOT NULL,   -- ADVISED | RELEASED | CONSUMED | RETURNED | CANCELLED
        picklistId      VARCHAR(64)  NULL,
        createdAt       DATETIME     NOT NULL,
        createdBy       VARCHAR(64)  NULL,
        updatedAt       DATETIME     NULL
    );

    CREATE INDEX ix_logistics_allocation_serial
        ON logistics_allocation (serialId);

    CREATE INDEX ix_logistics_allocation_status
        ON logistics_allocation (status);

    CREATE INDEX ix_logistics_allocation_sequence
        ON logistics_allocation (sequence);

    CREATE INDEX ix_logistics_allocation_material_zone
        ON logistics_allocation (refTissus, targetZone);
END;
