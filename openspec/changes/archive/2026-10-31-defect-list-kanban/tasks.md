## 1. 视图入口与壳

- [x] 1.1 在 `defect/index.vue` 扩展 `DEFECT_CONTENT_VIEW_ALLOWED`、import/注册 `DefectKanban`、radio「看板」置于甘特后，并支持 Cmd/Ctrl+O 循环
- [x] 1.2 新增 `list/kanban.vue`：承接 `left-tools` slot、接收 `:query`，空态与加载态可用

## 2. 数据分桶与卡片

- [x] 2.1 按当前筛选拉取列表（首版 pageSize 常量建议 200，不足可分段续拉），按 `defectStateName` 分入三泳道；`RESOLVED` 不入列
- [x] 2.2 卡片展示编号、标题、类型、处理人、已驳回标记；单击/Ctrl·⌘/同列 Shift 多选；拖动手势与打开详情防误触
- [x] 2.3 处理中列禁止 PROCESSING↔REJECTED 互拖改态

## 3. 拖拽流转与弹框

- [x] 3.1 实现落点→动作映射（repair/reject/pass 优先于 close/open），单卡用 `isDefectActionVisible`，非法回弹+提示
- [x] 3.2 接入 Assign/Repair/Reject/Pass/Close/Open 弹框；取消不调 API、卡回原列；成功后刷新或本地改桶
- [x] 3.3 多选整组拖：按「同一动作 + 每条均可」求交；串行调单条 API（可复用批量弹框模式）

## 4. 指派

- [x] 4.1 单卡指派入口打开 `AssignDialog`
- [x] 4.2 多选批量指派：严格求交后串行提交，看板内自管入口（不破坏表格专属批量条约定）

## 5. 回归与文档

- [x] 5.1 手工回归：各泳道单向拖拽、pass/close 优先、重开、多选非法回弹、与表格按钮权限一致
- [x] 5.2 回写长期知识：更新 `docs/04-UI规范/组件与交互约定.md` 缺陷视图切换；`docs/08-版本记录` 当月一行；确认无新 ADR / 无新踩坑则注明
