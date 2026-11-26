-- 建库与应用账号（在 SQL Server 主实例执行一次）
-- 默认实例 localhost,1433，Windows 身份验证
-- 用法（密码通过 sqlcmd 变量传入，不写入脚本）：
--   sqlcmd -S localhost -E -C -i scripts/init-databases.sql \
--     -v MASTER_PASSWORD="<你的主库强密码>" READ_PASSWORD="<你的从库强密码>"
-- 应用侧在 application-local.yml 或环境变量中配置相同密码。

IF DB_ID('smart_learning') IS NULL CREATE DATABASE smart_learning;
IF DB_ID('smart_learning_read') IS NULL CREATE DATABASE smart_learning_read;
GO

IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = 'smart_learning_app')
    CREATE LOGIN smart_learning_app WITH PASSWORD = '$(MASTER_PASSWORD)', CHECK_POLICY = OFF;
IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = 'smart_learning_read')
    CREATE LOGIN smart_learning_read WITH PASSWORD = '$(READ_PASSWORD)', CHECK_POLICY = OFF;
GO

USE smart_learning;
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'smart_learning_app')
    CREATE USER smart_learning_app FOR LOGIN smart_learning_app;
ALTER ROLE db_owner ADD MEMBER smart_learning_app;
GO

USE smart_learning_read;
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'smart_learning_read')
    CREATE USER smart_learning_read FOR LOGIN smart_learning_read;
ALTER ROLE db_datareader ADD MEMBER smart_learning_read;
GO
