## ADDED Requirements

### Requirement: MCP settings are opened from the AI management card

The project option page SHALL NOT show a separate MCP card. The "AI management" card SHALL contain an "MCP access" link, visible to users with `system:project:query` in the project, which opens the MCP settings dialog. The AI management card MUST be shown whenever at least one of its links is visible.

#### Scenario: Member without AI permissions sees MCP link

- **WHEN** a project member with `system:project:query` but no AI model or AI account permission opens the project option page
- **THEN** the AI management card is shown with only the "MCP access" link

### Requirement: Module names follow the current language

The MCP settings dialog SHALL show each selectable module by its localized name in the current UI language (zh_CN, zh_TW, en, ru, ja, ko, ar), and MUST NOT show raw keys or object text.

#### Scenario: English UI

- **WHEN** the UI language is English and the dialog is opened
- **THEN** modules are labeled like "Defects", "Test cases", "Code repositories"

### Requirement: Client setup examples use OAuth login only

The MCP settings dialog SHALL show the MCP URL and per-client setup examples (Cursor, Claude Code, Codex, Claude Desktop / Web) that contain only the URL and the client's login step. The dialog MUST NOT show token placeholders, a link to generate tokens, or a "project reference" field, and SHALL state that the team and project are chosen in the browser during login.

#### Scenario: Cursor example

- **WHEN** a user opens the Cursor tab in the dialog
- **THEN** the example config contains the MCP URL without any `Authorization` header
