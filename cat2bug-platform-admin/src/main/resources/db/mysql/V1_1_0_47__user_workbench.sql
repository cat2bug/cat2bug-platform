-- Cat2Bug 1.1.0 迁移 #47: 用户×项目工作台布局
-- MySQL。脚本可重复执行。

CREATE TABLE IF NOT EXISTS sys_user_workbench (
  workbench_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  config_json LONGTEXT COMMENT '布局 JSON',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (workbench_id),
  UNIQUE KEY uk_sys_user_workbench_user_project (user_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户项目工作台布局';
