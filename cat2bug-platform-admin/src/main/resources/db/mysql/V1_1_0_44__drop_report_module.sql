-- Cat2Bug 1.1.0 迁移 #44: 移除报告模块（表、菜单、评论、计划外键列、MCP 开放模块）
-- 云库已占用 1.1.0.43（cloud auth user and project push），本脚本使用下一号。
-- MySQL

DELETE FROM sys_comment WHERE module_type IN ('report', 'report_history');

SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_plan' AND COLUMN_NAME = 'report_id'
);
SET @q := IF(@exists > 0, 'ALTER TABLE sys_plan DROP COLUMN report_id', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

DROP TABLE IF EXISTS sys_report_history;
DROP TABLE IF EXISTS sys_report;
DROP TABLE IF EXISTS sys_report_template;

DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.perms LIKE 'system:report:%'
   OR m.component = 'system/report/index'
   OR (m.path = 'report' AND m.parent_id = 2013);

DELETE FROM sys_menu
WHERE perms LIKE 'system:report:%'
   OR component = 'system/report/index'
   OR (path = 'report' AND parent_id = 2013);

-- modules 为 JSON 字符串数组；有 JSON_SEARCH 时去掉 "report"
UPDATE sys_project_mcp
SET modules = JSON_REMOVE(modules, JSON_UNQUOTE(JSON_SEARCH(modules, 'one', 'report')))
WHERE modules IS NOT NULL
  AND JSON_VALID(modules)
  AND JSON_SEARCH(modules, 'one', 'report') IS NOT NULL;
