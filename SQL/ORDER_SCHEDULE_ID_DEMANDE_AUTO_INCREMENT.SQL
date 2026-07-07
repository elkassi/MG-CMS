-- SQL Server migration for auto-generated Order_Schedule.ID_Demande values.
-- This uses a sequence plus a default constraint instead of converting the column
-- to IDENTITY, because the application still performs explicit ID inserts in some flows.

SET NOCOUNT ON;

IF OBJECT_ID('dbo.Order_Schedule', 'U') IS NULL
BEGIN
    RAISERROR('dbo.Order_Schedule table was not found.', 16, 1);
    RETURN;
END;

DECLARE @maxId BIGINT = ISNULL((SELECT MAX(ID_Demande) FROM dbo.Order_Schedule), 0);
DECLARE @nextId BIGINT = @maxId + 1;

IF OBJECT_ID('dbo.seq_Order_Schedule_ID_Demande', 'SO') IS NULL
BEGIN
    EXEC(
        'CREATE SEQUENCE dbo.seq_Order_Schedule_ID_Demande '
        + 'AS BIGINT '
        + 'START WITH ' + CAST(@nextId AS VARCHAR(30)) + ' '
        + 'INCREMENT BY 1;'
    );
END;
ELSE
BEGIN
    DECLARE @sequenceCurrent BIGINT;

    SELECT @sequenceCurrent = CAST(current_value AS BIGINT)
    FROM sys.sequences
    WHERE name = 'seq_Order_Schedule_ID_Demande'
      AND schema_id = SCHEMA_ID('dbo');

    IF @sequenceCurrent IS NULL OR @sequenceCurrent < @maxId
    BEGIN
        EXEC(
            'ALTER SEQUENCE dbo.seq_Order_Schedule_ID_Demande '
            + 'RESTART WITH ' + CAST(@nextId AS VARCHAR(30)) + ';'
        );
    END;
END;

DECLARE @defaultConstraintName SYSNAME;

SELECT @defaultConstraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
JOIN sys.tables t ON t.object_id = c.object_id
JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE s.name = 'dbo'
  AND t.name = 'Order_Schedule'
  AND c.name = 'ID_Demande';

IF @defaultConstraintName IS NULL
BEGIN
    ALTER TABLE dbo.Order_Schedule
        ADD CONSTRAINT DF_Order_Schedule_ID_Demande
        DEFAULT (NEXT VALUE FOR dbo.seq_Order_Schedule_ID_Demande) FOR ID_Demande;
END;
ELSE
BEGIN
    PRINT 'Existing default constraint kept on dbo.Order_Schedule.ID_Demande: ' + @defaultConstraintName;
END;

PRINT 'Order_Schedule.ID_Demande auto-generation is ready.';