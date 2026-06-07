/**
 * 布局级导航快捷键（⇧⌘/Shift+Ctrl）：左侧菜单、顶部工具栏、侧栏折叠按钮。
 * 字母映射与 `g` 导航面板共用 buildNavItems / shortcutStore。
 */
import { buildNavItems } from '@/plugins/shortcut/nav-source'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  normalizeKey,
  LAYOUT_SIDEBAR_TOGGLE_BINDING,
  LAYOUT_SIDEBAR_TOGGLE_DEFAULT_LETTER,
  LAYOUT_TEAM_SELECT_BINDING,
  LAYOUT_TEAM_SELECT_DEFAULT_LETTER
} from '@/plugins/shortcut/keymap'
import { openLayoutTeamSelect } from '@/utils/layout-nav-dropdown-kbd'

export {
  LAYOUT_SIDEBAR_TOGGLE_BINDING,
  LAYOUT_SIDEBAR_TOGGLE_DEFAULT_LETTER,
  LAYOUT_TEAM_SELECT_BINDING,
  LAYOUT_TEAM_SELECT_DEFAULT_LETTER
}
export { openLayoutTeamSelect }

const TOP_NAV_SELECTOR = '[data-layout-nav]'

function normPath(p) {
  return String(p || '').replace(/^\//, '').toLowerCase()
}

export function isLayoutNavVisibleEl(el) {
  if (!el || !el.isConnected) return false
  const rect = el.getBoundingClientRect()
  if (rect.width <= 0 && rect.height <= 0) return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const opacity = parseFloat(style.opacity)
  if (!isNaN(opacity) && opacity <= 0) return false
  return true
}

function getSidebarNodeIndex(el) {
  if (!el) return ''
  const vm = el.__vue__
  if (vm && vm.index != null && vm.index !== '') {
    const raw = typeof vm.index === 'object' ? (vm.index.path || '') : vm.index
    return normPath(String(raw))
  }
  const link = el.querySelector('a[href]')
  if (link) {
    const href = (link.getAttribute('href') || '').replace(/^#/, '')
    return normPath(href)
  }
  return normPath(el.getAttribute('index') || '')
}

function pathMatchesIndex(target, idx) {
  if (!target || !idx) return false
  if (idx === target) return true
  if (idx.endsWith('/' + target) || target.endsWith('/' + idx)) return true
  const targetSeg = target.split('/').filter(Boolean).pop()
  const idxSeg = idx.split('/').filter(Boolean).pop()
  return !!(targetSeg && idxSeg && targetSeg === idxSeg)
}

function findVisibleSubmenuTitleFor(el) {
  let node = el
  while (node && node !== document.body) {
    const submenu = node.closest('.el-submenu')
    if (submenu) {
      const title = submenu.querySelector(':scope > .el-submenu__title')
      if (title && isLayoutNavVisibleEl(title)) return title
    }
    node = node.parentElement
  }
  return null
}

/** 按路由 path 匹配侧栏菜单项（展开/折叠均尽量命中可见锚点） */
export function findSidebarMenuAnchor(navTo) {
  const target = normPath(navTo)
  if (!target) return null

  const items = document.querySelectorAll('.sidebar-container .el-menu-item')
  let hiddenMatch = null
  for (let i = 0; i < items.length; i++) {
    const el = items[i]
    const idx = getSidebarNodeIndex(el)
    if (!idx || !pathMatchesIndex(target, idx)) continue
    if (isLayoutNavVisibleEl(el)) return el
    if (!hiddenMatch) hiddenMatch = el
  }

  if (hiddenMatch) {
    const title = findVisibleSubmenuTitleFor(hiddenMatch)
    if (title) return title
  }

  const visibleNodes = document.querySelectorAll(
    '.sidebar-container .el-menu-item, .sidebar-container .el-submenu > .el-submenu__title'
  )
  for (let i = 0; i < visibleNodes.length; i++) {
    const el = visibleNodes[i]
    if (!isLayoutNavVisibleEl(el)) continue
    const idx = getSidebarNodeIndex(el)
    if (idx && pathMatchesIndex(target, idx)) return el
  }

  const targetSeg = target.split('/').filter(Boolean).pop()
  if (targetSeg) {
    for (let i = 0; i < visibleNodes.length; i++) {
      const el = visibleNodes[i]
      if (!isLayoutNavVisibleEl(el)) continue
      const idx = getSidebarNodeIndex(el)
      if (!idx) continue
      if (idx === targetSeg || idx.endsWith('/' + targetSeg)) return el
    }
  }
  return null
}

/** 触发顶栏下拉（用户 U、语言 I 等） */
export function openLayoutNavDropdown(anchor) {
  if (!anchor) return false
  const vm = anchor.__vue__
  if (vm && typeof vm.show === 'function') {
    vm.show()
    return true
  }
  if (vm && vm.$children) {
    for (let i = 0; i < vm.$children.length; i++) {
      const child = vm.$children[i]
      if (child && typeof child.show === 'function') {
        child.show()
        return true
      }
    }
  }
  const trigger = anchor.querySelector(
    '.dropdown-title, .el-dropdown-selfdefine, .el-dropdown-link, [class*="reference"]'
  )
  const clickTarget = trigger || anchor
  if (clickTarget && typeof clickTarget.click === 'function') {
    clickTarget.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }))
    clickTarget.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }))
    clickTarget.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }))
    return true
  }
  return false
}

export function findTopNavAnchor(navKey) {
  if (!navKey) return null
  const el = document.querySelector(`${TOP_NAV_SELECTOR}[data-layout-nav="${navKey}"]`)
  return el && isLayoutNavVisibleEl(el) ? el : null
}

export function findTeamSelectAnchor() {
  const el = document.querySelector('[data-layout-nav="team"]')
  return el && isLayoutNavVisibleEl(el) ? el : null
}

/**
 * 构建布局导航提示项：{ letter, anchor, run, floatOffset }
 * run 由 mixin 注入 router/store 后绑定。
 */
export function buildLayoutNavHintDescriptors() {
  const descriptors = []
  const navItems = buildNavItems()

  navItems.forEach((item) => {
    const letter = normalizeKey(item.letter || item.preferred)
    if (!letter) return

    if (item.type === 'route' && item.to) {
      const anchor = findSidebarMenuAnchor(item.to) || findTopNavAnchor(item.bindingId && item.bindingId.replace('nav.top.', ''))
      if (!anchor) return
      descriptors.push({
        letter,
        anchor,
        navItem: item,
        floatOffset: anchor.closest('.sidebar-container')
          ? { placement: 'bottom-right-inset', outset: 4, dx: -2, dy: -2 }
          : { placement: 'bottom-right-inset', outset: 2 }
      })
      return
    }

    if (item.type === 'action' || item.type === 'dropdown') {
      const key = item.bindingId && item.bindingId.replace('nav.top.', '')
      const anchor = findTopNavAnchor(key)
      if (!anchor) return
      descriptors.push({
        letter,
        anchor,
        navItem: item,
        floatOffset: { placement: 'bottom-right-inset', outset: 2 }
      })
    }
  })

  const toggleLetter = normalizeKey(
    shortcutStore.getLetter(LAYOUT_SIDEBAR_TOGGLE_BINDING, LAYOUT_SIDEBAR_TOGGLE_DEFAULT_LETTER)
  )
  const toggleAnchor = findTopNavAnchor('sidebarToggle')
  if (toggleLetter && toggleAnchor) {
    descriptors.push({
      letter: toggleLetter,
      anchor: toggleAnchor,
      navItem: { action: 'layout:toggle-sidebar' },
      floatOffset: { placement: 'bottom-right-inset', outset: 2 }
    })
  }

  const teamLetter = normalizeKey(
    shortcutStore.getLetter(LAYOUT_TEAM_SELECT_BINDING, LAYOUT_TEAM_SELECT_DEFAULT_LETTER)
  )
  const teamAnchor = findTeamSelectAnchor()
  if (teamLetter && teamAnchor) {
    descriptors.push({
      letter: teamLetter,
      anchor: teamAnchor,
      navItem: { action: 'layout:open-team-select' },
      floatOffset: { placement: 'bottom-right-inset', outset: 4, dx: -2, dy: -2 }
    })
  }

  const map = {}
  const pending = []
  descriptors.forEach((d) => {
    if (!d.letter || map[d.letter]) return
    map[d.letter] = d
    pending.push(d)
  })
  return Object.keys(map).length ? { map, pending } : null
}
