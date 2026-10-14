-- Cat2Bug 1.1.0 迁移 #40: access_token_oauth
-- 个人访问令牌、OAuth 客户端与授权码（只存哈希）
CREATE TABLE IF NOT EXISTS sys_user_access_token (
  token_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '令牌ID',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  name VARCHAR(128) NOT NULL COMMENT '名称',
  token_hash VARCHAR(64) NOT NULL COMMENT '令牌SHA-256',
  token_prefix VARCHAR(16) NOT NULL COMMENT '明文前缀(展示用)',
  source VARCHAR(16) NOT NULL COMMENT '来源 manual/oauth/oauth_refresh',
  client_id VARCHAR(64) DEFAULT NULL COMMENT 'OAuth客户端ID',
  grant_id VARCHAR(64) DEFAULT NULL COMMENT '同一次OAuth授权的令牌链',
  resource VARCHAR(500) DEFAULT NULL COMMENT 'RFC 8707 resource',
  expires_at DATETIME DEFAULT NULL COMMENT '过期时间',
  last_used_at DATETIME DEFAULT NULL COMMENT '最后使用时间',
  last_used_ip VARCHAR(128) DEFAULT NULL COMMENT '最后使用IP',
  revoked_at DATETIME DEFAULT NULL COMMENT '吊销时间',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (token_id),
  UNIQUE KEY uk_sys_user_access_token_hash (token_hash),
  KEY idx_sys_user_access_token_user (user_id, source),
  KEY idx_sys_user_access_token_grant (grant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户访问令牌';

CREATE TABLE IF NOT EXISTS sys_oauth_client (
  client_id VARCHAR(64) NOT NULL COMMENT '客户端ID',
  client_name VARCHAR(255) NOT NULL COMMENT '客户端名称',
  redirect_uris VARCHAR(4000) NOT NULL COMMENT '回调地址(换行分隔)',
  create_time DATETIME DEFAULT NULL COMMENT '注册时间',
  last_used_at DATETIME DEFAULT NULL COMMENT '最后使用时间',
  PRIMARY KEY (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth动态注册客户端';

CREATE TABLE IF NOT EXISTS sys_oauth_code (
  code_hash VARCHAR(64) NOT NULL COMMENT '授权码SHA-256',
  client_id VARCHAR(64) NOT NULL COMMENT '客户端ID',
  user_id BIGINT NOT NULL COMMENT '授权用户',
  redirect_uri VARCHAR(2000) NOT NULL COMMENT '回调地址',
  code_challenge VARCHAR(128) NOT NULL COMMENT 'PKCE S256 challenge',
  resource VARCHAR(500) DEFAULT NULL COMMENT 'RFC 8707 resource',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (code_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth授权码';
