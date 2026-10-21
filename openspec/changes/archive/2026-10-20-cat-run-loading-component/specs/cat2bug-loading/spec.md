## ADDED Requirements

### Requirement: Brand loading component shows running cat and text

The system SHALL provide a Vue component `Cat2BugLoading` that displays a solid silhouette running-cat sprite animation and a loading text label beneath it. The default label MUST use the product i18n loading string (or an equivalent existing key); callers MAY override the text via a prop.

#### Scenario: Default render

- **WHEN** a page mounts `Cat2BugLoading` with default props
- **THEN** a single-cat viewport animation and the default loading text are visible

#### Scenario: Custom text

- **WHEN** a caller passes a custom `text` prop
- **THEN** that text is shown under the cat instead of the default label

### Requirement: Sprite animation uses six locked frames at 130ms

The running animation MUST use the approved six-frame horizontal sprite (source frames 0, 4, 5, 7, 8, 9) advanced with CSS `steps(6)` (or equivalent whole-frame stepping) at **130ms per frame** by default. The viewport width MUST keep the sprite cell aspect ratio (382∶280). Frame switching MUST NOT use a naive `0% → 100%` background-position technique that shows two frames at once.

#### Scenario: Whole-frame stepping

- **WHEN** the animation is playing at the default speed
- **THEN** each step shows exactly one full cat silhouette in the viewport without adjacent-frame bleed

#### Scenario: Aspect ratio across sizes

- **WHEN** the component height is set to 28px, 44px, or 56px
- **THEN** the cat is not horizontally stretched or squashed relative to the 382∶280 cell ratio

### Requirement: Dust and ground shadow match approved preview

The component SHALL render CSS dust particles trailing behind the cat (visible outside the clipped sprite viewport) and a **solid-color** elliptical ground shadow under the cat. The shadow size MUST change in sync with the six animation frames. On dark backgrounds, the shadow MUST remain visible (light-colored shade).

#### Scenario: Light background shadow and dust

- **WHEN** `Cat2BugLoading` is shown on a light surface
- **THEN** dark dust particles and a dark solid elliptical shadow are visible near the cat’s feet without sitting flush against the silhouette

#### Scenario: Dark background shadow

- **WHEN** `Cat2BugLoading` is shown on a dark surface
- **THEN** a light solid elliptical shadow remains visible under the cat

### Requirement: Unified loading integration for primary entry points

Primary product loading entry points (at least: configurable region `v-loading` used by major lists/tables, and fullscreen `ElLoading` / download wait used by shared request helpers) MUST present the brand running-cat loading (via custom spinner, wrapper, or mounted `Cat2BugLoading`) instead of the unmodified Element Plus default spinner alone.

#### Scenario: Region loading uses brand visual

- **WHEN** a P0 list or table enters its loading state
- **THEN** the user sees the running-cat loading treatment rather than only the default EP circle spinner

#### Scenario: Fullscreen loading uses brand visual

- **WHEN** a P0 fullscreen loading service is shown (e.g. download wait)
- **THEN** the overlay uses the running-cat loading treatment

### Requirement: Reduced motion falls back to a static frame

When the user agent indicates `prefers-reduced-motion: reduce`, the component MUST stop the running and dust animations and show a single static cat frame plus the text label.

#### Scenario: Reduced motion

- **WHEN** `prefers-reduced-motion: reduce` is active
- **THEN** the cat does not animate frame-by-frame and dust particles do not animate
