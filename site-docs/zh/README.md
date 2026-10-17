# site-docs 中文文档源

本目录与 cat2site 门户文档的**标题路径**同构：相对路径去掉 `.md` 即为标题路径。

示例：`指南/介绍.md` → 标题路径 `指南/介绍`（门户 `page-by-title` 参数 `page=指南/介绍`）。

首次内容请用 `cat2bug-build/scripts/export_site_docs.py` 从已发布门户文档导出基线，再在此维护 Markdown。
