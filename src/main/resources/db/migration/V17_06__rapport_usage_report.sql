-- ============================================================================
-- V17_06__rapport_usage_report.sql
-- Persisted snapshot of the Rapport Usage / BOM comparison: one row per
-- (cuttingRequest sequence + confirmReftissu) across ALL dates, so the report
-- can be browsed as a generic entity page (/rapportUsageReport) instead of being
-- recomputed per date/shift. Populated by RapportUsageReportService.refresh()
-- (nightly @Scheduled + manual Refresh button). qadUsage/variance are the BOM
-- comparison, computed server-side (ported from the RapportUsage page).
--
-- Columns stay camelCase to match the entity fields (no naming-strategy
-- conversion). hibernate.ddl-auto = none, so the table must be created here.
-- Catalog-safe / idempotent.
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'RapportUsageReport'))
BEGIN
    CREATE TABLE RapportUsageReport (
        id                      NVARCHAR(255) NOT NULL PRIMARY KEY,
        cuttingRequest_sequence NVARCHAR(255) NULL,
        dateDebutMatelassage    DATETIME2     NULL,
        dateFinMatelassage      DATETIME2     NULL,
        dateDebutCoupe          DATETIME2     NULL,
        dateFinCoupe            DATETIME2     NULL,
        confirmReftissu         NVARCHAR(255) NULL,
        description             NVARCHAR(MAX) NULL,
        totalConsommationPlan   FLOAT         NULL,
        overlap                 FLOAT         NULL,
        nonUtitlse              FLOAT         NULL,
        defaut                  FLOAT         NULL,
        totalUsage              FLOAT         NULL,
        excess                  FLOAT         NULL,
        finalUsage              FLOAT         NULL,
        cuttingPlanId           BIGINT        NULL,
        qadUsage                FLOAT         NULL,
        variance                FLOAT         NULL,
        statusMatelassage       NVARCHAR(255) NULL,
        lastUpdated             DATETIME2     NULL
    );

    CREATE INDEX IX_RapportUsageReport_sequence ON RapportUsageReport (cuttingRequest_sequence);
    CREATE INDEX IX_RapportUsageReport_reftissu ON RapportUsageReport (confirmReftissu);
END;
