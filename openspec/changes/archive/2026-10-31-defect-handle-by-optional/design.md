## Context

处理人（`handleBy`）在 UI 创建/编辑、Excel、内置字段注册表上均为必填；MCP create 已允许空列表。看板「未指派」泳道（`kanban-unassigned-lane`）按「活跃态 + 空 handleBy」分桶，但在必填约束下几乎永远为空。产品决定：建单可不选处理人，先进入未指派，再经 `assign` 认领。

约束：不改流转状态机；`repair`/`reject`/`pass` 仍要求当前处理人；指派/提测弹框选人仍必填。不新增 ADR。

## Goals / Non-Goals

**Goals:**

- 创建、编辑保存、Excel 导入允许空处理人。
- 内置字段 `handleBy` 元数据 `required=0`，仍 `ALWAYS_ENABLED`（始终展示）。
- 列表处理人列取消必填红标。
- 与既有未指派泳道、assign 可见性（无人时可指派）形成闭环。

**Non-Goals:**

- 不改变「清空处理人」为看板拖入未指派的能力（仍只出不进）。
- 不改 MCP 工具契约（本就可空）；不新增权限点。
- 不强制历史数据迁移；不把「未指派」改成独立状态枚举。
- 不把 `defectName` 改为可选。

## Decisions

1. **可空边界 = 持久化字段，不是流转动作**  
   仅放开「写入缺陷实体时的 handleBy」。`AssignDialog` / 批量指派、`RepairDialog` 审核人等动作表单保持 required。  
   *备选*：动作也可空 → 否决，会破坏「有人负责才能推进」。

2. **空值形态**  
   前端提交 `null` 或 `[]` 均可；前端分桶与可见性已按「非数组或 length===0」判空。后端保持现有 enrich/通知对空列表的跳过逻辑；若某处只认 `null`，在实现时统一归一为空列表或两边都认。  
   *备选*：强制只存 `null` → 不必要，成本高于收益。

3. **ALWAYS_ENABLED 保留，仅降 required**  
   处理人仍强制出现在表单/字段布局中（不可被团队关掉），避免「字段消失导致无人知道可指派」。只去掉必填星号/校验。  
   *备选*：同时允许关闭该字段 → 否决，看板与列表依赖该列语义。

4. **Excel**  
   `DefectExcelColumnSupport` 中 `handleByNames` 的 `required` 改为 false；导入解析空字符串 → 空处理人。列表格内编辑若仍有「不能为空」硬校验，一并去掉。

5. **后端是否加显式「允许空」校验变更**  
   以确认创建路径不因空 handleBy 返回 4xx 为准；`DefectBuiltinFieldRegistry` 改 required 后，依赖该注册表生成模板/校验的路径自动对齐。若存在硬编码「handleBy 必填」的断言或测试，改为可选用例。

6. **文档**  
   回写 `docs/04`：必填列先例改为「缺陷名称」为主（处理人不再红标）；看板未指派前提改为「创建可不选处理人」。

## Risks / Trade-offs

- [漏派堆积] → 未指派列可见计数即收件箱；不在本期做 SLA/提醒。  
- [旧客户端仍校验必填] → 仅改本仓前后端；外部集成本就可空。  
- [团队已缓存「处理人必填」心智] → UI 去星号 + 未指派列解释「无处理人」。  
- [Excel 旧模板表头带「(必填)」] → 新导出会更新；旧文件多写必填标记不阻塞空值（以列 required 标志为准）。

## Migration Plan

- 纯配置/校验变更，无 DB schema。发布后新建可不选处理人；已有数据不变。  
- 回滚：恢复前端 rules、registry required、Excel required、列表 `required: true`。

## Open Questions

- 无（创建可空 + 动作必填 + ALWAYS_ENABLED 已在探索中确认）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（必填列表头、看板未指派）
- `docs/03-技术方案/MCP接入与权限.md`（流转处理人规则；本变更不改规则）
- `docs/08-版本记录/2026-10.md`（合入后补一行）
- 拟新增 ADR：无
