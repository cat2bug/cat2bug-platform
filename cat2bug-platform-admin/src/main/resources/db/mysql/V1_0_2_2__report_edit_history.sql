-- 报告正文编辑历史：每次成功保存且正文有变更时写入一条记录（谁、何时、当时正文快照）
CREATE TABLE IF NOT EXISTS `sys_report_history` (
  `history_id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `report_id` bigint NOT NULL COMMENT '报告ID',
  `report_description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '保存时的正文快照',
  `create_by_id` bigint NULL COMMENT '修改人ID',
  `create_time` datetime NULL COMMENT '修改时间',
  PRIMARY KEY (`history_id`) USING BTREE,
  KEY `idx_sys_report_history_report_id` (`report_id`) USING BTREE,
  KEY `idx_sys_report_history_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报告编辑历史';
