import {
  isUpgradeLockMessage,
  resetUpgradeStatusCache,
  resolveUpgradeStatus
} from '@/utils/upgrade-status'

jest.mock('@/api/upgrade', () => ({
  getUpgradeStatus: jest.fn()
}))

const { getUpgradeStatus } = require('@/api/upgrade')

function mockSessionStorage() {
  const store = {}
  return {
    getItem: jest.fn(key => (Object.prototype.hasOwnProperty.call(store, key) ? store[key] : null)),
    setItem: jest.fn((key, value) => {
      store[key] = String(value)
    }),
    removeItem: jest.fn(key => {
      delete store[key]
    }),
    clear: jest.fn(() => {
      Object.keys(store).forEach(key => delete store[key])
    })
  }
}

describe('upgrade-status', () => {
  beforeEach(() => {
    Object.defineProperty(global, 'sessionStorage', {
      value: mockSessionStorage(),
      configurable: true
    })
    resetUpgradeStatusCache()
    getUpgradeStatus.mockReset()
  })

  test('isUpgradeLockMessage matches UpgradeFilter copy', () => {
    expect(isUpgradeLockMessage('系统正在升级，请先完成升级向导')).toBe(true)
    expect(isUpgradeLockMessage('Please complete the upgrade wizard')).toBe(true)
    expect(isUpgradeLockMessage('normal error')).toBe(false)
  })

  test('stale not-required cache is overridden by fresh pending status', async () => {
    sessionStorage.setItem('cat2bug-upgrade-status-cache', JSON.stringify({
      ts: Date.now(),
      data: {
        upgradeRequired: false,
        state: 'completed',
        pendingMigrations: [],
        restartRequired: false
      }
    }))
    getUpgradeStatus.mockResolvedValue({
      code: 200,
      data: {
        upgradeRequired: true,
        state: 'pending',
        pendingMigrations: ['1.1.0.11'],
        restartRequired: false
      }
    })

    const status = await resolveUpgradeStatus(false)
    expect(status.upgradeRequired).toBe(true)
    expect(status.state).toBe('pending')
  })

  test('pending upgrade is not written to sessionStorage', async () => {
    getUpgradeStatus.mockResolvedValue({
      code: 200,
      data: {
        upgradeRequired: true,
        state: 'pending',
        pendingMigrations: ['1.1.0.11'],
        restartRequired: false
      }
    })
    await resolveUpgradeStatus(true)
    expect(sessionStorage.getItem('cat2bug-upgrade-status-cache')).toBeNull()
  })
})
