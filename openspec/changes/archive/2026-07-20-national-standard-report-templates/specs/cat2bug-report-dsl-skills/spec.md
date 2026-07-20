## ADDED Requirements

### Requirement: DSL skills document all supported token forms

The project SHALL provide AI-consumable skills under `readme/development/skills/cat2bug-report-dsl/` that document Cat2Bug report template Markdown DSL token forms supported by the product, including at least: simple variables `${api.<ns>.<field>}`, plan-indexed variables `${api.plan.<field>[<n>]}`, dynamic tables `$table{...}`, images `$image`/`$img`, and cards `$card{...}`, with parameters and field catalogs derived from the codebase (`grammar.js` and Cat2BugMarkdown plugins).

#### Scenario: Variable catalog matches grammar

- **WHEN** an agent looks up project or plan fields in the DSL skills
- **THEN** the listed fields MUST include those exported as `PROJECT_VARIABLE_FIELDS` and `PLAN_VARIABLE_FIELDS` (or an explicitly documented superseding source)

#### Scenario: Table column syntax documented

- **WHEN** an agent authors a defect or case list table token
- **THEN** the skill MUST describe `field` and `field:label` column specs and default column sets for defect/case tables

### Requirement: DSL skills forbid inventing unsupported tokens

DSL skills SHALL require that generated templates only use documented token forms and data sources; unsupported metrics or document sections MUST be static Markdown placeholders labeled for human fill-in.

#### Scenario: No fabricated API paths

- **WHEN** a default template needs content with no corresponding DSL variable
- **THEN** the template MUST NOT invent tokens such as `${api.unknown.xyz}` and MUST use clearly marked static placeholder text instead

### Requirement: Authoring rules for plan index and namespaces

DSL skills SHALL state that plan index `<n>` is 1-based, and that preferred namespace form is `api.<namespace>.<field>` consistent with existing `report-template-dsl` requirements.

#### Scenario: Plan index guidance

- **WHEN** an agent inserts a plan metric variable
- **THEN** the skill MUST show the `[n]` form and warn that index refers to the selected plan ordinal in report context
