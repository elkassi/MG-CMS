-- ============================================================================
-- V18_02__system_health_hot_path_indexes.sql
-- Indexes and access paths found from /systemHealth reports on 2026-06-25.
--
-- These are deliberately narrow: each index maps to a repeated query from the
-- health report or to the optimized pass-through normalization query.
-- SQL Server Standard builds nonclustered indexes offline; run in a low-traffic
-- window on production.
-- ============================================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;

-- CMS-Prod log ingestion cursor:
--   findFirstByMachineAndFileReport(...) + update by fileReport/machine
IF OBJECT_ID('CoupeMachineHistoryLoading') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'IX_CoupeMachineHistoryLoading_machine_fileReport'
          AND object_id = OBJECT_ID('CoupeMachineHistoryLoading')
   )
CREATE NONCLUSTERED INDEX IX_CoupeMachineHistoryLoading_machine_fileReport
    ON CoupeMachineHistoryLoading (machine, fileReport)
    INCLUDE (lastUpdate, lineNumber);

-- PartNumberMaterialConfig applies the same machine/length/layer interval to
-- many placements; the current clustered key starts with cuttingPlan id, so the
-- report showed repeated scans for this update.
IF OBJECT_ID('CuttingPlanMaterialPlacement') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'IX_CuttingPlanMaterialPlacement_config_update'
          AND object_id = OBJECT_ID('CuttingPlanMaterialPlacement')
   )
CREATE NONCLUSTERED INDEX IX_CuttingPlanMaterialPlacement_config_update
    ON CuttingPlanMaterialPlacement (
        cuttingPlanMaterial_partNumberMaterial,
        machine,
        longueur,
        nbrCouche
    );

-- CuttingPlanMaterial bulk update by material reference.
IF OBJECT_ID('CuttingPlanMaterial') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'IX_CuttingPlanMaterial_partNumberMaterial'
          AND object_id = OBJECT_ID('CuttingPlanMaterial')
   )
CREATE NONCLUSTERED INDEX IX_CuttingPlanMaterial_partNumberMaterial
    ON CuttingPlanMaterial (partNumberMaterial);

-- Work-order-to-sequence lookup:
--   select cuttingRequest_sequence from CuttingRequestBox where wo = ? group by ...
IF OBJECT_ID('CuttingRequestBox') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'IX_CuttingRequestBox_wo_sequence'
          AND object_id = OBJECT_ID('CuttingRequestBox')
   )
CREATE NONCLUSTERED INDEX IX_CuttingRequestBox_wo_sequence
    ON CuttingRequestBox (wo, cuttingRequest_sequence);

-- Pass-through normalization: for each In-progress serie, find the next start
-- on the same table. Existing status indexes find the current rows; these
-- timeline indexes make the "next row on this table" lookup a seek.
IF OBJECT_ID('CuttingRequestSerie') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'IX_CuttingRequestSerie_coupe_timeline'
          AND object_id = OBJECT_ID('CuttingRequestSerie')
   )
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_coupe_timeline
    ON CuttingRequestSerie (tableCoupe, dateDebutCoupe)
    INCLUDE (statusCoupe)
    WHERE tableCoupe IS NOT NULL AND dateDebutCoupe IS NOT NULL;

IF OBJECT_ID('CuttingRequestSerie') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'IX_CuttingRequestSerie_matelassage_timeline'
          AND object_id = OBJECT_ID('CuttingRequestSerie')
   )
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_matelassage_timeline
    ON CuttingRequestSerie (tableMatelassage, dateDebutMatelassage)
    INCLUDE (statusMatelassage)
    WHERE tableMatelassage IS NOT NULL AND dateDebutMatelassage IS NOT NULL;
