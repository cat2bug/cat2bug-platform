### Requirement: Defect tab row full-width bottom border

The defect list page tab row (`.defect-tools-tab`) SHALL render a single continuous 1px bottom border across the full width of the row, including the area under the right-side statistic tool button (`.defect-tools-tab-right`), so the tab bar visually separates from the filter toolbar below.

#### Scenario: Tab row border spans statistic button

- **WHEN** the user views the defect list page with the statistic panel toggle visible
- **THEN** the bottom border under the tab labels continues uninterrupted under the right-side statistic icon

#### Scenario: No double border under tabs

- **WHEN** the full-width border is applied on `.defect-tools-tab`
- **THEN** the Element UI default `.el-tabs__nav-wrap::after` line MUST NOT produce a second border under the tab labels only
