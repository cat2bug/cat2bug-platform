import { computeAlignScrollTop } from '@/components/Report/annotation/report-scroll-align'

describe('report-scroll-align', () => {
  it('aligns target top to peer client Y within scroll bounds', () => {
    const scrollRoot = {
      scrollTop: 100,
      scrollHeight: 1000,
      clientHeight: 400,
    }
    const targetEl = {
      getBoundingClientRect: () => ({ top: 300 })
    }
    // peer at 150 → need to scroll up by 150 → scrollTop 100-150 clamped? 100 + (300-150) = 250
    expect(computeAlignScrollTop(scrollRoot, targetEl, 150)).toBe(250)
  })

  it('clamps to scroll range', () => {
    const scrollRoot = {
      scrollTop: 0,
      scrollHeight: 500,
      clientHeight: 400,
    }
    const targetEl = {
      getBoundingClientRect: () => ({ top: 50 })
    }
    // peer at 400 → delta -350 → scrollTop -350 → clamp 0
    expect(computeAlignScrollTop(scrollRoot, targetEl, 400)).toBe(0)
  })

  it('returns null for invalid args', () => {
    expect(computeAlignScrollTop(null, {}, 10)).toBeNull()
    expect(computeAlignScrollTop({}, null, 10)).toBeNull()
    expect(computeAlignScrollTop({}, { getBoundingClientRect: () => ({ top: 1 }) }, NaN)).toBeNull()
  })
})
