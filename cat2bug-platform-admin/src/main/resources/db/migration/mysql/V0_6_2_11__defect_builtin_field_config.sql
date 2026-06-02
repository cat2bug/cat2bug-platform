DELETE FROM sys_project_defect_field WHERE is_system = 1;

ALTER TABLE sys_project ADD COLUMN defect_builtin_field_config JSON NULL COMMENT '缺陷内置属性启用与排序' AFTER project_introduce;
