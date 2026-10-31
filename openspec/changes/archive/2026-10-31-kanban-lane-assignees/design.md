## Context

缺陷看板（`list/kanban.vue`）已交付三泳道与跨泳道拖拽：落点经 `resolveKanbanDropAction` 映射 `repair` / `reject` / `pass` / `close` / `open`，默认一律弹框。一期 OpenSpec 将「拖到成员 chips」列为 Non-Goal。角色键 `project.develop` / `project.tester` / `project.admin` / `project.create-by` 已存在；选人组件为 `SelectProjectMember`；全局执行人筛选为父页 `queryParams.handleBy`。`RepairDialog` 仅强制 `receiveBy`，描述可选，具备「砸头像直提」的 API 条件。

## Goals / Non-Goals

**Goals:**

- 处理中 / 待验证泳道头展示可多选的负责人头像，默认按开发/测试角色填充，空则管理员或创建人。
- 候选受全局 `handleBy` 约束；勾选结果本机持久化。
- 拖入待验证且动作为 `repair` 时叠加头像落点：命中直提，未命中弹框。
- 驳回不参与瞄准；已结束无负责人。

**Non-Goals:**

- 服务端持久化泳道负责人；项目级共享配置。
- 驳回 / 关闭 / 重开的头像直提或改后端 `resolveRejectHandleBy`。
- 同列拖到头像做 `assign`（可后续变更）。
- 已结束泳道负责人条。
- 新后端接口或权限码。

## Decisions

1. **负责人只服务「可无描述完成的选人动作」**  
   首期仅 `repair`：待验证列叠加层可直提。处理中列仍显示/配置开发负责人（对称 UI + 本机偏好），拖入处理中（`reject`/`open`）不叠瞄准层。  
   *备选*：处理中也叠层但仅高亮不改提交 → 易误导，否决。

2. **默认成员解析顺序**  
   `listProjectRole` 找到 `project.develop` / `project.tester` 的 `roleId` → `listMemberOfProject({ roleId })`（或 `getProject().members` 按角色过滤）。若结果为空：取 `project.admin` 与 `project.create-by`（及 `project.createBy` 用户）并集去重。再与 `query.handleBy` 求交；`handleBy` 空则不过滤。  
   *首次进入*：无本机缓存时用上述默认写入勾选；*已有缓存*：以缓存为准，但展示/可选仍受当前 `handleBy` 过滤（缓存里不在候选中的 id 隐藏，不自动删缓存，以免筛掉执行人后丢偏好）。

3. **本机存储**  
   Key：`defect.kanban.laneAssignees.<projectId>`，值形如 `{ processing: number[], audit: number[] }`。不存已结束。换项目换键。

4. **拖放命中**  
   在 `dragover`/`drop` 阶段识别指针下的头像节点（`data-user-id`）；命中则 `preventDefault` 后直调 `repair(id, { receiveBy: [uid], describe: '' 或省略 })`。未命中走现有 `onLaneDrop` → 弹框。多卡：对每条校验 `isDefectActionVisible('repair', …)`，全部可则串行直提；任一失败停止并 toast，已成功的保留（与批量工具「部分成功」可接受；实现时统一提示已完成数）。

5. **权限失败**  
   命中头像但当前用户不能对该卡 `repair`：toast，不弹框、不请求。整组有一张不可则整组不直提（与一期「非法整组回弹」一致），可改为弹框——**选整组不直提 + toast**，避免半自动半弹框。

6. **UI 复用**  
   泳道头：头像条 + 点击打开 `SelectProjectMember`（`multiple`，候选列表前端按上面规则过滤，或传入已过滤 `memberId` 列表若组件支持；否则弹层内过滤）。叠加层：放大头像，可与头部分离渲染在 `lane-body` 顶部，避免与卡片滚动抢事件。

7. **与一期看板关系**  
   不修改已完成变更目录；本变更 spec 定义增量行为。合入后 UI 文档写清：repair 在命中泳道负责人头像时可跳过弹框。

## Risks / Trade-offs

- [直提无描述] → 与表格「可填描述」不一致；接受（描述本可选），需要备注时可未命中头像走弹框。  
- [全局执行人过窄导致默认被滤空] → 头像条可空，点击仍打开多选（候选=过滤后集合）；不强制塞管理员进勾选以免违背筛选意图。  
- [HTML5 DnD 与头像命中抖动] → 头像热区足够大；`dragleave` 防抖与一期泳道同策略。  
- [本机缓存跨浏览器不同步] → 产品已选本机，可接受。

## Migration Plan

- 纯前端；无 DB 迁移。上线后无缓存用户看到角色默认；有旧看板会话无影响。回滚：去掉负责人 UI 与直提分支即可。

## Open Questions

- （无阻塞）同列拖到开发头像做快速指派是否单开后续变更——本设计明确不做。

## 相关长期文档

- `docs/03-技术方案/权限模型与鉴权链路.md`（项目角色键）
- `docs/04-UI规范/组件与交互约定.md`（缺陷视图 / 看板，合入后回写）
- `docs/05-接口文档/`（缺陷流转 `POST /system/defect/{id}/repair`，引用既有约定）
- `docs/07-技术决策/说明.md`：本次不新增 ADR
