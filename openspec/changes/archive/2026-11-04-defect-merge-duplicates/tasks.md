## 1. 权限与后端合并 API

- [x] 1.1 Flyway（建议 `V1_2_1_4`，若占用则顺延）：插入 `system:defect:merge` 菜单（父 2019），并按与 `system:defect:remove` 相同策略复制角色关联
- [x] 1.2 `DefectLogState`（及前端/i18n 日志文案若需要）增加 `MERGE`
- [x] 1.3 `SysDefectService` 实现 `mergeDefects`：校验项目/未删除/主 id∈集合/≥2；事务内完整更新主缺陷、软删从缺陷、写 MERGE 日志；缺字段不得清空 customFields/状态
- [x] 1.4 `DefectResource` 暴露 `POST /system/defect/merge`，权限 `system:defect:merge`；补充后端单测（成功、无权限、主 id 非法、源已删）

## 2. 前端批量入口

- [x] 2.1 `api/system/defect.js` 增加 `mergeDefects`
- [x] 2.2 `defect-action-visibility.js` / `index.vue` 批量菜单增加「合并」：≥2、有 `system:defect:merge`、未删除；不受混合状态流转限制
- [x] 2.3 i18n 文案：合并、主缺陷、用此属性、覆盖确认、未定主提示等

## 3. 合并抽屉 UI

- [x] 3.1 新建 `MergeDefectDrawer`（宽抽屉）：左 `DefectFormOrderedFields`，右勾选列表；头部取消 / 合并（未定主禁用）
- [x] 3.2 「主缺陷」整单写入左侧 + 覆盖确认；切换主 id
- [x] 3.3 左字段焦点 → 右同字段高亮 +「用此属性」单字段替换（含描述/附件/处理人整字段）
- [x] 3.4 提交调用 merge API；成功后关闭抽屉、刷新列表、清勾选；失败提示

## 4. 回归与文档回写

- [x] 4.1 手工或前端单测：混合状态可开合并；仅 1 条不可；覆盖确认取消不改左栏
- [x] 4.2 回写 `docs/05-接口文档/接口清单-缺陷与用例计划.md` 增加 merge 行；术语表如需补「缺陷合并」一行；无新 ADR / 无新踩坑则注明跳过
