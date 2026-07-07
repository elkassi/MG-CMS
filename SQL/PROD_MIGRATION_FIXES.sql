-- ============================================================================
-- PROD_MIGRATION_FIXES.sql
-- Target DB: LEAR_MG_CMS_Prod (and any sibling prod server)
-- Generated: 2026-05-04 by audit run
-- ============================================================================
--
-- WHAT THIS SCRIPT DOES
-- ---------------------
-- This is a one-shot, idempotent cleanup + seed script you run on a prod DB
-- AFTER the Spring Boot app has booted at least once (so Flyway V2_01..V9_01
-- and Hibernate hbm2ddl=update have already created the dispatcher tables and
-- columns). It:
--   1. Sanity-checks that the migrations actually landed.
--   2. Cleans the data quirks the audit found in LEAR_MG_CMS_Prod that would
--      block PdC / Dispatcher / Ordonnancement.
--   3. Seeds the minimum data the dispatcher needs to be testable for a shift.
--   4. Gives you the OPTIONAL deeper-fix queries (commented out) for the
--      historical lifecycle bug — those will mass-update tens of thousands of
--      rows so you should review them and back up first.
--
-- HOW TO RUN
-- ----------
--   sqlcmd -S "<server>\<instance>" -U <user> -P <pwd> ^
--          -d <DB> -i PROD_MIGRATION_FIXES.sql
--
-- Every block is wrapped in IF EXISTS / NOT EXISTS / WHERE NOT EXISTS so it is
-- safe to re-run. Each block prints a one-line status with PRINT.
--
-- BEFORE YOU RUN
-- --------------
--   * Take a backup. The deep-fix block in §6 touches > 35,000 rows.
--   * Decide which zone is the SHARED laser zone in your plant. On
--     LEAR_MG_CMS_Prod it is zone "C" (3 LASER-DXF machines C1, C2, C3).
--     If your other server has a different zone holding the laser machines,
--     edit §3 below before running.
--   * Identify a chef-de-zone matricule. On LEAR_MG_CMS_Prod use a row from
--     users where roles include ROLE_CHEF_DE_ZONE; the chef will be the one
--     who confirms shifts. Edit §5 with the right matricule.
-- ============================================================================


-- ============================================================================
-- §1. SANITY CHECK: are the dispatcher migrations on this DB?
-- ============================================================================
PRINT N'--- §1 sanity check ---';

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'flyway_schema_history')
BEGIN
    RAISERROR(N'flyway_schema_history is missing. Boot the Spring app first so Flyway can baseline + apply V2_01..V9_01. Aborting.', 16, 1);
    RETURN;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'Zone') AND name = N'category')
BEGIN
    RAISERROR(N'Zone.category missing. Phase 2 migrations did not land. Boot the Spring app first.', 16, 1);
    RETURN;
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'user_zone')
BEGIN
    RAISERROR(N'user_zone table missing. Phase 2 migrations did not land. Boot the Spring app first.', 16, 1);
    RETURN;
END;

PRINT N'§1 OK — dispatcher schema present.';


-- ============================================================================
-- §2. CLEANUP: capacite_installee NULL-key rows (causes startup
--     NonUniqueResultException in PlanDeChargeService.ensureCapaciteInstallee)
-- ============================================================================
PRINT N'--- §2 capacite_installee cleanup ---';

DECLARE @null_capa INT = (SELECT COUNT(*) FROM capacite_installee WHERE date_production IS NULL);
PRINT N'Rows with NULL date_production: ' + CAST(@null_capa AS NVARCHAR(10));

DELETE FROM capacite_installee WHERE date_production IS NULL;

PRINT N'§2 OK — NULL-keyed capacite_installee rows removed.';


-- ============================================================================
-- §3. ZONE CATEGORIES: flag the SHARED laser zone
-- ============================================================================
-- The dispatcher's SHARED zone is the one that holds the LASER-DXF + LASER-LSR
-- machines and acts as overflow when a STRICT zone needs laser work. On
-- LEAR_MG_CMS_Prod the only zone with LASER machines is zone "C" (C1, C2, C3).
-- Confirm this before running on a different server.
-- ============================================================================
PRINT N'--- §3 mark SHARED zone ---';

DECLARE @shared_zone NVARCHAR(255) = N'C';  -- <-- edit per server

IF NOT EXISTS (SELECT 1 FROM Zone WHERE nom = @shared_zone)
BEGIN
    PRINT N'§3 SKIP — Zone "' + @shared_zone + N'" not found. Edit @shared_zone.';
END
ELSE
BEGIN
    UPDATE Zone SET category = N'SHARED' WHERE nom = @shared_zone AND category <> N'SHARED';
    PRINT N'§3 OK — Zone "' + @shared_zone + N'" set to SHARED.';
END;


-- ============================================================================
-- §4. CAPACITY SEED: ensure capacite_installee covers the shifts you'll run
-- ============================================================================
-- PlanDeChargeService auto-provisions next-2-days, but never seeds a date
-- earlier than today. If you need to re-run PdC for a historical date (the
-- 2026-04-24 simulation case) or the next 14 days, you need rows for every
-- (date, shift, groupe) triple. This block fills the next 14 days for shifts
-- 1/2/3 × {Coupe, Laser}; tweak the loop bounds as needed.
-- ============================================================================
PRINT N'--- §4 capacite_installee seed (next 14 days) ---';

DECLARE @start DATE = CAST(GETDATE() AS DATE);
DECLARE @end   DATE = DATEADD(DAY, 14, @start);
DECLARE @d     DATE = @start;

WHILE @d <= @end
BEGIN
    INSERT INTO capacite_installee (date_production, shift_number, groupe, capacite_installee, temps_total_par_machine, efficience_target, created_at)
    SELECT @d, s.shift, g.grp, 1, 460.0, 90.0, SYSDATETIME()
    FROM (VALUES (1),(2),(3)) s(shift)
    CROSS JOIN (VALUES (N'Coupe'),(N'Laser')) g(grp)
    WHERE NOT EXISTS (
        SELECT 1 FROM capacite_installee
        WHERE date_production = @d AND shift_number = s.shift AND groupe = g.grp);

    SET @d = DATEADD(DAY, 1, @d);
END;

PRINT N'§4 OK — capacite_installee seeded for next 14 days.';


-- ============================================================================
-- §5. USER → ZONE LINKS: wire chefs-de-zone so the Chef-de-Zone page renders
-- ============================================================================
-- The Chef-de-Zone page (Phase 5) only shows zones the user has a UserZone
-- row for. ROLE_CHEF_EQUIPE sees all active zones automatically; everyone
-- else needs an explicit row. Edit @chef_matricule to a real chef account.
--
-- This block creates one default link per active zone for a single chef — use
-- it as a smoke-test seed only. Real production assignment should go through
-- /userZoneAdmin once you've enabled mgcms.dispatcher.enabled=true.
-- ============================================================================
PRINT N'--- §5 chef-de-zone seed ---';

DECLARE @chef_matricule NVARCHAR(255) = N'<EDIT ME>';  -- <-- e.g. N'13325'

IF @chef_matricule = N'<EDIT ME>'
BEGIN
    PRINT N'§5 SKIP — set @chef_matricule to a real users.matricule with ROLE_CHEF_DE_ZONE.';
END
ELSE IF NOT EXISTS (SELECT 1 FROM users WHERE matricule = @chef_matricule)
BEGIN
    PRINT N'§5 SKIP — matricule "' + @chef_matricule + N'" not found in users.';
END
ELSE
BEGIN
    -- Default link to the first active zone alphabetically; chef can pick the
    -- real default from the UI later.
    INSERT INTO user_zone (user_id, zone_nom, is_default, assigned_by, assigned_at)
    SELECT TOP 1 @chef_matricule, z.nom, 1, @chef_matricule, SYSDATETIME()
    FROM Zone z
    WHERE z.is_active = 1
      AND NOT EXISTS (SELECT 1 FROM user_zone uz WHERE uz.user_id = @chef_matricule AND uz.zone_nom = z.nom)
    ORDER BY z.nom;

    PRINT N'§5 OK — default UserZone link inserted for chef ' + @chef_matricule;
END;


-- ============================================================================
-- §6. OPTIONAL — DEEP HISTORICAL FIX: clear the "phantom in-progress" series
-- ============================================================================
-- The audit found 35,600 conflict pairs from CuttingRequestSerie rows whose
-- statusCoupe was never moved out of 'In progress' after dateFinCoupe was
-- written. Without fixing this the dispatcher's SerieZoneResolver thinks
-- those machines are still busy and rejects any new series for them.
--
-- Two strategies — UNCOMMENT ONE after backing up:
--
-- (a) Pure heuristic: if dateFinCoupe is set and not before dateDebutCoupe,
--     mark the row Complete. Safest — only touches rows that look done.
--
-- UPDATE CuttingRequestSerie
--   SET statusCoupe = N'Complete'
-- WHERE statusCoupe = N'In progress'
--   AND dateDebutCoupe IS NOT NULL
--   AND dateFinCoupe IS NOT NULL
--   AND dateFinCoupe >= dateDebutCoupe;
--
-- (b) Aggressive: any 'In progress' row older than 7 days is stale, mark it
--     Complete (or Cancelled if you prefer manual triage).
--
-- UPDATE CuttingRequestSerie
--   SET statusCoupe = N'Complete'
-- WHERE statusCoupe = N'In progress'
--   AND DATEDIFF(DAY, ISNULL(dateDebutCoupe, createdAt), GETDATE()) > 7;
--
-- Same problem and same two strategies apply to statusMatelassage:
--
-- UPDATE CuttingRequestSerie
--   SET statusMatelassage = N'Complete'
-- WHERE statusMatelassage = N'In progress'
--   AND dateDebutMatelassage IS NOT NULL
--   AND dateFinMatelassage IS NOT NULL
--   AND dateFinMatelassage >= dateDebutMatelassage;
--
-- And the 281 rows with dateFinCoupe set but dateDebutCoupe NULL — these are
-- corrupted and should be triaged by hand. Sample in §A4 of the audit report.
--
-- And the 79 rows with dateFinCoupe < dateDebutCoupe — also manual triage.
-- See §A5. One row (2025265716) differs only in sub-second precision; that
-- is a Hibernate-vs-driver truncation artefact and can be auto-fixed:
--
-- UPDATE CuttingRequestSerie
--   SET dateFinCoupe = dateDebutCoupe
-- WHERE dateFinCoupe < dateDebutCoupe
--   AND DATEDIFF(SECOND, dateFinCoupe, dateDebutCoupe) <= 1;
PRINT N'§6 SKIP — deep historical fixes left commented out. Read the comments and uncomment the strategy you want.';


-- ============================================================================
-- §7. OPTIONAL — fix the 1 row with machine='LASER' (typo, no MachineType)
-- ============================================================================
-- Audit B1 found one CuttingRequestSerie with machine='LASER' which doesn't
-- match any MachineType.name. Likely meant LASER-DXF. Uncomment after
-- inspecting the row to choose the right replacement value.
--
-- SELECT serie, machine, partNumberMaterial, planningDate
--   FROM CuttingRequestSerie WHERE machine = N'LASER';
--
-- UPDATE CuttingRequestSerie SET machine = N'LASER-DXF' WHERE machine = N'LASER';
PRINT N'§7 SKIP — review the LASER row first, then uncomment.';


-- ============================================================================
-- §8. SANITY: confirm dispatcher tables are reachable
-- ============================================================================
PRINT N'--- §8 final sanity ---';

SELECT name FROM sys.tables WHERE name IN (
    N'user_zone',
    N'shift_zone_confirmation',
    N'shift_zone_confirmation_machine',
    N'unassignable_serie',
    N'admission_blocked_audit',
    N'machine_queue'
) ORDER BY name;

SELECT name FROM sys.columns WHERE object_id = OBJECT_ID(N'Zone') AND name IN (N'category', N'is_active');
SELECT name FROM sys.columns WHERE object_id = OBJECT_ID(N'CuttingRequest') AND name IN (N'dispatched_zone', N'zone_acceptance_status', N'pinned_by_chef');
SELECT name FROM sys.columns WHERE object_id = OBJECT_ID(N'machine_queue') AND name = N'version';

SELECT category, COUNT(*) AS n FROM Zone GROUP BY category;
SELECT date_production, COUNT(*) AS rows_n
  FROM capacite_installee
 WHERE date_production BETWEEN CAST(GETDATE() AS DATE) AND DATEADD(DAY, 14, CAST(GETDATE() AS DATE))
 GROUP BY date_production ORDER BY date_production;

PRINT N'§8 OK — verify the SELECT outputs above match what you expect.';

PRINT N'';
PRINT N'PROD_MIGRATION_FIXES.sql complete.';
