## ADDED Requirements

### Requirement: Text avatar displays a single initial character

When a member has no avatar image, the system SHALL display exactly one character inside the avatar circle derived from the member display name.

The display name SHALL be resolved in order: `nickName`, then `userName`, then `name`.

For display names whose first Unicode character is a Latin letter (A–Z, a–z), the system SHALL show that letter uppercased.

For all other first characters (including CJK), the system SHALL show that character unchanged.

When the display name is missing or blank after trimming, the system SHALL show `?`.

When the member has a non-empty `avatar` or `avatarUrl`, the system SHALL show the image and SHALL NOT show the text initial.

#### Scenario: Chinese display name without photo

- **WHEN** a member has no `avatar` and no `avatarUrl` and display name is `张三`
- **THEN** the avatar shows the single character `张`

#### Scenario: Latin display name without photo

- **WHEN** a member has no `avatar` and no `avatarUrl` and display name is `dev`
- **THEN** the avatar shows the single character `D`

#### Scenario: Member with photo

- **WHEN** a member has a valid `avatar` or `avatarUrl`
- **THEN** the avatar shows the image and does not show a text initial

### Requirement: Text avatar uses palette-based background with white text

When showing a text initial (no image), the system SHALL apply a background color from a fixed palette of at least eight colors and SHALL render the initial in white (`#FFFFFF`) with bold font weight.

The palette index SHALL be computed as `hash(colorKey) % palette.length`, where `hash` is a deterministic string hash function.

The `colorKey` SHALL be the first character of the trimmed display name; Latin letters in `colorKey` SHALL be normalized to lowercase before hashing.

The system SHALL NOT use a pinyin library or backend field for color selection.

The system SHALL NOT use light/pastel-only styling (e.g. high-lightness HSL backgrounds with dark text) as the primary text-avatar scheme.

#### Scenario: Stable color for same initial

- **WHEN** two renders use the same `colorKey` (e.g. `d` from `dev`)
- **THEN** both avatars use the same background color from the palette

#### Scenario: White bold text on colored background

- **WHEN** a text initial is shown
- **THEN** the initial text color is `#FFFFFF`, the font weight is bold, and the background is one of the fixed palette colors

### Requirement: Statistics placeholder keeps neutral styling

When `member.isStatistics` is true, the system SHALL NOT apply the colorful palette.

The avatar SHALL retain neutral gray background and gray text styling suitable for aggregate/statistics placeholders.

#### Scenario: Statistics member avatar

- **WHEN** `member.isStatistics` is true and there is no photo
- **THEN** the avatar does not use a palette color and uses the existing statistics gray styling

### Requirement: Cat2BugAvatar is the single integration point

Member text avatar behavior SHALL be implemented in `Cat2BugAvatar` with shared helpers in `utils/member-avatar.js`.

Consumers such as `RowListMember` SHALL continue to pass `member` objects without requiring API changes.

#### Scenario: Defect list assignee column

- **WHEN** the defect table renders `handleByList` via `RowListMember`
- **THEN** each assignee without a photo shows a single-character text avatar with palette styling via `Cat2BugAvatar`
