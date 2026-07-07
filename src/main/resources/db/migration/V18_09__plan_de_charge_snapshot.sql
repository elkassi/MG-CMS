-- Plan de Charge: per-shift snapshot of the "Détails de charge" aggregates.
-- Old shifts (before the current shift) are served from this snapshot instead of
-- recomputing series + cut files, so the page stays fast. The payload is the JSON
-- the frontend builds (chargeSummary + detailedSeries + partNumberReport +
-- nonImportedCharge), mirroring the logistics_picklist.snapshotJson precedent.
-- hibernate.ddl-auto = none, so the table is created here. Idempotent / catalog-safe.

IF OBJECT_ID(N'plan_de_charge_snapshot', N'U') IS NULL
BEGIN
    CREATE TABLE plan_de_charge_snapshot (
        id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        shift_date    DATE          NOT NULL,
        shift_number  INT           NOT NULL,
        snapshot_json NVARCHAR(MAX) NULL,
        created_at    DATETIME2     NULL,
        updated_at    DATETIME2     NULL,
        created_by    NVARCHAR(100) NULL,
        CONSTRAINT UQ_plan_de_charge_snapshot UNIQUE (shift_date, shift_number)
    );
END;
