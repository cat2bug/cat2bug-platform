SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `sys_project` ADD COLUMN `is_lock` tinyint(3) UNSIGNED ZEROFILL NULL DEFAULT 000 COMMENT '是否禁用' AFTER `project_state`;

ALTER TABLE `sys_project` ADD COLUMN `lock_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '禁用描述' AFTER `is_lock`;

ALTER TABLE `sys_project` ADD INDEX `idx_lock`(`is_lock` ASC) USING BTREE;

ALTER TABLE `sys_team` ADD COLUMN `is_lock` tinyint(3) UNSIGNED ZEROFILL NULL DEFAULT 000 COMMENT '是否锁定' AFTER `is_del`;

ALTER TABLE `sys_team` ADD COLUMN `lock_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '锁定描述' AFTER `is_lock`;

ALTER TABLE `sys_team` ADD INDEX `idx_lock`(`is_lock` ASC) USING BTREE;

UPDATE `sys_menu` SET `order_num` = 3 WHERE `menu_id` = 2073;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2132, '团队管理', 2072, 0, 'team', 'admin/team/index', NULL, 1, 0, 'C', '0', '0', 'admin:team:list', 'website', 'admin', '2026-01-18 14:03:31', 'admin', '2026-01-21 06:56:01', '', 'team.manage');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2133, '项目管理', 2072, 1, 'project', 'admin/project/index', NULL, 1, 0, 'C', '0', '0', 'admin:project:list', 'mk-project', 'admin', '2026-01-21 01:35:42', 'admin', '2026-01-21 06:56:07', '', 'project.manage');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2134, '禁用', 2133, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'admin:project:lock', '#', 'admin', '2026-01-21 06:57:22', '', NULL, '', 'lock');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2135, '禁用', 2132, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'admin:team:lock', '#', 'admin', '2026-01-21 06:57:44', '', NULL, '', 'lock');

SET FOREIGN_KEY_CHECKS=1;