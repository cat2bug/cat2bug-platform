DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
							 `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
							 `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
							 `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
							 `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
							 `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
							 `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
							 `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
							 `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
							 `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
							 `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
							 `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
							 `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
							 `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
							 `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
							 `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
							 `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
							 `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
							 `remark` varchar(500) DEFAULT NULL COMMENT '备注'
);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1, 'sys_project', '项目', NULL, NULL
	   , 'SysProject', 'crud', 'com.cat2bug.system', 'system', 'project'
	   , '项目', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-12 16:22:36', '', '2023-11-12 16:25:16', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2, 'sys_team', '团队表', NULL, NULL
	   , 'SysTeam', 'crud', 'com.cat2bug.system', 'system', 'team'
	   , '团队', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-13 03:31:36', '', '2023-11-13 12:44:45', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (3, 'sys_user_config', '用户配置表', NULL, NULL
	   , 'SysUserConfig', 'crud', 'com.cat2bug.system', 'system', 'config'
	   , '用户配置', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-16 09:57:26', '', '2023-11-16 09:59:22', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (4, 'sys_user_team', '用户团队角色表', NULL, NULL
	   , 'SysUserTeam', 'crud', 'com.cat2bug.system', 'system', 'UserTeamRole'
	   , '用户团队角色', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-19 17:47:30', '', '2023-11-20 10:57:34', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (5, 'sys_user_team_role', '用户团队角色表', NULL, NULL
	   , 'SysUserTeamRole', 'crud', 'com.cat2bug.system', 'system', 'role'
	   , '用户团队角色', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-20 10:57:47', '', '2023-11-20 10:58:18', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6, 'sys_user_project', '用户项目表', NULL, NULL
	   , 'SysUserProject', 'crud', 'com.cat2bug.system', 'system', 'project'
	   , '用户项目', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-22 12:11:49', '', '2023-11-22 12:14:36', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (7, 'sys_user_project_role', '用户项目角色表', NULL, NULL
	   , 'SysUserProjectRole', 'crud', 'com.cat2bug.system', 'system', 'role'
	   , '用户项目角色', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-22 12:11:49', '', '2023-11-22 12:15:09', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (8, 'sys_defect', '缺陷表', NULL, NULL
	   , 'SysDefect', 'crud', 'com.cat2bug.system', 'system', 'defect'
	   , '缺陷', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-23 12:03:56', '', '2023-11-23 12:47:27', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (9, 'sys_defect_log', '缺陷日志表', NULL, NULL
	   , 'SysDefectLog', 'crud', 'com.cat2bug.system', 'system', 'log'
	   , '缺陷日志', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-11-23 12:03:56', '', '2023-11-23 12:45:58', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (10, 'sys_module', '模块表', '', ''
	   , 'SysModule', 'tree', 'com.cat2bug.system', 'system', 'module'
	   , '模块', 'yuzhantao', '0', '/', '{"treeCode":"module_id","treeName":"module_name","treeParentCode":"module_pid"}'
	   , 'admin', '2023-11-25 17:17:44', '', '2023-11-25 17:18:44', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (11, 'sys_temp_file', '临时文件', NULL, NULL
	   , 'SysTempFile', 'crud', 'com.cat2bug.system', 'system', 'temp'
	   , '临时文件', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-12-07 04:17:30', '', '2023-12-07 04:19:23', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (12, 'sys_screen_size', '屏幕尺寸', NULL, NULL
	   , 'SysScreenSize', 'crud', 'com.cat2bug.system', 'system', 'ScreenSize'
	   , '屏幕尺寸', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2023-12-09 19:15:04', '', '2023-12-09 19:15:49', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (13, 'sys_user_defect', '用户缺陷表', NULL, NULL
	   , 'SysUserDefect', 'crud', 'com.cat2bug.system', 'system', 'defect'
	   , '用户缺陷', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2024-01-10 08:26:01', '', '2024-01-10 08:27:03', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (14, 'sys_user_statistic_template', '用户统计模版', NULL, NULL
	   , 'SysUserStatisticTemplate', 'crud', 'com.cat2bug.system', 'system', 'statistic'
	   , '用户统计模版', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2024-01-23 18:01:04', '', '2024-01-23 18:16:40', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (15, 'sys_case', '测试用例表', NULL, NULL
	   , 'SysCase', 'crud', 'com.cat2bug.system', 'system', 'case'
	   , '测试用例', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2024-01-27 16:04:48', '', '2024-01-27 16:10:36', NULL);
INSERT INTO `gen_table` (`table_id`, `table_name`, `table_comment`, `sub_table_name`, `sub_table_fk_name`
						, `class_name`, `tpl_category`, `package_name`, `module_name`, `business_name`
						, `function_name`, `function_author`, `gen_type`, `gen_path`, `options`
						, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (16, 'sys_project_api', '项目API表', NULL, NULL
	   , 'SysProjectApi', 'crud', 'com.cat2bug.system', 'system', 'api'
	   , '项目API', 'yuzhantao', '0', '/', '{}'
	   , 'admin', '2024-02-10 20:33:46', '', '2024-02-10 20:36:09', NULL);

DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
									`column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
									`table_id` bigint DEFAULT NULL COMMENT '归属表编号',
									`column_name` varchar(200) DEFAULT NULL COMMENT '列名称',
									`column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
									`column_type` varchar(100) DEFAULT NULL COMMENT '列类型',
									`java_type` varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
									`java_field` varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
									`is_pk` char(1) DEFAULT NULL COMMENT '是否主键（1是）',
									`is_increment` char(1) DEFAULT NULL COMMENT '是否自增（1是）',
									`is_required` char(1) DEFAULT NULL COMMENT '是否必填（1是）',
									`is_insert` char(1) DEFAULT NULL COMMENT '是否为插入字段（1是）',
									`is_edit` char(1) DEFAULT NULL COMMENT '是否编辑字段（1是）',
									`is_list` char(1) DEFAULT NULL COMMENT '是否列表字段（1是）',
									`is_query` char(1) DEFAULT NULL COMMENT '是否查询字段（1是）',
									`query_type` varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
									`html_type` varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
									`dict_type` varchar(200) DEFAULT '' COMMENT '字典类型',
									`sort` int DEFAULT NULL COMMENT '排序',
									`create_by` varchar(64) DEFAULT '' COMMENT '创建者',
									`create_time` datetime DEFAULT NULL COMMENT '创建时间',
									`update_by` varchar(64) DEFAULT '' COMMENT '更新者',
									`update_time` datetime DEFAULT NULL COMMENT '更新时间'
);

INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (1, 1, 'project_id', '项目id', 'bigint(20)'
	   , 'Long', 'projectId', '1', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (2, 1, 'project_name', '项目名称', 'varchar(64)'
	   , 'String', 'projectName', '0', '0', '1'
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 2, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (3, 1, 'project_icon', '项目图标地址', 'varchar(255)'
	   , 'String', 'projectIcon', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 3, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (4, 1, 'project_introduce', '项目介绍', 'varchar(255)'
	   , 'String', 'projectIntroduce', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 4, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (5, 1, 'create_by', '创建者', 'varchar(64)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '0', NULL, '1', '1', 'LIKE'
	   , 'input', '', 5, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (6, 1, 'create_time', '创建时间', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '0', NULL, '1', NULL, 'EQ'
	   , 'datetime', '', 6, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (7, 1, 'update_by', '更新者', 'varchar(64)'
	   , 'String', 'updateBy', '0', '0', NULL
	   , '0', '0', '1', NULL, 'EQ'
	   , 'input', '', 7, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (8, 1, 'update_time', '更新时间', 'datetime'
	   , 'Date', 'updateTime', '0', '0', NULL
	   , '0', '0', '1', NULL, 'EQ'
	   , 'datetime', '', 8, 'admin', '2023-11-12 16:22:36'
	   , '', '2023-11-12 16:25:16');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (9, 2, 'team_id', '团队id', 'bigint(20)'
	   , 'Long', 'teamId', '1', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (10, 2, 'team_name', '团队名称', 'varchar(64)'
	   , 'String', 'teamName', '0', '0', NULL
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 2, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (11, 2, 'team_icon', '团队图标', 'varchar(255)'
	   , 'String', 'teamIcon', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'imageUpload', '', 3, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (12, 2, 'create_by', '创建者', 'varchar(64)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '0', NULL, '1', NULL, 'EQ'
	   , 'input', '', 4, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (13, 2, 'create_time', '创建时间', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '0', NULL, '1', NULL, 'EQ'
	   , 'datetime', '', 5, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (14, 2, 'update_by', '更新者', 'varchar(64)'
	   , 'String', 'updateBy', '0', '0', NULL
	   , '0', '0', '1', NULL, 'EQ'
	   , 'input', '', 6, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (15, 2, 'update_time', '更新时间', 'datetime'
	   , 'Date', 'updateTime', '0', '0', NULL
	   , '0', '0', '1', NULL, 'EQ'
	   , 'datetime', '', 7, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (16, 2, 'introduce', '团队介绍', 'varchar(255)'
	   , 'String', 'introduce', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 8, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (17, 2, 'is_del', '是否删除', 'char(1)'
	   , 'String', 'isDel', '0', '0', NULL
	   , '0', '0', '0', '0', 'EQ'
	   , 'input', '', 9, 'admin', '2023-11-13 03:31:36'
	   , '', '2023-11-13 12:44:45');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (18, 3, 'user_config_id', '用户配置id', 'bigint(20)'
	   , 'Long', 'userConfigId', '1', '1', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-16 09:57:26'
	   , '', '2023-11-16 09:59:22');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (19, 3, 'current_team_id', '当前团队id', 'bigint(20)'
	   , 'Long', 'currentTeamId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2023-11-16 09:57:26'
	   , '', '2023-11-16 09:59:22');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (20, 3, 'user_id', '用户id', 'bigint(20)'
	   , 'Long', 'userId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, '', '2023-11-16 09:58:52'
	   , '', '2023-11-16 09:59:22');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (21, 4, 'user_team_id', '用户团队id', 'bigint(20)'
	   , 'Long', 'userTeamId', '1', '1', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (22, 4, 'user_id', '用户id', 'bigint(20)'
	   , 'Long', 'userId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (23, 4, 'team_id', '团队id', 'bigint(20)'
	   , 'Long', 'teamId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (24, 4, 'team_role_id', '团队角色id', 'bigint(20)'
	   , 'Long', 'teamRoleId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 4, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (25, 4, 'create_by', '创建者', 'varchar(64)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 5, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (26, 4, 'create_time', '创建时间', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'datetime', '', 6, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (27, 4, 'update_by', '更新者', 'varchar(64)'
	   , 'String', 'updateBy', '0', '0', NULL
	   , '0', '0', NULL, NULL, 'EQ'
	   , 'input', '', 7, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (28, 4, 'update_time', '更新时间', 'datetime'
	   , 'Date', 'updateTime', '0', '0', NULL
	   , '0', '0', NULL, NULL, 'EQ'
	   , 'datetime', '', 8, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (29, 4, 'lock', '是否锁定', 'tinyint(1)'
	   , 'Integer', 'lock', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'checkbox', '', 9, 'admin', '2023-11-19 17:47:30'
	   , '', '2023-11-20 10:57:34');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (30, 5, 'user_team_role_id', '用户团队角色id', 'bigint(20)'
	   , 'Long', 'userTeamRoleId', '1', '1', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-20 10:57:47'
	   , '', '2023-11-20 10:58:19');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (31, 5, 'user_team_id', '用户团队id', 'bigint(20)'
	   , 'Long', 'userTeamId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2023-11-20 10:57:47'
	   , '', '2023-11-20 10:58:19');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (32, 5, 'role_id', '角色id', 'bigint(20)'
	   , 'Long', 'roleId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, 'admin', '2023-11-20 10:57:47'
	   , '', '2023-11-20 10:58:19');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (33, 6, 'user_project_id', '用户项目id', 'bigint(20)'
	   , 'Long', 'userProjectId', '1', '1', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (34, 6, 'user_id', '用户id', 'bigint(20)'
	   , 'Long', 'userId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (35, 6, 'project_id', '项目id', 'bigint(20)'
	   , 'Long', 'projectId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (36, 6, 'create_by', '创建者', 'varchar(64)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 4, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (37, 6, 'create_time', '创建时间', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'datetime', '', 5, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (38, 6, 'update_by', '更新者', 'varchar(64)'
	   , 'String', 'updateBy', '0', '0', NULL
	   , '0', '0', NULL, NULL, 'EQ'
	   , 'input', '', 6, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (39, 6, 'update_time', '更新时间', 'datetime'
	   , 'Date', 'updateTime', '0', '0', NULL
	   , '0', '0', NULL, NULL, 'EQ'
	   , 'datetime', '', 7, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (40, 6, 'project_lock', '是否锁定', 'tinyint(1)'
	   , 'Integer', 'projectLock', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 8, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:14:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (41, 7, 'user_project_role_id', '用户项目角色id', 'bigint(20)'
	   , 'Long', 'userProjectRoleId', '1', '1', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:15:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (42, 7, 'user_project_id', '用户项目id', 'bigint(20)'
	   , 'Long', 'userProjectId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:15:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (43, 7, 'role_id', '角色id', 'bigint(20)'
	   , 'Long', 'roleId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, 'admin', '2023-11-22 12:11:49'
	   , '', '2023-11-22 12:15:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (44, 8, 'defect_id', '缺陷id
', 'bigint(20)'
	   , 'Long', 'defectId', '1', '1', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (45, 8, 'defect_type', '缺陷类型
', 'int(1)'
	   , 'Integer', 'defectType', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 2, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (46, 8, 'defect_name', '缺陷标题', 'varchar(128)'
	   , 'String', 'defectName', '0', '0', '1'
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 3, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (47, 8, 'defect_describe', '缺陷描述', 'blob'
	   , 'String', 'defectDescribe', '0', '0', '1'
	   , '1', '1', '0', '0', 'EQ'
	   , 'editor', '', 4, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (48, 8, 'annex_urls', '附件', 'json'
	   , 'String', 'annexUrls', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'fileUpload', '', 5, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (49, 8, 'project_id', '项目id', 'bigint(20)'
	   , 'Long', 'projectId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 6, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (50, 8, 'test_plan_id', '测试计划id', 'bigint(20)'
	   , 'Long', 'testPlanId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 7, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (51, 8, 'case_id', '测试用例id', 'bigint(20)'
	   , 'Long', 'caseId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 8, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (52, 8, 'data_sources', '数据来源', 'varchar(10)'
	   , 'Integer', 'dataSources', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 9, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (53, 8, 'data_sources_params', '数据来源相关参数', 'varchar(255)'
	   , 'String', 'dataSourcesParams', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 10, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (54, 8, 'module_id', '测试模块id', 'bigint(20)'
	   , 'Long', 'moduleId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 11, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (55, 8, 'module_version', '版本', 'varchar(128)'
	   , 'String', 'moduleVersion', '0', '0', NULL
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 12, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (56, 8, 'create_by', '创建者', 'varchar(64)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '0', NULL, '1', '1', 'EQ'
	   , 'input', '', 13, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (57, 8, 'update_time', '更新时间', 'datetime'
	   , 'Date', 'updateTime', '0', '0', NULL
	   , '0', '0', '1', '1', 'BETWEEN'
	   , 'datetime', '', 14, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (58, 8, 'create_time', '创建时间', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '0', NULL, '1', '1', 'BETWEEN'
	   , 'datetime', '', 15, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (59, 8, 'update_by', '更新者', 'varchar(64)'
	   , 'String', 'updateBy', '0', '0', NULL
	   , '0', '0', '1', '1', 'EQ'
	   , 'input', '', 16, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (60, 8, 'defect_state', '缺陷状态', 'int(1)'
	   , 'Integer', 'defectState', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 17, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (61, 8, 'case_step_id', '用例步骤id', 'bigint(20)'
	   , 'Long', 'caseStepId', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'select', '', 18, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (62, 8, 'handle_by', '处理人id', 'varchar(64)'
	   , 'String', 'handleBy', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 19, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (63, 8, 'handle_time', '处理时间', 'datetime'
	   , 'Date', 'handleTime', '0', '0', NULL
	   , '1', '1', '1', '1', 'BETWEEN'
	   , 'datetime', '', 20, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (64, 8, 'defect_level', '缺陷等级', 'varchar(10)'
	   , 'String', 'defectLevel', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 21, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:47:27');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (65, 9, 'defect_log_id', '缺陷日志id', 'bigint(20)'
	   , 'Long', 'defectLogId', '1', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (66, 9, 'defect_log_describe', '缺陷日志的描述', 'blob'
	   , 'String', 'defectLogDescribe', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'editor', '', 2, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (67, 9, 'create_by', '创建人', 'varchar(64)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 3, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (68, 9, 'create_time', '创建时间', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'datetime', '', 4, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (69, 9, 'defect_log_type', '处理类型(转发\\评论\\关闭)', 'int(2)'
	   , 'Integer', 'defectLogType', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 5, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (70, 9, 'receive_by', '缺陷接收人', 'varchar(64)'
	   , 'String', 'receiveBy', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 6, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (71, 9, 'annex_urls', '附件集合', 'json'
	   , 'String', 'annexUrls', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'fileUpload', '', 7, 'admin', '2023-11-23 12:03:56'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (72, 9, 'defect_id', '缺陷id', 'bigint(20)'
	   , 'Long', 'defectId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 8, 'admin', '2023-11-23 12:03:57'
	   , '', '2023-11-23 12:45:58');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (73, 10, 'module_id', '模块id', 'bigint(20)'
	   , 'Long', 'moduleId', '1', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-11-25 17:17:44'
	   , '', '2023-11-25 17:18:44');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (74, 10, 'module_pid', '父模块id', 'bigint(20)'
	   , 'Long', 'modulePid', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2023-11-25 17:17:44'
	   , '', '2023-11-25 17:18:44');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (75, 10, 'module_name', '模块名称', 'varchar(128)'
	   , 'String', 'moduleName', '0', '0', NULL
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 3, 'admin', '2023-11-25 17:17:44'
	   , '', '2023-11-25 17:18:44');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (76, 10, 'remark', '备注', 'varchar(255)'
	   , 'String', 'remark', '0', '0', NULL
	   , '1', '1', '1', NULL, 'EQ'
	   , 'input', '', 4, 'admin', '2023-11-25 17:17:44'
	   , '', '2023-11-25 17:18:44');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (77, 10, 'project_id', '项目id', 'bigint(20)'
	   , 'Long', 'projectId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 5, 'admin', '2023-11-25 17:17:44'
	   , '', '2023-11-25 17:18:44');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (78, 11, 'file_id', '文件id', 'bigint'
	   , 'Long', 'fileId', '1', '1', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-12-07 04:17:31'
	   , '', '2023-12-07 04:19:23');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (79, 11, 'file_name', '文件名', 'varchar(64)'
	   , 'String', 'fileName', '0', '0', NULL
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 2, 'admin', '2023-12-07 04:17:31'
	   , '', '2023-12-07 04:19:23');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (80, 11, 'src_url', '来源地址', 'varchar(255)'
	   , 'String', 'srcUrl', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 3, 'admin', '2023-12-07 04:17:31'
	   , '', '2023-12-07 04:19:23');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (81, 11, 'create_by', '创建人', 'varchar(64)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '1', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 4, 'admin', '2023-12-07 04:17:31'
	   , '', '2023-12-07 04:19:23');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (82, 11, 'create_time', '创建时间', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '1', NULL, NULL, '1', 'BETWEEN'
	   , 'datetime', '', 5, 'admin', '2023-12-07 04:17:31'
	   , '', '2023-12-07 04:19:23');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (83, 11, 'file_type', '文件类型', 'int(10)'
	   , 'Integer', 'fileType', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 6, 'admin', '2023-12-07 04:17:31'
	   , '', '2023-12-07 04:19:23');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (84, 11, 'file_url', '文件地址', 'varchar(255)'
	   , 'String', 'fileUrl', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 7, 'admin', '2023-12-07 04:17:31'
	   , '', '2023-12-07 04:19:23');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (85, 12, 'screen_size_id', '屏幕尺寸id', 'bigint'
	   , 'Long', 'screenSizeId', '1', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2023-12-09 19:15:04'
	   , '', '2023-12-09 19:15:49');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (86, 12, 'name', '名称', 'varchar(32)'
	   , 'String', 'name', '0', '0', '1'
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 2, 'admin', '2023-12-09 19:15:04'
	   , '', '2023-12-09 19:15:49');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (87, 12, 'width', '宽', 'int'
	   , 'Long', 'width', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 3, 'admin', '2023-12-09 19:15:04'
	   , '', '2023-12-09 19:15:49');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (88, 12, 'height', '高', 'int'
	   , 'Long', 'height', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 4, 'admin', '2023-12-09 19:15:04'
	   , '', '2023-12-09 19:15:49');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (89, 12, 'remark', '备注', 'varchar(255)'
	   , 'String', 'remark', '0', '0', NULL
	   , '1', '1', '1', NULL, 'EQ'
	   , 'input', '', 5, 'admin', '2023-12-09 19:15:04'
	   , '', '2023-12-09 19:15:49');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (90, 13, 'user_defect_id', '用户缺陷id', 'bigint'
	   , 'Long', 'userDefectId', '1', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2024-01-10 08:26:01'
	   , '', '2024-01-10 08:27:03');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (91, 13, 'defect_id', '缺陷id', 'bigint'
	   , 'Long', 'defectId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2024-01-10 08:26:01'
	   , '', '2024-01-10 08:27:03');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (92, 13, 'user_id', '用户id', 'bigint'
	   , 'Long', 'userId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, 'admin', '2024-01-10 08:26:01'
	   , '', '2024-01-10 08:27:03');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (93, 13, 'collect', '是否收藏', 'tinyint(1)'
	   , 'Integer', 'collect', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 4, 'admin', '2024-01-10 08:26:01'
	   , '', '2024-01-10 08:27:03');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (94, 14, 'statistic_template_id', '统计模版id', 'bigint'
	   , 'Long', 'statisticTemplateId', '1', '0', NULL
	   , '0', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2024-01-23 18:01:04'
	   , '', '2024-01-23 18:16:40');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (95, 14, 'statistic_templat_code', '统计模版编码', 'varchar(255)'
	   , 'String', 'statisticTemplatCode', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2024-01-23 18:01:04'
	   , '', '2024-01-23 18:16:40');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (96, 14, 'module_type', '模型类型', 'int'
	   , 'Long', 'moduleType', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 3, 'admin', '2024-01-23 18:01:04'
	   , '', '2024-01-23 18:16:40');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (97, 14, 'project_id', '项目id', 'bigint'
	   , 'Long', 'projectId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 4, 'admin', '2024-01-23 18:01:04'
	   , '', '2024-01-23 18:16:40');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (98, 14, 'user_id', '用户id', 'bigint'
	   , 'Long', 'userId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 5, 'admin', '2024-01-23 18:01:04'
	   , '', '2024-01-23 18:16:40');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (99, 14, 'statistic_templat_config', '统计模版配置', 'json'
	   , 'String', 'statisticTemplatConfig', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , NULL, '', 6, 'admin', '2024-01-23 18:01:04'
	   , '', '2024-01-23 18:16:40');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (100, 15, 'case_id', '测试用例', 'bigint'
	   , 'Long', 'caseId', '1', '0', NULL
	   , '1', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (101, 15, 'case_name', '用例名称', 'varchar(255)'
	   , 'String', 'caseName', '0', '0', NULL
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 2, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (102, 15, 'module_id', '模块id', 'bigint'
	   , 'Long', 'moduleId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (103, 15, 'case_type', '用例类型', 'int'
	   , 'Long', 'caseType', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'select', '', 4, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (104, 15, 'case_expect', '预期', 'varchar(255)'
	   , 'String', 'caseExpect', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 5, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (105, 15, 'case_step', '步骤', 'json'
	   , 'String', 'caseStep', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , NULL, '', 6, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (106, 15, 'case_level', '用例级别', 'int'
	   , 'Long', 'caseLevel', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 7, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (107, 15, 'case_preconditions', '前置条件', 'varchar(255)'
	   , 'String', 'casePreconditions', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 8, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (108, 15, 'create_by', '', 'varchar(50)'
	   , 'String', 'createBy', '0', '0', NULL
	   , '1', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 9, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (109, 15, 'create_time', '', 'datetime'
	   , 'Date', 'createTime', '0', '0', NULL
	   , '1', NULL, NULL, NULL, 'EQ'
	   , 'datetime', '', 10, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (110, 15, 'update_by', '', 'varchar(50)'
	   , 'String', 'updateBy', '0', '0', NULL
	   , '1', '1', NULL, NULL, 'EQ'
	   , 'input', '', 11, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (111, 15, 'update_time', '', 'datetime'
	   , 'Date', 'updateTime', '0', '0', NULL
	   , '1', '1', NULL, NULL, 'EQ'
	   , 'datetime', '', 12, 'admin', '2024-01-27 16:04:48'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (112, 15, 'case_num', '用例号码', 'bigint'
	   , 'Long', 'caseNum', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 13, '', '2024-01-27 16:07:19'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (113, 15, 'project_id', '项目编号', 'bigint'
	   , 'Long', 'projectId', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 14, '', '2024-01-27 16:10:05'
	   , '', '2024-01-27 16:10:36');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (114, 16, 'api_id', 'API_KEY', 'varchar(32)'
	   , 'String', 'apiId', '1', '0', NULL
	   , '1', NULL, NULL, NULL, 'EQ'
	   , 'input', '', 1, 'admin', '2024-02-10 20:33:46'
	   , '', '2024-02-10 20:36:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (115, 16, 'project_id', '项目id', 'bigint'
	   , 'Long', 'projectId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 2, 'admin', '2024-02-10 20:33:46'
	   , '', '2024-02-10 20:36:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (116, 16, 'user_id', '用户id', 'bigint'
	   , 'Long', 'userId', '0', '0', '1'
	   , '1', '1', '1', '1', 'EQ'
	   , 'input', '', 3, 'admin', '2024-02-10 20:33:46'
	   , '', '2024-02-10 20:36:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (117, 16, 'white_list', '白名单', 'json'
	   , 'String', 'whiteList', '0', '0', NULL
	   , '1', '1', '1', '0', 'EQ'
	   , 'input', '', 4, 'admin', '2024-02-10 20:33:46'
	   , '', '2024-02-10 20:36:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (118, 16, 'expire_time', '有效时间', 'datetime'
	   , 'Date', 'expireTime', '0', '0', NULL
	   , '1', '1', '1', '1', 'EQ'
	   , 'datetime', '', 5, 'admin', '2024-02-10 20:33:46'
	   , '', '2024-02-10 20:36:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (119, 16, 'remark', '备注', 'varchar(255)'
	   , 'String', 'remark', '0', '0', NULL
	   , '1', '1', '1', NULL, 'EQ'
	   , 'input', '', 6, 'admin', '2024-02-10 20:33:46'
	   , '', '2024-02-10 20:36:09');
INSERT INTO `gen_table_column` (`column_id`, `table_id`, `column_name`, `column_comment`, `column_type`
							   , `java_type`, `java_field`, `is_pk`, `is_increment`, `is_required`
							   , `is_insert`, `is_edit`, `is_list`, `is_query`, `query_type`
							   , `html_type`, `dict_type`, `sort`, `create_by`, `create_time`
							   , `update_by`, `update_time`)
VALUES (120, 16, 'api_name', 'API名称', 'varchar(32)'
	   , 'String', 'apiName', '0', '0', '1'
	   , '1', '1', '1', '1', 'LIKE'
	   , 'input', '', 7, 'admin', '2024-02-10 20:33:46'
	   , '', '2024-02-10 20:36:09');

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers` (
									  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
									  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
									  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
									  `blob_data` blob COMMENT '存放持久化Trigger对象'
);


DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars` (
								  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
								  `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
								  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象'
);


DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers` (
									  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
									  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
									  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
									  `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
									  `time_zone_id` varchar(80) DEFAULT NULL COMMENT '时区'
);


DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers` (
									   `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
									   `entry_id` varchar(95) NOT NULL COMMENT '调度器实例id',
									   `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
									   `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
									   `instance_name` varchar(200) NOT NULL COMMENT '调度器实例名',
									   `fired_time` bigint NOT NULL COMMENT '触发的时间',
									   `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
									   `priority` int NOT NULL COMMENT '优先级',
									   `state` varchar(16) NOT NULL COMMENT '状态',
									   `job_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
									   `job_group` varchar(200) DEFAULT NULL COMMENT '任务组名',
									   `is_nonconcurrent` varchar(1) DEFAULT NULL COMMENT '是否并发',
									   `requests_recovery` varchar(1) DEFAULT NULL COMMENT '是否接受恢复执行'
);


DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details` (
									`sched_name` varchar(120) NOT NULL COMMENT '调度名称',
									`job_name` varchar(200) NOT NULL COMMENT '任务名称',
									`job_group` varchar(200) NOT NULL COMMENT '任务组名',
									`description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
									`job_class_name` varchar(250) NOT NULL COMMENT '执行任务类名称',
									`is_durable` varchar(1) NOT NULL COMMENT '是否持久化',
									`is_nonconcurrent` varchar(1) NOT NULL COMMENT '是否并发',
									`is_update_data` varchar(1) NOT NULL COMMENT '是否更新数据',
									`requests_recovery` varchar(1) NOT NULL COMMENT '是否接受恢复执行',
									`job_data` blob COMMENT '存放持久化job对象'
);


DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks` (
							  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
							  `lock_name` varchar(40) NOT NULL COMMENT '悲观锁名称'
);


DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps` (
											`sched_name` varchar(120) NOT NULL COMMENT '调度名称',
											`trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键'
);


DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state` (
										`sched_name` varchar(120) NOT NULL COMMENT '调度名称',
										`instance_name` varchar(200) NOT NULL COMMENT '实例名称',
										`last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
										`checkin_interval` bigint NOT NULL COMMENT '检查间隔时间'
);


DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers` (
										`sched_name` varchar(120) NOT NULL COMMENT '调度名称',
										`trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
										`trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
										`repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
										`repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
										`times_triggered` bigint NOT NULL COMMENT '已经触发的次数'
);


DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers` (
										 `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
										 `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
										 `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
										 `str_prop_1` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
										 `str_prop_2` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
										 `str_prop_3` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
										 `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
										 `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
										 `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
										 `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
										 `dec_prop_1` decimal(13, 4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
										 `dec_prop_2` decimal(13, 4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
										 `bool_prop_1` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
										 `bool_prop_2` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数'
);


DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers` (
								 `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
								 `trigger_name` varchar(200) NOT NULL COMMENT '触发器的名字',
								 `trigger_group` varchar(200) NOT NULL COMMENT '触发器所属组的名字',
								 `job_name` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
								 `job_group` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
								 `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
								 `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
								 `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
								 `priority` int DEFAULT NULL COMMENT '优先级',
								 `trigger_state` varchar(16) NOT NULL COMMENT '触发器状态',
								 `trigger_type` varchar(8) NOT NULL COMMENT '触发器的类型',
								 `start_time` bigint NOT NULL COMMENT '开始时间',
								 `end_time` bigint DEFAULT NULL COMMENT '结束时间',
								 `calendar_name` varchar(200) DEFAULT NULL COMMENT '日程表名称',
								 `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
								 `job_data` blob COMMENT '存放持久化job对象'
);


DROP TABLE IF EXISTS `sys_case`;
CREATE TABLE `sys_case` (
							`case_id` bigint NOT NULL AUTO_INCREMENT COMMENT '测试用例',
							`case_name` varchar(255) DEFAULT NULL COMMENT '用例名称',
							`module_id` bigint DEFAULT NULL COMMENT '模块id',
							`case_type` int DEFAULT NULL COMMENT '用例类型',
							`case_expect` varchar(255) DEFAULT NULL COMMENT '预期',
							`case_step` json DEFAULT NULL COMMENT '步骤',
							`case_level` int DEFAULT NULL COMMENT '用例级别',
							`case_preconditions` varchar(255) DEFAULT NULL COMMENT '前置条件',
							`create_by` varchar(50) DEFAULT NULL,
							`create_time` datetime DEFAULT NULL,
							`update_by` varchar(50) DEFAULT NULL,
							`update_time` datetime DEFAULT NULL,
							`case_num` bigint DEFAULT NULL COMMENT '用例号码',
							`project_id` bigint DEFAULT NULL COMMENT '项目编号',
							`remark` varchar(255) DEFAULT NULL COMMENT '备注'
);


DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
							  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
							  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
							  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
							  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
							  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
							  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
							  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
							  `remark` varchar(500) DEFAULT NULL COMMENT '备注'
);

INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`
						 , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y'
	   , 'admin', '2023-11-12 15:34:52', '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`
						 , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2, '用户管理-账号初始密码', 'sys.member.initPassword', '123456', 'Y'
	   , 'admin', '2023-11-12 15:34:52', 'admin', '2024-01-07 12:36:32', '初始化密码 123456');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`
						 , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y'
	   , 'admin', '2023-11-12 15:34:52', '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`
						 , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'false', 'Y'
	   , 'admin', '2023-11-12 15:34:52', 'admin', '2023-11-12 16:14:31', '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`
						 , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'true', 'Y'
	   , 'admin', '2023-11-12 15:34:52', 'admin', '2023-11-12 16:13:43', '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`
						 , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y'
	   , 'admin', '2023-11-12 15:34:52', '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');

DROP TABLE IF EXISTS `sys_defect`;
CREATE TABLE `sys_defect` (
							  `defect_id` bigint NOT NULL AUTO_INCREMENT COMMENT '缺陷id
',
							  `defect_type` int(1) NOT NULL DEFAULT '0' COMMENT '缺陷类型
',
							  `defect_name` varchar(128) NOT NULL COMMENT '缺陷标题',
							  `defect_describe` text NOT NULL COMMENT '缺陷描述',
							  `annex_urls` varchar(5000) DEFAULT NULL COMMENT '附件集合',
							  `project_id` bigint NOT NULL COMMENT '项目id',
							  `test_plan_id` bigint DEFAULT NULL COMMENT '测试计划id',
							  `case_id` bigint DEFAULT NULL COMMENT '测试用例id',
							  `data_sources` int DEFAULT NULL COMMENT '数据来源',
							  `data_sources_params` varchar(255) DEFAULT NULL COMMENT '数据来源相关参数',
							  `module_id` bigint DEFAULT NULL COMMENT '测试模块id',
							  `module_version` varchar(128) DEFAULT NULL COMMENT '测试模块的版本',
							  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
							  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
							  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
							  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
							  `defect_state` int(1) NOT NULL DEFAULT '0' COMMENT '缺陷状态',
							  `case_step_id` bigint DEFAULT NULL COMMENT '用例步骤id',
							  `handle_by` json DEFAULT NULL COMMENT '处理人id',
							  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
							  `defect_level` varchar(10) DEFAULT NULL COMMENT '缺陷等级',
							  `project_num` int DEFAULT NULL COMMENT '项目编号',
							  `img_urls` varchar(5000) DEFAULT NULL COMMENT '图片集合',
							  `create_by_id` bigint DEFAULT NULL COMMENT '创建者id',
							  `update_by_id` bigint DEFAULT NULL COMMENT '更新者id'
);


DROP TABLE IF EXISTS `sys_defect_log`;
CREATE TABLE `sys_defect_log` (
								  `defect_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '缺陷日志id',
								  `defect_log_describe` blob COMMENT '缺陷日志的描述',
								  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
								  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
								  `defect_log_type` int DEFAULT NULL COMMENT '处理类型(转发\\评论\\关闭)',
								  `receive_by` json DEFAULT NULL COMMENT '缺陷接收人',
								  `annex_urls` varchar(5000) DEFAULT NULL COMMENT '附件集合',
								  `defect_id` bigint NOT NULL COMMENT '缺陷id'
);


DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
							`dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
							`parent_id` bigint DEFAULT '0' COMMENT '父部门id',
							`ancestors` varchar(50) DEFAULT '' COMMENT '祖级列表',
							`dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
							`order_num` int DEFAULT '0' COMMENT '显示顺序',
							`leader` varchar(20) DEFAULT NULL COMMENT '负责人',
							`phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
							`email` varchar(50) DEFAULT NULL COMMENT '邮箱',
							`status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
							`del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
							`create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							`create_time` datetime DEFAULT NULL COMMENT '创建时间',
							`update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							`update_time` datetime DEFAULT NULL COMMENT '更新时间'
);

INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (100, 0, '0', '若依科技', 0
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (101, 100, '0,100', '深圳总公司', 1
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (102, 100, '0,100', '长沙分公司', 2
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (103, 101, '0,100,101', '研发部门', 1
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (104, 101, '0,100,101', '市场部门', 2
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (105, 101, '0,100,101', '测试部门', 3
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (106, 101, '0,100,101', '财务部门', 4
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (107, 101, '0,100,101', '运维部门', 5
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (108, 102, '0,100,102', '市场部门', 1
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`
					   , `leader`, `phone`, `email`, `status`, `del_flag`
					   , `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (109, 102, '0,100,102', '财务部门', 2
	   , '若依', '15888888888', 'ry@qq.com', '0', '0'
	   , 'admin', '2023-11-12 15:34:51', '', NULL);

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
								 `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
								 `dict_sort` int DEFAULT '0' COMMENT '字典排序',
								 `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
								 `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
								 `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
								 `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
								 `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
								 `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
								 `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
								 `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
								 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
								 `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
								 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
								 `remark` varchar(500) DEFAULT NULL COMMENT '备注'
);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1, 1, '男', '0', 'sys_user_sex'
	   , '', '', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '性别男');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2, 2, '女', '1', 'sys_user_sex'
	   , '', '', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '性别女');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (3, 3, '未知', '2', 'sys_user_sex'
	   , '', '', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '性别未知');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (4, 1, '显示', '0', 'sys_show_hide'
	   , '', 'primary', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '显示菜单');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (5, 2, '隐藏', '1', 'sys_show_hide'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6, 1, '正常', '0', 'sys_normal_disable'
	   , '', 'primary', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (7, 2, '停用', '1', 'sys_normal_disable'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (8, 1, '正常', '0', 'sys_job_status'
	   , '', 'primary', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (9, 2, '暂停', '1', 'sys_job_status'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group'
	   , '', '', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '默认分组');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group'
	   , '', '', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '系统分组');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (12, 1, '是', 'Y', 'sys_yes_no'
	   , '', 'primary', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '系统默认是');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (13, 2, '否', 'N', 'sys_yes_no'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '系统默认否');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (14, 1, '通知', '1', 'sys_notice_type'
	   , '', 'warning', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '通知');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (15, 2, '公告', '2', 'sys_notice_type'
	   , '', 'success', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '公告');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (16, 1, '正常', '0', 'sys_notice_status'
	   , '', 'primary', 'Y', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (17, 2, '关闭', '1', 'sys_notice_status'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '关闭状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (18, 99, '其他', '0', 'sys_oper_type'
	   , '', 'info', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '其他操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (19, 1, '新增', '1', 'sys_oper_type'
	   , '', 'info', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '新增操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (20, 2, '修改', '2', 'sys_oper_type'
	   , '', 'info', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '修改操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (21, 3, '删除', '3', 'sys_oper_type'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '删除操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (22, 4, '授权', '4', 'sys_oper_type'
	   , '', 'primary', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '授权操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (23, 5, '导出', '5', 'sys_oper_type'
	   , '', 'warning', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '导出操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (24, 6, '导入', '6', 'sys_oper_type'
	   , '', 'warning', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '导入操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (25, 7, '强退', '7', 'sys_oper_type'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '强退操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (26, 8, '生成代码', '8', 'sys_oper_type'
	   , '', 'warning', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '生成操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (27, 9, '清空数据', '9', 'sys_oper_type'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '清空操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (28, 1, '成功', '0', 'sys_common_status'
	   , '', 'primary', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (29, 2, '失败', '1', 'sys_common_status'
	   , '', 'danger', 'N', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (30, 0, '急', 'urgent', 'defect_level'
	   , NULL, 'default', 'N', '0', 'admin'
	   , '2023-11-23 12:05:32', 'admin', '2024-01-08 17:50:51', NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (31, 1, '高', 'height', 'defect_level'
	   , NULL, 'default', 'N', '0', 'admin'
	   , '2023-11-23 12:06:46', 'admin', '2024-01-08 17:51:02', NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (32, 2, '中', 'middle', 'defect_level'
	   , NULL, 'default', 'N', '0', 'admin'
	   , '2024-01-08 17:51:15', '', NULL, NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`
							, `css_class`, `list_class`, `is_default`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (33, 3, '低', 'low', 'defect_level'
	   , NULL, 'default', 'N', '0', 'admin'
	   , '2024-01-08 17:51:28', '', NULL, NULL);

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
								 `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
								 `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
								 `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
								 `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
								 `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
								 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
								 `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
								 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
								 `remark` varchar(500) DEFAULT NULL COMMENT '备注'
);

INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '用户性别列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '系统开关列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (4, '任务状态', 'sys_job_status', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '任务状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (5, '任务分组', 'sys_job_group', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '任务分组列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '系统是否列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '通知类型列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '通知状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '操作类型列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (10, '系统状态', 'sys_common_status', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '登录状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`
							, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (11, '缺陷等级', 'defect_level', '0', 'admin'
	   , '2023-11-23 12:04:58', 'admin', '2023-11-23 12:12:52', NULL);

DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
						   `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
						   `job_name` varchar(64) NOT NULL DEFAULT '' COMMENT '任务名称',
						   `job_group` varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
						   `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
						   `cron_expression` varchar(255) DEFAULT '' COMMENT 'cron执行表达式',
						   `misfire_policy` varchar(20) DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
						   `concurrent` char(1) DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
						   `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1暂停）',
						   `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
						   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
						   `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
						   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
						   `remark` varchar(500) DEFAULT '' COMMENT '备注信息'
);

INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`
					  , `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`
					  , `update_by`, `update_time`, `remark`)
VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?'
	   , '3', '1', '1', 'admin', '2023-11-12 15:34:52'
	   , '', NULL, '');
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`
					  , `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`
					  , `update_by`, `update_time`, `remark`)
VALUES (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(''ry'')', '0/15 * * * * ?'
	   , '3', '1', '1', 'admin', '2023-11-12 15:34:52'
	   , '', NULL, '');
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`
					  , `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`
					  , `update_by`, `update_time`, `remark`)
VALUES (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(''ry'', true, 2000L, 316.50D, 100)', '0/20 * * * * ?'
	   , '3', '1', '1', 'admin', '2023-11-12 15:34:52'
	   , '', NULL, '');

DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
							   `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
							   `job_name` varchar(64) NOT NULL COMMENT '任务名称',
							   `job_group` varchar(64) NOT NULL COMMENT '任务组名',
							   `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
							   `job_message` varchar(500) DEFAULT NULL COMMENT '日志信息',
							   `status` char(1) DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
							   `exception_info` varchar(2000) DEFAULT '' COMMENT '异常信息',
							   `create_time` datetime DEFAULT NULL COMMENT '创建时间'
);


DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor` (
								  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
								  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
								  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
								  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
								  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
								  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
								  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
								  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
								  `login_time` datetime DEFAULT NULL COMMENT '访问时间'
);


DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
							`menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
							`menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
							`parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
							`order_num` int DEFAULT '0' COMMENT '显示顺序',
							`path` varchar(200) DEFAULT '' COMMENT '路由地址',
							`component` varchar(255) DEFAULT NULL COMMENT '组件路径',
							`query` varchar(255) DEFAULT NULL COMMENT '路由参数',
							`is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
							`is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
							`menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
							`visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
							`status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
							`perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
							`icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
							`create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							`create_time` datetime DEFAULT NULL COMMENT '创建时间',
							`update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							`update_time` datetime DEFAULT NULL COMMENT '更新时间',
							`remark` varchar(500) DEFAULT '' COMMENT '备注',
							`menu_name_i18n_key` varchar(64) DEFAULT NULL COMMENT '菜单名称的国际化标识'
);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1, '系统管理', 0, 101, 'system'
	   , NULL, '', 1, 0, 'M'
	   , '0', '0', '', 'system', 'admin'
	   , '2023-11-12 15:34:52', 'admin', '2023-11-17 02:50:23', '系统管理目录', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2, '系统监控', 0, 102, 'monitor'
	   , NULL, '', 1, 0, 'M'
	   , '0', '0', '', 'monitor', 'admin'
	   , '2023-11-12 15:34:52', 'admin', '2023-11-17 02:50:30', '系统监控目录', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (3, '系统工具', 0, 103, 'tool'
	   , NULL, '', 1, 0, 'M'
	   , '0', '0', '', 'tool', 'admin'
	   , '2023-11-12 15:34:52', 'admin', '2023-11-17 02:50:36', '系统工具目录', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (100, '用户管理', 1, 1, 'user'
	   , 'system/user/index', '', 1, 0, 'C'
	   , '0', '0', 'system:user:list', 'user', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '用户管理菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (101, '角色管理', 1, 2, 'role'
	   , 'system/role/index', '', 1, 0, 'C'
	   , '0', '0', 'system:role:list', 'peoples', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '角色管理菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (102, '菜单管理', 1, 3, 'menu'
	   , 'system/menu/index', '', 1, 0, 'C'
	   , '0', '0', 'system:menu:list', 'tree-table', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '菜单管理菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (103, '部门管理', 1, 4, 'dept'
	   , 'system/dept/index', '', 1, 0, 'C'
	   , '0', '0', 'system:dept:list', 'tree', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '部门管理菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (104, '岗位管理', 1, 5, 'post'
	   , 'system/post/index', '', 1, 0, 'C'
	   , '0', '0', 'system:post:list', 'post', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '岗位管理菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (105, '字典管理', 1, 6, 'dict'
	   , 'system/dict/index', '', 1, 0, 'C'
	   , '0', '0', 'system:dict:list', 'dict', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '字典管理菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (106, '参数设置', 1, 7, 'config'
	   , 'system/config/index', '', 1, 0, 'C'
	   , '0', '0', 'system:config:list', 'edit', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '参数设置菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (107, '通知公告', 1, 8, 'notice'
	   , 'system/notice/index', '', 1, 0, 'C'
	   , '0', '0', 'system:notice:list', 'message', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '通知公告菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (108, '日志管理', 1, 9, 'log'
	   , '', '', 1, 0, 'M'
	   , '0', '0', '', 'log', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '日志管理菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (109, '在线用户', 2, 1, 'online'
	   , 'monitor/online/index', '', 1, 0, 'C'
	   , '0', '0', 'monitor:online:list', 'online', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '在线用户菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (110, '定时任务', 2, 2, 'job'
	   , 'monitor/job/index', '', 1, 0, 'C'
	   , '0', '0', 'monitor:job:list', 'job', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '定时任务菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (111, '数据监控', 2, 3, 'druid'
	   , 'monitor/druid/index', '', 1, 0, 'C'
	   , '0', '0', 'monitor:druid:list', 'druid', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '数据监控菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (112, '服务监控', 2, 4, 'server'
	   , 'monitor/server/index', '', 1, 0, 'C'
	   , '0', '0', 'monitor:server:list', 'server', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '服务监控菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (113, '缓存监控', 2, 5, 'cache'
	   , 'monitor/cache/index', '', 1, 0, 'C'
	   , '0', '0', 'monitor:cache:list', 'redis', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '缓存监控菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (114, '缓存列表', 2, 6, 'cacheList'
	   , 'monitor/cache/list', '', 1, 0, 'C'
	   , '0', '0', 'monitor:cache:list', 'redis-list', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '缓存列表菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (115, '表单构建', 3, 1, 'build'
	   , 'tool/build/index', '', 1, 0, 'C'
	   , '0', '0', 'tool:build:list', 'build', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '表单构建菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (116, '代码生成', 1, 0, 'gen'
	   , 'tool/gen/index', '', 1, 0, 'C'
	   , '0', '0', 'tool:gen:list', 'code', 'admin'
	   , '2023-11-12 15:34:52', 'admin', '2023-11-19 17:47:05', '代码生成菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (117, '系统接口', 3, 3, 'swagger'
	   , 'tool/swagger/index', '', 1, 0, 'C'
	   , '0', '0', 'tool:swagger:list', 'swagger', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '系统接口菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (500, '操作日志', 108, 1, 'operlog'
	   , 'monitor/operlog/index', '', 1, 0, 'C'
	   , '0', '0', 'monitor:operlog:list', 'form', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '操作日志菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (501, '登录日志', 108, 2, 'logininfor'
	   , 'monitor/logininfor/index', '', 1, 0, 'C'
	   , '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '登录日志菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1000, '用户查询', 100, 1, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:user:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1001, '用户新增', 100, 2, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:user:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1002, '用户修改', 100, 3, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:user:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1003, '用户删除', 100, 4, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:user:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1004, '用户导出', 100, 5, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:user:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1005, '用户导入', 100, 6, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:user:import', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1006, '重置密码', 100, 7, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:user:resetPwd', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1007, '角色查询', 101, 1, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:role:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1008, '角色新增', 101, 2, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:role:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1009, '角色修改', 101, 3, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:role:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1010, '角色删除', 101, 4, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:role:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1011, '角色导出', 101, 5, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:role:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1012, '菜单查询', 102, 1, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:menu:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1013, '菜单新增', 102, 2, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:menu:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1014, '菜单修改', 102, 3, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:menu:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1015, '菜单删除', 102, 4, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:menu:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1016, '部门查询', 103, 1, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dept:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1017, '部门新增', 103, 2, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dept:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1018, '部门修改', 103, 3, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dept:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1019, '部门删除', 103, 4, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dept:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1020, '岗位查询', 104, 1, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:post:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1021, '岗位新增', 104, 2, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:post:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1022, '岗位修改', 104, 3, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:post:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1023, '岗位删除', 104, 4, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:post:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1024, '岗位导出', 104, 5, ''
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:post:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1025, '字典查询', 105, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dict:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1026, '字典新增', 105, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dict:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1027, '字典修改', 105, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dict:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1028, '字典删除', 105, 4, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dict:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1029, '字典导出', 105, 5, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:dict:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1030, '参数查询', 106, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:config:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1031, '参数新增', 106, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:config:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1032, '参数修改', 106, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:config:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1033, '参数删除', 106, 4, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:config:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1034, '参数导出', 106, 5, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:config:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1035, '公告查询', 107, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:notice:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1036, '公告新增', 107, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:notice:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1037, '公告修改', 107, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:notice:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1038, '公告删除', 107, 4, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'system:notice:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1039, '操作查询', 500, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:operlog:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1040, '操作删除', 500, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:operlog:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1041, '日志导出', 500, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:operlog:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1042, '登录查询', 501, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:logininfor:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1043, '登录删除', 501, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:logininfor:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1044, '日志导出', 501, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:logininfor:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1045, '账户解锁', 501, 4, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:logininfor:unlock', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1046, '在线查询', 109, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:online:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1047, '批量强退', 109, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:online:batchLogout', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1048, '单条强退', 109, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:online:forceLogout', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1049, '任务查询', 110, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:job:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1050, '任务新增', 110, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:job:add', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1051, '任务修改', 110, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:job:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1052, '任务删除', 110, 4, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:job:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1053, '状态修改', 110, 5, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:job:changeStatus', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1054, '任务导出', 110, 6, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'monitor:job:export', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1055, '生成查询', 116, 1, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'tool:gen:query', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1056, '生成修改', 116, 2, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'tool:gen:edit', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1057, '生成删除', 116, 3, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'tool:gen:remove', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1058, '导入代码', 116, 4, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'tool:gen:import', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1059, '预览代码', 116, 5, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'tool:gen:preview', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (1060, '生成代码', 116, 6, '#'
	   , '', '', 1, 0, 'F'
	   , '0', '0', 'tool:gen:code', '#', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2000, '项目管理', 2043, 1, 'project'
	   , 'system/project/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:project:list', 'list', 'admin'
	   , '2023-11-12 16:31:17', 'admin', '2024-01-08 16:28:07', '项目菜单', 'project.manage');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2001, '项目查询', 2000, 1, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:query', '#', 'admin'
	   , '2023-11-12 16:31:17', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2002, '项目新增', 2000, 2, 'add'
	   , 'system/project/add', NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:add', '#', 'admin'
	   , '2023-11-12 16:31:17', 'admin', '2023-11-12 17:18:25', '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2003, '项目修改', 2000, 3, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:edit', '#', 'admin'
	   , '2023-11-12 16:31:17', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2004, '项目删除', 2000, 4, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:remove', '#', 'admin'
	   , '2023-11-12 16:31:18', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2005, '项目导出', 2000, 5, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:export', '#', 'admin'
	   , '2023-11-12 16:31:18', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2006, '团队管理', 1, 1, 'team'
	   , 'system/team/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:team:list', 'peoples', 'admin'
	   , '2023-11-13 03:34:40', 'admin', '2023-11-15 16:22:26', '团队菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2007, '团队查询', 2006, 1, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:query', '#', 'admin'
	   , '2023-11-13 03:34:40', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2008, '团队新增', 2006, 2, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:add', '#', 'admin'
	   , '2023-11-13 03:34:40', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2009, '团队修改', 2006, 3, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:edit', '#', 'admin'
	   , '2023-11-13 03:34:40', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2010, '团队删除', 2006, 4, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:remove', '#', 'admin'
	   , '2023-11-13 03:34:40', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2011, '团队导出', 2006, 5, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:export', '#', 'admin'
	   , '2023-11-13 03:34:40', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2013, '项目功能', 0, 1, 'project'
	   , NULL, NULL, 1, 0, 'M'
	   , '0', '0', '', 'redis', 'admin'
	   , '2023-11-17 11:34:46', 'admin', '2023-12-06 10:48:30', '', 'project.manage');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2015, '团队设置', 0, 2, 'team-options'
	   , NULL, NULL, 1, 0, 'M'
	   , '0', '0', '', 'dict', 'admin'
	   , '2023-11-17 23:19:30', 'admin', '2024-01-05 16:15:30', '', 'team.option');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2016, '团队设置', 2015, 1, 'team-option'
	   , 'system/team/option/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:team:query', 'form', 'admin'
	   , '2023-11-17 23:21:54', 'admin', '2024-01-06 06:57:58', '', 'team.option');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2017, '团队基础信息', 2015, 2, 'team-base-info'
	   , 'system/team/option/base/index', NULL, 1, 0, 'C'
	   , '1', '0', 'system:team:edit', '#', 'admin'
	   , '2023-11-18 00:03:19', 'admin', '2024-01-06 06:58:20', '', 'team.base-info');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2018, '团队成员管理', 2015, 3, 'team-member'
	   , 'system/team/option/member/index', NULL, 1, 0, 'C'
	   , '1', '0', 'system:team:member', '#', 'admin'
	   , '2023-11-18 06:12:51', 'admin', '2024-01-08 16:16:54', '', 'member.manage');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2019, '缺陷管理', 2013, 2, 'defect'
	   , 'system/defect/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:defect:list', 'bug', 'admin'
	   , '2023-11-23 12:50:56', 'admin', '2023-11-23 13:01:52', '缺陷菜单', 'defect.manage');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2020, '缺陷查询', 2019, 1, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:query', '#', 'admin'
	   , '2023-11-23 12:50:56', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2021, '缺陷新增', 2019, 2, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:add', '#', 'admin'
	   , '2023-11-23 12:50:56', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2022, '缺陷修改', 2019, 3, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:edit', '#', 'admin'
	   , '2023-11-23 12:50:56', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2023, '缺陷删除', 2019, 4, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:remove', '#', 'admin'
	   , '2023-11-23 12:50:56', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2024, '缺陷导出', 2019, 5, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:export', '#', 'admin'
	   , '2023-11-23 12:50:56', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2025, '模块', 2013, 3, 'module'
	   , 'system/module/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:module:list', 'cascader', 'admin'
	   , '2023-11-25 17:21:33', 'admin', '2023-12-30 07:19:34', '模块菜单', 'module');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2026, '模块查询', 2025, 1, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:module:query', '#', 'admin'
	   , '2023-11-25 17:21:33', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2027, '模块新增', 2025, 2, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:module:add', '#', 'admin'
	   , '2023-11-25 17:21:33', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2028, '模块修改', 2025, 3, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:module:edit', '#', 'admin'
	   , '2023-11-25 17:21:33', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2029, '模块删除', 2025, 4, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:module:remove', '#', 'admin'
	   , '2023-11-25 17:21:33', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2030, '模块导出', 2025, 5, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:module:export', '#', 'admin'
	   , '2023-11-25 17:21:33', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2043, '团队', 0, 0, 'team'
	   , NULL, NULL, 1, 0, 'M'
	   , '0', '0', '', '#', 'admin'
	   , '2023-12-06 10:37:40', 'admin', '2024-01-06 03:50:05', '', 'team');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2044, '工具', 2013, 3, 'tools'
	   , 'tool/project/index', NULL, 1, 0, 'C'
	   , '1', '1', '', 'tool', 'admin'
	   , '2023-12-06 13:51:08', 'admin', '2023-12-30 15:01:17', '', 'tools');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2045, '浏览器工具', 2013, 4, 'browser'
	   , 'tool/project/browser/index', NULL, 1, 0, 'C'
	   , '1', '0', '', '#', 'admin'
	   , '2023-12-06 14:48:16', 'admin', '2023-12-06 15:00:15', '', 'browser.tool');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2047, '新增团队', 2015, 4, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:add', '#', 'admin'
	   , '2023-12-28 19:02:50', 'admin', '2023-12-28 19:03:30', '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2048, '修改团队', 2015, 5, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:edit', '#', 'admin'
	   , '2023-12-28 19:03:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2049, '删除团队', 2015, 6, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:remove', '#', 'admin'
	   , '2023-12-28 19:04:17', 'admin', '2024-01-06 06:38:56', '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2050, '团队查询', 2015, 7, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:query', '#', 'admin'
	   , '2023-12-28 19:05:34', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2051, '团队列表', 2015, 8, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:list', '#', 'admin'
	   , '2023-12-28 19:05:52', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2052, '项目设置', 2013, 5, 'option'
	   , 'system/project/option/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:project:option', 'system', 'admin'
	   , '2023-12-30 16:21:17', 'admin', '2023-12-30 17:09:21', '', 'project.option');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2053, '项目基础信息', 2013, 6, 'project-base-info'
	   , 'system/project/edit', NULL, 1, 0, 'C'
	   , '1', '0', 'system:project:edit', '#', 'admin'
	   , '2023-12-30 17:12:23', '', NULL, '', 'project.base-info');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2054, '项目成员', 2013, 7, 'project-member'
	   , 'system/project/member/index', NULL, 1, 0, 'C'
	   , '1', '0', 'system:project:member', '#', 'admin'
	   , '2023-12-30 17:14:03', 'admin', '2023-12-30 17:14:30', '', 'project.project-and-member');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2055, '添加项目成员', 2054, 1, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:member:add', '#', 'admin'
	   , '2024-01-04 15:43:24', 'admin', '2024-01-09 17:00:55', '', 'project.add-member');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2057, '创建成员', 2018, 1, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:member:create', '#', 'admin'
	   , '2024-01-06 06:51:08', 'admin', '2024-01-06 06:52:28', '', 'member.create');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2058, '邀请成员', 2018, 2, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:member:invite', '#', 'admin'
	   , '2024-01-06 06:51:47', 'admin', '2024-01-06 06:52:12', '', 'team.invite-members');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2059, '锁定成员', 2018, 3, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:team:member:lock', '#', 'admin'
	   , '2024-01-06 07:01:22', '', NULL, '', 'lock');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2060, '缺陷指派', 2019, 6, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:assign', '#', 'admin'
	   , '2024-01-06 13:45:43', '', NULL, '', 'assign');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2061, '缺陷驳回', 2019, 7, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:reject', '#', 'admin'
	   , '2024-01-06 13:46:50', '', NULL, '', 'reject');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2062, '缺陷修复', 2019, 8, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:repair', '#', 'admin'
	   , '2024-01-06 13:47:13', '', NULL, '', 'repair');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2063, '缺陷通过', 2019, 9, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:pass', '#', 'admin'
	   , '2024-01-06 13:47:38', '', NULL, '', 'pass');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2064, '缺陷关闭', 2019, 10, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:close', '#', 'admin'
	   , '2024-01-06 13:48:22', '', NULL, '', 'close');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2065, '缺陷开启', 2019, 11, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:defect:open', '#', 'admin'
	   , '2024-01-06 13:48:48', '', NULL, '', 'open');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2070, '修改项目成员', 2054, 2, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:member:update', '#', 'admin'
	   , '2024-01-09 17:00:24', 'admin', '2024-01-09 17:00:58', '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2071, '删除项目成员', 2054, 3, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:project:member:remove', '#', 'admin'
	   , '2024-01-15 08:20:59', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2072, '系统', 0, 99, 'admin'
	   , NULL, NULL, 1, 0, 'M'
	   , '0', '0', NULL, 'redis-list', 'admin'
	   , '2024-01-26 05:52:13', '', NULL, '', 'system');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2073, '成员管理', 2072, 1, 'member'
	   , 'system/member/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:user:list', 'user', 'admin'
	   , '2024-01-26 05:53:50', 'admin', '2024-01-26 05:54:15', '', 'member.manage');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2074, '测试用例', 2013, 1, 'case'
	   , 'system/case/index', NULL, 1, 0, 'C'
	   , '0', '0', 'system:case:list', 'nested', 'admin'
	   , '2024-01-27 16:12:12', 'admin', '2024-01-27 16:15:08', '测试用例菜单', 'case');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2075, '测试用例查询', 2074, 1, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:case:query', '#', 'admin'
	   , '2024-01-27 16:12:12', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2076, '测试用例新增', 2074, 2, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:case:add', '#', 'admin'
	   , '2024-01-27 16:12:12', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2077, '测试用例修改', 2074, 3, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:case:edit', '#', 'admin'
	   , '2024-01-27 16:12:12', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2078, '测试用例删除', 2074, 4, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:case:remove', '#', 'admin'
	   , '2024-01-27 16:12:12', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2079, '测试用例导出', 2074, 5, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:case:export', '#', 'admin'
	   , '2024-01-27 16:12:12', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2080, '测试用例导入', 2074, 6, ''
	   , NULL, NULL, 1, 0, 'F'
	   , '0', '0', 'system:case:import', '#', 'admin'
	   , '2024-02-02 02:30:20', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2081, '项目API', 2013, 1, 'project-api'
	   , 'system/api/index', NULL, 1, 0, 'C'
	   , '1', '0', 'system:api:list', '#', 'admin'
	   , '2024-02-10 20:38:28', 'admin', '2024-02-10 20:48:21', '项目API菜单', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2082, '项目API查询', 2081, 1, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:api:query', '#', 'admin'
	   , '2024-02-10 20:38:28', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2083, '项目API新增', 2081, 2, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:api:add', '#', 'admin'
	   , '2024-02-10 20:38:28', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2084, '项目API修改', 2081, 3, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:api:edit', '#', 'admin'
	   , '2024-02-10 20:38:28', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2085, '项目API删除', 2081, 4, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:api:remove', '#', 'admin'
	   , '2024-02-10 20:38:28', '', NULL, '', NULL);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`
					   , `component`, `query`, `is_frame`, `is_cache`, `menu_type`
					   , `visible`, `status`, `perms`, `icon`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
VALUES (2086, '项目API导出', 2081, 5, '#'
	   , '', NULL, 1, 0, 'F'
	   , '0', '0', 'system:api:export', '#', 'admin'
	   , '2024-02-10 20:38:28', '', NULL, '', NULL);

DROP TABLE IF EXISTS `sys_module`;
CREATE TABLE `sys_module` (
							  `module_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模块id',
							  `module_pid` bigint(20) DEFAULT '00000000000000000000' COMMENT '父模块id',
							  `module_name` varchar(128) DEFAULT NULL COMMENT '模块名称',
							  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
							  `project_id` bigint DEFAULT NULL COMMENT '项目id'
);


DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
							  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
							  `notice_title` varchar(50) NOT NULL COMMENT '公告标题',
							  `notice_type` char(1) NOT NULL COMMENT '公告类型（1通知 2公告）',
							  `notice_content` longblob COMMENT '公告内容',
							  `status` char(1) DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
							  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
							  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
							  `remark` varchar(255) DEFAULT NULL COMMENT '备注'
);
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
								`oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
								`title` varchar(50) DEFAULT '' COMMENT '模块标题',
								`business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
								`method` varchar(100) DEFAULT '' COMMENT '方法名称',
								`request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
								`operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
								`oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
								`dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
								`oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
								`oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
								`oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
								`oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
								`json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
								`status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
								`error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
								`oper_time` datetime DEFAULT NULL COMMENT '操作时间',
								`cost_time` bigint DEFAULT '0' COMMENT '消耗时间'
);


DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
							`post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
							`post_code` varchar(64) NOT NULL COMMENT '岗位编码',
							`post_name` varchar(50) NOT NULL COMMENT '岗位名称',
							`post_sort` int NOT NULL COMMENT '显示顺序',
							`status` char(1) NOT NULL COMMENT '状态（0正常 1停用）',
							`create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							`create_time` datetime DEFAULT NULL COMMENT '创建时间',
							`update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							`update_time` datetime DEFAULT NULL COMMENT '更新时间',
							`remark` varchar(500) DEFAULT NULL COMMENT '备注'
);

INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`
					   , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1, 'ceo', '董事长', 1, '0'
	   , 'admin', '2023-11-12 15:34:52', '', NULL, '');
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`
					   , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2, 'se', '项目经理', 2, '0'
	   , 'admin', '2023-11-12 15:34:52', '', NULL, '');
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`
					   , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (3, 'hr', '人力资源', 3, '0'
	   , 'admin', '2023-11-12 15:34:52', '', NULL, '');
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`
					   , `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (4, 'user', '普通员工', 4, '0'
	   , 'admin', '2023-11-12 15:34:52', '', NULL, '');

DROP TABLE IF EXISTS `sys_project`;
CREATE TABLE `sys_project` (
							   `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目id',
							   `project_name` varchar(64) NOT NULL COMMENT '项目名称',
							   `project_icon` varchar(255) DEFAULT NULL COMMENT '项目图标地址',
							   `project_introduce` varchar(255) DEFAULT NULL COMMENT '项目介绍',
							   `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
							   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
							   `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
							   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
							   `team_id` bigint NOT NULL COMMENT '团队id',
							   `project_state` int DEFAULT '1' COMMENT '项目状态(0删除；1运行)'
);


DROP TABLE IF EXISTS `sys_project_api`;
CREATE TABLE `sys_project_api` (
								   `api_id` varchar(32) NOT NULL COMMENT '项目api主键',
								   `project_id` bigint NOT NULL COMMENT '项目id',
								   `user_id` bigint NOT NULL COMMENT '用户id',
								   `white_list` json DEFAULT NULL COMMENT '白名单',
								   `expire_time` datetime DEFAULT NULL COMMENT '有效时间',
								   `remark` varchar(255) DEFAULT NULL COMMENT '备注'
);


DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
							`role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
							`role_name` varchar(30) NOT NULL COMMENT '角色名称',
							`role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
							`role_sort` int NOT NULL COMMENT '显示顺序',
							`data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
							`menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
							`dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
							`status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
							`del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
							`create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							`create_time` datetime DEFAULT NULL COMMENT '创建时间',
							`update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							`update_time` datetime DEFAULT NULL COMMENT '更新时间',
							`remark` varchar(500) DEFAULT NULL COMMENT '备注',
							`is_team_role` tinyint(1) DEFAULT NULL COMMENT '是否是团队角色',
							`is_project_role` tinyint(1) DEFAULT NULL COMMENT '是否是项目角色',
							`role_name_i18n_key` varchar(30) DEFAULT NULL COMMENT '角色名称国际化标记'
);

INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (1, '超级管理员', 'admin', 1, '1'
	   , 1, 1, '0', '0', 'admin'
	   , '2023-11-12 15:34:52', '', NULL, '超级管理员', NULL
	   , NULL, NULL);
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (2, '普通角色', 'common', 3, '2'
	   , 1, 1, '0', '0', 'admin'
	   , '2023-11-12 15:34:52', 'admin', '2024-01-06 12:06:15', '普通角色', 0
	   , 0, NULL);
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (3, '外部人员', 'outsiders', 4, '1'
	   , 1, 1, '0', '0', 'admin'
	   , '2023-11-18 15:08:35', 'admin', '2024-01-06 12:06:19', NULL, 0
	   , 0, NULL);
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (4, '团队管理员', 'team.admin', 12, '1'
	   , 1, 1, '0', '0', 'admin'
	   , '2023-11-18 15:15:50', 'admin', '2024-02-10 20:40:32', NULL, 1
	   , 0, 'team.admin-members');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (5, '团队普通人员', 'team.default', 13, '1'
	   , 0, 1, '0', '0', 'admin'
	   , '2023-11-18 15:16:21', 'admin', '2024-01-15 09:34:51', NULL, 1
	   , 0, 'team.ordinary-members');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (6, '项目管理员', 'project.admin', 22, '1'
	   , 1, 1, '0', '0', 'admin'
	   , '2023-11-22 06:48:53', 'admin', '2024-02-10 20:40:48', NULL, 0
	   , 1, 'project.admin');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (7, '项目开发', 'project.develop', 23, '1'
	   , 0, 1, '0', '0', 'admin'
	   , '2023-11-22 06:50:14', 'admin', '2024-02-02 02:32:04', NULL, 0
	   , 1, 'project.develop');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (8, '项目测试', 'project.tester', 24, '1'
	   , 1, 1, '0', '0', 'admin'
	   , '2023-11-22 06:53:11', 'admin', '2024-02-02 02:32:14', NULL, 0
	   , 1, 'project.tester');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (9, '项目外部人员', 'project.outsider', 25, '1'
	   , 0, 1, '0', '0', 'admin'
	   , '2023-11-22 06:55:20', 'admin', '2024-02-02 02:32:30', NULL, 0
	   , 1, 'project.outsider');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (10, '默认成员', 'default', 2, '1'
	   , 0, 1, '0', '0', 'admin'
	   , '2023-12-28 18:56:22', 'admin', '2024-02-02 02:32:58', NULL, 0
	   , 0, NULL);
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (11, '项目创建人', 'project.create-by', 21, '1'
	   , 1, 1, '0', '0', 'admin'
	   , '2024-01-05 17:57:40', 'admin', '2024-02-10 20:40:40', NULL, 0
	   , 1, 'project.create-by');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`
					   , `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`, `is_team_role`
					   , `is_project_role`, `role_name_i18n_key`)
VALUES (12, '团队创建人', 'team.create-by', 11, '1'
	   , 1, 1, '0', '0', 'admin'
	   , '2024-01-06 03:41:45', 'admin', '2024-02-10 20:40:24', NULL, 1
	   , 0, 'team.create-by');

DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
								 `role_id` bigint NOT NULL COMMENT '角色ID',
								 `dept_id` bigint NOT NULL COMMENT '部门ID'
);

INSERT INTO `sys_role_dept` (`role_id`, `dept_id`)
VALUES (2, 100);
INSERT INTO `sys_role_dept` (`role_id`, `dept_id`)
VALUES (2, 101);
INSERT INTO `sys_role_dept` (`role_id`, `dept_id`)
VALUES (2, 105);

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
								 `role_id` bigint NOT NULL COMMENT '角色ID',
								 `menu_id` bigint NOT NULL COMMENT '菜单ID'
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 2);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 3);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 100);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 101);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 102);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 103);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 104);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 105);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 106);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 107);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 108);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 109);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 110);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 111);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 112);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 113);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 114);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 115);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 116);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 117);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 500);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 501);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1004);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1006);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1007);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1008);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1009);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1010);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1011);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1012);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1014);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1016);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1017);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1018);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1027);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1028);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1029);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1030);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1031);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1032);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1033);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1034);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1035);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1036);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1037);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1038);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1039);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1040);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1041);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1042);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1044);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1045);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1046);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1047);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1048);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1049);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1050);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1051);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1052);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1053);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1054);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1055);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1056);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1057);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1058);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1059);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1060);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2004);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2016);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2017);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2018);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (3, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2004);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2016);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2017);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2018);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2027);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2028);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2029);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2030);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2047);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2048);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2050);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2051);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2052);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2053);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2054);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2055);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2057);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2058);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2059);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2060);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2061);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2062);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2063);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2064);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2065);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2070);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2071);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2076);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2077);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2078);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2079);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2080);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2081);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2082);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2083);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2084);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2085);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (4, 2086);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2050);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (5, 2051);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2004);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2027);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2028);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2029);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2030);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2050);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2051);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2052);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2053);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2054);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2055);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2060);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2061);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2062);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2063);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2064);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2065);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2070);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2071);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2076);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2077);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2078);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2079);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2080);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2081);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2082);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2083);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2084);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2085);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (6, 2086);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2060);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2062);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (7, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2027);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2028);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2029);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2030);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2060);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2061);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2062);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2063);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2064);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2065);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2076);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2077);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2078);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2079);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (8, 2080);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2016);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2017);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2018);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2047);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2048);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2049);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2050);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2051);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2057);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2058);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2059);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (10, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2004);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2027);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2028);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2029);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2030);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2044);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2045);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2052);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2053);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2054);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2055);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2060);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2061);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2062);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2063);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2064);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2065);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2070);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2071);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2076);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2077);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2078);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2079);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2080);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2081);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2082);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2083);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2084);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2085);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (11, 2086);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2000);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2004);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2016);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2017);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2018);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2027);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2028);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2029);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2030);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2047);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2048);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2049);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2050);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2051);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2052);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2053);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2054);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2055);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2057);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2058);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2059);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2060);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2061);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2062);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2063);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2064);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2065);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2070);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2071);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2074);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2075);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2076);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2077);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2078);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2079);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2080);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2081);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2082);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2083);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2084);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2085);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (12, 2086);

DROP TABLE IF EXISTS `sys_screen_size`;
CREATE TABLE `sys_screen_size` (
								   `screen_size_id` bigint NOT NULL AUTO_INCREMENT COMMENT '屏幕尺寸id',
								   `name` varchar(32) NOT NULL COMMENT '名称',
								   `width` varchar(12) DEFAULT NULL COMMENT '宽',
								   `height` varchar(12) DEFAULT NULL COMMENT '高',
								   `remark` varchar(255) DEFAULT NULL COMMENT '备注'
);

INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (1, 'iPhone 6', '750px', '1334px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (2, 'iPhone 6 Plus', '1242px', '2208px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (3, 'iPhone 6 6s', '750px', '1334px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (4, 'iPhone 6s Plus', '1242px', '2208px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (5, 'iPhone 7', '750px', '1334px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (6, 'iPhone 7 Plus', '1242px', '2208px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (7, 'iPhone 8', '750px', '1334px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (8, 'iPhone 8 Plus', '1242px', '2208px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (9, 'iPhone X', '1125px', '2436px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (10, 'iPhone SE2', '750px', '1334px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (11, 'iPhone 11', '828px', '1792px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (12, 'iPhone 11 Pro', '1125px', '2436px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (13, 'iPhone 11 Pro Max', '1242px', '2688px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (14, 'iPhone 12 mini', '1080px', '2340px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (15, 'iPhone 12', '1170px', '2532px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (16, 'iPhone 12 Pro', '1170px', '2532px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (17, 'iPhone 12 Pro Max', '1284px', '2778px', NULL);
INSERT INTO `sys_screen_size` (`screen_size_id`, `name`, `width`, `height`, `remark`)
VALUES (18, 'test', '100px', '500px', NULL);

DROP TABLE IF EXISTS `sys_team`;
CREATE TABLE `sys_team` (
							`team_id` bigint NOT NULL AUTO_INCREMENT COMMENT '团队id',
							`team_name` varchar(64) DEFAULT NULL COMMENT '团队名称',
							`team_icon` varchar(255) DEFAULT NULL COMMENT '团队图标',
							`create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
							`create_time` datetime DEFAULT NULL COMMENT '创建时间',
							`update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
							`update_time` datetime DEFAULT NULL COMMENT '更新时间',
							`introduce` varchar(255) DEFAULT NULL COMMENT '团队介绍',
							`is_del` tinyint DEFAULT NULL COMMENT '是否删除'
);


DROP TABLE IF EXISTS `sys_temp_file`;
CREATE TABLE `sys_temp_file` (
								 `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件id',
								 `file_name` varchar(128) DEFAULT NULL COMMENT '文件名',
								 `src_url` varchar(255) DEFAULT NULL COMMENT '来源地址',
								 `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
								 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
								 `file_type` int(10) DEFAULT '0000000000' COMMENT '文件类型(0普通文件,1图片)',
								 `file_url` varchar(255) DEFAULT NULL COMMENT '文件地址'
);


DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
							`user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
							`dept_id` bigint(20) DEFAULT '00000000000000000000' COMMENT '部门ID',
							`user_name` varchar(30) NOT NULL COMMENT '用户账号',
							`nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
							`user_type` varchar(2) DEFAULT '00' COMMENT '用户类型（00系统用户;01API）',
							`email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
							`phonenumber` varchar(11) DEFAULT NULL COMMENT '手机号码',
							`sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
							`avatar` varchar(100) DEFAULT '' COMMENT '头像地址',
							`password` varchar(100) DEFAULT '' COMMENT '密码',
							`status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
							`del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
							`login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
							`login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
							`create_by` varchar(64) DEFAULT '' COMMENT '创建者',
							`create_time` datetime DEFAULT NULL COMMENT '创建时间',
							`update_by` varchar(64) DEFAULT '' COMMENT '更新者',
							`update_time` datetime DEFAULT NULL COMMENT '更新时间',
							`remark` varchar(500) DEFAULT NULL COMMENT '备注'
);

INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`
					   , `email`, `phonenumber`, `sex`, `avatar`, `password`
					   , `status`, `del_flag`, `login_ip`, `login_date`, `create_by`
					   , `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1, 0, 'admin', '黑猫警长', '00'
	   , 'admin@cat2bug.com', '18888888888', '0', '', '$2a$10$5enMqRePATm8OfHb0/64Ce3NGChvmefyvTA3o/7juZEm4mEf1dxgu'
	   , '0', '0', '127.0.0.1', '2024-02-15 23:47:59', 'admin'
	   , '2023-11-12 15:34:51', '', '2024-02-15 15:47:59', '管理员');

DROP TABLE IF EXISTS `sys_user_config`;
CREATE TABLE `sys_user_config` (
								   `user_config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户配置id',
								   `current_team_id` bigint DEFAULT NULL COMMENT '当前团队id',
								   `user_id` bigint DEFAULT NULL COMMENT '用户id',
								   `current_project_id` bigint DEFAULT NULL COMMENT '当前项目id'
);


DROP TABLE IF EXISTS `sys_user_defect`;
CREATE TABLE `sys_user_defect` (
								   `user_defect_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户缺陷id',
								   `defect_id` bigint DEFAULT NULL COMMENT '缺陷id',
								   `user_id` bigint DEFAULT NULL COMMENT '用户id',
								   `collect` tinyint(1) DEFAULT NULL COMMENT '是否收藏'
);


DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
								 `user_id` bigint NOT NULL COMMENT '用户ID',
								 `post_id` bigint NOT NULL COMMENT '岗位ID'
);


DROP TABLE IF EXISTS `sys_user_project`;
CREATE TABLE `sys_user_project` (
									`user_project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户项目id',
									`user_id` bigint NOT NULL COMMENT '用户id',
									`project_id` bigint NOT NULL COMMENT '项目id',
									`create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
									`create_time` datetime DEFAULT NULL COMMENT '创建时间',
									`update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
									`update_time` datetime DEFAULT NULL COMMENT '更新时间',
									`project_lock` tinyint(1) DEFAULT '0' COMMENT '是否锁定',
									`collect` tinyint(1) DEFAULT NULL COMMENT '是否收藏'
);


DROP TABLE IF EXISTS `sys_user_project_role`;
CREATE TABLE `sys_user_project_role` (
										 `user_project_role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户项目角色id',
										 `user_project_id` bigint NOT NULL COMMENT '用户项目id',
										 `role_id` bigint NOT NULL COMMENT '角色id'
);


DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
								 `user_id` bigint NOT NULL COMMENT '用户ID',
								 `role_id` bigint NOT NULL COMMENT '角色ID'
);


DROP TABLE IF EXISTS `sys_user_statistic_template`;
CREATE TABLE `sys_user_statistic_template` (
											   `statistic_template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计模版id',
											   `statistic_templat_code` varchar(255) DEFAULT NULL COMMENT '统计模版编码',
											   `module_type` int NOT NULL COMMENT '模型类型(1:缺陷)',
											   `project_id` bigint DEFAULT NULL COMMENT '项目id',
											   `user_id` bigint DEFAULT NULL COMMENT '用户id',
											   `statistic_templat_config` json DEFAULT NULL COMMENT '统计模版配置'
);


DROP TABLE IF EXISTS `sys_user_team`;
CREATE TABLE `sys_user_team` (
								 `user_team_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户团队id',
								 `user_id` bigint NOT NULL COMMENT '用户id',
								 `team_id` bigint NOT NULL COMMENT '团队id',
								 `team_role_id` bigint DEFAULT NULL COMMENT '团队角色id',
								 `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
								 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
								 `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
								 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
								 `team_lock` tinyint(1) DEFAULT '0' COMMENT '是否锁定'
);


DROP TABLE IF EXISTS `sys_user_team_role`;
CREATE TABLE `sys_user_team_role` (
									  `user_team_role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户团队角色id',
									  `user_team_id` bigint NOT NULL COMMENT '用户团队id',
									  `role_id` bigint NOT NULL COMMENT '角色id'
);

