## ADDED Requirements

### Requirement: Markdown editor replacement

The frontend SHALL replace Vue 2-only markdown editors with Vue 3-compatible alternatives.

#### Scenario: Mavon editor replacement

- **WHEN** a page previously used `mavon-editor` (defect description, report templates, notices)
- **THEN** it MUST use a Vue 3 markdown editor (e.g. `md-editor-v3`) with equivalent edit/preview/toolbar capability

#### Scenario: markdown-it-vue replacement

- **WHEN** a page previously used `markdown-it-vue` for rendering
- **THEN** it MUST render via `markdown-it` directly or a Vue 3 wrapper without functionality loss

### Requirement: Excel editor migration

The defect Excel list view SHALL migrate from `vue-excel-editor` to a Vue 3-compatible spreadsheet component.

#### Scenario: Excel view feature parity

- **WHEN** the user opens the defect Excel list view (`views/system/defect/list/excel.vue`)
- **THEN** column visibility settings, inline editing, and panel settings MUST behave equivalently to the current vue-excel-editor implementation

#### Scenario: Patch removal

- **WHEN** the migration completes
- **THEN** the `patches/vue-excel-editor+1.5.23.patch` patch-package entry MUST be removed or replaced with a Vue 3 library that does not require patching

### Requirement: Form and picker library replacements

Vue 2-specific picker and tree components SHALL be replaced with Vue 3 equivalents.

#### Scenario: Tree select

- **WHEN** a page uses `@riophae/vue-treeselect`
- **THEN** it MUST use a Vue 3 tree select component with equivalent search, multi-select, and disabled-node behavior

#### Scenario: Date picker component

- **WHEN** a page uses `vue2-datepicker` outside of Element date pickers
- **THEN** it MUST use `@vuepic/vue-datepicker` or Element Plus date picker with equivalent behavior

#### Scenario: Image cropper

- **WHEN** the user avatar upload cropper is used
- **THEN** `vue-cropper` MUST be replaced with a Vue 3 compatible cropper maintaining crop/upload flow

#### Scenario: Draggable lists

- **WHEN** drag-and-drop reordering is used (plans, form builder, modules)
- **THEN** `vuedraggable` MUST be upgraded to v4 (`vuedraggable@next`) with SortableJS compatibility

### Requirement: Meta and head management

Document head management SHALL migrate from `vue-meta` to a Vue 3 head library.

#### Scenario: Page title and meta tags

- **WHEN** a route defines title or meta information
- **THEN** `@unhead/vue` (or equivalent) MUST update `document.title` and meta tags identically to vue-meta behavior

### Requirement: Vendor and multipane handling

Vendored or patched libraries SHALL be evaluated and either upgraded or re-vendored for Vue 3.

#### Scenario: Vue-multipane compatibility

- **WHEN** split pane layouts render (defect list, plan views)
- **THEN** the patched `src/vendor/vue-multipane` MUST work with Vue 3 or be replaced with a Vue 3 split pane library preserving resize behavior

#### Scenario: VXE Table upgrade

- **WHEN** VXE Table features are used in the codebase
- **THEN** the library MUST be upgraded to vxe-table v4+ compatible with Vue 3

### Requirement: Code generator template update

The code generator (`tool/gen`) SHALL output Vue 3 + Element Plus compatible code.

#### Scenario: Generated Vue component syntax

- **WHEN** a developer generates code via the admin code generator
- **THEN** output templates MUST use Vue 3 syntax (no `.sync`, no filters, `v-model:` props) and Element Plus component names

#### Scenario: Generator utility templates

- **WHEN** generator utilities in `src/utils/generator/` produce HTML/JS/render code
- **THEN** those templates MUST be updated to Element Plus APIs and Vue 3 slot syntax
