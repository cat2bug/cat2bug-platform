-- 团队套餐不进左侧菜单：仅从「团队管理 → 维护套餐」进入（路由仍注册）
UPDATE sys_menu
SET visible = '1'
WHERE perms = 'admin:team-plan:list'
  AND visible <> '1';
