# ADR-001 Spring Native 作为单文件交付线

记录 2026-06 决定在 Spring Boot 主线上做 Native 单文件交付、并把当时的 Quarkus 分支冻结为对照的原因。

- 日期：2026-06-15
- 状态：被 [[ADR-003-恢复Quarkus为默认后端-Spring保留为回滚线]] 取代（Spring Native 降为对照/回滚）

## 背景

产品要求交付「无需 JRE、无需 nginx」的单个 Native ELF，且内嵌前端 SPA。当时主线 `core` 是 Spring Boot 3.3.13 多模块单体；另有 `feature/quarkus-full-migration` 分支已完成 Quarkus 全量迁移并实测 embedded Native（未压缩约 190 MB、UPX 后约 42 MB），但两条线 API 实现不同，维护两套成本高。依据：`openspec/changes/archive/2026-06-15-spring-native-delivery/design.md`。

## 候选方案

### 方案 A：Spring Boot 主线直接做 GraalVM Native

优点：不用重写 Controller；与 JVM 版共用代码；可 cherry-pick Quarkus 线与框架无关的优化（FastExcel、Captcha 去 AWT、UPX 脚本）。

缺点：体积预计大于 Quarkus（stretch 目标 raw < 250 MB、UPX < 65 MB）；MyBatis、IM、Quartz 等需大量 AOT hints。

### 方案 B：继续以 Quarkus 线为交付

优点：体积、启动已验证。

缺点：与主线 API 双栈，需持续把主线新功能重写到 Quarkus。

### 方案 C：Paketo `spring-boot:build-image`

优点：官方工具链。

缺点：产出 OCI 镜像而非裸 ELF，不符合「单文件分发」，否决。

## 最终决定

采用方案 A：在 `feature/spring-native-upx` 上做 Spring Native（`-Pnative -Pembedded`，Mandrel/GraalVM JDK 21 容器构建，可选 UPX），Quarkus 线冻结为对照/archive；两线禁止整分支 merge，只 cherry-pick 框架无关的优化。设了 Kill Gate：Phase 1 若 `native:compile` 无解，则改由 Quarkus 交付。

## 原因

避免双栈 API 重写；Quarkus 线已验证的 Native 优化可以移植到 Spring 代码路径。

## 副作用

- 实测 amd64 Spring Native 原始约 333 MB、UPX 后约 84 MB（提交 `1281ca1e`），明显大于 Quarkus。
- 带来一串 Native 运行期问题（i18n、J2Cache、MessageSource 绑定等），见 [[Native下i18n消息源加载失败显示原始key]]、[[Native-Docker登录J2Cache缓存未注册导致NPE]]。
- 相关脚本 `deploy/build-native-spring.sh`、文档 `readme/spring-native-delivery/` 仍保留，作为对照路径。（2026-10-14 已随 Spring 线删除，见 [[ADR-010-移除Spring后端并统一模块命名]]。）
