-- 修复套餐种子乱码；补齐/更正「团队套餐」菜单（原名误为团队角色）

UPDATE sys_team_plan
SET plan_name = '普通团队',
    remark = '系统默认套餐',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
WHERE plan_id = 1
  AND plan_name <> '普通团队';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2440, '团队套餐', 2072, 2, 'team-plan', 'admin/team-plan/index', NULL, 1, 0, 'C', '0', '0', 'admin:team-plan:list', 'peoples', 'admin', CURRENT_TIMESTAMP, '', NULL, '团队套餐配额', 'admin.team-plan'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2441, '团队套餐查询', 2440, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:query', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2442, '团队套餐修改', 2440, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:edit', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2443, '团队套餐新增', 2440, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:add', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2444, '绑定套餐', 2132, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team:plan', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', 'admin.team.bind-plan'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team:plan');

UPDATE sys_menu SET menu_name = '团队套餐', menu_name_i18n_key = 'admin.team-plan'
WHERE perms = 'admin:team-plan:list' AND menu_name <> '团队套餐';
UPDATE sys_menu SET menu_name = '团队套餐查询' WHERE perms = 'admin:team-plan:query' AND menu_name <> '团队套餐查询';
UPDATE sys_menu SET menu_name = '团队套餐修改' WHERE perms = 'admin:team-plan:edit' AND menu_name <> '团队套餐修改';
UPDATE sys_menu SET menu_name = '团队套餐新增' WHERE perms = 'admin:team-plan:add' AND menu_name <> '团队套餐新增';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('admin:team-plan:list', 'admin:team-plan:query', 'admin:team-plan:edit', 'admin:team-plan:add', 'admin:team:plan')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
