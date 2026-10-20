-- Cat2Bug 1.1.0 迁移 #46: 用例类型启用开关
-- H2。脚本可重复执行。

ALTER TABLE sys_case_type ADD COLUMN IF NOT EXISTS enabled INT NOT NULL DEFAULT 1;
