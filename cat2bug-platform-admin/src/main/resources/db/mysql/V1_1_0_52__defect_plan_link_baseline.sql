-- 缺陷甘特二期：计划依赖边 + 项目活跃基线快照（MySQL）

CREATE TABLE IF NOT EXISTS sys_defect_plan_link (
  link_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '依赖边ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  from_defect_id BIGINT NOT NULL COMMENT '前置缺陷ID',
  to_defect_id BIGINT NOT NULL COMMENT '后继缺陷ID',
  link_type VARCHAR(8) NOT NULL DEFAULT 'FS' COMMENT '依赖类型(FS)',
  create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (link_id),
  UNIQUE KEY uk_defect_plan_link (from_defect_id, to_defect_id, link_type),
  KEY idx_defect_plan_link_project (project_id),
  KEY idx_defect_plan_link_from (from_defect_id),
  KEY idx_defect_plan_link_to (to_defect_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷计划依赖边';

CREATE TABLE IF NOT EXISTS sys_defect_plan_baseline (
  baseline_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '基线ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  frozen_by VARCHAR(64) DEFAULT NULL COMMENT '冻结人',
  frozen_time DATETIME DEFAULT NULL COMMENT '冻结时间',
  PRIMARY KEY (baseline_id),
  UNIQUE KEY uk_defect_plan_baseline_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷计划活跃基线';

CREATE TABLE IF NOT EXISTS sys_defect_plan_baseline_item (
  item_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '基线条目ID',
  baseline_id BIGINT NOT NULL COMMENT '基线ID',
  defect_id BIGINT NOT NULL COMMENT '缺陷ID',
  plan_start_time DATETIME DEFAULT NULL COMMENT '基线计划开始',
  plan_end_time DATETIME DEFAULT NULL COMMENT '基线计划结束',
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_defect_plan_baseline_item (baseline_id, defect_id),
  KEY idx_defect_plan_baseline_item_base (baseline_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷计划基线条目';