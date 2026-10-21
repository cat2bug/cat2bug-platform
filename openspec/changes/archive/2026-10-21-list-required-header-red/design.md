## Context

`Cat2BugTable` 已支持列配置 `required: true`：表头标题与 pin 使用 `#f56c6c`，无星号。缺陷（名称、处理人）与用例（名称、交付物、预期）已启用。测试计划、交付物、文档三页列默认配置尚未标记，与各自表单必填字段不一致。

约束：不改表格组件样式契约；列宽缓存合并以默认列为 base（`mergeCachedColumns`），只需改默认配置。

## Goals / Non-Goals

**Goals:**

- 三页主列表中，与表单必填对应的名称列表头呈红色
- 与缺陷页观感一致，本地缓存用户改过的宽/固定/显隐后刷新仍保持红色

**Non-Goals:**

- 不改用例列表、执行计划抽屉子表、新建计划弹窗内表
- 不为文档「文件」字段造列表列
- 不新增 ADR、不改主题变量、不改表单校验规则本身

## Decisions

1. **只改列默认配置，不改 `Cat2BugTable`**  
   能力已具备；改配置成本最低、与缺陷/用例一致。  
   备选：在页面 CSS 硬编码某列表头色 → 否决（绕过统一契约、难维护）。

2. **必填映射以当前表单 rules 为准**  
   - 计划 → `plan.name`（仅 `planName`）  
   - 交付物 → `module.name`  
   - 文档 → `doc.name`（`fileUrl` 无列表列）  
   备选：凭产品直觉多标版本/时间 → 否决（用户明确「按必填项」）。

3. **不主动清 localStorage 列缓存**  
   `mergeCachedColumns` 用 `...base` 带入默认 `required`，旧缓存安全。

## Risks / Trade-offs

- [Risk] 交付物名称在部分对话框场景为条件校验，列表仍整列标红 → 接受：与「名称是核心必填」一致，且缺陷侧也是整列标红  
- [Risk] 用户若期望星号而非纯色 → 既有规范为无星号，本次不改

## Migration Plan

- 纯前端配置变更；发版后刷新即可  
- 回滚：去掉三处 `required: true`

## Open Questions

- 无

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/04-UI规范/组件与交互约定.md`（已回写「必填列表头」）
- `docs/08-版本记录/2026-10.md`
- 主规格：`openspec/specs/list-required-column-header/spec.md`
- 既有能力：`openspec/specs/cat2bug-table-header-handles/spec.md`（表头手柄；必填色与手柄共存）
- 拟新增 ADR：无
