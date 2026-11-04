-- 团队功能模块页面设置（开关 / 显示名 / 文档上传上限）；空值读时视为全开
-- 系统上传上限（MB），可在参数设置中修改；团队文档上限不得超过该值

ALTER TABLE sys_team ADD COLUMN IF NOT EXISTS module_page_config CLOB;

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '上传文件大小上限(MB)', 'sys.upload.maxFileSizeMb', '500', 'Y', 'admin', CURRENT_TIMESTAMP,
       '业务上传单文件最大兆字节；团队文档上限不得超过该值'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.upload.maxFileSizeMb');
