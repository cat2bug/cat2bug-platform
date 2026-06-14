# Spring Native 发布物度量

记录 **embedded 单文件 Native** 与 JVM 对照。Quarkus 列为参考基线（`feature/quarkus-full-migration` 实测）。

## 构建命令

| 产物 | 命令 | 输出路径 |
|------|------|----------|
| JVM embedded JAR | `mvn -pl cat2bug-platform-admin -am package -DskipTests` | `cat2bug-platform-admin/target/cat2bug-admin.jar` |
| Native embedded（amd64） | `./deploy/build-native-spring.sh x86_64` | `cat2bug-platform-admin/target/cat2bug-admin-linux-amd64` |
| Native embedded（arm64） | `./deploy/build-native-spring.sh aarch64` | `cat2bug-platform-admin/target/cat2bug-admin-linux-arm64` |
| 仅 UPX（已有二进制） | `./deploy/upx-native-binary.sh <binary>` | `<binary>.upx` |
| ELF UPX（Docker 内） | `./deploy/docker/run-native-spring-minimal.sh upx <elf>` | `<elf>.upx` |
| 运行 UPX 包（最小镜像） | `./deploy/docker/run-native-spring-minimal.sh run-bg debian` | 默认 `cat2bug-admin-linux-*.upx` |
| 无 AWT 验收 | `./deploy/scripts/verify-native-no-awt.sh <elf>` | 需 Linux `ldd`（macOS 主机跳过） |

**Phase 3 重建命令（与 Phase 1 可比，API-only）：**

```bash
SKIP_EMBEDDED=true UPX_IN_DOCKER=true UPX_SMOKE=true ./deploy/build-native-spring.sh aarch64
```

## 度量方法

```bash
du -h cat2bug-platform-admin/target/cat2bug-admin-linux-arm64
du -h cat2bug-platform-admin/target/cat2bug-admin-linux-arm64.upx

# Linux 容器内 ldd 检查 AWT
docker run --rm -v "$PWD/cat2bug-platform-admin/target/cat2bug-admin-linux-arm64:/bin/cat2bug:ro" \
  debian:bookworm-slim bash -lc 'ldd /bin/cat2bug | grep -i awt || echo OK'

# UPX 冷启动（最小 debian 镜像）
PORT=2024 BIN=.../cat2bug-admin-linux-arm64.upx ./deploy/docker/run-native-spring-minimal.sh run-bg debian
/usr/bin/time -p sh -c 'for i in $(seq 1 150); do curl -sf http://127.0.0.1:2024/version && exit 0; sleep 0.1; done'
```

## 参考对照（Quarkus embedded，非 Spring 实测）

| 指标 | Quarkus embedded Native | Spring Native（arm64 ELF，API-only） |
|------|------------------------|--------------------------------------|
| 未压缩 ELF amd64 | ~190 MB | _待双架构 CI_ |
| UPX amd64 | ~42 MB | _TBD_ |
| 未压缩 ELF arm64 | ~190 MB | **333 MB**（Phase 3，2026-06-14） |
| UPX arm64 | ~42 MB | **89 MB**（26.8%） |
| 未压缩 Mach-O arm64（本机 dev） | — | **296 MB** / UPX **87 MB**（Phase 1） |
| 冷启动至 `/version`（UPX） | ~0.3–1.5 s | 构建冒烟 **OK**（port 2023）；精确计时 _待 Docker 补测_ |
| 空闲 RSS | ~80–150 MB | _TBD_ |
| libawt.so | 无 | native-image 仍打包 **libawt**（POI/kaptcha 仍在 classpath）；**运行时 Captcha 走 CaptchaPngRenderer** |

## Stretch 目标（非硬卡）

| 指标 | 目标 | Phase 3 实测 | 结论 |
|------|------|--------------|------|
| 未压缩 raw | < 250 MB | **333 MB** | **未达标**（较 Phase 1 **-24 MB**） |
| UPX | < 65 MB | **89 MB** | **未达标**（较 Phase 1 **-5 MB**）；不阻塞 Phase 4 |
| 冷启动（UPX） | < 5 s | 冒烟通过 | 精确秒数待补 |

## 体积演进（Spring arm64 ELF，API-only）

| 阶段 | raw | UPX | 较上一阶段 | 说明 |
|------|-----|-----|-----------|------|
| Phase 1 | **357 MB** | **94 MB**（26.4%） | — | Docker GraalVM 16GB；`-Ob` |
| **Phase 3** | **333 MB** | **89 MB**（26.8%） | raw **-6.7%** / UPX **-5.3%** | packaging-slim + CaptchaPngRenderer + Graal locale；POI 仍在 |
| Quarkus 对照 | ~190 MB | ~42 MB | — | B9 FastExcel 已完成 |

**精确字节（Phase 3）：** raw `349,695,200` · UPX `93,545,968`

## Phase 3 变更摘要（2026-06-14）

- packaging-slim：stats.json 隔离、tool 路由剔除、dev-tools profile
- Captcha：`CaptchaPngRenderer` + `NativeCaptchaSupport`（Native 运行时无 kaptcha/AWT 路径）
- Graal：`-H:IncludeLocales=zh,en`、`-H:-AddAllCharsets`、`-Ob`
- 构建：`verify-native-no-awt.sh`、UPX 默认开启；**UPX 冒烟 `/version` OK**

**后续体积项：** Phase 2.10 FastExcel 替换 POI（预计最大收益）；彻底去除 libawt 需 POI + kaptcha 移出 Native classpath。
