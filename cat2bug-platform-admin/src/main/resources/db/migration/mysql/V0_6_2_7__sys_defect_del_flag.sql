ALTER TABLE `sys_defect` ADD COLUMN `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）' AFTER `sponsor`;
