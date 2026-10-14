-- 缺陷属性管理权限：仅团队创建人/管理员、项目创建人/管理员（role 4/6/11/12）

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2136, '缺陷属性查询', 2052, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:defect-field:list', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', 'defect.custom-field.manage'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2136);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2137, '缺陷属性新增', 2052, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:defect-field:add', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2137);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2138, '缺陷属性修改', 2052, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:defect-field:edit', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2138);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2139, '缺陷属性删除', 2052, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:defect-field:remove', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2139);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 4, 2136 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 2136);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 4, 2137 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 2137);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 4, 2138 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 2138);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 4, 2139 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 2139);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2136 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2136);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2137 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2137);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2138 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2138);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2139 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2139);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2136 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2136);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2137 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2137);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2138 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2138);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2139 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2139);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 12, 2136 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 12 AND `menu_id` = 2136);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 12, 2137 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 12 AND `menu_id` = 2137);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 12, 2138 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 12 AND `menu_id` = 2138);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 12, 2139 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 12 AND `menu_id` = 2139);
