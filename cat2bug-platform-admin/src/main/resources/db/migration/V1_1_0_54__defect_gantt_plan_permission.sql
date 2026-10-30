-- 甘特计划节点拖改独立权限（不复用 system:defect:edit）。
-- 若曾误给 project.develop 挂上缺陷修改（2022），一并撤回。

DELETE FROM sys_role_menu
WHERE role_id = 7 AND menu_id = 2022;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2428, '甘特计划编辑', 2019, 30, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:defect:gantt:plan', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '拖改甘特节点计划起止/处理人/交付物', 'defect.gantt.plan-edit'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2428 OR perms = 'system:defect:gantt:plan');

-- 与可参与缺陷排期的项目/团队角色对齐：创建人、管理员、开发、测试、外部人员、团队创建人/管理员
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM (
    SELECT 4 AS role_id UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 11 UNION ALL SELECT 12
) r
CROSS JOIN sys_menu m
WHERE m.perms = 'system:defect:gantt:plan'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
