-- 缺陷表不再使用独立 remark 列；若存在则删除（幂等，兼容未执行过 V0_6_3 的库）
SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND (UPPER(TABLE_NAME) = 'SYS_DEFECT' OR LOWER(TABLE_NAME) = 'sys_defect')
    AND (UPPER(COLUMN_NAME) = 'REMARK' OR LOWER(COLUMN_NAME) = 'remark')
);
SET @q := IF(@exists > 0, 'ALTER TABLE sys_defect DROP COLUMN remark', 'SELECT 1');
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;
