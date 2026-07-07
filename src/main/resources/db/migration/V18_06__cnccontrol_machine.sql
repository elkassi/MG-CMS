-- CNC quality control: the machine is now recorded per control row (per section: CNC / PRESS / BLIND),
-- replacing the per-session machinePress/machineBlind. hibernate.ddl-auto = none, so add the column here.
-- (The legacy session machinePressId/machineBlindId columns are left in place but unused; machineCnc
-- stays for the production flow.)

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'CncControl') AND name = N'machineId')
    ALTER TABLE CncControl ADD machineId BIGINT NULL;
