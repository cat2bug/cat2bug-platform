## ADDED Requirements

### Requirement: Create entry uses unified multi-mode dialog

The system SHALL open the same unified create-or-import dialog for repository creation from both the project repository list “new” action and the in-browse repository switcher add action. Empty-repository creation MUST remain available as one mode of that dialog and MUST continue to require repository manage permission.

#### Scenario: List new opens unified dialog

- **WHEN** a manage user clicks new repository on the project repository list
- **THEN** the unified dialog opens with mode switching including empty and import modes

#### Scenario: Switcher add opens unified dialog

- **WHEN** a manage user clicks add in the repository switcher popover
- **THEN** the same unified dialog capability opens (not a divergent create-only form)

#### Scenario: Non-manage user cannot open create

- **WHEN** a user without repository manage permission uses the UI
- **THEN** the new/add create controls are not offered (or the operation is denied)
