import request from '@/utils/request'

// 查询项目缺陷页签配置列表
export function listTabs(query) {
  return request({
    url: '/system/tabs/list',
    method: 'get',
    params: query
  })
}

// 查询项目缺陷页签配置详细
export function getTabs(tabId) {
  return request({
    url: '/system/tabs/' + tabId,
    method: 'get'
  })
}

// 新增项目缺陷页签配置
export function addTabs(data) {
  return request({
    url: '/system/tabs',
    method: 'post',
    data: data
  })
}

// 修改项目缺陷页签配置
export function updateTabs(data) {
  return request({
    url: '/system/tabs',
    method: 'put',
    data: data
  })
}

// 删除项目缺陷页签配置
export function delTabs(tabId) {
  return request({
    url: '/system/tabs/' + tabId,
    method: 'delete'
  })
}
