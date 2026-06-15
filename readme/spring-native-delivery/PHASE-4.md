# Phase 4 — 交付、RPM 与切换默认 Release

**Change:** `spring-native-delivery`  
**分支:** `feature/spring-native-upx`  
**状态:** 完成（2026-06-15；开源本地打包，不维护 CI）

## 目标

| 任务 | 说明 | 状态 |
|------|------|------|
| 4.1 | Spring Native RPM（`build-rpm-spring.sh`） | **已通过**（aarch64，2026-06-15） |
| 4.2 | AlmaLinux `smoke-install.sh` | **已通过**（AlmaLinux 9 + systemd 容器，2026-06-15） |
| 4.3 | Docker minimal 默认交付 | **完成**：`run-native-spring-minimal.sh` + OpenSpec 别名 `run-native-minimal.sh` |
| 4.4 | CI | **不采用**（开源仅本地打包，不维护 GitHub Actions） |
| 4.5 | 根 README / deploy 文档切换默认 Release | **已完成**（2026-06-15）：根 `README.md`、`CLAUDE.md`、`deploy/README.md` |
| 4.6 | Quarkus 线标注 archived | **已完成**（2026-06-15）：见 `readme/spring-native-delivery/README.md` |
| 4.7 | Go-Live 检查清单 | **完成** |

## 默认 Release 命令

```bash
# 生产 Native（embedded SPA + UPX，双架构之一）
./deploy/build-native-spring.sh x86_64    # linux/amd64
./deploy/build-native-spring.sh aarch64   # linux/arm64

# Docker 冒烟（UPX + debian 基础层）
PORT=2020 ./deploy/docker/run-native-spring-minimal.sh run-bg debian
# OpenSpec 兼容别名（等价于上一行）
PORT=2020 ./deploy/docker/run-native-minimal.sh run-bg debian

./deploy/test/native-api-smoke.sh
./deploy/test/native-h2-sql-smoke.sh
./deploy/test/native-parity-smoke.sh
./deploy/test/native-upgrade-smoke.sh   # Legacy Upgrade L3 全流程（隔离卷 @2028）

# JVM 开发 / 回滚
mvn -pl cat2bug-platform-admin -am package -DskipTests
mvn -pl cat2bug-platform-admin -am package -DskipTests -Pslim-jar   # 无 POI，约 112 MB
java -jar cat2bug-platform-admin/target/cat2bug-admin.jar
```

## Docker minimal 交付（4.3）

| 脚本 | 说明 |
|------|------|
| [`deploy/docker/run-native-spring-minimal.sh`](../../deploy/docker/run-native-spring-minimal.sh) | **主入口**：Debian/Distroless 最小镜像运行 UPX 或未压缩 ELF |
| [`deploy/docker/run-native-minimal.sh`](../../deploy/docker/run-native-minimal.sh) | **OpenSpec 兼容别名**，`exec` 委托 spring 脚本，参数完全一致 |

```bash
# 二者等价
./deploy/docker/run-native-spring-minimal.sh run-bg debian
./deploy/docker/run-native-minimal.sh run-bg debian
```

详见 [`deploy/README.md`](../../deploy/README.md) 与 [`deploy/docker/README.md`](../../deploy/docker/README.md)。

## RPM

### 依赖：nfpm

构建机需安装 [nfpm](https://nfpm.goreleaser.com/)。`build-rpm-spring.sh` 在未找到 `nfpm` 时会打印安装提示并退出。

| 环境 | 安装 |
|------|------|
| macOS | `brew install nfpm` |
| Linux | `go install github.com/goreleaser/nfpm/v2/cmd/nfpm@latest`（确保 `$GOPATH/bin` 在 `PATH`） |

### 构建

```bash
./deploy/build-native-spring.sh x86_64   # 或 aarch64
./deploy/rpm/cat2bug/build-rpm-spring.sh x86_64
# 产物：deploy/rpm/cat2bug/dist/cat2bug-platform_1.0.0_x86_64.rpm
```

本机验证（2026-06-15）：仅有 `cat2bug-admin-linux-arm64` 时执行 `build-rpm-spring.sh aarch64`，生成约 106 MB RPM。

**实现说明：** `nfpm.yaml` 使用固定 `stage/` 目录（nfpm 不会展开 `contents.src` 中的 `${NFPM_ARCH}`）；`postinstall` 为 `scripts/postinstall.sh` 文件（内联 YAML 会被 nfpm 当作路径）。

### AlmaLinux 9 冒烟

在已安装 systemd 的目标机：

```bash
sudo ./deploy/rpm/cat2bug/smoke-install.sh deploy/rpm/cat2bug/dist/cat2bug-platform_1.0.0_x86_64.rpm
```

本机无 RPM 包管理器时，可用 **systemd 容器**（Apple Silicon / aarch64 示例；无需映射宿主机 2020 端口）：

```bash
docker pull quay.io/almalinux/almalinux:9   # docker.io 镜像源失败时可改用 quay
docker run -d --name cat2bug-rpm-smoke --privileged --cgroupns=host \
  -v /sys/fs/cgroup:/sys/fs/cgroup:rw \
  -v "$(pwd)/deploy/rpm/cat2bug/dist/cat2bug-platform_1.0.0_aarch64.rpm":/tmp/cat2bug.rpm:ro \
  -v "$(pwd)/deploy/rpm/cat2bug/smoke-install.sh":/tmp/smoke-install.sh:ro \
  quay.io/almalinux/almalinux:9 /usr/sbin/init
sleep 12
docker exec cat2bug-rpm-smoke bash /tmp/smoke-install.sh /tmp/cat2bug.rpm
docker rm -f cat2bug-rpm-smoke
```

AlmaLinux 9 基础镜像已带 `curl-minimal`，无需再 `dnf install curl`（与 `curl` 包冲突）。

冒烟通过标准：`GET /version` 返回 `1.0.0`；`GET /setup/status` 返回 `installed:false`（空库首启）。

## Go-Live 检查清单

> Phase 2 冒烟项已于 2026-06-15 本地自动化验证；RPM aarch64 已验。

### 构建与产物

- [x] `./deploy/build-native-spring.sh` 目标架构构建成功（arm64 embedded 已验；amd64 同链）
- [x] UPX 产物存在且 `run-native-spring-minimal.sh run-bg` 可启动
- [x] `METRICS.md` 体积/冷启动/RSS 已记录（raw 冷启动 amd64 待补，不阻塞）

### 冒烟（Native Docker）

- [x] `./deploy/test/native-api-smoke.sh` L1–L4 全通过（Phase 2，2026-06-15）
- [x] `./deploy/test/native-h2-sql-smoke.sh` L5 全通过（Phase 2，2026-06-15）
- [x] `./deploy/test/native-parity-smoke.sh` Open API / WebSocket / AI / Quartz / Setup 通过（Phase 2.4–2.8）
- [x] `./deploy/test/native-upgrade-smoke.sh` Legacy Upgrade L3 全流程（drift→pending→全锁→submit→restart→completed，Phase 2.9）
- [x] `GET /` + `/static/js/app.js` 200（embedded 构建，Phase 1）

### JVM 回归

- [x] `mvn -pl cat2bug-platform-admin -am package -DskipTests` 成功（横切 6.1）
- [x] `./deploy/scripts/h2-mapper-smoke.sh` 通过（横切 6.1）

### 安全与配置（6.3）

- [x] 生产包未含 devtools / generator / Knife4j UI（`-Pnative` 不激活 `dev-tools` profile；见结论）
- [x] Native profile 关闭 swagger UI（`application-native.properties`）
- [x] `config/install` 与 RPM 默认配置无**生产级**硬编码密钥（占位默认由向导覆盖；见结论）

**6.3 安全审查结论（2026-06-15）**

| 检查项 | 结论 |
|--------|------|
| `application-native.properties` | 已关闭 devtools、springdoc/swagger-ui、knife4j；bootstrap H2 密码为本地引导占位，安装后由 `application-install.yml` 覆盖 |
| `-Pnative` / `pom.xml` | `dev-tools` profile 独立承载 devtools、generator、springdoc、Knife4j；`build-native-spring.sh` 仅用 `-Pnative -Pembedded -Prelease`，**不含** `-Pdev-tools`；Native 编译另排除 POI/CaptchaSupport JVM 源 |
| install 模板 | `defaults/application-install-*.yml` 含 H2/MySQL 占位口令（`cat2bug_password`）与 Druid 控制台默认（`123456`），属 classpath 骨架；**生产密钥由 Setup/Upgrade 向导写入磁盘 install**，非打包死值 |
| RPM 默认配置 | `deploy/rpm/cat2bug/application.properties` 仅 bootstrap H2 占位；runtime 数据源由 `/var/lib/.../config/install/` 覆盖 |
| 残余风险 | RPM 实机安装后是否误用占位口令需运维按向导改密；Druid console 默认口令应在生产 install 中禁用或改密（与 JVM 版一致） |

### 交付通道（可选）

- [x] RPM：`smoke-install.sh` 在 AlmaLinux 9 通过（aarch64 容器，2026-06-15）
- [x] 文档：根 README 指向 Spring Native 为默认 Release（4.5）
- [x] 本地冒烟脚本链：`deploy/test/native-*.sh`（不维护 CI）

## 与 Quarkus 线关系

- `feature/quarkus-full-migration`：**archived / 参考对照**（体积基线、FastExcel 实现源；见 `readme/spring-native-delivery/README.md`）
- 生产默认交付已切换至 **Spring Native**（任务 4.5/4.6 已完成）

## 已知差距（不阻塞 Go-Live）

- Stretch 体积未达标（304M raw / 76M UPX vs 250M / 65M）
- amd64 raw 冷启动与 x86_64 RPM：需在 **Linux x86_64** 或 Docker 内存 ≥16GB 的机器上执行 `./deploy/build-native-spring.sh x86_64`（Apple Silicon 交叉编译 embedded 可能 OOM exit 137）
