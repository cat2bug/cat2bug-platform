-- 团队套餐菜单图标改为 team-plan
UPDATE sys_menu
SET icon = 'team-plan'
WHERE perms = 'admin:team-plan:list'
  AND (icon IS NULL OR icon <> 'team-plan');
