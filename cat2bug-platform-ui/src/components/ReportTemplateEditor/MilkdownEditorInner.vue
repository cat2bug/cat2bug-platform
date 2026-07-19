<template>
  <div
    class="report-milkdown-editor milkdown"
    :class="{ 'is-dark': isDark }"
    data-milkdown-surface
  >
    <Milkdown />
  </div>
</template>

<script>
import { defineComponent, watch, ref, markRaw, onMounted, onUnmounted, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { Milkdown, useEditor } from '@milkdown/vue'
import { Crepe, CrepeFeature } from '@milkdown/crepe'
import { editorViewCtx, parserCtx } from '@milkdown/kit/core'
import { blockConfig } from '@milkdown/kit/plugin/block'
import { TextSelection } from '@milkdown/prose/state'
import { getMarkdown, insert, replaceAll, forceUpdate } from '@milkdown/kit/utils'
import { useSettingsStore } from '@/stores/settings'
import { isReportEditorDarkMode } from './report-editor-theme'
import { useReportDslPlugins } from './milkdown'
import { normalizeDslSource } from './dsl/normalize'
import { rewriteMarkdownTablesFromDoc, sourceNeedsMultimdSpanApply } from './dsl/serialize-multimd-table'
import { applyMultimdSpansToDoc } from './dsl/parse-multimd-table'
import { tableCellMergePluginKey } from './milkdown/table-cell-merge-plugin'
import { subscribeReportPreviewData, ensureReportPreviewData } from './milkdown/report-preview-data'
import { installBlockDragImageFix } from './milkdown/block-drag-image'
import { installReportBlockAddOverlay } from './milkdown/report-block-add-overlay'
import { installTableCellActionToolbar } from './milkdown/table-cell-action-toolbar'
import { documentContainsRawDslText } from './milkdown/dsl-doc-sync'
import { markdownSemanticallyEqual, shouldApplyExternalModelSync } from './milkdown/model-sync'
import {
  buildDefaultBlockMenuGroups,
  toolbarAddMenuToOverlayGroups
} from './milkdown/default-block-menu'
import {
  isBlockishDslContent,
  insertBlockFragmentAfterTable,
  prepareBlockHandleInsert,
  resolveTablePosAroundSelection,
  tableAwareFilterNodes
} from './milkdown/table-cell-block-handle'
import { resolveBusinessInsertMode, isInlineImageDslContent } from './milkdown/business-insert-mode'
import {
  insertInlineDslAtCaret,
  insertInlineFragmentAtCaret,
  extractInlineFragmentFromParsedDoc,
  cleanupEphemeralEmptyParagraph,
  resolveInlineInsertCaret
} from './milkdown/inline-dsl-insert'
import { Message } from 'element-ui'
import i18n from '@/utils/i18n/i18n'
import { isPlanStatisticsTableContent } from './dsl/complex-blocks'
import {
  appendBusinessGroupsToBlockMenu,
  loadReportAddMenuGroups
} from './composables/blockEditBusinessMenu'
import { upload } from '@/api/common/upload'
import { resolveUploadUrl } from '@/utils/upload-asset'

/** Crepe selection toolbar: 批注 icon (chat bubble) */
const SELECTION_COMMENT_ICON = `
  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
    <path fill="currentColor" d="M4 4h16c1.1 0 2 .9 2 2v10c0 1.1-.9 2-2 2H8l-4 4V6c0-1.1.9-2 2-2zm2 3v2h12V7H6zm0 4v2h8v-2H6z"/>
  </svg>
`

export default defineComponent({
  name: 'MilkdownEditorInner',
  components: { Milkdown },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    /** Template editor: true. Report body snapshot: false (no DSL variable blocks). */
    enableDsl: {
      type: Boolean,
      default: true
    },
    /** Report body: Crepe selection toolbar shows「批注」action. */
    enableSelectionComment: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'change', 'selection-comment'],
  setup(props, { emit, expose }) {
    const syncing = ref(false)
    /** Ignore markdownUpdated until this time (covers Milkdown listener 200ms debounce after replaceAll). */
    let ignoreMarkdownUpdatedUntil = 0
    const settingsStore = useSettingsStore()
    const { themeMode } = storeToRefs(settingsStore)
    const domDark = ref(isReportEditorDarkMode())
    const isDark = computed(() => themeMode.value === 'dark' || domDark.value)
    let themeObserver = null
    let unsubscribePreviewData = null
    let uninstallBlockDragImageFix = null
    let uninstallBlockAddOverlay = null
    let uninstallTableCellActionToolbar = null
    /** @type {{ destroy: () => void, openAt: (rect: DOMRect) => void } | null} */
    let blockAddOverlayApi = null
    let uninstallEditorBlurSync = null
    /** @type {number | null} */
    let insertSelectionPos = null
    /** Caret before block-handle prepare (empty para). Used for $image / ${} inline insert. */
    /** @type {number | null} */
    let caretBeforeBlockAdd = null
    /** Empty paragraph created by prepare — removed after inline insert. */
    /** @type {number | null} */
    let ephemeralEmptyParaPos = null
    /** @type {import('@milkdown/crepe').Crepe | null} */
    let crepeInstance = null
    const blockEditConfig = {
      /** Toolbar-aligned groups for left-+ overlay (and Crepe business append). */
      addMenuGroups: [],
      blockHandle: null,
      buildMenu(groupBuilder) {
        if (!props.enableDsl) {
          return
        }
        appendBusinessGroupsToBlockMenu(
          groupBuilder,
          blockEditConfig.addMenuGroups,
          insertBusinessContentAtSelection
        )
      }
    }

    async function refreshAddMenuGroups(projectId) {
      // Report body snapshot: no DSL business tabs; left-+ uses Crepe default groups.
      if (!props.enableDsl) {
        blockEditConfig.addMenuGroups = []
        return
      }
      try {
        blockEditConfig.addMenuGroups = await loadReportAddMenuGroups(projectId)
      } catch (_e) {
        blockEditConfig.addMenuGroups = []
      }
    }

    function captureInsertSelection() {
      const editor = get()
      if (!editor) {
        return
      }
      editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        if (!view) {
          return
        }
        insertSelectionPos = view.state.selection.from
      })
    }

    /** Capture caret before block-handle prepare inserts an empty paragraph. */
    function captureCaretBeforeBlockAdd() {
      const editor = get()
      if (!editor) {
        return
      }
      editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        if (!view) {
          return
        }
        const collapsed = resolveInlineInsertCaret(view.state)
        if (collapsed) {
          view.dispatch(view.state.tr.setSelection(collapsed))
        }
        caretBeforeBlockAdd = view.state.selection.from
        ephemeralEmptyParaPos = null
      })
    }

    function restoreInsertSelection(ctx) {
      if (insertSelectionPos == null) {
        return
      }
      const view = ctx.get(editorViewCtx)
      if (!view) {
        return
      }
      const { doc } = view.state
      const safePos = Math.max(1, Math.min(insertSelectionPos, doc.content.size - 1))
      try {
        const tr = view.state.tr.setSelection(TextSelection.near(doc.resolve(safePos)))
        view.dispatch(tr)
      } catch (_e) {
        // ignore invalid selection restore
      }
    }

    function insertBusinessContentAtSelection(ctx, content) {
      if (!content) {
        return
      }
      if (isPlanStatisticsTableContent(content)) {
        Message.info(
          i18n.t('mk.plan-table-insert-hint') ||
            '计划统计大表已插入；可在可视化表格中编辑单元格，并使用加减行/列手柄调整结构。'
        )
      }

      const view = ctx.get(editorViewCtx)
      if (!view) {
        return
      }

      syncing.value = true
      ignoreMarkdownUpdatedUntil = Date.now() + 450
      try {
        // $table/$card: keep outside cells (block insert after table).
        if (isBlockishDslContent(content)) {
          const tablePos = resolveTablePosAroundSelection(view, insertSelectionPos)
          if (tablePos != null) {
            const parsed = ctx.get(parserCtx)(content)
            if (parsed?.content?.size) {
              insertBlockFragmentAfterTable(view, tablePos, parsed.content)
            }
          } else {
            const insertMode = resolveBusinessInsertMode(view.state.selection, content)
            if (insertMode === 'block') {
              insert(content, false)(ctx)
            } else {
              insert(`\n${content}`, false)(ctx)
            }
          }
        } else {
          // $image / ${…}: insert inline nodes at the pre-+ caret. Do NOT insertText
          // + replaceAll — that serialize/parse round-trip drops sibling cell text.
          const preferredPos =
            caretBeforeBlockAdd != null ? caretBeforeBlockAdd : insertSelectionPos
          const parsed = ctx.get(parserCtx)(content)
          const inlineFrag = extractInlineFragmentFromParsedDoc(parsed)
          const { insertPos, insertedLength } = inlineFrag
            ? insertInlineFragmentAtCaret(view, inlineFrag, preferredPos)
            : insertInlineDslAtCaret(view, content, preferredPos)
          if (isInlineImageDslContent(content) || caretBeforeBlockAdd != null) {
            cleanupEphemeralEmptyParagraph(
              view,
              ephemeralEmptyParaPos,
              insertPos,
              insertedLength
            )
          }
          caretBeforeBlockAdd = null
          ephemeralEmptyParaPos = null
          const next = readEditorMarkdown(ctx)
          // Keep the live doc — only refresh multimd spans if needed.
          applyMultimdSpansAfterLoad(ctx, next)
          ctx.get(editorViewCtx)?.focus()
          refreshPreviewViews()
          emit('update:modelValue', next)
          emit('change', next)
          return
        }
        caretBeforeBlockAdd = null
        ephemeralEmptyParaPos = null
        const next = readEditorMarkdown(ctx)
        replaceAll(next, true)(ctx)
        applyMultimdSpansAfterLoad(ctx, next)
        ctx.get(editorViewCtx)?.focus()
        refreshPreviewViews()
        emit('update:modelValue', next)
        emit('change', next)
      } finally {
        syncing.value = false
      }
    }

    function applyTableAwareBlockFilter() {
      const editor = get()
      if (!editor) {
        return
      }
      editor.action((ctx) => {
        ctx.update(blockConfig.key, (prev) => ({
          ...prev,
          filterNodes: tableAwareFilterNodes
        }))
      })
    }

    function prepareTableCellInsert(handleEl) {
      const editor = get()
      if (!editor) {
        return false
      }
      let handled = false
      editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        if (!view) {
          return
        }
        const sizeBefore = view.state.doc.content.size
        // Always skip Crepe onAdd: cell / after-table / whole-table / normal block.
        handled = prepareBlockHandleInsert(view, handleEl)
        insertSelectionPos = view.state.selection.from
        if (view.state.doc.content.size > sizeBefore) {
          const $from = view.state.selection.$from
          for (let depth = $from.depth; depth > 0; depth -= 1) {
            if ($from.node(depth).type.name === 'paragraph') {
              ephemeralEmptyParaPos = $from.before(depth)
              break
            }
          }
        }
      })
      return handled
    }

    function runWithPreservedSelection(action) {
      const editor = get()
      if (!editor) {
        return
      }
      editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        if (!view) {
          action(ctx)
          return
        }
        const { from, to } = view.state.selection
        const hadFocus = view.hasFocus()
        action(ctx)
        if (!hadFocus) {
          return
        }
        const { doc } = view.state
        const safeFrom = Math.max(0, Math.min(from, doc.content.size))
        const safeTo = Math.max(0, Math.min(to, doc.content.size))
        try {
          const selection =
            safeFrom === safeTo
              ? TextSelection.near(doc.resolve(Math.max(1, safeFrom)))
              : TextSelection.create(doc, safeFrom, safeTo)
          const tr = view.state.tr.setSelection(selection)
          view.dispatch(tr)
          view.focus()
        } catch (_e) {
          view.focus()
        }
      })
    }

    function readEditorMarkdown(ctx) {
      const view = ctx.get(editorViewCtx)
      let markdown = getMarkdown()(ctx) || ''
      if (view?.state?.doc) {
        markdown = rewriteMarkdownTablesFromDoc(markdown, view.state.doc)
      }
      return normalizeDslSource(markdown)
    }

    function emitCurrentMarkdown(ctx) {
      const normalized = readEditorMarkdown(ctx)
      emit('update:modelValue', normalized)
      emit('change', normalized)
    }

    function applyMultimdSpansAfterLoad(ctx, source) {
      if (!sourceNeedsMultimdSpanApply(source)) {
        return
      }
      const view = ctx.get(editorViewCtx)
      if (!view) {
        return
      }
      const tr = applyMultimdSpansToDoc(view.state, { forceEmptyColspan: true })
      if (tr) {
        view.dispatch(tr.setMeta(tableCellMergePluginKey, { spansApplied: true }))
      }
    }

    function installEditorBlurSync() {
      uninstallEditorBlurSync?.()
      const editor = get()
      if (!editor) {
        return
      }
      editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        if (!view?.dom) {
          return
        }
        const onFocusOut = (event) => {
          const related = event.relatedTarget
          if (related instanceof Node) {
            if (view.dom.contains(related)) {
              return
            }
            const menuHost = document.querySelector('.report-block-add-menu-host')
            if (menuHost?.contains(related)) {
              return
            }
          }
          if (syncing.value) {
            return
          }
          syncing.value = true
          try {
            emitCurrentMarkdown(ctx)
          } finally {
            syncing.value = false
          }
        }
        view.dom.addEventListener('focusout', onFocusOut, true)
        uninstallEditorBlurSync = () => {
          view.dom.removeEventListener('focusout', onFocusOut, true)
        }
      })
    }

    function editorHasFocus() {
      const editor = get()
      if (!editor) {
        return false
      }
      let focused = false
      editor.action((ctx) => {
        focused = ctx.get(editorViewCtx)?.hasFocus() ?? false
      })
      return focused
    }

    function refreshPreviewViews() {
      const editor = get()
      if (!editor || editorBooting.value) {
        return
      }
      try {
        runWithPreservedSelection(() => {
          editor.action(forceUpdate())
        })
      } catch (_e) {
        // editor may not be ready yet
      }
    }

    function syncPreviewData(projectId) {
      if (!projectId) {
        return
      }
      ensureReportPreviewData(projectId).finally(() => {
        if (!editorHasFocus()) {
          refreshPreviewViews()
        }
      })
    }

    watch(() => props.projectId, (id) => {
      syncPreviewData(id)
      refreshAddMenuGroups(id)
    }, { immediate: true })

    onMounted(() => {
      themeObserver = new MutationObserver(() => {
        domDark.value = isReportEditorDarkMode()
      })
      themeObserver.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['class']
      })
      unsubscribePreviewData = subscribeReportPreviewData(() => {
        if (!editorHasFocus()) {
          refreshPreviewViews()
        }
      })
    })

    onUnmounted(() => {
      themeObserver?.disconnect()
      unsubscribePreviewData?.()
      uninstallBlockDragImageFix?.()
      uninstallBlockAddOverlay?.()
      uninstallTableCellActionToolbar?.()
      uninstallEditorBlurSync?.()
      blockAddOverlayApi = null
      crepeInstance = null
    })

    const { get, loading: editorBooting } = useEditor((root) => {
      blockEditConfig.blockHandle = {
        root,
        getOffset: () => 0,
        getPlacement: ({ active, blockDom }) => {
          if (active.node.type.name === 'heading') {
            return 'left'
          }
          let totalDescendant = 0
          active.node.descendants((node) => {
            totalDescendant += node.childCount
          })
          const dom = active.el
          const domRect = dom.getBoundingClientRect()
          const handleRect = blockDom.getBoundingClientRect()
          const style = window.getComputedStyle(dom)
          const paddingTop = Number.parseInt(style.paddingTop, 10) || 0
          const paddingBottom = Number.parseInt(style.paddingBottom, 10) || 0
          const height = domRect.height - paddingTop - paddingBottom
          const handleHeight = handleRect.height
          return totalDescendant > 2 || handleHeight < height ? 'left-start' : 'left'
        },
        floatingUIOptions: {
          strategy: 'absolute'
        }
      }

      const crepe = new Crepe({
        root,
        defaultValue: props.modelValue || '',
        features: {
          [CrepeFeature.BlockEdit]: true,
          [CrepeFeature.Table]: true,
          [CrepeFeature.Toolbar]: true,
          // Crepe slash 「高级」里的图片 / 代码块依赖这些 feature
          [CrepeFeature.ImageBlock]: true,
          [CrepeFeature.CodeMirror]: true,
          [CrepeFeature.TopBar]: false,
          // 避免 remark-math 干扰 $table{...} / ${...} DSL
          [CrepeFeature.Latex]: false
        },
        featureConfigs: {
          [CrepeFeature.Placeholder]: {
            text: props.enableDsl ? '开始编写报告模板…' : '开始编写报告…',
            mode: 'doc'
          },
          [CrepeFeature.BlockEdit]: blockEditConfig,
          [CrepeFeature.Toolbar]: props.enableSelectionComment
            ? {
                buildToolbar: (builder) => {
                  builder.getGroup('function').addItem('comment', {
                    icon: SELECTION_COMMENT_ICON,
                    active: () => false,
                    onRun: (ctx) => {
                      const view = ctx.get(editorViewCtx)
                      const { from, to, empty } = view.state.selection
                      if (empty || from === to) {
                        return
                      }
                      const quote = view.state.doc.textBetween(from, to, ' ').trim().slice(0, 400)
                      if (!quote) {
                        return
                      }
                      const docSize = view.state.doc.content.size
                      const contextBefore = view.state.doc
                        .textBetween(Math.max(0, from - 60), from, ' ')
                        .slice(-60)
                      const contextAfter = view.state.doc
                        .textBetween(to, Math.min(docSize, to + 60), ' ')
                        .slice(0, 60)
                      let anchorRect = null
                      try {
                        const start = view.coordsAtPos(from)
                        const end = view.coordsAtPos(to)
                        anchorRect = {
                          top: Math.min(start.top, end.top),
                          bottom: Math.max(start.bottom, end.bottom),
                          left: Math.min(start.left, end.left),
                          right: Math.max(start.right, end.right)
                        }
                      } catch (e) {
                        anchorRect = null
                      }
                      emit('selection-comment', { quote, contextBefore, contextAfter, anchorRect })
                    }
                  })
                }
              }
            : undefined,
          [CrepeFeature.ImageBlock]: {
            blockUploadButton: '上传文件',
            blockUploadPlaceholderText: '或粘贴图片链接',
            blockCaptionPlaceholderText: '图片说明',
            blockConfirmButton: '确认',
            inlineUploadButton: '上传',
            inlineUploadPlaceholderText: '或粘贴图片链接',
            onUpload: async (file) => {
              const formData = new FormData()
              formData.append('file', file)
              const res = await upload(formData)
              const fileName = res && (res.fileName || (res.data && res.data.fileName))
              if (!fileName) {
                throw new Error('image upload failed')
              }
              return resolveUploadUrl(fileName)
            }
          }
        }
      })

      useReportDslPlugins(crepe.editor, { enableDsl: props.enableDsl })

      crepe.on((listener) => {
        listener.markdownUpdated((ctx, markdown) => {
          if (syncing.value || Date.now() < ignoreMarkdownUpdatedUntil) {
            return
          }
          const view = ctx.get(editorViewCtx)
          let next = markdown || ''
          if (view?.state?.doc) {
            next = rewriteMarkdownTablesFromDoc(next, view.state.doc)
          }
          const normalized = normalizeDslSource(next)
          // Empty table cells serialize as <br />; after normalize they match model — skip echo
          if (markdownSemanticallyEqual(normalized, props.modelValue || '')) {
            return
          }
          emit('update:modelValue', normalized)
          emit('change', normalized)
        })
      })

      crepeInstance = markRaw(crepe)
      return crepeInstance
    })

    function syncModelToEditor(force = false) {
      if (editorBooting.value || !crepeInstance) {
        return
      }
      if (!shouldApplyExternalModelSync({ force, editorFocused: editorHasFocus() })) {
        return
      }
      const editor = get()
      if (!editor) {
        return
      }
      const next = normalizeDslSource(props.modelValue || '')
      let current = null
      try {
        current = editor.action((ctx) => readEditorMarkdown(ctx))
      } catch (_e) {
        current = null
      }
      // Always skip when equal — including force — to break split-pane replaceAll ↔ markdownUpdated loops
      if (current !== null && markdownSemanticallyEqual(current, next)) {
        return
      }
      if (!force) {
        const needsDslResync = editor.action((ctx) => documentContainsRawDslText(ctx, editorViewCtx))
        if (!needsDslResync && current !== null && current === next) {
          return
        }
      }
      syncing.value = true
      // Milkdown listener debounces markdownUpdated ~200ms; keep ignoring past finally{}
      ignoreMarkdownUpdatedUntil = Date.now() + 450
      try {
        runWithPreservedSelection((ctx) => {
          replaceAll(next, true)(ctx)
          applyMultimdSpansAfterLoad(ctx, next)
        })
        if (!editorHasFocus()) {
          refreshPreviewViews()
        }
      } catch (error) {
        if (import.meta.env.DEV) {
          console.error('[ReportTemplateEditor] Milkdown replaceAll failed:', error)
        }
      } finally {
        syncing.value = false
      }
    }

    watch(() => props.modelValue, syncModelToEditor, { flush: 'post' })

    function getOverlayMenuGroups() {
      // Static report editing: default text/list/advanced inserts (headings, table, image…).
      // Template editing: toolbar-aligned groups including project/case/defect/plan DSL.
      if (!props.enableDsl) {
        return buildDefaultBlockMenuGroups()
      }
      const groups = toolbarAddMenuToOverlayGroups(blockEditConfig.addMenuGroups, {
        onInsertContent: insertBusinessContentAtSelection
      })
      return groups.length ? groups : buildDefaultBlockMenuGroups()
    }

    watch(editorBooting, async (isLoading) => {
      if (!isLoading) {
        await refreshAddMenuGroups(props.projectId)
        uninstallBlockDragImageFix?.()
        uninstallBlockDragImageFix = installBlockDragImageFix()
        applyTableAwareBlockFilter()
        uninstallBlockAddOverlay?.()
        uninstallTableCellActionToolbar?.()
        blockAddOverlayApi = installReportBlockAddOverlay({
          getMenuGroups: getOverlayMenuGroups,
          runEditorAction: (fn) => {
            get()?.action(fn)
          },
          focusEditor: () => focus(),
          captureCaretBeforeBlockAdd,
          captureInsertSelection,
          restoreInsertSelection,
          prepareTableCellInsert,
          isDark: () => isDark.value
        })
        uninstallBlockAddOverlay = () => {
          blockAddOverlayApi?.destroy()
          blockAddOverlayApi = null
        }
        uninstallTableCellActionToolbar = installTableCellActionToolbar({
          getView: () => {
            let view = null
            get()?.action((ctx) => {
              view = ctx.get(editorViewCtx)
            })
            return view
          },
          onAdd: (anchorRect) => {
            captureInsertSelection()
            blockAddOverlayApi?.openAt(anchorRect)
          },
          isDark: () => isDark.value
        })
        installEditorBlurSync()
        syncModelToEditor()
        syncPreviewData(props.projectId)
        refreshPreviewViews()
      }
    })

    function insertMarkdown(text) {
      if (!text) {
        return
      }
      const editor = get()
      if (!editor) {
        return
      }
      // Toolbar may blur the DOM; ProseMirror selection is still at the caret (e.g. table cell).
      // Do not restore insertSelectionPos here — that is only for the block-add menu flow.
      editor.action((ctx) => {
        insertBusinessContentAtSelection(ctx, text)
      })
    }

    function focus() {
      get()?.action((ctx) => {
        ctx.get(editorViewCtx)?.focus()
      })
    }

    expose({
      insertMarkdown,
      focus,
      refreshPreviewViews,
      hasFocus: editorHasFocus,
      // Never force-replace while user is typing in the visual editor (split-pane MdEditor echoes).
      syncFromModel: () => {
        if (editorHasFocus()) {
          return
        }
        syncModelToEditor(true)
      }
    })

    return { isDark }
  }
})
</script>

<style scoped>
.report-milkdown-editor {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
