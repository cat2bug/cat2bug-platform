-- Cat2Bug 1.1.0 #50: 用例类型 / 评审模板 / 缺陷属性升到团队级
-- H2。脚本尽量幂等。

ALTER TABLE sys_team ADD COLUMN IF NOT EXISTS case_review_template CLOB;
ALTER TABLE sys_team ADD COLUMN IF NOT EXISTS defect_builtin_field_config CLOB;

ALTER TABLE sys_case_type ADD COLUMN IF NOT EXISTS team_id BIGINT;
ALTER TABLE sys_project_defect_field ADD COLUMN IF NOT EXISTS team_id BIGINT;

-- 回填 team_id
UPDATE sys_case_type t
SET team_id = (SELECT p.team_id FROM sys_project p WHERE p.project_id = t.project_id)
WHERE t.team_id IS NULL AND t.project_id IS NOT NULL;

UPDATE sys_project_defect_field f
SET team_id = (SELECT p.team_id FROM sys_project p WHERE p.project_id = f.project_id)
WHERE f.team_id IS NULL AND f.project_id IS NOT NULL;

-- 团队模板 / builtin：取该团队最早项目
UPDATE sys_team t
SET case_review_template = (
  SELECT p.case_review_template FROM sys_project p
  WHERE p.team_id = t.team_id
  ORDER BY p.create_time ASC NULLS LAST, p.project_id ASC
  LIMIT 1
)
WHERE t.case_review_template IS NULL;

UPDATE sys_team t
SET defect_builtin_field_config = (
  SELECT CAST(p.defect_builtin_field_config AS VARCHAR) FROM sys_project p
  WHERE p.team_id = t.team_id
  ORDER BY p.create_time ASC NULLS LAST, p.project_id ASC
  LIMIT 1
)
WHERE t.defect_builtin_field_config IS NULL;

-- 临时表：每团队源项目
CREATE TABLE IF NOT EXISTS tmp_team_source_project AS
SELECT p.team_id, MIN(p.project_id) AS source_project_id
FROM sys_project p
INNER JOIN (
  SELECT team_id, MIN(COALESCE(create_time, TIMESTAMP '1970-01-01 00:00:00')) AS min_ct
  FROM sys_project
  GROUP BY team_id
) m ON p.team_id = m.team_id
   AND COALESCE(p.create_time, TIMESTAMP '1970-01-01 00:00:00') = m.min_ct
GROUP BY p.team_id;

-- 非源项目用例类型按名称 remap 到源项目同名类型
UPDATE sys_case c
SET case_type = (
  SELECT src.type_id
  FROM sys_case_type old
  INNER JOIN sys_project op ON op.project_id = old.project_id
  INNER JOIN tmp_team_source_project sp ON sp.team_id = op.team_id
  INNER JOIN sys_case_type src ON src.project_id = sp.source_project_id AND src.type_name = old.type_name
  WHERE old.type_id = c.case_type
)
WHERE c.case_type IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM sys_case_type old
    INNER JOIN sys_project op ON op.project_id = old.project_id
    INNER JOIN tmp_team_source_project sp ON sp.team_id = op.team_id
    WHERE old.type_id = c.case_type
      AND old.project_id <> sp.source_project_id
  );

UPDATE sys_case c
SET case_type = NULL
WHERE c.case_type IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM sys_case_type old
    INNER JOIN sys_project op ON op.project_id = old.project_id
    INNER JOIN tmp_team_source_project sp ON sp.team_id = op.team_id
    WHERE old.type_id = c.case_type
      AND old.project_id <> sp.source_project_id
      AND NOT EXISTS (
        SELECT 1 FROM sys_case_type src
        WHERE src.project_id = sp.source_project_id AND src.type_name = old.type_name
      )
  );

-- 删除非源项目的类型行
DELETE FROM sys_case_type t
WHERE EXISTS (
  SELECT 1 FROM tmp_team_source_project sp
  WHERE sp.team_id = t.team_id AND t.project_id <> sp.source_project_id
);

-- 删除非源项目的缺陷字段行（同 key 保留源）
DELETE FROM sys_project_defect_field f
WHERE EXISTS (
  SELECT 1 FROM tmp_team_source_project sp
  WHERE sp.team_id = f.team_id AND f.project_id <> sp.source_project_id
);

DROP TABLE IF EXISTS tmp_team_source_project;

-- 索引：团队唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_case_type_team_name ON sys_case_type (team_id, type_name);
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_defect_field_key ON sys_project_defect_field (team_id, field_key);
CREATE INDEX IF NOT EXISTS idx_case_type_team ON sys_case_type (team_id);
CREATE INDEX IF NOT EXISTS idx_defect_field_team ON sys_project_defect_field (team_id);

-- 团队级行不再强制 project_id
ALTER TABLE sys_case_type ALTER COLUMN project_id SET NULL;
ALTER TABLE sys_project_defect_field ALTER COLUMN project_id SET NULL;

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

-- 赋给团队创建人(12)、团队管理员(4)
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

-- 收回项目侧配置菜单给项目角色（6/11）；缺陷字段原也赋过 4/12 保留团队侧即可
DELETE FROM sys_role_menu
WHERE role_id IN (6, 11)
  AND menu_id IN (
    SELECT menu_id FROM sys_menu WHERE perms IN (
      'system:project:case-type:list',
      'system:project:case-type:add',
      'system:project:case-type:edit',
      'system:project:case-type:remove',
      'system:project:defect-field:list',
      'system:project:defect-field:add',
      'system:project:defect-field:edit',
      'system:project:defect-field:remove'
    )
  );
