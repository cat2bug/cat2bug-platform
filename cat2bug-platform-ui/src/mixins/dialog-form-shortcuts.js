/**
 * 弹框/抽屉表单快捷键 mixin。
 *
 * Cmd/Ctrl + Enter 保存；Esc 关闭（未保存时由 defect-form-drawer-close 确认）。
 */

import {
  registerDefectDrawerShortcuts,
  unregisterDefectDrawerShortcuts,
  findTopFormDrawerVm,
  isEscapeCloseKey,
  isSaveShortcutKey
} from '@/utils/defect-drawer-shortcuts'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'

export const SAVE_SHORTCUT_LABEL = '↵'
export const CLOSE_SHORTCUT_LABEL = 'Esc'

function saveShortcutFullLabel() {
  if (typeof navigator !== 'undefined' && /Mac|iPhone|iPad|iPod/.test(navigator.platform)) {
    return '⌘↵'
  }
  return 'Ctrl+↵'
}

export default {
  computed: {
    dialogSaveShortcutLabel() {
      return this.fieldHintsActive ? SAVE_SHORTCUT_LABEL : saveShortcutFullLabel()
    },
    dialogCloseShortcutLabel() {
      return CLOSE_SHORTCUT_LABEL
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        if (val) {
          this.$_dialogShortcutsRegistered = true
          registerDefectDrawerShortcuts(this)
        } else {
          this.$_unbindDialogShortcuts()
        }
      }
    }
  },
  beforeDestroy() {
    this.$_unbindDialogShortcuts()
  },
  methods: {
    $_ownsTopFormDrawer() {
      if (!this.visible) return false
      const top = findTopFormDrawerVm()
      return !top || top === this
    },
    $_canCloseDrawerByShortcut(e) {
      if (!this.visible) return false
      if (!isEscapeCloseKey(e)) return false
      return !hasBlockingUiLayer({ excludeDefectFormDrawer: true })
    },
    $_invokeDrawerShortcutSave(e) {
      if (!this.$_ownsTopFormDrawer()) return false
      if (!this.visible || !isSaveShortcutKey(e)) return false
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      if (typeof this.shortcutSave === 'function') this.shortcutSave()
      else if (typeof this.submitForm === 'function') this.submitForm()
      return true
    },
    $_invokeDrawerShortcutClose(e) {
      if (!this.$_ownsTopFormDrawer()) return false
      if (!this.$_canCloseDrawerByShortcut(e)) return false
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      if (typeof this.shortcutClose === 'function') this.shortcutClose(e)
      else if (typeof this.requestCloseDefectFormDrawer === 'function') this.requestCloseDefectFormDrawer()
      else if (typeof this.cancel === 'function') this.cancel()
      return true
    },
    $_handleDrawerShortcutKey(e) {
      if (!this.visible || !this.$_ownsTopFormDrawer() || e.isComposing) return
      if (isSaveShortcutKey(e)) {
        this.$_invokeDrawerShortcutSave(e)
        return
      }
      if (isEscapeCloseKey(e)) {
        this.$_invokeDrawerShortcutClose(e)
      }
    },
    $_unbindDialogShortcuts() {
      if (!this.$_dialogShortcutsRegistered) return
      this.$_dialogShortcutsRegistered = false
      unregisterDefectDrawerShortcuts(this)
    }
  }
}
