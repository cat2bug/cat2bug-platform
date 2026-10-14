ALTER TABLE sys_project_defect_field
  ADD COLUMN default_value JSON NULL COMMENT '新建缺陷时的默认值' AFTER type_config;
