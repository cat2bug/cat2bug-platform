## Context

The game map editor lives in worktree `cat2bug-feature-team-game-engine` (`GameMapEditor.vue` + `map-editor/*`). Today:

- Left panel = categorized `assetLibrary` (地图/建筑/人物/物品).
- Center tabs = levels only (`map-editor-levels.js`).
- Character props / FSM attach to **placed instances**; composites group instances on a level.
- Element name / chroma live mainly in `SpriteSheetImportDialog`; post-import asset props are mostly penetrable masks.
- `TILE_SIZE = 16`; assets already expose `tileW` / `tileH` from pixel size.

This change introduces a definition-first **素材 / 对象 / 关卡** IA and a multi-document center workspace, without requiring new backend APIs if packages stay ZIP-based.

## Goals / Non-Goals

**Goals:**

- Clear separation: materials = images; objects = typed reusable definitions; levels = placement.
- Document tabs for material / object / level editors.
- Object refs to materials and other objects; manual parent footprint; character sprite slots with side-flip.
- Stamp both materials and objects onto levels; instance overrides of definition defaults.
- Persist new model in game package with migration from current projects.

**Non-Goals:**

- Splitting idle vs walk sprite slots (playback policy only).
- Separate left/right art sets (flip only).
- Auto-sizing parent footprint from children.
- Full rewrite of Phaser runtime combat; first ship editor + playtest draw path for new sprites.
- Backend catalog schema redesign beyond what the package already stores.
- Replacing FSM editing UX wholesale (reuse existing FSM editors; bind to character **definitions**, with instance override of `fsmId` etc. as needed).

## Decisions

### D1: Project model — `objectLibrary` at root beside `assetLibrary`

- **Choice:** Keep `assetLibrary` as materials/elements. Add `objectLibrary: ObjectDef[]` at project root (shared across levels, like assets).
- **Why:** Matches “definitions shared, levels place instances.” Avoids stuffing typed data into asset category fields.
- **Alternatives:** Encode objects as special assets (`kind: object`) — rejected (muddies image-only material UX). Store objects only inside levels — rejected (not reusable).

### D2: ObjectDef shape

```
ObjectDef {
  id, name, kind: 'building' | 'character' | 'item'
  tileW, tileH          // manual footprint in tiles
  parts: [{             // optional composition
    refType: 'element' | 'object'
    refId: string
    col, row            // relative; must satisfy 0..tileW/H bounds
  }]
  // kind-specific defaults (character: hp, name, fsmId, …)
  props: { … }
  // character visual slots (element ids = frames)
  sprites?: {
    down:   { frames: string[], fps?: number }
    up:     { frames: string[], fps?: number }
    side:   { frames: string[], fps?: number }  // faces right
    attack: { frames: string[], fps?: number }
    death:  { frames: string[], fps?: number }
  }
}
```

- **Validation:** Reject cycles in object→object refs (DFS/topo). Reject parts outside footprint.
- **Preview:** Center object tab renders a `tileW × tileH` grid at `TILE_SIZE` px/cell; compose parts; for characters show selected slot animation (default `down`).

### D3: Center document tabs (IDE model)

- **Choice:** Replace level-only tab strip with `openDocuments: [{ kind, id }]`, `activeDocumentId`. Clicking left list items opens or focuses a tab.
- **Why:** User can edit an object while a level stays open in another tab.
- **Level switch:** Opening a level document still uses `switchLevel` / flush snapshot semantics under the hood.
- **Close tab:** Closing a level tab does not delete the level; closing does not delete materials/objects.

### D4: Left panel structure

```
[素材] [对象] [关卡]
  │       │       └─ level list (+ add/remove)
  │       └─ [建筑|角色|物品] + list (+ add/remove)
  └─ [地图|建筑|人物|物品]  (existing RESOURCE_TABS categories)
```

Materials keep current category buckets (`terrain` → 地图, etc.).

### D5: Material right-panel editing

- **Choice:** When material document (or left selection) is active, right props allow: name, background color (for re-slice / display chroma where applicable), replace image; retain penetrable mask editing.
- **Why:** Moves “元素” controls out of import-only dialog.

### D6: Placement & instance overrides

- Level layer objects gain optional `objectId` (stamp from object) **or** continue `assetId` (stamp from material).
- On place from object: copy definition defaults into instance `character` / combat fields; store `objectId` for re-resolve of visuals.
- Selecting an instance on a level: right panel edits **instance** values (override). Definition edits do not forcibly rewrite existing instance overrides (definition is default source for new places; optional “reset to definition” can be later).

### D7: Character animation playback

- Moving → loop current direction frames (`down`/`up`/`side`).
- Idle → frame 0 of that slot.
- Left → `side` + horizontal flip; right → `side` unflipped.
- FSM `attack` / `die` → `attack` / `death` slots (exact interrupt/return-to-idle timing: sensible default in implementation; document in tasks).

### D8: Relationship to composites

- **Choice:** Prefer definition-level `parts` for reusable composition. Keep level composites working during migration; deprecate “compose only on level” as the primary authoring path for new content.
- **Why:** User wants reusable object→object refs, not only ad-hoc level groups.

### D9: Implementation locus

- Implement in `feature/team-game-engine` worktree (or equivalent branch), not `core`, until merged.

## Risks / Trade-offs

- **[Risk] Package/schema break for saved games** → Version field + migrate on load: invent empty `objectLibrary`; map character assets with instance props into optional seed objects only if straightforward, else leave instances as-is with `assetId`.
- **[Risk] Object→object cycles / deep trees** → Hard reject cycles; cap nest depth (e.g. 8) in validator.
- **[Risk] Dual stamp paths (`assetId` vs `objectId`)** → Shared stamp pipeline; draw resolve: objectId → sprites/parts → element srcs.
- **[Risk] Scope creep into full runtime animation** → Phase: editor preview first; playtest draw second; combat polish last.
- **[Trade-off] Manual footprint** → More author work, predictable collision/stamp size; validate parts fit.

## Migration Plan

1. On load: `ensureObjectLibrary(model)`; ensure document-tab state defaults to active level.
2. Existing projects: levels + `assetLibrary` unchanged; `objectLibrary` starts empty; authors create objects as needed.
3. Export ZIP includes `objectLibrary`.
4. Rollback: older editors ignore unknown `objectLibrary` key if needed; new editor tolerates missing key.

## Open Questions

- Attack animation: play once then return to idle, vs loop while FSM state active — default to **play while attack state active / once then hold last frame for death**.
- Max nest depth for object refs (propose **8**).
- Whether building/item require `sprites` or only `parts` + optional single display element — propose **parts-only** for building/item; character requires at least one frame in `down` or `side` for preview.
