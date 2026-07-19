import { withPreviewImageFit } from '@/components/ReportTemplateEditor/milkdown/preview-image-style'

describe('withPreviewImageFit', () => {
  it('adds object-fit:cover when both width and height are set', () => {
    expect(withPreviewImageFit({ width: '100px', height: '100px' })).toEqual({
      width: '100px',
      height: '100px',
      'object-fit': 'cover',
      'object-position': 'center'
    })
  })

  it('does not override an explicit object-fit', () => {
    expect(
      withPreviewImageFit({
        width: '100px',
        height: '100px',
        'object-fit': 'contain'
      })
    ).toEqual({
      width: '100px',
      height: '100px',
      'object-fit': 'contain'
    })
  })

  it('leaves width-only styles alone', () => {
    expect(withPreviewImageFit({ width: '100px' })).toEqual({ width: '100px' })
  })
})
