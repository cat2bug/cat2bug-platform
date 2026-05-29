import { getUpgradeStatus } from '@/api/upgrade'

let upgradeStatusPromise = null

function normalizeUpgradeStatus(res) {
  const payload = res && res.data ? res.data : (res || {})
  return {
    upgradeRequired: payload.upgradeRequired === true,
    state: payload.state || '',
    targetVersion: payload.targetVersion || '',
    pendingMigrations: Array.isArray(payload.pendingMigrations) ? payload.pendingMigrations : [],
    lastError: payload.lastError || '',
    lastStep: payload.lastStep || '',
    restartRequired: payload.restartRequired === true
  }
}

export function resolveUpgradeStatus(forceRefresh = false) {
  if (forceRefresh) {
    upgradeStatusPromise = null
  }
  if (!upgradeStatusPromise) {
    upgradeStatusPromise = getUpgradeStatus()
      .then(normalizeUpgradeStatus)
      .catch(() => ({
        upgradeRequired: false,
        state: '',
        targetVersion: '',
        pendingMigrations: [],
        lastError: '',
        lastStep: '',
        restartRequired: false
      }))
  }
  return upgradeStatusPromise
}

export function resetUpgradeStatusCache() {
  upgradeStatusPromise = null
}
