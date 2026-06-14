# Phase 4 — 交付、RPM 与切换默认 Release

**Change:** `spring-native-delivery`  
**分支:** `feature/spring-native-upx`  
**状态:** 进行中

## 目标

| 任务 | 说明 | 状态 |
|------|------|------|
| 4.1 | Spring Native RPM（`build-rpm-spring.sh`） | 骨架已建 |
| 4.2 | AlmaLinux `smoke-install.sh` | 脚本已建，待实机 |
| 4.3 | Docker minimal 默认交付 | `run-native-spring-minimal.sh` + OpenSpec 别名 `run-native-minimal.sh` |
| 4.4 | CI 双架构构建 + 冒烟 | `deploy/ci/spring-native.yml` 草案 |
| 4.5 | 根 README / deploy 文档切换默认 Release | 待更新 |
| 4.6 | Quarkus 线标注 archived | 待文档 |
| 4.7 | Go-Live 检查清单 | 见下文 |

## 默认 Release 命令

```bash
# 生产 Native（embedded SPA + UPX，双架构之一）
./deploy/build-native-spring.sh x86_64    # linux/amd64
./deploy/build-native-spring.sh aarch64   # linux/arm64

# Docker 冒烟（UPX + debian 基础层）
PORT=2020 ./deploy/docker/run-native-spring-minimal.sh run-bg debian
./deploy/test/native-api-smoke.sh
./deploy/test/native-h2-sql-smoke.sh

# JVM 开发 / 回滚
mvn -pl cat2bug-platform-admin -am package -DskipTests
mvn -pl cat2bug-platform-admin -am package -DskipTests -Pslim-jar   # 无 POI，约 112 MB
java -jar cat2bug-platform-admin/target/cat2bug-admin.jar
```

## RPM（草案）

```bash
./deploy/build-native-spring.sh x86_64
./deploy/rpm/cat2bug/build-rpm-spring.sh x86_64
sudo ./deploy/rpm/cat2bug/smoke-install.sh deploy/rpm/cat2bug/dist/cat2bug-platform_1.0.0_x86_64.rpm
```

需本机安装 [nfpm](https://nfpm.goreleaser.com/)。

## Go-Live 检查清单

### 构建与产物

- [ ] `./deploy/build-native-spring.sh` 目标架构构建成功（非 `SKIP_EMBEDDED` 时为默认 Release）
- [ ] UPX 产物存在且 `run-native-spring-minimal.sh run-bg` 可启动
- [ ] `METRICS.md` 体积/冷启动/RSS 已记录

### 冒烟（Native Docker）

- [ ] `./deploy/test/native-api-smoke.sh` L1–L4 全通过
- [ ] `./deploy/test/native-h2-sql-smoke.sh` L5 全通过
- [ ] `GET /` + `/static/js/app.js` 200（embedded 构建）

### JVM 回归

- [ ] `mvn -pl cat2bug-platform-admin -am package -DskipTests` 成功
- [ ] `./deploy/scripts/h2-mapper-smoke.sh` 通过

### 安全与配置

- [ ] 生产包未含 devtools / generator / Knife4j UI
- [ ] `config/install` 与 RPM 默认配置无硬编码密钥
- [ ] Native profile 关闭 swagger UI

### 交付通道（可选）

- [ ] RPM：`smoke-install.sh` 在 AlmaLinux 9 通过
- [ ] CI：`deploy/ci/spring-native.yml` 实跑绿
- [ ] 文档：根 README 指向 Spring Native 为默认 Release

## 与 Quarkus 线关系

- `feature/quarkus-full-migration`：**参考/对照**（体积基线、FastExcel 实现源）
- 生产默认交付切换至 **Spring Native** 后，Quarkus 文档标注 archived（任务 4.6）

## 已知差距（不阻塞 Go-Live 草案）

- Stretch 体积未达标（333M raw / ~84–89M UPX vs 250M / 65M）
- POI 仍在 Native classpath；Excel 依赖 Phase 2.10 FastExcel
- `libawt.so` 仍链接；运行时 Captcha 已走无 AWT 路径
