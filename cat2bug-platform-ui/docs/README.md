# Cat2Bug 系统文档

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

### 系统功能介绍

#### 团队管理
- [团队管理](./team-manage.md) - 创建和管理团队
- [项目管理](./project-manage.md) - 创建和管理项目
- [团队设置](./team-setting.md) - 团队配置和成员管理

#### 当前项目管理
- [缺陷管理](./defect.md) - 缺陷的全生命周期管理
- [测试用例](./case.md) - 测试用例的创建和维护
- [测试计划](./plan.md) - 测试计划的制定和执行
- [交付物管理](./module.md) - 项目交付物的管理
- [报告管理](./report.md) - 测试报告的生成和导出
- [文档管理](./document.md) - 项目文档的存储和管理

### 其他文档
- [快速开始](./quick-start.md) - 快速上手指南
- [用户指南](./user-guide.md) - 用户使用指南
- [管理员指南](./admin-guide.md) - 管理员配置指南
- [API 文档](./api.md) - 开放 API 接口说明
- [常见问题](./faq.md) - 常见问题解答

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
- GitHub：https://github.com/cat2bug/cat2bug-platform
- Gitee：https://gitee.com/cat2bug/cat2bug-platform
- 问题反馈：https://gitee.com/cat2bug/cat2bug-platform/issues

## 开源协议

MIT Licensed | Copyright © 2023-2025 cat2bug.com
