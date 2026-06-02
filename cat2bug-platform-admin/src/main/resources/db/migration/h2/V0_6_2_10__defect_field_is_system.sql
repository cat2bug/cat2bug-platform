ALTER TABLE sys_project_defect_field ADD COLUMN IF NOT EXISTS is_system TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统预置字段';
