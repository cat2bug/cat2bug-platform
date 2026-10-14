-- 浏览器测试工具前端已移除，隐藏相关菜单
UPDATE sys_menu SET visible = '1', status = '1' WHERE menu_id IN (2044, 2045);
