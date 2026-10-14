-- 项目 API：启用状态与权限配置（与 SysProjectApi / Open API 鉴权对齐）
ALTER TABLE sys_project_api
  ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态' AFTER remark,
  ADD COLUMN features JSON NULL COMMENT '权限配置 JSON' AFTER enabled;
