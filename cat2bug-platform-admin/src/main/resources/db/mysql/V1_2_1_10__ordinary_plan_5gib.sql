-- 普通团队每项目最大空间默认改为 5GiB
UPDATE sys_team_plan
SET max_project_bytes = 5368709120
WHERE plan_id = 1
   OR plan_name = '普通团队'
   OR is_system_default = 1;

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_team_plan' AND COLUMN_NAME = 'max_project_bytes'
);
SET @q := IF(@exists > 0,
  'ALTER TABLE sys_team_plan MODIFY COLUMN max_project_bytes BIGINT NOT NULL DEFAULT 5368709120 COMMENT ''每项目最大空间字节''',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;
