import { defineAsyncComponent } from 'vue'
import { checkPermi } from '@/utils/permission'

const BoardChartHost = defineAsyncComponent(() => import('./widgets/BoardChartHost.vue'))
const MyKpi = defineAsyncComponent(() => import('./widgets/MyKpi.vue'))
const MyDefectTrend = defineAsyncComponent(() => import('./widgets/MyDefectTrend.vue'))
const MyWorkloadMix = defineAsyncComponent(() => import('./widgets/MyWorkloadMix.vue'))
const MyDefects = defineAsyncComponent(() => import('./widgets/MyDefects.vue'))
const DefectsPendingRepair = defineAsyncComponent(() => import('./widgets/DefectsPendingRepair.vue'))
const CasePendingReview = defineAsyncComponent(() => import('./widgets/CasePendingReview.vue'))
const QuickCreate = defineAsyncComponent(() => import('./widgets/QuickCreate.vue'))
const NoticesSummary = defineAsyncComponent(() => import('./widgets/NoticesSummary.vue'))
const RecentCommits = defineAsyncComponent(() => import('./widgets/RecentCommits.vue'))
const GitRepoStats = defineAsyncComponent(() => import('./widgets/GitRepoStats.vue'))
const OpenPullRequests = defineAsyncComponent(() => import('./widgets/OpenPullRequests.vue'))
const MyWeeklyReport = defineAsyncComponent(() => import('./widgets/MyWeeklyReport.vue'))
const TeamTypeKpi = defineAsyncComponent(() => import('./widgets/TeamTypeKpi.vue'))
const DocumentList = defineAsyncComponent(() => import('./widgets/DocumentList.vue'))
const DocumentPin = defineAsyncComponent(() => import('./widgets/DocumentPin.vue'))

/**
 * tabAffinity: 'work' | 'deferred' | 'both'
 * deferred = 原看板统计，保留组件供动态 Tab，不进工作台目录
 * catalogHidden / disabled / toolbar 见各字段
 * bare = 查看态不套卡片边框/底色（自带视觉）；标题栏仍显示
 * minSpan = 24 栅格下最小占格（空槽默认 2）；拖拽/比例下拉不可低于此值
 */
/** 空槽最小占格 */
export const EMPTY_COL_MIN_SPAN = 2
/** 未声明 minSpan 的组件默认最小占格 */
export const DEFAULT_WIDGET_MIN_SPAN = 6

export const WORKBENCH_WIDGETS = {
  'my-kpi': { id: 'my-kpi', tabAffinity: 'work', titleKey: 'workbench.widget.my-kpi', component: MyKpi, bare: true, minSpan: 8 },
  'team-type-kpi': { id: 'team-type-kpi', tabAffinity: 'work', titleKey: 'workbench.widget.team-type-kpi', component: TeamTypeKpi, bare: true, minSpan: 8 },
  'my-defect-trend': { id: 'my-defect-trend', tabAffinity: 'work', titleKey: 'workbench.widget.my-defect-trend', component: MyDefectTrend, minSpan: 8 },
  'team-defect-trend': {
    id: 'team-defect-trend',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.team-defect-trend',
    component: MyDefectTrend,
    props: { scope: 'team' },
    minSpan: 8
  },
  'my-workload-mix': { id: 'my-workload-mix', tabAffinity: 'work', titleKey: 'workbench.widget.my-workload-mix', component: MyWorkloadMix, minSpan: 8 },
  'team-workload-mix': {
    id: 'team-workload-mix',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.team-workload-mix',
    component: MyWorkloadMix,
    props: { scope: 'team' },
    minSpan: 8
  },
  'my-defects': { id: 'my-defects', tabAffinity: 'work', titleKey: 'workbench.widget.my-defects', component: MyDefects, bare: true, minSpan: 8 },
  'team-defects': {
    id: 'team-defects',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.team-defects',
    component: MyDefects,
    props: { scope: 'team' },
    bare: true,
    minSpan: 8
  },
  'defects-pending-repair': {
    id: 'defects-pending-repair',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.defects-pending-repair',
    component: DefectsPendingRepair,
    bare: true,
    minSpan: 8
  },
  'case-pending-review': {
    id: 'case-pending-review',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.case-pending-review',
    component: CasePendingReview,
    bare: true,
    minSpan: 6
  },
  'team-weekly-report': {
    id: 'team-weekly-report',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.team-weekly-report',
    component: MyWeeklyReport,
    props: { scope: 'team' },
    bare: true,
    minSpan: 8
  },
  'quick-create': {
    id: 'quick-create',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.quick-create',
    component: QuickCreate,
    toolbar: true,
    catalogHidden: true,
    minSpan: 4
  },
  'notices-summary': {
    id: 'notices-summary',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.notices-summary',
    component: NoticesSummary,
    minSpan: 4
  },
  'recent-commits': {
    id: 'recent-commits',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.recent-commits',
    component: RecentCommits,
    minSpan: 6
  },
  'git-repo-stats': {
    id: 'git-repo-stats',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.git-repo-stats',
    component: GitRepoStats,
    minSpan: 6
  },
  'open-pull-requests': {
    id: 'open-pull-requests',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.open-pull-requests',
    component: OpenPullRequests,
    minSpan: 6
  },
  'prs-to-review': {
    id: 'prs-to-review',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.open-pull-requests',
    component: OpenPullRequests,
    catalogHidden: true,
    minSpan: 6
  },
  'my-documents': {
    id: 'my-documents',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.my-documents',
    component: DocumentList,
    props: { scope: 'me' },
    bare: true,
    minSpan: 6
  },
  'document-list': {
    id: 'document-list',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.document-list',
    component: DocumentList,
    props: { scope: 'all' },
    bare: true,
    minSpan: 6
  },
  'document-pin': {
    id: 'document-pin',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.document-pin',
    component: DocumentPin,
    bare: true,
    minSpan: 6
  },
  'plan-burndown': {
    id: 'plan-burndown',
    tabAffinity: 'both',
    titleKey: 'workbench.widget.plan-burndown',
    component: BoardChartHost,
    boardChart: 'PlanBurndownChart',
    props: { embedded: true },
    minSpan: 8
  },

  // —— 项目级统计（原看板，现入组件库） ——
  'stat-case': {
    id: 'stat-case',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.stat-case',
    component: BoardChartHost,
    boardChart: 'CaseStatisticsChart'
  },
  'stat-defect': {
    id: 'stat-defect',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.stat-defect',
    component: BoardChartHost,
    boardChart: 'DefectStatisticsChart'
  },
  'stat-module': {
    id: 'stat-module',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.stat-module',
    component: BoardChartHost,
    boardChart: 'ModuleStatisticsChart'
  },
  'stat-document': {
    id: 'stat-document',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.stat-document',
    component: BoardChartHost,
    boardChart: 'DocumentStatisticsChart'
  },
  'stat-member': {
    id: 'stat-member',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.stat-member',
    component: BoardChartHost,
    boardChart: 'MemberStatisticsChart'
  },
  'defect-state-chart': {
    id: 'defect-state-chart',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.defect-state-chart',
    component: BoardChartHost,
    boardChart: 'DefectStateChart'
  },
  'member-defect-line': {
    id: 'member-defect-line',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.member-defect-line',
    component: BoardChartHost,
    boardChart: 'MemberOfDefectLine'
  },
  'plan-statistics': {
    id: 'plan-statistics',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.plan-statistics',
    component: BoardChartHost,
    boardChart: 'PlanStatisticsChart'
  },
  'action-list': {
    id: 'action-list',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.action-list',
    component: BoardChartHost,
    boardChart: 'ActionList'
  },
  'member-defect-rank': {
    id: 'member-defect-rank',
    tabAffinity: 'work',
    titleKey: 'workbench.widget.member-defect-rank',
    component: BoardChartHost,
    boardChart: 'MemberOfDefectRank'
  }
}

export function getWidgetDef(id) {
  return WORKBENCH_WIDGETS[id] || null
}

/** 组件在 24 栅格中的最小占格 */
export function getWidgetMinSpan(type) {
  if (!type) return EMPTY_COL_MIN_SPAN
  const def = getWidgetDef(type)
  const n = Number(def?.minSpan)
  if (Number.isFinite(n) && n > 0) return Math.min(24, Math.max(EMPTY_COL_MIN_SPAN, Math.round(n)))
  return DEFAULT_WIDGET_MIN_SPAN
}

/** 一行各列的最小占格（按已放组件） */
export function colMinSpans(cols) {
  return (cols || []).map(c => {
    const list = Array.isArray(c?.widgets) && c.widgets.length
      ? c.widgets
      : (c?.widget ? [c.widget] : [])
    if (!list.length) return getWidgetMinSpan(null)
    return Math.max(...list.map(w => getWidgetMinSpan(w?.type)))
  })
}

function widgetAllowed(def) {
  if (!def || def.disabled) return false
  if (def.perms && def.perms.length && !checkPermi(def.perms)) return false
  return true
}

/** 看板顶部项目 KPI 墙（与走势类图表分区） */
export const BOARD_STAT_IDS = new Set([
  'stat-defect',
  'stat-case',
  'stat-module',
  'stat-document',
  'stat-member'
])

/** 工作台编辑器组件库 */
export function listCatalogWidgets() {
  return listWidgetsForTab('work')
}

/** 按页签列出可配置组件 */
export function listWidgetsForTab(tab) {
  const want = tab === 'board' ? 'board' : 'work'
  return Object.values(WORKBENCH_WIDGETS).filter(w => {
    if (!widgetAllowed(w)) return false
    if (w.catalogHidden || w.toolbar) return false
    if (w.tabAffinity === 'deferred') return false
    return w.tabAffinity === want || w.tabAffinity === 'both'
  })
}

/** 过滤非法 / 无权限的组件 id */
export function resolveWidgetIds(ids, tab) {
  const allowed = new Set(listWidgetsForTab(tab).map(w => w.id))
  // plan-burndown 等 both 已在 allowed；看板配置里的已知 id 即使曾被隐藏也放行已保存项
  return (ids || []).filter(id => {
    if (allowed.has(id)) return true
    const def = getWidgetDef(id)
    if (!def || !widgetAllowed(def)) return false
    if (tab === 'board') return def.tabAffinity === 'board' || def.tabAffinity === 'both'
    return def.tabAffinity === 'work' || def.tabAffinity === 'both'
  })
}

export function hasToolbarQuickCreate() {
  return true
}
