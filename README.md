<div align="center">
  <!-- 与登录页 CatIllustration 同源（login-cat-scene-light/dark.jpg） -->
  <img src="readme/images/login-cat-scene-dark.png" width="550" alt="猫陪我改 BUG — Cat2Bug Platform" style="max-width: 100%; height: auto; margin-top: 50px;" />
</div>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Cat2Bug-Platform v1.1.1</h1>
<h4 align="center">轻量化智能BUG管理平台</h4>

## 平台简介

Cat2Bug-Platform是一套永久免费开源的Bug管理平台，可以完全私有化部署，它利用目前比较流行的AI大数据模型技术作为辅助，快速提升软件管理的质量，我们将毫无保留给个人及团体免费使用。
它的使用人群锁定个人或中小型软件开发团队，Cat2Bug的理念是免去了项目管理中各种重度管理，让个人或团队可以快速上手，把控软件质量。
平台采用JAVA+VUE前后台分离模式开发，支持在各系统平台部署使用。

## 内置功能

1.  工作台：项目落地页；角色预设组件、行排版与自定义页签（私有 / 项目共享），可钉选文档与统计待办。
2.  团队管理：管理团队中的项目、成员；用例类型、评审纪要模板、缺陷属性在团队设置统一维护（仅团队创建人/管理员可配）。
3.  项目管理：管理项目中的成员与模块入口。
4.  用例管理：管理测试用例；勾选用例后可发起可选评审并生成 Markdown 纪要。测试计划里类型和评审只是筛选项，不作为加入计划的门禁。
5.  缺陷管理：管理 BUG、需求、任务；支持自定义字段、页签与批量流转。
6.  源码管理：每项目可托管多个 Git 仓库；在线浏览、分支与 PR、标签与发行版、Webhook；Smart HTTP 克隆/推送。
7.  测试计划：组织用例执行与筛选。
8.  交付物管理：维护项目中的可交付物品。
9.  文档管理：项目文档列表与在线 Markdown。
10. API管理：用于管理API接口密钥。
11. 通知管理：发送系统业务通知到系统内部、邮件、钉钉等平台中。
12. 安装向导：一站式图形化安装配置，引导用户完成数据库、缓存、文件存储、AI及初始管理员账号的设置。

## 最新版本更新说明

当前最新版本是1.1.1

**运行时要求（1.1.1 后端）**：构建与运行需 **JDK 17+**。后端基于 **Quarkus**（默认 Maven 构建，无需 profile），原 Spring Boot 后端已移除，最后版本保留在 tag `spring-final-1.1.0`。Maven 版本号为 1.1.1。

* 项目落地页改为可定制「工作台」：角色预设、组件显隐与行排版、私有/共享自定义页签、组件库搜索与分组预览等
* 用例类型、评审纪要模板、缺陷属性改由团队设置统一维护，仅团队创建人/管理员可配
* 缺陷列表支持批量指派 / 修复 / 驳回 / 通过 / 关闭 / 重开 / 删除
* （1.1.0 已具备）项目源码管理（Git 自托管）、用例可选评审与 Markdown 纪要；详见上文「内置功能」

## 特色

* 开源私有化AI+BUG系统部署;
* 通过AI技术自动生成测试用例并录入到系统，解决费时费力录入用例的痛点;
* 已测试平台为生态中心，衍生多种缺陷监控测试框架，可以一站式解决软件生产运维中的诸多痛点；
* 专注于软件的缺陷的跟踪管理，简单直接，即开即用，减少学习成本；

## 在线体验

- 体验账号：demo
- 体验密码：123456

演示地址：[https://www.cat2bug.com:8022](https://www.cat2bug.com:8022)

## 关联产品

| 名称                                                         | 类型          | 说明                                                                |
|------------------------------------------------------------|-------------|:------------------------------------------------------------------|
| [Cat2Bug-App](https://gitee.com/cat2bug/cat2bug-app)       | 缺陷管理手机端     | Cat2Bug-Platform平台的手机端系统                                          |
| [Cat2Bug-Chrome](https://gitee.com/cat2bug/cat2bug-chrome) | 缺陷管理谷歌浏览器插件 | Cat2Bug-Platform平台的谷歌浏览器插件                                        |
| [Cat2Bug-MCP](https://gitee.com/cat2bug/cat2bug-mcp)       | MCP代理服务     | Cat2Bug-Platform与AI大模型通讯代理服务                                      |
| [Cat2Bug-JLog](https://gitee.com/cat2bug/cat2bug-jlog)     | 错误日志采集框架    | 获取项目中的异常日志；平台已移除 `/api/report/defect`，请改用缺陷开放接口写入                              |
| [Cat2Bug-JUnit](https://gitee.com/cat2bug/cat2bug-junit)   | 单元测试框架      | 自动化单元测试框架，目前可以自动扫描Controller接口，随机提供参数测试；平台已移除 `/api/report/defect`，请改用缺陷开放接口写入 |

## 系统架构

![系统架构](readme/images/cat2bug-platform-framework.png)

## 技术选型

1. 系统环境

* Java 17
* Jakarta Servlet 6
* Apache Maven 3

2. 主框架

* Quarkus 3.27 LTS（RESTEasy Reactive、Vert.x、Scheduler、WebSockets）
* JGit 6.10（Git 仓库托管与 Smart HTTP）

3. 持久层

* Apache MyBatis 3.5.x（quarkus-mybatis）
* Agroal 连接池
* Flyway 数据库迁移（H2 / MySQL）

4. 视图层

* Vue 3.5 + Vite 6
* Pinia 2 + Vue Router 4
* Element Plus 2.9
* Axios 1.7.x
* vue-i18n 9（7 语言）

## 模块

````
--cat2bug-platform
------|----cat2bug-platform-admin       # 主程序模块（Quarkus 应用、REST 资源、安装向导、Native 配置）
------|----cat2bug-platform-system      # 业务模块（Service、Mapper、Git 仓库）
------|----cat2bug-platform-framework   # 系统框架（安全、Web、缓存）
------|----cat2bug-platform-common      # 通用模块
------|----cat2bug-platform-ui          # 前端 Vue 工程
------|----deploy                       # 部署脚本（JVM、Native、Docker、rpm）
------|----sql                          # 数据库文件
------|----readme                       # 文档
--------------|----development          # 项目开发文档
--------------|----production           # 生产环境使用文档
````

## 部署

### 源码构建与运行

需 **Java 17+**、Maven 3。模块说明见 [cat2bug-platform-admin/README.md](cat2bug-platform-admin/README.md)，部署脚本见 [deploy/README.md](deploy/README.md)。

```bash
# 开发模式
./cat2bug-platform-admin/dev.sh
# 或
mvn -pl cat2bug-platform-admin -am quarkus:dev

# 嵌入式前端 + 单 JAR（端口 2020）
./deploy/build-embedded.sh
./deploy/run-jar.sh

# Native 单文件（含前端，UPX 压缩）
./deploy/build-native.sh
```

前端独立开发：`cd cat2bug-platform-ui && npm run dev`（代理到 `:2020`）。

### Docker单容器部署

以下提供的是Docker官网容器化的部署方案，执行命令如下：

```docker
docker run -it -d -p 8022:8022 --name cat2bug-platform docker.cat2bug.com/cat2bug/cat2bug-platform/single:latest
```

### 本地通过Docker-Compose容器化部署

先构建前端与后端产物，再启动：

```shell
./deploy/docker/build.sh
cd deploy/docker
docker compose build
docker compose up -d
```

数据卷、环境变量与升级说明见 [deploy/docker/README.md](deploy/docker/README.md)。

部署成功后，打开浏览器访问 [http://127.0.0.1:8022](http://127.0.0.1:8022)，系统在首次启动时会引导至 **图形化首次安装向导 (`/setup`)**：

*   **配置向导**：支持可视化完成数据库（嵌入式 H2 或 MySQL）、缓存（Caffeine 或 Redis）、文件存储路径、Ollama AI 机器人以及超级管理员账号等配置。
*   **重启生效**：提交配置后，**需要重启系统进程**，使新的数据源和系统运行配置完全生效。重启后即可使用您刚刚在向导中设置的超级管理员账号进行登录。
*   **默认/推荐账号**：系统管理员默认建议为 `admin`，密码为 `cat2bug`（可在向导中进行自定义修改）。
*   **静默/自动部署**：若通过 Docker 或 CI/CD 自动部署且需要跳过配置向导，可在启动前配置环境变量 `CAT2BUG_INSTALL_SKIP=true`，系统将直接跳过向导并以默认 H2 配置启动。

此部署方式为单容器最精简方式部署，如需多容器分布式或复杂生产环境部署方案，请查看 [系统使用文档中的管理员指南](readme/production/admin-guide/install-wizard.md) 或 [官网文档](https://www.cat2bug.com/download/cat2bug-platform/#%E9%83%A8%E7%BD%B2)。

## 演示图

<table>
    <tr>
        <td><img src="readme/images/1.png"></td>
        <td><img src="readme/images/2.png"></td>
    </tr>
    <tr>
        <td><img src="readme/images/3.png"></td>
        <td><img src="readme/images/4.png"></td>
    </tr>
    <tr>
        <td><img src="readme/images/5.png"></td>
        <td><img src="readme/images/6.png"></td>
    </tr>
    <tr>
        <td><img src="readme/images/7.png"></td>
        <td><img src="readme/images/8.png"></td>
    </tr>
</table>

## 未来计划

目前Cat2Bug还在持续成长中，后续我们将在测试工具、自动化、AI几个方向持续投入，完善平台的功能。2026计划如下：

* cat2bug-platform: 功能叠加，完善系统统计管理功能；
* cat2bug-app：完善移动端APP；
* cat2bug-cloud：cat2bug云平台的建设；

## Cat2Bug交流群

| QQ群： [731462000](https://qm.qq.com/cgi-bin/qm/qr?k=G_vJa478flcFo_1ohJxNYD0mRKafQ7I1&jump_from=webapi&authKey=EL0KrLpnjYWqNN9YXTVksNlNFrV9DHYyPMx2RVOhXqLzfnmc+Oz8oQ38aBOGx90t) | 微信群：Cat2Bug                                                                 |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| <img src="https://www.cat2bug.com/prod-api/images/qq.png" style="width: 150px; height: 150px;">                                                                  | <img src="https://www.cat2bug.com/prod-api/images/wechat.png" style="width: 150px; height: 150px;"> |
