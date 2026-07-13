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
import { editorViewCtx } from '@milkdown/kit/core'
import { getMarkdown, replaceAll, forceUpdate } from '@milkdown/kit/utils'
import { useSettingsStore } from '@/stores/settings'
import { isReportEditorDarkMode } from './report-editor-theme'
import { useReportDslPlugins } from './milkdown'
import { normalizeDslSource } from './dsl/normalize'
import { subscribeReportPreviewData, ensureReportPreviewData } from './milkdown/report-preview-data'
import { installBlockDragImageFix } from './milkdown/block-drag-image'
import { installReportBlockAddOverlay } from './milkdown/report-block-add-overlay'
import {
  buildDefaultBlockMenuGroups,
  businessGroupsToOverlayGroups
} from './milkdown/default-block-menu'
import { Message } from 'element-ui'
import i18n from '@/utils/i18n/i18n'
import { isPlanStatisticsTableContent } from './dsl/complex-blocks'
import {
  appendBusinessGroupsToBlockMenu,
  loadReportBusinessBlockMenuGroups,
  sortBusinessMenuGroups
} from './composables/blockEditBusinessMenu'

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
    }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit, expose }) {
    const syncing = ref(false)
    const settingsStore = useSettingsStore()
    const { themeMode } = storeToRefs(settingsStore)
    const domDark = ref(isReportEditorDarkMode())
    const isDark = computed(() => themeMode.value === 'dark' || domDark.value)
    let themeObserver = null
    let unsubscribePreviewData = null
    let uninstallBlockDragImageFix = null
    let uninstallBlockAddOverlay = null
    /** @type {import('@milkdown/crepe').Crepe | null} */
    let crepeInstance = null
    const blockEditConfig = {
      businessMenuGroups: [],
      blockHandle: null,
      buildMenu(groupBuilder) {
        appendBusinessGroupsToBlockMenu(
          groupBuilder,
          blockEditConfig.businessMenuGroups,
          insertBusinessContentAtSelection
        )
      }
    }

    async function refreshBusinessBlockMenus(projectId) {
      try {
        blockEditConfig.businessMenuGroups = await loadReportBusinessBlockMenuGroups(projectId)
      } catch (_e) {
        blockEditConfig.businessMenuGroups = []
      }
    }

    function insertBusinessContentAtSelection(ctx, content) {
      if (!content) {
        return
      }
      if (isPlanStatisticsTableContent(content)) {
        Message.info(
          i18n.t('mk.plan-table-insert-hint') ||
            '计划统计大表已插入；表格结构请在「源码」模式中编辑，计划变量将完整保留。'
        )
      }
      const view = ctx.get(editorViewCtx)
      if (!view) {
        return
      }
      const { from, to } = view.state.selection
      const tr = view.state.tr.insertText(content, from, to)
      view.dispatch(tr)
      view.focus()
    }

    function refreshPreviewViews() {
      const editor = get()
      if (!editor || editorBooting.value) {
        return
      }
      try {
        editor.action(forceUpdate())
      } catch (_e) {
        // editor may not be ready yet
      }
    }

    function syncPreviewData(projectId) {
      if (!projectId) {
        return
      }
      ensureReportPreviewData(projectId).finally(() => {
        refreshPreviewViews()
      })
    }

    watch(() => props.projectId, (id) => {
      syncPreviewData(id)
      refreshBusinessBlockMenus(id)
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
        refreshPreviewViews()
      })
    })

    onUnmounted(() => {
      themeObserver?.disconnect()
      unsubscribePreviewData?.()
      uninstallBlockDragImageFix?.()
      uninstallBlockAddOverlay?.()
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
          [CrepeFeature.Toolbar]: false,
          [CrepeFeature.TopBar]: false,
          // 避免 remark-math 干扰 $table{...} / ${...} DSL
          [CrepeFeature.Latex]: false
        },
        featureConfigs: {
          [CrepeFeature.Placeholder]: {
            text: '开始编写报告模板…',
            mode: 'doc'
          },
          [CrepeFeature.BlockEdit]: blockEditConfig
        }
      })

      useReportDslPlugins(crepe.editor)

      crepe.on((listener) => {
        listener.markdownUpdated((_ctx, markdown) => {
          if (syncing.value) {
            return
          }
          const normalized = normalizeDslSource(markdown || '')
          emit('update:modelValue', normalized)
          emit('change', normalized)
        })
      })

      crepeInstance = markRaw(crepe)
      return crepeInstance
    })

    function syncModelToEditor() {
      if (editorBooting.value || !crepeInstance) {
        return
      }
      const editor = get()
      if (!editor) {
        return
      }
      const next = normalizeDslSource(props.modelValue || '')
      let current = ''
      try {
        current = normalizeDslSource(editor.action(getMarkdown()) || '')
      } catch (_e) {
        return
      }
      if (current === next) {
        return
      }
      syncing.value = true
      try {
        editor.action(replaceAll(next, true))
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
      return [
        ...buildDefaultBlockMenuGroups(),
        ...sortBusinessMenuGroups(
          businessGroupsToOverlayGroups(
            blockEditConfig.businessMenuGroups,
            insertBusinessContentAtSelection
          )
        )
      ]
    }

    watch(editorBooting, (isLoading) => {
      if (!isLoading) {
        uninstallBlockDragImageFix?.()
        uninstallBlockDragImageFix = installBlockDragImageFix()
        uninstallBlockAddOverlay?.()
        uninstallBlockAddOverlay = installReportBlockAddOverlay({
          getMenuGroups: getOverlayMenuGroups,
          runEditorAction: (fn) => {
            get()?.action(fn)
          },
          focusEditor: () => focus(),
          isDark: () => isDark.value
        })
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
      editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        if (!view) {
          return
        }
        const { from, to } = view.state.selection
        const tr = view.state.tr.insertText(text, from, to)
        view.dispatch(tr)
        view.focus()
      })
    }

    function focus() {
      get()?.action((ctx) => {
        ctx.get(editorViewCtx)?.focus()
      })
    }

    expose({ insertMarkdown, focus, refreshPreviewViews })

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
