/**
 * Namespaced Git browse routes: `/{teamKey}/{projectKey}/code/{slug}/**`
 * Clone / Smart HTTP stays `.../code/{slug}.git` (not a Vue route).
 */

/** @param {unknown} slug */
export function isGitCloneSlugParam(slug) {
  return typeof slug === 'string' && /\.git$/i.test(slug)
}

/**
 * SPA browse path (no `.git`). Empty string if keys/slug invalid or slug looks like clone.
 * @param {string} teamKey
 * @param {string} projectKey
 * @param {string} slug
 * @param {...string} rest
 */
export function buildGitRepoSpaPath(teamKey, projectKey, slug, ...rest) {
  if (!teamKey || !projectKey || !slug) return ''
  if (isGitCloneSlugParam(slug)) return ''
  const base = `/${teamKey}/${projectKey}/code/${slug}`
  const tail = rest
    .filter(s => s != null && s !== '')
    .map(s => String(s).replace(/^\/+|\/+$/g, ''))
    .filter(Boolean)
  return tail.length ? `${base}/${tail.join('/')}` : base
}

/**
 * Clone / Smart HTTP path (includes `.git`).
 * @param {string} teamKey
 * @param {string} projectKey
 * @param {string} slug
 */
export function buildGitRepoClonePath(teamKey, projectKey, slug) {
  if (!teamKey || !projectKey || !slug) return ''
  return `/${teamKey}/${projectKey}/code/${slug}.git`
}

/**
 * True when SPA and clone paths are distinct for the same keys+slug.
 * @param {string} teamKey
 * @param {string} projectKey
 * @param {string} slug
 */
export function spaAndClonePathsDistinct(teamKey, projectKey, slug) {
  const spa = buildGitRepoSpaPath(teamKey, projectKey, slug)
  const clone = buildGitRepoClonePath(teamKey, projectKey, slug)
  return Boolean(spa && clone && spa !== clone && clone === `${spa}.git`)
}

/**
 * @param {{ params?: Record<string, string> }|Record<string, string>} routeOrCtx
 * @returns {{ teamKey: string, projectKey: string, slug: string }}
 */
export function gitRepoRouteParams(routeOrCtx) {
  const p = (routeOrCtx && routeOrCtx.params) || routeOrCtx || {}
  return {
    teamKey: p.teamKey != null ? String(p.teamKey) : '',
    projectKey: p.projectKey != null ? String(p.projectKey) : '',
    slug: p.slug != null ? String(p.slug) : ''
  }
}

function withCtx(name, routeOrCtx, extraParams, query) {
  const params = { ...gitRepoRouteParams(routeOrCtx), ...(extraParams || {}) }
  const route = { name, params }
  if (query && Object.keys(query).length) {
    route.query = query
  }
  return route
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoBrowseRoute(routeOrCtx, query) {
  return withCtx('GitRepoBrowse', routeOrCtx, null, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoBranchesRoute(routeOrCtx, query) {
  return withCtx('GitRepoBranches', routeOrCtx, null, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoPullsRoute(routeOrCtx, query) {
  return withCtx('GitRepoPullRequests', routeOrCtx, null, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoPullNewRoute(routeOrCtx, query) {
  return withCtx('GitRepoPullRequestNew', routeOrCtx, null, query)
}

/**
 * @param {object} routeOrCtx
 * @param {string|number} number
 * @param {Record<string, string>} [query]
 */
export function gitRepoPullDetailRoute(routeOrCtx, number, query) {
  return withCtx('GitRepoPullRequestDetail', routeOrCtx, { number: String(number) }, query)
}

/**
 * @param {object} routeOrCtx
 * @param {string|number} releaseId
 * @param {Record<string, string>} [query]
 */
export function gitRepoReleaseDetailRoute(routeOrCtx, releaseId, query) {
  return withCtx('GitRepoReleaseDetail', routeOrCtx, { releaseId: String(releaseId) }, query)
}

/**
 * @param {object} routeOrCtx
 * @param {string} commitId
 * @param {Record<string, string>} [query]
 */
export function gitRepoCommitDetailRoute(routeOrCtx, commitId, query) {
  return withCtx('GitRepoCommitDetail', routeOrCtx, { commitId: String(commitId) }, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoSettingsRoute(routeOrCtx, query) {
  return withCtx('GitRepoSettings', routeOrCtx, null, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoSettingsGeneralRoute(routeOrCtx, query) {
  return withCtx('GitRepoSettingsGeneral', routeOrCtx, null, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoSettingsDangerRoute(routeOrCtx, query) {
  return withCtx('GitRepoSettingsDanger', routeOrCtx, null, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoSettingsMembersRoute(routeOrCtx, query) {
  return withCtx('GitRepoSettingsMembers', routeOrCtx, null, query)
}

/** @param {object} routeOrCtx @param {Record<string, string>} [query] */
export function gitRepoSettingsWebhooksRoute(routeOrCtx, query) {
  return withCtx('GitRepoSettingsWebhooks', routeOrCtx, null, query)
}

/**
 * Browse page primary pane from route params / query.
 * Used so `?tab=releases` (e.g. from Pull Request shell) is not lost on init.
 * @param {{ params?: Record<string, string>, query?: Record<string, string> }|null|undefined} route
 * @returns {'code'|'releases'|'commits'}
 */
export function resolveBrowseActiveTab(route) {
  if (!route) return 'code'
  const params = route.params || {}
  if (params.releaseId) return 'releases'
  if (params.commitId) return 'commits'
  const tab = route.query && route.query.tab
  if (tab === 'commits' || tab === 'releases' || tab === 'code') return tab
  return 'code'
}

/**
 * Primary shell tab from the current route name / browse query.
 * @param {{ name?: string|symbol|null, query?: Record<string, string> }|null|undefined} route
 * @returns {'code'|'pulls'|'releases'|'settings'}
 */
export function resolveShellTabFromRoute(route) {
  if (!route) return 'code'
  const name = route.name != null ? String(route.name) : ''
  if (name.startsWith('GitRepoSettings')) return 'settings'
  if (name.startsWith('GitRepoPull')) return 'pulls'
  if (name === 'GitRepoBrowse' && route.query && route.query.tab === 'releases') {
    return 'releases'
  }
  return 'code'
}
