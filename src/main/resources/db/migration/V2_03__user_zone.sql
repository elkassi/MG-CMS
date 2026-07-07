-- V2_03: UserZone — many-to-many between users (chefs-de-zone) and Zone.
--
-- A chef can own multiple zones; is_default marks the zone they land on
-- when they open the Chef-de-Zone page. revoked_at soft-deletes the link
-- without losing the historical audit trail.

IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = N'user_zone'
)
BEGIN
    -- users.matricule and Zone.nom are VARCHAR(255) in this DB; FK columns
    -- must match the referenced column length exactly (SQL Server err 1753).
    CREATE TABLE user_zone (
        id             BIGINT IDENTITY(1,1) NOT NULL,
        user_id        VARCHAR(255)         NOT NULL,  -- users.matricule
        zone_nom       VARCHAR(255)         NOT NULL,  -- Zone.nom
        is_default     BIT                  NOT NULL CONSTRAINT DF_user_zone_is_default DEFAULT 0,
        assigned_by    VARCHAR(255)         NULL,      -- users.matricule of admin
        assigned_at    DATETIME2            NOT NULL CONSTRAINT DF_user_zone_assigned_at DEFAULT SYSDATETIME(),
        revoked_at     DATETIME2            NULL,
        CONSTRAINT PK_user_zone PRIMARY KEY (id),
        CONSTRAINT UQ_user_zone_user_zone UNIQUE (user_id, zone_nom),
        CONSTRAINT FK_user_zone_user FOREIGN KEY (user_id)
            REFERENCES users (matricule),
        CONSTRAINT FK_user_zone_zone FOREIGN KEY (zone_nom)
            REFERENCES Zone (nom)
    );

    CREATE INDEX IX_user_zone_user_id ON user_zone (user_id);
    CREATE INDEX IX_user_zone_zone_nom ON user_zone (zone_nom);
END;
