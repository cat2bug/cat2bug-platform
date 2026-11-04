-- 新建项目时从源项目按勾选异步复制的任务

CREATE TABLE IF NOT EXISTS sys_project_copy_job (
  job_id VARCHAR(64) NOT NULL PRIMARY KEY,
  source_project_id BIGINT,
  target_project_id BIGINT,
  state VARCHAR(32) NOT NULL,
  percent INT NOT NULL DEFAULT 0,
  selection_json CLOB,
  items_json CLOB,
  estimated_bytes BIGINT,
  actual_bytes BIGINT,
  error_message CLOB,
  create_by VARCHAR(64),
  create_time TIMESTAMP,
  update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_project_copy_job_target ON sys_project_copy_job (target_project_id, state);
