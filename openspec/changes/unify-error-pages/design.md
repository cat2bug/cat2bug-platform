## Context

预览已定稿（`openspec/changes/unify-error-pages/preview/`）：404 路标猫、401 挂锁猫、禁用矮门猫；浅 `#f5f6f8` / 深 `#1e1e20` 各一套。现网四处错误页共用旧 1200px 横排 + 云朵动画。

## Goals / Non-Goals

**Goals:**

- 四处错误页同一竖排壳与品牌插画。
- 浅/深色插画随主题切换，不依赖 CSS 滤镜压暗。
- 保留 team-lock / project-lock 的切换团队、选项目逻辑与 lock remark。

**Non-Goals:**

- 不改权限拦截、路由守卫、后端禁用判定。
- 不改列表空态 `Cat2BugEmpty`。
- 不做插画再生成；以预览定稿 PNG 入库为准。

## Decisions

1. **独立 `ErrorStateShell`，不用 `Cat2BugEmpty`**  
   错误页是整页状态，需要大状态码与多按钮；空态是列表内嵌且带浮箱动效。分开避免互相污染。

2. **插画按 variant + themeMode 选图**  
   资源目录 `src/assets/images/error/{404,401,disabled}-{light,dark}.png`。组件读 `useSettingsStore().themeMode`（与 Navbar 一致）。

3. **禁用页共用 `disabled` 插画**  
   team-lock / project-lock 同图，文案与操作不同。

4. **布局**  
   404 / 401 / 禁用页均挂在 `Layout` 下，保留侧栏与顶栏；`ErrorStateShell` 只铺满主内容区居中（最大内容宽约 280–320px）；状态码主色 `--el-color-primary`；无云朵、无绝对定位大横排、不另开全屏孤页。

## Risks / Trade-offs

- [暗色插画体积] → 六张 PNG 可控；不做 sprite。  
- [旧图残留] → 错误页停用引用；资源文件可随后删，避免误伤其他引用。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/04-UI规范/主题与配色.md`（若存在）
