-- 清理旧版飞书菜单（2136-2138）与 V0_6_2_9 标准集（2141-2143）并存导致的重复
-- 按 perms 校验，避免误删 MCP（2138）/ CI 环境（2137）等非飞书占用 ID

-- 将仍绑定旧飞书菜单的角色授权迁移到 2141-2143
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2141
FROM `sys_role_menu` rm
WHERE rm.menu_id = 2136
  AND EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2136 AND `perms` = 'feishu:list')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.role_id = rm.role_id AND x.menu_id = 2141);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2142
FROM `sys_role_menu` rm
WHERE rm.menu_id = 2137
  AND EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2137 AND `perms` = 'feishu:query')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.role_id = rm.role_id AND x.menu_id = 2142);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2143
FROM `sys_role_menu` rm
WHERE rm.menu_id = 2138
  AND EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2138 AND `perms` = 'feishu:save')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.role_id = rm.role_id AND x.menu_id = 2143);

DELETE rm FROM `sys_role_menu` rm
INNER JOIN (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `menu_id` = 2141 AND `perms` = 'feishu:list'
) canonical ON 1 = 1
INNER JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE rm.`menu_id` IN (2136, 2137, 2138)
  AND (
    (rm.`menu_id` = 2136 AND m.`perms` = 'feishu:list')
    OR (rm.`menu_id` = 2137 AND m.`perms` = 'feishu:query')
    OR (rm.`menu_id` = 2138 AND m.`perms` = 'feishu:save')
  );

DELETE FROM `sys_menu`
WHERE `menu_id` IN (2138, 2137, 2136)
  AND (
    (`menu_id` = 2138 AND `perms` = 'feishu:save')
    OR (`menu_id` = 2137 AND `perms` = 'feishu:query')
    OR (`menu_id` = 2136 AND `perms` = 'feishu:list' AND `path` = 'feishu')
  )
  AND EXISTS (
    SELECT 1 FROM (
      SELECT `menu_id` FROM `sys_menu` WHERE `menu_id` = 2141 AND `perms` = 'feishu:list'
    ) AS `canonical_feishu`
  );
