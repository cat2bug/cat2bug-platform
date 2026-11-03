-- 私有端出站同步 Job / part（刷新与重启后可恢复）

CREATE TABLE IF NOT EXISTS sys_cloud_push_job (
  job_id VARCHAR(64) NOT NULL PRIMARY KEY,
  project_id BIGINT NOT NULL,
  pull_key VARCHAR(1024) NOT NULL,
  state VARCHAR(32) NOT NULL,
  percent INT NOT NULL DEFAULT 0,
  phase VARCHAR(255),
  error_message CLOB,
  message CLOB,
  session_id VARCHAR(64),
  selection_json CLOB,
  logs_json CLOB,
  packed_bytes BIGINT NOT NULL DEFAULT 0,
  max_total_bytes BIGINT NOT NULL,
  legacy_full SMALLINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP,
  update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_cloud_push_job_project ON sys_cloud_push_job (project_id, state);

CREATE TABLE IF NOT EXISTS sys_cloud_push_part (
  job_id VARCHAR(64) NOT NULL,
  part_id VARCHAR(128) NOT NULL,
  part_type VARCHAR(16) NOT NULL,
  ref_key VARCHAR(256),
  state VARCHAR(32) NOT NULL,
  expected_bytes BIGINT,
  actual_bytes BIGINT,
  sha256 VARCHAR(64),
  temp_path VARCHAR(1024),
  attempt_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(2000),
  sort_order INT NOT NULL DEFAULT 0,
  update_time TIMESTAMP,
  PRIMARY KEY (job_id, part_id)
);
