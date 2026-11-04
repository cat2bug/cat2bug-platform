-- 团队套餐（角色）配额 + 项目已用空间；Admin「团队角色」菜单（MySQL 幂等）

CREATE TABLE IF NOT EXISTS sys_team_plan (
  plan_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  plan_name VARCHAR(64) NOT NULL COMMENT '套餐名称',
  max_members INT NOT NULL DEFAULT 10 COMMENT '团队最大人数',
  max_projects INT NOT NULL DEFAULT 20 COMMENT '团队最大项目数',
  max_project_bytes BIGINT NOT NULL DEFAULT 1073741824 COMMENT '每项目最大空间字节',
  is_system_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统默认',
  remark VARCHAR(255) DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (plan_id),
  UNIQUE KEY uk_sys_team_plan_name (plan_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队套餐配额';

INSERT INTO sys_team_plan (plan_id, plan_name, max_members, max_projects, max_project_bytes, is_system_default, remark, create_by, create_time)
SELECT 1, '普通团队', 10, 20, 1073741824, 1, '系统默认套餐', 'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_team_plan WHERE plan_id = 1 OR plan_name = '普通团队');

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'plan_id'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_team ADD COLUMN plan_id BIGINT NULL COMMENT ''绑定套餐ID'' AFTER team_id',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_project' AND COLUMN_NAME = 'used_bytes'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_project ADD COLUMN used_bytes BIGINT NOT NULL DEFAULT 0 COMMENT ''已用存储字节'' AFTER project_introduce',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_team SET plan_id = 1 WHERE plan_id IS NULL;

-- 菜单 ID 用 2440+，避免与 MCP(2160) 等冲突；幂等按 perms
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2440, '团队套餐', 2072, 2, 'team-plan', 'admin/team-plan/index', NULL, 1, 0, 'C', '0', '0', 'admin:team-plan:list', 'peoples', 'admin', NOW(), '', NULL, '团队套餐配额', 'admin.team-plan'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2441, '团队套餐查询', 2440, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:query', '#', 'admin', NOW(), '', NULL, '', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2442, '团队套餐修改', 2440, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:edit', '#', 'admin', NOW(), '', NULL, '', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2443, '团队套餐新增', 2440, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team-plan:add', '#', 'admin', NOW(), '', NULL, '', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team-plan:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2444, '绑定套餐', 2132, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'admin:team:plan', '#', 'admin', NOW(), '', NULL, '', 'admin.team.bind-plan'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'admin:team:plan');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('admin:team-plan:list', 'admin:team-plan:query', 'admin:team-plan:edit', 'admin:team-plan:add', 'admin:team:plan')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
