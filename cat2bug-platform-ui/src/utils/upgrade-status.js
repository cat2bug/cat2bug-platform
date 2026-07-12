import { getUpgradeStatus } from '@/api/upgrade'

const UPGRADE_STATUS_CACHE_KEY = 'cat2bug-upgrade-status-cache'
const UPGRADE_STATUS_CACHE_TTL_MS = 30 * 60 * 1000

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

function isUpgradeStatusCacheable(data) {
  if (!data) return false
  if (data.restartRequired) return false
  if (data.upgradeRequired && ['pending', 'running', 'failed'].includes(data.state)) return false
  return true
}

function readUpgradeStatusCache() {
  try {
    const raw = sessionStorage.getItem(UPGRADE_STATUS_CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || !parsed.data || Date.now() - parsed.ts > UPGRADE_STATUS_CACHE_TTL_MS) {
      sessionStorage.removeItem(UPGRADE_STATUS_CACHE_KEY)
      return null
    }
    return parsed.data
  } catch (e) {
    return null
  }
}

function writeUpgradeStatusCache(data) {
  try {
    if (!isUpgradeStatusCacheable(data)) return
    sessionStorage.setItem(UPGRADE_STATUS_CACHE_KEY, JSON.stringify({ ts: Date.now(), data }))
  } catch (e) {
    /* ignore */
  }
}

function fetchUpgradeStatus() {
  return getUpgradeStatus()
    .then(normalizeUpgradeStatus)
    .then(data => {
      writeUpgradeStatusCache(data)
      return data
    })
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

export function resolveUpgradeStatus(forceRefresh = false) {
  if (forceRefresh) {
    upgradeStatusPromise = null
    try {
      sessionStorage.removeItem(UPGRADE_STATUS_CACHE_KEY)
    } catch (e) {
      /* ignore */
    }
  }
  if (!upgradeStatusPromise) {
    const cached = !forceRefresh ? readUpgradeStatusCache() : null
    if (cached) {
      upgradeStatusPromise = Promise.resolve(cached)
      fetchUpgradeStatus().catch(() => {})
    } else {
      upgradeStatusPromise = fetchUpgradeStatus()
    }
  }
  return upgradeStatusPromise
}

export function resetUpgradeStatusCache() {
  upgradeStatusPromise = null
  try {
    sessionStorage.removeItem(UPGRADE_STATUS_CACHE_KEY)
  } catch (e) {
    /* ignore */
  }
}
