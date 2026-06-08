/**
 * 查看报告抽屉（ViewReport）：按住 ⌘/Ctrl 显示删除 / 导出快捷键徽标；
 * Esc 关闭（dialog-form-shortcuts，不显示 Esc 徽标）；导出下拉遵循 split-dropdown-kbd（↑ 收起）；
 * 右侧工具栏 ←/→ 切换焦点。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
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

export default {
  mixins: [dialogFormShortcuts],
  data() {
    return {
      viewReportKbdHintsActive: false,
      viewReportToolbarNavIndex: -1,
      exportDropdownMenuState: createDropdownMenuKeyboardState()
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.$_attachViewReportKbdListeners()
        this.$nextTick(() => {
          this.$_bindViewReportExportDropdown()
          this.$_bindViewReportToolbarFocusIn()
        })
      } else {
        this.$_detachViewReportKbdListeners()
        this.$_unbindViewReportToolbarFocusIn()
        this.$_resetViewReportToolbarNav()
        this.$_hideViewReportKbdHints()
      }
    }
  },
  beforeDestroy() {
    this.$_detachViewReportKbdListeners()
    this.$_unbindViewReportToolbarFocusIn()
    this.$_resetViewReportToolbarNav()
    this.$_hideViewReportKbdHints()
  },
  methods: {
    getViewReportToolbarEl() {
      const root = this.getViewReportKbdDrawerRoot()
      return (root && root.querySelector('.report-edit-tools')) || null
    },
    $_resetViewReportToolbarNav() {
      this.viewReportToolbarNavIndex = -1
      this.clearViewReportToolbarFocusMarks()
    },
    clearViewReportToolbarFocusMarks() {
      const toolbar = this.getViewReportToolbarEl()
      if (!toolbar) return
      toolbar.querySelectorAll(`.${TOOLBAR_FOCUS_CLASS}`).forEach((el) => {
        el.classList.remove(TOOLBAR_FOCUS_CLASS)
      })
    },
    syncViewReportToolbarFocusClass() {
      const toolbar = this.getViewReportToolbarEl()
      if (toolbar) clearAllSplitDropdownHostFocusVisuals(toolbar)
      this.clearViewReportToolbarFocusMarks()
      if (this.viewReportToolbarNavIndex < 0) return
      const list = getToolbarFocusables(toolbar)
      const el = list[this.viewReportToolbarNavIndex]
      if (el) el.classList.add(TOOLBAR_FOCUS_CLASS)
    },
    applyViewReportToolbarFocus(index) {
      const toolbar = this.getViewReportToolbarEl()
      const list = getToolbarFocusables(toolbar)
      if (!toolbar || index < 0 || index >= list.length) return false
      this.viewReportToolbarNavIndex = index
      const { focused, el, disabled } = focusToolbarAtIndex(toolbar, index)
      if (el && el.classList.contains(SPLIT_DROPDOWN_FOCUS_CLASS)) {
        const host = resolveSplitDropdownHost(el)
        if (host) {
          bindSplitDropdownHost(host)
          if (!disabled) focusSplitDropdownHost(host, { toolbarNav: true })
        }
      }
      this.syncViewReportToolbarFocusClass()
      return focused || !!el
    },
    moveViewReportToolbar(delta) {
      const toolbar = this.getViewReportToolbarEl()
      const list = getToolbarFocusables(toolbar)
      if (!list.length) return false
      let idx = this.viewReportToolbarNavIndex
      if (idx < 0) {
        idx = getToolbarFocusableIndex(toolbar, document.activeElement)
      }
      const next = idx + delta
      if (next < 0 || next >= list.length) return false
      return this.applyViewReportToolbarFocus(next)
    },
    isFocusInViewReportToolbar(target) {
      const toolbar = this.getViewReportToolbarEl()
      if (isFocusInToolbar(toolbar, target)) return true
      if (!toolbar) return false
      const marked = toolbar.querySelector(`.${TOOLBAR_FOCUS_CLASS}`)
      return !!marked
    },
    isViewReportToolbarOverlayOpen() {
      const toolbar = this.getViewReportToolbarEl()
      if (!toolbar) return false
      const exportHost = toolbar.querySelector('.report-view-export')
      return !!(exportHost && isSplitDropdownOpen(exportHost))
    },
    $_bindViewReportToolbarFocusIn() {
      const toolbar = this.getViewReportToolbarEl()
      if (!toolbar || this.$_viewReportToolbarFocusInBound) return
      this.$_viewReportToolbarFocusInBound = true
      this.$_onViewReportToolbarFocusIn = (e) => {
        if (!this.visible || !e.target || !toolbar.contains(e.target)) return
        const idx = getToolbarFocusableIndex(toolbar, e.target)
        if (idx >= 0) {
          this.viewReportToolbarNavIndex = idx
          this.syncViewReportToolbarFocusClass()
        }
      }
      toolbar.addEventListener('focusin', this.$_onViewReportToolbarFocusIn, true)
    },
    $_unbindViewReportToolbarFocusIn() {
      if (!this.$_viewReportToolbarFocusInBound) return
      this.$_viewReportToolbarFocusInBound = false
      const toolbar = this.getViewReportToolbarEl()
      if (toolbar && this.$_onViewReportToolbarFocusIn) {
        toolbar.removeEventListener('focusin', this.$_onViewReportToolbarFocusIn, true)
      }
      this.$_onViewReportToolbarFocusIn = null
    },
    $_handleViewReportToolbarKeydown(e) {
      const toolbar = this.getViewReportToolbarEl()
      if (!toolbar) return false
      if (this.isViewReportToolbarOverlayOpen()) return false

      const inToolbar = this.isFocusInViewReportToolbar(document.activeElement)
      const list = getToolbarFocusables(toolbar)
      if (!list.length) return false

      if (inToolbar && (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar' || e.key === 'ArrowDown')) {
        if (tryOpenToolbarFocusedSplitDropdown(toolbar)) {
          e.preventDefault()
          e.stopPropagation()
          return true
        }
        const cur = list[this.viewReportToolbarNavIndex] ||
          list[getToolbarFocusableIndex(toolbar, document.activeElement)]
        if (cur && cur.classList.contains(SPLIT_DROPDOWN_FOCUS_CLASS)) {
          const host = resolveSplitDropdownHost(cur)
          if (host) {
            e.preventDefault()
            e.stopPropagation()
            focusSplitDropdownHost(host, { toolbarNav: true })
            openSplitDropdown(host, this.exportDropdownMenuState)
            return true
          }
        }
      }

      if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return false
      if (!inToolbar && this.viewReportToolbarNavIndex < 0) return false

      e.preventDefault()
      e.stopPropagation()

      let idx = this.viewReportToolbarNavIndex
      if (idx < 0) idx = getToolbarFocusableIndex(toolbar, document.activeElement)

      if (e.key === 'ArrowLeft') {
        if (idx <= 0) {
          if (idx < 0 && list.length) this.applyViewReportToolbarFocus(0)
          return true
        }
        this.moveViewReportToolbar(-1)
        return true
      }

      if (idx < 0) {
        this.applyViewReportToolbarFocus(0)
        return true
      }
      if (idx >= list.length - 1) return true
      this.moveViewReportToolbar(1)
      return true
    },
    $_bindViewReportExportDropdown() {
      const ref = this.$refs.exportDropdown
      const host = ref && ref.$el
      if (host && host.classList && host.classList.contains('el-dropdown')) {
        bindSplitDropdownHost(host)
      }
    },
    getViewReportKbdDrawerRoot() {
      const drawer = this.$refs.viewReportDrawer
      return (drawer && drawer.$el) || this.$el
    },
    $_canActivateViewReportKbdHints() {
      return this.visible && isTopDrawerVm(this)
    },
    $_attachViewReportKbdListeners() {
      if (this.$_viewReportKbdListenersBound) return
      this.$_viewReportKbdListenersBound = true
      this.$_onViewReportKbdKeydown = this.$_viewReportKbdKeydown.bind(this)
      this.$_onViewReportKbdKeyup = this.$_viewReportKbdKeyup.bind(this)
      this.$_onViewReportKbdBlur = () => {
        if (isNativeFilePickerOpen()) return
        this.$_clearViewReportKbdRevealTimer()
        this.$_hideViewReportKbdHints()
      }
      document.addEventListener('keydown', this.$_onViewReportKbdKeydown, true)
      window.addEventListener('keyup', this.$_onViewReportKbdKeyup, true)
      window.addEventListener('blur', this.$_onViewReportKbdBlur)
    },
    $_detachViewReportKbdListeners() {
      if (!this.$_viewReportKbdListenersBound) return
      this.$_viewReportKbdListenersBound = false
      this.$_clearViewReportKbdRevealTimer()
      document.removeEventListener('keydown', this.$_onViewReportKbdKeydown, true)
      window.removeEventListener('keyup', this.$_onViewReportKbdKeyup, true)
      window.removeEventListener('blur', this.$_onViewReportKbdBlur)
    },
    $_clearViewReportKbdRevealTimer() {
      if (this.$_viewReportKbdRevealTimer) {
        clearTimeout(this.$_viewReportKbdRevealTimer)
        this.$_viewReportKbdRevealTimer = null
      }
    },
    $_viewReportKbdKeydown(e) {
      if (!this.$_canActivateViewReportKbdHints() || e.isComposing) return

      if (!e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey) {
        if (this.$_handleViewReportToolbarKeydown(e)) return
      }

      if ((this.$_viewReportKbdModifierHeld || this.viewReportKbdHintsActive) &&
        !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
        this.$_hideViewReportKbdHints()
      }
      if (isModifierKeyEvent(e)) {
        if (e.shiftKey) return
        this.$_viewReportKbdModifierHeld = true
        this.$_refreshViewReportKbdHints(false)
        if (!this.viewReportKbdHintsActive && !this.$_viewReportKbdRevealTimer) {
          this.$_viewReportKbdRevealTimer = setTimeout(() => {
            this.$_viewReportKbdRevealTimer = null
            if (!this.$_viewReportKbdModifierHeld) return
            this.$_refreshViewReportKbdHints(true)
          }, 180)
        }
        return
      }
      if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey) return
      this.$_refreshViewReportKbdHints(false)
      const k = e.key
      if (!k || k.length !== 1) return
      const letter = /^\d$/.test(k) ? k : k.toUpperCase()
      if (this.$_viewReportKbdMap && this.$_viewReportKbdMap[letter]) {
        e.preventDefault()
        e.stopPropagation()
        const target = this.$_viewReportKbdMap[letter]
        this.$_hideViewReportKbdBadges()
        if (target && typeof target.run === 'function') {
          target.run()
        }
        this.$nextTick(() => {
          if (this.$_viewReportKbdModifierHeld && (e.metaKey || e.ctrlKey)) {
            this.$_refreshViewReportKbdHints(true)
          }
        })
      }
    },
    $_viewReportKbdKeyup(e) {
      if (!this.visible) return
      if (isModifierKeyEvent(e)) {
        this.$_viewReportKbdModifierHeld = false
        this.$_clearViewReportKbdRevealTimer()
        this.$_hideViewReportKbdHints()
      }
    },
    $_buildViewReportKbdHints() {
      const drawerRoot = this.getViewReportKbdDrawerRoot()
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
              this.viewReportToolbarNavIndex = getToolbarFocusableIndex(
                this.getViewReportToolbarEl(),
                item.anchor
              )
              this.syncViewReportToolbarFocusClass()
              openSplitDropdown(item.anchor, this.exportDropdownMenuState)
              return
            }
            const focusTarget = item.anchor.querySelector('button') || item.anchor
            if (focusTarget && typeof focusTarget.focus === 'function') {
              focusTarget.focus()
            }
            if (focusTarget && typeof focusTarget.click === 'function') {
              focusTarget.click()
            }
            this.$nextTick(() => {
              const idx = getToolbarFocusableIndex(this.getViewReportToolbarEl(), focusTarget)
              if (idx >= 0) {
                this.viewReportToolbarNavIndex = idx
                this.syncViewReportToolbarFocusClass()
              }
            })
          }
        }
        pending.push({ anchor: item.anchor, letter })
      })

      return pending.length ? { map, pending } : null
    },
    $_refreshViewReportKbdHints(reveal) {
      const built = this.$_buildViewReportKbdHints()
      this.$_hideViewReportKbdBadges()
      if (!built) {
        this.$_viewReportKbdMap = null
        this.$_viewReportKbdPending = null
        return
      }
      this.$_viewReportKbdMap = built.map
      this.$_viewReportKbdPending = built.pending
      if (reveal) this.$_revealViewReportKbdBadges()
    },
    $_revealViewReportKbdBadges() {
      if (!this.$_viewReportKbdPending) return
      if (this.viewReportKbdHintsActive) this.$_hideViewReportKbdBadges()
      const nodes = []
      this.$_viewReportKbdPending.forEach((p) => {
        if (p.anchor && p.anchor.isConnected !== false) {
          nodes.push(this.$_injectViewReportKbdBadge(p.anchor, p.letter, p.hintClass))
        }
      })
      this.$_viewReportKbdNodes = nodes
      this.viewReportKbdHintsActive = true
    },
    $_injectViewReportKbdBadge(anchor, letter, hintClass) {
      anchor.classList.add('cat2bug-field-hint-host')
      const badge = document.createElement('span')
      badge.className = 'cat2bug-field-hint view-report-kbd-hint' + (hintClass ? ` ${hintClass}` : '')
      badge.textContent = letter
      badge.setAttribute('aria-hidden', 'true')
      anchor.appendChild(badge)
      return { badge, host: anchor }
    },
    $_hideViewReportKbdBadges() {
      if (this.$_viewReportKbdNodes) {
        this.$_viewReportKbdNodes.forEach((entry) => {
          const badge = entry && entry.badge ? entry.badge : entry
          const host = entry && entry.host
          if (badge && badge.parentNode) badge.parentNode.removeChild(badge)
          if (host && host.classList) host.classList.remove('cat2bug-field-hint-host')
        })
      }
      this.$_viewReportKbdNodes = null
      if (this.viewReportKbdHintsActive) this.viewReportKbdHintsActive = false
    },
    $_hideViewReportKbdHints() {
      this.$_viewReportKbdModifierHeld = false
      this.$_clearViewReportKbdRevealTimer()
      this.$_hideViewReportKbdBadges()
      this.$_viewReportKbdMap = null
      this.$_viewReportKbdPending = null
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.cancel()
    }
  }
}
