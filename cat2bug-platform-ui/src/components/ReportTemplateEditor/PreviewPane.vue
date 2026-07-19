<template>
  <div class="report-template-preview" v-loading="loading">
    <markdown-renderer
      ref="markdownView"
      class="report-template-preview__body markdown-body"
      :content="content"
      :options="options"
    />
  </div>
</template>

<script>
import { MarkdownRenderer } from '@/components/Markdown'
import { setupReportMarkdownPlugins } from './composables/useReportMarkdownPlugins'

export default {
  name: 'PreviewPane',
  components: { MarkdownRenderer },
  props: {
    content: {
      type: String,
      default: ''
    },
    projectId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      loading: true,
      options: {
        markdownIt: {
          html: true,
          linkify: true
        }
      }
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(id) {
        if (id) {
          this.initPlugins(id)
        }
      }
    }
  },
  methods: {
    async initPlugins(projectId) {
      this.loading = true
      try {
        await this.$nextTick()
        await setupReportMarkdownPlugins(this.$refs.markdownView, projectId)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.report-template-preview {
  height: 100%;
  overflow: auto;
  padding: 12px;
  background: var(--el-bg-color);
}

.report-template-preview__body {
  min-height: 100%;
}
</style>
