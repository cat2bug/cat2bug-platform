## Context

Tags today exist only as a side effect of Releases: `GitReleaseTagHelper.ensureTagAtCommit` creates a **lightweight** `refs/tags/<name>` when a release is published (or binds an existing tag at the same commit). Deleting a release keeps the Git tag. There is no tag list/create/delete UI or API. Branches already demonstrate the desired UX pattern (independent route under `code/:slug/…`, toolbar search, manage-gated create via `el-dialog`). Archive-by-ref and release create flows can be reused for download and “创建 release from tag.”

## Goals / Non-Goals

**Goals:**

- First-class Tags page at `code/{slug}/tags` with search, create dialog, list, download, create-release shortcut, and delete.
- UI-created tags are **annotated** so description maps to tag message.
- Delete strategy B: when a `sys_git_release` row shares the tag name, UI confirms then API cascade-deletes the release (same cleanup as `deleteRelease`) and then deletes the tag ref.
- Permissions aligned with branches/releases: pull read/download; manage create/delete.

**Non-Goals:**

- Editing an existing tag’s message or moving a tag to another commit (delete + recreate if needed).
- Signed tags / GPG verification UI.
- Syncing remote GitHub/Gitee tag metadata into a platform table (tags remain Git-native).
- Changing release create semantics (still auto-ensure lightweight tag when missing) or “delete release keeps tag.”
- New Shell primary tab (Tags stays under code, like Branches).

## Decisions

### 1. Tags are Git refs; list is live from the bare repo

- **Choice**: `GET …/tags` walks `refs/tags/*` via JGit (peel annotated → commit); enrich with short commit fields and optional `releaseId` from `sys_git_release` by `(repository_id, tag_name)`.
- **Why**: Source of truth for pointers is Git; release link is optional metadata.
- **Alt**: Mirror tags into DB → drift risk with Smart HTTP pushes.

### 2. Create from Tags UI = annotated tag; release path unchanged

- **Choice**: `POST …/tags` creates an **annotated** tag (`TagBuilder` / `annotatedTag`) with `message` (optional empty string allowed). Target = `targetCommitId` or `targetRef` (branch/commit), same resolution style as releases. Reject duplicate tag names. Release `ensureTagAtCommit` continues to create **lightweight** tags when a release introduces a new name.
- **Why**: User asked for description-backed tags; keep release path backward-compatible and cheap.
- **Alt**: Always annotated everywhere → would change release helper behavior for little gain.

### 3. Create UX = `el-dialog` (not popover)

- **Choice**: Mirror branches page: primary “新建标签” opens dialog with name, description textarea, target ref select (branches + allow commit SHA input or reuse browse ref picker patterns).
- **Why**: User decision; more robust than Gitee-style popover on scroll/mobile.
- **Alt**: Popover anchored to button (screenshot) — rejected.

### 4. Routing & shell mapping

- **Choice**: Route `code/:slug/tags` (+ legacy numeric redirect if branches have one). `GitRepoShell` maps `active-tab="tags"` onto the Code primary tab (same pattern as branches). Entry: link from browse toolbar / branch-count area (tags count or “标签” link) — at minimum route reachable; prefer a visible tags entry next to branches if browse already exposes branch management link.
- **Why**: User chose `/tags` under code, not a Shell Releases sibling.

### 5. Delete cascade (strategy B)

- **Choice**:
  1. Resolve tag; if missing → 404.
  2. If release exists for tag name and request lacks `cascadeRelease=true` → **409** with clear message (UI shows confirm then retries with flag).
  3. If release exists and `cascadeRelease=true` → call same asset+DB cleanup as `deleteRelease`, then delete `refs/tags/<name>`.
  4. If no release → delete tag ref only.
- **Why**: Avoids orphaned releases (Gitea-class failure mode) while matching “confirm then joint delete.” Flag prevents silent API cascade.
- **Alt**: Always cascade without flag → unsafe for scripts; block forever until release deleted manually → rejects user’s B.

### 6. Download & create release

- **Choice**: Download uses existing archive API with `ref=<tagName>`. “创建 release” navigates/opens existing release create UI with `tagName` prefilled (and commit bound to tag tip); if release already exists, link to that release instead of create.
- **Why**: No duplicate archive or release form.

### 7. Search

- **Choice**: Client or server filter by substring on tag name (required); message substring MAY be included in the same `q` param. Server-side `q` preferred for consistency with branches.
- **Why**: User requested search in v1.

### 8. API sketch

| Method | Path | Auth |
|--------|------|------|
| GET | `/system/git/repo/{repositoryId}/tags?q=` | pull |
| POST | `/system/git/repo/{repositoryId}/tags` body `{ name, message?, targetRef?, targetCommitId? }` | manage |
| DELETE | `/system/git/repo/{repositoryId}/tags/{name}?cascadeRelease=` | manage |

Prefer placing endpoints on `SysGitBrowseController` (or a thin `SysGitTagController`) alongside branches; share path validation and repo ACL helpers.

### 9. Webhooks

- **Choice**: On successful platform create/delete tag, enqueue existing `tag` webhook event types (same payload shape as push-driven tag events where feasible).
- **Why**: Settings already advertise tag subscriptions; API mutations should not bypass them.

## Risks / Trade-offs

- **[Risk] Lightweight tags from releases show empty description** → Accept; UI shows blank description; optional later “convert to annotated” out of scope.
- **[Risk] Concurrent Smart HTTP push deletes/recreates tag during cascade** → Keep delete in short JGit critical section; release DB delete transactional; accept rare races like branch delete.
- **[Risk] Cascade surprises users who only meant to drop the Git pointer** → Confirm copy MUST state release + assets will be removed; 409 without flag protects non-UI clients.
- **[Risk] Large tag lists** → Soft cap / pagination later if needed; v1 list all tags with search (typical repos have few tags).
- **[Trade-off] Two tag flavors (annotated UI vs lightweight release)** → Documented asymmetry; list must handle both.

## Migration Plan

1. Ship backend list/create/delete (+ tests) with no Flyway (no new tables).
2. Ship frontend route, dialog, i18n, shell mapping, browse entry link.
3. Rollback: hide route/UI; unused tag refs remain in Git (safe).

## Open Questions

- None blocking — cascade flag, annotated create, dialog, `/tags` route, and search confirmed in explore.

## 相关长期文档

- `docs/03-技术方案/Git Smart HTTP与在线浏览.md`（标签真相源与级联删除约定）
- `docs/05-接口文档/接口清单-Git代码仓库.md`
- ADR：`docs/07-技术决策/ADR-006-Git仓库用JGit自托管与Smart-HTTP.md`；本变更无新 ADR
