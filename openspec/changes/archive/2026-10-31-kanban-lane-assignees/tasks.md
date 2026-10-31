## 1. 数据与本机缓存

- [x] 1.1 实现泳道负责人解析：按 `project.develop` / `project.tester` 取成员，空则管理员∪创建人；再与 `query.handleBy` 求交
- [x] 1.2 实现本机读写 `defect.kanban.laneAssignees.<projectId>`（`processing` / `audit` id 数组）；无缓存时用默认写入勾选状态
- [x] 1.3 监听 `projectId` 与全局 `handleBy` 变化时重算候选与头像条可见集合（不自动清缓存）

## 2. 泳道头 UI

- [x] 2.1 处理中 / 待验证泳道头在搜索框后展示负责人头像条；已结束不展示
- [x] 2.2 点击头像条打开 `SelectProjectMember` 多选，保存后更新本机缓存与展示
- [x] 2.3 补充必要 i18n（负责人空态、无权限直提失败等）

## 3. 拖放直提 repair

- [x] 3.1 拖入待验证且动作为 `repair` 时渲染可 drop 的负责人叠加层（`data-user-id` 热区）
- [x] 3.2 命中头像：校验集合内每条均可 `repair` 后串行调用 `repair` API（`receiveBy` 为该人）；成功刷新列表；失败 toast
- [x] 3.3 未命中头像：走现有弹框路径；驳回 / 关闭 / 通过 / 重开不启用叠加瞄准
- [x] 3.4 多卡含不可 repair 成员时整组不直提、不弹修复框，提示后留在原泳道

## 4. 回归与文档

- [x] 4.1 手工核对：默认角色、全局执行人过滤、命中/未命中头像、驳回仍弹框、已结束无负责人
- [x] 4.2 回写 `docs/04-UI规范/组件与交互约定.md` 看板小节（负责人 + repair 头像直提）；`docs/08-版本记录` 当月一行；无新 ADR / 无新踩坑则注明
