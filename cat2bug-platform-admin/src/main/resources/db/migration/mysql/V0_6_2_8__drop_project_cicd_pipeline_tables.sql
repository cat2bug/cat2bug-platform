-- 移除已废弃的 CI/CD 环境与流水线相关表（与 sql/cat2bug_platform.sql 基线一致）
DROP TABLE IF EXISTS `sys_project_pipeline_run_step`;
DROP TABLE IF EXISTS `sys_project_pipeline_run`;
DROP TABLE IF EXISTS `sys_project_pipeline_step`;
DROP TABLE IF EXISTS `sys_project_pipeline`;
DROP TABLE IF EXISTS `sys_project_cicd_env`;
DROP TABLE IF EXISTS `sys_project_mcp`;
