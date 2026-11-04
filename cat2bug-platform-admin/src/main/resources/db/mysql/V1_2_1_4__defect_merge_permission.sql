-- 缺陷合并独立权限（不复用 system:defect:edit + remove 组合）。
-- menu_id 2431：2429/2430 已被 project push 占用；仅按 perms 判重。
-- 仅授予项目创建人 / 项目管理员 / 项目测试（不含团队创建人、团队管理员、开发等）。

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2431, '缺陷合并', 2019, 31, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:defect:merge', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '合并重复缺陷（更新主缺陷并软删从缺陷）', 'defect.merge'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:defect:merge');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE m.perms = 'system:defect:merge'
  AND r.role_key IN ('project.create-by', 'project.admin', 'project.tester')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.role_id AND x.menu_id = m.menu_id
  );
