## ADDED Requirements

### Requirement: Object library with building, character, and item kinds

The project SHALL maintain a shared object library. The left Objects panel SHALL provide sub-tabs for building, character, and item. Users SHALL be able to add and delete objects in the active kind.

#### Scenario: Create character object

- **WHEN** the user adds an object while the character sub-tab is active
- **THEN** the system SHALL create a character-kind object definition in the object library and select it

#### Scenario: Delete object

- **WHEN** the user deletes an object definition
- **THEN** the definition SHALL be removed from the object library

### Requirement: Manual footprint for object preview

Each object definition SHALL store a manual footprint in tile width and height. The object document center view SHALL preview a grid of that footprint using the editor tile size (16px per tile). A footprint of 1×1 SHALL display a single cell.

#### Scenario: Preview one-tile character

- **WHEN** an object has footprint tileW=1 and tileH=1 (e.g. 16×16px content)
- **THEN** the object preview SHALL show exactly one tile cell

#### Scenario: Change footprint

- **WHEN** the user sets an object’s footprint to tileW=2 and tileH=2
- **THEN** the object preview SHALL show a 2×2 tile grid

### Requirement: Objects reference materials and other objects

An object MAY include parts that reference a material (element) or another object, each with a relative column and row inside the parent footprint. Object-to-object references MUST form a DAG (no cycles). Every part’s placement MUST lie entirely within the parent’s manual footprint.

#### Scenario: Reference material as part

- **WHEN** the user adds a part that references a material at relative col/row inside the footprint
- **THEN** the object preview SHALL include that material’s image at the relative cell

#### Scenario: Reference nested object

- **WHEN** the user adds a part that references another object within the footprint bounds
- **THEN** the object preview SHALL compose the referenced object’s visual within the parent grid

#### Scenario: Reject cyclic reference

- **WHEN** the user attempts to create an object reference cycle
- **THEN** the system SHALL reject the change and keep the previous valid reference graph

#### Scenario: Reject part outside footprint

- **WHEN** the user places a part that extends outside the parent tileW×tileH footprint
- **THEN** the system SHALL reject the placement

### Requirement: Character sprite slots and playback rules

Character objects SHALL support sprite slots: down, up, side, attack, and death. Each slot SHALL be an ordered list of one or more material (element) frame references forming an animation. The editor SHALL NOT require separate idle and walk slots. The side slot art SHALL face right; facing left SHALL use horizontal mirroring of side frames.

#### Scenario: Configure multi-frame walk-facing-down

- **WHEN** the user assigns multiple materials to the down slot
- **THEN** those frames SHALL be stored in order as the down animation

#### Scenario: Idle uses first frame

- **WHEN** a character is facing down and not moving in preview/playtest draw rules
- **THEN** the system SHALL display down slot frame index 0

#### Scenario: Moving loops direction frames

- **WHEN** a character is moving while facing down
- **THEN** the system SHALL loop the down slot frames

#### Scenario: Left uses side mirrored

- **WHEN** a character faces left
- **THEN** the system SHALL draw the side slot frames mirrored horizontally

#### Scenario: Right uses side unflipped

- **WHEN** a character faces right
- **THEN** the system SHALL draw the side slot frames without horizontal mirroring

### Requirement: Kind-specific definition properties

Character object definitions SHALL support configuring definition-level properties including at least display name, hit points, and state-machine binding. Building and item definitions SHALL support their kind-appropriate definition fields exposed in the right panel.

#### Scenario: Edit character definition HP

- **WHEN** the user sets HP on a character object definition
- **THEN** the value SHALL persist on the definition and apply as the default for newly placed instances of that object
