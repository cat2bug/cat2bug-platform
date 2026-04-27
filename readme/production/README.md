# Cat2Bug介绍

欢迎使用 Cat2Bug 轻量级 BUG 管理平台！

## 关于 Cat2Bug

Cat2Bug-Platform 是一个开源的轻量级 BUG 管理平台，采用 Spring Boot + Vue.js 前后端分离架构。系统起名叫"猫陪我改BUG"，是因为每天坐在工作台，家里的猫总会过来陪伴。系统的宗旨是轻量化、简单、便捷，让测试管理更贴近生活。

## 核心特点

### 简洁至上
以 BUG 管理为核心关注点，免去了项目管理中繁多的流程和模块，不管是新手还是老鸟，都可以快速上手，将精力专注在质量管理之中。

### 专注测试
Cat2Bug-Platform 专门针对测试工作的各个环节及不同角色，开发了专属的功能，解决传统缺陷系统只负责录入，忽略测试效率的问题。

### AI 智能化
借助于人工智能化技术的飞速提升，系统通过 AI 辅助进行缺陷的发现与管理。在测试用例的生成、自动化测试等方面，都有不俗的表现。

## 快速导航

### 快速开始
- [快速开始](quick-start.md) - 快速上手指南

### 角色介绍
- [团队创建人](role-guide/team-creator.md) - 团队创建人的权限和职责
- [团队管理员](role-guide/team-admin.md) - 团队管理员的权限和职责
- [团队普通人员](role-guide/team-member.md) - 团队普通成员的权限和职责
- [项目创建人](role-guide/project-creator.md) - 项目创建人的权限和职责
- [项目管理员](role-guide/project-admin.md) - 项目管理员的权限和职责
- [开发](role-guide/developer.md) - 开发人员的权限和职责
- [测试](role-guide/tester.md) - 测试人员的权限和职责
- [外部人员](role-guide/external.md) - 外部人员的权限和职责
- [系统管理员](role-guide/system-admin.md) - 系统管理员的权限和职责

### 用户指南

#### 用户管理
- [登录](user-guide/user-management/user-login.md) - 用户登录说明
- [注册](user-guide/user-management/user-register.md) - 用户注册说明
- [个人中心](user-guide/user-management/user-profile.md) - 个人信息管理

#### 团队和项目
- [团队管理](user-guide/team-manage.md) - 创建和管理团队
- [项目管理](user-guide/project-manage.md) - 创建和管理项目

#### 当前项目功能
- [仪表盘](user-guide/current-project/dashboard.md) - 项目数据概览
- 测试用例
  - [测试用例概述](user-guide/current-project/case.md) - 测试用例介绍和名词解释
  - [用例列表](user-guide/current-project/case/case-list.md) - 查看和管理测试用例列表
  - [新建用例](user-guide/current-project/case/case-create.md) - 手动创建测试用例
  - [编辑用例](user-guide/current-project/case/case-edit.md) - 编辑已有测试用例
  - [删除用例](user-guide/current-project/case/case-delete.md) - 删除测试用例（单独或批量）
  - [导入用例](user-guide/current-project/case/case-import.md) - 从 Excel 文件导入测试用例
  - [导出用例](user-guide/current-project/case/case-export.md) - 导出测试用例为 Excel 文件
  - [AI用例生成](user-guide/current-project/case/case-ai.md) - 使用 AI 自动生成测试用例
- 测试计划
  - [测试计划概述](user-guide/current-project/plan.md) - 测试计划介绍和功能指南
  - [创建测试计划](user-guide/current-project/plan/plan-create.md) - 如何创建和配置测试计划
  - [执行测试计划](user-guide/current-project/plan/plan-execute.md) - 执行测试用例和跟踪进度
  - [测试报告](user-guide/current-project/plan/plan-report.md) - 生成和导出测试报告
  - [测试计划模板](user-guide/current-project/plan/plan-template.md) - 使用预定义的测试计划模板
  - [最佳实践](user-guide/current-project/plan/plan-best-practice.md) - 测试计划的最佳实践建议
- [缺陷管理](user-guide/current-project/defect.md) - 缺陷的全生命周期管理
- [交付物管理](user-guide/current-project/module.md) - 项目需要交付的软/硬件模块
- [报告管理](user-guide/current-project/report.md) - 测试报告的生成和导出
- [文档管理](user-guide/current-project/document.md) - 项目文档的存储和管理

### 管理员指南
- [团队管理](admin-guide/admin-team.md) - 系统管理员的团队管理
- [项目管理](admin-guide/admin-project.md) - 系统管理员的项目管理
- [角色管理](admin-guide/admin-role.md) - 系统角色配置
- [成员管理](admin-guide/admin-member.md) - 系统成员管理

### API 文档
- [API 介绍](api/api-intro.md) - API 接口说明
- [交付物接口](api/api-deliverable.md) - 交付物相关接口
- [测试用例接口](api/api-case.md) - 测试用例相关接口
- [缺陷接口](api/api-defect.md) - 缺陷相关接口
- [报告接口](api/api-report-defect.md) - 报告相关接口
- [成员接口](api/api-member.md) - 成员相关接口
- [文件接口](api/api-file.md) - 文件上传下载接口
- [项目接口](api/api-project.md) - 项目相关接口

### 其他
- [常见问题](faq.md) - 常见问题解答

## 系统架构

### 技术栈

**后端技术：**
- Spring Boot 2.x
- MyBatis Plus
- MySQL / H2
- Redis
- WebSocket

**前端技术：**
- Vue.js 2.x
- Element UI
- Vuex
- Vue Router

## 演示环境

- 在线演示：https://www.cat2bug.com:8022
- 演示账号：
  - 管理员：admin / cat2bug
  - 普通用户：demo / 123456

## 技术支持

- 官方网站：https://www.cat2bug.com
- Gitee：https://gitee.com/cat2bug/cat2bug-platform
- GitHub：https://github.com/cat2bug/cat2bug-platform
- 问题反馈：https://gitee.com/cat2bug/cat2bug-platform/issues

## 开源协议

MIT Licensed | Copyright © 2023-2026 cat2bug.com
