/** @jest-environment jsdom */
import { textContainsRawDslTokens } from '@/components/ReportTemplateEditor/milkdown/dsl-doc-sync'

describe('textContainsRawDslTokens', () => {
  it('detects unparsed $table token', () => {
    expect(textContainsRawDslTokens('$table{api.defect.list[projectNum]}')).toBe(true)
  })

  it('detects unparsed ${variable}', () => {
    expect(textContainsRawDslTokens('项目名称：${api.project.projectName}')).toBe(true)
  })

  it('detects $image token', () => {
    expect(textContainsRawDslTokens('$image{api.project.projectIcon}')).toBe(true)
  })

  it('returns false for plain text', () => {
    expect(textContainsRawDslTokens('hello world')).toBe(false)
  })
})
