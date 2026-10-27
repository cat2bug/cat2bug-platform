-- Cat2Bug 1.1.0 #50: 用例类型 / 评审模板 / 缺陷属性升到团队级
-- MySQL。幂等（INFORMATION_SCHEMA 判列/索引）；勿用 ADD COLUMN IF NOT EXISTS。

-- sys_team.case_review_template
SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'case_review_template'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_team ADD COLUMN case_review_template MEDIUMTEXT NULL COMMENT ''团队用例评审纪要模板''',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_team.defect_builtin_field_config
SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'defect_builtin_field_config'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_team ADD COLUMN defect_builtin_field_config JSON NULL COMMENT ''团队缺陷内置属性布局''',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_case_type.team_id
SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_case_type' AND COLUMN_NAME = 'team_id'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_case_type ADD COLUMN team_id BIGINT NULL COMMENT ''团队ID'' AFTER type_id',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_project_defect_field.team_id
SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_project_defect_field' AND COLUMN_NAME = 'team_id'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_project_defect_field ADD COLUMN team_id BIGINT NULL COMMENT ''团队ID'' AFTER field_id',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回填 team_id
UPDATE sys_case_type t
INNER JOIN sys_project p ON p.project_id = t.project_id
SET t.team_id = p.team_id
WHERE t.team_id IS NULL AND t.project_id IS NOT NULL;

UPDATE sys_project_defect_field f
INNER JOIN sys_project p ON p.project_id = f.project_id
SET f.team_id = p.team_id
WHERE f.team_id IS NULL AND f.project_id IS NOT NULL;

-- 临时表：每团队源项目（最早 create_time，同分取最小 project_id）
DROP TABLE IF EXISTS tmp_team_source_project;
CREATE TABLE tmp_team_source_project (
  team_id BIGINT NOT NULL PRIMARY KEY,
  source_project_id BIGINT NOT NULL
) ENGINE=InnoDB;

INSERT INTO tmp_team_source_project (team_id, source_project_id)
SELECT p.team_id, MIN(p.project_id) AS source_project_id
FROM sys_project p
INNER JOIN (
  SELECT team_id, MIN(COALESCE(create_time, '1970-01-01 00:00:00')) AS min_ct
  FROM sys_project
  GROUP BY team_id
) m ON p.team_id = m.team_id
  AND COALESCE(p.create_time, '1970-01-01 00:00:00') = m.min_ct
GROUP BY p.team_id;

-- 团队模板 / builtin：从源项目拷贝
UPDATE sys_team t
INNER JOIN tmp_team_source_project sp ON sp.team_id = t.team_id
INNER JOIN sys_project p ON p.project_id = sp.source_project_id
SET t.case_review_template = p.case_review_template
WHERE t.case_review_template IS NULL AND p.case_review_template IS NOT NULL;

UPDATE sys_team t
INNER JOIN tmp_team_source_project sp ON sp.team_id = t.team_id
INNER JOIN sys_project p ON p.project_id = sp.source_project_id
SET t.defect_builtin_field_config = p.defect_builtin_field_config
WHERE t.defect_builtin_field_config IS NULL AND p.defect_builtin_field_config IS NOT NULL;

-- 非源项目用例类型按名称 remap 到源项目同名类型
UPDATE sys_case c
INNER JOIN sys_case_type oldt ON oldt.type_id = c.case_type
INNER JOIN sys_project op ON op.project_id = oldt.project_id
INNER JOIN tmp_team_source_project sp ON sp.team_id = op.team_id AND oldt.project_id <> sp.source_project_id
INNER JOIN sys_case_type src ON src.project_id = sp.source_project_id AND src.type_name = oldt.type_name
SET c.case_type = src.type_id
WHERE c.case_type IS NOT NULL;

UPDATE sys_case c
INNER JOIN sys_case_type oldt ON oldt.type_id = c.case_type
INNER JOIN sys_project op ON op.project_id = oldt.project_id
INNER JOIN tmp_team_source_project sp ON sp.team_id = op.team_id AND oldt.project_id <> sp.source_project_id
LEFT JOIN sys_case_type src ON src.project_id = sp.source_project_id AND src.type_name = oldt.type_name
SET c.case_type = NULL
WHERE c.case_type IS NOT NULL AND src.type_id IS NULL;

-- 删除非源项目的类型行 / 缺陷字段行
DELETE t FROM sys_case_type t
INNER JOIN tmp_team_source_project sp ON sp.team_id = t.team_id
WHERE t.project_id IS NOT NULL AND t.project_id <> sp.source_project_id;

DELETE f FROM sys_project_defect_field f
INNER JOIN tmp_team_source_project sp ON sp.team_id = f.team_id
WHERE f.project_id IS NOT NULL AND f.project_id <> sp.source_project_id;

DROP TABLE IF EXISTS tmp_team_source_project;

-- 索引（存在则跳过）
SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_case_type' AND INDEX_NAME = 'uk_case_type_team_name'
);
SET @q := IF(@exists = 0, 'CREATE UNIQUE INDEX uk_case_type_team_name ON sys_case_type (team_id, type_name)', 'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_project_defect_field' AND INDEX_NAME = 'uk_team_defect_field_key'
);
SET @q := IF(@exists = 0, 'CREATE UNIQUE INDEX uk_team_defect_field_key ON sys_project_defect_field (team_id, field_key)', 'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_case_type' AND INDEX_NAME = 'idx_case_type_team'
);
SET @q := IF(@exists = 0, 'CREATE INDEX idx_case_type_team ON sys_case_type (team_id)', 'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_project_defect_field' AND INDEX_NAME = 'idx_defect_field_team'
);
SET @q := IF(@exists = 0, 'CREATE INDEX idx_defect_field_team ON sys_project_defect_field (team_id)', 'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 团队级行不再强制 project_id（Java seed / 无项目团队均只写 team_id）
SET @nullable := (
  SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_case_type' AND COLUMN_NAME = 'project_id'
);
SET @q := IF(@nullable = 'NO',
  'ALTER TABLE sys_case_type MODIFY COLUMN project_id BIGINT NULL COMMENT ''项目ID（团队级可空）''',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @nullable := (
  SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_project_defect_field' AND COLUMN_NAME = 'project_id'
);
SET @q := IF(@nullable = 'NO',
  'ALTER TABLE sys_project_defect_field MODIFY COLUMN project_id BIGINT NULL COMMENT ''项目ID（团队级可空）''',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 无项目团队 seed 默认类型
INSERT INTO sys_case_type (team_id, project_id, type_name, sort_order, enabled)
SELECT t.team_id, NULL, x.type_name, x.sort_order, 1
FROM sys_team t
CROSS JOIN (
  SELECT '功能' AS type_name, 1 AS sort_order
  UNION ALL SELECT '接口', 2
  UNION ALL SELECT '界面', 3
) x
WHERE NOT EXISTS (SELECT 1 FROM sys_project p WHERE p.team_id = t.team_id)
  AND NOT EXISTS (
    SELECT 1 FROM sys_case_type c WHERE c.team_id = t.team_id AND c.type_name = x.type_name
  );

-- 菜单：团队级权限（挂在团队设置 2015）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2420, '团队用例类型查询', 2015, 20, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:case-type:list', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', 'case.type.manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:case-type:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2421, '团队用例类型新增', 2015, 21, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:case-type:add', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:case-type:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2422, '团队用例类型修改', 2015, 22, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:case-type:edit', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:case-type:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2423, '团队用例类型删除', 2015, 23, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:case-type:remove', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:case-type:remove');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2424, '团队缺陷属性查询', 2015, 24, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:defect-field:list', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', 'defect.custom-field.manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:defect-field:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2425, '团队缺陷属性新增', 2015, 25, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:defect-field:add', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:defect-field:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2426, '团队缺陷属性修改', 2015, 26, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:defect-field:edit', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:defect-field:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2427, '团队缺陷属性删除', 2015, 27, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:team:defect-field:remove', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:team:defect-field:remove');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM (SELECT 4 AS role_id UNION ALL SELECT 12) r
CROSS JOIN sys_menu m
WHERE m.perms IN (
  'system:team:case-type:list',
  'system:team:case-type:add',
  'system:team:case-type:edit',
  'system:team:case-type:remove',
  'system:team:defect-field:list',
  'system:team:defect-field:add',
  'system:team:defect-field:edit',
  'system:team:defect-field:remove'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
);

DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id IN (6, 11)
  AND m.perms IN (
    'system:project:case-type:list',
    'system:project:case-type:add',
    'system:project:case-type:edit',
    'system:project:case-type:remove',
    'system:project:defect-field:list',
    'system:project:defect-field:add',
    'system:project:defect-field:edit',
    'system:project:defect-field:remove'
  );
