## Why

缺陷列表等场景中，无照片用户当前以完整用户名显示在灰色圆形头像内，信息密度高、辨识度弱，且与常见协作产品（饱和色底 + 单字/首字母 + 白字）不一致。需要在统一头像组件内改为首字展示，并用稳定、可复现的彩色背景提升扫读效率。

## What Changes

- 无头像时，`Cat2BugAvatar` 仅显示 **一个字符**：中文为显示名首字（如「张」），拉丁字母为大写首字母（如 `D`）。
- 文字头像背景采用 **固定色板 + 白字**（主流方案），颜色由显示名首字符的码点哈希取模得到；**不**引入拼音库。
- 有 `avatar` / `avatarUrl` 时仍显示图片，逻辑不变。
- 统计占位（`isStatistics`）保持现有灰色样式，不参与彩色色板。
- 新增 `utils/member-avatar.js` 集中首字解析与配色；`RowListMember` 等调用方无需改接口。
- 移除或替换此前误加的淡色 HSL 实现（若工作区存在未提交 spike），以本变更为准。

## Capabilities

### New Capabilities

- `member-text-avatar`: 成员文字头像的展示字符规则、着色算法、色板约束及 `Cat2BugAvatar` 集成范围。

### Modified Capabilities

（无：项目 `openspec/specs/` 中尚无既有能力需增量修改。）

## Impact

- **前端**：`cat2bug-platform-ui/src/components/Cat2BugAvatar/index.vue`（主改动）、新建 `utils/member-avatar.js`。
- **影响面**：所有使用 `Cat2BugAvatar` 的界面（缺陷/用例列表处理人与创建人、顶栏、评论、统计卡片等）视觉一致变化。
- **依赖**：无新增 npm 包；无后端/API 变更。
- **文档**：实现完成后可在 `readme/` 分支目录补充简短说明（可选）。
