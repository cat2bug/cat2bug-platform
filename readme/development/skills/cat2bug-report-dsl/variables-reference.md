# 变量参考（variables-reference）

本文档列出报告模版中 **简单变量** 与 **计划下标变量** 的合法路径。字段目录来自 `grammar.js` 的 `PROJECT_VARIABLE_FIELDS`、`PLAN_VARIABLE_FIELDS`，以及插件实际注册的命名空间。

**优先写法**：始终使用带 `api.` 前缀的路径，例如 `${api.project.projectName}`，不要写裸 `project.projectName`（编辑器历史兼容路径存在，但新模版应统一 `api.<ns>.<field>`）。

---

## 1. 简单变量 `${api.<ns>.<field>}`

### 语法

```text
${api.<namespace>.<field>}
```

可选历史别名（解析器也接受，但 **新模版禁止使用**）：

```text
${t{...}}   /   ${text{...}}   （VariablePlugin 可选前缀 t|text）
```

推荐仅使用：

```text
${api.project.projectName}
${api.case.total}
```

### 正则（grammar.js）

```js
/\$\{((?:\w|\.)+)\}/
```

### 已注册命名空间

| 命名空间前缀 | 插件 | 说明 |
|--------------|------|------|
| `api.project` | `VariablePlugin` + `ImagePlugin` | 当前项目对象字段 |
| `api.case.total` | `VariablePlugin` | 用例总数（标量，路径即完整 name） |
| `api.plan` | `PlanVariablePlugin` | **必须**带 `[n]` 下标，见下文 |

---

## 2. 项目字段 `PROJECT_VARIABLE_FIELDS`

命名空间：`api.project`

| field | 推荐 token | 说明（编辑器 hover 标签） |
|-------|------------|---------------------------|
| `projectName` | `${api.project.projectName}` | 项目名称 |
| `projectIcon` | 见图片 token（`$image`/`$img`），一般不作为文本变量 | 项目图标 |
| `createTime` | `${api.project.createTime}` | 创建时间 |
| `updateTime` | `${api.project.updateTime}` | 更新时间 |
| `projectIntroduce` | `${api.project.projectIntroduce}` | 项目简介 / 描述 |

源码常量：

```js
export const PROJECT_VARIABLE_FIELDS = [
  'projectName',
  'projectIcon',
  'createTime',
  'updateTime',
  'projectIntroduce'
]
```

---

## 3. 用例标量

| 路径 | token | 说明 |
|------|-------|------|
| `api.case.total` | `${api.case.total}` | 用例总数 |

> 用例 **列表** 不走简单变量，而使用 `$table{api.case.list[...]}` 或 `$card{api.case.list}`，见 [tables-images-cards.md](./tables-images-cards.md)。

---

## 4. 计划下标变量 `${api.plan.<field>[n]}`

### 语法

```text
${api.plan.<field>[<n>]}
```

- `<n>`：**1-based** 整数，表示报告上下文中选中的测试计划序号（与计划对象的 `planNumber` 匹配）。
- 下标写在路径 **末尾** 的方括号内，例如 `${api.plan.defectCount[1]}`。
- **禁止**省略下标写 `${api.plan.defectCount}`（简单变量插件不会按计划列表解析）。

### 正则（grammar.js）

```js
/\$\{((?:\w|\.)+)\[(\d+)\]\}/
```

### 字段目录 `PLAN_VARIABLE_FIELDS`

命名空间：`api.plan`

| field | 推荐 token 示例 | 含义（i18n / 工具栏） |
|-------|-----------------|----------------------|
| `planName` | `${api.plan.planName[1]}` | 计划名称 |
| `planVersion` | `${api.plan.planVersion[1]}` | 测试版本号 |
| `passCount` | `${api.plan.passCount[1]}` | 用例通过数 |
| `failCount` | `${api.plan.failCount[1]}` | 用例未通过数 |
| `unexecutedCount` | `${api.plan.unexecutedCount[1]}` | 用例未执行数 |
| `executedCount` | `${api.plan.executedCount[1]}` | 已执行用例数 |
| `itemTotal` | `${api.plan.itemTotal[1]}` | 计划项总数 |
| `defectCount` | `${api.plan.defectCount[1]}` | 缺陷总数 |
| `createDefectCountByTester` | `${api.plan.createDefectCountByTester[1]}` | 测试人员创建缺陷数 |
| `createDefectCountByOutsider` | `${api.plan.createDefectCountByOutsider[1]}` | 非测试人员创建缺陷数 |
| `defectProcessingStateCount` | `${api.plan.defectProcessingStateCount[1]}` | 处理中缺陷数 |
| `defectAuditStateCount` | `${api.plan.defectAuditStateCount[1]}` | 审核中缺陷数 |
| `defectRejectedStateCount` | `${api.plan.defectRejectedStateCount[1]}` | 已驳回缺陷数 |
| `defectCloseStateCount` | `${api.plan.defectCloseStateCount[1]}` | 已关闭缺陷数 |
| `defectHistoryPassCount` | `${api.plan.defectHistoryPassCount[1]}` | 历史通过缺陷数 |
| `defectLevelUrgentCount` | `${api.plan.defectLevelUrgentCount[1]}` | 紧急缺陷数 |
| `defectLevelHeightCount` | `${api.plan.defectLevelHeightCount[1]}` | 高优先级缺陷数 |
| `defectLevelMiddleCount` | `${api.plan.defectLevelMiddleCount[1]}` | 中优先级缺陷数 |
| `defectLevelLowCount` | `${api.plan.defectLevelLowCount[1]}` | 低优先级缺陷数 |
| `defectRestartCount` | `${api.plan.defectRestartCount[1]}` | 重启缺陷数 |
| `defectUseHourTime` | `${api.plan.defectUseHourTime[1]}` | 缺陷耗时（小时） |
| `defectDiscoveryRate` | `${api.plan.defectDiscoveryRate[1]}` | 缺陷发现率 |
| `defectRepairRate` | `${api.plan.defectRepairRate[1]}` | 缺陷修复率 |
| `defectDensity` | `${api.plan.defectDensity[1]}` | 缺陷密度 |
| `defectDetectionRate` | `${api.plan.defectDetectionRate[1]}` | 缺陷检出率 |
| `defectSeverityRate` | `${api.plan.defectSeverityRate[1]}` | 缺陷严重率 |
| `defectRestartRate` | `${api.plan.defectRestartRate[1]}` | 缺陷重启率 |
| `defectEscapeRate` | `${api.plan.defectEscapeRate[1]}` | 缺陷逃逸率 |
| `defectRepairAvgHour` | `${api.plan.defectRepairAvgHour[1]}` | 平均修复时长（小时） |
| `moduleCount` | `${api.plan.moduleCount[1]}` | 模块数 |

源码常量：

```js
export const PLAN_VARIABLE_FIELDS = [
  'planName',
  'planVersion',
  'passCount',
  'failCount',
  'unexecutedCount',
  'executedCount',
  'itemTotal',
  'defectCount',
  'createDefectCountByTester',
  'createDefectCountByOutsider',
  'defectProcessingStateCount',
  'defectAuditStateCount',
  'defectRejectedStateCount',
  'defectCloseStateCount',
  'defectHistoryPassCount',
  'defectLevelUrgentCount',
  'defectLevelHeightCount',
  'defectLevelMiddleCount',
  'defectLevelLowCount',
  'defectRestartCount',
  'defectUseHourTime',
  'defectDiscoveryRate',
  'defectRepairRate',
  'defectDensity',
  'defectDetectionRate',
  'defectSeverityRate',
  'defectRestartRate',
  'defectEscapeRate',
  'defectRepairAvgHour',
  'moduleCount'
]
```

---

## 5. 不在目录中的路径

若标准要求某指标但上表 **没有** 对应 field：

1. **不要**发明 `${api.unknown.xyz}` 或未列出的 `api.plan.*`
2. 使用静态 Markdown 占位，明确标为人工填写，例如：

```markdown
> 【待填写】测试环境配置：________
```

详见 [authoring-rules.md](./authoring-rules.md)。
