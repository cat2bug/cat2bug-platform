## ADDED Requirements

### Requirement: code-tabs Markdown container

The documentation renderer SHALL support a `::: code-tabs` / `:::` Markdown container that wraps one or more fenced code blocks and renders them as a single tabbed code example region.

#### Scenario: Container renders tabbed panels

- **WHEN** a Markdown document contains a valid `::: code-tabs` block with multiple fenced code blocks
- **THEN** the rendered HTML SHALL display one tab strip and one code panel per language
- **AND** only one panel SHALL be visible at a time until the user selects another tab

### Requirement: Supported language tabs

Each `code-tabs` region SHALL include exactly six code panels with titles, in order: `cURL`, `Java`, `Python`, `Node.js`, `PHP`, `C#`.

#### Scenario: All six languages present

- **WHEN** an API endpoint section includes a `code-tabs` block
- **THEN** the block SHALL contain fenced code for all six titles listed above
- **AND** missing any one language SHALL fail documentation validation

### Requirement: Syntax highlighting

The documentation renderer SHALL apply syntax highlighting to code inside `code-tabs` panels using the project's highlight.js integration, mapped from fence language identifiers (`bash`, `java`, `python`, `javascript`, `php`, `csharp`).

#### Scenario: Highlighted code display

- **WHEN** a `code-tabs` panel is visible
- **THEN** its code content SHALL be rendered with language-appropriate highlighting
- **AND** inline styles or classes SHALL not break existing `.markdown-body` layout

### Requirement: Line numbers on code panels

Each visible code panel in `code-tabs` SHALL display line numbers alongside the code lines.

#### Scenario: Line numbers visible

- **WHEN** a user views any tab in a `code-tabs` region
- **THEN** each line of code SHALL have a corresponding line number indicator
- **AND** line numbers SHALL remain aligned when the panel scrolls horizontally for long lines

### Requirement: Placeholder substitution in reader UI

The documentation reader MAY provide inputs for API base URL and API key that replace `${baseUrl}` and `${apiKey}` in rendered `code-tabs` content on the current page without mutating source Markdown files.

#### Scenario: User applies deployment values

- **WHEN** the user enters a base URL and API key and activates substitution
- **THEN** all `${baseUrl}` and `${apiKey}` occurrences in visible code examples on the page SHALL reflect the entered values
