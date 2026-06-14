# Phase 3 — 体积优化与 UPX

**Change:** `spring-native-delivery`  
**分支:** `feature/spring-native-upx`  
**状态:** 已完成（Stretch 目标未达标，不阻塞 Phase 4）

## 目标

| 任务 | 说明 |
|------|------|
| 3.1 | 合并 `backend-prod-packaging-slim` |
| 3.2 | Graal 裁剪（charset / locale） |
| 3.3 | Native 无 `libawt.so` |
| 3.4 | 构建脚本默认 UPX |
| 3.5 | 填入 `METRICS.md` |
| 3.6 | Stretch 评估 |
| 3.7 | 本文档 |

## 已实施

### 3.1 packaging-slim

- **前端：** `vue.config.js` 仅在 `ANALYZE=true` 时生成 `stats.json` / `report.html`；生产 `IgnorePlugin` 剔除 `views/tool/**`（`VUE_APP_ENABLE_DEV_TOOLS=false`）
- **后端 Maven：** `embedded` / `api-only` / `dev-tools` profile；默认资源排除 `static/**`，`-Pembedded` 再打回；`generator`、Knife4j、swagger-ui、devtools 仅在 `-Pdev-tools`
- **Flyway：** `V0_6_2_16__hide_dev_tool_menus.sql`（h2 + mysql）隐藏代码生成菜单
- **OpenAPI：** 生产依赖 `springdoc-openapi-starter-webmvc-api`（无 UI）

### 3.2 Graal 裁剪

- 已有：`-Ob`、`-H:-AddAllCharsets`
- 新增：`-H:IncludeLocales=zh,en`（注意：**不要**写 `-H:+IncludeLocales=...`，GraalVM 21 会报 invalid boolean）
- `application-native.properties`：`spring.web.locale=zh_CN`

### 3.3 去 AWT（Captcha）

- `CaptchaPngRenderer`：纯 Java PNG，无 `java.awt`
- `@Profile("native")` → `NativeCaptchaSupport`（运行时 `/captchaImage` 无 kaptcha）
- `@Profile("!native")` → `JvmCaptchaSupport` + `CaptchaConfig`（kaptcha）
- 验收脚本：`deploy/scripts/verify-native-no-awt.sh`（需 Linux `ldd`；macOS 主机跳过）
- **重建结论：** native-image 仍链接 `libawt.so`（POI + kaptcha 字节码仍在 classpath）；**Captcha 业务路径已不依赖 AWT**。彻底去除 libawt 需 FastExcel + 移出 kaptcha 编译依赖。

### 3.4 UPX

- `UPX_COMPRESS` 默认 `true`；RPM 仍用未压缩 ELF（见 `deploy/upx-native-binary.sh` 说明）

### Native 依赖裁剪

- 曾尝试 `-Pnative` 下 `poi/kaptcha.scope=provided`，因 **多模块 compile 传递** 失败已回退
- POI 仍通过 `ExcelUtil` 打入镜像；**完整排除需 Phase 2.10 FastExcel**

## Phase 3 重建实测（2026-06-14）

命令：`SKIP_EMBEDDED=true UPX_IN_DOCKER=true UPX_SMOKE=true ./deploy/build-native-spring.sh aarch64`

| 指标 | Phase 1 | Phase 3 | Δ |
|------|---------|---------|---|
| arm64 raw | 357 MB | **333 MB** | **-24 MB（-6.7%）** |
| arm64 UPX | 94 MB（26.4%） | **89 MB（26.8%）** | **-5 MB（-5.3%）** |
| UPX 冒烟 | OK | **OK**（port 2023 `/version`） | — |
| libawt | 有 | **仍有**（native-image 日志） | Captcha 运行时无 AWT |

详见 [METRICS.md](./METRICS.md)。

## Stretch 目标评估（3.6）

| 指标 | Stretch | Phase 3 实测 | 结论 |
|------|---------|--------------|------|
| raw | < 250 MB | **333 MB** | **未达标** |
| UPX | < 65 MB | **89 MB** | **未达标**；不阻塞 Phase 4 |
| 冷启动 UPX | < 5 s | 冒烟通过 | 精确秒数待 Docker 补测 |

**Quarkus 对照（embedded）：** raw ~190 MB / UPX ~42 MB — Spring 仍偏大主因：**POI + 未完成的 FastExcel 替换**、Spring 框架本身大于 Quarkus。

## 重建与验收

```bash
cd .worktree/feature-spring-native-upx

# JVM 回归（无 native）
mvn -pl cat2bug-platform-admin -am package -DskipTests

# Native + UPX（Docker ≥12GB 内存）
SKIP_EMBEDDED=true UPX_IN_DOCKER=true ./deploy/build-native-spring.sh aarch64

# 无 AWT 检查
./deploy/scripts/verify-native-no-awt.sh cat2bug-platform-admin/target/cat2bug-admin-linux-arm64
```

## 后续（不阻塞 Phase 4）

1. **Phase 2.10 / B9：** cherry-pick Quarkus FastExcel 服务层，Native 排除 POI（预计最大体积收益）
2. amd64 双架构 CI + 冷启动/RSS 系统补测
3. embedded SPA 全量重建对比（当前 API-only 与 Phase 1 可比）

## 参考

- [B9-EXCEL-NATIVE-MIGRATION.md](../../readme/quarkus-full-migration/B9-EXCEL-NATIVE-MIGRATION.md)
- [METRICS.md](./METRICS.md)
