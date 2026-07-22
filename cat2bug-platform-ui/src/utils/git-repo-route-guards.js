import { getGitRepo, getGitRepoMyAccess, resolveGitRepoByProjectAndSlug } from '@/api/system/gitRepo'
import { checkPermi } from '@/utils/permission'
import { getCurrentProjectId } from '@/utils/store-context'
import { gitRepoBrowseRoute, isGitCloneSlugParam } from '@/utils/git-repo-routes'

/** Named routes under `code/:slug/**` that need `repositoryId` injected. */
export const GIT_REPO_SLUG_ROUTE_NAMES = new Set([
  'GitRepoBrowse',
  'GitRepoBranches',
  'GitRepoPullRequests',
  'GitRepoPullRequestNew',
  'GitRepoPullRequestDetail',
  'GitRepoReleaseDetail',
  'GitRepoCommitDetail',
  'GitRepoSettings',
  'GitRepoSettingsGeneral',
  'GitRepoSettingsDanger',
  'GitRepoSettingsMembers',
  'GitRepoSettingsWebhooks'
])

function isValidRepositoryId(value) {
  if (value == null || value === '') return false
  const n = Number(value)
  return Number.isFinite(n) && n > 0
}

/**
 * @param {{ name?: string|symbol|null }} route
 * @returns {boolean}
 */
export function routeNeedsGitRepoId(route) {
  const name = route && route.name != null ? String(route.name) : ''
  return GIT_REPO_SLUG_ROUTE_NAMES.has(name)
}

/**
 * Resolve slug → repositoryId and write onto `to.params`.
 * Must run on every navigation (including same-route slug changes): route
 * `beforeEnter` does NOT re-fire when only params change.
 *
 * @param {{ params?: Record<string, string> }} to
 * @returns {Promise<'ok'|'not_found'>}
 */
export async function ensureGitRepoIdAttached(to) {
  if (!to || !to.params) return 'not_found'
  const slug = to.params.slug
  if (isGitCloneSlugParam(slug) || !slug) {
    return 'not_found'
  }
  try {
    const projectId = getCurrentProjectId()
    const repo = await resolveGitRepoByProjectAndSlug(projectId, slug)
    if (!repo || !isValidRepositoryId(repo.repositoryId)) {
      return 'not_found'
    }
    // Always re-align to slug so switching repos cannot keep a stale id
    to.params.repositoryId = String(repo.repositoryId)
    return 'ok'
  } catch (e) {
    return 'not_found'
  }
}

/**
 * Attach `repositoryId` onto route params from `slug` + current project.
 * Rejects clone-looking `*.git` slugs so SPA never claims Smart HTTP paths.
 *
 * IMPORTANT: `repositoryId` is not a path segment. Mutate `to.params` in place and
 * call `next()` — never `next({ params: { repositoryId } })`, or Vue Router will
 * drop the undeclared param and re-enter this guard forever.
 *
 * Prefer `ensureGitRepoIdAttached` from the global beforeEach for slug switches;
 * this `beforeEnter` wrapper still covers first entry onto the route record.
 */
export async function attachGitRepoIdFromSlug(to, _from, next) {
  const result = await ensureGitRepoIdAttached(to)
  if (result !== 'ok') {
    next({ path: '/404', replace: true })
    return
  }
  next()
}

/**
 * True when the current user may manage the repository.
 * Prefers permission word, then falls back to `GET .../my-access` (repo ACL admin
 * may lack `system:git:repo:manage`). On my-access failure, only the permission word counts.
 * @param {string|number} repositoryId
 * @returns {Promise<boolean>}
 */
export async function canManageGitRepo(repositoryId) {
  if (checkPermi(['system:git:repo:manage'])) {
    return true
  }
  const id = Number(repositoryId)
  if (!Number.isFinite(id) || id <= 0) {
    return false
  }
  try {
    const res = await getGitRepoMyAccess(id)
    const access = (res && res.data) || {}
    return !!access.canManage
  } catch (e) {
    return false
  }
}

/**
 * Attach repositoryId then require effective manage:
 * `system:git:repo:manage` OR `my-access.canManage`.
 * Used for settings routes (constant routes; not filtered via dynamic `permissions`).
 * Denied users are redirected to the repository browse page.
 */
export async function attachGitRepoIdAndRequireManage(to, _from, next) {
  const result = await ensureGitRepoIdAttached(to)
  if (result !== 'ok') {
    next({ path: '/404', replace: true })
    return
  }
  const allowed = await canManageGitRepo(to.params.repositoryId)
  if (!allowed) {
    next({
      ...gitRepoBrowseRoute(to),
      replace: true
    })
    return
  }
  next()
}

/**
 * Legacy `/project/git-repo/browse/:repositoryId/**` → namespaced slug path when resolvable.
 * @param {(to: import('vue-router').RouteLocationNormalized) => string} suffixFn
 */
export function createLegacyGitBrowseRedirect(suffixFn) {
  return async (to, _from, next) => {
    try {
      const res = await getGitRepo(to.params.repositoryId)
      const repo = res && res.data
      if (!repo || !repo.teamKey || !repo.projectKey || !repo.slug) {
        next({ path: '/404', replace: true })
        return
      }
      const suffix = typeof suffixFn === 'function' ? (suffixFn(to) || '') : ''
      next({
        path: `/${repo.teamKey}/${repo.projectKey}/code/${repo.slug}${suffix}`,
        query: to.query,
        hash: to.hash,
        replace: true
      })
    } catch (e) {
      next({ path: '/404', replace: true })
    }
  }
}
