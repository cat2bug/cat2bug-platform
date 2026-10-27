-- 缺陷自定义页签 tab_sort：前端新建用 Date.now()，原 INT 溢出；改为 BIGINT（H2）。

ALTER TABLE sys_project_defect_tabs ALTER COLUMN tab_sort BIGINT;
