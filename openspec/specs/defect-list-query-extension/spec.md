## ADDED Requirements

### Requirement: 缺陷列表扩展查询参数注册

系统 SHALL 在前端维护缺陷列表「扩展查询参数」注册表，用于标识不得与通用 `search` 合并逻辑隐式保留的 `queryParams.params` 键。

当前注册的扩展域 MUST 仅包含：

- `participation`：`participationLogDate`、`participationUserId`

`clearExtensionParams(queryParams)` MUST 从 `queryParams.params` 删除上述全部键（若存在）。

#### Scenario: 清除参与扩展键

- **WHEN** 调用 `clearExtensionParams` 且 `params` 含 `participationLogDate` 与 `participationUserId`
- **THEN** 两键均不再出现在 `queryParams.params` 中

### Requirement: 统一列表查询入口 searchQuery

缺陷页 `index.vue` MUST 提供 `searchQuery({ common, extension, stack })` 作为列表筛选的统一入口。

- `common`：可选对象，字段语义与既有 `search(params)` 相同，通过深度合并写入 `queryParams`
- `extension`：可选对象，键 MUST 为注册表中的扩展键，写入 `queryParams.params`
- `stack`：布尔，默认 `true`

当 `stack` 为 `false` 时，MUST 在应用本次条件前执行 `reset()`，且 MUST 在写入扩展参数前调用 `clearExtensionParams()`。

当 `stack` 为 `true` 且未提供 `extension` 时，MUST 在合并 `common` 前调用 `clearExtensionParams()`。

当 `stack` 为 `true` 且提供 `extension` 时，MUST 先 `clearExtensionParams()`，再合并 `common`，再写入 `extension`。

每次成功应用条件后 MUST 调用既有 `handleQuery()` 刷新列表。

#### Scenario: 通用统计点击清除参与条件

- **WHEN** 当前列表带有参与日扩展参数，用户点击另一统计块并调用 `searchQuery({ common: { handleBy: [uid] } })`（默认 `stack: true`）
- **THEN** 请求中不再包含 `participationLogDate` 与 `participationUserId`
- **THEN** 列表按本次 `common` 条件筛选

#### Scenario: stack false 重建查询上下文

- **WHEN** 调用 `searchQuery({ stack: false, common: {...}, extension: {...} })`
- **THEN** `queryParams` 经 `reset()` 重建后再应用 `common` 与 `extension`
- **THEN** 不包含调用前残留的通用字段（除非 `common` 显式传入）

### Requirement: 按当前 Tab 重置列表查询

缺陷页 MUST 提供 `resetQueryByCurrentTab()`，行为与用户对「重置」的预期一致：恢复**当前激活 Tab** 的基准查询条件，而非任意全局空白。

`resetQueryByCurrentTab()` MUST：

1. 调用 `clearExtensionParams()`
2. 按当前 Tab 应用与 `selectDefectTabHandle` 相同的基准逻辑（「全部」/`已删除`/自定义 Tab config）
3. 调用 `handleQuery()`

#### Scenario: 全部 Tab 上重置

- **WHEN** 用户在「全部」Tab 且列表曾带参与扩展参数，点击重置
- **THEN** 扩展参数被清除
- **THEN** 查询条件等同于「全部」Tab 默认（含 `params.delFlag = '0'`）

#### Scenario: 自定义 Tab 上重置

- **WHEN** 用户在某一自定义 Tab 上点击重置
- **THEN** 扩展参数被清除
- **THEN** `queryParams` 恢复为该 Tab 保存的 `config` 基准

### Requirement: Tab 切换清除扩展参数

切换缺陷页 Tab（`selectDefectTabHandle`）时，在触发列表查询前 MUST 调用 `clearExtensionParams()`，避免扩展参数泄漏到其它 Tab 的查询上下文。

#### Scenario: 从参与筛选切到自定义 Tab

- **WHEN** 列表当前带参与扩展参数，用户切换到自定义 Tab
- **THEN** 随后列表查询的请求中不包含参与扩展键

### Requirement: 统计条转发 searchQuery

`Cat2BugStatistic` MUST 向缺陷页转发 `searchQuery`，供子统计组件调用。

统计模版 `read` 模式下，子组件 MUST NOT 调用 `searchQuery`。
