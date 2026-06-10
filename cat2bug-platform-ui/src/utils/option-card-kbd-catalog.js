/**
 * 项目/团队设置枢纽页卡片入口目录（静态，供键盘设置与运行时 lookup 共用）。
 * defaultLetter 按 catalog 顺序分配（1–9 优先），并预留子页 B/U/P/I。
 */

/** 项目设置枢纽页卡片入口（与卡片组件 DOM 顺序一致） */
export const PROJECT_OPTION_CARD_CATALOG = [
  { key: 'route-project-base-info', titleKey: 'project.base-info', defaultLetter: '1' },
  { key: 'route-project-api', titleKey: 'project.api-key', defaultLetter: '2' },
  { key: 'route-push', titleKey: 'project.push', defaultLetter: '3' },
  { key: 'delete-project', titleKey: 'project.delete', defaultLetter: '4' },
  { key: 'route-ollama', titleKey: 'project.ai-model-manager', defaultLetter: '5' },
  { key: 'route-openai', titleKey: 'project.ai-account-manager', defaultLetter: '6' },
  { key: 'route-defect-fields', titleKey: 'defect.custom-field.manage', defaultLetter: '7' },
  { key: 'route-ding', titleKey: 'ding', defaultLetter: '8' },
  { key: 'route-feishu', titleKey: 'feishu', defaultLetter: '9' },
  { key: 'route-enterprise-wechat', titleKey: 'enterprise-wechat', defaultLetter: 'E' },
  { key: 'route-project-member', titleKey: 'member.manage', defaultLetter: 'F' }
]

/** 团队设置枢纽页卡片入口 */
export const TEAM_OPTION_CARD_CATALOG = [
  { key: 'route-team-base-info', titleKey: 'team.base-info', defaultLetter: '1' },
  { key: 'delete-team', titleKey: 'team.delete', defaultLetter: '2' },
  { key: 'route-team-member', titleKey: 'member.manage', defaultLetter: '3' }
]

export const PROJECT_OPTION_SUB_ACTION_DEFAULTS = [
  { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.project-option-back' },
  { key: 'changeIcon', defaultLetter: 'I', titleKey: 'keyboard.act.project-change-icon' },
  { key: 'prevPage', defaultLetter: 'U', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

/** 项目设置枢纽页（卡片内可见入口，供空格面板 / ⌘ 徽标 / 键盘设置） */
export const PROJECT_OPTION_HUB_ACTION_DEFAULTS = [...PROJECT_OPTION_CARD_CATALOG]

/** 团队设置枢纽页（卡片内可见入口） */
export const TEAM_OPTION_HUB_ACTION_DEFAULTS = [...TEAM_OPTION_CARD_CATALOG]

/** 项目设置 scope 全量默认（含子页，供运行时 titleKey 解析） */
export const PROJECT_OPTION_ACTION_DEFAULTS = [
  ...PROJECT_OPTION_CARD_CATALOG,
  ...PROJECT_OPTION_SUB_ACTION_DEFAULTS
]

export const TEAM_OPTION_ACTION_DEFAULTS = [
  ...TEAM_OPTION_CARD_CATALOG,
  { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.team-option-back' }
]

const CATALOG_BY_SCOPE = {
  'project-option': PROJECT_OPTION_CARD_CATALOG,
  'team-option': TEAM_OPTION_CARD_CATALOG
}

export function lookupOptionCardCatalogEntry(key, scopeKey) {
  const catalog = CATALOG_BY_SCOPE[scopeKey]
  if (!catalog) return null
  return catalog.find((item) => item.key === key) || null
}

export function getOptionCardCatalogDefaultLetter(scopeKey, key) {
  const entry = lookupOptionCardCatalogEntry(key, scopeKey)
  return entry && entry.defaultLetter ? entry.defaultLetter : ''
}
