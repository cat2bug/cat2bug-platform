## Context

- 选择器：`cat2bug-platform-ui/src/components/Cat2BugReportTemplateSelect/index.vue`；列表 `GET /system/report/template/list`，按 `projectId` 查 `sys_report_template`；复制已实现为再 `POST` 一条自定义记录。
- Skills 源：`readme/development/skills/report-templates/29119-3/` 与 `9001/{manuals,records}/`（全量纳入预制）。
- 领域对象已有 `templateKey`、`templateIconUrl`，无分组/只读字段。
- 仓库当前分支 `core`：父 POM / 目录侧 **未见** `cat2bug-platform-quarkus-*` 模块；但 `CLAUDE.md` 等仍写 Quarkus 为默认。退役工作以「清残留 + 叙述改 Spring」为主，若其它分支/脚本仍构建 Quarkus 则一并切断。

## Goals / Non-Goals

**Goals:**

- 选择器三组：自定义 / 29119-3:2021 / ISO 9001；标准组仅复制
- classpath 预制（Spring admin resources）+ 接口获取正文与预览
- HTML→PNG 批生成预览并写入配置（manifest）
- 产品交付与文档默认 **Spring Boot admin**；Quarkus 交付线退役

**Non-Goals:**

- 不把预制行写入 `sys_report_template`（否决 DB seed / 方案 B）
- 不在本变更实现完整 QMS 审批工作流或符合性引擎
- 不恢复或增强 Quarkus 模块
- 不要求用户购买的 ISO/GB PDF 入库（skills 已有正文）

## Decisions

### D1 — 存储：classpath + 列表拼合（方案 A）

- **选择**：预制只存 classpath；列表 API 返回 `groups[]`，每组含 items；自定义来自 DB（`projectId`），标准组来自 `manifest.json`。
- **DTO**：`source=custom|preset`，`groupCode=custom|iso-29119-3|iso-9001`，`editable`/`deletable` 布尔（预制 false）；预制用稳定 `templateKey`（非 DB id）标识。
- **复制**：`POST .../preset/{group}/{key}/copy` 或现有 add 接口接受 `presetKey`；服务端读 classpath md → insert 自定义（可改标题为「副本」）；之后可编可删。
- **守卫**：对自定义 `templateId` 的 update/delete 不变；任何试图用预制 id 更新的请求 404/403。预制无 DB id。
- **替代（已否决）**：启动 sync 入库只读行。

### D2 — 资源布局与源同步

```
cat2bug-platform-admin/src/main/resources/报告/模版/
  manifest.json
  29119-3/<key>.md
  29119-3/<key>.png
  9001/manuals/<key>.md
  9001/manuals/<key>.png
  9001/records/<key>.md
  9001/records/<key>.png
```

- **源**：`readme/development/skills/report-templates/{29119-3,9001}/`
- **脚本**：`scripts/sync-report-preset-templates.sh`（或 npm）从 skills 复制 md → resources，再跑预览生成更新 png + manifest
- **范围**：29119-3 **全部**业务 md；9001 **manuals + records 全部**（不含 9001 顶层 SKILL/overview 等非模版 md）
- **不做** Quarkus `META-INF/resources` 双份

### D3 — manifest 为配置真源

示例字段（实现可微调）：

```json
{
  "groups": [
    {
      "code": "iso-29119-3",
      "title": "ISO/IEC/IEEE 29119-3:2021",
      "items": [
        {
          "key": "test-completion-report",
          "title": "测试完成报告",
          "contentPath": "29119-3/test-completion-report.md",
          "previewPath": "29119-3/test-completion-report.png"
        }
      ]
    },
    {
      "code": "iso-9001",
      "title": "ISO 9001",
      "items": []
    }
  ]
}
```

自定义组不写进 manifest，由 API 运行时填充。

### D4 — 预览：HTML→PNG 批生成

- **选择**：脚本将每个 md 包进固定宽度 HTML（简单 Markdown→HTML，深色卡片风格贴近产品），用 headless Chromium（Playwright 已在仓库出现）截图导出 PNG；路径写入 manifest。
- **失败**：单项失败不阻断其它；manifest 可缺 preview，UI 回退 empty 图。
- **替代**：手工设计图 —— 成本高；纯前端实时渲染 md —— 列表性能差。

### D5 — UI 与分页

- 按组纵向分区；组标题固定文案（i18n）。
- 「+ 添加模版」仅作用于自定义组。
- 标准卡片：只显示复制；隐藏编辑/删除。
- 分页：优先 **自定义组内分页**；标准组可滚动或「展开更多」，避免跨组混页。键盘导航按「当前可见可操作卡片」重算。
- 模版管理页同步只读规则，防止旁路编辑预制。

### D6 — 预览/正文 API

- `GET /system/report/template/picker`（或扩展 list）：返回分组结构。
- `GET /system/report/template/preset/{group}/{key}/content`：md 文本（复制或生成报告时用）。
- `GET /system/report/template/preset/{group}/{key}/preview`：png 字节或静态映射。
- 权限与现有 `system:report:list` / `add` 对齐。

### D7 — 退役 Quarkus 交付

- **选择**：文档与脚本将默认后端改为 `cat2bug-platform-admin`（Spring Boot）；删除或改写 `deploy/*quarkus*`、`build-embedded-quarkus`、`CLAUDE.md` Quarkus 段、前端 `build:embedded-quarkus` 若仅服务 Quarkus。
- 若磁盘仍残留未跟踪的 quarkus 目录/产物：删除并确保 `.gitignore` 不鼓励恢复为交付物。
- CI/CD 与 Docker 合成以 Spring 为准（含既有 native Spring 线若仍保留）。
- **不**在本变更重写业务功能到 Quarkus。

### D8 — 与 skills / 国标变更关系

- 引用已归档 `national-standard-report-templates` 产物；本变更是 **产品化入库（classpath）+ UI**，不重写 skills 正文（除非同步脚本发现路径漂移）。

## Risks / Trade-offs

- [popover 变长] → 组折叠 + 自定义分页；标准组可默认折叠至标题行
- [预览与正文不一致] → 同步脚本在同一流水线先 copy md 再截图
- [中文路径 `报告/模版`] → 构建与 IDE 需验证；若踩坑可改为 `report/templates` 并在 design 修订（默认尊重用户路径）
- [Quarkus 文档清理遗漏] → tasks 含全仓 `rg quarkus` 清零门禁（允许历史 archive openspec 提及）
- [生成报告用预制] → 选预制卡片创建报告时服务端须能读 classpath 正文，不能假设必有 DB id

## Migration Plan

1. 同步 skills → resources + 生成 png/manifest  
2. 后端 picker/preset API + 复制  
3. 前端分组 UI  
4. 清 Quarkus 文档/脚本并改默认叙述  
5. 回滚：移除 resources 下预制目录与 API；UI 恢复扁平 list；文档可 git revert

## Open Questions

- 中文资源目录名若在部分 CI 文件系统异常，是否允许改名为 `report/templates`？（默认先用用户指定的 `报告/模版`）
- 复制后是否自动打开编辑页？（现有 copy 行为：是；保持）
