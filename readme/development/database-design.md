# Cat2Bug-Platform 数据库设计文档

## 1. 概述

Cat2Bug-Platform 是一个轻量化的缺陷管理平台，采用 MySQL/H2 数据库存储。本文档详细描述了系统的数据库设计，包括核心业务表、系统管理表、AI 模块表等。

**数据库版本**: 0.6.1  
**字符集**: utf8mb4  
**排序规则**: utf8mb4_bin
**存储引擎**: InnoDB

---

## 2. 数据库架构

### 2.1 模块划分

数据库表按功能模块划分为以下几类：

- **核心业务模块**: 团队、项目、缺陷、测试用例、测试计划
- **用户权限模块**: 用户、角色、权限、部门
- **AI 模块**: AI 账号、AI 模块配置
- **通讯模块**: 项目配置、用户配置
- **系统管理模块**: 配置、字典、日志、通知
- **报告模块**: 报告、报告模板
- **文档模块**: 文档管理
- **定时任务模块**: Quartz 调度器相关表
- **代码生成模块**: 代码生成器配置

---

## 3. 核心业务表设计

### 3.1 团队管理

#### sys_team (团队表)

团队是 Cat2Bug 平台的顶层组织单位，一个团队可以包含多个项目。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| team_id | bigint | 团队ID | 主键，自增 |
| team_name | varchar(64) | 团队名称 | 唯一索引 |
| team_icon | varchar(255) | 团队图标 | |
| introduce | varchar(255) | 团队介绍 | |
| is_del | tinyint | 是否删除 | |
| is_lock | tinyint(3) | 是否锁定 | 索引 |
| lock_remark | varchar(255) | 锁定描述 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 团队名称全局唯一
- 支持软删除（is_del）和锁定（is_lock）机制
- 团队是项目的容器，删除团队会影响其下所有项目

---

### 3.2 项目管理

#### sys_project (项目表)

项目是缺陷管理的基本单位，隶属于某个团队。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| project_id | bigint | 项目ID | 主键，自增 |
| project_name | varchar(64) | 项目名称 | 非空 |
| project_icon | varchar(255) | 项目图标 | |
| project_introduce | varchar(255) | 项目介绍 | |
| team_id | bigint | 团队ID | 非空，外键索引 |
| project_state | int | 项目状态 | 0=删除，1=运行 |
| is_lock | tinyint(3) | 是否禁用 | 索引 |
| lock_remark | varchar(255) | 禁用描述 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 项目必须归属于某个团队
- 项目状态控制项目的可见性和可用性
- 支持项目级别的锁定功能

---

### 3.3 缺陷管理

#### sys_defect (缺陷表)

缺陷表是系统的核心表，记录所有 Bug、需求、任务等信息。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| defect_id | bigint | 缺陷ID | 主键，自增 |
| defect_type | int(1) | 缺陷类型 | 非空 |
| defect_name | varchar(1024) | 缺陷标题 | 非空 |
| defect_describe | text | 缺陷描述 | |
| defect_state | int(1) | 缺陷状态 | 非空 |
| defect_level | varchar(10) | 缺陷等级 | |
| project_id | bigint | 项目ID | 非空 |
| project_num | int | 项目编号 | |
| module_id | bigint | 测试模块ID | |
| module_version | varchar(128) | 测试模块版本 | |
| test_plan_id | bigint | 测试计划ID | |
| case_id | bigint | 测试用例ID | |
| case_step_id | bigint | 用例步骤ID | |
| handle_by | json | 处理人ID | JSON 数组 |
| handle_time | datetime | 处理时间 | |
| data_sources | int | 数据来源 | |
| data_sources_params | varchar(255) | 数据来源参数 | |
| img_urls | varchar(5000) | 图片集合 | |
| annex_urls | varchar(5000) | 附件集合 | |
| defect_key | varchar(1024) | 缺陷关键字 | |
| defect_group_key | varchar(512) | 缺陷组关键字 | |
| create_by | varchar(64) | 创建者 | |
| create_by_id | bigint | 创建者ID | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_by_id | bigint | 更新者ID | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持多种缺陷类型：Bug、需求、任务等
- 缺陷状态流转：新建 → 处理中 → 已解决 → 已关闭等
- 支持多处理人（handle_by 为 JSON 数组）
- 支持关联测试用例和测试计划
- 支持图片和附件上传
- 缺陷关键字用于搜索和分组

#### sys_defect_log (缺陷日志表)

记录缺陷的所有变更历史。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| defect_log_id | bigint | 日志ID |
| defect_id | bigint | 缺陷ID |
| log_type | int | 日志类型 |
| log_content | text | 日志内容 |
| create_by_id | bigint | 创建者ID |
| create_time | datetime | 创建时间 |

---

### 3.4 测试用例管理

#### sys_case (测试用例表)

测试用例用于规范化测试流程，支持 AI 自动生成。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| case_id | bigint | 测试用例ID | 主键，自增 |
| case_num | bigint | 用例编号 | 联合唯一索引 |
| case_name | varchar(255) | 用例名称 | 联合唯一索引 |
| case_type | int | 用例类型 | |
| case_level | int | 用例级别 | |
| case_step | json | 测试步骤 | JSON 格式 |
| case_expect | text | 预期结果 | |
| case_preconditions | text | 前置条件 | |
| case_data | text | 用例数据 | |
| module_id | bigint | 模块ID | 联合唯一索引 |
| project_id | bigint | 项目ID | 联合唯一索引 |
| img_urls | text | 图片集合 | |
| annex_urls | text | 附件集合 | |
| remark | varchar(500) | 备注 | |
| create_by | varchar(50) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(50) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 用例名称在同一模块和项目下唯一
- 测试步骤使用 JSON 格式存储，支持复杂的步骤定义
- 支持 AI 自动生成测试用例
- 用例可以关联到缺陷

#### sys_case_prompt (用例提示词表)

存储 AI 生成测试用例的提示词模板。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| case_prompt_id | varchar(36) | 测试用例提示词ID | 主键，UUID |
| case_prompt_content | text | 测试用例提示词内容 | 非空，全文索引 |
| project_id | bigint | 项目ID | 索引 |
| create_by_id | bigint | 创建人ID | |
| create_time | datetime | 创建时间 | |

**业务说明**:
- 存储 AI 生成测试用例的提示词
- 支持全文搜索
- 项目级别的提示词管理

---

### 3.5 测试计划管理

#### sys_plan (测试计划表)

测试计划用于组织和管理测试活动。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| plan_id | varchar(36) | 测试计划ID | 主键，UUID |
| plan_number | int | 测试计划编号 | 联合索引 |
| plan_name | varchar(255) | 测试计划名称 | 非空 |
| plan_version | varchar(255) | 测试默认版本 | |
| plan_start_time | datetime | 计划开始时间 | |
| plan_end_time | datetime | 计划结束时间 | |
| project_id | bigint | 项目ID | 索引 |
| report_id | bigint | 报告ID | |
| remark | varchar(1000) | 备注 | |
| create_by_id | bigint | 创建人ID | |
| create_time | datetime | 创建时间 | |
| update_by_id | bigint | 更新人ID | 索引 |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 测试计划使用 UUID 作为主键
- 支持版本管理
- 可以关联测试报告

#### sys_plan_item (测试计划项表)

测试计划的具体测试项，关联测试用例。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| plan_item_id | varchar(36) | 测试计划子项ID | 主键，UUID |
| plan_id | varchar(36) | 测试计划ID | 索引 |
| module_id | bigint | 交付物ID | 索引 |
| case_id | bigint | 测试用例ID | |
| plan_item_state | varchar(12) | 计划子项状态 | 索引 |
| defect_ids | varchar(10000) | 缺陷ID | 多个缺陷ID，逗号分隔 |
| update_by_id | bigint | 更新人ID | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 测试计划项关联测试用例
- 支持记录测试过程中发现的缺陷
- 联合索引优化查询性能

---

### 3.6 模块管理

#### sys_module (模块表)

模块用于组织项目的功能结构，支持树形结构。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| module_id | bigint | 模块ID | 主键，自增 |
| module_name | varchar(128) | 模块名称 | 索引 |
| module_pid | bigint | 父模块ID | 默认 0 |
| project_id | bigint | 项目ID | |
| remark | varchar(255) | 备注 | |
| annex_urls | varchar(5000) | 附件数组 | |
| create_by_id | bigint | 创建成员 | |
| create_time | datetime | 创建时间 | |
| update_by_id | bigint | 更新成员 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持树形结构（通过 module_pid）
- 模块可以关联缺陷和测试用例
- 支持附件上传

---

## 4. 用户权限模块

### 4.1 用户管理

#### sys_user (用户信息表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_id | bigint | 用户ID | 主键，自增 |
| user_name | varchar(30) | 用户账号 | 非空 |
| nick_name | varchar(30) | 用户昵称 | 非空 |
| user_type | varchar(2) | 用户类型 | 00=系统用户，01=API |
| password | varchar(100) | 密码 | |
| email | varchar(50) | 用户邮箱 | |
| phonenumber | varchar(16) | 手机号码 | 唯一索引 |
| sex | char(1) | 用户性别 | 0=男，1=女，2=未知 |
| avatar | varchar(100) | 头像地址 | |
| status | char(1) | 帐号状态 | 0=正常，1=停用 |
| del_flag | char(1) | 删除标志 | 0=存在，2=删除 |
| dept_id | bigint(20) | 部门ID | |
| login_ip | varchar(128) | 最后登录IP | |
| login_date | datetime | 最后登录时间 | |
| ding_user_id | varchar(255) | 钉钉账号 | |
| wechat_user_id | varchar(255) | 微信账号 | |
| wechat_mp_user_id | varchar(255) | 微信小程序openid | 索引 |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持多种登录方式：账号密码、钉钉、微信、微信小程序
- 手机号码全局唯一
- 支持软删除
- 用户类型区分系统用户和 API 用户

### 4.2 角色权限管理

#### sys_role (角色表)

系统角色定义，支持团队角色和项目角色。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| role_id | bigint | 角色ID | 主键，自增 |
| role_name | varchar(30) | 角色名称 | 非空 |
| role_key | varchar(100) | 角色权限字符串 | 非空 |
| role_sort | int | 显示顺序 | 非空 |
| data_scope | char(1) | 数据范围 | 1=全部，2=自定义，3=本部门，4=本部门及以下 |
| menu_check_strictly | tinyint(1) | 菜单树选择项是否关联显示 | |
| dept_check_strictly | tinyint(1) | 部门树选择项是否关联显示 | |
| status | char(1) | 角色状态 | 0=正常，1=停用 |
| del_flag | char(1) | 删除标志 | 0=存在，2=删除 |
| is_team_role | tinyint(1) | 是否是团队角色 | |
| is_project_role | tinyint(1) | 是否是项目角色 | |
| role_name_i18n_key | varchar(30) | 角色名称国际化标记 | |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持系统角色、团队角色、项目角色
- 数据权限控制粒度灵活
- 支持国际化

#### sys_menu (菜单权限表)

系统菜单和权限定义。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| menu_id | bigint | 菜单ID | 主键，自增 |
| menu_name | varchar(50) | 菜单名称 | 非空 |
| parent_id | bigint | 父菜单ID | 默认 0 |
| order_num | int | 显示顺序 | 默认 0 |
| path | varchar(200) | 路由地址 | |
| component | varchar(255) | 组件路径 | |
| query | varchar(255) | 路由参数 | |
| is_frame | int | 是否为外链 | 0=是，1=否 |
| is_cache | int | 是否缓存 | 0=缓存，1=不缓存 |
| menu_type | char(1) | 菜单类型 | M=目录，C=菜单，F=按钮 |
| visible | char(1) | 菜单状态 | 0=显示，1=隐藏 |
| status | char(1) | 菜单状态 | 0=正常，1=停用 |
| perms | varchar(100) | 权限标识 | |
| icon | varchar(100) | 菜单图标 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |
| remark | varchar(500) | 备注 | |

**业务说明**:
- 支持树形菜单结构
- 菜单类型：目录、菜单、按钮
- 权限标识用于后端权限控制

#### sys_dept (部门表)

组织架构部门管理。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| dept_id | bigint | 部门ID | 主键，自增 |
| parent_id | bigint | 父部门ID | 默认 0 |
| ancestors | varchar(50) | 祖级列表 | |
| dept_name | varchar(30) | 部门名称 | |
| order_num | int | 显示顺序 | 默认 0 |
| leader | varchar(20) | 负责人 | |
| phone | varchar(11) | 联系电话 | |
| email | varchar(50) | 邮箱 | |
| status | char(1) | 部门状态 | 0=正常，1=停用 |
| del_flag | char(1) | 删除标志 | 0=存在，2=删除 |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持树形部门结构
- ancestors 字段存储祖级路径，便于查询

#### sys_post (岗位表)

岗位信息管理。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| post_id | bigint | 岗位ID | 主键，自增 |
| post_code | varchar(64) | 岗位编码 | 非空 |
| post_name | varchar(50) | 岗位名称 | 非空 |
| post_sort | int | 显示顺序 | 非空 |
| status | char(1) | 状态 | 0=正常，1=停用 |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

### 4.3 用户关系表

#### sys_user_team (用户团队角色表)

用户与团队的多对多关系，包含角色信息。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_team_id | bigint | 用户团队ID | 主键，自增 |
| user_id | bigint | 用户ID | 联合唯一索引 |
| team_id | bigint | 团队ID | 联合唯一索引 |
| team_role_id | bigint | 团队角色ID | 联合唯一索引 |
| team_lock | tinyint(1) | 是否锁定 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

#### sys_user_project (用户项目表)

用户与项目的多对多关系。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_project_id | bigint | 用户项目ID | 主键，自增 |
| user_id | bigint | 用户ID | 联合唯一索引 |
| project_id | bigint | 项目ID | 联合唯一索引 |
| project_lock | tinyint(1) | 是否锁定 | |
| collect | tinyint(1) | 是否收藏 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 用户可以加入多个团队和项目
- 支持项目收藏功能
- 支持锁定机制，控制用户访问权限

#### sys_user_project_role (用户项目角色表)

用户在项目中的角色关系。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_project_role_id | bigint | 用户项目角色ID | 主键，自增 |
| user_project_id | bigint | 用户项目ID | 联合唯一索引 |
| role_id | bigint | 角色ID | 联合唯一索引 |

**业务说明**:
- 关联 sys_user_project 和 sys_role
- 一个用户在一个项目中可以有多个角色

#### sys_user_role (用户角色表)

用户与系统角色的关系。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_id | bigint | 用户ID | 联合主键 |
| role_id | bigint | 角色ID | 联合主键 |

**业务说明**:
- 用户与系统角色的多对多关系
- 用于系统级权限控制

#### sys_role_menu (角色菜单关联表)

角色与菜单的关系。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| role_id | bigint | 角色ID | 联合主键 |
| menu_id | bigint | 菜单ID | 联合主键 |

**业务说明**:
- 角色与菜单的多对多关系
- 用于菜单权限控制

#### sys_role_dept (角色部门关联表)

角色与部门的关系。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| role_id | bigint | 角色ID | 联合主键 |
| dept_id | bigint | 部门ID | 联合主键 |

**业务说明**:
- 用于数据权限控制
- 角色可以访问指定部门的数据

#### sys_user_post (用户岗位关联表)

用户与岗位的关系。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_id | bigint | 用户ID | 联合主键 |
| post_id | bigint | 岗位ID | 联合主键 |

**业务说明**:
- 用户与岗位的多对多关系
- 一个用户可以担任多个岗位

#### sys_user_team_role (用户团队角色关联表)

用户在团队中的角色关系。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_team_role_id | bigint | ID | 主键，自增 |
| user_team_id | bigint | 用户团队ID | 联合唯一索引 |
| role_id | bigint | 角色ID | 联合唯一索引 |

**业务说明**:
- 关联 sys_user_team 和 sys_role
- 定义用户在团队中的角色权限

### 4.4 用户配置和偏好

#### sys_user_config (用户配置表)

用户的个人配置和偏好设置。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_config_id | bigint | 用户配置ID | 主键，自增 |
| user_id | bigint | 用户ID | 索引 |
| current_team_id | bigint | 当前团队ID | |
| current_project_id | bigint | 当前项目ID | |
| exit_time | datetime | 退出时间 | |
| defect_last_access_time | datetime | 最后访问缺陷时间 | |
| life_content | varchar(255) | 人生格言 | |

**业务说明**:
- 记录用户当前工作上下文
- 记录用户最后活动时间
- 支持个性化设置

#### sys_user_defect (用户缺陷表)

用户与缺陷的关系，如收藏等。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| user_defect_id | bigint | 用户缺陷ID | 主键，自增 |
| defect_id | bigint | 缺陷ID | 联合唯一索引 |
| user_id | bigint | 用户ID | 联合唯一索引 |
| collect | tinyint(1) | 是否收藏 | |

**业务说明**:
- 用户可以收藏关注的缺陷
- 支持快速访问收藏的缺陷

#### sys_user_statistic_template (用户统计模板表)

用户自定义的统计模板。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| statistic_template_id | bigint | 统计模版ID | 主键，自增 |
| statistic_templat_code | varchar(255) | 统计模版编码 | |
| module_type | int | 模型类型 | 1=缺陷，联合唯一索引 |
| project_id | bigint | 项目ID | 联合唯一索引 |
| user_id | bigint | 用户ID | 联合唯一索引 |
| statistic_templat_config | json | 统计模版配置 | JSON 格式 |

**业务说明**:
- 用户可以自定义统计图表
- 每个用户在每个项目的每种模块类型只能有一个统计模板
- 配置使用 JSON 格式存储

---

## 5. AI 模块

### 5.1 AI 账号管理

#### ai_account (OpenAI 账号表)

存储项目关联的 AI 服务账号信息。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| account_id | bigint | 账号ID | 主键，自增 |
| account_name | varchar(64) | 账号名称 | 非空 |
| ai_url | varchar(255) | AI 服务网址 | 非空 |
| model_name | varchar(255) | 模型名称 | 非空 |
| max_completion_tokens | bigint | 最大Token | |
| api_key | varchar(255) | 密钥 | 非空 |
| project_id | bigint | 关联项目ID | 非空，索引 |
| create_by | bigint | 创建用户ID | |

**业务说明**:
- 每个项目可以配置自己的 AI 服务
- 支持多种 AI 模型（OpenAI、本地模型等）
- API Key 需要加密存储

### 5.2 AI 模块配置

#### sys_ai_module_config (AI 模块配置表)

配置 AI 功能在各个模块的启用状态和参数。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| ai_id | bigint | 模型ID | 主键，自增 |
| business_module | varchar(255) | 业务模型名称 | |
| image_module | varchar(255) | 图片识别模型名称 | |
| project_id | bigint | 项目ID | 索引 |
| create_time | datetime | 创建时间 | |
| create_by_id | bigint | 创建人ID | |
| update_time | datetime | 更新时间 | |
| update_by_id | bigint | 更新人ID | |

**业务说明**:
- 控制 AI 功能在不同模块的启用
- 支持业务模型和图片识别模型分别配置
- 项目级别的 AI 配置

---

## 6. 项目扩展模块

### 6.1 项目 API 管理

#### sys_project_api (项目 API 表)

管理项目的 API 密钥和访问控制。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| api_id | varchar(32) | 项目API主键 | 主键 |
| project_id | bigint | 项目ID | 联合唯一索引 |
| user_id | bigint | 用户ID | 联合唯一索引 |
| white_list | json | 白名单 | JSON 格式 |
| expire_time | datetime | 有效时间 | |
| remark | varchar(255) | 备注 | |

**业务说明**:
- 每个项目可以生成 API 密钥
- 支持 IP 白名单控制
- 支持过期时间设置

### 6.2 项目缺陷页签配置

#### sys_project_defect_tabs (项目缺陷页签配置表)

用户自定义的缺陷列表页签配置。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| tab_id | bigint | 页签ID | 主键，自增 |
| tab_name | varchar(255) | 页签名称 | 非空 |
| project_id | bigint | 项目ID | 索引 |
| user_id | bigint | 用户ID | 索引 |
| config | json | 配置项 | JSON 格式 |
| tab_sort | int | 排序 | |
| create_time | datetime | 创建时间 | |

**业务说明**:
- 用户可以自定义缺陷列表的筛选条件
- 支持多个页签，快速切换不同视图
- 配置包括筛选条件、列显示等

### 6.3 其他项目表

#### sys_defect_shard (缺陷分片表)

用于缺陷分享功能，生成分享链接。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| defect_shard_id | varchar(32) | 缺陷分享ID | 主键 |
| defect_id | bigint | 缺陷ID | 索引 |
| password | varchar(64) | 分享密码 | |
| create_time | datetime | 创建日期 | |
| create_by_id | bigint | 创建人 | 索引 |
| update_time | datetime | 更新日期 | |
| aging_time | datetime | 时效时间 | |
| aging_hour | int(10) | 时效小时 | |
| default_lang | varchar(32) | 默认语言 | |

**业务说明**:
- 支持缺陷外部分享
- 支持密码保护
- 支持时效控制

#### sys_screen_size (屏幕尺寸表)

记录不同设备的屏幕尺寸信息，用于截图功能。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| screen_size_id | bigint | 屏幕尺寸ID | 主键，自增 |
| name | varchar(32) | 名称 | 非空 |
| width | varchar(12) | 宽 | |
| height | varchar(12) | 高 | |
| remark | varchar(255) | 备注 | |

**业务说明**:
- 预设常见设备屏幕尺寸
- 用于截图功能的尺寸选择

#### sys_temp_file (临时文件表)

临时文件管理，用于文件上传等场景。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| file_id | bigint | 文件ID | 主键，自增 |
| file_name | varchar(128) | 文件名 | |
| file_url | varchar(255) | 文件地址 | |
| src_url | varchar(255) | 来源地址 | |
| file_type | int(10) | 文件类型 | 0=普通文件，1=图片 |
| create_by | varchar(64) | 创建人 | |
| create_time | datetime | 创建时间 | |

**业务说明**:
- 临时存储上传的文件
- 定期清理过期文件
- 支持文件类型区分

---

## 7. 报告模块

### 6.1 报告管理

#### sys_report (报告表)

存储各类测试报告和统计报告。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| report_id | bigint | 报告ID | 主键，自增 |
| report_title | varchar(255) | 报告标题 | 非空 |
| report_time | datetime | 报告时间 | |
| report_description | longtext | 报告描述 | |
| report_data | json | 数据 | JSON 格式 |
| report_data_coder | varchar(255) | 数据解码器 | |
| report_source | varchar(255) | 报告源 | |
| report_key | varchar(512) | 报告KEY | 唯一索引 |
| project_id | bigint | 项目ID | 非空，索引 |
| create_by_id | bigint | 推送人ID | |

**业务说明**:
- 报告数据使用 JSON 格式存储，灵活支持各种报告类型
- report_key 用于防止重复报告
- 支持自定义数据解码器

#### sys_report_template (报告模板表)

存储报告模板，支持动态生成报告。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| template_id | bigint | 报告模版ID | 主键，自增 |
| template_key | varchar(64) | 模版唯一标识 | 非空 |
| template_title | varchar(512) | 模版标题 | |
| module_type | varchar(64) | 交付物类型 | |
| template_content | longtext | 模版内容 | |
| template_icon_url | varchar(255) | 模版图标路径 | |
| major_version | int | 主版本 | 默认 0 |
| minor_version | int | 次版本 | 默认 0 |
| is_shop | tinyint | 是否是商店模版 | 默认 0 |
| project_id | bigint | 项目ID | |
| update_by_id | bigint | 更新用户 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持版本管理（主版本.次版本）
- 模版内容使用 longtext 存储，支持复杂的模板定义
- 支持模版商店功能（is_shop）
- 模版可以是全局的或项目级别的

---

## 8. 文档模块

### 7.1 文档管理

#### sys_document (文档表)

项目文档管理，支持文件夹和文件。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| doc_id | bigint | 文档ID | 主键，自增 |
| doc_name | varchar(255) | 文档名称 | |
| doc_type | int | 文档类型 | 0=文件夹，1=文件 |
| doc_pid | bigint | 文件夹ID | |
| file_extension | varchar(12) | 文件类型 | |
| file_url | varchar(255) | 文件路径 | |
| file_version | int | 文档版本 | |
| project_id | bigint | 项目ID | |
| doc_remakr | varchar(255) | 备注 | |
| create_by_id | bigint | 创建人ID | |
| create_time | datetime | 创建时间 | |
| update_by_id | bigint | 更新人ID | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持树形文件夹结构
- 支持文档版本管理
- 文件存储在文件系统，数据库只存储路径

---

## 9. 系统管理模块

### 8.1 系统配置

#### sys_config (系统配置表)

存储系统级别的配置参数。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| config_id | bigint | 参数主键 | 主键，自增 |
| config_name | varchar(100) | 参数名称 | |
| config_key | varchar(100) | 参数键名 | |
| config_value | varchar(500) | 参数键值 | |
| config_type | char(1) | 系统内置 | Y=是，N=否 |
| project_id | bigint | 项目ID | |
| receiver_id | bigint | 接受者ID | |
| read | int | 读取状态 | 0=未读，1=已读 |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持系统级和项目级配置
- 系统内置配置不可删除
- 配置可以指定接收者

#### sys_dict_type (字典类型表)

字典类型定义。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| dict_id | bigint | 字典主键 | 主键，自增 |
| dict_name | varchar(100) | 字典名称 | |
| dict_type | varchar(100) | 字典类型 | 唯一索引 |
| status | char(1) | 状态 | 0=正常，1=停用 |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 字典类型全局唯一
- 用于定义下拉选项、状态码等枚举值

#### sys_dict_data (字典数据表)

字典数据，用于下拉选项等。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| dict_code | bigint | 字典编码 | 主键，自增 |
| dict_sort | int | 字典排序 | |
| dict_label | varchar(100) | 字典标签 | |
| dict_value | varchar(100) | 字典键值 | |
| dict_type | varchar(100) | 字典类型 | |
| css_class | varchar(100) | 样式属性 | |
| list_class | varchar(100) | 表格回显样式 | |
| is_default | char(1) | 是否默认 | Y=是，N=否 |
| status | char(1) | 状态 | 0=正常，1=停用 |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 字典数据关联字典类型
- 支持排序和默认值设置
- 支持自定义样式

### 8.2 日志管理

#### sys_oper_log (操作日志表)

记录用户的操作行为。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| oper_id | bigint | 日志主键 | 主键，自增 |
| title | varchar(50) | 模块标题 | |
| business_type | int | 业务类型 | 0=其它，1=新增，2=修改，3=删除 |
| method | varchar(100) | 方法名称 | |
| request_method | varchar(10) | 请求方式 | GET/POST/PUT/DELETE |
| operator_type | int | 操作类别 | 0=其它，1=后台用户，2=手机端用户 |
| oper_name | varchar(50) | 操作人员 | |
| dept_name | varchar(50) | 部门名称 | |
| oper_url | varchar(255) | 请求URL | |
| oper_ip | varchar(128) | 主机地址 | |
| oper_location | varchar(255) | 操作地点 | |
| oper_param | varchar(2000) | 请求参数 | |
| json_result | varchar(2000) | 返回参数 | |
| status | int | 操作状态 | 0=正常，1=异常 |
| error_msg | varchar(2000) | 错误消息 | |
| oper_time | datetime | 操作时间 | 索引 |
| cost_time | bigint | 消耗时间 | 毫秒 |

**业务说明**:
- 记录所有用户操作行为
- 支持性能分析（cost_time）
- 多个索引优化查询性能

#### sys_logininfor (登录日志表)

记录用户登录信息。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| info_id | bigint | 访问ID | 主键，自增 |
| user_name | varchar(50) | 用户账号 | |
| ipaddr | varchar(128) | 登录IP地址 | |
| login_location | varchar(255) | 登录地点 | |
| browser | varchar(50) | 浏览器类型 | |
| os | varchar(50) | 操作系统 | |
| status | char(1) | 登录状态 | 0=成功，1=失败 |
| msg | varchar(255) | 提示消息 | |
| login_time | datetime | 访问时间 | 索引 |

**业务说明**:
- 记录登录成功和失败的日志
- 用于安全审计和异常登录检测

#### sys_job_log (定时任务日志表)

记录定时任务执行日志。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| job_log_id | bigint | 任务日志ID | 主键，自增 |
| job_name | varchar(64) | 任务名称 | |
| job_group | varchar(64) | 任务组名 | |
| invoke_target | varchar(500) | 调用目标字符串 | |
| job_message | varchar(500) | 日志信息 | |
| status | char(1) | 执行状态 | 0=正常，1=失败 |
| exception_info | varchar(2000) | 异常信息 | |
| create_time | datetime | 创建时间 | |

**业务说明**:
- 记录定时任务的执行情况
- 保存异常信息用于问题排查

### 8.3 通知管理

#### sys_notice (通知公告表)

系统通知和公告管理。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| notice_id | varchar(32) | 公告ID | 主键 |
| notice_title | varchar(255) | 公告标题 | 非空 |
| notice_type | char(1) | 公告类型 | 1=通知，2=公告 |
| notice_content | longtext | 公告内容 | |
| status | char(1) | 公告状态 | 0=正常，1=关闭 |
| project_id | bigint | 项目ID | |
| group_name | varchar(255) | 分组 | |
| receiver_id | bigint | 接收人 | 索引 |
| is_read | tinyint(1) | 是否已读 | |
| remark | varchar(255) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持系统级和项目级通知
- 支持分组管理通知
- 支持已读/未读状态跟踪
- 联合索引优化查询性能

#### sys_comment (评论表)

支持对缺陷日志、用例等进行评论。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| comment_id | bigint | 评论ID | 主键，自增 |
| comment_content | varchar(1024) | 评论内容 | 非空 |
| module_type | varchar(12) | 所属模块 | defect_log=缺陷日志评论 |
| correlation_id | bigint | 关联ID | 非空 |
| create_by_id | bigint | 创建人ID | |
| create_time | datetime | 创建时间 | |

**业务说明**:
- 支持多种模块的评论功能
- 通过 module_type 和 correlation_id 关联到具体业务对象
- 评论内容限制 1024 字符

---

## 10. 通讯模块

### 9.1 即时通讯配置

#### im_project_config (项目通讯配置表)

配置项目的第三方通知渠道（钉钉、邮件、企业微信等）。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| config_id | bigint | 配置ID | 主键，自增 |
| project_id | bigint | 项目ID | 联合唯一索引 |
| system_code | varchar(64) | 第三方系统编码 | 联合唯一索引 |
| config_params | json | 配置参数 | JSON 格式 |

**业务说明**:
- 每个项目可以配置多个第三方通知系统
- 同一项目的同一系统编码只能有一个配置
- 配置参数使用 JSON 格式，灵活支持不同系统的参数需求

#### im_user_config (用户通讯配置表)

用户的个人通知偏好设置。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| config_id | bigint | 配置ID | 主键，自增 |
| user_id | bigint | 用户ID | |
| system_code | varchar(64) | 第三方系统编码 | |
| config_params | json | 配置参数 | JSON 格式 |

**业务说明**:
- 用户可以自定义通知接收方式
- 支持多种通知渠道的个性化配置

---

## 11. 定时任务模块

### 10.1 系统定时任务

#### sys_job (定时任务表)

系统定时任务配置。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| job_id | bigint | 任务ID | 主键，自增 |
| job_name | varchar(64) | 任务名称 | 非空 |
| job_group | varchar(64) | 任务组名 | 非空，默认 DEFAULT |
| invoke_target | varchar(500) | 调用目标字符串 | 非空 |
| cron_expression | varchar(255) | cron执行表达式 | |
| misfire_policy | varchar(20) | 计划执行错误策略 | 1=立即执行，2=执行一次，3=放弃执行 |
| concurrent | char(1) | 是否并发执行 | 0=允许，1=禁止 |
| status | char(1) | 状态 | 0=正常，1=暂停 |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持 Cron 表达式定时执行
- 支持并发控制
- 支持错误策略配置

### 10.2 Quartz 调度器

系统使用 Quartz 框架实现定时任务，包含以下表：

- **qrtz_job_details**: 任务详情
- **qrtz_triggers**: 触发器
- **qrtz_cron_triggers**: Cron 触发器
- **qrtz_simple_triggers**: 简单触发器
- **qrtz_fired_triggers**: 已触发的触发器
- **qrtz_locks**: 锁表
- **qrtz_scheduler_state**: 调度器状态
- **qrtz_paused_trigger_grps**: 暂停的触发器组
- **qrtz_calendars**: 日历
- **qrtz_blob_triggers**: Blob 触发器
- **qrtz_simprop_triggers**: 简单属性触发器

---

## 12. 代码生成模块

### 11.1 代码生成器

#### gen_table (代码生成业务表)

存储需要生成代码的表信息。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| table_id | bigint | 编号 | 主键，自增 |
| table_name | varchar(200) | 表名称 | |
| table_comment | varchar(500) | 表描述 | |
| sub_table_name | varchar(64) | 关联子表的表名 | |
| sub_table_fk_name | varchar(64) | 子表关联的外键名 | |
| class_name | varchar(100) | 实体类名称 | |
| tpl_category | varchar(200) | 使用的模板 | crud=单表操作，tree=树表操作 |
| package_name | varchar(100) | 生成包路径 | |
| module_name | varchar(30) | 生成模块名 | |
| business_name | varchar(30) | 生成业务名 | |
| function_name | varchar(50) | 生成功能名 | |
| function_author | varchar(50) | 生成功能作者 | |
| gen_type | char(1) | 生成代码方式 | 0=zip压缩包，1=自定义路径 |
| gen_path | varchar(200) | 生成路径 | |
| options | varchar(1000) | 其他生成选项 | |
| remark | varchar(500) | 备注 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 支持单表和树表代码生成
- 支持主子表关联
- 支持自定义生成路径

#### gen_table_column (代码生成业务字段表)

存储表字段的生成配置。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| column_id | bigint | 编号 | 主键，自增 |
| table_id | varchar(64) | 归属表编号 | |
| column_name | varchar(200) | 列名称 | |
| column_comment | varchar(500) | 列描述 | |
| column_type | varchar(100) | 列类型 | |
| java_type | varchar(500) | JAVA类型 | |
| java_field | varchar(200) | JAVA字段名 | |
| is_pk | char(1) | 是否主键 | |
| is_increment | char(1) | 是否自增 | |
| is_required | char(1) | 是否必填 | |
| is_insert | char(1) | 是否为插入字段 | |
| is_edit | char(1) | 是否编辑字段 | |
| is_list | char(1) | 是否列表字段 | |
| is_query | char(1) | 是否查询字段 | |
| query_type | varchar(200) | 查询方式 | EQ=等于，NE=不等于，GT=大于，LIKE=模糊 |
| html_type | varchar(200) | 显示类型 | input=文本框，textarea=文本域，select=下拉框 |
| dict_type | varchar(200) | 字典类型 | |
| sort | int | 排序 | |
| create_by | varchar(64) | 创建者 | |
| create_time | datetime | 创建时间 | |
| update_by | varchar(64) | 更新者 | |
| update_time | datetime | 更新时间 | |

**业务说明**:
- 配置字段的生成规则
- 支持字段级别的显示和查询配置
- 支持字典类型关联

---

## 13. 其他辅助表

### 12.1 数据库版本管理

#### sys_db_version (数据库版本表)

Flyway 数据库版本管理表。

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| installed_rank | int | 安装顺序 | 主键 |
| version | varchar(50) | 版本号 | |
| description | varchar(200) | 描述 | |
| type | varchar(20) | 类型 | SQL/JAVA |
| script | varchar(1000) | 脚本名称 | |
| checksum | int | 校验和 | |
| installed_by | varchar(100) | 安装者 | |
| installed_on | timestamp | 安装时间 | |
| execution_time | int | 执行时间 | 毫秒 |
| success | tinyint(1) | 是否成功 | |

**业务说明**:
- Flyway 自动管理
- 记录所有数据库变更历史
- 用于版本控制和回滚

### 12.2 其他辅助表说明

以下表在前面章节已有详细说明，这里不再重复：

- **sys_case_prompt**: 见 3.4 测试用例管理
- **sys_defect_shard**: 见 6.3 其他项目表
- **sys_screen_size**: 见 6.3 其他项目表
- **sys_temp_file**: 见 6.3 其他项目表

---

## 14. 数据库关系图

### 13.1 核心业务关系

```
团队 (sys_team)
  └─ 1:N ─ 项目 (sys_project)
              ├─ 1:N ─ 缺陷 (sys_defect)
              │         ├─ N:1 ─ 模块 (sys_module)
              │         ├─ N:1 ─ 测试用例 (sys_case)
              │         ├─ N:1 ─ 测试计划 (sys_plan)
              │         ├─ 1:N ─ 缺陷日志 (sys_defect_log)
              │         └─ 1:N ─ 缺陷分享 (sys_defect_shard)
              ├─ 1:N ─ 测试用例 (sys_case)
              │         ├─ N:1 ─ 模块 (sys_module)
              │         └─ 1:N ─ 测试计划项 (sys_plan_item)
              ├─ 1:N ─ 测试计划 (sys_plan)
              │         ├─ 1:N ─ 测试计划项 (sys_plan_item)
              │         │         ├─ N:1 ─ 测试用例 (sys_case)
              │         │         └─ N:1 ─ 模块 (sys_module)
              │         └─ 1:1 ─ 报告 (sys_report)
              ├─ 1:N ─ 模块 (sys_module)
              │         └─ 1:N ─ 子模块 (sys_module) [树形结构]
              ├─ 1:N ─ 文档 (sys_document)
              │         └─ 1:N ─ 子文档 (sys_document) [树形结构]
              ├─ 1:N ─ 报告 (sys_report)
              │         └─ N:1 ─ 报告模板 (sys_report_template)
              ├─ 1:N ─ AI 账号 (ai_account)
              ├─ 1:N ─ AI 模块配置 (sys_ai_module_config)
              ├─ 1:N ─ 项目 API (sys_project_api)
              ├─ 1:N ─ 项目通讯配置 (im_project_config)
              ├─ 1:N ─ 缺陷页签配置 (sys_project_defect_tabs)
              └─ 1:N ─ 用例提示词 (sys_case_prompt)
```

### 13.2 用户关系

```
用户 (sys_user)
  ├─ N:1 ─ 部门 (sys_dept)
  ├─ N:M ─ 团队 (sys_team) ─ 通过 sys_user_team
  │         └─ 关联角色 (sys_user_team_role → sys_role)
  ├─ N:M ─ 项目 (sys_project) ─ 通过 sys_user_project
  │         └─ 关联角色 (sys_user_project_role → sys_role)
  ├─ N:M ─ 角色 (sys_role) ─ 通过 sys_user_role
  ├─ N:M ─ 岗位 (sys_post) ─ 通过 sys_user_post
  ├─ 1:N ─ 缺陷 (sys_defect) ─ 创建者/处理者
  ├─ 1:N ─ 测试用例 (sys_case) ─ 创建者
  ├─ 1:N ─ 测试计划 (sys_plan) ─ 创建者
  ├─ 1:N ─ 报告 (sys_report) ─ 创建者
  ├─ 1:N ─ 文档 (sys_document) ─ 创建者
  ├─ 1:N ─ 缺陷日志 (sys_defect_log) ─ 创建者
  ├─ 1:N ─ 评论 (sys_comment) ─ 创建者
  ├─ 1:N ─ 通知 (sys_notice) ─ 接收者
  ├─ 1:1 ─ 用户配置 (sys_user_config)
  ├─ N:M ─ 缺陷收藏 (sys_user_defect)
  ├─ 1:N ─ 用户通讯配置 (im_user_config)
  ├─ 1:N ─ 统计模板 (sys_user_statistic_template)
  └─ 1:N ─ 缺陷页签配置 (sys_project_defect_tabs)
```

### 13.3 权限关系

```
角色 (sys_role)
  ├─ N:M ─ 菜单 (sys_menu) ─ 通过 sys_role_menu
  │         └─ 1:N ─ 子菜单 (sys_menu) [树形结构]
  ├─ N:M ─ 部门 (sys_dept) ─ 通过 sys_role_dept
  │         └─ 1:N ─ 子部门 (sys_dept) [树形结构]
  ├─ N:M ─ 用户 (sys_user) ─ 通过 sys_user_role
  ├─ N:M ─ 团队用户 (sys_user_team) ─ 通过 sys_user_team_role
  └─ N:M ─ 项目用户 (sys_user_project) ─ 通过 sys_user_project_role
```

### 13.4 测试关系

```
测试计划 (sys_plan)
  ├─ N:1 ─ 项目 (sys_project)
  ├─ 1:N ─ 测试计划项 (sys_plan_item)
  │         ├─ N:1 ─ 测试用例 (sys_case)
  │         ├─ N:1 ─ 模块 (sys_module)
  │         └─ 关联多个缺陷 (defect_ids 字段)
  └─ 1:1 ─ 测试报告 (sys_report)

测试用例 (sys_case)
  ├─ N:1 ─ 项目 (sys_project)
  ├─ N:1 ─ 模块 (sys_module)
  ├─ 1:N ─ 缺陷 (sys_defect)
  └─ 1:N ─ 测试计划项 (sys_plan_item)

缺陷 (sys_defect)
  ├─ N:1 ─ 项目 (sys_project)
  ├─ N:1 ─ 模块 (sys_module)
  ├─ N:1 ─ 测试用例 (sys_case)
  ├─ N:1 ─ 测试计划 (sys_plan)
  ├─ 1:N ─ 缺陷日志 (sys_defect_log)
  ├─ 1:N ─ 缺陷分享 (sys_defect_shard)
  └─ N:M ─ 用户收藏 (sys_user_defect)
```

### 13.5 系统管理关系

```
字典类型 (sys_dict_type)
  └─ 1:N ─ 字典数据 (sys_dict_data)

定时任务 (sys_job)
  └─ 1:N ─ 任务日志 (sys_job_log)

代码生成表 (gen_table)
  └─ 1:N ─ 表字段 (gen_table_column)

评论 (sys_comment)
  └─ N:1 ─ 关联对象 (通过 module_type + correlation_id)
              ├─ 缺陷日志 (sys_defect_log)
              └─ 其他模块...
```

### 13.6 完整 ER 关系总览

```
核心实体：
- 团队 (sys_team)
- 项目 (sys_project)
- 用户 (sys_user)
- 缺陷 (sys_defect)
- 测试用例 (sys_case)
- 测试计划 (sys_plan)

关联实体：
- 模块 (sys_module) - 组织项目功能结构
- 文档 (sys_document) - 项目文档管理
- 报告 (sys_report) - 测试报告
- 角色 (sys_role) - 权限控制
- 菜单 (sys_menu) - 功能权限
- 部门 (sys_dept) - 组织架构

配置实体：
- AI 配置 (ai_account, sys_ai_module_config)
- 通讯配置 (im_project_config, im_user_config)
- 用户配置 (sys_user_config)
- 系统配置 (sys_config, sys_dict_type, sys_dict_data)

日志实体：
- 缺陷日志 (sys_defect_log)
- 操作日志 (sys_oper_log)
- 登录日志 (sys_logininfor)
- 任务日志 (sys_job_log)
```

---

## 15. 索引设计

### 14.1 主键索引

所有表都有主键索引，大部分使用自增 bigint 类型。

### 14.2 唯一索引

1. **业务唯一性**:
   - sys_team.team_name: 团队名称唯一
   - sys_user.phonenumber: 手机号唯一
   - sys_dict_type.dict_type: 字典类型唯一
   - sys_case(case_name, module_id, project_id): 用例名称在模块内唯一
   - sys_user_project(user_id, project_id): 用户项目关系唯一

2. **防重复**:
   - sys_report.report_key: 防止重复报告
   - im_project_config(project_id, system_code): 项目通知配置唯一

### 14.3 外键索引

1. **关联查询优化**:
   - sys_project.team_id: 加速团队查询
   - sys_defect.project_id: 加速项目缺陷查询
   - sys_case.project_id: 加速项目用例查询
   - ai_account.project_id: 加速项目 AI 账号查询

2. **用户关系索引**:
   - sys_user_config.user_id: 用户配置查询
   - sys_user_defect(defect_id, user_id): 用户缺陷关系
   - sys_project_defect_tabs(project_id, user_id): 用户页签配置

### 14.4 业务索引

1. **状态过滤**:
   - sys_team.is_lock: 锁定状态过滤
   - sys_project.is_lock: 锁定状态过滤
   - sys_oper_log.status: 操作状态过滤

2. **时间查询**:
   - sys_oper_log.oper_time: 操作时间查询
   - sys_logininfor.login_time: 登录时间查询

3. **业务查询**:
   - sys_user.wechat_mp_user_id: 微信小程序登录
   - sys_module.module_name: 模块名称搜索
   - sys_notice(notice_title, group_name, receiver_id): 通知查询优化

---

## 16. 附录

### 16.1 字符集说明

- **utf8mb4**: 支持完整的 Unicode 字符集，包括 Emoji
- **utf8mb4_bin**: 二进制排序，区分大小写

### 16.2 存储引擎

- **InnoDB**: 支持事务、外键、行级锁，适合高并发场景

### 16.3 数据类型选择

- **bigint**: 主键和外键，支持大数据量
- **varchar**: 变长字符串，节省空间
- **text/longtext**: 大文本内容
- **json**: 灵活的结构化数据
- **datetime**: 时间戳，精确到秒

---

## 17. 总结

Cat2Bug-Platform 数据库设计遵循以下原则：

1. **规范化**: 减少数据冗余，保证数据一致性
2. **扩展性**: 支持业务扩展，如 JSON 字段存储灵活数据
3. **性能**: 合理的索引设计，支持高并发访问
4. **安全性**: 敏感数据加密，完善的权限控制
5. **可维护性**: 清晰的命名规范，完整的注释说明

数据库版本随系统升级持续演进，通过 Flyway 实现平滑升级。
