-- legacy 库可能已手工或部分执行过本脚本，按列/索引/菜单幂等处理
SET FOREIGN_KEY_CHECKS=0;
SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_project' AND COLUMN_NAME = 'is_lock'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_project` ADD COLUMN `is_lock` tinyint(3) UNSIGNED ZEROFILL NULL DEFAULT 000 COMMENT ''是否禁用'' AFTER `project_state`',
  'SELECT 1'
);
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_project' AND COLUMN_NAME = 'lock_remark'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_project` ADD COLUMN `lock_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT ''禁用描述'' AFTER `is_lock`',
  'SELECT 1'
);
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_project' AND INDEX_NAME = 'idx_lock'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_project` ADD INDEX `idx_lock`(`is_lock` ASC) USING BTREE',
  'SELECT 1'
);
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'is_lock'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_team` ADD COLUMN `is_lock` tinyint(3) UNSIGNED ZEROFILL NULL DEFAULT 000 COMMENT ''是否锁定'' AFTER `is_del`',
  'SELECT 1'
);
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'lock_remark'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_team` ADD COLUMN `lock_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT ''锁定描述'' AFTER `is_lock`',
  'SELECT 1'
);
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_team' AND INDEX_NAME = 'idx_lock'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_team` ADD INDEX `idx_lock`(`is_lock` ASC) USING BTREE',
  'SELECT 1'
);
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE `sys_menu` SET `order_num` = 3 WHERE `menu_id` = 2073;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2132, '团队管理', 2072, 0, 'team', 'admin/team/index', NULL, 1, 0, 'C', '0', '0', 'admin:team:list', 'website', 'admin', '2026-01-18 14:03:31', 'admin', '2026-01-21 06:56:01', '', 'team.manage'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2132);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2133, '项目管理', 2072, 1, 'project', 'admin/project/index', NULL, 1, 0, 'C', '0', '0', 'admin:project:list', 'mk-project', 'admin', '2026-01-21 01:35:42', 'admin', '2026-01-21 06:56:07', '', 'project.manage'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2133);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2134, '禁用', 2133, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'admin:project:lock', '#', 'admin', '2026-01-21 06:57:22', '', NULL, '', 'lock'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2134);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2135, '禁用', 2132, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'admin:team:lock', '#', 'admin', '2026-01-21 06:57:44', '', NULL, '', 'lock'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2135);

SET FOREIGN_KEY_CHECKS=1;
