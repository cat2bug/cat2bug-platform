# 数据库迁移说明

## 概述

本次迁移（V0.6.2）为智能体系统创建了新的数据库表结构，并提供了从现有 AI 配置迁移的脚本。

## 新增表

### 1. sys_robot_agent（智能体配置表）

存储智能体的配置信息，包括容器镜像、资源限制、环境变量等。

**主要字段：**
- `agent_id`: 智能体ID（主键）
- `agent_name`: 智能体名称
- `agent_type`: 智能体类型（case-generation, defect-handling, report-generation, bdd-testing）
- `scope_type`: 作用域类型（project, team）
- `scope_id`: 作用域ID（项目ID或团队ID）
- `container_image`: 容器镜像
- `env_vars`: 环境变量（JSON格式）
- `resource_cpu`: CPU限制（毫核）
- `resource_memory`: 内存限制（MB）
- `resource_disk`: 磁盘限制（MB）
- `timeout_seconds`: 超时时间（秒）
- `status`: 状态（active, disabled）

### 2. sys_agent_task（智能体任务队列表）

存储智能体任务的执行状态和结果。

**主要字段：**
- `task_id`: 任务ID（主键）
- `agent_id`: 智能体ID
- `task_type`: 任务类型
- `task_params`: 任务参数（JSON格式）
- `priority`: 优先级
- `status`: 状态（pending, running, completed, failed, cancelled, timeout）
- `progress`: 进度（0-100）
- `result`: 任务结果（JSON格式）
- `orchestrator_job_id`: 容器编排系统的任务ID
- `orchestrator_type`: 容器编排系统类型（nomad, k8s）
- `submit_time`: 提交时间
- `start_time`: 开始时间
- `end_time`: 结束时间

## 数据迁移

### 自动迁移

迁移脚本 `V0.6.2_1__migrate_ai_to_robot_agent.sql` 会自动将现有的 `ai_account` 表中的配置转换为智能体配置。

**迁移规则：**
- 每个 AI 账号会被转换为一个"测试用例生成"类型的智能体
- 智能体名称格式：`AI-{原账号名称}`
- 作用域类型：project
- 作用域ID：原项目ID
- 容器镜像：`cat2bug/ai-agent:latest`（需要根据实际情况修改）
- 环境变量：包含 AI_URL, MODEL_NAME, API_KEY, MAX_TOKENS

### 手动配置

其他类型的智能体（缺陷处理、报告生成、BDD测试）需要在系统中手动配置。

## 索引

为了提高查询性能，已为以下字段创建索引：

**sys_agent_task 表：**
- `idx_agent_task_status`: status
- `idx_agent_task_priority`: priority
- `idx_agent_task_submit_time`: submit_time
- `idx_agent_task_agent_id`: agent_id
- `idx_agent_task_project_id`: project_id
- `idx_agent_task_orchestrator`: orchestrator_job_id, orchestrator_type

**sys_robot_agent 表：**
- `idx_robot_agent_scope`: scope_type, scope_id
- `idx_robot_agent_type`: agent_type
- `idx_robot_agent_status`: status

## 注意事项

1. **容器镜像准备**：迁移后需要确保容器镜像 `cat2bug/ai-agent:latest` 已准备好并推送到镜像仓库。

2. **容器编排系统**：需要部署并配置容器编排系统（Nomad 或 Kubernetes）。

3. **环境变量**：迁移后的智能体环境变量可能需要根据实际情况调整。

4. **权限配置**：需要为相关角色配置智能体管理的权限。

5. **兼容性**：现有的 `ai_account` 表不会被删除，可以继续使用旧的 AI 功能。

## 回滚

如果需要回滚，可以执行以下操作：

```sql
-- 删除智能体配置表
DROP TABLE IF EXISTS sys_robot_agent;

-- 删除任务队列表
DROP TABLE IF EXISTS sys_agent_task;
```

注意：回滚会丢失所有智能体配置和任务数据。
