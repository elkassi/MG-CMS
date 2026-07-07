-- Qn (Flash qualité): add "appliquerSur" — where the QN applies (Matelassage / Coupe / Les deux).
-- CMS-Prod uses it to decide whether to show the QN at the spreading (matelassage) vs cutting (coupe)
-- station; null means show everywhere. hibernate.ddl-auto = none, so the column is added here.
-- camelCase to match the entity field (no naming-strategy conversion). Idempotent / catalog-safe.

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'Qn') AND name = N'appliquerSur')
    ALTER TABLE Qn ADD appliquerSur NVARCHAR(255) NULL;
