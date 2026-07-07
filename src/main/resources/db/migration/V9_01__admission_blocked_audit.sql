-- Phase 9 — admission gate audit table.
--
-- One row per rejected admission check. The audit lets ops see why the
-- engine refused to schedule a serie into a zone, without having to
-- correlate log lines.
--
-- Retention: 7 days (Phase 11 cron).

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'admission_blocked_audit')
BEGIN
    CREATE TABLE admission_blocked_audit (
        id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
        serie_id           VARCHAR(100) NOT NULL,
        zone_nom           VARCHAR(64)  NOT NULL,
        date_production    DATE         NOT NULL,
        shift_number       INT          NOT NULL,
        reason_code        VARCHAR(64)  NOT NULL,
        reason_detail      VARCHAR(500) NULL,
        requested_by_matricule VARCHAR(50) NULL,
        created_at         DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX IX_aba_created_at
        ON admission_blocked_audit (created_at);

    CREATE INDEX IX_aba_serie_id
        ON admission_blocked_audit (serie_id, created_at);
END;
