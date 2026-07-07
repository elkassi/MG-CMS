-- CNC quality-control tracking on the session + programme on a control row.
-- Status lifecycle: NULL (not started) -> 'En cours' (some controls) -> 'Terminé' (all stages controlled).
ALTER TABLE CncPsSession ADD qualiteStatus VARCHAR(20) NULL;
ALTER TABLE CncPsSession ADD userQualite VARCHAR(255) NULL;
ALTER TABLE CncPsSession ADD startDateControl DATETIME2 NULL;
ALTER TABLE CncPsSession ADD endDateControl DATETIME2 NULL;

-- Programme CNC chosen when a NOK control is recorded (defect attribution / traceability).
ALTER TABLE CncControl ADD programNumber VARCHAR(255) NULL;
