-- V0.6.2: 智能体系统 - 创建智能体配置表和任务队列表

-- 智能体配置表
CREATE TABLE sys_robot_agent (
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
);

-- 任务队列表
CREATE TABLE sys_agent_task (
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
);

-- 为任务队列表添加索引
CREATE INDEX idx_agent_task_status ON sys_agent_task(status);
CREATE INDEX idx_agent_task_priority ON sys_agent_task(priority);
CREATE INDEX idx_agent_task_submit_time ON sys_agent_task(submit_time);
CREATE INDEX idx_agent_task_agent_id ON sys_agent_task(agent_id);
CREATE INDEX idx_agent_task_project_id ON sys_agent_task(project_id);
CREATE INDEX idx_agent_task_orchestrator ON sys_agent_task(orchestrator_job_id, orchestrator_type);

-- 为智能体配置表添加索引
CREATE INDEX idx_robot_agent_scope ON sys_robot_agent(scope_type, scope_id);
CREATE INDEX idx_robot_agent_type ON sys_robot_agent(agent_type);
CREATE INDEX idx_robot_agent_status ON sys_robot_agent(status);
