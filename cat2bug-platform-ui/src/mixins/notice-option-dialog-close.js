/**
 * 通知设置弹框：Esc / 取消 / 关闭前，若有未保存修改则确认。
 */

export default {
  data() {
    return {
      toolDialogCloseBaseline: null
    }
  },
  methods: {
    getNoticeOptionCloseSnapshot() {
      return this.option
    },
    serializeNoticeOptionCloseSnapshot(snapshot) {
      try {
        return JSON.stringify(snapshot || {})
      } catch (e) {
        return ''
      }
    },
    captureToolDialogCloseBaseline() {
      this.toolDialogCloseBaseline = this.serializeNoticeOptionCloseSnapshot(
        this.getNoticeOptionCloseSnapshot()
      )
    },
    isToolDialogCloseDirty() {
      if (!this.toolDialogCloseBaseline) return false
      return this.serializeNoticeOptionCloseSnapshot(
        this.getNoticeOptionCloseSnapshot()
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
