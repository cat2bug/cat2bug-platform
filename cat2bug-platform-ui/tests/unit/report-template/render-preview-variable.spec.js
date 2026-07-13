import { formatPreviewScalar } from '@/components/ReportTemplateEditor/milkdown/render-preview-variable'

describe('render-preview-variable', () => {
  it('formatPreviewScalar stringifies scalars', () => {
    expect(formatPreviewScalar('hello')).toBe('hello')
    expect(formatPreviewScalar(42)).toBe('42')
    expect(formatPreviewScalar(null)).toBeNull()
    expect(formatPreviewScalar('')).toBeNull()
  })
})
