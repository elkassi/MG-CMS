-- Run these migrations before deploying

CREATE TABLE etat_machine_historique (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine VARCHAR(50) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NULL,
    code_etat VARCHAR(10) NOT NULL,
    cause VARCHAR(500) NULL,
    action VARCHAR(500) NULL,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT GETDATE(),
    updated_by VARCHAR(100),
    updated_at DATETIME,
    closed_by VARCHAR(100),
    closed_at DATETIME
);

CREATE INDEX idx_etat_machine_dates ON etat_machine_historique(machine, start_date, end_date);

CREATE TABLE shift_load_calculation (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shift_date DATE NOT NULL,
    shift_number INT NOT NULL,
    machine_type VARCHAR(50),
    total_planned_time DECIMAL(10,2),
    total_actual_time DECIMAL(10,2),
    available_time DECIMAL(10,2),
    load_percentage DECIMAL(5,2),
    efficiency_percentage DECIMAL(5,2),
    carryover_time DECIMAL(10,2),
    machines_count INT,
    calculated_at DATETIME,
    calculated_by VARCHAR(100),
    CONSTRAINT UK_shift_calculation UNIQUE (shift_date, shift_number, machine_type)
);