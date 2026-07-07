UPDATE [dbo].[users] SET password = '$2a$10$qV5tx/sZKkEEiQKMM8bYwOH7FEKDUr1LVs0cAIoMk2z2ArxnezSWK', active = 1 WHERE username = 'testChef';
SELECT username, password, active FROM [dbo].[users] WHERE username = 'testChef';
