## 1. 推挤算法与权限

- [x] 1.1 在 `defect-gantt.js` 实现 `pushOverlapsAfterPinned`：锚定落点为钉，同泳道可改重叠同伴只后移、工期不变
- [x] 1.2 保留/实现候选过滤与管理员判定：未关闭 ∧ 已排期 ∧（`project.create-by`/`project.admin` 或当前用户 ∈ `handleBy`）
- [x] 1.3 拖动同伴取自当前已加载 `defectList`（与改期一致）
- [x] 1.4 为推挤与权限补单元测试（重叠后移、空气不挡、空结果；可保留 `levelDefectPlans` 夹具）

## 2. 图层开关与拖动写回

- [x] 2.1 图层下拉增加「自动排列」勾选；无 `system:defect:edit` 时禁用；持久化 `layers.autoLevel`
- [x] 2.2 勾选后单条拖/改期预览推挤；多选批量平移不推挤
- [x] 2.3 松手后有限并发 `updateDefect` 写回被推条计划起止；失败汇总提示
- [x] 2.4 中英 i18n（开关文案与部分失败提示）

## 3. 验收与文档

- [x] 3.1 手工/单测覆盖：开关关不推；开则重叠后移；空气不挡；批量不推
- [x] 3.2 回写 `docs/04-UI规范/组件与交互约定.md`、`docs/08-版本记录/2026-10.md`；无新 ADR
