-- 数据迁移脚本：将现有AI模型配置转换为智能体配置
-- 此脚本将 ai_account 表中的配置迁移到 sys_robot_agent 表

-- 为H2数据库创建迁移脚本
-- 将现有的 ai_account 配置转换为测试用例生成智能体
INSERT INTO sys_robot_agent (
    agent_name,
    agent_type,
    scope_type,
    scope_id,
    container_image,
    env_vars,
    resource_cpu,
    resource_memory,
    resource_disk,
    timeout_seconds,
    status,
    remark,
    create_by,
    create_time,
    update_by,
    update_time
)
SELECT
    CONCAT('AI-', account_name) as agent_name,
    'case-generation' as agent_type,
    'project' as scope_type,
    project_id as scope_id,
    'cat2bug/ai-agent:latest' as container_image,
    CONCAT('{"AI_URL":"', ai_url, '","MODEL_NAME":"', model_name, '","API_KEY":"', api_key, '","MAX_TOKENS":"', COALESCE(max_completion_tokens, 4096), '"}') as env_vars,
    1000 as resource_cpu,
    512 as resource_memory,
    1024 as resource_disk,
    300 as timeout_seconds,
    'active' as status,
    CONCAT('从AI账号迁移: ', account_name) as remark,
    COALESCE(CAST(create_by AS VARCHAR), 'system') as create_by,
    CURRENT_TIMESTAMP as create_time,
    'system' as update_by,
    CURRENT_TIMESTAMP as update_time
FROM ai_account
WHERE account_id IS NOT NULL;

-- 注意：此迁移脚本仅创建测试用例生成类型的智能体
-- 其他类型的智能体（缺陷处理、报告生成、BDD测试）需要手动配置
