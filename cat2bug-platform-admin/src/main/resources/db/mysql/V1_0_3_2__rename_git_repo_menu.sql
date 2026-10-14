-- 源码管理菜单：改用 code 图标，侧栏折叠时更易辨认
UPDATE `sys_menu`
SET `menu_name` = '源码管理',
    `icon` = 'code',
    `remark` = '项目源码管理',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 2150;
