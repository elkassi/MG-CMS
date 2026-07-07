-- V15_01__engine_schedule_entry.sql
-- Option C — Full Scheduling.
-- Persistence layer for the engine's per-serie planned slot. One row per
-- (serie, phase): the engine produces both MATELASSAGE and COUPE phase
-- entries for each serie. Replaced every snapshot rebuild — we keep a
-- single "current best" snapshot, not a history.

CREATE TABLE engine_schedule_entry (
    serie_id        VARCHAR(64)  NOT NULL,
    phase           VARCHAR(16)  NOT NULL,   -- MATELASSAGE | COUPE
    machine_nom     VARCHAR(64)  NULL,        -- null when unassigned in this snapshot
    sequence_id     VARCHAR(64)  NOT NULL,
    zone_nom        VARCHAR(64)  NULL,
    planned_start   DATETIME2    NULL,
    planned_end     DATETIME2    NULL,
    run_id          BIGINT       NULL,
    planned_at      DATETIME2    NOT NULL,
    CONSTRAINT pk_engine_schedule_entry PRIMARY KEY (serie_id, phase)
);

CREATE INDEX ix_engine_schedule_entry_machine
    ON engine_schedule_entry (machine_nom, planned_start);

CREATE INDEX ix_engine_schedule_entry_sequence
    ON engine_schedule_entry (sequence_id);

CREATE INDEX ix_engine_schedule_entry_run
    ON engine_schedule_entry (run_id);
