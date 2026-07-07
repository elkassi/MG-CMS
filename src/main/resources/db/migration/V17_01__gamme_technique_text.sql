-- V17_01__gamme_technique_text.sql
-- Extra text annotations for Gamme PN pieces.
--
-- applyToPattern = 0: annotation belongs to the exact partNumber + panelNumber.
-- applyToPattern = 1: annotation is reusable by pattern and can load for future PNs.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'GammeTechniqueText')
BEGIN
    CREATE TABLE GammeTechniqueText (
        id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        partNumber         VARCHAR(128)  NULL,
        panelNumber        VARCHAR(128)  NULL,
        partNumberMaterial VARCHAR(128)  NULL,
        pattern            VARCHAR(128)  NULL,
        content            NVARCHAR(512) NOT NULL,
        labelX             FLOAT         NULL,
        labelY             FLOAT         NULL,
        labelSize          FLOAT         NULL,
        fontFamily         VARCHAR(64)   NULL,
        fontWeight         VARCHAR(32)   NULL,
        fontStyle          VARCHAR(32)   NULL,
        fillColor          VARCHAR(32)   NULL,
        rotation           INT           NULL,
        applyToPattern     BIT           NOT NULL CONSTRAINT df_GammeTechniqueText_applyToPattern DEFAULT 0,
        createdBy          VARCHAR(64)   NULL,
        createdAt          DATETIME2     NULL,
        updatedBy          VARCHAR(64)   NULL,
        updatedAt          DATETIME2     NULL
    );

    CREATE INDEX ix_GammeTechniqueText_partNumber
        ON GammeTechniqueText (partNumber, applyToPattern);

    CREATE INDEX ix_GammeTechniqueText_pattern
        ON GammeTechniqueText (pattern, applyToPattern);

    CREATE INDEX ix_GammeTechniqueText_panelNumber
        ON GammeTechniqueText (panelNumber);
END;
