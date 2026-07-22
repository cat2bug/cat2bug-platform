import { getProject } from '@/api/system/project'
import { getCurrentProjectId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'

/**
 * Normalize vue-router `pathMatch` param to path segments.
 * @param {unknown} pathMatch
 * @returns {string[]}
 */
export function pathMatchToSegments(pathMatch) {
  if (pathMatch == null || pathMatch === '') return []
  if (Array.isArray(pathMatch)) {
    return pathMatch
      .filter(s => s != null && s !== '')
      .map(s => String(s).replace(/^\/+|\/+$/g, ''))
      .filter(Boolean)
  }
  return String(pathMatch)
    .replace(/^\/+|\/+$/g, '')
    .split('/')
    .filter(Boolean)
}

/**
 * Resolve current project path keys for legacy → namespaced redirects.
 * @returns {Promise<{ teamKey: string, projectKey: string }|null>}
 */
export function resolveCurrentProjectPathKeysForRedirect() {
  const projectId = getCurrentProjectId()
  if (!projectId) return Promise.resolve(null)
  return getProject(projectId).then(res => {
    const project = res && res.data
    if (!project || !project.teamKey || !project.projectKey) return null
    return { teamKey: project.teamKey, projectKey: project.projectKey }
  }).catch(() => null)
}

/**
 * beforeEnter: legacy business URL → `/{teamKey}/{projectKey}/{module}/...`
 * when current project path keys are available; otherwise `/index` (safe home).
 *
 * @param {string} module whitelist module segment
 * @returns {import('vue-router').NavigationGuardWithThis<undefined>}
 */
export function createLegacyProjectModuleRedirect(module) {
  return async (to, _from, next) => {
    try {
      const keys = await resolveCurrentProjectPathKeysForRedirect()
      if (!keys) {
        next({ path: '/index', replace: true })
        return
      }
      const rest = pathMatchToSegments(to.params.pathMatch)
      const target = buildProjectModulePath(keys.teamKey, keys.projectKey, module, ...rest)
      if (!target) {
        next({ path: '/404', replace: true })
        return
      }
      next({
        path: target,
        query: to.query,
        hash: to.hash,
        replace: true
      })
    } catch (e) {
      next({ path: '/index', replace: true })
    }
  }
}
