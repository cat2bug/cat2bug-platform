# project-mcp-settings Specification

## Requirements
### Requirement: Projects have an MCP switch and module selection

Each project SHALL have MCP settings consisting of an enabled flag and a list of selected modules. Selecting a module only allows MCP access to that module; read or write ability MUST still come from the user's project role.

#### Scenario: Admin disables a module

- **WHEN** a project administrator unchecks the "code" module and saves
- **THEN** MCP calls for that project can no longer use code-related tools or operations, while other modules keep working

#### Scenario: Settings require project settings permission

- **WHEN** a project member without project settings permission tries to change MCP settings
- **THEN** the change is rejected and nothing is saved

### Requirement: Default settings for new and existing projects

A newly created project SHALL get MCP enabled with every module selected that exists at creation time. A project without any MCP settings record MUST be treated as MCP disabled; upgrading the platform MUST NOT create settings for existing projects.

#### Scenario: New project is enabled by default

- **WHEN** a user creates a new project
- **THEN** its MCP settings show enabled with all modules selected

#### Scenario: Existing project stays disabled after upgrade

- **WHEN** the platform is upgraded and a project created before the upgrade is opened
- **THEN** its MCP settings show disabled, and MCP calls for that project are refused until an administrator enables it

### Requirement: Modules added later are not auto-selected

When a new module is added to the platform, projects that already have MCP settings MUST NOT get it selected automatically.

#### Scenario: New module appears unchecked

- **WHEN** a new module is introduced and an administrator opens an existing project's MCP settings
- **THEN** the new module is listed but unchecked

### Requirement: Only project-scoped modules are selectable

The module list SHALL contain only project-scoped modules mapped to permission prefixes. System-level functions (users, roles, departments, menus, dictionaries, parameters, posts, scheduled jobs, monitoring, team management) MUST NOT appear and MUST NOT be reachable through MCP.

#### Scenario: System-level operation is unreachable

- **WHEN** a system administrator's token is used to look for user management operations through MCP
- **THEN** no such operation is listed and direct calls to it are refused

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
