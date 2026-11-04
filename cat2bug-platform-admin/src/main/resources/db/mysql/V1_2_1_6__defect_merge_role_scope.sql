-- 收窄 system:defect:merge：仅项目创建人 / 项目管理员 / 项目测试。
-- 撤回此前从「缺陷删除」复制到团队创建人、团队管理员等角色的关联。

DELETE FROM sys_role_menu
WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms = 'system:defect:merge')
  AND role_id IN (
    SELECT role_id FROM sys_role
    WHERE role_key NOT IN ('project.create-by', 'project.admin', 'project.tester')
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE m.perms = 'system:defect:merge'
  AND r.role_key IN ('project.create-by', 'project.admin', 'project.tester')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.role_id AND x.menu_id = m.menu_id
  );
