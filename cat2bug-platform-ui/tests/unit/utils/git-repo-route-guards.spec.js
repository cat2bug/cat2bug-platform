import {
  GIT_REPO_SLUG_ROUTE_NAMES,
  ensureGitRepoIdAttached,
  routeNeedsGitRepoId
} from '@/utils/git-repo-route-guards'

jest.mock('@/api/system/gitRepo', () => ({
  getGitRepo: jest.fn(),
  resolveGitRepoByProjectAndSlug: jest.fn()
}))

jest.mock('@/utils/store-context', () => ({
  getCurrentProjectId: jest.fn(() => 10)
}))

import { resolveGitRepoByProjectAndSlug } from '@/api/system/gitRepo'

describe('git-repo-route-guards', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  describe('routeNeedsGitRepoId', () => {
    it('matches browse and nested slug routes', () => {
      expect(routeNeedsGitRepoId({ name: 'GitRepoBrowse' })).toBe(true)
      expect(routeNeedsGitRepoId({ name: 'GitRepoBranches' })).toBe(true)
      expect(routeNeedsGitRepoId({ name: 'GitRepoCommitDetail' })).toBe(true)
      expect(routeNeedsGitRepoId({ name: 'GitRepoList' })).toBe(false)
      expect(routeNeedsGitRepoId({ name: 'ProjectScopedHome' })).toBe(false)
    })

    it('lists all slug route names', () => {
      expect(GIT_REPO_SLUG_ROUTE_NAMES.has('GitRepoBrowse')).toBe(true)
      expect(GIT_REPO_SLUG_ROUTE_NAMES.has('GitRepoList')).toBe(false)
    })
  })

  describe('ensureGitRepoIdAttached', () => {
    it('writes repositoryId from slug resolve', async () => {
      resolveGitRepoByProjectAndSlug.mockResolvedValue({ repositoryId: 42, slug: 'backend' })
      const to = { params: { slug: 'backend', teamKey: 'acme', projectKey: 'demo' } }
      await expect(ensureGitRepoIdAttached(to)).resolves.toBe('ok')
      expect(to.params.repositoryId).toBe('42')
      expect(resolveGitRepoByProjectAndSlug).toHaveBeenCalledWith(10, 'backend')
    })

    it('re-aligns stale repositoryId when switching slug', async () => {
      resolveGitRepoByProjectAndSlug.mockResolvedValue({ repositoryId: 99, slug: 'other' })
      const to = { params: { slug: 'other', repositoryId: '1' } }
      await expect(ensureGitRepoIdAttached(to)).resolves.toBe('ok')
      expect(to.params.repositoryId).toBe('99')
    })

    it('rejects clone-looking slug', async () => {
      const to = { params: { slug: 'backend.git' } }
      await expect(ensureGitRepoIdAttached(to)).resolves.toBe('not_found')
      expect(resolveGitRepoByProjectAndSlug).not.toHaveBeenCalled()
    })

    it('returns not_found when resolve misses', async () => {
      resolveGitRepoByProjectAndSlug.mockResolvedValue(null)
      const to = { params: { slug: 'missing' } }
      await expect(ensureGitRepoIdAttached(to)).resolves.toBe('not_found')
      expect(to.params.repositoryId).toBeUndefined()
    })
  })
})
