import request from '@/utils/request'

// 查询测试计划子项列表
export function listPlanItem(query) {
  return request({
    url: '/system/PlanItem/list',
    method: 'get',
    params: query
  })
}

// 查询测试计划中显示的用例列表
export function listPlanItemCase(query) {
  return request({
    url: '/system/PlanItem/case/list',
    method: 'get',
    params: query
  })
}

// 查询模块列表
export function listPlanItemModule(query) {
  return request({
    url: '/system/PlanItem/module/list',
    method: 'get',
    params: query
  })
}

// 查询测试计划子项详细
export function getPlanItem(planItemId) {
  return request({
    url: '/system/PlanItem/' + planItemId,
    method: 'get'
  })
}

// 新增测试计划子项
export function addPlanItem(data) {
  return request({
    url: '/system/PlanItem',
    method: 'post',
    data: data
  })
}

// 修改测试计划子项
export function updatePlanItem(data) {
  return request({
    url: '/system/PlanItem',
    method: 'put',
    data: data
  })
}

// 删除测试计划子项
export function delPlanItem(planItemId) {
  return request({
    url: '/system/PlanItem/' + planItemId,
    method: 'delete'
  })
}
