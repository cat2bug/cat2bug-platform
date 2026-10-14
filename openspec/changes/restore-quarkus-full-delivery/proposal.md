## Why

产品需要更优的冷启动与常驻内存，并最终交付「含嵌入式前端的 Native 二进制」（历史目标约 20MB）。仓库曾完成 Quarkus 全量迁移后又退役（`retire-quarkus-delivery`），改为 Spring Boot + Spring Native。当前在 AI 辅助改写可大幅降低迁移成本的前提下，决定**再次以 Quarkus 为默认后端交付**，并要求与现网 Spring 主线（含源码/Git 管理）**功能与界面完全一致**。

## What Changes

- **恢复并默认启用** Quarkus 交付线：`cat2bug-platform-quarkus-{common,framework,system,admin}`（以历史 `feature/quarkus-full-migration` / `4598a68f` 为基线），重新接入 Maven `-Pquarkus`
- **功能/界面 parity**：与当前 `core`（源码管理收尾点及之后已纳入的业务）行为一致；历史 Quarkus 缺口（尤其 **项目 Git 源码仓库**）必须补齐到与 Spring 同级
- **Excel**：Spring 侧已有的 Excel **导入/导出功能必须完整保留，功能不得裁减**；底层用 Apache POI、FastExcel 或其它库由实现自行选型，但用户可见能力与结果须与现网一致
- **分两阶段交付**（本 change 任务明确拆分）：
  1. **JVM 先跑通**：编译、启动、API/UI 自检与人工验收通过后再继续
  2. **Native 后做**：含 embedded SPA 的二进制体积目标约 **20MB**（可 UPX），本阶段不阻塞人工验收
- **BREAKING（交付叙事）**：文档/脚本/默认运行入口从「Spring admin 唯一」改回「Quarkus admin 为产品默认」；Spring admin 可暂留作对照/回滚，但不再作为推荐发布路径
- **修改既有规格**：推翻或改写 `retire-quarkus-delivery` 中「不以 Quarkus 为默认交付」的要求；Spring Native 相关规格降为可选对照，不再是唯一二进制路径

## Capabilities

### New Capabilities

- `quarkus-backend-parity`: Quarkus 后端与现 Spring 主线在业务 API、鉴权、安装向导、双库（H2/MySQL）、核心模块上的功能等价要求
- `quarkus-git-repositories`: 将当前 Spring 侧项目 Git/源码管理能力迁入 Quarkus（列表/浏览/分支/PR/设置等与 UI 契约一致）
- `quarkus-jvm-acceptance-gate`: JVM 模式下可启动、可登录、冒烟与人工验收通过后才允许进入 Native
- `quarkus-native-slim-binary`: Native + embedded SPA 体积与启动目标（约 20MB 量级），在 JVM 验收后再实施

### Modified Capabilities

- `retire-quarkus-delivery`: **撤销「退役 Quarkus 为默认交付」**；改为允许/要求 Quarkus 作为主交付线（delta 明确废弃旧约束）
- `spring-native-graal-hints`: 降级为可选对照路径；产品默认二进制改为 Quarkus Native（delta 标明优先级变更）

## Impact

- 模块：恢复并维护 `cat2bug-platform-quarkus-*`；根 `pom.xml` `-Pquarkus`；`deploy/build-*-quarkus.sh` 等脚本
- 前端：继续 embedded SPA；路由/API 契约不变（parity）
- 后端：Quarkus REST/MyBatis/Security/Flyway/缓存等替代 Spring 栈；Git 能力需从 `cat2bug-platform-system` 迁入 Quarkus system/admin
- 规格：与 `retire-quarkus-delivery`、`spring-native-graal-hints` 冲突处用 delta 解决
- 风险：历史 Quarkus 基线早于源码管理功能；parity 工作量集中在 Git 与后续 Spring 独有增量
- 已提前检出的工作区文件（Quarkus 模块树、部分 pom/profile）仅作提案实施起点，**正式实现以本 change 的 `/opsx:apply` 为准**
