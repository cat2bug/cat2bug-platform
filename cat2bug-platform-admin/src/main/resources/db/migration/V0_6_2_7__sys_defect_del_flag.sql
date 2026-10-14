ALTER TABLE sys_defect ADD COLUMN IF NOT EXISTS del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
