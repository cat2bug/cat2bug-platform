## Context

缺陷页 `index.vue` 持有唯一列表查询状态 `queryParams`，子视图通过 `handleRefreshQuery()` 下发。统计组件经 `Cat2BugStatistic` 的 `parent` 调用 `search(params)`，实现为 `_setProperty` 递归合并，**不会删除**未再次传入的 `params` 键。

已落地的参与筛选：

- Mapper：`params.participationLogDate` + `params.participationUserId`
- 前端特例：`searchByParticipation` → `reset()` → 切「全部」Tab → 写入参与参数

产品决策（探索阶段已确认）：

1. 通用条件与扩展条件**可以叠加**，由 `stack` 控制；**我的参与必须 `stack: false`**
2. 当前仅「我的参与」有非共享扩展参数
3. 采用**方案 B**：统一 `searchQuery` 入口
4. 参与筛选**必须**切换到「全部」Tab
5. 列表**重置**按**当前 Tab**恢复基准，并清除扩展参数

## Goals / Non-Goals

**Goals:**

- 单一查询协调 API，避免扩展参数泄漏
- 我的参与行为与现网一致（不叠加、切全部、无 Message）
- 其它统计点击自动清除参与扩展条件
- Tab 切换与工具栏重置均清除扩展参数

**Non-Goals:**

- 不修改 `SysDefectMapper` 参与 SQL
- 不把日历视图 `calendarStartDate/EndDate` 纳入扩展注册表（本变更仅参与）
- 不改变 Tab 自定义 config 的持久化结构

## Decisions

### 1. 扩展参数注册表（前端常量）

```javascript
// 示例：utils/defect-query-extension.js
export const DEFECT_QUERY_EXTENSION_KEYS = {
  participation: ['participationLogDate', 'participationUserId']
}

export function clearExtensionParams(queryParams) {
  const params = queryParams.params
  if (!params) return
  Object.values(DEFECT_QUERY_EXTENSION_KEYS).flat().forEach(key => {
    if (Object.prototype.hasOwnProperty.call(params, key)) {
      Vue.delete(params, key) // 或 delete + $set 触发响应
    }
  })
}
```

后续新增扩展域时只扩表，不改各 Statistic 组件。

### 2. `searchQuery` 语义

```javascript
/**
 * @param {Object} options
 * @param {Object} [options.common] - 合并进 queryParams 的通用字段
 * @param {Object} [options.extension] - 写入 queryParams.params 的扩展字段
 * @param {boolean} [options.stack=true]
 */
searchQuery({ common, extension, stack = true } = {}) {}
```

| 条件 | 行为 |
|------|------|
| `stack === false` | `reset()` →（若含参与扩展）`activeDefectTabName = 全部` → `clearExtensionParams()` → 应用 `common` → 应用 `extension` → `handleQuery()` |
| `stack === true` 且无 `extension` | `clearExtensionParams()` → `_setProperty(queryParams, common)` → `handleQuery()` |
| `stack === true` 且有 `extension` | `clearExtensionParams()` → merge `common` → 写入 `extension` → `handleQuery()` |

**叠加含义：**

- `stack: true`：保留当前 Tab 与已选通用字段，仅 merge 本次 `common`；扩展键在写入前先清空再设新值（当前仅参与，等价替换）
- `stack: false`：整页查询上下文重建（我的参与专用）

### 3. 我的参与调用约定

```javascript
// MyParticipationHeatmap.vue（read 模式仍 return）
parent.searchQuery({
  stack: false,
  extension: {
    participationLogDate: date,
    participationUserId: currentUserId
  },
  common: {
    params: { defectStates: [], delFlag: '0' }
  }
})
```

不再直接调用 `searchByParticipation`（可保留为：

```javascript
searchByParticipation(date, userId) {
  this.searchQuery({ stack: false, extension: { ... }, common: { ... } })
}
```

）。

### 4. 其它统计块

```javascript
// 例：MyOpenTodoGauge
parent.searchQuery({
  common: {
    handleBy: [userId],
    params: { defectStates: OPEN_DEFECT_STATE_IDS }
  }
})
// 无 extension → 自动 clearExtension，参与筛选被移除
```

`search(params)` 可标记 `@deprecated` 并委托 `searchQuery({ common: params })`，减少迁移面。

### 5. `resetQueryByCurrentTab()`

复用 `selectDefectTabHandle` 的分支逻辑，保证与**点击 Tab** 一致：

```
resetQueryByCurrentTab()
  → clearExtensionParams()
  → selectDefectTabHandle({ name: activeDefectTabName })
     或内联：全部 → reset + delFlag 0；已删除 → reset + delFlag 2；自定义 → queryParams = tab.config
  → handleQuery()
```

工具栏「重置」按钮绑定此方法（若尚未绑定）。

Tab 切换时：`selectDefectTabHandle` 开头或各分支末尾增加 **`clearExtensionParams()`**，避免从「全部+参与」切到自定义 Tab 时 `participation*` 仍挂在 `params` 上。

### 6. Cat2BugStatistic 转发

```javascript
searchQuery(opts) {
  if (this.$parent.searchQuery) {
    this.$parent.searchQuery(opts)
  }
}
```

`searchByParticipation` 可继续存在并转发，避免模版/外部调用断裂。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 自定义 Tab config 曾保存 `participation*` | Tab 切换/重置时 `clearExtension`；保存 Tab 前可考虑剥离扩展键（非本变更必须） |
| `_setProperty` 与 `reset` 顺序错误 | 单元测试或 `TESTING.md` 列 3 条手工路径 |
| 遗漏某处仍调 `search()` | 迁移清单 + grep `parent.search` |

## Migration Plan

1. 新增 `defect-query-extension.js`（或放在 `views/system/defect/query-extension.js`）
2. 实现 `searchQuery` / `clearExtension` / `resetQueryByCurrentTab`
3. Tab 处理补 `clearExtension`
4. 迁移 `MyParticipationHeatmap` 与 `Cat2BugStatistic`
5. 其余 Statistic 改为 `searchQuery({ common })` 或保留 `search` 薄封装
6. 手工验收：参与 → 待办 / 重置 / 切 Tab

无数据库迁移；可单独前端发布。

## Open Questions

- 无（参与切全部、重置按 Tab 已确认）
