/** 与菜单 component 字段一致 */
export const PROJECT_LIST_PAGE_VIEWS = [
  'system/case/index',
  'system/defect/index',
  'system/plan/index',
  'system/report/index',
  'system/document/index'
]

const PROJECT_LIST_PAGE_VIEW_SET = new Set(PROJECT_LIST_PAGE_VIEWS)

const PROJECT_LIST_ROUTE_NAMES = new Set([
  'Case',
  'Defect',
  'Plan',
  'Report',
  'Document'
])

/** 路由 path 末段（动态菜单 path 多为 case / defect 等） */
const PROJECT_LIST_PATH_TAIL = /\/(case|defect|plan|report|document)(?:\/|$)/

/**
 * 是否为用例/缺陷/计划/报告/文档列表页（用于布局级 overflow，须在组件 mount 前判定）
 * @param {import('vue-router').Route} route
 */
export function isProjectListPageRoute(route) {
  if (!route) return false
  if (route.meta && route.meta.projectListPage) return true
  if (route.name && PROJECT_LIST_ROUTE_NAMES.has(route.name)) return true
  if (route.matched && route.matched.some((r) => r.meta && r.meta.projectListPage)) {
    return true
  }
  if (PROJECT_LIST_PATH_TAIL.test(route.path || '')) return true
  return false
}

export function isProjectListPageView(component) {
  return typeof component === 'string' && PROJECT_LIST_PAGE_VIEW_SET.has(component)
}
