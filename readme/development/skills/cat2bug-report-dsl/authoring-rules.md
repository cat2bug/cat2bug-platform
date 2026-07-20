# 编写规则（authoring-rules）

生成或审查 Cat2Bug 报告模版时 **必须** 遵守下列约束。权威字段与 token 形态见 [variables-reference.md](./variables-reference.md)、[tables-images-cards.md](./tables-images-cards.md) 与 `grammar.js`。

---

## 1. 禁止发明未支持的 token

- **只允许** 本 skill 已文档化的形态：
  - `${api.<ns>.<field>}`
  - `${api.plan.<field>[n]}`
  - `$table{api.defect.list[...]}` / `$table{api.case.list[...]}`
  - `$image[...]{...}` / `$img[...]{...}`
  - `$card{...}`
- **禁止** 编造未登记路径，例如：
  - `${api.unknown.xyz}`
  - `${api.project.nonexistentField}`
  - `${api.plan.madeUpMetric[1]}`
  - `$table{api.member.all}`（未在当前产品数据源中启用）
- 标准要求某章节但无对应 DSL 数据时：使用 **静态 Markdown 占位**，并标明人工填写，例如：

```markdown
**【待填写｜无 DSL 数据源】** 测试结论与遗留风险：________
```

不要用假 token「假装」可自动填充。

---

## 2. 命名空间：优先 `api.<ns>.<field>`

- 新模版一律写完整路径：`${api.project.projectName}`、`${api.plan.passCount[1]}`。
- 不要依赖无 `api.` 前缀的历史短路径（如编辑器 hover 兼容的 `project.projectName`）。
- 表 / 图 / 卡片的 dataSource 同样使用完整名：`api.defect.list`、`api.case.list`、`api.project.projectIcon`。

---

## 3. 计划下标 `<n>` 为 1-based

- 写法：`${api.plan.<field>[n]}`，其中 `n` 为正整数，从 **1** 开始。
- `n` 对应报告上下文中选中计划的序号（与计划数据的 `planNumber` 对齐），**不是** 0-based 数组下标。
- 示例：第一个（或报告所选）计划用 `[1]`：

```markdown
缺陷总数: ${api.plan.defectCount[1]}
用例通过数: ${api.plan.passCount[1]}
```

- **禁止**：`${api.plan.defectCount}`（缺下标）、`${api.plan.defectCount[0]}`（0 不是合法计划序号惯例）。
- 多计划场景：按业务需要使用 `[1]`、`[2]`…，并确保与报告选择的计划序号一致；不确定时默认 `[1]` 并在模版旁注释说明。

---

## 4. 表格列只使用真实字段

- 缺陷列优先从 `DEFECT_TABLE_DEFAULT_COLUMNS` 选取；用例列从 `CASE_TABLE_DEFAULT_COLUMNS` 选取。
- 自定义表头用 `field:label`，但 `field` 必须仍是真实属性名。
- 空列列表依赖运行时默认列；显式写出默认列更利于审阅与 diff。

---

## 5. 静态 Markdown 与 DSL 混排

- 章节标题、说明性文字、合规声明等用普通 Markdown。
- 仅在有数据绑定处插入 DSL token。
- 保持 UTF-8；token 内不要插入换行打断路径（`$table{...}` 应写在同一逻辑行；若需折行须符合产品 normalize 规则，默认避免）。

---

## 6. 自检清单（生成后）

1. 每个 `${...}` / `$table` / `$image`/`$img` / `$card` 都能在本 skill 找到对应说明。
2. 所有 `api.plan.*` 均带 `[n]`，且 `n ≥ 1`。
3. 路径带 `api.` 前缀。
4. 无数据源的标准条目已改为「待填写」静态占位，而非假 token。
5. 与 `phase0-sample.md` 同类写法一致时，优先复用其 token 形态。

---

## 7. 反例 / 正例

| 反例（禁止） | 正例 |
|--------------|------|
| `${api.report.coverage}` | `**【待填写】** 覆盖率：__%` |
| `${api.plan.defectCount}` | `${api.plan.defectCount[1]}` |
| `${project.projectName}` | `${api.project.projectName}` |
| `$table{api.bug.list}` | `$table{api.defect.list[...]}` |
| `$image{api.case.icon}` | `$image["style":"width:200px;height:200px;"]{api.project.projectIcon}` |
