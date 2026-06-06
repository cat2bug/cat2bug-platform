/**
 * 弹框/抽屉表单快捷键 mixin。
 *
 * Cmd/Ctrl + Enter 保存；Cmd/Ctrl + B 关闭。
 */

import {
  registerDefectDrawerShortcuts,
  unregisterDefectDrawerShortcuts,
  findTopFormDrawerVm
} from '@/utils/defect-drawer-shortcuts'

export const SAVE_SHORTCUT_LABEL = '↵'
export const CLOSE_SHORTCUT_LABEL = 'B'

function isCloseShortcutKey(e) {
  if (!e || e.altKey) return false
  if (!(e.metaKey || e.ctrlKey)) return false
  const k = e.key
  return k === 'B' || k === 'b'
}

export default {
  computed: {
    dialogSaveShortcutLabel() {
      return SAVE_SHORTCUT_LABEL
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
      return isCloseShortcutKey(e)
    },
    $_invokeDrawerShortcutSave(e) {
      if (!this.visible) return false
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
      if (typeof this.shortcutClose === 'function') this.shortcutClose()
      else if (typeof this.cancel === 'function') this.cancel()
      return true
    },
    $_handleDrawerShortcutKey(e) {
      if (!this.visible || !this.$_ownsTopFormDrawer() || e.isComposing) return
      if ((e.metaKey || e.ctrlKey) && !e.altKey && (e.key === 'Enter' || e.keyCode === 13)) {
        this.$_invokeDrawerShortcutSave(e)
        return
      }
      if (isCloseShortcutKey(e)) {
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
