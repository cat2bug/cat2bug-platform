import { isNativeFilePickerOpen } from '@/utils/native-file-picker'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { normalizeKey } from '@/plugins/shortcut/keymap'

/**
 * 列表/页面级快捷键提示：按住 Cmd/Ctrl 在工具栏控件上显示字母徽标，再按字母触发动作。
 * 徽标通过 fixed 浮层渲染到 body，避免 Tab/表格等 stacking context 遮挡。
 *
 * 组件需实现 getPageActionHints()，返回：
 *   { key?, letter, badgeSelector, floatOffset?: { dx, dy }, run, visible? }
 */

const OVERLAY_ID = 'cat2bug-page-kbd-overlay'
/** T/M 与浏览器保留键冲突（新建标签页 / 最小化窗口），禁止用于页面字母快捷键 */
const RESERVED_LETTERS = new Set(['A', 'C', 'M', 'T', 'V', 'X', 'Z'])

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

function isVisibleEl(el) {
  return !!(el && el.offsetParent !== null)
}

function isElementInViewport(el, minPx = 4) {
  if (!isVisibleEl(el)) return false
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
  },
  activated() {
    this.$_attachPageActionHintListeners()
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
      this.$_onPageActionHintKeydown = this.$_pageActionHintKeydown.bind(this)
      this.$_onPageActionHintKeyup = this.$_pageActionHintKeyup.bind(this)
      this.$_onPageActionHintBlur = () => {
        if (isNativeFilePickerOpen()) return
        this.$_clearPageHintRevealTimer()
        this.$_hidePageActionHints()
      }
      document.addEventListener('keydown', this.$_onPageActionHintKeydown, true)
      window.addEventListener('keyup', this.$_onPageActionHintKeyup, true)
      window.addEventListener('blur', this.$_onPageActionHintBlur)
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
    $_pageActionHintKeydown(e) {
      if (e.isComposing || !this.isPageActionHintsEnabled()) return
      if ((this.$_pageActionModifierHeld || this.pageHintsActive) &&
        !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
        this.$_hidePageActionHints()
      }
      if (isModifierKeyEvent(e)) {
        if (hasBlockingUiLayer()) return
        this.$_pageActionModifierHeld = true
        this.$_preparePageActionHints()
        if (!this.pageHintsActive && !this.$_pageActionHintRevealTimer) {
          this.$_pageActionHintRevealTimer = setTimeout(() => {
            this.$_pageActionHintRevealTimer = null
            if (!this.$_pageActionModifierHeld) return
            this.$_revealPageActionBadges()
          }, 180)
        }
        return
      }
      if (!(e.metaKey || e.ctrlKey) || e.altKey) return
      if (!this.$_pageActionHintMap) {
        this.$_preparePageActionHints()
      }
      const k = e.key
      if (k && k.length === 1) {
        const letter = /^\d$/.test(k) ? k : k.toUpperCase()
        if (RESERVED_LETTERS.has(letter)) return
        if (this.$_pageActionHintMap && this.$_pageActionHintMap[letter]) {
          e.preventDefault()
          e.stopPropagation()
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
      }
    },
    $_pageActionHintKeyup(e) {
      if (isModifierKeyEvent(e)) {
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
      if (!container || !list.length) return null
      const map = {}
      const pending = []
      const keyLetters = {}
      list.forEach((item) => {
        if (!item) return
        if (item.visible === false) return
        if (typeof item.visible === 'function' && !item.visible()) return
        const letter = normalizeKey(item.letter)
        if (!letter || map[letter]) return
        if (typeof item.run !== 'function') return
        map[letter] = { run: item.run }
        if (item.key) keyLetters[item.key] = letter
        if (!item.badgeSelector) return
        const anchor = container.querySelector(item.badgeSelector)
        if (!anchor || !isElementInViewport(anchor)) return
        pending.push({ anchor, letter, floatOffset: item.floatOffset, key: item.key })
      })
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
          nodes.push(this.$_injectPageActionBadge(p.anchor, p.letter, p.floatOffset, p.key))
        }
      })
      this.$_pageActionHintNodes = nodes.filter(Boolean)
      this.pageHintsActive = this.$_pageActionHintNodes.length > 0
    },
    /** fixed 浮层徽标：锚点控件右下角，置顶不被 Tab/统计区遮挡 */
    $_injectPageActionBadge(anchor, letter, floatOffset, hintKey) {
      if (!anchor) return null
      const rect = anchor.getBoundingClientRect()
      const overlay = ensurePageHintOverlay()
      const badge = document.createElement('span')
      badge.className = 'cat2bug-page-kbd-hint-float defect-list-kbd-hint'
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
