import request from '@/utils/request'

/** 固定「工作台」个人布局 */
export function getWorkbenchLayout(projectId) {
  return request({
    url: '/system/workbench/' + projectId,
    method: 'get'
  })
}

export function saveWorkbenchLayout(projectId, configJson) {
  return request({
    url: '/system/workbench/' + projectId,
    method: 'put',
    data: { configJson: typeof configJson === 'string' ? configJson : JSON.stringify(configJson) }
  })
}

/** 自定义页签：共享 ∪ 我的私有 */
export function listWorkbenchTabs(projectId) {
  return request({
    url: '/system/workbench/' + projectId + '/tabs',
    method: 'get'
  })
}

export function createWorkbenchTab(projectId, payload) {
  const data = { ...payload }
  if (data.configJson != null && typeof data.configJson !== 'string') {
    data.configJson = JSON.stringify(data.configJson)
  }
  return request({
    url: '/system/workbench/' + projectId + '/tabs',
    method: 'post',
    data
  })
}

export function updateWorkbenchTab(projectId, tabId, payload) {
  const data = { ...payload }
  if (data.configJson != null && typeof data.configJson !== 'string') {
    data.configJson = JSON.stringify(data.configJson)
  }
  return request({
    url: '/system/workbench/' + projectId + '/tabs/' + tabId,
    method: 'put',
    data
  })
}

export function deleteWorkbenchTab(projectId, tabId) {
  return request({
    url: '/system/workbench/' + projectId + '/tabs/' + tabId,
    method: 'delete'
  })
}

/** 同可见性组内排序：[{ tabId, tabSort }, ...] */
export function sortWorkbenchTabs(projectId, list) {
  return request({
    url: '/system/workbench/' + projectId + '/tabs/sort',
    method: 'put',
    data: list
  })
}
