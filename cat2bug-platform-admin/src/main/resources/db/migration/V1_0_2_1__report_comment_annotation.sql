-- 报告批注：sys_comment 增加父评论与锚点字段（moduleType=report 时使用；defect_log 路径不读写新列，保持兼容）
ALTER TABLE sys_comment ADD COLUMN IF NOT EXISTS parent_id BIGINT COMMENT '父评论ID（NULL=主评论，非空=扁平回复挂主评）';
ALTER TABLE sys_comment ADD COLUMN IF NOT EXISTS anchor_kind VARCHAR(16) COMMENT '锚点类型：line=整行，selection=选中文字';
ALTER TABLE sys_comment ADD COLUMN IF NOT EXISTS quote CLOB COMMENT '锚点原文片段快照';
ALTER TABLE sys_comment ADD COLUMN IF NOT EXISTS context_before CLOB COMMENT '锚点前文片段快照（消歧用）';
ALTER TABLE sys_comment ADD COLUMN IF NOT EXISTS context_after CLOB COMMENT '锚点后文片段快照（消歧用）';
ALTER TABLE sys_comment ADD COLUMN IF NOT EXISTS anchor_hint VARCHAR(255) COMMENT '锚点辅助信息（如最近标题ID/块指纹，仅辅助）';

CREATE INDEX IF NOT EXISTS idx_sys_comment_parent_id ON sys_comment (parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_comment_module_correlation ON sys_comment (module_type, correlation_id);
