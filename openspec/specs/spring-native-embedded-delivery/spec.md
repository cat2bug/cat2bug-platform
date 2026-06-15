## ADDED Requirements

### Requirement: Spring Native embedded 双架构构建

项目 MUST 提供 `deploy/build-native-spring.sh`（或等价脚本）与 Maven profile **`-Pnative -Pembedded -Prelease`**，在 Docker 容器构建环境下产出 **linux/amd64** 与 **linux/arm64** 的 Spring Native 可执行文件；构建前 MUST 执行 `npm run build:embedded`（或文档约定的 `ui-build embedded`），使 Vue SPA 与 `static/docs/**` 进入 `cat2bug-platform-admin` classpath。

#### Scenario: amd64 embedded Native 构建

- **WHEN** 在 macOS/Linux 执行 `./deploy/build-native-spring.sh x86_64`
- **THEN** 产出 `cat2bug-platform-admin/target/cat2bug-admin-linux-amd64` 且可执行
- **AND** 二进制内嵌 `classpath:/static/index.html` 与 `static/docs/**` 资源

#### Scenario: arm64 embedded Native 构建

- **WHEN** 执行 `./deploy/build-native-spring.sh aarch64`
- **THEN** 产出 `cat2bug-admin-linux-arm64` 且可执行

#### Scenario: 构建前前端 embedded

- **WHEN** 执行 Native 构建脚本且未设置跳过 embedded
- **THEN** 脚本先完成 embedded 前端构建再执行 `mvn clean package -Pnative -Pembedded`
- **AND** 产物中不包含 `stats.json` 或 `report.html`

### Requirement: 单文件 Native 部署形态

默认 Release 交付物 MUST 为 **单个 Native ELF**（embedded SPA + 全量 API），用户 **无需 JRE、无需 nginx** 即可访问根路径 SPA 与 `/prod-api/**` API。进程 MUST 同时提供 Setup/Upgrade 向导 REST 与 WebSocket（功能 parity 阶段完成后）。

#### Scenario: 仅启动 Native 二进制访问 SPA

- **WHEN** 在 Linux 上仅运行 embedded Native 二进制并访问 `/`
- **THEN** 返回 Vue SPA（`index.html` 或等价 HTML）
- **AND** 浏览器加载 `/static/js/**`、`/static/css/**` 返回 200

#### Scenario: SPA history 路由 fallback

- **WHEN** 客户端 `GET /login` 且 `Accept` 含 `text/html`
- **THEN** 返回 SPA 入口 HTML（`EmbeddedSpaFallbackFilter` 行为）
- **AND** `POST /login` API 仍由后端处理，不返回 405

#### Scenario: system/doc 在 Native embedded 下可访问

- **WHEN** 用户经 Native 单进程访问 `/system/doc` 及 `/docs/images/**`
- **THEN** 文档 Markdown 与图片与 JVM embedded JAR 场景一致

#### Scenario: API 与 SPA 同进程

- **WHEN** 前端经 `/prod-api` 调用登录接口
- **THEN** 请求由同一 Native 进程处理且 `AjaxResult` 形态不变

### Requirement: Native 冒烟验收

每个架构 embedded Native 二进制 MUST 在 Linux 容器（UBI9 或等价 glibc 环境）中通过：**health 或 version**、**login**、**至少一条业务只读 API**、**静态资源 GET** 冒烟。

#### Scenario: 容器内 health 或 version

- **WHEN** 在 UBI 容器启动 embedded Native 二进制
- **THEN** `GET /actuator/health`（若启用）或 `GET /version` 返回 200

#### Scenario: 容器内 login

- **WHEN** 对已安装实例调用 `POST /login` 且凭证有效
- **THEN** 返回含 `token` 的 `AjaxResult` 且后续鉴权请求成功

#### Scenario: 容器内静态资源

- **WHEN** 对已构建 embedded Native 二进制 `GET /static/js/` 下任意存在的 chunk 文件
- **THEN** HTTP 200 且 `Content-Type` 为 JavaScript 或约定 MIME

### Requirement: UPX 可选分发包

构建脚本 MUST 支持默认生成 UPX 压缩副本（`upx --best --lzma`），文件名 **`cat2bug-admin-linux-{arch}.upx`**；MUST 同时保留 **未压缩 ELF** 供 RPM/systemd 使用。可通过 `UPX_COMPRESS=false` 关闭。

#### Scenario: 默认产出 UPX 与未压缩版

- **WHEN** 执行 `UPX_COMPRESS=true ./deploy/build-native-spring.sh` 且本机已安装 `upx`
- **THEN** 同时存在未压缩 `cat2bug-admin-linux-amd64` 与 `.upx` 文件
- **AND** `.upx` 文件在 Linux 容器内可启动并通过 version 冒烟

#### Scenario: 无 upx 时降级

- **WHEN** 本机未安装 `upx` 且 `UPX_COMPRESS=true`
- **THEN** 构建仍成功产出未压缩 ELF
- **AND** 脚本输出 WARN 说明跳过 UPX

### Requirement: RPM 与 systemd（无 JRE）

项目 MUST 提供 Spring Native 版 RPM 打包路径，安装后包含 `/usr/bin/cat2bug-admin`（**未压缩 Native ELF**）、`/etc/cat2bug/` 配置、`systemd` unit；**Requires 不得依赖 java-\***。

#### Scenario: AlmaLinux dnf 安装

- **WHEN** 在 AlmaLinux 9 执行 `dnf install` Spring Native 构建的 RPM
- **THEN** `systemctl start cat2bug` 后 health/version 为 UP
- **AND** 浏览器可访问 embedded SPA 根路径

### Requirement: 体积与启动度量

Phase 3 完成后 MUST 在 `readme/spring-native-delivery/METRICS.md` 记录 **amd64 与 arm64** 的：未压缩体积、UPX 体积（若有）、冷启动至 health/version、空闲 RSS；MUST 含 **Quarkus embedded 对照列**（参考 ~190MB / ~42MB UPX）。stretch 目标：raw **< 250MB**、UPX **< 65MB**，**未达标不阻塞 Phase 4 切换**。

#### Scenario: 度量文档含双架构

- **WHEN** Phase 3 验收
- **THEN** `METRICS.md` 含 amd64 与 arm64 实测行
- **AND** 注明 embedded 含 SPA 与 docs static

### Requirement: 默认 Release 切换

Phase 4 验收后，根 `README.md` 与 `deploy/README.md` MUST 将 **默认生产构建** 文档化为 `./deploy/build-native-spring.sh`；JVM `cat2bug-admin.jar` MUST 仍可通过 `-Pembedded` 构建，作为开发/回滚路径文档化，但 **非对外主推 Release 产物**。

#### Scenario: 文档默认 Native 构建

- **WHEN** 运维查阅 deploy README 执行 Release 构建
- **THEN** 首选命令为 `build-native-spring.sh` 且说明 embedded 单文件语义

#### Scenario: JVM 回滚路径存在

- **WHEN** 需回滚至 JRE 部署
- **THEN** 文档说明 `mvn package -Pembedded` 产物可替代 Native 连接同一数据库
