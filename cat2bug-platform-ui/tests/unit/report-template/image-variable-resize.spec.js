import {
  applyBoxSizeToStyle,
  applyImageTransformToStyle,
  buildDynamicImageRaw,
  parseRotateDeg,
  resetImageTransformStyle,
  serializeInlineStyle,
  withRotateTransform
} from '@/components/ReportTemplateEditor/milkdown/preview-image-style'
import {
  normalizeRotateDeg,
  parseCssPx,
  pointerAngleDeg,
  resizeBoxFromHandle,
  resolveImageBoxSize,
  rotatedBoundingBox,
  scaleBoxFromDelta,
  toLocalDelta
} from '@/components/ReportTemplateEditor/milkdown/image-variable-resize'

describe('image variable resize helpers', () => {
  it('serializes and applies box size while keeping object-fit', () => {
    expect(serializeInlineStyle({ width: '100px', height: '80px' })).toBe('width:100px;height:80px')
    expect(applyBoxSizeToStyle('width:100px;height:100px;', 120, 90)).toBe(
      'width:120px;height:90px;object-fit:cover;object-position:center'
    )
    expect(buildDynamicImageRaw('api.project.projectIcon', 'width:120px;height:90px')).toBe(
      '$image["style":"width:120px;height:90px"]{api.project.projectIcon}'
    )
  })

  it('parses px sizes and falls back to measured box', () => {
    expect(parseCssPx('200px')).toBe(200)
    expect(parseCssPx('auto')).toBeNull()
    expect(resolveImageBoxSize('width:160px;height:120px;', { width: 10, height: 10 })).toEqual({
      width: 160,
      height: 120,
      rotate: 0
    })
    expect(resolveImageBoxSize('', { width: 88, height: 66 })).toEqual({
      width: 88,
      height: 66,
      rotate: 0
    })
  })

  it('scales proportionally from drag delta with min/max clamps', () => {
    expect(scaleBoxFromDelta(100, 50, 50)).toEqual({ width: 150, height: 75 })
    // Min height 40 forces width to 80 when aspect is 1:2
    expect(scaleBoxFromDelta(100, 50, -100)).toEqual({ width: 80, height: 40 })
    expect(scaleBoxFromDelta(100, 50, 5000).width).toBeLessThanOrEqual(960)
  })

  it('resizes freely from named handles without forcing aspect ratio', () => {
    expect(resizeBoxFromHandle('se', 100, 80, 20, 10)).toEqual({ width: 120, height: 90 })
    expect(resizeBoxFromHandle('e', 100, 80, 40, 999)).toEqual({ width: 140, height: 80 })
    expect(resizeBoxFromHandle('n', 100, 80, 999, -20)).toEqual({ width: 100, height: 100 })
    expect(resizeBoxFromHandle('nw', 100, 80, -10, -10)).toEqual({ width: 110, height: 90 })
  })

  it('keeps aspect ratio when keepAspect is set on corner drag', () => {
    expect(resizeBoxFromHandle('se', 100, 50, 50, 0, { keepAspect: true })).toEqual({
      width: 150,
      height: 75
    })
  })

  it('computes axis-aligned layout box for rotated images', () => {
    expect(rotatedBoundingBox(100, 50, 0)).toEqual({ width: 100, height: 50 })
    expect(rotatedBoundingBox(100, 50, 90)).toEqual({ width: 50, height: 100 })
    const box = rotatedBoundingBox(100, 50, 45)
    expect(box.width).toBeGreaterThan(100)
    expect(box.height).toBeGreaterThan(50)
    expect(box.width).toBe(box.height)
  })

  it('parses and writes rotate transform on image style', () => {
    expect(parseRotateDeg('width:100px;transform:rotate(15deg)')).toBe(15)
    expect(withRotateTransform('', 30)).toBe('rotate(30deg)')
    expect(withRotateTransform('rotate(10deg)', 0)).toBe('')
    expect(applyImageTransformToStyle('width:100px;height:80px', 120, 90, 15)).toContain(
      'transform:rotate(15deg)'
    )
    expect(normalizeRotateDeg(370)).toBe(10)
    expect(pointerAngleDeg(0, 0, 0, -10)).toBe(-90)
    expect(toLocalDelta(10, 0, 90).y).toBeCloseTo(-10, 5)
  })

  it('resets size and rotate from image style', () => {
    expect(
      resetImageTransformStyle(
        'width:120px;height:90px;object-fit:cover;object-position:center;transform:rotate(15deg)'
      )
    ).toBe('')
    expect(resetImageTransformStyle('width:120px;border-radius:8px')).toBe('border-radius:8px')
  })
})
