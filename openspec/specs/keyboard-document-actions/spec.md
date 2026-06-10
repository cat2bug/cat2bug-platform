## ADDED Requirements

### Requirement: 文档列表动作面板

项目文档页 SHALL `registerPage('document', …)` + `page-action-hints`。

### Requirement: 文档列表默认字母

| 键 | 动作 |
|----|------|
| O | 新建文件夹 |
| I | 新建文件 |

#### Scenario: O 新建文件夹

- **WHEN** 用户有 `system:document:add` 且按 O
- **THEN** 打开新建文件夹流程

#### Scenario: I 新建文件

- **WHEN** 用户有 `system:document:add` 且按 I
- **THEN** 打开新建文件流程

### Requirement: 文档弹框

新建/重命名、移动目录 `el-dialog` SHALL 复用工具弹框套系。

### Requirement: 文档编辑页（c2d）

`/document/c2d` 编辑页 SHOULD 在后续迭代接入简化动作（Esc 返回）；本变更 MUST 至少保证打开 c2d 时列表页动作已注销。

#### Scenario: 进入 c2d 注销列表动作

- **WHEN** 用户打开文档编辑器路由
- **THEN** 文档列表 `unregisterPage('document')`
