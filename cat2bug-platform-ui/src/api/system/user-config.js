import request from '@/utils/request'

// 查询用户配置列表
export function listConfig(query) {
  return request({
    url: '/system/user-config/list',
    method: 'get',
    params: query
  })
}

// 查询用户配置详细
export function getConfig(userConfigId) {
  return request({
    url: '/system/user-config/' + userConfigId,
    method: 'get'
  })
}

// 新增用户配置
export function addConfig(data) {
  return request({
    url: '/system/user-config',
    method: 'post',
    data: data
  })
}

// 修改用户配置
export function updateConfig(data) {
  return request({
    url: '/system/user-config',
    method: 'put',
    data: data
  })
}

// 删除用户配置
export function delConfig(userConfigId) {
  return request({
    url: '/system/user-config/' + userConfigId,
    method: 'delete'
  })
}
