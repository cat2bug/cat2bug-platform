import {
  pathMatchToSegments,
  createLegacyProjectModuleRedirect
} from '@/utils/legacy-project-module-redirect'

jest.mock('@/api/system/project', () => ({
  getProject: jest.fn()
}))

jest.mock('@/utils/store-context', () => ({
  getCurrentProjectId: jest.fn()
}))

import { getProject } from '@/api/system/project'
import { getCurrentProjectId } from '@/utils/store-context'

describe('legacy-project-module-redirect', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('pathMatchToSegments handles array and string forms', () => {
    expect(pathMatchToSegments(['x', 'y'])).toEqual(['x', 'y'])
    expect(pathMatchToSegments('x/y')).toEqual(['x', 'y'])
    expect(pathMatchToSegments('')).toEqual([])
  })

  it('redirects to namespaced path when project keys exist', async () => {
    getCurrentProjectId.mockReturnValue(9)
    getProject.mockResolvedValue({
      data: { teamKey: 'acme', projectKey: 'demo', projectId: 9 }
    })
    const guard = createLegacyProjectModuleRedirect('defect')
    const next = jest.fn()
    await guard(
      { params: { pathMatch: ['extra'] }, query: { q: '1' }, hash: '' },
      {},
      next
    )
    expect(next).toHaveBeenCalledWith({
      path: '/acme/demo/defect/extra',
      query: { q: '1' },
      hash: '',
      replace: true
    })
  })

  it('falls back to /index when no current project keys', async () => {
    getCurrentProjectId.mockReturnValue(0)
    const guard = createLegacyProjectModuleRedirect('plan')
    const next = jest.fn()
    await guard({ params: {}, query: {}, hash: '' }, {}, next)
    expect(next).toHaveBeenCalledWith({ path: '/index', replace: true })
    expect(getProject).not.toHaveBeenCalled()
  })
})
