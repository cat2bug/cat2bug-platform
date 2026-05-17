## Context

- 缺陷列表「处理人」「创建人」等通过 `RowListMember` → `Cat2BugAvatar` 渲染成员头像。
- 当前 `Cat2BugAvatar` 在无图片时将完整 `nickName || userName || name` 填入 `el-avatar`，背景为 Element UI 默认灰或 `isStatistics` 灰色。
- 产品已定：展示汉字首字；着色不依赖拼音，用首字符码点哈希；配色采用主流 **固定色板 + 白字**（非淡色 HSL）。

## Goals / Non-Goals

**Goals:**

- 统一在 `Cat2BugAvatar` 实现文字头像：单字符展示 + 稳定彩色背景 + 白色文字。
- 提供可复用的 `utils/member-avatar.js`（解析显示名、首字、colorKey、样式对象）。
- 全站凡使用该组件处行为一致；`isStatistics` 保持灰底灰字。

**Non-Goals:**

- 不引入拼音库或后端新字段。
- 不修改 `MemberNameplate` 等未使用 `Cat2BugAvatar` 的组件（可后续单独变更）。
- 不改变有图头像的加载与 URL 逻辑。

## Decisions

### 1. 改动落点：`Cat2BugAvatar` + 工具函数

**选择**：逻辑集中在 `Cat2BugAvatar` 与 `utils/member-avatar.js`。  
**理由**：所有列表/顶栏/评论已走同一组件，避免在 `table.vue` 等处重复实现。  
**备选**：仅缺陷表包装一层 — 拒绝，无法全站一致。

### 2. 展示字符规则

| 输入 | 展示 |
|------|------|
| 有 `avatar` 或 `avatarUrl` | 图片（slot 文本为空） |
| 中文等 | `displayName` 第一个 Unicode 字符（如「张」） |
| 拉丁字母开头 | 首字母大写（`dev` → `D`） |
| 空/仅空白 | `?` |

显示名解析顺序：`nickName → userName → name`（与现逻辑一致）。

### 3. 着色：固定色板 + 白字（主流）

**选择**：预置 10～12 个 hex 背景色，`index = hash(colorKey) % palette.length`，文字 `#FFFFFF`。  
**理由**：与 Ant Design Avatar、钉钉/飞书等常见模式一致；表格小尺寸下对比度优于淡色底。  
**备选**：连续 HSL 淡色 — 已明确不采用。  
**备选**：连续 HSL 中饱和 + 白字 — 色相较难控，不如色板统一。

**colorKey**：首字符；拉丁字母统一为小写再哈希，中文用原字符。

**建议色板**（实现时可微调，需满足白字对比度）：

```text
#1890ff  #52c41a  #faad14  #f5222d  #722ed1
#13c2c2  #eb2f96  #fa8c16  #2f54eb  #a0d911
#597ef7
```

避免纯 `#ffbf00` 类过亮黄底 + 白字对比不足；橙系用 `#fa8c16`。

### 4. 哈希函数

**选择**：简单字符串哈希（如 djb2 风格），对 `colorKey` 取绝对值后取模。  
**理由**：零依赖、确定性、跨端一致。  
**说明**：仅首字符作 key 时，同姓/同首字母用户会同色 — 产品已接受。

### 5. `isStatistics` 例外

**选择**：`member.isStatistics === true` 时不应用色板，沿用 scoped 样式 `#E4E7ED` / `#909399`。  
**理由**：统计占位非真实用户，不应进入彩色身份体系。

### 6. 与现有 spike 的关系

若工作区存在未提交的淡色 HSL 实现（`getAvatarPastelStyle`），实现本变更时 **删除淡色逻辑**，改为色板方案。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 全站头像风格突变 | 属预期；在 proposal 中已说明影响面 |
| 首字符哈希碰撞导致同色 | 色板 10+ 色；tooltip 仍显示完整 `nickName`（`RowListMember`） |
| 色板与深色主题不协调 | 当前产品为浅色后台；深色主题另开变更 |
| `el-avatar` 行高与小字号中文裁切 | 沿用现有 `small`/`medium` 尺寸样式，必要时微调 `line-height` |

## Migration Plan

1. 实现 `utils/member-avatar.js` 与 `Cat2BugAvatar` 更新。
2. 本地验证：缺陷列表处理人/创建人、顶栏、无图/有图/多处理人叠放。
3. 无数据迁移；无 API 变更；可单独前端发布。
4. 回滚：还原 `Cat2BugAvatar` 与工具文件即可。

## Open Questions

- 色板是否需与 Cat2Bug 品牌主色 `#409EFF` 强制占一位（设计建议保留，实现时确认）。
- 是否在首个版本将哈希输入从「首字符」扩展为「完整 displayName」（分布更匀）— **当前按首字符执行**，若产品变更再改 spec。
