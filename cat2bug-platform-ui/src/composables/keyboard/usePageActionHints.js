/**
 * 列表/页面级快捷键提示：按住 Cmd/Ctrl 在工具栏控件上显示字母徽标，再按字母触发动作。
 * 徽标通过 fixed 浮层渲染到 body，避免 Tab/表格等 stacking context 遮挡。
 *
 * 宿主组件需实现 getPageActionHints()，返回：
 *   { key?, letter, badgeSelector, floatOffset?: { dx, dy }, run, visible? }
 */
import {
  getCurrentInstance,
  nextTick,
  onActivated,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  ref
} from 'vue'
import { isNativeFilePickerOpen } from '@/utils/native-file-picker'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { findTopFormDrawerVm } from '@/utils/defect-drawer-shortcuts'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { normalizeKey } from '@/plugins/shortcut/keymap'
import {
  getElementTextRect,
  PAGE_KBD_OVERLAY_ID,
  resolvePageActionLetter
} from '@/utils/defect-row-kbd-hints'
import { PAGE_ACTION_RESERVED } from '@/plugins/shortcut/reserved-keys'

const OVERLAY_ID = PAGE_KBD_OVERLAY_ID

/** 跨路由切换时追踪 Cmd/Ctrl 是否仍按住（避免监听器卸载间隙丢失 keyup） */
let pageActionGlobalMetaHeld = false
let pageActionGlobalModifierTrackerBound = false

function ensurePageActionGlobalModifierTracker() {
  if (pageActionGlobalModifierTrackerBound) return
  pageActionGlobalModifierTrackerBound = true
  window.addEventListener('keydown', (e) => {
    if (e.metaKey || e.ctrlKey) pageActionGlobalMetaHeld = true
  }, true)
  window.addEventListener('keyup', (e) => {
    if (isModifierKeyEvent(e)) pageActionGlobalMetaHeld = false
  }, true)
  window.addEventListener('blur', () => {
    pageActionGlobalMetaHeld = false
  })
}

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

function isShiftKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Shift' || k === 'ShiftLeft' || k === 'ShiftRight'
}

function shouldDeferPageActionShortcuts() {
  return hasBlockingUiLayer() || !!findTopFormDrawerVm()
}

function shouldDeferPageActionHintsForVm(vm) {
  if (vm && typeof vm.shouldDeferPageActionHints === 'function') {
    return vm.shouldDeferPageActionHints()
  }
  return shouldDeferPageActionShortcuts()
}

function isElementInViewport(el, minPx = 4) {
  if (!el || typeof el.getBoundingClientRect !== 'function') return false
  const rect = el.getBoundingClientRect()
  if (rect.width <= 0 && rect.height <= 0) return false
  const vh = window.innerHeight || document.documentElement.clientHeight
  const vw = window.innerWidth || document.documentElement.clientWidth
  const overlapTop = Math.max(rect.top, 0)
  const overlapBottom = Math.min(rect.bottom, vh)
  const overlapLeft = Math.max(rect.left, 0)
  const overlapRight = Math.min(rect.right, vw)
  return overlapBottom - overlapTop >= minPx && overlapRight - overlapLeft >= minPx
}

function ensurePageHintOverlay() {
  let el = document.getElementById(OVERLAY_ID)
  if (!el) {
    el = document.createElement('div')
    el.id = OVERLAY_ID
    el.className = 'cat2bug-page-kbd-hint-layer'
    el.setAttribute('aria-hidden', 'true')
    document.body.appendChild(el)
  }
  return el
}

function clearPageHintOverlay() {
  const el = document.getElementById(OVERLAY_ID)
  if (el) el.innerHTML = ''
}

/** 将徽标右下角对齐到锚点右下角（可微偏移） */
function buildFloatBadgeStyle(rect, floatOffset) {
  const dx = (floatOffset && floatOffset.dx) || 0
  const dy = (floatOffset && floatOffset.dy) || 0
  const outset = floatOffset && floatOffset.outset != null ? floatOffset.outset : 2
  const placement = (floatOffset && floatOffset.placement) || 'bottom-right-outset'

  if (placement === 'center-right') {
    return {
      left: `${rect.right + dx + outset}px`,
      top: `${rect.top + rect.height / 2 + dy}px`,
      transform: 'translate(-100%, -50%)'
    }
  }
  if (placement === 'bottom-right-inset') {
    return {
      left: `${rect.right + dx - outset}px`,
      top: `${rect.bottom + dy - outset}px`,
      transform: 'translate(-100%, -100%)'
    }
  }
  if (placement === 'below-right-outset') {
    return {
      left: `${rect.right + dx + outset}px`,
      top: `${rect.bottom + dy + outset}px`,
      transform: 'translate(-100%, 0)'
    }
  }
  if (placement === 'center-left-inset') {
    return {
      left: `${rect.left + dx + outset}px`,
      top: `${rect.top + rect.height / 2 + dy}px`,
      transform: 'translate(0, -50%)'
    }
  }
  if (placement === 'center-cell') {
    return {
      left: `${rect.left + rect.width / 2 + dx}px`,
      top: `${rect.top + rect.height / 2 + dy}px`,
      transform: 'translate(-50%, -50%)'
    }
  }
  return {
    left: `${rect.right + dx + outset}px`,
    top: `${rect.bottom + dy + outset}px`,
    transform: 'translate(-100%, -100%)'
  }
}

export function usePageActionHints() {
  const instance = getCurrentInstance()
  const vm = () => instance?.proxy

  const pageHintsActive = ref(false)

  let pageActionHintListenersBound = false
  let onPageActionHintKeydown = null
  let onPageActionHintKeyup = null
  let onPageActionHintBlur = null
  let pageActionHintRevealTimer = null
  let pageActionModifierHeld = false
  let pageActionHintMap = null
  let pageActionHintPending = null
  let pageActionKeyLetters = null
  let pageActionHintNodes = null
  let pageHintScrollRoots = null
  let pageHintScrollHandler = null
  let pageHintScrollResizeHandler = null
  let pageHintScrollRaf = null

  function isPageActionHintsEnabled() {
    return shortcutStore.isEnabled()
  }

  function pageActionLetter(key) {
    const map = pageActionKeyLetters || {}
    return map[key] || ''
  }

  function clearPageHintRevealTimer() {
    if (pageActionHintRevealTimer) {
      clearTimeout(pageActionHintRevealTimer)
      pageActionHintRevealTimer = null
    }
  }

  function getPageActionHintContainer() {
    const host = vm()
    if (!host) return null
    if (typeof host.getPageActionHintContainer === 'function') {
      const c = host.getPageActionHintContainer()
      if (c) return c
    }
    return host.$el
  }

  function hidePageActionHintBadges() {
    unbindPageHintScrollRefresh()
    clearPageHintOverlay()
    pageActionHintNodes = null
    if (pageHintsActive.value) pageHintsActive.value = false
  }

  function hidePageActionHints() {
    pageActionModifierHeld = false
    clearPageHintRevealTimer()
    hidePageActionHintBadges()
    pageActionHintMap = null
    pageActionHintPending = null
  }

  function unbindPageHintScrollRefresh() {
    if (pageHintScrollRaf) {
      cancelAnimationFrame(pageHintScrollRaf)
      pageHintScrollRaf = null
    }
    if (pageHintScrollRoots && pageHintScrollHandler) {
      pageHintScrollRoots.forEach((el) => {
        el.removeEventListener('scroll', pageHintScrollHandler)
      })
    }
    if (pageHintScrollResizeHandler) {
      window.removeEventListener('resize', pageHintScrollResizeHandler)
    }
    pageHintScrollRoots = null
    pageHintScrollHandler = null
    pageHintScrollResizeHandler = null
  }

  function bindPageHintScrollRefresh() {
    unbindPageHintScrollRefresh()
    const host = vm()
    const roots = host && typeof host.getPageActionHintScrollRoots === 'function'
      ? host.getPageActionHintScrollRoots()
      : []
    const bound = (roots || []).filter(Boolean)
    if (!bound.length) return
    pageHintScrollRoots = bound
    pageHintScrollHandler = () => {
      if (!pageActionModifierHeld) return
      if (pageHintScrollRaf) return
      pageHintScrollRaf = requestAnimationFrame(() => {
        pageHintScrollRaf = null
        if (pageActionModifierHeld) {
          revealPageActionBadges()
        }
      })
    }
    bound.forEach((el) => {
      el.addEventListener('scroll', pageHintScrollHandler, { passive: true })
    })
    pageHintScrollResizeHandler = pageHintScrollHandler
    window.addEventListener('resize', pageHintScrollResizeHandler, { passive: true })
  }

  function buildPageActionHints() {
    const host = vm()
    if (!host) return null
    const list = typeof host.getPageActionHints === 'function' ? host.getPageActionHints() : []
    const container = getPageActionHintContainer()
    if (!container) return null
    const map = {}
    const pending = []
    const keyLetters = {}
    list.forEach((item) => {
      if (!item) return
      if (item.visible === false) return
      if (typeof item.visible === 'function' && !item.visible()) return
      const letter = normalizeKey(item.letter)
      if (!letter || map[letter]) return
      if (PAGE_ACTION_RESERVED.has(letter)) return
      if (typeof item.run !== 'function') return
      map[letter] = { run: item.run }
      if (item.key) keyLetters[item.key] = letter
      let anchor = null
      if (typeof item.resolveAnchor === 'function') {
        anchor = item.resolveAnchor(host)
      }
      if (!anchor && item.badgeSelector) {
        anchor = container.querySelector(item.badgeSelector)
      }
      if (!anchor) return
      if (!item.skipViewportCheck && !isElementInViewport(anchor)) return
      pending.push({
        anchor,
        letter,
        floatOffset: item.floatOffset,
        key: item.key,
        getAnchorRect: item.getAnchorRect
      })
    })
    const dynamic = typeof host.getPageDynamicActionHints === 'function'
      ? host.getPageDynamicActionHints({ usedLetters: new Set(Object.keys(map)) })
      : []
    ;(dynamic || []).forEach((item) => {
      if (!item || !item.anchor || typeof item.run !== 'function') return
      const letter = normalizeKey(item.letter)
      if (!letter || map[letter]) return
      if (PAGE_ACTION_RESERVED.has(letter)) return
      if (!item.skipViewportCheck && !isElementInViewport(item.anchor)) return
      map[letter] = { run: item.run }
      if (item.key) keyLetters[item.key] = letter
      pending.push({
        anchor: item.anchor,
        letter,
        floatOffset: item.floatOffset,
        key: item.key,
        getAnchorRect: item.getAnchorRect
      })
    })
    if (!Object.keys(map).length && !pending.length) return null
    return { map, pending, keyLetters }
  }

  function preparePageActionHints() {
    const built = buildPageActionHints()
    if (!built) {
      pageActionHintMap = null
      pageActionHintPending = null
      return
    }
    pageActionHintMap = built.map
    pageActionHintPending = built.pending
    pageActionKeyLetters = built.keyLetters
  }

  function injectPageActionBadge(anchor, letter, floatOffset, hintKey, getAnchorRect) {
    if (!anchor) return null
    let rect = anchor.getBoundingClientRect()
    if (typeof getAnchorRect === 'function') {
      const pinned = getAnchorRect()
      if (pinned) rect = pinned
    } else if (floatOffset && floatOffset.useCellBounds) {
      const td = anchor.closest && anchor.closest('td')
      if (td) rect = td.getBoundingClientRect()
    } else if (floatOffset && floatOffset.anchorTextBounds) {
      const textRect = getElementTextRect(anchor)
      if (textRect) rect = textRect
    }
    const overlay = ensurePageHintOverlay()
    const badge = document.createElement('span')
    badge.className = 'cat2bug-page-kbd-hint-float defect-list-kbd-hint'
    if (hintKey && String(hintKey).startsWith('row-')) {
      badge.classList.add('defect-row-kbd-hint-float')
    }
    badge.textContent = letter
    badge.setAttribute('aria-hidden', 'true')
    if (hintKey) badge.setAttribute('data-cat2bug-hint-key', hintKey)
    const pos = buildFloatBadgeStyle(rect, floatOffset)
    badge.style.cssText = [
      'position:fixed',
      `left:${pos.left}`,
      `top:${pos.top}`,
      `transform:${pos.transform}`,
      'right:auto',
      'bottom:auto',
      'width:max-content',
      'max-width:none',
      'transform-origin:0 0',
      'z-index:99999',
      'pointer-events:none'
    ].join(';')
    overlay.appendChild(badge)
    return { badge, anchor, float: true }
  }

  function revealPageActionBadges() {
    const built = buildPageActionHints()
    hidePageActionHintBadges()
    if (!built) {
      pageActionHintMap = null
      pageActionHintPending = null
      return
    }
    pageActionHintMap = built.map
    pageActionHintPending = built.pending
    pageActionKeyLetters = built.keyLetters
    const nodes = []
    pageActionHintPending.forEach((p) => {
      if (p.anchor && p.anchor.isConnected !== false) {
        nodes.push(injectPageActionBadge(p.anchor, p.letter, p.floatOffset, p.key, p.getAnchorRect))
      }
    })
    pageActionHintNodes = nodes.filter(Boolean)
    pageHintsActive.value = pageActionHintNodes.length > 0
    if (pageHintsActive.value) {
      if (!pageHintScrollRoots) {
        bindPageHintScrollRefresh()
      }
    } else {
      unbindPageHintScrollRefresh()
    }
  }

  function isPageActionHostReady() {
    const host = vm()
    if (!host || host._inactive) return false
    const el = getPageActionHintContainer()
    if (!el || !el.isConnected) return false
    let node = el
    while (node && node !== document.body) {
      const style = window.getComputedStyle(node)
      if (style.display === 'none' || style.visibility === 'hidden') return false
      node = node.parentElement
    }
    const rect = el.getBoundingClientRect()
    return rect.width > 0 || rect.height > 0
  }

  function focusPageActionHost() {
    const root = getPageActionHintContainer()
    if (!root || typeof root.focus !== 'function') return
    if (!root.hasAttribute('tabindex')) {
      root.setAttribute('tabindex', '-1')
    }
    root.focus({ preventScroll: true })
  }

  function ensurePageActionHostFocus() {
    const root = getPageActionHintContainer()
    if (!root) return
    const active = document.activeElement
    if (active && active !== root && active !== document.body && root.contains(active)) {
      const tag = active.tagName
      const role = active.getAttribute && active.getAttribute('role')
      if (tag === 'BUTTON' || tag === 'A' || role === 'button' || role === 'link') {
        if (typeof active.blur === 'function') active.blur()
      }
    }
    focusPageActionHost()
  }

  function armPageActionModifierHints() {
    if (!isPageActionHintsEnabled()) return
    if (!isPageActionHostReady()) return
    if (shouldDeferPageActionHintsForVm(vm())) {
      hidePageActionHints()
      return
    }
    pageActionModifierHeld = true
    preparePageActionHints()
    if (!pageHintsActive.value && !pageActionHintRevealTimer) {
      pageActionHintRevealTimer = setTimeout(() => {
        pageActionHintRevealTimer = null
        if (!pageActionModifierHeld) return
        revealPageActionBadges()
      }, 180)
    }
  }

  function tryArmFromGlobalModifier() {
    if (!pageActionGlobalMetaHeld) return
    armPageActionModifierHints()
  }

  function onPageActionHostMounted() {
    const host = vm()
    if (!host || host._inactive) return
    const run = () => {
      const h = vm()
      if (!h || h._inactive) return
      if (typeof h.shouldAutoFocusPageActionHost === 'function' && h.shouldAutoFocusPageActionHost()) {
        ensurePageActionHostFocus()
      }
    }
    nextTick(() => {
      run()
      requestAnimationFrame(() => {
        const h = vm()
        if (!h || h._inactive) return
        run()
        tryArmFromGlobalModifier()
      })
      setTimeout(run, 320)
    })
  }

  function pageActionHintKeydown(e) {
    const host = vm()
    if (!host || host._inactive) return
    if (e.isComposing || !isPageActionHintsEnabled()) return
    if (!isPageActionHostReady()) return
    if ((pageActionModifierHeld || pageHintsActive.value) &&
      !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
      hidePageActionHints()
    }
    if (isShiftKeyEvent(e) && (e.metaKey || e.ctrlKey)) {
      pageActionGlobalMetaHeld = false
      pageActionModifierHeld = false
      clearPageHintRevealTimer()
      hidePageActionHints()
      return
    }
    if (isModifierKeyEvent(e)) {
      if (e.shiftKey) return
      pageActionGlobalMetaHeld = true
      armPageActionModifierHints()
      return
    }
    if ((e.metaKey || e.ctrlKey) && !e.altKey && !e.shiftKey) {
      const arrow = e.key
      if (arrow === 'ArrowUp' || arrow === 'ArrowDown' || arrow === 'ArrowLeft' || arrow === 'ArrowRight') {
        if (pageActionModifierHeld || pageHintsActive.value) {
          if (typeof host.handlePageModifierArrowScroll === 'function') {
            const handled = host.handlePageModifierArrowScroll(e)
            if (handled) {
              e.preventDefault()
              e.stopPropagation()
              if (pageActionModifierHeld) {
                nextTick(() => revealPageActionBadges())
              }
              return
            }
          }
        }
      }
    }
    if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey) return
    if (shouldDeferPageActionHintsForVm(host)) return
    preparePageActionHints()
    const letter = resolvePageActionLetter(e)
    if (!letter) return
    if (PAGE_ACTION_RESERVED.has(letter)) return
    if (pageActionHintMap && pageActionHintMap[letter]) {
      e.preventDefault()
      e.stopPropagation()
      if (/^\d$/.test(letter) && typeof e.stopImmediatePropagation === 'function') {
        e.stopImmediatePropagation()
      }
      const target = pageActionHintMap[letter]
      hidePageActionHintBadges()
      if (target && typeof target.run === 'function') {
        target.run()
      }
      nextTick(() => {
        if (pageActionModifierHeld && (e.metaKey || e.ctrlKey)) {
          revealPageActionBadges()
        }
      })
    }
  }

  function pageActionHintKeyup(e) {
    if (isModifierKeyEvent(e)) {
      pageActionGlobalMetaHeld = false
      pageActionModifierHeld = false
      clearPageHintRevealTimer()
      hidePageActionHints()
    }
  }

  function attachPageActionHintListeners() {
    if (pageActionHintListenersBound) return
    pageActionHintListenersBound = true
    ensurePageActionGlobalModifierTracker()
    onPageActionHintKeydown = (e) => pageActionHintKeydown(e)
    onPageActionHintKeyup = (e) => pageActionHintKeyup(e)
    onPageActionHintBlur = () => {
      if (isNativeFilePickerOpen()) return
      pageActionGlobalMetaHeld = false
      clearPageHintRevealTimer()
      hidePageActionHints()
    }
    document.addEventListener('keydown', onPageActionHintKeydown, true)
    window.addEventListener('keyup', onPageActionHintKeyup, true)
    window.addEventListener('blur', onPageActionHintBlur)
  }

  function detachPageActionHintListeners() {
    if (!pageActionHintListenersBound) return
    pageActionHintListenersBound = false
    clearPageHintRevealTimer()
    document.removeEventListener('keydown', onPageActionHintKeydown, true)
    window.removeEventListener('keyup', onPageActionHintKeyup, true)
    window.removeEventListener('blur', onPageActionHintBlur)
  }

  function onHostMounted() {
    attachPageActionHintListeners()
    onPageActionHostMounted()
  }

  function onHostDeactivated() {
    detachPageActionHintListeners()
    hidePageActionHints()
  }

  onMounted(onHostMounted)
  onActivated(onHostMounted)
  onDeactivated(onHostDeactivated)
  onBeforeUnmount(onHostDeactivated)

  return {
    pageHintsActive,
    isPageActionHintsEnabled,
    pageActionLetter
  }
}
