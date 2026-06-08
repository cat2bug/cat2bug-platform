/**
 * CloudCase：主抽屉 + 批量导入弹框键盘集成。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import defectToolDialogClose from '@/mixins/defect-tool-dialog-close'
import formFieldHints from '@/mixins/form-field-hints'
import cloudCaseDrawerClose from '@/mixins/cloud-case-drawer-close'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export default {
  mixins: [dialogFormShortcuts, defectToolDialogClose, formFieldHints, cloudCaseDrawerClose],
  computed: {
    $_formShortcutSurfaceVisible() {
      return this.visible === true || this.importDialogVisible === true
    },
    $_fieldHintSurfaceVisible() {
      return this.visible === true || this.importDialogVisible === true
    }
  },
  methods: {
    onImportDialogOpened() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.captureToolDialogCloseBaseline()
          this.$_focusCloudCaseImportFirstField()
        })
      })
    },
    captureToolDialogCloseBaseline() {
      if (!this.importDialogVisible) return
      this.toolDialogCloseBaseline = JSON.stringify({ ...(this.importForm || {}) })
    },
    isToolDialogCloseDirty() {
      if (!this.importDialogVisible || !this.toolDialogCloseBaseline) return false
      return JSON.stringify({ ...(this.importForm || {}) }) !== this.toolDialogCloseBaseline
    },
    $_focusCloudCaseImportFirstField() {
      const container = this.getFieldHintContainer()
      if (!container) return
      const first = getLogicalTabStopsInScope(container)[0]
      if (!first) return
      if (typeof this.$_focusControl === 'function') {
        this.$_focusControl(first)
      } else if (typeof first.focus === 'function') {
        try {
          first.focus({ preventScroll: false })
        } catch (e) {
          first.focus()
        }
      }
    },
    shortcutSave() {
      if (this.importDialogVisible) {
        this.submitImportForm()
        return
      }
      this.searchHandle()
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      if (this.importDialogVisible) {
        this.requestCloseToolDialog()
        return
      }
      this.requestCloseCloudCaseDrawer()
    },
    getFieldHintContainer() {
      if (this.importDialogVisible) {
        const form = this.$refs.form
        return (form && form.$el) || this.$el
      }
      const search = this.$refs.caseSearch
      return search || this.$el
    },
    getFieldHintScrollContainer() {
      if (this.importDialogVisible) {
        const body = this.$el && this.$el.querySelector('.el-dialog__body')
        return body || this.getFieldHintContainer()
      }
      const drawerBody = this.$el && this.$el.querySelector('.el-drawer__body')
      return drawerBody || this.getFieldHintContainer()
    },
    doCloseToolDialog() {
      this.importDialogVisible = false
      this.toolDialogCloseBaseline = null
    }
  }
}
