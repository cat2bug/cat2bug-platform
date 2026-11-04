## 1. 工具条与筛选 UI

- [x] 1.1 将 `defect-fields/index.vue` 顶部改为 `.list-page-toolbar`：左 `__filters`（来源下拉 + 属性名输入，均带前缀图标）、右 `__actions`（「新增属性」`primary plain` + `cat2bug-btn-outline`）
- [x] 1.2 增加 `queryParams.source`（`'' | 'system' | 'custom'`）与 `queryParams.keyword`；改值即过滤、重置 `pageNum = 1`；无「查询」按钮
- [x] 1.3 补齐 7 语 i18n：来源选项、属性名 placeholder、相关 aria/label 文案

## 2. 过滤与分页逻辑

- [x] 2.1 实现 `filteredFieldList`：来源精确映射 `isBuiltinField`；关键字 trim 后对 `fieldKey` 与显示名称大小写不敏感 `includes`；两条件 AND
- [x] 2.2 将 `paginatedFieldList` / `total`（`syncPaginationTotal`）改为基于 `filteredFieldList`；空结果空表且不额外请求
- [x] 2.3 确认过滤态下上下改序仍操作全量 `fieldList` 邻居并落库；测「只看自定义」时改序不写错相对位置

## 3. 走查与收尾

- [x] 3.1 手工走查：全部/系统/自定义、按键/按显示名/大小写、组合过滤、翻页后再改条件回第 1 页、暗色主题工具条对比度
- [x] 3.2 回写长期知识：无（无新 ADR / 无可复用踩坑；未改 docs/04）
