-- 缺陷表不再使用独立 remark 列（描述使用 defect_describe）；若曾执行过 0.6.3 加列则删除
SET @db := DATABASE();
SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_defect' AND COLUMN_NAME = 'remark'
);
SET @q := IF(@exists > 0,
  'ALTER TABLE sys_defect DROP COLUMN remark',
  'SELECT 1'
);
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;
