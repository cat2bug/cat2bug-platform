# Gate A vs Gate B

本 change 分两阶段交付；**不得跳过 Gate A 直接做 Gate B 体积优化**。

## Gate A — JVM 功能验收

| 项 | 说明 |
|---|---|
| 目标 | Quarkus **JVM** 与现 Spring 主线功能 / UI 契约一致，可启动、可登录、可冒烟 |
| 产物 | 可运行的 Quarkus JVM JAR（可用 `./deploy/build-embedded-quarkus.sh` + `./deploy/run-quarkus-jar.sh`） |
| 范围 | 编译、配置、核心 SPA 流程、Excel、Git 等 parity；见 `tasks.md` §2–§3、§5 |
| 验收 | 产品负责人 **人工 UI/功能验收** 通过 |
| 明确不做 | Native 构建、体积压测、UPX 达标声明 |
| **状态（2026-10-14）** | **Gate A 已人工通过** |

本地路径见 [LOCAL-RUN.md](./LOCAL-RUN.md)。

## Gate B — Native 瘦身二进制（验收通过后再做）

| 项 | 说明 |
|---|---|
| 前置 | Gate A 人工验收通过（**已满足**）；维护者确认可进入 Native |
| 目标 | Quarkus Native + embedded SPA；体积约 **~20MB** |
| 体积口径 | **默认按 UPX 压缩后** 的可分发二进制字节数计（与历史 Quarkus 交付一致）；若未使用 UPX 须在发布说明中写明实测口径 |
| 验收 | Native 可启动、SPA、登录，以及至少一条 Git 与一条核心业务路径冒烟 |
| 脚本 | `deploy/build-native-quarkus.sh`（embedded + Native + UPX）、`cat2bug-platform-quarkus-admin/smoke-native-docker.sh` |
| **状态（2026-10-14）** | Native 功能冒烟通过（已有库 + 全新安装）；UPX 后 ≈38.8 MiB（已去 POI/XmlBeans/Saxon 与 docs，并完成前端瘦身），未达 ~20MB，产品负责人已接受该体积。详见 [NATIVE-RELEASE.md](./NATIVE-RELEASE.md) |

## 关系示意

```
Quarkus JVM parity → 冒烟 →【Gate A 人工验收】→ Quarkus Native + UPX →【Gate B ~20MB】
```

Spring Boot admin / Spring Native 仅作对照或紧急回滚，不参与上述默认门禁路径。
