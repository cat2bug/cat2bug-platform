import request from '@/utils/request'

// 查询测试用例列表
export function listCase(query) {
  return request({
    url: '/system/case/list',
    method: 'get',
    params: query
  })
}

// 获取指定模块的用例ID集合
export function listPlanCaseId(moduleId) {
  return request({
    url: `/system/case/module/${moduleId}/ids`,
    method: 'get',
  })
}

// 查询测试用例详细
export function getCase(caseId) {
  return request({
    url: '/system/case/' + caseId,
    method: 'get'
  })
}

// 新增测试用例
export function addCase(data) {
  return request({
    url: '/system/case',
    method: 'post',
    data: data
  })
}

// 批量新增测试用例
export function batchAddCase(data) {
  return request({
    url: '/system/case/batch',
    method: 'post',
    data: data
  })
}

// 修改测试用例
export function updateCase(data) {
  return request({
    url: '/system/case',
    method: 'put',
    data: data
  })
}

// 删除测试用例
export function delCase(caseId) {
  return request({
    url: '/system/case/' + caseId,
    method: 'delete'
  })
}

export function closeEditWindow() {
  return request({
    url: '/system/case/close-edit-window',
    method: 'post',
  })
}
