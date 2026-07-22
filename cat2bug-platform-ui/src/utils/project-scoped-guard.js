import { getProjectByPathKeys } from '@/api/system/project'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'
import { isPathKeyFormatValid, isPathKeyReserved } from '@/utils/path-key'
import { parseProjectScopedPath } from '@/utils/project-module-path'

/**
 * @typedef {'skip'|'ok'|'switched'|'forbidden'|'not_found'} ProjectScopedGuardResult
 */

function isProjectScopedMatch(route) {
  return Array.isArray(route && route.matched)
    && route.matched.some(r => r.meta && r.meta.projectScoped)
}

/**
 * Resolve path keys on a project-scoped URL and align `currentProjectId`.
 * Non-namespaced paths return `skip`.
 *
 * @param {{ path?: string, params?: Record<string, string>, matched?: Array<{ meta?: Record<string, unknown> }> }} route
 * @returns {Promise<ProjectScopedGuardResult>}
 */
export async function ensureProjectScopedContext(route) {
  const path = (route && route.path) || ''
  const parsed = parseProjectScopedPath(path)
  if (!parsed) {
    // Matched namespaced parent/child but keys or module are invalid → 404
    return isProjectScopedMatch(route) ? 'not_found' : 'skip'
  }

  const teamKey = (route.params && route.params.teamKey) || parsed.teamKey
  const projectKey = (route.params && route.params.projectKey) || parsed.projectKey
  if (isPathKeyReserved(teamKey) || !isPathKeyFormatValid(teamKey) || !isPathKeyFormatValid(projectKey)) {
    return 'not_found'
  }

  try {
    const res = await getProjectByPathKeys(teamKey, projectKey)
    const project = res && res.data
    if (!project || !project.projectId) {
      return 'not_found'
    }

    const userStore = useUserStore(pinia)
    const currentId = Number((userStore.config || {}).currentProjectId || 0)
    if (currentId === Number(project.projectId)) {
      return 'ok'
    }

    await userStore.SwitchCurrentProject(project.projectId)
    return 'switched'
  } catch (err) {
    return mapProjectResolveError(err)
  }
}

/**
 * @param {unknown} err
 * @returns {'forbidden'|'not_found'}
 */
export function mapProjectResolveError(err) {
  const code = err && typeof err === 'object' ? err.code : undefined
  if (code === 403 || code === 401) {
    return 'forbidden'
  }
  return 'not_found'
}
