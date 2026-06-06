import { isNativeFilePickerOpen } from '@/utils/native-file-picker'
import { dismissComboPopoverIfLeaving } from '@/utils/combo-focus-tab'

/**
 * 表单字段快捷聚焦 mixin（弹框/抽屉内）。
 *
 * 交互：
 *   - 默认不显示任何快捷键提示。
 *   - 按住 Ctrl（Windows/Linux）或 Command（Mac）→ 在各字段标签（左列）或内容区（右列）
 *     宿主右下角浮现字母徽标（绝对定位、不占排版），同时显示保存/关闭等动作提示。
 *   - 保持修饰键按下，再按对应字母 → 焦点跳转到该字段；松开修饰键 → 提示消失。
 *
 * 说明：
 *   - 仅对当前可视区域内的表单项分配快捷键（滚动后随视口刷新徽标与映射）。
 *   - A/C/V/X/Z 永不分配给字段，始终保留给全选/复制/粘贴/剪切/撤销。
 *   - 字母池约 24 个（其余字母 + 数字），仅映射命中时 intercept。
 *   - 字段切换：默认 Tab 正向；按住 ↑ 再 Tab 反向；按住 ↓ 再 Tab 正向（见 tab-direction 插件）。
 *
 * 组件要求：具备响应式 `visible`；可实现 `getFieldHintContainer()` 返回扫描容器（默认 this.$el）。
 */

// 不可映射：浏览器/OS 保留（N/T/W/Q/R 窗口与刷新，L Safari 地址栏，H Mac 隐藏应用）
// A/C/V/X/Z 永不映射，保留系统全选/复制/粘贴/剪切/撤销
const FIELD_HINT_KEYS_BLOCKED = new Set([
  'N', 'T', 'W', 'Q', 'R', 'L', 'H',
  'A', 'C', 'V', 'X', 'Z',
  'B' // 保留给 Cmd/Ctrl+B 关闭抽屉
])

/** 字母优先，用尽后使用数字（映射命中时会 preventDefault，避免误触 Cmd+1 切标签） */
const FIELD_HINT_KEYS_PREFERRED = [
  'D', 'F', 'G', 'S', 'E', 'M', 'J', 'K', 'O', 'P', 'U', 'Y', 'I'
]

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}


function isCloseShortcutKey(e) {
  if (!e || e.altKey) return false
  if (!(e.metaKey || e.ctrlKey)) return false
  const k = e.key
  return k === 'B' || k === 'b'
}

function buildFieldHintKeyPool(reservedLetters) {
  const reserved = reservedLetters || {}
  const pool = []
  const used = new Set()
  FIELD_HINT_KEYS_PREFERRED.forEach((k) => {
    if (reserved[k] || FIELD_HINT_KEYS_BLOCKED.has(k) || used.has(k)) return
    pool.push(k)
    used.add(k)
  })
  for (let c = 65; c <= 90; c++) {
    const k = String.fromCharCode(c)
    if (reserved[k] || FIELD_HINT_KEYS_BLOCKED.has(k) || used.has(k)) continue
    pool.push(k)
    used.add(k)
  }
  for (let d = 1; d <= 9; d++) {
    const k = String(d)
    if (reserved[k] || used.has(k)) continue
    pool.push(k)
    used.add(k)
  }
  if (!reserved['0'] && !used.has('0')) pool.push('0')
  return pool
}

function isVisibleEl(el) {
  return !!(el && el.offsetParent !== null)
}

function getFieldHintViewportRect(scrollContainer, fallbackEl) {
  if (scrollContainer) return scrollContainer.getBoundingClientRect()
  if (fallbackEl) return fallbackEl.getBoundingClientRect()
  return {
    top: 0,
    left: 0,
    bottom: window.innerHeight,
    right: window.innerWidth
  }
}

/** 元素与视口矩形是否有足够垂直可见区域（用于滚动表单内分配快捷键） */
function isElementInViewportRect(el, viewportRect, minVisiblePx = 8) {
  if (!el || !isVisibleEl(el)) return false
  const rect = el.getBoundingClientRect()
  if (rect.height <= 0 && rect.width <= 0) return false
  const overlapTop = Math.max(rect.top, viewportRect.top)
  const overlapBottom = Math.min(rect.bottom, viewportRect.bottom)
  return overlapBottom - overlapTop >= minVisiblePx
}

/** 取某表单项内首个可聚焦控件（优先真实输入框，跳过隐藏的 popover 包裹） */
function pickControl(item) {
  const content = item.querySelector('.el-form-item__content') || item
  // 成员/交付物选择器：聚焦外框（自定义组合组件），避免内部 input 出现独立焦点环
  const comboOuter =
    content.querySelector('.select-project-member-input') ||
    content.querySelector('.select-module-input')
  if (comboOuter && isVisibleEl(comboOuter)) {
    return comboOuter
  }
  // 文件/图片上传：聚焦外框（Tab 单停靠点；图片区默认高亮添加按钮）
  const uploadShell = content.querySelector('.cat2bug-upload-focus-target')
  if (uploadShell && isVisibleEl(uploadShell)) {
    return uploadShell
  }
  // 开关：聚焦内部 input（单一 Tab 停靠点；←/→ 见 switch-keyboard 插件）
  const sw = content.querySelector('.el-switch')
  if (sw && isVisibleEl(sw)) {
    return sw.querySelector('.el-switch__input') || sw
  }
  // 按优先级分组查找首个可见控件
  const groups = [
    'textarea, input:not([type="hidden"])',
    'button:not([disabled])',
    '[contenteditable="true"]',
    '[tabindex]:not([tabindex="-1"])'
  ]
  for (const sel of groups) {
    const list = content.querySelectorAll(sel)
    for (const el of list) {
      if (!isVisibleEl(el) || el.closest('[aria-hidden="true"]')) continue
      if (
        el.closest('.select-project-member-input') ||
        el.closest('.select-module-input') ||
        el.closest('.cat2bug-upload-focus-target') ||
        el.closest('.el-switch')
      ) continue
      return el
    }
  }
  // 兜底：内容区首个可见元素（如自定义下拉触发器）
  const kids = content.querySelectorAll('*')
  for (const el of kids) {
    if (isVisibleEl(el)) return el
  }
  return null
}

export default {
  data() {
    return {
      fieldHintsActive: false
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.$_attachFieldHintListeners()
      } else {
        this.$_detachFieldHintListeners()
        this.$_hideFieldHints()
      }
    }
  },
  beforeDestroy() {
    this.$_detachFieldHintListeners()
    this.$_hideFieldHints()
  },
  methods: {
    $_attachFieldHintListeners() {
      if (this.$_fieldHintListenersBound) return
      this.$_fieldHintListenersBound = true
      this.$_onFieldHintKeydown = this.$_fieldHintKeydown.bind(this)
      this.$_onFieldHintKeyup = this.$_fieldHintKeyup.bind(this)
      this.$_onFieldHintBlur = () => {
        if (isNativeFilePickerOpen()) return
        this.$_clearRevealTimer()
        this.$_hideFieldHints()
      }
      document.addEventListener('keydown', this.$_onFieldHintKeydown, true)
      window.addEventListener('keyup', this.$_onFieldHintKeyup, true)
      window.addEventListener('blur', this.$_onFieldHintBlur)
    },
    $_detachFieldHintListeners() {
      if (!this.$_fieldHintListenersBound) return
      this.$_fieldHintListenersBound = false
      this.$_clearRevealTimer()
      this.$_detachViewportScrollListener()
      document.removeEventListener('keydown', this.$_onFieldHintKeydown, true)
      window.removeEventListener('keyup', this.$_onFieldHintKeyup, true)
      window.removeEventListener('blur', this.$_onFieldHintBlur)
    },
    $_clearRevealTimer() {
      if (this.$_fieldHintRevealTimer) {
        clearTimeout(this.$_fieldHintRevealTimer)
        this.$_fieldHintRevealTimer = null
      }
    },
    $_fieldHintKeydown(e) {
      if (!this.visible || e.isComposing) return
      if (typeof this.$_ownsTopFormDrawer === 'function' && !this.$_ownsTopFormDrawer()) return
      if ((this.$_modifierHeld || this.fieldHintsActive) &&
        !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
        this.$_hideFieldHints()
      }
      const k = e.key
      if (isCloseShortcutKey(e)) {
        if (typeof this.$_invokeDrawerShortcutClose === 'function' &&
          this.$_invokeDrawerShortcutClose(e)) {
          return
        }
      }
      if (isModifierKeyEvent(e)) {
        this.$_modifierHeld = true
        this.$_modifierArmedAt = Date.now()
        this.$_prepareFieldHints()
        this.$_attachViewportScrollListener()
        if (!this.fieldHintsActive && !this.$_fieldHintRevealTimer) {
          this.$_fieldHintRevealTimer = setTimeout(() => {
            this.$_fieldHintRevealTimer = null
            if (!this.$_modifierHeld) return
            this.$_refreshFieldHintsForViewport(true)
          }, 180)
        }
        return
      }
      if (!(e.metaKey || e.ctrlKey) || e.altKey) return
      if (!this.$_fieldHintMap) {
        this.$_prepareFieldHints()
      }
      // 修饰键 + 上/下：滚动属性区滚动条
      if (k === 'ArrowDown' || k === 'ArrowUp') {
        if (this.$_fieldHintMap) {
          const sc = this.$_getScrollContainer()
          if (sc) {
            e.preventDefault()
            e.stopPropagation()
            const delta = Math.max(120, Math.round(sc.clientHeight * 0.4))
            sc.scrollBy({ top: k === 'ArrowDown' ? delta : -delta, behavior: 'smooth' })
            this.$_scheduleViewportHintRefresh()
            return
          }
        }
      }
      // 修饰键 + 字母：直达字段 / 触发固定动作（映射已在修饰键按下时同步构建）
      if (k && k.length === 1) {
        const letter = /^\d$/.test(k) ? k : k.toUpperCase()
        // 系统保留组合（全选/复制/粘贴等）永不拦截
        if (FIELD_HINT_KEYS_BLOCKED.has(letter)) return
        if (this.$_fieldHintMap && this.$_fieldHintMap[letter]) {
          e.preventDefault()
          e.stopPropagation()
          const target = this.$_fieldHintMap[letter]
          this.$_hideFieldHintBadges()
          if (target && typeof target.run === 'function') {
            target.run()
          } else if (target && target.el) {
            this.$_focusControl(target.el)
          }
          this.$nextTick(() => {
            if (this.$_modifierHeld && (e.metaKey || e.ctrlKey)) {
              this.$_refreshFieldHintsForViewport(true)
            }
          })
        }
        // 非映射字母（如 Cmd+C 复制）：放行浏览器默认行为
      }
    },
    $_fieldHintKeyup(e) {
      if (!this.visible) return
      if (isModifierKeyEvent(e)) {
        this.$_modifierHeld = false
        this.$_clearRevealTimer()
        this.$_detachViewportScrollListener()
        this.$_hideFieldHints()
      }
    },
    $_attachViewportScrollListener() {
      const sc = this.$_getScrollContainer()
      if (!sc) return
      if (this.$_fieldHintScrollEl === sc) return
      this.$_detachViewportScrollListener()
      this.$_fieldHintScrollEl = sc
      this.$_onFieldHintScroll = () => this.$_scheduleViewportHintRefresh()
      sc.addEventListener('scroll', this.$_onFieldHintScroll, { passive: true })
    },
    $_detachViewportScrollListener() {
      this.$_clearViewportHintRefresh()
      if (this.$_fieldHintScrollEl && this.$_onFieldHintScroll) {
        this.$_fieldHintScrollEl.removeEventListener('scroll', this.$_onFieldHintScroll)
      }
      this.$_fieldHintScrollEl = null
      this.$_onFieldHintScroll = null
    },
    $_clearViewportHintRefresh() {
      if (this.$_fieldHintScrollRefreshRaf) {
        cancelAnimationFrame(this.$_fieldHintScrollRefreshRaf)
        this.$_fieldHintScrollRefreshRaf = null
      }
    },
    /** 滚动或聚焦后按当前视口重建映射；reveal 为 true 时同步刷新徽标 */
    $_scheduleViewportHintRefresh(reveal) {
      if (this.$_fieldHintScrollRefreshRaf) return
      this.$_fieldHintScrollRefreshRaf = requestAnimationFrame(() => {
        this.$_fieldHintScrollRefreshRaf = null
        if (!this.visible || !this.$_modifierHeld ||
          !(this.$_fieldHintMap || this.$_fieldHintRevealTimer || this.fieldHintsActive)) {
          return
        }
        this.$_refreshFieldHintsForViewport(reveal || this.fieldHintsActive)
      })
    },
    $_refreshFieldHintsForViewport(reveal) {
      const built = this.$_buildFieldHints()
      this.$_hideFieldHintBadges()
      if (!built) {
        this.$_fieldHintMap = null
        this.$_fieldHintPending = null
        return
      }
      this.$_fieldHintMap = built.map
      this.$_fieldHintPending = built.pending
      if (reveal) this.$_revealBadges()
    },
    /** 同步构建字母→目标映射与待注入徽标清单（不操作 DOM 显示） */
    $_prepareFieldHints() {
      if (this.$_fieldHintMap) return
      const built = this.$_buildFieldHints()
      if (!built) return
      this.$_fieldHintMap = built.map
      this.$_fieldHintPending = built.pending
    },
    /** 按 DOM 顺序扫描表单项，仅对当前视口内可见项分配快捷键 */
    $_buildFieldHints() {
      const container =
        (typeof this.getFieldHintContainer === 'function' && this.getFieldHintContainer()) || this.$el
      if (!container) return null
      const scrollContainer = this.$_getScrollContainer()
      const viewportRect = getFieldHintViewportRect(scrollContainer, container)
      const map = {}
      const pending = []
      const reserved = {}

      // 1) 组件声明的固定快捷键（如复选框等无 label 的控件）
      const fixed =
        (typeof this.getFixedFieldHints === 'function' && this.getFixedFieldHints()) || []
      fixed.forEach((f) => {
        if (!f || !f.letter) return
        const letter = String(f.letter).toUpperCase()
        const focusEl = f.focusSelector ? container.querySelector(f.focusSelector) : null
        const anchor = f.badgeSelector ? container.querySelector(f.badgeSelector) : focusEl
        const badgeHost = f.injectBadge !== false ? anchor : null
        if (badgeHost && !isElementInViewportRect(badgeHost, viewportRect)) return
        if (f.injectBadge !== false && (!anchor || !isVisibleEl(anchor))) return
        if (typeof f.onActivate === 'function') {
          map[letter] = { run: f.onActivate }
        } else if (focusEl) {
          map[letter] = { el: focusEl }
        } else {
          return
        }
        reserved[letter] = true
        if (f.injectBadge !== false) {
          pending.push({ anchor, letter })
        }
      })

      // 2) 视口内表单字段自动分配（跳过已被固定占用的字母）
      const pool = buildFieldHintKeyPool(reserved)
      const items = Array.from(container.querySelectorAll('.el-form-item'))
      let idx = 0
      for (const item of items) {
        if (idx >= pool.length) break
        if (!isElementInViewportRect(item, viewportRect)) continue
        const label = item.querySelector('.el-form-item__label')
        const text = label && label.textContent && label.textContent.trim()
        if (!label || !text) continue
        const ctrl = pickControl(item)
        if (!ctrl) continue
        const letter = pool[idx++]
        map[letter] = { el: ctrl }
        pending.push({ anchor: label, letter })
      }

      return { map, pending }
    },
    /** 真正注入徽标 DOM 并标记激活（延迟调用，避免闪烁） */
    $_revealBadges() {
      if (!this.$_fieldHintPending) return
      if (this.fieldHintsActive) this.$_hideFieldHintBadges()
      const nodes = []
      this.$_fieldHintPending.forEach((p) => {
        if (p.anchor && p.anchor.isConnected !== false) {
          nodes.push(this.$_injectBadge(p.anchor, p.letter))
        }
      })
      this.$_fieldHintNodes = nodes
      this.fieldHintsActive = true
    },
    /** 查找属性区可滚动容器（默认向上找最近的可滚动祖先，如抽屉 body） */
    $_getScrollContainer() {
      if (typeof this.getFieldHintScrollContainer === 'function') {
        const c = this.getFieldHintScrollContainer()
        if (c) return c
      }
      let el = (typeof this.getFieldHintContainer === 'function' && this.getFieldHintContainer()) || this.$el
      while (el && el !== document.body) {
        const oy = window.getComputedStyle(el).overflowY
        if ((oy === 'auto' || oy === 'scroll') && el.scrollHeight > el.clientHeight + 2) return el
        el = el.parentElement
      }
      return null
    },
    /** 徽标浮在宿主右下角，不挤占标签/按钮排版 */
    $_injectBadge(anchor, letter) {
      anchor.classList.add('cat2bug-field-hint-host')
      const badge = document.createElement('span')
      badge.className = 'cat2bug-field-hint'
      badge.textContent = letter
      badge.setAttribute('aria-hidden', 'true')
      anchor.appendChild(badge)
      return { badge, host: anchor }
    },
    /** 仅移除徽标 DOM；保留映射以便按住 Cmd 时连续跳转字段 */
    $_hideFieldHintBadges() {
      if (this.$_fieldHintNodes) {
        this.$_fieldHintNodes.forEach((entry) => {
          const badge = entry && entry.badge ? entry.badge : entry
          const host = entry && entry.host
          if (badge && badge.parentNode) badge.parentNode.removeChild(badge)
          if (host && host.classList) host.classList.remove('cat2bug-field-hint-host')
        })
      }
      this.$_fieldHintNodes = null
      if (this.fieldHintsActive) this.fieldHintsActive = false
    },
    $_hideFieldHints() {
      this.$_modifierHeld = false
      this.$_clearViewportHintRefresh()
      this.$_clearRevealTimer()
      this.$_detachViewportScrollListener()
      this.$_hideFieldHintBadges()
      this.$_fieldHintMap = null
      this.$_fieldHintPending = null
    },
    $_focusControl(el) {
      if (!el) return
      dismissComboPopoverIfLeaving(document.activeElement, el)
      const nativelyFocusable =
        ['INPUT', 'TEXTAREA', 'SELECT', 'BUTTON', 'A'].indexOf(el.tagName) !== -1 ||
        el.hasAttribute('tabindex') ||
        el.isContentEditable
      if (!nativelyFocusable) {
        el.setAttribute('tabindex', '-1')
      }
      try {
        el.focus({ preventScroll: false })
      } catch (err) {
        if (typeof el.focus === 'function') el.focus()
      }
      if (typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ block: 'center', behavior: 'smooth' })
      }
      this.$_flashField(el)
    },
    /** 聚焦后在所属字段行上闪烁高亮，确保开关/下拉等无明显焦点样式的控件也可见 */
    $_flashField(el) {
      const target = (el.closest && el.closest('.el-form-item')) || el
      if (!target || !target.classList) return
      target.classList.remove('cat2bug-field-flash')
      // 强制重排以重启动画
      void target.offsetWidth
      target.classList.add('cat2bug-field-flash')
      if (target.__cat2bugFlashTimer) clearTimeout(target.__cat2bugFlashTimer)
      target.__cat2bugFlashTimer = setTimeout(() => {
        target.classList.remove('cat2bug-field-flash')
        target.__cat2bugFlashTimer = null
      }, 1200)
    }
  }
}
