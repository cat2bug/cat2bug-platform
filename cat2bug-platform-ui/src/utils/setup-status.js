import { getSetupPreflight, getSetupStatus } from '@/api/setup'

const SETUP_STATUS_CACHE_KEY = 'cat2bug-setup-status-cache'
const SETUP_STATUS_CACHE_TTL_MS = 30 * 60 * 1000
const ALREADY_INSTALLED_RE = /已完成安装|already\s*install/i

let setupStatusPromise = null
let setupStatusSnapshot = null

function normalizeSetupStatus(res) {
  const payload = res && res.data ? res.data : (res || {})
  // 兼容异常体被当成成功包一层的情况
  if (payload && payload.code && payload.code !== 200 && payload.installed === undefined) {
    return null
  }
  return {
    installed: payload.installed === true,
    skipped: payload.skipped === true,
    restartRequired: payload.restartRequired === true
  }
}

function extractErrorMessage(err) {
  if (!err) return ''
  if (typeof err === 'string') return err
  if (err.msg) return String(err.msg)
  if (err.message) return String(err.message)
  return ''
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

function applySetupStatus(data) {
  setupStatusSnapshot = data
  setupStatusPromise = Promise.resolve(data)
  writeSetupStatusCache(data)
  return data
}

function fallbackFromCacheOrUnknown() {
  if (setupStatusSnapshot) {
    return setupStatusSnapshot
  }
  const cached = readSetupStatusCache()
  if (cached) {
    setupStatusSnapshot = cached
    setupStatusPromise = Promise.resolve(cached)
    return cached
  }
  return {
    installed: false,
    skipped: false,
    restartRequired: false
  }
}

/**
 * /setup/status 异常时，用 SetupFilter 对已安装接口的拦截文案判断是否已安装。
 * 避免 status→/error→401 时前端误判未安装、把登录也导回向导。
 */
function probeInstalledViaSetupFilter() {
  return getSetupPreflight()
    .then(() => applySetupStatus({
      installed: false,
      skipped: false,
      restartRequired: false
    }))
    .catch(err => {
      const msg = extractErrorMessage(err)
      if (ALREADY_INSTALLED_RE.test(msg)) {
        return applySetupStatus({
          installed: true,
          skipped: false,
          restartRequired: false
        })
      }
      return fallbackFromCacheOrUnknown()
    })
}

const SETUP_STATUS_REQUEST_TIMEOUT_MS = 8000

function withTimeout(promise, ms, onTimeout) {
  let timer
  return Promise.race([
    promise.finally(() => clearTimeout(timer)),
    new Promise(resolve => {
      timer = setTimeout(() => resolve(onTimeout()), ms)
    })
  ])
}

function fetchSetupStatus() {
  return withTimeout(
    getSetupStatus()
      .then(res => {
        const normalized = normalizeSetupStatus(res)
        if (!normalized) {
          return probeInstalledViaSetupFilter()
        }
        return applySetupStatus(normalized)
      })
      .catch(() => probeInstalledViaSetupFilter()),
    SETUP_STATUS_REQUEST_TIMEOUT_MS,
    () => fallbackFromCacheOrUnknown()
  )
}

export function resolveSetupStatus(forceRefresh = false) {
  if (forceRefresh) {
    // 仅作废进行中的请求；保留内存/会话缓存作超时回退，避免接口挂起时误判未安装把登录导去向导
    setupStatusPromise = null
  }
  if (!setupStatusPromise) {
    const cached = readSetupStatusCache()
    if (cached && !setupStatusSnapshot) {
      setupStatusSnapshot = cached
    }
    if (cached && !forceRefresh) {
      setupStatusPromise = Promise.resolve(cached)
      // 后台刷新并更新内存快照，避免 SPA 会话内一直用陈旧的 installed:false
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
  setupStatusSnapshot = null
  try {
    sessionStorage.removeItem(SETUP_STATUS_CACHE_KEY)
  } catch (e) {
    /* ignore */
  }
}
