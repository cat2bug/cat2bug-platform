/**
 * 全局：el-date-picker 获焦不展开面板，↓/↑/Enter/Space 或鼠标点击后展开。
 */
import {
  handleActiveDateRangeKeydown,
  patchDatePickerKeyboard,
  wrapDatePickerKeyboard
} from '@/utils/date-picker-kbd'
import { installDatePickerPanelHints } from '@/utils/date-picker-panel-hints'

const DatePickerKbdPlugin = {
  install(Vue) {
    Vue.mixin({
      mounted() {
        const el = this.$el
        if (el && el.classList && el.classList.contains('el-date-editor') &&
          typeof this.handleFocus === 'function' && 'pickerVisible' in this) {
          wrapDatePickerKeyboard(this)
        }
      }
    })

    patchDatePickerKeyboard(document.body)
    installDatePickerPanelHints()

    if (typeof document !== 'undefined') {
      document.addEventListener('keydown', (event) => {
        handleActiveDateRangeKeydown(event)
      }, true)
    }

    const observer = new MutationObserver((mutations) => {
      mutations.forEach((m) => {
        m.addedNodes.forEach((node) => {
          if (node.nodeType !== 1) return
          if (node.classList && node.classList.contains('el-date-editor')) {
            patchDatePickerKeyboard(node.parentElement || document.body)
            return
          }
          if (node.querySelectorAll) {
            const editors = node.querySelectorAll('.el-date-editor')
            if (editors.length) patchDatePickerKeyboard(node)
          }
        })
      })
    })

    if (document.body) {
      observer.observe(document.body, { childList: true, subtree: true })
    }
  }
}

export default DatePickerKbdPlugin
