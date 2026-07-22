import { mapProjectResolveError } from '@/utils/project-scoped-guard'
import {
  isProjectModule,
  parseProjectScopedPath,
  PROJECT_MODULES,
  PROJECT_MODULE_VIEW,
  rewriteProjectMenuPaths,
  resolveMenuPathToModule,
  keepLegacyProjectMenuRoute,
  buildPathForProjectSwitch,
  resolveProjectScopedActiveMenu,
  isNonProjectMenuRoot
} from '@/utils/project-module-path'

describe('project-scoped-guard', () => {
  describe('mapProjectResolveError', () => {
    it('maps 401/403 to forbidden', () => {
      expect(mapProjectResolveError({ code: 401 })).toBe('forbidden')
      expect(mapProjectResolveError({ code: 403 })).toBe('forbidden')
    })

    it('maps other errors to not_found', () => {
      expect(mapProjectResolveError({ code: 404 })).toBe('not_found')
      expect(mapProjectResolveError({ code: 500 })).toBe('not_found')
      expect(mapProjectResolveError('error')).toBe('not_found')
      expect(mapProjectResolveError(null)).toBe('not_found')
    })
  })
})

describe('project-module-path (scoped routes)', () => {
  it('includes dashboard and singular whitelist modules', () => {
    expect(PROJECT_MODULES).toContain('dashboard')
    expect(PROJECT_MODULES).toContain('defect')
    expect(PROJECT_MODULES).toContain('code')
    expect(isProjectModule('DEFECT')).toBe(true)
    expect(isProjectModule('unknown')).toBe(false)
  })

  it('parses empty module and dashboard', () => {
    expect(parseProjectScopedPath('/acme/demo')).toEqual({
      teamKey: 'acme',
      projectKey: 'demo',
      module: null,
      rest: []
    })
    expect(parseProjectScopedPath('/acme/demo/dashboard')).toEqual({
      teamKey: 'acme',
      projectKey: 'demo',
      module: 'dashboard',
      rest: []
    })
  })

  it('rejects illegal third segment', () => {
    expect(parseProjectScopedPath('/acme/demo/not-a-module')).toBeNull()
  })

  it('does not treat /invite/:token as project-scoped path', () => {
    expect(parseProjectScopedPath(
      '/invite/b7bd912c89ad43e88139e089ad716cffa9fa8b062a5a4f79a14eeb5c6c9dc370'
    )).toBeNull()
  })

  it('does not treat /team-options/* as project-scoped path', () => {
    expect(parseProjectScopedPath('/team-options/team-option')).toBeNull()
    expect(parseProjectScopedPath('/team-options/team-base-info')).toBeNull()
    expect(parseProjectScopedPath('/team-options/team-member')).toBeNull()
  })
})

describe('project menu rewrite (3.1 / 3.3)', () => {
  it('maps menu aliases to whitelist modules', () => {
    expect(resolveMenuPathToModule('defect')).toBe('defect')
    expect(resolveMenuPathToModule('git-repo')).toBe('code')
    expect(resolveMenuPathToModule('project-api')).toBe('api')
    expect(resolveMenuPathToModule('project-member')).toBe('member')
    expect(resolveMenuPathToModule('ollama')).toBeNull()
  })

  it('keeps integrations and git-repo on legacy dynamic registration', () => {
    expect(keepLegacyProjectMenuRoute('defect')).toBe(false)
    expect(keepLegacyProjectMenuRoute('case')).toBe(false)
    expect(keepLegacyProjectMenuRoute('member')).toBe(false)
    expect(keepLegacyProjectMenuRoute('git-repo')).toBe(false)
    expect(keepLegacyProjectMenuRoute('ollama')).toBe(true)
    expect(keepLegacyProjectMenuRoute('feishu')).toBe(true)
    expect(keepLegacyProjectMenuRoute('enterprise-wechat')).toBe(true)
  })

  it('exposes view paths for namespaced whitelist modules', () => {
    expect(PROJECT_MODULE_VIEW.defect).toBe('system/defect/index')
    expect(PROJECT_MODULE_VIEW.member).toBe('system/project/member/index')
    expect(PROJECT_MODULE_VIEW.notice).toBe('notice/index')
    expect(PROJECT_MODULE_VIEW.tools).toBe('tool/project/index')
  })

  it('prefixes only project-function children with keys', () => {
    const routes = [
      {
        name: 'Project',
        path: 'project',
        children: [
          { path: 'defect', meta: { title: '缺陷' } },
          { path: 'git-repo', meta: { title: '源码' } },
          { path: 'ollama', meta: { title: 'AI' } }
        ]
      },
      {
        name: 'System',
        path: 'system',
        children: [{ path: 'user', meta: { title: '用户' } }]
      },
      {
        name: 'Team',
        path: 'team',
        children: [{ path: 'project-list', meta: { title: '项目' } }]
      }
    ]
    rewriteProjectMenuPaths(routes, 'acme', 'demo')
    expect(routes[0].children[0].path).toBe('/acme/demo/defect')
    expect(routes[0].children[1].path).toBe('/acme/demo/code')
    expect(routes[0].children[2].path).toBe('ollama')
    expect(routes[1].children[0].path).toBe('user')
    expect(routes[2].children[0].path).toBe('project-list')
    expect(isNonProjectMenuRoot(routes[1])).toBe(true)
  })

  it('rewrites URL prefix on project switch', () => {
    expect(buildPathForProjectSwitch('/acme/a/defect', 'beta', 'b')).toBe('/beta/b/defect')
    expect(buildPathForProjectSwitch('/acme/a/code/repo/commits', 'beta', 'b')).toBe(
      '/beta/b/code/repo/commits'
    )
    expect(buildPathForProjectSwitch('/project/defect', 'beta', 'b')).toBe('/beta/b/defect')
    expect(buildPathForProjectSwitch('/defect', 'beta', 'b')).toBe('/beta/b/defect')
    expect(buildPathForProjectSwitch('/system/user', 'beta', 'b')).toBe('')
    expect(buildPathForProjectSwitch('/team/project-list', 'beta', 'b')).toBe('')
  })

  it('resolves sidebar activeMenu to module root', () => {
    expect(resolveProjectScopedActiveMenu('/acme/demo')).toBe('/acme/demo/dashboard')
    expect(resolveProjectScopedActiveMenu('/acme/demo/defect/extra')).toBe('/acme/demo/defect')
    expect(resolveProjectScopedActiveMenu('/system/user', '/system/member')).toBe('/system/member')
  })
})
