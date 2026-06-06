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

- [ ] 5.1 缺陷页 `activated/deactivated` 调用 `registerPage('defect', …)`
- [ ] 5.2 绑定动作回调：新建/导出/导入/查询聚焦/切Tab/统计模版/上下页
- [ ] 5.3 数字键 1–9 定位置顶第 N 条缺陷

## 6. 键盘设置页

- [ ] 6.1 `views/member/keyboard/index.vue`：分组展示、启用开关、引导键展示
- [ ] 6.2 行内编辑捕获新键 + 同作用域冲突即时标红
- [ ] 6.3 `shortcut-store.js`：localStorage 读写（仅存 overrides 差异）+ 序列化
- [ ] 6.4 单项 / 全局「恢复默认」

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

- [x] 9.1 `dialog-form-shortcuts.js`：`Cmd/Ctrl+Enter` 保存、`Cmd/Ctrl+Esc` 关闭；新建缺陷禁用单独 `Esc`
- [x] 9.2 `form-field-hints.js`：按住 `Cmd/Ctrl` 显示字段字母徽标 + 字母跳转与闪动高亮
- [x] 9.2a 视口分配：仅当前可见表单项分配键位；滚动/`Cmd+↑↓` 时刷新徽标与映射
- [x] 9.2b 连续跳转：跳转时保留映射；`A/C/V/X/Z` 永不分配、永不拦截
- [x] 9.2c 键位池扩展：优先字母 + 数字 `1`–`0`；固定 `O` 绑定「保存本次选项」
- [x] 9.1a `dialog-form-shortcuts.js`：Mac `Cmd+Esc` latch、`getModifierState`、松键/徽标宽限期
- [x] 9.7a `native-file-picker.js`：选文件会话、暂停 Excel 键盘、上传 focusin 豁免、blur 不清 latch
- [x] 9.3 `tab-direction.js`：默认 Tab 正向、`↑+Tab` 反向、`↓+Tab` 正向
- [x] 9.4 `SelectProjectMember`：外框单 Tab 停靠点、下拉键盘（含空格选中）、tag 折叠、`Esc` 关闭
- [x] 9.5 `SelectModule`：外框单 Tab 停靠点与焦点行为对齐成员选择器
- [x] 9.6 `switch-keyboard.js`：开关单 Tab 停靠点、`←/→` 切换、空格原生切换
- [x] 9.7 `upload-focus-tab.js` + `upload-focus-tab` 工具：文件/图片上传外框单 Tab 停靠点、`Enter`/空格触发上传
- [x] 9.8 `AddDefect.vue`：组合组件与数字输入框聚焦边框（暗色金/浅色蓝）
- [x] 9.9 `shortcut/service.js`：页面动作空格仅在缺陷一级列表且无遮挡层时打开（`canOpenDefectPageActions`）
- [x] 9.10 E2E：`e2e/form-tab-order.spec.cjs`、`e2e/select-project-member-tags.spec.cjs`
- [x] 9.11 E2E：`e2e/debug-drawer-cmd-esc.spec.cjs`（Cmd+Esc 关闭抽屉）
- [x] 9.12 E2E：`e2e/debug-image-filepicker-arrows.spec.cjs`（原生文件框方向键）
