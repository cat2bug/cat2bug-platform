/**
 * 统计模块弹框：Esc / 取消 / 关闭按钮前，若有未保存修改则确认。
 */
import { serializeToolDialogFormCloseState } from '@/utils/defect-tool-dialog-close-state'

export default {
  data() {
    return {
      toolDialogCloseBaseline: null
    }
  },
  methods: {
    getStatisticDialogCloseSnapshot() {
      return this.form
    },
    serializeStatisticDialogCloseSnapshot(snapshot) {
      return serializeToolDialogFormCloseState(snapshot)
    },
    captureToolDialogCloseBaseline() {
      this.toolDialogCloseBaseline = this.serializeStatisticDialogCloseSnapshot(
        this.getStatisticDialogCloseSnapshot()
      )
    },
    isToolDialogCloseDirty() {
      if (!this.toolDialogCloseBaseline) return false
      return this.serializeStatisticDialogCloseSnapshot(
        this.getStatisticDialogCloseSnapshot()
      ) !== this.toolDialogCloseBaseline
    },
    confirmCloseToolDialog() {
      if (!this.isToolDialogCloseDirty()) {
        return Promise.resolve(true)
      }
      return this.$modal.confirm(
        this.$i18n.t('defect.unsaved-close-confirm').toString(),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: this.$i18n.t('ok').toString(),
          cancelButtonText: this.$i18n.t('cancel').toString(),
          type: 'warning'
        }
      ).then(() => true).catch(() => false)
    },
    async requestCloseToolDialog(options = {}) {
      const { done } = options
      const ok = await this.confirmCloseToolDialog()
      if (!ok) return false
      this.doCloseToolDialog()
      if (typeof done === 'function') done()
      return true
    },
    doCloseToolDialog() {
      this.dialogVisible = false
      this.toolDialogCloseBaseline = null
      if (typeof this.reset === 'function') this.reset()
    },
    onToolDialogBeforeClose(done) {
      this.requestCloseToolDialog({ done })
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.requestCloseToolDialog()
    }
  }
}
