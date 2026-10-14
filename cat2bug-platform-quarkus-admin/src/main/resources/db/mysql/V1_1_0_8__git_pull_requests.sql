-- Cat2Bug 1.1.0 迁移 #8: git_pull_requests
-- Pull Request 元数据与整单讨论评论
CREATE TABLE IF NOT EXISTS sys_git_pull_request (
  pull_request_id BIGINT NOT NULL AUTO_INCREMENT,
  repository_id BIGINT NOT NULL,
  number INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  body MEDIUMTEXT,
  source_branch VARCHAR(255) NOT NULL,
  target_branch VARCHAR(255) NOT NULL,
  source_sha VARCHAR(64) DEFAULT NULL,
  target_sha VARCHAR(64) DEFAULT NULL,
  state VARCHAR(16) NOT NULL DEFAULT 'open',
  author_id BIGINT NOT NULL,
  merged_by BIGINT DEFAULT NULL,
  merged_at DATETIME DEFAULT NULL,
  closed_at DATETIME DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (pull_request_id),
  UNIQUE KEY uk_sys_git_pull_request_repo_number (repository_id, number),
  KEY idx_sys_git_pull_request_repo (repository_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_git_pull_request_comment (
  comment_id BIGINT NOT NULL AUTO_INCREMENT,
  pull_request_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  body MEDIUMTEXT NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (comment_id),
  KEY idx_sys_git_pull_request_comment_pr (pull_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
