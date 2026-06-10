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

/**
 * 列表/页面级快捷键提示：按住 Cmd/Ctrl 在工具栏控件上显示字母徽标，再按字母触发动作。
 * 徽标通过 fixed 浮层渲染到 body，避免 Tab/表格等 stacking context 遮挡。
 *
 * 组件需实现 getPageActionHints()，返回：
 *   { key?, letter, badgeSelector, floatOffset?: { dx, dy }, run, visible? }
 */

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

function isVisibleEl(el) {
  return !!(el && el.offsetParent !== null)
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
  /** 徽标在文字右缘外、紧贴文字下缘（不遮挡序号） */
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
  /** 徽标在锚点单元格内水平垂直居中（用于表格行快捷键列） */
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

export default {
  data() {
    return {
      pageHintsActive: false
    }
  },
  mounted() {
    this.$_attachPageActionHintListeners()
    this.$_onPageActionHostMounted()
  },
  activated() {
    this.$_attachPageActionHintListeners()
    this.$_onPageActionHostMounted()
  },
  deactivated() {
    this.$_detachPageActionHintListeners()
    this.$_hidePageActionHints()
  },
  beforeDestroy() {
    this.$_detachPageActionHintListeners()
    this.$_hidePageActionHints()
  },
  methods: {
    isPageActionHintsEnabled() {
      return shortcutStore.isEnabled()
    },
    pageActionLetter(key) {
      const map = this.$_pageActionKeyLetters || {}
      return map[key] || ''
    },
    $_attachPageActionHintListeners() {
      if (this.$_pageActionHintListenersBound) return
      this.$_pageActionHintListenersBound = true
      ensurePageActionGlobalModifierTracker()
      this.$_onPageActionHintKeydown = this.$_pageActionHintKeydown.bind(this)
      this.$_onPageActionHintKeyup = this.$_pageActionHintKeyup.bind(this)
      this.$_onPageActionHintBlur = () => {
        if (isNativeFilePickerOpen()) return
        pageActionGlobalMetaHeld = false
        this.$_clearPageHintRevealTimer()
        this.$_hidePageActionHints()
      }
      document.addEventListener('keydown', this.$_onPageActionHintKeydown, true)
      window.addEventListener('keyup', this.$_onPageActionHintKeyup, true)
      window.addEventListener('blur', this.$_onPageActionHintBlur)
    },
    /** 页面进入后聚焦宿主并同步跨路由仍按住的 Cmd/Ctrl */
    $_onPageActionHostMounted() {
      if (this._inactive) return
      this.$nextTick(() => {
        if (this._inactive) return
        if (typeof this.shouldAutoFocusPageActionHost === 'function' && this.shouldAutoFocusPageActionHost()) {
          this.$_focusPageActionHost()
        }
        requestAnimationFrame(() => {
          if (!this._inactive) this.$_tryArmFromGlobalModifier()
        })
      })
    },
    $_isPageActionHostReady() {
      if (this._inactive) return false
      const el = this.$_getPageActionHintContainer()
      if (!el || !el.isConnected) return false
      let node = el
      while (node && node !== document.body) {
        const style = window.getComputedStyle(node)
        if (style.display === 'none' || style.visibility === 'hidden') return false
        node = node.parentElement
      }
      const rect = el.getBoundingClientRect()
      return rect.width > 0 || rect.height > 0
    },
    $_focusPageActionHost() {
      const root = this.$_getPageActionHintContainer()
      if (!root || typeof root.focus !== 'function') return
      if (!root.hasAttribute('tabindex')) {
        root.setAttribute('tabindex', '-1')
      }
      root.focus({ preventScroll: true })
    },
    $_armPageActionModifierHints() {
      if (!this.isPageActionHintsEnabled()) return
      if (!this.$_isPageActionHostReady()) return
      if (shouldDeferPageActionHintsForVm(this)) {
        this.$_hidePageActionHints()
        return
      }
      this.$_pageActionModifierHeld = true
      this.$_preparePageActionHints()
      if (!this.pageHintsActive && !this.$_pageActionHintRevealTimer) {
        this.$_pageActionHintRevealTimer = setTimeout(() => {
          this.$_pageActionHintRevealTimer = null
          if (!this.$_pageActionModifierHeld) return
          this.$_revealPageActionBadges()
        }, 180)
      }
    },
    $_tryArmFromGlobalModifier() {
      if (!pageActionGlobalMetaHeld) return
      this.$_armPageActionModifierHints()
    },
    $_detachPageActionHintListeners() {
      if (!this.$_pageActionHintListenersBound) return
      this.$_pageActionHintListenersBound = false
      this.$_clearPageHintRevealTimer()
      document.removeEventListener('keydown', this.$_onPageActionHintKeydown, true)
      window.removeEventListener('keyup', this.$_onPageActionHintKeyup, true)
      window.removeEventListener('blur', this.$_onPageActionHintBlur)
    },
    $_clearPageHintRevealTimer() {
      if (this.$_pageActionHintRevealTimer) {
        clearTimeout(this.$_pageActionHintRevealTimer)
        this.$_pageActionHintRevealTimer = null
      }
    },
    $_bindPageHintScrollRefresh() {
      this.$_unbindPageHintScrollRefresh()
      const roots = typeof this.getPageActionHintScrollRoots === 'function'
        ? this.getPageActionHintScrollRoots()
        : []
      const bound = (roots || []).filter(Boolean)
      if (!bound.length) return
      this.$_pageHintScrollRoots = bound
      this.$_pageHintScrollHandler = () => {
        if (!this.$_pageActionModifierHeld) return
        if (this.$_pageHintScrollRaf) return
        this.$_pageHintScrollRaf = requestAnimationFrame(() => {
          this.$_pageHintScrollRaf = null
          if (this.$_pageActionModifierHeld) {
            this.$_revealPageActionBadges()
          }
        })
      }
      bound.forEach((el) => {
        el.addEventListener('scroll', this.$_pageHintScrollHandler, { passive: true })
      })
      this.$_pageHintScrollResizeHandler = this.$_pageHintScrollHandler
      window.addEventListener('resize', this.$_pageHintScrollResizeHandler, { passive: true })
    },
    $_unbindPageHintScrollRefresh() {
      if (this.$_pageHintScrollRaf) {
        cancelAnimationFrame(this.$_pageHintScrollRaf)
        this.$_pageHintScrollRaf = null
      }
      if (this.$_pageHintScrollRoots && this.$_pageHintScrollHandler) {
        this.$_pageHintScrollRoots.forEach((el) => {
          el.removeEventListener('scroll', this.$_pageHintScrollHandler)
        })
      }
      if (this.$_pageHintScrollResizeHandler) {
        window.removeEventListener('resize', this.$_pageHintScrollResizeHandler)
      }
      this.$_pageHintScrollRoots = null
      this.$_pageHintScrollHandler = null
      this.$_pageHintScrollResizeHandler = null
    },
    $_pageActionHintKeydown(e) {
      if (this._inactive) return
      if (e.isComposing || !this.isPageActionHintsEnabled()) return
      if (!this.$_isPageActionHostReady()) return
      if ((this.$_pageActionModifierHeld || this.pageHintsActive) &&
        !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
        this.$_hidePageActionHints()
      }
      if (isShiftKeyEvent(e) && (e.metaKey || e.ctrlKey)) {
        pageActionGlobalMetaHeld = false
        this.$_pageActionModifierHeld = false
        this.$_clearPageHintRevealTimer()
        this.$_hidePageActionHints()
        return
      }
      if (isModifierKeyEvent(e)) {
        if (e.shiftKey) return
        pageActionGlobalMetaHeld = true
        this.$_armPageActionModifierHints()
        return
      }
      if ((e.metaKey || e.ctrlKey) && !e.altKey && !e.shiftKey) {
        const arrow = e.key
        if (arrow === 'ArrowUp' || arrow === 'ArrowDown' || arrow === 'ArrowLeft' || arrow === 'ArrowRight') {
          if (this.$_pageActionModifierHeld || this.pageHintsActive) {
            if (typeof this.handlePageModifierArrowScroll === 'function') {
              const handled = this.handlePageModifierArrowScroll(e)
              if (handled) {
                e.preventDefault()
                e.stopPropagation()
                if (this.$_pageActionModifierHeld) {
                  this.$nextTick(() => this.$_revealPageActionBadges())
                }
                return
              }
            }
          }
        }
      }
      if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey) return
      if (shouldDeferPageActionHintsForVm(this)) return
      this.$_preparePageActionHints()
      const letter = resolvePageActionLetter(e)
      if (!letter) return
      if (PAGE_ACTION_RESERVED.has(letter)) return
      if (this.$_pageActionHintMap && this.$_pageActionHintMap[letter]) {
        e.preventDefault()
        e.stopPropagation()
        if (/^\d$/.test(letter) && typeof e.stopImmediatePropagation === 'function') {
          e.stopImmediatePropagation()
        }
        const target = this.$_pageActionHintMap[letter]
        this.$_hidePageActionHintBadges()
        if (target && typeof target.run === 'function') {
          target.run()
        }
        this.$nextTick(() => {
          if (this.$_pageActionModifierHeld && (e.metaKey || e.ctrlKey)) {
            this.$_revealPageActionBadges()
          }
        })
      }
    },
    $_pageActionHintKeyup(e) {
      if (isModifierKeyEvent(e)) {
        pageActionGlobalMetaHeld = false
        this.$_pageActionModifierHeld = false
        this.$_clearPageHintRevealTimer()
        this.$_hidePageActionHints()
      }
    },
    $_getPageActionHintContainer() {
      if (typeof this.getPageActionHintContainer === 'function') {
        const c = this.getPageActionHintContainer()
        if (c) return c
      }
      return this.$el
    },
    $_buildPageActionHints() {
      const list = typeof this.getPageActionHints === 'function' ? this.getPageActionHints() : []
      const container = this.$_getPageActionHintContainer()
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
          anchor = item.resolveAnchor(this)
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
      const dynamic = typeof this.getPageDynamicActionHints === 'function'
        ? this.getPageDynamicActionHints({ usedLetters: new Set(Object.keys(map)) })
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
    },
    $_preparePageActionHints() {
      const built = this.$_buildPageActionHints()
      if (!built) {
        this.$_pageActionHintMap = null
        this.$_pageActionHintPending = null
        return
      }
      this.$_pageActionHintMap = built.map
      this.$_pageActionHintPending = built.pending
      this.$_pageActionKeyLetters = built.keyLetters
    },
    $_revealPageActionBadges() {
      const built = this.$_buildPageActionHints()
      this.$_hidePageActionHintBadges()
      if (!built) {
        this.$_pageActionHintMap = null
        this.$_pageActionHintPending = null
        return
      }
      this.$_pageActionHintMap = built.map
      this.$_pageActionHintPending = built.pending
      this.$_pageActionKeyLetters = built.keyLetters
      const nodes = []
      this.$_pageActionHintPending.forEach((p) => {
        if (p.anchor && p.anchor.isConnected !== false) {
          nodes.push(this.$_injectPageActionBadge(p.anchor, p.letter, p.floatOffset, p.key, p.getAnchorRect))
        }
      })
      this.$_pageActionHintNodes = nodes.filter(Boolean)
      this.pageHintsActive = this.$_pageActionHintNodes.length > 0
      if (this.pageHintsActive) {
        if (!this.$_pageHintScrollRoots) {
          this.$_bindPageHintScrollRefresh()
        }
      } else {
        this.$_unbindPageHintScrollRefresh()
      }
    },
    /** fixed 浮层徽标：锚点控件右下角，置顶不被 Tab/统计区遮挡 */
    $_injectPageActionBadge(anchor, letter, floatOffset, hintKey, getAnchorRect) {
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
    },
    $_hidePageActionHintBadges() {
      this.$_unbindPageHintScrollRefresh()
      clearPageHintOverlay()
      this.$_pageActionHintNodes = null
      if (this.pageHintsActive) this.pageHintsActive = false
    },
    $_hidePageActionHints() {
      this.$_pageActionModifierHeld = false
      this.$_clearPageHintRevealTimer()
      this.$_hidePageActionHintBadges()
      this.$_pageActionHintMap = null
      this.$_pageActionHintPending = null
    }
  }
}
