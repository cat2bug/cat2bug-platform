/**
 * 列表页查询区键盘导航：←/→ 在查询控件间切换；最右项 → 进入右侧工具栏，
 * 工具栏内 ←/→ 在按钮间切换，首按钮 ← 回到最右查询项。Esc 退出。
 */

import {
  focusToolbarAtIndex,
  getFirstToolbarFocusable,
  getToolbarFocusableIndex,
  getToolbarFocusables,
  isFocusInToolbar,
  isQueryNavOverlayOpen
} from '@/utils/list-query-toolbar-nav'
import { focusDatePickerInputSilently, handleActiveDateRangeKeydown, isDatePickerFocusEl, isDatePickerPanelOpen, patchDatePickerKeyboard } from '@/utils/date-picker-kbd'
import {
  bindSplitDropdownHost,
  clearAllSplitDropdownHostFocusVisuals,
  DROPDOWN_HOST_SELECTOR,
  focusSplitDropdownHost,
  isSplitDropdownOpen,
  openSplitDropdown,
  resolveSplitDropdownHost,
  shouldBlockToolbarSplitDropdownInteraction,
  SPLIT_DROPDOWN_FOCUS_CLASS,
  tryOpenToolbarFocusedSplitDropdown
} from '@/utils/split-dropdown-kbd'

const NAV_ITEM_CLASS = 'list-query-nav-item'
const QUERY_FOCUS_CLASS = 'list-query-nav-focused'
const TOOLBAR_FOCUS_CLASS = 'list-query-toolbar-nav-focused'

export default {
  data() {
    return {
      listQueryNavActive: false,
      listQueryNavIndex: -1,
      listQueryNavToolbarIndex: -1,
      listQueryNavSuppressBlur: false
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn()
      this.$_bindListQueryNavToolbarFocusIn()
    })
  },
  beforeDestroy() {
    this.exitListQueryKeyboardNav()
    this.$_unbindListQueryNavFocusIn()
    this.$_unbindListQueryNavToolbarFocusIn()
  },
  deactivated() {
    this.exitListQueryKeyboardNav()
  },
  methods: {
    getListQueryNavFormRef() {
      return 'queryForm'
    },
    getListQueryNavToolbarRef() {
      return null
    },
    getListQueryNavItems() {
      return []
    },
    getListQueryNavDefaultIndex() {
      return 0
    },
    isListQueryNavToolbarOverlayOpen() {
      const toolbar = this.getListQueryNavToolbarEl()
      if (!toolbar) return false
      const hosts = toolbar.querySelectorAll(DROPDOWN_HOST_SELECTOR)
      for (let i = 0; i < hosts.length; i++) {
        if (isSplitDropdownOpen(hosts[i])) return true
      }
      return false
    },
    getListQueryNavFormEl() {
      const ref = this.getListQueryNavFormRef()
      const form = ref && this.$refs[ref]
      return (form && form.$el) || null
    },
    getListQueryNavToolbarEl() {
      const ref = this.getListQueryNavToolbarRef()
      if (!ref) return null
      const node = this.$refs[ref]
      return (node && node.$el) || node || null
    },
    hasListQueryNavToolbarBridge() {
      return getToolbarFocusables(this.getListQueryNavToolbarEl()).length > 0
    },
    getListQueryNavItemEl(key) {
      const form = this.getListQueryNavFormEl()
      if (!form || !key) return null
      return form.querySelector(`.${NAV_ITEM_CLASS}[data-query-key="${String(key)}"]`)
    },
    getListQueryNavFocusEl(key) {
      const itemEl = this.getListQueryNavItemEl(key)
      if (!itemEl) return null
      return itemEl.querySelector('.select-project-member-input') ||
        itemEl.querySelector('input.el-input__inner') ||
        itemEl.querySelector('input, textarea, [tabindex]:not([tabindex="-1"])')
    },
    isListQueryNavOverlayOpen() {
      return isQueryNavOverlayOpen()
    },
    getListQueryNavIndexByKey(key) {
      return this.getListQueryNavItems().findIndex((item) => item.key === key)
    },
    isListQueryNavToolbarFirstFocused() {
      return this.listQueryNavToolbarIndex === 0 &&
        this.isFocusInListQueryNavToolbar(document.activeElement)
    },
    hasListQueryNavToolbarMarkedFocus() {
      if (!this.listQueryNavActive || this.listQueryNavToolbarIndex < 0) return false
      const toolbar = this.getListQueryNavToolbarEl()
      if (!toolbar) return false
      const list = getToolbarFocusables(toolbar)
      const el = list[this.listQueryNavToolbarIndex]
      return !!(el && el.classList && el.classList.contains(TOOLBAR_FOCUS_CLASS))
    },
    isFocusInListQueryNavToolbar(target) {
      const toolbar = this.getListQueryNavToolbarEl()
      if (isFocusInToolbar(toolbar, target)) return true
      return this.hasListQueryNavToolbarMarkedFocus()
    },
    clearListQueryNavToolbarFocusMarks() {
      const toolbar = this.getListQueryNavToolbarEl()
      if (!toolbar) return
      toolbar.querySelectorAll(`.${TOOLBAR_FOCUS_CLASS}`).forEach((el) => {
        el.classList.remove(TOOLBAR_FOCUS_CLASS)
      })
    },
    syncListQueryNavToolbarFocusClass() {
      const toolbar = this.getListQueryNavToolbarEl()
      if (toolbar) clearAllSplitDropdownHostFocusVisuals(toolbar)
      this.clearListQueryNavToolbarFocusMarks()
      if (!this.listQueryNavActive || this.listQueryNavToolbarIndex < 0) return
      const list = getToolbarFocusables(toolbar)
      const el = list[this.listQueryNavToolbarIndex]
      if (el) el.classList.add(TOOLBAR_FOCUS_CLASS)
    },
    syncListQueryNavToolbarIndexFromFocus(target) {
      const toolbar = this.getListQueryNavToolbarEl()
      const idx = getToolbarFocusableIndex(toolbar, target || document.activeElement)
      if (idx >= 0) {
        this.listQueryNavToolbarIndex = idx
        this.clearListQueryNavFocusMarks()
        this.syncListQueryNavToolbarFocusClass()
      }
    },
    ensureListQueryNavFromFocus(target) {
      if (!target || (this.showSearch === false)) return
      const itemEl = target.closest && target.closest(`.${NAV_ITEM_CLASS}[data-query-key]`)
      if (!itemEl) return
      const form = this.getListQueryNavFormEl()
      if (!form || !form.contains(itemEl)) return
      const key = itemEl.getAttribute('data-query-key')
      const index = this.getListQueryNavIndexByKey(key)
      if (index < 0) return
      this.listQueryNavActive = true
      this.listQueryNavIndex = index
      this.listQueryNavToolbarIndex = -1
      this.syncListQueryNavFocusClass()
      this.clearListQueryNavToolbarFocusMarks()
      this.$_attachListQueryNavListeners()
    },
    ensureListQueryNavFromToolbarFocus(target) {
      if (!target) return
      const toolbar = this.getListQueryNavToolbarEl()
      if (!toolbar || !toolbar.contains(target)) return
      const idx = getToolbarFocusableIndex(toolbar, target)
      if (idx < 0) return
      this.listQueryNavActive = true
      this.listQueryNavToolbarIndex = idx
      this.clearListQueryNavFocusMarks()
      this.syncListQueryNavToolbarFocusClass()
      this.$_attachListQueryNavListeners()
    },
    clearListQueryNavFocusMarks() {
      const form = this.getListQueryNavFormEl()
      if (!form) return
      form.querySelectorAll(`.${NAV_ITEM_CLASS}.${QUERY_FOCUS_CLASS}`).forEach((el) => {
        el.classList.remove(QUERY_FOCUS_CLASS)
      })
    },
    syncListQueryNavFocusClass() {
      this.clearListQueryNavFocusMarks()
      if (!this.listQueryNavActive || this.listQueryNavIndex < 0) return
      if (this.isFocusInListQueryNavToolbar(document.activeElement)) return
      const items = this.getListQueryNavItems()
      const item = items[this.listQueryNavIndex]
      if (!item) return
      const itemEl = this.getListQueryNavItemEl(item.key)
      if (itemEl) itemEl.classList.add(QUERY_FOCUS_CLASS)
    },
    applyListQueryNavFocus(index) {
      const items = this.getListQueryNavItems()
      if (index < 0 || index >= items.length) return
      this.listQueryNavSuppressBlur = true
      this.listQueryNavIndex = index
      this.listQueryNavToolbarIndex = -1
      const item = items[index]
      const itemEl = this.getListQueryNavItemEl(item.key)
      const form = this.getListQueryNavFormEl()
      if (form) patchDatePickerKeyboard(form)

      let focusEl = this.getListQueryNavFocusEl(item.key)
      const dateEditor = itemEl && itemEl.querySelector('.el-date-editor')
      if (dateEditor) {
        const rangeInput = dateEditor.querySelector('.el-range-input') ||
          dateEditor.querySelector('input:not([type="hidden"])')
        if (rangeInput) focusEl = rangeInput
      }

      if (focusEl && (dateEditor || isDatePickerFocusEl(focusEl))) {
        focusDatePickerInputSilently(focusEl)
      } else if (focusEl && typeof focusEl.focus === 'function') {
        try {
          focusEl.focus({ preventScroll: false })
        } catch (e) {
          focusEl.focus()
        }
      }
      this.clearListQueryNavToolbarFocusMarks()
      this.syncListQueryNavFocusClass()
      this.$nextTick(() => {
        this.listQueryNavSuppressBlur = false
      })
    },
    applyListQueryNavToolbarFocus(index) {
      const toolbar = this.getListQueryNavToolbarEl()
      const list = getToolbarFocusables(toolbar)
      if (index < 0 || index >= list.length) return false
      this.listQueryNavSuppressBlur = true
      this.listQueryNavToolbarIndex = index
      this.listQueryNavActive = true
      this.clearListQueryNavFocusMarks()
      const prevActive = document.activeElement
      const { focused, el, disabled } = focusToolbarAtIndex(toolbar, index)
      if (el && el.classList.contains(SPLIT_DROPDOWN_FOCUS_CLASS)) {
        const host = resolveSplitDropdownHost(el)
        if (host) {
          bindSplitDropdownHost(host)
          if (!disabled) focusSplitDropdownHost(host, { toolbarNav: true })
        }
      } else if (!focused && disabled && el) {
        if (prevActive && toolbar.contains(prevActive) && prevActive !== el && typeof prevActive.blur === 'function') {
          prevActive.blur()
        }
      }
      this.syncListQueryNavToolbarFocusClass()
      this.$_attachListQueryNavListeners()
      this.$nextTick(() => {
        this.listQueryNavSuppressBlur = false
      })
      return true
    },
    focusListQueryNavToolbarFirst() {
      const items = this.getListQueryNavItems()
      if (items.length) {
        this.listQueryNavIndex = items.length - 1
      }
      return this.applyListQueryNavToolbarFocus(0)
    },
    focusListQueryNavLastQuery() {
      const items = this.getListQueryNavItems()
      if (!items.length) return
      this.applyListQueryNavFocus(items.length - 1)
    },
    moveListQueryNavToolbar(delta) {
      const toolbar = this.getListQueryNavToolbarEl()
      const list = getToolbarFocusables(toolbar)
      if (!list.length) return
      let idx = this.listQueryNavToolbarIndex
      if (idx < 0) {
        idx = getToolbarFocusableIndex(toolbar, document.activeElement)
      }
      const next = idx + delta
      if (next < 0 || next >= list.length) return
      this.applyListQueryNavToolbarFocus(next)
    },
    enterListQueryKeyboardNav() {
      if (this.showSearch === false) return
      const items = this.getListQueryNavItems()
      if (!items.length) return
      let idx = this.getListQueryNavDefaultIndex()
      if (idx < 0 || idx >= items.length) idx = 0
      this.listQueryNavActive = true
      this.listQueryNavIndex = idx
      this.listQueryNavToolbarIndex = -1
      this.$nextTick(() => {
        const form = this.getListQueryNavFormEl()
        if (form) patchDatePickerKeyboard(form)
        this.applyListQueryNavFocus(idx)
        this.$nextTick(() => this.$_attachListQueryNavListeners())
      })
    },
    exitListQueryKeyboardNav() {
      if (!this.listQueryNavActive && !this.$_listQueryNavListenersBound) return
      this.listQueryNavActive = false
      this.listQueryNavIndex = -1
      this.listQueryNavToolbarIndex = -1
      this.clearListQueryNavFocusMarks()
      this.clearListQueryNavToolbarFocusMarks()
      this.$_detachListQueryNavListeners()
    },
    moveListQueryNav(delta) {
      const items = this.getListQueryNavItems()
      const next = this.listQueryNavIndex + delta
      if (next < 0 || next >= items.length) return
      this.applyListQueryNavFocus(next)
    },
    $_attachListQueryNavListeners() {
      if (this.$_listQueryNavListenersBound) return
      this.$_listQueryNavListenersBound = true
      this.$_onListQueryNavKeydown = (e) => {
        if (!this.listQueryNavActive) return
        const inToolbar = this.isFocusInListQueryNavToolbar(document.activeElement)
        if (!inToolbar && isDatePickerPanelOpen()) {
          const rangeKeys = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Enter', ' ', 'Spacebar']
          if (rangeKeys.indexOf(e.key) >= 0) {
            if (handleActiveDateRangeKeydown(e)) return
            return
          }
        }
        if (inToolbar && this.isListQueryNavToolbarOverlayOpen()) return
        if (!inToolbar && this.isListQueryNavOverlayOpen()) return

        if (e.key === 'ArrowLeft') {
          if (inToolbar) {
            e.preventDefault()
            e.stopPropagation()
            const toolbar = this.getListQueryNavToolbarEl()
            let curIdx = this.listQueryNavToolbarIndex
            if (curIdx < 0) {
              curIdx = getToolbarFocusableIndex(toolbar, document.activeElement)
            }
            if (curIdx <= 0) {
              this.focusListQueryNavLastQuery()
              return
            }
            this.moveListQueryNavToolbar(-1)
            return
          }
          e.preventDefault()
          e.stopPropagation()
          this.moveListQueryNav(-1)
          return
        }
        if (e.key === 'ArrowRight') {
          if (inToolbar) {
            e.preventDefault()
            e.stopPropagation()
            this.moveListQueryNavToolbar(1)
            return
          }
          const items = this.getListQueryNavItems()
          if (items.length && this.listQueryNavIndex === items.length - 1 && this.hasListQueryNavToolbarBridge()) {
            e.preventDefault()
            e.stopPropagation()
            this.focusListQueryNavToolbarFirst()
            return
          }
          e.preventDefault()
          e.stopPropagation()
          this.moveListQueryNav(1)
          return
        }
        if (e.key === 'Escape') {
          if (!inToolbar && this.isListQueryNavOverlayOpen()) return
          e.preventDefault()
          e.stopPropagation()
          this.exitListQueryKeyboardNav()
          return
        }
        if (inToolbar && (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar' || e.key === 'ArrowDown')) {
          const toolbar = this.getListQueryNavToolbarEl()
          if (tryOpenToolbarFocusedSplitDropdown(toolbar)) {
            e.preventDefault()
            e.stopPropagation()
            return
          }
          const list = getToolbarFocusables(toolbar)
          const cur = list[this.listQueryNavToolbarIndex]
          if (cur && cur.classList.contains(SPLIT_DROPDOWN_FOCUS_CLASS)) {
            const host = resolveSplitDropdownHost(cur)
            if (host) {
              e.preventDefault()
              e.stopPropagation()
              focusSplitDropdownHost(host, { toolbarNav: true })
              openSplitDropdown(host)
            }
          }
        }
      }
      this.$_onListQueryNavFocusOut = (e) => {
        if (!this.listQueryNavActive || this.listQueryNavSuppressBlur) return
        const form = this.getListQueryNavFormEl()
        const toolbar = this.getListQueryNavToolbarEl()
        if (form && e.relatedTarget && form.contains(e.relatedTarget)) return
        if (toolbar && e.relatedTarget && toolbar.contains(e.relatedTarget)) return
        setTimeout(() => {
          if (!this.listQueryNavActive || this.listQueryNavSuppressBlur) return
          const active = document.activeElement
          if (form && form.contains(active)) return
          if (toolbar && toolbar.contains(active)) return
          if (this.hasListQueryNavToolbarMarkedFocus()) return
          this.exitListQueryKeyboardNav()
        }, 0)
      }
      document.addEventListener('keydown', this.$_onListQueryNavKeydown, true)
      const form = this.getListQueryNavFormEl()
      if (form) {
        form.addEventListener('focusout', this.$_onListQueryNavFocusOut, true)
      }
      const toolbar = this.getListQueryNavToolbarEl()
      if (toolbar) {
        toolbar.addEventListener('focusout', this.$_onListQueryNavFocusOut, true)
      }
    },
    $_detachListQueryNavListeners() {
      if (!this.$_listQueryNavListenersBound) return
      this.$_listQueryNavListenersBound = false
      document.removeEventListener('keydown', this.$_onListQueryNavKeydown, true)
      const form = this.getListQueryNavFormEl()
      if (form && this.$_onListQueryNavFocusOut) {
        form.removeEventListener('focusout', this.$_onListQueryNavFocusOut, true)
      }
      const toolbar = this.getListQueryNavToolbarEl()
      if (toolbar && this.$_onListQueryNavFocusOut) {
        toolbar.removeEventListener('focusout', this.$_onListQueryNavFocusOut, true)
      }
    },
    $_bindListQueryNavFocusIn() {
      if (this.$_listQueryNavFocusInBound) return
      const form = this.getListQueryNavFormEl()
      if (!form) return
      this.$_listQueryNavFocusInBound = true
      this.$_onListQueryNavFocusIn = (e) => {
        this.ensureListQueryNavFromFocus(e.target)
      }
      form.addEventListener('focusin', this.$_onListQueryNavFocusIn, true)
    },
    $_unbindListQueryNavFocusIn() {
      if (!this.$_listQueryNavFocusInBound) return
      this.$_listQueryNavFocusInBound = false
      const form = this.getListQueryNavFormEl()
      if (form && this.$_onListQueryNavFocusIn) {
        form.removeEventListener('focusin', this.$_onListQueryNavFocusIn, true)
      }
      this.$_onListQueryNavFocusIn = null
    },
    $_bindListQueryNavToolbarFocusIn() {
      if (this.$_listQueryNavToolbarFocusInBound) return
      const toolbar = this.getListQueryNavToolbarEl()
      if (!toolbar) return
      this.$_listQueryNavToolbarFocusInBound = true
      this.$_onListQueryNavToolbarFocusIn = (e) => {
        this.ensureListQueryNavFromToolbarFocus(e.target)
      }
      toolbar.addEventListener('focusin', this.$_onListQueryNavToolbarFocusIn, true)
    },
    $_unbindListQueryNavToolbarFocusIn() {
      if (!this.$_listQueryNavToolbarFocusInBound) return
      this.$_listQueryNavToolbarFocusInBound = false
      const toolbar = this.getListQueryNavToolbarEl()
      if (toolbar && this.$_onListQueryNavToolbarFocusIn) {
        toolbar.removeEventListener('focusin', this.$_onListQueryNavToolbarFocusIn, true)
      }
      this.$_onListQueryNavToolbarFocusIn = null
    }
  }
}
