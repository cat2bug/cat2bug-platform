## MODIFIED Requirements

### Requirement: Production and embedded build outputs

The build system SHALL support all existing npm build modes with correct output destinations.

#### Scenario: Standard production build

- **WHEN** the developer runs `npm run build:prod` or `npm run build:cloud`
- **THEN** artifacts MUST be written to `dist/` with `publicPath` `/`

#### Scenario: Embedded backend build

- **WHEN** the developer runs `npm run build:embedded`
- **THEN** artifacts MUST be written to `cat2bug-platform-admin/src/main/resources/META-INF/resources/`
- **AND** no separate `build:embedded-quarkus` script or `vue.config.js` exists

#### Scenario: Static assets directory

- **WHEN** any production build completes
- **THEN** JS/CSS/image assets MUST be placed under the directory specified by `VUE_APP_STATIC_PATH`

#### Scenario: Gzip compression

- **WHEN** a production build completes (non-analyze mode)
- **THEN** gzip-compressed copies of JS, CSS, and HTML files MUST be generated alongside originals
