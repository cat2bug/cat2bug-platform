## 1. 后端状态与流转

- [x] 1.1 `DefectConstants`：追加 `CREATED`（码 5）、`STATE_CREATED`；`getDefectStateList` 重新包含 `RESOLVED` 并加入 `CREATED`（展示序可已创建→处理中→…）
- [x] 1.2 Open API `ApiDefectStateEnum` 等与导入标签解析支持 `CREATED`；非法码仍 400
- [x] 1.3 创建默认态：无人 → `CREATED`，有人 → `PROCESSING`
- [x] 1.4 `pass` → `RESOLVED`；`close` 仍 → `CLOSED`；`open` 允许 `CLOSED`|`RESOLVED` → `PROCESSING`
- [x] 1.5 `assign`：源含 `CREATED`，成功后强制 `PROCESSING` + 非空处理人；更新接口清空/补人时按 design 规范 CREATED↔PROCESSING
- [x] 1.6 动作可见性与 MCP 工具/指南 `mcp-guide` 缺陷主题与 `pass` 后状态同步

## 2. 前端四视图与文案

- [x] 2.1 i18n：`CREATED`→已创建；`RESOLVED`→通过（各语言）；状态旗/颜色变量覆盖两态
- [x] 2.2 表筛选与展示、Excel 状态下拉与导入（含 CREATED/RESOLVED，不再隐藏 RESOLVED）
- [x] 2.3 甘特条色 / 图例纳入已创建与通过
- [x] 2.4 看板：左列改为 `CREATED`（文案已创建）；去掉空 `handleBy` 投影优先；已结束=`RESOLVED`|`CLOSED`；拖拽映射按 design（已创建→处理中=assign；待验证→已结束优先 pass）
- [x] 2.5 DefectTools / 批量 / `defect-action-visibility`：CREATED 可 assign；RESOLVED 可 open；pass 后 UI 进通过

## 3. 验收与文档

- [x] 3.1 手工：无人新建→已创建列；指派→处理中；pass→通过且在已结束；close→关闭；重开通过/关闭→处理中；有人新建不进已创建
- [x] 3.2 手工：表/Excel/甘特能筛或识别已创建与通过；旧 CLOSED 未变
- [x] 3.3 回写 `docs/01` 术语表、`docs/04` 看板与状态、`docs/03` MCP、`docs/05` 若有状态表、`docs/08` 当月一行；可选 ADR「缺陷已创建与通过状态」——若只改术语表则注明无新 ADR
