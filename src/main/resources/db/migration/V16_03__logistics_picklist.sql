-- V16_03__logistics_picklist.sql
-- Stable printable snapshot for logistics release picklists.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'logistics_picklist')
BEGIN
    CREATE TABLE logistics_picklist (
        id            VARCHAR(64)   NOT NULL PRIMARY KEY,
        releaseDate   DATE          NULL,
        shift         INT           NULL,
        sequenceCount INT           NULL,
        createdAt     DATETIME      NOT NULL,
        createdBy     VARCHAR(64)   NULL,
        snapshotJson  NVARCHAR(MAX) NULL
    );

    CREATE INDEX ix_logistics_picklist_date_shift
        ON logistics_picklist (releaseDate, shift);
END;
