## ADDED Requirements

### Requirement: Document page shows a collapsible folder tree

文档管理页 SHALL 在列表左侧提供文件夹树（仅文件夹节点）。用户 MUST 能显示或隐藏该树；显隐状态 MUST 写入全局本地键并在刷新后恢复。树的布局与拖宽交互 SHOULD 对齐缺陷页交付物树（multipane），但 MUST NOT 复用交付物业务数据源。

#### Scenario: Toggle tree visibility persists across refresh

- **WHEN** 用户隐藏左侧文件夹树后刷新页面
- **THEN** 树保持隐藏；再次显示后仍可浏览文件夹

#### Scenario: Select folder in tree loads right-hand list

- **WHEN** 用户点击树中某一文件夹节点
- **THEN** 右侧列表展示该文件夹下的子文件夹与文件（含分页）

### Requirement: Folder tree supports project-wide name filter

树顶部 MUST 提供模糊查询输入。有关键字时，匹配范围 MUST 为当前项目下全部文件夹名称（不仅是已展开节点）。清空关键字后 MUST 恢复常规树浏览。

#### Scenario: Filter matches folders outside the expanded path

- **WHEN** 用户在树搜索框输入关键字，且匹配文件夹位于尚未展开的深层路径
- **THEN** 树中仍能看到该匹配文件夹（或其可达路径），用户可点击并打开右侧列表

#### Scenario: Clear filter restores normal tree

- **WHEN** 用户清空树搜索关键字
- **THEN** 树恢复为可正常展开的文件夹浏览，且当前选中夹（若仍存在）保持同步

### Requirement: Create folder from tree node

每个文件夹树节点旁 MUST 提供图标形式的「新建文件夹」入口；点击后 MUST 在该节点对应文件夹下创建子文件夹（父级为该节点），成功后树与右侧列表 MUST 反映新文件夹。

#### Scenario: Create child folder on a tree node

- **WHEN** 用户点击某树节点旁的新建文件夹图标并提交有效名称
- **THEN** 该节点下出现新子文件夹，且若右侧正显示该父文件夹，列表中也能看到新文件夹

### Requirement: List and tree navigation stay in sync

用户通过树节点、列表中的文件夹行、或「上级目录」行切换当前位置时，系统 MUST 更新同一当前位置，并 MUST 同步树的当前高亮（必要时展开路径）。「上级目录」行 MUST 保留。

#### Scenario: Click folder row highlights tree node

- **WHEN** 用户在右侧列表点击某一子文件夹行进入该夹
- **THEN** 左侧树高亮对应节点（路径已展开）

#### Scenario: Parent-directory row syncs tree to parent

- **WHEN** 用户点击列表中的「上级目录」行
- **THEN** 右侧回到父文件夹内容，左侧树高亮父节点（或根）

### Requirement: Multi-select with batch move and batch delete

文档列表 MUST 支持多选。工具条在「新建文件夹」按钮之前 MUST 提供批量移动与批量删除；无选中项时这些操作 MUST 不可用或明确无操作。批量删除前 MUST 经用户确认。

#### Scenario: Batch delete requires confirmation

- **WHEN** 用户多选若干文档/文件夹并点击批量删除
- **THEN** 系统先弹出确认；用户取消则 MUST NOT 删除任何项；用户确认后删除所选（文件夹含级联，见级联要求）

#### Scenario: Batch move reuses move target picker

- **WHEN** 用户多选若干项并点击批量移动，选择目标文件夹后确认
- **THEN** 所选项的父文件夹更新为目标夹，右侧列表与树反映移动结果

### Requirement: Folder delete cascades to descendants

删除文件夹（含批量中的文件夹）时，系统 MUST 删除该文件夹及其全部子孙文件夹与文件的文档记录，行为 MUST 与「删除后会自动删除此文件夹下的所有文件及文件夹」的产品文案一致。仅删除文件时 MUST NOT 影响其它文档。

#### Scenario: Delete non-empty folder removes children

- **WHEN** 用户确认删除一个含有子文件夹与文件的文件夹
- **THEN** 该文件夹及其所有子孙文档记录均不再出现在列表/树中，且不以孤儿 `doc_pid` 残留

#### Scenario: Delete file only removes that file

- **WHEN** 用户确认删除单个文件文档
- **THEN** 仅该文件被删除，同夹其它项保留

### Requirement: Page size is remembered globally before first list

文档列表分页的「每页条数」MUST 写入全局本地键。进入或刷新文档页时，系统 MUST 先读取该值再发起列表查询，MUST NOT 先用默认 pageSize 查一次再换成缓存值重查。

#### Scenario: Refresh keeps page size and lists once

- **WHEN** 用户将每页条数改为非默认值后刷新文档页
- **THEN** 分页组件显示该条数，且该次进入仅按该条数发起一次列表请求（排除树自身的文件夹查询）
