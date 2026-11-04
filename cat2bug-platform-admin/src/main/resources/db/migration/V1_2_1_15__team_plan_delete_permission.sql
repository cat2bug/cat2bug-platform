-- 团队套餐删除权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2445, '团队套餐删除', 2440, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:remove', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:remove');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms = 'admin:team-plan:remove'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
