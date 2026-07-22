import { projectOptionHref } from '@/utils/project-option-card'

describe('projectOptionHref', () => {
  it('prefixes menu segments under /project/', () => {
    expect(projectOptionHref('ollama')).toBe('/project/ollama')
    expect(projectOptionHref('project-member')).toBe('/project/project-member')
    expect(projectOptionHref('defect-fields')).toBe('/project/defect-fields')
    expect(projectOptionHref('push')).toBe('/project/push')
  })

  it('accepts already-qualified project paths', () => {
    expect(projectOptionHref('/project/push')).toBe('/project/push')
    expect(projectOptionHref('project/openai')).toBe('/project/openai')
  })

  it('falls back to option hub when empty', () => {
    expect(projectOptionHref('')).toBe('/project/option')
    expect(projectOptionHref(null)).toBe('/project/option')
  })
})
