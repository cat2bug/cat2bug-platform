<template>
  <div class="report-template-editor">
    <div class="report-template-editor__toolbar">
      <report-template-toolbar
        :project-id="projectId"
        @insert="onToolbarInsert"
        @clear="clear"
      />
      <el-button-group class="report-template-editor__mode">
        <el-button
          size="small"
          :type="!sourceMode ? 'primary' : 'default'"
          @click="sourceMode = false"
        >
          {{ $t('mk.visual-mode') || '可视化' }}
        </el-button>
        <el-button
          size="small"
          :type="sourceMode ? 'primary' : 'default'"
          @click="enableSourceMode"
        >
          {{ $t('mk.source-mode') || '源码' }}
        </el-button>
      </el-button-group>
      <el-button
        size="small"
        :type="previewVisible ? 'primary' : 'default'"
        class="report-template-editor__preview-toggle"
        @click="previewVisible = !previewVisible"
      >
        {{ previewVisible ? ($t('mk.hide-preview') || '隐藏预览') : ($t('mk.show-preview') || '显示预览') }}
      </el-button>
    </div>

    <multipane layout="vertical" class="report-template-editor__body" @pane-resize-stop="onPaneResize">
      <div class="report-template-editor__edit" :style="editPaneStyle">
        <md-editor-pane
          v-if="sourceMode"
          ref="sourceEditor"
          v-model="sourceContent"
          @change="onSourceChange"
        />
        <segment-canvas
          v-else
          :segments="segments"
          :plan-options="planOptions"
          @update:segments="onSegmentsChange"
        />
      </div>
      <multipane-resizer v-show="previewVisible" />
      <preview-pane
        v-show="previewVisible"
        class="report-template-editor__preview"
        :style="previewPaneStyle"
        :content="serializedContent"
        :project-id="projectId"
      />
    </multipane>
  </div>
</template>

<script>
import { Multipane, MultipaneResizer } from '@/vendor/vue-multipane'
import MdEditorPane from '@/components/Markdown/MdEditorPane.vue'
import SegmentCanvas from './SegmentCanvas.vue'
import PreviewPane from './PreviewPane.vue'
import ReportTemplateToolbar from './ReportTemplateToolbar.vue'
import { parseTemplate, serializeTemplate } from './dsl'
import { appendToolContent } from './composables/useReportTemplateTools'
import { markdownData } from '@/api/markdown/markdown'

export default {
  name: 'ReportTemplateEditor',
  components: {
    Multipane,
    MultipaneResizer,
    MdEditorPane,
    SegmentCanvas,
    PreviewPane,
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
  emits: ['update:content', 'input'],
  data() {
    return {
      segments: [],
      sourceMode: false,
      sourceContent: '',
      previewVisible: true,
      planOptions: [],
      editPaneStyle: { flex: '1 1 50%', minWidth: '280px' },
      previewPaneStyle: { flex: '1 1 50%', minWidth: '280px' }
    }
  },
  computed: {
    projectId() {
      return this.template?.projectId
    },
    serializedContent() {
      if (this.sourceMode) {
        return this.sourceContent
      }
      return serializeTemplate(this.segments)
    }
  },
  watch: {
    content: {
      immediate: true,
      handler(value) {
        if (this.sourceMode) {
          this.sourceContent = value || ''
        } else {
          this.segments = parseTemplate(value || '')
          if (!this.segments.length && this.placeholder) {
            this.segments = [{ type: 'text', value: '' }]
          }
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
      try {
        const res = await markdownData(projectId)
        this.planOptions = res.data?.plan?.list || []
      } catch (_e) {
        this.planOptions = []
      }
    },
    onToolbarInsert(content) {
      this.segments = appendToolContent(this.segments, content)
      this.emitContent()
    },
    onSegmentsChange(segments) {
      this.segments = segments
      this.emitContent()
    },
    onSourceChange() {
      this.emitContent()
    },
    emitContent() {
      const value = this.serializedContent
      this.$emit('update:content', value)
      this.$emit('input', value)
    },
    enableSourceMode() {
      this.sourceContent = serializeTemplate(this.segments)
      this.sourceMode = true
    },
    onPaneResize(_pane, _resizer, sizes) {
      if (sizes && sizes.length >= 2) {
        this.editPaneStyle = { flex: `0 0 ${sizes[0]}%`, minWidth: '280px' }
        this.previewPaneStyle = { flex: `0 0 ${sizes[1]}%`, minWidth: '280px' }
      }
    },
    focusEditor() {
      if (this.sourceMode && this.$refs.sourceEditor) {
        this.$refs.sourceEditor.focus()
      }
    },
    clear() {
      this.segments = [{ type: 'text', value: '' }]
      this.sourceContent = ''
      this.emitContent()
    }
  }
}
</script>

<style scoped>
.report-template-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
  min-height: 400px;
}

.report-template-editor__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  flex-wrap: wrap;
}

.report-template-editor__mode {
  margin-left: auto;
  flex-shrink: 0;
}

.report-template-editor__preview-toggle {
  flex-shrink: 0;
}

.report-template-editor__body {
  flex: 1;
  min-height: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}

.report-template-editor__edit,
.report-template-editor__preview {
  overflow: auto;
  height: 100%;
}

.report-template-editor__edit {
  display: flex;
  flex-direction: column;
}
</style>
