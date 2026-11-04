# project-workbench Specification

## Purpose

项目落地页工作台壳层、页签与组件放置；组件显隐须尊重团队功能模块闸门。
## Requirements
### Requirement: Project landing shows workbench shell with tabs

Within a project, the module formerly presented as a fixed dashboard SHALL present a **workbench** shell with a fixed **工作台** tab, optional custom tabs (private and project-shared), and an add control, as specified by `workbench-custom-tabs`. The browser path MAY remain `/{teamKey}/{projectKey}` or `/{teamKey}/{projectKey}/dashboard`. The shell MUST NOT present a **看板** tab.

#### Scenario: Open work tab by default

- **WHEN** a project member opens the project landing or dashboard module and has no valid locally cached custom tab selection
- **THEN** the workbench shell is shown
- **THEN** the **工作台** tab is selected by default
- **THEN** the personal work layout rows are rendered

#### Scenario: No board tab

- **WHEN** the member inspects the workbench tab bar
- **THEN** there is no **看板** tab
- **THEN** former dashboard statistics remain available only as catalog widgets on editable layouts

### Requirement: Per-user per-project layout persistence

The system SHALL persist the personal **工作台** layout configuration keyed by `(userId, projectId)`, including preset id and v3 row layout for that personal surface. Configuration MUST sync across sessions for that user and project. Custom tab layouts MUST persist separately per `workbench-custom-tabs` and MUST NOT be stored only inside another user's personal workbench blob.

#### Scenario: Save layout after edit

- **WHEN** a member finishes editing the personal **工作台** layout and saves
- **THEN** a subsequent visit to the same project by the same user restores that personal layout

#### Scenario: Missing layout uses role preset

- **WHEN** a member has no saved personal layout for the project
- **THEN** the system applies a default role-based preset (`manager` / `tester` / `developer` / `outsider`) without requiring a prior save

### Requirement: Role presets

The system SHALL provide role-oriented presets at least `tester`, `developer`, `manager`, and `outsider` for personal **工作台** defaults, plus create-time `document` and edit-time `custom` as specified by `workbench-row-layout`. Applying a preset from edit mode MUST replace the current surface layout after explicit confirmation. Presets MUST define v3 rows and MUST NOT depend on a board widget list.

#### Scenario: Apply tester preset

- **WHEN** a member confirms applying the `tester` preset on the personal workbench
- **THEN** the work surface includes at least my-defects style widgets (per product preset table)
- **THEN** the previous custom rows for that personal surface are replaced

### Requirement: Personal statistics on work tab

The personal **工作台** SHALL support personal statistics widgets scoped to the current authenticated user in the current project (for example: my open defect counts, my handled/closed trend, my plan-item progress). Personal widgets MUST NOT show project-wide aggregates unless the widget is explicitly documented as project-scoped. Project-scoped statistic widgets MAY be placed on the personal workbench or on custom tabs via the catalog; they MUST NOT require a board tab.

#### Scenario: Tester preset shows personal KPI

- **WHEN** a member uses the `tester` preset with no custom personal layout
- **THEN** the work tab includes at least one personal KPI or personal trend widget whose data is filtered to the current user

#### Scenario: Personal trend excludes others' volume

- **WHEN** the member views a personal defect-handling trend widget
- **THEN** the series reflect only actions attributed to the current user in that project

### Requirement: Actionable work widgets

Work-tab widgets that list defects or plan items SHALL allow common actions without navigating away from the workbench shell (inline controls and/or existing detail drawers/dialogs). Actions MUST respect the member's existing business permissions.

#### Scenario: Close defect from my-defects widget

- **WHEN** a member with close permission triggers close on a defect in the my-defects widget and completes the required dialog
- **THEN** the defect state updates
- **THEN** the widget list refreshes to reflect the change

### Requirement: Scope is project-local in phase one

Phase one MUST scope workbench layout and content to the current project only. The system MUST NOT require a cross-project personal home workbench for phase one.

#### Scenario: Switching project loads that project's layout

- **WHEN** a member switches from project A to project B
- **THEN** the workbench shows layout and data for project B
- **THEN** project A's layout is unchanged

### Requirement: Workbench hides widgets for team-disabled modules

工作台画布与组件目录 MUST 尊重所属团队的模块页面设置：当某开关键关闭时，声明依赖该开关键（或其映射业务域）的组件 MUST 不渲染在画布上，且 MUST NOT 出现在可添加目录中（或等价不可添加）。全部业务模块关闭时，依赖这些模块的组件 MUST 均不显示。

#### Scenario: Defect widgets hidden when defect module off

- **WHEN** 团队关闭缺陷模块且用户打开项目工作台
- **THEN** 已放置的缺陷类组件不显示，目录中亦不可再添加缺陷类组件

#### Scenario: Case and plan widgets follow case_plan switch

- **WHEN** 团队关闭 `case_plan`
- **THEN** 用例类与计划类工作台组件均不显示

