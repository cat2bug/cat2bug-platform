import { getSetupStatus } from '@/api/setup'

let setupStatusPromise = null

function normalizeSetupStatus(res) {
  const payload = res && res.data ? res.data : (res || {})
  return {
    installed: payload.installed === true,
    skipped: payload.skipped === true,
    restartRequired: payload.restartRequired === true
  }
}

export function resolveSetupStatus(forceRefresh = false) {
  if (forceRefresh) {
    setupStatusPromise = null
  }
  if (!setupStatusPromise) {
    setupStatusPromise = getSetupStatus()
      .then(normalizeSetupStatus)
      .catch(() => ({
        installed: false,
        skipped: false,
        restartRequired: false
      }))
  }
  return setupStatusPromise
}

/** 是否可进入正常业务（已安装且无需等待重启） */
export function resolveSetupInstalled() {
  return resolveSetupStatus().then(status => status.installed && !status.restartRequired)
}

export function resetSetupStatusCache() {
  setupStatusPromise = null
}
