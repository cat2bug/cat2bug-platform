## Context

登录页位于 `cat2bug-platform-ui/src/views/login.vue`，默认子组件 `UserNameAndPasswordLogin.vue`，插画 `CatIllustration.vue`（Three.js GLB + CSS 月亮），底部 `MouseRunner.vue`（Canvas 2D）。

全局主题通过 `html.dark` 与 `theme-dark.scss` 变量控制；用户可在应用内切换 `themeMode`（`localStorage` + `applyThemeMode`）。

已有资产：`mouse_sprite_clean.png`（8×8 写实鼠，含坐姿行）、`cat-illustration-transparent.png`、`login-scene.glb`。mockup 要求底部为**灰白像素鼠**，与写实精灵不一致，需 B2 新图。

## 产品决策（已确认）

| 项 | 决策 |
|----|------|
| 左栏文案 | 7 语言 i18n，英文 mockup 为 en-US 参考 |
| 品牌名 | `Cat2` 白 + `Bug` 黄 + `-Platform` 灰 |
| 登录页主题 | **不强制** dark；token 在 light/dark 均可用 |
| 老鼠自动跑 | 出屏后 respawn，**随机**左/右 |
| 老鼠画风 | **B2** 新像素精灵 `mouse_sprite_pixel.png` |
| 老鼠控制 | **按住右键**移动；松开 → 坐直左右看 → 恢复自动跑 |
| 睡眠符号 | 小 `z` + 大 `Z`，CSS 向上飘淡出（方案 1） |

## Goals / Non-Goals

**Goals:**

- 暗色模式下登录页视觉与 `mockup_dark_v5.png` 左栏、猫插画区、底鼠行为一致（右栏表单允许部分差异，见 Non-Goals）
- 登录页专用 token 不污染全局 `--bg-color-base`（暗色 mockup 更黑）
- 老鼠交互完整状态机 + B2 资源
- z/Z 动画锚点对齐猫头（以 GLB 加载后为准调参）

**Non-Goals:**

- 登录页强制 `html.dark`
- 浅色主题像素级验收（仅保证 token 不崩）
- 右栏：Forgot password 路由、去掉 `system-name` 标题、Login 按钮箭头等（另开变更）
- 老鼠触屏长按替代右键（v2）
- Blender 3D 老鼠

## Decisions

### 1. 登录页 token 作用域

在 `.logo-page` 定义变量；`html.dark .logo-page` 与 `:root:not(.dark) .logo-page`（或 `.logo-page--light`）分别赋值。

```scss
html.dark .logo-page {
  --login-bg: #0d0d0d;
  --login-card-bg: #1a1a1a;
  --login-accent: #ffc107;
  --login-text-primary: #ffffff;
  --login-text-secondary: #9ca3af;
  --login-text-muted: #6b7280;
  --login-zzz-color: #e5e7eb;
}
```

表单卡片 `UserNameAndPasswordLogin` 将 `background` / `border` 改为 `var(--login-card-bg)` 等，而非写死 `#2b2b2d`。

### 2. 左栏 i18n 键

| 键 | 用途 |
|----|------|
| `login.hero.title` | Simplify Bugs… / 各语言 slogan |
| `login.hero.desc` | 一段描述 |
| `login.features.tracking.title/desc` | 智能缺陷跟踪 |
| `login.features.collaboration.title/desc` | 团队协作 |
| `login.features.reports.title/desc` | 洞察报告 |

图标：`peoples`、`chart`；跟踪用新增 `login-feature-target` 或复用 `guide`/`point`（实现时择最接近 mockup 者）。

### 3. B2 精灵表布局

文件：`src/assets/images/mouse_sprite_pixel.png`

- 单帧 **32×24px**，透明底
- 网格 **8 列 × 5 行**（总 256×120）
- row0：侧面跑 8 帧（西向 `flipX`）
- row1：远离（AWAY，Y 减小）
- row2：靠近（TOWARD，Y 增大）
- row3：停→坐→看左→看右→回正（SIT_LOOK 序列）

调色：主体 `#9CA3AF`，尾巴 `#D1A5A5`，1px 轮廓 `#6B7280`，无抗锯齿。

### 4. MouseRunner 状态机

```
AUTO_RUN ──RMB down──► USER_CONTROL ──RMB up──► SIT_LOOK_AROUND ──► AUTO_RUN
```

**AUTO_RUN:**

- `direction = random() < 0.5 ? -1 : 1`
- 从屏外 spawn：`x = direction > 0 ? -margin : width + margin`
- `groundY ≈ 0.82 * canvasHeight`；`scale` 近端大、可选 30% 从远端 lerp 入画
- 出屏后重新 random direction 并 spawn
- 尘土粒子保留，颜色随 `html.dark`

**USER_CONTROL:**

- `mousedown`/`mousemove`/`mouseup` 监听 `button === 2`；`contextmenu` preventDefault
- 容器 `pointer-events: auto`（仅底部 120–160px 条带）
- 光标目标 lerp；`depthScale = lerp(SCALE_FAR, SCALE_NEAR, mouseY/height)`
- 方向行：`|dy|>|dx|` 且 dy<0 → AWAY；dy>0 → TOWARD；否则 row0 + flipX

**SIT_LOOK_AROUND:**

- 播放 row3 帧序列 ~2.5–3s，不产尘
- 结束切 `AUTO_RUN`

**绘制：** `imageSmoothingEnabled = false`；坐标取整；`scale` 为 2 或 3 整数倍。

**健壮性：** `document` 级 `mouseup` 防止屏外松键卡死；`prefers-reduced-motion` 可降低 AUTO_RUN 速度或暂停。

### 5. 猫睡觉 z/Z（CatIllustration）

- 移除三元素 `Z/z/z`，改为 `.zzz-anchor` 内 `--small` / `--large` 两气泡
- `@keyframes zzzRise`：上移 32–48px、略向右、opacity 淡出；大 Z `animation-delay ~0.55s`
- 锚点初值：`left: 54%`; `top: 28%`（实现时按 GLB 微调）
- `prefers-reduced-motion: reduce` → 静止显示，opacity 0.75

### 6. 与 CatIllustration GLB 的关系

zzz 为 HTML  overlay，`z-index: 3`，不写入 Three.js。GLB 加载前后均显示；若锚点偏差 >10px，可增加 `.zzz-anchor--glb` 微调 class。

## Risks / Mitigations

| 风险 | 缓解 |
|------|------|
| B2 精灵未就绪阻塞联调 | 任务拆为先状态机+占位矩形，后换真图 |
| macOS 右键为 Ctrl+点击 | TESTING 中注明；`contextmenu` 仍拦截 |
| 右键与浏览器菜单 | `preventDefault` on `contextmenu` |
| 底部 canvas 挡表单 | 条带仅底部全宽，表单在上部不重叠 |
| light 主题对比不足 | `--login-*` 浅色分支显式定义 |

## Open Questions

- （已闭合）全部产品项已在探索阶段确认，无开放项。
