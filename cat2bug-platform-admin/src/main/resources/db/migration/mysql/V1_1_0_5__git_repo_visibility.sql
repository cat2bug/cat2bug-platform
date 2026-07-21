-- Cat2Bug 1.1.0 迁移 #5: git_repo_visibility
-- 仓库可见性：public=开源所有人可见，project=项目内部开源

ALTER TABLE sys_git_repository
  ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'project' COMMENT '可见性：public/project' AFTER default_branch;
