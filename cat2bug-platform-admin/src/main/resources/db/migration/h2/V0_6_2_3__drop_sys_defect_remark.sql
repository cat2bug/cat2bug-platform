-- 缺陷表不再使用独立 remark 列；若存在则删除（幂等）
ALTER TABLE sys_defect DROP COLUMN IF EXISTS remark;
