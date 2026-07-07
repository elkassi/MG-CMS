-- V2_06: MachineQueue.version — optimistic-concurrency + kiosk polling cursor.
--
-- Every saveQueues() commit bumps version by +1 for each affected machine.
-- The operator kiosk polls /api/kiosk/nextSerie with its last-seen version;
-- if the server's version has advanced, the banner refreshes. This avoids
-- WebSocket churn for a high-traffic read-only path (Phase 8).
--
-- Default 0 so existing rows are well-formed without a backfill.

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'machine_queue')
      AND name = N'version'
)
BEGIN
    ALTER TABLE machine_queue
    ADD version BIGINT NOT NULL
        CONSTRAINT DF_machine_queue_version DEFAULT 0;
END;
