## ADDED Requirements

### Requirement: Users can annotate report body by line or selection
For a saved report detail view, a user who can view the report SHALL be able to create an annotation on a whole block/line or on a text selection in the rendered body, including ordinary prose (not only tables). Each annotation SHALL store a quote snapshot and surrounding context for later rematching. Annotations SHALL use module type `report` correlated to the report id.

#### Scenario: Selection annotation
- **WHEN** the user selects text in the report body and confirms add comment
- **THEN** the system creates a root annotation with `anchor_kind=selection`, the selected quote, and context snapshots, and shows it in the right panel

#### Scenario: Line annotation
- **WHEN** the user adds a comment for a block/line without a text selection
- **THEN** the system creates a root annotation with `anchor_kind=line` using that block’s text as the quote

#### Scenario: Not available in template editor
- **WHEN** the user is editing a report template
- **THEN** the system does not offer report body annotation actions from this capability

### Requirement: Flat replies on annotation threads
The system SHALL support flat replies under a root annotation (`parent_id` points to the root). Nested replies-to-replies SHALL NOT be required. The right panel SHALL list root threads and their flat replies.

#### Scenario: Reply to root annotation
- **WHEN** the user submits a reply on a root annotation
- **THEN** the reply appears under that thread as a sibling of other replies (not nested deeper)

### Requirement: Anchor rematch after body edits
Because authorized users may edit report markdown, the system SHALL attempt to locate each annotation’s quote (using context when needed) in the current rendered body when the detail view loads or the body changes. Matched annotations SHALL be highlightable and focusable from the comments panel. Unmatched annotations SHALL remain readable in the comments panel and SHALL be marked as original text changed (orphaned). The system SHALL NOT auto-delete annotations when rematch fails.

#### Scenario: Quote still present
- **WHEN** the report body still contains the annotation quote (disambiguated by context if needed)
- **THEN** focusing the thread highlights the matched location in the center pane

#### Scenario: Quote missing after edit
- **WHEN** the quote can no longer be found in the body
- **THEN** the thread remains listed, is marked as original text changed, and does not silently highlight an unrelated location

### Requirement: Bidirectional navigation between body and comments
The system SHALL let the user open an annotation from a body highlight or gutter affordance into the comments panel, and from a comments thread back to the matched body location when the anchor is matched.

#### Scenario: Click thread with matched anchor
- **WHEN** the user clicks a matched thread in the comments panel
- **THEN** the center pane scrolls to and highlights the matched quote

#### Scenario: Click orphaned thread
- **WHEN** the user clicks an orphaned thread
- **THEN** the panel focuses that thread and does not claim a false body highlight

### Requirement: Annotation delete authorization
The system SHALL allow deleting an annotation or reply when the current user is the creator of that comment, or a project administrator of the report’s project, or a team administrator of the report’s team. Deleting a root annotation SHALL remove its flat replies as well. Unauthorized users SHALL NOT be able to delete others’ annotations.

#### Scenario: Creator deletes own reply
- **WHEN** the creator of a reply requests delete
- **THEN** the system deletes that reply

#### Scenario: Project or team admin deletes root thread
- **WHEN** a project administrator or team administrator deletes a root annotation they did not create
- **THEN** the system deletes the root annotation and its flat replies

#### Scenario: Unauthorized delete rejected
- **WHEN** a user who is neither the creator nor a project/team admin attempts to delete another user’s annotation
- **THEN** the system rejects the delete and the annotation remains

### Requirement: No notification side effects
Creating, replying to, or deleting report annotations SHALL NOT require @-mentions or push/inbox notifications as part of this capability.

#### Scenario: Create annotation without notify
- **WHEN** a user creates a report annotation
- **THEN** the annotation is stored and shown in the detail UI without requiring a notification send
