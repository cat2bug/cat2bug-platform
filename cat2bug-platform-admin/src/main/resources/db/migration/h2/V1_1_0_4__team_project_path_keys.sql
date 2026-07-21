-- Cat2Bug 1.1.0 迁移 #4: team_project_path_keys
-- 团队/项目英文路径键；存量用 team-{id} / project-{id} 回填（冲突极低，可后台再改）

ALTER TABLE sys_team ADD COLUMN IF NOT EXISTS team_key VARCHAR(100);

UPDATE sys_team SET team_key = CONCAT('team-', CAST(team_id AS VARCHAR(20)))
WHERE team_key IS NULL OR TRIM(team_key) = '';

ALTER TABLE sys_team ALTER COLUMN team_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_team_team_key ON sys_team (team_key);

ALTER TABLE sys_project ADD COLUMN IF NOT EXISTS project_key VARCHAR(100);

UPDATE sys_project SET project_key = CONCAT('project-', CAST(project_id AS VARCHAR(20)))
WHERE project_key IS NULL OR TRIM(project_key) = '';

ALTER TABLE sys_project ALTER COLUMN project_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_project_team_project_key ON sys_project (team_id, project_key);
