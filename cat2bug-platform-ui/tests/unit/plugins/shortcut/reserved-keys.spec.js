import {
  BROWSER_RESERVED_SYMBOLS,
  PAGE_ACTION_RESERVED,
  FIELD_HINT_BLOCKED,
  ROW_KBD_RESERVED,
  SETTINGS_NEVER_BIND,
  isReservedForScope,
  isReservedSymbol
} from '@/plugins/shortcut/reserved-keys'
import {
  isBindingLetterAllowed,
  validateOverride
} from '@/plugins/shortcut/shortcut-store'

const PAGE_LETTERS = ['A', 'C', 'M', 'N', 'Q', 'T', 'V', 'X', 'Z']
const FIELD_EXTRA = ['W', 'R', 'L', 'H']
const SETTINGS_LETTERS = [...PAGE_LETTERS, ...FIELD_EXTRA]

describe('reserved-keys', () => {
  it('exports PAGE_ACTION_RESERVED with expected letters', () => {
    expect([...PAGE_ACTION_RESERVED].sort()).toEqual(PAGE_LETTERS.sort())
  })

  it('FIELD_HINT_BLOCKED is PAGE union plus W,R,L,H', () => {
    FIELD_EXTRA.forEach((ch) => expect(FIELD_HINT_BLOCKED.has(ch)).toBe(true))
    PAGE_LETTERS.forEach((ch) => expect(FIELD_HINT_BLOCKED.has(ch)).toBe(true))
    expect(FIELD_HINT_BLOCKED.size).toBe(SETTINGS_LETTERS.length)
  })

  it('ROW_KBD_RESERVED aliases PAGE_ACTION_RESERVED', () => {
    expect(ROW_KBD_RESERVED).toBe(PAGE_ACTION_RESERVED)
  })

  it('SETTINGS_NEVER_BIND matches field blocked set', () => {
    expect([...SETTINGS_NEVER_BIND].sort()).toEqual([...FIELD_HINT_BLOCKED].sort())
  })

  it('BROWSER_RESERVED_SYMBOLS blocks bracket keys', () => {
    expect(BROWSER_RESERVED_SYMBOLS.has('[')).toBe(true)
    expect(BROWSER_RESERVED_SYMBOLS.has(']')).toBe(true)
    expect(isReservedSymbol('[')).toBe(true)
    expect(isReservedForScope('[', 'settings')).toBe(true)
  })

  it('isReservedForScope respects scope boundaries', () => {
    expect(isReservedForScope('t', 'page')).toBe(true)
    expect(isReservedForScope('W', 'page')).toBe(false)
    expect(isReservedForScope('W', 'field')).toBe(true)
    expect(isReservedForScope('W', 'settings')).toBe(true)
    expect(isReservedForScope('D', 'settings')).toBe(false)
    expect(isReservedForScope('M', 'row')).toBe(true)
    expect(isReservedForScope('L', 'row')).toBe(false)
  })

  it('isReservedForScope normalizes lowercase input', () => {
    expect(isReservedForScope('a', 'page')).toBe(true)
    expect(isReservedForScope(' w ', 'field')).toBe(true)
  })
})

describe('shortcut-store reserved validation', () => {
  it('validateOverride rejects bracket symbols', () => {
    expect(validateOverride('action.defect.query', '[', 'Q')).toEqual({ ok: false, reason: 'reserved' })
    expect(validateOverride('action.defect.query', ']', 'Q')).toEqual({ ok: false, reason: 'reserved' })
  })

  it('validateOverride rejects SETTINGS_NEVER_BIND letters', () => {
    expect(validateOverride('action.defect.query', 'T', 'Q')).toEqual({ ok: false, reason: 'reserved' })
    expect(validateOverride('action.defect.query', 'W', 'Q')).toEqual({ ok: false, reason: 'reserved' })
  })

  it('validateOverride allows empty or default-equivalent values', () => {
    expect(validateOverride('action.defect.query', '', 'Q')).toEqual({ ok: true })
    expect(validateOverride('action.defect.query', 'Q', 'Q')).toEqual({ ok: true })
  })

  it('validateOverride allows non-reserved letters', () => {
    expect(validateOverride('action.defect.query', 'D', 'Q')).toEqual({ ok: true })
  })

  it('isBindingLetterAllowed mirrors settings scope', () => {
    expect(isBindingLetterAllowed('N')).toBe(false)
    expect(isBindingLetterAllowed('H')).toBe(false)
    expect(isBindingLetterAllowed('D')).toBe(true)
    expect(isBindingLetterAllowed('')).toBe(true)
  })
})
