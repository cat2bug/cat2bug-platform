-- 团队缺陷页面设置（视图 / 色阶 / 分享）

ALTER TABLE sys_team ADD COLUMN IF NOT EXISTS defect_page_config CLOB;
