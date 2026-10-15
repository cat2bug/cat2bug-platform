-- Cat2Bug 1.1.0 迁移 #42: mcp_oauth_project_binding
-- 删除个人访问令牌；OAuth 授权码与令牌绑定项目，存量未绑定项目的令牌与授权码作废
SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_oauth_code' AND COLUMN_NAME = 'project_id'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_oauth_code ADD COLUMN project_id BIGINT DEFAULT NULL COMMENT ''授权时选择的项目''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user_access_token' AND COLUMN_NAME = 'project_id'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_user_access_token ADD COLUMN project_id BIGINT DEFAULT NULL COMMENT ''令牌绑定的项目''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

DELETE FROM sys_user_access_token WHERE source = 'manual' OR project_id IS NULL;
DELETE FROM sys_oauth_code WHERE project_id IS NULL;
