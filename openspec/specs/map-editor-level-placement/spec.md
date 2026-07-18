# map-editor-level-placement Specification

## Purpose

Defines level management and placement in the map editor: listing levels under the Levels top-level tab, stamping materials and objects onto levels with a consistent interaction, and per-instance property overrides of definition defaults.

## Requirements

### Requirement: Levels listed under the Levels top-level tab

The left Levels tab SHALL list project levels and allow adding and removing levels (subject to keeping at least one level). Selecting a level SHALL open or focus that level’s center document tab for map editing.

#### Scenario: Select level from list

- **WHEN** the user clicks a level in the left levels list
- **THEN** the center workspace SHALL show that level’s map editor as the current document

#### Scenario: Add level

- **WHEN** the user adds a level
- **THEN** a new level SHALL appear in the list and become the current level document

### Requirement: Stamp materials and objects onto levels with the same interaction

While a level document is active, the user SHALL be able to drag a material or an object from the left panel onto the level canvas to place a stamp. Click-to-stamp behavior SHALL remain available consistently for both sources where brush/stamp tools apply.

#### Scenario: Drag material onto level

- **WHEN** the user drags a material onto the level canvas and drops on a valid cell
- **THEN** the system SHALL place a material-backed instance at that location

#### Scenario: Drag object onto level

- **WHEN** the user drags an object onto the level canvas and drops on a valid cell
- **THEN** the system SHALL place an object-backed instance at that location using the object’s footprint and definition defaults

### Requirement: Instance properties may override definition defaults

Placed object instances on a level SHALL store per-instance property values that MAY differ from the object definition. Editing instance fields in the right panel SHALL update the instance without requiring a change to the definition.

#### Scenario: Override instance HP

- **WHEN** a character object definition has HP 100 and the user sets a placed instance HP to 150
- **THEN** that instance SHALL use HP 150 while the definition remains HP 100

#### Scenario: New placement uses definition defaults

- **WHEN** the user places a new instance of an object whose definition HP is 100
- **THEN** the new instance SHALL initialize HP to 100
