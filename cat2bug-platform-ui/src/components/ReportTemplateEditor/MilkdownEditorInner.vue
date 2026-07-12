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
    /** @type {import('@milkdown/crepe').Crepe | null} */
    let crepeInstance = null

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
      crepeInstance = null
    })

    const { get, loading: editorBooting } = useEditor((root) => {
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
          [CrepeFeature.BlockEdit]: {
            blockHandle: {
              root,
              // placement=left 时正数将手柄推向左侧 gutter
              getOffset: () => 20,
              floatingUIOptions: {
                strategy: 'absolute'
              }
            }
          }
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

    watch(editorBooting, (isLoading) => {
      if (!isLoading) {
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
