## Requirements

### Requirement: Lane owner chips on processing and audit

处理中与待验证泳道头在列内搜索框之后 MUST 展示该泳道负责人头像条；已结束泳道 MUST NOT 展示负责人。用户 MUST 能点击头像条打开成员多选以增减负责人。无本机缓存时，处理中 MUST 默认勾选项目「开发」角色成员，待验证 MUST 默认勾选「测试」角色成员；对应角色无成员时 MUST 回退为项目管理员与项目创建人（并集去重）。负责人勾选 MUST 仅持久化在本机（按项目与泳道区分），MUST NOT 写入服务端项目配置。

#### Scenario: First visit defaults by role

- **WHEN** 用户首次在某项目打开看板且本机无该项目泳道负责人缓存
- **THEN** 处理中勾选开发角色成员（若无则管理员/创建人），待验证勾选测试角色成员（若无则管理员/创建人），已结束无负责人条

#### Scenario: Persist locally after edit

- **WHEN** 用户在待验证泳道多选中增减负责人并关闭选择器
- **THEN** 本机保存该项目待验证负责人 id 列表，刷新页面后仍恢复（在候选过滤规则下可见）

### Requirement: Owner candidates filtered by global handleBy

泳道负责人多选的可选成员 MUST 为项目成员与当前缺陷列表全局「执行人」筛选（`queryParams.handleBy`）的交集；当全局未选择任何执行人时，可选集合 MUST 为全部项目成员（再按实现需要分页/检索）。已缓存在勾选中但不在当前候选集合内的成员 MUST NOT 出现在头像条展示中，但 MUST NOT 因此自动清除本机缓存项。

#### Scenario: Global executors narrow the picker

- **WHEN** 全局执行人已选用户 A、B，且 A 属于测试角色
- **THEN** 待验证负责人候选仅包含 A、B 与项目成员的交集中的人，头像条只展示当前勾选且仍在该交集内的成员

#### Scenario: No global executor shows all members

- **WHEN** 全局执行人为空
- **THEN** 负责人多选可选项目内全部成员（默认勾选仍按角色/兜底规则）

### Requirement: Drop on audit owner avatar submits repair directly

当用户将一张或多张卡片从其他泳道拖入待验证，且落点映射动作为 `repair`，系统 MUST 在待验证泳道展示可放置的负责人头像叠加层。若松手时指针命中某一负责人头像，且拖拽集合中每条缺陷对当前用户均允许 `repair`，系统 MUST NOT 打开修复弹框，MUST 以该成员为 `receiveBy` 对每条缺陷调用既有 `repair` 接口（描述可空），成功后刷新看板数据。若未命中任何负责人头像，系统 MUST 保持现有行为：打开单条或批量修复弹框。

#### Scenario: Hit avatar repairs without dialog

- **WHEN** 用户将处理中卡片拖到待验证某测试负责人头像上松手，且具备对该缺陷的修复权限
- **THEN** 系统直接提交 repair（审核人为该成员），不打开 RepairDialog，成功后卡片出现在待验证

#### Scenario: Miss avatar opens dialog

- **WHEN** 用户将处理中卡片拖入待验证泳道空白或卡片区域（未命中负责人头像）松手
- **THEN** 系统打开既有修复弹框，取消则不调用接口且卡片留在原泳道

#### Scenario: Multi-card hit requires all repairable

- **WHEN** 用户多选多张卡拖到同一负责人头像，且其中至少一张当前用户不可 repair
- **THEN** 系统 MUST NOT 对任一卡直接提交，MUST 提示并保持卡片在原泳道（不打开修复弹框）

### Requirement: Reject and non-repair drops ignore owner aiming

待验证拖入处理中（`reject`）以及所有映射为 `close` / `pass` / `open` 的拖放 MUST NOT 使用负责人头像瞄准或直提；处理中泳道负责人条仅用于展示与本机配置。此类落点 MUST 继续走既有弹框与必填规则（含驳回描述必填、处理人由后端决定）。

#### Scenario: Reject always uses dialog

- **WHEN** 用户将待验证卡片拖入处理中（无论是否经过开发头像区域）松手
- **THEN** 系统打开驳回弹框，不因头像改变处理人或跳过弹框

#### Scenario: Closed lane has no owner overlay

- **WHEN** 用户将卡片拖向已结束泳道
- **THEN** 已结束不展示负责人头像叠加层，行为与看板一期关闭/通过弹框规则一致
