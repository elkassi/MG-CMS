-- ============================================================================
-- V17_04__capacite_installee_rule.sql
-- Interval-based default/override rules for CapaciteInstallee values.
-- CapaciteInstallee itself is unchanged (dispatcher unaffected).
-- See com.lear.MGCMS.domain.CapaciteInstalleeRule for resolution semantics.
--
-- Column names are explicit snake_case to match the @Column annotations on the
-- entity (same style as the existing capacite_installee table).
-- hibernate.ddl-auto = none, so the table must be created here.
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'capacite_installee_rule'))
BEGIN
    CREATE TABLE capacite_installee_rule (
        id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        date_debut              DATE        NULL,
        date_fin                DATE        NULL,
        day_of_week             INT         NULL,
        shift_number            INT         NULL,
        groupe                  VARCHAR(50) NULL,
        capacite_installee      INT         NULL,
        temps_total_par_machine FLOAT       NULL,
        efficience_target       FLOAT       NULL,
        created_at              DATETIME2   NULL,
        updated_at              DATETIME2   NULL
    );

    -- Lookup is by interval + groupe (day/shift filtered in Java on the small result set).
    CREATE INDEX IX_capacite_installee_rule_lookup
        ON capacite_installee_rule (groupe, date_debut, date_fin);
END;
