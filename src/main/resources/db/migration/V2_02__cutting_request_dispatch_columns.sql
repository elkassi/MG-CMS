-- V2_02: CuttingRequest dispatcher output columns.
--
-- Dispatcher publishes its proposed zone into CuttingRequest.dispatched_zone.
-- zone_acceptance_status tracks the chef-de-zone confirmation flow:
--     PENDING   — dispatcher proposed, no confirmation yet
--     ACCEPTED  — chef confirmed the shift for this zone
--     REJECTED  — chef explicitly declined (rare; usually they re-dispatch)
-- pinned_by_chef: a chef-de-zone can pin a specific serie to a specific
-- queue position; the engine must never reshuffle a pinned row. See Phase 7.
--
-- All three columns are NULL-able / zero-default so rolling back is cheap:
-- drop the FK nowhere-referenced columns later if we abandon the feature.

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'CuttingRequest')
      AND name = N'dispatched_zone'
)
BEGIN
    ALTER TABLE CuttingRequest
    ADD dispatched_zone VARCHAR(64) NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'CuttingRequest')
      AND name = N'zone_acceptance_status'
)
BEGIN
    ALTER TABLE CuttingRequest
    ADD zone_acceptance_status VARCHAR(16) NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'CuttingRequest')
      AND name = N'pinned_by_chef'
)
BEGIN
    ALTER TABLE CuttingRequest
    ADD pinned_by_chef BIT NOT NULL
        CONSTRAINT DF_CuttingRequest_pinned_by_chef DEFAULT 0;
END;
