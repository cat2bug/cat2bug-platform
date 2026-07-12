/**
 * Global date-picker keyboard behavior (Element Plus adapter + utils).
 */
import {
  handleActiveDateRangeKeydown,
  patchDatePickerKeyboard,
  wrapDatePickerKeyboard
} from '@/utils/date-picker-kbd'
import { installDatePickerPanelHints } from '@/utils/date-picker-panel-hints'
import { isDatePickerEditorEl } from '@/adapters/element-plus/date-picker-adapter'

function observeDatePickerEditors(root, patchFn) {
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((m) => {
      m.addedNodes.forEach((node) => {
        if (node.nodeType !== 1) return
        if (isDatePickerEditorEl(node)) {
          patchFn(node.parentElement || document.body)
          return
        }
        if (node.querySelectorAll) {
          const editors = node.querySelectorAll('.el-date-editor')
          if (editors.length) patchFn(node)
        }
      })
    })
  })
  if (root) {
    observer.observe(root, { childList: true, subtree: true })
  }
  return observer
}

export function installDatePickerKbd(app) {
  app.mixin({
    mounted() {
      const el = this.$el
      if (el && isDatePickerEditorEl(el) &&
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

  observeDatePickerEditors(document.body, patchDatePickerKeyboard)
}

export function useDatePickerKbd() {
  return {
    install: installDatePickerKbd,
    wrapDatePickerKeyboard,
    patchDatePickerKeyboard,
    handleActiveDateRangeKeydown
  }
}
