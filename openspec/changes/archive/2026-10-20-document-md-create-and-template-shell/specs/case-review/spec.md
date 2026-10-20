## ADDED Requirements

### Requirement: 评审纪要模板编辑壳与文档 Markdown 页一致

项目评审纪要模板编辑页 MUST 使用与文档 Markdown 编辑页相同的 `Cat2BugEditor` 封装，并启用纸面模式（`paper`）与扁平壳（`flat`）：内容区内层 MUST NOT 再套额外边框，内容区与外层背景色一致，MUST NOT 在编辑器上下再加与文档页不一致的间隔条带。有编辑权限时，保存控件 MUST 出现在编辑器工具条内；页面 MUST NOT 在编辑器下方再放独立保存按钮行。保存 API 与权限（`system:project:case-type:edit`）保持不变。

#### Scenario: 工具条保存且无底部保存行

- **WHEN** 有权限的用户打开项目评审纪要模板页
- **THEN** 编辑器为 paper + flat，保存入口在工具条，编辑器下方没有独立保存按钮行

#### Scenario: 无权限只读

- **WHEN** 无模板编辑权限的用户打开该页
- **THEN** 编辑器只读，工具条不提供保存
