-- 源码管理菜单排到仪表盘之后（项目功能 parent_id=2013，仪表盘 order_num=1）
UPDATE `sys_menu`
SET `order_num` = `order_num` + 1
WHERE `parent_id` = 2013
  AND `order_num` >= 2
  AND `menu_id` <> 2150;

UPDATE `sys_menu`
SET `order_num` = 2,
    `update_by` = 'admin',
    `update_time` = CURRENT_TIMESTAMP
WHERE `menu_id` = 2150;
