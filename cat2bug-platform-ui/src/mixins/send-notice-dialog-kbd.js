/**
 * 发送通知弹框：Cmd/Ctrl+Enter 发送、Esc 关闭（有未保存修改时确认）、Cmd/Ctrl 字段字母聚焦。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export default {
  mixins: [dialogFormShortcuts, formFieldHints],
  data() {
    return {
      sendDialogCloseBaseline: null
    }
  },
  methods: {
    serializeSendDialogCloseState() {
      return JSON.stringify({ ...(this.form || {}) })
    },
    captureSendDialogCloseBaseline() {
      this.sendDialogCloseBaseline = this.serializeSendDialogCloseState()
    },
    isSendDialogCloseDirty() {
      if (!this.sendDialogCloseBaseline) return false
      return this.serializeSendDialogCloseState() !== this.sendDialogCloseBaseline
    },
    confirmCloseSendDialog() {
      if (!this.isSendDialogCloseDirty()) {
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
    async requestCloseSendDialog(options = {}) {
      const { done } = options
      const ok = await this.confirmCloseSendDialog()
      if (!ok) return false
      this.sendDialogCloseBaseline = null
      this.visible = false
      if (typeof done === 'function') done()
      return true
    },
    onSendDialogOpened() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.captureSendDialogCloseBaseline()
          this.$_focusSendDialogFirstField()
        })
      })
    },
    $_focusSendDialogFirstField() {
      const container = typeof this.getFieldHintContainer === 'function'
        ? this.getFieldHintContainer()
        : this.$el
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
      if (typeof this.handleSend === 'function') this.handleSend()
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.requestCloseSendDialog()
    },
    getFieldHintContainer() {
      const form = this.$refs.form
      return (form && form.$el) || this.$el
    },
    getFieldHintScrollContainer() {
      const body = this.$el && this.$el.querySelector('.el-dialog__body')
      return body || this.getFieldHintContainer()
    }
  }
}
