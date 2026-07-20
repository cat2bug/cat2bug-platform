# 表格 · 图片 · 卡片（tables-images-cards）

文档动态块语法。实现对齐 `TablePlugin`、`ImagePlugin`、`CardPlugin`、`CaseCardPlugin` 与 `grammar.js`。

---

## 1. 动态表 `$table{...}`

### 语法

```text
$table{<dataSource>[<columns>]}
$table{<dataSource>}
$table{<dataSource>[]}
```

- `<dataSource>`：已注册的列表数据源路径（点分标识）
- `<columns>`：可选。逗号分隔的列规格；省略或空 `[]` 时使用默认列（parse 时补齐）

### 列规格

| 形式 | 含义 |
|------|------|
| `field` | 列字段名；表头优先用 i18n key=`field`，否则显示字段名 |
| `field:label` | 自定义表头；`label` 若是 i18n key 则翻译，否则原文 |

多列示例：

```text
$table{api.defect.list[projectNum,defectName:缺陷标题,defectLevel]}
```

`grammar.js` 解析逻辑（`parseTableColumns`）：按 `,` 拆分；每段若含 `:`，左侧为 `field`，右侧为 `label`。

### 正则（grammar.js，允许空列括号）

```js
/\$table\{((?:\w|\.)+)(?:\[([^\]]*)\])?\}/
```

### 已支持数据源

| dataSource | 用途 | 默认列常量 |
|------------|------|------------|
| `api.defect.list` | 缺陷清单表 | `DEFECT_TABLE_DEFAULT_COLUMNS` |
| `api.case.list` | 用例清单表 | `CASE_TABLE_DEFAULT_COLUMNS` |

### 缺陷表默认列 `DEFECT_TABLE_DEFAULT_COLUMNS`

```js
[
  'projectNum',
  'defectTypeName',
  'defectLevel',
  'defectName',
  'defectStateName',
  'moduleName',
  'moduleVersion',
  'defectDescribe',
  'createBy',
  'createTime',
  'updateBy',
  'updateTime',
  'imgList',
  'annexList'
]
```

工具栏完整 token（`DEFECT_TABLE_TOOLBAR_TOKEN`）：

```text
$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}
```

### 用例表默认列 `CASE_TABLE_DEFAULT_COLUMNS`

```js
[
  'caseNum',
  'caseName',
  'moduleName',
  'casePreconditions',
  'caseExpect',
  'caseStep',
  'createBy',
  'createTime',
  'updateBy',
  'updateTime'
]
```

示例：

```text
$table{api.case.list}
$table{api.case.list[caseNum,caseName,moduleName]}
$table{api.case.list[caseNum:编号,caseName:名称]}
```

### 编写注意

- 列 `field` 必须是行对象上真实存在的属性名；不要编造列名。
- 未在上表登记的 `$table{api.member.all}` 等历史/注释掉的数据源 **不要** 用于新模版。

---

## 2. 图片 `$image` / `$img`

### 语法

```text
$image{<path>}
$img{<path>}
$image[<attrJSON>]{<path>}
$img[<attrJSON>]{<path>}
```

- `$image` 与 `$img` 等价。
- 可选属性括号：内容按「伪 JSON 对象片段」解析，例如 `"style":"width:200px;height:200px;"`（外层再包 `[]`）。
- `<path>`：点分路径，须以插件注册的 `name` 前缀开头。

### 正则（grammar.js）

```js
/\$(?:img|image)((?:\[[^\]]+\])*)?\s*\{((?:\w|\.)+)\}/
```

### 已支持路径

| path | 说明 |
|------|------|
| `api.project.projectIcon` | 项目图标（支持预设索引或上传相对路径） |

推荐示例（与工具栏 / phase0 fixture 一致）：

```text
$image["style":"width:200px;height:200px;"]{api.project.projectIcon}
```

### 编写注意

- 属性 JSON 须合法；畸形属性会被忽略。
- 不要为未绑定图片字段编造 `$image{api.xxx.yyy}`。

---

## 3. 卡片 `$card{...}`

### 语法

```text
$card{<dataSource>}
```

正则（grammar.js）：

```js
/\$card\{((?:\w|\.)+)\}/
```

### 已支持数据源

| dataSource | 插件 | 渲染形态 |
|------------|------|----------|
| `api.case.list` | `CaseCardPlugin`（专用） | 每条用例一张详情卡片（编号、名称、模块、预期、前置、步骤表） |
| `api.defect.list` | `CardPlugin`（通用键值表） | 按行对象全部 key 生成表头/单元格（预览侧亦有注册） |

工具栏常用（用例卡片）：

```text
$card{api.case.list}
```

### 编写注意

- 用例详情展示优先 `$card{api.case.list}`；扁平列表用 `$table{api.case.list[...]}`。
- `$card` **没有** 列选择参数；需要自定义列请用 `$table`。

---

## 4. 对照速查

| 需求 | Token |
|------|--------|
| 缺陷列表（默认列） | `$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}` |
| 用例列表 | `$table{api.case.list}` 或带列规格 |
| 用例卡片 | `$card{api.case.list}` |
| 项目图标 | `$image["style":"width:200px;height:200px;"]{api.project.projectIcon}` |

更多变量见 [variables-reference.md](./variables-reference.md)；编写约束见 [authoring-rules.md](./authoring-rules.md)。
