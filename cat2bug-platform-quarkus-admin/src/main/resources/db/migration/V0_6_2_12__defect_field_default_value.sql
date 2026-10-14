ALTER TABLE sys_project_defect_field ADD COLUMN IF NOT EXISTS default_value JSON COMMENT '新建缺陷时的默认值';
