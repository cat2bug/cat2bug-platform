/** sessionStorage：升级/重启完成后等待应用就绪，就绪前禁止路由离开 /upgrade */
export const UPGRADE_AWAITING_READY_KEY = 'upgrade-awaiting-ready'

export function isUpgradeAwaitingReady() {
  try {
    return sessionStorage.getItem(UPGRADE_AWAITING_READY_KEY) === '1'
  } catch (e) {
    return false
  }
}

export function markUpgradeAwaitingReady() {
  try {
    sessionStorage.setItem(UPGRADE_AWAITING_READY_KEY, '1')
  } catch (e) {
    /* ignore */
  }
}

export function clearUpgradeAwaitingReady() {
  try {
    sessionStorage.removeItem(UPGRADE_AWAITING_READY_KEY)
  } catch (e) {
    /* ignore */
  }
}
