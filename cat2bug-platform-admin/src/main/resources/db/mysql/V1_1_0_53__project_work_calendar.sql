-- 项目工作日历（MySQL）

CREATE TABLE IF NOT EXISTS sys_project_work_calendar (
  project_id BIGINT NOT NULL COMMENT '项目ID',
  work_week VARCHAR(64) NOT NULL COMMENT '工作周 JSON，周一=1…周日=7，默认 [1,2,3,4,5]',
  exceptions JSON DEFAULT NULL COMMENT '例外日 YYYY-MM-DD → holiday|workday',
  update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目工作日历';
