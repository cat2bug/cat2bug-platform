-- Cat2Bug 1.1.0 迁移 #4: team_project_path_keys
-- 团队/项目英文路径键；存量用 team-{id} / project-{id} 回填（冲突极低，可后台再改）

ALTER TABLE sys_team ADD COLUMN team_key VARCHAR(100) NULL COMMENT '英文路径键（全局唯一）' AFTER team_name;

UPDATE sys_team SET team_key = CONCAT('team-', team_id)
WHERE team_key IS NULL OR TRIM(team_key) = '';

ALTER TABLE sys_team MODIFY COLUMN team_key VARCHAR(100) NOT NULL COMMENT '英文路径键（全局唯一）';

CREATE UNIQUE INDEX uk_sys_team_team_key ON sys_team (team_key);

ALTER TABLE sys_project ADD COLUMN project_key VARCHAR(100) NULL COMMENT '英文路径键（团队内唯一）' AFTER project_name;

UPDATE sys_project SET project_key = CONCAT('project-', project_id)
WHERE project_key IS NULL OR TRIM(project_key) = '';

ALTER TABLE sys_project MODIFY COLUMN project_key VARCHAR(100) NOT NULL COMMENT '英文路径键（团队内唯一）';

CREATE UNIQUE INDEX uk_sys_project_team_project_key ON sys_project (team_id, project_key);
