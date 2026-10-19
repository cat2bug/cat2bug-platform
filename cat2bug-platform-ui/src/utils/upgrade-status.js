import { getUpgradeStatus } from '@/api/upgrade'

const UPGRADE_STATUS_CACHE_KEY = 'cat2bug-upgrade-status-cache'
const UPGRADE_STATUS_CACHE_TTL_MS = 30 * 60 * 1000

let upgradeStatusPromise = null
let upgradeStatusSnapshot = null

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

/** 升级进行中/待重启不得写入长期缓存，避免路由误判留在登录页 */
function isUpgradeStatusCacheable(data) {
  if (!data) return false
  if (data.restartRequired) return false
  if (data.upgradeRequired) return false
  if (['pending', 'running', 'failed', 'restart_required'].includes(data.state)) return false
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
    // 防御：旧版曾把 upgradeRequired=true 写入缓存
    if (!isUpgradeStatusCacheable(parsed.data)) {
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
    if (!isUpgradeStatusCacheable(data)) {
      sessionStorage.removeItem(UPGRADE_STATUS_CACHE_KEY)
      return
    }
    sessionStorage.setItem(UPGRADE_STATUS_CACHE_KEY, JSON.stringify({ ts: Date.now(), data }))
  } catch (e) {
    /* ignore */
  }
}

function applyUpgradeStatus(data) {
  upgradeStatusSnapshot = data
  upgradeStatusPromise = Promise.resolve(data)
  writeUpgradeStatusCache(data)
  return data
}

function fallbackUpgradeStatus() {
  if (upgradeStatusSnapshot) {
    return upgradeStatusSnapshot
  }
  const cached = readUpgradeStatusCache()
  if (cached) {
    upgradeStatusSnapshot = cached
    return cached
  }
  return {
    upgradeRequired: false,
    state: '',
    targetVersion: '',
    pendingMigrations: [],
    lastError: '',
    lastStep: '',
    restartRequired: false
  }
}

const UPGRADE_STATUS_REQUEST_TIMEOUT_MS = 8000

function withTimeout(promise, ms, onTimeout) {
  let timer
  return Promise.race([
    promise.finally(() => clearTimeout(timer)),
    new Promise(resolve => {
      timer = setTimeout(() => resolve(onTimeout()), ms)
    })
  ])
}

function fetchUpgradeStatus() {
  return withTimeout(
    getUpgradeStatus()
      .then(normalizeUpgradeStatus)
      .then(applyUpgradeStatus)
      .catch(() => applyUpgradeStatus(fallbackUpgradeStatus())),
    UPGRADE_STATUS_REQUEST_TIMEOUT_MS,
    () => applyUpgradeStatus(fallbackUpgradeStatus())
  )
}

/**
 * @param {boolean} [forceRefresh=false]
 * @returns {Promise<object>}
 */
export function resolveUpgradeStatus(forceRefresh = false) {
  if (forceRefresh) {
    // 保留快照/缓存作超时回退，避免接口挂起时路由永久等在空白页
    upgradeStatusPromise = null
  }
  if (!upgradeStatusPromise) {
    // 始终以网络结果为准；缓存仅作请求失败/超时回退
    upgradeStatusPromise = fetchUpgradeStatus()
  }
  return upgradeStatusPromise
}

export function resetUpgradeStatusCache() {
  upgradeStatusPromise = null
  upgradeStatusSnapshot = null
  try {
    sessionStorage.removeItem(UPGRADE_STATUS_CACHE_KEY)
  } catch (e) {
    /* ignore */
  }
}

/** 后端 UpgradeFilter 锁业务 API 时的提示（中/英） */
export function isUpgradeLockMessage(msg) {
  return /系统正在升级|完成升级向导|upgrade wizard|system is upgrading/i.test(String(msg || ''))
}
