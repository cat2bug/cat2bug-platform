## 1. OCR 与源材料（整册落盘，禁止一次灌入上下文）

- [x] 1.1 安装本机 OCR 工具链（如 tesseract + chi_sim/eng，及 PDF 转图或 ocrmypdf）
- [x] 1.2 对 GB/T 38634.1–.4 四本扫描 PDF 执行 OCR，文本写入 `readme/development/skills/_sources/ocr/`（可先按册大文件，再切片）
- [x] 1.3 按「册 → 章/条款 → 页码区间」切片；每册生成 `manifest.json`（标题、起止页、对应 ISO 条款指针）
- [x] 1.4 抽取 ISO/IEC/IEEE 29119-3:2021 并同样按章/信息项切片到 `_sources/iso-29119-3/`
- [x] 1.5 抽检 OCR：第 3 部分文档类型/信息项目录是否可读；失败页登记 TODO
- [x] 1.6 在 apply 指引中明确：读全 = 磁盘完整；单次模型上下文只加载 manifest + 当前任务相关切片

## 2. Skills 骨架与索引

- [x] 2.1 创建 `readme/development/skills/` 目录结构（`standards/`、`cat2bug-report-dsl/`、`report-templates/`、`_sources/`）
- [x] 2.2 编写 `SKILL-INDEX.md`（触发场景、加载顺序、国标裁剪原则、**分段读取协议**）

## 3. 国标 skills（Agent-Standards，可与 §4 并行；按册/章分治）

- [x] 3.1 编写 `standards/gb-t-38634-overview.md`（四部分地图 + 与 29119 对照 + 指向各册 manifest）
- [x] 3.2 编写文档专题 skill（只读 GB.3 / ISO.3 **相关切片**）：信息项、公共信息元素、full/tailored 符合性；正文用页码指针而非整章粘贴
- [x] 3.3 编写过程/概念/技术要点 skills（可摘要级；agent 按章分片阅读 OCR，禁止单 agent 通读四本）
- [x] 3.4 编写 `report-templates/taxonomy.md` 初稿：**按国标**标注独立文档 / 单文档多章节 / 可裁剪合并 / 骨架-only（禁止机械一类一文件）

## 4. Cat2Bug DSL skills（Agent-DSL，可与 §3 并行）

- [x] 4.1 从 `grammar.js`、Cat2BugMarkdown plugins、现有 fixtures 整理变量与表列清单
- [x] 4.2 编写 `cat2bug-report-dsl/SKILL.md` + `variables-reference.md`
- [x] 4.3 编写 `tables-images-cards.md` 与 `authoring-rules.md`（禁止发明未支持 token；计划下标 1-based）

## 5. 默认模版（汇合后）

- [x] 5.1 根据定稿 taxonomy 列出默认模版清单（独立 / 组合 / 附录 / 骨架）
- [x] 5.2 编写测试状态报告导向默认模版（合法 DSL）
- [x] 5.3 编写测试完成报告导向默认模版（章节跟从标准信息项，非随意拆文件）
- [x] 5.4 编写缺陷/事件清单相关默认可渲染内容（独立或附录，按 taxonomy）
- [x] 5.5 为 DSL 覆盖不足的国标文档类型补充骨架模版（静态标题 + 占位）
- [x] 5.6 用现有 DSL parse/单测或编辑器预览抽检默认模版 token 可解析

## 6. 收尾

- [x] 6.1 更新 `SKILL-INDEX.md` 链到全部 skills 与 defaults
- [x] 6.2 在 change 下补充简短 `TESTING.md`（OCR 抽检 + 模版预览检查清单）
- [x] 6.3 （可选）评估是否导入 `sys_report_template` 预置；默认本变更不做库 seed
