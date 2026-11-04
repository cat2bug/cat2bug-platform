# navbar-feedback-mailto Specification

## Purpose
TBD - created by archiving change navbar-feedback-mailto. Update Purpose after archive.
## Requirements
### Requirement: Top navbar exposes a feedback mailto action

The system SHALL provide a top-navbar icon action that lets a signed-in user open a mail compose flow addressed to `service@cat2bug.com` for product suggestions. The action MUST use the same visual pattern as existing navbar external entries (icon + tooltip) and MUST NOT introduce a server-side send-mail API for this capability.

#### Scenario: Desktop user opens feedback mail

- **WHEN** a signed-in user on a non-mobile layout clicks the feedback (suggest) icon in the top-right navbar
- **THEN** the browser opens a `mailto:` URL whose recipient is `service@cat2bug.com`
- **AND** the mailto includes a prefilled subject that identifies the message as a Cat2Bug product suggestion

#### Scenario: Tooltip describes the action

- **WHEN** the user hovers the feedback icon
- **THEN** the UI shows a localized tooltip that communicates the purpose (suggest / feedback by email)

### Requirement: Mailto includes helpful readonly context

When constructing the `mailto:` URL, the system SHALL prefill a body that includes readonly product/environment context available on the client (at minimum: product name or app identifier, and UI locale). The body MUST leave clear space for the user to write their suggestion. The system MUST NOT require the user to fill a form before opening mail.

#### Scenario: Body contains context and writing space

- **WHEN** the user activates the feedback icon
- **THEN** the mailto body contains environment context lines and an empty area or prompt for the user’s suggestion text

### Requirement: Feedback entry visibility matches peer navbar tools

On layouts where peer external navbar tools (website / source / docs) are shown, the feedback icon MUST also be shown. On layouts where those peer tools are hidden (current mobile right-menu pattern), the feedback icon MUST follow the same visibility rule unless a later change explicitly adds a mobile-specific entry.

#### Scenario: Hidden with peer tools on mobile

- **WHEN** the layout device mode hides the desktop right-menu tool group that contains website/docs
- **THEN** the feedback icon is hidden with that group

