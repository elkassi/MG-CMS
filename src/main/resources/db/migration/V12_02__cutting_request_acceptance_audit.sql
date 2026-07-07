-- V12_02__cutting_request_acceptance_audit.sql
-- Phase A — Add per-sequence acceptance audit timestamps.
-- Plan: SEQUENCE_DISPATCHER_PLAN.md §3.2, MASTER_SCHEDULING_VISION_v3.md §4.6.

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'CuttingRequest') AND name = N'dispatchedAt')
    ALTER TABLE CuttingRequest ADD dispatchedAt DATETIME2 NULL;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'CuttingRequest') AND name = N'dispatchedBy')
    ALTER TABLE CuttingRequest ADD dispatchedBy VARCHAR(50) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'CuttingRequest') AND name = N'zoneAcceptedAt')
    ALTER TABLE CuttingRequest ADD zoneAcceptedAt DATETIME2 NULL;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'CuttingRequest') AND name = N'zoneAcceptedBy')
    ALTER TABLE CuttingRequest ADD zoneAcceptedBy VARCHAR(50) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'CuttingRequest') AND name = N'zoneRejectionReason')
    ALTER TABLE CuttingRequest ADD zoneRejectionReason NVARCHAR(512) NULL;
