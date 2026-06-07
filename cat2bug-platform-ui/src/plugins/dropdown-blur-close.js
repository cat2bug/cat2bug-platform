/**
 * 全局：下拉 / 浮层在失去焦点后自动收起。
 */
import {
  onDropdownBlurFocusIn,
  onDropdownBlurFocusOut,
  onDropdownBlurPointerDown
} from '@/utils/dropdown-blur-close'

const DropdownBlurClosePlugin = {
  install() {
    document.addEventListener('focusout', onDropdownBlurFocusOut, true)
    document.addEventListener('focusin', onDropdownBlurFocusIn, true)
    document.addEventListener('mousedown', onDropdownBlurPointerDown, true)
  }
}

export default DropdownBlurClosePlugin
