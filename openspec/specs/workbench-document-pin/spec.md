# workbench-document-pin Specification

## Requirements

### Requirement: Document pin widget type

The workbench widget catalog SHALL include a widget type `document-pin` that binds to exactly one project document via `props.docId`. Only Markdown documents (`fileExtension` of `md` or `markdown`) MAY be selected. The widget MUST support multiple instances on the same workbench with different `docId` values.

#### Scenario: Place document pin

- **WHEN** a member places a `document-pin` widget in a column and selects a Markdown document
- **THEN** the column widget `type` is `document-pin`
- **THEN** `props.docId` equals the selected document id

#### Scenario: Reject non-markdown

- **WHEN** a member attempts to bind a non-Markdown document to `document-pin`
- **THEN** the editor refuses the selection
- **THEN** `props.docId` is not set to that document

### Requirement: Card presentation in the column

In view mode, `document-pin` MUST render the bound document's Markdown **content** as a read-only preview inside the column (title/meta MAY appear in a compact header). The widget MUST NOT embed a full Markdown editor; activating the preview MUST still navigate to `HandleMarkdownDocument`.

#### Scenario: Show bound document content

- **WHEN** the bound document exists and the member can read it
- **THEN** the column shows a read-only Markdown preview of that document's body
- **THEN** a title (or equivalent identity) remains visible in the header

#### Scenario: Missing or unauthorized document

- **WHEN** the bound document is missing or the member cannot access it
- **THEN** the column shows an empty or error state that allows re-selecting a document in edit mode

### Requirement: Open preview or edit by permission

Activating (clicking) the document pin card MUST navigate to the existing Markdown document handler page for that `docId`. If the member has `system:document:edit`, the page MUST allow editing; otherwise the page MUST open in preview (read-only) mode.

#### Scenario: Member with edit permission

- **WHEN** a member with `system:document:edit` clicks a document pin card
- **THEN** the Markdown document page opens for that `docId` in editable mode

#### Scenario: Member without edit permission

- **WHEN** a member without `system:document:edit` clicks a document pin card
- **THEN** the Markdown document page opens for that `docId` in preview mode
