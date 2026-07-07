-- Scrap voucher number + replacement-status lifecycle on a scrapped control row (both optional).
-- scrapStatus codes: EN_ATTENTE_VALIDATION, EN_ATTENTE_MATIERE, REMPLACE, NON_REMPLACABLE (NULL = pending).
ALTER TABLE CncControl ADD numBonScrap VARCHAR(255) NULL;
ALTER TABLE CncControl ADD scrapStatus VARCHAR(30) NULL;

-- Widen qualiteStatus to hold the incomplete form, e.g. 'Terminé (-3 pièces)'.
ALTER TABLE CncPsSession ALTER COLUMN qualiteStatus VARCHAR(40) NULL;
