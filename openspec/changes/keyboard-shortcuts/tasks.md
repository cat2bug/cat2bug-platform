## 1. 引擎与插件骨架

- [ ] 1.1 `src/plugins/shortcut/ShortcutService.js`：document keydown 监听、引导态状态机、超时取消
- [ ] 1.2 焦点守卫：input/textarea/contenteditable/select/Excel 编辑态/对话框输入/IME 组合
- [ ] 1.3 `registry.js`：`registerPage` / `unregisterPage` 运行时动作注册表
- [ ] 1.4 `install.js`：Vue 插件，挂 `Vue.prototype.$shortcut`；在 `layout/index.vue` 初始化
- [ ] 1.5 全局启用开关接入（`enabled=false` 时不拦截）

## 2. 命令面板组件

- [ ] 2.1 `components/Shortcut/CommandPalette.vue`：居中悬浮、遮罩、Esc/超时/点击关闭、明暗适配
- [ ] 2.2 `components/Shortcut/ShortcutBadge.vue`：右下角字母角标
- [ ] 2.3 字母选择触发（叶子直达 / 下拉进二级）+ 面板内字母唯一性兜底（数字回退）
- [ ] 2.4 二级面板栈式切换与 `Esc` 返回
- [ ] 2.5 模糊搜索：顶部搜索框、标题过滤、方向键+回车选中、与字母直达并存（聚焦后字母作搜索内容）
- [ ] 2.6 无障碍：`role="dialog"`、aria 标注

## 3. 导航映射

- [ ] 3.1 从 `sidebarRouters` 派生左侧菜单导航项（标题/path/权限过滤）
- [ ] 3.2 默认字母表（缺陷/用例/测试计划/交付物/仪表盘/报告/项目文档/项目设置/团队设置/项目管理）
- [ ] 3.3 顶部工具栏项（通知/系统文档/国际化/个人中心/主题/官网/Git）接入面板
- [ ] 3.4 跳转、打开外链、主题切换等动作执行器

## 4. 下拉二级面板

- [ ] 4.1 国际化二级面板：列语言并复用 `lang-select` 切换逻辑
- [ ] 4.2 个人中心二级面板：个人中心 `P` / 键盘设置 `K` / 退出 `Q`
- [ ] 4.3 主题二级面板：浅色 `L` / 暗色 `D`，复用 `changeThemeMode`

## 5. 缺陷页动作面板

- [x] 5.1 缺陷页 `activated/deactivated` 调用 `registerPage('defect', …)`
- [x] 5.2 绑定动作回调：L 切换项目、E 新建菜单、S 查询、J 切 Tab、I 统计模版、O 切换视图、B/P 翻页
- [x] 5.3 `page-action-hints`：按住 `Cmd/Ctrl` 工具栏浮层徽标 + 字母直接触发
- [x] 5.4 表格/Excel 可见行动态徽标（`defect-row-kbd-hints.js`）
- [x] 5.5 统计区 G 导航：有模块时注册/显示，左右选择 + Delete 移除；无模块时不显示

## 6. 键盘设置页

- [x] 6.1 `views/member/keyboard/index.vue`：分组展示、启用开关、引导键展示
- [x] 6.2 行内编辑捕获新键 + 同作用域冲突即时标红
- [x] 6.3 `shortcut-store.js`：localStorage 读写（仅存 overrides 差异）+ 序列化
- [x] 6.4 单项 / 全局「恢复默认」
- [x] 6.5 「统计模版页」分组（P/G/H）+ i18n 7 语言

## 7. 入口、路由与国际化

- [ ] 7.1 `Navbar.vue` 头像下拉新增「键盘设置」入口
- [ ] 7.2 `router/index.js` 新增键盘设置页路由（隐藏路由，挂 Layout）
- [ ] 7.3 i18n 7 语言文案（面板/设置页/各快捷键标题）

## 8. 验证

- [ ] 8.1 焦点守卫手工用例：输入框/Excel/对话框/IME 下不误触
- [ ] 8.2 导航与缺陷动作全键位手工走查
- [ ] 8.3 自定义、冲突检测、恢复默认、刷新持久化验证
- [ ] 8.4 明暗主题与 7 语言下面板与设置页显示正确
- [ ] 8.5 模糊搜索与主题二级面板手工走查（搜索过滤、`g m d` 切暗色）
- [ ] 8.6 （可选）Playwright e2e：`g d` 跳缺陷、`空格 n` 新建、`g m d` 切主题、搜索过滤、设置页改键生效

## 9. 缺陷表单与组合组件键盘集成

- [x] 9.1 `dialog-form-shortcuts.js` + `defect-drawer-shortcuts.js`：`Cmd/Ctrl+Enter` 保存、`Esc` 关闭（未保存确认）
- [x] 9.2 `form-field-hints.js`：按住 `Cmd/Ctrl` 显示字段字母徽标 + 字母跳转与闪动高亮
- [x] 9.2a 视口分配：仅当前可见表单项分配键位；滚动/`Cmd+↑↓` 时刷新徽标与映射
- [x] 9.2b 连续跳转：跳转时保留映射；`A/C/V/X/Z` 永不分配、永不拦截
- [x] 9.2c 键位池扩展：优先字母 + 数字 `1`–`0`；固定 `O` 绑定「保存本次选项」
- [x] 9.1a 关闭改为 `Esc`；关闭按钮无徽标；保存按钮仅在 `Cmd/Ctrl` 按住时显示 `↵`
- [x] 9.1b 松开修饰键立即隐藏字段提示与保存 `↵` 徽标
- [x] 9.7a `native-file-picker.js`：选文件会话、暂停 Excel 键盘、上传 focusin 豁免、blur 不清 latch
- [x] 9.3 `tab-direction.js`：默认 Tab 正向、`↑+Tab` 反向、`↓+Tab` 正向
- [x] 9.4 `SelectProjectMember`：外框单 Tab 停靠点、下拉键盘（含空格选中）、tag 折叠、`Esc` 关闭
- [x] 9.5 `SelectModule`：外框单 Tab 停靠点与焦点行为对齐成员选择器
- [x] 9.6 `switch-keyboard.js`：开关单 Tab 停靠点、`←/→` 切换、空格原生切换
- [x] 9.7 `upload-focus-tab.js` + `upload-focus-tab` 工具：文件/图片上传外框单 Tab 停靠点、`Enter`/空格触发上传
- [x] 9.8 `AddDefect.vue`：组合组件与数字输入框聚焦边框（暗色金/浅色蓝）
- [x] 9.9 `shortcut/service.js`：页面动作空格仅在缺陷一级列表且无遮挡层时打开（`canOpenDefectPageActions`）
- [x] 9.10 E2E：`e2e/form-tab-order.spec.cjs`、`e2e/select-project-member-tags.spec.cjs`
- [x] 9.11 E2E：`e2e/debug-drawer-cmd-esc.spec.cjs`（抽屉 Esc 关闭）
- [x] 9.12 E2E：`e2e/debug-image-filepicker-arrows.spec.cjs`（原生文件框方向键）

## 10. 统计模版页快捷键

- [x] 10.1 `StatisticTemplate.vue` 注册 `statistic-template` 动作（P/G/H）
- [x] 10.2 三区键盘导航：预览左右、个人/团队网格四向
- [x] 10.3 `statistic-grid-kbd.js` 跨区上下传递（边界行 + x 对齐）
- [x] 10.4 Esc：导航中退出导航，否则返回上一页
- [x] 10.5 全局 `page-kbd-hints.scss` 浮层徽标样式

## 11. 缺陷工具弹框键盘

- [x] 11.1 `defect-tool-dialog-kbd.js` + `defect-tool-dialog-close.js`：六个工具弹框统一 mixin
- [x] 11.2 `Cmd/Ctrl+Enter` 提交、`Esc`/取消/关闭前未保存确认（`$modal.confirm`）
- [x] 11.3 弹框打开后聚焦首字段 + `Cmd/Ctrl` 字段字母徽标
- [x] 11.4 `defect-tool-dialog-close-state.js` 表单快照序列化

## 12. 处理缺陷抽屉徽标

- [x] 12.1 `handle-defect-kbd-hints.js`：工具栏字母 + 风琴组数字/字母徽标
- [x] 12.2 `panel-kbd-hints.js`：视口检测与键位分配器
- [x] 12.3 `Cmd/Ctrl+↑/↓` 滚动抽屉内容区；栈顶抽屉守卫

## 13. 统计模块配置弹框

- [x] 13.1 `statistic-dialog-kbd.js` + `statistic-dialog-close.js`
- [x] 13.2 `PersonalRemindTimer` / `MyLife` 接入关闭确认与保存快捷键
- [x] 13.3 `statistic-dialog-close-state.js` 各弹框自定义快照

## 14. 报时提醒表格与时间控件

- [x] 14.1 `remind-timer-table-kbd.js`：`Cmd/Ctrl+数字` 跳行、`+` 新增、`Del` 删行
- [x] 14.2 时间控件失焦关面板、`pick`/回车回焦、`↑/↓` 重开下拉
- [x] 14.3 试播/删除/新增按钮统一焦点环
- [x] 14.4 E2E：`e2e/remind-timer-time-picker-blur.spec.cjs`

## 15. 统计区 G 导航增强

- [x] 15.1 `statistic-item-kbd.js`：模块主点击区映射表
- [x] 15.2 导航中 `Enter`/空格触发 `activateStatisticItemClick`

## 16. 表单焦点视觉统一

- [x] 16.1 移除 `cat2bug-field-flash` 荧光动画，改用全站 `:focus` 焦点环
- [x] 16.2 MessageBox 底部按钮区：分割线与按钮间距 10px（`cat2bug.scss`）

## 17. 布局级 Shift+Cmd 导航浮层

- [x] 17.1 `layout-nav-hints.js` mixin + `utils/layout-nav-hints.js`
- [x] 17.2 Navbar / LangSelect / Hamburger 增加 `data-layout-nav` 锚点
- [x] 17.3 与 `g` 导航共用 `buildNavItems` 字母；侧栏折叠默认 `` ` ``
- [x] 17.4 页面级 Cmd 浮层在 `shiftKey` 时互斥跳过
- [x] 17.5 `layout-nav-dropdown-kbd.js`：团队/用户/国际化下拉 ↑↓ 切换、Enter 确认、Esc 关闭

## 18. 登录页老鼠 D-pad 操控

- [x] 18.1 `MouseDpadControls.vue` + `mouse-arrow-keys.js`：⌘/Ctrl 显示 D-pad、方向键解析与 macOS keyup 兜底
- [x] 18.2 `MouseRunner` 键盘/触控/D-pad 共用 `pressMoveKey` / `releaseMoveKey`
- [x] 18.3 松开方向键 D-pad 高亮消失；无移动键时播放坐立动画（⌘ 仍按住亦然）
- [x] 18.4 手动控制左右屏幕边界（精灵半宽动态 clamp）；自动奔跑折返逻辑不变
- [x] 18.5 `login.vue` / `register.vue` 挂载；i18n D-pad 文案

## 19. 通知弹框键盘

- [x] 19.1 `send-notice-dialog-kbd.js`：发送弹框 Cmd/Ctrl+Enter、Esc、字段徽标、打开后聚焦
- [x] 19.2 `notice-option-dialog-kbd.js` + `notice-option-dialog-close.js`：设置弹框保存/关闭确认
- [x] 19.3 通知设置主 Tab（G/P）、平台子 Tab（数字）、系统开关/背景音乐固定徽标
- [x] 19.4 Tab 切换时刷新字段徽标映射
