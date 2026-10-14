-- Cat2Bug 1.1.0 迁移 #5: git_repo_visibility
-- 仓库可见性：public=开源所有人可见，project=项目内部开源

ALTER TABLE sys_git_repository ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) DEFAULT 'project';

UPDATE sys_git_repository SET visibility = 'project' WHERE visibility IS NULL OR TRIM(visibility) = '';

ALTER TABLE sys_git_repository ALTER COLUMN visibility SET NOT NULL;
