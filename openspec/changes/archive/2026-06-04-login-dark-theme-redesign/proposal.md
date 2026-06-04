## Why

登录页视觉与交互与产品 mockup（`mockup_dark_v5.png`）差距较大：左栏仍为中文硬编码长文案、缺少特性列表与品牌双色；底部老鼠使用写实棕鼠精灵且为键盘控制；猫头顶睡眠符号数量与轨迹不符合设计。需要按 mockup 分阶段落地**暗色主题优先**的登录体验，并为后续浅色主题预留 token。

## What Changes

### A — 左栏品牌与暗色 token（不强制全局 dark）

- 在 `login.vue` 挂载登录页专用 CSS 变量（`--login-bg`、`--login-accent` 等），暗色下对齐 mockup（`#0D0D0D` / `#1A1A1A` / `#FFC107`），浅色下保持可读
- **不**在 `/login` 强制 `html.dark`；跟随用户 `themeMode` / 系统偏好
- 左栏结构：Logo + 双色品牌名（`Cat2` 白 + `Bug` 黄 + `-Platform` 灰）+ 黄色装饰线 + Hero 标题/描述 + 三条 Feature（图标 + 标题 + 说明）
- 新增 i18n 键 `login.hero.*`、`login.features.*`，覆盖 7 语言；移除左栏对硬编码中文与 `login.introduce1/2/3` 的依赖

### B — 8-bit 像素鼠与右键交互

- 新增精灵资源 `mouse_sprite_pixel.png`（B2），弃用 `mouse_sprite_clean.png`
- 重写 `MouseRunner.vue` 状态机：`AUTO_RUN`（随机左右、出屏再入，可选远近平进）→ `USER_CONTROL`（按住右键跟随光标 + 伪 3D scale）→ `SIT_LOOK_AROUND`（坐姿左右看）→ `AUTO_RUN`
- 底部条带启用 `pointer-events`；`contextmenu` / 右键 `preventDefault`
- Canvas `imageSmoothingEnabled = false`，整数坐标绘制

### C — 猫睡觉 z/Z 动态符号

- 调整 `CatIllustration.vue`：锚点定位的小写 `z` + 大写 `Z` 两气泡，向上飘并淡出；支持 `prefers-reduced-motion`
- 与 GLB / PNG fallback 共用锚点，zzz 置于场景层之上

### 非本变更（后续）

- 登录右栏表单完整 mockup 对齐（Forgot password、去表单大标题、按钮箭头图标等）
- 浅色主题完整打磨（仅预留 token，不验收浅色像素级还原）

## Capabilities

### New Capabilities

- `login-hero-branding`：左栏 Hero、特性列表、品牌双色、登录页 token、7 语言 i18n
- `login-mouse-pixel-runner`：B2 像素精灵、右键状态机、随机左右自动跑、伪 3D、尘土粒子
- `login-cat-sleep-zzz`：猫头顶 z/Z 睡眠气泡动画与无障碍降级

### Modified Capabilities

- （无）不修改后端 API 或认证流程

## Impact

- **前端**：`login.vue`、`UserNameAndPasswordLogin.vue`（token 绑定）、`MouseRunner.vue`、`CatIllustration.vue`；新增 `mouse_sprite_pixel.png`；7 个 `i18n-*.json`
- **后端**：无
- **参考稿**：`mockup_dark_v5.png`（仓库根目录）
- **Non-Goals**：不强制登录页 dark；不替换浅色主题全套；不在此变更完成右栏表单 mockup 全量对齐
