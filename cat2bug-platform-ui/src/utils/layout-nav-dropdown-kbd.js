/**
 * 布局级快捷键打开的下拉：↑/↓ 切换、Enter 确认、Esc 关闭。
 * 用于团队选择（el-select）、用户菜单与国际化（el-dropdown）。
 *
 * keydown 由 layout-nav-hints mixin 统一委托至 handleLayoutNavDropdownKeydown，
 * 勿在此重复注册 document 监听（否则方向键会被处理两次、跳项）。
 */

const ACTIVE_CLASS = 'layout-nav-kbd-dropdown-active'

let session = null

function isVisibleEl(el) {
  if (!el || !el.isConnected) return false
  const rect = el.getBoundingClientRect()
  if (rect.width <= 0 && rect.height <= 0) return false
  const style = window.getComputedStyle(el)
  return style.display !== 'none' && style.visibility !== 'hidden'
}

function clearHighlights() {
  document.querySelectorAll(`.${ACTIVE_CLASS}`).forEach((el) => {
    el.classList.remove(ACTIVE_CLASS)
  })
}

function scrollItemIntoView(el) {
  if (!el || typeof el.scrollIntoView !== 'function') return
  try {
    el.scrollIntoView({ block: 'nearest' })
  } catch (e) {
    el.scrollIntoView(false)
  }
}

function getTeamSelectVm(anchor) {
  const root = anchor && anchor.querySelector('.el-select')
  return root && root.__vue__ ? root.__vue__ : null
}

function getTeamDropdownRoot(anchor) {
  if (!anchor) return null
  const vm = getTeamSelectVm(anchor)
  const popperRef = vm && vm.$refs && vm.$refs.popper
  const popperEl = popperRef && (popperRef.$el || popperRef)
  if (popperEl && isVisibleEl(popperEl)) {
    return popperEl.classList && popperEl.classList.contains('el-select-dropdown')
      ? popperEl
      : popperEl.querySelector('.el-select-dropdown') || popperEl
  }
  const menus = Array.from(document.querySelectorAll('.team-select-kbd-popper, .el-select-dropdown.el-popper'))
    .filter(isVisibleEl)
  if (!menus.length) return null
  const anchorRect = anchor.getBoundingClientRect()
  let best = menus[0]
  let bestDist = Infinity
  menus.forEach((menu) => {
    const mr = menu.getBoundingClientRect()
    const dist = Math.hypot(mr.left - anchorRect.left, mr.top - anchorRect.bottom)
    if (dist < bestDist) {
      bestDist = dist
      best = menu
    }
  })
  return best
}

function getTeamDropdownItems(anchor) {
  const root = getTeamDropdownRoot(anchor)
  if (!root) return []
  return Array.from(root.querySelectorAll('.el-select-dropdown__item:not(.is-disabled)'))
    .filter(isVisibleEl)
}

function findElDropdownVm(anchor) {
  if (!anchor) return null
  const visit = (vm) => {
    if (!vm) return null
    const queue = [vm]
    const seen = new Set()
    while (queue.length) {
      const cur = queue.shift()
      if (!cur || seen.has(cur)) continue
      seen.add(cur)
      const name = cur.$options && cur.$options.name
      if (name === 'ElDropdown') return cur
      if (cur.$children && cur.$children.length) {
        cur.$children.forEach((child) => queue.push(child))
      }
    }
    let p = vm.$parent
    while (p) {
      if (p.$options && p.$options.name === 'ElDropdown') return p
      p = p.$parent
    }
    return null
  }
  return visit(anchor.__vue__)
}

function getElDropdownPopperEl(anchor) {
  const vm = findElDropdownVm(anchor)
  if (!vm) return null
  if (vm.popperElm && isVisibleEl(vm.popperElm)) return vm.popperElm

  const popperRef = vm.$refs && vm.$refs.popper
  let popperEl = popperRef && (popperRef.$el || popperRef)
  if (!popperEl) return null
  if (popperEl.classList && popperEl.classList.contains('el-dropdown-menu')) {
    return isVisibleEl(popperEl) ? popperEl : null
  }
  const menu = popperEl.querySelector && popperEl.querySelector('.el-dropdown-menu')
  if (menu && isVisibleEl(menu)) return menu
  return isVisibleEl(popperEl) ? popperEl : null
}

function getElDropdownMenuRoot(anchor) {
  const fromRef = getElDropdownPopperEl(anchor)
  if (fromRef) return fromRef

  const menus = Array.from(document.querySelectorAll('.el-dropdown-menu.el-popper'))
  const visible = menus.filter(isVisibleEl)
  if (!visible.length) return null
  if (anchor) {
    const rect = anchor.getBoundingClientRect()
    let best = visible[0]
    let bestDist = Infinity
    visible.forEach((menu) => {
      const mr = menu.getBoundingClientRect()
      const dist = Math.hypot(mr.left - rect.right, mr.top - rect.top)
      if (dist < bestDist) {
        bestDist = dist
        best = menu
      }
    })
    return best
  }
  return visible[visible.length - 1]
}

function getElDropdownMenuItems(anchor) {
  const root = getElDropdownMenuRoot(anchor)
  if (!root) return []
  return Array.from(root.querySelectorAll('.el-dropdown-menu__item:not(.is-disabled)'))
    .filter(isVisibleEl)
}

function getSessionItems() {
  if (!session) return []
  if (session.kind === 'team') return getTeamDropdownItems(session.anchor)
  if (session.kind === 'user' || session.kind === 'lang') {
    return getElDropdownMenuItems(session.anchor)
  }
  return []
}

function highlightIndex(index) {
  if (!session) return
  const items = getSessionItems()
  if (!items.length) return
  const idx = Math.max(0, Math.min(index, items.length - 1))
  session.index = idx
  clearHighlights()
  const el = items[idx]
  el.classList.add(ACTIVE_CLASS)
  scrollItemIntoView(el)
}

function moveSession(delta) {
  const items = getSessionItems()
  if (!items.length) return false
  const next = session.index == null ? 0 : session.index + delta
  highlightIndex(next)
  return true
}

function closeTeamDropdown(anchor) {
  const vm = getTeamSelectVm(anchor)
  if (vm) {
    vm.visible = false
    if (typeof vm.blur === 'function') vm.blur()
  }
}

function closeElDropdown(anchor) {
  const vm = findElDropdownVm(anchor)
  if (vm && typeof vm.hide === 'function') {
    vm.hide()
    return
  }
  document.body.click()
}

function activateTeamItem(item) {
  if (!item || !session) return
  if (item.classList.contains('team-select-footer')) {
    const btn = item.querySelector('.el-button')
    if (btn) btn.click()
    return
  }
  if (typeof item.click === 'function') item.click()
}

function activateElDropdownItem(item) {
  if (!item) return
  const link = item.closest('a') || item.querySelector('a')
  if (link && typeof link.click === 'function') {
    link.click()
    return
  }
  if (typeof item.click === 'function') item.click()
}

function activateCurrent() {
  if (!session) return false
  const items = getSessionItems()
  if (!items.length || session.index == null) return false
  const item = items[session.index]
  if (!item) return false
  if (session.kind === 'team') activateTeamItem(item)
  else if (session.kind === 'user' || session.kind === 'lang') activateElDropdownItem(item)
  return true
}

function onSessionKeydown(e) {
  if (!session) return false
  const key = e.key
  if (key === 'ArrowDown') {
    e.preventDefault()
    e.stopPropagation()
    moveSession(1)
    return true
  }
  if (key === 'ArrowUp') {
    e.preventDefault()
    e.stopPropagation()
    moveSession(-1)
    return true
  }
  if (key === 'Enter') {
    e.preventDefault()
    e.stopPropagation()
    if (activateCurrent()) endLayoutNavDropdownSession()
    return true
  }
  if (key === 'Escape') {
    e.preventDefault()
    e.stopPropagation()
    endLayoutNavDropdownSession()
    return true
  }
  return false
}

export function hasLayoutNavDropdownSession() {
  return !!session
}

export function handleLayoutNavDropdownKeydown(e) {
  return onSessionKeydown(e)
}

export function endLayoutNavDropdownSession() {
  if (!session) return
  clearHighlights()
  if (session.kind === 'team') closeTeamDropdown(session.anchor)
  else if (session.kind === 'user' || session.kind === 'lang') closeElDropdown(session.anchor)
  session = null
}

export function openLayoutTeamSelect(anchor) {
  if (!anchor) return false
  const vm = getTeamSelectVm(anchor)
  if (!vm) return false
  if (vm.visible) return true
  if (typeof vm.toggleMenu === 'function') {
    vm.toggleMenu()
    return true
  }
  vm.visible = true
  return true
}

/**
 * @param {'team'|'user'|'lang'} kind
 * @param {HTMLElement} anchor
 */
function waitForDropdownItems(getItems, onReady, attempt = 0) {
  const items = getItems()
  if (items.length || attempt >= 20) {
    onReady(items)
    return
  }
  setTimeout(() => waitForDropdownItems(getItems, onReady, attempt + 1), 50)
}

export function startLayoutNavDropdownSession(kind, anchor) {
  endLayoutNavDropdownSession()
  session = { kind, anchor, index: 0 }
  waitForDropdownItems(
    () => getSessionItems(),
    (items) => {
      if (!session) return
      if (items.length) highlightIndex(0)
    }
  )
}
