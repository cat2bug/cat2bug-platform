/**
 * 布局级快捷键提示：按住 Shift+Cmd/Ctrl 在左侧菜单、顶部工具栏、折叠按钮显示字母徽标；
 * 团队/用户/国际化下拉打开后支持 ↑↓ 切换、Enter 确认、Esc 关闭。
 */
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import {
  buildLayoutNavHintDescriptors,
  openLayoutNavDropdown,
  openLayoutTeamSelect
} from '@/utils/layout-nav-hints'
import { hasActivePageRowKbdHints } from '@/utils/defect-row-kbd-hints'
import {
  handleLayoutNavDropdownKeydown,
  hasLayoutNavDropdownSession,
  startLayoutNavDropdownSession,
  endLayoutNavDropdownSession
} from '@/utils/layout-nav-dropdown-kbd'

const OVERLAY_ID = 'cat2bug-layout-nav-kbd-overlay'
/** 侧栏 width 过渡 0.28s，刷新徽标需等动画结束 */
const SIDEBAR_TRANSITION_MS = 320

function isLayoutNavModifier(e) {
  if (!e || e.altKey || e.isComposing) return false
  return (e.metaKey || e.ctrlKey) && e.shiftKey
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

/** ⇧ 按住时按减号键 e.key 为 '_'，需归一为 '-' */
function resolveLayoutHintLetter(e) {
  if (!e) return ''
  const k = e.key
  if (e.code === 'Minus' || k === '-' || k === '_' || k === '−') return '-'
  if (k === '`' || k === '~' || e.code === 'Backquote') return '`'
  if (!k || k.length !== 1) return ''
  if (/^\d$/.test(k)) return k
  return k.toUpperCase()
}

function ensureOverlay() {
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

function clearOverlay() {
  const el = document.getElementById(OVERLAY_ID)
  if (el) el.innerHTML = ''
}

function opensLayoutDropdownKbd(descriptor) {
  const item = descriptor && descriptor.navItem
  if (!item) return false
  if (item.action === 'layout:open-team-select') return true
  if (item.type === 'dropdown') {
    const key = item.bindingId && item.bindingId.replace('nav.top.', '')
    return key === 'user' || key === 'lang'
  }
  return false
}

function buildBadgeStyle(rect, floatOffset) {
  const dx = (floatOffset && floatOffset.dx) || 0
  const dy = (floatOffset && floatOffset.dy) || 0
  const outset = floatOffset && floatOffset.outset != null ? floatOffset.outset : 2
  const placement = (floatOffset && floatOffset.placement) || 'bottom-right-outset'

  if (placement === 'center-left-inset') {
    return {
      left: `${rect.left + dx + outset}px`,
      top: `${rect.top + rect.height / 2 + dy}px`,
      transform: 'translate(0, -50%)'
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
      layoutNavHintsActive: false
    }
  },
  mounted() {
    this.$_attachLayoutNavHintListeners()
    this.$_bindLayoutNavRefreshObservers()
  },
  beforeDestroy() {
    this.$_detachLayoutNavHintListeners()
    this.$_unbindLayoutNavRefreshObservers()
    endLayoutNavDropdownSession()
  },
  methods: {
    $_attachLayoutNavHintListeners() {
      if (this.$_layoutNavHintListenersBound) return
      this.$_layoutNavHintListenersBound = true
      this.$_onLayoutNavKeydown = (e) => this.$_layoutNavHintKeydown(e)
      this.$_onLayoutNavKeyup = (e) => this.$_layoutNavHintKeyup(e)
      document.addEventListener('keydown', this.$_onLayoutNavKeydown, true)
      window.addEventListener('keyup', this.$_onLayoutNavKeyup, true)
    },
    $_detachLayoutNavHintListeners() {
      if (!this.$_layoutNavHintListenersBound) return
      this.$_layoutNavHintListenersBound = false
      this.$_clearLayoutNavRevealTimer()
      this.$_hideLayoutNavHints()
      document.removeEventListener('keydown', this.$_onLayoutNavKeydown, true)
      window.removeEventListener('keyup', this.$_onLayoutNavKeyup, true)
    },
    $_clearLayoutNavRevealTimer() {
      if (this.$_layoutNavRevealTimer) {
        clearTimeout(this.$_layoutNavRevealTimer)
        this.$_layoutNavRevealTimer = null
      }
    },
    $_clearLayoutNavBadgeRefreshTimer() {
      if (this.$_layoutNavRefreshTimer) {
        clearTimeout(this.$_layoutNavRefreshTimer)
        this.$_layoutNavRefreshTimer = null
      }
    },
    $_bindLayoutNavRefreshObservers() {
      if (this.$_layoutNavRefreshBound) return
      this.$_layoutNavRefreshBound = true
      this.$_onSidebarTransitionEnd = () => {
        if (this.$_layoutNavModifierHeld || this.layoutNavHintsActive) {
          this.$_scheduleLayoutNavBadgeRefresh(0)
        }
      }
      this.$_onWindowResize = () => {
        if (this.$_layoutNavModifierHeld || this.layoutNavHintsActive) {
          this.$_scheduleLayoutNavBadgeRefresh(80)
        }
      }
      this.$_unwatchSidebarOpened = this.$watch(
        () => this.$store && this.$store.state.app.sidebar.opened,
        () => {
          if (this.$_layoutNavModifierHeld || this.layoutNavHintsActive) {
            this.$_scheduleLayoutNavBadgeRefresh()
          }
        }
      )
      this.$nextTick(() => {
        const el = document.querySelector('.sidebar-container')
        if (el) {
          this.$_layoutNavSidebarEl = el
          el.addEventListener('transitionend', this.$_onSidebarTransitionEnd)
        }
      })
      window.addEventListener('resize', this.$_onWindowResize, { passive: true })
    },
    $_unbindLayoutNavRefreshObservers() {
      if (!this.$_layoutNavRefreshBound) return
      this.$_layoutNavRefreshBound = false
      this.$_clearLayoutNavBadgeRefreshTimer()
      if (this.$_unwatchSidebarOpened) {
        this.$_unwatchSidebarOpened()
        this.$_unwatchSidebarOpened = null
      }
      if (this.$_layoutNavSidebarEl && this.$_onSidebarTransitionEnd) {
        this.$_layoutNavSidebarEl.removeEventListener('transitionend', this.$_onSidebarTransitionEnd)
      }
      this.$_layoutNavSidebarEl = null
      if (this.$_onWindowResize) {
        window.removeEventListener('resize', this.$_onWindowResize)
      }
    },
    /** 侧栏折叠/展开后重新测量锚点并绘制徽标 */
    $_scheduleLayoutNavBadgeRefresh(delay = SIDEBAR_TRANSITION_MS) {
      this.$_clearLayoutNavBadgeRefreshTimer()
      this.$_layoutNavRefreshTimer = setTimeout(() => {
        this.$_layoutNavRefreshTimer = null
        if (!this.$_layoutNavModifierHeld && !this.layoutNavHintsActive) return
        this.$_revealLayoutNavBadges()
      }, delay)
    },
    $_armLayoutNavHints() {
      this.$_layoutNavModifierHeld = true
      this.$_prepareLayoutNavHints()
      if (!this.layoutNavHintsActive && !this.$_layoutNavRevealTimer) {
        this.$_layoutNavRevealTimer = setTimeout(() => {
          this.$_layoutNavRevealTimer = null
          if (!this.$_layoutNavModifierHeld) return
          this.$_revealLayoutNavBadges()
        }, 180)
      }
    },
    $_layoutNavHintKeydown(e) {
      if (e.isComposing) return
      if (hasLayoutNavDropdownSession() && handleLayoutNavDropdownKeydown(e)) return
      if (hasBlockingUiLayer()) {
        if (this.layoutNavHintsActive || this.$_layoutNavModifierHeld) {
          this.$_hideLayoutNavHints()
        }
        return
      }

      if ((this.$_layoutNavModifierHeld || this.layoutNavHintsActive) &&
        !isLayoutNavModifier(e) && !isModifierKeyEvent(e) && !hasLayoutNavDropdownSession()) {
        this.$_hideLayoutNavHints()
      }

      if (isLayoutNavModifier(e) && (isModifierKeyEvent(e) || isShiftKeyEvent(e))) {
        this.$_armLayoutNavHints()
        return
      }

      if (!isLayoutNavModifier(e)) return
      if (!this.$_layoutNavHintMap) this.$_prepareLayoutNavHints()

      const letter = resolveLayoutHintLetter(e)
      if (letter) {
        if (/^\d$/.test(letter) && hasActivePageRowKbdHints()) return
        if (this.$_layoutNavHintMap && this.$_layoutNavHintMap[letter]) {
          e.preventDefault()
          e.stopPropagation()
          const descriptor = this.$_layoutNavHintMap[letter]
          const opensDropdown = opensLayoutDropdownKbd(descriptor)
          const togglesSidebar = descriptor.navItem &&
            descriptor.navItem.action === 'layout:toggle-sidebar'
          this.$_runLayoutNavHint(descriptor)
          this.$_hideLayoutNavHintBadges()
          if (opensDropdown) {
            this.$_layoutNavModifierHeld = false
            this.$_clearLayoutNavRevealTimer()
            return
          }
          if (togglesSidebar) {
            if (this.$_layoutNavModifierHeld || isLayoutNavModifier(e)) {
              this.$_scheduleLayoutNavBadgeRefresh()
            }
            return
          }
          this.$nextTick(() => {
            if (this.$_layoutNavModifierHeld && isLayoutNavModifier(e)) {
              this.$_revealLayoutNavBadges()
            }
          })
        }
      }
    },
    $_layoutNavHintKeyup(e) {
      if (isModifierKeyEvent(e) || !e.shiftKey) {
        this.$_layoutNavModifierHeld = false
        this.$_clearLayoutNavRevealTimer()
        if (hasLayoutNavDropdownSession()) {
          this.$_hideLayoutNavHintBadges()
          return
        }
        this.$_hideLayoutNavHints()
      }
    },
    $_prepareLayoutNavHints() {
      const built = buildLayoutNavHintDescriptors()
      if (!built) {
        this.$_layoutNavHintMap = null
        this.$_layoutNavHintPending = null
        return
      }
      this.$_layoutNavHintMap = built.map
      this.$_layoutNavHintPending = built.pending
    },
    $_revealLayoutNavBadges() {
      this.$_prepareLayoutNavHints()
      this.$_hideLayoutNavHintBadges()
      if (!this.$_layoutNavHintPending || !this.$_layoutNavHintPending.length) {
        this.layoutNavHintsActive = false
        return
      }
      const overlay = ensureOverlay()
      this.$_layoutNavHintPending.forEach((p) => {
        if (!p.anchor || !p.anchor.isConnected) return
        const rect = p.anchor.getBoundingClientRect()
        const badge = document.createElement('span')
        badge.className = 'cat2bug-page-kbd-hint-float defect-list-kbd-hint layout-nav-kbd-hint-float'
        badge.textContent = p.letter
        badge.setAttribute('aria-hidden', 'true')
        const pos = buildBadgeStyle(rect, p.floatOffset)
        badge.style.cssText = [
          'position:fixed',
          `left:${pos.left}`,
          `top:${pos.top}`,
          `transform:${pos.transform}`,
          'z-index:99999',
          'pointer-events:none'
        ].join(';')
        overlay.appendChild(badge)
      })
      this.layoutNavHintsActive = true
    },
    $_hideLayoutNavHintBadges() {
      clearOverlay()
      this.layoutNavHintsActive = false
    },
    $_hideLayoutNavHints() {
      this.$_layoutNavModifierHeld = false
      this.$_clearLayoutNavRevealTimer()
      this.$_clearLayoutNavBadgeRefreshTimer()
      this.$_hideLayoutNavHintBadges()
      this.$_layoutNavHintMap = null
      this.$_layoutNavHintPending = null
    },
    $_runLayoutNavHint(descriptor) {
      const item = descriptor && descriptor.navItem
      if (!item) return
      if (item.action === 'layout:toggle-sidebar') {
        this.$store.dispatch('app/toggleSideBar')
        return
      }
      if (item.type === 'route' && item.to) {
        if (this.$route.path !== item.to) {
          this.$router.push(item.to).catch(() => {})
        }
        return
      }
      if (item.action === 'layout:open-team-select') {
        if (openLayoutTeamSelect(descriptor.anchor)) {
          this.$nextTick(() => startLayoutNavDropdownSession('team', descriptor.anchor))
        }
        return
      }
      if (item.type === 'dropdown') {
        const key = item.bindingId && item.bindingId.replace('nav.top.', '')
        // 顶栏主题是单击切换（非 el-dropdown），⇧⌘M 直接触发锚点点击
        if (key === 'theme') {
          if (descriptor.anchor && typeof descriptor.anchor.click === 'function') {
            descriptor.anchor.click()
          }
          return
        }
        if (openLayoutNavDropdown(descriptor.anchor)) {
          if (key === 'user' || key === 'lang') {
            this.$nextTick(() => startLayoutNavDropdownSession(key, descriptor.anchor))
          }
        }
        return
      }
      if (item.type === 'action') {
        if (descriptor.anchor && typeof descriptor.anchor.click === 'function') {
          descriptor.anchor.click()
          return
        }
      }
      const getExecutor = this.$shortcut && this.$shortcut.service && this.$shortcut.service.getExecutor
      const executor = typeof getExecutor === 'function' ? getExecutor() : null
      if (typeof executor === 'function') {
        executor(item)
      }
    }
  }
}
