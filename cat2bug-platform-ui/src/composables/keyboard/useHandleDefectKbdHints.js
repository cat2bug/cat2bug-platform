import {
  getCurrentInstance,
  onActivated,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  ref,
  watch
} from 'vue'
import { isNativeFilePickerOpen } from '@/utils/native-file-picker'
import {
  assignDigitThenLetterKeys,
  assignToolbarHintKeys,
  getViewportRect,
  isElementInViewportRect,
  isTopDrawerVm,
  isVisibleEl,
  resolveDrawerScrollContainer
} from '@/utils/panel-kbd-hints'

const TOOL_LETTER_PREF = {
  assign: 'G',
  repair: 'F',
  reject: 'J',
  pass: 'P',
  open: 'O',
  close: 'B',
  edit: 'E',
  delete: 'D',
  restore: 'U'
}

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

export function useHandleDefectKbdHints() {
  const instance = getCurrentInstance()
  const vm = () => instance && instance.proxy
  const handleKbdHintsActive = ref(false)

  let $_handleKbdListenersBound = false
  let $_onHandleKbdKeydown = null
  let $_onHandleKbdKeyup = null
  let $_onHandleKbdBlur = null
  let $_handleKbdRevealTimer = null
  let $_handleKbdModifierHeld = false
  let $_handleKbdMap = null
  let $_handleKbdPending = null
  let $_handleKbdNodes = null
  let $_handleKbdScrollEl = null
  let $_onHandleKbdScroll = null
  let $_handleKbdRefreshRaf = null

  const $_nextTick = (cb) => {
    const host = vm()
    if (host && typeof host.$nextTick === 'function') {
      return host.$nextTick(cb)
    }
    return Promise.resolve().then(() => {
      if (typeof cb === 'function') cb()
    })
  }

  const getHandleKbdContentRoot = () => {
    const host = vm()
    if (!host) return null
    if (host.$refs.handleDefectBody) return host.$refs.handleDefectBody
    const drawer = host.$refs.handleDefectDrawer
    const drawerEl = drawer && drawer.$el
    if (drawerEl && typeof drawerEl.querySelector === 'function') {
      return drawerEl.querySelector('.defect-edit-body') ||
        drawerEl.querySelector('.el-drawer__body')
    }
    if (host.$el && typeof host.$el.querySelector === 'function') {
      return host.$el.querySelector('.defect-edit-body') ||
        host.$el.querySelector('.el-drawer__body')
    }
    return null
  }

  const getHandleKbdDrawerRoot = () => {
    const host = vm()
    if (!host) return null
    const drawer = host.$refs.handleDefectDrawer
    return (drawer && drawer.$el) || host.$el
  }

  const getHandleKbdContainer = () => {
    return getHandleKbdContentRoot() || getHandleKbdDrawerRoot()
  }

  const getHandleKbdScrollContainer = () => {
    return resolveDrawerScrollContainer(
      getHandleKbdContentRoot(),
      getHandleKbdDrawerRoot()
    )
  }

  const $_canActivateHandleKbdHints = () => {
    const host = vm()
    return !!(host && host.visible && isTopDrawerVm(host))
  }

  const $_clearHandleKbdRevealTimer = () => {
    if ($_handleKbdRevealTimer) {
      clearTimeout($_handleKbdRevealTimer)
      $_handleKbdRevealTimer = null
    }
  }

  const $_clearHandleKbdRefreshRaf = () => {
    if ($_handleKbdRefreshRaf) {
      cancelAnimationFrame($_handleKbdRefreshRaf)
      $_handleKbdRefreshRaf = null
    }
  }

  const $_detachHandleKbdScrollListener = () => {
    $_clearHandleKbdRefreshRaf()
    if ($_handleKbdScrollEl && $_onHandleKbdScroll) {
      $_handleKbdScrollEl.removeEventListener('scroll', $_onHandleKbdScroll)
    }
    $_handleKbdScrollEl = null
    $_onHandleKbdScroll = null
  }

  const $_hideHandleKbdBadges = () => {
    if ($_handleKbdNodes) {
      $_handleKbdNodes.forEach((entry) => {
        const badge = entry && entry.badge ? entry.badge : entry
        const host = entry && entry.host
        if (badge && badge.parentNode) badge.parentNode.removeChild(badge)
        if (host && host.classList) host.classList.remove('cat2bug-field-hint-host')
      })
    }
    $_handleKbdNodes = null
    if (handleKbdHintsActive.value) handleKbdHintsActive.value = false
  }

  const $_injectHandleKbdBadge = (anchor, letter) => {
    anchor.classList.add('cat2bug-field-hint-host')
    const badge = document.createElement('span')
    badge.className = 'cat2bug-field-hint handle-defect-kbd-hint'
    badge.textContent = letter
    badge.setAttribute('aria-hidden', 'true')
    anchor.appendChild(badge)
    return { badge, host: anchor }
  }

  const $_activateHandleCollapse = (name, anchor) => {
    const host = vm()
    if (!host) return
    if (name) {
      if (host.activeNames.includes(name)) {
        host.activeNames = host.activeNames.filter((n) => n !== name)
      } else {
        host.activeNames = [...host.activeNames, name]
      }
    }
    const header = anchor && anchor.closest('.el-collapse-item__header')
    const scrollTarget = header || anchor
    if (scrollTarget && typeof scrollTarget.scrollIntoView === 'function') {
      scrollTarget.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
    if (header && header.classList) {
      header.classList.remove('handle-defect-collapse-flash')
      void header.offsetWidth
      header.classList.add('handle-defect-collapse-flash')
      if (header.__handleCollapseFlashTimer) clearTimeout(header.__handleCollapseFlashTimer)
      header.__handleCollapseFlashTimer = setTimeout(() => {
        header.classList.remove('handle-defect-collapse-flash')
        header.__handleCollapseFlashTimer = null
      }, 1200)
    }
  }

  const $_buildHandleKbdHints = () => {
    const container = getHandleKbdContainer()
    const drawerRoot = getHandleKbdDrawerRoot()
    if (!container && !drawerRoot) return null
    const scrollContainer = getHandleKbdScrollContainer()
    const viewportRect = getViewportRect(scrollContainer, container || drawerRoot)
    const map = {}
    const pending = []
    const reserved = {}

    const toolBar = (drawerRoot && drawerRoot.querySelector('.defect-tools__bar')) ||
      (container && container.querySelector('.defect-tools__bar'))
    const toolButtons = toolBar
      ? Array.from(toolBar.querySelectorAll('[data-defect-tool]')).filter((btn) => isVisibleEl(btn))
      : []
    const toolItems = toolButtons.map((btn) => ({
      key: btn.getAttribute('data-defect-tool'),
      anchor: btn
    })).filter((t) => t.key)
    const toolLetters = assignToolbarHintKeys(toolItems, TOOL_LETTER_PREF)
    toolItems.forEach((item) => {
      const letter = toolLetters[item.key]
      if (!letter) return
      reserved[letter] = true
      map[letter] = {
        run: () => {
          if (item.anchor && typeof item.anchor.click === 'function') {
            item.anchor.click()
          }
        }
      }
      pending.push({ anchor: item.anchor, letter })
    })

    const collapseScope = container || drawerRoot
    const collapseHeaders = collapseScope
      ? Array.from(collapseScope.querySelectorAll('.handle-defect-collapse-title'))
        .filter((el) => isElementInViewportRect(el, viewportRect))
      : []
    const collapseKeys = assignDigitThenLetterKeys(collapseHeaders.length, reserved)
    collapseHeaders.forEach((anchor, i) => {
      const letter = collapseKeys[i]
      if (!letter) return
      const name = anchor.getAttribute('data-collapse-name')
      map[letter] = {
        run: () => $_activateHandleCollapse(name, anchor)
      }
      pending.push({ anchor, letter })
    })

    return Object.keys(map).length ? { map, pending } : null
  }

  const $_prepareHandleKbdHints = () => {
    if ($_handleKbdMap) return
    const built = $_buildHandleKbdHints()
    if (!built) return
    $_handleKbdMap = built.map
    $_handleKbdPending = built.pending
  }

  const $_revealHandleKbdBadges = () => {
    if (!$_handleKbdPending) return
    if (handleKbdHintsActive.value) $_hideHandleKbdBadges()
    const nodes = []
    $_handleKbdPending.forEach((p) => {
      if (p.anchor && p.anchor.isConnected !== false) {
        nodes.push($_injectHandleKbdBadge(p.anchor, p.letter))
      }
    })
    $_handleKbdNodes = nodes
    handleKbdHintsActive.value = true
  }

  const $_refreshHandleKbdHints = (reveal) => {
    const built = $_buildHandleKbdHints()
    $_hideHandleKbdBadges()
    if (!built) {
      $_handleKbdMap = null
      $_handleKbdPending = null
      return
    }
    $_handleKbdMap = built.map
    $_handleKbdPending = built.pending
    if (reveal) $_revealHandleKbdBadges()
  }

  const $_scheduleHandleKbdRefresh = () => {
    if ($_handleKbdRefreshRaf) return
    $_handleKbdRefreshRaf = requestAnimationFrame(() => {
      $_handleKbdRefreshRaf = null
      if (!$_handleKbdModifierHeld) return
      $_refreshHandleKbdHints(handleKbdHintsActive.value)
    })
  }

  const $_attachHandleKbdScrollListener = () => {
    const sc = getHandleKbdScrollContainer()
    if (!sc) return
    if ($_handleKbdScrollEl === sc) return
    $_detachHandleKbdScrollListener()
    $_handleKbdScrollEl = sc
    $_onHandleKbdScroll = () => $_scheduleHandleKbdRefresh()
    sc.addEventListener('scroll', $_onHandleKbdScroll, { passive: true })
  }

  const $_hideHandleKbdHints = () => {
    $_handleKbdModifierHeld = false
    $_clearHandleKbdRefreshRaf()
    $_clearHandleKbdRevealTimer()
    $_detachHandleKbdScrollListener()
    $_hideHandleKbdBadges()
    $_handleKbdMap = null
    $_handleKbdPending = null
  }

  const $_handleKbdKeydown = (e) => {
    if (!$_canActivateHandleKbdHints() || e.isComposing) return
    if (($_handleKbdModifierHeld || handleKbdHintsActive.value) &&
      !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
      $_hideHandleKbdHints()
    }
    if (isModifierKeyEvent(e)) {
      if (e.shiftKey) return
      $_handleKbdModifierHeld = true
      $_prepareHandleKbdHints()
      $_attachHandleKbdScrollListener()
      if (!handleKbdHintsActive.value && !$_handleKbdRevealTimer) {
        $_handleKbdRevealTimer = setTimeout(() => {
          $_handleKbdRevealTimer = null
          if (!$_handleKbdModifierHeld) return
          $_refreshHandleKbdHints(true)
        }, 180)
      }
      return
    }
    if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey) return
    if (!$_handleKbdMap) $_prepareHandleKbdHints()

    const k = e.key
    if (k === 'ArrowDown' || k === 'ArrowUp') {
      if (!$_handleKbdModifierHeld) return
      const sc = getHandleKbdScrollContainer()
      if (sc) {
        e.preventDefault()
        e.stopPropagation()
        const delta = Math.max(120, Math.round(sc.clientHeight * 0.4))
        sc.scrollBy({ top: k === 'ArrowDown' ? delta : -delta, behavior: 'smooth' })
        $_scheduleHandleKbdRefresh()
      }
      return
    }

    if (k && k.length === 1) {
      const letter = /^\d$/.test(k) ? k : k.toUpperCase()
      if ($_handleKbdMap && $_handleKbdMap[letter]) {
        e.preventDefault()
        e.stopPropagation()
        const target = $_handleKbdMap[letter]
        $_hideHandleKbdBadges()
        if (target && typeof target.run === 'function') {
          target.run()
        }
        $_nextTick(() => {
          if ($_handleKbdModifierHeld && (e.metaKey || e.ctrlKey)) {
            $_refreshHandleKbdHints(true)
          }
        })
      }
    }
  }

  const $_handleKbdKeyup = (e) => {
    const host = vm()
    if (!host || !host.visible) return
    if (isModifierKeyEvent(e)) {
      $_handleKbdModifierHeld = false
      $_clearHandleKbdRevealTimer()
      $_detachHandleKbdScrollListener()
      $_hideHandleKbdHints()
    }
  }

  const $_attachHandleKbdListeners = () => {
    if ($_handleKbdListenersBound) return
    $_handleKbdListenersBound = true
    $_onHandleKbdKeydown = (e) => $_handleKbdKeydown(e)
    $_onHandleKbdKeyup = (e) => $_handleKbdKeyup(e)
    $_onHandleKbdBlur = () => {
      if (isNativeFilePickerOpen()) return
      $_clearHandleKbdRevealTimer()
      $_hideHandleKbdHints()
    }
    document.addEventListener('keydown', $_onHandleKbdKeydown, true)
    window.addEventListener('keyup', $_onHandleKbdKeyup, true)
    window.addEventListener('blur', $_onHandleKbdBlur)
  }

  const $_detachHandleKbdListeners = () => {
    if (!$_handleKbdListenersBound) return
    $_handleKbdListenersBound = false
    $_clearHandleKbdRevealTimer()
    $_detachHandleKbdScrollListener()
    document.removeEventListener('keydown', $_onHandleKbdKeydown, true)
    window.removeEventListener('keyup', $_onHandleKbdKeyup, true)
    window.removeEventListener('blur', $_onHandleKbdBlur)
  }

  watch(() => vm() && vm().visible, (val) => {
    if (val) {
      $_attachHandleKbdListeners()
    } else {
      $_detachHandleKbdListeners()
      $_hideHandleKbdHints()
    }
  })

  const onHostMounted = () => {
    const host = vm()
    if (host && host.visible) {
      $_attachHandleKbdListeners()
    }
  }

  const onHostDeactivated = () => {
    $_detachHandleKbdListeners()
    $_hideHandleKbdHints()
  }

  onMounted(onHostMounted)
  onActivated(onHostMounted)
  onDeactivated(onHostDeactivated)
  onBeforeUnmount(onHostDeactivated)

  return {
    handleKbdHintsActive,
    getHandleKbdContentRoot,
    getHandleKbdDrawerRoot,
    getHandleKbdContainer,
    getHandleKbdScrollContainer,
    $_canActivateHandleKbdHints,
    $_attachHandleKbdListeners,
    $_detachHandleKbdListeners,
    $_clearHandleKbdRevealTimer,
    $_handleKbdKeydown,
    $_handleKbdKeyup,
    $_attachHandleKbdScrollListener,
    $_detachHandleKbdScrollListener,
    $_clearHandleKbdRefreshRaf,
    $_scheduleHandleKbdRefresh,
    $_prepareHandleKbdHints,
    $_refreshHandleKbdHints,
    $_buildHandleKbdHints,
    $_activateHandleCollapse,
    $_revealHandleKbdBadges,
    $_injectHandleKbdBadge,
    $_hideHandleKbdBadges,
    $_hideHandleKbdHints
  }
}
