import request from '@/utils/request'

// 查询模块列表
export function listModule(query) {
  return request({
    url: '/system/module/list',
    method: 'get',
    params: query
  })
}

// 查询模块详细
export function getModule(moduleId) {
  return request({
    url: '/system/module/' + moduleId,
    method: 'get'
  })
}

// 新增模块
export function addModule(data) {
  return request({
    url: '/system/module',
    method: 'post',
    data: data
  })
}

// 修改模块
export function updateModule(data) {
  return request({
    url: '/system/module',
    method: 'put',
    data: data
  })
}

// 删除模块
export function delModule(moduleId) {
  return request({
    url: '/system/module/' + moduleId,
    method: 'delete'
  })
}
