import { buildProjectPathSegment, normalizePathKey } from '@/utils/path-key'

/** Singular project-module segments under `/{teamKey}/{projectKey}/{module}` */
export const PROJECT_MODULES = Object.freeze([
  'dashboard',
  'defect',
  'case',
  'plan',
  'document',
  'module',
  'notice',
  'member',
  'option',
  'api',
  'tools',
  'code'
])

export const PROJECT_MODULE_SET = new Set(PROJECT_MODULES)

/** Root sidebar groups that must stay outside `/{teamKey}/{projectKey}` */
export const NON_PROJECT_MENU_ROOTS = Object.freeze([
  'system',
  'monitor',
  'tool',
  'team',
  'team-options',
  'admin'
])

export const NON_PROJECT_MENU_ROOT_SET = new Set(NON_PROJECT_MENU_ROOTS)

/**
 * Historical sys_menu.path under root `project` → whitelist module segment.
 * Unmapped paths (integrations etc.) keep legacy `/project/...` links.
 *
 * MVP: integration / tooling menus intentionally stay on legacy `/project/...`
 * (not rewritten into the namespace) because mapping into `option`/`tools` is
 * ambiguous. Examples that remain legacy until a later change:
 *   ollama, openai, mcp, feishu, ding, enterprise-wechat, push, browser
 * Namespaced SPA mounts real views for whitelist modules only; see
 * PROJECT_MODULE_VIEW and buildProjectScopedChildren().
 */
export const PROJECT_MENU_PATH_TO_MODULE = Object.freeze({
  dashboard: 'dashboard',
  defect: 'defect',
  case: 'case',
  plan: 'plan',
  document: 'document',
  module: 'module',
  notice: 'notice',
  member: 'member',
  'project-member': 'member',
  option: 'option',
  api: 'api',
  'project-api': 'api',
  tools: 'tools',
  code: 'code',
  'git-repo': 'code'
})

/**
 * Whitelist module → Vue view path under `src/views/` (no `.vue` suffix).
 * Used by namespaced routes (5.1 / 5.3). `code` is handled separately via
 * `code/{slug}/**` git browse children; `dashboard` is empty-path + segment.
 */
export const PROJECT_MODULE_VIEW = Object.freeze({
  dashboard: 'system/dashboard/index',
  defect: 'system/defect/index',
  case: 'system/case/index',
  plan: 'system/plan/index',
  document: 'system/document/index',
  module: 'system/module/index',
  notice: 'notice/index',
  member: 'system/project/member/index',
  option: 'system/project/option/index',
  api: 'system/api/index',
  tools: 'tool/project/index'
})

/**
 * Modules whose legacy `/project/{path}` (and bare `/{module}`) URLs redirect
 * into the namespace. `code` / `git-repo` use dedicated browse redirects;
 * do not include them here. `/member` (profile) is never redirected.
 */
export const LEGACY_MODULE_REDIRECT_MODULES = Object.freeze([
  'dashboard',
  'defect',
  'case',
  'plan',
  'document',
  'module',
  'notice',
  'member',
  'option',
  'api',
  'tools'
])

/**
 * Legacy History paths that redirect to a whitelist module (5.2).
 * More-specific constant routes (`/defect/template`, `/document/c2d`,
 * `/member/profile`) stay registered ahead of these.
 *
 * @type {ReadonlyArray<{ path: string, module: string, name: string }>}
 */
export const LEGACY_PROJECT_MODULE_REDIRECTS = Object.freeze([
  // Bare roots (bookmarks / older absolute links)
  ...LEGACY_MODULE_REDIRECT_MODULES.filter(m => m !== 'member' && m !== 'notice').map(m => ({
    path: `/${m}/:pathMatch(.*)*`,
    module: m,
    name: `LegacyRoot${m.charAt(0).toUpperCase()}${m.slice(1)}Redirect`
  })),
  // Historical `/project/{menuPath}` registrations
  { path: '/project/dashboard/:pathMatch(.*)*', module: 'dashboard', name: 'LegacyProjectDashboardRedirect' },
  { path: '/project/defect/:pathMatch(.*)*', module: 'defect', name: 'LegacyProjectDefectRedirect' },
  { path: '/project/case/:pathMatch(.*)*', module: 'case', name: 'LegacyProjectCaseRedirect' },
  { path: '/project/plan/:pathMatch(.*)*', module: 'plan', name: 'LegacyProjectPlanRedirect' },
  { path: '/project/document/:pathMatch(.*)*', module: 'document', name: 'LegacyProjectDocumentRedirect' },
  { path: '/project/module/:pathMatch(.*)*', module: 'module', name: 'LegacyProjectModuleRedirect' },
  { path: '/project/notice/:pathMatch(.*)*', module: 'notice', name: 'LegacyProjectNoticeRedirect' },
  { path: '/project/option/:pathMatch(.*)*', module: 'option', name: 'LegacyProjectOptionRedirect' },
  { path: '/project/tools/:pathMatch(.*)*', module: 'tools', name: 'LegacyProjectToolsRedirect' },
  { path: '/project/api/:pathMatch(.*)*', module: 'api', name: 'LegacyProjectApiRedirect' },
  { path: '/project/project-api/:pathMatch(.*)*', module: 'api', name: 'LegacyProjectProjectApiRedirect' },
  { path: '/project/member/:pathMatch(.*)*', module: 'member', name: 'LegacyProjectMemberRedirect' },
  { path: '/project/project-member/:pathMatch(.*)*', module: 'member', name: 'LegacyProjectProjectMemberRedirect' },
  // Repo list (browse/* has dedicated slug redirects registered separately)
  { path: '/project/git-repo', module: 'code', name: 'LegacyProjectGitRepoListRedirect' }
])

/**
 * @param {unknown} segment
 * @returns {boolean}
 */
export function isProjectModule(segment) {
  if (segment == null || segment === '') return false
  return PROJECT_MODULE_SET.has(String(segment).trim().toLowerCase())
}

/**
 * Build History path `/{teamKey}/{projectKey}` or `/{teamKey}/{projectKey}/{module}/...`.
 * Omits module when falsy; rejects unknown modules.
 *
 * @param {string} teamKey
 * @param {string} projectKey
 * @param {string} [module]
 * @param {...string} restSegments
 * @returns {string} absolute path (leading `/`) or `''` if keys/module invalid
 */
export function buildProjectModulePath(teamKey, projectKey, module, ...restSegments) {
  const base = buildProjectPathSegment(teamKey, projectKey)
  if (!base) return ''

  if (module == null || module === '') {
    return base
  }

  const mod = String(module).trim().toLowerCase()
  if (!isProjectModule(mod)) return ''

  const rest = restSegments
    .filter(s => s != null && s !== '')
    .map(s => String(s).replace(/^\/+|\/+$/g, ''))
    .filter(Boolean)

  return rest.length ? `${base}/${mod}/${rest.join('/')}` : `${base}/${mod}`
}

/**
 * Parse a History pathname into project-scoped parts.
 * Returns null when the path is not `/{teamKey}/{projectKey}` or
 * `/{teamKey}/{projectKey}/{module}/**` with valid keys (and whitelist module when present).
 *
 * @param {string} pathname
 * @returns {{ teamKey: string, projectKey: string, module: string|null, rest: string[] }|null}
 */
export function parseProjectScopedPath(pathname) {
  if (!pathname || typeof pathname !== 'string') return null

  const raw = pathname.split('?')[0].split('#')[0]
  const parts = raw.replace(/^\/+|\/+$/g, '').split('/').filter(Boolean)
  if (parts.length < 2) return null

  // Sidebar roots (/team-options, /admin, …) must never be treated as teamKey/projectKey
  if (NON_PROJECT_MENU_ROOT_SET.has(String(parts[0]).trim().toLowerCase())) {
    return null
  }

  const teamKey = normalizePathKey(parts[0])
  const projectKey = normalizePathKey(parts[1])
  if (!teamKey || !projectKey) return null

  if (parts.length === 2) {
    return { teamKey, projectKey, module: null, rest: [] }
  }

  const module = String(parts[2]).trim().toLowerCase()
  if (!isProjectModule(module)) return null

  return {
    teamKey,
    projectKey,
    module,
    rest: parts.slice(3)
  }
}

/**
 * Map a menu path segment (or full relative path) to a whitelist module, or null.
 *
 * @param {unknown} menuPath
 * @returns {string|null}
 */
export function resolveMenuPathToModule(menuPath) {
  if (menuPath == null || menuPath === '' || menuPath === '#') return null
  const raw = String(menuPath).trim().replace(/^\/+|\/+$/g, '')
  if (!raw) return null
  const first = raw.split('/')[0].toLowerCase()
  if (PROJECT_MENU_PATH_TO_MODULE[first]) {
    return PROJECT_MENU_PATH_TO_MODULE[first]
  }
  if (isProjectModule(first)) return first
  return null
}

/**
 * Whether a project-menu child should still be registered as a dynamic
 * `/project/...` route. Whitelist modules are served under the namespace;
 * integrations stay registered.
 *
 * @param {unknown} menuPath
 * @returns {boolean} true = keep on legacy dynamic registration
 */
export function keepLegacyProjectMenuRoute(menuPath) {
  const module = resolveMenuPathToModule(menuPath)
  if (!module) return true
  return false
}

/**
 * @param {{ path?: string, name?: string }|null|undefined} route
 * @returns {boolean}
 */
export function isProjectRootMenu(route) {
  if (!route) return false
  const name = route.name != null ? String(route.name).toLowerCase() : ''
  const path = route.path != null ? String(route.path).replace(/^\/+|\/+$/g, '').toLowerCase() : ''
  if (name === 'project' || path === 'project') return true
  return false
}

/**
 * @param {{ path?: string, name?: string }|null|undefined} route
 * @returns {boolean}
 */
export function isNonProjectMenuRoot(route) {
  if (!route) return false
  const name = route.name != null ? String(route.name).toLowerCase() : ''
  const path = route.path != null ? String(route.path).replace(/^\/+|\/+$/g, '').toLowerCase() : ''
  const key = name || path
  return NON_PROJECT_MENU_ROOT_SET.has(key)
}

/**
 * Rewrite only the `project` (项目功能) menu tree so leaf links use
 * `/{teamKey}/{projectKey}/{module}`. System/team/admin roots are untouched.
 * Mutates `routes` in place; skips when keys missing.
 *
 * @param {Array<{ path?: string, name?: string, children?: unknown[] }>} routes
 * @param {string} teamKey
 * @param {string} projectKey
 * @returns {Array}
 */
export function rewriteProjectMenuPaths(routes, teamKey, projectKey) {
  if (!Array.isArray(routes) || !teamKey || !projectKey) return routes

  routes.forEach(route => {
    if (isNonProjectMenuRoot(route)) return
    if (!isProjectRootMenu(route) || !Array.isArray(route.children)) return
    rewriteProjectMenuChildren(route.children, teamKey, projectKey)
  })
  return routes
}

/**
 * @param {Array<{ path?: string, children?: unknown[], meta?: Record<string, unknown> }>} children
 * @param {string} teamKey
 * @param {string} projectKey
 */
function rewriteProjectMenuChildren(children, teamKey, projectKey) {
  children.forEach(child => {
    if (!child || typeof child !== 'object') return
    const module = resolveMenuPathToModule(child.path)
    if (module) {
      const namespaced = buildProjectModulePath(teamKey, projectKey, module)
      if (namespaced) {
        child.path = namespaced
        child.meta = {
          ...(child.meta || {}),
          projectScopedMenu: true,
          projectModule: module,
          activeMenu: namespaced
        }
      }
    }
    if (Array.isArray(child.children) && child.children.length) {
      // Nested under an already-namespaced leaf is uncommon; only rewrite
      // relative children that still map to modules when parent was unmapped.
      if (!module) {
        rewriteProjectMenuChildren(child.children, teamKey, projectKey)
      }
    }
  })
}

/**
 * Build the destination History path when switching projects.
 * Keeps module (+ rest) when on a namespaced or legacy project-business URL;
 * returns `''` when the current location should not be rewritten.
 *
 * @param {string} currentPath
 * @param {string} teamKey
 * @param {string} projectKey
 * @returns {string}
 */
export function buildPathForProjectSwitch(currentPath, teamKey, projectKey) {
  if (!teamKey || !projectKey) return ''
  if (!currentPath || typeof currentPath !== 'string') return ''

  const raw = currentPath.split('?')[0].split('#')[0]
  const parts = raw.replace(/^\/+|\/+$/g, '').split('/').filter(Boolean)
  if (!parts.length) return ''

  // System / team / admin (and related) roots stay outside project namespace
  if (NON_PROJECT_MENU_ROOT_SET.has(parts[0].toLowerCase())) return ''

  const scoped = parseProjectScopedPath(currentPath)
  if (scoped) {
    return buildProjectModulePath(
      teamKey,
      projectKey,
      scoped.module || undefined,
      ...scoped.rest
    )
  }

  // Legacy `/project/{seg}/**` (sidebar historical base)
  if (parts[0].toLowerCase() === 'project' && parts.length >= 2) {
    const module = resolveMenuPathToModule(parts[1])
    if (!module) return ''
    return buildProjectModulePath(teamKey, projectKey, module, ...parts.slice(2))
  }

  // Legacy root business paths `/defect`, `/plan`, …
  const module = resolveMenuPathToModule(parts[0])
  if (!module) return ''
  return buildProjectModulePath(teamKey, projectKey, module, ...parts.slice(1))
}

/**
 * Sidebar `default-active` index for a route path (module root under namespace).
 *
 * @param {string} pathname
 * @param {string} [metaActiveMenu]
 * @returns {string}
 */
export function resolveProjectScopedActiveMenu(pathname, metaActiveMenu) {
  if (metaActiveMenu) return metaActiveMenu
  const scoped = parseProjectScopedPath(pathname)
  if (!scoped) return pathname || ''
  if (!scoped.module) {
    return buildProjectModulePath(scoped.teamKey, scoped.projectKey, 'dashboard')
      || buildProjectModulePath(scoped.teamKey, scoped.projectKey)
      || pathname
  }
  return buildProjectModulePath(scoped.teamKey, scoped.projectKey, scoped.module) || pathname
}
