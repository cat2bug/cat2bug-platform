-- Admin 团队管理菜单图标改为双人 peoples
UPDATE sys_menu
SET icon = 'peoples'
WHERE perms = 'admin:team:list'
  AND (icon IS NULL OR icon <> 'peoples');
