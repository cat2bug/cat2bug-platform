-- Cat2Bug 1.1.0 迁移 #46: 用例类型启用开关
-- MySQL。脚本可重复执行。

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_case_type'
    AND COLUMN_NAME = 'enabled'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_case_type ADD COLUMN enabled INT NOT NULL DEFAULT 1 COMMENT ''是否启用 1启用 0停用''',
  'SELECT 1');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
