-- 移除项目 Ollama 配置表中已不再使用的列；若不存在则跳过
SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_ai_module_config' AND COLUMN_NAME = 'business_module'
);
SET @q := IF(@exists > 0, 'ALTER TABLE sys_ai_module_config DROP COLUMN business_module', 'SELECT 1');
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_ai_module_config' AND COLUMN_NAME = 'image_module'
);
SET @q := IF(@exists > 0, 'ALTER TABLE sys_ai_module_config DROP COLUMN image_module', 'SELECT 1');
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;
