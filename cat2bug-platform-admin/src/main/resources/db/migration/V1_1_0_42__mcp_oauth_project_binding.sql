-- Cat2Bug 1.1.0 迁移 #42: mcp_oauth_project_binding
-- 删除个人访问令牌；OAuth 授权码与令牌绑定项目，存量未绑定项目的令牌与授权码作废
ALTER TABLE sys_oauth_code ADD COLUMN IF NOT EXISTS project_id BIGINT;
ALTER TABLE sys_user_access_token ADD COLUMN IF NOT EXISTS project_id BIGINT;

DELETE FROM sys_user_access_token WHERE source = 'manual' OR project_id IS NULL;
DELETE FROM sys_oauth_code WHERE project_id IS NULL;
