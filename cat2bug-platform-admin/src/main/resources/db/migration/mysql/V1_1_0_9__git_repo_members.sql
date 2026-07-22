-- Cat2Bug 1.1.0 迁移 #9: git_repo_members
-- 仓级 ACL：用户 × 仓库 × 角色（read|write|admin）
CREATE TABLE IF NOT EXISTS sys_git_repository_member (
  member_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  repository_id BIGINT NOT NULL COMMENT '仓库ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role VARCHAR(16) NOT NULL COMMENT 'read|write|admin',
  create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (member_id),
  UNIQUE KEY uk_sys_git_repo_member_repo_user (repository_id, user_id),
  KEY idx_sys_git_repo_member_repo (repository_id),
  KEY idx_sys_git_repo_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Git仓库成员ACL';

-- 历史仓补默认 admin：create_by 能解析为用户名时写入
INSERT INTO sys_git_repository_member (repository_id, user_id, role, create_by, create_time)
SELECT r.repository_id, u.user_id, 'admin', 'migration', NOW()
FROM sys_git_repository r
INNER JOIN sys_user u ON u.user_name = r.create_by
WHERE r.create_by IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_git_repository_member m
    WHERE m.repository_id = r.repository_id AND m.role = 'admin'
  );
