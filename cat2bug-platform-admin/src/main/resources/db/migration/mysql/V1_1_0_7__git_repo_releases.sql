-- Cat2Bug 1.1.0 迁移 #7: git_repo_releases
-- GitHub 风格发行版与附件元数据

CREATE TABLE IF NOT EXISTS sys_git_release (
  release_id BIGINT NOT NULL AUTO_INCREMENT,
  repository_id BIGINT NOT NULL,
  tag_name VARCHAR(128) NOT NULL,
  target_commit_id VARCHAR(64) DEFAULT NULL,
  title VARCHAR(255) NOT NULL,
  body MEDIUMTEXT,
  draft TINYINT NOT NULL DEFAULT 0,
  prerelease TINYINT NOT NULL DEFAULT 0,
  publisher_id BIGINT DEFAULT NULL,
  publisher_name VARCHAR(64) DEFAULT NULL,
  published_at DATETIME DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (release_id),
  UNIQUE KEY uk_sys_git_release_repo_tag (repository_id, tag_name),
  KEY idx_sys_git_release_repo (repository_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_git_release_asset (
  asset_id BIGINT NOT NULL AUTO_INCREMENT,
  release_id BIGINT NOT NULL,
  repository_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) DEFAULT NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  storage_path VARCHAR(512) NOT NULL,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (asset_id),
  KEY idx_sys_git_release_asset_release (release_id),
  KEY idx_sys_git_release_asset_repo (repository_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
