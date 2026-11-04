-- 团队功能模块页面设置（开关 / 显示名 / 文档上传上限）；MySQL 幂等
-- 系统上传上限（MB），可在参数设置中修改

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_team' AND COLUMN_NAME = 'module_page_config'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE sys_team ADD COLUMN module_page_config JSON NULL COMMENT ''功能模块页面设置（开关/显示名/文档上传上限）'' AFTER defect_page_config',
  'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '上传文件大小上限(MB)', 'sys.upload.maxFileSizeMb', '500', 'Y', 'admin', CURRENT_TIMESTAMP,
       '业务上传单文件最大兆字节；团队文档上限不得超过该值'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.upload.maxFileSizeMb');
