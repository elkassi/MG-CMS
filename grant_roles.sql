DECLARE @UserMatricule VARCHAR(255);
SELECT @UserMatricule = matricule FROM [dbo].[users] WHERE username = 'testChef';

IF @UserMatricule IS NOT NULL
BEGIN
    -- Remove existing roles just to be safe
    DELETE FROM [dbo].[users_roles] WHERE user_matricule = @UserMatricule;

    -- Insert all available roles for this user
    INSERT INTO [dbo].[users_roles] (user_matricule, role_id)
    SELECT @UserMatricule, id FROM [dbo].[roles];
END

-- Verify the inserted roles
SELECT r.name as RoleName, r.description as RoleDescription
FROM [dbo].[users_roles] ur
JOIN [dbo].[roles] r ON ur.role_id = r.id
WHERE ur.user_matricule = @UserMatricule;
