-- V12_01__dispatch_audit.sql
-- Phase A — Dispatch audit trail for every routing change.
-- Plan: MASTER_SCHEDULING_VISION_v3.md §4.6.

CREATE TABLE dispatch_audit (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    sequence      VARCHAR(64)   NOT NULL,
    from_zone     VARCHAR(64)   NULL,
    to_zone       VARCHAR(64)   NULL,
    reason        NVARCHAR(512) NULL,
    trigger_code  VARCHAR(32)   NOT NULL,
    matricule     VARCHAR(32)   NULL,
    created_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX IX_dispatch_audit_seq ON dispatch_audit (sequence, created_at DESC);
CREATE INDEX IX_dispatch_audit_at  ON dispatch_audit (created_at DESC);
CREATE INDEX IX_dispatch_audit_trg ON dispatch_audit (trigger_code, created_at DESC);
