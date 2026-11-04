import { parseProjectScopedPath } from '@/utils/project-module-path'

export const DEFAULT_SYSTEM_MAX_UPLOAD_MB = 500

/** 项目路径段 → 团队开关键。case / plan 共用 case_plan。 */
export const SEGMENT_TO_TEAM_MODULE = Object.freeze({
  case: 'case_plan',
  plan: 'case_plan',
  defect: 'defect',
  module: 'deliverable',
  document: 'document',
  code: 'code',
  'git-repo': 'code'
})

/** MCP 模块键 → 团队开关键。未列出的键不受团队闸门约束。 */
export const MCP_KEY_TO_TEAM_MODULE = Object.freeze({
  case: 'case_plan',
  plan: 'case_plan',
  defect: 'defect',
  deliverable: 'deliverable',
  document: 'document',
  code: 'code'
})

const NOUNS = Object.freeze({
  zh_CN: [
    { from: '测试用例', field: 'labelCase' },
    { from: '用例', field: 'labelCase' },
    { from: '测试计划', field: 'labelPlan' },
    { from: '交付物', field: 'deliverable' },
    { from: '缺陷', field: 'defect' },
    { from: '文档', field: 'document' }
  ],
  zh_TW: [
    { from: '測試用例', field: 'labelCase' },
    { from: '用例', field: 'labelCase' },
    { from: '測試計劃', field: 'labelPlan' },
    { from: '交付物', field: 'deliverable' },
    { from: '缺陷', field: 'defect' },
    { from: '文檔', field: 'document' },
    { from: '文档', field: 'document' }
  ],
  en_US: [
    { from: 'Test Case', field: 'labelCase' },
    { from: 'Test Plan', field: 'labelPlan' },
    { from: 'Deliverable', field: 'deliverable' },
    { from: 'Defect', field: 'defect' },
    { from: 'defect', field: 'defect' },
    { from: 'Document', field: 'document' }
  ],
  ar: [
    { from: 'حالة اختبار', field: 'labelCase' },
    { from: 'خطة اختبار', field: 'labelPlan' },
    { from: 'وحدة التسليم', field: 'deliverable' },
    { from: 'خلل', field: 'defect' },
    { from: 'إدارة الوثائق', field: 'document' },
    { from: 'الوثائق', field: 'document' }
  ],
  ja_JP: [
    { from: 'テストケース', field: 'labelCase' },
    { from: 'テストプラン', field: 'labelPlan' },
    { from: 'デリバラブル', field: 'deliverable' },
    { from: '不具合', field: 'defect' },
    { from: 'ドキュメント管理', field: 'document' },
    { from: 'ドキュメント', field: 'document' }
  ],
  ko_KR: [
    { from: '테스트 케이스', field: 'labelCase' },
    { from: '테스트 계획', field: 'labelPlan' },
    { from: '전달물', field: 'deliverable' },
    { from: '결함', field: 'defect' },
    { from: '문서 관리', field: 'document' },
    { from: '문서', field: 'document' }
  ],
  ru: [
    { from: 'Тест-кейс', field: 'labelCase' },
    { from: 'Тест-план', field: 'labelPlan' },
    { from: 'Модуль', field: 'deliverable' },
    { from: 'Дефект', field: 'defect' },
    { from: 'Управление документами', field: 'document' },
    { from: 'Документ', field: 'document' }
  ]
})

function emptyToNull(value) {
  if (value == null) return null
  const text = String(value).trim()
  return text ? text : null
}

function positiveIntOrNull(value) {
  if (value == null || value === '') return null
  const n = Number(value)
  if (!Number.isFinite(n) || n <= 0) return null
  return Math.round(n)
}

function readBool(value, fallback) {
  return typeof value === 'boolean' ? value : fallback
}

function sectionOf(raw, key, altKey) {
  if (!raw || typeof raw !== 'object') return null
  if (raw[key] && typeof raw[key] === 'object') return raw[key]
  if (altKey && raw[altKey] && typeof raw[altKey] === 'object') return raw[altKey]
  return null
}

export function defaultModulePageSettings() {
  return {
    case_plan: { enabled: true, labelCase: null, labelPlan: null },
    defect: { enabled: true, label: null },
    deliverable: { enabled: true, label: null },
    document: { enabled: true, label: null, maxUploadMb: null },
    code: { enabled: true, label: null },
    systemMaxUploadMb: DEFAULT_SYSTEM_MAX_UPLOAD_MB
  }
}

export function readSystemMaxUploadMb(raw) {
  const doc = sectionOf(raw, 'document') || {}
  const candidates = [
    raw && raw.systemMaxUploadMb,
    raw && raw.systemMax,
    raw && raw.systemMaxMb,
    doc.systemMaxUploadMb,
    doc.systemMax,
    doc.systemMaxMb
  ]
  for (const item of candidates) {
    const n = positiveIntOrNull(item)
    if (n) return n
  }
  return DEFAULT_SYSTEM_MAX_UPLOAD_MB
}

export function mergeModulePageSettings(raw) {
  const base = defaultModulePageSettings()
  const casePlan = sectionOf(raw, 'case_plan', 'casePlan') || {}
  const defect = sectionOf(raw, 'defect') || {}
  const deliverable = sectionOf(raw, 'deliverable') || {}
  const document = sectionOf(raw, 'document') || {}
  const code = sectionOf(raw, 'code') || {}
  return {
    case_plan: {
      enabled: readBool(casePlan.enabled, true),
      labelCase: emptyToNull(casePlan.labelCase),
      labelPlan: emptyToNull(casePlan.labelPlan)
    },
    defect: {
      enabled: readBool(defect.enabled, true),
      label: emptyToNull(defect.label)
    },
    deliverable: {
      enabled: readBool(deliverable.enabled, true),
      label: emptyToNull(deliverable.label)
    },
    document: {
      enabled: readBool(document.enabled, true),
      label: emptyToNull(document.label),
      maxUploadMb: positiveIntOrNull(document.maxUploadMb)
    },
    code: {
      enabled: readBool(code.enabled, true),
      label: emptyToNull(code.label)
    },
    systemMaxUploadMb: readSystemMaxUploadMb(raw)
  }
}

/** 写回团队接口的正文，不含只读的系统上限。 */
export function toModulePageSettingsPayload(settings) {
  const s = mergeModulePageSettings(settings)
  return {
    case_plan: {
      enabled: s.case_plan.enabled,
      labelCase: s.case_plan.labelCase,
      labelPlan: s.case_plan.labelPlan
    },
    defect: { enabled: s.defect.enabled, label: s.defect.label },
    deliverable: { enabled: s.deliverable.enabled, label: s.deliverable.label },
    document: {
      enabled: s.document.enabled,
      label: s.document.label,
      maxUploadMb: s.document.maxUploadMb
    },
    code: { enabled: s.code.enabled, label: s.code.label }
  }
}

export function isTeamModuleEnabled(settings, key) {
  if (!key) return true
  const s = mergeModulePageSettings(settings)
  const block = s[key]
  if (!block) return true
  return block.enabled !== false
}

export function effectiveDocumentUploadMb(settings) {
  const s = mergeModulePageSettings(settings)
  return s.document.maxUploadMb || s.systemMaxUploadMb || DEFAULT_SYSTEM_MAX_UPLOAD_MB
}

/**
 * 显示名。which: case | plan | defect | deliverable | document | code
 * 空别名回退 fallback（通常是默认 i18n）。
 */
export function teamModuleLabel(settings, which, fallback) {
  const s = mergeModulePageSettings(settings)
  let label = null
  if (which === 'case') label = s.case_plan.labelCase
  else if (which === 'plan') label = s.case_plan.labelPlan
  else if (s[which]) label = s[which].label
  return label || fallback
}

function labelForField(settings, field) {
  const s = mergeModulePageSettings(settings)
  if (field === 'labelCase') return s.case_plan.labelCase
  if (field === 'labelPlan') return s.case_plan.labelPlan
  if (field === 'defect') return s.defect.label
  if (field === 'deliverable') return s.deliverable.label
  if (field === 'document') return s.document.label
  return null
}

function replaceNouns(value, settings, locale) {
  const rules = NOUNS[locale] || []
  const slots = []
  let out = value
  rules.forEach((rule) => {
    const label = labelForField(settings, rule.field)
    if (!label || label === rule.from || out.indexOf(rule.from) < 0) return
    const token = `\u0000${slots.length}\u0000`
    slots.push(label)
    out = out.split(rule.from).join(token)
  })
  slots.forEach((label, index) => {
    out = out.split(`\u0000${index}\u0000`).join(label)
  })
  return out
}

function walkMessages(node, settings, locale) {
  if (!node || typeof node !== 'object') return
  Object.keys(node).forEach((key) => {
    const value = node[key]
    if (typeof value === 'string') {
      node[key] = replaceNouns(value, settings, locale)
    } else if (value && typeof value === 'object') {
      walkMessages(value, settings, locale)
    }
  })
}

function forceRootKeys(messages, settings) {
  const s = mergeModulePageSettings(settings)
  if (s.case_plan.labelCase) messages.case = s.case_plan.labelCase
  if (s.case_plan.labelPlan) messages.plan = s.case_plan.labelPlan
  if (s.defect.label) {
    messages.defect = s.defect.label
    messages['defect.manage'] = s.defect.label
  }
  if (s.deliverable.label) messages.module = s.deliverable.label
  if (s.document.label) messages.doc = s.document.label
  if (s.code.label) {
    messages.code = s.code.label
    messages['git.repo.menu'] = s.code.label
  }
}

/**
 * 用团队别名覆盖根键，并替换文案中的默认模块名。
 * 不修改入参。
 */
export function buildLabelOverrideMessages(localeMessages, settings, locale) {
  const next = JSON.parse(JSON.stringify(localeMessages || {}))
  walkMessages(next, settings, locale)
  forceRootKeys(next, settings)
  return next
}

export function isTeamSettingsPath(path) {
  const value = String(path || '')
  return value.startsWith('/team-options')
    || value.startsWith('/team/')
    || value === '/team'
}

export function teamModuleKeyFromLocation(route) {
  const path = route && (route.path || route.fullPath) ? String(route.path || route.fullPath) : ''
  const pathname = path.split('?')[0]
  if (isTeamSettingsPath(pathname)) return null
  const metaModule = route && route.meta && route.meta.projectModule
  if (metaModule && SEGMENT_TO_TEAM_MODULE[metaModule]) {
    return SEGMENT_TO_TEAM_MODULE[metaModule]
  }
  const parsed = parseProjectScopedPath(pathname)
  if (parsed && parsed.module && SEGMENT_TO_TEAM_MODULE[parsed.module]) {
    return SEGMENT_TO_TEAM_MODULE[parsed.module]
  }
  const legacy = pathname.match(/^\/project\/(defect|case|plan|document|module|code|git-repo)(\/|$)/)
  if (legacy) return SEGMENT_TO_TEAM_MODULE[legacy[1]]
  const bare = pathname.match(/^\/(defect|case|plan|document|module|code)(\/|$)/)
  if (bare) return SEGMENT_TO_TEAM_MODULE[bare[1]]
  const parts = pathname.split('/').filter(Boolean)
  if (parts.length === 1 && SEGMENT_TO_TEAM_MODULE[parts[0]]) {
    return SEGMENT_TO_TEAM_MODULE[parts[0]]
  }
  return null
}

export function teamModuleFallbackPath(route) {
  const path = route && route.path ? String(route.path) : ''
  const parsed = parseProjectScopedPath(path.split('?')[0])
  if (parsed && parsed.teamKey && parsed.projectKey) {
    return `/${parsed.teamKey}/${parsed.projectKey}/dashboard`
  }
  return '/team/project-list'
}

export function filterMenuRoutesByTeamModules(routes, settings) {
  if (!Array.isArray(routes)) return []
  const out = []
  routes.forEach((route) => {
    if (!route) return
    if (route.hidden || isTeamSettingsPath(route.path)) {
      out.push(route)
      return
    }
    const children = Array.isArray(route.children)
      ? filterMenuRoutesByTeamModules(route.children, settings)
      : null
    const key = teamModuleKeyFromLocation(route)
    if (key && !isTeamModuleEnabled(settings, key)) return
    if (children && route.children && route.children.length && children.length === 0) {
      return
    }
    out.push(children ? { ...route, children } : route)
  })
  return out
}

/** 缺陷表单 / 筛选里要藏起来的内置 fieldKey。 */
export function hiddenDefectBuiltinFormKeys(settings) {
  const hidden = []
  if (!isTeamModuleEnabled(settings, 'case_plan')) hidden.push('caseId')
  if (!isTeamModuleEnabled(settings, 'deliverable')) hidden.push('moduleId')
  return hidden
}

export function omitTeamGatedDefectColumns(columns, settings) {
  const hideCase = !isTeamModuleEnabled(settings, 'case_plan')
  const hideDeliverable = !isTeamModuleEnabled(settings, 'deliverable')
  if (!hideCase && !hideDeliverable) return columns || []
  return (columns || []).filter((col) => {
    if (!col) return false
    if (hideCase && (col.key === 'case' || col.prop === 'caseId')) return false
    if (
      hideDeliverable &&
      (col.key === 'module' || col.prop === 'moduleId' || col.key === 'moduleId')
    ) {
      return false
    }
    return true
  })
}

export function mcpKeyTeamModule(mcpKey) {
  return MCP_KEY_TO_TEAM_MODULE[mcpKey] || null
}

export function isMcpKeyClosedByTeam(settings, mcpKey) {
  const teamKey = mcpKeyTeamModule(mcpKey)
  if (!teamKey) return false
  return !isTeamModuleEnabled(settings, teamKey)
}
