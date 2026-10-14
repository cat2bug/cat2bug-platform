-- 飞书第三方应用菜单与角色权限（commit 364751d9；legacy 库可能缺失，幂等补全）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2141, '飞书', 2013, 10, 'feishu', 'system/project/other/feishu/index.vue', NULL, 1, 0, 'C', '1', '0', 'feishu:list', '#', 'admin', '2026-04-16 04:43:01', 'admin', '2026-04-16 04:44:24', '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2141);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2142, '查询飞书', 2141, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'feishu:query', '#', 'admin', '2026-04-16 04:55:57', '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2142);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2143, '保存飞书', 2141, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'feishu:save', '#', 'admin', '2026-04-16 04:56:21', '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2143);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 4, 2141 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 2141);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2141 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2141);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2141 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2141);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 12, 2141 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 12 AND `menu_id` = 2141);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 4, 2142 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 2142);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2142 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2142);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2142 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2142);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 12, 2142 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 12 AND `menu_id` = 2142);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 4, 2143 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 2143);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2143 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2143);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2143 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2143);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 12, 2143 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 12 AND `menu_id` = 2143);

-- 项目缺陷自定义字段定义 + 缺陷 custom_fields JSON 列
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
