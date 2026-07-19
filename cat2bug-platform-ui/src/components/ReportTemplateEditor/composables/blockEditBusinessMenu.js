import i18n from '@/utils/i18n/i18n'
import { resolveToolbarMenuLabel } from '@/components/Cat2BugMarkdown/resolveToolbarMenuLabel'
import { loadReportToolbarMenus } from './toolbarMenuLoader'
import { resolveToolContent } from './useReportTemplateTools'

/** Toolbar roots that map to report DSL business inserts. */
const BUSINESS_ROOT_NAMES = new Set(['project', 'case', 'defect', 'plan'])

/**
 * Toolbar actions that are not "insert element" — excluded from the left + menu.
 */
const NON_INSERT_TOOL_NAMES = new Set([
  'import-markdown',
  'save',
  'clear',
  'variables-expand-collapse'
])

/**
 * Labels for toolbar segments that are not business roots (split by siding).
 * Matches historical Crepe tabs: 文本 → 高级.
 */
const ANON_SEGMENT_LABELS = ['文本', '高级']

/** Simple document icon for Crepe block menu (SVG string). */
export const BUSINESS_BLOCK_MENU_ICON =
  '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 2l5 5h-5V4zM8 13h8v2H8v-2zm0 4h5v2H8v-2z"/></svg>'

function isBusinessRoot(tool) {
  return tool?.name && BUSINESS_ROOT_NAMES.has(tool.name)
}

function isPlanDisplayName(name) {
  return name && !BUSINESS_ROOT_NAMES.has(name) && !name.includes('.')
}

function isNonInsertTool(tool) {
  if (!tool) {
    return true
  }
  if (tool.type === 'command' || tool.type === 'check') {
    return true
  }
  if (tool.name && NON_INSERT_TOOL_NAMES.has(tool.name)) {
    return true
  }
  return false
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
 * Flatten non-business toolbar tools into insertable add-menu items (toolbar order).
 * Component dialogs (image upload) are skipped; image → image-block, link → link dialog.
 *
 * @param {object[]} tools
 * @param {import('vue-i18n').Composer} localeI18n
 * @returns {{ key: string, label: string, content?: string, insertMode?: string }[]}
 */
function collectSegmentInsertItems(tools, localeI18n) {
  /** @type {{ key: string, label: string, content?: string, insertMode?: string }[]} */
  const items = []
  for (const tool of tools || []) {
    if (isNonInsertTool(tool) || tool.type === 'siding') {
      continue
    }
    if (isBusinessRoot(tool)) {
      continue
    }
    // Toolbar image submenu is component-only — map to Crepe image block.
    if (tool.name === 'image') {
      items.push({
        key: 'image',
        label: resolveToolbarMenuLabel(localeI18n, 'image') || '图片',
        insertMode: 'image-block'
      })
      continue
    }
    // Link dialog component → open shared link insert dialog from the add menu.
    if (tool.name === 'link' || (tool.type === 'component' && tool.name === 'link')) {
      items.push({
        key: 'link',
        label: resolveToolbarMenuLabel(localeI18n, 'link') || '链接',
        insertMode: 'link'
      })
      continue
    }
    if (tool.type === 'component') {
      continue
    }
    if (tool.children?.length) {
      for (const child of tool.children) {
        if (child.type === 'siding' || isNonInsertTool(child) || child.type === 'component') {
          continue
        }
        if (child.children?.length) {
          collectInsertableItems(child.children, [tool, child], localeI18n, items)
          continue
        }
        const content = resolveToolContent(child)
        if (!content) {
          continue
        }
        items.push({
          key: child.name,
          label: resolveToolbarMenuLabel(localeI18n, child.name),
          content
        })
      }
      continue
    }
    const content = resolveToolContent(tool)
    if (!content) {
      continue
    }
    items.push({
      key: tool.name,
      label: resolveToolbarMenuLabel(localeI18n, tool.name),
      content
    })
  }
  return items
}

/**
 * Split toolbar tools into segments by `siding`, dropping non-insert top-level actions.
 * @param {object[]} toolbarTools
 * @returns {object[][]}
 */
export function splitToolbarToolsBySiding(toolbarTools) {
  /** @type {object[][]} */
  const segments = []
  /** @type {object[]} */
  let current = []
  for (const tool of toolbarTools || []) {
    if (tool.type === 'siding') {
      if (current.length) {
        segments.push(current)
        current = []
      }
      continue
    }
    if (isNonInsertTool(tool)) {
      continue
    }
    current.push(tool)
  }
  if (current.length) {
    segments.push(current)
  }
  return segments
}

/**
 * Build left-+ add menu groups from the top toolbar (same content & order).
 * Excludes import / save / variables-expand and other non-insert actions.
 *
 * @param {object[]} toolbarTools
 * @param {import('vue-i18n').Composer} [localeI18n]
 * @returns {{ key: string, label: string, items: object[] }[]}
 */
export function extractAddMenuGroupsFromToolbar(toolbarTools, localeI18n = i18n) {
  const segments = splitToolbarToolsBySiding(toolbarTools)
  /** @type {{ key: string, label: string, items: object[] }[]} */
  const groups = []
  let anonSegmentIndex = 0

  for (let segmentIndex = 0; segmentIndex < segments.length; segmentIndex += 1) {
    const segment = segments[segmentIndex]
    const segmentItems = collectSegmentInsertItems(segment, localeI18n)
    if (segmentItems.length) {
      const label = ANON_SEGMENT_LABELS[anonSegmentIndex]
        || `分组${anonSegmentIndex + 1}`
      groups.push({
        key: `toolbar-segment-${anonSegmentIndex}`,
        label,
        items: segmentItems
      })
      anonSegmentIndex += 1
    }

    // Business roots keep their own tabs, in toolbar order within the segment.
    for (const tool of segment) {
      if (!isBusinessRoot(tool) || !tool.children?.length) {
        continue
      }
      const items = collectInsertableItems(tool.children, [tool], localeI18n)
      if (!items.length) {
        continue
      }
      groups.push({
        key: tool.name,
        label: resolveToolbarMenuLabel(localeI18n, tool.name),
        items
      })
    }
  }

  return groups
}

/**
 * Extract block-menu groups mirroring toolbar business sections only.
 * @param {object[]} toolbarTools
 * @param {import('vue-i18n').Composer} [localeI18n]
 */
export function extractBusinessMenuGroups(toolbarTools, localeI18n = i18n) {
  return extractAddMenuGroupsFromToolbar(toolbarTools, localeI18n).filter((group) =>
    BUSINESS_ROOT_NAMES.has(group.key)
  )
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
  return extractBusinessMenuGroups(tools)
}

/**
 * Full left-+ menu groups aligned with the top toolbar.
 * @param {number|string|null} projectId
 */
export async function loadReportAddMenuGroups(projectId) {
  const tools = await loadReportToolbarMenus(projectId)
  return extractAddMenuGroupsFromToolbar(tools)
}

/**
 * Append business component groups to Crepe block-edit menu.
 * @param {import('@milkdown/crepe').GroupBuilder} groupBuilder
 * @param {{ key: string, label: string, items: { key: string, label: string, content: string }[] }[]} groups
 * @param {(ctx: import('@milkdown/ctx').Ctx, content: string) => void} onInsert
 */
export function appendBusinessGroupsToBlockMenu(groupBuilder, groups, onInsert) {
  for (const group of groups || []) {
    if (!BUSINESS_ROOT_NAMES.has(group.key)) {
      continue
    }
    const menuGroup = groupBuilder.addGroup(`business-${group.key}`, group.label)
    for (const item of group.items) {
      if (!item.content) {
        continue
      }
      const content = item.content
      menuGroup.addItem(`business-${group.key}-${item.key}`, {
        label: item.label,
        icon: BUSINESS_BLOCK_MENU_ICON,
        onRun: (ctx) => onInsert(ctx, content)
      })
    }
  }
}
