## Requirements

### Requirement: Taxonomy maps standard documents to template shapes

The project SHALL provide a taxonomy document under `readme/development/skills/report-templates/` that maps GB/T 38634.3 / ISO 29119-3 documentation information items to recommended Cat2Bug template shapes: standalone template, combined document template, appendix block, or structure-only skeleton—**according to the standard**, not a blanket one-file-per-type rule.

#### Scenario: Completion report sections stay coherent

- **WHEN** taxonomy describes the test completion report
- **THEN** it MUST list the standard’s required information elements as sections of that report template (or explicitly allowed appendices), rather than scattering mandatory elements across unrelated templates without rationale

#### Scenario: Tailoring is explicit

- **WHEN** a default offering merges or omits an information item
- **THEN** the taxonomy MUST cite tailored conformance (or equivalent standard allowance) and state what was merged or omitted

### Requirement: Default templates are valid Cat2Bug Markdown DSL

Default report templates SHALL be UTF-8 Markdown files whose embedded DSL tokens are valid per `report-template-dsl` and the cat2bug-report-dsl skills; they MUST be previewable with existing report rendering.

#### Scenario: Default template parses

- **WHEN** a default template file is parsed with the project’s DSL parse utilities
- **THEN** parsing MUST succeed without unknown token kinds invented for this change

### Requirement: Default set covers primary reporting scenarios

The default template set SHALL cover the primary standard reporting scenarios needed for Cat2Bug users, including at least: test status reporting, test completion reporting, and defect/incident listing appropriate to the product—each shaped per taxonomy. Additional standard document types MAY be provided as skeletons when DSL coverage is insufficient.

#### Scenario: Status and completion defaults exist

- **WHEN** an agent lists `readme/development/skills/report-templates/defaults/`
- **THEN** files (or clearly named equivalents) for test status and test completion oriented templates MUST be present

#### Scenario: Skeleton for low-DSL documents

- **WHEN** a standard document type cannot be substantially filled by existing variables
- **THEN** a skeleton template MAY be provided with static section headings from the standard and minimal or no dynamic tokens
