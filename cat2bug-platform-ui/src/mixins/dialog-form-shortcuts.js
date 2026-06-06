/**
 * 弹框/抽屉表单快捷键 mixin。
 *
 * 弹框内输入框处于聚焦态，全局引导键（g / 空格）会被焦点守卫拦截，
 * 因此弹框采用业界通用的「直接组合键」：
 *   - Cmd/Ctrl + Enter  → 保存（调用组件的 submitForm，可用 shortcutSave 覆盖）
 *   - Cmd/Ctrl + Esc    → 关闭（调用组件的 cancel，可用 shortcutClose 覆盖）
 *
 * 使用要求：组件需具备响应式 data `visible`，并实现 submitForm / cancel
 * （或 shortcutSave / shortcutClose）。
 *
 * 表单字段切换：默认 Tab 正向；按住 ↑ 再 Tab 反向；按住 ↓ 再 Tab 正向（全局 tab-direction 插件）。
 */

import { isNativeFilePickerOpen } from '@/utils/native-file-picker'

/** 保存快捷键提示文案（修饰键已按下时显示，故仅展示第二键） */
export const SAVE_SHORTCUT_LABEL = '↵'

/** 关闭快捷键提示文案（修饰键已按下时显示，故仅展示第二键） */
export const CLOSE_SHORTCUT_LABEL = 'Esc'

/** Meta/Ctrl 松开后仍接受 Esc 关闭的宽限期（ms） */
const CLOSE_MODIFIER_GRACE_MS = 400

/** 字段提示刚消失后仍接受 Esc 关闭的宽限期（ms） */
const FIELD_HINTS_GRACE_MS = 800

function isModifierKey(key) {
  return key === 'Meta' || key === 'Control' || key === 'OS'
}

/** Cmd/Ctrl 是否处于按下态（Mac 上 Esc 的 metaKey 有时为 false，需 getModifierState /  latch 兜底） */
function isCloseModifier(e) {
  if (!e || e.altKey) return false
  if (e.metaKey || e.ctrlKey) return true
  if (typeof e.getModifierState === 'function') {
    return e.getModifierState('Meta') || e.getModifierState('Control')
  }
  return false
}

function isEscapeKey(e) {
  return e && (e.key === 'Escape' || e.key === 'Esc' || e.keyCode === 27)
}

export default {
  computed: {
    /** 保存快捷键提示文案（供模板展示） */
    dialogSaveShortcutLabel() {
      return SAVE_SHORTCUT_LABEL
    },
    /** 关闭快捷键提示文案（供模板展示） */
    dialogCloseShortcutLabel() {
      return CLOSE_SHORTCUT_LABEL
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        if (val) {
          this.$_bindDialogShortcuts()
        } else {
          this.$_unbindDialogShortcuts()
        }
      }
    },
    fieldHintsActive(val) {
      if (val) {
        this.$_fieldHintsShownAt = Date.now()
      } else if (this.$_fieldHintsShownAt) {
        this.$_fieldHintsDismissedAt = Date.now()
      }
    }
  },
  beforeDestroy() {
    this.$_unbindDialogShortcuts()
  },
  methods: {
    $_markCloseModifierActive() {
      this.$_closeModifierLatched = true
      this.$_closeModifierLatchedAt = Date.now()
      this.$_clearCloseModifierLatchTimer()
    },
    $_clearCloseModifierLatchTimer() {
      if (this.$_closeModifierLatchTimer) {
        clearTimeout(this.$_closeModifierLatchTimer)
        this.$_closeModifierLatchTimer = null
      }
    },
    $_scheduleCloseModifierLatchRelease() {
      this.$_clearCloseModifierLatchTimer()
      this.$_closeModifierLatchTimer = setTimeout(() => {
        this.$_closeModifierLatchTimer = null
        this.$_closeModifierLatched = false
      }, CLOSE_MODIFIER_GRACE_MS)
    },
    $_invokeDialogClose() {
      this.$_closeModifierLatched = false
      this.$_closeModifierLatchedAt = 0
      this.$_clearCloseModifierLatchTimer()
      if (typeof this.shortcutClose === 'function') this.shortcutClose()
      else if (typeof this.cancel === 'function') this.cancel()
    },
    /** Mac 上 Esc 事件可能不带 metaKey；结合 latch / form-field-hints 的 fieldHintsActive 判断 */
    $_shouldCloseOnEscape(e) {
      if (!isEscapeKey(e)) return false
      if (this.fieldHintsActive === true) return true
      if (this.$_closeModifierLatched) return true
      if (isCloseModifier(e)) return true
      const now = Date.now()
      if (this.$_closeModifierLatchedAt && now - this.$_closeModifierLatchedAt <= CLOSE_MODIFIER_GRACE_MS) {
        return true
      }
      if (this.$_fieldHintsDismissedAt && now - this.$_fieldHintsDismissedAt <= FIELD_HINTS_GRACE_MS) {
        return true
      }
      return false
    },
    $_trackCloseModifierKey(e) {
      if (!e || e.type !== 'keydown') return
      if (isModifierKey(e.key)) {
        this.$_markCloseModifierActive()
      }
    },
    $_releaseCloseModifierKey(e) {
      if (!e || e.type !== 'keyup') return
      if (isModifierKey(e.key)) {
        this.$_scheduleCloseModifierLatchRelease()
      }
    },
    $_bindDialogShortcuts() {
      if (this.$_dialogShortcutsBound) return
      this.$_dialogShortcutsBound = true
      this.$_closeModifierLatched = false
      this.$_closeModifierLatchedAt = 0
      this.$_fieldHintsShownAt = 0
      this.$_fieldHintsDismissedAt = 0
      this.$_dialogShortcutHandler = (e) => {
        if (!this.visible || e.isComposing) return
        this.$_trackCloseModifierKey(e)
        if ((e.metaKey || e.ctrlKey || this.$_closeModifierLatched) &&
          !e.altKey && (e.key === 'Enter' || e.keyCode === 13)) {
          e.preventDefault()
          e.stopPropagation()
          if (typeof this.shortcutSave === 'function') this.shortcutSave()
          else if (typeof this.submitForm === 'function') this.submitForm()
          return
        }
        if (this.$_shouldCloseOnEscape(e)) {
          e.preventDefault()
          e.stopPropagation()
          this.$_invokeDialogClose()
        }
      }
      this.$_dialogShortcutKeyupHandler = (e) => {
        if (!this.visible) return
        this.$_releaseCloseModifierKey(e)
      }
      this.$_dialogShortcutBlurHandler = () => {
        if (isNativeFilePickerOpen()) return
        this.$_scheduleCloseModifierLatchRelease()
      }
      // window 捕获优先于 document 上其它 Esc 监听（成员下拉等）
      window.addEventListener('keydown', this.$_dialogShortcutHandler, true)
      window.addEventListener('keyup', this.$_dialogShortcutKeyupHandler, true)
      window.addEventListener('blur', this.$_dialogShortcutBlurHandler)
    },
    $_unbindDialogShortcuts() {
      if (!this.$_dialogShortcutsBound) return
      this.$_dialogShortcutsBound = false
      this.$_closeModifierLatched = false
      this.$_closeModifierLatchedAt = 0
      this.$_clearCloseModifierLatchTimer()
      if (this.$_dialogShortcutHandler) {
        window.removeEventListener('keydown', this.$_dialogShortcutHandler, true)
        this.$_dialogShortcutHandler = null
      }
      if (this.$_dialogShortcutKeyupHandler) {
        window.removeEventListener('keyup', this.$_dialogShortcutKeyupHandler, true)
        this.$_dialogShortcutKeyupHandler = null
      }
      if (this.$_dialogShortcutBlurHandler) {
        window.removeEventListener('blur', this.$_dialogShortcutBlurHandler)
        this.$_dialogShortcutBlurHandler = null
      }
    }
  }
}
