# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Cat2Bug-Platform 是一个开源的轻量级 AI 辅助 BUG 管理平台。采用 Spring Boot + Vue.js 前后端分离架构，支持 H2（嵌入式）和 MySQL 数据库。

**版本:** 0.6.1
**技术栈:** Java 8, Spring Boot 2.5.15, Vue 2.7.16, MyBatis, Element UI
**主入口:** [Cat2BugApplication.java](cat2bug-platform-admin/src/main/java/com/cat2bug/Cat2BugApplication.java)

## 常用命令

### 后端开发

```bash
# 构建整个项目
mvn clean package

# 运行应用（开发模式，默认使用 H2 数据库）
mvn spring-boot:run -pl cat2bug-platform-admin

# 使用 MySQL 数据库运行
mvn spring-boot:run -pl cat2bug-platform-admin -Dspring-boot.run.profiles=mysql

# 运行测试
mvn test

# 运行特定模块的测试
mvn test -pl cat2bug-platform-system

# 运行特定测试类
mvn test -Dtest=DefectServiceTest

# 运行特定测试方法
mvn test -Dtest=DefectServiceTest#testCreateDefect

# 跳过测试（仅构建时）
mvn clean package -DskipTests

# 代码格式化（如果配置了）
mvn spotless:apply
```

### 前端开发

```bash
cd cat2bug-platform-ui

# 安装依赖
npm install

# 开发服务器（http://localhost:2222）
npm run dev

# 代码检查
npm run lint

# 生产环境构建
npm run build:prod

# 云部署构建（输出到 dist/）
npm run build:cloud

# 嵌入式部署构建（输出到后端 static 目录）
npm run build:embedded

# 构建并生成分析报告
npm run build:prod --report
```

### 部署

```bash
# 直接运行 JAR（端口 2020）
java -jar cat2bug-platform-admin/target/cat2bug-admin.jar

# 使用服务脚本
./deploy/cat2bug-service.sh start
./deploy/cat2bug-service.sh stop
./deploy/cat2bug-service.sh status

# Docker Compose 部署
cd deploy/docker && docker-compose up -d
```

## 架构概览

### 关键依赖版本

**后端:**
- Spring Boot: 2.5.15
- MyBatis: 通过 PageHelper 1.4.6
- Druid: 1.2.16
- FastJSON: 2.0.60
- JWT (JJWT): 0.9.1
- Hutool: 5.8.25
- Guava: 32.1.3-jre
- OkHttp: 4.12.0
- POI: 5.4.1 (Excel)
- Velocity: 2.3 (代码生成)

**前端:**
- Vue: 2.7.16
- Element UI: 2.15.13
- Axios: 1.7.4
- Vuex: 3.6.0
- Vue Router: 3.4.9

### 模块依赖关系

```
cat2bug-platform-admin (主程序)
  ├─ cat2bug-platform-framework (框架层：安全、配置、AOP)
  ├─ cat2bug-platform-system (业务层：服务、Mapper、Domain)
  ├─ cat2bug-platform-api (Open API 模块)
  ├─ cat2bug-platform-ai (AI 集成)
  └─ cat2bug-platform-im (即时通讯)

cat2bug-platform-framework
  └─ cat2bug-platform-common (通用工具、常量、基类)

cat2bug-platform-system
  └─ cat2bug-platform-common
```

### 后端分层架构

```
Controller (admin/web/controller)
  → Service (system/service)
  → Mapper (system/mapper)
  → Domain (system/domain)
```

**关键约定:**
- Controller 返回统一格式 `AjaxResult` (extends HashMap)
  ```java
  {
    "code": 200,
    "msg": "success",
    "data": {...}
  }
  ```
- Service 使用接口 + 实现类模式，事务用 `@Transactional`
- Mapper 继承 `BaseMapper<T>` 获得通用 CRUD
- Domain 继承 `BaseEntity` 获得 createTime、updateTime 等字段
- 分页使用 PageHelper，通过 `startPage()` 方法启用

### 双重安全配置（重要架构特点）

系统使用两套独立的 Spring Security 配置：

1. **JWT 认证** (@Order(2)) - 标准用户登录
   - 过滤器: [JwtAuthenticationTokenFilter](cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter/JwtAuthenticationTokenFilter.java)
   - Token 服务: [TokenService](cat2bug-platform-framework/src/main/java/com/cat2bug/framework/web/service/TokenService.java)
   - Token 有效期: 240 分钟
   - 用户信息缓存在 J2Cache (L1: Caffeine, L2: Redis)

2. **API Key 认证** (@Order(1)) - Open API
   - 配置: [ApiSecurityConfig.java](cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/ApiSecurityConfig.java)
   - 仅应用于 `/api/**` 路径
   - 通过 `cat2bug.api.enabled=true` 启用

**权限控制:**
- Controller 方法用 `@PreAuthorize` 注解
- 数据权限过滤通过 [DataScopeAspect](cat2bug-platform-framework/src/main/java/com/cat2bug/framework/aspectj/DataScopeAspect.java) AOP 实现
- 数据权限级别: ALL, CUSTOM, DEPT, DEPT_AND_CHILD, SELF

### 数据库配置

**双数据库支持:**
- **H2 (默认):** 嵌入式，数据文件 `./data/cat2bug_platform.mv.db`
  - 配置: [application-h2.yml](cat2bug-platform-admin/src/main/resources/application-h2.yml)
- **MySQL:** 生产环境
  - 配置: [application-mysql.yml](cat2bug-platform-admin/src/main/resources/application-mysql.yml)
  - 切换: `spring.profiles.active=mysql`

**数据库迁移:**
- Flyway 版本管理
- 脚本位置: `src/main/resources/db/migration/{h2|mysql}/`
- 基线版本: 0.5.0

**缓存:**
- J2Cache 两级缓存（L1: Caffeine, L2: Redis）

### AOP 横切关注点

- **LogAspect:** 操作日志（`@Log`）
- **DataScopeAspect:** 数据权限过滤（`@DataScope`）
- **RateLimiterAspect:** 接口限流（`@RateLimiter`）
- **DataSourceAspect:** 动态数据源切换（`@DataSource`）

### 模块条件加载

```yaml
cat2bug:
  api:
    enabled: true  # 启用 Open API
  ai:
    enabled: true  # 启用 AI 模块
  open-ai:
    enabled: true  # 启用新版 OpenAI 集成
```

模块通过 `@ConditionalOnProperty` 注解实现条件加载，可在配置文件中灵活启用/禁用。

### 国际化

- 后端: Spring i18n (`i18n/messages_{locale}.properties`)
- 前端: vue-i18n (`src/lang/`)
- 支持: zh_CN, zh_TW, en, ru, ja, ko, ar
- 通过 `Accept-Language` 请求头传递

### WebSocket 实时通信

- 服务端点: [MessageWebsocket](cat2bug-platform-common/src/main/java/com/cat2bug/common/websocket/MessageWebsocket.java)
- 连接地址: `/websocket/{memberId}/message`
- 前端: vue-native-websocket

### 代码生成器

- 访问: http://localhost:2020/tool/gen
- 模板引擎: Velocity
- 生成: Controller, Service, Mapper, Domain, Vue 页面
- 模板位置: `cat2bug-platform-generator/src/main/resources/vm/`

### 前端架构

**技术栈:**
- Vue 2.7.16 + Vuex 3.6.0 + Vue Router 3.4.9
- Element UI 2.15.13
- Axios 1.7.4
- vue-i18n 8.22.2 (国际化)
- vue-native-websocket 2.0.15 (WebSocket)

**开发服务器:**
- 端口: 2222
- 代理配置: API → `http://localhost:2020`, WebSocket → `ws://localhost:2020`

**构建优化:**
- Gzip 压缩
- 代码分割: ElementUI、vendor libs、commons
- SVG sprite loader (图标)
- Bundle 分析报告 (`--report` 参数)

## 开发规范

### 代码放置位置

**后端:**
- Controller: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/controller/`
- Service: `cat2bug-platform-system/src/main/java/com/cat2bug/system/service/`
- Mapper: `cat2bug-platform-system/src/main/java/com/cat2bug/system/mapper/`
- Domain: `cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/`
- MyBatis XML: `cat2bug-platform-system/src/main/resources/mapper/`

**前端:**
```
cat2bug-platform-ui/src/
├── api/          # API 调用（按功能分文件）
├── components/   # 可复用组件
├── views/        # 页面组件
│   ├── system/   # 业务页面
│   └── admin/    # 管理页面
├── store/        # Vuex 状态管理
├── router/       # 路由配置
└── utils/        # 工具函数
```

### 命名约定

- Java 类: PascalCase (`DefectController`, `ProjectService`)
- Java 方法: camelCase (`getDefectList`, `updateProject`)
- 数据库表: snake_case 带前缀 (`sys_defect`, `sys_project`)
- Vue 组件: PascalCase (`DefectList.vue`)
- Vue 方法: camelCase (`handleSubmit`, `fetchData`)

## API 路由结构

```
/system/defect/*   # 缺陷管理
/system/project/*  # 项目管理
/system/team/*     # 团队管理
/system/member/*   # 成员管理
/system/case/*     # 测试用例管理
/system/report/*   # 报告生成
/api/*             # Open API（需要 API Key）
/ai/*              # AI 服务
/im/*              # 即时通讯
/monitor/*         # 系统监控
/tool/*            # 工具（代码生成器等）
```

## 配置文件

**后端主配置:** [application.yml](cat2bug-platform-admin/src/main/resources/application.yml)
- 服务端口: 2020
- Token 过期: 240 分钟
- 文件上传: 500MB

**前端配置:** [vue.config.js](cat2bug-platform-ui/vue.config.js)
- 环境变量: `.env.{development|production|cloud|embedded}`
- 开发代理: `http://localhost:2020`
- 构建输出:
  - `build:prod` / `build:cloud` → `dist/`
  - `build:embedded` → `cat2bug-platform-admin/src/main/resources/static/`

## Claude Code 技能命令

项目配置了以下 Claude Code 技能，可通过斜杠命令调用：

- `/plan` - 需求规划命令（用于非平凡功能改动的前期规划）
- `/code-review` - 代码审查命令（安全审查、代码质量检查）
- `/tdd` - 测试驱动开发命令（TDD 流程辅助）
- `/build-fix` - 构建错误修复命令（增量修复构建错误）

## 团队协作约束

1. **Git 工作流：强制使用 worktree**
   - 每个功能/修复必须在独立的 git worktree 分支中开发
   - 创建 worktree 前必须询问从哪个分支创建（main/develop/其他）
   - 详细规范见 [.claude/rules/git.md](.claude/rules/git.md)

2. **默认先用 Plan Mode 分析，再动代码**
   - 对于非平凡的功能改动，使用 `/plan` 命令先规划
   - 明确风险、依赖、验收标准后再实施

3. **任何会影响行为的改动：必须补测试/补文档**
   - 新增 API 接口必须补充测试
   - 修改核心业务逻辑必须更新相关文档

4. **涉及凭证/权限：先安全审查，再合并**
   - 禁止硬编码密钥、Token
   - 使用 `/code-review` 命令进行安全审查

5. **构建失败必须修复后才能继续**
   - 使用 `/build-fix` 命令增量修复构建错误

## 开发规范详细文档

项目在 `.claude/rules/` 目录下包含详细的开发规范：

- [coding-style.md](.claude/rules/coding-style.md) - 代码风格、命名、注释、错误处理规范
- [git.md](.claude/rules/git.md) - Git Worktree 工作流、分支策略、提交规范
- [security.md](.claude/rules/security.md) - 安全规则、密钥管理、输入校验、权限控制
- [testing.md](.claude/rules/testing.md) - 测试规则、TDD 流程、测试覆盖率要求

## 常见问题

### 端口被占用
后端默认 2020，在 `application.yml` 中修改 `server.port`

### 数据库连接失败
- H2: 确保 `./data/` 目录存在且可写
- MySQL: 检查 `application-mysql.yml` 配置

### WebSocket 连接失败
- 确保后端已启动
- 检查 [SecurityConfig.java](cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/SecurityConfig.java) CORS 配置
