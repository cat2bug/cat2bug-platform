## 1. Data model & migration

- [x] 1.1 Add `objectLibrary` helpers (create/normalize/find/remove, kind filters) beside asset library modules
- [x] 1.2 Define ObjectDef schema (footprint, parts, character props, sprite slots) and validators (cycle check, part-in-bounds, nest depth cap)
- [x] 1.3 Ensure load path initializes empty `objectLibrary` for legacy packages; preserve `assetLibrary` + levels
- [x] 1.4 Persist `objectLibrary` in game package export/import

## 2. Workspace shell (left / center / right)

- [x] 2.1 Replace left single resource strip with top-level tabs: 素材 / 对象 / 关卡
- [x] 2.2 Keep existing material category tabs under 素材; wire list to `assetLibrary` only
- [x] 2.3 Implement center document-tab state (open/focus/close) for material, object, and level kinds
- [x] 2.4 Route center canvas/preview by active document kind; keep level editing on MapEditorCanvas
- [x] 2.5 Move level list UX into left 关卡 tab; remove level-only semantics from being the sole center tab source
- [x] 2.6 Add i18n keys for new shell labels

## 3. Materials (元素) editing

- [x] 3.1 Material document + right panel: edit name, background color, replace image
- [x] 3.2 Keep penetrable mask editing for materials
- [x] 3.3 Ensure import/slice dialog still creates materials into categorized `assetLibrary`

## 4. Object library & preview

- [x] 4.1 Left 对象 panel: sub-tabs 建筑 / 角色 / 物品 with add/delete
- [x] 4.2 Object document center: render manual `tileW×tileH` footprint preview at TILE_SIZE
- [x] 4.3 Right panel: edit footprint and kind-specific definition props (character: name, HP, FSM binding, etc.)
- [x] 4.4 Parts UI: add/remove references to materials or objects with relative col/row; enforce footprint + DAG rules
- [x] 4.5 Character sprites UI: configure down/up/side/attack/death frame lists (material refs); preview slot + flip for left

## 5. Level placement & instance overrides

- [x] 5.1 Support drag/drop (and stamp) of materials onto level canvas (existing path hardened)
- [x] 5.2 Support drag/drop (and stamp) of objects onto level canvas using object footprint + definition defaults; store `objectId`
- [x] 5.3 Resolve draw for object-backed instances (parts + character sprite slots, side flip)
- [x] 5.4 Right panel on selected instance: edit overrides without mutating definition; new places copy definition defaults
- [x] 5.5 Playback policy in editor/playtest draw: idle = frame 0; moving = loop direction frames; attack/death slots hooked to FSM actions with documented default

## 6. Verification

- [x] 6.1 Unit tests for object validators (cycles, bounds, normalize sprites)
- [x] 6.2 Unit/migration tests for package load with/without `objectLibrary`
- [ ] 6.3 Manual checklist: open material/object/level tabs; create character with side frames; stamp object + material; override instance HP
