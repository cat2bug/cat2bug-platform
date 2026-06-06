/**
 * 注册缺陷抽屉全局快捷键（须在 DatePickerEscape 等 window keydown 之后 install，
 * 以便 capture 阶段优先于其它 window 监听）。
 */
import {
  registerDefectDrawerShortcuts,
  unregisterDefectDrawerShortcuts,
  bootDefectDrawerShortcuts
} from '@/utils/defect-drawer-shortcuts'

export { registerDefectDrawerShortcuts, unregisterDefectDrawerShortcuts }

export default {
  install() {
    bootDefectDrawerShortcuts()
  }
}
