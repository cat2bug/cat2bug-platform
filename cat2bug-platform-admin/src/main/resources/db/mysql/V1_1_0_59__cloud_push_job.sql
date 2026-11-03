-- 私有端出站同步 Job / part（刷新与重启后可恢复）

CREATE TABLE IF NOT EXISTS sys_cloud_push_job (
  job_id VARCHAR(64) NOT NULL COMMENT '任务ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  pull_key VARCHAR(1024) NOT NULL COMMENT '同步密钥，仅服务端续传，接口不回传',
  state VARCHAR(32) NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED',
  percent INT NOT NULL DEFAULT 0 COMMENT '进度 0-100',
  phase VARCHAR(255) DEFAULT NULL COMMENT '阶段文案',
  error_message LONGTEXT COMMENT '失败原因',
  message LONGTEXT COMMENT '结果说明',
  session_id VARCHAR(64) DEFAULT NULL COMMENT '云站 push session',
  selection_json LONGTEXT COMMENT '勾选：gitSlugs/documentIds/legacyFull',
  logs_json LONGTEXT COMMENT '进度日志 JSON',
  packed_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '已打包字节',
  max_total_bytes BIGINT NOT NULL COMMENT '合计上限',
  legacy_full TINYINT NOT NULL DEFAULT 0 COMMENT '1=旧客户端未传勾选，包含全部可选内容',
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (job_id),
  KEY idx_sys_cloud_push_job_project (project_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出站云同步任务';

CREATE TABLE IF NOT EXISTS sys_cloud_push_part (
  job_id VARCHAR(64) NOT NULL COMMENT '任务ID',
  part_id VARCHAR(128) NOT NULL COMMENT 'core / doc-{id} / git-{slug}',
  part_type VARCHAR(16) NOT NULL COMMENT 'core/doc/git',
  ref_key VARCHAR(256) DEFAULT NULL COMMENT '文档ID或仓库slug',
  state VARCHAR(32) NOT NULL COMMENT 'PENDING/PACKING/UPLOADING/OK/FAILED',
  expected_bytes BIGINT DEFAULT NULL COMMENT '预估字节',
  actual_bytes BIGINT DEFAULT NULL COMMENT '分包文件字节',
  sha256 VARCHAR(64) DEFAULT NULL,
  temp_path VARCHAR(1024) DEFAULT NULL COMMENT '本地临时zip',
  attempt_count INT NOT NULL DEFAULT 0 COMMENT '已上传次数',
  error_message VARCHAR(2000) DEFAULT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (job_id, part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出站云同步分包';
