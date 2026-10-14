-- report_history(14) 超出原 varchar(12)；扩列以支持报告历史回复等模块类型
ALTER TABLE `sys_comment`
  MODIFY COLUMN `module_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属模块(defect_log/report/report_history 等)';
