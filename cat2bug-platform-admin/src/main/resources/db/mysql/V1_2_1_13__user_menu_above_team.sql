-- Admin「系统」：成员/用户 放到 团队 上面；系统管理侧栏同步
-- 目标顺序（parent=2072）：成员(0) → 团队(1) → 项目(2) → 角色(3) → 团队套餐(4)
UPDATE sys_menu SET order_num = 0 WHERE menu_id = 2073 AND parent_id = 2072;
UPDATE sys_menu SET order_num = 1 WHERE menu_id = 2132 AND parent_id = 2072;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 2133 AND parent_id = 2072;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 101 AND parent_id = 2072;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 2440 AND parent_id = 2072;

-- 系统管理：用户管理 在 团队管理 之前
UPDATE sys_menu SET order_num = 1 WHERE menu_id = 100 AND parent_id = 1;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 2006 AND parent_id = 1;
