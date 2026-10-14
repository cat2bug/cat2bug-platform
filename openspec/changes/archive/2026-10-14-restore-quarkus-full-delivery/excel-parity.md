# Excel 全量对等对照（Spring ↔ Quarkus）

> Change: `restore-quarkus-full-delivery` 任务 2.4  
> 库：Quarkus 使用 Apache POI（`SystemExcelExportService` / `SimpleExcelExportSupport` / 业务专用 Export/Import Service）  
> 约定：路径与 Spring 一致；SPA 未调用的导出端点默认仍补齐（产品要求功能不能少）

## 状态图例

| 标记 | 含义 |
|------|------|
| ✅ | Quarkus 已对等 |
| 🔧 | 本次补齐 |
| ⚠️ | Spring 有、Quarkus 刻意范围外（见备注） |
| 🔀 | 等价别名路径（不同 Resource 名，路径对齐 SPA） |

## 导出（Export）

| Spring 入口 | HTTP 路径 | SPA 调用 | Quarkus | 备注 |
|-------------|-----------|----------|---------|------|
| `SysDefectController` | `POST /system/defect/export` | 是 | ✅ `DefectResource` | |
| `SysCaseController` | `POST /system/case/export` | 是 | ✅ `CaseResource` | |
| `SysUserController` | `POST /system/user/export` | 间接 | ✅ `UserResource` | SPA 主用 member 别名 |
| `Member` 别名 | `POST /system/member/export` | 是 | 🔀 `MemberResource` | 转发/等价用户导出 |
| `SysRoleController` | `POST /system/role/export` | 是 | ✅ `RoleResource` | |
| `SysPostController` | `POST /system/post/export` | 是 | ✅ `PostResource` | |
| `SysConfigController` | `POST /system/config/export` | 是 | ✅ `ConfigResource` | |
| `SysDictTypeController` | `POST /system/dict/type/export` | 是 | ✅ `DictTypeResource` | |
| `SysDictDataController` | `POST /system/dict/data/export` | 是 | ✅ `DictDataResource` | |
| `SysPlanController` | `POST /system/plan/export` | 是 | ✅ `PlanResource` | |
| `SysPlanItemController` | `POST /system/PlanItem/export` | 是 | ✅ `PlanItemResource` | |
| `SysProjectController` | `POST /system/project/export` | （管理） | ✅ `ProjectResource` | |
| `SysTeamController` | `POST /system/team/export` | （管理） | ✅ `TeamResource` | |
| `SysModuleController` | `POST /system/module/export` | （管理） | ✅ `ModuleResource` | |
| `SysDocumentController` | `POST /system/document/export` | （管理） | ✅ `DocumentResource` | |
| `SysReportController` | `POST /system/report/export` | （管理） | ✅ `ReportResource` | |
| `SysReportTemplateController` | `POST /system/report/template/export` 等 | （管理） | ✅ `ReportTemplateResource` | |
| `SysProjectApiController` | `POST /system/project/api/export` 等 | （管理） | ✅ `ProjectApiResource` | |
| `SysDefectLogController` | `POST /system/defect/log/export` 等 | （管理） | ✅ `DefectLogResource` | |
| `SysUserStatisticTemplateController` | `POST .../export` | （管理） | ✅ `StatisticTemplateResource` | |
| `SysAiModuleConfigController` | `POST /system/ai/export` | 是 | ✅ `AiResource` | |
| `SysAiAccountController` | `POST /ai/openai/account/export` 等 | 间接 | ✅ `AiAccountResource` | |
| AI 账号 SPA 别名 | `POST /ai/account/export` | 是 | 🔀 `AiAccountExportAliasResource` | |
| `SysDashboardController` | `POST /system/dashboard/{id}/defect-line/export` | 是 | ✅ `DashboardResource` | |
| `SysDashboardController` | `POST /system/dashboard/{id}/member-defect-line/export` | 是 | ✅ `DashboardResource` | |
| `SysDashboardController` | `POST /system/dashboard/{id}/plan-statistics/export` | 是 | ✅ `DashboardResource` | |
| `SysDefectStatisticController` | `POST /system/defect/statistic/defect-state-line/{id}/export` | 是 | ✅ `DefectStatisticResource` | |
| `SysDefectStatisticController` | `POST /system/defect/statistic/member-defect-line/{id}/export` | 是 | ✅ `DefectStatisticResource` | |
| `SysOperlogController` | `POST /monitor/operlog/export` | **是** | 🔧 `OperlogResource` | 本次补齐；含 list/remove/clean |
| `SysLogininforController` | `POST /monitor/logininfor/export` | **是** | 🔧 `LogininforResource` | 本次补齐；含 list/remove/clean/unlock |
| `SysScreenSizeController` | `POST /system/ScreenSize/export` | **否**（SPA 仅 list） | 🔧 `ScreenSizeResource` | Spring 有、SPA 未用、**仍补齐**；并补 list/CRUD 供浏览器组件 |
| `SysCasePromptController` | `POST /export`（已注释） | 否 | — | Spring 已停用，不对等 |
| `SysJobController`（quartz 模块） | `POST /monitor/job/export` | 页面有 | ⚠️ | Quarkus 未纳入 quartz 定时任务模块；非本轮 Excel 业务范围 |
| `SysJobLogController`（quartz） | `POST /monitor/jobLog/export` | 页面有 | ⚠️ | 同上 |

## 导入 / 模板（Import）

| Spring 入口 | HTTP 路径 | SPA 调用 | Quarkus | 备注 |
|-------------|-----------|----------|---------|------|
| `SysDefectController` | `POST /system/defect/import` | 是 | ✅ | |
| `SysDefectController` | `POST /system/defect/importTemplate` | 是 | ✅ | |
| `SysCaseController` | `POST /system/case/importData` | 是 | ✅ | |
| `SysCaseController` | `POST /system/case/importTemplate` | 是 | ✅ | |
| `SysUserController` | `POST /system/user/importData` | 间接 | ✅ `UserResource` | |
| `SysUserController` | `POST /system/user/importTemplate` | 间接 | ✅ | |
| Member 别名 | `POST /system/member/importData` | 是 | 🔀 `MemberResource` | |
| Member 别名 | `POST /system/member/importTemplate` | 是 | 🔀 | |
| `SysGitRepositoryController` | `POST .../import` | Git UI | — | **另 agent 负责 Git，本文件不改 Git 迁移** |

## 本次缺口结论（原「可能缺口」）

| 项 | 核实结果 | 处置 |
|----|----------|------|
| ScreenSize `/export` | Quarkus 原无等价 Resource；SPA 无 export 调用，但有 `listScreenSize` | **已补齐** Resource + `SystemExcelExportService.exportScreenSizes` |
| Operlog `/export` | Quarkus 原无；SPA `monitor/operlog` 页调用 export | **已补齐** |
| Logininfor `/export` | Quarkus 原无；SPA `monitor/logininfor` 页调用 export | **已补齐** |
| unlock 端点 | Spring 清密码重试缓存；Quarkus 无对等缓存 | 保留路径，返回成功（契约兼容） |

## 2.5 契约（Excel 相关）

- 导出路径、权限码、`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 响应与 Spring/SPA 一致。
- 已有 SPA 别名：`/system/member/*`、`/ai/account/export` 保持，无需改 Vue。
- quartz Job 导出不在 Quarkus 交付范围内（模块未恢复），不记为 Excel 业务缺口。

## 编译说明

- Excel 相关源码与 Resource 在排除并行 Git 迁移源后，`mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am package -DskipTests` **BUILD SUCCESS**。
- 全量命令当前仍会被另一 agent 未完成的 Git 源编译错误挡住（与本 Excel 改动无关）；Git 完成后应再跑全量 package。
