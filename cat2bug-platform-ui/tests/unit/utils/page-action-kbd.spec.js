/** @jest-environment jsdom */

import {
  PAGE_KBD_OVERLAY_ID,
  hasActivePageRowKbdHints,
  resolvePageActionLetter
} from '@/utils/defect-row-kbd-hints'

describe('page-action-kbd helpers', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('resolvePageActionLetter normalizes digit row keys', () => {
    expect(resolvePageActionLetter({ key: '1' })).toBe('1')
    expect(resolvePageActionLetter({ key: 'a' })).toBe('A')
    expect(resolvePageActionLetter({ key: 'Z' })).toBe('Z')
    expect(resolvePageActionLetter({ code: 'Digit3', key: 'Process' })).toBe('3')
    expect(resolvePageActionLetter({ code: 'Numpad7', key: 'Process' })).toBe('7')
  })

  it('hasActivePageRowKbdHints detects row hint badges in overlay', () => {
    expect(hasActivePageRowKbdHints()).toBe(false)
    const overlay = document.createElement('div')
    overlay.id = PAGE_KBD_OVERLAY_ID
    const badge = document.createElement('span')
    badge.className = 'cat2bug-page-kbd-hint-float defect-row-kbd-hint-float'
    badge.textContent = '1'
    overlay.appendChild(badge)
    document.body.appendChild(overlay)
    expect(hasActivePageRowKbdHints()).toBe(true)
  })
})
