-- 兼容旧版 V0_6_2_9（仅飞书菜单）已执行、未建自定义字段表的库
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
  update_time DATETIME COMMENT '更新时间'
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_defect_field_key ON sys_project_defect_field (project_id, field_key);
CREATE INDEX IF NOT EXISTS idx_project_defect_field_project ON sys_project_defect_field (project_id);

ALTER TABLE sys_defect ADD COLUMN IF NOT EXISTS custom_fields JSON COMMENT '自定义字段值';

ALTER TABLE sys_project_defect_field ADD COLUMN IF NOT EXISTS is_system TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统预置字段';
