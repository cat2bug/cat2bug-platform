import i18n from '@/utils/i18n/i18n'
import { resolveToolbarMenuLabel } from '@/components/Cat2BugMarkdown/resolveToolbarMenuLabel'
import { loadReportToolbarMenus } from './toolbarMenuLoader'
import { resolveToolContent } from './useReportTemplateTools'

/** Toolbar roots that map to report DSL business inserts. */
const BUSINESS_ROOT_NAMES = new Set(['project', 'case', 'defect', 'plan'])

/** Simple document icon for Crepe block menu (SVG string). */
export const BUSINESS_BLOCK_MENU_ICON =
  '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 2l5 5h-5V4zM8 13h8v2H8v-2zm0 4h5v2H8v-2z"/></svg>'

function isBusinessRoot(tool) {
  return tool?.name && BUSINESS_ROOT_NAMES.has(tool.name)
}

function isPlanDisplayName(name) {
  return name && !BUSINESS_ROOT_NAMES.has(name) && !name.includes('.')
}

function buildInsertItemLabel(ancestors, tool, localeI18n) {
  const parts = ancestors
    .filter((a) => !isBusinessRoot(a))
    .map((a) => {
      if (isPlanDisplayName(a.name)) {
        return a.name
      }
      return resolveToolbarMenuLabel(localeI18n, a.name)
    })
  parts.push(resolveToolbarMenuLabel(localeI18n, tool.name))
  return parts.filter(Boolean).join(' / ')
}

function collectInsertableItems(tools, ancestors, localeI18n, out = []) {
  for (const tool of tools || []) {
    if (tool.type === 'siding') {
      continue
    }
    if (tool.children?.length) {
      collectInsertableItems(tool.children, [...ancestors, tool], localeI18n, out)
      continue
    }
    if (tool.type === 'command' || tool.type === 'component' || tool.type === 'check') {
      continue
    }
    const content = resolveToolContent(tool)
    if (!content) {
      continue
    }
    out.push({
      key: tool.name,
      label: buildInsertItemLabel(ancestors, tool, localeI18n),
      content
    })
  }
  return out
}

/**
 * Extract block-menu groups mirroring toolbar business sections.
 * @param {object[]} toolbarTools
 * @param {import('vue-i18n').Composer} [localeI18n]
 */
export function extractBusinessMenuGroups(toolbarTools, localeI18n = i18n) {
  return (toolbarTools || [])
    .filter((tool) => isBusinessRoot(tool) && tool.children?.length)
    .map((root) => ({
      key: root.name,
      label: resolveToolbarMenuLabel(localeI18n, root.name),
      items: collectInsertableItems(root.children, [root], localeI18n)
    }))
    .filter((group) => group.items.length > 0)
}

/** Preferred tab order for business roots (defect before dynamic plan menus). */
const BUSINESS_GROUP_ORDER = ['project', 'case', 'defect', 'plan']

/**
 * @param {{ key: string, label: string, items: object[] }[]} groups
 */
export function sortBusinessMenuGroups(groups) {
  return [...(groups || [])].sort((a, b) => {
    const rank = (key) => {
      const index = BUSINESS_GROUP_ORDER.indexOf(key)
      return index === -1 ? BUSINESS_GROUP_ORDER.length : index
    }
    return rank(a.key) - rank(b.key)
  })
}
/**
 * @param {number|string|null} projectId
 */
export async function loadReportBusinessBlockMenuGroups(projectId) {
  const tools = await loadReportToolbarMenus(projectId)
  return sortBusinessMenuGroups(extractBusinessMenuGroups(tools))
}

/**
 * Append business component groups to Crepe block-edit menu.
 * @param {import('@milkdown/crepe').GroupBuilder} groupBuilder
 * @param {{ key: string, label: string, items: { key: string, label: string, content: string }[] }[]} groups
 * @param {(ctx: import('@milkdown/ctx').Ctx, content: string) => void} onInsert
 */
export function appendBusinessGroupsToBlockMenu(groupBuilder, groups, onInsert) {
  for (const group of groups || []) {
    const menuGroup = groupBuilder.addGroup(`business-${group.key}`, group.label)
    for (const item of group.items) {
      const content = item.content
      menuGroup.addItem(`business-${group.key}-${item.key}`, {
        label: item.label,
        icon: BUSINESS_BLOCK_MENU_ICON,
        onRun: (ctx) => onInsert(ctx, content)
      })
    }
  }
}
