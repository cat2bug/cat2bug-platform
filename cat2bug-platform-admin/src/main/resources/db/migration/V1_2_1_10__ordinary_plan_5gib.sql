-- 普通团队每项目最大空间默认改为 5GiB
UPDATE sys_team_plan
SET max_project_bytes = 5368709120
WHERE plan_id = 1
   OR plan_name = '普通团队'
   OR is_system_default = 1;

ALTER TABLE sys_team_plan ALTER COLUMN max_project_bytes SET DEFAULT 5368709120;
