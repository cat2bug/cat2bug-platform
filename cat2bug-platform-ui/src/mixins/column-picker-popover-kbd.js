/**
 * 「显示字段」el-popover 内 ↑↓ 导航：打开后聚焦首项；首项 ↑ 关闭。
 * 页面需：popper-class="defect-column-picker-popover"、checkbox-group class="defect-column-picker"、
 * @show / @hide 绑定 onColumnPickerPopoverShow / Hide，并实现 closeColumnPickerPopoverKbd()。
 */
import {
  clearColumnPickerFocusMarks,
  focusColumnPickerItem,
  getVisibleColumnPickerPopperEl,
  handleColumnPickerMenuKeydown,
  isColumnPickerPopoverOpen
} from '@/utils/column-picker-popover-kbd'

export default {
  data() {
    return {
      columnPickerMenuIndex: 0
    }
  },
  beforeDestroy() {
    this.$_detachColumnPickerMenuListeners()
  },
  deactivated() {
    this.$_detachColumnPickerMenuListeners()
  },
  methods: {
    /** @returns {HTMLElement|null} 触发按钮，用于判断 popover 是否仍打开 */
    getColumnPickerTriggerEl() {
      return null
    },
    /** 关闭浮层（通常 v-model = false） */
    closeColumnPickerPopoverKbd() {
      /* 子页面实现 */
    },
    getColumnPickerPopperElForKbd() {
      return getVisibleColumnPickerPopperEl()
    },
    isColumnPickerPopoverOpenForKbd() {
      const trigger = this.getColumnPickerTriggerEl()
      if (trigger && isColumnPickerPopoverOpen(trigger)) return true
      return !!getVisibleColumnPickerPopperEl()
    },
    onColumnPickerPopoverShow() {
      this.columnPickerMenuIndex = 0
      this.$nextTick(() => this.$_columnPickerTryFocusFirst(0))
    },
    onColumnPickerPopoverHide() {
      this.columnPickerMenuIndex = 0
      clearColumnPickerFocusMarks(this.getColumnPickerPopperElForKbd())
      this.$_detachColumnPickerMenuListeners()
      this.$nextTick(() => {
        const btn = this.getColumnPickerTriggerEl()
        if (btn && typeof btn.focus === 'function') {
          try {
            btn.focus({ preventScroll: false })
          } catch (e) {
            btn.focus()
          }
        }
      })
    },
    $_columnPickerTryFocusFirst(attempt = 0) {
      const popper = this.getColumnPickerPopperElForKbd()
      const menuIndexRef = { value: this.columnPickerMenuIndex }
      if (focusColumnPickerItem(popper, 0, menuIndexRef)) {
        this.columnPickerMenuIndex = menuIndexRef.value
        this.$_attachColumnPickerMenuListeners()
        return
      }
      if (attempt < 8) {
        setTimeout(() => this.$_columnPickerTryFocusFirst(attempt + 1), 40)
      }
    },
    $_attachColumnPickerMenuListeners() {
      if (this.$_columnPickerMenuListenersBound) return
      this.$_columnPickerMenuListenersBound = true
      this.$_columnPickerMenuIndexRef = { value: this.columnPickerMenuIndex }
      this.$_onColumnPickerMenuKeydown = (e) => {
        this.$_columnPickerMenuIndexRef.value = this.columnPickerMenuIndex
        const handled = handleColumnPickerMenuKeydown(e, {
          isOpen: () => this.isColumnPickerPopoverOpenForKbd(),
          getPopperEl: () => this.getColumnPickerPopperElForKbd(),
          menuIndexRef: this.$_columnPickerMenuIndexRef,
          close: () => this.closeColumnPickerPopoverKbd()
        })
        if (handled) {
          this.columnPickerMenuIndex = this.$_columnPickerMenuIndexRef.value
        }
      }
      document.addEventListener('keydown', this.$_onColumnPickerMenuKeydown, true)
    },
    $_detachColumnPickerMenuListeners() {
      if (!this.$_columnPickerMenuListenersBound) return
      this.$_columnPickerMenuListenersBound = false
      document.removeEventListener('keydown', this.$_onColumnPickerMenuKeydown, true)
      this.$_onColumnPickerMenuKeydown = null
      this.$_columnPickerMenuIndexRef = null
    }
  }
}
