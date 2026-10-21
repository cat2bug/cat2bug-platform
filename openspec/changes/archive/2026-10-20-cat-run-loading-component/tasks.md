## 1. 资源与组件骨架

- [x] 1.1 将确认版雪碧 `run-sheet.png` / `run-sheet-dark.png`（及必要时单帧）从 `.cursor/office-preview/cat-run-frames-v4-fix/` 拷入 `cat2bug-platform-ui` 静态资源目录（如 `src/assets/images/loading/`）
- [x] 1.2 新增 `Cat2BugLoading` 组件（props：`text`、`size`、`variant`），实现 6 帧 `steps` 切帧、130ms/帧、等比视口
- [x] 1.3 实现 CSS 尘土（v4）与单色椭圆阴影（按帧缩放；深色浅影）
- [x] 1.4 支持 `prefers-reduced-motion: reduce`（停动画、静态一帧 + 文案）

## 2. Element Plus 接入（P0）

- [x] 2.1 调研并实现区域 `v-loading` 自定义 spinner / 包装，使默认加载呈现 `Cat2BugLoading`
- [x] 2.2 接入全屏 `ElLoading`（至少覆盖 `utils/request.js` 下载等待等共享入口）
- [x] 2.3 在 1～2 个高频列表页（如缺陷表 / Cat2BugTable 场景）验证浅色与深色主题观感

## 3. 扩展替换（P1，可同 change 勾选）

- [x] 3.1 梳理并替换其余明显加载占位（文档、用例、计划、Git 详情等长尾 `v-loading`），按页勾选推进
- [x] 3.2 清理仅服务于旧转圈的局部覆盖样式（若有）

## 4. 验收

- [x] 4.1 对照预览 v8：无串帧、无拉伸、阴影距离与按帧大小变化、深色可见阴影、尘土可见
- [x] 4.2 确认默认文案走 i18n；自定义 `text` 生效

## 5. 回写长期知识

- [x] 5.1 更新 `docs/04-UI规范/组件与交互约定.md`：加载态统一使用品牌奔跑猫组件 / 自定义 spinner
- [x] 5.2 在当月 `docs/08-版本记录` 记一行（变更名 `cat-run-loading-component`）
- [x] 5.3 确认无需新增 ADR（设计已写「无」）；若实现中出现长期决策再补
