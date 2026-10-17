/**
 * Lightweight route-table check for namespaced modules + legacy redirects (no full app boot).
 */
import { createRouter, createMemoryHistory } from 'vue-router'
import {
  LEGACY_PROJECT_MODULE_REDIRECTS,
  PROJECT_MODULES,
  PROJECT_MODULE_VIEW
} from '@/utils/project-module-path'
import { pathMatchToSegments } from '@/utils/legacy-project-module-redirect'

const Stub = { template: '<div />' }
const LayoutStub = { template: '<router-view />' }

function buildGitCodeChildren() {
  return [
    { path: 'code', name: 'GitRepoList', component: Stub },
    { path: 'code/:slug', name: 'GitRepoBrowse', component: Stub },
    { path: 'code/:slug/branches', name: 'GitRepoBranches', component: Stub },
    { path: 'code/:slug/pulls', name: 'GitRepoPullRequests', component: Stub },
    { path: 'code/:slug/pulls/new', name: 'GitRepoPullRequestNew', component: Stub },
    { path: 'code/:slug/pulls/:number(\\d+)', name: 'GitRepoPullRequestDetail', component: Stub },
    { path: 'code/:slug/releases/:releaseId(\\d+)', name: 'GitRepoReleaseDetail', component: Stub },
    { path: 'code/:slug/commits/:commitId', name: 'GitRepoCommitDetail', component: Stub }
  ]
}

function buildChildren() {
  const children = [
    { path: '', name: 'ProjectScopedHome', component: Stub },
    { path: 'dashboard', name: 'ProjectScopedDashboard', component: Stub },
    ...buildGitCodeChildren()
  ]
  PROJECT_MODULES.filter(m => m !== 'dashboard' && m !== 'code').forEach(mod => {
    const pascal = mod.charAt(0).toUpperCase() + mod.slice(1)
    children.push({
      path: `${mod}/:pathMatch(.*)*`,
      name: `ProjectScoped${pascal}`,
      component: Stub,
      meta: { projectModule: mod, projectModuleView: PROJECT_MODULE_VIEW[mod] }
    })
  })
  children.push({
    path: ':unknownModule(.*)*',
    name: 'ProjectScopedUnknown',
    component: Stub
  })
  return children
}

describe('project-scoped route table', () => {
  let router

  beforeEach(() => {
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'Root', component: Stub },
        { path: '/404', name: 'NotFound', component: Stub },
        { path: '/login', name: 'Login', component: Stub },
        { path: '/index', name: 'Index', component: Stub },
        {
          path: '/:teamKey/:projectKey',
          component: LayoutStub,
          children: buildChildren()
        }
      ]
    })
  })

  it('resolves empty path and dashboard to scoped dashboard names', async () => {
    await router.push('/acme/demo')
    expect(router.currentRoute.value.name).toBe('ProjectScopedHome')
    expect(router.currentRoute.value.path).toBe('/acme/demo')

    await router.push('/acme/demo/dashboard')
    expect(router.currentRoute.value.name).toBe('ProjectScopedDashboard')
    expect(router.currentRoute.value.path).toBe('/acme/demo/dashboard')
  })

  it('resolves whitelist business modules to scoped route names', async () => {
    await router.push('/acme/demo/defect')
    expect(router.currentRoute.value.name).toBe('ProjectScopedDefect')
    expect(router.currentRoute.value.meta.projectModuleView).toBe('system/defect/index')

    await router.push('/acme/demo/case')
    expect(router.currentRoute.value.name).toBe('ProjectScopedCase')

    await router.push('/acme/demo/plan')
    expect(router.currentRoute.value.name).toBe('ProjectScopedPlan')

    await router.push('/acme/demo/document')
    expect(router.currentRoute.value.name).toBe('ProjectScopedDocument')

    await router.push('/acme/demo/member')
    expect(router.currentRoute.value.name).toBe('ProjectScopedMember')

    await router.push('/acme/demo/option')
    expect(router.currentRoute.value.name).toBe('ProjectScopedOption')

    await router.push('/acme/demo/api')
    expect(router.currentRoute.value.name).toBe('ProjectScopedApi')

    await router.push('/acme/demo/tools')
    expect(router.currentRoute.value.name).toBe('ProjectScopedTools')

    await router.push('/acme/demo/notice')
    expect(router.currentRoute.value.name).toBe('ProjectScopedNotice')

    await router.push('/acme/demo/module')
    expect(router.currentRoute.value.name).toBe('ProjectScopedModule')
  })

  it('resolves code/{slug} browse deep links', async () => {
    await router.push('/acme/demo/code')
    expect(router.currentRoute.value.name).toBe('GitRepoList')

    await router.push('/acme/demo/code/my-repo')
    expect(router.currentRoute.value.name).toBe('GitRepoBrowse')
    expect(router.currentRoute.value.params.slug).toBe('my-repo')

    await router.push('/acme/demo/code/my-repo/branches')
    expect(router.currentRoute.value.name).toBe('GitRepoBranches')

    await router.push('/acme/demo/code/my-repo/pulls/new')
    expect(router.currentRoute.value.name).toBe('GitRepoPullRequestNew')

    await router.push('/acme/demo/code/my-repo/pulls/12')
    expect(router.currentRoute.value.name).toBe('GitRepoPullRequestDetail')

    await router.push('/acme/demo/code/my-repo/releases/3')
    expect(router.currentRoute.value.name).toBe('GitRepoReleaseDetail')

    await router.push('/acme/demo/code/my-repo/commits/abc123')
    expect(router.currentRoute.value.name).toBe('GitRepoCommitDetail')
  })

  it('matches illegal third segment on unknown child', async () => {
    await router.push('/acme/demo/not-a-real-module')
    expect(router.currentRoute.value.name).toBe('ProjectScopedUnknown')
  })

  it('keeps system routes outside namespace', async () => {
    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('Login')
  })
})

describe('legacy project module redirects table', () => {
  it('lists bare and /project legacy redirect paths for whitelist modules', () => {
    const paths = LEGACY_PROJECT_MODULE_REDIRECTS.map(r => r.path)
    expect(paths).toContain('/defect/:pathMatch(.*)*')
    expect(paths).toContain('/project/defect/:pathMatch(.*)*')
    expect(paths).toContain('/project/project-member/:pathMatch(.*)*')
    expect(paths).toContain('/project/notice/:pathMatch(.*)*')
    // Avoid clobbering /member/profile and constant /notice
    expect(paths.some(p => p.startsWith('/member'))).toBe(false)
    expect(paths.some(p => p === '/notice/:pathMatch(.*)*')).toBe(false)
  })

  it('normalizes pathMatch params to segments', () => {
    expect(pathMatchToSegments(undefined)).toEqual([])
    expect(pathMatchToSegments(['a', 'b'])).toEqual(['a', 'b'])
    expect(pathMatchToSegments('a/b')).toEqual(['a', 'b'])
  })
})
