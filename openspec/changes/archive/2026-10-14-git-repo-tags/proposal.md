## Why

Repositories already create Git tags as a side effect of releases, but users cannot list, search, create (with description), or delete tags independently. Hosting products (GitHub / Gitee) expose a dedicated Tags surface so version pointers can be managed without opening the Releases flow. We need the same capability so tags are first-class in Cat2Bug.

## What Changes

- Add a repository **Tags** page at `code/{slug}/tags` (sibling to branches; not a new Shell primary tab).
- List Git tags with name, annotated message (description), pointed commit summary, and actions: download archive, create release (prefill tag), delete.
- Create tag via **`el-dialog`** (name, optional description, target branch/commit) — annotated tag when description is present; lightweight when empty is acceptable only if we choose one rule (design: prefer annotated always when creating from UI, message may be empty string).
- Search / filter tags by name (and optionally message substring).
- Delete tag: if a `sys_git_release` row exists for that tag name, confirm and **cascade-delete the release** (assets + DB) then delete `refs/tags/*`; if no release, delete tag only.
- Reuse existing archive-by-ref for download; reuse create-release flow with tag prefilled.
- No change to existing rule: creating a release still ensures a tag exists (lightweight if newly created by release path); deleting a release still retains the Git tag.

## Capabilities

### New Capabilities

- `git-repo-tags`: List, search, create (dialog + annotated tag), download, create-release shortcut, and delete (with optional cascade to release) for repository Git tags.

### Modified Capabilities

- (none) — release create/delete semantics stay as today; tag delete may *invoke* the same release cleanup as `deleteRelease`, without changing release-facing requirements.

## Impact

- Backend: new tag list/create/delete APIs on bare repo (JGit); extend or share `GitReleaseTagHelper` for annotated create + delete; release lookup by `tag_name` for cascade.
- Frontend: `tags.vue` under git repo views, route + shell mapping like branches, i18n, API client.
- Permissions: list/download = pull; create/delete = manage (align with branches / releases).
- Webhooks: tag create/delete SHOULD enqueue existing `tag` subscription events when implemented via platform API (same as push-driven tag events where applicable).
- Related existing: `git-repo-releases` (ensure-tag on create; tag retained on release delete), archive download by ref, `GitRepoShell` / route guards.
