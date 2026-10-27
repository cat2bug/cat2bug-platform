-- 缺陷自定义页签 tab_sort：前端新建用 Date.now()（约 1.7e12），原 INT 溢出导致「服务器内部错误」。
-- 与工作台页签 tab_sort（BIGINT）对齐。脚本可重复执行。

SET @col_type := (
  SELECT DATA_TYPE
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_project_defect_tabs'
    AND COLUMN_NAME = 'tab_sort'
  LIMIT 1
);

SET @sql := IF(
  @col_type IS NOT NULL AND @col_type <> 'bigint',
  'ALTER TABLE `sys_project_defect_tabs` MODIFY COLUMN `tab_sort` BIGINT DEFAULT NULL COMMENT ''排序''',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
