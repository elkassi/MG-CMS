-- V2_05: UnassignableSerie — audit log of series the dispatcher couldn't place.
--
-- Written by SchedulableSerieFilter whenever a serie is rejected. Phase 4's
-- Process page shows these as "needs manual intervention". A 7-day retention
-- cron prunes old rows (see Phase 11).
--
-- reason_code is one of:
--     NO_ZONE_ACCEPTING_TYPE      — no STRICT zone and no SHARED zone cover
--                                   this machineType at all (misconfig).
--     ALL_ZONES_CLOSED_FOR_SHIFT  — zones exist, but no ShiftZoneConfirmation
--                                   has been written for this (date, shift).
--     NO_ACTIVE_MACHINE_IN_ZONE   — zone confirmed but every machine is_up=0.
--     OTHER                       — free-form fallback; see reason_detail.

IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = N'unassignable_serie'
)
BEGIN
    CREATE TABLE unassignable_serie (
        id             BIGINT IDENTITY(1,1) NOT NULL,
        serie_id       VARCHAR(100)         NOT NULL,
        reason_code    VARCHAR(64)          NOT NULL,
        reason_detail  VARCHAR(500)         NULL,
        created_at     DATETIME2            NOT NULL CONSTRAINT DF_unassignable_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT PK_unassignable_serie PRIMARY KEY (id)
    );

    CREATE INDEX IX_unassignable_serie_id_created
        ON unassignable_serie (serie_id, created_at DESC);
    CREATE INDEX IX_unassignable_created_at
        ON unassignable_serie (created_at);
END;
