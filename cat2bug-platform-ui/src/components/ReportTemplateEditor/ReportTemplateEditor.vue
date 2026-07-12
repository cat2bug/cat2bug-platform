<template>
  <div class="report-template-editor" :class="{ 'report-template-editor--dark': isDark }">
    <div class="report-template-editor__toolbar markdown-tools">
      <report-template-toolbar
        class="report-template-editor__menus"
        :project-id="projectId"
        @insert="onToolbarInsert"
        @clear="clear"
        @save="$emit('save')"
        @import="onImport"
      />
      <div class="report-template-editor__view-tools">
        <div
          class="report-template-editor__view-mode"
          role="radiogroup"
          aria-label="编辑视图"
        >
          <button
            v-for="option in viewModeOptions"
            :key="option.value"
            type="button"
            class="report-template-editor__view-mode-segment"
            :class="{ 'is-active': viewMode === option.value }"
            :title="option.label"
            role="radio"
            :aria-checked="viewMode === option.value"
            @click="setViewMode(option.value)"
          >
            <svg-icon :icon-class="option.icon" />
          </button>
        </div>
      </div>
    </div>

    <multipane layout="vertical" class="report-template-editor__body markdown-body">
      <div
        class="report-template-editor__edit markdown-body-edit"
        :class="{ 'markdown-body-edit--single': viewMode !== 'split' }"
      >
        <md-editor-pane
          v-if="viewMode === 'source'"
          ref="sourceEditor"
          v-model="editorContent"
          @change="onEditorChange"
        />
        <milkdown-editor
          v-else
          ref="milkdownEditor"
          :project-id="projectId"
          v-model="editorContent"
          @change="onEditorChange"
        />
      </div>
      <multipane-resizer v-show="viewMode === 'split'" class="markdown-body-resizer" />
      <md-editor-pane
        v-if="viewMode === 'split'"
        class="report-template-editor__source markdown-body-view"
        :show-code-row-number="true"
        v-model="editorContent"
        @change="onEditorChange"
      />
    </multipane>
  </div>
</template>

<script>
import { Multipane, MultipaneResizer } from '@/vendor/vue-multipane'
import MdEditorPane from '@/components/Markdown/MdEditorPane.vue'
import MilkdownEditor from './MilkdownEditor.vue'
import ReportTemplateToolbar from './ReportTemplateToolbar.vue'
import { useSettingsStore } from '@/stores/settings'
import { useUserStore } from '@/stores/user'
import { isReportEditorDarkMode } from './report-editor-theme'
import { normalizeDslSource } from './dsl/normalize'
import { isPlanStatisticsTableContent } from './dsl/complex-blocks'
import { ensureReportPreviewData, getReportPreviewState } from './milkdown/report-preview-data'
import './milkdown-crepe-theme.scss'

export default {
  name: 'ReportTemplateEditor',
  components: {
    Multipane,
    MultipaneResizer,
    MdEditorPane,
    MilkdownEditor,
    ReportTemplateToolbar
  },
  props: {
    content: {
      type: String,
      default: ''
    },
    template: {
      type: Object,
      default: () => ({})
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  emits: ['update:content', 'input', 'save'],
  data() {
    return {
      editorContent: '',
      viewMode: 'split',
      planOptions: []
    }
  },
  computed: {
    projectId() {
      const fromTemplate = this.template?.projectId
      if (fromTemplate != null && fromTemplate !== '') {
        return fromTemplate
      }
      const store = useUserStore()
      return store.config?.currentProjectId ?? null
    },
    isDark() {
      const store = useSettingsStore()
      return store.themeMode === 'dark' || isReportEditorDarkMode()
    },
    viewModeOptions() {
      return [
        { value: 'visual', icon: 'mk-paragraph', label: '可视化' },
        { value: 'source', icon: 'mk-code', label: '源码' },
        { value: 'split', icon: 'split', label: '可视化和源码' }
      ]
    }
  },
  mounted() {
    this.themeObserver = new MutationObserver(() => {
      this.$forceUpdate()
    })
    this.themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    })
  },
  beforeUnmount() {
    this.themeObserver?.disconnect()
  },
  watch: {
    content: {
      immediate: true,
      handler(value) {
        const next = normalizeDslSource(value || '')
        if (next !== this.editorContent) {
          this.editorContent = next
        }
      }
    },
    projectId: {
      immediate: true,
      handler(id) {
        if (id) {
          this.loadPlanOptions(id)
        }
      }
    }
  },
  methods: {
    async loadPlanOptions(projectId) {
      const id = projectId ?? this.projectId
      if (!id) {
        ensureReportPreviewData(null)
        this.planOptions = []
        return
      }
      try {
        await ensureReportPreviewData(id)
        this.planOptions = getReportPreviewState().data?.plan?.list || []
        this.$nextTick(() => {
          this.$refs.milkdownEditor?.refreshPreviewViews?.()
        })
      } catch (_e) {
        this.planOptions = []
        this.$nextTick(() => {
          this.$refs.milkdownEditor?.refreshPreviewViews?.()
        })
      }
    },
    onToolbarInsert(content) {
      if (isPlanStatisticsTableContent(content)) {
        this.$message.info(
          this.$t('mk.plan-table-insert-hint') ||
            '计划统计大表已插入；表格结构请在「源码」模式中编辑，计划变量将完整保留。'
        )
      }
      const prefix = this.editorContent && !this.editorContent.endsWith('\n') ? '\n' : ''
      this.editorContent = `${this.editorContent || ''}${prefix}${content}\n`
      this.emitContent()
    },
    onImport(content) {
      this.editorContent = normalizeDslSource(content || '')
      this.emitContent()
    },
    onEditorChange() {
      const next = normalizeDslSource(this.editorContent)
      if (next !== this.editorContent) {
        this.editorContent = next
      }
      this.emitContent()
    },
    emitContent() {
      const content = normalizeDslSource(this.editorContent)
      this.$emit('update:content', content)
      this.$emit('input', content)
    },
    setViewMode(mode) {
      this.viewMode = mode
    },
    focusEditor() {
      if (this.viewMode === 'source' && this.$refs.sourceEditor) {
        this.$refs.sourceEditor.focus()
      } else if (this.$refs.milkdownEditor) {
        this.$refs.milkdownEditor.focus()
      }
    },
    clear() {
      this.editorContent = ''
      this.emitContent()
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

.report-template-editor {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 4px;
  box-shadow: rgba(0, 0, 0, 0.1) 0 2px 12px 0;
  min-height: 400px;
}

.report-template-editor__toolbar {
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  padding: 5px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;

  :deep(.el-submenu__icon-arrow) {
    display: none !important;
  }
}

.report-template-editor__menus {
  flex: 1;
  min-width: 0;
}

.report-template-editor__view-tools {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  flex-shrink: 0;
  padding-top: 2px;
}

.report-template-editor__view-mode {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light, #f0f2f5);
  gap: 0;
}

.report-template-editor__view-mode-segment {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 28px;
  padding: 0 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;

  .svg-icon {
    width: 16px;
    height: 16px;
  }

  &:hover:not(.is-active) {
    color: var(--el-text-color-primary);
    background: var(--el-fill-color, rgba(0, 0, 0, 0.04));
  }

  &.is-active {
    background: var(--el-color-primary);
    color: var(--el-color-primary-light-9, #1e1e20);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.12);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 1px;
  }
}

html.dark .report-template-editor .report-template-editor__view-mode,
.report-template-editor--dark .report-template-editor__view-mode {
  border-color: #363637;
  background: #252529;
}

html.dark .report-template-editor .report-template-editor__view-mode-segment,
.report-template-editor--dark .report-template-editor__view-mode-segment {
  color: #a3a6ad;

  &:hover:not(.is-active) {
    color: #e5eaf3;
    background: #2d2d31;
  }

  &.is-active {
    background: #f4bd6f;
    color: #1e1e20;
  }
}

.report-template-editor__body {
  flex: 1;
  min-height: 0;
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  background-color: #f6f6f6;
  user-select: none;
  overflow: hidden;
  height: 100%;

  > * {
    min-height: 100px;
  }

  > .report-template-editor__edit,
  > .report-template-editor__source {
    user-select: text;
  }

  > .report-template-editor__edit {
    padding: 0 !important;
    background-color: transparent !important;
    width: calc(50% - 5px);
    flex-shrink: 0;
    margin-right: 5px;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }

  > .report-template-editor__edit :deep(.report-milkdown-host) {
    flex: 1;
    min-height: 0;
    border: none;
    padding: 0;
    background: transparent;
    display: flex;
    flex-direction: column;
  }

  > .markdown-body-edit--single {
    width: auto;
    flex: 1 !important;
    margin-right: 0;
  }

  > .report-template-editor__source {
    flex: 1;
    min-width: 0;
    background-color: #fcfcfc;
    padding: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  > .markdown-body-resizer {
    flex-shrink: 0;
    width: 8px;
    margin: 0 0 0 -8px;
    left: 4px;
    cursor: col-resize;
    position: relative;
    z-index: 100;
    @include multipane-resizer-vertical-appearance;
  }
}

.report-template-editor--dark .report-template-editor__body {
  background-color: #1e1e20;
}

html.dark .report-template-editor .report-template-editor__body {
  background-color: #1e1e20;
}

html.dark .report-template-editor .report-template-editor__edit,
.report-template-editor--dark .report-template-editor__edit {
  background-color: #1e1e20 !important;
}

html.dark .report-template-editor .report-template-editor__source,
.report-template-editor--dark .report-template-editor__source {
  background-color: #2a2a2e !important;
}
</style>
