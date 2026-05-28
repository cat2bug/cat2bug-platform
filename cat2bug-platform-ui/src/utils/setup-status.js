import { getSetupStatus } from '@/api/setup'

let setupInstalledPromise = null

export function resolveSetupInstalled() {
  if (!setupInstalledPromise) {
    setupInstalledPromise = getSetupStatus()
      .then(res => {
        const payload = res && res.data ? res.data : res
        return payload && payload.installed === true
      })
      .catch(() => false)
  }
  return setupInstalledPromise
}

export function resetSetupStatusCache() {
  setupInstalledPromise = null
}
