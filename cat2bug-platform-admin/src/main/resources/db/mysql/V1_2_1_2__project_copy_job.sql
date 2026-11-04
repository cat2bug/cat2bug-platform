-- 新建项目时从源项目按勾选异步复制的任务（MySQL）

CREATE TABLE IF NOT EXISTS sys_project_copy_job (
  job_id VARCHAR(64) NOT NULL COMMENT '任务ID',
  source_project_id BIGINT DEFAULT NULL COMMENT '源项目ID',
  target_project_id BIGINT DEFAULT NULL COMMENT '新项目ID',
  state VARCHAR(32) NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/PARTIAL',
  percent INT NOT NULL DEFAULT 0 COMMENT '进度 0-100',
  selection_json LONGTEXT COMMENT '勾选 JSON',
  items_json LONGTEXT COMMENT '分项状态 JSON',
  estimated_bytes BIGINT DEFAULT NULL COMMENT '预估字节',
  actual_bytes BIGINT DEFAULT NULL COMMENT '实际复制字节',
  error_message LONGTEXT COMMENT '失败摘要',
  create_by VARCHAR(64) DEFAULT NULL COMMENT '发起人',
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (job_id),
  KEY idx_sys_project_copy_job_target (target_project_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新建项目复制任务';
