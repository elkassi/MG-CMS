-- CNC Control: 3-stage flow (CNC / Press / Blind) + typed machines
-- Machines gain a type so a box can pick one CNC, one Press and one Blind machine.
ALTER TABLE MachineCnc ADD [type] VARCHAR(20) NOT NULL CONSTRAINT DF_MachineCnc_type DEFAULT 'CNC';

-- Each control row belongs to a stage; existing rows are CNC controls.
ALTER TABLE CncControl ADD [stage] VARCHAR(20) NOT NULL CONSTRAINT DF_CncControl_stage DEFAULT 'CNC';

-- Per-box machine for the Press and Blind stages (CNC stage reuses machineCncId).
ALTER TABLE CncPsSession ADD machinePressId BIGINT NULL
    CONSTRAINT FK_CncPsSession_machinePress FOREIGN KEY REFERENCES MachineCnc(id);
ALTER TABLE CncPsSession ADD machineBlindId BIGINT NULL
    CONSTRAINT FK_CncPsSession_machineBlind FOREIGN KEY REFERENCES MachineCnc(id);
