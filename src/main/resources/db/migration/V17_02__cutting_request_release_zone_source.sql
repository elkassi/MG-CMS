-- ============================================================================
-- V17_02__cutting_request_release_zone_source.sql
-- Track WHO set CuttingRequest.releaseZone so the zone auto-correction job
-- knows which zones it may overwrite.
-- ============================================================================
-- Values (see com.lear.MGCMS.domain.CuttingRequest.ReleaseZoneSource):
--   'LOGISTICS' — set by the /logisticsRelease picklist release; LOCKED, the
--                 auto-correction job never touches it.
--   'CHEF'      — set manually by a chef (floor "pas ma zone" / rectification);
--                 LOCKED against auto-correction.
--   'AUTO'      — inferred by SequenceZoneAutoCorrectService from the STRICT
--                 zone of the table that worked the sequence's last serie;
--                 may be re-inferred on every pass.
--   NULL        — legacy/unknown writer (pre-feature rows); treated like AUTO,
--                 i.e. the job may correct it. Deliberately NOT back-filled:
--                 the picklist flow is not in real use yet, so existing zones
--                 are guesses that SHOULD be auto-corrected.
--
-- Column stays camelCase to match the entity field (no naming-strategy
-- conversion in this project).
-- ============================================================================

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'CuttingRequest')
      AND name = N'releaseZoneSource'
)
BEGIN
    ALTER TABLE CuttingRequest
    ADD releaseZoneSource VARCHAR(16) NULL;
END;
