import { getCurrentInstance, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { isNativeFilePickerOpen } from '@/utils/native-file-picker'
import { dismissComboPopoverIfLeaving } from '@/utils/combo-focus-tab'
import { releaseControlFocusBeforeJump, syncFieldFocusVisual } from '@/utils/dropdown-blur-close'
import { FIELD_HINT_BLOCKED } from '@/plugins/shortcut/reserved-keys'
import { scrollVerticalContainerByArrow } from '@/utils/scroll-vertical-by-arrow'

/**
 * 表单字段快捷聚焦 composable（弹框/抽屉内）。
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


function buildFieldHintKeyPool(reservedLetters) {
  const reserved = reservedLetters || {}
  const pool = []
  const used = new Set()
  FIELD_HINT_KEYS_PREFERRED.forEach((k) => {
    if (reserved[k] || FIELD_HINT_BLOCKED.has(k) || used.has(k)) return
    pool.push(k)
    used.add(k)
  })
  for (let c = 65; c <= 90; c++) {
    const k = String.fromCharCode(c)
    if (reserved[k] || FIELD_HINT_BLOCKED.has(k) || used.has(k)) continue
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

export function useFormFieldHints() {
  const instance = getCurrentInstance()
  const vm = () => instance && instance.proxy

  const fieldHintsActive = ref(false)
  const $_modifierHeld = ref(false)
  const $_fieldHintMap = shallowRef(null)

  let $_fieldHintListenersBound = false
  let $_onFieldHintKeydown = null
  let $_onFieldHintKeyup = null
  let $_onFieldHintBlur = null
  let $_fieldHintRevealTimer = null
  let $_fieldHintPending = null
  let $_fieldHintNodes = null
  let $_fieldHintScrollEl = null
  let $_onFieldHintScroll = null
  let $_fieldHintScrollRefreshRaf = null
  let $_modifierArmedAt = null

  const $_fieldHintSurfaceVisible = () => {
    const host = vm()
    return !!host && (host.visible === true || host.dialogVisible === true)
  }

  const $_nextTick = (cb) => {
    const host = vm()
    if (host && typeof host.$nextTick === 'function') {
      return host.$nextTick(cb)
    }
    return Promise.resolve().then(() => {
      if (typeof cb === 'function') cb()
    })
  }

  const $_syncFieldHintListeners = (open) => {
    if (open) {
      $_attachFieldHintListeners()
    } else if (!$_fieldHintSurfaceVisible()) {
      $_detachFieldHintListeners()
      $_hideFieldHints()
    }
  }

  const $_attachFieldHintListeners = () => {
    if ($_fieldHintListenersBound) return
    $_fieldHintListenersBound = true
    $_onFieldHintKeydown = $_fieldHintKeydown.bind(null)
    $_onFieldHintKeyup = $_fieldHintKeyup.bind(null)
    $_onFieldHintBlur = () => {
      if (isNativeFilePickerOpen()) return
      $_clearRevealTimer()
      $_hideFieldHints()
    }
    document.addEventListener('keydown', $_onFieldHintKeydown, true)
    window.addEventListener('keyup', $_onFieldHintKeyup, true)
    window.addEventListener('blur', $_onFieldHintBlur)
  }

  const $_detachFieldHintListeners = () => {
    if (!$_fieldHintListenersBound) return
    $_fieldHintListenersBound = false
    $_clearRevealTimer()
    $_detachViewportScrollListener()
    document.removeEventListener('keydown', $_onFieldHintKeydown, true)
    window.removeEventListener('keyup', $_onFieldHintKeyup, true)
    window.removeEventListener('blur', $_onFieldHintBlur)
  }

  const $_clearRevealTimer = () => {
    if ($_fieldHintRevealTimer) {
      clearTimeout($_fieldHintRevealTimer)
      $_fieldHintRevealTimer = null
    }
  }

  const $_fieldHintKeydown = (e) => {
    if (!$_fieldHintSurfaceVisible() || e.isComposing) return
    const host = vm()
    if (host && typeof host.$_ownsTopFormDrawer === 'function' && !host.$_ownsTopFormDrawer()) return
    if (($_modifierHeld.value || fieldHintsActive.value) &&
      !isModifierKeyEvent(e) && !(e.metaKey || e.ctrlKey)) {
      $_hideFieldHints()
    }
    const k = e.key
    if (isModifierKeyEvent(e)) {
      if (e.shiftKey) return
      $_modifierHeld.value = true
      $_modifierArmedAt = Date.now()
      $_prepareFieldHints()
      $_attachViewportScrollListener()
      if (!fieldHintsActive.value && !$_fieldHintRevealTimer) {
        $_fieldHintRevealTimer = setTimeout(() => {
          $_fieldHintRevealTimer = null
          if (!$_modifierHeld.value) return
          $_refreshFieldHintsForViewport(true)
        }, 180)
      }
      return
    }
    if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey) return
    if (!$_fieldHintMap.value) {
      $_prepareFieldHints()
    }
    // 修饰键 + 上/下：滚动属性区或页面声明的辅助滚动区（如 IM 配置说明）
    if (k === 'ArrowDown' || k === 'ArrowUp') {
      const arrowSc = host && typeof host.getFieldHintArrowScrollContainer === 'function'
        ? host.getFieldHintArrowScrollContainer()
        : null
      const sc = arrowSc || ($_fieldHintMap.value ? $_getScrollContainer() : null)
      if (sc && scrollVerticalContainerByArrow(sc, k)) {
        e.preventDefault()
        e.stopPropagation()
        $_scheduleViewportHintRefresh()
        return
      }
    }
    // 修饰键 + 字母：直达字段 / 触发固定动作（映射已在修饰键按下时同步构建）
    if (k && k.length === 1) {
      const letter = /^\d$/.test(k) ? k : k.toUpperCase()
      if ($_fieldHintMap.value && $_fieldHintMap.value[letter]) {
        e.preventDefault()
        e.stopPropagation()
        const target = $_fieldHintMap.value[letter]
        $_hideFieldHintBadges()
        if (target && typeof target.run === 'function') {
          target.run()
        } else if (target && target.el) {
          $_focusControl(target.el)
        }
        $_nextTick(() => {
          if ($_modifierHeld.value && (e.metaKey || e.ctrlKey)) {
            $_refreshFieldHintsForViewport(true)
          }
        })
        return
      }
      // 系统保留组合（全选/复制/粘贴等）永不拦截
      if (FIELD_HINT_BLOCKED.has(letter)) return
      // 非映射字母（如 Cmd+C 复制）：放行浏览器默认行为
    }
  }

  const $_fieldHintKeyup = (e) => {
    if (!$_fieldHintSurfaceVisible()) return
    if (isModifierKeyEvent(e)) {
      $_modifierHeld.value = false
      $_clearRevealTimer()
      $_detachViewportScrollListener()
      $_hideFieldHints()
    }
  }

  const $_attachViewportScrollListener = () => {
    const sc = $_getScrollContainer()
    if (!sc) return
    if ($_fieldHintScrollEl === sc) return
    $_detachViewportScrollListener()
    $_fieldHintScrollEl = sc
    $_onFieldHintScroll = () => $_scheduleViewportHintRefresh()
    $_fieldHintScrollEl.addEventListener('scroll', $_onFieldHintScroll, { passive: true })
  }

  const $_detachViewportScrollListener = () => {
    $_clearViewportHintRefresh()
    if ($_fieldHintScrollEl && $_onFieldHintScroll) {
      $_fieldHintScrollEl.removeEventListener('scroll', $_onFieldHintScroll)
    }
    $_fieldHintScrollEl = null
    $_onFieldHintScroll = null
  }

  const $_clearViewportHintRefresh = () => {
    if ($_fieldHintScrollRefreshRaf) {
      cancelAnimationFrame($_fieldHintScrollRefreshRaf)
      $_fieldHintScrollRefreshRaf = null
    }
  }

  /** 滚动或聚焦后按当前视口重建映射；reveal 为 true 时同步刷新徽标 */
  const $_scheduleViewportHintRefresh = (reveal) => {
    if ($_fieldHintScrollRefreshRaf) return
    $_fieldHintScrollRefreshRaf = requestAnimationFrame(() => {
      $_fieldHintScrollRefreshRaf = null
      if (!$_fieldHintSurfaceVisible() || !$_modifierHeld.value ||
        !($_fieldHintMap.value || $_fieldHintRevealTimer || fieldHintsActive.value)) {
        return
      }
      $_refreshFieldHintsForViewport(reveal || fieldHintsActive.value)
    })
  }

  const $_refreshFieldHintsForViewport = (reveal) => {
    const built = $_buildFieldHints()
    $_hideFieldHintBadges()
    if (!built) {
      $_fieldHintMap.value = null
      $_fieldHintPending = null
      return
    }
    $_fieldHintMap.value = built.map
    $_fieldHintPending = built.pending
    if (reveal) $_revealBadges()
  }

  /** 同步构建字母→目标映射与待注入徽标清单（不操作 DOM 显示） */
  const $_prepareFieldHints = () => {
    if ($_fieldHintMap.value) return
    const built = $_buildFieldHints()
    if (!built) return
    $_fieldHintMap.value = built.map
    $_fieldHintPending = built.pending
  }

  /** 按 DOM 顺序扫描表单项，仅对当前视口内可见项分配快捷键 */
  const $_buildFieldHints = () => {
    const host = vm()
    const container =
      (host && typeof host.getFieldHintContainer === 'function' && host.getFieldHintContainer()) || (host && host.$el)
    if (!container) return null
    const scrollContainer = $_getScrollContainer()
    const viewportRect = getFieldHintViewportRect(scrollContainer, container)
    const map = {}
    const pending = []
    const reserved = {}
    if (host && typeof host.getFieldHintReservedLetters === 'function') {
      Object.assign(reserved, host.getFieldHintReservedLetters())
    }

    // 1) 组件声明的固定快捷键（如复选框等无 label 的控件）
    const fixed =
      (host && typeof host.getFixedFieldHints === 'function' && host.getFixedFieldHints()) || []
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

    const assignMode = host && typeof host.getFieldHintAssignMode === 'function'
      ? host.getFieldHintAssignMode()
      : null

    // 2a) 逐 checkbox / 下拉框分配（通知设置等）
    if (assignMode === 'checkbox-first') {
      $_assignPerCheckboxFieldHints(container, viewportRect, map, pending, reserved, fixed)
      $_assignPerSelectFieldHints(container, viewportRect, map, pending, reserved, fixed)
    }

    // 2b) 视口内表单字段自动分配（跳过已被固定占用的字母）
    const pool = buildFieldHintKeyPool(reserved)
    const items = Array.from(container.querySelectorAll('.el-form-item'))
    let idx = 0
    for (const item of items) {
      if (idx >= pool.length) break
      if (item.classList && item.classList.contains('cat2bug-field-hint-exclude')) continue
      if (assignMode === 'checkbox-first' && $_formItemIsCheckboxOnly(item)) continue
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

    if (assignMode === 'checkbox-first') {
      $_assignPerTestButtonFieldHints(container, viewportRect, map, pending, pool, idx)
    }

    return { map, pending }
  }

  /** 表单项内是否仅有 checkbox（已由逐 checkbox 分配覆盖） */
  const $_formItemIsCheckboxOnly = (item) => {
    const content = item.querySelector('.el-form-item__content') || item
    if (!content.querySelector('.el-checkbox')) return false
    if (content.querySelector('.el-switch') && isVisibleEl(content.querySelector('.el-switch'))) return false
    if (content.querySelector('textarea')) return false
    if (content.querySelector('input:not([type="checkbox"]):not([type="hidden"])')) return false
    if (content.querySelector('.el-select') && isVisibleEl(content.querySelector('.el-select'))) return false
    if (content.querySelector('.cat2bug-upload-focus-target')) return false
    return true
  }

  /** 为视口内每个可见 checkbox 分配字母；跳过固定徽标宿主内的勾选框 */
  const $_assignPerCheckboxFieldHints = (container, viewportRect, map, pending, reserved, fixed) => {
    const fixedHosts = []
    ;(fixed || []).forEach((f) => {
      if (!f || !f.badgeSelector || f.injectBadge === false) return
      const host = container.querySelector(f.badgeSelector)
      if (host) fixedHosts.push(host)
    })
    const pool = buildFieldHintKeyPool(reserved)
    let idx = 0
    const checkboxes = Array.from(container.querySelectorAll('.el-checkbox'))
    for (const cb of checkboxes) {
      if (idx >= pool.length) break
      if (!isVisibleEl(cb) || cb.classList.contains('is-disabled')) continue
      if (!isElementInViewportRect(cb, viewportRect)) continue
      if (fixedHosts.some((host) => host.contains(cb))) continue
      const letter = pool[idx++]
      map[letter] = { run: () => $_activateCheckbox(cb) }
      pending.push({ anchor: cb, letter })
      reserved[letter] = true
    }
  }

  /** 为视口内每个可见 el-select 分配字母（聚焦下拉触发器） */
  const $_assignPerSelectFieldHints = (container, viewportRect, map, pending, reserved, fixed) => {
    const fixedHosts = []
    ;(fixed || []).forEach((f) => {
      if (!f || !f.badgeSelector || f.injectBadge === false) return
      const host = container.querySelector(f.badgeSelector)
      if (host) fixedHosts.push(host)
    })
    const pool = buildFieldHintKeyPool(reserved)
    let idx = 0
    const selects = Array.from(container.querySelectorAll('.el-select'))
    for (const sel of selects) {
      if (idx >= pool.length) break
      if (!isVisibleEl(sel) || sel.classList.contains('is-disabled')) continue
      if (!isElementInViewportRect(sel, viewportRect)) continue
      if (fixedHosts.some((host) => host.contains(sel))) continue
      const input = sel.querySelector('input')
      if (!input || input.disabled) continue
      const letter = pool[idx++]
      map[letter] = { el: input }
      pending.push({ anchor: sel, letter })
      reserved[letter] = true
    }
  }

  /** 通知设置等平台：单发/群发测试等 append 按钮各占一键 */
  const $_assignPerTestButtonFieldHints = (container, viewportRect, map, pending, pool, startIdx) => {
    let idx = startIdx
    const buttons = Array.from(container.querySelectorAll('.notice-option-test-btn'))
    for (const btn of buttons) {
      if (idx >= pool.length) break
      if (!isVisibleEl(btn) || btn.disabled || btn.classList.contains('is-disabled')) continue
      if (!isElementInViewportRect(btn, viewportRect)) continue
      const letter = pool[idx++]
      map[letter] = { run: () => btn.click() }
      pending.push({ anchor: btn, letter })
    }
  }

  /** Cmd/Ctrl+字母：切换 checkbox（与点击勾选框等效） */
  const $_activateCheckbox = (checkboxEl) => {
    if (!checkboxEl || checkboxEl.classList.contains('is-disabled')) return
    const input = checkboxEl.querySelector('input[type="checkbox"]')
    if (input && !input.disabled) {
      input.click()
      return
    }
    if (typeof checkboxEl.click === 'function') checkboxEl.click()
  }

  /** 真正注入徽标 DOM 并标记激活（延迟调用，避免闪烁） */
  const $_revealBadges = () => {
    if (!$_fieldHintPending) return
    if (fieldHintsActive.value) $_hideFieldHintBadges()
    const nodes = []
    $_fieldHintPending.forEach((p) => {
      if (p.anchor && p.anchor.isConnected !== false) {
        nodes.push($_injectBadge(p.anchor, p.letter))
      }
    })
    $_fieldHintNodes = nodes
    fieldHintsActive.value = true
  }

  /** 查找属性区可滚动容器（默认向上找最近的可滚动祖先，如抽屉 body） */
  const $_getScrollContainer = () => {
    const host = vm()
    if (host && typeof host.getFieldHintScrollContainer === 'function') {
      const c = host.getFieldHintScrollContainer()
      if (c) return c
    }
    let el = (host && typeof host.getFieldHintContainer === 'function' && host.getFieldHintContainer()) || (host && host.$el)
    while (el && el !== document.body) {
      const oy = window.getComputedStyle(el).overflowY
      if ((oy === 'auto' || oy === 'scroll') && el.scrollHeight > el.clientHeight + 2) return el
      el = el.parentElement
    }
    return null
  }

  /** 徽标浮在宿主右下角，不挤占标签/按钮排版 */
  const $_injectBadge = (anchor, letter) => {
    anchor.classList.add('cat2bug-field-hint-host')
    const badge = document.createElement('span')
    badge.className = 'cat2bug-field-hint'
    badge.textContent = letter
    badge.setAttribute('aria-hidden', 'true')
    anchor.appendChild(badge)
    return { badge, host: anchor }
  }

  /** 仅移除徽标 DOM；保留映射以便按住 Cmd 时连续跳转字段 */
  const $_hideFieldHintBadges = () => {
    if ($_fieldHintNodes) {
      $_fieldHintNodes.forEach((entry) => {
        const badge = entry && entry.badge ? entry.badge : entry
        const host = entry && entry.host
        if (badge && badge.parentNode) badge.parentNode.removeChild(badge)
        if (host && host.classList) host.classList.remove('cat2bug-field-hint-host')
      })
    }
    $_fieldHintNodes = null
    if (fieldHintsActive.value) fieldHintsActive.value = false
  }

  const $_hideFieldHints = () => {
    $_modifierHeld.value = false
    $_clearViewportHintRefresh()
    $_clearRevealTimer()
    $_detachViewportScrollListener()
    $_hideFieldHintBadges()
    $_fieldHintMap.value = null
    $_fieldHintPending = null
  }

  const $_focusControl = (el) => {
    if (!el) return
    const host = vm()
    const container =
      (host && typeof host.getFieldHintContainer === 'function' && host.getFieldHintContainer()) || (host && host.$el)
    dismissComboPopoverIfLeaving(document.activeElement, el)
    releaseControlFocusBeforeJump(el, container)
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
    syncFieldFocusVisual(container, el)
    $_nextTick(() => syncFieldFocusVisual(container, el))
    if (typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ block: 'center', behavior: 'smooth' })
    }
    if (el.classList && el.classList.contains('component-upload-image-focus-target')) {
      let vm = el.__vue__
      while (vm) {
        if (typeof vm.applyKeyboardFocusVisual === 'function') {
          vm.$nextTick(() => {
            if (typeof vm.getDefaultKeyboardFocusIndex === 'function') {
              vm.keyboardFocusIndex = vm.getDefaultKeyboardFocusIndex()
            }
            vm.applyKeyboardFocusVisual()
          })
          break
        }
        vm = vm.$parent
      }
    }
  }

  watch(() => vm()?.visible, (val) => {
    $_syncFieldHintListeners(!!val)
  })

  watch(() => vm()?.dialogVisible, (val) => {
    $_syncFieldHintListeners(!!val)
  })

  onBeforeUnmount(() => {
    $_detachFieldHintListeners()
    $_hideFieldHints()
  })

  return {
    fieldHintsActive,
    $_modifierHeld,
    $_fieldHintMap,
    $_refreshFieldHintsForViewport,
    $_focusControl
  }
}
