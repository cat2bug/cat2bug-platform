-- 新建团队未显式绑定时，库级默认普通团队套餐（plan_id=1）
UPDATE sys_team SET plan_id = 1 WHERE plan_id IS NULL;

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'plan_id'
);
SET @q := IF(@exists > 0,
  'ALTER TABLE sys_team MODIFY COLUMN plan_id BIGINT NOT NULL DEFAULT 1 COMMENT ''绑定套餐ID''',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;
