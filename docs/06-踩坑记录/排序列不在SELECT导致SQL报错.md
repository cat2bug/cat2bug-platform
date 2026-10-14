# 排序列不在 SELECT 导致 SQL 报错

记录 Quarkus 列表接入前端排序参数后，部分列排序 500 的原因。

- 日期：2026-10-14
- 涉及：Quarkus 后端 / 数据库

## 现象

Quarkus 补齐 `orderByColumn` / `isAsc` 排序后，前端按「创建人」「更新人」等列排序时接口 500（如缺陷 `createMember`、文档 `updateByName`）。此前 Quarkus 完全忽略排序参数，缺陷页默认按更新时间倒序也失效。

## 根因

前端传的是 VO 字段名，驼峰转下划线后得到 `create_member`、`update_by_name`，这些是展示用的派生字段，SQL 里并没有这一列（或只在 SELECT 别名里、ORDER BY 处不可用），直接拼进 `order by` 报错。

## 解决办法

提交 `59f6a23e`：

- 新增 `PageOrder` / `PageOrderFilter`（`cat2bug-platform-quarkus-framework/.../web/`）：驼峰转下划线、忽略 `custom_` 列、沿用 `SqlUtil.escapeOrderBySql` 的白名单与长度校验，非法参数返回「参数不符合规范，不能进行查询」。
- Mapper 里用 `<bind name="_pageOrder" value="@com.cat2bug.quarkus.framework.web.PageOrder@current('别名=实际列')"/>` 做映射，如 `SysDefectMapper.xml` 的 `create_member=d.create_by_id`、`SysDocumentMapper.xml` 的 `update_by_name=u.nick_name`。
- `QuarkusListSortTest` 覆盖 10 个列表 × 全部可排序列 × 升/降序；Native 冒烟增加排序与注入拦截检查。

## 以后怎么避免

- 新增可排序列时，确认它是真实列；派生/联表展示字段必须在 `PageOrder.current(...)` 里写映射。
- 新列表接入排序后，把列名加进 `QuarkusListSortTest`。
