## ADDED Requirements

### Requirement: Preset project covers are original cover tiles

The system SHALL provide exactly ten preset project covers as `project_icon1.svg` through `project_icon10.svg`. Each file MUST be an original full-tile cover (colored background plus one simple subject), not a menu-style line glyph and not the previous full-scene illustrations. Subjects MUST be: 1 phone app, 2 open book, 3 calendar, 4 stacked papers, 5 boxes, 6 bar chart, 7 chat bubbles, 8 paper plane, 9 folder, 10 kanban columns.

#### Scenario: Preset files replace the illustrations

- **WHEN** the preset cover assets are built
- **THEN** `project_icon1.svg` through `project_icon10.svg` are self-drawn cover tiles of at most a few kilobytes each and contain neither the old scene illustrations nor 24px stroke-only menu icons

### Requirement: Selecting a preset stores the shared path

Choosing preset index N MUST set `project_icon` to `/static/img/project_iconN.svg`. The system MUST NOT copy that file into the per-project upload directory. Resolving a stored path that matches `project_iconN` (with or without a historical content hash) MUST display preset N.

#### Scenario: Create form default

- **WHEN** a user opens the create-project form
- **THEN** the preview shows preset 1 and the value to be saved is `/static/img/project_icon1.svg`

#### Scenario: Existing preset projects follow the new cover

- **WHEN** a project row stores `/static/img/project_icon3.svg` or `/static/img/project_icon3.<hash>.svg`
- **THEN** the nameplate and edit preview show preset 3, the calendar cover

#### Scenario: Custom upload is unchanged

- **WHEN** a project row stores a `/profile/upload/` image path
- **THEN** the nameplate and edit preview show that uploaded image and MUST NOT replace it with a preset cover

### Requirement: Cover picker and upload control are compact

On the create and edit project forms, the cover preview MUST be about 96 by 96 pixels. The preset picker cells and the upload control in that picker MUST be about 72 by 72 pixels. The upload control MUST remain the last cell. The picker popover MUST size to that grid rather than a 550px-wide layout with 130px cells.

#### Scenario: Change-cover popover

- **WHEN** the user opens the project cover picker
- **THEN** ten preset tiles and one upload control are shown at about 72 pixels each

#### Scenario: Custom upload still selects a file

- **WHEN** the user uses the upload cell and picks an image
- **THEN** `project_icon` becomes that upload URL and the preview shows the uploaded image at the compact preview size
