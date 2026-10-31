## Context

现行状态序：`PROCESSING(0)` / `AUDIT(1)` / `RESOLVED(2 隐藏)` / `REJECTED(3)` / `CLOSED(4)`。新建默认 `PROCESSING`；`pass` 与 `close` 均写 `CLOSED`。看板「未指派」靠空 `handleBy` 投影（`kanban-unassigned-lane` + `defect-handle-by-optional`）。产品决定：无人新建 → **已创建**；验证通过 → **通过**（与关闭并列）；看板仍四列，末列「已结束」共桶通过+关闭。

## Goals / Non-Goals

**Goals:**

- 引入 `CREATED(5)`；复活并重释义 `RESOLVED` 为「通过」。
- 创建 / 指派 / pass / close / open 与四视图（含看板分桶）对齐上述语义。
- 配置态列表重新包含 `RESOLVED`，并加入 `CREATED`。

**Non-Goals:**

- 不迁移历史 `CLOSED`（含旧 pass 落关闭）。
- 不位移既有 0～4 序号；不新增「通过后再关闭」强制步骤。
- 不改缺陷类型枚举；不重做甘特计划权限模型。

## Decisions

1. **`CREATED` 码值 = 5（追加）**  
   避免改写库内既有 0～4。UI/配置列表展示顺序可把「已创建」排在处理中之前，与码值无关。  
   *备选*：插入 0 并整体位移 → 否决（全库迁移）。

2. **`RESOLVED` 重开为「通过」**  
   `pass` → `STATE_RESOLVED`；i18n `RESOLVED` →「通过」。`getDefectStateList` 不再过滤 `RESOLVED`。  
   *备选*：新码 `PASSED` → 否决（多一档、历史 RESOLVED 行无归宿）。

3. **创建默认态**  
   `handleBy` 空 → `CREATED`；非空 → `PROCESSING`。编辑保存：若清空处理人且当前非 `CLOSED`/`RESOLVED`，规范为 `CREATED`；若 `CREATED` 且写入非空处理人，升为 `PROCESSING`（与指派一致）。

4. **`assign`**  
   允许源态含 `CREATED`；成功后状态 MUST 为 `PROCESSING`，处理人非空。其它源态指派仍只改人（保持现语义）除非产品后补。

5. **看板分桶**  
   - 已创建：`CREATED`  
   - 处理中：`PROCESSING` | `REJECTED`  
   - 待验证：`AUDIT`  
   - 已结束：`RESOLVED` | `CLOSED`（角标/状态旗区分）  
   删除「空 handleBy → 未指派」优先分支；左列加号仍可打开新建（默认无人则入已创建）。

6. **拖拽**  
   - 已创建 → 处理中：`assign`（须人）  
   - 已创建 → 待验证/已结束：禁止（或仅当未来定义直通规则；本期禁止）  
   - 待验证 → 已结束：优先 `pass`（→ 通过）；菜单另可 `close`  
   - 已结束 → 处理中：`open`（`CLOSED` 与 `RESOLVED` 均可重开；有人 → `PROCESSING`，无人 → `CREATED`/未指派）  
   - 处理中 ↔ 待验证：既有 repair/reject  

7. **Open API / MCP**  
   `ApiDefectStateEnum` 增 `CREATED`；指南与 `pass_defect` 说明改为进入通过。工具层处理人规则同步：`CREATED` 可被有指派权者 assign。

8. **拟新增 ADR**  
   建议：`ADR-0xx-缺陷已创建与通过状态`（长期状态机约定）——若团队希望术语表 suffices 可仅改术语表、不单开 ADR；本变更默认 **改术语表 + 交互约定**，ADR 可选。

## Risks / Trade-offs

- [旧客户端假定 pass→CLOSED] → 文档/指南标明；MCP 实况表更新。  
- [统计把 RESOLVED 当「已解决」旧义] → 改文案与色板；趋势图键名可保留 `RESOLVED`。  
- [PROCESSING 空人历史行] → 不强制迁 `CREATED`；可选后续批处理，本期不做。  
- [看板「已结束」两态共列] → 靠状态旗区分，避免五列过挤。

## Migration Plan

- 无 Flyway 改列；枚举追加即可。  
- 发布后新建无人单为 `CREATED`；新 pass 为 `RESOLVED`。  
- 回滚：恢复 pass→CLOSED、隐藏 RESOLVED、去掉 CREATED 创建分支（码 5 存量行需兼容展示）。

## Open Questions

- 无（看板四列末列共桶、不迁历史、仅无人叫已创建已确认）。

## 相关长期文档

- `docs/01-产品需求/术语表.md`
- `docs/04-UI规范/组件与交互约定.md`
- `docs/03-技术方案/MCP接入与权限.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/08-版本记录/2026-10.md`
- 拟新增 ADR（可选）：`ADR-0xx-缺陷已创建与通过状态`
