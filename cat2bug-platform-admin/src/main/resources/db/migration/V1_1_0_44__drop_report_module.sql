-- Cat2Bug 1.1.0 迁移 #44: 移除报告模块（表、菜单、评论、计划外键列、MCP 开放模块）
-- 云库已占用 1.1.0.43（cloud auth user and project push），本脚本使用下一号。
-- H2

DELETE FROM sys_comment WHERE module_type IN ('report', 'report_history');

ALTER TABLE sys_plan DROP COLUMN IF EXISTS report_id;

DROP TABLE IF EXISTS sys_report_history;
DROP TABLE IF EXISTS sys_report;
DROP TABLE IF EXISTS sys_report_template;

DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu
    WHERE perms LIKE 'system:report:%'
       OR component = 'system/report/index'
       OR path = 'report' AND parent_id = 2013
);
DELETE FROM sys_menu WHERE perms LIKE 'system:report:%'
   OR component = 'system/report/index'
   OR (path = 'report' AND parent_id = 2013);

-- modules 为 JSON 字符串数组，去掉元素 "report"
UPDATE sys_project_mcp
SET modules = REPLACE(REPLACE(REPLACE(modules, ',"report"', ''), '"report",', ''), '["report"]', '[]')
WHERE modules IS NOT NULL AND modules LIKE '%"report"%';
