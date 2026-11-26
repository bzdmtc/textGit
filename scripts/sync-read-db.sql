-- 开发环境：把主库完整同步到只读从库（备份 + 还原）
-- 执行前先停止应用，避免从库连接占用。
-- 生产环境请改用 SQL Server Always On 只读副本或事务复制。

DECLARE @backupPath NVARCHAR(400) = N'C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\Backup\smart_learning_sync.bak';

BACKUP DATABASE smart_learning
TO DISK = @backupPath
WITH INIT, FORMAT;

RESTORE DATABASE smart_learning_read
FROM DISK = @backupPath
WITH REPLACE,
     MOVE N'smart_learning' TO N'C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\DATA\smart_learning_read.mdf',
     MOVE N'smart_learning_log' TO N'C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\DATA\smart_learning_read_log.ldf';

-- 还原会替换数据库用户，重新授权只读账号
USE smart_learning_read;
-- 从库不应保留主库写账号
IF EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'smart_learning_app')
BEGIN
    ALTER ROLE db_owner DROP MEMBER smart_learning_app;
    DROP USER smart_learning_app;
END
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'smart_learning_read')
    CREATE USER smart_learning_read FOR LOGIN smart_learning_read;
ALTER ROLE db_datareader ADD MEMBER smart_learning_read;
GO
