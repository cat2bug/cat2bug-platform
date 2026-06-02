-- 项目缺陷自定义字段定义 + 缺陷 custom_fields JSON 列

SET @db := DATABASE();

CREATE TABLE IF NOT EXISTS sys_project_defect_field (
  field_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字段ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  field_key VARCHAR(64) NOT NULL COMMENT '字段键（项目内唯一）',
  field_label VARCHAR(128) NOT NULL COMMENT '显示名称',
  field_type VARCHAR(32) NOT NULL COMMENT '字段类型',
  max_length INT COMMENT '字符串最大长度',
  required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填',
  nullable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可空',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  type_config JSON COMMENT '类型扩展配置',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间',
  UNIQUE KEY uk_project_defect_field_key (project_id, field_key),
  KEY idx_project_defect_field_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目缺陷自定义字段定义';

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_defect' AND COLUMN_NAME = 'custom_fields'
);
SET @q := IF(@exists = 0,
  'ALTER TABLE `sys_defect` ADD COLUMN `custom_fields` JSON NULL COMMENT ''自定义字段值'' AFTER `del_flag`',
  'SELECT 1'
);
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;
