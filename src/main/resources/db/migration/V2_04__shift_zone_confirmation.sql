-- V2_04: ShiftZoneConfirmation + ShiftZoneConfirmationMachine.
--
-- At shift start the chef-de-zone confirms "these machines are up in my zone
-- for this shift". Until that row exists, the engine refuses to schedule
-- into that zone for that shift (SchedulableSerieFilter rejects with reason
-- ALL_ZONES_CLOSED_FOR_SHIFT). This is the admission gate.
--
-- One confirmation row per (date, shift, zone); N child rows per machine.
-- is_up=0 means the chef marked a machine as down (e.g. maintenance) so the
-- engine should not feed it even if the zone itself is confirmed.

IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = N'shift_zone_confirmation'
)
BEGIN
    CREATE TABLE shift_zone_confirmation (
        id                     BIGINT IDENTITY(1,1) NOT NULL,
        date_production        DATE                 NOT NULL,
        shift_number           INT                  NOT NULL,
        zone_nom               VARCHAR(255)         NOT NULL,  -- FK to Zone.nom (varchar 255)
        confirmed_by_user_id   VARCHAR(255)         NOT NULL,  -- FK to users.matricule (varchar 255)
        confirmed_at           DATETIME2            NOT NULL CONSTRAINT DF_szc_confirmed_at DEFAULT SYSDATETIME(),
        CONSTRAINT PK_shift_zone_confirmation PRIMARY KEY (id),
        CONSTRAINT UQ_szc_date_shift_zone UNIQUE (date_production, shift_number, zone_nom),
        CONSTRAINT FK_szc_zone FOREIGN KEY (zone_nom)
            REFERENCES Zone (nom),
        CONSTRAINT FK_szc_user FOREIGN KEY (confirmed_by_user_id)
            REFERENCES users (matricule)
    );

    CREATE INDEX IX_szc_date_shift ON shift_zone_confirmation (date_production, shift_number);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = N'shift_zone_confirmation_machine'
)
BEGIN
    CREATE TABLE shift_zone_confirmation_machine (
        id               BIGINT IDENTITY(1,1) NOT NULL,
        confirmation_id  BIGINT               NOT NULL,
        machine_nom      VARCHAR(100)         NOT NULL,
        is_up            BIT                  NOT NULL CONSTRAINT DF_szcm_is_up DEFAULT 1,
        CONSTRAINT PK_shift_zone_confirmation_machine PRIMARY KEY (id),
        CONSTRAINT UQ_szcm_confirmation_machine UNIQUE (confirmation_id, machine_nom),
        CONSTRAINT FK_szcm_confirmation FOREIGN KEY (confirmation_id)
            REFERENCES shift_zone_confirmation (id) ON DELETE CASCADE
    );

    CREATE INDEX IX_szcm_confirmation_id ON shift_zone_confirmation_machine (confirmation_id);
END;
