<template>
  <div class="report-template-toolbar">
    <input
      ref="importInput"
      type="file"
      class="report-template-toolbar__import-input"
      accept=".md,.markdown,.txt,text/markdown"
      @change="onImportFile"
    />
    <tools-menu :tools="tools" :ellipsis="false" @select="onToolSelect" />
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
  emits: ['insert', 'clear', 'save', 'import', 'component-tool'],
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
        } else if (tool.name === 'save') {
          this.$emit('save')
        } else if (tool.name === 'import-markdown') {
          this.$refs.importInput?.click()
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
    },
    onImportFile(event) {
      const file = event.target.files?.[0]
      if (!file) {
        return
      }
      const reader = new FileReader()
      reader.onload = (e) => {
        this.$emit('import', e.target?.result || '')
        event.target.value = ''
      }
      reader.onerror = () => {
        this.$message.error(this.$t('upload.failed') || '文件读取失败')
        event.target.value = ''
      }
      reader.readAsText(file, 'UTF-8')
    }
  }
}
</script>

<style scoped>
.report-template-toolbar {
  width: 100%;
  min-width: 0;
}

.report-template-toolbar__import-input {
  display: none;
}

.report-template-toolbar :deep(.tools-menu) {
  border: none;
  background: transparent;
  width: 100%;
  height: auto !important;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
}

.report-template-toolbar :deep(.el-menu--horizontal) {
  height: auto !important;
  border-bottom: none !important;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
}

.report-template-toolbar :deep(.el-menu--horizontal > .el-menu-item),
.report-template-toolbar :deep(.el-menu--horizontal > .el-sub-menu) {
  float: none;
  flex-shrink: 0;
  width: 30px;
  min-width: 30px;
  padding: 0 !important;
  margin: 0 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.report-template-toolbar :deep(.el-menu--horizontal > .el-sub-menu .el-sub-menu__title) {
  width: 30px;
  min-width: 30px;
  padding: 0 !important;
  margin: 0 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-bottom: none !important;
}

.report-template-toolbar :deep(.tools-menu .siding) {
  float: none;
  flex-shrink: 0;
  margin: 0 4px;
}

.report-template-toolbar :deep(.tools-menu .icon),
.report-template-toolbar :deep(.tools-menu .svg-icon) {
  color: var(--el-text-color-regular, #606266);
}

.report-template-toolbar :deep(.tools-menu .svg-icon path) {
  fill: currentColor;
}

html.dark .report-template-toolbar :deep(.tools-menu .icon),
html.dark .report-template-toolbar :deep(.tools-menu .svg-icon) {
  color: #a3a6ad;
}
</style>
