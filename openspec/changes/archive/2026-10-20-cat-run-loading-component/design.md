## Context

产品加载态普遍使用 Element Plus 默认转圈（`v-loading`、`ElLoading.service`、局部 `element-loading-text`）。品牌侧已有登录狸花猫与错误页插画；加载等待仍是通用 spinner，识别度弱。

设计预览已锁定（`.cursor/office-preview/cat-run-loading-preview-v8.html` + `cat-run-frames-v4-fix/`）：

- 6 帧雪碧（原 v4 帧 0/4/5/7/8/9），去掉与首帧近重复的末帧以避免循环顿挫
- 视口宽随高度等比：`width = height × 382/280`（勿在 `:root` 把 `--cat-w` 算死）
- CSS `steps(6)` + 按视口宽整像素平移切帧（禁用易串帧的 `0%→100%`）
- 默认 130ms/帧；尘土用 v4 CSS 粒子；脚底单色椭圆阴影并按帧缩放；深色主题浅色阴影

## Goals / Non-Goals

**Goals:**

- 提供可复用组件 `Cat2BugLoading`（猫奔跑 + 文案），作为产品统一加载视觉。
- 覆盖浅色 / 深色 / 半透明遮罩三种对比场景。
- 优先接入高频入口：全局/区域 `v-loading`、全屏 `ElLoading`、明显的空加载占位；其余按清单推进。
- 把加载态约定写入 `docs/04-UI规范/`。

**Non-Goals:**

- 不重新生成或用 SVG/AI 替换已确认的位图轮廓（除非后续单独变更）。
- 不改后端、不改加载业务时序（仅替换视觉）。
- 不要求第一期替换仓库内每一个 `v-loading`（允许分批；tasks 标明 P0/P1）。
- 不做可配置「换猫皮」的主题市场。

## Decisions

1. **组件形态：Vue 3 展示组件 + 可选 Loading 包装**  
   - `Cat2BugLoading`：props 含 `text`（默认 i18n「加载中...」）、`size`（高度 px，默认 44）、`variant`（`light` | `dark` | `auto` 跟主题）。  
   - 接入 Element：优先自定义 `v-loading` 的 spinner（Element Plus 支持 `element-loading-spinner` / 自定义 SVG 或通过 CSS 覆盖 `.el-loading-spinner`）；全屏 `ElLoading` 用同类自定义。若 EP 版本限制无法插组件，则用「遮罩 + 居中挂载 `Cat2BugLoading`」薄包装。  
   - 备选：只提供组件、业务页手写替换 → 易漏，否决为唯一策略。

2. **动画实现：PNG 雪碧 + CSS steps**  
   - 资源：浅色轮廓 `run-sheet.png`、深色轮廓 `run-sheet-dark.png`（白影），放在 `cat2bug-platform-ui` 静态资源目录（如 `src/assets/images/loading/`）。  
   - 切帧：`background-size: (frameW×6) × frameH`；`to { background-position: -6×frameW }` + `steps(6)`；帧时 130ms → 周期 780ms。  
   - 备选：逐帧换 `img.src` → 易闪，已否决。  
   - 备选：百分比 `0%→100%` 切帧 → 易串帧，已否决。

3. **尘土与阴影用 CSS，不烤进雪碧**  
   - 尘土：runner 左侧留白 + `overflow: visible`，粒子动画对齐预览 v4。  
   - 阴影：单色 `border-radius: 50%` 椭圆；与奔跑同周期按帧 `scale`（约 1.00 / 0.82 / 0.95 / 0.77 / 0.95 / 0.76）；深色主题 `--shade` 用浅色半透明白。  
   - 位图内脚影在 44px 下几乎不可见，不依赖位图阴影。

4. **尺寸与主题**  
   - 默认高度 44px；允许 28 / 56 等通过 prop；宽随高等比。  
   - `auto`：跟随应用 `themeMode`（与错误页插画策略一致，见 `error-state-pages`）。

5. **文案**  
   - 默认走现有 i18n key（如 `loading` / `page.loading`），允许 prop 覆盖；不硬编码中文进组件逻辑（预览 HTML 除外）。

6. **文档**  
   - 回写 `docs/04-UI规范/组件与交互约定.md`：加载态使用 `Cat2BugLoading` / 统一自定义 spinner。  
   - 当月 `docs/08-版本记录` 一行。  
   - **不新增 ADR**（展示层组件约定，非架构分叉）。

## Risks / Trade-offs

- [EP 自定义 spinner 能力差异] → 先做组件；接入时验证 EP API，不行则用遮罩包装，不阻塞组件交付。  
- [全量替换遗漏] → tasks 分 P0（全局服务 / 高频列表）与 P1（长尾页）；验收列检查表。  
- [雪碧体积] → 6 帧单色 PNG，体积可控；若后续膨胀再考虑 sprite 压缩。  
- [reduced-motion] → 尊重 `prefers-reduced-motion: reduce` 时停步动画，仅静态一帧 + 文案（实现时写入组件样式）。

## Migration Plan

1. 合入组件与资源，不影响现有 spinner。  
2. 替换 P0 入口并目测浅/深主题。  
3. 按 P1 清单推进；回滚时去掉自定义 spinner / 包装即可回到 EP 默认。

## Open Questions

- 全量替换完成度：第一期是否只做「组件 + P0 接入」，P1 另开 change？（默认：**本 change 含 P0，P1 列在 tasks 可勾选推进**。）

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/04-UI规范/组件与交互约定.md`（拟回写加载态）
- `docs/04-UI规范/说明.md`
- `docs/08-版本记录/`（当月一行）
- 预览真相源（非 docs）：`.cursor/office-preview/cat-run-loading-preview-v8.html`、`cat-run-frames-v4-fix/`
- 拟新增 ADR：无
