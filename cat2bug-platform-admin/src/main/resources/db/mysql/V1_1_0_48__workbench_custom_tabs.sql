-- Cat2Bug 1.1.0 迁移 #48: 工作台自定义页签（私有 / 项目共享）
-- MySQL。脚本可重复执行。

CREATE TABLE IF NOT EXISTS sys_project_workbench_tabs (
  tab_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  owner_user_id BIGINT NOT NULL COMMENT '创建人用户ID',
  visibility VARCHAR(16) NOT NULL COMMENT 'private|project',
  tab_name VARCHAR(64) NOT NULL COMMENT '页签名称',
  preset VARCHAR(32) DEFAULT NULL COMMENT '创建时预设',
  config_json LONGTEXT COMMENT 'v3 rows JSON',
  tab_sort BIGINT DEFAULT NULL COMMENT '排序',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (tab_id),
  KEY idx_wb_tabs_project_visibility (project_id, visibility),
  KEY idx_wb_tabs_project_owner (project_id, owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目工作台自定义页签';
