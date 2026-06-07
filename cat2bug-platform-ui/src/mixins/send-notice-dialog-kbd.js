/**
 * 发送通知弹框：Cmd/Ctrl+Enter 发送、Esc 关闭、Cmd/Ctrl 字段字母聚焦。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export default {
  mixins: [dialogFormShortcuts, formFieldHints],
  methods: {
    onSendDialogOpened() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
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
      this.visible = false
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
