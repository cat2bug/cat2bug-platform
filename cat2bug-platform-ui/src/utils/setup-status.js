import { getSetupStatus } from '@/api/setup'

const SETUP_STATUS_CACHE_KEY = 'cat2bug-setup-status-cache'
const SETUP_STATUS_CACHE_TTL_MS = 30 * 60 * 1000

let setupStatusPromise = null

function normalizeSetupStatus(res) {
  const payload = res && res.data ? res.data : (res || {})
  return {
    installed: payload.installed === true,
    skipped: payload.skipped === true,
    restartRequired: payload.restartRequired === true
  }
}

function readSetupStatusCache() {
  try {
    const raw = sessionStorage.getItem(SETUP_STATUS_CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || !parsed.data || Date.now() - parsed.ts > SETUP_STATUS_CACHE_TTL_MS) {
      sessionStorage.removeItem(SETUP_STATUS_CACHE_KEY)
      return null
    }
    return parsed.data
  } catch (e) {
    return null
  }
}

function writeSetupStatusCache(data) {
  try {
    if (!data || data.restartRequired) return
    sessionStorage.setItem(SETUP_STATUS_CACHE_KEY, JSON.stringify({ ts: Date.now(), data }))
  } catch (e) {
    /* ignore */
  }
}

function fetchSetupStatus() {
  return getSetupStatus()
    .then(normalizeSetupStatus)
    .then(data => {
      writeSetupStatusCache(data)
      return data
    })
    .catch(() => ({
      installed: false,
      skipped: false,
      restartRequired: false
    }))
}

export function resolveSetupStatus(forceRefresh = false) {
  if (forceRefresh) {
    setupStatusPromise = null
    try {
      sessionStorage.removeItem(SETUP_STATUS_CACHE_KEY)
    } catch (e) {
      /* ignore */
    }
  }
  if (!setupStatusPromise) {
    const cached = !forceRefresh ? readSetupStatusCache() : null
    if (cached) {
      setupStatusPromise = Promise.resolve(cached)
      fetchSetupStatus().catch(() => {})
    } else {
      setupStatusPromise = fetchSetupStatus()
    }
  }
  return setupStatusPromise
}

/** 是否可进入正常业务（已安装且无需等待重启） */
export function resolveSetupInstalled() {
  return resolveSetupStatus().then(status => status.installed && !status.restartRequired)
}

export function resetSetupStatusCache() {
  setupStatusPromise = null
  try {
    sessionStorage.removeItem(SETUP_STATUS_CACHE_KEY)
  } catch (e) {
    /* ignore */
  }
}
