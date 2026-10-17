## MODIFIED Requirements

### Requirement: Theme and SCSS variable migration

Element Plus theming SHALL replace Element UI SCSS variable system. Light and dark themes MUST share one brand blue `#1890ff` via `--cat2bug-primary` and the Element Plus primary color. Dark theme links and keyboard focus rings MUST use `#69b1ff`. Amber MUST NOT be the brand color. Dark mode (`themeMode`) and table styles MUST remain in effect.

#### Scenario: Theme variables

- **WHEN** the app loads styles
- **THEN** Cat2Bug custom SCSS (`cat2bug.scss`, `element-variables` equivalent) MUST apply the shared brand blue, table styles, and dark mode (`themeMode`) behavior, with `#69b1ff` used for dark-theme links and focus rings

#### Scenario: Custom table scrollbar

- **WHEN** tables with fixed columns render
- **THEN** custom scrollbar width alignment (current `element-scrollbar-width` alias behavior) MUST be preserved so fixed columns do not cover the scrollbar
