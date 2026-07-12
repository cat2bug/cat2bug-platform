# frontend-vite-build Specification

## Purpose
TBD - created by archiving change vue3-vite-pinia-migration. Update Purpose after archive.
## Requirements
### Requirement: Vite development server parity

The frontend build system SHALL use Vite as the development and production bundler, preserving equivalent behavior to the current Vue CLI dev server.

#### Scenario: API proxy to backend

- **WHEN** the developer runs `npm run dev`
- **THEN** requests to `VUE_APP_BASE_API` MUST be proxied to `http://localhost:2020` with path rewrite

#### Scenario: WebSocket proxy

- **WHEN** the frontend opens a WebSocket via `VUE_APP_BASE_WEBSOCKET`
- **THEN** the Vite dev server MUST proxy WebSocket connections to the backend on port 2020

#### Scenario: File preview WebSocket proxy

- **WHEN** the frontend connects to `VUE_APP_FILE_VIEW`
- **THEN** the Vite dev server MUST proxy WebSocket connections to port 8012

#### Scenario: Docs static serving in development

- **WHEN** a developer navigates to `/docs/**` during local development
- **THEN** the server MUST serve Markdown static files from `readme/production` without falling back to SPA index.html

### Requirement: Production and embedded build outputs

The build system SHALL support all existing npm build modes with correct output destinations.

#### Scenario: Standard production build

- **WHEN** the developer runs `npm run build:prod` or `npm run build:cloud`
- **THEN** artifacts MUST be written to `dist/` with `publicPath` `/`

#### Scenario: Embedded Spring Boot build

- **WHEN** the developer runs `npm run build:embedded`
- **THEN** artifacts MUST be written to `cat2bug-platform-admin/src/main/resources/static/`

#### Scenario: Static assets directory

- **WHEN** any production build completes
- **THEN** JS/CSS/image assets MUST be placed under the directory specified by `VUE_APP_STATIC_PATH`

#### Scenario: Gzip compression

- **WHEN** a production build completes (non-analyze mode)
- **THEN** gzip-compressed copies of JS, CSS, and HTML files MUST be generated alongside originals

### Requirement: Build aliases and special handling

The Vite configuration SHALL preserve critical webpack aliases and build behaviors.

#### Scenario: Vue-multipane vendor alias

- **WHEN** code imports `vue-multipane`
- **THEN** the bundler MUST resolve to `src/vendor/vue-multipane/index.js` (SVG-safe patched build)

#### Scenario: Tool views excluded from production

- **WHEN** `VUE_APP_ENABLE_DEV_TOOLS` is not `true` and a production build runs
- **THEN** `views/tool/**` routes MUST NOT be included in the production bundle

#### Scenario: Docs copy to output

- **WHEN** a production build completes
- **THEN** `readme/production` documentation assets MUST be copied into the build output under `/docs`

#### Scenario: Bundle analysis

- **WHEN** the developer runs `npm run analyze`
- **THEN** a bundle analysis report MUST be generated without breaking the embedded output path

### Requirement: Environment variable compatibility

Existing `.env.*` files and `VUE_APP_*` variables SHALL remain usable without renaming during the migration.

#### Scenario: Development environment variables

- **WHEN** the app runs in development mode
- **THEN** all `VUE_APP_*` variables used by the current codebase MUST resolve correctly via Vite env loading

#### Scenario: LAN HMR mode

- **WHEN** `CAT2BUG_DEV_HMR=1` is set
- **THEN** the dev server MUST support reliable hot module replacement over LAN connections

