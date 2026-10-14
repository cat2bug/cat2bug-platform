# ADR-004 Native 交付分两道门，并接受约 38 MiB

记录 Quarkus Native 交付的门禁顺序和体积口径，防止以后为了体积去裁功能。

- 日期：2026-10-14
- 状态：已采纳

## 背景

目标是 Quarkus Native + 内嵌 SPA 单文件，体积约 20 MB（按 UPX 后计）。但 parity 未稳时做 Native 容易浪费构建时间。依据：`openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/GATE-NOTES.md`、`NATIVE-RELEASE.md`、`SMOKE-RESULTS.md`、`design.md` D2/D4。

## 候选方案

### 方案 A：先 JVM 功能对等并人工验收（Gate A），通过后再做 Native（Gate B）

优点：功能稳定后再优化体积，避免返工。

缺点：Native 问题暴露得晚。

### 方案 B：JVM 与 Native 并行

缺点：parity 未稳时浪费构建时间，否决。

## 最终决定

- **Gate A**：Quarkus JVM 与 Spring 功能/UI 契约一致，产品负责人人工验收（2026-10-14 已通过）。不做 Native、不声明体积。
- **Gate B**：Gate A 通过后才做 Native + UPX；验收为可启动、SPA、登录、至少一条 Git 与一条核心业务路径冒烟（`smoke-native-docker.sh`，已有库 + `FRESH=true` 全新安装两种模式）。
- **体积口径**：默认按 UPX 后可分发二进制计。
- **接受约 38.8 MiB**（arm64，原始 169 MiB），未达 ~20 MB，产品负责人已确认接受，不再强求。
- 硬约束：**禁止以体积裁功能**（Excel 含图表、Git、H2+MySQL 同包、嵌入 SPA 单文件均保留）。

## 原因

用户明确要求「先功能一致并人工确认，再做二进制」。瘦身过程：POI 版约 60 MiB → FastExcel 版约 46.6 MiB → 前端瘦身约 38.4 MiB → 对等补齐后约 38.8 MiB；在保留 JGit、双库、嵌入 SPA 前提下已接近下限。

## 副作用

- 要继续接近 20 MB 只能做产品取舍：按数据库拆 H2/MySQL 两个包（约 −2~3 MB）、fastjson2 统一为 Jackson（约 −1~2 MB）、SPA 不嵌入（约 −5 MB，放弃单文件）、收窄代码高亮语言包等。
- FastExcel 导入只支持 `.xlsx`；图表靠自研 `XlsxPatcher` 注入。
- 安装向导完成后进程退出，需 systemd / Docker `restart` 拉起；整个工作目录（`data/`、`upload/`、`git/`、`config/install/`）必须持久化。
- amd64 未在本机构建，体积预计同量级（待确认）。
