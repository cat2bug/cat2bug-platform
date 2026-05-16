ALTER TABLE `sys_defect` ADD COLUMN `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
