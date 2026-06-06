/**
 * 统计模块弹框键盘集成：Cmd/Ctrl+Enter 提交、Esc 关闭、Cmd/Ctrl 字段字母聚焦。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import statisticDialogClose from '@/mixins/statistic-dialog-close'
import formFieldHints from '@/mixins/form-field-hints'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export default {
  mixins: [dialogFormShortcuts, statisticDialogClose, formFieldHints],
  methods: {
    onToolDialogOpened() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          if (!this.deferToolDialogCloseBaseline) {
            this.captureToolDialogCloseBaseline()
          }
          this.$_focusToolDialogFirstField()
        })
      })
    },
    $_focusToolDialogFirstField() {
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
    getFieldHintScrollContainer() {
      const body = this.$el && this.$el.closest('.el-dialog__body')
      return body || this.getFieldHintContainer()
    }
  }
}
