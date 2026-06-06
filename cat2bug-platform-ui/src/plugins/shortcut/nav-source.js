/**
 * 从实时左侧菜单树（store.sidebarRouters）与顶部工具栏派生导航面板项。
 */
import store from '@/store'
import i18n from '@/utils/i18n/i18n'
import { TOP_ITEMS, DEFAULT_MENU_LETTERS, DEFAULT_LEADERS, assignLetters } from './keymap'
import { shortcutStore } from './shortcut-store'

const MENU_GROUPS = ['project', 'team', 'team-options', 'admin']

function joinPath(base, seg) {
  if (!seg) return base
  if (seg.startsWith('/')) return seg
  return `${base.replace(/\/$/, '')}/${seg}`
}

function isHidden(route) {
  return route && route.hidden === true
}

/** 取菜单标题（优先 i18n key） */
function menuTitle(route) {
  const meta = route.meta || {}
  if (meta.titleI18nKey) return i18n.t(meta.titleI18nKey)
  if (meta.title) return meta.title
  return route.name || route.path
}

/** 递归收集某分组下的叶子菜单页面 */
function collectLeaves(route, basePath, out) {
  if (isHidden(route)) return
  const children = (route.children || []).filter((c) => !isHidden(c))
  const fullPath = joinPath(basePath, route.path)
  if (children.length === 0) {
    const meta = route.meta || {}
    if (!meta || (!meta.title && !meta.titleI18nKey && !route.name)) return
    out.push({ path: fullPath, route })
    return
  }
  // 单一可见子项时，Element 菜单将其作为根项展示，仍可作为一个导航目标
  if (children.length === 1) {
    collectLeaves(children[0], fullPath, out)
    return
  }
  children.forEach((c) => collectLeaves(c, fullPath, out))
}

function lastSegment(path) {
  const parts = String(path || '').split('/').filter(Boolean)
  return parts.length ? parts[parts.length - 1].toLowerCase() : ''
}

/** 构建左侧菜单导航项（已解析标题、默认字母、跳转回调描述） */
function buildMenuItems() {
  const routers = (store.getters.sidebarRouters || store.state.permission.sidebarRouters || [])
  const items = []
  MENU_GROUPS.forEach((groupName) => {
    const group = routers.find(
      (r) => r.name && String(r.name).toLowerCase() === groupName
    )
    if (!group || !group.children) return
    const leaves = []
    group.children.forEach((child) => collectLeaves(child, `/${groupName}`, leaves))
    leaves.forEach((leaf) => {
      const seg = lastSegment(leaf.path)
      const bindingId = `nav.menu.${groupName}.${seg}`
      items.push({
        bindingId,
        type: 'route',
        title: menuTitle(leaf.route),
        to: leaf.path,
        defaultLetter: DEFAULT_MENU_LETTERS[seg] || ''
      })
    })
  })
  return items
}

/** 构建顶部工具项 */
function buildTopItems() {
  return TOP_ITEMS.map((item) => {
    const bindingId = `nav.top.${item.key}`
    const base = {
      bindingId,
      type: item.type,
      title: item.titleKey ? i18n.t(item.titleKey) : item.title || '',
      defaultLetter: item.defaultLetter
    }
    if (item.type === 'route') base.to = item.to
    if (item.type === 'action') base.action = item.action
    if (item.type === 'dropdown') {
      base.children = item.children.map((c) => ({
        bindingId: `${bindingId}.${c.key}`,
        type: c.to ? 'route' : 'action',
        title: c.titleKey ? i18n.t(c.titleKey) : c.title || '',
        to: c.to,
        action: c.action,
        defaultLetter: c.defaultLetter,
        preferred: shortcutStore.getLetter(`${bindingId}.${c.key}`, c.defaultLetter)
      }))
    }
    return base
  })
}

/** 把绑定项解析为带唯一字母的面板项 */
function resolveLevel(rawItems) {
  const list = rawItems.map((it) => ({
    ...it,
    preferred: shortcutStore.getLetter(it.bindingId, it.defaultLetter)
  }))
  assignLetters(list)
  return list
}

/** 顶层导航面板项（菜单页面 + 顶部项），均带唯一字母 */
export function buildNavItems() {
  const raw = [...buildMenuItems(), ...buildTopItems()]
  const resolved = resolveLevel(raw)
  // 二级面板项的字母在各自层内单独保证唯一
  resolved.forEach((it) => {
    if (it.children && it.children.length) {
      assignLetters(it.children)
    }
  })
  return resolved
}

/** 导航引导键 */
export function navLeader() {
  return shortcutStore.getLeader('nav') || DEFAULT_LEADERS.nav
}

/** 动作引导键 */
export function actionLeader() {
  return shortcutStore.getLeader('action') || DEFAULT_LEADERS.action
}
