## ADDED Requirements

### Requirement: Share card picks first image field with loadable images by attribute sort order

缺陷分享图卡（Default 模版）MUST 按项目缺陷属性管理中系统与自定义字段共用的 `sortOrder`（升序），在已启用且 `fieldType` 为 `image` 的字段中，选择**第一个**至少含一张可成功加载图片的字段，并仅渲染该字段下加载成功的图片 URL。系统内置 `imgUrls` MUST 作为普通 `image` 字段参与同一排序竞争，MUST NOT 再作为唯一硬编码数据源。未启用的图片字段 MUST NOT 参与。

#### Scenario: Custom image field ranks before broken system image

- **WHEN** 系统 `imgUrls` 的 `sortOrder` 大于某自定义 `image` 字段，或系统图 URL 全部加载失败，且该自定义字段存在至少一张可加载图
- **THEN** 分享图卡展示该自定义字段的成功图片，不展示失败的系统图黑块

#### Scenario: System imgUrls wins when first and loadable

- **WHEN** 按 `sortOrder` 排在最前的已启用 `image` 字段是 `imgUrls`，且其中至少一张 URL 可加载
- **THEN** 分享图卡仅使用 `imgUrls` 中加载成功的图片

#### Scenario: 404 counts as no image for that field

- **WHEN** 某 `image` 字段解析出的全部候选 URL 加载失败（含 HTTP 404）
- **THEN** 系统 MUST 将该字段视为无图，并继续检查下一 `sortOrder` 的 `image` 字段

#### Scenario: No loadable image in any field

- **WHEN** 所有已启用 `image` 字段均无有效可加载图
- **THEN** 分享图卡 MUST NOT 展示空白图片占位区域（无黑块）

### Requirement: Dirty and non-path values are ignored when resolving share images

解析图片字段值时，系统 MUST 忽略非上传路径脏数据（例如 `[object Event]`），MUST 仅保留可识别的上传/HTTP 图片路径后再做加载探测。

#### Scenario: Custom field mixes dirty token and real path

- **WHEN** 自定义 `image` 字段值为含 `[object Event]` 与真实 `/profile/upload/...` 路径的列表
- **THEN** 探测与渲染 MUST 只针对真实路径；若该真实路径可加载，该字段可作为「第一个有图字段」
