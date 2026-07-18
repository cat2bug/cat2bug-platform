## Why

The map editor currently mixes image assets, typed gameplay properties, and level editing in one flat left-panel taxonomy. Character stats and FSM live on placed instances, while “元素” naming/background color only exist in the import dialog. Authors need a clear split between **materials (images)**, **reusable object definitions**, and **levels**, with a document-tab workspace so they can open and edit any of those without losing context.

## What Changes

- Restructure the left panel into three top-level tabs: **素材** (materials), **对象** (objects), **关卡** (levels).
- Keep the existing material category tabs (地图 / 建筑 / 人物 / 物品) under **素材**; terrain stays materials. Materials are image-only: rename, background color, replace image in the right properties panel.
- Add an **对象** library with sub-tabs 建筑 / 角色 / 物品: create/delete definitions; each has a manually set footprint (tile grid); objects **reference** materials and may **reference other objects** (DAG, no cycles); parts must fit inside the parent footprint.
- Character objects gain sprite slots: `down` / `up` / `side` / `attack` / `death`. Each slot is 1..N material frames (animation). Idle/walk are **not** split: play frames while moving, hold frame 0 when idle. `side` art faces **right**; left uses horizontal flip.
- Change the center top tabs from “levels only” to a **document workspace**: clicking a material, object, or level opens (or focuses) a tab and makes it current. Object tabs show a footprint-sized preview (e.g. 16×16px → one cell at `TILE_SIZE`).
- Levels remain listed under the left **关卡** tab; editing happens in the center document tab. Both materials and objects can be dragged onto a level with the same stamp semantics. Placed instances may **override** definition defaults (e.g. HP).
- Persist object definitions in the game package / project model; migrate or adapt existing `assetLibrary`, level tabs, composites, and instance-bound character props toward definition-first data (instance overrides retained).

## Capabilities

### New Capabilities

- `map-editor-workspace`: Three-pane IA (素材 / 对象 / 关卡), center document tabs for material/object/level, context-sensitive right properties.
- `map-editor-object-definitions`: Object library (building/character/item), footprint, element/object references, character sprite slots and playback rules.
- `map-editor-level-placement`: Level list UX, stamp materials or objects onto levels, instance property overrides.

### Modified Capabilities

- (none — no existing OpenSpec capabilities cover the game map editor)

## Impact

- **Primary UI**: `cat2bug-feature-team-game-engine` worktree — `GameMapEditor.vue`, `MapEditorCanvas.vue`, asset library / levels / character / composites / FSM modules under `components/Game/map-editor/`.
- **Project model & package**: root model shape (`assetLibrary`, new `objectLibrary` or equivalent), ZIP game-package import/export, possibly composites evolution.
- **Runtime playtest / Phaser draw path**: facing flip for `side`, multi-frame animation from character slots (may start editor-only then wire playtest).
- **i18n**: new left-tab and property strings.
- **No backend API change expected** if packages remain file-based; revisit if server-side game catalog must store object defs separately.
