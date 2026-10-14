-- 修复 V0_6_2_13：2136-2138 已被飞书菜单占用，缺陷属性 list/add/edit 权限未入库
-- 按 perms 幂等插入菜单，并为团队/项目创建人与管理员角色授权

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2144, '缺陷属性查询', 2052, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:defect-field:list', '#', 'admin', NOW(), '', NULL, '', 'defect.custom-field.manage'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:project:defect-field:list');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2145, '缺陷属性新增', 2052, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:defect-field:add', '#', 'admin', NOW(), '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:project:defect-field:add');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2146, '缺陷属性修改', 2052, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:defect-field:edit', '#', 'admin', NOW(), '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:project:defect-field:edit');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM (SELECT 4 AS role_id UNION ALL SELECT 6 UNION ALL SELECT 11 UNION ALL SELECT 12) r
CROSS JOIN `sys_menu` m
WHERE m.perms IN (
    'system:project:defect-field:list',
    'system:project:defect-field:add',
    'system:project:defect-field:edit',
    'system:project:defect-field:remove'
)
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
);
