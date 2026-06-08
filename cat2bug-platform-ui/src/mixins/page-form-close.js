/**
 * 页面级表单（非 el-dialog）：Esc / 返回前若有未保存修改则确认。
 * 组件需实现 serializePageFormCloseState()，并在数据就绪后调用 capturePageFormCloseBaseline()。
 */
export default {
  data() {
    return {
      pageFormCloseBaseline: null
    }
  },
  methods: {
    serializePageFormCloseState() {
      return ''
    },
    capturePageFormCloseBaseline() {
      this.pageFormCloseBaseline = this.serializePageFormCloseState()
    },
    isPageFormCloseDirty() {
      if (!this.pageFormCloseBaseline) return false
      return this.serializePageFormCloseState() !== this.pageFormCloseBaseline
    },
    confirmClosePageForm() {
      if (!this.isPageFormCloseDirty()) {
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
    async requestClosePageForm(options = {}) {
      const { onClose } = options
      const ok = await this.confirmClosePageForm()
      if (!ok) return false
      this.pageFormCloseBaseline = null
      if (typeof onClose === 'function') onClose()
      return true
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      if (typeof this.onPageFormShortcutClose === 'function') {
        this.onPageFormShortcutClose()
      }
    }
  }
}
