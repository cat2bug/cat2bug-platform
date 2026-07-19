<template>
  <div class="report-template-toolbar" @mousedown.prevent>
    <input
      ref="importInput"
      type="file"
      class="report-template-toolbar__import-input"
      accept=".md,.markdown,.txt,text/markdown"
      @change="onImportFile"
    />
    <tools-menu :tools="tools" :ellipsis="false" @select="onToolSelect" />
    <el-dialog
      :title="$t('mk.add-link')"
      v-model="linkDialogVisible"
      append-to-body
      width="500px"
      @closed="onLinkDialogClosed"
    >
      <el-form :model="linkForm" @submit.prevent>
        <el-form-item :label="$t('mk.link-describe')" label-width="120px">
          <el-input v-model="linkForm.describe" autocomplete="off" maxlength="255" />
        </el-form-item>
        <el-form-item :label="$t('mk.link-url')" label-width="120px">
          <el-input v-model="linkForm.url" autocomplete="off" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelLinkDialog">{{ $t('cancel') }}</el-button>
          <el-button type="primary" @click="confirmLinkDialog">{{ $t('ok') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import ToolsMenu from '@/components/Cat2BugMarkdown/components/ToolsMenu'
import { loadReportToolbarMenus } from './composables/toolbarMenuLoader'
import { resolveToolContent } from './composables/useReportTemplateTools'
import { getVariablesExpanded, setVariablesExpanded } from './milkdown/variable-expand-state'
import {
  formatMarkdownLink,
  setLinkInsertRequester
} from './milkdown/link-insert-bridge'

const VARIABLES_EXPAND_TOOL_NAME = 'variables-expand-collapse'

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
      tools: [],
      linkDialogVisible: false,
      linkForm: {
        describe: '',
        url: ''
      },
      /** @type {null | ((value: string|null) => void)} */
      linkDialogResolve: null
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
  mounted() {
    setLinkInsertRequester(() => this.openLinkDialog())
  },
  beforeUnmount() {
    setLinkInsertRequester(null)
    this.resolveLinkDialog(null)
  },
  methods: {
    async loadMenus(projectId) {
      this.tools = await loadReportToolbarMenus(projectId)
      this.syncVariablesExpandTool()
    },
    syncVariablesExpandTool() {
      const tool = this.tools.find((t) => t.name === VARIABLES_EXPAND_TOOL_NAME)
      if (tool) {
        tool.check = getVariablesExpanded()
      }
    },
    openLinkDialog() {
      return new Promise((resolve) => {
        this.resolveLinkDialog(null)
        this.linkDialogResolve = resolve
        this.linkForm = { describe: '', url: '' }
        this.linkDialogVisible = true
      })
    },
    resolveLinkDialog(value) {
      if (typeof this.linkDialogResolve === 'function') {
        const resolve = this.linkDialogResolve
        this.linkDialogResolve = null
        resolve(value)
      }
    },
    confirmLinkDialog() {
      const markdown = formatMarkdownLink(this.linkForm.describe, this.linkForm.url)
      this.linkDialogVisible = false
      this.resolveLinkDialog(markdown)
    },
    cancelLinkDialog() {
      this.linkDialogVisible = false
      this.resolveLinkDialog(null)
    },
    onLinkDialogClosed() {
      // Esc / mask click — treat as cancel if still pending
      this.resolveLinkDialog(null)
    },
    onToolSelect(_tools, tool) {
      // Keep editor selection: blur would make insert fall back to document end.
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
        if (tool.name === 'link') {
          this.openLinkDialog().then((markdown) => {
            if (markdown) {
              this.$emit('insert', markdown)
            }
          })
          return
        }
        this.$emit('component-tool', tool)
        this.$message.info(
          this.$t('mk.use-source-mode-for-component') ||
            '链接/上传图片请使用源码模式'
        )
        return
      }
      if (tool.type === 'check') {
        if (tool.name === VARIABLES_EXPAND_TOOL_NAME) {
          setVariablesExpanded(!!tool.check)
        }
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
  display: flex;
  align-items: center;
}

.report-template-toolbar__import-input {
  display: none;
}

.report-template-toolbar :deep(.tools-menu) {
  border: none;
  background: transparent;
  width: 100%;
  height: auto !important;
  min-height: 30px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
}

.report-template-toolbar :deep(.el-menu--horizontal) {
  height: auto !important;
  min-height: 30px;
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
  height: 30px;
  min-width: 30px;
  padding: 0 !important;
  margin: 0 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

.report-template-toolbar :deep(.el-menu--horizontal > .el-sub-menu .el-sub-menu__title) {
  width: 30px;
  height: 30px;
  min-width: 30px;
  padding: 0 !important;
  margin: 0 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-bottom: none !important;
  border-radius: 6px;
  line-height: 1;
}

.report-template-toolbar :deep(.tools-menu .siding) {
  float: none;
  flex-shrink: 0;
  align-self: center;
  margin: 0 4px;
}

.report-template-toolbar :deep(.tools-menu .row) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.report-template-toolbar :deep(.tools-menu .icon),
.report-template-toolbar :deep(.tools-menu .svg-icon) {
  color: var(--el-text-color-regular, #606266);
}

.report-template-toolbar :deep(.tools-menu .svg-icon path) {
  fill: currentColor;
}

.report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover),
.report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu:hover > .el-sub-menu__title),
.report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:hover) {
  color: var(--el-color-primary, #409eff) !important;
  background-color: var(--el-color-primary-light-9, #ecf5ff) !important;
  border-color: transparent !important;
}

.report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:focus),
.report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item.is-active),
.report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:focus),
.report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title) {
  outline: none !important;
  color: var(--el-text-color-regular, #606266) !important;
  background-color: transparent !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

html.dark .report-template-toolbar :deep(.tools-menu .icon),
html.dark .report-template-toolbar :deep(.tools-menu .svg-icon) {
  color: #a3a6ad;
}

html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover),
html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu:hover > .el-sub-menu__title),
html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:hover) {
  color: #f4bd6f !important;
  background-color: rgba(244, 189, 111, 0.14) !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:focus),
html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item.is-active),
html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:focus),
html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title) {
  color: #a3a6ad !important;
  background-color: transparent !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover .icon),
html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover .svg-icon),
html.dark .report-template-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu:hover .svg-icon) {
  color: #f4bd6f !important;
}

html.dark .report-template-toolbar :deep(.tools-menu .siding) {
  background-color: #363637;
}
</style>

<style lang="scss">
/* 子菜单 teleport 到 body，须全局暗色覆盖 */
html.dark .el-menu.el-menu--popup.el-popper {
  .tools-row .el-menu-item {
    color: var(--text-color-regular, #a3a6ad) !important;

    &:hover,
    &:focus {
      color: #ffc107 !important;
    }

    .row span {
      color: inherit !important;
    }
  }
}
</style>
