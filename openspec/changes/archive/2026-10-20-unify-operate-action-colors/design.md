## Context

缺陷操作列已用语义 class + token：`green` → `--cat2bug-operate-text-success`，`red` → `--cat2bug-operate-text-danger`，流转动作用 `orange` → muted。多数其它列表「修改/添加」无 class，暗色下默认 text/link 落成品牌蓝，修改与添加不可区分。长期约定见 `docs/04-UI规范/组件与交互约定.md`，尚未写清 CRUD 三色。与 `unify-list-density`（密度/工具栏轨）正交，单独变更。

## Goals / Non-Goals

**Goals:**

- 列表操作列：修改绿、添加蓝、删除红；浅色/暗色均走 token。
- 补显式 `.blue`，避免「无 class = 蓝」的隐式约定。
- 文档写清 class ↔ token；首批页打齐 class。

**Non-Goals:**

- 不改缺陷流转 `orange` / `restore-orange`。
- 不改工具栏「新建」、对话框确认、实心按钮体系。
- 不重命名历史 class `orange`（虽非字面橙）。
- 不一次扫完所有非列表场景的编辑/删除链接。

## Decisions

1. **三色映射（对齐缺陷「修改/删除」）**  
   - 修改：`class="green"` → `--cat2bug-operate-text-success`（及 hover）。  
   - 删除：`class="red"` → `--cat2bug-operate-text-danger`。  
   - 添加：`class="blue"` → 新增 `--cat2bug-operate-text-primary`（值绑 `--cat2bug-primary` / 暗色 accent 同源），浅暗均定义。  
   - 否决：添加继续靠「无 class 默认蓝」——隐式、易被其它规则盖掉。

2. **样式落点**  
   - 在 `cat2bug.scss` 与 `theme-dark.scss` 为 `.el-button.is-link.blue` / `--text.blue`（及 `.cat2bug-operate-tools` 作用域）补规则，镜像 `.green` / `.red`。  
   - 色值只引 token，不写死 `#67c23a` 等到业务 Vue。

3. **扫页范围（首批）**  
   - 必改：交付物 `module`、OpenAI 账号、用例类型；顺带同模式列表操作列（计划/文档/用例等已有删除 `red`、缺修改 `green` / 添加 `blue` 的）。  
   - 缺陷：`DefectTools` 修改已是 `green`、删除已是 `red`；无行内「添加」则跳过。  
   - 识别启发式：操作列内文案/ i18n 为修改·编辑 + edit 图标 → `green`；添加·新建子项 + plus → `blue`；删除 + delete → `red`。测试/查看/分享等保持默认或既有 `orange`。

4. **拟新增 ADR：无** — UI 约定写入组件文档即可。

## Risks / Trade-offs

- [误给非 CRUD 钮打绿/蓝] → 只打明确增改删；流转/测试不加。  
- [`.blue` 与全局默认 link 蓝重复] → 接受；显式 class 可防默认规则变化。  
- [暗色 `!important` 覆盖] → 与 `.green`/`.red` 同级写进 theme-dark 操作列块。

## Migration Plan

1. 加 token + `.blue` 样式。  
2. 扫首批列表页 class。  
3. 更新组件约定与版本记录。  
4. 浅/暗色对照缺陷与交付物操作列。  
5. 回滚：还原 class 与样式即可。

## Open Questions

无。添加用显式 `.blue`（探索已定）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/04-UI规范/主题与配色.md`
- `docs/08-版本记录/2026-10.md`

拟新增 ADR：无
