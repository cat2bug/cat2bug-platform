-- 报告批注：sys_comment 增加父评论与锚点字段（moduleType=report 时使用；defect_log 路径不读写新列，保持兼容）
ALTER TABLE `sys_comment`
  ADD COLUMN `parent_id` bigint NULL COMMENT '父评论ID（NULL=主评论，非空=扁平回复挂主评）' AFTER `correlation_id`,
  ADD COLUMN `anchor_kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '锚点类型：line=整行，selection=选中文字' AFTER `parent_id`,
  ADD COLUMN `quote` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '锚点原文片段快照' AFTER `anchor_kind`,
  ADD COLUMN `context_before` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '锚点前文片段快照（消歧用）' AFTER `quote`,
  ADD COLUMN `context_after` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '锚点后文片段快照（消歧用）' AFTER `context_before`,
  ADD COLUMN `anchor_hint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '锚点辅助信息（如最近标题ID/块指纹，仅辅助）' AFTER `context_after`;

CREATE INDEX `idx_sys_comment_parent_id` ON `sys_comment` (`parent_id`);
CREATE INDEX `idx_sys_comment_module_correlation` ON `sys_comment` (`module_type`, `correlation_id`);
