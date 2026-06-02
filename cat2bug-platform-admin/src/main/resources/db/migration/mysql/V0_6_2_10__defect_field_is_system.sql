ALTER TABLE sys_project_defect_field ADD COLUMN is_system TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统预置字段' AFTER enabled;
