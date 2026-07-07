-- SQL Server migration script for capacite_installee table

CREATE TABLE capacite_installee (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    date_production DATE NULL,
    shift_number INT NULL,
    groupe VARCHAR(50) NOT NULL,
    capacite_installee INT NOT NULL,
    temps_total_par_machine FLOAT NOT NULL DEFAULT 460.0,
    efficience_target FLOAT NOT NULL DEFAULT 90.0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME
);

-- Unique constraint: one entry per date/shift/groupe (NULL date/shift = default)
CREATE UNIQUE INDEX idx_capacite_unique 
    ON capacite_installee(date_production, shift_number, groupe) 
    WHERE date_production IS NOT NULL AND shift_number IS NOT NULL;

CREATE INDEX idx_capacite_date_range ON capacite_installee(date_production, shift_number);

-- Insert default entries (null date/shift = fallback defaults)
INSERT INTO capacite_installee (date_production, shift_number, groupe, capacite_installee, temps_total_par_machine, efficience_target)
VALUES (NULL, NULL, 'Coupe', 5, 460.0, 90.0);

INSERT INTO capacite_installee (date_production, shift_number, groupe, capacite_installee, temps_total_par_machine, efficience_target)
VALUES (NULL, NULL, 'Laser', 1, 460.0, 90.0);
