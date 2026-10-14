-- Cat2Bug 1.1.0 迁移 #6: git_repo_collect
-- 用户关注（收藏）代码仓库

CREATE TABLE IF NOT EXISTS sys_user_git_repository (
  user_git_repository_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  repository_id BIGINT NOT NULL COMMENT '仓库ID',
  collect TINYINT NOT NULL DEFAULT 0 COMMENT '是否关注：1是 0否',
  create_by VARCHAR(64) COMMENT '创建者',
  create_time DATETIME COMMENT '创建时间',
  update_by VARCHAR(64) COMMENT '更新者',
  update_time DATETIME COMMENT '更新时间',
  UNIQUE KEY uk_sys_user_git_repository (user_id, repository_id),
  KEY idx_sys_user_git_repository_repo (repository_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注的代码仓库';
