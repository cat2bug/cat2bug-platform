-- Cat2Bug 1.1.0 迁移 #45: 项目用例类型与可选评审
-- MySQL。脚本可重复执行。

CREATE TABLE IF NOT EXISTS sys_case_type (
  type_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  type_name VARCHAR(64) NOT NULL COMMENT '类型名称',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (type_id),
  UNIQUE KEY uk_case_type_project_name (project_id, type_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目用例类型';

CREATE TABLE IF NOT EXISTS sys_case_review (
  review_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评审ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  status VARCHAR(16) NOT NULL COMMENT 'open/completed',
  report_markdown MEDIUMTEXT COMMENT '纪要Markdown',
  create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  complete_by VARCHAR(64) DEFAULT NULL COMMENT '完成人',
  complete_time DATETIME DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (review_id),
  KEY idx_case_review_project (project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例评审';

CREATE TABLE IF NOT EXISTS sys_case_review_item (
  item_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '子项ID',
  review_id BIGINT NOT NULL COMMENT '评审ID',
  case_id BIGINT NOT NULL COMMENT '用例ID',
  verdict VARCHAR(16) NOT NULL COMMENT 'pending/passed/changes',
  item_comment VARCHAR(2000) DEFAULT NULL COMMENT '意见',
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_case_review_item (review_id, case_id),
  KEY idx_case_review_item_case (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例评审子项';

SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_case' AND COLUMN_NAME = 'review_status'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_case ADD COLUMN review_status VARCHAR(16) DEFAULT NULL COMMENT ''评审章 none空/in_review/passed/changes''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_project' AND COLUMN_NAME = 'case_review_template'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_project ADD COLUMN case_review_template MEDIUMTEXT NULL COMMENT ''用例评审纪要模版，空则用内置''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

INSERT INTO sys_case_type (project_id, type_name, sort_order)
SELECT p.project_id, t.type_name, t.sort_order
FROM sys_project p
CROSS JOIN (
  SELECT '功能' AS type_name, 1 AS sort_order
  UNION ALL SELECT '接口', 2
  UNION ALL SELECT '界面', 3
) t
WHERE NOT EXISTS (
  SELECT 1 FROM sys_case_type c
  WHERE c.project_id = p.project_id AND c.type_name = t.type_name
);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2410, '用例类型查询', 2052, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:case-type:list', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', 'case.type.manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:project:case-type:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2411, '用例类型新增', 2052, 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:case-type:add', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:project:case-type:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2412, '用例类型修改', 2052, 7, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:case-type:edit', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:project:case-type:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2413, '用例类型删除', 2052, 8, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:project:case-type:remove', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:project:case-type:remove');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, menu_name_i18n_key)
SELECT 2414, '用例评审', 2074, 8, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:case:review', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', 'case.review.start'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:case:review');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM (SELECT 6 AS role_id UNION ALL SELECT 11) r
CROSS JOIN sys_menu m
WHERE m.perms IN (
  'system:project:case-type:list',
  'system:project:case-type:add',
  'system:project:case-type:edit',
  'system:project:case-type:remove'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM (SELECT 6 AS role_id UNION ALL SELECT 8 UNION ALL SELECT 11) r
CROSS JOIN sys_menu m
WHERE m.perms = 'system:case:review'
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
);
