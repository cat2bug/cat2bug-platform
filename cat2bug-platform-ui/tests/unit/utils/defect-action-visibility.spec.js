import {
  getBatchActionFlags,
  isMergeBatchActionAvailable,
  PROCESSING_STATE,
  AUDIT_STATE
} from '@/utils/defect-action-visibility'

jest.mock('@/utils/permission', () => ({
  checkPermi: jest.fn((perms) => {
    if (!perms || !perms.length) return false
    if (perms.includes('system:defect:merge')) return true
    return false
  })
}))

const { checkPermi } = require('@/utils/permission')

function defect(id, state = PROCESSING_STATE) {
  return {
    defectId: id,
    projectNum: id,
    defectStateName: state,
    delFlag: '0',
    handleBy: [1],
    createById: 1
  }
}

describe('isMergeBatchActionAvailable', () => {
  beforeEach(() => {
    checkPermi.mockImplementation((perms) => perms && perms.includes('system:defect:merge'))
  })

  it('requires at least two non-deleted defects and merge permission', () => {
    expect(isMergeBatchActionAvailable([defect(1)])).toBe(false)
    expect(isMergeBatchActionAvailable([defect(1), defect(2)])).toBe(true)
    expect(isMergeBatchActionAvailable([defect(1), { ...defect(2), delFlag: '2' }])).toBe(false)
  })

  it('is false without merge permission', () => {
    checkPermi.mockReturnValue(false)
    expect(isMergeBatchActionAvailable([defect(1), defect(2)])).toBe(false)
  })
})

describe('getBatchActionFlags merge', () => {
  beforeEach(() => {
    checkPermi.mockImplementation((perms) => {
      if (perms && perms.includes('system:defect:merge')) return true
      return false
    })
  })

  it('allows merge when states are mixed', () => {
    const flags = getBatchActionFlags(
      [defect(1, PROCESSING_STATE), defect(2, AUDIT_STATE)],
      1
    )
    expect(flags.mixed).toBe(true)
    expect(flags.merge).toBe(true)
    expect(flags.repair).toBe(false)
  })

  it('disables merge for single selection', () => {
    const flags = getBatchActionFlags([defect(1)], 1)
    expect(flags.merge).toBe(false)
  })
})
