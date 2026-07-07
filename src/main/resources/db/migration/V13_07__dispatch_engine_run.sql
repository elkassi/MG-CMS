-- V13_07__dispatch_engine_run.sql
-- Phase B — Continuous Dispatch Optimizer tables.

CREATE TABLE dispatch_engine_run (
    id              BIGINT IDENTITY PRIMARY KEY,
    started_at      DATETIME2 NOT NULL,
    ended_at        DATETIME2 NULL,
    mode            VARCHAR(32) NOT NULL,
    duration_sec    INT NULL,
    started_by      VARCHAR(50) NOT NULL,
    final_state     VARCHAR(32) NOT NULL,
    iterations      INT NOT NULL DEFAULT 0,
    improvements    INT NOT NULL DEFAULT 0,
    initial_spread  DECIMAL(6,2) NULL,
    final_spread    DECIMAL(6,2) NULL,
    notes           NVARCHAR(512) NULL
);

CREATE TABLE dispatch_engine_run_suggestion (
    run_id          BIGINT NOT NULL,
    sequence        VARCHAR(64) NOT NULL,
    suggested_zone  VARCHAR(64) NOT NULL,
    previous_zone   VARCHAR(64) NULL,
    PRIMARY KEY (run_id, sequence)
);

CREATE TABLE dispatch_engine_indicator_sample (
    run_id          BIGINT NOT NULL,
    sample_at       DATETIME2 NOT NULL,
    iteration       INT NOT NULL,
    spread_pct      DECIMAL(6,2) NOT NULL,
    max_load_pct    DECIMAL(6,2) NOT NULL,
    min_load_pct    DECIMAL(6,2) NOT NULL,
    accepted        BIT NOT NULL DEFAULT 0,
    PRIMARY KEY (run_id, iteration)
);
