-- ============================================================================
-- V18_01__perf_indexes.sql
-- Index gaps that remain AFTER V14_01 + the June-2026 manual indexes.
-- ============================================================================
-- Verified 2026-06-25 against a live import of the prod DB (LEAR_CMS_V9):
-- real row counts + sys.indexes. Every CREATE here covers a query pattern that
-- currently scans, and every column was confirmed to exist. Builds are OFFLINE
-- on SQL Server Standard, so the CoupeMachineHistory one (3.67M rows, ~11s)
-- should land in the night-shift gap.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. WorkOrder (196k rows) — had ZERO non-clustered indexes.
--    /preparation and the 5-min woRapportAsprova job both scan the whole table
--    on dueDate/shift.
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_WorkOrder_due' AND object_id = OBJECT_ID('WorkOrder'))
CREATE NONCLUSTERED INDEX IX_WorkOrder_due
    ON WorkOrder (dueDate, shift)
    INCLUDE (woid, item, partNumber, qtyOpen, qtyCompleted, status, deactivated);

-- ----------------------------------------------------------------------------
-- 2. CoupeMachineHistory (3.67M rows / 635 MB) — its only index is on `ind`.
--    Every date-window read AND the archive job (WHERE lineDate < X
--    ORDER BY lineDate DESC) scan+sort all 3.67M rows. This is the index that
--    turns "read the end of the table" into a backward seek instead of a scan.
--    (The archive DELETE also filters type='PieceCut'; if that step is slow,
--     switch the key to (type, lineDate).)
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CoupeMachineHistory_lineDate' AND object_id = OBJECT_ID('CoupeMachineHistory'))
CREATE NONCLUSTERED INDEX IX_CoupeMachineHistory_lineDate
    ON CoupeMachineHistory (lineDate)
    INCLUDE (machine, type);

-- ----------------------------------------------------------------------------
-- 3. CuttingRequestSerieRouleau — roll-lifecycle lookup by roll serial
--    (only the FK index _serie exists today).
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CuttingRequestSerieRouleau_idRouleau' AND object_id = OBJECT_ID('CuttingRequestSerieRouleau'))
CREATE NONCLUSTERED INDEX IX_CuttingRequestSerieRouleau_idRouleau
    ON CuttingRequestSerieRouleau (idRouleau)
    INCLUDE (cuttingRequestSerie_serie, confirmReftissu, machine, updatedAt);

-- ----------------------------------------------------------------------------
-- 4. Per-serie lookups on tables that had no non-clustered index.
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_overlap_saving_serie' AND object_id = OBJECT_ID('overlap_saving'))
CREATE NONCLUSTERED INDEX IX_overlap_saving_serie
    ON overlap_saving (serie) INCLUDE (machine, shift);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_ScanXPL_serie' AND object_id = OBJECT_ID('ScanXPL'))
CREATE NONCLUSTERED INDEX IX_ScanXPL_serie
    ON ScanXPL (serie) INCLUDE (machine, scanDate);

-- ----------------------------------------------------------------------------
-- 5. CoupeDrill (694k) — lineDate is only an INCLUDE in IX_CoupeDrill_ind,
--    so date-range reads still scan. Add a date key.
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CoupeDrill_lineDate' AND object_id = OBJECT_ID('CoupeDrill'))
CREATE NONCLUSTERED INDEX IX_CoupeDrill_lineDate
    ON CoupeDrill (lineDate) INCLUDE (machine, ind);

-- ----------------------------------------------------------------------------
-- 6. FirstCheck (284k) — daily maintenance views by date/shift/machine,
--    no non-clustered index today.
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_FirstCheck_date' AND object_id = OBJECT_ID('FirstCheck'))
CREATE NONCLUSTERED INDEX IX_FirstCheck_date
    ON FirstCheck ([date], shift, machine);

-- ----------------------------------------------------------------------------
-- NOTE: two more indexes live in satellite databases (Flyway only manages the
-- primary DB) — apply them there by hand / via their own migration:
--   LEAR_qualite:    CREATE INDEX IX_GammeTechniqueImprimer_NSerie ON GammeTechniqueImprimer (NSerieGammeImp);
--   LEAR_MG_PLS_NEW: CREATE INDEX IX_ProdTicket_labelId ON ProdTicket (labelId) INCLUDE (reftissu, quantity, createdAt);
-- ============================================================================
