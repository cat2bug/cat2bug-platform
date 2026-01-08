SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE `ai_account`  (
     `account_id` bigint NOT NULL AUTO_INCREMENT COMMENT '账号ID',
     `ai_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'AI服务网址',
     `model_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '模型名称',
     `max_completion_tokens` bigint NULL DEFAULT NULL COMMENT '最大Token',
     `api_key` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密钥',
     `create_by` bigint NULL DEFAULT NULL COMMENT '创建用户ID',
     `project_id` bigint NOT NULL COMMENT '关联项目ID',
     `account_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '账号名称',
     PRIMARY KEY (`account_id`) USING BTREE,
     INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_bin COMMENT = 'OpenAI账号' ROW_FORMAT = DYNAMIC;

ALTER TABLE `sys_defect` ADD COLUMN `sponsor` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL COMMENT '发起人' AFTER `extend_properties`;

ALTER TABLE `sys_module` ADD COLUMN `annex_urls` varchar(5000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL COMMENT '附件数组';

ALTER TABLE `sys_user` ADD COLUMN `wechat_mp_user_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL COMMENT '微信小程序openid' AFTER `wechat_user_id`;

ALTER TABLE `sys_user` ADD INDEX `idx_wechat_mp_user_id`(`wechat_mp_user_id` ASC) USING BTREE;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2126, 'OpenAI账号', 2013, 1, 'system/project/ai/account', 'system/project/ai/account/index', NULL, 1, 0, 'C', '1', '0', 'ai:account:list', '#', 'admin', '2025-12-28 14:57:42', 'admin', '2025-12-28 16:02:12', 'OpenAI账号菜单', NULL);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2127, 'OpenAI账号查询', 2126, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'ai:account:query', '#', 'admin', '2025-12-28 14:57:42', '', NULL, '', NULL);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2128, 'OpenAI账号新增', 2126, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'ai:account:add', '#', 'admin', '2025-12-28 14:57:42', '', NULL, '', NULL);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2129, 'OpenAI账号修改', 2126, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'ai:account:edit', '#', 'admin', '2025-12-28 14:57:42', '', NULL, '', NULL);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2130, 'OpenAI账号删除', 2126, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'ai:account:remove', '#', 'admin', '2025-12-28 14:57:42', '', NULL, '', NULL);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`) VALUES (2131, 'OpenAI账号导出', 2126, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'ai:account:export', '#', 'admin', '2025-12-28 14:57:42', '', NULL, '', NULL);

UPDATE `sys_role` SET `role_name` = '团队管理员', `role_key` = 'team.admin', `role_sort` = 6, `data_scope` = '1', `menu_check_strictly` = 1, `dept_check_strictly` = 1, `status` = '0', `del_flag` = '0', `create_by` = 'admin', `create_time` = '2023-11-18 15:15:50', `update_by` = 'admin', `update_time` = '2025-12-28 16:05:53', `remark` = NULL, `is_team_role` = 1, `is_project_role` = 0, `role_name_i18n_key` = 'team.admin-members' WHERE `role_id` = 4;

UPDATE `sys_role` SET `role_name` = '项目管理员', `role_key` = 'project.admin', `role_sort` = 9, `data_scope` = '1', `menu_check_strictly` = 1, `dept_check_strictly` = 1, `status` = '0', `del_flag` = '0', `create_by` = 'admin', `create_time` = '2023-11-22 06:48:53', `update_by` = 'admin', `update_time` = '2025-12-28 16:06:33', `remark` = NULL, `is_team_role` = 0, `is_project_role` = 1, `role_name_i18n_key` = 'project.admin' WHERE `role_id` = 6;

UPDATE `sys_role` SET `role_name` = '项目创建人', `role_key` = 'project.create-by', `role_sort` = 8, `data_scope` = '1', `menu_check_strictly` = 1, `dept_check_strictly` = 1, `status` = '0', `del_flag` = '0', `create_by` = 'admin', `create_time` = '2024-01-05 17:57:40', `update_by` = 'admin', `update_time` = '2025-12-28 16:06:19', `remark` = NULL, `is_team_role` = 0, `is_project_role` = 1, `role_name_i18n_key` = 'project.create-by' WHERE `role_id` = 11;

UPDATE `sys_role` SET `role_name` = '团队创建人', `role_key` = 'team.create-by', `role_sort` = 5, `data_scope` = '1', `menu_check_strictly` = 1, `dept_check_strictly` = 1, `status` = '0', `del_flag` = '0', `create_by` = 'admin', `create_time` = '2024-01-06 03:41:45', `update_by` = 'admin', `update_time` = '2025-12-28 16:05:44', `remark` = NULL, `is_team_role` = 1, `is_project_role` = 0, `role_name_i18n_key` = 'team.create-by' WHERE `role_id` = 12;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4, 2126);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4, 2127);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4, 2128);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4, 2129);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4, 2130);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4, 2131);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (6, 2126);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (6, 2127);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (6, 2128);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (6, 2129);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (6, 2130);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (6, 2131);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (11, 2126);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (11, 2127);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (11, 2128);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (11, 2129);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (11, 2130);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (11, 2131);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (12, 2126);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (12, 2127);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (12, 2128);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (12, 2129);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (12, 2130);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (12, 2131);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4127863588668069888, 2933727266500434944);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4127863588668069888, 3385774394113413120);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4127863588668069888, 5013510433510386688);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4127863588668069888, 5130452802259828736);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4127863588668069888, 7331262730750886912);

SET FOREIGN_KEY_CHECKS=1;

