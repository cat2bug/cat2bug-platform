/**
 * 列表页查询区键盘导航：←/→ 在查询控件间切换；最右项 → 进入右侧工具栏，
 * 工具栏内 ←/→ 在按钮间切换，首按钮 ← 回到最右查询项。Esc 退出。
 */
import { getCurrentInstance, nextTick, onBeforeUnmount, onDeactivated, onMounted, ref } from 'vue'
import {
  focusToolbarAtIndex,
  getFirstToolbarFocusable,
  getToolbarFocusableIndex,
  getToolbarFocusables,
  isFocusInToolbar,
  isQueryNavOverlayOpen
} from '@/utils/list-query-toolbar-nav'
import {
  isColumnPickerOpenInToolbar,
  openColumnPickerFromToolbarFocus
} from '@/utils/column-picker-popover-kbd'
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

export function useListQueryKeyboardNav() {
  const listQueryNavActive = ref(false)
  const listQueryNavIndex = ref(-1)
  const listQueryNavToolbarIndex = ref(-1)
  const listQueryNavSuppressBlur = ref(false)

  const instance = getCurrentInstance()
  let listQueryNavListenersBound = false
  let listQueryNavFocusInBound = false
  let listQueryNavToolbarFocusInBound = false
  let onListQueryNavKeydown = null
  let onListQueryNavFocusOut = null
  let onListQueryNavFocusIn = null
  let onListQueryNavToolbarFocusIn = null

  const vm = () => (instance && instance.proxy) || null

  const vmNextTick = (cb) => {
    const host = vm()
    if (host && typeof host.$nextTick === 'function') {
      return host.$nextTick(cb)
    }
    return nextTick(cb)
  }

  const getHostOverrideMethod = (name) => {
    const host = vm()
    if (!host || !host.$options || !host.$options.methods) return null
    const method = host.$options.methods[name]
    return typeof method === 'function' ? method : null
  }

  const getHostShowSearch = () => {
    const host = vm()
    return host ? host.showSearch : undefined
  }

  const getListQueryNavFormRef = () => {
    const method = getHostOverrideMethod('getListQueryNavFormRef')
    if (method) return method.call(vm())
    return 'queryForm'
  }

  const getListQueryNavToolbarRef = () => {
    const method = getHostOverrideMethod('getListQueryNavToolbarRef')
    if (method) return method.call(vm())
    return null
  }

  const getListQueryNavItems = () => {
    const method = getHostOverrideMethod('getListQueryNavItems')
    const result = method ? method.call(vm()) : []
    return Array.isArray(result) ? result : []
  }

  const getListQueryNavDefaultIndex = () => {
    const method = getHostOverrideMethod('getListQueryNavDefaultIndex')
    const result = method ? method.call(vm()) : 0
    return Number.isInteger(result) ? result : 0
  }

  const isListQueryNavToolbarOverlayOpen = () => {
    const toolbar = getListQueryNavToolbarEl()
    if (!toolbar) return false
    if (isColumnPickerOpenInToolbar(toolbar)) return true
    const hosts = toolbar.querySelectorAll(DROPDOWN_HOST_SELECTOR)
    for (let i = 0; i < hosts.length; i++) {
      if (isSplitDropdownOpen(hosts[i])) return true
    }
    return false
  }

  const getListQueryNavFormEl = () => {
    const host = vm()
    if (!host) return null
    const ref = getListQueryNavFormRef()
    const form = ref && host.$refs && host.$refs[ref]
    return (form && form.$el) || null
  }

  const getListQueryNavToolbarEl = () => {
    const host = vm()
    if (!host) return null
    const ref = getListQueryNavToolbarRef()
    if (!ref) return null
    const node = host.$refs && host.$refs[ref]
    return (node && node.$el) || node || null
  }

  const hasListQueryNavToolbarBridge = () => {
    return getToolbarFocusables(getListQueryNavToolbarEl()).length > 0
  }

  const getListQueryNavItemEl = (key) => {
    const form = getListQueryNavFormEl()
    if (!form || !key) return null
    return form.querySelector(`.${NAV_ITEM_CLASS}[data-query-key="${String(key)}"]`)
  }

  const getListQueryNavFocusEl = (key) => {
    const itemEl = getListQueryNavItemEl(key)
    if (!itemEl) return null
    return itemEl.querySelector('.select-project-member-input') ||
      itemEl.querySelector('input.el-input__inner') ||
      itemEl.querySelector('input, textarea, [tabindex]:not([tabindex="-1"])')
  }

  const isListQueryNavOverlayOpen = () => {
    return isQueryNavOverlayOpen()
  }

  const getListQueryNavIndexByKey = (key) => {
    return getListQueryNavItems().findIndex((item) => item.key === key)
  }

  const isListQueryNavToolbarFirstFocused = () => {
    return listQueryNavToolbarIndex.value === 0 &&
      isFocusInListQueryNavToolbar(document.activeElement)
  }

  const hasListQueryNavToolbarMarkedFocus = () => {
    if (!listQueryNavActive.value || listQueryNavToolbarIndex.value < 0) return false
    const toolbar = getListQueryNavToolbarEl()
    if (!toolbar) return false
    const list = getToolbarFocusables(toolbar)
    const el = list[listQueryNavToolbarIndex.value]
    return !!(el && el.classList && el.classList.contains(TOOLBAR_FOCUS_CLASS))
  }

  const isFocusInListQueryNavToolbar = (target) => {
    const toolbar = getListQueryNavToolbarEl()
    if (isFocusInToolbar(toolbar, target)) return true
    return hasListQueryNavToolbarMarkedFocus()
  }

  const clearListQueryNavToolbarFocusMarks = () => {
    const toolbar = getListQueryNavToolbarEl()
    if (!toolbar) return
    toolbar.querySelectorAll(`.${TOOLBAR_FOCUS_CLASS}`).forEach((el) => {
      el.classList.remove(TOOLBAR_FOCUS_CLASS)
    })
  }

  const syncListQueryNavToolbarFocusClass = () => {
    const toolbar = getListQueryNavToolbarEl()
    if (toolbar) clearAllSplitDropdownHostFocusVisuals(toolbar)
    clearListQueryNavToolbarFocusMarks()
    if (!listQueryNavActive.value || listQueryNavToolbarIndex.value < 0) return
    const list = getToolbarFocusables(toolbar)
    const el = list[listQueryNavToolbarIndex.value]
    if (!el) return
    const disabled = !!(el.disabled || el.getAttribute('aria-disabled') === 'true')
    if (disabled) {
      el.classList.add(TOOLBAR_FOCUS_CLASS)
      return
    }
    const active = document.activeElement
    const hasNativeFocus = el === active || (active && el.contains && el.contains(active))
    // 已有原生 :focus 时不再叠加导航 class，避免同一按钮出现双层焦点环
    if (!hasNativeFocus) {
      el.classList.add(TOOLBAR_FOCUS_CLASS)
    }
  }

  const syncListQueryNavToolbarIndexFromFocus = (target) => {
    const toolbar = getListQueryNavToolbarEl()
    const idx = getToolbarFocusableIndex(toolbar, target || document.activeElement)
    if (idx >= 0) {
      listQueryNavToolbarIndex.value = idx
      clearListQueryNavFocusMarks()
      syncListQueryNavToolbarFocusClass()
    }
  }

  const ensureListQueryNavFromFocus = (target) => {
    if (!target || getHostShowSearch() === false) return
    const itemEl = target.closest && target.closest(`.${NAV_ITEM_CLASS}[data-query-key]`)
    if (!itemEl) return
    const form = getListQueryNavFormEl()
    if (!form || !form.contains(itemEl)) return
    const key = itemEl.getAttribute('data-query-key')
    const index = getListQueryNavIndexByKey(key)
    if (index < 0) return
    listQueryNavActive.value = true
    listQueryNavIndex.value = index
    listQueryNavToolbarIndex.value = -1
    syncListQueryNavFocusClass()
    clearListQueryNavToolbarFocusMarks()
    $_attachListQueryNavListeners()
  }

  const ensureListQueryNavFromToolbarFocus = (target) => {
    if (!target) return
    const toolbar = getListQueryNavToolbarEl()
    if (!toolbar || !toolbar.contains(target)) return
    const idx = getToolbarFocusableIndex(toolbar, target)
    if (idx < 0) return
    listQueryNavActive.value = true
    listQueryNavToolbarIndex.value = idx
    clearListQueryNavFocusMarks()
    syncListQueryNavToolbarFocusClass()
    $_attachListQueryNavListeners()
  }

  const clearListQueryNavFocusMarks = () => {
    const form = getListQueryNavFormEl()
    if (!form) return
    form.querySelectorAll(`.${NAV_ITEM_CLASS}.${QUERY_FOCUS_CLASS}`).forEach((el) => {
      el.classList.remove(QUERY_FOCUS_CLASS)
    })
  }

  const syncListQueryNavFocusClass = () => {
    clearListQueryNavFocusMarks()
    if (!listQueryNavActive.value || listQueryNavIndex.value < 0) return
    if (isFocusInListQueryNavToolbar(document.activeElement)) return
    const items = getListQueryNavItems()
    const item = items[listQueryNavIndex.value]
    if (!item) return
    const itemEl = getListQueryNavItemEl(item.key)
    if (itemEl) itemEl.classList.add(QUERY_FOCUS_CLASS)
  }

  const applyListQueryNavFocus = (index) => {
    const items = getListQueryNavItems()
    if (index < 0 || index >= items.length) return
    listQueryNavSuppressBlur.value = true
    listQueryNavIndex.value = index
    listQueryNavToolbarIndex.value = -1
    const item = items[index]
    const itemEl = getListQueryNavItemEl(item.key)
    const form = getListQueryNavFormEl()
    const toolbar = getListQueryNavToolbarEl()
    const prevActive = document.activeElement
    if (prevActive && toolbar && toolbar.contains(prevActive) && typeof prevActive.blur === 'function') {
      prevActive.blur()
    }
    if (form) patchDatePickerKeyboard(form)

    let focusEl = getListQueryNavFocusEl(item.key)
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
    clearListQueryNavToolbarFocusMarks()
    syncListQueryNavFocusClass()
    vmNextTick(() => {
      listQueryNavSuppressBlur.value = false
    })
  }

  const applyListQueryNavToolbarFocus = (index) => {
    const toolbar = getListQueryNavToolbarEl()
    const list = getToolbarFocusables(toolbar)
    if (index < 0 || index >= list.length) return false
    listQueryNavSuppressBlur.value = true
    listQueryNavToolbarIndex.value = index
    listQueryNavActive.value = true
    clearListQueryNavFocusMarks()
    clearListQueryNavToolbarFocusMarks()
    const prevActive = document.activeElement
    const form = getListQueryNavFormEl()
    if (prevActive) {
      if (toolbar && toolbar.contains(prevActive) && prevActive !== list[index] && typeof prevActive.blur === 'function') {
        prevActive.blur()
      } else if (form && form.contains(prevActive) && typeof prevActive.blur === 'function') {
        prevActive.blur()
      }
    }
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
    syncListQueryNavToolbarFocusClass()
    $_attachListQueryNavListeners()
    vmNextTick(() => {
      listQueryNavSuppressBlur.value = false
      syncListQueryNavToolbarFocusClass()
    })
    return true
  }

  const focusListQueryNavToolbarFirst = () => {
    const items = getListQueryNavItems()
    if (items.length) {
      listQueryNavIndex.value = items.length - 1
    }
    return applyListQueryNavToolbarFocus(0)
  }

  const focusListQueryNavLastQuery = () => {
    const items = getListQueryNavItems()
    if (!items.length) return
    applyListQueryNavFocus(items.length - 1)
  }

  const moveListQueryNavToolbar = (delta) => {
    const toolbar = getListQueryNavToolbarEl()
    const list = getToolbarFocusables(toolbar)
    if (!list.length) return
    let idx = listQueryNavToolbarIndex.value
    if (idx < 0) {
      idx = getToolbarFocusableIndex(toolbar, document.activeElement)
    }
    const next = idx + delta
    if (next < 0 || next >= list.length) return
    applyListQueryNavToolbarFocus(next)
  }

  const enterListQueryKeyboardNav = () => {
    if (getHostShowSearch() === false) return
    const items = getListQueryNavItems()
    if (!items.length) return
    let idx = getListQueryNavDefaultIndex()
    if (idx < 0 || idx >= items.length) idx = 0
    listQueryNavActive.value = true
    listQueryNavIndex.value = idx
    listQueryNavToolbarIndex.value = -1
    vmNextTick(() => {
      const form = getListQueryNavFormEl()
      if (form) patchDatePickerKeyboard(form)
      applyListQueryNavFocus(idx)
      vmNextTick(() => $_attachListQueryNavListeners())
    })
  }

  const exitListQueryKeyboardNav = () => {
    if (!listQueryNavActive.value && !listQueryNavListenersBound) return
    listQueryNavActive.value = false
    listQueryNavIndex.value = -1
    listQueryNavToolbarIndex.value = -1
    clearListQueryNavFocusMarks()
    clearListQueryNavToolbarFocusMarks()
    $_detachListQueryNavListeners()
  }

  const moveListQueryNav = (delta) => {
    const items = getListQueryNavItems()
    const next = listQueryNavIndex.value + delta
    if (next < 0 || next >= items.length) return
    applyListQueryNavFocus(next)
  }

  const $_attachListQueryNavListeners = () => {
    if (listQueryNavListenersBound) return
    listQueryNavListenersBound = true
    onListQueryNavKeydown = (e) => {
      if (!listQueryNavActive.value) return
      const inToolbar = isFocusInListQueryNavToolbar(document.activeElement)
      if (!inToolbar && isDatePickerPanelOpen()) {
        const rangeKeys = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Enter', ' ', 'Spacebar']
        if (rangeKeys.indexOf(e.key) >= 0) {
          if (handleActiveDateRangeKeydown(e)) return
          return
        }
      }
      if (inToolbar && isListQueryNavToolbarOverlayOpen()) return
      if (!inToolbar && isListQueryNavOverlayOpen()) return

      if (e.key === 'ArrowLeft') {
        if (inToolbar) {
          e.preventDefault()
          e.stopPropagation()
          const toolbar = getListQueryNavToolbarEl()
          let curIdx = listQueryNavToolbarIndex.value
          if (curIdx < 0) {
            curIdx = getToolbarFocusableIndex(toolbar, document.activeElement)
          }
          if (curIdx <= 0) {
            focusListQueryNavLastQuery()
            return
          }
          moveListQueryNavToolbar(-1)
          return
        }
        e.preventDefault()
        e.stopPropagation()
        moveListQueryNav(-1)
        return
      }
      if (e.key === 'ArrowRight') {
        if (inToolbar) {
          e.preventDefault()
          e.stopPropagation()
          moveListQueryNavToolbar(1)
          return
        }
        const items = getListQueryNavItems()
        if (items.length && listQueryNavIndex.value === items.length - 1 && hasListQueryNavToolbarBridge()) {
          e.preventDefault()
          e.stopPropagation()
          focusListQueryNavToolbarFirst()
          return
        }
        e.preventDefault()
        e.stopPropagation()
        moveListQueryNav(1)
        return
      }
      if (e.key === 'Escape') {
        if (!inToolbar && isListQueryNavOverlayOpen()) return
        e.preventDefault()
        e.stopPropagation()
        exitListQueryKeyboardNav()
        return
      }
      if (inToolbar && (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar' || e.key === 'ArrowDown')) {
        const toolbar = getListQueryNavToolbarEl()
        if (e.key === 'ArrowDown' && openColumnPickerFromToolbarFocus(toolbar)) {
          e.preventDefault()
          e.stopPropagation()
          return
        }
        if (tryOpenToolbarFocusedSplitDropdown(toolbar)) {
          e.preventDefault()
          e.stopPropagation()
          return
        }
        const list = getToolbarFocusables(toolbar)
        const cur = list[listQueryNavToolbarIndex.value]
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
    onListQueryNavFocusOut = (e) => {
      if (!listQueryNavActive.value || listQueryNavSuppressBlur.value) return
      const form = getListQueryNavFormEl()
      const toolbar = getListQueryNavToolbarEl()
      if (form && e.relatedTarget && form.contains(e.relatedTarget)) return
      if (toolbar && e.relatedTarget && toolbar.contains(e.relatedTarget)) return
      setTimeout(() => {
        if (!listQueryNavActive.value || listQueryNavSuppressBlur.value) return
        const active = document.activeElement
        if (form && form.contains(active)) return
        if (toolbar && toolbar.contains(active)) return
        if (hasListQueryNavToolbarMarkedFocus()) return
        exitListQueryKeyboardNav()
      }, 0)
    }
    document.addEventListener('keydown', onListQueryNavKeydown, true)
    const form = getListQueryNavFormEl()
    if (form) {
      form.addEventListener('focusout', onListQueryNavFocusOut, true)
    }
    const toolbar = getListQueryNavToolbarEl()
    if (toolbar) {
      toolbar.addEventListener('focusout', onListQueryNavFocusOut, true)
    }
  }

  const $_detachListQueryNavListeners = () => {
    if (!listQueryNavListenersBound) return
    listQueryNavListenersBound = false
    document.removeEventListener('keydown', onListQueryNavKeydown, true)
    const form = getListQueryNavFormEl()
    if (form && onListQueryNavFocusOut) {
      form.removeEventListener('focusout', onListQueryNavFocusOut, true)
    }
    const toolbar = getListQueryNavToolbarEl()
    if (toolbar && onListQueryNavFocusOut) {
      toolbar.removeEventListener('focusout', onListQueryNavFocusOut, true)
    }
  }

  const $_bindListQueryNavFocusIn = () => {
    if (listQueryNavFocusInBound) return
    const form = getListQueryNavFormEl()
    if (!form) return
    listQueryNavFocusInBound = true
    onListQueryNavFocusIn = (e) => {
      ensureListQueryNavFromFocus(e.target)
    }
    form.addEventListener('focusin', onListQueryNavFocusIn, true)
  }

  const $_unbindListQueryNavFocusIn = () => {
    if (!listQueryNavFocusInBound) return
    listQueryNavFocusInBound = false
    const form = getListQueryNavFormEl()
    if (form && onListQueryNavFocusIn) {
      form.removeEventListener('focusin', onListQueryNavFocusIn, true)
    }
    onListQueryNavFocusIn = null
  }

  const $_bindListQueryNavToolbarFocusIn = () => {
    if (listQueryNavToolbarFocusInBound) return
    const toolbar = getListQueryNavToolbarEl()
    if (!toolbar) return
    listQueryNavToolbarFocusInBound = true
    onListQueryNavToolbarFocusIn = (e) => {
      ensureListQueryNavFromToolbarFocus(e.target)
    }
    toolbar.addEventListener('focusin', onListQueryNavToolbarFocusIn, true)
  }

  const $_unbindListQueryNavToolbarFocusIn = () => {
    if (!listQueryNavToolbarFocusInBound) return
    listQueryNavToolbarFocusInBound = false
    const toolbar = getListQueryNavToolbarEl()
    if (toolbar && onListQueryNavToolbarFocusIn) {
      toolbar.removeEventListener('focusin', onListQueryNavToolbarFocusIn, true)
    }
    onListQueryNavToolbarFocusIn = null
  }

  onMounted(() => {
    vmNextTick(() => {
      $_bindListQueryNavFocusIn()
      $_bindListQueryNavToolbarFocusIn()
    })
  })

  onBeforeUnmount(() => {
    exitListQueryKeyboardNav()
    $_unbindListQueryNavFocusIn()
    $_unbindListQueryNavToolbarFocusIn()
  })

  onDeactivated(() => {
    exitListQueryKeyboardNav()
  })

  return {
    listQueryNavActive,
    listQueryNavIndex,
    listQueryNavToolbarIndex,
    listQueryNavSuppressBlur,
    isListQueryNavToolbarOverlayOpen,
    getListQueryNavFormEl,
    getListQueryNavToolbarEl,
    hasListQueryNavToolbarBridge,
    getListQueryNavItemEl,
    getListQueryNavFocusEl,
    isListQueryNavOverlayOpen,
    getListQueryNavIndexByKey,
    isListQueryNavToolbarFirstFocused,
    hasListQueryNavToolbarMarkedFocus,
    isFocusInListQueryNavToolbar,
    clearListQueryNavToolbarFocusMarks,
    syncListQueryNavToolbarFocusClass,
    syncListQueryNavToolbarIndexFromFocus,
    ensureListQueryNavFromFocus,
    ensureListQueryNavFromToolbarFocus,
    clearListQueryNavFocusMarks,
    syncListQueryNavFocusClass,
    applyListQueryNavFocus,
    applyListQueryNavToolbarFocus,
    focusListQueryNavToolbarFirst,
    focusListQueryNavLastQuery,
    moveListQueryNavToolbar,
    enterListQueryKeyboardNav,
    exitListQueryKeyboardNav,
    moveListQueryNav,
    $_attachListQueryNavListeners,
    $_detachListQueryNavListeners,
    $_bindListQueryNavFocusIn,
    $_unbindListQueryNavFocusIn,
    $_bindListQueryNavToolbarFocusIn,
    $_unbindListQueryNavToolbarFocusIn
  }
}
