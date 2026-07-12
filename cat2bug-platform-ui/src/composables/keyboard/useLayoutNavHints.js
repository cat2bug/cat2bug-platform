/**
 * 布局级快捷键提示：按住 Shift+Cmd/Ctrl 在左侧菜单、顶部工具栏、折叠按钮显示字母徽标；
 * 团队/用户/国际化下拉打开后支持 ↑↓ 切换、Enter 确认、Esc 关闭。
 */
import { getCurrentInstance, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
import { useAppStore } from '@/stores/app'
import { storeToRefs } from 'pinia'

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

export function useLayoutNavHints() {
  const instance = getCurrentInstance()
  const router = useRouter()
  const route = useRoute()
  const appStore = useAppStore()
  const { sidebar } = storeToRefs(appStore)

  const layoutNavHintsActive = ref(false)

  let layoutNavHintListenersBound = false
  let onLayoutNavKeydown = null
  let onLayoutNavKeyup = null
  let layoutNavRevealTimer = null
  let layoutNavRefreshTimer = null
  let layoutNavRefreshBound = false
  let onSidebarTransitionEnd = null
  let onWindowResize = null
  let unwatchSidebarOpened = null
  let layoutNavSidebarEl = null
  let layoutNavModifierHeld = false
  let layoutNavHintMap = null
  let layoutNavHintPending = null

  function clearLayoutNavRevealTimer() {
    if (layoutNavRevealTimer) {
      clearTimeout(layoutNavRevealTimer)
      layoutNavRevealTimer = null
    }
  }

  function clearLayoutNavBadgeRefreshTimer() {
    if (layoutNavRefreshTimer) {
      clearTimeout(layoutNavRefreshTimer)
      layoutNavRefreshTimer = null
    }
  }

  function hideLayoutNavHintBadges() {
    clearOverlay()
    layoutNavHintsActive.value = false
  }

  function hideLayoutNavHints() {
    layoutNavModifierHeld = false
    clearLayoutNavRevealTimer()
    clearLayoutNavBadgeRefreshTimer()
    hideLayoutNavHintBadges()
    layoutNavHintMap = null
    layoutNavHintPending = null
  }

  function prepareLayoutNavHints() {
    const built = buildLayoutNavHintDescriptors()
    if (!built) {
      layoutNavHintMap = null
      layoutNavHintPending = null
      return
    }
    layoutNavHintMap = built.map
    layoutNavHintPending = built.pending
  }

  function revealLayoutNavBadges() {
    prepareLayoutNavHints()
    hideLayoutNavHintBadges()
    if (!layoutNavHintPending || !layoutNavHintPending.length) {
      layoutNavHintsActive.value = false
      return
    }
    const overlay = ensureOverlay()
    layoutNavHintPending.forEach((p) => {
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
    layoutNavHintsActive.value = true
  }

  function scheduleLayoutNavBadgeRefresh(delay = SIDEBAR_TRANSITION_MS) {
    clearLayoutNavBadgeRefreshTimer()
    layoutNavRefreshTimer = setTimeout(() => {
      layoutNavRefreshTimer = null
      if (!layoutNavModifierHeld && !layoutNavHintsActive.value) return
      revealLayoutNavBadges()
    }, delay)
  }

  function armLayoutNavHints() {
    layoutNavModifierHeld = true
    prepareLayoutNavHints()
    if (!layoutNavHintsActive.value && !layoutNavRevealTimer) {
      layoutNavRevealTimer = setTimeout(() => {
        layoutNavRevealTimer = null
        if (!layoutNavModifierHeld) return
        revealLayoutNavBadges()
      }, 180)
    }
  }

  function runLayoutNavHint(descriptor) {
    const item = descriptor && descriptor.navItem
    if (!item) return
    if (item.action === 'layout:toggle-sidebar') {
      appStore.toggleSideBar()
      return
    }
    if (item.type === 'route' && item.to) {
      if (route.path !== item.to) {
        router.push(item.to).catch(() => {})
      }
      return
    }
    if (item.action === 'layout:open-team-select') {
      if (openLayoutTeamSelect(descriptor.anchor)) {
        instance?.proxy?.$nextTick(() => startLayoutNavDropdownSession('team', descriptor.anchor))
      }
      return
    }
    if (item.type === 'dropdown') {
      const key = item.bindingId && item.bindingId.replace('nav.top.', '')
      if (key === 'theme') {
        if (descriptor.anchor && typeof descriptor.anchor.click === 'function') {
          descriptor.anchor.click()
        }
        return
      }
      if (openLayoutNavDropdown(descriptor.anchor)) {
        if (key === 'user' || key === 'lang') {
          instance?.proxy?.$nextTick(() => startLayoutNavDropdownSession(key, descriptor.anchor))
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
    const shortcut = instance?.proxy?.$shortcut
    const getExecutor = shortcut && shortcut.service && shortcut.service.getExecutor
    const executor = typeof getExecutor === 'function' ? getExecutor() : null
    if (typeof executor === 'function') {
      executor(item)
    }
  }

  function layoutNavHintKeydown(e) {
    if (e.isComposing) return
    if (hasLayoutNavDropdownSession() && handleLayoutNavDropdownKeydown(e)) return
    if (hasBlockingUiLayer()) {
      if (layoutNavHintsActive.value || layoutNavModifierHeld) {
        hideLayoutNavHints()
      }
      return
    }

    if ((layoutNavModifierHeld || layoutNavHintsActive.value) &&
      !isLayoutNavModifier(e) && !isModifierKeyEvent(e) && !hasLayoutNavDropdownSession()) {
      hideLayoutNavHints()
    }

    if (isLayoutNavModifier(e) && (isModifierKeyEvent(e) || isShiftKeyEvent(e))) {
      armLayoutNavHints()
      return
    }

    if (!isLayoutNavModifier(e)) return
    if (!layoutNavHintMap) prepareLayoutNavHints()

    const letter = resolveLayoutHintLetter(e)
    if (letter) {
      if (/^\d$/.test(letter) && hasActivePageRowKbdHints()) return
      if (layoutNavHintMap && layoutNavHintMap[letter]) {
        e.preventDefault()
        e.stopPropagation()
        const descriptor = layoutNavHintMap[letter]
        const opensDropdown = opensLayoutDropdownKbd(descriptor)
        const togglesSidebar = descriptor.navItem &&
          descriptor.navItem.action === 'layout:toggle-sidebar'
        runLayoutNavHint(descriptor)
        hideLayoutNavHintBadges()
        if (opensDropdown) {
          layoutNavModifierHeld = false
          clearLayoutNavRevealTimer()
          return
        }
        if (togglesSidebar) {
          if (layoutNavModifierHeld || isLayoutNavModifier(e)) {
            scheduleLayoutNavBadgeRefresh()
          }
          return
        }
        instance?.proxy?.$nextTick(() => {
          if (layoutNavModifierHeld && isLayoutNavModifier(e)) {
            revealLayoutNavBadges()
          }
        })
      }
    }
  }

  function layoutNavHintKeyup(e) {
    if (isModifierKeyEvent(e) || !e.shiftKey) {
      layoutNavModifierHeld = false
      clearLayoutNavRevealTimer()
      if (hasLayoutNavDropdownSession()) {
        hideLayoutNavHintBadges()
        return
      }
      hideLayoutNavHints()
    }
  }

  function attachLayoutNavHintListeners() {
    if (layoutNavHintListenersBound) return
    layoutNavHintListenersBound = true
    onLayoutNavKeydown = (e) => layoutNavHintKeydown(e)
    onLayoutNavKeyup = (e) => layoutNavHintKeyup(e)
    document.addEventListener('keydown', onLayoutNavKeydown, true)
    window.addEventListener('keyup', onLayoutNavKeyup, true)
  }

  function detachLayoutNavHintListeners() {
    if (!layoutNavHintListenersBound) return
    layoutNavHintListenersBound = false
    clearLayoutNavRevealTimer()
    hideLayoutNavHints()
    document.removeEventListener('keydown', onLayoutNavKeydown, true)
    window.removeEventListener('keyup', onLayoutNavKeyup, true)
  }

  function bindLayoutNavRefreshObservers() {
    if (layoutNavRefreshBound) return
    layoutNavRefreshBound = true
    onSidebarTransitionEnd = () => {
      if (layoutNavModifierHeld || layoutNavHintsActive.value) {
        scheduleLayoutNavBadgeRefresh(0)
      }
    }
    onWindowResize = () => {
      if (layoutNavModifierHeld || layoutNavHintsActive.value) {
        scheduleLayoutNavBadgeRefresh(80)
      }
    }
    unwatchSidebarOpened = watch(
      () => sidebar.value.opened,
      () => {
        if (layoutNavModifierHeld || layoutNavHintsActive.value) {
          scheduleLayoutNavBadgeRefresh()
        }
      }
    )
    instance?.proxy?.$nextTick(() => {
      const el = document.querySelector('.sidebar-container')
      if (el) {
        layoutNavSidebarEl = el
        el.addEventListener('transitionend', onSidebarTransitionEnd)
      }
    })
    window.addEventListener('resize', onWindowResize, { passive: true })
  }

  function unbindLayoutNavRefreshObservers() {
    if (!layoutNavRefreshBound) return
    layoutNavRefreshBound = false
    clearLayoutNavBadgeRefreshTimer()
    if (unwatchSidebarOpened) {
      unwatchSidebarOpened()
      unwatchSidebarOpened = null
    }
    if (layoutNavSidebarEl && onSidebarTransitionEnd) {
      layoutNavSidebarEl.removeEventListener('transitionend', onSidebarTransitionEnd)
    }
    layoutNavSidebarEl = null
    if (onWindowResize) {
      window.removeEventListener('resize', onWindowResize)
    }
  }

  onMounted(() => {
    attachLayoutNavHintListeners()
    bindLayoutNavRefreshObservers()
  })

  onBeforeUnmount(() => {
    detachLayoutNavHintListeners()
    unbindLayoutNavRefreshObservers()
    endLayoutNavDropdownSession()
  })

  return {
    layoutNavHintsActive
  }
}
