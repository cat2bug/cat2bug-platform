## 1. Data model & backend API

- [x] 1.1 Add Flyway migrations (h2 + mysql) extending `sys_comment` with `parent_id`, `anchor_kind`, `quote`, `context_before`, `context_after`, `anchor_hint` (nullable; compatible with `defect_log`)
- [x] 1.2 Widen `module_type` if needed so `report` fits; update `SysComment` domain, mapper XML, and list/insert/update mappings
- [x] 1.3 Support create/list report annotations: `moduleType=report`, `correlationId=reportId`, root vs flat reply via `parent_id`
- [x] 1.4 Enforce delete authorization (creator, project admin, team admin) and cascade-delete flat replies when deleting a root annotation
- [x] 1.5 Add/adjust backend tests for create, reply, rematch-irrelevant persistence, and unauthorized delete rejection

## 2. Report detail three-pane layout

- [x] 2.1 Refactor `ViewReport.vue` body into TOC / document / comments shell with collapse controls for side panes
- [x] 2.2 Extract heading TOC from rendered `h1`–`h6`, assign stable ids, wire click → scroll and scroll-spy highlight
- [x] 2.3 Empty TOC state when no headings; ensure narrow drawer layout remains usable when panes collapse
- [x] 2.4 Keep Word/PDF export scoped to center `#report-data` body (exclude TOC/comments chrome)

## 3. Annotation UX & anchor rematch

- [x] 3.1 Add selection floating action and line/block annotate affordance on rendered body (prose and tables)
- [x] 3.2 Right panel: thread list, create root annotation, flat replies, orphaned badge「原文已变更」
- [x] 3.3 Implement quote + context rematch on load/body change; highlight matched ranges; no auto-delete on miss
- [x] 3.4 Bidirectional focus: body highlight ↔ comments thread; orphaned threads do not false-highlight
- [x] 3.5 Wire delete UI to authorized roles only; confirm cascade on root delete
- [x] 3.6 Ensure report template editor paths remain unchanged (no annotation shell)

## 4. Verification

- [x] 4.1 Frontend unit tests for rematch helpers (match / ambiguous / orphaned) and TOC extraction
- [ ] 4.2 Manual smoke: open report detail, TOC jump, selection + line comment, reply, edit body to orphan, admin/creator delete, export Word/PDF
