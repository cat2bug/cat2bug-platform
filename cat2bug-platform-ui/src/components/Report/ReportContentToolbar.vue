<template>
  <div class="report-content-toolbar" @mousedown.prevent>
    <tools-menu :tools="tools" :ellipsis="false" @select="onToolSelect" />
    <div class="report-content-toolbar__status" aria-live="polite">
      <span v-if="saving" class="is-muted">{{ $t('report.saving') }}</span>
      <span v-else-if="dirty" class="is-dirty">{{ $t('report.unsaved') }}</span>
      <span v-else-if="statusText" class="is-muted">{{ statusText }}</span>
    </div>
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
import { loadReportContentToolbarMenus } from './reportContentToolbarMenu'
import {
  formatMarkdownLink,
  setLinkInsertRequester
} from '@/components/ReportTemplateEditor/milkdown/link-insert-bridge'

export default {
  name: 'ReportContentToolbar',
  components: { ToolsMenu },
  props: {
    dirty: {
      type: Boolean,
      default: false
    },
    saving: {
      type: Boolean,
      default: false
    },
    statusText: {
      type: String,
      default: ''
    }
  },
  emits: ['insert', 'insert-tool', 'save', 'clear'],
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
  created() {
    this.tools = loadReportContentToolbarMenus()
  },
  mounted() {
    setLinkInsertRequester(() => this.openLinkDialog())
  },
  beforeUnmount() {
    setLinkInsertRequester(null)
    this.resolveLinkDialog(null)
  },
  methods: {
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
      this.resolveLinkDialog(null)
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
        this.$message.info(
          this.$t('mk.use-source-mode-for-component') ||
            '请通过链接插入图片，或直接粘贴 Markdown 图片语法'
        )
        return
      }
      // Parent wraps selection for {0} placeholders (same as Cat2BugMarkdown).
      this.$emit('insert-tool', tool)
    }
  }
}
</script>

<style scoped>
.report-content-toolbar {
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  box-sizing: border-box;
  border-bottom: 1px solid var(--el-border-color, #dcdfe6);
  background: var(--el-bg-color, #fff);
  position: sticky;
  top: 0;
  z-index: 15;
}

.report-content-toolbar__status {
  flex-shrink: 0;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.report-content-toolbar__status .is-muted {
  color: var(--el-text-color-secondary, #909399);
}

.report-content-toolbar__status .is-dirty {
  color: var(--el-color-warning, #e6a23c);
}

.report-content-toolbar :deep(.tools-menu) {
  border: none;
  background: transparent;
  width: 100%;
  height: auto !important;
  min-height: 30px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.report-content-toolbar :deep(.el-menu--horizontal) {
  height: auto !important;
  min-height: 30px;
  border-bottom: none !important;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
}

.report-content-toolbar :deep(.el-menu--horizontal > .el-menu-item),
.report-content-toolbar :deep(.el-menu--horizontal > .el-sub-menu) {
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

.report-content-toolbar :deep(.el-menu--horizontal > .el-sub-menu .el-sub-menu__title) {
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

.report-content-toolbar :deep(.tools-menu .siding) {
  float: none;
  flex-shrink: 0;
  align-self: center;
  margin: 0 4px;
}

.report-content-toolbar :deep(.tools-menu .row) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.report-content-toolbar :deep(.tools-menu .icon),
.report-content-toolbar :deep(.tools-menu .svg-icon) {
  color: var(--el-text-color-regular, #606266);
}

.report-content-toolbar :deep(.tools-menu .svg-icon path) {
  fill: currentColor;
}

.report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover),
.report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu:hover > .el-sub-menu__title),
.report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:hover) {
  color: var(--el-color-primary, #409eff) !important;
  background-color: var(--el-color-primary-light-9, #ecf5ff) !important;
  border-color: transparent !important;
}

.report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:focus),
.report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item.is-active),
.report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:focus),
.report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title) {
  outline: none !important;
  color: var(--el-text-color-regular, #606266) !important;
  background-color: transparent !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

html.dark .report-content-toolbar {
  background: #1d1e1f;
  border-bottom-color: #363637;
}

html.dark .report-content-toolbar :deep(.tools-menu .icon),
html.dark .report-content-toolbar :deep(.tools-menu .svg-icon) {
  color: #a3a6ad;
}

html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover),
html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu:hover > .el-sub-menu__title),
html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:hover) {
  color: #f4bd6f !important;
  background-color: rgba(244, 189, 111, 0.14) !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:focus),
html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item.is-active),
html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:focus),
html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title) {
  color: #a3a6ad !important;
  background-color: transparent !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover .icon),
html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-menu-item:hover .svg-icon),
html.dark .report-content-toolbar :deep(.tools-menu.el-menu--horizontal > .el-sub-menu:hover .svg-icon) {
  color: #f4bd6f !important;
}

html.dark .report-content-toolbar :deep(.tools-menu .siding) {
  background-color: #363637;
}
</style>
