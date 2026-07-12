<template>
  <div class="report-template-toolbar">
    <tools-menu :tools="tools" @select="onToolSelect" />
  </div>
</template>

<script>
import ToolsMenu from '@/components/Cat2BugMarkdown/components/ToolsMenu'
import { loadReportToolbarMenus } from './composables/toolbarMenuLoader'
import { resolveToolContent } from './composables/useReportTemplateTools'

export default {
  name: 'ReportTemplateToolbar',
  components: { ToolsMenu },
  props: {
    projectId: {
      type: [Number, String],
      default: null
    }
  },
  emits: ['insert', 'clear', 'component-tool'],
  data() {
    return {
      tools: []
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(id) {
        this.loadMenus(id)
      }
    }
  },
  methods: {
    async loadMenus(projectId) {
      this.tools = await loadReportToolbarMenus(projectId)
    },
    onToolSelect(_tools, tool) {
      if (!tool) {
        return
      }
      if (tool.type === 'command') {
        if (tool.name === 'clear') {
          this.$emit('clear')
        }
        return
      }
      if (tool.type === 'component') {
        this.$emit('component-tool', tool)
        this.$message.info(this.$t('mk.use-source-mode-for-component') || '链接/上传图片请使用源码模式')
        return
      }
      if (tool.type === 'check') {
        return
      }
      const content = resolveToolContent(tool)
      if (content) {
        this.$emit('insert', content)
      }
    }
  }
}
</script>

<style scoped>
.report-template-toolbar {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}

.report-template-toolbar :deep(.tools-menu) {
  border: none;
  background: transparent;
}
</style>
