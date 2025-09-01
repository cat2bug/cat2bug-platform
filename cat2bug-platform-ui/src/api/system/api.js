import request from '@/utils/request'

// 查询项目API列表
export function listApi(query) {
  return request({
    url: '/system/api/list',
    method: 'get',
    params: query
  })
}

// 查询项目API详细
export function getApi(apiId) {
  return request({
    url: '/system/api/' + apiId,
    method: 'get'
  })
}

// 新增项目API
export function addApi(data) {
  return request({
    url: '/system/api',
    method: 'post',
    data: data
  })
}

// 修改项目API
export function updateApi(data) {
  return request({
    url: '/system/api',
    method: 'put',
    data: data
  })
}

// 删除项目API
export function delApi(apiId) {
  return request({
    url: '/system/api/' + apiId,
    method: 'delete'
  })
}
