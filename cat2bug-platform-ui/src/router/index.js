import { createRouter, createWebHistory } from 'vue-router'
import i18n from '@/utils/i18n/i18n.js'
import { isPathKeyFormatValid, isPathKeyReserved } from '@/utils/path-key.js'
import {
  LEGACY_PROJECT_MODULE_REDIRECTS,
  PROJECT_MODULES,
  PROJECT_MODULE_VIEW
} from '@/utils/project-module-path.js'
import {
  attachGitRepoIdAndRequireManage,
  attachGitRepoIdFromSlug,
  createLegacyGitBrowseRedirect
} from '@/utils/git-repo-route-guards.js'
import { createLegacyProjectModuleRedirect } from '@/utils/legacy-project-module-redirect.js'

/* Layout */
import Layout from '@/layout'

const projectDashboardView = () => import('@/views/system/dashboard/index')
const projectModulePlaceholder = () => import('@/views/system/project/module-placeholder')
const gitRepoBrowseView = () => import('@/views/system/git/repo/browse')
const gitRepoBranchesView = () => import('@/views/system/git/repo/branches')
const gitRepoTagsView = () => import('@/views/system/git/repo/tags')
const gitRepoPullsView = () => import('@/views/system/git/repo/pulls')
const gitRepoPullNewView = () => import('@/views/system/git/repo/pull-new')
const gitRepoPullDetailView = () => import('@/views/system/git/repo/pull-detail')
const gitRepoSettingsLayout = () => import('@/views/system/git/repo/settings/SettingsLayout')
const gitRepoSettingsGeneralView = () => import('@/views/system/git/repo/settings/general')
const gitRepoSettingsDangerView = () => import('@/views/system/git/repo/settings/danger')
const gitRepoSettingsMembersView = () => import('@/views/system/git/repo/settings/members')
const gitRepoSettingsWebhooksView = () => import('@/views/system/git/repo/settings/webhooks')

/** Lazy view loader for namespaced whitelist modules (paths from PROJECT_MODULE_VIEW). */
const PROJECT_MODULE_LAZY_VIEWS = Object.freeze({
  dashboard: projectDashboardView,
  defect: () => import('@/views/system/defect/index'),
  case: () => import('@/views/system/case/index'),
  plan: () => import('@/views/system/plan/index'),
  report: () => import('@/views/system/report/index'),
  document: () => import('@/views/system/document/index'),
  module: () => import('@/views/system/module/index'),
  notice: () => import('@/views/notice/index'),
  member: () => import('@/views/system/project/member/index'),
  option: () => import('@/views/system/project/option/index'),
  api: () => import('@/views/system/api/index'),
  tools: () => import('@/views/tool/project/index')
})

/**
 * Real `code` list + `code/{slug}/**` browse tree (replaces module-placeholder for code).
 */
function buildGitCodeChildren() {
  const codeMeta = {
    projectScoped: true,
    projectModule: 'code',
    projectListPage: true
  }
  const breadcrumbBase = [
    { path: '/project', redirect: 'noRedirect', meta: { titleI18nKey: 'project.manage' } },
    { path: 'code', meta: { titleI18nKey: 'git.repo.menu' } }
  ]
  const browseCrumb = {
    name: 'GitRepoBrowse',
    meta: { titleI18nKey: 'git.repo.library' }
  }

  return [
    {
      path: 'code',
      name: 'GitRepoList',
      component: () => import('@/views/system/git/repo/index'),
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.menu'),
        titleI18nKey: 'git.repo.menu'
      }
    },
    {
      path: 'code/:slug',
      name: 'GitRepoBrowse',
      component: gitRepoBrowseView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.library'),
        titleI18nKey: 'git.repo.library',
        breadcrumbParents: breadcrumbBase
      }
    },
    {
      path: 'code/:slug/branches',
      name: 'GitRepoBranches',
      component: gitRepoBranchesView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.branches.title'),
        titleI18nKey: 'git.repo.branches.title',
        breadcrumbParents: [...breadcrumbBase, browseCrumb]
      }
    },
    {
      path: 'code/:slug/tags',
      name: 'GitRepoTags',
      component: gitRepoTagsView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.tags.title'),
        titleI18nKey: 'git.repo.tags.title',
        breadcrumbParents: [...breadcrumbBase, browseCrumb]
      }
    },
    {
      path: 'code/:slug/pulls',
      name: 'GitRepoPullRequests',
      component: gitRepoPullsView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.pr.title'),
        titleI18nKey: 'git.repo.pr.title',
        breadcrumbParents: [...breadcrumbBase, browseCrumb]
      }
    },
    {
      path: 'code/:slug/pulls/new',
      name: 'GitRepoPullRequestNew',
      component: gitRepoPullNewView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.pr.new'),
        titleI18nKey: 'git.repo.pr.new',
        breadcrumbParents: [...breadcrumbBase, browseCrumb]
      }
    },
    {
      path: 'code/:slug/pulls/:number(\\d+)',
      name: 'GitRepoPullRequestDetail',
      component: gitRepoPullDetailView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.pr.detail-title'),
        titleI18nKey: 'git.repo.pr.detail-title',
        breadcrumbParents: [...breadcrumbBase, browseCrumb]
      }
    },
    {
      path: 'code/:slug/releases/:releaseId(\\d+)',
      name: 'GitRepoReleaseDetail',
      component: gitRepoBrowseView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.release.detail-title'),
        titleI18nKey: 'git.repo.release.detail-title',
        breadcrumbParents: [...breadcrumbBase, browseCrumb]
      }
    },
    {
      path: 'code/:slug/commits/:commitId',
      name: 'GitRepoCommitDetail',
      component: gitRepoBrowseView,
      beforeEnter: attachGitRepoIdFromSlug,
      meta: {
        ...codeMeta,
        title: i18n.global.t('git.repo.commit.detail-title'),
        titleI18nKey: 'git.repo.commit.detail-title',
        breadcrumbParents: [...breadcrumbBase, browseCrumb]
      }
    },
    {
      path: 'code/:slug/settings',
      name: 'GitRepoSettings',
      component: gitRepoSettingsLayout,
      beforeEnter: attachGitRepoIdAndRequireManage,
      redirect: { name: 'GitRepoSettingsGeneral' },
      meta: {
        ...codeMeta,
        breadcrumb: false,
        // Documented for parity with dynamicRoutes; enforced via beforeEnter (constant route).
        permissions: ['system:git:repo:manage']
      },
      children: [
        {
          path: 'general',
          name: 'GitRepoSettingsGeneral',
          component: gitRepoSettingsGeneralView,
          meta: {
            ...codeMeta,
            title: i18n.global.t('git.repo.settings'),
            titleI18nKey: 'git.repo.settings',
            breadcrumbParents: [...breadcrumbBase, browseCrumb],
            permissions: ['system:git:repo:manage']
          }
        },
        {
          path: 'danger',
          name: 'GitRepoSettingsDanger',
          component: gitRepoSettingsDangerView,
          meta: {
            ...codeMeta,
            title: i18n.global.t('git.repo.settings.danger'),
            titleI18nKey: 'git.repo.settings.danger',
            breadcrumbParents: [...breadcrumbBase, browseCrumb],
            permissions: ['system:git:repo:manage']
          }
        },
        {
          path: 'members',
          name: 'GitRepoSettingsMembers',
          component: gitRepoSettingsMembersView,
          meta: {
            ...codeMeta,
            title: i18n.global.t('git.repo.settings.nav.members'),
            titleI18nKey: 'git.repo.settings.nav.members',
            breadcrumbParents: [...breadcrumbBase, browseCrumb],
            permissions: ['system:git:repo:manage']
          }
        },
        {
          path: 'webhooks',
          name: 'GitRepoSettingsWebhooks',
          component: gitRepoSettingsWebhooksView,
          meta: {
            ...codeMeta,
            title: i18n.global.t('git.repo.settings.nav.webhooks'),
            titleI18nKey: 'git.repo.settings.nav.webhooks',
            breadcrumbParents: [...breadcrumbBase, browseCrumb],
            permissions: ['system:git:repo:manage']
          }
        }
      ]
    }
  ]
}

/**
 * Children under `/:teamKey/:projectKey`:
 * - `` and `dashboard` → real project dashboard (URL stays namespaced)
 * - `code` → repo list; `code/{slug}/**` → git browse (4.x)
 * - whitelist business modules → real views (5.1 / 5.3); see PROJECT_MODULE_VIEW
 * - anything else → 404
 */
function buildProjectScopedChildren() {
  const dashboardMeta = {
    title: i18n.global.t('dashboard'),
    titleI18nKey: 'dashboard',
    projectScoped: true,
    projectModule: 'dashboard'
  }
  const children = [
    {
      path: '',
      name: 'ProjectScopedHome',
      component: projectDashboardView,
      meta: { ...dashboardMeta }
    },
    {
      path: 'dashboard',
      name: 'ProjectScopedDashboard',
      component: projectDashboardView,
      meta: { ...dashboardMeta }
    }
  ]

  children.push(...buildGitCodeChildren())

  PROJECT_MODULES.filter(mod => mod !== 'dashboard' && mod !== 'code').forEach(mod => {
    const pascal = mod.charAt(0).toUpperCase() + mod.slice(1)
    const view = PROJECT_MODULE_LAZY_VIEWS[mod] || projectModulePlaceholder
    children.push({
      path: `${mod}/:pathMatch(.*)*`,
      name: `ProjectScoped${pascal}`,
      component: view,
      meta: {
        title: mod,
        titleI18nKey: mod === 'document' ? 'doc' : mod,
        projectScoped: true,
        projectModule: mod,
        projectModuleView: PROJECT_MODULE_VIEW[mod] || null
      }
    })
  })

  // Non-whitelist third segment (and deeper) → 404
  children.push({
    path: ':unknownModule(.*)*',
    name: 'ProjectScopedUnknown',
    beforeEnter: (_to, _from, next) => {
      next({ path: '/404', replace: true })
    },
    component: () => import('@/views/error/404'),
    meta: { projectScoped: true }
  })

  return children
}

/** Tiny stand-in while legacy beforeEnter redirects to namespaced paths. */
const legacyGitRedirectStub = { render: () => null }

/**
 * Client redirects from legacy `/defect` and `/project/defect` style paths (5.2).
 * Registered after special sub-routes (`/defect/template`, …) and before the
 * namespaced `/:teamKey/:projectKey` parent.
 */
function buildLegacyProjectModuleRedirectRoutes() {
  return LEGACY_PROJECT_MODULE_REDIRECTS.map(entry => ({
    path: entry.path,
    name: entry.name,
    beforeEnter: createLegacyProjectModuleRedirect(entry.module),
    component: legacyGitRedirectStub,
    hidden: true,
    meta: { legacyProjectModuleRedirect: true, projectModule: entry.module }
  }))
}

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/member'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/setup',
    component: () => import('@/views/setup/index'),
    hidden: true
  },
  {
    path: '/upgrade',
    component: () => import('@/views/upgrade/index'),
    hidden: true
  },
  {
    path: '/shard/defect',
    component: () => import('@/views/system/defect/DefectShardInfo'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: '/invite/:token',
    component: () => import('@/views/invite/index'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '/error',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'team-lock',
        component: () => import('@/views/error/team-lock'),
        name: 'TeamLockError',
        meta: { title: i18n.global.t('prompted'), titleI18nKey:'prompted', icon: 'dashboard', affix: true }
      }, {
        path: 'project-lock',
        component: () => import('@/views/error/project-lock'),
        name: 'ProjectLockError',
        meta: { title: i18n.global.t('prompted'), titleI18nKey:'prompted', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '',
    component: Layout,
    redirect: 'index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: i18n.global.t('home'), titleI18nKey:'home', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/member',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: i18n.global.t('personal-center'), titleI18nKey:'personal-center', icon: 'user' }
      },
      {
        path: 'keyboard',
        component: () => import('@/views/member/keyboard/index'),
        name: 'KeyboardSettings',
        meta: { title: i18n.global.t('keyboard.title'), titleI18nKey:'keyboard.title', icon: 'keyboard' }
      }
    ]
  },
  {
    path: '/document/c2d',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'index',
        component: () => import('@/views/system/document/HandleCat2Document'),
        name: 'HandleCat2Document',
      }
    ]
  },
  {
    path: '/defect/template',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'index',
        component: () => import('@/views/system/defect/StatisticTemplate'),
        name: 'DefectStatisticTemplate',
        meta: { title: i18n.global.t('defect.statistic-template'), titleI18nKey:'defect.statistic-template', icon: 'user' }
      }
    ]
  },
  {
    path: '/report/template',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'index',
        component: () => import('@/views/system/report/template/index'),
        name: 'ReportTemplate',
        meta: { title: i18n.global.t('report.template'), titleI18nKey:'report.template', icon: 'user' }
      }
    ]
  },
  {
    path: '/report/template/add',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'index',
        component: () => import('@/views/system/report/template/EditReportTemplate'),
        name: 'AddDefectReport',
        meta: { title: i18n.global.t('report.template'), titleI18nKey:'report.template', icon: 'user' }
      }
    ]
  },
  {
    path: '/system/project/add',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: '/system/project/add-project',
        component: () => import('@/views/system/project/add'),
        name: 'ProjectAdd',
        meta: { title: i18n.global.t('project.create-project'), titleI18nKey:'project.create-project', icon: 'user' }
      }
    ]
  },
  {
    path: '/system/team/add',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: '/system/team/add-member',
        component: () => import('@/views/system/team/add'),
        name: 'TeamAdd',
        meta: { title: i18n.global.t('team.create'), titleI18nKey:'team.create', icon: 'user' }
      }
    ]
  },
  ...(process.env.VUE_APP_ENABLE_DEV_TOOLS === 'true' ? [{
    path: '/tool/gen-edit/',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
      }
    ]
  }] : []),
  {
    path: '/notice',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: '/notice/index',
        component: () => import('@/views/notice'),
        name: 'SystemNotice',
        meta: { title: i18n.global.t('notice'), titleI18nKey:'notice' }
      }
    ]
  },
  // Legacy id-based browse → namespaced code/{slug} (must precede /:teamKey/:projectKey)
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoBrowseLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(() => ''),
        component: legacyGitRedirectStub,
        meta: { titleI18nKey: 'git.repo.library' }
      }
    ]
  },
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)/branches',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoBranchesLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(() => '/branches'),
        component: legacyGitRedirectStub
      }
    ]
  },
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)/tags',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoTagsLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(() => '/tags'),
        component: legacyGitRedirectStub
      }
    ]
  },
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)/pulls',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoPullRequestsLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(() => '/pulls'),
        component: legacyGitRedirectStub
      }
    ]
  },
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)/pulls/new',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoPullRequestNewLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(() => '/pulls/new'),
        component: legacyGitRedirectStub
      }
    ]
  },
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)/pulls/:number(\\d+)',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoPullRequestDetailLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(to => `/pulls/${to.params.number}`),
        component: legacyGitRedirectStub
      }
    ]
  },
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)/releases/:releaseId(\\d+)',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoReleaseDetailLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(to => `/releases/${to.params.releaseId}`),
        component: legacyGitRedirectStub
      }
    ]
  },
  {
    path: '/project/git-repo/browse/:repositoryId(\\d+)/commits/:commitId',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'GitRepoCommitDetailLegacy',
        beforeEnter: createLegacyGitBrowseRedirect(to => `/commits/${to.params.commitId}`),
        component: legacyGitRedirectStub
      }
    ]
  },
  // Legacy `/defect`, `/project/defect`, … → `/{teamKey}/{projectKey}/{module}` (5.2)
  ...buildLegacyProjectModuleRedirectRoutes(),
  {
    path: '/:teamKey/:projectKey',
    component: Layout,
    hidden: true,
    meta: { projectScoped: true },
    beforeEnter: (to, from, next) => {
      const { teamKey, projectKey } = to.params
      if (isPathKeyReserved(teamKey)
        || !isPathKeyFormatValid(teamKey)
        || !isPathKeyFormatValid(projectKey)) {
        next({ path: '/404', replace: true })
        return
      }
      next()
    },
    children: buildProjectScopedChildren()
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: '/system/member-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:member:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/member' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'member/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/project/defect-fields',
    component: Layout,
    hidden: true,
    permissions: ['system:project:defect-field:list'],
    children: [
      {
        path: '',
        component: () => import('@/views/system/project/defect-fields/index'),
        name: 'ProjectDefectFields',
        meta: {
          title: i18n.global.t('defect.custom-field.title'),
          titleI18nKey: 'defect.custom-field.title',
          activeMenu: '/project/option',
          projectListPage: true
        }
      }
    ]
  }
]

// 防止连续点击多次路由报错 — vue-router 4 默认返回 rejected promise，保留 catch 行为
const constantRouteNames = new Set()
function collectRouteNames(routes) {
  routes.forEach(route => {
    if (route.name) constantRouteNames.add(route.name)
    if (route.children) collectRouteNames(route.children)
  })
}

// base: vite `base` / vue-cli `publicPath`（当前均为 `/`；子路径部署时依赖 BASE_URL）
const createRouterInstance = () => createRouter({
  history: createWebHistory(process.env.BASE_URL || '/'),
  scrollBehavior: () => ({ top: 0 }),
  routes: constantRoutes
})

const router = createRouterInstance()
collectRouteNames(constantRoutes)

/** 切换项目后重新注入动态菜单时，先移除动态路由再 addRoute，避免旧项目路由残留 */
export function resetRouter() {
  router.getRoutes().forEach(route => {
    if (route.name && !constantRouteNames.has(route.name)) {
      router.removeRoute(route.name)
    }
  })
}

export default router
