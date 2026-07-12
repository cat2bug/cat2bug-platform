# frontend-element-plus Specification

## Purpose
TBD - created by archiving change vue3-vite-pinia-migration. Update Purpose after archive.
## Requirements
### Requirement: Element Plus as sole UI library

The frontend SHALL replace Element UI 2.x with Element Plus as the only component library for all `el-*` components.

#### Scenario: On-demand component loading

- **WHEN** a production build completes
- **THEN** Element Plus components MUST be tree-shaken via `unplugin-vue-components` and `unplugin-auto-import` (or equivalent) to reduce bundle size versus full Element UI import

#### Scenario: Default component size

- **WHEN** the app initializes
- **THEN** Element Plus default size MUST respect the cookie-stored size preference (fallback `medium`)

### Requirement: Theme and SCSS variable migration

Element Plus theming SHALL replace Element UI SCSS variable system while preserving Cat2Bug visual identity.

#### Scenario: Theme variables

- **WHEN** the app loads styles
- **THEN** Cat2Bug custom SCSS (`cat2bug.scss`, `element-variables` equivalent) MUST apply consistent primary colors, table styles, and dark mode (`themeMode`) behavior

#### Scenario: Custom table scrollbar

- **WHEN** tables with fixed columns render
- **THEN** custom scrollbar width alignment (current `element-scrollbar-width` alias behavior) MUST be preserved so fixed columns do not cover the scrollbar

### Requirement: Component API migration

All Element UI component usages MUST be updated to Element Plus API equivalents across 332 Vue files.

#### Scenario: v-model sync migration

- **WHEN** a component previously used `.sync` modifier on Element props (e.g. `:visible.sync`)
- **THEN** it MUST use Vue 3 `v-model:propName` syntax with Element Plus compatible prop/event names

#### Scenario: Slot syntax migration

- **WHEN** a component used `slot-scope` or legacy named slot syntax
- **THEN** it MUST use Vue 3 `#slotName` shorthand without behavior change

#### Scenario: Message and notification APIs

- **WHEN** code calls `this.$message`, `this.$confirm`, `this.$notify`, or plugin equivalents
- **THEN** Element Plus message box and notification APIs MUST provide equivalent UX

#### Scenario: Form validation

- **WHEN** a form uses `el-form` with rules
- **THEN** validation, reset, and field-level error display MUST behave identically to the Element UI version

### Requirement: Legacy Vue API cleanup

The migration SHALL remove Vue 2-only APIs that Vue 3 no longer supports.

#### Scenario: Reactivity helpers removed

- **WHEN** components previously used `this.$set` or `this.$delete`
- **THEN** those calls MUST be replaced with direct reactive assignment (Vue 3 Proxy reactivity)

#### Scenario: Lifecycle hook rename

- **WHEN** components used `beforeDestroy` or `destroyed`
- **THEN** they MUST use `beforeUnmount` or `unmounted` respectively

#### Scenario: Filters removed

- **WHEN** components used Vue 2 filters
- **THEN** they MUST be replaced with computed properties or methods

