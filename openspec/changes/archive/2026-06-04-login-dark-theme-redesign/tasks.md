## 1. 资产：B2 像素鼠精灵

- [x] 1.1 按 design.md 绘制或生成 `src/assets/images/mouse_sprite_pixel.png`（8×32×24，5 行，透明底，灰鼠粉尾）
- [x] 1.2 在 README 或变更 TESTING 中附帧行说明，便于后续维护

## 2. A：登录页 token 与左栏

- [x] 2.1 在 `login.vue` 为 `.logo-page` 定义 `html.dark` / 浅色两套 `--login-*` 变量
- [x] 2.2 重构左栏模板：品牌双色 + 黄线 + `$t('login.hero.*')` + 三条 `$t('login.features.*')`
- [x] 2.3 新增 `login-feature-target` 图标（或选定 `guide`/`point`）并绑定 Feature 列表样式
- [x] 2.4 更新 7 个 `i18n-*.json`：hero、features 全套键；废弃左栏对 `login.introduce1/2/3` 硬编码引用
- [x] 2.5 `UserNameAndPasswordLogin.vue` 表单卡片/输入框改用 `--login-card-bg`、`--login-input-bg` 等（暗色对齐 mockup）

## 3. B：MouseRunner 状态机

- [x] 3.1 启用底部容器 `pointer-events: auto`；绑定 `contextmenu`、`mousedown`、`mousemove`、`mouseup`（含 document 兜底）
- [x] 3.2 实现 `AUTO_RUN` / `USER_CONTROL` / `SIT_LOOK_AROUND` 与随机方向、出屏 respawn
- [x] 3.3 接入 `mouse_sprite_pixel.png`、行常量、`imageSmoothingEnabled = false`、伪 3D scale/速度
- [x] 3.4 实现 row3 坐姿左右看帧序列；USER_CONTROL 跟指针与方向行切换
- [x] 3.5 尘土粒子随主题色；`prefers-reduced-motion` 降级

## 4. C：猫睡觉 z/Z

- [x] 4.1 `CatIllustration.vue`：`.zzz-anchor` + 小 z / 大 Z 两气泡，移除旧三元素
- [x] 4.2 实现 `zzzRise` 关键帧与 delay；定义 `--login-zzz-color`（或在组件内 dark/light 分支）
- [x] 4.3 按 GLB 画面微调锚点；验证 fallback PNG 与 GLB 两种状态
- [x] 4.4 `prefers-reduced-motion: reduce` 静态降级

## 5. 验证

- [x] 5.1 按 `TESTING.md` 在 dark 主题下对照 `mockup_dark_v5.png` 手工验收
- [x] 5.2 `npm run lint`（`cat2bug-platform-ui`）无新增错误
- [x] 5.3 确认未调用强制 `applyThemeMode('dark')` 于登录路由
