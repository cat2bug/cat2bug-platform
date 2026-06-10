import { scrollVerticalContainerByArrow } from '@/utils/scroll-vertical-by-arrow'

describe('scrollVerticalContainerByArrow', () => {
  it('scrolls down by viewport ratio', () => {
    const el = {
      clientHeight: 400,
      scrollHeight: 1200,
      scrollTop: 0,
      scrollBy: jest.fn()
    }
    expect(scrollVerticalContainerByArrow(el, 'ArrowDown')).toBe(true)
    expect(el.scrollBy).toHaveBeenCalledWith({ top: 160, behavior: 'smooth' })
  })

  it('scrolls up by viewport ratio', () => {
    const el = {
      clientHeight: 400,
      scrollHeight: 1200,
      scrollTop: 200,
      scrollBy: jest.fn()
    }
    expect(scrollVerticalContainerByArrow(el, 'ArrowUp')).toBe(true)
    expect(el.scrollBy).toHaveBeenCalledWith({ top: -160, behavior: 'smooth' })
  })

  it('returns false when container is not scrollable', () => {
    const el = {
      clientHeight: 400,
      scrollHeight: 400,
      scrollTop: 0,
      scrollBy: jest.fn()
    }
    expect(scrollVerticalContainerByArrow(el, 'ArrowDown')).toBe(false)
    expect(el.scrollBy).not.toHaveBeenCalled()
  })
})
