import request from '@/utils/request'

// 查询测试计划列表
export function listPlan(query) {
  return request({
    url: '/system/plan/list',
    method: 'get',
    params: query
  })
}

// 查询测试计划详细
export function getPlan(planId) {
  return request({
    url: '/system/plan/' + planId,
    method: 'get'
  })
}

// 新增测试计划
export function addPlan(data) {
  return request({
    url: '/system/plan',
    method: 'post',
    data: data
  })
}

// 修改测试计划
export function updatePlan(data) {
  return request({
    url: '/system/plan',
    method: 'put',
    data: data
  })
}

// 删除测试计划
export function delPlan(planId) {
  return request({
    url: '/system/plan/' + planId,
    method: 'delete'
  })
}