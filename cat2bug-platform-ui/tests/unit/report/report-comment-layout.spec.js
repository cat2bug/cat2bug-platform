/** @jest-environment jsdom */

import {
  getContentOffsetTop,
  resolveCommentCollisions,
  resolveDraftTopAmongComments,
  findElementByQuote,
  buildDesiredCommentTops,
  clampCommentCardHeight
} from '@/components/Report/annotation/report-comment-layout'

describe('report-comment-layout', () => {
  it('computes content offset top', () => {
    const scrollRoot = {
      scrollTop: 40,
      contains: () => true,
      getBoundingClientRect: () => ({ top: 100 })
    }
    const el = {
      getBoundingClientRect: () => ({ top: 180 })
    }
    expect(getContentOffsetTop(scrollRoot, el)).toBe(120)
  })

  it('returns null when element is detached from scroll root', () => {
    const scrollRoot = {
      scrollTop: 0,
      contains: () => false,
      getBoundingClientRect: () => ({ top: 0 })
    }
    const el = { getBoundingClientRect: () => ({ top: 50 }) }
    expect(getContentOffsetTop(scrollRoot, el)).toBeNull()
  })

  it('clamps abnormal card heights', () => {
    expect(clampCommentCardHeight(12)).toBe(48)
    expect(clampCommentCardHeight(120)).toBe(120)
    expect(clampCommentCardHeight(2000)).toBe(280)
  })

  it('keeps distant anchors in place and only pushes overlaps', () => {
    const resolved = resolveCommentCollisions([
      { id: 1, top: 100, height: 80 },
      { id: 2, top: 400, height: 80 },
      { id: 3, top: 110, height: 80 }
    ], 8)
    expect(resolved.find(r => r.id === 1).top).toBe(100)
    expect(resolved.find(r => r.id === 3).top).toBe(100 + 80 + 8)
    expect(resolved.find(r => r.id === 2).top).toBe(400)
  })

  it('does not cascade-push far anchors when first height is huge before clamp', () => {
    const resolved = resolveCommentCollisions([
      { id: 1, top: 100, height: 5000 },
      { id: 2, top: 600, height: 80 }
    ], 8)
    // height clamped to 280 → first bottom 380, second stays 600
    expect(resolved.find(r => r.id === 1).top).toBe(100)
    expect(resolved.find(r => r.id === 2).top).toBe(600)
  })

  it('builds desired tops for matched threads only', () => {
    const scrollRoot = {
      scrollTop: 0,
      contains: () => true,
      getBoundingClientRect: () => ({ top: 0 })
    }
    const el = { getBoundingClientRect: () => ({ top: 50 }) }
    const tops = buildDesiredCommentTops([
      { root: { commentId: 1 }, matched: true, element: el },
      { root: { commentId: 2 }, matched: false, element: null }
    ], scrollRoot, 0)
    expect(tops['1']).toBe(50)
    expect(tops['2']).toBeUndefined()
  })

  it('keeps draft at desired top when no occupied cards', () => {
    expect(resolveDraftTopAmongComments(240, [], 120)).toBe(240)
  })

  it('pushes draft below an overlapping existing comment', () => {
    // existing card [8, 96), draft wants 40 → push to 96+8
    expect(resolveDraftTopAmongComments(40, [{ top: 8, height: 88 }], 120, 8)).toBe(104)
  })

  it('does not cover a higher card when draft target is below', () => {
    // existing at top, draft wants mid-page → stay
    expect(resolveDraftTopAmongComments(400, [{ top: 8, height: 88 }], 120, 8)).toBe(400)
  })

  it('finds element by quote text', () => {
    const root = document.createElement('div')
    root.innerHTML = '<h2>标题二</h2><h2>飞书地方就哭了</h2>'
    expect(findElementByQuote(root, '飞书地方就哭了').textContent).toBe('飞书地方就哭了')
    expect(findElementByQuote(root, '不存在')).toBeNull()
  })
})
