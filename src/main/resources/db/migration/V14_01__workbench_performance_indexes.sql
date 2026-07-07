-- ============================================================================
-- V14_01__workbench_performance_indexes.sql
-- Performance indexes for /api/workbench/data hot path
-- ============================================================================
-- Problem: /api/workbench/data takes 5-15s because the cache rebuild does
-- full table scans on CuttingRequestSerie (179k rows) and CuttingRequest.
-- These indexes support the incremental cache + reduce full-rebuild time.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. CuttingRequestSerie — the heaviest table (179k rows)
-- ----------------------------------------------------------------------------

-- FK to CuttingRequest.sequence is queried in EVERY workbench load
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequestSerie_sequence' AND object_id = OBJECT_ID('CuttingRequestSerie'))
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_sequence
    ON CuttingRequestSerie (cuttingRequest_sequence);

-- statusCoupe is filtered in findActiveSeriesBySequencesLight and
-- droppable-sequence detection (Complete vs non-Complete)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequestSerie_statusCoupe' AND object_id = OBJECT_ID('CuttingRequestSerie'))
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_statusCoupe
    ON CuttingRequestSerie (statusCoupe)
    INCLUDE (cuttingRequest_sequence, dateDebutCoupe, dateFinCoupe);

-- Date columns drive incremental refresh (countChangedSince, findRelevantSequences)
-- One covering index for all four date columns + sequence for fast change detection
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequestSerie_dates' AND object_id = OBJECT_ID('CuttingRequestSerie'))
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_dates
    ON CuttingRequestSerie (dateDebutCoupe, dateFinCoupe, dateDebutMatelassage, dateFinMatelassage)
    INCLUDE (cuttingRequest_sequence, statusCoupe, statusMatelassage);

-- statusMatelassage is used to detect started sequences
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequestSerie_statusMatelassage' AND object_id = OBJECT_ID('CuttingRequestSerie'))
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_statusMatelassage
    ON CuttingRequestSerie (statusMatelassage)
    INCLUDE (cuttingRequest_sequence);

-- Combined index for the "active sequences" fast path:
-- sequence + statusCoupe covers most live-charge queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequestSerie_seq_status' AND object_id = OBJECT_ID('CuttingRequestSerie'))
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerie_seq_status
    ON CuttingRequestSerie (cuttingRequest_sequence, statusCoupe)
    INCLUDE (machine, tempsDeCoupe, nbrCouche, placement, tableCoupe, dateDebutCoupe);

-- ----------------------------------------------------------------------------
-- 2. CuttingRequest — active sequence lookup
-- ----------------------------------------------------------------------------

-- findAllActiveLight filters on sequenceStatus; dueDate+dueShift filters
-- the date/shift scoped queries.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequest_active' AND object_id = OBJECT_ID('CuttingRequest'))
CREATE NONCLUSTERED INDEX IX_CuttingRequest_active
    ON CuttingRequest (sequenceStatus, dueDate, dueShift)
    INCLUDE (dispatched_zone, zone_acceptance_status, pinned_by_chef, zone_nom);

-- Due-date lookups for Plan de Charge and engine snapshot
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequest_due' AND object_id = OBJECT_ID('CuttingRequest'))
CREATE NONCLUSTERED INDEX IX_CuttingRequest_due
    ON CuttingRequest (dueDate, dueShift)
    INCLUDE (sequence, dispatched_zone, zone_acceptance_status, pinned_by_chef);

-- ----------------------------------------------------------------------------
-- 3. Scan_Rouleau — stock availability (cached separately, but rebuild
--    still hits this table)
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Scan_Rouleau_emplacement' AND object_id = OBJECT_ID('Scan_Rouleau'))
CREATE NONCLUSTERED INDEX IX_Scan_Rouleau_emplacement
    ON Scan_Rouleau (emplacement)
    INCLUDE (reftissu, quantite, metrage, lot);

-- ----------------------------------------------------------------------------
-- 4. CuttingRequestBox — box status lookups by parent sequence
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequestBox_sequence' AND object_id = OBJECT_ID('CuttingRequestBox'))
CREATE NONCLUSTERED INDEX IX_CuttingRequestBox_sequence
    ON CuttingRequestBox (cuttingRequest_sequence)
    INCLUDE (id, partNumber, description, item, wo, woid, qtyBox, gammePrinted);
