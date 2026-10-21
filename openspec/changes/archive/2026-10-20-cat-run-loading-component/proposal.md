## Why

平台各处加载态目前依赖 Element Plus 默认转圈（`v-loading` / `ElLoading`），与 Cat2Bug「狸花猫」品牌形象脱节。设计预览已确认：用一只轮廓猫的奔跑雪碧动画 +「加载中...」文案，可作为统一加载组件替换现有加载样式，增强品牌辨识与等待体验一致性。

## What Changes

- 新增前端组件 `Cat2BugLoading`：单猫视口奔跑动画 + 文案（默认「加载中...」），支持尺寸与深浅色背景。
- 素材：v4 位图精选 **6 帧** 水平雪碧（原帧 0/4/5/7/8/9），CSS `background-position` + `steps(6)` 切帧；默认 **130ms/帧**。
- 效果：身后 CSS 尘土粒子（对齐预览 v4）；脚底 **单色椭圆阴影**，与帧同步缩放；深色主题用浅色阴影。
- 接入：统一替换产品内常见加载展示（`v-loading` 自定义 spinner、全屏 `ElLoading`、局部加载占位等可落地入口）；具体替换清单在 tasks 中枚举。
- 回写 `docs/04-UI规范/` 加载态约定一句，并记入当月 `docs/08-版本记录`。

## Capabilities

### New Capabilities

- `cat2bug-loading`: Cat2Bug 品牌加载组件（奔跑猫轮廓 + 文案），作为产品统一加载视觉与接入约定。

### Modified Capabilities

- （无）不改现有 API / 后端能力；仅前端展示层。

## Impact

- 前端：`cat2bug-platform-ui` 新增组件与静态雪碧资源；可能扩展 `v-loading` 自定义 SVG/slot 或封装指令/服务包装 `ElLoading`。
- 受益面：列表、详情、对话框、下载等待等所有展示「加载中」的入口（按 tasks 分批替换，避免一次改爆）。
- 无后端 / API / **BREAKING**；不推翻已有 ADR。
