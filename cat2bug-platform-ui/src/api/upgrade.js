import request from '@/utils/request'

const upgradeHeaders = {
  isToken: false,
  repeatSubmit: false
}

/** 查询升级状态 */
export function getUpgradeStatus() {
  return request({
    url: '/upgrade/status',
    headers: upgradeHeaders,
    method: 'get',
    silentError: true
  })
}

/** 查询升级预检信息 */
export function getUpgradePreflight() {
  return request({
    url: '/upgrade/preflight',
    headers: upgradeHeaders,
    method: 'get',
    silentError: true
  })
}

/** 提交升级 */
export function submitUpgrade(data) {
  return request({
    url: '/upgrade/submit',
    headers: upgradeHeaders,
    method: 'post',
    data,
    timeout: 300000
  })
}

/** 重试升级 */
export function retryUpgrade(data) {
  return request({
    url: '/upgrade/retry',
    headers: upgradeHeaders,
    method: 'post',
    data,
    timeout: 300000
  })
}
