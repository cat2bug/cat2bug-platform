import { getCurrentInstance, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from 'vue'
import { isNativeFilePickerOpen } from '@/utils/native-file-picker'
import {
  focusToolbarAtIndex,
  getToolbarFocusableIndex,
  getToolbarFocusables,
  isFocusInToolbar
} from '@/utils/list-query-toolbar-nav'
import {
  assignToolbarHintKeys,
  isTopDrawerVm,
  isVisibleEl
} from '@/utils/panel-kbd-hints'
import {
  bindSplitDropdownHost,
  clearAllSplitDropdownHostFocusVisuals,
  createDropdownMenuKeyboardState,
  focusSplitDropdownHost,
  isSplitDropdownOpen,
  openSplitDropdown,
  resolveSplitDropdownHost,
  SPLIT_DROPDOWN_FOCUS_CLASS,
  tryOpenToolbarFocusedSplitDropdown
} from '@/utils/split-dropdown-kbd'

const TOOL_LETTER_PREF = {
  delete: 'D',
  export: 'O'
}

const TOOLBAR_FOCUS_CLASS = 'list-query-toolbar-nav-focused'

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

export function useViewReportKbd() {
  const instance = getCurrentInstance()
  const vm = () => (instance && instance.proxy) || null

  const viewReportKbdHintsActive = ref(false)
  const viewReportToolbarNavIndex = ref(-1)
  const exportDropdownMenuState = ref(createDropdownMenuKeyboardState())

  let viewReportKbdListenersBound = false
  let viewReportToolbarFocusInBound = false
  let onViewReportKbdKeydown = null
  let onViewReportKbdKeyup = null
  let onViewReportKbdBlur = null
  let onViewReportToolbarFocusIn = null
  let viewReportKbdRevealTimer = null
  let viewReportKbdModifierHeld = false
  let viewReportKbdMap = null
  let viewReportKbdPending = null
  let viewReportKbdNodes = null

  const vmNextTick = (cb) => {
    const host = vm()
    if (host && typeof host.$nextTick === 'function') {
      return host.$nextTick(cb)
    }
    return nextTick(cb)
  }

  const getVisible = () => {
    const host = vm()
    return !!(host && host.visible)
  }

  const getViewReportToolbarEl = () => {
    const root = getViewReportKbdDrawerRoot()
    return (root && root.querySelector('.report-edit-tools')) || null
  }

  const $_resetViewReportToolbarNav = () => {
    viewReportToolbarNavIndex.value = -1
    clearViewReportToolbarFocusMarks()
  }

  const clearViewReportToolbarFocusMarks = () => {
    const toolbar = getViewReportToolbarEl()
    if (!toolbar) return
    toolbar.querySelectorAll(`.${TOOLBAR_FOCUS_CLASS}`).forEach((el) => {
      el.classList.remove(TOOLBAR_FOCUS_CLASS)
    })
  }

  const syncViewReportToolbarFocusClass = () => {
    const toolbar = getViewReportToolbarEl()
    if (toolbar) clearAllSplitDropdownHostFocusVisuals(toolbar)
    clearViewReportToolbarFocusMarks()
    if (viewReportToolbarNavIndex.value < 0) return
    const list = getToolbarFocusables(toolbar)
    const el = list[viewReportToolbarNavIndex.value]
    if (el) el.classList.add(TOOLBAR_FOCUS_CLASS)
  }

  const applyViewReportToolbarFocus = (index) => {
    const toolbar = getViewReportToolbarEl()
    const list = getToolbarFocusables(toolbar)
    if (!toolbar || index < 0 || index >= list.length) return false
    viewReportToolbarNavIndex.value = index
    const { focused, el, disabled } = focusToolbarAtIndex(toolbar, index)
    if (el && el.classList.contains(SPLIT_DROPDOWN_FOCUS_CLASS)) {
      const host = resolveSplitDropdownHost(el)
      if (host) {
        bindSplitDropdownHost(host)
        if (!disabled) focusSplitDropdownHost(host, { toolbarNav: true })
      }
    }
    syncViewReportToolbarFocusClass()
    return focused || !!el
  }

  const moveViewReportToolbar = (delta) => {
    const toolbar = getViewReportToolbarEl()
    const list = getToolbarFocusables(toolbar)
    if (!list.length) return false
    let idx = viewReportToolbarNavIndex.value
    if (idx < 0) {
      idx = getToolbarFocusableIndex(toolbar, document.activeElement)
    }
    const next = idx + delta
    if (next < 0 || next >= list.length) return false
    return applyViewReportToolbarFocus(next)
  }

  const isFocusInViewReportToolbar = (target) => {
    const toolbar = getViewReportToolbarEl()
    if (isFocusInToolbar(toolbar, target)) return true
    if (!toolbar) return false
    const marked = toolbar.querySelector(`.${TOOLBAR_FOCUS_CLASS}`)
    return !!marked
  }

  const isViewReportToolbarOverlayOpen = () => {
    const toolbar = getViewReportToolbarEl()
    if (!toolbar) return false
    const exportHost = toolbar.querySelector('.report-view-export')
    return !!(exportHost && isSplitDropdownOpen(exportHost))
  }

  const $_bindViewReportToolbarFocusIn = () => {
    const toolbar = getViewReportToolbarEl()
    if (!toolbar || viewReportToolbarFocusInBound) return
    viewReportToolbarFocusInBound = true
    onViewReportToolbarFocusIn = (e) => {
      if (!getVisible() || !e.target || !toolbar.contains(e.target)) return
      const idx = getToolbarFocusableIndex(toolbar, e.target)
      if (idx >= 0) {
        viewReportToolbarNavIndex.value = idx
        syncViewReportToolbarFocusClass()
      }
    }
    toolbar.addEventListener('focusin', onViewReportToolbarFocusIn, true)
  }

  const $_unbindViewReportToolbarFocusIn = () => {
    if (!viewReportToolbarFocusInBound) return
    viewReportToolbarFocusInBound = false
    const toolbar = getViewReportToolbarEl()
    if (toolbar && onViewReportToolbarFocusIn) {
      toolbar.removeEventListener('focusin', onViewReportToolbarFocusIn, true)
    }
    onViewReportToolbarFocusIn = null
  }

  const $_handleViewReportToolbarKeydown = (e) => {
    const toolbar = getViewReportToolbarEl()
    if (!toolbar) return false
    if (isViewReportToolbarOverlayOpen()) return false

    const inToolbar = isFocusInViewReportToolbar(document.activeElement)
    const list = getToolbarFocusables(toolbar)
    if (!list.length) return false

    if (inToolbar && (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar' || e.key === 'ArrowDown')) {
      if (tryOpenToolbarFocusedSplitDropdown(toolbar)) {
        e.preventDefault()
        e.stopPropagation()
        return true
      }
      const cur = list[viewReportToolbarNavIndex.value] ||
        list[getToolbarFocusableIndex(toolbar, document.activeElement)]
      if (cur && cur.classList.contains(SPLIT_DROPDOWN_FOCUS_CLASS)) {
        const host = resolveSplitDropdownHost(cur)
        if (host) {
          e.preventDefault()
          e.stopPropagation()
          focusSplitDropdownHost(host, { toolbarNav: true })
          openSplitDropdown(host, exportDropdownMenuState.value)
          return true
        }
      }
    }

    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return false
    if (!inToolbar && viewReportToolbarNavIndex.value < 0) return false

    e.preventDefault()
    e.stopPropagation()

    let idx = viewReportToolbarNavIndex.value
    if (idx < 0) idx = getToolbarFocusableIndex(toolbar, document.activeElement)

    if (e.key === 'ArrowLeft') {
      if (idx <= 0) {
        if (idx < 0 && list.length) applyViewReportToolbarFocus(0)
        return true
      }
      moveViewReportToolbar(-1)
      return true
    }

    if (idx < 0) {
      applyViewReportToolbarFocus(0)
      return true
    }
    if (idx >= list.length - 1) return true
    moveViewReportToolbar(1)
    return true
  }

  const $_bindViewReportExportDropdown = () => {
    const host = vm()
    const ref = host && host.$refs && host.$refs.exportDropdown
    const el = ref && ref.$el
    if (el && el.classList && el.classList.contains('el-dropdown')) {
      bindSplitDropdownHost(el)
    }
  }

  const getViewReportKbdDrawerRoot = () => {
    const host = vm()
    const drawer = host && host.$refs && host.$refs.viewReportDrawer
    return (drawer && drawer.$el) || (host && host.$el) || null
  }

  const $_canActivateViewReportKbdHints = () => {
    const host = vm()
    return getVisible() && !!host && isTopDrawerVm(host)
  }

  const $_attachViewReportKbdListeners = () => {
    if (viewReportKbdListenersBound) return
    viewReportKbdListenersBound = true
    onViewReportKbdKeydown = (e) => $_viewReportKbdKeydown(e)
    onViewReportKbdKeyup = (e) => $_viewReportKbdKeyup(e)
    onViewReportKbdBlur = () => {
      if (isNativeFilePickerOpen()) return
      $_clearViewReportKbdRevealTimer()
      $_hideViewReportKbdHints()
    }
    document.addEventListener('keydown', onViewReportKbdKeydown, true)
    window.addEventListener('keyup', onViewReportKbdKeyup, true)
    window.addEventListener('blur', onViewReportKbdBlur)
  }

  const $_detachViewReportKbdListeners = () => {
    if (!viewReportKbdListenersBound) return
    viewReportKbdListenersBound = false
    $_clearViewReportKbdRevealTimer()
    document.removeEventListener('keydown', onViewReportKbdKeydown, true)
    window.removeEventListener('keyup', onViewReportKbdKeyup, true)
    window.removeEventListener('blur', onViewReportKbdBlur)
  }

  const $_clearViewReportKbdRevealTimer = () => {
    if (viewReportKbdRevealTimer) {
      clearTimeout(viewReportKbdRevealTimer)
      viewReportKbdRevealTimer = null
    }
  }

  const $_viewReportKbdKeydown = (e) => {
    if (!$_canActivateViewReportKbdHints() || e.isComposing) return

    if (!e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey) {
      if ($_handleViewReportToolbarKeydown(e)) return
    }

    if ((viewReportKbdModifierHeld || viewReportKbdHintsActive.value) &&
      !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
      $_hideViewReportKbdHints()
    }
    if (isModifierKeyEvent(e)) {
      if (e.shiftKey) return
      viewReportKbdModifierHeld = true
      $_refreshViewReportKbdHints(false)
      if (!viewReportKbdHintsActive.value && !viewReportKbdRevealTimer) {
        viewReportKbdRevealTimer = setTimeout(() => {
          viewReportKbdRevealTimer = null
          if (!viewReportKbdModifierHeld) return
          $_refreshViewReportKbdHints(true)
        }, 180)
      }
      return
    }
    if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey) return
    $_refreshViewReportKbdHints(false)
    const k = e.key
    if (!k || k.length !== 1) return
    const letter = /^\d$/.test(k) ? k : k.toUpperCase()
    if (viewReportKbdMap && viewReportKbdMap[letter]) {
      e.preventDefault()
      e.stopPropagation()
      const target = viewReportKbdMap[letter]
      $_hideViewReportKbdBadges()
      if (target && typeof target.run === 'function') {
        target.run()
      }
      vmNextTick(() => {
        if (viewReportKbdModifierHeld && (e.metaKey || e.ctrlKey)) {
          $_refreshViewReportKbdHints(true)
        }
      })
    }
  }

  const $_viewReportKbdKeyup = (e) => {
    if (!getVisible()) return
    if (isModifierKeyEvent(e)) {
      viewReportKbdModifierHeld = false
      $_clearViewReportKbdRevealTimer()
      $_hideViewReportKbdHints()
    }
  }

  const $_buildViewReportKbdHints = () => {
    const drawerRoot = getViewReportKbdDrawerRoot()
    if (!drawerRoot) return null
    const map = {}
    const pending = []

    const toolButtons = Array.from(
      drawerRoot.querySelectorAll('[data-report-tool]')
    ).filter((btn) => isVisibleEl(btn))
    const toolItems = toolButtons.map((btn) => ({
      key: btn.getAttribute('data-report-tool'),
      anchor: btn
    })).filter((t) => t.key)
    const toolLetters = assignToolbarHintKeys(toolItems, TOOL_LETTER_PREF)
    toolItems.forEach((item) => {
      const letter = toolLetters[item.key]
      if (!letter) return
      map[letter] = {
        run: () => {
          if (item.key === 'export') {
            focusSplitDropdownHost(item.anchor, { toolbarNav: true })
            viewReportToolbarNavIndex.value = getToolbarFocusableIndex(
              getViewReportToolbarEl(),
              item.anchor
            )
            syncViewReportToolbarFocusClass()
            openSplitDropdown(item.anchor, exportDropdownMenuState.value)
            return
          }
          const focusTarget = item.anchor.querySelector('button') || item.anchor
          if (focusTarget && typeof focusTarget.focus === 'function') {
            focusTarget.focus()
          }
          if (focusTarget && typeof focusTarget.click === 'function') {
            focusTarget.click()
          }
          vmNextTick(() => {
            const idx = getToolbarFocusableIndex(getViewReportToolbarEl(), focusTarget)
            if (idx >= 0) {
              viewReportToolbarNavIndex.value = idx
              syncViewReportToolbarFocusClass()
            }
          })
        }
      }
      pending.push({ anchor: item.anchor, letter })
    })

    return pending.length ? { map, pending } : null
  }

  const $_refreshViewReportKbdHints = (reveal) => {
    const built = $_buildViewReportKbdHints()
    $_hideViewReportKbdBadges()
    if (!built) {
      viewReportKbdMap = null
      viewReportKbdPending = null
      return
    }
    viewReportKbdMap = built.map
    viewReportKbdPending = built.pending
    if (reveal) $_revealViewReportKbdBadges()
  }

  const $_revealViewReportKbdBadges = () => {
    if (!viewReportKbdPending) return
    if (viewReportKbdHintsActive.value) $_hideViewReportKbdBadges()
    const nodes = []
    viewReportKbdPending.forEach((p) => {
      if (p.anchor && p.anchor.isConnected !== false) {
        nodes.push($_injectViewReportKbdBadge(p.anchor, p.letter, p.hintClass))
      }
    })
    viewReportKbdNodes = nodes
    viewReportKbdHintsActive.value = true
  }

  const $_injectViewReportKbdBadge = (anchor, letter, hintClass) => {
    anchor.classList.add('cat2bug-field-hint-host')
    const badge = document.createElement('span')
    badge.className = 'cat2bug-field-hint view-report-kbd-hint' + (hintClass ? ` ${hintClass}` : '')
    badge.textContent = letter
    badge.setAttribute('aria-hidden', 'true')
    anchor.appendChild(badge)
    return { badge, host: anchor }
  }

  const $_hideViewReportKbdBadges = () => {
    if (viewReportKbdNodes) {
      viewReportKbdNodes.forEach((entry) => {
        const badge = entry && entry.badge ? entry.badge : entry
        const host = entry && entry.host
        if (badge && badge.parentNode) badge.parentNode.removeChild(badge)
        if (host && host.classList) host.classList.remove('cat2bug-field-hint-host')
      })
    }
    viewReportKbdNodes = null
    if (viewReportKbdHintsActive.value) viewReportKbdHintsActive.value = false
  }

  const $_hideViewReportKbdHints = () => {
    viewReportKbdModifierHeld = false
    $_clearViewReportKbdRevealTimer()
    $_hideViewReportKbdBadges()
    viewReportKbdMap = null
    viewReportKbdPending = null
  }

  const shortcutClose = (e) => {
    const host = vm()
    if (e) {
      e.preventDefault()
      e.stopPropagation()
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
    }
    if (host && typeof host.cancel === 'function') {
      host.cancel()
    }
  }

  const syncListenersByVisible = (visible) => {
    if (visible) {
      $_attachViewReportKbdListeners()
      vmNextTick(() => {
        $_bindViewReportExportDropdown()
        $_bindViewReportToolbarFocusIn()
      })
    } else {
      $_detachViewReportKbdListeners()
      $_unbindViewReportToolbarFocusIn()
      $_resetViewReportToolbarNav()
      $_hideViewReportKbdHints()
    }
  }

  watch(
    () => {
      const host = vm()
      return !!(host && host.visible)
    },
    (val) => {
      syncListenersByVisible(!!val)
    }
  )

  const onHostMounted = () => {
    syncListenersByVisible(getVisible())
  }

  const onHostDeactivated = () => {
    $_detachViewReportKbdListeners()
    $_unbindViewReportToolbarFocusIn()
    $_resetViewReportToolbarNav()
    $_hideViewReportKbdHints()
  }

  onMounted(onHostMounted)
  onActivated(onHostMounted)
  onDeactivated(onHostDeactivated)
  onBeforeUnmount(onHostDeactivated)

  return {
    viewReportKbdHintsActive,
    viewReportToolbarNavIndex,
    exportDropdownMenuState,
    getViewReportToolbarEl,
    $_resetViewReportToolbarNav,
    clearViewReportToolbarFocusMarks,
    syncViewReportToolbarFocusClass,
    applyViewReportToolbarFocus,
    moveViewReportToolbar,
    isFocusInViewReportToolbar,
    isViewReportToolbarOverlayOpen,
    $_bindViewReportToolbarFocusIn,
    $_unbindViewReportToolbarFocusIn,
    $_handleViewReportToolbarKeydown,
    $_bindViewReportExportDropdown,
    getViewReportKbdDrawerRoot,
    $_canActivateViewReportKbdHints,
    $_attachViewReportKbdListeners,
    $_detachViewReportKbdListeners,
    $_clearViewReportKbdRevealTimer,
    $_viewReportKbdKeydown,
    $_viewReportKbdKeyup,
    $_buildViewReportKbdHints,
    $_refreshViewReportKbdHints,
    $_revealViewReportKbdBadges,
    $_injectViewReportKbdBadge,
    $_hideViewReportKbdBadges,
    $_hideViewReportKbdHints,
    shortcutClose
  }
}
