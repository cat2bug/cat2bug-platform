## 1. 取图工具

- [x] 1.1 新增分享取图工具（如 `resolveShareCardImageUrls`）：按项目已启用 `image` 字段的 `sortOrder` 排序，解析 `imgUrls` / `customFields`，过滤脏路径，探测可加载 URL，返回第一个有成功图字段的 URL 列表（无则空数组）
- [x] 1.2 字段列表用 `listEnabledFields`（或等价 API），跳过未启用字段；同字段内 URL 可并行探测，字段间串行

## 2. 分享图卡接入

- [x] 2.1 `DefaultDefectShareCard` 用工具结果替代仅读 `params.imgUrls`；无图时隐藏 `.img-list`；有图时走现有 canvas 绘制
- [x] 2.2 在 `refresh` / `drawImage` 路径 await 探测，结束后触发 ShareCard `schedulePopperUpdate`（若需）

## 3. 验证与回写

- [x] 3.1 手工：系统图 404 + 自定义有图 → 显示自定义；系统图靠前且可加载 → 显示系统图；全无图 → 无黑块
- [x] 3.2 回写长期知识：若 `docs/01` 分享描述需补一句取图规则则更新；无新 ADR / 无新踩坑则注明无
  - 已更新 `docs/01-产品需求/术语表.md` 缺陷分享一行；无新 ADR / 无新踩坑
