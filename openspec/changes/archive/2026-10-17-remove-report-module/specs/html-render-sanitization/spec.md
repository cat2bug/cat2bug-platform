## MODIFIED Requirements

### Requirement: 用户内容渲染为 HTML 前必须清洗
前端 SHALL 在把用户可编辑的内容（缺陷、用例、通知、评论、文档、云端用例提示词等）通过 `v-html` 或等价方式插入页面前，统一经过 HTML 清洗：移除 `script`、`style`、`iframe`、`object`、`embed` 元素，移除所有 `on*` 事件属性，移除 `javascript:`、`vbscript:` 及非图片 `data:` 链接。

#### Scenario: 缺陷描述含脚本
- **WHEN** 用户把缺陷描述保存为 `<img src=x onerror=alert(1)>` 并由其他成员打开缺陷
- **THEN** 页面显示图片占位或空白，不执行任何脚本

#### Scenario: 评论含 HTML 标签
- **WHEN** 用户发表评论 `<script>alert(1)</script>你好`
- **THEN** 评论区显示文字 `<script>alert(1)</script>你好`（作为文本），不执行脚本

#### Scenario: 链接协议
- **WHEN** Markdown 中包含 `[点我](javascript:alert(1))`
- **THEN** 渲染出的链接不含 `javascript:` 地址

### Requirement: 清洗保留报告与文档的排版
清洗 SHALL 保留 Markdown 常见元素（标题、列表、表格、代码、引用、图片、链接）以及 `style` 属性中的 `color`、`background-color`、`text-align`；外部链接在新窗口打开时 MUST 带 `rel="noopener noreferrer"`。该规则适用于仍然渲染富文本的文档等页面。

#### Scenario: 文档对齐与颜色
- **WHEN** 打开一份含居中对齐与彩色文字的项目文档
- **THEN** 对齐与颜色显示与清洗前一致
