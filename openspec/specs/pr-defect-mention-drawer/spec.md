# pr-defect-mention-drawer Specification

## Requirements
### Requirement: Clickable defect mentions on PR detail

On the pull request detail page, the system SHALL treat tokens of the form `#` followed by one or more digits (project-scoped defect number) in the PR title and in the non-code-fence portions of the PR description as clickable defect mentions. Mentions inside fenced code blocks MUST remain plain text.

#### Scenario: Title mention is clickable

- **WHEN** a PR title contains `#61` and the user can view the PR
- **THEN** `#61` is presented as a control the user can activate

#### Scenario: Description mention is clickable

- **WHEN** a PR body contains `修复缺陷 #61` outside a code fence
- **THEN** `#61` is clickable in the rendered description

#### Scenario: Code fence not linked

- **WHEN** a PR body contains a fenced code block with the text `#61`
- **THEN** that occurrence is not turned into a defect link

### Requirement: Mention opens defect in a right-side drawer

Activating a defect mention SHALL open the defect for the current repository's project with that `projectNum` in a right-side drawer without navigating away from the PR detail route. The drawer SHOULD reuse the existing defect edit drawer. If the defect cannot be found or the user lacks permission to load it, the system MUST show a non-blocking warning and MUST NOT open an empty drawer.

#### Scenario: Existing defect opens drawer

- **WHEN** the user activates `#61` and defect project number 61 exists in the current project and the user may load it
- **THEN** a right-side drawer opens showing that defect while the PR detail remains mounted

#### Scenario: Missing defect warns

- **WHEN** the user activates `#99999` and no such defect exists in the current project
- **THEN** the UI shows a warning and does not open a defect drawer

#### Scenario: No permission warns

- **WHEN** the user activates a mention but cannot load the defect due to permission
- **THEN** the UI shows a warning and does not open a defect drawer
