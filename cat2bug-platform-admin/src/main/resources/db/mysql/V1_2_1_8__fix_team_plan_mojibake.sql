-- 修复 V1_2_1_1 种子写入的套餐中文乱码；补齐/更正「团队套餐」菜单（原名误为团队角色，且 2160 与 MCP 冲突）

UPDATE sys_team_plan
SET plan_name = CONVERT(UNHEX('E699AEE9809AE59BA2E9989F') USING utf8mb4),
    remark = CONVERT(UNHEX('E7B3BBE7BB9FE9BB98E8AEA4E5A597E9A490') USING utf8mb4),
    update_by = 'admin',
    update_time = NOW()
WHERE plan_id = 1
  AND plan_name <> CONVERT(UNHEX('E699AEE9809AE59BA2E9989F') USING utf8mb4);

SET @q = CONCAT('ALTER TABLE sys_team_plan COMMENT = ''', CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E9858DE9A29D') USING utf8mb4), '''');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @q = CONCAT(
  'ALTER TABLE sys_team_plan ',
  'MODIFY COLUMN plan_id BIGINT NOT NULL AUTO_INCREMENT COMMENT ''', CONVERT(UNHEX('E5A597E9A4904944') USING utf8mb4), ''', ',
  'MODIFY COLUMN plan_name VARCHAR(64) NOT NULL COMMENT ''', CONVERT(UNHEX('E5A597E9A490E5908DE7A7B0') USING utf8mb4), ''', ',
  'MODIFY COLUMN max_members INT NOT NULL DEFAULT 10 COMMENT ''', CONVERT(UNHEX('E59BA2E9989FE69C80E5A4A7E4BABAE695B0') USING utf8mb4), ''', ',
  'MODIFY COLUMN max_projects INT NOT NULL DEFAULT 20 COMMENT ''', CONVERT(UNHEX('E59BA2E9989FE69C80E5A4A7E9A1B9E79BAEE695B0') USING utf8mb4), ''', ',
  'MODIFY COLUMN max_project_bytes BIGINT NOT NULL DEFAULT 1073741824 COMMENT ''', CONVERT(UNHEX('E6AF8FE9A1B9E79BAEE69C80E5A4A7E7A9BAE997B4E5AD97E88A82') USING utf8mb4), ''', ',
  'MODIFY COLUMN is_system_default TINYINT NOT NULL DEFAULT 0 COMMENT ''', CONVERT(UNHEX('E698AFE590A6E7B3BBE7BB9FE9BB98E8AEA4') USING utf8mb4), ''''
);
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2440, CONVERT(UNHEX('E59BA2E9989FE5A597E9A490') USING utf8mb4), 2072, 2, 'team-plan', 'admin/team-plan/index', NULL, 1, 0, 'C', '0', '0', 'admin:team-plan:list', 'peoples', 'admin', NOW(), '', NULL, CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E9858DE9A29D') USING utf8mb4), 'admin.team-plan'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2441, CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E69FA5E8AFA2') USING utf8mb4), 2440, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:query', '#', 'admin', NOW(), '', NULL, '', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2442, CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E4BFAEE694B9') USING utf8mb4), 2440, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:edit', '#', 'admin', NOW(), '', NULL, '', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2443, CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E696B0E5A29E') USING utf8mb4), 2440, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:add', '#', 'admin', NOW(), '', NULL, '', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2444, CONVERT(UNHEX('E7BB91E5AE9AE5A597E9A490') USING utf8mb4), 2132, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team:plan', '#', 'admin', NOW(), '', NULL, '', 'admin.team.bind-plan'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team:plan');

-- 已插入但名称仍为「团队角色」时更正为「团队套餐」
UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E59BA2E9989FE5A597E9A490') USING utf8mb4), menu_name_i18n_key = 'admin.team-plan'
WHERE perms = 'admin:team-plan:list' AND menu_name <> CONVERT(UNHEX('E59BA2E9989FE5A597E9A490') USING utf8mb4);

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E69FA5E8AFA2') USING utf8mb4)
WHERE perms = 'admin:team-plan:query' AND menu_name <> CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E69FA5E8AFA2') USING utf8mb4);

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E4BFAEE694B9') USING utf8mb4)
WHERE perms = 'admin:team-plan:edit' AND menu_name <> CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E4BFAEE694B9') USING utf8mb4);

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E696B0E5A29E') USING utf8mb4)
WHERE perms = 'admin:team-plan:add' AND menu_name <> CONVERT(UNHEX('E59BA2E9989FE5A597E9A490E696B0E5A29E') USING utf8mb4);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('admin:team-plan:list', 'admin:team-plan:query', 'admin:team-plan:edit', 'admin:team-plan:add', 'admin:team:plan')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
