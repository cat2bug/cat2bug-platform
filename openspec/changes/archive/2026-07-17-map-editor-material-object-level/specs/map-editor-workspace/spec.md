## ADDED Requirements

### Requirement: Left panel has Material, Object, and Level top-level tabs

The map editor left panel SHALL provide three top-level tabs labeled for materials (素材), objects (对象), and levels (关卡). Only one top-level tab SHALL be active at a time.

#### Scenario: Switch top-level tab

- **WHEN** the user selects the Objects top-level tab
- **THEN** the left panel shows the objects library UI and hides the materials and levels list UIs

#### Scenario: Materials retain category tabs

- **WHEN** the Materials top-level tab is active
- **THEN** the editor SHALL show the existing category tabs for map/terrain, building, character, and item material groups

### Requirement: Center area uses document tabs for materials, objects, and levels

The center workspace SHALL show a tab strip of open documents. Each document SHALL be one of: material, object, or level. Activating a document SHALL show the corresponding editor/preview in the center and drive the right properties context.

#### Scenario: Open material as document

- **WHEN** the user activates a material from the left materials list
- **THEN** the system SHALL open a document tab for that material if not already open, make it current, and show a material preview in the center

#### Scenario: Open object as document

- **WHEN** the user activates an object from the left objects list
- **THEN** the system SHALL open a document tab for that object if not already open, make it current, and show the object footprint preview in the center

#### Scenario: Open level as document

- **WHEN** the user activates a level from the left levels list
- **THEN** the system SHALL open a document tab for that level if not already open, make it current, and show the level map canvas for editing

#### Scenario: Re-activate existing tab

- **WHEN** the user activates an item whose document tab is already open
- **THEN** the system SHALL focus that existing tab without creating a duplicate

#### Scenario: Close document tab does not delete entity

- **WHEN** the user closes a level, material, or object document tab
- **THEN** the underlying level/material/object SHALL remain in the project

### Requirement: Right properties follow the active document and selection

The right properties panel SHALL show editors appropriate to the active document kind and, when a level is active, to the current canvas selection (including empty/level props).

#### Scenario: Material document properties

- **WHEN** a material document is current and no conflicting multi-select applies
- **THEN** the right panel SHALL allow editing at least material name, background color, and replacing the image

#### Scenario: Object document properties

- **WHEN** an object document is current
- **THEN** the right panel SHALL allow editing that object’s footprint and kind-specific definition fields

#### Scenario: Level instance selection

- **WHEN** a level document is current and a placed instance is selected
- **THEN** the right panel SHALL show instance properties that can override definition defaults
