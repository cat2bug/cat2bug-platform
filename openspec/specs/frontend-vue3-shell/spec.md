# frontend-vue3-shell Specification

## Purpose
TBD - created by archiving change vue3-vite-pinia-migration. Update Purpose after archive.
## Requirements
### Requirement: Vue 3 application bootstrap

The frontend application SHALL boot using Vue 3 `createApp()` API with equivalent plugin registration to the current `main.js`.

#### Scenario: Application mount

- **WHEN** the SPA loads in the browser
- **THEN** the app MUST mount to `#app` and render the root `App` component without console errors

#### Scenario: Global methods

- **WHEN** any component calls global helpers (`getDicts`, `parseTime`, `resetForm`, `download`, etc.)
- **THEN** those methods MUST be available via `app.config.globalProperties` with the same signatures

#### Scenario: Global component registration

- **WHEN** any page uses globally registered components (`Pagination`, `DictTag`, `FileUpload`, `CommandPalette`, etc.)
- **THEN** those components MUST render without explicit local imports

### Requirement: Vue Router 4 integration

The routing system SHALL migrate to vue-router 4 while preserving current navigation, permission, and keep-alive behavior.

#### Scenario: History mode routing

- **WHEN** the user navigates between pages
- **THEN** vue-router 4 MUST use history mode with the same route definitions and lazy-loading patterns

#### Scenario: Permission-gated routes

- **WHEN** a user without required permissions attempts to access a protected route
- **THEN** the router guard MUST redirect or deny access identically to the current behavior

#### Scenario: Dynamic route generation

- **WHEN** the user logs in or switches current project
- **THEN** dynamic routes MUST regenerate and the sidebar MUST reflect updated menus after `SwitchCurrentProject` completes

#### Scenario: TagsView keep-alive

- **WHEN** the user opens multiple tagged pages
- **THEN** `keep-alive` caching via `tagsView` store MUST preserve visited/cached view state

### Requirement: Pinia state management

Application state SHALL migrate from Vuex 3 (6 modules) to Pinia stores with equivalent state shape and actions.

#### Scenario: Store module parity

- **WHEN** the migration completes
- **THEN** Pinia stores MUST exist for: `app`, `user`, `permission`, `tagsView`, `settings`, `dict`

#### Scenario: User authentication flow

- **WHEN** the user logs in with valid credentials
- **THEN** token, roles, permissions, and user profile MUST be stored and accessible to route guards and components

#### Scenario: Project switch orchestration

- **WHEN** the user switches current project via `SwitchCurrentProject`
- **THEN** the system MUST: set routes refreshing flag → persist config → refresh user info → regenerate routes → clear refreshing flag

#### Scenario: Getter compatibility

- **WHEN** components previously used `mapGetters` for sidebar, token, permissions, theme, etc.
- **THEN** equivalent reactive state MUST be available from Pinia stores without behavior change

### Requirement: vue-i18n 9 with seven locales

Internationalization SHALL migrate to vue-i18n 9 supporting all current languages with merged Element and custom messages.

#### Scenario: Supported locales

- **WHEN** the user selects or the system resolves a locale
- **THEN** the app MUST support: `zh_CN`, `zh_TW`, `en_US`, `ar`, `ja_JP`, `ko_KR`, `ru_RU`

#### Scenario: Element Plus locale sync

- **WHEN** the active locale changes
- **THEN** Element Plus component locale MUST update to match the selected language

#### Scenario: Accept-Language propagation

- **WHEN** the frontend sends API requests
- **THEN** the `Accept-Language` header MUST continue to reflect the active locale

### Requirement: WebSocket connection

Real-time messaging SHALL use a Vue 3-compatible WebSocket wrapper replacing `vue-native-websocket`.

#### Scenario: WebSocket connect on login

- **WHEN** the user is authenticated with a valid member ID
- **THEN** a WebSocket connection MUST be established to `/websocket/{memberId}/message` with reconnection (5 attempts, 3s delay)

#### Scenario: Manual connect mode

- **WHEN** the WebSocket plugin initializes
- **THEN** connection MUST NOT auto-start until explicitly connected (equivalent to current `connectManually: true`)

