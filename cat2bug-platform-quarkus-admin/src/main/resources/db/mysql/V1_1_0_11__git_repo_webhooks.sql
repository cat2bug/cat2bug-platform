-- Cat2Bug 1.1.0 迁移 #11: git_repo_webhooks
-- 仓级出站 Webhook 配置与投递日志
CREATE TABLE IF NOT EXISTS sys_git_repository_webhook (
  webhook_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Webhook ID',
  repository_id BIGINT NOT NULL COMMENT '仓库ID',
  url VARCHAR(2000) NOT NULL COMMENT '目标URL',
  alias VARCHAR(128) DEFAULT NULL COMMENT '别名',
  secret_cipher VARCHAR(512) NOT NULL COMMENT 'Secret密文',
  events VARCHAR(256) NOT NULL COMMENT '订阅事件JSON数组',
  active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (webhook_id),
  KEY idx_sys_git_repo_webhook_repo (repository_id),
  KEY idx_sys_git_repo_webhook_repo_active (repository_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Git仓库Webhook';

CREATE TABLE IF NOT EXISTS sys_git_webhook_delivery (
  delivery_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '投递ID',
  webhook_id BIGINT NOT NULL COMMENT 'Webhook ID',
  repository_id BIGINT NOT NULL COMMENT '仓库ID',
  event VARCHAR(64) NOT NULL COMMENT '事件名',
  delivery_guid VARCHAR(64) NOT NULL COMMENT '投递GUID',
  request_headers_summary VARCHAR(2000) DEFAULT NULL COMMENT '请求头摘要',
  request_body MEDIUMTEXT COMMENT '请求体(可截断)',
  response_status INT DEFAULT NULL COMMENT 'HTTP状态码',
  response_body_truncated MEDIUMTEXT COMMENT '响应摘要',
  duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
  success TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功',
  error_message VARCHAR(2000) DEFAULT NULL COMMENT '错误信息',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (delivery_id),
  UNIQUE KEY uk_sys_git_webhook_delivery_guid (delivery_guid),
  KEY idx_sys_git_webhook_delivery_webhook (webhook_id, create_time),
  KEY idx_sys_git_webhook_delivery_repo (repository_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Git仓库Webhook投递日志';
