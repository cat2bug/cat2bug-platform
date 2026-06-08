/**
 * 弹框/抽屉表单快捷键 mixin。
 *
 * Cmd/Ctrl + Enter 保存；Esc 关闭（未保存时由 defect-form-drawer-close / defect-tool-dialog-close 确认）。
 */

import {
  registerDefectDrawerShortcuts,
  unregisterDefectDrawerShortcuts,
  findTopFormDrawerVm,
  isEscapeCloseKey,
  isSaveShortcutKey
} from '@/utils/defect-drawer-shortcuts'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { dismissToolbarSplitDropdownSessions } from '@/utils/split-dropdown-kbd'

export const SAVE_SHORTCUT_LABEL = '↵'

export default {
  computed: {
    /** 抽屉 visible 或工具弹框 dialogVisible */
    $_formShortcutSurfaceVisible() {
      return this.visible === true || this.dialogVisible === true
    },
    /** 仅 ⌘/Ctrl 浮层激活时显示回车徽标 */
    dialogSaveShortcutLabel() {
      return this.fieldHintsActive ? SAVE_SHORTCUT_LABEL : ''
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.$_syncFormShortcutBinding(!!val)
      }
    },
    dialogVisible: {
      immediate: true,
      handler(val) {
        this.$_syncFormShortcutBinding(!!val)
      }
    }
  },
  beforeDestroy() {
    this.$_unbindDialogShortcuts()
  },
  methods: {
    $_syncFormShortcutBinding(open) {
      if (open) {
        this.$_dialogShortcutsRegistered = true
        registerDefectDrawerShortcuts(this)
        this.$_hideAncestorPageActionHints()
      } else if (!this.$_formShortcutSurfaceVisible) {
        this.$_unbindDialogShortcuts()
      }
    },
    $_ownsTopFormDrawer() {
      if (!this.$_formShortcutSurfaceVisible) return false
      const top = findTopFormDrawerVm()
      return !top || top === this
    },
    $_canCloseDrawerByShortcut(e) {
      if (!this.$_formShortcutSurfaceVisible) return false
      if (!isEscapeCloseKey(e)) return false
      return !hasBlockingUiLayer({
        excludeDefectFormDrawer: true,
        excludeHandleDefectDrawer: true,
        excludeViewReportDrawer: true,
        excludeDefectToolDialog: true,
        excludeCaseImportDialog: true
      })
    },
    $_invokeDrawerShortcutSave(e) {
      if (!this.$_ownsTopFormDrawer()) return false
      if (!this.$_formShortcutSurfaceVisible || !isSaveShortcutKey(e)) return false
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
      else if (typeof this.requestCloseCaseFormDrawer === 'function') this.requestCloseCaseFormDrawer()
      else if (typeof this.close === 'function') this.close()
      else if (typeof this.cancel === 'function') this.cancel()
      return true
    },
    $_handleDrawerShortcutKey(e) {
      if (!this.$_formShortcutSurfaceVisible || !this.$_ownsTopFormDrawer() || e.isComposing) return
      if (isSaveShortcutKey(e)) {
        this.$_invokeDrawerShortcutSave(e)
        return
      }
      if (isEscapeCloseKey(e)) {
        this.$_invokeDrawerShortcutClose(e)
      }
    },
    $_hideAncestorPageActionHints() {
      let parent = this.$parent
      while (parent) {
        if (typeof parent.$_hidePageActionHints === 'function') {
          parent.$_hidePageActionHints()
          if (parent.$el) dismissToolbarSplitDropdownSessions(parent.$el)
        }
        if (typeof parent.exitListQueryKeyboardNav === 'function') {
          parent.exitListQueryKeyboardNav()
        }
        if (typeof parent.$_hidePageActionHints === 'function') return
        parent = parent.$parent
      }
    },
    $_unbindDialogShortcuts() {
      if (!this.$_dialogShortcutsRegistered) return
      this.$_dialogShortcutsRegistered = false
      unregisterDefectDrawerShortcuts(this)
    }
  }
}
