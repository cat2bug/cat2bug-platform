/**
 * 处理缺陷抽屉（HandleDefect）：按住 Cmd/Ctrl 显示快捷键。
 * - 右上角工具按钮：字母
 * - 风琴组标题：数字优先（视口内动态），用尽后字母
 * - Cmd/Ctrl + ↑/↓：滚动抽屉内容区
 */
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

export default {
  data() {
    return {
      handleKbdHintsActive: false
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.$_attachHandleKbdListeners()
      } else {
        this.$_detachHandleKbdListeners()
        this.$_hideHandleKbdHints()
      }
    }
  },
  beforeDestroy() {
    this.$_detachHandleKbdListeners()
    this.$_hideHandleKbdHints()
  },
  methods: {
    getHandleKbdContentRoot() {
      if (this.$refs.handleDefectBody) return this.$refs.handleDefectBody
      const drawer = this.$refs.handleDefectDrawer
      const drawerEl = drawer && drawer.$el
      if (drawerEl && typeof drawerEl.querySelector === 'function') {
        return drawerEl.querySelector('.defect-edit-body') ||
          drawerEl.querySelector('.el-drawer__body')
      }
      if (this.$el && typeof this.$el.querySelector === 'function') {
        return this.$el.querySelector('.defect-edit-body') ||
          this.$el.querySelector('.el-drawer__body')
      }
      return null
    },
    getHandleKbdDrawerRoot() {
      const drawer = this.$refs.handleDefectDrawer
      return (drawer && drawer.$el) || this.$el
    },
    getHandleKbdContainer() {
      return this.getHandleKbdContentRoot() || this.getHandleKbdDrawerRoot()
    },
    getHandleKbdScrollContainer() {
      return resolveDrawerScrollContainer(
        this.getHandleKbdContentRoot(),
        this.getHandleKbdDrawerRoot()
      )
    },
    $_canActivateHandleKbdHints() {
      return this.visible && isTopDrawerVm(this)
    },
    $_attachHandleKbdListeners() {
      if (this.$_handleKbdListenersBound) return
      this.$_handleKbdListenersBound = true
      this.$_onHandleKbdKeydown = this.$_handleKbdKeydown.bind(this)
      this.$_onHandleKbdKeyup = this.$_handleKbdKeyup.bind(this)
      this.$_onHandleKbdBlur = () => {
        if (isNativeFilePickerOpen()) return
        this.$_clearHandleKbdRevealTimer()
        this.$_hideHandleKbdHints()
      }
      document.addEventListener('keydown', this.$_onHandleKbdKeydown, true)
      window.addEventListener('keyup', this.$_onHandleKbdKeyup, true)
      window.addEventListener('blur', this.$_onHandleKbdBlur)
    },
    $_detachHandleKbdListeners() {
      if (!this.$_handleKbdListenersBound) return
      this.$_handleKbdListenersBound = false
      this.$_clearHandleKbdRevealTimer()
      this.$_detachHandleKbdScrollListener()
      document.removeEventListener('keydown', this.$_onHandleKbdKeydown, true)
      window.removeEventListener('keyup', this.$_onHandleKbdKeyup, true)
      window.removeEventListener('blur', this.$_onHandleKbdBlur)
    },
    $_clearHandleKbdRevealTimer() {
      if (this.$_handleKbdRevealTimer) {
        clearTimeout(this.$_handleKbdRevealTimer)
        this.$_handleKbdRevealTimer = null
      }
    },
    $_handleKbdKeydown(e) {
      if (!this.$_canActivateHandleKbdHints() || e.isComposing) return
      if ((this.$_handleKbdModifierHeld || this.handleKbdHintsActive) &&
        !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
        this.$_hideHandleKbdHints()
      }
      if (isModifierKeyEvent(e)) {
        this.$_handleKbdModifierHeld = true
        this.$_prepareHandleKbdHints()
        this.$_attachHandleKbdScrollListener()
        if (!this.handleKbdHintsActive && !this.$_handleKbdRevealTimer) {
          this.$_handleKbdRevealTimer = setTimeout(() => {
            this.$_handleKbdRevealTimer = null
            if (!this.$_handleKbdModifierHeld) return
            this.$_refreshHandleKbdHints(true)
          }, 180)
        }
        return
      }
      if (!(e.metaKey || e.ctrlKey) || e.altKey) return
      if (!this.$_handleKbdMap) this.$_prepareHandleKbdHints()

      const k = e.key
      if (k === 'ArrowDown' || k === 'ArrowUp') {
        if (!this.$_handleKbdModifierHeld) return
        const sc = this.getHandleKbdScrollContainer()
        if (sc) {
          e.preventDefault()
          e.stopPropagation()
          const delta = Math.max(120, Math.round(sc.clientHeight * 0.4))
          sc.scrollBy({ top: k === 'ArrowDown' ? delta : -delta, behavior: 'smooth' })
          this.$_scheduleHandleKbdRefresh()
        }
        return
      }

      if (k && k.length === 1) {
        const letter = /^\d$/.test(k) ? k : k.toUpperCase()
        if (this.$_handleKbdMap && this.$_handleKbdMap[letter]) {
          e.preventDefault()
          e.stopPropagation()
          const target = this.$_handleKbdMap[letter]
          this.$_hideHandleKbdBadges()
          if (target && typeof target.run === 'function') {
            target.run()
          }
          this.$nextTick(() => {
            if (this.$_handleKbdModifierHeld && (e.metaKey || e.ctrlKey)) {
              this.$_refreshHandleKbdHints(true)
            }
          })
        }
      }
    },
    $_handleKbdKeyup(e) {
      if (!this.visible) return
      if (isModifierKeyEvent(e)) {
        this.$_handleKbdModifierHeld = false
        this.$_clearHandleKbdRevealTimer()
        this.$_detachHandleKbdScrollListener()
        this.$_hideHandleKbdHints()
      }
    },
    $_attachHandleKbdScrollListener() {
      const sc = this.getHandleKbdScrollContainer()
      if (!sc) return
      if (this.$_handleKbdScrollEl === sc) return
      this.$_detachHandleKbdScrollListener()
      this.$_handleKbdScrollEl = sc
      this.$_onHandleKbdScroll = () => this.$_scheduleHandleKbdRefresh()
      sc.addEventListener('scroll', this.$_onHandleKbdScroll, { passive: true })
    },
    $_detachHandleKbdScrollListener() {
      this.$_clearHandleKbdRefreshRaf()
      if (this.$_handleKbdScrollEl && this.$_onHandleKbdScroll) {
        this.$_handleKbdScrollEl.removeEventListener('scroll', this.$_onHandleKbdScroll)
      }
      this.$_handleKbdScrollEl = null
      this.$_onHandleKbdScroll = null
    },
    $_clearHandleKbdRefreshRaf() {
      if (this.$_handleKbdRefreshRaf) {
        cancelAnimationFrame(this.$_handleKbdRefreshRaf)
        this.$_handleKbdRefreshRaf = null
      }
    },
    $_scheduleHandleKbdRefresh() {
      if (this.$_handleKbdRefreshRaf) return
      this.$_handleKbdRefreshRaf = requestAnimationFrame(() => {
        this.$_handleKbdRefreshRaf = null
        if (!this.$_handleKbdModifierHeld) return
        this.$_refreshHandleKbdHints(this.handleKbdHintsActive)
      })
    },
    $_prepareHandleKbdHints() {
      if (this.$_handleKbdMap) return
      const built = this.$_buildHandleKbdHints()
      if (!built) return
      this.$_handleKbdMap = built.map
      this.$_handleKbdPending = built.pending
    },
    $_refreshHandleKbdHints(reveal) {
      const built = this.$_buildHandleKbdHints()
      this.$_hideHandleKbdBadges()
      if (!built) {
        this.$_handleKbdMap = null
        this.$_handleKbdPending = null
        return
      }
      this.$_handleKbdMap = built.map
      this.$_handleKbdPending = built.pending
      if (reveal) this.$_revealHandleKbdBadges()
    },
    $_buildHandleKbdHints() {
      const container = this.getHandleKbdContainer()
      const drawerRoot = this.getHandleKbdDrawerRoot()
      if (!container && !drawerRoot) return null
      const scrollContainer = this.getHandleKbdScrollContainer()
      const viewportRect = getViewportRect(scrollContainer, container)
      const map = {}
      const pending = []
      const reserved = {}

      // 工具栏在抽屉 title 槽，不在滚动 body 内，需从 drawer 根节点查找
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
        // 标题区工具按钮固定可见，不按内容区滚动视口裁剪
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

      const collapseHeaders = Array.from(
        container.querySelectorAll('.handle-defect-collapse-title')
      ).filter((el) => isElementInViewportRect(el, viewportRect))
      const collapseKeys = assignDigitThenLetterKeys(collapseHeaders.length, reserved)
      collapseHeaders.forEach((anchor, i) => {
        const letter = collapseKeys[i]
        if (!letter) return
        const name = anchor.getAttribute('data-collapse-name')
        map[letter] = {
          run: () => this.$_activateHandleCollapse(name, anchor)
        }
        pending.push({ anchor, letter })
      })

      return Object.keys(map).length ? { map, pending } : null
    },
    $_activateHandleCollapse(name, anchor) {
      if (name) {
        if (this.activeNames.includes(name)) {
          this.activeNames = this.activeNames.filter((n) => n !== name)
        } else {
          this.activeNames = [...this.activeNames, name]
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
    },
    $_revealHandleKbdBadges() {
      if (!this.$_handleKbdPending) return
      if (this.handleKbdHintsActive) this.$_hideHandleKbdBadges()
      const nodes = []
      this.$_handleKbdPending.forEach((p) => {
        if (p.anchor && p.anchor.isConnected !== false) {
          nodes.push(this.$_injectHandleKbdBadge(p.anchor, p.letter))
        }
      })
      this.$_handleKbdNodes = nodes
      this.handleKbdHintsActive = true
    },
    $_injectHandleKbdBadge(anchor, letter) {
      anchor.classList.add('cat2bug-field-hint-host')
      const badge = document.createElement('span')
      badge.className = 'cat2bug-field-hint handle-defect-kbd-hint'
      badge.textContent = letter
      badge.setAttribute('aria-hidden', 'true')
      anchor.appendChild(badge)
      return { badge, host: anchor }
    },
    $_hideHandleKbdBadges() {
      if (this.$_handleKbdNodes) {
        this.$_handleKbdNodes.forEach((entry) => {
          const badge = entry && entry.badge ? entry.badge : entry
          const host = entry && entry.host
          if (badge && badge.parentNode) badge.parentNode.removeChild(badge)
          if (host && host.classList) host.classList.remove('cat2bug-field-hint-host')
        })
      }
      this.$_handleKbdNodes = null
      if (this.handleKbdHintsActive) this.handleKbdHintsActive = false
    },
    $_hideHandleKbdHints() {
      this.$_handleKbdModifierHeld = false
      this.$_clearHandleKbdRefreshRaf()
      this.$_clearHandleKbdRevealTimer()
      this.$_detachHandleKbdScrollListener()
      this.$_hideHandleKbdBadges()
      this.$_handleKbdMap = null
      this.$_handleKbdPending = null
    }
  }
}
