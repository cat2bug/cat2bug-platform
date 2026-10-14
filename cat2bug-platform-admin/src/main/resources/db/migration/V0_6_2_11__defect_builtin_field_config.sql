-- 移除误作为「系统自定义字段」写入的预置行
DELETE FROM sys_project_defect_field WHERE is_system = 1;

ALTER TABLE sys_project ADD COLUMN IF NOT EXISTS defect_builtin_field_config JSON COMMENT '缺陷内置属性启用与排序';
