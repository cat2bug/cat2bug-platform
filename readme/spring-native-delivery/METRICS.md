# Spring Native 发布物度量

记录 **embedded 单文件 Native** 与 JVM 对照。Quarkus 列为参考基线（`feature/quarkus-full-migration` 实测）。

## 构建命令

| 产物 | 命令 | 输出路径 |
|------|------|----------|
| JVM embedded JAR | `mvn -pl cat2bug-platform-admin -am package -DskipTests` | `cat2bug-platform-admin/target/cat2bug-admin.jar` |
| Native embedded（amd64） | `./deploy/build-native-spring.sh x86_64` | `cat2bug-platform-admin/target/cat2bug-admin-linux-amd64` |
| Native embedded（arm64） | `./deploy/build-native-spring.sh aarch64` | `cat2bug-platform-admin/target/cat2bug-admin-linux-arm64` |
| 仅 UPX（已有二进制） | `./deploy/upx-native-binary.sh <binary>` | `<binary>.upx` |

## 度量方法

```bash
du -h cat2bug-platform-admin/target/cat2bug-admin-linux-amd64
du -h cat2bug-platform-admin/target/cat2bug-admin-linux-amd64.upx

/usr/bin/time -p sh -c '
  BIN=cat2bug-platform-admin/target/cat2bug-admin-linux-amd64
  "$BIN" & pid=$!
  for i in $(seq 1 100); do curl -sf http://127.0.0.1:2020/version && break; sleep 0.1; done
  kill $pid
'
```

## 参考对照（Quarkus embedded，非 Spring 实测）

| 指标 | Quarkus embedded Native | Spring Native embedded（待填） |
|------|------------------------|------------------------------|
| 未压缩 ELF amd64 | ~190 MB | _待 Docker 容器构建_ |
| UPX amd64 | ~42 MB | _TBD_ |
| 未压缩 ELF arm64 | ~190 MB | _待 Docker 容器构建_ |
| UPX arm64 | ~42 MB | _TBD_ |
| 未压缩 Mach-O arm64（本机 dev） | **370 MB** | 2026-06-13 本机 GraalVM |
| UPX Mach-O arm64（本机 dev，不可运行） | **103 MB（27.9%）** | UPX 5.2 需 `--force-macos`；压缩约 4 分钟；macOS 上 UPX 包无法本地执行，仅作体积参考 |
| 冷启动至 version/health | ~0.3–1.5 s | _TBD_ |
| 空闲 RSS | ~80–150 MB | _TBD_ |

## Stretch 目标（非硬卡）

| 指标 | 目标 |
|------|------|
| 未压缩 embedded raw | < 250 MB |
| UPX embedded | < 65 MB |
| 冷启动（UPX） | < 5 s |

## 体积演进（Spring，待 Phase 1+ 填写）

| 阶段 | amd64 raw | 说明 |
|------|-----------|------|
| Phase 1 基线 | **370 MB** Mach-O arm64 / **103 MB** UPX（27.9%） | 本机 GraalVM 冒烟通过；UPX 发布以 linux ELF 为准 |
| Phase 3 优化后 | _TBD_ | FastExcel / 去 AWT / slim |
