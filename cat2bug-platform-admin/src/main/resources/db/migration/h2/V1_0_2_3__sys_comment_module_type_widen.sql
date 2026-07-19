-- report_history(14) 超出原 varchar(12)；扩列以支持报告历史回复等模块类型
ALTER TABLE sys_comment ALTER COLUMN module_type VARCHAR(32);
