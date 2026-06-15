# Spring Native 发布物度量

记录 **embedded 单文件 Native** 与 JVM 对照。Quarkus 列为参考基线（`feature/quarkus-full-migration` 实测）。

## 构建命令

| 产物 | 命令 | 输出路径 |
|------|------|----------|
| JVM embedded JAR | `mvn -pl cat2bug-platform-admin -am package -DskipTests` | `cat2bug-platform-admin/target/cat2bug-admin.jar` |
| JVM release slim JAR | `mvn -pl cat2bug-platform-admin -am package -DskipTests -Pslim-jar` | 同上（约 **112 MB**，剔除 POI 栈 + lombok） |
| Native embedded（amd64） | `./deploy/build-native-spring.sh x86_64` | `cat2bug-platform-admin/target/cat2bug-admin-linux-amd64` |
| Native embedded（arm64） | `./deploy/build-native-spring.sh aarch64` | `cat2bug-platform-admin/target/cat2bug-admin-linux-arm64` |
| 仅 UPX（已有二进制） | `./deploy/upx-native-binary.sh <binary>` | `<binary>.upx` |
| ELF UPX（Docker 内） | `./deploy/docker/run-native-spring-minimal.sh upx <elf>` | `<elf>.upx` |
| 运行 UPX 包（最小镜像） | `./deploy/docker/run-native-spring-minimal.sh run-bg debian` | 默认 `cat2bug-admin-linux-*.upx` |
| 无 AWT 验收 | `./deploy/scripts/verify-native-no-awt.sh <elf>` | 需 Linux `ldd`（macOS 主机跳过） |
| 无 POI 验收 | `./deploy/scripts/verify-native-no-poi.sh <elf>` | `strings` 检查 `org.apache.poi` |

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

# raw vs UPX 冷启动（隔离 H2 卷，已完成 install；勿共用 cat2bug-spring-native-data，避免 H2 文件锁）
DATA_VOL=cat2bug-native-coldstart-data CONFIG_VOL=cat2bug-native-coldstart-config
./deploy/test/measure-native-coldstart.sh raw 3          # arm64 Mac 默认
ARCH=amd64 ./deploy/test/measure-native-coldstart.sh raw 3   # x86_64 Linux / ubuntu-latest CI（需 cat2bug-admin-linux-amd64）
```

## 参考对照（Quarkus embedded，非 Spring 实测）

| 指标 | Quarkus embedded Native | Spring Native（arm64 ELF，API-only） |
|------|------------------------|--------------------------------------|
| 未压缩 ELF amd64 | ~190 MB | **333 MB**（2026-06-14，API-only） |
| UPX amd64 | ~42 MB | **84 MB**（25.1%） |
| 未压缩 ELF arm64 | ~190 MB | **312 MB**（2026-06-15，**embedded**）/ **333 MB**（API-only） |
| UPX arm64 | ~42 MB | **77 MB**（24.7%）/ **89 MB**（API-only） |
| 未压缩 Mach-O arm64（本机 dev） | — | **296 MB** / UPX **87 MB**（Phase 1） |
| 冷启动至 `/version`（Debian 挂载 ELF） | ~0.3–1.5 s（Quarkus UPX） | **UPX amd64 ~9.9 s**；**raw arm64 ~1.5 s**；**UPX arm64 ~5–7 s**（2026-06-15；`time` + `run-bg debian`） |
| 冷启动至 `/version`（**raw**，arm64 Debian） | — | **~1.1 s**（2026-06-15，3 次均值 1.06s；隔离卷 + completed install） |
| 冷启动至 `/version`（**UPX**，arm64 Debian） | — | **~4.1 s**（2026-06-15，3 次均值 4.14s；同条件） |
| 空闲 RSS（Debian 挂载） | ~80–150 MB（Quarkus UPX） | **UPX amd64 ~803 MiB**；**raw arm64 ~300 MiB**；**UPX arm64 ~700 MiB**（2026-06-15） |
| 空闲 RSS（raw arm64） | — | **~293 MiB**（Docker stats，/version 后约 5s） |
| 空闲 RSS（UPX arm64） | — | **~675 MiB**（Docker stats，/version 后约 5s） |
| JVM fat JAR（embedded，含 POI） | — | **127 MB**（2026-06-14） |
| JVM fat JAR（`-Pslim-jar`，无 POI） | — | **112 MB**（**-15 MB**） |
| libawt.so | 无 | embedded arm64 **ldd 未检出**（2026-06-15）；POI 已从 Native 编译/runtime 剔除，`strings` 无 `org.apache.poi`（2026-06-15） |

## Stretch 目标（非硬卡）

| 指标 | 目标 | Phase 3 实测 | 结论 |
|------|------|--------------|------|
| 未压缩 raw | < 250 MB | **312 MB**（embedded）/ **333 MB**（API-only） | **未达标** |
| UPX | < 65 MB | **77 MB**（embedded）/ **89 MB**（API-only） | **未达标**；不阻塞 Phase 4 |
| 冷启动（UPX） | < 5 s | **~9.9 s**（amd64）/ **~4.1 s**（arm64） | **arm64 达标**；amd64 UPX 未达标 |
| 冷启动（raw） | < 5 s | **~1.1 s**（arm64）；amd64 raw **待测** | arm64 **达标**；UPX 较 raw 慢约 **4×**（解压开销） |

## 冷启动对比（Spring Native，debian:bookworm-slim，2026-06-15）

条件：`docker run -d` 挂载 **隔离** H2/config 卷（completed install 种子自 `cat2bug-spring-native-*`）；`CAT2BUG_UPGRADE_SKIP=true`；测 `docker run` 至 `curl /version` 成功（3 次取均值）。

| 架构 | 压缩 | 体积 | 冷启动（s） | 空闲 RSS |
|------|------|------|-------------|----------|
| arm64 | raw | 304 MB | **1.06**（0.91–1.20） | ~293 MiB |
| arm64 | UPX | 76 MB | **4.14**（4.05–4.23） | ~675 MiB |
| amd64 | UPX | 84 MB | **~9.9**（2026-06-14） | ~803 MiB |
| amd64 | raw | 333 MB | **待 CI ubuntu-latest 实测**（`ARCH=amd64 ./deploy/test/measure-native-coldstart.sh raw 3`） | 待 CI 实测 |


## CI 自动填充 amd64 raw 冷启动（OpenSpec 1.7 / 3.5）

GitHub Actions workflow [`.github/workflows/spring-native.yml`](../../.github/workflows/spring-native.yml)（与 [`deploy/ci/spring-native.yml`](../../deploy/ci/spring-native.yml) 同步）矩阵包含：

| Runner | 架构 | Artifact 名称 |
|--------|------|----------------|
| `ubuntu-latest` | x86_64 / amd64 | `cat2bug-admin-spring-x86_64` |
| `ubuntu-24.04-arm` | aarch64 / arm64 | `cat2bug-admin-spring-aarch64` |

**流程：**

1. CI 在 `ubuntu-latest` 上执行 `build-native-spring.sh x86_64` 并 `upload-artifact`（含 `cat2bug-admin-linux-amd64` 与 `.upx`）。
2. 在 **同一 amd64 Linux 环境**（Actions runner 或下载 artifact 后的机器）执行冷启动脚本并回填本表：

   ```bash
   # artifact 解压到 cat2bug-platform-admin/target/ 后
   ARCH=amd64 ./deploy/test/measure-native-coldstart.sh raw 3
   ```

3. 将输出的 `avg` / `min` / `max` / `RSS` 写入上文「冷启动对比」表中 **amd64 raw** 行（替换「待 CI ubuntu-latest 实测」占位）。

**arm64 Mac 开发机：** 可测 arm64 raw/UPX；amd64 raw 以 CI 数据为准，本地仅作 QEMU/跨平台试跑，不写入正式度量 unless 在 x86_64 Linux 复现。


**说明：** 早期 amd64 UPX ~9.9s 可能含首次 Flyway/空库或不同卷条件；本次 arm64 在**已安装实例**上 raw 明显快于 UPX。测量时勿与其它容器共用 H2 卷（`MVStoreException: file is locked`）。

## 体积演进（Spring arm64 ELF）

| 阶段 | raw | UPX | 较上一阶段 | 说明 |
|------|-----|-----|-----------|------|
| Phase 1 | **357 MB** | **94 MB**（26.4%） | — | Docker GraalVM 16GB；`-Ob` |
| Phase 3（API-only） | **333 MB** | **89 MB**（26.8%） | raw **-6.7%** / UPX **-5.3%** | packaging-slim + CaptchaPngRenderer + Graal locale；POI 仍在 |
| **Phase 3（embedded）** | **312 MB** | **77 MB**（24.7%） | raw **-6.3%** vs API-only | `UPX_IN_DOCKER=true ./deploy/build-native-spring.sh aarch64`（2026-06-15） |
| **Phase 3（embedded，POI 剔除后）** | **304 MB** | **76 MB**（24.85%） | raw **-2.6%** / UPX **-1.3%** vs POI 前 | 同上命令（2026-06-15）；`verify-native-no-poi.sh` **通过** |
| Quarkus 对照 | ~190 MB | ~42 MB | — | B9 FastExcel 已完成 |

**精确字节（Phase 3 embedded arm64，2026-06-15）：** raw `326,823,136` · UPX `80,733,516`

**精确字节（POI 剔除后 embedded arm64，2026-06-15）：** raw `319,089,888` · UPX `79,286,536`

## Phase 3 变更摘要（2026-06-14）

- packaging-slim：stats.json 隔离、tool 路由剔除、dev-tools profile
- Captcha：`CaptchaPngRenderer` + `NativeCaptchaSupport`（Native 运行时无 kaptcha/AWT 路径）
- Graal：`-H:IncludeLocales=zh,en`、`-H:-AddAllCharsets`、`-Ob`
- 构建：`verify-native-no-awt.sh`、UPX 默认开启；**UPX 冒烟 `/version` OK**

**后续体积项：** Phase 2.10 FastExcel 替换 POI（预计最大收益）；彻底去除 libawt 需 POI + kaptcha 移出 Native classpath。
