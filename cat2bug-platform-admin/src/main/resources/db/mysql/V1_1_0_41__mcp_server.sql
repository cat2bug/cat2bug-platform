-- Cat2Bug 1.1.0 迁移 #41: mcp_server
-- 项目 MCP 设置、缺陷修复关联提交、MCP 幂等记录；操作日志与缺陷日志增加来源渠道与客户端
CREATE TABLE IF NOT EXISTS sys_project_mcp (
  project_id BIGINT NOT NULL COMMENT '项目ID',
  enabled INT NOT NULL DEFAULT 0 COMMENT '是否开启 MCP',
  modules VARCHAR(1000) NOT NULL COMMENT '已勾选模块键(JSON数组)',
  update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目MCP设置';

CREATE TABLE IF NOT EXISTS sys_defect_commit (
  commit_link_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  defect_id BIGINT NOT NULL COMMENT '缺陷ID',
  defect_log_id BIGINT DEFAULT NULL COMMENT '修复日志ID',
  repository_id BIGINT DEFAULT NULL COMMENT '托管仓库ID',
  commit_sha VARCHAR(64) NOT NULL COMMENT '提交SHA',
  commit_url VARCHAR(1000) DEFAULT NULL COMMENT '外部提交链接',
  commit_message VARCHAR(1000) DEFAULT NULL COMMENT '提交说明',
  commit_time DATETIME DEFAULT NULL COMMENT '提交时间',
  create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (commit_link_id),
  KEY idx_sys_defect_commit_defect (defect_id),
  KEY idx_sys_defect_commit_log (defect_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷修复关联提交';

CREATE TABLE IF NOT EXISTS sys_mcp_idempotency (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  tool VARCHAR(64) NOT NULL COMMENT '工具名',
  idem_key VARCHAR(128) NOT NULL COMMENT '幂等键',
  result_id VARCHAR(64) NOT NULL COMMENT '首次调用结果ID',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (user_id, tool, idem_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP写操作幂等记录';

SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_oper_log' AND COLUMN_NAME = 'oper_channel'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_oper_log ADD COLUMN oper_channel VARCHAR(16) DEFAULT NULL COMMENT ''来源渠道 web/mcp/api''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_oper_log' AND COLUMN_NAME = 'client_name'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_oper_log ADD COLUMN client_name VARCHAR(128) DEFAULT NULL COMMENT ''MCP客户端名称''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_defect_log' AND COLUMN_NAME = 'oper_channel'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_defect_log ADD COLUMN oper_channel VARCHAR(16) DEFAULT NULL COMMENT ''来源渠道 web/mcp/api''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_defect_log' AND COLUMN_NAME = 'client_name'
);
SET @q := IF(@exists = 0, 'ALTER TABLE sys_defect_log ADD COLUMN client_name VARCHAR(128) DEFAULT NULL COMMENT ''MCP客户端名称''', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;
