-- 缺陷列表常用筛选/排序与日志聚合索引
CREATE INDEX idx_sys_defect_project_del_update ON sys_defect (project_id, del_flag, update_time);
CREATE INDEX idx_sys_defect_log_defect_type_time ON sys_defect_log (defect_id, defect_log_type, create_time);
CREATE INDEX idx_sys_user_defect_defect_user ON sys_user_defect (defect_id, user_id);
