-- 数据迁移：ai_account → sys_robot_agent
-- 若 V0_6_2 已在 flyway 历史中标记成功但实际未建表（checksum repair 场景），在此补建

CREATE TABLE IF NOT EXISTS sys_robot_agent (
  agent_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '智能体ID',
  agent_name VARCHAR(100) NOT NULL COMMENT '智能体名称',
  agent_type VARCHAR(50) NOT NULL COMMENT '智能体类型：case-generation, defect-handling, report-generation, bdd-testing',
  scope_type VARCHAR(20) NOT NULL COMMENT '作用域类型：project, team',
  scope_id BIGINT NOT NULL COMMENT '作用域ID（项目ID或团队ID）',
  container_image VARCHAR(255) NOT NULL COMMENT '容器镜像',
  env_vars TEXT COMMENT '环境变量（JSON格式）',
  resource_cpu INT DEFAULT 1000 COMMENT 'CPU限制（毫核，1000=1核）',
  resource_memory INT DEFAULT 512 COMMENT '内存限制（MB）',
  resource_disk INT DEFAULT 1024 COMMENT '磁盘限制（MB）',
  timeout_seconds INT DEFAULT 300 COMMENT '超时时间（秒）',
  status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active, disabled',
  remark VARCHAR(500) COMMENT '备注',
  create_by VARCHAR(64) COMMENT '创建者',
  create_time DATETIME COMMENT '创建时间',
  update_by VARCHAR(64) COMMENT '更新者',
  update_time DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体配置表';

CREATE TABLE IF NOT EXISTS sys_agent_task (
  task_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  agent_id BIGINT NOT NULL COMMENT '智能体ID',
  task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
  task_params TEXT COMMENT '任务参数（JSON格式）',
  priority INT DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
  status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending, running, completed, failed, cancelled, timeout',
  progress INT DEFAULT 0 COMMENT '进度（0-100）',
  progress_desc VARCHAR(255) COMMENT '进度描述',
  result TEXT COMMENT '任务结果（JSON格式）',
  error_message TEXT COMMENT '错误信息',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  max_retry INT DEFAULT 3 COMMENT '最大重试次数',
  timeout_seconds INT DEFAULT 300 COMMENT '超时时间（秒）',
  orchestrator_job_id VARCHAR(100) COMMENT '容器编排系统的任务ID',
  orchestrator_type VARCHAR(20) DEFAULT 'nomad' COMMENT '容器编排系统类型：nomad, k8s',
  submit_time DATETIME COMMENT '提交时间',
  start_time DATETIME COMMENT '开始时间',
  end_time DATETIME COMMENT '结束时间',
  create_by VARCHAR(64) COMMENT '创建者',
  create_time DATETIME COMMENT '创建时间',
  project_id BIGINT COMMENT '项目ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体任务队列表';

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
    CONCAT('AI-', a.account_name) as agent_name,
    'case-generation' as agent_type,
    'project' as scope_type,
    a.project_id as scope_id,
    'cat2bug/ai-agent:latest' as container_image,
    JSON_OBJECT(
        'AI_URL', a.ai_url,
        'MODEL_NAME', a.model_name,
        'API_KEY', a.api_key,
        'MAX_TOKENS', COALESCE(a.max_completion_tokens, 4096)
    ) as env_vars,
    1000 as resource_cpu,
    512 as resource_memory,
    1024 as resource_disk,
    300 as timeout_seconds,
    'active' as status,
    CONCAT('从AI账号迁移: ', a.account_name) as remark,
    COALESCE(CAST(a.create_by AS CHAR), 'system') as create_by,
    NOW() as create_time,
    'system' as update_by,
    NOW() as update_time
FROM ai_account a
WHERE a.account_id IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'ai_account'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_robot_agent r
    WHERE r.agent_name = CONCAT('AI-', a.account_name) AND r.scope_id = a.project_id
  );
