import { getGitRepo, resolveGitRepoByProjectAndSlug } from '@/api/system/gitRepo'
import { getCurrentProjectId } from '@/utils/store-context'
import { isGitCloneSlugParam } from '@/utils/git-repo-routes'

/**
 * Attach `repositoryId` onto route params from `slug` + current project.
 * Rejects clone-looking `*.git` slugs so SPA never claims Smart HTTP paths.
 */
export async function attachGitRepoIdFromSlug(to, _from, next) {
  const slug = to.params.slug
  if (isGitCloneSlugParam(slug)) {
    next({ path: '/404', replace: true })
    return
  }
  if (to.params.repositoryId) {
    next()
    return
  }
  try {
    const projectId = getCurrentProjectId()
    const repo = await resolveGitRepoByProjectAndSlug(projectId, slug)
    if (!repo || repo.repositoryId == null) {
      next({ path: '/404', replace: true })
      return
    }
    to.params.repositoryId = String(repo.repositoryId)
    next()
  } catch (e) {
    next({ path: '/404', replace: true })
  }
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
