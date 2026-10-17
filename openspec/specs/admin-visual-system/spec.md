# admin-visual-system Specification

## Purpose

统一管理端浅色与暗色的品牌蓝，登录页保留猫与老鼠，缺陷列表靠 token 继承颜色。

## Requirements

### Requirement: Shared brand blue

The admin UI SHALL use one brand blue for both light and dark themes. The filled brand color MUST be `#1890ff`. Amber `#FFC107` MUST NOT be used as a brand or hover color. Success, warning, and danger colors MUST stay unchanged, and warning yellow remains a status color only.

#### Scenario: Light theme brand color

- **WHEN** the document root does not have the `dark` class
- **THEN** `--cat2bug-primary` and the Element Plus primary color MUST both resolve to `#1890ff`

#### Scenario: Dark theme brand color

- **WHEN** the document root has the `dark` class
- **THEN** filled buttons and the brand token MUST still use `#1890ff`, and links plus keyboard focus rings MUST use `#69b1ff` so they remain readable on `#252529`

### Requirement: Shell uses the same active color

The sidebar, top navbar, and tags view SHALL take their surface colors and active color from the shared theme tokens. Control height MUST remain 32px and border radius MUST remain 4px. In dark mode, tab labels and sidebar current items MUST use the same `#1890ff` as filled buttons. Tab icons MUST follow the tab text color.

#### Scenario: Active navigation

- **WHEN** a sidebar item or tag is active in either theme
- **THEN** its emphasis color MUST be the shared brand blue, not amber

### Requirement: Login keeps its character

The login page SHALL keep the cat scene (light and dark images), the Z bubbles, the running mouse and its direction controls, the pixel captcha, the language icons, and the existing keyboard hints. This change MUST only recolor buttons, links, and focus to the shared brand blue.

#### Scenario: Login still shows the cat and the mouse

- **WHEN** a user opens the login page in either theme
- **THEN** the cat scene for that theme is visible, the mouse runner is still present, and the login button uses `#1890ff`

### Requirement: Defect list inherits tokens

The defect list SHALL pick up the shared colors through existing tokens and components. This change MUST NOT rewrite `Cat2BugTable` column handles, pin, resize, or sort behavior.

#### Scenario: Defect list after theme change

- **WHEN** a user opens the defect list in either theme
- **THEN** primary actions use the shared brand blue and the table header handles still behave as specified in `cat2bug-table-header-handles`

### Requirement: No new visual asset packs

This change MUST NOT add an icon font, a second icon library, a webfont, an illustration pack, or a second UI component library.

#### Scenario: Production assets

- **WHEN** the frontend production build finishes
- **THEN** the bundle MUST NOT contain a newly added icon font or UI webfont introduced by this change
