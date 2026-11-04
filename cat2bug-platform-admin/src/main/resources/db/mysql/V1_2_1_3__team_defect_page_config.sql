-- 团队缺陷页面设置（视图 / 色阶 / 分享）；MySQL 幂等

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'defect_page_config'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_team ADD COLUMN defect_page_config JSON NULL COMMENT ''缺陷页面设置（视图/色阶/分享）'' AFTER defect_builtin_field_config',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;
