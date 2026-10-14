-- 缺陷表增加逻辑删除标志；若 legacy 库已存在该列则跳过
SET @db := DATABASE();
SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_defect' AND COLUMN_NAME = 'del_flag'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_defect` ADD COLUMN `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）'' AFTER `sponsor`',
  'SELECT 1'
);
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;
