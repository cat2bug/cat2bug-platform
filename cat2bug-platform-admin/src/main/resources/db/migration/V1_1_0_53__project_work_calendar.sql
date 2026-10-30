-- 项目工作日历（H2）

CREATE TABLE IF NOT EXISTS sys_project_work_calendar (
  project_id BIGINT NOT NULL PRIMARY KEY,
  work_week VARCHAR(64) NOT NULL,
  exceptions CLOB,
  update_by VARCHAR(64),
  update_time TIMESTAMP
);
